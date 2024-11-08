package gbench.sandbox.data.pignatoro.fmv;

import static gbench.util.io.Output.println;
import static gbench.util.lisp.Lisp.CONS;

import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import gbench.util.array.INdarray;
import gbench.util.lisp.DFrame;
import gbench.util.data.xls.SimpleExcel;
import gbench.util.io.Output;
import gbench.util.lisp.IRecord;

/**
 * 利润表的数据核算
 */
public class IncomeStmtTest {

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
				.map(k -> k.replaceAll("\s+%s$".formatted(growth_suffix), "")) // 去除掉指标名称尾部的Margin后缀,以获取到该比率指标的基础数据数据名
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

	private final String datahome = "F:/slicef/ws/gitws/malonylcoa/src/test/java/gbench/sandbox/data/pignatoro/files/%s";

}
