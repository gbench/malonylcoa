import { mapGetters, mapState } from "vuex";
import { http_post, http_get, sqlquery, sqlquery2, sqlexecute } from "../../gbench/util/sqlquery";

const AComp = {

	template: `<div class="highlight">{{name}}</div>`,

	/**
	 * 
	 * @returns 
	 */
	data() {
		return { component: "-", tables: [], tbldata: [] };
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
		sqlquery("show tables").then(res => {
			this.tables = res.data.data;
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

	methods: {

		/**
		 * 查看数据
		 */
		on_tr_click({ line, i, event }) {
			const tbl = (line["TABLE_NAME"]);
			sqlquery2(`select * from ${tbl}`, e => e).then(data => {
				this.tbldata = data;
			});
		}
	}

};

export { AComp };