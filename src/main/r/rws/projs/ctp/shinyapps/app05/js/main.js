// main.js
const { Kafka } = require('kafkajs');
const { IgniteClient } = require('apache-ignite-client');
const { klines } = require('./klines');
const WebSocket = require('ws');          // 推送给前端

/* 配置常量（对应你给出的 Java 片段） */
const CTP_TOPIC   = 'test_cxx_ctp_topic';
const KAFKA_BS    = '192.168.1.41:9092';
const GROUP_ID    = 'ctp_cxx_ctp_topic_group_ignite-3.10';
const IGNITE_ADDR = '192.168.1.41:10800';
const PREFIX_TK   = 'TK';
const PREFIX_KL   = 'KL';
const TNAME       = 'TBL';

/* 1. 连接 Ignite */
const ignite = new IgniteClient();
ignite.connect(IGNITE_ADDR).then(() => console.log('Ignite connected'));

/* 2. 连接 Kafka */
const kafka = new Kafka({ clientId: 'kline-node', brokers: KAFKA_BS.split(',') });
const consumer = kafka.consumer({ groupId: GROUP_ID });
await consumer.connect();
await consumer.subscribe({ topic: CTP_TOPIC, fromBeginning: false });

/* 3. 启动本地 WebSocket 服务器，向前端推 K 线 */
const wss = new WebSocket.Server({ port: 8080 });
wss.on('connection', ws => {
  console.log('前端图表已连接');
  ws.on('close', () => console.log('前端断开'));
});

function broadcast(kline) {
  wss.clients.forEach(ws => {
    if (ws.readyState === WebSocket.OPEN) ws.send(JSON.stringify(kline));
  });
}

/* 4. 消费 Tick → 1 分钟 K 线 → 写 Ignite → 调 klines 更新缓存 → 推前端 */
const MIN1 = 60_000;
let currentBar = null;          // 当前正在累积的 1 分钟 Bar

consumer.run({
  eachMessage: async ({ message }) => {
    const tick = JSON.parse(message.value.toString()); // {sym, ts, px, vol}
    const ts = new Date(tick.ts);
    const px = +tick.px;
    const vol = +tick.vol;
    const barTs = new Date(Math.floor(ts / MIN1) * MIN1).toISOString();

    /* 初始化或换分钟 */
    if (!currentBar || currentBar.TS !== barTs) {
      if (currentBar) {
        /* 上一根 Bar 完成，写 Ignite */
        const sql = `INSERT INTO ${PREFIX_KL}_${tick.sym} (TS,O,H,L,C,V) VALUES (?,?,?,?,?,?)`;
        await ignite.query(sql, [
          currentBar.TS, currentBar.O, currentBar.H, currentBar.L, currentBar.C, currentBar.V
        ]);
        /* 刷新缓存并广播 */
        const arr = await klines(`${PREFIX_KL}_${tick.sym}`);
        broadcast(arr[arr.length - 1]);   // 把最新一根推给前端
      }
      currentBar = { TS: barTs, O: px, H: px, L: px, C: px, V: vol };
    } else {
      /* 更新当前 Bar */
      currentBar.H = Math.max(currentBar.H, px);
      currentBar.L = Math.min(currentBar.L, px);
      currentBar.C = px;
      currentBar.V += vol;
    }
  }
});
