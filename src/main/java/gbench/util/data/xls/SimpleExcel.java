package gbench.util.data.xls;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Array;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.openxml4j.util.ZipSecureFile;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.CellValue;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import gbench.util.io.FileSystem;
import gbench.util.lisp.DFrame;
import gbench.util.lisp.IRecord;
import gbench.util.data.xls.DataMatrix.Tuple2;
import gbench.util.type.Pack;
import gbench.util.type.Times;

/**
 * EXCEL 数据文件的处理与分析工具
 * 
 * @author xuqinghua
 *
 */
public class SimpleExcel implements AutoCloseable {

	/**
	 * 简单的EXCEL, try to load a excel file
	 * 
	 * @param path 文件路径
	 */
	public SimpleExcel(final String path) {
		this.load(path);
	}

	/**
	 * 简单的EXCEL, try to load a excel file
	 * 
	 * @param excelfile excel文件对象
	 */
	public SimpleExcel(final File excelfile) {
		this.load(excelfile, true);
	}

	/**
	 * 简单的EXCEL, try to load a excel file
	 * 
	 * @param inputStream 数据流
	 * @param extension   文件扩展名 xls,xlsx,或者 xlsm等等。
	 */
	public SimpleExcel(final InputStream inputStream, final String extension) {
		if (extension == null || !extension.trim().toLowerCase().startsWith("xls")) {
			println("扩展名不正确，为null 或是 没有以xls开头");
		}
		this.loadWithExtension(inputStream, extension);
	}

	/**
	 * 加载一个EXCEL的文件
	 * 
	 * @param path excel文件路径
	 * @return SimpleExcel 对象本身 以实现链式编程
	 */
	public SimpleExcel load(final String path) {
		return load(path, true);
	}

	/**
	 * 加载一个EXCEL的文件
	 * 
	 * @param path     excel文件的绝对路径
	 * @param readonly 是否以只读模式加载
	 * @return SimpleExcel 对象本身 以实现链式编程
	 */
	public SimpleExcel load(final String path, final boolean readonly) {
		final File file = new File(path);
		return this.load(file, readonly);
	}

	/**
	 * 不支持读写的并存
	 * 
	 * @param excelfile excel文件对象
	 * @param readonly  是否只读
	 * @return SimpleExcel 对象本身 以实现链式编程
	 */
	public SimpleExcel load(final File excelfile, final boolean readonly) {
		try {
			if (this.workbook != null) {
				this.workbook.close();
			}

			final String fullpath = excelfile.getAbsolutePath();
			final String ext = FileSystem.extensionpicker(fullpath).toLowerCase();

			try {
				if (!ext.equals("xlsx") && !ext.equals("xls"))
					throw new Exception("数据格式错误,文件需要xls 或者 xlsx");
			} catch (Exception e) {
				e.printStackTrace();
				return this;
			} // try

			ZipSecureFile.setMinInflateRatio(0); // 处理zipbomb问题
			if (excelfile.exists() && readonly) {// 文件只读
				workbook = ext.equals("xlsx") ? new XSSFWorkbook(fullpath)
						: new HSSFWorkbook(new FileInputStream(excelfile));
			} else {
				workbook = ext.equals("xlsx") ? new XSSFWorkbook() : new HSSFWorkbook();
			} // if

			FORMULA_EVALUATOR = workbook.getCreationHelper().createFormulaEvaluator();
			this.xlsfile = excelfile;
		} catch (IOException e) {
			e.printStackTrace();
		} // try

		return this;
	}

	/**
	 * 带扩展名的 资源加载
	 * 
	 * @param inputStream 文件对象
	 * @param ext         扩展名,excel文件类型扩展名,如果为null，默认为xlsx
	 * @param readonly    是否只读
	 * @return SimpleExcel 对象本身 以实现链式编程
	 */
	public SimpleExcel loadWithExtension(final InputStream inputStream, final String ext, final boolean readonly) {
		try {
			final String final_ext = ext == null ? "xlsx" : ext;
			if (this.workbook != null)
				this.workbook.close();

			if (readonly) {// 文件只读
				workbook = final_ext.equals("xlsx") ? new XSSFWorkbook(inputStream) : new HSSFWorkbook(inputStream);
			} else {
				workbook = final_ext.equals("xlsx") ? new XSSFWorkbook() : new HSSFWorkbook();
			}
			FORMULA_EVALUATOR = workbook.getCreationHelper().createFormulaEvaluator();
			this.xlsfile = File.createTempFile("xlsfile-" + System.nanoTime(), ext);
		} catch (IOException e) {
			e.printStackTrace();
		} // try
		return this;
	}

	/**
	 * 带扩展名的 资源加载:只读加载
	 * 
	 * @param inputStream 文件对象
	 * @param ext         扩展名,excel文件类型扩展名,如果为null，默认为xlsx
	 * @return SimpleExcel 对象本身 以实现链式编程
	 */
	public SimpleExcel loadWithExtension(final InputStream inputStream, final String ext) {
		return this.loadWithExtension(inputStream, ext, true);
	}

	/**
	 * 采用 区域遍历法在 [firstRowIndex,maxSize)的范围内遍历获得出有包含有有效数据的单元格，其地址索引为最小（最上，最左） 左上
	 * 单元的位置，可能为空.left top .<br>
	 * 
	 * @param sht           表单名称
	 * @param firstRowIndex 首行索引；从0开始，包含
	 * @param maxSize       遍历行索引的上限。不包含
	 * @return 左上顶点的为cell单元格地址
	 */
	public Tuple2<Integer, Integer> lt(final Sheet sht, final Integer firstRowIndex, final Integer maxSize) {
		if (sht == null) {
			return null;
		}

		int c1 = Integer.MAX_VALUE;
		int c2 = Integer.MAX_VALUE;

		for (int i = firstRowIndex; i < maxSize; i++) {
			final Row row = sht.getRow(i);
			if (row == null) {
				continue;
			}

			if (row.getPhysicalNumberOfCells() < 1) {
				continue;
			}

			if (c1 == Integer.MAX_VALUE) {
				c1 = i;
			}

			if (row.getFirstCellNum() < c2) {
				c2 = row.getFirstCellNum();
			}
		} // for

		if (c1 == Integer.MAX_VALUE || c2 == Integer.MAX_VALUE) {
			return null;
		} // if

		// println("lt:"+c1+","+c2);
		return new Tuple2<>(c1, c2);
	}

	/**
	 * 采用 区域遍历法在 [firstRowIndex,maxSize)的范围内遍历获得出有包含有有效数据的单元格，其地址索引为最大（最上，最左）<br>
	 * 右下 right botttom<br>
	 * 
	 * @param sht           excel的表单 sheet对象
	 * @param firstRowIndex 首行索引；从0开始，包含
	 * @param maxSize       遍历行索引的上限。不包含
	 * @return 右下顶点的为cell单元格地址
	 */
	public Tuple2<Integer, Integer> rb(final Sheet sht, final Integer firstRowIndex, final Integer maxSize) {
		int c1 = Integer.MIN_VALUE;
		int c2 = Integer.MIN_VALUE;

		if (sht == null) {
			return null;
		}

		for (int i = firstRowIndex; i < maxSize; i++) {
			final Row row = sht.getRow(i);
			if (row == null)
				continue;
			if (row.getPhysicalNumberOfCells() < 1)
				continue;
			c1 = i;
			if (row.getLastCellNum() > c2)
				c2 = row.getLastCellNum() - 1;
		} // for

		if (c1 == Integer.MIN_VALUE) {
			return null;
		}

		return new Tuple2<>(c1, c2);
	}

	/**
	 * 读取数据内容
	 * 
	 * @param nameline Excel 的 Range 名称,比如 'Sheet1!A1:B2'
	 * @return 数据内容矩阵
	 */
	public DataMatrix<Cell> dmx(final String nameline) {
		return this.select(nameline).dmx();
	}

	/**
	 * 读取数据内容
	 * 
	 * @param nameline Excel 的 Range 名称,比如 'Sheet1!A1:B2'
	 * @return 数据内容矩阵
	 */
	public DFrame dfm(final String nameline) {
		return this.select(nameline).dfm();
	}

	/**
	 * 自动检测sheetname中的数据区域<br>
	 * 
	 * @param sheetname 表单sheet名称
	 * @return 可用的 sheetname 页面的数据区域
	 */
	public DataMatrix<Object> autoDetectDMX(final String sheetname) {
		final var data = this.autoDetectAA(sheetname).rowS()
				.map(row -> row.stream(this::evaluate).toArray(Object[]::new)).toArray(Object[][]::new);
		return new DataMatrix<Object>(data);
	}

	/**
	 * 自动检测sheetname中的数据区域<br>
	 * 
	 * @param sheetname 表单sheet名称
	 * @return 可用的 sheetname 页面的数据区域
	 */
	public DataMatrix<Object> autoDetectDMX(final Sheet sht) {
		return this.autoDetectDMX(sht.getSheetName());
	}

	/**
	 * 自动检测sheetname中的数据区域<br>
	 * 
	 * @param sheetname 表单sheet名称
	 * @return 可用的 sheetname 页面的数据区域
	 */
	public AffectedArea autoDetectAA(final String sheetname) {
		return this.select("%s!%s".formatted(sheetname, this.autoDetectRN(sheetname)));
	}

	/**
	 * 自动检测sheetname中的数据区域<br>
	 * 
	 * @param sht 表单对象
	 * @return 可用的 sheetname 页面的数据区域
	 */
	public AffectedArea autoDetectAA(final Sheet sht) {
		return this.autoDetectAA(sht.getSheetName());
	}

	/**
	 * 自动检测sheetname中的数据区域<br>
	 * 
	 * @param sheetname 表单sheet名称
	 * @return 可用的 sheetname 页面的数据区域
	 */
	public String autoDetectRN(final String sheetname) {
		final var i = this.shtname2shtid(sheetname);
		return this.autoDetectRN(this.sheet(i), 0, MAX_SIZE);
	}

