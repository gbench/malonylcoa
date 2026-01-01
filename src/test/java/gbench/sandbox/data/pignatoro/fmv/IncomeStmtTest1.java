package gbench.sandbox.data.pignatoro.fmv;

import static gbench.util.io.Output.println;
import static gbench.util.lisp.Lisp.CONS;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import gbench.util.array.INdarray;
import gbench.util.lisp.DFrame;
import gbench.util.data.xls.SimpleExcel;
import gbench.util.io.Output;
import gbench.util.lisp.IRecord;
import gbench.util.lisp.Lisp;

/**
 * 利润表的数据核算
 */
public class IncomeStmtTest1 {

	/**
	 * 典型的利润表分析
	 */
	@Test
	public void foo() {
		final var dataexcel = SimpleExcel.of(datahome.formatted("amazon-2021-10k.xls"));
		final var stmtaa = dataexcel.select("INCOME_STATEMENT3!B14:E41"); // 获取数据选区
		final var stmtdmx = stmtaa.skipRows(2).evalmx(); // 报表数据矩阵(计算矩阵)
		final var titlemx = stmtaa.rangeAa("B2:D2").evalmx(); // 表头数据计算矩阵（行）

		println(titlemx, titlemx.shape()); // 打印表头

		final var keyname = "item"; // 首项的键名
		final Map<String, IRecord> lines = stmtdmx.keys(titlemx.row(0, INdarray::nd) // 设置矩阵标题
				.fmap(e -> "%d".formatted(((Double) e).intValue())).prepend(keyname)) // 增加item项目
				.mutate(SimpleExcel::dmx2dfm).toMap(); // 转换成 数据矩阵转换成DFrame,并进行 键值 映射

		final Function<String[], IRecord> calculate_item = defination -> { // 财务指标计算
			final var item = defination[0].strip(); // 指标名:左边变量去
			final var expression = defination[1].strip(); // 指标计算表达式：右边表达式区域
			final var opattern = "\s+[-\\+\\*/]+\s+"; // 运算符的结构模式：注意前后各有空白\s,以便将连字符'-'与减号' - '进行区分
			final var terms = expression.split(opattern); // 通过算数运算符把运算项目分开
			final var matcher = Pattern.compile(opattern).matcher(expression); // 算符提取器
			final var ops = new CopyOnWriteArrayList<String>(); // 运算符收集器
			final var ai = new AtomicInteger(); // 算符位置索引（计算次序的序号）

			while (matcher.find()) { // 记录算符索引：计算位置
				ops.add(matcher.group());
			}

			return ops.size() < 1 // 没有发现算符
					? lines.computeIfAbsent(item, k -> Optional.ofNullable(lines.get(expression)) //
							.map(e -> e.duplicate().set(0, item)).orElse(null)) // 字段改名
					: Stream.of(terms).map(String::strip).map(lines::get) // 提取行项目并给予运算标示进行计算
							.reduce((a, b) -> switch (ops.get(ai.getAndIncrement()).strip()) { // 根据算符索引依次进行数据计算
							case "+" -> a.plus(b); // 加法
							case "-" -> a.subtract(b); // 减法
							case "*" -> a.multiply(b); // 乘法
							case "/" -> a.divide(b); // 除法
							default -> null; // 其他
							}) // reduce 规约计算
							.map(e -> e.set(0, item)) // 结果键值为key的数据记录
							.map(e -> lines.computeIfAbsent(item, k -> e)) // 数据写入缓存表lines
							.orElse(null); // 默认返回null
		};

		println("-".repeat(100)); // 打印分割行

		// 财务报表的相关概念的逻辑关系:变量定义式,注意:算术符号的'+','-','*','/'需要前后至少留有一个空格,否则会被视为连接符
		final var definations = """
					Total Net Sales = Net product sales + Net service sales
					Gross Profit = Total Net Sales - Cost of sales
					Gross Profit Margin = Gross Profit / Total Net Sales
					Operating expenses = Cost of sales + Fulfillment + Technology and content + Marketing + General and administrative + Other operating expense (income), net
					Operating income = Total Net Sales - Operating expenses
					Total non-operating income (expense) = Interest income + Interest expense + Other income (expense), net
					Income before income taxes = Operating income + Total non-operating income (expense)
					EBT = Income before income taxes
					Net income = Income before income taxes + Provision for income taxes + Equity-method investment activity, net of tax
					Basic earnings per share = Net income / Basic
					Diluted earnings per share = Net income / Diluted
				"""
				.strip().split("\n"); // 定义式
		final var growth_ratio_prefixes = """
				Total Net Sales
				Gross Profit
				Operating expenses
				Operating income
				Total non-operating income (expense)
				EBT
				Net income
				Basic earnings per share
				Diluted earnings per share
				""".strip().split("\n"); // 各种增长率指标的前缀名称

		// 指标定义与计算:提取定义式的左右半边(handside) 已进行 指标计算 与 结果输出，目前指支持 平摊形式的计算,即不带有括号 和 优先级的结构数据
		Stream.of(definations).map(e -> e.split("=")).map(calculate_item).forEach(Output::println);

		final var rb = lines.values().iterator().next().rb(); // IRecord Builder 表项构建器
		final var growth_suffix = "Growth"; // 增长率后缀
		final Function<String, IRecord> calculate_growth = key -> Optional.ofNullable(key) // 增长率指标计算器
				.map(k -> k.replaceFirst("\s+%s$".formatted(growth_suffix), "")) // 去除掉指标名称尾部的Growth后缀,以获取到该比率指标的基础数据数据名
				.map(lines::get).map(e -> e.filterNot(0).valueS() // 剔除首项键名列
						.collect(IRecord.slidingclc(IRecord.rb("previous,current")::get, 2, 1, true)) // 齐次的宽度2步长1的连续窗口滑动
						.map(entry -> entry.dbl("current") / entry.dbl("previous") - 1).toArray(Object[]::new)) // 计算增长率:注意,需要返回对象数组以保证可以CONS
				.map(e -> CONS(key, CONS(null, e))).map(rb::get).orElse(null); // item,首项的增长率为null

		println("-".repeat(100)); // 打印分割行

		// 计算增长率指标
		Stream.of(growth_ratio_prefixes)
				.map(key -> lines.computeIfAbsent("%s %s".formatted(key, growth_suffix), calculate_growth))
				.forEach(Output::println);

		// 分析结果
		final var reportdfm = lines.values().stream().collect(DFrame.dfmclc);

		// 结果输出
		println("%s\n%s".formatted("-".repeat(100), reportdfm));
	}

