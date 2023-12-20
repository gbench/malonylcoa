import { mapGetters, mapState } from "vuex";
import { http_post, http_get, sqlquery, sqlquery2, sqlexecute } from "../../gbench/util/sqlquery";
import $ from "jquery";
import moment from "moment";

const removed_srch_keys = "file,search_field,score,py0,py1"; // 查询结果移除键名

/**
 * AComp 
 */
const AComp = {

	template: `<div class="highlight">{{name}}</div>`,

	/**
	 * 
	 * @returns 
	 */
	data() {
		return {
			component: "-",
			articles: [],
			lines: [],
			keyword: "",
			sessId: moment().format("YYYYMMDDHHmmssSSSS"), // 会话id,每次页面刷新会重新生成
			agentId: 1, // 客户端id
			lookup2_size: 10, // lookup2的页面请求的大小
			fileHome: "F:/slicef/ws/gitws/malonylcoa/src/test/java/gbench/webapps/crawler/api/model/data/docs",
			corpusHome: "F:/slicef/ws/gitws/malonylcoa/src/test/java/gbench/webapps/crawler/api/model/data/corpus",
		};
	},

	/**
	 * 
	 */
	mounted() {
		console.log(this.name);

		// 开始信息
		http_post("/h5/api/component", { name: "AComp" }).then(res => {
			const data = res.data.data;
			this.state.name = data.name;
			this.component = data.name + " In " + data.service + " @ " + data.time;
		});

		// sql data 
		sqlquery("SELECT ID,TITLE,VOLUME,TIME FROM t_maozedong LIMIT 5").then(res => {
			this.articles = res.data.data;
		});
	},

	/**
	 * 
	 */
	computed: {
		/**
		 * Getters 数据
		 */
		...mapGetters("ACompStore", ["name"]),
		...mapState("ACompStore", { state: state => state }),
	},

	/**
	 * 
	 */
	methods: {

		/**
		 * 限宽渲染
		 * @param {*} maxsize 最大字数
		 * @returns datatable 渲染函数
		 */
		max_render(maxsize) {
			return (td, h, line, i) => {
				const pattern = /^[;。，：”】；）]+/;
				const s = (n => 'text,snapfile'.indexOf(h) < 0 ? td
					: td.substr(0, n).replace(/\s+/g, '') + (td.length > n ? '...' : ''))(maxsize)
					.replace(pattern, '');
				return `<a title='${td.replace(pattern, '')}'>${s}</a>`;
			};
		},

		/**
		 * 删除指定键名序列
		 * @param {*} keys 键名序列，用逗号分割 
		 * @returns 
		 */
		remove_keys(keys) {
			return e => { // 行数据调整
				keys.split(/[,]+/).forEach(k => delete e[k]);  // 删除过滤字段
				return e;
			};
		},

		/**
		 * 格式化检索 
		 * @param {*} e 
		 * @returns 
		 */
		format_srch_line(e) {
			if (e['snapfile']) { // 快照文件
				e['snapfile'] = e['snapfile'].replace(/.*\/([^/]+)\/[^/]+$/, "$1");
			} // if

			const json_pos = e['position'].replace(/\=/g, ":"); // 改换成json格式
			const pos = eval(`(${json_pos})`);
			if (pos) { // 关键词位置
				e['position'] = `${pos.rownum}#${pos.start},${pos.end - pos.start + 1}`;
			} // if

			const symbol = e['symbol'];
			const text = e['text'];
			const tags = e['tags'];
			e['text'] = text.replace(new RegExp(symbol, "g"), `【${symbol}】`); // 修改文本的内容
			e['tags'] = tags.replace(/^\[|\]$/g, ""); // 去除首尾的括号

			return e;
		},

		/**
		 * 全文检索 
		 * @param {*} event 
		 */
		on_srch_click(event) {
			const keyword = this.keyword;
			// 开始信息
			http_post("/h5/api/srch/lookup", { keyword: keyword }).then(res => {
				const lines = res.data.result;
				this.lines = lines.map(this.remove_keys(removed_srch_keys)).map(this.format_srch_line);
			});
		},

		/**
		 * 全文检索 
		 * @param {*} event 
		 */
		on_srch_click2(event) {
			const keyword = this.keyword;
			// 开始信息
			http_post("/h5/api/srch/lookup2", {
				line: keyword, sessId: this.sessId, agentId: this.agentId, size: this.lookup2_size
			}).then(res => {
				const lines = res.data.result;
				this.lines = lines.map(this.remove_keys(removed_srch_keys)).map(this.format_srch_line);
			});
		},

		/**
		 * 全文检索 
		 * @param {*} event 
		 */
		on_clear_lookup2_click(event) {
			const keyword = this.keyword;
			// 开始信息
			http_post("/h5/api/srch/clear_lookup2", {
				line: keyword, sessId: this.sessId, agentId: this.agentId, size: this.lookup2_size
			}).then(res => {
				const lines = res.data;
				alert(JSON.stringify(lines));
			});
		},

		/**
		 * 索引函数
		 * @param {*} event 
		 */
		on_index_click(event) {
			const fileHome = this.fileHome;
			// 开始信息
			http_post("/h5/api/srch/indexfiles", { fileHome }).then(res => {
				const lines = res.data.result;
				alert(JSON.stringify(lines));
			});
		},

		/**
		 * 刷新词汇表
		 * @param {*} event 
		 */
		on_refresh_click(event) {
			// 开始信息, 
			http_post("/h5/api/srch/refresh", {
				_: Math.random(), // '_'用于占位符参数，以保证webflux的方法调用,post方法不能没有参数
				corpusHome: this.corpusHome
			}).then(res => {
				const lines = res.data.result;
				alert(JSON.stringify(lines));
			});
		}

	}

};

export { AComp };