	/**
	 * 自动定位数据位置<br>
	 * 读取指定sht 的最大可用状态
	 * 
	 * @param sht 表单对象
	 * @return 可用的 sheetname 页面的数据区域
	 */
	public String autoDetectRN(final Sheet sht) {
		return this.autoDetectRN(sht, 0, MAX_SIZE);
	}

	/**
	 * 自动定位数据位置 <br>
	 * 读取指定sht 的最大可用状态
	 * 
	 * @param sht           表单对象
	 * @param firstRowIndex 首行索引从0开始
	 * @param maxSize       检索范围边界
	 * @return 数据矩阵
	 */
	public String autoDetectRN(final Sheet sht, final Integer firstRowIndex, final Integer maxSize) {
		final Tuple2<Integer, Integer> lt = this.lt(sht, firstRowIndex, maxSize);
		final Tuple2<Integer, Integer> rb = this.rb(sht, firstRowIndex, maxSize);
		final String rangeName = ltrb2name(lt, rb);// 转换成rangename

		return rangeName;
	}

	/**
	 * 自动定位数据位置 <br>
	 * 读取指定sht 的最大可用状态
	 * 
	 * @param sht           表单对象
	 * @param firstRowIndex 首行索引从0开始
	 * @param maxSize       检索范围边界
	 * @return 数据矩阵
	 */
	public StrMatrix autoDetect(final Sheet sht, final Integer firstRowIndex, final Integer maxSize) {
		final Tuple2<Integer, Integer> lt = this.lt(sht, firstRowIndex, maxSize);
		final Tuple2<Integer, Integer> rb = this.rb(sht, firstRowIndex, maxSize);
		final String rangeName = ltrb2name(lt, rb);// 转换成rangename

		// println(rangeName);

		return this.range(sht, DataMatrix.name2rdf(rangeName));
	}

	/**
	 * 自动定位数据位置<br>
	 * 读取指定sht 的最大可用状态
	 * 
	 * @param sht 表单对象
	 * @return 数据矩阵
	 */
	public StrMatrix autoDetect(final Sheet sht) {
		return this.autoDetect(sht, 0, MAX_SIZE);
	}

	/**
	 * 读取指定sht 的最大可用状态 <br>
	 * 默认数据首行为标题：
	 * 
	 * @param shtid   sheetid 编号从0开始
	 * @param maxSize 检索范围边界
	 * @return 可用的 sheetid 页面的数据区域
	 */
	public StrMatrix autoDetect(final int shtid, final Integer firstRowIndex, final Integer maxSize) {
		if (shtid >= this.sheets().size()) {
			return null;
		}

		return autoDetect(this.sheet(shtid), firstRowIndex, maxSize);
	}

	/**
	 * 读取指定sht 的最大可用状态<br>
	 * 默认数据首行为标题：<br>
	 * 
	 * @param sheetname     表单sheet名称
	 * @param firstRowIndex 首行索引号 从0开始
	 * @param maxSize       检索范围边界
	 * @return 可用的 sheetname 页面的数据区域
	 */
	public StrMatrix autoDetect(final String sheetname, final Integer firstRowIndex, final Integer maxSize) {
		final int shtid = this.shtname2shtid(sheetname);
		if (shtid < 0) {
			println(sheetname + " sheet,不存在！");
			return null;
		} // if
		return autoDetect(this.sheet(shtid), firstRowIndex, maxSize);
	}

	/**
	 * 读取指定sht 的最大可用状态 <br>
	 * 默认数据首行为标题：<br>
	 * 
	 * @param sheetname 表单sheet名称
	 * @param maxSize   检索范围边界
	 * @return 可用的 sheetname 页面的数据区域
	 */
	public StrMatrix autoDetect(final String sheetname, final Integer maxSize) {
		return this.autoDetect(sheetname, 0, maxSize);
	}

	/**
	 * 自动检测sheetname中的数据区域<br>
	 * 
	 * @param sheetname 表单sheet名称
	 * @return 可用的 sheetname 页面的数据区域
	 */
	public StrMatrix autoDetect(final String sheetname) {
		return this.autoDetect(sheetname, 0, MAX_SIZE);
	}

	/**
	 * 自动检测sheetname中的数据区域<br>
	 * 默认数据首行为标题：<br>
	 * 
	 * @param shtid   表单id 编号从0开始
	 * @param maxSize 检索范围边界
	 * @return 可用的 sheetid 页面的数据区域
	 */
	public StrMatrix autoDetect(final int shtid, final Integer maxSize) {
		return this.autoDetect(shtid, 0, maxSize);
	}

	/**
	 * 自动检测sheetname中的数据区域<br>
	 * 默认数据首行为标题：<br>
	 * 
	 * @param shtid 表单sheet编号从0开始
	 * @return 可用的 sheetid 页面的数据区域
	 */
	public StrMatrix autoDetect(final int shtid) {
		return this.autoDetect(shtid, 0, MAX_SIZE);
	}

	/**
	 * 从excel中读取数据矩阵<br>
	 * 
	 * @param <U>    目标矩阵的元素类型
	 * @param shtid  表单sheet编号从0开始
	 * @param mapper 值变换函数
	 * @return DataMatrix &lt;U&gt;
	 */
	public <U> DataMatrix<U> autoDetect(final int shtid, final Function<String, U> mapper) {
		return this.autoDetect(shtid, 0, MAX_SIZE).corece(mapper);
	}

	/**
	 * 从excel中读取数据矩阵<br>
	 * 
	 * @param <U>     目标矩阵的元素类型
	 * @param shtname sheet的名称
	 * @param mapper  值变换函数
	 * @return DataMatrix &lt;U&gt;
	 */
	public <U> DataMatrix<U> autoDetect(final String shtname, final Function<String, U> mapper) {
		return this.autoDetect(shtname, 0, MAX_SIZE).corece(mapper);
	}

	/**
	 * 按照行进行数据变换 <br>
	 * 自动定位数据位置<br>
	 * 读取指定sht 的最大可用状态
	 * 
	 * @param <U>    目标类型
	 * @param sht    表单对象
	 * @param mapper 值变换函数 row-&gt;u
	 * @return U类型的数据流
	 */
	public <U> Stream<U> autoDetectS(final Sheet sht, final Function<LinkedHashMap<String, String>, U> mapper) {
		return this.autoDetect(sht).rowS(mapper);
	}

	/**
	 * 按照行进行数据变换 <br>
	 * 自动定位数据位置<br>
	 * 读取指定sht 的最大可用状态
	 * 
	 * @param <U>    目标类型
	 * @param shtid  表单引号,从0开始
	 * @param mapper 值变换函数 row->u
	 * @return U类型的数据流
	 */
	public <U> Stream<U> autoDetectS(final int shtid, final Function<LinkedHashMap<String, String>, U> mapper) {
		return this.autoDetect(shtid).rowS(mapper);
	}

	/**
	 * 按照行进行数据变换 <br>
	 * 自动定位数据位置<br>
	 * 读取指定sht 的最大可用状态
	 * 
	 * @param <U>     目标类型
	 * @param shtname 表单名称
	 * @param mapper  值变换函数 row-&gt;u
	 * @return U类型的数据流
	 */
	public <U> Stream<U> autoDetectS(final String shtname, final Function<LinkedHashMap<String, String>, U> mapper) {
		return this.autoDetect(shtname).rowS(mapper);
	}

	/**
	 * 选择哪些列数据 <br>
	 * 这有点类似于 select mapper(keys) from name 这样的数据操作。 <br>
	 * 
	 * 对于有 name 进行标识的excel中的区域给予计算求求职 <br>
	 * 由于name标识的区域是一个 数据框，所以可以通过 在 keys 中指定列名的形式对于每个元素a[i,j]应用mapper 函数，进而得到一个新的矩阵
	 * <br>
	 * c1 c2 c3 :列名 ----> ci cj ck [i,j,k是 一个c1,c2,c3...的子集] <br>
	 * a11 a12 a13 :行数据 ----> b1i b1j b1k <br>
	 * a21 a22 a23 :行数据 ----> b2i b2j b2k <br>
	 * ... ... ... ----> ... ... ... <br>
	 * 
	 * @param <T>       元素类型
	 * @param sht       sheet 名
	 * @param rangeName 区域名称
	 * @param keys      表头名称
	 * @param mapper    数据变换操作: o-&gt;t 完成 aij-&gt;bij的变换
	 * @return 新数据矩阵
	 */
	public <T> DataMatrix<T> evaluate(final Sheet sht, final String rangeName, final List<String> keys,
			final Function<Object, T> mapper) {
		return this.evaluate(sht, DataMatrix.name2rdf(rangeName), keys, mapper);

	}

	/**
	 * 选择哪些列数据 <br>
	 * 这有点类似于 select mapper(keys) from name 这样的数据操作。 <br>
	 * 
	 * 对于有 name 进行标识的excel中的区域给予计算求求职 <br>
	 * 由于name标识的区域是一个 数据框，所以可以通过 在 keys 中指定列名的形式对于每个元素a[i,j]应用mapper 函数，进而得到一个新的矩阵
	 * <br>
	 * c1 c2 c3 :列名 ----> ci cj ck [i,j,k是 一个c1,c2,c3...的子集] <br>
	 * a11 a12 a13 :行数据 ----> b1i b1j b1k <br>
	 * a21 a22 a23 :行数据 ----> b2i b2j b2k <br>
	 * ... ... ... ----> ... ... ... <br>
	 * 
	 * @param <T>       元素类型
	 * @param sht       表单名
	 * @param rangeName 区域名称
	 * @param mapper    数据变换操作: o-&gt;t 完成 aij-&gt;bij的变换
	 * @return 新数据矩阵
	 */
	public <T> DataMatrix<T> evaluate(final Sheet sht, final String rangeName, final Function<Object, T> mapper) {
		return this.evaluate(sht, DataMatrix.name2rdf(rangeName), null, mapper);
	}

