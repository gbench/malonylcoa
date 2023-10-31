import { mapGetters } from "vuex";
import { http_post, http_get, sqlquery, sqlquery2, sqlexecute } from "../../gbench/util/sqlquery";

const AComp = {

	template: `<div class="highlight">{{name}}</div>`,

	/**
	 * 
	 * @returns 
	 */
	data() {
		return { label: "-", data: "-" };
	},

	/**
	 * 
	 */
	mounted() {
		console.log(this.name);

		// 开始信息
		http_get("/h5/api/hello", { }).then(res => {
			this.label = res.data.message + " @ " + res.data.time;
		});

		// sql data 
		sqlquery("SELECT H2VERSION() VERSION FROM DUAL").then(res => {
			this.data = res.data.data;
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