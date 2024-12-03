package gbench.util.jdbc.sql;

import static gbench.util.jdbc.kvp.IRecord.REC;
import static gbench.util.jdbc.kvp.IRecord.rb;
import static gbench.util.jdbc.kvp.Tuple2.P;
import static gbench.util.jdbc.sql.SQL.OpMode.BLANK;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import gbench.util.jdbc.Jdbcs;
import gbench.util.jdbc.kvp.DFrame;
import gbench.util.jdbc.kvp.IRecord;
import gbench.util.jdbc.kvp.Json;
import gbench.util.jdbc.kvp.KVPair;
import gbench.util.jdbc.kvp.SimpleRecord;
import gbench.util.jdbc.kvp.Tuple2;

/**
 * 命名SQL语句对象，每个SQL都有一个名字 含有模板参数(unix风格和mybatis风格) #variable ${variable}
 * 占位符即模板参数式样：#xxx 字符串类型, ##xxx 数值类型,或是 ${xxx} 一个SQL对象包含有： 名称:name，一个sql模板数据,
 * 模板:sqltpl， sql语句模板或者框架，含有必要的参数，参数可以有sqlctx来提供 或者个与计算生成。参数采用 #xxx 字符串类型, ##xxx
 * 数值类型,或是 ${xxx} 字符串结尾会添加换行符号类型 来进行占位,其中 语句上下文：sqlctx 一系列的环境参数，参数蚕蛹键值对的形式
 * 在sqlctx 以IRecord为单位进行存放。 sqlctx 是一个IRecord 集合用来表示多组数据，比如：插入语句 的多条数据记录。
 * 
 * @author gbench
 */
public class SQL {

	/**
	 * 这是一个查询语句
	 * 
	 * @param name   语句名称
	 * @param sqltpl 查询语句，参数采用 #xxx 字符串类型, ##xxx 数值类型,或是 ${xxx} 字符串结尾会添加换行符号类型
	 *               来进行占位,其中
	 */
	public SQL(final String name, final String sqltpl) {
		this.name = name;
		this.sqltpl = sqltpl;
	}

	/**
	 * 这是一个查询语句
	 * 
	 * @param name   语句名称
	 * @param sqltpl 查询语句，参数采用 #xxx 字符串类型, ##xxx 数值类型,或是 ${xxx} 字符串结尾会添加换行符号类型
	 *               来进行占位,其中
	 * @param recs   参数列表
	 */
	public SQL(final String name, final String sqltpl, final IRecord... recs) {
		this.name = name;
		this.sqltpl = sqltpl;
		this.sqlctx = Arrays.asList(recs);
	}

	/**
	 * 命名SQL语句对象，每个SQL都有一个名字
	 * 
	 * @param name SQL语句的名，可以是文本字符串，由于标识SQL，便于检索
	 * @param rec  SQL 语句的上下文数据：比如插入的操作的 插入记录
	 */
	public SQL(final String name, final IRecord rec) {
		this.name = name;
		this.sqlctx = Collections.singletonList(rec);// 保证sqlctx为一个记录集合
	}

	/**
	 * 命名SQL语句对象，每个SQL都有一个名字
	 * 
	 * @param name SQL语句的名，可以是文本字符串，由于标识SQL，便于检索
	 */
	public SQL(final String name) {
		this.name = name;
		this.sqlctx = Collections.singletonList((IRecord) null);// 行数据
	}

	/**
	 * 命名SQL语句对象，每个SQL都有一个名字
	 * 
	 * @param name   SQL语句的名，可以是文本字符串，由于标识SQL，便于检索
	 * @param sqlctx 行数据，SQL 语句的上下文数据：比如插入的操作的，插入记录
	 */
	public SQL(final String name, final List<IRecord> sqlctx) {
		this.name = name;
		this.sqlctx = sqlctx;
	}

	/**
	 * 语句的名字
	 * 
	 * @return 语句的名字
	 */
	public String name() {
		return this.name;
	}

	/**
	 * 查询语句：直接放回sqltpl
	 * 
	 * @return 文本串
	 */
	public String string() {
		return this.sqltpl;
	}

	/**
	 * 查询语句<br>
	 * 使用getSqlCtxAsOneRecord 格式化输出 <br>
	 * 对模式参数如:foreach 进行结构解析
	 * 
	 * @return 文本串
	 */
	public String format() {
		return this.string(this.getSqlCtxAsOneRecord());
	}

	/**
	 * 数据查询:这里关于 字符转义的描述很有问题，有时间的是需要要补充 <br>
	 * 带有变量值的语句内容
	 *
	 * 变量的替换规则如下: <br>
	 * 1、#便开头的变量 使用 单引号进行包被 <br>
	 * 2、##开头的变量不是使用单引号包被 <br>
	 * 3、${foreach id in ids id } 为单值形式foreach 结构。ids为单值列表 <br>
	 * 4、${foreach fld in kvset %fld.name=fld.value} 为复合值形式的列表,kvset为集合列表， <br>
	 * 列表中每个元素为 字段记录,每个字段属性有name,和value <br>
	 * 如果以%开头，会根据替换值的类型智能判断是否添加单引号：为数字不加单引号，否则加上单引号 <br>
	 * 由于 rec 中的值可能会被加上单引号，所以转义字符需要进行手动给予转换完成。以此保证语法合理性。
	 * 5、如果占位符以$结尾则则视该值为数字，即不加引号：此种标识为为强制转换为数字，<br>
	 * 而不管该字段是否是真的为为数字类型，即使是字符串也不加引号。会覆盖掉%的规则 <br>
	 * 例子： kvset 中的字段标识符的 正则表达式的格式为：[_0-9a-z\\$]+,即java 中标识符的规则。 <br>
	 * 其中以$结尾的字段一般用于标识这是一个数字类型的字段。<br>
	 * 1、List&lt;IRecord&gt; kvset = Arrays.asList( <br>
	 * REC2("name","id","value",437), <br>
	 * REC2("name","name","value","张三"), <br>
	 * REC2("name","sex","value","男"), <br>
	 * REC2("name","address","value","上海市徐汇区") ); <br>
	 * IRecord rec = REC2("kvset",kvset,"code",98) <br>
	 * update t_student set ${ foreach fld in kvset %fld.name=fld.value} where
	 * code=#code <br>
	 * 生成：update t_student set id=437, name='张三', sex='男', address='上海市徐汇区' where
	 * code='98' <br>
	 * <br>
	 * 注意： <br>
	 * 若是kvset中的 是这样的结构：注意kvset中的value$,有一个'$'后缀,于是fld.value$
	 * 就会被视作数字：而不管它是否真的是数字了，<br>
	 * 即$是强制为数字额形式来显示（去除单引号） <br>
	 * final var kvset = Arrays.asList( // 数据列表 <br>
	 * REC2("name", "id", "value$", 437), <br>
	 * REC2("name", "name", "value$", "张三"), <br>
	 * REC2("name", "sex", "value$", "男"), <br>
	 * REC2("name", "address", "value$", "上海市徐汇区") <br>
	 * );// kvset <br>
	 * update t_student set ${ foreach fld in kvset %fld.name=fld.value$ } where
	 * code=##code <br>
	 * 生成update t_student set id=437, name=张三, sex=男, address=上海市徐汇区 where code=98
	 * <br>
	 * 请注意：##code是用于非foreach结构的数字类型的显示方法，而fld.value$是专用于foreach 结构的强制显示为数字的方法。<br>
	 *
	 * 2、IRecord rec = REC2("users",Arrays.asList(1,2,3,4)) <br>
	 * select * from t_student where id in (${ foreach id in users id } ) <br>
	 * 生成: select * from t_student where id in (1, 2, 3, 4 ) <br>
	 *
	 * 3、IRecord rec = REC2("users",Arrays.asList("1,2,3,4".split(","))) <br>
	 * select * from t_student where id in ( ${ foreach id in users id } ) <br>
	 * 生成: select * from t_student where id in ('1', '2', '3', '4' ) <br>
	 *
	 * @param rec 被替换数据的键值对
	 * @return 替换后的sql语句。
	 */
	public String string(final IRecord rec) {
		if (rec == null)
			return this.string();
		return this.string(rec.toMap());
	}