	/**
	 * 选择哪些列数据 <br>
	 * 这有点类似于 select mapper(keys) from name 这样的数据操作。 <br>
	 * 
	 * 对于有 name 进行标识的excel中的区域给予计算求求职 <br>
	 * 由于name标识的区域是一个 数据框，所以可以通过 在 keys 中指定列名的形式对于每个元素a[i,j]应用mapper 函数，进而得到一个新的矩阵
	 * <br>
	 * c1 c2 c3 :列名 ----> ci cj ck [i,j,k是 一个c1,c2,c3...的子集] <br>
	 * a11 a12 a13 :行数据 ----> b1i b1j b1k <br>
	 * a21 a22 a23 :行数据 ----> b2i b2j b2k <br>
	 * ... ... ... ----> ... ... ... <br>
	 * 
	 * @param <T>    元素类型
	 * @param name   区域全名称比如sheet2!A1:B100
	 * @param keys   列名序列,即选择那些列数据 这就点类似于 select mapper(keys) from name 这样的数据操作。
	 * @param mapper 数据变换操作: o-&gt;t 完成 aij-&gt;bij的变换
	 * @return 重新计算后的新的数据矩阵
	 */
	public <T> DataMatrix<T> evaluate(final String name, final List<String> keys, final Function<Object, T> mapper) {
		final String names[] = name.split("[!]+");// 多个！视为一个
		Sheet sht = this.sheet(0);
		String rangeName = name;

		// 默认为第一个sheet的区域名
		if (names.length >= 2) {
			sht = this.sheet(this.shtid(names[0]));// 获取sheetid
			rangeName = names[1];// 选区第二项目作为区域名称
		} // 选区第二项目作为区域名称

		return this.evaluate(sht, DataMatrix.name2rdf(rangeName), keys, mapper);
	}

	/**
	 * 选择哪些列数据 <br>
	 * 这有点类似于 select mapper(keys) from name 这样的数据操作。 <br>
	 * 
	 * 对于有 name 进行标识的excel中的区域给予计算求求职 <br>
	 * 由于name标识的区域是一个 数据框，所以可以通过 在 keys 中指定列名的形式对于每个元素a[i,j]应用mapper 函数，进而得到一个新的矩阵
	 * <br>
	 * c1 c2 c3 :列名 ----> ci cj ck [i,j,k是 一个c1,c2,c3...的子集] <br>
	 * a11 a12 a13 :行数据 ----> b1i b1j b1k <br>
	 * a21 a22 a23 :行数据 ----> b2i b2j b2k <br>
	 * ... ... ... ----> ... ... ... <br>
	 * 
	 * @param <T>    元素类型
	 * @param name   区域全名称比如sheet2!A1:B100
	 * @param mapper 数据变换操作: o-&gt;t 完成 aij-&gt;bij的变换
	 * @return 重新计算后的新的数据矩阵
	 */
	public <T> DataMatrix<T> evaluate(final String name, final Function<Object, T> mapper) {
		return this.evaluate(name, null, mapper);
	}

	/**
	 * 选择哪些列数据 <br>
	 * 这有点类似于 select mapper(keys) from name 这样的数据操作。 <br>
	 * 
	 * 对于有 name 进行标识的excel中的区域给予计算求求职 <br>
	 * 由于name标识的区域是一个 数据框，所以可以通过 在 keys 中指定列名的形式对于每个元素a[i,j]应用mapper 函数，进而得到一个新的矩阵
	 * <br>
	 * c1 c2 c3 :列名 ----> ci cj ck [i,j,k是 一个c1,c2,c3...的子集] <br>
	 * a11 a12 a13 :行数据 ----> b1i b1j b1k <br>
	 * a21 a22 a23 :行数据 ----> b2i b2j b2k <br>
	 * ... ... ... ----> ... ... ... <br>
	 * 
	 * @param <T>    元素类型
	 * @param sht    表单数据
	 * @param mapper 数据变换操作: o-&gt;t 完成 aij-&gt;bij的变换
	 * @param keys   null,表示数据中包含表头,第一行就是表头
	 * @return 新数据矩阵
	 */
	public <T> DataMatrix<T> evaluate(final Sheet sht, final RangeDef rangedef, final List<String> keys,
			final Function<Object, T> mapper) {
		return this.evaluate(sht, rangedef.x0(), rangedef.y0(), rangedef.x1(), rangedef.y1(), keys, mapper);

	}

	/**
	 * 选择哪些列数据 <br>
	 * 这有点类似于 select mapper(keys) from name 这样的数据操作。 <br>
	 * 
	 * 对于有 name 进行标识的excel中的区域给予计算求求职 <br>
	 * 由于name标识的区域是一个 数据框，所以可以通过 在 keys 中指定列名的形式对于每个元素a[i,j]应用mapper 函数，进而得到一个新的矩阵
	 * <br>
	 * c1 c2 c3 :列名 ----> ci cj ck [i,j,k是 一个c1,c2,c3...的子集] <br>
	 * a11 a12 a13 :行数据 ----> b1i b1j b1k <br>
	 * a21 a22 a23 :行数据 ----> b2i b2j b2k <br>
	 * ... ... ... ----> ... ... ... <br>
	 * 
	 * 行范围 从 _i0 开始,包含 _i1结束 <br>
	 * 行范围 从 _j0 开始,包含 _j1结束 <br>
	 * 
	 * @param <T>     数据矩阵的的元素类型
	 * @param sht     sht 表单数据
	 * @param _i0     行范围 开始位置,从 0开始,包含
	 * @param _j0     行范围 开始位置,从 0开始,包括
	 * @param _i1     列范围 结束位置,包含
	 * @param _j1     列范围 结束位置,包含
	 * @param _mapper 元素变换函数 即生成T类型的数元素。
	 * @return excel 数据矩阵
	 */
	@SuppressWarnings("unchecked")
	public <T> DataMatrix<T> evaluate(final Sheet sht, final int _i0, final int _j0, final int _i1, final int _j1,
			final List<String> _keys, final Function<Object, T> _mapper) {

		if (sht == null) {
			println("指定的sheet为空,无法获得表单数据");
			return null;
		}

		List<String> keys = _keys; // 表头对象
		final int i0 = Math.min(_i0, _i1);
		final int i1 = Math.max(_i0, _i1);
		final int j0 = Math.min(_j0, _j1);
		final int j1 = Math.max(_j0, _j1);
		final Function<Object, T> mapper = _mapper == null ? e -> (T) e : _mapper;
		final int offset = keys == null ? 1 : 0;// 数据从哪一行开始
		final Object[][] mm = new Object[(i1 - i0 + 1 - offset)][(j1 - j0 + 1)];

		if (mm.length <= 0 || mm[0] == null || mm[0].length <= 0) {
			println("数据矩阵的行数为空，没有数据！");
			return null;
		}

		final List<String> firstrow = new ArrayList<>(mm[0].length);
		final Set<Class<?>> classes = new HashSet<>();
		for (int i = i0; i <= i1 - offset; i++) { // 数据尾行需要去掉offset部分因为这些跳过了
			for (int j = j0; j <= j1; j++) {// 当keys==null的时候数据要偏移一行
				if (i == i0)
					firstrow.add(this.strval(sht, i, j));// 记录首行
				Cell c = null;// 单元格
				try {
					final Row row = sht.getRow(i + offset); // 数据行对象
					c = (row == null) ? null : row.getCell(j);// 获取产品单元格,注意含有行偏移值：这里是 i+offset
					final T value = mapper.apply(this.evaluate(c));// 跳过offset行;
					mm[i - i0][j - j0] = value;// 计算矩阵数值
					if (value != null)
						classes.add(value.getClass());
				} catch (Exception e) {
					e.printStackTrace();
					println("error on (" + i + "," + j + ")," + (sht.getRow(i) == null ? "行对象:'" + sht.getRow(i) + "为空"
							: "行对象或者cell单元格异常,请指定有效的EXCEL数据范围（或是EXCEL自行判断的数据范围有错误）！"));
				} // try
			} // for j
		} // for i

		if (keys == null || keys.size() < 1) {// 使用第一行作为表头
			final AtomicInteger ai = new AtomicInteger(0);
			keys = firstrow.stream().map(e -> {
				if (e == null || "null".equals(e) || e.matches("\\s*")) { // 自动生成表头
					return "_" + DataMatrix.index_to_excel_name(ai.get()); // 生成动态表头
				}
				ai.incrementAndGet();
				return e;
			}).collect(Collectors.toList());// 默认的第一行作表头
		} else {// 使用指定表头
			final String[] nn = keys.toArray(new String[0]);
			final int size = keys.size();
			keys = Stream.iterate(1, i -> i + 1).limit(mm[0].length)
					.map(e -> size >= e ? nn[e - 1] : ("_" + DataMatrix.index_to_excel_name(e)))
					.collect(Collectors.toList());
		}

		Class<?> cls = null;// 获取矩阵的数据分类
		if (classes.size() > 0) {
			cls = classes.iterator().next();
			if (classes.size() > 1) {
				println("warnnings:矩阵中出现不同类别:" + classes + ",取用类别:" + classes.iterator().next());
			} // if
		} // if

		final int m = mm.length; // 行数
		final int n = mm[0].length;// 表列宽度，列数
		final T[][] tt = (T[][]) Array.newInstance((Class<T>) cls, mm.length, mm[0].length); // 数据矩阵

		for (int i = 0; i < m; i++) {
			for (int j = 0; j < n; j++) {
				try {
					tt[i][j] = (T) mm[i][j];
				} catch (Exception e) {
					e.printStackTrace();
				} // try
			} // for j
		} // for i

		return new DataMatrix<>(tt, keys);
	}

