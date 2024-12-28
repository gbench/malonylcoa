package gbench.util.jdbc.sql;

import static gbench.util.jdbc.kvp.IRecord.REC;

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import gbench.util.jdbc.Jdbcs;
import gbench.util.jdbc.kvp.IRecord;
import gbench.util.jdbc.kvp.KVPair;
import gbench.util.jdbc.kvp.Tuple2;
import gbench.util.jdbc.kvp.Tuple3;

/**
 * 术语项目
 * 
 * @author gbench
 *
 */
public class Term {
	/**
	 * 字段项目
	 * 
	 * @param data 自动换项目的字符串序列
	 */
	public Term(String data) {
		final List<Predicate<String>> handlers = Arrays.asList(foreach_handler, where_handler, symbol_handler);
		final Iterator<Predicate<String>> itr = handlers.iterator();
		while (itr.hasNext() && itr.next().test(data))
			;
	}// Term

	/**
	 * 符号处理器
	 * 
	 * data 数据字符串
	 */
	public final Predicate<String> symbol_handler = (final String data) -> {
		final var SYMBOL_PATTERN = "\\s*([_$a-z][a-z0-9]*)\\s*";// 符号模式
		final var mat = Pattern.compile(SYMBOL_PATTERN, Pattern.CASE_INSENSITIVE).matcher(data);
		if (mat.find()) {
			this.data = data;
			this.type = TermType.SYMBOL;
			this.pattern = SYMBOL_PATTERN;
			return false;
		} else
			return true;
	};

	/**
	 * foreach 的处理
	 * 
	 * data 数据字符串
	 */
	public final Predicate<String> foreach_handler = (final String data) -> {
		final var FOREACH_PATTERN = "foreach\\s+([a-z_\\$]+)\\s+in\\s+([a-z%-_\\$]+)\\s+(.+)";// foreach 模式
		final var p = Pattern.compile(FOREACH_PATTERN, Pattern.CASE_INSENSITIVE);// %表示字段替换不需要甲引号
		final var m = p.matcher(data.trim());

		if (m.find()) {
			final var loopvar = m.group(1);// 迭代变量
			final var containerName = m.group(2);// 迭代的数据集合用于从 映射对象（map) 中提取循环变量的数据集合
			final var loopbody = m.group(3);// 循环体

			this.foreachTerm = new ForeachTerm(loopvar, containerName, loopbody);
			// System.out.println(loopvar+"==>"+container+"==>"+loopbody);
			final var pat = Pattern.compile(loopvar + "\\s*\\.\\s*" + "([_0-9a-z$]+)", Pattern.CASE_INSENSITIVE);// 注意不区分大消息
			final var matcher = pat.matcher(loopbody);
			while (matcher.find()) {
				final var placeholder = matcher.group();// 占位符 是由[ 循环变量/loopvar+"."+字段属性名/keyname]二字段来结构的
				final var keyname = matcher.group(1);// 对应到Record对象中的属性值,也就是占位符在"."之后的部分。
				this.foreachTerm.add(placeholder, keyname);
			} // while matcher
			this.type = TermType.FOREACH;// 修改类为foreach
			this.data = data;
			this.pattern = loopvar;
			return false;
		} else {// if(m.find)
			return true;
		} // if
	};

	/**
	 * 符号处理器
	 * 
	 * data 源数据
	 */
	public final Predicate<String> where_handler = (final String data) -> {
		final String WHERE_PATTERN = "where\\s+(.+)";// foreach 模式
		final Matcher mat = Pattern.compile(WHERE_PATTERN, Pattern.CASE_INSENSITIVE).matcher(data);
		if (mat.find()) {
			this.data = data;
			this.type = TermType.WHERE;
			this.pattern = WHERE_PATTERN;
			// System.out.println(mat.group(1));
			final var m = Pattern.compile("([^#]+)\\s*#\\s*([a-z]+)([^a-z_]+)\\s*").matcher(mat.group(1));

			this.whereTerm = new WhereTerm();
			this.whereTerm.setData(data);
			while (m.find()) {
				this.whereTerm.add(m.group(1), m.group(2), m.group(3));
			}

			return false; // 不再进行处理
		} else
			return true;
	};

