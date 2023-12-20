package gbench.util.jdbc;

import static gbench.util.jdbc.Jdbc.namedsql_processor;
import static gbench.util.jdbc.Jdbc.namedsql_processor_escape_brace;
import static gbench.util.jdbc.Jdbc.parse2namedsqls;

import java.io.File;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import gbench.util.data.xls.SimpleExcel;
import gbench.util.data.xls.StrMatrix;
import gbench.util.io.FileSystem;
import gbench.util.jdbc.kvp.IRecord;

/**
 * JdbcApp的演示 demo
 * 
 * @author gbench
 *
 */
public interface IJdbcApp {

	/**
	 * 获取AppName
	 * 
	 * @return 获取AppName
	 */
	default String getAppName() {
		return this.getClass().getSimpleName();
	}

	/**
	 * NamedSql_Processor Preprocessor (NSPP) SqlPatternPreprocessor 的处理类
	 * 
	 * @param sqlfile sql 文件的名称,可以为null,但会得到一个不做任何语句处理的spp:SqlPatternPrePreocessor
	 * @param clazz   sqlfile 相对基路径
	 * @return SqlPatternPreprocessor
	 */
	static SqlPatternPreprocessor nspp(String sqlfile, Class<?> clazz) {
		return namedsql_processor(namedsqls(sqlfile, clazz));
	}

	/**
	 * NamedSql_Processor Preprocessor (NSPP) SqlPatternPreprocessor 的处理类
	 * 
	 * @param sqlfile sql 文件的名称,可以为null,但会得到一个不做任何语句处理的spp:SqlPatternPrePreocessor
	 * @return SqlPatternPreprocessor
	 */
	static SqlPatternPreprocessor nspp(String sqlfile) {
		return nspp(sqlfile, null);
	}

	/**
	 * Named Sql Preprocessor Escaped Brace (NSPEB) SqlPatternPreprocessor 的处理类 对
	 * ‘{’进行转义，以以提供Neo4j类型的数据库处理。
	 * 
	 * @param sqlfile sql文件的名称，可以为null,但会得到一个不做任何语句处理的spp:SqlPatternPrePreocessor
	 * @return SqlPatternPreprocessor
	 */
	static SqlPatternPreprocessor nspeb(final String sqlfile) {
		return nspeb(sqlfile, null);
	}

	/**
	 * Named Sql Preprocessor Escaped Brace (NSPEB) SqlPatternPreprocessor 的处理类 对
	 * ‘{’进行转义，以以提供Neo4j类型的数据库处理。
	 * 
	 * @param sqlfile sql文件的名称，可以为null,但会得到一个不做任何语句处理的spp:SqlPatternPrePreocessor
	 * @param clazz   sqlfile 相对基路径
	 * @return SqlPatternPreprocessor
	 */
	static SqlPatternPreprocessor nspeb(final String sqlfile, final Class<?> clazz) {

		final var spp = namedsql_processor_escape_brace(namedsqls(sqlfile, clazz)); //
		final var status = IRecord.REC("times", 0);// 状态数据
		return new SqlPatternPreprocessor() { // 构造匿名类并 指定 SqlPatternPreprocessor 的名称

			@Override
			public String handle(Method method, IRecord params, String sqlpattern, Jdbc jdbc) { // 默认的处理
				status.compute("times", (Integer v) -> v == null ? 0 : v + 1); // 统计状态数据
				return spp.handle(method, params, sqlpattern, jdbc);
			}

			@Override
			public String name() { // 指定名称
				return "namedsql_processor_escape_brace";
			}

			@Override
			public synchronized IRecord status() {
				return status;
			}

			@Override
			public String toString() {
				return name();
			}

		};
	}

	/**
	 * 创建数据库接口实例 注意：当sql文件不存在的啥时候，返回一个不含有任何匀速的 空Map，即new HashMap<String,String>() 的
	 * spp.
	 * 
	 * @param <T>     数据库类型
	 * @param sqlfile sql语句脚本。 可以为null,但会得到一个不做任何语句处理的spp:SqlPatternPrePreocessor
	 * @param dbClazz 数据库类型
	 * @return 数据库接口实例
	 */
	static <T> T newNsppDBInstance(String sqlfile, Class<T> dbClazz) {
		return newDBInstance(() -> nspp(sqlfile, dbClazz), dbClazz);
	}