	/**
	 * 格式化输出结构
	 * 
	 * @param cell excel表单sheet单元格对象
	 * @return 字符串输出
	 */
	public Object evaluate(final Cell cell) {
		Object value = null;
		if (cell == null) {
			return null;
		}

		final BiFunction<String, Number, String> repeat = (s, n) -> {
			return Stream.iterate(0, i -> i + 1).limit(n.intValue()).map(e -> s).collect(Collectors.joining());
		};

		final Function<Cell, String> strcell = c -> { // 元素值的字符串
			final CellStyle style = c.getCellStyle();
			final short count = style.getIndention(); // 缩进数量

			return repeat.apply("\t", count) + c.getStringCellValue(); // 数据格式化
		};

		final CellType cellType = cell.getCellType();

		if (cellType == CellType.STRING) { // 字符串类型
			value = strcell.apply(cell);
		} else if (cellType == CellType.NUMERIC) { // 数值类型
			if (DateUtil.isCellDateFormatted(cell)) {
				final Date date = cell.getDateCellValue();
				value = sdf.format(date);
			} else { //
				final Double dbl = cell.getNumericCellValue();
				value = dbl;
			} // if
		} else if (cellType == CellType.FORMULA) { // 公式处理
			CellValue cellvalue = null; // 公式的值

			try {
				cellvalue = FORMULA_EVALUATOR.evaluate(cell);// 尝试采用公式计算单元格的数值
			} catch (Exception e) { // 计算失败
				// e.printStackTrace();
				for (final Function<Cell, Object> cell_reader : SimpleExcel.CELL_READERS) { // 根据优先级顺序读取单元格数据
					try {
						value = cell_reader.apply(cell); // 读取单元格数据
						break;
					} catch (Exception ex) {
						// do nothing
					}
				} // for

				return value;
			} // try

			try {
				if (DateUtil.isCellDateFormatted(cell)) {// 日期值处理
					final Date date = cell.getDateCellValue();
					value = sdf.format(date);
				} else if (cellvalue.getCellType() == CellType.NUMERIC) { // 数值类型
					value = cellvalue.getNumberValue();
				} else if (cellvalue.getCellType() == CellType.BOOLEAN) { // BOOL 类型
				} else if (cellvalue.getCellType() == CellType.ERROR) { // 错误类型
					value = cellvalue.getErrorValue();
				} else { // default 默认类型
					value = cellvalue.getStringValue();
				} // 值类型的处理

			} catch (IllegalStateException e) { // 默认采用时间格式
				value = strcell.apply(cell);
			} // try

		} else if (cellType == CellType.NUMERIC) {// 数值得处理
			if (DateUtil.isCellDateFormatted(cell)) {
				value = sdf.format(cell.getDateCellValue());
			} else {
				value = cell.getNumericCellValue();
			}
		} else if (cellType == CellType.BOOLEAN) {// 布尔值得处理
			value = cell.getBooleanCellValue();
		} else { // 默认值类型
			// do nothin so value == null
		}

		// println(value);
		return value;
	}

	/**
	 * 名称转id
	 * 
	 * @param name sheet 名称 默认数据首行为标题：
	 * @return 把 sheet名称转换成sheetdi
	 */
	public Integer shtname2shtid(final String name) {
		return this.sheets().stream().map(Sheet::getSheetName).collect(Collectors.toList()).indexOf(name);
	}

	/**
	 * 根据sheet 的名称确定sheet的编号
	 * 
	 * @param name 表单sheet的名称
	 * @return name 对应的编号
	 */
	public Integer shtid(final String name) {
		int shtid = -1;

		final List<Sheet> shts = sheets();// 获取表单列表
		for (int i = 0; i < sheets().size(); i++) {
			if (shts.get(i) == null) {
				continue;
			}

			String shtname = shts.get(i).getSheetName();
			if (shtname != null && shtname.equals(name)) {
				shtid = i;
				break;
			} // if
		} // forEach

		return shtid;
	}

	/**
	 * 书写单元格
	 * 
	 * @param i 从0开始
	 * @param j 从0开始
	 */
	public String strval(final int i, final int j) {
		return strval(activesht, i, j);
	}

	/**
	 * 书写单元格
	 * 
	 * @param i   从0开始
	 * @param j   从0开始
	 * @param sht 当前的表单
	 */
	public String strval(final Sheet sht, final int i, final int j) {
		if (i < 0 || j < 0) {
			return null;
		}

		if (sht == null) {
			println("未指定表单,sht==null");
			return null;
		}

		Row row = sht.getRow(i);
		if (row == null) {
			return "";
		}

		final Cell cell = row.getCell(j);
		if (cell == null) {
			return "";
		}

		return format(cell);
	}

	/**
	 * 格式化输出结构
	 * 
	 * @param cell excel的输出结构
	 * @return 字符串输出
	 */
	public String format(final Cell cell) {
		return this.evaluate(cell) + "";
	}

	/**
	 * 读取指定区域的数据内容
	 * 
	 * @param sht      表单对象
	 * @param rangedef 数据区域
	 * @param keys     表头列表,null表示数据中包含表头,第一行数据作为表头列表。
	 * @return 数据区域的数据内容
	 */
	public StrMatrix range(final Sheet sht, final RangeDef rangedef, final List<String> keys) {
		if (rangedef == null) {
			println("无法获得rangedef数据,rangedef 为空数据");
			return null;
		} else {
			final DataMatrix<String> strmx = this.evaluate(sht, rangedef.x0(), rangedef.y0(), rangedef.x1(),
					rangedef.y1(), keys, e -> { // 数字类型 的值转换
						if (e instanceof Number) { // 数值类型
							final Double dbl = ((Number) e).doubleValue();
							final Long lng = ((Number) e).longValue();

							if (Math.abs(dbl - lng) < SimpleExcel.EPSILON) // 浮点数与整形误差小于 误差容忍限度 EPSILON ，采用 整数表述
								return String.valueOf(lng); // 长整形
							else
								return String.valueOf(dbl); // 浮点数
						} else { // 其他类型
							return Optional.ofNullable(e).map(Object::toString).orElse(null);
						} // if
					}); // 计算数据集合

			if (strmx == null) {
				return null;
			} else {
				final String cells[][] = strmx.data();
				final List<String> headers = strmx.keys();

				return new StrMatrix(cells, headers);
			} // if
		} // if
	}

	/**
	 * 默认表头 读取指定区域的数据内容
	 * 
	 * @param rangedef 数据区域
	 * @param sht      表单对象
	 * @return 数据区域的数据内容
	 */
	public StrMatrix range(final Sheet sht, final RangeDef rangedef) {
		return this.range(sht, rangedef, null);
	}

	/**
	 * 读取sheet
	 * 
	 * @param shtid     sheet 编号从0开始
	 * @param rangeName 表单的名称 比如：A1:B10
	 * @return 获得指定表单的值
	 */
	public StrMatrix range(final int shtid, final String rangeName, final List<String> keys) {
		final Sheet sht = sheet(shtid);// 获得表单名称
		if (sht == null) {
			System.out.println("不存在编号为" + shtid + "sheet,不予读取任何数据");
			return null;
		} else {
			return this.range(sht, DataMatrix.name2rdf(rangeName), keys);
		}
	}

	/**
	 * 默认表头 读取指定区域的数据内容
	 * 
	 * @param rangedef 数据区域
	 * @return 数据区域的数据内容
	 */
	public StrMatrix range(final RangeDef rangedef) {
		return this.range(activesht, rangedef, null);
	}

	/**
	 * 读取指定区域的数据内容
	 * 
	 * @param rangedef 数据区域
	 * @param keys     键名列表
	 * @return 数据区域的数据内容
	 */
	public StrMatrix range(final RangeDef rangedef, final List<String> keys) {
		return this.range(activesht, rangedef, keys);
	}

	/**
	 * 默认表头
	 * 
	 * @param name 获得数据区域,比如 Sheet2!A1:B10
	 * @return excel 范围数据
	 */
	public StrMatrix range(final String name) {
		return this.range(name, (List<String>) null);
	}

	/**
	 * range
	 * 
	 * @param name 获得数据区域
	 * @param keys null,表示数据中包含表头,第一行就是表头
	 * 
	 * @return excel 范围数据
	 */
	public StrMatrix range(final String name, final String[] keys) {
		return this.range(name, Arrays.asList(keys));
	}

	/**
	 * range
	 * 
	 * @param name 获得数据区域：range 全名用比如sheet1!A1:B10
	 * @param keys null,表示数据中包含表头,第一行就是表头
	 * @return excel 范围数据
	 */
	public StrMatrix range(final String name, final List<String> keys) {
		if (name.contains("!")) {// 名称中包含有表单名
			final String ss[] = name.split("!"); // 从name里提取sheet与区域名称
			if (ss.length > 1) { //
				final String sheetname = ss[0].trim();
				final String rangeName = ss[1].trim();
				// 按表单进行区域rangedef内容的获取
				return this.range(sheetname, rangeName, keys);
			} else {// 名称不包含表单名
				System.out.println("非法表单名称:" + name);
				return null;
			} // if
		} // if name.contains("!")

		// 设置默认的额sheet数据
		if (activesht == null) {
			this.activesht = selectSheet(0);
		}

		return this.range(DataMatrix.name2rdf(name), keys);
	}

	/**
	 * 读取sheet
	 * 
	 * @param shtname   sheet 名字
	 * @param rangeName 区域名称
	 * @return StrMatrix
	 */
	public StrMatrix range(final String shtname, final String rangeName, final List<String> keys) {
		final Integer shtid = this.shtid(shtname);
		if (shtid == null || shtid < 0) {
			System.out.println("不存在表单:\"" + shtname + "\"");
			return null;
		}
		return this.range(shtid, rangeName, keys);
	}

	/**
	 * 读取数据区域
	 * 
	 * @param name 获得数据区域
	 * @param keys null,表示数据中包含表头,第一行就是表头
	 * 
	 * @return excel 范围数据
	 */
	public Optional<StrMatrix> rangeOpt(final String name, final String[] keys) {
		return Optional.ofNullable(this.range(name, keys));
	}

	/**
	 * 读取数据区域
	 * 
	 * @param name   获得数据区域
	 * @param keys   null,表示数据中包含表头,第一行就是表头
	 * @param mapper StrMatrix 结果变换器
	 * 
	 * @return excel 范围数据
	 */
	public <T> Optional<T> rangeOpt(final String name, final String[] keys,
			final Function<? super StrMatrix, T> mapper) {
		return Optional.ofNullable(this.range(name, keys)).map(mapper);
	}

