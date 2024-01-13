import axios from 'axios';

/**
 * http_post
 * 
 * @param {*} url 
 * @param {*} data 
 * @returns 数据请求
 */
const http_post = function (url, data) {
    return axios({
        headers: { // axios 默认 application/json
            "Content-Type": "application/x-www-form-urlencoded",
            "component-label": "mymall"
        },
        method: "post",
        url: url,
        data: data,
    });
};

/**
 * http_post
 * 
 * @param {*} url 
 * @param {*} data 
 * @returns 数据请求
 */
const http_get = function (url, data) {
    return axios({
        headers: { // axios 默认 application/json
            "Content-Type": "application/x-www-form-urlencoded",
            "component-label": "mymall"
        },
        method: "get",
        url: url,
        data: data,
    });
};

/**
 * 数据处理
 * 
 * @param {*} data 
 * @param {*} resolve 
 * @param {*} reject 
 */
function handle_response(response, resolve, reject) {
    const data = response.data.data;

    if (data.length > 0 && data[0]["$error"]) { // 出现了错误标记
        const { $error, $attributes, $exception } = data[0];
        const attrs = {}; // 错误明细
        if ($attributes) {
            const _attrs = $attributes[Object.keys($attributes)[0]];
            if (_attrs) { // 提取属性信息
                Object.keys(_attrs).reduce((acc, k) => { acc[k] = _attrs[k]; return acc; }, attrs);
            } // if
        } // if
        reject({ error: $error, attrs });
    } else {
        resolve(response);
    } // if
}

/**
 * 
 * @param {*} sql 
 * @returns 
 */
const sqlquery = function (sql) {

    const executor = (resolve, reject) => {
        axios({
            headers: { // axios 默认 application/json
                "Content-Type": "application/x-www-form-urlencoded",
                "component-label": "mymall"
            },
            method: "post",
            url: "/h5/finance/data/sqlquery",
            data: { sql: sql },
        }).then(response => {
            handle_response(response, resolve, reject);
        }).catch(reject);
    };

    return new Promise(executor);
};

/**
 * SQL数据请求,sqlquery 请求的简化版 
 * 
 * @param {*} sql 查询sql 
 * @param {*} data_handler 结果处理 
 * @returns 处理后的promise
 */
const sqlquery2 = function (sql, data_handler) {
    const _data_handler = !data_handler ? e => e : data_handler;
    return sqlquery(sql).then(r => new Promise((resolve, reject) => !r.data.data
        ? reject(r.data) // 异常处理
        : resolve(_data_handler(r.data.data)))); // 正常返回
};

/**
 * 
 * @param {*} sql 
 * @returns 
 */
const sqlexecute = function (sql) {

    const executor = (resolve, reject) => {
        axios({
            headers: { // axios 默认 application/json
                "Content-Type": "application/x-www-form-urlencoded",
                "component-label": "mymall"
            },
            method: "post",
            url: "/h5/finance/data/sqlexecute",
            data: { sql: sql },
        }).then(response => {
            handle_response(response, resolve, reject);
        }).catch(reject);
    };

    return new Promise(executor);
};

/**
 * 生成promise对象 
 * @param {*} x 
 * @returns 
 */
const PS = function (x) {
    return new Promise((resolve, reject) => !x
        ? reject("x is null") // 异常处理
        : resolve(x));
};

export { PS, http_post, http_get, sqlquery, sqlquery2, sqlexecute };