	/**
	 * 符号项目
	 * 
	 * @return 符号
	 */
	public String getSymbol() {
		return this.getType() == TermType.SYMBOL ? data : null;
	}

	/**
	 * 符号项目
	 * 
	 * @return 项类型
	 */
	public Term.TermType getType() {
		return type;
	}

	/**
	 * foreach 字符串
	 * 
	 * @param map 符号值列表
	 * @return foreach的字符串
	 */
	public String toForeachString(final Map<String, Object> map) {
		return this.foreachTerm != null ? this.foreachTerm.toString(map) : null;
	}

	/**
	 * where 字符串
	 * 
	 * @param map 符号值列表
	 * @return foreach的字符串
	 */
	public String toWhereString(Map<String, Object> map) {
		return this.whereTerm != null ? this.whereTerm.toString(map) : null;
	}

	/**
	 * 字符串
	 */
	public String toString() {
		return this.data;
	}

	/**
	 * fill_template 的别名<br>
	 * Term.FT("$0+$1=$2",1,2,3) 替换后的结果为 1+2=3
	 * 
	 * @param template 模版字符串，占位符$0,$1,$2,...
	 * @param tt       模版参数序列
	 * @return template 被模版参数替换后的字符串
	 */
	public static String FT(final String template, final Object... tt) {
		final var rec = IRecord.A2R(tt).aoks2rec(i -> "$" + i); // 修改键名
		return fill_template(template, rec);
	}

	/**
	 * fill_template 的别名<br>
	 * 把template中的占位符用rec中对应名称key的值value给予替换,默认 非null值会用 单引号'括起来，<br>
	 * 对于使用$开头/结尾的占位符,比如$name或name$等不采用单引号括起来。<br>
	 * 还有对于 数值类型的值value不论占位符/key是否以$开头或结尾都不会用单引号括起来 。<br>
	 * 例如 : FT("insert into tblname$ (name,sex) values
	 * (#name,#sex)",REC("tblname$","user","#name","张三","#sex","男")) <br>
	 * 返回: insert into user (name,sex) values ('张三','男') <br>
	 * 
	 * @param template           模版字符串
	 * @param placeholder2values 关键词列表:占位符/key以及与之对应的值value集合
	 * @return 把template中的占位符/key用placeholder2values中的值value给予替换
	 */
	public static String FT(final String template, final IRecord placeholder2values) {
		return fill_template(template, placeholder2values);
	}