	/**
	 * 带有变量值的语句内容。 用map中的值数据替换掉 sql模板语句的内容。
	 * 
	 * @param map 变量的名与值的集合
	 * @return SQL语句
	 */
	public String string(final Map<String, Object> map) {
		String s = this.string();
		final Function<Object, String> escape = obj -> asString(obj) //
				.replace("(", "\\(") //
				.replace(")", "\\)") //
				.replace("$", "\\$");
		for (final Term term : this.terms()) {
			// try {// 确保出现错误依旧可以运行
			if (term.getType() == Term.TermType.SYMBOL) {// 记录符号
				Object obj = map.get(term.getSymbol());
				String v = (obj + "").replace("$", "\\$");// 转义$符号
				if (!v.matches("[.\\d]+"))
					v = v.replace(".", "\\.");// 如果不是字符进行。转义
				// System.out.println(v); // 测试数据模式
				if (v == null)
					continue; // 空值不予处理
				s = s.replaceAll("\\$?\\s*\\{\\s*" + term + "\\s*}", "'" + v + "'");
				s = s.replaceAll("##" + term, v + "");// 使用双#号表示为数字不需要用括号括起来
				s = s.replaceAll("#" + term, "'" + v + "'");// 使用单警号表示是字符串需要采用括号括起来
			} else if (term.getType() == Term.TermType.FOREACH) {// foreach 解析
				Object t = term.toForeachString(map);// 获得term 的替换后的数据
				if (t == null)
					continue;
				String foreach_term = "\\$?(\\s*)\\{\\s*" + escape.apply(term.data) + "\\s*}";// foreach的字符描述
				final var matcher = Pattern.compile(foreach_term).matcher(s);
				if (matcher.find()) {
					final var ws = matcher.group(1); // 空白
					s = matcher.replaceAll(ws + escape.apply(t));
				}
			} else if (term.getType() == Term.TermType.WHERE) {
				final Object t = term.toWhereString(map);// 获得term 的替换后的数据
				if (t == null)
					continue;
				final String where_term = "\\$?\\s*\\{\\s*" + escape.apply(term.data) + "\\s*}";// foreach的字符描述
				s = s.replaceAll(where_term, escape.apply(t));
			}
			// }catch(Exception e) {e.printStackTrace();}
		} // for

		return s;
	}

	/**
	 * 插入数据
	 * 
	 * @param datas   插入的数据的字段列表，用recB-recA的中的变量来实例化SQL模板，生成一个insert 插入语句
	 * @param pfilter 插入的字段过滤器
	 * @return insert SQL 语句
	 */
	public String insql(final Iterable<IRecord> datas, final Predicate<KVPair<String, Object>> pfilter) {
		final var buffer = new StringBuilder();
		final var initialCapacity = datas instanceof Collection<?> c ? c.size() : 30; // 默认30个字段
		final var fldsAr = new AtomicReference<List<String>>(new ArrayList<String>(initialCapacity)); // 字段列表
		final var values = new ArrayList<List<String>>(initialCapacity);// 字段值列表的列表其元素value与flds一一对应
		final Predicate<KVPair<String, Object>> pft = pfilter == null ? kv -> true : pfilter;
		final var indices = new HashMap<String, Integer>(); // 位置索引缓存

		for (final var rec : datas) {
			final var vv = new ArrayList<String>(initialCapacity);// 字段值列表与flds 一一对应
			rec.tupleS().filter(pft).forEach(e -> { // 删除空值字符串 并进行数据处理
				final var value = quoteString(e.value()); // 键值用引号括起来
				final var name = parseFieldName(e.key()).str("name");// 获取字段名
				final var i = indices.computeIfAbsent(name, k -> { // 获得字段名的位置索引
					final var flds = fldsAr.get();
					final var j = flds.indexOf(k);
					if (j < 0) { // 新字段
						flds.add(name);
						return flds.size() - 1; // 返回索引位置
					} else { // 返回索引位置
						return j;
					} // if
				}); // 需要插入的位置

				if (i >= vv.size()) { // 所谓i位于值数组之后
					final var n = i - vv.size(); // 区间长度不包含最有i所在的位置
					if (n > 0) { // 填充空隙值
						vv.addAll(Arrays.asList(new String[n]));
					} // if
					vv.add(value);
				} else { // 覆盖对应位置的字段值
					vv.set(i, value);
				} // if
			});// forEach

			values.add(vv);
		} // for

		// 字段值复核
		final var flds = fldsAr.get(); // 提取字段列表
		final var len = flds.size(); // 最大的行记录的长度
		for (final var value : values) { // 补充空缺字段值
			final var n = len - value.size();
			if (n > 0) { // 补充空缺值
				value.addAll(Arrays.asList(new String[n]));
			} // if
		} // for

		buffer.append("insert into ").append(this.name).append(" (");
		buffer.append(String.join(",", flds));
		buffer.append(") values\n  ");

		final var line = values.stream().map(vals -> String.format("(%s)", String.join(",", vals))) //
				.collect(Collectors.joining(",\n  "));
		buffer.append(line);
		buffer.append("");

		if (debug) {
			System.out.println(String.format("insql:%s", buffer));
		}

		return buffer.toString();
	}

	/**
	 * 插入数据
	 * 
	 * @param pfilter 插入的字段过滤器
	 * @return insert SQL 语句
	 */
	public String insql(final Predicate<KVPair<String, Object>> pfilter) {
		return this.insql(this.sqlctx, pfilter);
	}

	/**
	 * 插入数据
	 * 
	 * @param datas 插入的数据的字段列表，用recB-recA的中的变量来实例化SQL模板，生成一个insert 插入语句
	 * @return insert SQL 语句
	 */
	public String insql(final IRecord... datas) {
		return this.insql(Arrays.asList(datas), null);
	}

	/**
	 * 插入数据
	 * 
	 * @param datas 插入的数据的字段列表，用recB-recA的中的变量来实例化SQL模板，生成一个insert 插入语句
	 * @return insert SQL 语句
	 */
	public String insql(final Iterable<IRecord> datas) {
		return this.insql(datas, null);
	}

	/**
	 * 生成insert 插入语句
	 * 
	 * @return insert 插入语句
	 */
	public String insql() {
		final var line = this.insql(this.sqlctx, kv -> kv.value() != null);
		return line.endsWith(";") ? line : (line + ";");
	}

	/**
	 * 字段信息扩展
	 * 
	 * @param recB 覆盖到本字段信息的数据记录
	 * @return 字段信息扩展
	 */
	public IRecord extend(final IRecord recB) {
		if (this.sqlctx.size() <= 0)
			return recB.duplicate();
		IRecord recA = this.getSqlCtxAsOneRecord();
		return SimpleRecord.extend(recA, recB);
	}

	/**
	 * 更新语句(and 操作模式)
	 * 
	 * @param data 数据源,where的更新条件数据的键名用前缀:'*'或'#'进行标记,<br>
	 *             比如:REC("*a",1,"b",2,"*c",3) 表示 set b=2 where a=1 mode c=3
	 * @return 更新语句
	 */
	public String upsql(final IRecord data) {
		return upsql(data, null);
	}