	/**
	 * 创建数据库接口实例 注意：当sql文件不存在的啥时候，返回一个不含有任何匀速的 空Map，即new HashMap<String,String>() 的
	 * spp.
	 * 
	 * @param <T>                  数据库类型
	 * @param sqlfile              sql语句脚本。
	 *                             可以为null,但会得到一个不做任何语句处理的spp:SqlPatternPrePreocessor
	 * @param dbClazz              数据库类型
	 * @param sqlFileRelativeClass sqlfile 相对基路径
	 * @return 数据库接口实例
	 */
	static <T> T newNsppDBInstance(String sqlfile, Class<T> dbClazz, Class<?> sqlFileRelativeClass) {
		return newDBInstance(() -> nspp(sqlfile, sqlFileRelativeClass), dbClazz);
	}

	/**
	 * NamedSqlpreProcessor Escaped Brace 创建数据库接口实例
	 * 
	 * @param <T>     数据库类型
	 * @param sqlfile sql语句脚本,可以为null,但会得到一个不做任何语句处理的spp:SqlPatternPrePreocessor
	 * @param dbClazz 数据库类型
	 * @return 数据库接口实例
	 */
	static <T> T newNspebDBInstance(String sqlfile, Class<T> dbClazz) {
		return newDBInstance(() -> nspeb(sqlfile, dbClazz), dbClazz);
	}

	/**
	 * NamedSqlpreProcessor Escaped Brace 创建数据库接口实例
	 * 
	 * @param <T>                  数据库类型
	 * @param sqlfile              sql语句脚本,可以为null,但会得到一个不做任何语句处理的spp:SqlPatternPrePreocessor
	 * @param dbClazz              数据库类型
	 * @param sqlFileRelativeClass sqlfile 相对基路径
	 * @return 数据库接口实例
	 */
	static <T> T newNspebDBInstance(String sqlfile, Class<T> dbClazz, Class<?> sqlFileRelativeClass) {
		return newDBInstance(() -> nspeb(sqlfile, sqlFileRelativeClass), dbClazz);
	}

	/**
	 * 创建数据库接口实例
	 * 
	 * @param <T>          数据库类型
	 * @param spp_supplier SqlPatternPreprocessor Supplier
	 * @param dbClazz      数据库类型
	 * @return 数据库接口实例
	 */
	static <T> T newDBInstance(Supplier<SqlPatternPreprocessor> spp_supplier, Class<T> dbClazz) {
		return Jdbc.newInstance(dbClazz, spp_supplier.get());
	}

	/**
	 * 命名SQL文件（namedsql file）是一个 sql 脚本文件 只是 这个 sql脚本 包含有特别的 用于说明 语句结构的 文法注释。<br>
	 * 一个 namedsql file 是如下的结构： <br>
	 * 其中 语句名称用 # 空格 ‘语句名称’ 的形式。 而不带#与 名称之间不带有空格的 将被视作一般注释。给予忽略。<br>
	 * 即他会按照如下进行解释：<br>
	 * 命名SQL文件的解析，类似于如下的格式。需要注意的#后面必须带有空格，才可以作为 sql语句的名称<br>
	 * 需要注意：‘# count1’ 才是sql语句的名称，而‘#name: 用户名称’ 则是参数的说明。区别在于<br>
	 * count1与# 之间有一个 空格。而name 与#之间没有 <br>
	 * 
	 * -- -------------------------- <br>
	 * -- # count1 <br>
	 * -- name: 用户名称 <br>
	 * -- --------------------------<br>
	 * select count(*) from t_user <br>
	 * where name = #name <br>
	 *
	 * -- --------------------------<br>
	 * -- # count2 -- --------------------------<br>
	 * select count(*) from t_user;<br>
	 *
	 * -- --------------------------<br>
	 * -- # insertUser <br>
	 * -- -------------------------- <br>
	 * insert into ##tableName (name,password,sex,address,birth,phonenumber,email)
	 * <br>
	 * values (#name,#password,#sex,#address,#birth,#phonenumber,#email) <br>
	 * 
	 * @param lines 命名sql 的行的数据流
	 * @return {name->sql},当sql文件不存在的啥时候，返回一个不含有任何匀速的 空Map，即new
	 *         HashMap<String,String>()
	 */
	static Map<String, String> namedsqls(final Stream<String> lines) {
		final var namedsqls = parse2namedsqls(lines);
		return namedsqls;
	}

