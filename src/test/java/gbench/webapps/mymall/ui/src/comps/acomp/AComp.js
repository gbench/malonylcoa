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
 * @param {*} items 数据集合
 * @param {*} item 待见检测数据
 */
function select(items, item) {
	const i = _.findIndex(items, _item => _.isEqual(_item, item));
	if (i >= 0) { // 已经存在 
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


// 订单头寸
const LONG = 1; // 长头 
const SHORT = -1; // 空头

// 初始化数据 
const INIT_DATA = {
	component: "-", //  组件名
	current: { // 当前对象
		//
		tbl: "-", // 表名
		tbl_index: -1, // 数据表行行索引
		//
		tbldata_index: -1, // 行数据索引
		tbldata_selected: [], // 表数据是否被选择
		//
		line_index: -1, // 明细行行索引
		lines_selected: [], // 明细行选择索引集合
		//
		warehouse_index: -1, // 表数据是否被选择
		warehouse_selected: [], // 表数据是否被选择
		//
		product_index: -1, // 表数据是否被选择
		product_selected: [], // 表数据是否被选择
		//
		user: { // 用户信息
			name: "gbench",
			password: "123456"
		},
		//
		company: null // 当前公司对象,仅当用户登录后才有效
	},  //  当前对象
	tables: [], // 数据表
	tbldata: [], // 表数据
	lines: [], // 行项目
	warehouses: [], // 仓库
	pid2pcts: [], // 公司产品id->产品明细 
	counterpart: -1, // 交易对手方
	counterparts: [], // 对手方
	position: SHORT, // 默认订单头寸,空头,即 创建一个卖出单,order的partb为当前的用户的company_id 
}; // INIT_DATA

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
		return Object.assign({}, INIT_DATA);
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
							const company_id = company.id; // 当前用户的企业id
							const sql3 = `select * from t_company_warehouse where company_id=${company_id}`;
							// 加载公司仓库信息	
							sqlquery2(sql3).then(data2 => {
								const warehouse_ids = data2.map(e => e["warehouse_id"]);
								return sqlquery2(`select * from t_warehouse where id in (${warehouse_ids.join(",")})`);
							}).then(data3 => { // 公司仓库
								this.warehouses = data3; // 加载公司仓库
							}); // 公司仓库
							// 加载公司信息
							sqlquery2(`select * from t_company where id!=${company_id}`).then(data => {
								this.counterparts = data.map(e => { return { key: e["name"], value: e["id"] }; });
								this.counterpart = this.counterparts[0].value; // 选择默认交易对手方
							}); // 公司信息
							//公司产品信息
							sqlquery2(`select cp.id, p.id product_id,p.name name, cp.attrs
								from t_company_product cp left join t_product p on cp.product_id = p.id`
							).then(data => {
								this.pid2pcts = assoc_by("id", data); // 公司产品id
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
			// 数据退出重置
			const reserved_words = "component,tables".split(","); // 保留数据内容
			this.current.company = null; // 公司清空
			Object.keys(INIT_DATA).filter(k => !_.includes(reserved_words, k)).forEach(key => {
				this.$data[key] = INIT_DATA[key];
			});
		},

		/**
		 * 查看数据
		 * @param {*} param0 
		 */
		on_tables_trclick({ line, i, event }) {
			this.current.tbl_index = i; // 设置表偏移索引
			this.current.tbldata_index = -1; // 设置一个非法值
			this.current.line_index = -1; // 设置表偏移索引
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
		 * 订单处理 
		 * @param {*} param0 
		 */
		handle_order({ line, i }) {
			const order_id = line["id"];
			const pcts = line.details.items; // 订单中的公司产品
			const ids = pcts.map(e => e.id); // 公司产品id

			// 逐渐展开处理层级
			if (ids.length < 1) { //  产品数大于0
				alert(`订单: ${JSON.stringify(line)} 中没有有效产品`);
				return;
			}

			const pmt_sql = `select * from t_payment where order_id=${order_id} and id in (${ids.join(",")})`; // 支付集合
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
				this.lines = _.sortBy(___lines.map(e => { // 翻译产品名称
					const id = e.id; // 公司产品id
					const name = _.defaults(this.pid2pcts[id], { name: "-" }).name; // 产品详情
					return Object.assign({ id, name }, e); // 加入产品名称字段
				}), e => e.id); // 按照产品id进行排序
			});
		},

		/**
		 * 公司产品 
		 * @param {*} param0 
		 */
		handle_company_product({ line, i }) {
			const attrs = line.attrs;
			const lines = Object.keys(attrs).map(k => { return { key: k, value: attrs[k] }; });
			this.lines = lines;
		},

		/**
		 * 仓库的处理 
		 * @param {*} param0 
		 */
		handle_warehouse({ line, i }) {
			select(this.warehouses, line);
		},

		/**
		 * 数据表的行点击 
		 * @param {*} param 
		 */
		on_tbldata_trclick({ line, i, event }) {
			if (event) { // 事件对象有效,无效事件对象是刷新请求
				this.current.line_index = -1;
				this.current.lines_selected = []; // 设置非法值
				if (select(this.current.tbldata_selected, i)) {
					this.current.tbldata_index = i;
				} else { //  清空当前选的行
					this.current.tbldata_index = -1;
					this.lines = [];
					return;
				};
			} // if

			switch (this.current.tbl) { // 表数据的处理
				case "t_order": return this.handle_order({ line, i });
				case "t_company_product": return this.handle_company_product({ line, i });
				case "t_warehouse": return this.handle_warehouse({ line, i });
				default: {
					alert(JSON.stringify(line));
				}
			} // switch

			console.log("on_tbl_data_trclick 的默认收尾", this.current.tbl);
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
		 * 刷新详情区域中的数据
		 */
		refresh_lines() {
			const i = this.current.tbldata_index;
			const line = this.tbldata[i];
			this.on_tbldata_trclick({ line, i }); // 注意这里传入了一个null的event对象
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
		 * 明细行项目 
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
			const counterpart = this.counterpart;  // 对手方
			const parta = this.position == LONG ? this.company_id : counterpart;
			const partb = this.position == SHORT ? this.company_id : counterpart;
			const volume = rnd(10); // 模拟订单产品规模
			const groups = assoc_by("id", // 依据产品id进行数据分组
				_.repeat("1", volume).split(/\s*/).map((v, i) => { // 随机生成数据序列
					return { id: rnd(10), quantity: rnd(10) }; // 随机生成产品id和交易数量
				})); // 随机生成订单项目
			const items = Object.keys(groups).map(id => { // 产品id
				const values = aslist(groups[id]); // 提取指定id订单产品项目列表
				const quantity = _.sumBy(values, e => e["quantity"]); // 累计交易数量
				const item = Object.assign(values[0], { quantity, price: rnd2(100) }); // 累计行项目的数量并补充价格
				return item; // 返回产品行项目
			});
			const order_bill = { // 订单数据
				name: "t_order", // 表名
				lines: [{ parta, partb, details: { items }, creator_id: 1, "time": now }] // 行项目 
			}; // order

			persist(order_bill).then(e => { this.refresh_tbldata("t_order"); });
		},

		/**
		 * 发票订单按钮是否开启 
		 * @returns 
		 */
		is_invoice_btn_enabled() {
			if (this.current.tbl != "t_order") { // 订单试图才能进行发货
				return false;
			}
			if (this.warehouses.length < 1) { // 至少需要有一个出品仓库
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
		 * 发票 
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

			persist(invoice_bill).then(e => { // 刷新订单行项目
				this.refresh_lines();
			});
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