	/**
	 * 更新语句
	 * 
	 * @param data 数据源,where的更新条件数据的键名用前缀:'*'或'#'进行标记,<br>
	 *             比如:REC("*a",1,"b",2,"*c",3) 表示 set b=2 where a=1 mode c=3
	 * @param mode AND:条件间使用与关系, 条件间使用 OR 关系
	 * @return 更新语句
	 */
	public String upsql(final IRecord data, final OpMode mode) {
		OpMode _mode = null == mode ? OpMode.AND : mode;
		final var opmode = switch (_mode) {
		case AND -> "and";
		case OR -> "or";
		case BLANK -> ""; // 空白算符
		default -> "and";
		};
		final var is_blankmode = mode == BLANK; // 是否是空白拼装模式
		if (data.size() < 2) {
			return null;
		} else {
			final var keys = data.keys();
			final var k_pattern = Pattern.compile("^[#*]+(.+$)"); // 键名结构
			final var v_pattern = Pattern.compile("^(\\s*(not\\s+)?([><=]+|between|in|is))(.+)$",
					Pattern.CASE_INSENSITIVE); // 键值结构
			final var lhs = keys.stream() // 条件字段的键名序列
					.filter(e -> k_pattern.matcher(e).matches()) // 键名模式皮欸
					.toArray(String[]::new); // 条件数据
			final var _lhs = lhs.length > 0 ? lhs : new String[] { keys.get(0) }; // 默认使用第一个键名作为条件数据
			final var whereclause = data.filter(_lhs).tupleS().map(e -> { // 条件语句的拼装
				final var k_matcher = k_pattern.matcher(e._1());
				Matcher v_matcher = null; // 值模式匹配
				final Object value; // 值对象
				final String op; // 操作符
				if (!is_blankmode && e._2() instanceof String s && (v_matcher = v_pattern.matcher(s)).matches()) {
					op = v_matcher.group(1).strip();
					value = v_matcher.group(4).strip();
				} else { // 其他形式按照等于号进行处理
					value = is_blankmode ? asString(e._2()) : quoteString(e._2());
					op = is_blankmode ? "" : "=";
				} // if
				final String condition; // 条件表达式
				final var _op = !op.isBlank() ? String.format(" %s ", op) : ""; // 对于非空表的op添加两边添加一个空格
				if (!k_matcher.find()) { // 是否寻找得到
					condition = String.format("%s%s%s", e._1(), _op, value);
				} else {
					final var key = k_matcher.group(1); // 提取键名
					condition = String.format("%s%s%s", key, _op, value);
				} // if
				final var n = _lhs.length; // 过滤条件数
				return (n > 1 && !is_blankmode) ? String.format("(%s)", condition) : condition; // 过滤条件数大于1,每个条件加括号否则不加
			}).collect(Collectors.joining( // 条件数拼接
					String.format("%s", !opmode.isBlank() //
							? String.format(" %s ", opmode) // 非空白模式两边添加一个空格
							: " ") // 空白模式使用一个空格替代
			)); // collect
			final var _data = data.filterNot(_lhs); // 更新数据
			final var sqlpattern = "update ##tbl set {foreach p in kvs %p.key=p.value} ##wherecluase";
			final var __data = rb("tbl,kvs,wherecluase").get(this.getName(), _data.kvs2(),
					whereclause.matches("^\\s+$") ? "" : String.format("where %s", whereclause));
			return nsql(sqlpattern, __data).format();
		}
	}

	/**
	 * 注意：不能把id 设置成null
	 *
	 * 使用 recB 去更新 recA
	 * 
	 * @param recA  老数据
	 * @param recB  新数据，用recB-recA的中的变量来实例化SQL模板，生成一个update语句
	 * @param which 更新范围,IRecord 解构个字段采用and 进行过滤,String 类型直接拼接到 update 语句结尾
	 * @return 更新的sql语句
	 */
	public String upsql(final IRecord recA, final IRecord recB, final Object which) {
		final StringBuilder buffer = new StringBuilder();
		buffer.append("update ").append(this.name());

		var __recA = (recA == null) ? new SimpleRecord() : recA;
		var __recB = (recB == null) ? new SimpleRecord() : recB;

		__recA = IRecord.REC(__recA.applyOnkeys(k -> parseFieldName(k).str("name")));
		final var rec = SimpleRecord.REC2(); // 使用SimpleRecord 以保证可以存放null 值
		__recB.foreach((k, v) -> {
			final var key = parseFieldName(k).get("name");
			rec.add(key, v);
		});
		__recB = rec;

		List<String> keys = __recA.kvs().stream().map(KVPair::key).collect(Collectors.toList());
		keys.addAll(__recB.kvs().stream().map(KVPair::key).collect(Collectors.toList()));
		keys = keys.stream().distinct().collect(Collectors.toList());
		final var kvs = new LinkedList<KVPair<String, Object>>();// 键值对
		// System.out.println("全量的更新键值表："+keys);
		for (String key : keys) {
			Object oA = __recA.get(key);
			Object oB = __recB.get(key);
			if (oA != null && oB != null && oA.equals(oB))
				continue; // 值相同不予更新
			if (__recB.has(key) && __recA.has(key)) {
				kvs.add(new KVPair<>(key, oB));
			} // if
		} // for

		if (kvs.size() > 0) {
			buffer.append(" set ");
			// 字段修改 set field = value
			buffer.append(kvs.stream().filter(kv -> !"id".equals(kv.key()) || kv.value() != null) // 过滤掉id==null 的键值
					.map(e -> e.key() + "=" + Jdbcs.format(e.value() == null ? "null" : "''{0}''",
							(e.value() instanceof Number ? e.value().toString() : e.value()) // e.value 的格式化
					)) // map
					.collect(Collectors.joining(",")));
		} // if kvs.size()>0

		// 指定更新范围
		if (which != null && which instanceof IRecord) {
			String filter = ((IRecord) which).kvs().stream().map(e -> e.key() + "='" + e.value() + "'")
					.collect(Collectors.joining(" and "));
			buffer.append(" where ").append(filter);
		} else if (which != null && which instanceof String) {
			buffer.append(" where ").append(which);
		} else if (which == null && __recA.get("id") != null) {// 默认更新id
			buffer.append(" where id='").append(__recA.get("id")).append("'");
		} // if which!=null

		return buffer.toString();
	}

	/**
	 * 更新语句
	 *
	 * @param newData  新数据，用 newData的中的变量来实例化SQL模板，生成一个update语句
	 * @param whichone 更新范围, 可以是 IRecord 或 String IRecord 解构每个字段采用and 进行过滤, String
	 *                 类型直接拼接到 update 语句结尾
	 * @return 更新后的sql 语句
	 */
	public String upsql(final IRecord newData, final Object whichone) {
		return this.upsql(this.getSqlCtxAsOneRecord(), newData, whichone);
	}

	/**
	 * 把sqlctx 合并成一个IRecord 以获得完整的keys 列表
	 * 
	 * @return IRecord
	 */
	public IRecord getSqlCtxAsOneRecord() {
		final var rec = IRecord.REC();
		this.sqlctx.forEach(rec::add);
		return rec;
	}

	/**
	 * 把sqlctx中的数据拼装成一个update 语句 <br>
	 * SQL.of("user",REC("id",1,"name","张三")).update("id") <br>
	 * 生成 update user set name='张三' where id='1'
	 * 
	 * @param ids 用作过滤条件的 字段名称 ,比如 <br>
	 * @return update 语句
	 */
	public String upsql2(final String... ids) {
		final var newData = this.getSqlCtxAsOneRecord();
		final var ss = Arrays.asList(ids);
		final var empty = IRecord.builder(newData.filter(kvp -> !ss.contains(kvp._1()))).get(UUID.randomUUID());
		return this.upsql(empty, newData, newData.filter(ids));
	}

	/**
	 * 把sqlctx中的数据拼装成一个update 语句 ids 默认为 id
	 * 
	 * @return update 语句
	 */
	public String upsql2() {
		return this.upsql2("id");
	}

	/**
	 * 生成select的sql语句
	 * 
	 * @param flds  select 的字段列表，其中字符串中的 ' 和 “ 会被转义
	 * @param join1 组内的连接词
	 * @param join2 组件间连接词
	 * @return select flds from name where sqlctx
	 */
	public String select(final String flds, final String join1, final String join2) {
		final var _join1 = join1 == null ? "and" : join1;
		final var _join2 = join2 == null ? "and" : join2;
		final Function<Object, String> escape = line -> (line + "").replace("'", "\\'").replace("\"", "\\\""); // 字符转义
		final var where = this.getSqlCtx().stream()
				.map(e -> e.tupleS().map(p -> Term.FT("$0='$1'", p._1(), escape.apply(p._2())))
						.collect(Collectors.joining(Term.FT(" $0 ", _join1))))
				.map(e -> Term.FT("($0)", e)).collect(Collectors.joining(Term.FT(" $0 ", _join2))).strip();
		final var whereline = where.length() < 1 ? "" : Term.FT("where $0", where);
		return Term.FT("select $0 from $1 $2 ", flds == null ? "*" : flds, this.name, whereline).strip();
	}

