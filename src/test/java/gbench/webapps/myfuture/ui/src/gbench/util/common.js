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
    return !obj
        ? v
        : (!obj[k]
            ? v
            : obj[k]);
}

/**
 * 依据键名进行分组
 * 对于一个键k,当只有一个元素v的时候,键值保持原来不变.即k->v
 * 若是对应多个值,v0,v1则返回一个数组[v0,v1]对应键值,即k->[v0,v1]
 * 结果返回键值对
 *  
 * @param {*} keyname 键名用户从lines中的数据行提取分组k=line[keyname]
 * @param {*} lines 数据行[line]一个又line元素构成的数组 
 * @param {*} default_value 当line[keyname]属性不存在的时候采用的默认值
 * @returns 加过返回一个键值对儿对象{k->v,k1:[v0,v1,...]}
 */
function assoc_by(keyname, lines, default_value) {
    return lines.reduce((acc, line) => {
        const k = get(line, keyname, default_value); // 计算分组k
        const vv = acc[k]; // 提取键值对一个分组
        if (!vv) { // 第一次添加
            acc[k] = line;
        } else if (Array.isArray(vv)) { // 至少是第三次添加
            vv.push(line);
        } else { // 第二次添加
            acc[k] = [vv, line]; // 把acc[k]转换成键值
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