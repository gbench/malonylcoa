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
 * 选入数据 
 * @param {*} items 
 * @param {*} item 
 */
function select(items, item) {
	const i = _.findIndex(items, x => item == x);
	if (i >= 0) {
		items.splice(i, 1);
		return false;
	} else {
		items.push(item);
		return true;
	}
}

/**
 * 持久化数据 
 * 
 * @param entity 实体对象
 */
function persist(entity) {
	return http_post("/h5/finance/data/insert", { json: JSON.stringify(entity) });
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
				tbl: "-", // 表名
				tbl_index: -1, // 数据表行行索引
				tbldata_index: -1, // 行数据索引
				tbldata_selected: [], // 表数据是否被选择
				line_index: -1, // 明细行行索引
				lines_selected: [], // 明细行选择索引集合
				warehouse_index: [], // 表数据是否被选择
				warehouse_selected: [], // 表数据是否被选择
				product_index: [], // 表数据是否被选择
				product_selected: [], // 表数据是否被选择
				user: { // 用户信息
					name: "gbench",
					password: "123456"
				},
				company: null // 当前公司对象
			},  //  当前对象
			tables: [], // 数据表
			tbldata: [], // 表数据
			lines: [], // 行项目
			warehouses: [], // 仓库
			products: [] // 产品 
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
					sqlquery2(sql2).then(data1 => {
						if (data1.length > 0) {
							const company = this.current.company = data1[0];
							const sql3 = `select * from t_company_warehouse where company_id=${company.id}`;
							sqlquery2(sql3).then(data2 => {
								const warehouse_ids = data2.map(e => e["warehouse_id"]);
								return sqlquery2(`select * from t_warehouse where id in (${warehouse_ids.join(",")})`);
							}).then(data3 => {
								this.warehouses = data3; // 加载共公司仓库
							});
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
			this.current.tbldata_index = -1; // 设置一个非法值
			this.current.line_index = i; // 设置表偏移索引
			this.lines = []; // 设置非法值 
			this.current.tbldata_selected = []; // 设置非法值
			this.current.lines_selected = []; // 设置非法值
			this.current.product_selected = []; // 设置非法值
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
			this.current.line_index = -1;
			this.current.lines_selected = []; // 设置非法值
			if (select(this.current.tbldata_selected, i)) {
				this.current.tbldata_index = i;
			} else { //  清空当前选的行
				this.current.tbldata_index = -1;
				this.lines = [];
				return;
			};

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
			} else if ("t_warehouse" == tbl) {
				select(this.warehouses, line);
			}
		},

		/**
		 * 数据表的行点击 
		 * @param {*} param 
		 */
		on_warehouse_trclick({ line, i, event }) {
			if (select(this.current.warehouse_selected, i)) {
				this.current.warehouse_index = i;
			} else {
				this.current.warehouse_index = -1;
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
		 * 行是否被选中 
		 * @param {*} i 
		 */
		is_tbldata_selected(i) {
			return _.includes(this.current.tbldata_selected, i);
		},

		/**
		 * 行是否被选中 
		 * @param {*} i 
		 */
		is_line_selected(i) {
			return _.includes(this.current.lines_selected, i);
		},

		/**
		 * 行是否被选中 
		 * @param {*} i 
		 */
		is_warehouse_selected(i) {
			return _.includes(this.current.warehouse_selected, i);
		},

		/**
		 * 
		 * @param {*} param0 
		 */
		on_lines_trclick({ line, i, event }) {
			if (select(this.current.lines_selected, i)) {
				this.current.line_index = i;
			} else {
				this.current.line_index = -1;
			}
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

			persist(order).then(e => { this.refresh_tbldata("t_order"); });
		},

		/**
		 * 发票订单按钮是否开启 
		 * @returns 
		 */
		is_invoice_btn_enabled() {
			if (this.current.tbl != "t_order") { // 订单试图才能进行发货
				return false;
			}
			if (this.warehouses.length < 2) {
				return false;
			}
			const order = this.tbldata[this.current.tbldata_index];
			if (!order || !order.partb || this.company_id != order.partb) { // 只有当前公司id是乙方公司才能进行发货 
				return false;
			}
			const lines = this.current.lines_selected.map(i => this.lines[i])
				.filter(e => !_.includes("invoice,receipt".split(","), e["bill_type"]));
			if (lines.length < 1) {
				return false;
			}
			return true;
		},

		/**
		 * 
		 * @param {*} event 
		 */
		on_invoice_btn_click(event) {
			const lines = this.current.lines_selected.map(i => this.lines[i])
				.filter(e => !_.includes("invoice,receipt".split(","), e["bill_type"]));
			const order_id = this.tbldata[this.current.tbldata_index].id;
			const warehouse_id = _.defaults(this.warehouses[this.current.warehouse_index],
				this.warehouses[0]).id; // 默认仓库id
			const items = lines.map(e => gets(e, "id,quantity,price"));
			const invoice_bill = {
				name: "t_billof_product",
				lines: [{
					bill_type: "invoice",
					company_id: this.company_id,
					warehouse_id: warehouse_id,
					order_id: order_id,
					freight_order_id: -1,
					details: { items },
					creator_id: 1
				}]
			};

			persist(invoice_bill).then(e => { this.refresh_tbldata("t_billof_product"); });
		},

		/**
		 * 
		 * @param {*} event 
		 */
		on_receipt_btn_click(event) {
			alert("receipt btn");
		},

		/**
		 * 
		 * @param {*} event 
		 */
		on_payment_btn_click(event) {
			alert("payment btn");
		},

		/**
		 * 
		 * @param {*} event 
		 */
		on_freight_btn_click(event) {
			alert("freight btn");
		},

	}

};

export { AComp };