	/**
	 * 把template中的占位符用rec中对应名称key的值value给予替换,默认 非null值会用 单引号'括起来，<br>
	 * 对于使用$开头/结尾的占位符,比如$name或name$等不采用单引号括起来,<br>
	 * 还有对于 数值类型的值value不论占位符/key是否以$开头或结尾都不会用单引号括起来 。<br>
	 * 例如 : fill_template("insert into tblname$ (name,sex) values
	 * (#name,#sex)",REC("tblname$","user","#name","张三","#sex","男")) <br>
	 * 返回: insert into user (name,sex) values ('张三','男') <br>
	 * 
	 * @param template           模版字符串
	 * @param placeholder2values 关键词列表:占位符/key以及与之对应的值value集合
	 * @return 把template中的占位符/key用placeholder2values中的值value给予替换
	 */
	public static String fill_template(final String template, final IRecord placeholder2values) {
		final var len = template.length(); // 模版的长度
		final StringBuilder buffer = new StringBuilder();// 工作缓存
		final var keys = placeholder2values.keys().stream().sorted((a, b) -> -(a.length() - b.length())) // 按照从长到短的顺序进行排序。以保证keys的匹配算法采用的是贪婪算法。
				.collect(Collectors.toList());// 键名 也就是template中的占位符号。
		final var firstCharMap = keys.stream().collect(Collectors.groupingBy(key -> key.charAt(0)));// keys的首字符集合，这是为了加快
																									// 读取的步进速度
		int i = 0;// 当前读取的模版字符位置,从0开始。

		while (i < len) {// 从前向后[0,len)的依次读取模板字符串的各个字符
			// 注意:substring(0,0) 或是 substring(x,x) 返回的是一个长度为0的字符串 "",
			// 特别是,当 x大于等于字符串length会抛异常:StringIndexOutOfBoundsException
			final var line = template.substring(0, i);// 业已读过的模版字符串内容[0,i),当前所在位置为i等于line.length
			String placeholder = null;// 占位符号 的内容
			final var kk = firstCharMap.get(template.charAt(i));// 以 i为首字符开头的keys
			if (kk != null) {// 使用firstCharMap加速步进速度读取模版字符串
				for (final var key : kk) {// 寻找 可以被替换的key
					final var endIndex = line.length() + key.length(); // 终点索引 exclusive
					final var b = endIndex > len // 是否匹配到placeholder
							? false // 拼装的组合串(line+key) 超长（超过模板串的长度)
							: template.substring(line.length(), endIndex).equals(key); // 当前位置之后刚好存在一个key模样的字符串
					if (b) { // 定位到一个完整的占位符：长度合适（endIndex<=len) && 内容匹配 equals
						placeholder = key; // 提取key作为占位符
						break; // 确定了占位符 ,跳出本次i位置的kk循环。
					} // if b 定位到一个完整的占位符
				} // for key:kk
			} // if kk!=null

			if (placeholder != null) {// 已经发现了占位符号，用rec中的值给予替换
				boolean isnumberic = false;// 默认不是数字格式
				final var value = placeholder2values.get(placeholder); // 提取占位符内容
				if (placeholder.startsWith("$") || placeholder.endsWith("$"))
					isnumberic = true; // value 是否是 强制 使用 数字格式，即用$作为前/后缀。
				if (value instanceof Number)
					isnumberic = true; // 如果值是数字类型就一定会不加引号的。即采用数字格式

				buffer.append((isnumberic || value == null) ? value + "" : "'" + value + "'"); // 为字符串添加引号，数字格式或是null不加引号
				i += placeholder.length();// 步进placeholder的长度，跳着前进
			} else {// 发现了占位符号则附加当前的位置的。字符
				buffer.append(template.charAt(i));
				i++;// 后移一步
			} // if placeholder
		} // while

		return buffer.toString(); // 返回替换后的结果值
	}

	/**
	 * 清理line <br>
	 * 把 含有%符号的line 如 :<br>
	 * update t_student set %'id'=437, %'name'='张三', %'sex'='男',%'address'='上海市徐汇区'
	 * where code=234 <br>
	 * 转换成 <br>
	 * update t_student set id=437, name='张三', sex='男', address='上海市徐汇区' where
	 * code=234 <br>
	 * 
	 * @param final_line 待处理的line
	 * @return 清理后的line
	 */
	public static String percent_tidyline(final String final_line) {
		final var pattern = "%'([^']+)'";// 对%号的标记进行去除单引号
		var line = final_line;// 行数据。
		var matcher = Pattern.compile(pattern).matcher(line); // 匹配器
		while (matcher.find()) {
			final var grp = matcher.group(1);// 提取括号内的内容
			line = matcher.replaceFirst(grp);// 去除掉单引号'构成的括号('...')。
			matcher = Pattern.compile(pattern).matcher(line); // 重定义匹配器
		} // while
		return line; // 返回结果。
	}

	public enum TermType {
		SYMBOL, FOREACH, WHERE
	}// 语句项

	/**
	 * 条件表达式
	 * 
	 * @author gbench
	 *
	 */
	public static class WhereTerm {

		/**
		 * 增加一个项目
		 * 
		 * @param key     键名
		 * @param value   键值
		 * @param postfix 后缀,即剩余的其他部分
		 */
		void add(final String key, final String value, final String postfix) {
			tuples.add(new Tuple3<>(key, value, postfix));
		}

