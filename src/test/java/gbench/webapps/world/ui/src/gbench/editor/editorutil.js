import $ from "jquery";
import JSONEditor from "jsoneditor";

/**
 * 添加 JSON 编辑器
 * 
 * @param {*} panel 停靠面板 一般为 一个 div 
 * @param {*} position {left,right} 
 * @param {*} jsondata 绑定到面板上的数据
 * @param {*} to_url  goto 地址 比如: #/CComp
 * @param {*} title 编辑器标题 
 * @param {*} event_btns 事件按钮, 默认为 "confirm,discard" 用逗号分隔
 */
function add_json_editor(panel, { left, top }, jsondata, to_url, title, event_btns) {

    return new Promise((resolve, reject) => {
        const $panel = $(panel); // 操作面板
        const css = { display: "block", left, top };
        $panel.css(css);
        const title_html = !title ? "" : title; // 标题区域的html
        const buttons = (!event_btns ? "confirm,discard" : event_btns).split(",");
        const buttons_panel = buttons
            .map(e => `<a href='${to_url}' id='${e}'>${e.toUpperCase()}</a>`)
            .join("&nbsp;&nbsp;\n");
        $panel.html(`<div>
            ${title_html}
            <div>
                <div id='jsoneditor'></div>
                <hr>
                <div style='font-weight:bold'> ${buttons_panel} </div>
            </div>
        </div>`); // 生成JSON编辑器
        const local = {}; // 本地变量
        buttons.forEach(button => {
            const button_handler = e => { // 确认处理器
                resolve({ event: button, data: local.editor.get() });
                $(panel).css({ display: "none" });
            };
            $(`#${button}`).on("click", button_handler);
        }); // forEach
        const container = $("#jsoneditor", $panel)[0];
        const options = {
            mode: "tree",
            onChangeJSON(json) { // 变更内容回调
                console.log(json);
            } // onChangeJSON
        }; // 编辑器 选项
        const editor = new JSONEditor(container, options);
        editor.set(jsondata);
        local.editor = editor;
    }); // Promise
}

export { add_json_editor };