	/**
	 * 生成select的sql语句
	 * 
	 * @return select * from name where sqlctx
	 */
	public String select() {
		return this.select(null, null, null);
	}

	/**
	 * 更新语句:update all
	 *
	 * @param newData 新数据 用 newData的中的变量来实例化SQL模板，生成一个update语句
	 * @return 更新数据的sql
	 */
	public String update(final IRecord newData) {
		return this.upsql(this.getSqlCtxAsOneRecord(), newData, null);
	}

	/**
	 * 根据字段的数据类型进行表定义，这个函数不建议在生产环境中使用。<br>
	 * 仅用于原型开发时快速测试使用 <br>
	 * 对于以数字开始的字段名例如: "123abc" 会被视为 abc的字段名 ,该字段的类型长度为 123<br>
	 * 对于以#开头的字段名视为主键约束 比如:"#id" <br>
	 * 对于以~开头的字段名视为唯一约束 比如:"~name" 表示 添加唯一性约束<br>
	 * 比如:"~32name" 表示 添加唯一性约束,字符长度为32<br>
	 * int_pk_autocrement 默认为 false , 即 对于int 类型的key 不予添加自增长
	 * 
	 * @return [ <br>
	 *         0:drop table, <br>
	 *         1:drop table if exists &amp; create, <br>
	 *         2:create, <br>
	 *         3:primarykey (当为单值主键的时候 主键约束会直接写入 字段定义之中, 索引3就变成了唯一约束了,后续的项目也会提前),
	 *         <br>
	 *         4:unique constraints <br>
	 *         ],3和4 根据record设置 可能包含。 <br>
	 */
	public List<String> ctsqls() {
		return this.ctsqls(false);
	}

	/**
	 * 根据字段的数据类型进行表定义，这个函数不建议在生产环境中使用。<br>
	 * 仅用于原型开发时快速测试使用 <br>
	 * 对于以数字开始的字段名例如: "123abc" 会被视为 abc的字段名 ,该字段的类型长度为 123<br>
	 * 对于以#开头的字段名视为主键约束 比如:"#id" <br>
	 * 对于以~开头的字段名视为唯一约束 比如:"~name" 表示 添加唯一性约束<br>
	 * 比如:"~32name" 表示 添加唯一性约束,字符长度为32<br>
	 * 
	 * @param int_pk_autocrement 对于int类型的主键是否增加自增长
	 * @return [ <br>
	 *         0:drop table, <br>
	 *         1:drop table if exists &amp; create, <br>
	 *         2:create, <br>
	 *         3:primarykey (当为单值主键的时候 主键约束会直接写入 字段定义之中, 索引3就变成了唯一约束了,后续的项目也会提前),
	 *         <br>
	 *         4:unique constraints <br>
	 *         ],3和4 根据record设置 可能包含。 <br>
	 */
	public List<String> ctsqls(final boolean int_pk_autocrement) {
		return ctsqls(int_pk_autocrement, 0.5);
	}

	/**
	 * 根据字段的数据类型进行表定义，这个函数不建议在生产环境中使用。<br>
	 * 仅用于原型开发时快速测试使用 <br>
	 * 对于以数字开始的字段名例如: "123abc" 会被视为 abc的字段名 ,该字段的类型长度为 123<br>
	 * 对于以#开头的字段名视为主键约束 比如:"#id" <br>
	 * 对于以~开头的字段名视为唯一约束 比如:"~name" 表示 添加唯一性约束<br>
	 * 比如:"~32name" 表示 添加唯一性约束,字符长度为32<br>
	 * 
	 * @param int_pk_autocrement 对于int类型的主键是否增加自增长
	 * @param ratio              空间预留倍率
	 * @return [ <br>
	 *         0:drop table, <br>
	 *         1:drop table if exists &amp; create, <br>
	 *         2:create, <br>
	 *         3:primarykey (当为单值主键的时候 主键约束会直接写入 字段定义之中, 索引3就变成了唯一约束了,后续的项目也会提前),
	 *         <br>
	 *         4:unique constraints <br>
	 *         ],3和4 根据record设置 可能包含。 <br>
	 */
	public List<String> ctsqls(final boolean int_pk_autocrement, final double ratio) {
		final List<String> sqls = new LinkedList<>();// sql 语句集合
		final StringBuilder buffer = new StringBuilder();

		buffer.append("-- -----------------------------------\n");
		buffer.append("-- #").append(name).append("\n");
		buffer.append("-- author:gbench/").append(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()))
				.append("\n");
		buffer.append("-- -----------------------------------\n");
		buffer.append("drop table if exists ").append(name).append(";\n");
		sqls.add(buffer.toString());

		final var primaryKeys = new LinkedList<Tuple2<String, String>>(); // 主键列表
		final var uniqueKeys = new LinkedList<String>(); // 主键列表
		final var fldDefs = new LinkedHashMap<String, String>();
		final var tableDefs = IRecord.REC(); // 表的定义

		this.sqlctx.forEach(tableDefs::add);
		if (debug) {
			System.out.println(String.format("%s", tableDefs));
		}

		tableDefs.tupleS().forEach(e -> {
			final var key = e.key().strip();// 提取主键描述字符串
			final var fldrec = parseFieldName(key);// 接卸字段描述
			final var value = e.value(); // 键值
			final Supplier<Integer> default_size = () -> { // 根据示例数据值提取长度
				final int size = Optional.ofNullable(quoteString(value)) //
						.map(String::length).map(length -> {
							final var len_preseved = (int) Math.ceil(length * (1 + ratio)); // 空间预
							if (debug) {
								final var format = "\n%s[%s] ----> %d[原长度] , %d[预留倍率%.2f长度]";
								final var line = String.format(format, value, value.getClass(), length, len_preseved,
										1 + ratio);
								System.out.println(line);
							} // if
							return len_preseved; // 空间预留长度
						}).orElse(null);
				return size; // 字段长度
			}; // 根据值内容确定空间大小
			final var type = value instanceof String && Json.isJson(value) //
					? "JSON" // json 类型
					: (e.value() == null) //
							? "VARCHAR(512)" // 默认类型为 字符串
							: javaType2SqlType((value instanceof Class<?> ? (Class<?>) value : value.getClass()), //
									Optional.ofNullable(fldrec.i4("size")).orElseGet(default_size));
			final var name = fldrec.str("name");

			if (fldrec.bool("primarykey")) {
				primaryKeys.add(P(name, type));// 加入主键
			}
			if (fldrec.bool("uniquekey")) {
				uniqueKeys.add(fldrec.str("name"));// 加入唯一性约束
			}
			final var line = String.format("%s %s", name, type);
			if (debug) {
				System.out.println(String.format("fld def line %s : %s", name, line));
			}
			fldDefs.put(name, line);
		});// map

		final var constraints = new LinkedList<String>(); // 约束的定义
		if (primaryKeys.size() > 0) {
			var flag = false;// 是否在行内添加
			if (primaryKeys.size() == 1) {
				final var tup = primaryKeys.getFirst();
				final var typename = tup._2().toUpperCase();
				if ("INT".equals(typename) || "BIGINT".equals(typename)) {// 对于 int key 是否 auto_increment
					flag = true;
					fldDefs.computeIfPresent(tup._1(), (fldname, def) -> Jdbcs.format("{0} {1} {2}", def,
							int_pk_autocrement ? "auto_increment" : "", "primary key"));
				} // if
			} // if
			if (!flag) {
				constraints.add(Jdbcs.format("alter table {0} add constraint primary key pk_{0} ({1});", name,
						primaryKeys.stream().map(e -> e._1().toLowerCase()).collect(Collectors.joining(","))));
			}
		} // primaryKeys.size

		if (uniqueKeys.size() > 0) {// 唯一约束
			constraints.add(Jdbcs.format("alter table {0} add constraint unique uq_{0} ({1});", name.toLowerCase(),
					String.join(",", uniqueKeys)));
		}

		if (debug) {
			System.out.println(String.format("%s", fldDefs));
		}
		final var flds = String.join(",\n\t", fldDefs.values());

		buffer.append("create table ").append(name).append(" (\n\t");
		buffer.append(flds);
		buffer.append("\n)");
		buffer.append(" comment '").append(name).append("';");
		sqls.add(buffer.toString());
		sqls.add(buffer.substring(sqls.get(0).length()));
		sqls.addAll(constraints);