		/**
		 * SQL语句格式化输出
		 * 
		 * @param map 参数列表 key-&gt;value
		 * @return 字符换
		 */
		public String toString(final Map<String, Object> map) {
			final StringBuffer buffer = new StringBuffer();
			final Stream<Tuple3<String, String, String>> stream = this.tuples.stream();
			// System.out.println(this.data);
			final Function<Object, List<Tuple2<String, Object>>> variables = (oo) -> { // 提取环境变量
				final List<Tuple2<String, Object>> vars = new LinkedList<>();
				if (oo != null) {
					if (oo instanceof IRecord)
						oo = ((IRecord) oo).toMap(); // 把 IRecord 对象转换成Map结构,注意此处 不能与线面的if组合成elseif

					if (oo instanceof Collection) { // 不要写成elseif
						@SuppressWarnings("unchecked")
						final Collection<Object> coll = (Collection<Object>) oo;
						coll.forEach(o -> {
							if (o instanceof String)
								vars.add(new Tuple2<>(o + "", o));
						});
					} else if (oo.getClass().isArray()) {
						assert oo instanceof Object[];
						Object[] aa = (Object[]) oo;
						for (Object a : aa)
							vars.add(new Tuple2<>(a + "", a));
					} else if (oo instanceof Map) {
						final Map<?, ?> aa = (Map<?, ?>) oo;
						vars.addAll(aa.entrySet().stream()
								.map(e -> new Tuple2<String, Object>(e.getKey() + "", e.getValue()))
								.collect(Collectors.toList()));
					} else {
						vars.add(new Tuple2<>(oo + "", oo));
					} // if
				} // if oo
				return vars;
			};

			stream.forEach(tuple -> {
				String line = "";
				Object oo = map.get(tuple._2());// 获取字段并展开字段项目
				String func_name = ""; // 函数名称
				String prefix = ""; // 前缀字符串
				Matcher mat = Pattern.compile("([a-z_]+)\\s*$").matcher(tuple._1());
				if (!mat.find())
					return;
				func_name = mat.group(1);// 获得关键词：函数名
				prefix = tuple._1().substring(0, tuple._1().lastIndexOf(func_name)); // 把函数名之前的部分作为 前缀字符串
				if ("and_null".equalsIgnoreCase(func_name)) {
					line = Jdbcs.JOIN(Jdbcs.MAP(variables.apply(oo), o -> o._1() + " is null"), " AND ");
				} else if ("and_notnull".equalsIgnoreCase(func_name)) {
					line = Jdbcs.JOIN(Jdbcs.MAP(variables.apply(oo), o -> o._1() + " is not null"), " AND ");
				} else if ("or_notnull".equalsIgnoreCase(func_name)) {
					line = Jdbcs.JOIN(Jdbcs.MAP(variables.apply(oo), o -> o._1() + " is not null"), " OR ");
				} else if ("and_in".equalsIgnoreCase(func_name)) {
					line = Jdbcs.JOIN(Jdbcs.MAP(variables.apply(oo), o -> o._1() + " not in " + tuple._2()), " AND ");
				} else if ("and_not_in".equalsIgnoreCase(func_name)) {
					line = Jdbcs.JOIN(Jdbcs.MAP(variables.apply(oo), o -> o._1() + " not in " + tuple._2()), " AND ");
				} else if ("and_equals".equalsIgnoreCase(func_name)) {
					line = Jdbcs.JOIN(
							Jdbcs.MAP(variables.apply(oo),
									o -> o._1().replaceAll("##", "") + " = "
											+ (((o._1() + "")).startsWith("##") ? o._2() : "'" + o._2() + "'")),
							" AND ");
				} // if

				buffer.append(prefix).append(line).append(tuple._3());
			});

			return buffer.length() <= 0 ? "" : "where " + buffer.toString();
		}

		/**
		 * 原来的数据文本
		 * 
		 * @return 数据文本
		 */
		public String getData() {
			return data;
		}

		/**
		 * 设置 数据文本
		 * 
		 * @param data 数据文本
		 * @return WhereTerm 对象本省方便实现 链式编程
		 */
		public Term.WhereTerm setData(String data) {
			this.data = data;
			return this;
		}

		private String data = null; // 数据文本
		private final List<Tuple3<String, String, String>> tuples = new LinkedList<>(); // 数据项目 {键名,键值,后缀}
	}

