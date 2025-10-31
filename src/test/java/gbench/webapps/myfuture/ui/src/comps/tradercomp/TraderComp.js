import { mapGetters, mapState } from "vuex";
import { http_post, http_get, sqlquery, sqlquery2, sqlexecute } from "../../gbench/util/sqlquery";
import { is_valid_url, image_url, alias, pathget, gets, get, assoc_by, aslist, select, clear } from "../../gbench/util/common";
import axios from 'axios';
import moment from 'moment';

const TraderComp = {

	template: `<div>
		<div class="highlight">{{name}}</div>
		<hr>
		交易者: <select v-model="current.traderid" @change="refresh_orders(current.traderid, current.securityid)"> 
				<option v-for="trd in current.traders" :value="trd.ID">{{trd.NAME}}</option>
			</select> &nbsp;
		头寸: <select v-model="orderfrm.position"> 
				<option v-for="position in orderfrm.positions" :value="position">
					{{position}}
				</option>
			 </select> &nbsp;	
		证券: <select v-model="current.securityid"  @change="refresh_orders(current.traderid, current.securityid)"> 
				<option v-for="sec in current.securities" :value="sec.ID">{{sec.NAME}}</option>
			</select> &nbsp;
		价格: <input v-model="orderfrm.price" style='width:100px;' /> &nbsp;
		数量: <input v-model="orderfrm.quantity" style='width:100px;' /> &nbsp;
		描述: <input v-model="orderfrm.description" style='width:80px;' /> &nbsp;
		<button @click="create_order(orderfrm)"> 挂单 </button> &nbsp;
		<button @click="remove_order()"> 删除 </button> &nbsp;
		<button @click="refresh_orders(current.traderid, current.securityid)"> 刷新 </button>
		<hr>
		<div style="height:180px;overflow:auto;border:solid 1px red;">
			<data-table :data="orders" 
				@trclick="on_orderdata_trclick"
				:trclass="(line,i)=>is_orderdata_selected(i)?current.orderdata_index==i?'highlight2':'highlight':'tdclass'"
				style="width:100%" />
		</div>
	</div>
	`,

	/**
	 * 
	 * @returns 
	 */
	data() {
		return {
			component: "-", articles: [], orders: [],
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
				traderid: 1,
				securityid: 1,
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
		http_post("/h5/trader/component", { name: "TraderComp" }).then(res => {
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
			this.current.traders = res.data.data;
		});

		// sql data 
		sqlquery("SELECT * FROM t_security where ID!=0 LIMIT 10").then(res => {
			this.current.securities = res.data.data;
		});

		// sql data 
		this.refresh_orders(this.current.traderid, this.current.securityid);
	},

	/**
	 * 
	 */
	computed: {
		/**
		 * Getters 数据
		 */
		...mapGetters("TraderCompStore", ["name"]),
		...mapState("TraderCompStore", { state: state => state }),
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
				this.refresh_orders(this.current.traderid, this.current.securityid);
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
					this.refresh_orders(this.current.traderid, this.current.securityid);
				});
			} else {
				alert("请选择有效头寸单");
			}
		},


		/**
		 *  刷新挂单
		 */
		refresh_orders(traderid, securityid, flag = true) {
			if (flag) { // 同步变更orderfrm的字段
				this.orderfrm.traderid = traderid;
				this.orderfrm.securityid = securityid;
			}

			const positions = function(position) {
				return `select
					o.ID, -- 头寸单主键
					o.POSITION, -- 头寸
					-- t.NAME TNAME, -- 交易者
					s.NAME SNAME,  -- 证券名称
					ROUND(o.PRICE, 2) PRICE, -- 价格
					o.QUANTITY, -- 数量
					o.UNMATCHED, -- 数量
					o.REVISION, -- 版本
					o.UPDATE_TIME, -- 变更时间
					o.CREATE_TIME -- 下单时间
					-- o.DESCRIPTION -- 说明
				from (select * from t_order where POSITION=${position} and SECURITY_ID=${securityid}) o -- 检索指定头寸单
					left join t_trader t on o.TRADER_ID=t.ID -- 交易者
					left join t_security s on o.SECURITY_ID=s.ID -- 证券
				where t.ID=${traderid}`;
			}; // 头寸资产单

			const ask_sql = `${positions(-1)} order by o.PRICE desc, CREATE_TIME desc`; // 卖单 - 空头
			const bid_sql = `${positions(1)} order by o.PRICE desc, CREATE_TIME`; // 买单 - 多头

			sqlquery(`select * from  ((${ask_sql}) union all (${bid_sql})) t order by t.ID desc `).then(res => {
				this.orders = res.data.data.map(e => {
					e.POSITION = e.POSITION == 1 ? "LONG" : "SHORT";
					return e;
				});
				console.log(JSON.stringify(this.orders));
			});
		}
	}

};

export { TraderComp };