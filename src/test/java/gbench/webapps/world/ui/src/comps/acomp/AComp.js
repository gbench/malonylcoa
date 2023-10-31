import { mapGetters } from "vuex";
import { http_post, http_get, sqlquery, sqlquery2, sqlexecute } from "../../gbench/util/sqlquery";

const AComp = {

	template: `<div class="highlight">{{name}}</div>`,

	/**
	 * 
	 * @returns 
	 */
	data() {
		return { label: "-", articles: [] };
	},

	/**
	 * 
	 */
	mounted() {
		console.log(this.name);

		// 开始信息
		http_post("/h5/api/component", {name:"AComp"}).then(res => {
			const data = res.data.data;
			this.label = data.name + " @ " + data.time;
		});

		// sql data 
		sqlquery("SELECT ID,TITLE,VOLUME,TIME FROM t_maozedong LIMIT 10").then(res => {
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
	}

};

export { AComp };