	/**
	 * Foreach Term的结构处理
	 * 
	 * @author gbench
	 */
	static class ForeachTerm {

		/**
		 * 构建foreach 结构
		 * 
		 * @param loopvar     循环变量
		 * @param entriesName 循环体容器
		 * @param loopbody    循环体
		 */
		ForeachTerm(String loopvar, String entriesName, String loopbody) {
			this.loopvar = loopvar;
			this.entriesName = entriesName;
			this.loopbody = loopbody;
		}

		/**
		 * 添加一个占位符并指定与其对应的键值
		 * 
		 * @param placeholder 占位符号
		 * @param keyname     字段的键名
		 */
		public void add(String placeholder, String keyname) {
			kvs.add(new KVPair<>(placeholder, keyname));
		}

		/**
		 * 获得循环变量
		 * 
		 * @return 循环变量
		 */
		public String getLoopvar() {
			return loopvar;
		}

		/**
		 * 设置循环变量（占位符）
		 * 
		 * @param loopvar 设置循环变量（占位符）
		 */
		public void setLoopvar(String loopvar) {
			this.loopvar = loopvar;
		}

		/**
		 * 获得容器对象的名称：用于从 映射对象（map) 中提取循环变量的数据集合
		 * 
		 * @return 获得容器对象的名称
		 */
		public String getEntriesName() {
			return entriesName;
		}

		/**
		 * 设置容器对象的名称：用于从 映射对象（map) 中提取循环变量的数据集合
		 * 
		 * @param entriesName 容器对象的名称
		 */
		public void setEntriesName(String entriesName) {
			this.entriesName = entriesName;
		}

		/**
		 * 获得键值名称
		 * 
		 * @return 占位符与容器元素之间的对应名称。 （占位符,容器对象中的IRecord的键名）
		 */
		public List<KVPair<String, String>> getKvs() {
			return kvs;
		}

		/**
		 * 设置键值对儿。
		 * 
		 * @param kvs 占位符与容器元素之间的对应名称。 （占位符,容器对象中的IRecord的键名）
		 */
		public void setKvs(List<KVPair<String, String>> kvs) {
			this.kvs = kvs;
		}

