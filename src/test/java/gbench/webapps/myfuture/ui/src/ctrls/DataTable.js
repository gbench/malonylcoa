import _ from 'lodash';

const DataTable = {
    template: ` 
    <table :id="id" :class="tblclass" style="border-collapse:collapse"> 
            <tr> 
                <th :class="thclass" 
                    v-for="h in heads(data)">{{label(h)}}</th> 
            </tr> 
            <tr v-for="line,i in data" 
                :class="f_trclass(line,i)" 
                @click="on_tr_click($event,line,i)" 
                @mouseover="on_tr_mouseover($event,line,i)" > 
                <td :class="tdclass" 
                    @click="on_td_click($event,line[h],h,line,i)" 
                    @blur="on_td_blur($event,line[h],h,line,i)" 
                    v-for="h in heads(data)" v-html="f_tdrender(line[h],h,line,i)" > 
                </td> 
            </tr> 
    </table> 
    `,

    props: { // 组件属性
        id: String, // 组件id
        data: Array, // 组件数据
        dics: { type: Object, default: () => { } }, // 键名字典
        tblclass: { type: String, default: "tblclass" }, // table class 
        thclass: { type: String, default: "thclass" }, // th classs 
        tdclass: { type: String, default: "tdclass" }, // td class
        trclass: { type: Function, default: () => { } }, // tr class 
        tdrender: { type: Function, default: (td, h, line, i) => td }, // td 渲染函数
        header: { type: Function, default: null }, // 表头函数
    },

    computed: {

    },

    methods: {

        label(key) {
            let lbl = key;
            if (this.dics) {
                const v = this.dics[key];
                if (v) {
                    lbl = v;
                }
            }

            return lbl;
        },

        /**
         * 提取表头表头数据
         * 
         * @param {*} dd 列表数据
         * @returns 表头数据
         */
        heads(dd) {
            if (this.header) { // 头部函数有效
                return this.header(dd);
            } else { // 头部函数无效
                if (!dd || dd.length < 1) {
                    return [];
                } else { // 遍历所有的 数据 元素 获取 完整的 键名列表
                    return _.reduce(dd.map(e => Object.keys(e)), (acc, cur) => { // 累计元素,当前元素
                        const diffs = _.difference(cur, acc); // 提取差异部分，acc 中所没有的部分
                        return _.concat(acc, diffs);
                    });
                } // if
            } // if header
        },

        /**
         * 
         * @param {*} event 事件
         * @param {*} line 行记录
         * @param {*} i 行号
         */
        on_tr_click(event, line, i) {
            this.$emit("trclick", { line, i, event });
        },

        /**
         * 
         * @param {*} event 事件
         * @param {*} td 单元格
         * @param {*} h 表头
         * @param {*} line 行记录
         * @param {*} i 行号
         */
        on_td_click(event, td, h, line, i) {
            this.$emit("tdclick", { td, h, line, i, event });
        },

        /**
         * 
         * @param {*} event 事件
         * @param {*} td 单元格
         * @param {*} h 表头
         * @param {*} line 行记录
         * @param {*} i 行号
         */
        on_td_blur(event, td, h, line, i) {
            this.$emit("tdblur", { td, h, line, i, event });
        },

        /**
         * 
         * @param {*} event 事件
         * @param {*} line 行记录
         * @param {*} i 行号
         */
        on_tr_mouseover(event, line, i) {
            this.$emit("trmouseover", { line, i, event });
        },

        /**
         * 渲染行class
         * 
         * @param {*} line 
         * @param {*} i 
         * @returns 
         */
        f_trclass(line, i) {
            if (this.trclass && typeof this.trclass == "function") {
                return this.trclass(line, i);
            } else {
                return "trclass";
            }
        },

        /**
         *  td 值的渲染处理：渲染结果为html
         * 
         * @param {*} td 
         * @param {*} h 
         * @param {*} line 
         * @param {*} i 
         * @returns 
         */
        f_tdrender(td, h, line, i) {
            if (this.tdrender && typeof this.tdrender == "function") {
                return this.tdrender(td, h, line, i);
            } else {
                return td;
            }
        },

    }

};

export { DataTable };