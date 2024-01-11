import { mapGetters, mapState } from "vuex";
import { http_post, http_get, sqlquery, sqlquery2, sqlexecute } from "../../gbench/util/sqlquery";
import moment from "moment";

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
			const row = this.tbldata[i];
			if ("t_order" == tbl) {
				this.details = row.details.items;
			} else if ("t_company_product" == tbl) {
				this.details = row.attrs;
			}
		},

		/**
		 * 刷新表数据
		 * @param {*} tbl 表名 
		 */
		refresh_tbldata(tbl) {
			sqlquery2(`select * from ${tbl}`, e => e).then(data => {
				this.tbldata = data;
			});
		},

		/**
		 * 随机创建订单 
		 * @param {*} event 
		 */
		on_order_btn_click(event) {
			const rnd = n => parseInt((Math.random() * n) + 1);
			const rnd2 = n => (Math.random() * n + 1).toFixed(2);
			const now = moment().format("YYYY-MM-DD HH:mm:ss");
			const flag = Math.random() > 0.5;
			const order = { // 订单数据
				name: "t_order",
				lines: [
					{
						parta: flag ? 1 : 2, partb: flag ? 2 : 1,
						details: {
							items: [ // 订单项目
								{ id: rnd(10), quantity: rnd(10), price: rnd2(5) },
								{ id: rnd(10), quantity: rnd(10), price: rnd2(3) }
							],
						},
						creator_id: 1, "time": now
					}
				] // lines
			}; // order
			http_post("/h5/finance/data/insert", { json: JSON.stringify(order) }).then(e => {
				this.refresh_tbldata("t_order");
			});
		}

	}

};

export { AComp };