import { mapGetters, mapState } from "vuex";
import { PS, http_post, http_get, sqlquery, sqlquery2, sqlexecute } from "../../gbench/util/sqlquery";
import { is_valid_url, image_url, alias, pathget, gets, get, assoc_by, aslist, select, clear } from "../../gbench/util/common";
import moment from "moment";
import $ from "jquery";
import ztree from "ztree";
import _ from "lodash";
import "../../css/acomp.css";

/**
 * 持久化数据 
 * 设计BUG:t_freight_order应该是多对一于t_billof_product(invoice)的但是这里给设计成了一对一的了。
 * 于是当为一个发货单进行多次货运t_freight_order的时候,invoice只能记录第一次给予发货的货单,结果就是
 * 对于一个invoice只要货运一次,不是不是将其所有产品都给予货运,系统一概都将该invoice的中的商品视为全部
 * 货运,结果就造成了收货方的应收于实收不一致的情况了。
 * 
 * @param entity 实体对象
 */
function persist(entity, data_handler) {
	const _data_handler = !data_handler ? e => e.data.data : data_handler;
	return http_post("/h5/finance/data/insert", { json: JSON.stringify(entity) })
		.then(e => { return PS(_data_handler(e)); });
}

/**
 * 获取所持有的订单头寸
 * @param {*} order 
 */
function position(order, entity_id) {
	const parta_id = order.parta_id;
	const partb_id = order.partb_id;

	if (_.isEqual(entity_id, parta_id) && !_.isEqual(entity_id, partb_id)) {
		return 1;
	} else if (!_.isEqual(entity_id, parta_id) && _.isEqual(entity_id, partb_id)) {
		return -1;
	} else if (_.isEqual(entity_id, parta_id) && _.isEqual(entity_id, partb_id)) {
		return 0;
	} else {
		return NaN;
	}
}

/**
 * 交易者
 * @param {*} id 公司id
 * @returns 
 */
function trader(id) {
	return {
		id,

		/**
		 * 信息重新加载
		 * @param {*} action 
		 * @returns 
		 */
		reload(action) {
			return sqlquery2(`select * from t_company where id=${id} limit 1`).then(action);
		},
		/**
		 * 公司的信息 
		 * @returns 
		 */
		warehouses(action) {
			const sql = `select wh.* from (select * from t_company_warehouse where company_id=${id}) cy 
				left join t_warehouse wh on cy.warehouse_id=wh.id`;
			return sqlquery2(sql).then(action);
		}
	};
}

// 订单头寸
const LONG = 1; // 多头 
const SHORT = -1; // 空头

// 初始化数据 
const INIT_DATA = {
	component: "-", //  组件名
	current: { // 当前对象
		// 数据表
		tbl_index: -1, // 数据表行行索引
		tbls_selected: [],// 选择的表格
		// 表数据
		tbldata_index: -1, // 行数据索引
		tbldatas_selected: [], // 表数据是否被选择
		// 明细行
		line_index: -1, // 明细行行索引
		lines_selected: [], // 明细行选择索引集合
		// 仓库
		warehouse_index: -1, //仓库行索引
		warehouses_selected: [], // 表数据是否被选择
		// 产品 
		product_index: -1, // 公司产品行索引
		products_selected: [], // 表数据是否被选择
		// 用户
		user: { // 用户信息
			name: "gbench",
			password: "123456"
		},
		// 交易对手方
		counterpart: { // 对方明细
			id: -1, // 交易对手
			default_warehouse_id: -1, // 默认仓库
			warehouses: []
		},
		// 公司对象
		company: null, // 当前公司对象,仅当用户登录后才有效
		// 默认的仓库
		default_warehouse_id: -1 // 默认的公司仓库
	},  //  当前对象
	tables: [], // 数据表
	tbldata: [], // 表数据
	lines: [], // 行项目
	warehouses: [], // 仓库
	iid2pcts: {}, // 公司产品id->产品明细 
	wid2whs: {}, //  仓库id->仓库明细
	cid2cys: {}, // 公司id->公司明细
	counterpart_id: -1, // 交易对手方id
	counterparts: [], // 对手方集合
	order_position: SHORT, // 默认订单头寸,空头,即 创建一个卖出单,order的partb_id为当前的用户的company_id 
	btype: "all",// bill_type 单据类型
	pvtkeys: "ledger_id,name,warehouse,item,drcr", // pvt 透视表 
	accts: [], // 会计分录 
}; // INIT_DATA

/**
 * A组件 
 */
