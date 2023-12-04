import { mapGetters, mapState } from "vuex";
import { http_post, http_get, sqlquery, sqlquery2, sqlexecute } from "../../gbench/util/sqlquery";
import $ from "jquery";

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
			fileHome: "F:/slicef/ws/gitws/malonylcoa/src/test/java/gbench/webapps/crawler/api/model/data/docs"
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
			this.component = data.name + " @ " + data.time;
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
			return (td, h, line, i) => (n => 'text,position'.indexOf(h) < 0 ? td
				: td.substr(0, n).replace(/\s+/g, '') + (td.length > n ? '...' : ''))(maxsize)
				.replace(/^[。，：”】；）]+/, '');
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
		 * 全文检索 
		 * @param {*} event 
		 */
		on_srch_click(event) {
			const keyword = this.keyword;
			// 开始信息
			http_post("/h5/api/srch/lookup", { keyword: keyword }).then(res => {
				const lines = res.data.result;
				this.lines = lines.map(this.remove_keys("file,search_field,position,score,position,py0,py1"));
			});
		},

		/**
		 * 全文检索 
		 * @param {*} event 
		 */
		on_srch_click2(event) {
			const keyword = this.keyword;
			// 开始信息
			http_post("/h5/api/srch/lookup2", { line: keyword, sessId: 1, agentId: 1, size: 10 }).then(res => {
				const lines = res.data.result;
				this.lines = lines.map(this.remove_keys("file,search_field,position,score,position,py0,py1"));
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
				_: Math.random() // '_'用于占位符参数，以保证webflux的方法调用,post方法不能没有参数
			}).then(res => {
				const lines = res.data.result;
				alert(JSON.stringify(lines));
			});
		}

	}

};

export { AComp };