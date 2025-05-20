import { mapGetters, mapState } from "vuex";
import { http_post, http_get, sqlquery, sqlquery2, sqlexecute } from "../../gbench/util/sqlquery";
import axios from 'axios';

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

const BrokerComp = {

	template:
		`<div>
		<div class="highlight">{{name}}</div>
		{{component}}
		<hr>
		<div>
		   姓名: <input v-model="reqform.name"/> &nbsp;
		   身份证: <input v-model="reqform.idcard"/> &nbsp;
		   银行卡: <input v-model="reqform.bankcard"/> &nbsp;
		   描述: <input v-model="reqform.description"/> &nbsp;
		   <button @click="regist(reqform)"> 注册交易者</button>
		</div>
		<hr>
		<data-table :data="traders" />
	</div>`,

	/**
	 * 
	 * @returns 
	 */
	data() {
		return {
			component: "-",
			articles: [],
			traders: [],
			reqform: {
				name: randgen(surnames, 3),
				idcard: randgen(digits, 18),
				bankcard: randgen(digits, 19),
				description: "普通交易者"
			}
		};
	},

	/**
	 * 
	 */
	mounted() {
		console.log(this.name);

		// 开始信息
		http_post("/h5/broker/component", { name: "BrokerComp" }).then(res => {
			const data = res.data.data;
			this.state.name = data.name;
			this.component = data.name + " In " + data.service + " @ " + data.time;
		});

		// sql data 
		sqlquery("SELECT ID,TITLE,VOLUME,TIME FROM t_maozedong LIMIT 10").then(res => {
			this.articles = res.data.data;
		});

		// sql data 
		sqlquery("SELECT * FROM t_trader LIMIT 10").then(res => {
			this.traders = res.data.data;
		});
	},

	/**
	 * 
	 */
	computed: {
		/**
		 * Getters 数据
		 */
		...mapGetters("BrokerCompStore", ["name"]),
		...mapState("BrokerCompStore", { state: state => state }),
	},

	methods: {
		/**
		 * 
		 */
		regist(reqform) {
			// 注册应用
			axios.post("/h5/api/broker/createTraderAccount", reqform)
				.then(res => {
					const data = res.data.data;
					console.log(JSON.stringify(data));
					sqlquery("select * from t_trader order by ID desc limit 10").then(res => { // 刷新交易者
						this.traders = res.data.data;
					});
					this.reqform.name = randgen(surnames, 3);
					this.reqform.idcard = randgen(digits, 18);
					this.reqform.bankcard = randgen(digits, 19);
				});
		}
	}

};

export { BrokerComp };