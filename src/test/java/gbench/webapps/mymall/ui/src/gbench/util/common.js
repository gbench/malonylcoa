/**
 * 是否是url
 * @param {*} str 
 * @returns 
 */
function is_valid_url(str) {
    const a = document.createElement('a');
    a.href = str;
    return (a.host && a.host != window.location.host);
}

/**
 * 图片url的调整 
 * @param {*} url 
 */
function image_url(url) {
    return is_valid_url(url) ? url : `/h5/api/readfile?file=${url}`;
}

/**
 * 别名 
 * @param {*} obj 数据对象 
 * @param {*} dft 默认值
 * @returns 
 */
function alias(obj, dft) {
    return function (names) {
        const _names = _.isObject(names) ? names : {};
        let flag = false; // 是否是数组的标志位
        if ((flag = Array.isArray(names)) || _.isString(names)) { // names 的分类处理
            const ss = flag ? names : names.split(/[,;]+/);
            for (let i = 0; i < ss.length - 1; i += 2) { // 分解成old->new的二元组
                _names[ss[i]] = ss[i + 1];
            }
        }
        return obj == null
            ? _.values(_names).reduce((acc, a) => { acc[a] = dft; return acc; }, {})
            : _.mapKeys(obj, (v, k) => _names[k] ? _names[k] : k);
    };
}

/**
 * 路径读取 
 * @param {*} obj 
 * @param {*} path 
 * @returns 
 */
function pathget(obj, path) {
    const i = path.indexOf("/");
    if (i < 0) {
        return obj[path];
    } else {
        const key = path.substring(0, i);
        const _path = path.substring(i + 1);
        return pathget(obj[key], _path);
    }
}

/**
 * 提取数据 
 * @param {*} obj 
 * @param {*} keys 
 * @returns 
 */
function gets(obj, keys) {
    return _.pick(obj, keys.split(","));
}

/**
 * 带有默认属性的键值提取 
 * @param {*} obj 
 * @param {*} k 
 * @param {*} v 
 * @returns 
 */
function get(obj, k, v) {
    return !obj ? v : !obj[k] ? v : obj[k];
}

/**
 * 依据键名进行分组
 *  
 * @param {*} key 
 * @param {*} lines 
 */
function assoc_by(key, lines) {
    return lines.reduce((acc, a) => {
        const value = a[key];
        const vv = acc[value];
        if (!vv) { // 第一次添加
            acc[value] = a;
        } else if (Array.isArray(vv)) { // 至少是第三次添加
            vv.push(value);
        } else { // 第二次添加
            acc[value] = [vv, a];
        } // if
        return acc;
    }, {});
}

/**
 * 转换成 列表
 * @param {*} obj 
 * @returns 
 */
function aslist(obj) {
    return Array.isArray(obj) ? obj : [obj];
}

/**
 * 选入数据 
 * @param {*} items 数据集合
 * @param {*} item 待见检测数据
 */
function select(items, item) {
    const i = _.findIndex(items, _item => _.isEqual(_item, item));
    if (i >= 0) { // 已经存在 
        items.splice(i, 1);
        return false;
    } else {
        items.push(item);
        return true;
    }
}

/**
 * 清空数组 
 * @param {*} items 
 */
function clear(items) {
    if (Array.isArray(items)) {
        items.splice(0, items.length);
    }
}


export { is_valid_url, image_url, alias, pathget, gets, get, assoc_by, aslist, select, clear };