	/**
	 * 创建一个单元格(getOrCreateCell 的别名)
	 * 
	 * @param sht 表单对象
	 * @param i   行索引 从0开始
	 * @param j   列索引 从0开始
	 * @return 单元格对象
	 */
	public Cell cell(final int i, final int j) {
		return this.getOrCreateCell(i, j);
	}

	/**
	 * 创建一个单元格(getOrCreateCell 的别名)
	 * 
	 * @param sheetName 表单名称
	 * @param i         行索引 从0开始
	 * @param j         列索引 从0开始
	 * @return 单元格对象
	 */
	public Cell cell(final String sheetName, final int i, final int j) {
		final Sheet sheet = Optional.ofNullable(sheetName).map(this::getOrCreateSheet).orElse(activesht);
		return this.getOrCreateCell(sheet, i, j);
	}

	/**
	 * 创建一个单元格
	 * 
	 * @param i 行索引 从0开始
	 * @param j 列索引 从0开始
	 * @return 单元格对象
	 */
	public Cell getOrCreateCell(final int i, final int j) {
		return this.getOrCreateCell(this.activesht, i, j);
	}

	/**
	 * 创建一个单元格
	 * 
	 * @param sht 表单对象
	 * @param i   行索引 从0开始
	 * @param j   列索引 从0开始
	 * @return 单元格对象
	 */
	public Cell getOrCreateCell(final Sheet sheet, final int i, final int j) {
		return Optional.ofNullable(sheet).map(Optional::of) //
				.orElseGet(() -> Optional.ofNullable(this.activesht)) //
				.map(sht -> {
					Row row = sht.getRow(i);
					if (row == null) {
						row = sht.createRow(i);
					}
					Cell c = row.getCell(j);
					if (c == null) {
						c = row.createCell(j);
					}
					return c;
				}).orElse(null);
	}

	/**
	 * 根据sheet 名称获取或创建sheet
	 * 
	 * @param shtname exceel表单名称
	 * @return excel 表单
	 */
	public Sheet getOrCreateSheet(final String shtname) {
		final Integer shtid = this.shtname2shtid(shtname);
		final Sheet sht = shtid < 0 ? this.workbook.createSheet(shtname) : sheet(shtid);
		return sht;
	}

	/**
	 * 写入一段数据(不带有表头的书写格式) <br>
	 * 公式中的单元格的引用：行数 从0开始,eg: A0 对应第一行第一列, A1对应第二行第一列，B0 对应第一行第二列。 <br>
	 * <br>
	 * !!需要注意,write后一定要save才可以将数据写入到文件,没有save操作数据不会写入!
	 * 
	 * @param firstCell 第一个单元格
	 * @param height    行数
	 * @param width     列数
	 * @param lines     按照行序列编排的数据内容，对于无法填充到height与width的数据不予写入任何数据。即保留空值
	 * @return SimpleExcel 对象本身 以实现链式编程
	 */
	public SimpleExcel write(final Cell firstCell, final int height, final int width, final List<List<String>> lines) {
		final Sheet sht = firstCell.getSheet();
		final int start_i = firstCell.getAddress().getRow();// 开始行号
		final int start_j = firstCell.getAddress().getColumn();// 开始列号
		final Iterator<List<String>> litr = lines.iterator();
		final int final_height = Math.min(lines.size(), height);// 调整写入行数的数量

		for (int i = start_i; i < start_i + final_height; i++) {
			final Iterator<String> itr = litr.next().iterator();
			int j = start_j;

			while (itr.hasNext() && j - start_j < width) {

				final Cell cell = getOrCreateCell(sht, i, j++);
				final String value = itr.next();

				if (this.datafmt == DataFormat.AUTO_FORMAT) {

					// 浮点数
					final Double d = parseDouble(value);
					if (d != null) {
						cell.setCellValue(d);
						continue;
					}

					// 日期类型
					final LocalDateTime ldt = Times.asLocalDateTime(value);
					if (ldt != null) {
						cell.setCellValue(ldt);
						final short fmt = workbook.createDataFormat().getFormat("yyyy-MM-dd HH:mm:ss");
						final CellStyle cellStyle = this.createWKCellStyle();
						cellStyle.setDataFormat(fmt);
						cell.setCellStyle(cellStyle);
						continue;
					}

					// 公式设置
					if (value != null && value.trim().length() > 0 && value.trim().charAt(0) == '=') { // 公式设置
						final String _value = value.trim();
						int k = 0;
						while (_value.charAt(k++) == '=') {
							// do nothing
						}
						final String formula = _value.substring(k - 1).trim(); // 数据公式
						if (formula.length() > 0) { // if
							final int row_offset = this.isWriteKeysFlag() ? 1 : 0; // 若是写入表头则偏移增加1
							final String adjusted_formula = DataMatrix.adjust_formula(formula,
									Tuple2.of(firstCell.getRowIndex() + row_offset, firstCell.getColumnIndex())); // 公式偏移位置调整
							cell.setCellFormula(adjusted_formula);
							continue;
						} // if
					} // if

					// 设置单元格的字符串数值
					cell.setCellValue(value);

				} else { // 其他数据格式
					cell.setCellValue(value);
				} // if

			} // while
		} // for

		// 更新影响区域
		this.setAffectedArea(new AffectedArea(this, firstCell, height, width));
		// System.err.println(this.affectedArea);

		return this;
	}

	/**
	 * 写入一段数据<br>
	 * 公式中的单元格的引用：行数 从0开始,eg: A0 对应第一行第一列, A1对应第二行第一列，B0 对应第一行第二列。 <br>
	 * <br>
	 * !!需要注意,write后一定要save才可以将数据写入到文件,没有save操作数据不会写入!
	 * 
	 * @param firstCell 第一个单元格
	 * @param lines     按照行序列编排的数据内容，
	 * @return SimpleExcel 对象本身 以实现链式编程
	 */
	public SimpleExcel write(final Cell firstCell, final List<List<String>> lines) {
		return this.write(firstCell, Integer.MAX_VALUE, Integer.MAX_VALUE, lines);
	}

	/**
	 * EXCEL 写入 <br>
	 * 若开启表头写入（默认），公式中的单元格的引用：行数 从1开始,eg: A1 对应第一行第一列, A2对应第二行第一列，B1 对应第一行第二列。 <br>
	 * <br>
	 * !!需要注意,write后一定要save才可以将数据写入到文件,没有save操作数据不会写入!
	 * 
	 * @param firstCell 第一个单元格
	 * @param mx        待写入的数据内容
	 * @return SimpleExcel 对象本身 以实现链式编程
	 */
	public SimpleExcel write(final Cell firstCell, final StrMatrix mx) {
		if (firstCell == null || mx == null) {
			System.out.println("传入参数存在空值");
			return this;
		}
		// System.out.println(mx);
		final List<List<String>> rows = new LinkedList<>();
		if (this.write_keys_flag) {
			rows.add(mx.keys());
		}
		rows.addAll(mx.rows());
		final int width = mx.width();

		return this.write(firstCell, rows.size(), width, rows);

	}

	/**
	 * 写入excel(不有表头的书写格式)<br>
	 * 公式中的单元格的引用：行数 从0开始,eg: A0 对应第一行第一列, A1对应第二行第一列，B0 对应第一行第二列。 <br>
	 * <br>
	 * !!需要注意,write后一定要save才可以将数据写入到文件,没有save操作数据不会写入!
	 * 
	 * @param shtname sheet 名称
	 * @param address 地址名称
	 * @param datas   待写入的数据矩阵
	 * @return SimpleExcel 对象本身 以实现链式编程
	 */
	public SimpleExcel write(String shtname, String address, String[][] datas) {
		return write(shtname, address, new StrMatrix(datas, null));
	}

	/**
	 * 写入excel (有表头的书写格式) <br>
	 * 若开启表头写入（默认），公式中的单元格的引用：行数 从1开始,eg: A1 对应第一行第一列, A2对应第二行第一列，B1 对应第一行第二列。 <br>
	 * <br>
	 * !!需要注意,write后一定要save才可以将数据写入到文件,没有save操作数据不会写入!
	 * 
	 * @param shtname sheet 名称
	 * @param address 地址名称
	 * @param mx      待写入的数据矩阵
	 * @return SimpleExcel 对象本身 以实现链式编程
	 */
	public SimpleExcel write(final String shtname, final String address, final StrMatrix mx) {
		final String ss[] = address.split("[:]+");
		final Tuple2<Integer, Integer> tup2 = SimpleExcel.addr2tuple(ss[0]);
		return this.write(getOrCreateCell(this.getOrCreateSheet(shtname), tup2._1, tup2._2), mx);
	}

	/**
	 * 写入excel <br>
	 * <br>
	 * !!需要注意,write后一定要save才可以将数据写入到文件,没有save操作数据不会写入!
	 * 
	 * @param fulladdress 位置全地址 比如：sheet2!C3
	 * @param datas       数据内容
	 * @param keys        表头
	 * @return SimpleExcel 对象本身 以实现链式编程
	 */
	public SimpleExcel write(String fulladdress, String datas[][], List<String> keys) {
		return this.write(fulladdress, new StrMatrix(datas, keys));
	}

	/**
	 * 写入excel (有表头的书写格式) <br>
	 * 若开启表头写入（默认），公式中的单元格的引用：行数 从1开始,eg: A1 对应第一行第一列, A2对应第二行第一列，B1 对应第一行第二列。 <br>
	 * <br>
	 * !!需要注意,write后一定要save才可以将数据写入到文件,没有save操作数据不会写入!
	 * 
	 * @param fulladdress sheet2!C3
	 * @param dmx         数据内容
	 * @return SimpleExcel 对象本身 以实现链式编程
	 */
	public SimpleExcel write(final String fulladdress, final DataMatrix<?> dmx) {
		final List<String> header = dmx.keys();
		final String[][] mm = objs2strs(dmx.data());

		return this.write(fulladdress, mm, header);
	}

