// klines.js
// 内存缓存：Map 比 Object 更快，且保留插入顺序
const cache = new Map();          // key -> [{TS:'...', O,H,L,C,V}, ...]

/**
 * 模拟 R 语言里的 klines 函数
 * @param {string}  sym       表名，默认 'kl_rb2605'
 * @param {string}  startTime 开始时间（含） ISO 字符串，缺省表示“取缓存后全部”
 * @param {string}  endTime   结束时间（含） ISO 字符串
 * @returns {Promise<Array>}  本次应返回的 K 线数组
 */
async function klines(sym = 'kl_rb2605', startTime, endTime) {
  const key = sym;
  let lc = cache.get(key) || [];          // 本地拷贝
  const lastUpTime = lc.length ? lc[lc.length - 1].TS : '1970-01-01T00:00:00Z';

  /* 1. 如果查询区间完全落在缓存里，直接切片返回 */
  if (startTime && endTime && lc.length) {
    const first = lc[0].TS, last = lc[lc.length - 1].TS;
    if (first <= startTime && last >= endTime) {
      return lc.filter(r => r.TS >= startTime && r.TS <= endTime);
    }
  }

  /* 2. 构造 SQL：只拿比 lastUpTime 新的数据 */
  let where = `TS > '${lastUpTime}'`;
  if (startTime || endTime) {
    const arr = [];
    if (startTime) arr.push(`TS >= '${startTime}'`);
    if (endTime)   arr.push(`TS <= '${endTime}'`);
    where += ' AND ' + arr.join(' AND ');
  }
  const sql = `SELECT * FROM ${sym} WHERE ${where} ORDER BY TS`;
  console.log('[SQL]', sql);

  /* 3. 查库（这里用假函数 sqlQuery 演示，真实场景换成 Ignite thin-client） */
  const nu = await sqlQuery(sql);   // nu: 比 lastUpTime 新的记录

  /* 4. 删尾拼新 */
  const mu = lc.filter(r => r.TS !== lastUpTime); // 去掉最后一条（可能未闭合）
  const flag = !startTime && nu.some(r => r.TS === lastUpTime);
  const res = flag ? [...mu, ...nu] : nu;

  /* 5. 更新缓存 */
  if (!startTime) cache.set(key, res);
  return res;
}

/* 伪 sqlQuery：真实环境请用 ignite-client 的 sql 接口 */
async function sqlQuery(sql) {
  // 这里直接返回空数组，实际请调用 Ignite
  return [];
}

module.exports = { klines };
