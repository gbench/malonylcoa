/**
 * 是否是url
 * @param {*} str 
 * @returns 
 */
function isValidURL(str) {
    const a = document.createElement('a');
    a.href = str;
    return (a.host && a.host != window.location.host);
}

/**
 * 图片url的调整 
 * @param {*} url 
 */
function image_url(url) {
    return isValidURL(url) ? url : `/h5/api/readfile?file=${url}`;
}

export default { isValidURL, image_url };