	/**
	 * 写入excel (有表头的书写格式) <br>
	 * 若开启表头写入（默认），公式中的单元格的引用：行数 从1开始,eg: A1 对应第一行第一列, A2对应第二行第一列，B1 对应第一行第二列。
	 * 
	 * 书写位置 默认为: DEFAULT_WRITE_CELL_ADDRESS <br>
	 * <br>
	 * <br>
	 * !!需要注意,write后一定要save才可以将数据写入到文件,没有save操作数据不会写入!
	 * 
	 * @param dmx 数据内容
	 * @return SimpleExcel 对象本身 以实现链式编程
	 */
	public SimpleExcel write(final DataMatrix<?> dmx) {
		final var fulladdress = Optional.ofNullable(this.activesht).map(e -> e.getSheetName()).map(sheetName -> {
			final var d = DEFAULT_WRITE_CELL_ADDRESS; // 简写
			final var i = d.indexOf("!"); // 索引偏移位置
			final var rngName = i >= 0 && d.length() > i + 1 // d 中包含了 sheetName
					? d.substring(i + 1) // 提出掉sheetName
					: d; // d中没有SheetName,d 就是一个 表单的内的Range
			final var fulladdr = "%s!%s".formatted(sheetName, rngName); // 皮装全地址
			return fulladdr;
		}).orElse(DEFAULT_WRITE_CELL_ADDRESS);
		return this.write(fulladdress, dmx);
	}

	/**
	 * 写入excel (有表头的书写格式) <br>
	 * 若开启表头写入（默认），公式中的单元格的引用：行数 从1开始,eg: A1 对应第一行第一列, A2对应第二行第一列，B1 对应第一行第二列。 <br>
	 * <br>
	 * !!需要注意,write后一定要save才可以将数据写入到文件,没有save操作数据不会写入!
	 * 
	 * @param fulladdress sheet2!C3
	 * @param dfm         数据框
	 * @return SimpleExcel 对象本身 以实现链式编程
	 */
	public SimpleExcel write(final String fulladdress, final DFrame dfm) {
		final DataMatrix<Object> dmx = dfm.rowS().map(e -> e.toArray(o -> o + ""))
				.collect(DataMatrix.dmxclc(Arrays::asList, dfm.keys()));
		return this.write(fulladdress, dmx);
	}

	/**
	 * 写入excel (有表头的书写格式) <br>
	 * 若开启表头写入（默认），公式中的单元格的引用：行数 从1开始,eg: A1 对应第一行第一列, A2对应第二行第一列，B1 对应第一行第二列。<br>
	 * 
	 * 书写位置 默认为: DEFAULT_WRITE_CELL_ADDRESS <br>
	 * <br>
	 * <br>
	 * !!需要注意,write后一定要save才可以将数据写入到文件,没有save操作数据不会写入!
	 * 
	 * @param dfm 数据内容
	 * @return SimpleExcel 对象本身 以实现链式编程
	 */
	public SimpleExcel write(final DFrame dfm) {
		return this.write(SimpleExcel.dfm2dmx(dfm));
	}

	/**
	 * 写入excel (不带有表头的书写格式) <br>
	 * 公式中的单元格的引用：行数 从0开始,eg: A0 对应第一行第一列, A1对应第二行第一列，B0 对应第一行第二列。 <br>
	 * <br>
	 * !!需要注意,write后一定要save才可以将数据写入到文件,没有save操作数据不会写入!
	 * 
	 * @param <T>         元素类型
	 * @param fulladdress eg. sheet2!C3
	 * @param datas       数据内容
	 * @return SimpleExcel 对象本身 以实现链式编程
	 */
	public <T> SimpleExcel write(String fulladdress, T datas[]) {

		final String[][] lines = new String[][] {
				Arrays.stream(datas).map(e -> e instanceof String ? (String) e : e + "").toArray(String[]::new) };

		return this.write(fulladdress, lines);
	}

	/**
	 * 写入excel (不带有表头的书写格式) <br>
	 * 公式中的单元格的引用：行数 从0开始,eg: A0 对应第一行第一列, A1对应第二行第一列，B0 对应第一行第二列。 <br>
	 * <br>
	 * !!需要注意,write后一定要save才可以将数据写入到文件,没有save操作数据不会写入!
	 * 
	 * @param <T>         元素类型
	 * @param fulladdress eg. sheet2!C3
	 * @param datas       数据内容
	 * @return SimpleExcel 对象本身 以实现链式编程
	 */
	public <T> SimpleExcel write(String fulladdress, T datas[][]) {

		final int height = datas.length;
		final int width = datas[0].length;
		final List<List<String>> lines = Arrays.stream(datas).map(line -> //
		Arrays.stream(line).map(e -> e instanceof String //
				? (String) e //
				: e + "").collect(Collectors.toList()) //
		).collect(Collectors.toList());

		String shtname = null;
		String address = null;

		final String ss[] = fulladdress.split("[!]+");
		if (ss.length < 2) {
			address = fulladdress;
			final int index = this.workbook.getActiveSheetIndex();
			final Sheet sht = index >= 0 && index < this.workbook.getNumberOfSheets() //
					? this.workbook.getSheetAt(index)
					: this.workbook.createSheet();
			shtname = sht.getSheetName();
		} else {
			shtname = ss[0];
			address = ss[1];
		}

		final Tuple2<Integer, Integer> tup2 = SimpleExcel.addr2tuple(address);
		return write(this.getOrCreateCell(this.getOrCreateSheet(shtname), tup2._1, tup2._2), height, width, lines);
	}

	/**
	 * 写入excel (有表头的书写格式) <br>
	 * 若开启表头写入（默认），公式中的单元格的引用：行数 从1开始,eg: A1 对应第一行第一列, A2对应第二行第一列，B1 对应第一行第二列。 <br>
	 * <br>
	 * !!需要注意,write后一定要save才可以将数据写入到文件,没有save操作数据不会写入!
	 * 
	 * @param fulladdress sheet的全路径名
	 * @param mx          数据矩阵
	 * @return SimpleExcel 对象本身 以实现链式编程
	 */
	public SimpleExcel write(final String fulladdress, final StrMatrix mx) {
		final String ss[] = fulladdress.split("[!]+");// 使用 ! 分隔出sheet名称。
		String shtname = null;
		String address = null;
		if (ss.length < 2) {
			address = fulladdress;
			final List<Sheet> sheets = this.sheets();
			Sheet sht = sheets.size() > 0 ? sheets.get(0) : this.workbook.createSheet();
			shtname = sht.getSheetName();
		} else {
			shtname = ss[0];
			address = ss[1];
		}
		return this.write(shtname, address, mx);
	}

	/**
	 * 数据范围的操作回调
	 * 
	 * @param sheet    表单对象
	 * @param rangeDef 数据范围（首尾均包含） 比如：A1:B10
	 * @return SimpleExcel 对象本身
	 */
	public SimpleExcel withRange(final Sheet sheet, final RangeDef rangeDef, final Consumer<Cell> action) {
		for (int i = rangeDef.x0(); i <= rangeDef.x1(); i++) {
			for (int j = rangeDef.y0(); j <= rangeDef.y1(); j++) {
				final Cell cell = this.getOrCreateCell(sheet, i, j);
				action.accept(cell);
			}
		}
		return this;
	}

	/**
	 * 当前表单的数据范围的操作回调
	 * 
	 * @param rangeDef 数据范围rangeDef
	 * @return SimpleExcel 对象本身
	 */
	public SimpleExcel withRange(final RangeDef rangeDef, final Consumer<Cell> action) {
		return this.withRange(activesht, rangeDef, action);
	}

	/**
	 * 数据范围的操作回调
	 * 
	 * @param rangeName 表单的名称 比如：SHEET!A1:B10, A1:B10 均为包含
	 * @return SimpleExcel 对象本身
	 */
	public SimpleExcel withRange(final String rangeName, final Consumer<Cell> action) {
		final Tuple2<String, RangeDef> tup = SimpleExcel.name2tupdef(rangeName);
		final String sheetname = tup._1;
		final Sheet sheet = this.getOrCreateSheet(sheetname);
		final RangeDef rangeDef = tup._2;
		return this.withRange(sheet, rangeDef, action);
	}

	/**
	 * 默认式样 <br>
	 * 创建/注册Workbook级别的单元格式样 !! 需要注意 Excell 为了避免荣誉，Cell的式样是无法进行修改的，<br>
	 * 而只能从workook创建一个式样style，让后将cell.setCellStyle(style)去引用。 <br>
	 * 
	 * @return CellStyle
	 */
	public CellStyle createWKCellStyle() {
		return this.createWKCellStyle(null);
	}

	/**
	 * 创建/注册Workbook级别的单元格式样 !! 需要注意 Excell 为了避免荣誉，Cell的式样是无法进行修改的，<br>
	 * 而只能从workook创建一个式样style，让后将cell.setCellStyle(style)去引用。 <br>
	 * 
	 * @param 参照式样的Cell,新创建的式样一Cell式样为基础。cell 为null表示默认式样
	 * @return CellStyle
	 */
	public CellStyle createWKCellStyle(final Cell cell) {
		final var style = this.workbook.createCellStyle(); // 创建默认式样
		if (!Objects.isNull(cell)) { // 存在惨遭cell 拷贝参照cell的式样
			style.cloneStyleFrom(cell.getCellStyle());
		}
		return style;
	}

	/**
	 * 创建单元格式样<br>
	 * !! 需要注意 Excell 为了避免荣誉，Cell的式样是无法进行修改的，<br>
	 * 而只能从workook创建一个式样style，让后将cell.setCellStyle(style)去引用。 <br>
	 * 
	 * @param 参照式样的Cell,新创建的式样一Cell式样为基础。cell 为null表示默认式样
	 * @return Pack&lt;CellStyle&gt;
	 */
	public Pack<CellStyle> packWKCellStyle(final Cell cell) {
		return Pack.of(this.createWKCellStyle(cell));

	}

