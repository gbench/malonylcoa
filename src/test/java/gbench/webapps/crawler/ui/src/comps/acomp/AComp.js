import { mapGetters } from "vuex";
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
			this.name = this.component = data.name + " @ " + data.time;
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
	},

	/**
	 * 
	 */
	methods: {
		/**
		 * 全文检索 
		 * @param {*} event 
		 */
		on_srch_click(event) {
			const keyword = $(event.target).val();
			// 开始信息
			http_post("/h5/api/srch/lookup", { keyword: keyword }).then(res => {
				const lines= res.data.result;
				this.lines = lines;
			});
		}
	}

};

export { AComp };