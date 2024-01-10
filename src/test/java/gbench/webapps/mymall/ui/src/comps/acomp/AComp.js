import { mapGetters, mapState } from "vuex";
import { http_post, http_get, sqlquery, sqlquery2, sqlexecute } from "../../gbench/util/sqlquery";

const AComp = {

	template: `<div class="highlight">{{name}}</div>`,

	/**
	 * 
	 * @returns 
	 */
	data() {
		return { component: "-", tbl: "-", tables: [], tbldata: [], details: [] };
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
		 * @param {*} param0 
		 */
		on_tables_trclick({ line, i, event }) {
			this.tbl = (line["TABLE_NAME"]);
			sqlquery2(`select * from ${this.tbl}`, e => e).then(data => {
				this.tbldata = data;
			});
		},

		/**
		 * 数据表的行点击 
		 * @param {*} param0 
		 */
		on_tbldata_trclick({ line, i, event }) {
			const tbl = this.tbl;
			const row = this.tbldata[0];
			if ("t_order" == tbl) {
				this.details = row.details.items;
			} else if ("t_company_product" == tbl) {
				this.details = row.attrs;
			}
		},

		/**
		 * 
		 * @param {*} event 
		 */
		on_order_btn_click(event) {
			const order = {
				key: "t_order", lines: [
					{
						parta: 1, partb: 2, details: {
							items: [
								{ id: 1, quantity: 1, price: 1 },
								{ id: 1, quantity: 1, price: 1 }
							]
						}
					}
				]
			};
			http_post("/h5/finance/data/insert", { json: JSON.stringify(order) }).then(e => {
				alert(JSON.stringify(e.data));
			});
		}

	}

};

export { AComp };