import { mapGetters, mapState } from "vuex";
import { PS, http_post, http_get, sqlquery, sqlquery2, sqlexecute } from "../../gbench/util/sqlquery";
import moment from "moment";
import _ from "lodash";

/**
 * 别名 
 * @param {*} obj 数据对象 
 * @param {*} dft 默认值
 * @returns 
 */
function alias(obj, dft) {
	return function (names) {
		return obj == null
			? _.values(names).reduce((acc, a) => { acc[a] = dft; return acc; }, {})
			: _.mapKeys(obj, (v, k) => names[k] ? names[k] : k);
	};
}

/**
 * 
 * @param {*} obj 
 * @param {*} path 
 * @returns 
 */
function pathget(obj, path) {
	const i = path.indexOf("/");
	if (i < 0) {
		return obj[path];
	} else {
		const key = path.substring(0, i);
		const _path = path.substring(i + 1);
		return pathget(obj[key], _path);
	}
}

/**
 * 提取数据 
 * @param {*} obj 
 * @param {*} keys 
 * @returns 
 */
function gets(obj, keys) {
	return _.pick(obj, keys.split(","));
}

/**
 * 依据键名进行分组
 *  
 * @param {*} key 
 * @param {*} lines 
 */
function assoc_by(key, lines) {
	return lines.reduce((acc, a) => {
		const value = a[key];
		const vv = acc[value];
		if (!vv) { // 第一次添加
			acc[value] = a;
		} else if (Array.isArray(vv)) { // 至少是第三次添加
			vv.push(value);
		} else { // 第二次添加
			acc[value] = [vv, a];
		} // if
		return acc;
	}, {});
}

/**
 * 转换成 列表
 * @param {*} obj 
 * @returns 
 */
function aslist(obj) {
	return Array.isArray(obj) ? obj : [obj];
}

/**
 * 
 */
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
		sqlquery2("show tables").then(data => {
			this.tables = data.map(e => { return { name: e["TABLE_NAME"] }; });
		});
	},

	/**
	 * 
	 */
	computed: {
		/**
		 * 公司id 
		 */
		company_id() {
			return this.current.company == null ? -1 : this.current.company.id;
		},

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
			sqlquery2(sql).then(e => {
				if (e.length > 0) { // 登录成功
					const user = e[0]; // 用户记录
					const sql2 = `select c.* from (
						select * from t_user_company where id='${user.id}'
					) uc left join t_company c on uc.company_id = c.id `;
					sqlquery2(sql2).then(e1 => {
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
			let conditions = "";

			switch (tbl) {
				case "t_order": {
					conditions = this.company_id < 0 ? "" : ` where parta=${this.company_id} or partb=${this.company_id}`;
					break;
				}
				case "t_payment": {
					conditions = this.company_id < 0 ? "" : ` where payer_id=${this.company_id} or payee_id=${this.company_id}`;
					break;
				}
				default: {

				}
			};
			sqlquery2(`select * from ${tbl} ${conditions}`).then(data => {
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
				const order_id = line["id"];
				const pcts = row.details.items; // 公司产品
				const ids = pcts.map(e => e.id); // 公司产品id

				// 逐渐展开处理层级
				if (ids.length > 0) { //  产品数大于0
					const pmt_sql = `select * from t_payment where id in (${ids.join(",")})`; // 支付集合
					sqlquery2(pmt_sql).then(_lines => { // 一级 
						const pid2pmts = assoc_by("product_id", _.flatMap(_lines,
							pmt => pathget(pmt, "details/items").map( // 支付行项目
								item => Object.assign({}, alias(item)({ id: "product_id" }), // 改名
									gets(pmt, "id,order_id,payer_id,payee_id") // 提取指定字段
								)))); // assocs 
						const __lines = pcts.flatMap(p => { // 为此产品数据添加支付信息
							const pmts = pid2pmts[p["id"]];
							return _.flatMap(aslist(pmts), pmt => {
								return Object.assign({}, p, alias(pmt, -1)({ id: "payment_id" }));
							});
						});
						return PS(__lines); // Promise对象
					}).then(__lines => { // 二级数据行
						const bill_sql = `select * from t_billof_product where order_id = ${order_id}`;
						return sqlquery2(bill_sql).then(bills => {
							const pid2bills = assoc_by("product_id", _.flatMap(bills,
								bill => pathget(bill, "details/items").map( // 支付行项目
									item => Object.assign({}, alias(item)({ id: "product_id" }), // 改名
										gets(bill, "id,bill_type,order_id,warehouse_id,freight_order_id") // 提取指定字段
									)))); // assocs
							const ___lines = __lines.flatMap(p => { // 为此产品数据添加支付信息
								const bills = pid2bills[p["id"]]; // 提取产品相关单据
								return _.flatMap(aslist(bills), bill => {
									return Object.assign({}, p, alias(bill, -1)({ id: "bill_id" }));
								});
							});
							return PS(___lines);
						}); // sqlquery2
					}).then(___lines => { // 三级数据行
						this.lines = ___lines;
					});
				} // if 产品数>0
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
			sqlquery2(`select * from ${tbl}`).then(data => {
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