const AComp = {

	template: `<div class="highlight">{{name}}</div>`,

	/**
	 * 组件的数据 
	 * @returns 
	 */
	data() {
		return Object.assign({}, INIT_DATA);
	},

	/**
	 *  数据监听
	 */
	watch: {
		/**
		 * 透视表keys的监听事件 
		 * @param {*} _old 
		 * @param {*} _new 
		 */
		pvtkeys(_old, _new) {
			this.build_pivot_table(this.pvtkeys); // 更新数据透视表
		}
	},

	/**
	 * 数据加载 
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
	 * 计算属性 
	 */
	computed: {
		/**
		 * 公司id 
		 */
		company_id() {
			return this.current.company == null ? -1 : this.current.company.id;
		},

		/**
		 * 当前选择额数据行 
		 * @returns 
		 */
		selected_lines() {
			return this.current.lines_selected.map(i => this.lines[i]).filter(e => e);
		},

		/**
		 * 当前数据表 
		 * @returns 
		 */
		current_tbl() {
			return _.defaults(this.tables[this.current.tbl_index], { name: "-" }).name;
		},

		/**
		 * 当前的表数据行 
		 * @returns 
		 */
		current_tbldata() {
			if (this.current.tbldata_index < 0 || this.tbldata.length < 1) {
				return null;
			} else {
				return this.tbldata[this.current.tbldata_index];
			}
		},

		/**
		 * 当前行项目 
		 * @returns 
		 */
		current_line() {
			if (this.current_line_index < 0 || this.lines.length < 1) {
				return null;
			} else {
				return this.lines[this.current.line_index];
			}
		},

		/**
		 * 当前的仓库 
		 * @returns 
		 */
		current_warehouse() {
			return this.warehouses[this.current.warehouse_index];
		},

		/**
		 * 当前用户是否登录 
		 */
		is_logined() {
			return !!this.current.company;
		},

		/**
		 * 是否是处于多头位置 
		 * @returns 
		 */
		is_long_position() {
			const order = this.current_tbldata;
			if (!order || !order.partb_id || this.company_id != order.parta_id) { // 只有当前公司id是乙方公司才能进行发货 
				return false;
			} else { // this.company_id==order.parta_id
				return true;
			}
		},

		/**
		 * 是否是处于空头位置 
		 * @returns 
		 */
		is_short_position() {
			const order = this.current_tbldata;
			if (!order || !order.partb_id || this.company_id != order.partb_id) { // 只有当前公司id是乙方公司才能进行发货 
				return false;
			} else { // this.company_id==order.partb_id
				return true;
			}
		},

		/**
		 * 可以开发票的项目 
		 */
		invoice_avail_lines() {
			return this.avail_bill_lines("invoice");
		},

		/**
		 * 可以开入库单的项目 
		 */
		receipt_avail_lines() {
			return this.avail_bill_lines("receipt")
				.filter(e => (ids => // 检查关联单据的id字段是否有效
					_.every(ids, id => id && !_.isEqual(-1, id)))
					([e['bill_id'], e['freight_order_id']]) // 发票id和货运单id都有效
				);
		},

		/**
		 * 可以等级或元旦的行项目 
		 * @returns 
		 */
		freight_avail_lines() {
			return this.selected_lines.filter(e =>
				_.isEqual("invoice", e["bill_type"])
				&& (id => !id || _.isEqual(-1, id))(e["freight_order_id"]));
		},

		/**
		 * 可以等级或元旦的行项目 
		 * @returns 
		 */
		pmt_avail_lines() {
			return this.selected_lines.filter(e =>
				_.isEqual("receipt", e["bill_type"]) && ((o => !o || o == -1)(e["payment_id"])));
		},

		/**
		 * 表单类型 
		 * @returns 
		 */
		btypes() {
			const bb = _.uniq(this.lines.map(e => e["bill_type"]));
			return _.concat(["all"], bb); // 补充默认的all类型
		},

		/**
		 * 数据行 
		 * @returns 
		 */
		datalines() {
			return this.lines.filter(e => {
				if (_.isEqual("all", this.btype)) return true;
				const btype = e["bill_type"];
				return _.isEqual(btype, this.btype);
			});
		},

		/**
		 * Getters 数据
		 */
		...mapGetters("ACompStore", ["name"]),
		...mapState("ACompStore", { state: state => state }),
	},

	/**
	 * 方法属性
	 */
	methods: {

		/**
		 * 行是否被选中 
		* @param {*} i 
		*/
		is_tbldata_selected(i) {
			return _.includes(this.current.tbldatas_selected, i);
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
			return _.includes(this.current.warehouses_selected, i);
		},

		/**
		 * 发票订单按钮是否开启 
		 * @returns 
		 */
		is_invoice_btn_enabled() {
			if (!this.is_short_position || this.invoice_avail_lines < 1) { // 订单视图才能进行发货
				return false;
			} else {
				return true;
			}
		},

		/**
		 * 发票订单按钮是否开启 
		 * @returns 
		 */
		is_receipt_btn_enabled() {
			if (!this.is_long_position || this.receipt_avail_lines < 1) { // 订单视图才能进行收货
				return false;
			} else {
				return true;
			}
		},

		/**
		 * 是否开启货运按钮 
		 * @returns 
		 */
		is_freight_btn_enabled() {
			return this.is_short_position // 发货方即发票的签出方,亦即产品的空头位置尾提供货运单
				&& this.freight_avail_lines.length > 0;
		},

		/**
		 * 是否开启货运按钮 
		 * @returns 
		 */
		is_pmt_btn_enabled() {
			return this.pmt_avail_lines.length > 0;
		},

		/**
		 * 行项目 
		 */
		reset_lines() {
			this.lines = [];
		},

		/**
		 * 表数据
		 */
		reset_tbldata() {
			this.tbldata = [];
		}
		,
		/**
		 * 重置行项目 
		 */
		reset_selected_lines() {
			this.current.lines_selected = [];
			this.current.line_index = -1;
		},

		/**
		 * 重置行项目 
		 */
		reset_selected_tbldata() {
			this.current.tbldatas_selected = [];
			this.current.tbldata_index = -1;
		},

		/**
		 * 重置行项目 
		 */
		reset_selected_warehouses() {
			this.current.warehouses_selected = [];
			this.current.warehouse_index = -1;
		},

		/**
		 * 重置数据
		 */
		on_reset_click(event) {
			http_post("/h5/finance/data/reset", {
				datafile: "F:/slicef/ws/gitws/malonylcoa/src/test/java/gbench/webapps/mymall/api/model/data/acct_data.xlsx"
			}).then(e => {
				console.log(JSON.stringify(e.data));
				this.refresh_tbldata(this.current_tbl);
			});
		},

		/**
		 * 刷新表数据
		 * @param {*} tbl 表名 
		 */
		refresh_tbldata(tbl) {
			const i = _.findIndex(this.tables, e => _.isEqual(e.name, tbl));
			if (i >= 0) { // 表名索引有效
				const line = this.tables[i];
				this.on_tables_trclick({ line, i }); // 注意这里传入了一个null的event对象
				this.refresh_trial_balance(this.company_id); // 试算平衡表
			} // if
		},

		/**
		 * 刷新详情区域中的数据
		 */
		refresh_lines() {
			const i = this.current.tbldata_index;
			const line = this.tbldata[i];
			this.on_tbldata_trclick({ line, i }); // 注意这里传入了一个null的event对象
			this.refresh_trial_balance(this.company_id); // 刷新试算平衡表
		},

		/**
		 * 可以开发票的项目 
		 */
		avail_bill_lines(bill_type) {
			const issued_ids = this.lines.filter(e => e["bill_type"] == bill_type).map(e => e.id); // 已经开票的行项目id 
			const unissued_ids = _.uniq(this.lines.filter(e => !_.includes(issued_ids, e.id)).map(e => e.id)); // 尚未开票的行项目id
			const unissued_lines = _.keyBy(this.lines.filter(e => _.includes(unissued_ids, e.id)), "id"); // 尚未开票的行项目 
			const selected_ids = this.selected_lines.map(e => e.id); // 当前选择项目的id
			const lines = _.values(unissued_lines).filter(e => _.includes(selected_ids, e.id)); // 当前选择的尚未开票的行项目
			return lines;
		},

		/**
		 * 获取所持有的订单头寸
		 * @param {*} order 
		 */
		position(order) {
			return position(order, this.company_id);
		},

		/**
		 * 登录 
		 * @param {*} event 
		 */
		on_login_click(event) {
			const name = this.current.user.name;
			const password = this.current.user.password;
			const us_sql = `select * from t_user where name='${name}' and password='${password}'`;
			sqlquery2(us_sql).then(usdata => {
				// 验证用户的登录数据是否存在
				if (usdata.length < 1) { // 登录数据不存在,即登录失败
					sqlquery2(`select count(*) cnt from t_user where name='${name}'`).then(rs => { // 检查账号是否存在
						if (rs.length > 0 && rs[0]["cnt"] > 0) { // 结果集(resultset)rs中的cnt字段大于零表明账号存在但是密码错误
							alert(`【${name}】用户密码【${password}】错误`);
						} else { // 账号错误
							alert(`【${name}】用户账号错误`);
						} // if
					}); // 验证账户是否有效
					return; // 登录失则退出
				} // 登录失败

				const user = this.current.user = usdata[0]; // 记录当前登录的用户信息
				const uc_sql = `select c.* from (
						select * from t_user_company where id='${user.id}'
					) uc left join t_company c on uc.company_id = c.id `;
				sqlquery2(uc_sql).then(ucdata => {
					if (ucdata.length > 0) { // 用户与公司进行关联,默认登录用户的一个公司
						this.current.company = ucdata[0]; // this.current.company用于指示用户是否登录成功
						const cw_sql = `select * from t_company_warehouse where company_id=${this.company_id}`;
						// 加载公司仓库信息	
						sqlquery2(cw_sql).then(cwdata => {
							if (cwdata.length > 0) {
								const warehouse_ids = cwdata.map(e => e["warehouse_id"]);
								const wh_sql = `select * from t_warehouse where id in (${warehouse_ids.join(",")})`;
								return sqlquery2(wh_sql);
							} else {
								alert(`${name}所在公司${this.current.company.name}没有配有仓库`);
							} // if
						}).then(whdata => { // 公司仓库
							if (whdata && whdata.length > 0) { // 仓库非空 
								this.warehouses = whdata; // 加载公司仓库
								this.current.default_warehouse_id = whdata[0].id;
							} // if
						}); // 公司仓库
						// 加载公司信息
						sqlquery2(`select * from t_company where id!=${this.company_id}`).then(cydata => {
							this.counterparts = cydata;
							this.on_counterpart_change(null, this.counterparts[0].id); // 选择并刷新刷新对手方信息
							this.cid2cys = assoc_by("id", _.concat([this.current.company], cydata)); // 公司字典
						}); // 公司信息
						//公司产品信息
						sqlquery2(`select cp.id, p.id product_id,p.name name, cp.attrs
								from t_company_product cp left join t_product p on cp.product_id = p.id`
						).then(cpdata => {
							this.iid2pcts = assoc_by("id", cpdata); // 公司产品id
						}); // cpdata
						//仓库数据
						sqlquery2("select * from t_warehouse").then(whdata => {
							this.wid2whs = assoc_by("id", whdata);
						});
						this.refresh_tbldata("t_order");
					}// if ucdata
				}); // uc_sql
			}); // us_sql
		},

		/**
		 * 退出当前系统 
		 * @param {*} event 
		 */
		on_logout_click(event) {
			// 数据退出重置
			const reserved_words = "component,tables".split(","); // 保留数据内容
			this.current.company = null; // 公司清空
			this.current.tbl_index = -1; // 清空选择表
			clear(this.current.lines_selected); // 清空当前的行号选择标记缓存
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
			this.reset_selected_lines();
			this.reset_selected_tbldata();
			clear(this.lines); // 清空数据行
			let conditions = ""; // 过滤条件
			let projections = "*"; // 选择列表
			let orderby = ""; // 排序字段
			let data_handler = (data) => this.tbldata = data;

			if (this.company_id > 0) { // 当前公司id有效
				switch (this.current_tbl) { // 根据表名添加过滤条件
					case "t_order": { // 产品订单
						projections = `
						CASE ${this.company_id} WHEN parta_id THEN 'LONG' WHEN partb_id THEN 'SHORT' ELSE '-' END AS position
						,CASE ${this.company_id} WHEN parta_id THEN partb_id WHEN partb_id THEN parta_id ELSE '-' END as counterpart
						,tbl.*`;
						conditions = `WHERE parta_id=${this.company_id} OR partb_id=${this.company_id}`;
						orderby = "order by position desc,id desc";
						data_handler = data => {
							const _data = data.map(e => { // 翻译对手方id
								const cid = e["counterpart"];
								e["counterpart"] = get(this.cid2cys[cid], "name", cid);
								return e;
							}); // _data
							this.tbldata = _data;
							if (_data.length > 0) { // 展开第一行
								const line = _data[0];
								const i = this.current_tbldata_index = 0;
								this.reset_selected_tbldata();
								this.on_tbldata_trclick({ line, i, event: new Object() }); // 模拟点击事件
							} // if
						}; // data_handler
						break;
					} // t_order
					case "t_payment": { // 付款凭证
						conditions = `WHERE payer_id=${this.company_id} OR payee_id=${this.company_id}`;
						break;
					} // t_payment
					case "t_company_product": { // 公司产品
						conditions = `WHERE company_id=${this.company_id}`;
						break;
					} // t_company_product
					default: {
						// do nothing
					} // default
				} // switch
			} // if

			// 读取指定表的数据
			const sql = `SELECT ${projections} FROM ${this.current_tbl} tbl ${conditions} ${orderby}`;
			sqlquery2(sql).then(data_handler); // 数据处理
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

			const pmt_sql = `select * from t_payment where order_id=${order_id}`; // 支付集合
			sqlquery2(pmt_sql).then(_lines => { // 一级 
				const iid2pmts = assoc_by("item_id", _.flatMap(_lines,
					pmt => pathget(pmt, "details/items").map( // 支付行项目
						item => Object.assign({}, alias(item)("id,item_id"), // 改名
							gets(pmt, "id,order_id,payer_id,payee_id") // 提取指定字段
						)))); // assocs 
				const __lines = pcts.flatMap(p => { // 为此产品数据添加支付信息
					const pmts = iid2pmts[p["id"]];
					return _.flatMap(aslist(pmts), pmt => {
						return Object.assign({}, p, alias(pmt, -1)({ id: "payment_id" }));
					});
				});
				return PS(__lines); // Promise对象
			}).then(__lines => { // 二级数据行
				const bill_sql = `select * from t_billof_product where order_id = ${order_id}`;
				return sqlquery2(bill_sql).then(bills => {
					const iid2bills = assoc_by("item_id", _.flatMap(bills,
						bill => pathget(bill, "details/items").map( // 支付行项目
							item => Object.assign({}, alias(item)("id,item_id"), // 改名
								gets(bill, "id,bill_type,order_id,warehouse_id,freight_order_id") // 提取指定字段
							)))); // assocs
					const ___lines = __lines.flatMap(p => { // 为此产品数据添加支付信息
						const bills = iid2bills[p["id"]]; // 提取产品相关单据
						return _.flatMap(aslist(bills), bill => {
							return Object.assign({}, p, alias(bill, -1)("id,bill_id"));
						});
					});
					return PS(___lines);
				}); // sqlquery2
			}).then(___lines => { // 三级数据行
				this.lines = _.sortBy(___lines.map(e => { // 翻译产品名称
					const id = e.id; // 公司产品id
					const cp = this.iid2pcts[id]; // 公司产品
					const name = get(cp, "name", "-"); // 产品名称
					const quantity = get(e, "quantity", 0); // 单据类型 
					const price = get(e, "price", 0); // 单据类型 
					const wh = get(this.wid2whs[get(e, "warehouse_id", 0)], "name", "-"); // 单据id 
					return Object.assign({ id, name, quantity, price, wh }, e); // 加入产品名称字段
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
				this.reset_selected_lines();
				this.btype = "all"; // 修改表单类型
				clear(this.current.tbldatas_selected); // 清空前期选择,若是需要多选则注释掉这一行就可以了
				if (select(this.current.tbldatas_selected, i)) {
					this.current.tbldata_index = i;
				} else { //  清空当前选的行
					this.current.tbldata_index = -1;
					this.lines = [];
					return;
				};

				// 根据当前数据表的位置进行对应项目的处理
				switch (this.current_tbl) {
					case "t_order": { // 订单表，则刷新交易对手区域的信息
						const counterpart_id = line["position"] == "LONG"
							? line['partb_id']  //  多头持有订单的对手是卖方
							: line["parta_id"]; // 空头持有的订单的对手方买方
						this.on_counterpart_change(null, counterpart_id); // 更新对手方信息
						break;
					} // t_order
					default: { // 其余不予处置
						// do nothing
					} // default
				} // switch
			} // if

			switch (this.current_tbl) { // 表数据的处理
				case "t_order": return this.handle_order({ line, i });
				case "t_company_product": return this.handle_company_product({ line, i });
				case "t_warehouse": return this.handle_warehouse({ line, i });
				default: {
					alert(JSON.stringify(line));
				}
			} // switch

			console.log("on_tbl_data_trclick 的默认收尾", this.current_tbl);
		},

		/**
		 * 数据表的行点击 
		 * @param {*} param 
		 */
		on_warehouse_trclick({ line, i, event }) {
			if (select(this.current.warehouses_selected, i)) {
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
			if (td == null) { // 空对象
				return "-";
			} else { // 非空对象
				let t = td;
				if (_.isObject(td)) { // json 对象格式化
					t = JSON.stringify(td);
				} else {
					t = td;
				} // if

				if (t.length > 10) { // 超长省略
					return `<a title='${t}'>${t.substring(0, 10)}...<a>`;
				} else {
					return t;
				}
			}
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
		 * 刷新订单数据
		 * @param {*} data 
		 */
		refresh_orders(data) {
			const ids = (o => !o || !Array.isArray(o) ? [] : o)(data.ids);
			if (ids.length < 1) return; // 没有返回订单id则返回
			this.refresh_tbldata("t_order"); // 刷新数据
			setTimeout(() => { //  等待数据加载
				const id = ids[0].id;
				const i = _.findIndex(this.tbldata, e => _.isEqual(id, e.id));
				if (i >= 0) {
					const line = this.tbldata[i];
					this.on_tbldata_trclick({ line, i, event: 1 });
				} // if
			}, 1000); // seTimeout
		},

		/**
		 * 随机创建订单,counterpart_id的最大数量受限于t_company_product设计,目前受最大值为4
		 * @param {*} event 
		 */
		on_order_btn_click(event) {
			const rnd = n => parseInt((Math.random() * n) + 1); // 生成n范围内的整数
			const rnd2 = n => (Math.random() * n + 1).toFixed(2); // 生成n范围内的浮点数,两位小数
			const time = moment().format("YYYY-MM-DD HH:mm:ss"); // 当前系统时间
			const parta_id = this.order_position == LONG ? this.company_id : this.counterpart_id; // 甲方id
			const partb_id = this.order_position == SHORT ? this.company_id : this.counterpart_id; // 乙方id
			if (partb_id > 4) { //
				alert(`供应商partb_id:${partb_id}必须小于等于4,否则由于模拟数据限制:t_company_product表将没有对应产品数据`);
				return;
			}
			const volume = rnd(5); // 模拟订单产品规模
			const iid2pcts = assoc_by("id", // 依据产品id进行数据分组
				_.repeat("1", volume).split(/\s*/).map((v, i) => { // 随机生成数据序列
					// 产品id生成规则: 乙方id+10以内的随机数，这个产品id是有限制，依据t_company_prodduct的结构来进行设置
					return { id: (partb_id - 1) * 10 + rnd(10), quantity: rnd(10) }; // 随机生成产品id和交易数量
				})); // 随机生成订单项目
			const items = Object.keys(iid2pcts).map(pid => { // 产品id
				const pcts = aslist(iid2pcts[pid]); // 提取指定id订单产品项目列表
				const quantity = _.sumBy(pcts, e => e["quantity"]); // 累计交易数量
				const item = Object.assign(pcts[0], { quantity, price: rnd2(100) }); // 累计行项目的数量并补充价格
				return item; // 返回产品行项目
			});
			const order_bill = { // 订单数据
				name: "t_order", // 表名
				lines: [{ parta_id, partb_id, details: { items }, creator_id: 1, time }] // 行项目 
			}; // order_bill 订单数据

			// 持久化订单数据
			persist(order_bill).then(data => { //  持久化事后回调
				this.reset_selected_lines(); // 重置选择行项目
				this.refresh_orders(data); // 刷新订单
			}); // 订单数据写入并刷新订单列表
		}, // on_order_btn_click

		/**
		 * 发票又称发货单 
		 * @param {*} event 
		 */
		on_invoice_btn_click(event) {
			const bill_type = "invoice"; // 单据类型
			const issuer_id = this.company_id; // 单据发出公司id:空方
			const warehouse_id = this.current.default_warehouse_id; // 默认仓库id
			const order_id = this.current_tbldata.id; // 当前表数据行
			const lines = this.invoice_avail_lines; // 发票的产品项目
			const fid2items = assoc_by("freight_order_id", lines, -1); // 根据货运单编号进行分组,没有货运单默认为-1
			const insert_invoices = (freight_order_id, items) => { // 录入收货单
				const creator_id = -1; //  创建人
				const time = moment().format("YYYY-MM-DD HH:mm:ss"); // 当前系统时间
				const invoice_bill = { // 发票数据
					name: "t_billof_product",
					lines: [{ bill_type, issuer_id, warehouse_id, order_id, freight_order_id, details: { items }, creator_id, time }]
				}; // 发票项目
				// 发票数据持久化
				persist(invoice_bill).then(e => { // 刷新订单行项目
					this.reset_selected_lines(); // 重置选择行项目
					this.refresh_lines(); // 刷新行项目
				});
			}; // insert_invoices
			const freight_order_ids = Object.keys(fid2items); // 根据货运单号按照批次进行收货

			// 批次写入发货单
			freight_order_ids.forEach(freight_order_id => { // 依据货源单号(如果有的话,没有直接写入-1)进行分组写入
				const items = aslist(fid2items[freight_order_id]).map(e => gets(e, "id,quantity,price"));
				insert_invoices(freight_order_id, items);
			}); // forEach		
		}, // on_invoice_btn_click 

		/**
		 * 入库单 
		 * @param {*} event 
		 */
		on_receipt_btn_click(event) {
			const bill_type = "receipt"; // 单据类型
			const issuer_id = this.company_id; // 单据发出公司id:空方id
			const warehouse_id = this.current.default_warehouse_id; // 默认仓库id
			const order_id = this.current_tbldata.id; // 当前表数据行
			const lines = this.receipt_avail_lines; // 收据的产品项目
			const fid2items = assoc_by("freight_order_id", lines, -1); // 根据货运单编号进行分组,没有货运单默认为-1
			const insert_receipts = (freight_order_id, items) => { // 录入收货单
				const creator_id = -1; //  创建人
				const time = moment().format("YYYY-MM-DD HH:mm:ss"); // 当前系统时间
				const invoice_bill = { // 发票数据
					name: "t_billof_product",
					lines: [{ bill_type, issuer_id, warehouse_id, order_id, freight_order_id, details: { items }, creator_id, time }]
				}; // 发票项目
				// 数据持久化
				persist(invoice_bill).then(e => { // 刷新订单行项目
					this.reset_selected_lines(); // 重置选择行项目
					this.refresh_lines(); // 刷新行项目
				});
			}; // insert_receipts
			const freight_order_ids = Object.keys(fid2items); // 根据货运单号按照批次进行收货

			// 批次写入收货单
			freight_order_ids.forEach(freight_order_id => {
				const items = aslist(fid2items[freight_order_id]).map(e => gets(e, "id,quantity,price"));
				insert_receipts(freight_order_id, items);
			}); // forEach
		}, // on_receipt_btn_click

		/**
		 * 货运单&又称收据 
		 * @param {*} event 
		 */
		on_freight_btn_click(event) {
			const order = this.current_tbldata; // 订单数据
			const order_id = order.id; // 订单号
			const consigner_id = order.partb_id; // 发货方
			const consignee_id = order.parta_id; // 收货方
			const shipping_from = this.current.default_warehouse_id; // 发货仓库
			const shipping_to = this.current.counterpart.default_warehouse_id; // 收货仓库
			const bid2pcts = assoc_by("bill_id", this.freight_avail_lines); // 单据发票号->产品
			const bill_ids = Object.keys(bid2pcts); // 提取发货单编号
			const completed_ids = []; // 已经完成付款的付款单号
			const insert_freights = (bill_id, items) => { // 根据指定的单据发表号创建对应的发货单
				const details = { items }; // 发货产品详情
				const creator_id = 1; // 创建人
				const time = moment().format("YYYY-MM-DD HH:mm:ss"); // 当前系统时间
				const freight_bill = { // 货运单
					name: "t_freight_order",
					lines: [{ order_id, consigner_id, consignee_id, shipping_from, shipping_to, details, creator_id, time }]
				}; // 货运单
				// 货运单持久化
				persist(freight_bill).then(data => { // 刷新订单行项目
					const freight_order_id = data.ids[0].id; // 写入的运单号
					// 将货运单id写入单据（发票或收货单)
					const sql = `update t_billof_product set freight_order_id = ${freight_order_id} 
						where id =${bill_id}`;
					sqlexecute(sql).then(data => { this.refresh_lines(); });
					completed_ids.push(freight_order_id);
					if (completed_ids.length == bill_ids.length) {
						this.reset_selected_lines(); // 清除当前选择行
						this.refresh_lines();
						console.log("完成所有发货数据写入,各个发货单号为", completed_ids);
					} else {
						console.log("完成发货数据写入[", freight_order_id, "]", items);
					} // if
				}); // persist
			}; // insert_freights

			// 依据单据号进行分批写入
			bill_ids.map(bill_id => {// 根据receipt_id 进行分组付款
				const pcts = bid2pcts[bill_id]; // 提取单据下的产品
				const items = _.values(_.keyBy(aslist(pcts), e => e.id)).map(e => gets(e, "id,quantity,price"));
				insert_freights(bill_id, items); // 根据发单填写发货单
			}); // bill_ids 
		}, //  on_freight_btn_click

		/**
		 * 付款单 
		 * @param {*} event 
		 */
		on_payment_btn_click(event) {
			const order = this.current_tbldata; // 订单对象
			const order_id = order.id; // 订单id
			const payer_id = order.parta_id; // 甲方,付款方
			const payee_id = order.partb_id; // 乙方,收款方
			const rid2pcts = assoc_by("bill_id", this.pmt_avail_lines); // 单据号->产品
			const receipt_ids = Object.keys(rid2pcts); // 提取单据号
			const completed_ids = []; // 已经完成付款的付款单号
			const insert_pmts = (receipt_id, items) => { // items
				const details = { items }; // 付款单的产品明细
				const creator_id = 1; // 创建者
				const time = moment().format("YYYY-MM-DD HH:mm:ss"); // 当前系统时间
				const amount = _.sumBy(items, e => e["quantity"] * e["price"]); // 付款金额
				const payment_bill = { // 付款单
					name: "t_payment",
					lines: [{ order_id, payer_id, payee_id, amount, receipt_id, details, creator_id, time }]
				}; // 付款单
				// 货运单持久化
				persist(payment_bill).then(e => { // 刷新订单行项目
					const id = e.ids[0].id; // 付款单编号
					completed_ids.push(id);
					if (completed_ids.length == receipt_ids.length) { // 所有收货数据都已付款完成
						this.refresh_lines();
						console.log("完成所有付款数据写入,各个付款单号为", completed_ids);
					} else {
						console.log("完成付款数据写入[", id, "]", items);
					} // if
					this.reset_selected_lines(); // 清除当前选择行
				});
			}; // 支付数据写入数据库

			// 依据单据号进行分批写入
			receipt_ids.map(receipt_id => {// 根据receipt_id 进行分组付款
				const pcts = rid2pcts[receipt_id]; // 提取单据(收款单)下的产品
				const items = _.values(_.keyBy(aslist(pcts), e => e.id)).map(e => gets(e, "id,quantity,price"));
				insert_pmts(receipt_id, items); // 根据收款单填写付款单
			}); // map
		}, // on_payment_btn_click

		/**
		 * 表单类型的变更事件处理
		 * @param {*} event 
		 */
		on_btype_change(event) {
			this.reset_selected_lines();
		},

		/**
		 * 对手方处理
		 * @param {*} event 事件对象,当为null的时候表名这是自定义刷新用户信息的请求
		 */
		on_counterpart_change(event, counterpart_id) {
			if (counterpart_id) { // 优先接收counterpart_id参数中给的值 
				this.counterpart_id = counterpart_id;
			} else if (event && event.target && event.target.value) { // 事件对象上的绑定作为默认值给予确认 
				this.counterpart_id = event.target.value;
			} // if
			const ct = trader(this.counterpart_id);
			// 交易者的事件处理
			ct.warehouses(whdata => {
				if (whdata.length > 0) { // 对手方的仓库有效
					this.current.counterpart.default_warehouse_id = whdata[0].id;
				} //if
				this.current.counterpart.warehouses = whdata;
			}); // warehouses

			// 基础信息重新加载
			ct.reload(lines => {
				if (lines.length > 0) {
					this.current.counterpart = Object.assign(this.current.counterpart, lines[0]);
				}
			});
		},

		/**
		 * 刷新试算平衡表 
		 */
		refresh_trial_balance(company_id) {
			http_post("/h5/finance/acct/entries", { company_ids: company_id })
				.then(e => {
					const data = e.data.data;
					const translate = p => { // 节点信息翻译
						const pct = this.iid2pcts[p.item_id]; // 提取产品信息
						return { name: `${p.name}` };
					}; // 翻译函数
					const lines = data.map(e => Object.assign({}, gets(e, "drcr,amount"), translate(e)));
					const glines = _.groupBy(lines, line => line.name); // 分组后的行
					const entries = Object.keys(glines).map(k => { // 计算每一组的总额
						const value = _.sumBy(glines[k], v => v.drcr * v.amount).toFixed(2);
						return { "key": k, "value": value };
					}); // 科目分录行
					this.accts = entries;
					this.build_pivot_table(this.pvtkeys); // 数据透视表
				});
		},

		/**
		 * 树形节点击事件 
		 * @param {*} event 
		 * @param {*} treeId 
		 * @param {*} treeNode 
		 * @param {*} clickFlag 
		 */
		on_treenode_click(event, treeId, treeNode, clickFlag) {
			// alert(JSON.stringify(treeNode));
		},

		/**
		 * 根据键值路径创建数据透视表
		 * @param {*} keys 键值路径 
		 */
		build_pivot_table(keys) {
			http_post("/h5/finance/acct/trial_balance", {
				company_ids: this.company_id,
				keys
			}).then(e => {
				const rootdata = e.data.data; // 根节点数据
				const tree = document.querySelector("#ztree"); // 树形控件
				this.$nextTick(p => { // z_tree 初始化
					const $container = $(tree);
					const setting = { // 树形节点的设置
						view: { showLine: true, selectedMulti: true },
						callback: { onClick: this.on_treenode_click } // 树形节点操作
					}; // setting
					const $ztree = $.fn.zTree.init($container, setting, rootdata);
					$ztree.getNodesByParam("level", 1).forEach(node => { // 展开二级节点
						$ztree.expandNode(node, true, false, false);
					});
				}); // nextTick
			});
		}

	} // methods

};

export { AComp };