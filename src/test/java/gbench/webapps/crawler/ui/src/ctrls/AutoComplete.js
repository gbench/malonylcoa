import $ from "jquery";
import { sqlquery } from '../gbench/util/sqlquery';

/**
 * 自动回填函数
 * @param {*} textbox 文本框id,需要带有井号#
 * @param {*} datasource 数据源名称:- 或者 为一条SQL 语句,返回值需要包括 code,name,value 三个字段。
 * @param {*} onresponse 响应事件的回调函数:(event,contents)
 * @param {*} onselect 选择后的回调函数:(code,label,value),即选定回调：买定离手
 * @param {*} onclose 关闭事件的回调函数
 * @param {*} onchange 值变动事件回调函数
 * @param {*} onerror 异常错误事件的回调函数
 * @param {*} where 条件过滤语句
 * @param {*} maxsize 提示项目的最大数量
 * @param {*} cacheable 查询结构的缓存时间，单位为妙
 */
function autocomplete(textbox, datasource, onresponse, onselect, onclose, onchange, onerror, where, maxsize, cacheable) {

	/**
	 * 判断fn 是否是一个函数 
	 * @param {*} fn 待检测的对象
	 */
	const isFunction = fn => {
		return Object.prototype.toString.call(fn) === '[object Function]'; // 使用Object原型的toString不能以保证原生的结构命名
	};// isFunction

	// 为为文本框添加一个自动回调
	$(textbox).autocomplete({

		source: function (request, response) {// 数据源
			const value = $(textbox).val().trim().replace("'", "\\'"); // 转义输入的关键词
			const keywords_tbl = isFunction(datasource) // 判断数据源是否是一个函数结构
				? datasource(request.term) // 函数结构的数据源
				: datasource; // 关键词的检索语句
			const where_line = !where || /^\s*$/.test(where) ? `label like '%${value}%'` : where.replace(/\$value/g, value); // 过滤语句
			const sql = `select * from ( ${keywords_tbl}) temptbl where ${where_line} limit ${!maxsize ? 15 : maxsize}`; // 生成SQL语句

			// 动态请求数据
			sqlquery(sql).then(ret => {// 相应结果
				const dd = ret.data.data; // 提取结果
				if (ret.error) throw ret.error; // 抛出异常错误

				const tailor_size = 45; // 剪裁数据尺寸
				const tailoring = line => line.length > tailor_size
					? `${line.substring(0, tailor_size)} ... ` // 裁剪过长的字符串
					: line; // 标准长度保持不变
				const suggestions = dd.filter(e => e && e.label)
					.map(e => { // 下拉选项的预处理
						return {
							code: e.code,
							label: `${e.code}\t#\t${tailoring(e.label)} --- ${e.tbl} `,
							value: e.value,
							$entry: e // 原始记录
						};
					});

				// 响应回调
				response(suggestions); // 数据响应
			}).catch(e => {
				onerror(e);
				console.log("autocomplete error", e);
				console.log("sql", sql);
			});// 数据成功返回
		},// source 函数

		response: onresponse, // 响应事件

		select: function (event, data) {// 选中下拉选项后所触发的事件:返回3字段数据信息:code,name,value
			const code = data.item.code;// code 编码
			const name = data.item.label;// label 标签名称
			const value = data.item.value;// value 数据值
			onselect(code, name, value);// 自动进行数据检索,选定回调
		},// select 买定离手的回调

		close: onclose, // 关闭事件的回调函数

		change: onchange, // 值事件变动事件

		autoFocus: true // 设置为 true，当菜单显示时，第一个条目将自动获得焦点。
	});// 自动完成 $(text).autocomplete

}// autocomplete

/**
 * 内容自动补充的控件
 * @param {自定义消息} select({code:选中的项目代码,label:选中的项目的显示名称,value:选中的项目的值}) 
 */
const AutoComplete = {// 自动完成的组件
	template: "<input :id='id' type='text' :value='value' :placeholder='placeholder' />",
	props: {
		id: String, // 组件id
		sql: String, // 组件的数据提取SQL,返回结果中一定需要包含3列 id,label,value
		value: String, // 输入框的值
		where: String, // 数据过滤器
		maxsize: String, // 提示项目尺寸
		placeholder: String, // 占位符
		cacheable: [String, Number, Boolean] // 查询结果的缓存时间，单位为妙
	},
	mounted() {
		//关键词全文检索
		autocomplete(`#${this.id}`,
			this.sql,//  输入项term的动态数据源:id,label,value
			(event, resp) => { // 响应事情
				this.$emit("response", event, resp);
			}, // onresponse
			(code, label, value) => {// 节点点击后的回调函数
				this.$emit("select", { code, label, value });// 发布点击事件
			}, // onselect
			(event) => { // 关闭事件的回调
				this.$emit("close", event);
			}, // close
			(event) => { // 值变动事件回调函数
				this.$emit("change", event);
			}, // change
			(error) => { // 错误异常事件
				this.$emit("error", error);
			}, // error
			this.where // 用$value 占位输入框中的值
			, this.maxsize // 提示项目的数量大小
			, this.cacheable // 查询结果的缓存时间，单位为妙
		); // autocomplete
	} // mounted
};

export { AutoComplete };