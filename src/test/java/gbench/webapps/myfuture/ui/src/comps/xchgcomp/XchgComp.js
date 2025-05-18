import { mapGetters, mapState } from "vuex";
import { http_post, http_get, sqlquery, sqlquery2, sqlexecute } from "../../gbench/util/sqlquery";
import { is_valid_url, image_url, alias, pathget, gets, get, assoc_by, aslist, select, clear } from "../../gbench/util/common";
import _ from "lodash";

const XchgComp = {

	template: ` <div>
		<div class="highlight">{{name}}</div>
		{{component}}
		<hr>
		证券: <select v-model="securityid"  
				@change="refresh_orders(securityid)"> 
				<option v-for="sec in securities" :value="sec.ID">{{sec.NAME}}</option>
			</select> &nbsp
			
		<hr>
		<div style="height:500px;overflow:auto;border:solid 1px red;">
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
			component: "-", articles: [],
			securities: [],
			securityid: 1,
			orders: [],
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
		http_post("/h5/xchg/component", { name: "XchgComp" }).then(res => {
			const data = res.data.data;
			this.state.name = data.name;
			this.component = data.name + " In " + data.service + " @ " + data.time;
		});

		// sql data 
		sqlquery("SELECT ID,TITLE,VOLUME,TIME FROM t_maozedong LIMIT 10").then(res => {
			this.articles = res.data.data;
		});

		// sql data 
		sqlquery("SELECT * FROM t_security where ID!=0 LIMIT 10").then(res => {
			this.securities = res.data.data;
		});

		// sql data 
		this.refresh_orders(this.securityid);
	},

	/**
	 * 
	 */
	computed: {
		/**
		 * Getters 数据
		 */
		...mapGetters("XchgCompStore", ["name"]),
		...mapState("XchgCompStore", { state: state => state }),

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
		 *  刷新挂单
		 */
		refresh_orders(securityid) {
			const positions = function(position) {
				return `select
							o.ID, -- 头寸单主键
							o.POSITION, -- 头寸
							t.NAME TNAME, -- 交易者
							s.NAME SNAME,  -- 证券名称
							ROUND(o.PRICE, 2) PRICE, -- 价格
							o.QUANTITY, -- 数量
							o.CREATE_TIME -- 下单时间
							-- o.DESCRIPTION -- 说明
						from (select * from t_order where POSITION=${position} and SECURITY_ID=${securityid}) o -- 检索指定头寸单
							left join t_trader t on o.TRADER_ID=t.ID -- 交易者
							left join t_security s on o.SECURITY_ID=s.ID -- 证券
						`;
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

export { XchgComp };