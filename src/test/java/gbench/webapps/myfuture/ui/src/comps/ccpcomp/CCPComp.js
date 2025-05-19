import { mapGetters, mapState } from "vuex";
import { http_post, http_get, sqlquery, sqlquery2, sqlexecute } from "../../gbench/util/sqlquery";
import { is_valid_url, image_url, alias, pathget, gets, get, assoc_by, aslist, select, clear } from "../../gbench/util/common";
import axios from 'axios';
import moment from 'moment';
import _ from "lodash";

/**
 * 随机选择letters数组中的n个元素，并将这些元素拼接成一个字符串
 */
const randgen = function(letters, n, flag = true) {
	// 处理空数组或n小于等于0的情况
	if (!letters || letters.length === 0 || n <= 0) {
		return '';
	}

	// 如果n大于数组长度且不允许重复，则调整n为数组长度
	if (!flag && n > letters.length) {
		n = letters.length;
	}

	const result = [];
	const usedIndices = new Set(); // 用于记录已使用的索引（当不允许重复时）

	for (let i = 0; i < n; i++) {
		let randomIndex;
		if (flag) {
			// 允许重复：直接随机选择
			randomIndex = Math.floor(Math.random() * letters.length);
		} else {
			// 不允许重复：随机选择未使用的索引
			do {
				randomIndex = Math.floor(Math.random() * letters.length);
			} while (usedIndices.has(randomIndex));
			usedIndices.add(randomIndex);
		}
		result.push(letters[randomIndex]);
	}

	return result.join('');
};

const surnames = [
	"赵", "钱", "孙", "李", "周", "吴", "郑", "王", "冯", "陈",
	"褚", "卫", "蒋", "沈", "韩", "杨", "朱", "秦", "尤", "许",
	"何", "吕", "施", "张", "孔", "曹", "严", "华", "金", "魏",
	"陶", "姜", "戚", "谢", "邹", "喻", "柏", "水", "窦", "章",
	"云", "苏", "潘", "葛", "奚", "范", "彭", "郎", "鲁", "韦",
	"昌", "马", "苗", "凤", "花", "方", "俞", "任", "袁", "柳",
	"酆", "鲍", "史", "唐"
];

const digits = [1, 2, 3, 4, 5, 6, 7, 8, 9, 0];