	/**
	 * 创建字体 <br>
	 * !! 需要注意 Excell 为了避免荣誉，Cell的式样是无法进行修改的，<br>
	 * 而只能从workook创建一个式样style，让后将cell.setCellStyle(style)去引用。 <br>
	 * 
	 * @param 参照式样的Cell,新创建的式样一Cell式样为基础。cell 为null表示默认式样
	 * @return Pack&lt;Font&gt;
	 */
	public Pack<Font> packWKFont(final Cell cell) {
		return Optional.ofNullable(cell).map(c -> c.getCellStyle().getFontIndex()).map(this.workbook::getFontAt)
				.map(Pack::of).orElseGet(() -> Pack.of(this.workbook.createFont()));

	}

	/**
	 * 选择制定位置的sheet
	 * 
	 * @param i sheet 的编号
	 * @return 选择表单
	 */
	public Sheet selectSheet(final String name) {
		return Optional.ofNullable(getOrCreateSheet(name)).map(this.workbook::getSheetIndex).map(this::selectSheet)
				.orElse(null);
	}

	/**
	 * 选择制定位置的sheet
	 * 
	 * @param i sheet 的编号
	 * @return 选择表单
	 */
	public Sheet selectSheet(final int i) {
		if (i < 0 || i >= this.workbook.getNumberOfSheets()) {
			System.out.println("sheet[" + i + "] 编号非法!");
			return null;
		} else {
			// 所有sheet取消选择（防止多重选择），这里采用先全部unselected而后再专门select的策略。
			for (final var sheet : this.sheets()) {
				sheet.setSelected(false);
			}
			this.workbook.setActiveSheet(i); // 选中i号sheet
			this.activesht = this.workbook.getSheetAt(this.workbook.getActiveSheetIndex());

			return this.activesht;
		}
	}

	/**
	 * 选择制定位置的sheet
	 * 
	 * @param i sheet 的编号
	 * @return 选择表单
	 */
	public Optional<Sheet> selectSheetOpt(final int i) {
		return Optional.ofNullable(this.selectSheet(i));
	}

	/**
	 * 所有的Sheet 的流
	 */
	public Stream<Sheet> sheetS() {
		// 遍历workbook获取sheet 列表
		final Spliterator<Sheet> splitr = Spliterators.spliteratorUnknownSize(workbook.sheetIterator(),
				Spliterator.ORDERED);

		return StreamSupport.stream(splitr, false);
	}

	/**
	 * 所有的Sheet 列表
	 */
	public List<Sheet> sheets() {
		// 遍历workbook获取sheet 列表
		return this.sheetS().collect(Collectors.toList());
	}