	/**
	 * 简单四则混合运算的的实现
	 */
	@Test
	public void bar() {
		/**
		 * 扁平计算：calculate_flattened
		 */
		final Function<Map<String, IRecord>, Function<String[], IRecord>> calculate_flattened = lines -> defination -> { // 扁平计算
			final var item = defination[0].strip(); // 指标名:左边变量区域
			final var expression = defination[1].strip(); // 指标计算表达式：右边表达式区域
			final var opattern = "\s+[-\\+\\*/]+\s+"; // 运算符的结构模式：注意前后各有空白\s,以便将连字符'-'与减号' - '进行区分
			final var terms = expression.split(opattern); // 通过算数运算符把运算项目分开
			final var matcher = Pattern.compile(opattern).matcher(expression); // 算符提取器
			final var ops = new CopyOnWriteArrayList<String>(); // 运算符收集器
			final var ai = new AtomicInteger(); // 算符位置索引（计算次序的序号）

			while (matcher.find()) { // 记录算符索引：计算位置
				ops.add(matcher.group());
			}

			final var rbopt = lines.values().stream().findFirst().map(e -> e.rb()); // 行记录构建器
			final Function<String, Optional<IRecord>> readlineopt = key -> Optional.ofNullable(lines.get(key))
					.map(Optional::of).orElseGet(() -> rbopt.map(e -> e.get(key))); // 行记录读取函数opt版本
			final Function<String, IRecord> readline = key -> readlineopt.apply(key).orElse(null); // 行记录读取

			return ops.size() < 1 // 没有发现算符
					? lines.computeIfAbsent(item, k -> Optional.ofNullable(readline.apply(expression)) //
							.map(e -> e.duplicate().set(0, item)).orElse(null)) // 字段改名
					: Stream.of(terms).map(String::strip).map(readline) // 提取行项目并给予运算标示进行计算
							.reduce((a, b) -> switch (ops.get(ai.getAndIncrement()).strip()) { // 根据算符索引依次进行数据计算
							case "+" -> a.plus(b); // 加法
							case "-" -> a.subtract(b); // 减法
							case "*" -> a.multiply(b); // 乘法
							case "/" -> a.divide(b); // 除法
							default -> null; // 其他
							}) // reduce 规约计算
							.map(e -> e.set(0, item)) // 结果键值为key的数据记录
							.map(e -> lines.computeIfAbsent(item, k -> e)) // 数据写入缓存表lines
							.orElse(null); // 默认返回null
		}; // calculate_flattened

		/**
		 * 公式分解-函数<br>
		 * 输入formula: a + b * (c + d * (e - f)) - g * 4 <br>
		 * 分解（扁平）公式:analyzed_formula: a + #3 - #4 <br>
		 * 符号表: {#0=e - f, #1=d * #0, #2=c + #1, #3=b * #2, #4=g * 4, #5=a + #3 - #4}
		 * <br>
		 * 把字符串列行转换成分词结构 analyzer
		 */
		final Function<Map<String, String>, Function<String, String>> analyzer = // 把字符串列行转换成分词结构
				symboldefs -> Lisp.yCombinator(analyzer_f -> formula -> { // analyzer_f:analyzer自身引用, dataline:数据行
					final Predicate<String> predicate_mixed = line -> { // 是否是混合运算
						final var b0 = line.indexOf(" * ") >= 0 || line.indexOf(" / ") >= 0; // 是否含有乘除符号
						final var b1 = line.indexOf(" + ") >= 0 || line.indexOf(" - ") >= 0; // 是否含有加减符号
						return b0 && b1;
					}; // predicate_mixed 是否是混合运算
					final Supplier<String> keyer = () -> "#%s".formatted(symboldefs.size()); // 生成符号定义名：符号键名
					final Function<String, Function<String, String>> flattened_analyzer = //
							Lisp.yCombinator(flattened_analyzer_f -> pattern -> line -> { // flattened_analyzer_f:flatten_analyzer自身引用,line:数据行
								final var matcher = Pattern.compile(pattern).matcher(line);
								final boolean flag; // 是否包含有括号
								final var expression = (flag = matcher.find()) ? matcher.group(1) : line; // 定义表达式
								final BiFunction<String, String, String> flattened_handler = (flattened_line, key) -> { // flattened_line:不带括号的表达式,key:符号名
									if (predicate_mixed.test(flattened_line)) { // 混合运算，进行乘除法分析
										return flattened_analyzer_f.apply("(([^/*+\\-]+)\s+([*/]+)\s+([^/*+\\-]+))")
												.apply(flattened_line); // 乘除结构数据分析
									} else { // flattened_line表达式,直接写入符号key
										symboldefs.put(key, flattened_line.strip());
										return flattened_line; // 扁平行
									} // if
								}; // flattened_handler
								final var key = keyer.get(); // 符号定义名:键名
								if (flag) {// 包含有括号
									flattened_handler.apply(expression, key); // 写入括号子表达式，增加了一个符号key保证了exprkeyopt非空
									// flattened_handler会把expression的符号定义(表达式)写在符号表symboldefs的最后一项（key）,这里就是通过
									// reduce((a, b) -> b)逐一遍历（(a, b)->b表示只是简单遍历而不做别的）的迭代到最后一项（key)即expression的符号key
									final var exprkeyopt = symboldefs.entrySet().stream() //
											.reduce((a, b) -> b).map(Map.Entry::getKey); // expression的符号key
									return exprkeyopt.map(exprkey -> analyzer_f.apply(line.replaceFirst(pattern, //
											"\s%s\s".formatted(exprkey.strip())))// 需要注意key的两边的空格
									).get(); // symboldefs不可能为没有没有数据，所以这里就不使用orElse(null)来返回了
								} else {
									final String flattened_line = expression; // 不包含有括号的数据行，被称为平整过的行住是flattened的平整过还不一定是绝对的flat，需要进一步的flattened_handler
									return flattened_handler.apply(flattened_line, key);
								} // if
							}); // flattened_analyzer 括号分析
					final var flattened_line = flattened_analyzer.apply("\\(\s*([^()]+)\s*\\)").apply(formula); // 将括号变成扁平

					return flattened_line;
				}); // 分词器

		/**
		 * 公式计算器 formula_eval
		 */
		final Function<Map<String, IRecord>, Function<String, IRecord>> formula_eval = lines -> formula -> {
			final var symboldefs = new LinkedHashMap<String, String>(); // 符号定义表
			final var formula_analyzer = analyzer.apply(symboldefs); // 公式分析器
			final var analyzed_formula = formula_analyzer.apply(formula); // 分析公式结构
			if (debug) { // 打印调试信息-公式的的最终分析结果，语法树根节点样貌:极简式子
				println("\nformula:%s,\t analyzed_formula:%s".formatted(formula, analyzed_formula)); // 生成符号记录表
			}
			final var localscope = new LinkedHashMap<String, IRecord>(lines); // 制作一个本地拷贝本地变量集合
			final var calculate_symboldef = calculate_flattened.apply(localscope); // 符号计算
			final var result = symboldefs.entrySet().stream().map(e -> new String[] { e.getKey(), e.getValue() })
					.map(calculate_symboldef).reduce((a, b) -> b).orElse(null); // reduce保证可以计算到最后一个符号定义以获取最终的计算的结果
			if (debug) { // 打印调试信息-符号定义与本地计算环境信息
				println("symboldefs:%s,\t localscope:%s".formatted(symboldefs, localscope));
			}
			return result;
		};

		final var rb = IRecord.rb("item,x"); // 行项目构建器
		final Map<String, IRecord> lines = Stream.of( // 符号定义
				rb.get("a", 1), rb.get("b", 2), rb.get("c", 3), rb.get("d", 4), //
				rb.get("e", 5), rb.get("f", 6), rb.get("g", 7) //
		).collect(DFrame.dfmclc).toMap(); // 基础要素定义定义行

		// 公式计算
		Stream.of("""
				a + (b * (c + (d * (e - f))) - g)
				a + b + c
				""".split("[,\n]+")).peek(Output::println).map(formula_eval.apply(lines)).forEach(Output::println);

	}

	private static boolean debug = false; // 调试标志

	private final String datahome = "F:/slicef/ws/gitws/malonylcoa/src/test/java/gbench/sandbox/data/pignatoro/files/%s";

}