const CCPComp = {

	template:
		`<div>
			<div class="highlight">{{name}}</div>
			{{component}}
			<hr>
			<div>
			   姓名: <input v-model="traderfrm.name" style='width:100px;' /> &nbsp;
			   密码: <input v-model="traderfrm.password" style='width:100px;' /> &nbsp;
			   身份证: <input v-model="traderfrm.idcard" /> &nbsp;
			   银行卡: <input v-model="traderfrm.bankcard"/> &nbsp;
			   描述: <input v-model="traderfrm.description"/> &nbsp;
			   <button @click="regist_trader(traderfrm)"> 注册交易者</button>
			</div>
			<hr>
			<div style="height:100px;overflow:auto;border:solid 1px red;">
				<data-table :data="traders" />
			</div>
			<hr>
			<div>
			   证券类型: <select v-model=securityfrm.type> 
			   		<option v-for="type in securityfrm.types">{{type}}</option>
			   </select>&nbsp;
			   交易所: <select v-model=securityfrm.xchg> 
			   		<option v-for="type in securityfrm.xchgs">{{type}}</option>
			   </select>&nbsp;
			   CODE: <input v-model="securityfrm.code" style='width:100px;' /> &nbsp;
			   名称: <input v-model="securityfrm.name" style='width:100px;' /> &nbsp;
			   发行日期: <input v-model="securityfrm.open" type="date"  style='width:100px;'/> &nbsp;
			   <span v-if="securityfrm.type!='STOCK'"> 
			   	结束日期: <input v-model="securityfrm.close" type="date"  style='width:100px;' /> &nbsp; 
			   </span>
			   描述: <input v-model="securityfrm.description" style='width:80px;' /> &nbsp;
			   <button @click="regist_security(securityfrm)"> 登记证券</button>
			</div>
			<hr>
			<div style="height:100px;overflow:auto;border:solid 1px red;">
				<data-table :data="securities" style="width:100%" />
			</div>
			<hr>
			头寸: <select v-model="orderfrm.position"> 
				<option v-for="position in orderfrm.positions" :value="position">
					{{position}}
				</option>
			</select> &nbsp;
			交易者: <select v-model="orderfrm.traderid" @change="refresh_orders(orderfrm.traderid, orderfrm.securityid)"> 
			  <option v-for="trd in traders" :value="trd.ID">{{trd.NAME}}</option>
		    </select> &nbsp;
			证券: <select v-model="orderfrm.securityid"  @change="refresh_orders(orderfrm.traderid, orderfrm.securityid)"> 
			   <option v-for="sec in securities" :value="sec.ID">{{sec.NAME}}</option>
		    </select> &nbsp;
			价格: <input v-model="orderfrm.price" style='width:100px;' /> &nbsp;
			数量: <input v-model="orderfrm.quantity" style='width:100px;' /> &nbsp;
			描述: <input v-model="orderfrm.description" style='width:80px;' /> &nbsp;
			<button @click="create_order(orderfrm)"> 挂单</button> &nbsp;
			<button @click="remove_order()"> 删除</button>
			<hr>
			<div style="height:180px;overflow:auto;border:solid 1px red;">
				<data-table :data="orders" 
					@trclick="on_orderdata_trclick"
					:trclass="(line,i)=>is_orderdata_selected(i)?current.orderdata_index==i?'highlight2':'highlight':'tdclass'"
					style="width:100%" />
			</div>
		</div>`,

	/**
	 * 
	 * @returns 
	 */
	data() {
		return {
			component: "-", articles: [],
			traders: [], securities: [],
			orders: [],
			traderfrm: {
				name: randgen(surnames, 3),
				password: 123456,
				idcard: randgen(digits, 18),
				bankcard: randgen(digits, 19),
				description: "普通交易者"
			},
			securityfrm: {
				types: "FUTURE,STOCK,BOND,FUND".split(/,/),
				xchgs: "SHEX,SHFE,DCE,CZCE,CFFEX,INE,GFEX".split(/,/),
				type: "FUTURE",
				xchg: "SHFE",
				code: "CODE" + randgen(digits, 3),
				name: "证券" + randgen(digits, 3),
				open: moment().format('YYYY-MM-DD'),
				close: moment().add(1, 'years').format('YYYY-MM-DD'),
				description: "金融证券"
			},
			orderfrm: {
				position: "LONG",
				positions: "LONG,SHORT".split(/,/),
				traderid: "1",
				securityid: "1",
				price: 1000,
				quantity: 10,
				description: "普通交易单"
			},
			current: {
				orderdata_index: -1,
				orderdatas_selected: []
			}
		};
	},

	/**
	 * 
	 */
	mounted() {
		console.log(this.name);

		// 开始信息
		http_post("/h5/ccp/component", { name: "CPComp" }).then(res => {
			const data = res.data.data;
			this.state.name = data.name;
			this.component = data.name + " In " + data.service + " @ " + data.time;
		});

		// sql data 
		sqlquery("SELECT ID,TITLE,VOLUME,TIME FROM t_maozedong LIMIT 10").then(res => {
			this.articles = res.data.data;
		});

		// sql data 
		sqlquery("SELECT * FROM t_trader where ID!=0 LIMIT 10").then(res => {
			this.traders = res.data.data;
		});

		// sql data 
		sqlquery("SELECT * FROM t_security where ID!=0 LIMIT 10").then(res => {
			this.securities = res.data.data;
		});

		// sql data 
		this.refresh_orders(this.orderfrm.traderid, this.orderfrm.securityid);
	},

	/**
	 * 
	 */
	computed: {
		/**
		 * Getters 数据
		 */
		...mapGetters("CCPCompStore", ["name"]),
		...mapState("CCPCompStore", { state: state => state }),

		/**
		 * 当前的表数据行 
		 * @returns 
		 */
		current_orderdata() {
			if (this.current.orderdata_index < 0 || this.orderdata_index.length < 1) {
				return null;
			} else {
				return this.orderdata[this.current.orderdata_index];
			}
		},
	},

	methods: {

		/**
		 * 行是否被选中 
		* @param {*} i 
		*/
		is_orderdata_selected(i) {
			return _.includes(this.current.orderdatas_selected, i);
		},

		/**
		 * 表数据
		 */
		reset_orderdata() {
			this.orderdata = [];
		},

		/**
		 * 重置行项目 
		 */
		reset_selected_orderdata() {
			this.current.orderdatas_selected = [];
			this.current.orderdata_index = -1;
		},

		/**
		 * 数据表的行点击 
		 * @param {*} param 
		 */
		on_orderdata_trclick({ line, i, event }) {
			if (select(this.current.orderdatas_selected, i)) {
				this.current.orderdata_index = i;
			} else { //  清空当前选的行
				this.current.orderdata_index = -1;
				return;
			};
		},

		/**
		 * 
		 */
		regist_trader(traderfrm) {
			// 注册应用
			axios.post("/h5/api/ccp/createTraderAccount", traderfrm)
				.then(res => {
					const data = res.data.data;
					console.log(JSON.stringify(data));
					sqlquery("select * from t_trader order by ID desc limit 10").then(res => { // 刷新交易者
						this.traders = res.data.data;
					});
					this.traderfrm.name = randgen(surnames, 3);
					this.traderfrm.idcard = randgen(digits, 18);
					this.traderfrm.bankcard = randgen(digits, 19);
				});
		},

		/**
		 * 
		 */
		regist_security(securityfrm) {
			// 注册应用
			axios.post("/h5/api/ccp/createSecurity", {
				code: securityfrm.code,
				xchg: securityfrm.xchg,
				type: securityfrm.type,
				name: securityfrm.name,
				open: securityfrm.open,
				close: securityfrm.close
			}).then(res => {
				const data = res.data.data;
				console.log(JSON.stringify(data));
				sqlquery("select * from t_security order by ID desc limit 10").then(res => { // 刷新交易者
					this.securities = res.data.data;
				});
				this.securityfrm.code = "CODE" + randgen(digits, 3);
				this.securityfrm.name = "证券" + randgen(digits, 3);
			});
		},

		/**
		* 创建订单
		*/
		create_order(orderfrm) {
			// 注册应用
			axios.post("/h5/api/ccp/createOrder", {
				traderid: orderfrm.traderid,
				securityid: orderfrm.securityid,
				position: orderfrm.position,
				price: orderfrm.price,
				quantity: orderfrm.quantity,
				description: orderfrm.description
			}).then(res => {
				const data = res.data.data;
				console.log(JSON.stringify(data));
				this.refresh_orders(orderfrm.traderid, orderfrm.securityid);
			});
		},

		/**
		* 创建订单
		*/
		remove_order() {
			const ii = this.current.orderdatas_selected;
			if (ii.length > 0) {
				const ids = this.orders.filter((e, i) => _.includes(ii, i)).map(e => e.ID);
				const sql = `delete from t_order where ID in (${ids.join(",")})`;
				console.log(sql);
				// sql data 
				sqlexecute(sql).then(res => {
					this.reset_selected_orderdata();
					const data = res.data.data;
					this.refresh_orders(this.orderfrm.traderid, this.orderfrm.securityid);
				});
			} else {
				alert("请选择有效头寸单");
			}
		},

		/**
		 *  刷新挂单
		 */
		refresh_orders(traderid, securityid) {
			const positions = function(position) {
				return `select
					o.ID, -- 头寸单主键
					o.POSITION, -- 头寸
					-- t.NAME TNAME, -- 交易者
					s.NAME SNAME,  -- 证券名称
					ROUND(o.PRICE, 2) PRICE, -- 价格
					o.QUANTITY, -- 数量
					o.CREATE_TIME -- 下单时间
					-- o.DESCRIPTION -- 说明
				from (select * from t_order where POSITION=${position} and SECURITY_ID=${securityid}) o -- 检索指定头寸单
					left join t_trader t on o.TRADER_ID=t.ID -- 交易者
					left join t_security s on o.SECURITY_ID=s.ID -- 证券
				where t.ID=${traderid}`;
			}; // 头寸资产单

			const ask_sql = `${positions(-1)} order by o.PRICE desc, CREATE_TIME desc`; // 卖单 - 空头
			const bid_sql = `${positions(1)} order by o.PRICE desc, CREATE_TIME`; // 买单 - 多头

			sqlquery(`(${ask_sql}) union all (${bid_sql})`).then(res => {
				this.orders = res.data.data.map(e => {
					e.POSITION = e.POSITION == 1 ? "LONG" : "SHORT";
					return e;
				});
			});
		}
	}

};

export { CCPComp };