import { mapGetters, mapState } from "vuex";
import { http_post, http_get, sqlquery, sqlquery2, sqlexecute } from "../../gbench/util/sqlquery";
import moment from "moment";
import _ from "lodash";

const AComp = {

	template: `<div class="highlight">{{name}}</div>`,

	/**
	 * 
	 * @returns 
	 */
	data() {
		return {
			component: "-", //  组件名
			current: { // 当前对象
				data_index: -1, // 行数据索引
				tbl: "-", // 表名
				tbl_index: -1, // 数据表行行索引
				user: { // 用户信息
					name: "gbench",
					password: "123456"
				},
				company: null // 当前公司对象
			},  //  当前对象
			tables: [], // 数据表
			tbldata: [], // 表数据
			lines: [] // 行项目
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
		sqlquery("show tables").then(res => {
			this.tables = res.data.data.map(e => { return { name: e["TABLE_NAME"] }; });
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
		 * 
		 * @param {*} event 
		 */
		on_login_click(event) {
			const name = this.current.user.name;
			const password = this.current.user.password;
			const sql = `select * from t_user where name='${name}' and password='${password}'`;
			sqlquery2(sql, e => e).then(e => {
				if (e.length > 0) { // 登录成功
					const user = e[0];
					const sql2 = `select c.* from (
						select * from t_user_company where id='${user.id}'
					) uc left join t_company c on uc.company_id = c.id `;
					sqlquery2(sql2, e => e).then(e1 => {
						if (e1.length > 0) {
							this.current.company = e1[0];
						}// if
					}); //
				} else {
					alert("登录失败!");
				}
			});
		},

		/**
		 * 退出当前系统 
		 * @param {*} event 
		 */
		on_logout_click(event) {
			this.current.company = null;
		},

		/**
		 * 查看数据
		 * @param {*} param0 
		 */
		on_tables_trclick({ line, i, event }) {
			this.current.tbl_index = i; // 设置表偏移索引
			this.current.data_index = -1; // 设置一个非法值
			const tbl = this.current.tbl = (line["name"]); // 更新当前表
			sqlquery2(`select * from ${tbl}`, e => e).then(data => {
				this.tbldata = data;
			});
		},

		/**
		 * 数据表的行点击 
		 * @param {*} param 
		 */
		on_tbldata_trclick({ line, i, event }) {
			this.current.data_index = i;
			const tbl = this.current.tbl;
			const row = this.tbldata[i];
			if ("t_order" == tbl) { // 订单表的行项目的处理
				this.lines = row.details.items;
				const product_ids = this.lines.map(e => e.id);
				if (product_ids.length > 0) {
					const sql = `select * from t_payment where id in (${product_ids.join(",")})`;
					sqlquery2(sql, e => e).then(e => {
						this.lines = e;
					});
				}
			} else if ("t_company_product" == tbl) {
				const lines = Object.keys(row.attrs).map(k => { return { key: k, value: row.attrs[k] }; });
				this.lines = lines;
			}
		},

		/**
		 * 数据元素渲染 
		 * @param {*} td 
		 * @param {*} h 
		 * @param {*} line 
		 * @param {*} i 
		 */
		tbldata_td_render(td, h, line, i) {
			if (_.isObject(td)) { // json 对象格式化
				return JSON.stringify(td);
			} else {
				return td;
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