		if (debug) {
			System.out.println(String.format("create sql: \n%s", sqls));
		}

		return sqls;
	}

	/**
	 * 根据字段的数据类型进行表定义，这个函数不建议在生产环境中使用。<br>
	 * 仅用于原型开发时快速测试使用 <br>
	 * 对于以数字开始的字段名例如: "123abc" 会被视为 abc的字段名 ,该字段的类型长度为 123<br>
	 * 对于以#开头的字段名视为主键约束 比如:"#id" <br>
	 * 对于以~开头的字段名视为唯一约束 比如:"~name" 表示 添加唯一性约束<br>
	 * 比如:"~32name" 表示 添加唯一性约束,字符长度为32<br>
	 * 
	 * @param datas 源数据，通过分析源数据却确定表结构
	 * @return 返回对应索引号所指定的sql,若 i&lt;0 或 i&gt;4 返回null
	 */
	public List<String> ctsqls(final Iterable<IRecord> datas) {
		return sql(this.name, SQL.proto_of(datas)).ctsqls();
	}

	/**
	 * 根据字段的数据类型进行表定义，这个函数不建议在生产环境中使用。<br>
	 * 仅用于原型开发时快速测试使用 <br>
	 * 对于以数字开始的字段名例如: "123abc" 会被视为 abc的字段名 ,该字段的类型长度为 123<br>
	 * 对于以#开头的字段名视为主键约束 比如:"#id" <br>
	 * 对于以~开头的字段名视为唯一约束 比如:"~name" 表示 添加唯一性约束<br>
	 * 比如:"~32name" 表示 添加唯一性约束,字符长度为32<br>
	 * 
	 * @param datas              源数据，通过分析源数据却确定表结构
	 * @param id_flag            是否需要补充id主键，即 是否添加一个int 类型的id主键,true:添加,false
	 *                           不添加,注意仅当id不存在或是id类型为int类型的时候才会创建自增长
	 * @param int_pk_autocrement 对于int类型的主键是否增加自增长，与 id_flag 相关
	 * @return sql语句的索引序号: [ <br>
	 *         0:drop table, <br>
	 *         1:drop table if exists &amp; create, <br>
	 *         2:create, <br>
	 *         3:primarykey (当为单值主键的时候 主键约束会直接写入 字段定义之中, 索引3就变成了唯一约束了,后续的项目也会提前),
	 *         <br>
	 *         4:unique constraints <br>
	 *         ],3和4 根据record设置 可能包含。 <br>
	 * 
	 */
	public List<String> ctsqls(final Iterable<IRecord> datas, final boolean id_flag, final boolean int_pk_autocrement) {
		final var proto = proto_of(datas).aoks2rec(e -> e.toLowerCase());
		final var _proto = id_flag // 是否需要补充id主键
				? REC("#id", proto.opt("id").orElse(1)) // 重用原来的id值,没有id值的化默认为1
						.derive(proto.remove("id")) // 移除id键，如果有的话，即 用 #id 替换了 id
				: proto;
		return sql(this.name, _proto).ctsqls(int_pk_autocrement);
	}

	/**
	 * 根据字段的数据类型进行表定义，这个函数不建议在生产环境中使用。<br>
	 * 仅用于原型开发时快速测试使用 <br>
	 * 对于以数字开始的字段名例如: "123abc" 会被视为 abc的字段名 ,该字段的类型长度为 123<br>
	 * 对于以#开头的字段名视为主键约束 比如:"#id" <br>
	 * 对于以~开头的字段名视为唯一约束 比如:"~name" 表示 添加唯一性约束<br>
	 * 比如:"~32name" 表示 添加唯一性约束,字符长度为32<br>
	 * 
	 * @param int_pk_autocrement 对于int类型的主键是否增加自增长
	 * @param i                  sql语句的索引序号: [ <br>
	 *                           0:drop table, <br>
	 *                           1:drop table if exists &amp; create, <br>
	 *                           2:create, <br>
	 *                           3:primarykey (当为单值主键的时候 主键约束会直接写入 字段定义之中,
	 *                           索引3就变成了唯一约束了,后续的项目也会提前), <br>
	 *                           4:unique constraints <br>
	 *                           ],3和4 根据record设置 可能包含。 <br>
	 * 
	 * 
	 * @return 返回对应索引号所指定的sql,若 i&lt;0 或 i&gt;4 返回null
	 */
	public String ctsql(final boolean int_pk_autocrement, final int i) {
		return i < 0 || i > 4 ? null : this.ctsqls(int_pk_autocrement).get(i);
	}

	/**
	 * 带有自自增长主键功能SQL创建语句函数<br>
	 * this.ctsql(datas, true, true, i) 的 简写 <br>
	 * 根据字段的数据类型进行表定义，这个函数不建议在生产环境中使用。<br>
	 * 仅用于原型开发时快速测试使用 <br>
	 * 对于以数字开始的字段名例如: "123abc" 会被视为 abc的字段名 ,该字段的类型长度为 123<br>
	 * 对于以#开头的字段名视为主键约束 比如:"#id" <br>
	 * 对于以~开头的字段名视为唯一约束 比如:"~name" 表示 添加唯一性约束<br>
	 * 比如:"~32name" 表示 添加唯一性约束,字符长度为32<br>
	 * 
	 * @param data 源数据，通过分析源数据却确定表结构
	 * @param i    sql语句的索引序号: [ <br>
	 *             0:drop table, <br>
	 *             1:drop table if exists &amp; create, <br>
	 *             2:create, <br>
	 *             3:primarykey (当为单值主键的时候 主键约束会直接写入 字段定义之中,
	 *             索引3就变成了唯一约束了,后续的项目也会提前), <br>
	 *             4:unique constraints <br>
	 *             ],3和4 根据record设置 可能包含。 <br>
	 * @return 返回对应索引号所指定的sql,若 i&lt;0 或 i&gt;4 返回null
	 */
	public String ctsql(final IRecord data, final int i) {
		return this.ctsql(Arrays.asList(data), true, true, i);
	}

	/**
	 * 根据字段的数据类型进行表定义，这个函数不建议在生产环境中使用。<br>
	 * 仅用于原型开发时快速测试使用 <br>
	 * 对于以数字开始的字段名例如: "123abc" 会被视为 abc的字段名 ,该字段的类型长度为 123<br>
	 * 对于以#开头的字段名视为主键约束 比如:"#id" <br>
	 * 对于以~开头的字段名视为唯一约束 比如:"~name" 表示 添加唯一性约束<br>
	 * 比如:"~32name" 表示 添加唯一性约束,字符长度为32<br>
	 * 
	 * @param data               源数据，通过分析源数据却确定表结构
	 * @param id_flag            是否需要补充id主键，即 是否添加一个int 类型的id主键,true:添加,false
	 *                           不添加,注意仅当id不存在或是id类型为int类型的时候才会创建自增长
	 * @param int_pk_autocrement 对于int类型的主键是否增加自增长，与 id_flag 相关
	 * @param i                  sql语句的索引序号: [ <br>
	 *                           0:drop table, <br>
	 *                           1:drop table if exists &amp; create, <br>
	 *                           2:create, <br>
	 *                           3:primarykey (当为单值主键的时候 主键约束会直接写入 字段定义之中,
	 *                           索引3就变成了唯一约束了,后续的项目也会提前), <br>
	 *                           4:unique constraints <br>
	 *                           ],3和4 根据record设置 可能包含。 <br>
	 * 
	 * 
	 * @return 返回对应索引号所指定的sql,若 i&lt;0 或 i&gt;4 返回null
	 */
	public String ctsql(final IRecord data, final boolean id_flag, final boolean int_pk_autocrement, final int i) {
		return ctsql(Arrays.asList(data), id_flag, int_pk_autocrement, i);
	}

	/**
	 * 带有自自增长主键功能SQL创建语句函数<br>
	 * this.ctsql(datas, true, true, i) 的 简写 <br>
	 * 根据字段的数据类型进行表定义，这个函数不建议在生产环境中使用。<br>
	 * 仅用于原型开发时快速测试使用 <br>
	 * 对于以数字开始的字段名例如: "123abc" 会被视为 abc的字段名 ,该字段的类型长度为 123<br>
	 * 对于以#开头的字段名视为主键约束 比如:"#id" <br>
	 * 对于以~开头的字段名视为唯一约束 比如:"~name" 表示 添加唯一性约束<br>
	 * 比如:"~32name" 表示 添加唯一性约束,字符长度为32<br>
	 * 
	 * @param datas 源数据，通过分析源数据却确定表结构
	 * @param i     sql语句的索引序号: [ <br>
	 *              0:drop table, <br>
	 *              1:drop table if exists &amp; create, <br>
	 *              2:create, <br>
	 *              3:primarykey (当为单值主键的时候 主键约束会直接写入 字段定义之中,
	 *              索引3就变成了唯一约束了,后续的项目也会提前), <br>
	 *              4:unique constraints <br>
	 *              ],3和4 根据record设置 可能包含。 <br>
	 * @return 返回对应索引号所指定的sql,若 i&lt;0 或 i&gt;4 返回null
	 */
	public String ctsql(final Iterable<IRecord> datas, final int i) {
		return this.ctsql(datas, true, true, i);
	}

	/**
	 * 根据字段的数据类型进行表定义，这个函数不建议在生产环境中使用。<br>
	 * 仅用于原型开发时快速测试使用 <br>
	 * 对于以数字开始的字段名例如: "123abc" 会被视为 abc的字段名 ,该字段的类型长度为 123<br>
	 * 对于以#开头的字段名视为主键约束 比如:"#id" <br>
	 * 对于以~开头的字段名视为唯一约束 比如:"~name" 表示 添加唯一性约束<br>
	 * 比如:"~32name" 表示 添加唯一性约束,字符长度为32<br>
	 * 
	 * @param datas              源数据，通过分析源数据却确定表结构
	 * @param id_flag            是否需要补充id主键，即 是否添加一个int 类型的id主键,true:添加,false
	 *                           不添加,注意仅当id不存在或是id类型为int类型的时候才会创建自增长
	 * @param int_pk_autocrement 对于int类型的主键是否增加自增长，与 id_flag 相关
	 * @param i                  sql语句的索引序号: [ <br>
	 *                           0:drop table, <br>
	 *                           1:drop table if exists &amp; create, <br>
	 *                           2:create, <br>
	 *                           3:primarykey (当为单值主键的时候 主键约束会直接写入 字段定义之中,
	 *                           索引3就变成了唯一约束了,后续的项目也会提前), <br>
	 *                           4:unique constraints <br>
	 *                           ],3和4 根据record设置 可能包含。 <br>
	 * 
	 * 
	 * @return 返回对应索引号所指定的sql,若 i&lt;0 或 i&gt;4 返回null
	 */
	public String ctsql(final Iterable<IRecord> datas, final boolean id_flag, final boolean int_pk_autocrement,
			final int i) {
		return i < 0 || i > 4 ? null : sql(this.name).ctsqls(datas, id_flag, int_pk_autocrement).get(i);
	}

	/**
	 * 获得模板变量
	 * 
	 * @return 去重了之后的模板变量
	 */
	public List<Term> terms() {
		return getAllTerms(sqltpl).stream().distinct().collect(Collectors.toList());
	}

	/**
	 * 返回sqlCtx
	 * 
	 * @return sqlctx
	 */
	public List<IRecord> getSqlCtx() {
		return this.sqlctx;
	}

	/**
	 * 返回sqlCtx的IRecord 数组
	 * 
	 * @return IRecord 的数组
	 */
	public IRecord[] getSqlCtx2() {
		return this.sqlctx.toArray(IRecord[]::new);
	}

	/**
	 * 返回sqltpl
	 * 
	 * @return sqltpl
	 */
	public String getSqlTpl() {
		return this.sqltpl;
	}

	/**
	 * 返回名称
	 * 
	 * @return 名称
	 */
	public String getName() {
		return this.name;
	}

	/**
	 * 格式化输出:尝试 返回sqltpl,若sqltpl为空则返回insert()语句
	 * 
	 * @return 数据文本字符串
	 */
	public String toString() {
		if (this.string() != null) {
			return this.string() + (terms().size() <= 0 ? "" : "\nterms:" + terms());
		} else {
			return this.insql();
		}
	}

	/**
	 * SQL 语句Builder
	 * 
	 * @author gbench
	 *
	 */
	public static class Builder {

		/**
		 * 构造函数
		 * 
		 * @param name SQL名称
		 */
		public Builder(final String name) {
			this.name = name;
		}

		/**
		 * 生成一个SQL语句对象
		 * 
		 * @param recs sql 上下文集合
		 * @return SQL对象
		 */
		public SQL get(final IRecord... recs) {
			return new SQL(name, Arrays.asList(recs));
		}

		private final String name;// SQL语句的名称
	}

	/**
	 * 命名SQL语句对象，每个SQL都有一个名字
	 * 
	 * @param name SQL语句的名，可以是文本字符串，由于标识SQL，便于检索
	 * @param recs 行数据，SQL 语句的上下文数据：比如插入的操作的，插入记录.数据的键名
	 *             采用普通命名:即name,不需要加入格式化标记,比如#name,或##name
	 * @return SQL对象
	 */
	public static SQL of(final String name, final IRecord... recs) {
		return new SQL(name, Arrays.asList(recs));
	}

	/**
	 * 命名SQL语句对象，每个SQL都有一个名字
	 * 
	 * @param name   SQL语句的名，可以是文本字符串，由于标识SQL，便于检索
	 * @param sqltpl sql 模版,占位符用#标记,#name表示字符串类项目即值被引号括起来,##name,表示数值项目,即值不会被引号括起来引号
	 * @param recs   行数据，SQL 语句的上下文数据：比如插入的操作的，插入记录,上下文的数据的键名
	 *               采用普通命名:即name,不需要加入格式化标记,比如#name,或##name
	 * @return SQL对象
	 */
	public static SQL of(final String name, final String sqltpl, final IRecord... recs) {
		return new SQL(name, sqltpl, recs);
	}

	/**
	 * of的别名 &amp; 命名SQL语句对象，每个SQL都有一个名字
	 * 
	 * @param name SQL语句的名，可以是文本字符串，由于标识SQL，便于检索
	 * @param recs 行数据，SQL 语句的上下文数据：比如插入的操作的，插入记录.数据的键名
	 *             采用普通命名:即name,不需要加入格式化标记,比如#name,或##name
	 * @return SQL对象
	 */
	public static SQL sql(final String name, final IRecord... recs) {
		return SQL.of(name, recs);
	}

	/**
	 * of的别名 &amp; 命名SQL语句对象，每个SQL都有一个名字
	 * 
	 * @param name SQL语句的名，可以是文本字符串，由于标识SQL，便于检索
	 * @param recs 行数据，SQL 语句的上下文数据：比如插入的操作的，插入记录.数据的键名
	 *             采用普通命名:即name,不需要加入格式化标记,比如#name,或##name
	 * @return SQL对象
	 */
	public static SQL sql(final String name, final Iterable<IRecord> recs) {
		return SQL.of(name, StreamSupport.stream(recs.spliterator(), false).toArray(IRecord[]::new));
	}

	/**
	 * 命名SQL语句对象，每个SQL都有一个名字 <br>
	 * name SQL语句的任意名称 <br>
	 * 
	 * final var sql = nsql("insert into ##tbl(id,name) values
	 * (##id,#name)",REC("tbl","t_user","id",1,"name","zhangsan"));<br>
	 * println(sql.format()); 的结果是 <br>
	 * insert into t_user(id,name) values (1,'zhangsan')
	 * 
	 * @param pattern sql模版,占位符 用#标记,如: #name表示字符串类项目 即 值 会被 引号括起来, <br>
	 *                ##name,表示数值项目 即 值 不会被 引号括起来 <br>
	 * @param recs    pattern 的上下文数据, pattern 中的占位符的字典(key,value)集合，占位符对应key <br>
	 *                占位符采用普通命名法:即name,不需要加入 格式化标记(#), 比如#name,或##name 的key是错误的
	 * @return SQL对象
	 */
	public static SQL nsql(final String pattern, final IRecord... recs) {
		return new SQL(UUID.randomUUID().toString(), pattern, recs);
	}

	/**
	 * 命名SQL语句对象，每个SQL都有一个名字 <br>
	 * name SQL语句的任意名称 <br>
	 * 
	 * final var sql = nsql("insert into ##_0(id,name,rank) values
	 * (##_1,#_2,0)","t_user",1,"zhangsan");<br>
	 * println(sql.format()); 的结果是 <br>
	 * insert into t_user(id,name,rank) values (1,'zhangsan',0)
	 * 
	 * @param pattern sql模版,占位符 用#标记,如: #_0表示字符串类项目 即 值 会被 引号括起来, <br>
	 *                ##_0,表示数值项目 即 值 不会被 引号括起来 <br>
	 * @param vv      pattern的上下文数据, pattern 中的占位符的字典(key,value)集合，占位符对应key <br>
	 *                占位符采用普通序列命名法:即_0,_2,...,_n等
	 * @return SQL对象
	 */
	public static SQL nsql(final String pattern, final Object... vv) {
		return nsql(pattern, IRecord.A2R(vv).aoks2rec(e -> "_" + e));
	}

	/**
	 * 命名SQL语句对象，每个SQL都有一个名字
	 * 
	 * @param name SQL语句的名，可以是文本字符串，用于标识SQL，便于检索
	 * @return SQL对象
	 */
	public static SQL.Builder builder(final String name) {
		return new Builder(name);
	}

	/**
	 * builder 的别名 <br>
	 * 命名SQL语句对象，每个SQL都有一个名字
	 * 
	 * @param name SQL语句的名，可以是文本字符串，由于标识SQL，便于检索
	 * @return SQL对象
	 */
	public static SQL.Builder bd(final String name) {
		return builder(name);
	}

	/**
	 * 解析一个SQL字段定义
	 * 
	 * @param line 字段定义行 <br>
	 *             对于以数字开始的字段名例如: "123abc" 会被视为 abc的字段名 ,该字段的类型长度为 123<br>
	 *             对于以#开头的字段名视为主键约束 比如:"#id" <br>
	 *             对于以~开头的字段名视为唯一约束 比如:"~name" 表示 添加唯一性约束<br>
	 *             比如:"~32name" 表示 添加唯一性约束,字符长度为32<br>
	 * @return {name:字段名,size:字段size,primarykey:是否为主键约束,"uniquekey",是否为唯一约束}
	 */
	public static IRecord parseFieldName(final String line) {

		final String FIELD_NAME_PATTERN = "\\s*([-#~!@\\+])?\\s*([\\d]+)?\\s*([a-zA-z].*)\\s*"; // 字段名Pattern
		final var pattern = Pattern.compile(FIELD_NAME_PATTERN);
		final var matcher = pattern.matcher(line);

		if (matcher.find()) {
			final var flagstr = matcher.group(1);
			final var sizestr = matcher.group(2);
			final var size = sizestr == null ? null : Integer.parseInt(sizestr);
			final var name = matcher.group(3);
			return IRecord.REC("size", size, "name", name, "primarykey", flagstr != null && flagstr.startsWith("#"),
					"uniquekey", flagstr != null && flagstr.startsWith("~"));
		} else {
			return IRecord.REC("name", line);
		} // if
	}

	/**
	 * 创建表的sql语句
	 * 
	 * @param table 表名
	 * @param rec   字段定义
	 * @param brief 表摘要信息
	 * @return 创建表解构
	 */
	public static List<String> createTable(final String table, final IRecord rec, final String brief) {
		final var sqls = new LinkedList<String>();
		var buffer = new StringBuffer();
		buffer.append("-- -----------------------------------\n");
		buffer.append("-- #").append(brief).append("\n");
		buffer.append("-- author:gbench/").append(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()))
				.append("\n");
		buffer.append("-- -----------------------------------\n");
		buffer.append("drop table if exists ").append(table).append(";\n");
		sqls.add(buffer.toString());

		buffer = new StringBuffer();
		buffer.append("create table ").append(table).append("(\n\t");
		String flds = rec.kvs().stream().map(e -> e.key() + " " + e.value() + "").collect(Collectors.joining(",\n\t"));
		buffer.append(flds);
		buffer.append("\n)");
		buffer.append(" comment '").append(brief).append("';");
		sqls.add(buffer.toString());
		return sqls;
	}

	/**
	 * @param table 表名
	 * @param rec   字段定义
	 * @return 创建表解构
	 */
	public static List<String> createTable(final String table, final IRecord rec) {
		return createTable(table, rec, table);
	}

	/**
	 * 从模板文字中提取文件参数变量
	 * 
	 * @param line 模板
	 * @return 返回变量的次序就是变量在模板中出现测序，并且没有去重
	 */
	public static List<Term> getAllTerms(final String line) {
		final List<Term> ll = new LinkedList<>();

		if (line != null) {
			for (final String var_pattern : VAR_NAME_PATTERNS) {// 变量名模式
				final Pattern p = Pattern.compile(var_pattern);
				final Matcher mat = p.matcher(line); // 从上一次位置之后的继续读
				while (mat.find()) {
					final var term = new Term(mat.group(1)); // 需要注意 term 的不能以数字开头
					ll.add(term);
				} // while
			} // for
		} // if

		return ll;
	}

	/**
	 * javaType转sqlType
	 * 
	 * @param cls  JAVA类名
	 * @param size 类型参数的长度
	 * @return 对应于cls的SQL 数据类型
	 */
	public static String javaType2SqlType(final Class<?> cls, final Number size) {
		if (cls.equals(Double.class) || cls.equals(double.class) || cls.equals(Float.class)
				|| cls.equals(float.class)) {
			return "DOUBLE";
		} else if (cls.equals(Boolean.class) || cls.equals(boolean.class)) {
			return "TINYINT";
		} else if (cls.equals(Character.class) || cls.equals(char.class)) {
			return "VARCHAR(12)";// 字节
		} else if (cls.equals(Integer.class) || cls.equals(int.class) || cls.equals(Short.class)
				|| cls.equals(short.class)) {
			return "INT";
		} else if (cls.equals(Long.class) || cls.equals(long.class)) {
			return "BIGINT";
		} else if (cls.equals(LocalDate.class)) {
			return "DATE";
		} else if (cls.equals(Date.class) || cls.equals(Timestamp.class) || cls.equals(LocalDateTime.class)
				|| cls.equals(LocalTime.class)) {
			// return "TIMESTAMP";
			return "DATETIME";
		} else if (Map.class.isAssignableFrom(cls) || IRecord.class.isAssignableFrom(cls)
				|| ((cls instanceof Class<?>) && Iterable.class.isAssignableFrom(cls))) {
			return "JSON";
		} else {
			return Jdbcs.format("VARCHAR({0})", size == null ? 512 : size.intValue());
		}
	}

	/**
	 * 获得原型
	 * 
	 * @param <U>        数据元素
	 * @param partitions 数据分组
	 * @return IRecord
	 */
	public static <U extends List<IRecord>> IRecord proto_of(final Map<? extends Integer, U> partitions) {
		return proto_of(partitions.entrySet().stream().flatMap(e -> e.getValue().stream()));
	}

	/**
	 * 获得原型
	 * 
	 * @param partitions 数据分组
	 * @return IRecord
	 */
	public static IRecord proto_of(final IRecord... partitions) {
		return proto_of(Stream.of(partitions));
	}

	/**
	 * 获得原型
	 * 
	 * @param partitions 数据分组
	 * @return IRecord
	 */
	public static IRecord proto_of(final Iterable<? extends IRecord> partitions) {
		return proto_of(StreamSupport.stream(partitions.spliterator(), false));
	}

	/**
	 * create table sql
	 * 
	 * @param name  表名
	 * @param proto 数据原型
	 * @return create table sql
	 */
	public static String ctsql(final String name, final IRecord proto) {
		return ctsql(name, proto, true);
	}

	/**
	 * create table sql
	 * 
	 * @param name   表名
	 * @param proto  数据原型,各个字段的数据定义
	 * @param adjust 是否对proto进行智能调整,如：增加id主键,将数字字符串转换成整数或是浮点数等
	 * @return create table sql
	 */
	public static String ctsql(final String name, final IRecord proto, final boolean adjust) {
		var __proto = proto;
		if (debug) {
			System.out.println(String.format("ctsql proto before:[ %s ], adjust:[ %s ]", proto, adjust));
		}

		if (adjust) { // proto
			Object id = proto.get("id"); // 提取主键
			final var _proto = (id == null ? proto : proto.remove("id")) // 如果存在id字段则给删除
					.aov2rec(e -> { // 数据类型调整
						final var line = e.toString();
						final var matcher = Pattern.compile("^(\\+\\-)?[0-9.]+$") // 数字类型检测器
								.matcher(line);
						final var b = matcher.matches();
						if (b && line.contains(".")) { // 浮点数
							try {
								return Double.parseDouble(line);
							} catch (Exception ex) {
								return e;
							}
						} else if (b) { // 整数
							try {
								return Integer.parseInt(line);
							} catch (Exception ex) {
								return e;
							}
						} else {
							return e;
						} // if
					}); // aov2rec
			__proto = REC("#id", Optional.ofNullable(id).orElse(1)).add(_proto);
		} // _proto

		if (debug) {
			System.out.println(String.format("ctsql proto before:[ %s ], adjust:[ %s ]", proto, adjust));
		}

		return sql(name, __proto).ctsqls(true).get(2);
	}

	/**
	 * 数据插入
	 * 
	 * @param name 数据表
	 * @param data 数据原型
	 * @return 数据插入 insert sql
	 */
	public static String insql(final String name, final List<IRecord> data) {
		return sql(name, data).insql();
	}

	/**
	 * 数据插入
	 * 
	 * @param name 数据表
	 * @param data 数据原型
	 * @return 数据插入 insert sql
	 */
	public static String insql(final String name, final IRecord... data) {
		return sql(name, data).insql();
	}

	/**
	 * 获得原型
	 * 
	 * @param partitions 数据分组
	 * @return IRecord
	 */
	public static IRecord proto_of(final Stream<? extends IRecord> partitions) {
		final var dfm = partitions.collect(DFrame.dfmclc);
		final var proto = dfm.aov2rec((List<Object> v) -> { // 提取
			return v.stream().map(e -> new Tuple2<>(SQL.quoteString(e), e))
					.sorted((a, b) -> -(a._1().length() - b._1().length())) // 获取最长长度作为原型
					.findFirst().map(e -> e._2()).get();
		});
		return proto;
	}

	/**
	 * 转成带有引号的字符串
	 * 
	 * @param obj
	 * @return
	 */
	public static String asString(final Object obj) {
		Object value = obj;

		// value 数据类型格式化
		if (value instanceof LocalDateTime) {
			value = Jdbcs.format("{0}", dtf1.format((LocalDateTime) value));
		} else if (value instanceof Date) {
			value = Jdbcs.format("{0}", sdf.format((Date) value));
		} else if (value instanceof LocalTime) {
			value = Jdbcs.format("{0}", dtf1.format((LocalTime) value));
		} else if (value instanceof LocalDate) {
			value = Jdbcs.format("{0}", dtf2.format((LocalDate) value));
		} else if (value instanceof Iterable || value instanceof Map || value instanceof IRecord
				|| (value != null && value.getClass().isArray())) {
			value = Json.obj2json(value);
		} else {
			// do Nothing
		} // if

		return String.valueOf(value);
	}

	/**
	 * 转成带有引号的字符串
	 * 
	 * @param obj 目标对象
	 * @return 对象的字符串的形式
	 */
	public static String quoteString(final Object obj) {
		final var qs = String.format("'%s'", asString(obj) // 用单引号括起来
				.replace("'", "''")); // 格式化字段值

		return qs;
	}

	/**
	 * 生成SQL语句: 依赖于字段反射，从obj(pojo)重提取属性数据，给予闯将对应ORM 关系映射里的 数据库表<br>
	 * 生成的表名默认为 obj的类名 :obj.getClass()。getSimpleName()
	 * 
	 * @param obj 待从中提取属性字段数据的javabean
	 * @return 生成SQL语句
	 */
	public static String DDL_CREATE_TABLE(final Object obj) {
		return DDL_CREATE_TABLE(obj, null);
	}

	/**
	 * 生成SQL语句 依赖于字段反射，从obj(pojo)重提取属性数据，给予闯将对应ORM 关系映射里的 数据库表
	 * 
	 * @param obj  待从中提取属性字段数据的javabean
	 * @param name 数据表明
	 * @return 生成SQL语句
	 */
	public static String DDL_CREATE_TABLE(final Object obj, final String name) {
		final Class<?> cls = obj instanceof Class<?> ? (Class<?>) obj : obj.getClass();
		final var tab = name == null ? cls.getSimpleName() : name;
		final var ll = new LinkedList<Tuple2<String, String>>();
		IRecord.FIELDS(cls).forEach((k, v) -> {
			String fld = k;
			String type = IRecord.OBJ2SQLTYPE(v.getType());
			ll.add(new Tuple2<>(fld, type));
		});
		final var s = "create table if not exists " + tab + " ("
				+ ll.stream().map(e -> e._1() + " " + e._2()).collect(Collectors.joining(",")) + ")";
		return s;
	}

	/**
	 * 生成SQL语句: <br>
	 * 依赖于字段反射，从obj(pojo)重提取属性数据，给予闯将对应ORM 关系映射里的 数据库表 <br>
	 * 表名默认为 obj的类名 :obj.getClass()。getSimpleName() <br>
	 * 
	 * @param obj 待从中提取属性字段数据的javabean
	 * @return 删除表的SQL语句
	 */
	public static String DDL_DROP_TABLE(final Object obj) {
		return DDL_DROP_TABLE(obj, null);
	}

	/**
	 * 生成SQL语句 <br>
	 * 依赖于字段反射，从obj(pojo)重提取属性数据，给予闯将对应ORM 关系映射里的 数据库表 <br>
	 * 表名默认为 obj的类名 :obj.getClass()。getSimpleName() <br>
	 * 
	 * @param obj  待从中提取属性字段数据的javabean <br>
	 * @param name 表名
	 * @return 删除表的SQL语句
	 */
	public static String DDL_DROP_TABLE(final Object obj, final String name) {
		final Class<?> cls = obj instanceof Class<?> ? (Class<?>) obj : obj.getClass();
		final var tab = name == null ? cls.getSimpleName() : name;
		return "drop table if exists " + tab;
	}

	/**
	 * 清除表中数据 <br>
	 * 依赖于字段反射，从obj(pojo)重提取属性数据，给予闯将对应ORM 关系映射里的 数据库表 <br>
	 * 表名默认为 obj的类名 :obj.getClass()。getSimpleName() <br>
	 * 
	 * @param obj 待从中提取属性字段数据的javabean
	 * @return truncate 的SQL语句
	 */
	public static String DDL_TRUNCATE_TABLE(final Object obj) {
		final Class<?> cls = obj instanceof Class<?> ? (Class<?>) obj : obj.getClass();
		return "truncate table " + cls.getSimpleName();
	}

	/**
	 * 参数变量的模式
	 */
	final static String[] VAR_NAME_PATTERNS = { // 参数变量的模式
			Term.TERM_PATTERN, // unix 变量格式
			"#{1,2}([_a-zA-z0-9]+)" // mybatis 变量格式
	};

	/**
	 * 时间是格式化
	 */
	final static DateTimeFormatter dtf1 = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
	final static DateTimeFormatter dtf2 = DateTimeFormatter.ofPattern("yyyy-MM-dd");
	final static SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	// debug 调试标记
	public static boolean debug = false;

	/**
	 * 更新的条件模式
	 */
	public enum OpMode {
		/**
		 * AND 拼装
		 */
		AND,
		/**
		 * OR 拼装
		 */
		OR,
		/**
		 * 自由拼装
		 */
		BLANK
	}

	private String name = null;// sql 名称
	private String sqltpl = null;// sql 语句模板， 占位符即模板参数式样：#xxx 字符串类型, ##xxx 数值类型,或是 ${xxx}
	private List<IRecord> sqlctx = null;// sql 上下文{参数集}
}