package gbench.sandbox.data.pignatoro.fmv;

import static gbench.global.Globals.WS_HOME;
import static gbench.util.io.Output.println;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.poi.ss.usermodel.IndexedColors;
import org.junit.jupiter.api.Test;

import gbench.util.data.xls.SimpleExcel;

/**
 * 文件书写测试
 */
public class WriteFileTest2 {

	/**
	 * 文件书写测试
	 */
	@Test
	public void foo() {
		final var file = outhome.formatted("foo.xlsx");

		try (final var excel = SimpleExcel.of(file)) {
			final var rname = "%s!F1:B10".formatted("foo");
			final var aa = excel.select(rname).poutline(IndexedColors.BLACK);
			println(aa.segment(2, 5).background(IndexedColors.YELLOW));
			excel.save();
		}
		println("completed:%s".formatted(file));
	}

	/**
	 * 文件书写测试
	 */
	@Test
	public void bar() {
		final var file = outhome.formatted("bar.xlsx");

		try (final var excel = SimpleExcel.of(file)) {
			final var aa = excel.select("BAR!B1:F7");
			int i = 0;
			final var cs = new IndexedColors[] { IndexedColors.GREEN, IndexedColors.RED, IndexedColors.BLUE,
					IndexedColors.YELLOW, IndexedColors.CORAL };

			for (var s : aa.splits(true, 2, 4)) { // 相当于0,2,4,7
				s.set(i).background(cs[i]);
				i++;
			}

			excel.save();
		}
		println("completed:%s".formatted(file));
	}

	/**
	 * 数据读取测试
	 */
	@Test
	public void quz() {
		final var draftfile = outhome.formatted("wft2-quz.xlsx"); // 底稿文件
		final var stmtname = "INCOME_STATEMENT3!B14:E41"; // 利润表名位置
		final var exists_flag = Files.exists(Path.of(draftfile)); // 底稿是否已将存在
		final var dataexcel = SimpleExcel.of(exists_flag ? draftfile : datafile); // 数据源表

		try (final var draftexcel = SimpleExcel.of(draftfile)) { // 写出excel
			if (!exists_flag) { // 如果outfile不存在则从dataexcel中拷贝数据
				draftexcel.select("INCOME_STATEMENT3!B13") // 注意这里的的选取指定B13比stmtname的B14提前一行
						.writeLines(dataexcel.select(stmtname)); // 拷贝datafile数据数据到out
			} // if
			final var datadmx = dataexcel.dmx(stmtname); // 读取数据表-利润表
			final var draftdmx = draftexcel.dmx(stmtname); // 写入表

			println("DMX", datadmx.mutate(mx -> { // 数据处理
				final var ai = new AtomicInteger(0);
				mx.col(0).forEach(cell -> { // 提取第一列
					final var value = "*%s*".formatted(cell); // 首列加入提醒标记
					draftdmx.get(ai.getAndIncrement(), 0).setCellValue(value);
				});
				return mx;
			}));

			println("---------------------------------------------------------");

			// 变换数据内容 加入引号与数据类型
			println("DFM", dataexcel.select("INCOME_STATEMENT3!B14:E41").dfm(null, //
					e -> "'%s:%s'".formatted(e.getCellType(), e)));

			draftexcel.save(); // 底稿保存
		}
	}

	/**
	 * 数据源文件
	 */
	final String outhome = "E:/slicee/temp/malonylcoa/test/data/excel/%s"; // 输出文件路径
	final String outfile = outhome.formatted("wftest.xlsx"); // 输出文件
	final String fileshome = "%s/gitws/malonylcoa/src/test/java/gbench/sandbox/data/pignatoro/files".formatted(WS_HOME); // 数据文件目录
	final String datafile = "%s/%s".formatted(fileshome, "amazon-2021-10k.xls"); // 财务数据文件

}