	/**
	 * 相对于clazz的存在位置的sqlfile的namedsql
	 * 
	 * @param sqlfile sql 文件的名称，当sql文件不存在的啥时候，返回一个不含有任何匀速的 空Map，即new
	 *                HashMap<String,String>()
	 * @param clazz   sqlfile 相对基路径
	 * @return {name->sql},当sql文件不存在的啥时候，返回一个不含有任何匀速的 空Map，即new
	 *         HashMap<String,String>()
	 */
	static Map<String, String> namedsqls(final String sqlfile, final Class<?> clazz) {
		if (sqlfile == null)
			return new HashMap<String, String>();// 返回一个空的namedsqls
		final var lineS = FileSystem.readLineS(new File(FileSystem.path(sqlfile, clazz)), "utf8");
		if (lineS == null)
			return new HashMap<String, String>();// 返回一个空的namedsqls
		return namedsqls(lineS);
	}

	/**
	 * 加载EXCEL文件
	 * 
	 * @param excelFile     excel文件名称
	 * @param relativeClass excelFile的相对位置：以class的文件为基准
	 * @return SimpleExcel 对象
	 */
	static SimpleExcel loadExcel(final String excelFile, final Class<?> relativeClass) {
		return SimpleExcel.of(excelFile);
	}

	/**
	 * 自动检测excel中的数据区域内容
	 * 
	 * @param excelFile     excel文件名称
	 * @param sheetName     sheet名称
	 * @param relativeClass excelFile的相对位置：以class的文件为基准
	 * @return StrMatrix 对象
	 */
	static StrMatrix xlsAutoDetect(final String excelFile, String sheetName, final Class<?> relativeClass) {
		final var excel = loadExcel(excelFile, relativeClass);
		final var strmx = excel.autoDetect(sheetName);
		excel.close();
		return strmx;
	}

	/**
	 * 自动检测excel中的数据区域内容
	 * 
	 * @param excelFile     excel文件名称
	 * @param shtid         sheet的编号从 0开始
	 * @param relativeClass excelFile的相对位置：以class的文件为基准
	 * @return StrMatrix 对象
	 */
	static StrMatrix xlsAutoDetect(final String excelFile, int shtid, final Class<?> relativeClass) {
		final var excel = loadExcel(excelFile, relativeClass);
		final var strmx = excel.autoDetect(shtid);
		excel.close();
		return strmx;
	}

	/**
	 * 自动检测excel中的数据区域内容
	 * 
	 * @param sheetAddress  path!sheetName,当没有sheetName默认为第一号sheet
	 *                      例如：clinics.xlsx!cytokines
	 * @param relativeClass excelFile的相对位置：以class的文件为基准
	 * @return StrMatrix 对象
	 */
	static StrMatrix xlsAutoDetect(final String sheetAddress, final Class<?> relativeClass) {
		final var ss = sheetAddress.split("[!]+");// 分解出地址内容:[0] 文件路径,[1]:sheetName
		final var excelFile = ss[0];
		final var excel = loadExcel(excelFile, relativeClass);
		final StrMatrix strmx = ss.length > 1 ? excel.autoDetect(ss[1]) : excel.autoDetect(0);
		excel.close();
		return strmx;
	}

	/**
	 * 自动检测excel中的数据区域内容:并把结果转换 成IRecord类型的 Stream<IRecord>
	 * 
	 * @param sheetAddress  path!sheetName,当没有sheetName默认为第一号sheet
	 *                      例如：clinics.xlsx!cytokines
	 * @param relativeClass excelFile的相对位置：以class的文件为基准
	 * @return IRecord类型的Stream<IRecord> 对象
	 */
	static Stream<IRecord> sht2recs(final String sheetAddress, final Class<?> relativeClass) {
		return xlsAutoDetect(sheetAddress, relativeClass).rowS(IRecord::REC);
	}

	/**
	 * 去除重复数据
	 * 
	 * @param <T>  元素类型
	 * @param <U>  ID类型
	 * @param cc   数据源
	 * @param t2id 唯一值 id
	 * @return 唯一值的数据源流
	 */
	static <T, U> Stream<T> distinct(Collection<T> cc, Function<T, U> t2id) {
		@SuppressWarnings("unchecked")
		final var collector = Collector.of(() -> new AtomicReference<T>(null), (atom, a) -> atom.set((T) a),
				(aa, bb) -> aa.get() == null ? bb : aa);
		final var mm = cc.stream().collect(Collectors.groupingBy(t2id, collector));
		return mm.values().stream().map(AtomicReference::get);
	};
}