	/**
	 * 获取表单数据
	 * 
	 * @param i 表单id 从0开始
	 * @return 表单对象
	 */
	public Sheet sheet(final int i) {
		Sheet sht = null;
		try {
			sht = this.workbook.getSheetAt(i);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return sht;
	}

	/**
	 * 数据保存
	 * 
	 * @param filename 保存的文件名
	 * @return SimpleExcel 对象本身 以实现链式编程
	 */
	public SimpleExcel saveAs(String filename) {
		return this.saveAs(new File(filename));
	}

	/**
	 * 保存成文件
	 * 
	 * @param file excel文件对象,如果file业已存在则给与添加
	 * @return SimpleExcel 对象本身 以实现链式编程
	 */
	public SimpleExcel saveAs(final File file) {
		return this.saveAs(file, null);
	}

	/**
	 * 保存成文件
	 * 
	 * @param file   excel文件对象,如果file业已存在则给与添加
	 * @param append 是否是追加模式, null 通过判断 文件是否存在作为依据,存在追加,否则不追加
	 * @return SimpleExcel 对象本身 以实现链式编程
	 */
	public SimpleExcel saveAs(final File file, final Boolean append) {
		final File parentDir = file.getParentFile(); // 上级目录
		if (parentDir != null && !parentDir.exists()) {
			parentDir.mkdirs();
		}

		final boolean appendFlag = Optional.ofNullable(append).orElse(file.isDirectory());
		try (final FileOutputStream fos = new FileOutputStream(file, appendFlag)) {
			this.workbook.write(fos);
		} catch (Exception e) {
			e.printStackTrace();
		} // try
		return this;
	}

	/**
	 * excel 文件保存
	 * 
	 * @return SimpleExcel 对象本身 以实现链式编程
	 */
	public SimpleExcel save() {
		return this.save(null);
	}

	/**
	 * excel 文件保存
	 * 
	 * @param append 是否是追加模式, null 通过判断 文件是否存在作为依据,存在追加,否则不追加
	 * @return SimpleExcel 对象本身 以实现链式编程
	 */
	public SimpleExcel save(final Boolean append) {
		return this.saveAs(this.xlsfile, append);
	}

	/**
	 * 设置数据格式
	 * 
	 * @param datafmt 设置为write行为的数据模式
	 * @return
	 */
	public SimpleExcel setDataFormat(final DataFormat datafmt) {
		this.datafmt = datafmt;
		return this;
	}

	/**
	 * 设置数据格式
	 * 
	 * @param datafmt 设置为write行为的数据模式
	 * @return
	 */
	public SimpleExcel datafmt(final DataFormat datafmt) {
		return this.setDataFormat(datafmt);
	}

	/**
	 * 设置为write行为：数据为字符串模式
	 * 
	 * @return
	 */
	public SimpleExcel strfmt() {
		return this.setDataFormat(DataFormat.STR_FORMAT);
	}

	/**
	 * 设置为write行为：自动探测串模式
	 * 
	 * @return
	 */
	public SimpleExcel autofmt() {
		return this.setDataFormat(DataFormat.AUTO_FORMAT);
	}

	/**
	 * 写入表头 标记
	 * 
	 * @return
	 */
	public boolean isWriteKeysFlag() {
		return write_keys_flag;
	}

	/**
	 * 设置 写入表头 标记
	 * 
	 * @param write_keys_flag
	 */
	public SimpleExcel setWriteKeysFlag(boolean write_keys_flag) {
		this.write_keys_flag = write_keys_flag;
		return this;
	}

	/**
	 * write 操作的影响范围
	 * 
	 * @return (左上角的单元格,(影响的行数,影响的列数))
	 */
	public AffectedArea getAffectedArea() {
		return this.affectedArea;
	}

	/**
	 * 
	 * @param action
	 * @return
	 */
	public SimpleExcel withAffectedArea(final Consumer<AffectedArea> action) {
		action.accept(this.getAffectedArea());
		return this;
	}

	/**
	 * 设置影响区
	 * 
	 * @param aa 影响区
	 * @return
	 */
	public synchronized SimpleExcel setAffectedArea(final AffectedArea aa) {
		this.affectedArea = aa;
		return this;
	}

	/**
	 * 指定(AffectedArea)作为落笔留白 <br>
	 * 选择一块数据区域: (响应区域)落笔点设置作为书写位置的前一行: <br>
	 * 比如：excel.select("Sheet2!B1").writeTable(dfm) <br>
	 * dfm实际写在B2
	 * 
	 * @param rangeName 区域名称
	 */
	public AffectedArea select(final String rangeName) {
		final var rdf = DataMatrix.name2rdf(rangeName);

		if (rdf.isBlankName()) { // 区域中没有有效的表单名称，打开默认表单
			this.selectSheetOpt(0).orElseGet(() -> {
				final var shtname = this.getDefaultSheet(); // 默认表单名称
				println("使用默认“%s”代替".formatted(shtname));
				return this.selectSheet(shtname); // 打开默认Sheet1
			});
		} else { // 表单名称有效
			this.selectSheet(rdf.sheetName());
		}

		// 构建0偏移
		return new AffectedArea(this, rdf, 0, 0).focus();
	}

	/**
	 * 默认sheet名称
	 * 
	 * @return
	 */
	public String getDefaultSheet() {
		return DEFAULT_WRITE_CELL_ADDRESS.substring(0, DEFAULT_WRITE_CELL_ADDRESS.indexOf("!"));
	}

	/**
	 * 文件关闭
	 */
	@Override
	public void close() {
		try {
			workbook.close();
		} catch (IOException e) {
			e.printStackTrace();
		} // try
	}

	/**
	 * 文件关闭
	 * 
	 * @param 是否保存文件
	 */
	public void close(final boolean saveflag) {
		if (saveflag) {
			this.save();
		}
		this.close();
	}

	/**
	 * 文件保存与关闭 <br>
	 * this.close(true) 的别名
	 */
	public void sclose() {
		this.close(true);
	}

	/**
	 * 解析 双精度 浮点数
	 * 
	 * @param obj 数据对象
	 * @return 双精度
	 */
	public static Double parseDouble(final Object obj) {
		if (obj instanceof Number) {
			return ((Number) obj).doubleValue();
		} else if (obj != null) {
			final String line = obj + "";
			Double d = null;
			try {
				d = Double.parseDouble(line);
			} catch (Exception e) {
				// do nothing
			}
			return d;
		} else {
			return null;
		}

	}

	/**
	 * 把一个对象数组转换成一个字符串数组。
	 * 
	 * @param objs 对象二维数据
	 * @return 字符串类型的二维数组
	 */
	public static String[][] objs2strs(final Object[][] objs) {
		final int height = objs.length;
		final int width = objs[0].length;
		final String[][] strs = new String[height][];

		for (int i = 0; i < height; i++) {
			strs[i] = new String[width];
			for (int j = 0; j < Math.min(width, objs[i] == null ? 0 : objs[i].length); j++) {
				strs[i][j] = objs[i][j] + "";
			} // for
		} // for

		return strs;
	}

	/**
	 * A3:--->2,0
	 * 
	 * @param address excel 格式的单元格地址描述
	 * @return （行索引从0开始，列索引从0开始）
	 */
	public static Tuple2<Integer, Integer> addr2tuple(final String address) {
		final Pattern p = Pattern.compile("\\s*([A-Z]+)\\s*([0-9]+)\\s*", Pattern.CASE_INSENSITIVE);
		final Matcher matcher = p.matcher(address.toUpperCase());
		if (matcher.find()) {
			final String g1 = matcher.group(1);
			final String g2 = matcher.group(2);
			// System.out.println(g1+"/"+excelname2i(g1)+"----->"+g2+"/"+excelname2i(g2));
			final Integer x0 = DataMatrix.excel_name_to_index(g1);// 列名
			final Integer y0 = DataMatrix.excel_name_to_index(g2);// 行名
			return new Tuple2<>(y0, x0);
		} // if
		return null;
	}

	/**
	 * 把一个点索引转换成cell地址名
	 * 
	 * @param tuple 顶坐标 x(水平),y （垂直）
	 * @return cell位置的字符串字符串
	 */
	public static String tuple2addr(Tuple2<Integer, Integer> tuple) {
		return DataMatrix.index_to_excel_name(tuple._2) + "" + tuple._1;
	}

	/**
	 * 把excel 名称转换成位置坐标（包含sheetname)
	 * 
	 * @param rangeName A1:B13这样的字符串
	 * @return (sheetname,名称转rangedef)
	 */
	public static Tuple2<String, RangeDef> name2tupdef(final String rangeName) {
		final int i = rangeName.indexOf("!");
		String _rangename = rangeName;
		String sheetname = null;
		if (i >= 0 && i < rangeName.length()) {
			sheetname = rangeName.substring(0, i);
			_rangename = rangeName.substring(i + 1);
		} else {
			// do nothing
		}

		return Tuple2.of(sheetname, DataMatrix.name2rdf(_rangename));
	}

	/**
	 * 把坐标索引(左上与右下)转换成 EXCEL 区域名称，比如 (0,0),(1,1) 转换成 A1:B2
	 * 
	 * @param lt 左上角单元的坐标索引,从0开始:(0,0)表示第一个单元格
	 * @param rb 右下角的坐标索引，从0开始:(0,0)表示第一个单元格
	 * @return range 的名称
	 */
	public static String ltrb2name(final Tuple2<Integer, Integer> lt, final Tuple2<Integer, Integer> rb) {
		if (lt == null || rb == null) {
			return null;
		}

		final Tuple2<Integer, Integer> _lt = lt.fmap1(e -> e + 1);
		final Tuple2<Integer, Integer> _rb = rb.fmap1(e -> e + 1);
		final String ltaddr = tuple2addr(_lt);
		final String rbaddr = tuple2addr(_rb);

		return MessageFormat.format("{0}:{1}", ltaddr, rbaddr);
	}

	/**
	 * 数据矩阵转数据框
	 * 
	 * @param<T> 元素类型
	 * @param dmx 数据框
	 * @return 数据框
	 */
	public static <T> DFrame dmx2dfm(final DataMatrix<T> dmx) {
		return dmx.rowS(IRecord::REC).collect(DFrame.dfmclc);
	}

	/**
	 * 数据框转数据矩阵
	 * 
	 * @param dfm 数据框
	 * @return 数据矩阵
	 */
	public static DataMatrix<Object> dfm2dmx(final DFrame dfm) {
		return dfm2dmx(dfm, e -> e);
	}

	/**
	 * 数据框转数据矩阵
	 * 
	 * @param <T>    元素类型
	 * @param dfm    数据框
	 * @param mapper 数据转换函数 obj->t
	 * @return 数据矩阵
	 */
	public static <T> DataMatrix<T> dfm2dmx(final DFrame dfm, final Function<Object, T> mapper) {
		final List<String> keys = dfm.keys();
		return dfm.rowS().map(e -> e.valueS().map(mapper).collect(Collectors.toList()))
				.collect(DataMatrix.dmxclc(keys));
	}

	/**
	 * SimpleExcel 生成器
	 * 
	 * @param file excel 文件对象
	 * @return SimpleExcel
	 */
	public static SimpleExcel of(final File file) {
		return new SimpleExcel(file);
	}

	/**
	 * SimpleExcel 生成器
	 * 
	 * @param file excel 文件对象
	 * @return SimpleExcel
	 */
	public static SimpleExcel xls(final File file) {
		return new SimpleExcel(file);
	}

	/**
	 * SimpleExcel 生成器
	 * 
	 * @param path excel 文件路径的绝对路径
	 * @return SimpleExcel
	 */
	public static SimpleExcel of(final String path) {
		return SimpleExcel.of(new File(path));
	}

	/**
	 * SimpleExcel 生成器
	 * 
	 * @param path excel 文件路径的绝对路径
	 * @return SimpleExcel
	 */
	public static SimpleExcel xls(final String path) {
		return SimpleExcel.of(new File(path));
	}

	/**
	 * 格式化输出
	 * 
	 * @param objs 数据列表
	 * @return 格式化输出
	 */
	public static String println(final Object... objs) {
		final String line = Stream.of(objs).map(e -> e + "").collect(Collectors.joining("\n"));
		System.out.println(line);
		return line;
	}

	/**
	 * 数据格式
	 * 
	 * @author xuqinghua
	 *
	 */
	public enum DataFormat {
		AUTO_FORMAT, // 自动格式
		STR_FORMAT // 字符串格式
	};

	/**
	 * 喷涂背景颜色
	 */
	public static final Function<IndexedColors, Consumer<CellStyle>> background_color = color -> cellstyle -> {
		cellstyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
		cellstyle.setFillBackgroundColor(color.getIndex());
		cellstyle.setFillForegroundColor(color.getIndex());
	};

	/**
	 * 喷涂背景颜色
	 */
	public final static Function<BorderStyle, BiFunction<BorderName, IndexedColors, Consumer<CellStyle>>> border_color = borderstyle -> (
			border, color) -> cellstyle -> {
				switch (border) {
				case L, LEFT: {
					cellstyle.setBorderLeft(borderstyle);
					cellstyle.setRightBorderColor(color.getIndex());
					break;
				}
				case T, TOP: {
					cellstyle.setBorderTop(borderstyle);
					cellstyle.setTopBorderColor(color.getIndex());
					break;
				}
				case R, RIGHT: {
					cellstyle.setBorderRight(borderstyle);
					cellstyle.setRightBorderColor(color.getIndex());
					break;
				}
				case B, BOTTOM: {
					cellstyle.setBorderBottom(borderstyle);
					cellstyle.setBottomBorderColor(color.getIndex());
					break;
				}
				case LT, LEFT_TOP: {
					cellstyle.setBorderLeft(borderstyle);
					cellstyle.setLeftBorderColor(color.getIndex());
					cellstyle.setBorderTop(borderstyle);
					cellstyle.setTopBorderColor(color.getIndex());
					break;
				}
				case LB, LEFT_BOTTOM: {
					cellstyle.setBorderLeft(borderstyle);
					cellstyle.setLeftBorderColor(color.getIndex());
					cellstyle.setBorderBottom(borderstyle);
					cellstyle.setBottomBorderColor(color.getIndex());
					break;
				}
				case TR, TOP_RIGHT: {
					cellstyle.setBorderTop(borderstyle);
					cellstyle.setTopBorderColor(color.getIndex());
					cellstyle.setBorderRight(borderstyle);
					cellstyle.setRightBorderColor(color.getIndex());
					break;
				}
				case RB, RIGHT_BOTTOM: {
					cellstyle.setBorderRight(borderstyle);
					cellstyle.setRightBorderColor(color.getIndex());
					cellstyle.setBorderBottom(borderstyle);
					cellstyle.setBottomBorderColor(color.getIndex());
					break;
				}
				case ALL: {
					cellstyle.setBorderLeft(borderstyle);
					cellstyle.setLeftBorderColor(color.getIndex());
					cellstyle.setBorderTop(borderstyle);
					cellstyle.setTopBorderColor(color.getIndex());
					cellstyle.setBorderRight(borderstyle);
					cellstyle.setRightBorderColor(color.getIndex());
					cellstyle.setBorderBottom(borderstyle);
					cellstyle.setBottomBorderColor(color.getIndex());
					break;
				}
				case NONE: {
					// goto default
				}
				default: {
					// nothing
				}
				} // switch
			};

	/**
	 * 边名称
	 */
	public static enum BorderName {
		L, LEFT, T, TOP, R, RIGHT, B, BOTTOM, LT, LEFT_TOP, LB, LEFT_BOTTOM, TR, TOP_RIGHT, RB, RIGHT_BOTTOM, ALL, NONE
	}

	/**
	 * 最大处理行数
	 */
	public static final Integer MAX_SIZE = 1000000;// 最大处理行数
	/**
	 * 最小的数字精度
	 */
	public static Double EPSILON = 1e-20;// 最小的数字精度
	private static FormulaEvaluator FORMULA_EVALUATOR;
	private static String DEFAULT_WRITE_CELL_ADDRESS = "Sheet1!A1";
	private static List<Function<Cell, Object>> CELL_READERS = new ArrayList<Function<Cell, Object>>(); // 单元格读值器
	static { // 初始化单元格读值器
		CELL_READERS.add(cell -> cell.getRichStringCellValue());
		CELL_READERS.add(cell -> cell.getStringCellValue());
		CELL_READERS.add(cell -> cell.getNumericCellValue());
		CELL_READERS.add(cell -> cell.getLocalDateTimeCellValue());
		CELL_READERS.add(cell -> cell.getDateCellValue());
		CELL_READERS.add(cell -> cell.getBooleanCellValue());
		CELL_READERS.add(cell -> cell.getErrorCellValue());
	} // static

	private File xlsfile = null;
	private Sheet activesht = null;// 当前的对象
	private Workbook workbook = new XSSFWorkbook();
	private DataFormat datafmt = DataFormat.AUTO_FORMAT;
	private boolean write_keys_flag = true;
	private AffectedArea affectedArea = null; // 操作的影响范围
	private final SimpleDateFormat sdf = new SimpleDateFormat("YYYY-MM-dd HH:mm:ss");

}