		/**
		 * 注意 map 值中的字符需要需要先转移化后传入,如果 值的字符串标记 仅仅会 在收尾 增加一个 单引号"'"+v+"'".<br>
		 * 对于 以$字符结尾的key比如:amount$,值被视为数值类型，不予添加引号。<br>
		 * 
		 * @param context 映射对象:变量名与变量值的映射集合,其中包含一个 entriesName的缩影定义的一个KVP,用于代表循环变量。
		 * @return 使用map对模版字符串进行替换
		 */
		@SuppressWarnings("unchecked")
		public String toString(final Map<String, Object> context) {

			List<IRecord> recordEntries = null;
			final String foreachExpr = "foreach " + this.loopvar + " in " + " " + this.entriesName + " "
					+ this.loopbody;// foreach 的源代码表达式
			final var entriesName = this.getEntriesName();
			try {
				final Object entriesValue = context.get(entriesName);// 提取数据集合
				if (entriesValue == null) {
					return foreachExpr;
				}
				final Class<?> entriesCls = entriesValue.getClass();
				if (entriesValue instanceof Collection || entriesCls.isArray()) {
					final Collection<Object> entries = entriesCls.isArray() //
							? Arrays.asList((Object[]) entriesValue)
							: (Collection<Object>) entriesValue;
					if (entries.size() > 0) {// 集合类非空
						// 是否存在非IRecord元素
						final var opt = entries.stream().filter(entry -> !(entry instanceof IRecord)).findAny();
						if (opt.isPresent()) {// 尝试给予人工修复
							// System.out.println("List中含有非IRecord对象"+oo);
							final var x = entries.stream().map(entry -> {// 在kvs增加占位符placeholder->键名keyname之间的对应关系
								// 特别注意对于非IRecord数据placeholder与keyname一样都等于循环变量
								final var placeholder = loopvar; // 占位符
								final var key = loopvar; // 值key
								kvs.add(new KVPair<>(placeholder, key));//
								if (entry instanceof IRecord) {
									return REC(key, entry);
								} else {
									// 转为IRecord对象，仅有一个以循环变量名loopvar 作为键名 key 的IRecord元素
									return REC(key, entry);// 这里的loopvar代表 值key
								} // if
							}).collect(Collectors.toList());
							context.put(entriesName, x);// put
						} // 尝试给予人工修复
					} else {
						System.out.println(entriesName + " 为 空list");
						return foreachExpr;
					} // if
				} // if
				recordEntries = REC(context).lla(entriesName, IRecord.class);// 使用IRecord提取列表数据,可以有智能转换功能
			} catch (Exception e) {
				e.printStackTrace();
			}

			if (recordEntries == null)
				return foreachExpr; // 直接返回foreach表达式

			final var kvsrec = IRecord.KVS2REC(kvs);// placeholder --> 值key 的映射。

			// 返回结果
			final var line = recordEntries.stream().map(rec -> { // 利用循环填充 loopbody 使之形成 实例化 sql语句。
				final var placeholder2values = kvsrec.aov2rec((Function<String, Object>) rec::get);// 占位符与其值的对应关系。
				final var b = false; // 模版填充的方法模式选择
				if (b) { // fill_template 模式
					return Term.fill_template(loopbody, placeholder2values); // 较快的算法。
				} else { // 自定义模式,会根据值类型进行数据格式化选择
					final var tokens = Jdbcs.tokenize(loopbody, placeholder2values.keys()); // 分词
					return tokens.stream().map(token -> { // 关键字(占位符)的值替换
						final Function<Object, Object> formatter = v -> { // 值类型格式化
							if (token.str("name").endsWith("$") || v instanceof Number) { // 数字类型的 值
								return v; // 数字类型的值
							} else { // 提取数字类型的值
								return Optional.ofNullable(v).map(SQL::quoteString).orElse(null);
							} // if
						}; // 值类型格式化
						return token.bool("flag") // 是否为关键字(占位符)
								? placeholder2values.get(token.str("name"), formatter) + "" // 替换关键词(占位符)为具体的值
								: token.str("name"); // 模版字符
					}).collect(Collectors.joining()); // 模版的填充
				} // if
			}).collect(Collectors.joining(", ")); // recordEntries 返回结果

			return percent_tidyline(line);// 对百分号进行转义

		}// toString

		/**
		 * 一个 foreach 结构是这样的形式： foreach loopvar in container loopbody 其中 在 loopody 会出现
		 * placeholder,placeholder 一般是 loopvar+"."+keyname
		 * 但这并不是一定的。比如对于非IRecord的集合数据，loopvar可以干脆就是keyname
		 * keyname对应到具体的字段名称。这样我们就可以根据字段名称给予获取具体的值了。 placeholder 用于在替换循环体中的数值信息。
		 *
		 * 写几个例子: foreach u in user (user.name,user.password):
		 * ---------------------------------------------- placeholder keyname user.name
		 * path user.password password kvs:user.name->path,user.password->password
		 *
		 * foreach u in user u: -------------------------------------------- placeholder
		 * keyname u u kvs:u->u
		 */
		private String loopvar;// 循环变量名
		private String entriesName;// 循环容器的名称，也就一个List的对象集合(一般为IRecord对象)
		private final String loopbody;// 循环体
		private List<KVPair<String, String>> kvs = new LinkedList<>(); // e.name->name : placeholder 与
																		// 值(IRecord)中的key的对应。
	} // ForeachTerm

	/**
	 * 返回模式
	 * 
	 * @return 返回模式
	 */
	public String getPattern() {
		return pattern;
	}

	String data;// 字段项目的字符串序列
	private Term.TermType type; // 字段项目类型,简单符号 还是foreach结构
	private Term.ForeachTerm foreachTerm = null;// forEach 字段项目
	private Term.WhereTerm whereTerm = null;
	private String pattern = null;// 模式

	public final static String TERM_PATTERN = "\\$?\\s*\\{([^\\{\\}]+)\\}";
}