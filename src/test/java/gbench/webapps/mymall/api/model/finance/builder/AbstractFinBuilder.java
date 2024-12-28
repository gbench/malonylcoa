package gbench.webapps.mymall.api.model.finance.builder;

import static gbench.util.data.xls.SimpleExcel.xls;
import static gbench.util.jdbc.Jdbcs.imports;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import gbench.global.Globals;
import gbench.util.jdbc.IJdbcApp;
import gbench.util.jdbc.IMySQL;
import gbench.util.jdbc.kvp.DFrame;
import gbench.util.jdbc.kvp.IRecord;

/**
 * 抽象构建器
 * 
 * @param <S> 本身类：具体的构建器类型
 * @param <T> 构建器的构建对象类型
 */
public abstract class AbstractFinBuilder<S, T extends AbstractFinBase<T>> {
	/**
	 * AbstractBuilder
	 * 
	 * @param jdbcApp 数据库
	 * @param params  构建参数
	 */
	public AbstractFinBuilder(final IMySQL jdbcApp, final IRecord params) {
		this.jdbcApp = jdbcApp;
		this.params = params;
	}

	/**
	 * AbstractBuilder
	 * 
	 * @param jdbcApp 数据库
	 */
	public AbstractFinBuilder(final IMySQL jdbcApp) {
		this(jdbcApp, IRecord.REC());
	}

	/**
	 * AbstractBuilder
	 * 
	 * @param jdbcApp 数据库
	 */
	public AbstractFinBuilder(final String datafile, final String sqlfile) {
		this(jdbcApp(datafile, sqlfile), IRecord.REC());
	}

	/**
	 * 默认构建
	 */
	public AbstractFinBuilder() {
		this(datafile, sqlfile);
	}

	/**
	 * 本身元素
	 * 
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public S self() {
		return (S) this;
	}

	/**
	 * 添加元素
	 * 
	 * @param kvs
	 * @return
	 */
	public S add(final Object... kvs) {
		this.params.add(kvs);
		return this.self();
	}

	/**
	 * 创建一个 Finvent 对象
	 * 
	 * @param companyId 策略名称
	 * @return Finvent 对象
	 */
	public abstract T build();

	/**
	 * 生成指定的类型的构建器
	 * 
	 * @param <S>
	 * @param clazz
	 * @return
	 */
	public static <S> S of(final Class<S> clazz) {
		S s = null;
		try {
			s = clazz.getConstructor().newInstance();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return s;
	}

	/**
	 * jdbcApp
	 * 
	 * @return 财务会计数据库
	 */
	public static IMySQL jdbcApp(final String datafile, final String sqlfile) {
		final var key = String.format("%s%s", datafile, sqlfile); // 分析key

		return jdbcApps.computeIfAbsent(key, k -> {
			// 共享静态变量便于测试应用进行本地化修改与测试:建立一个静态块给予重新赋值就可以了
			final String db = "myacct"; // 数据库名
			final var datafileXls = xls(datafile); // 数据-源文件
			final var url = String.format("jdbc:h2:mem:%s;mode=mysql;db_close_delay=-1;database_to_upper=false;", db); // h2连接字符串
			final var h2_rec = gbench.util.jdbc.kvp.IRecord.REC("url", url, "driver", "org.h2.Driver", "user", "root",
					"password", "123456"); // h2数据库
			final var jdbcApp = IJdbcApp.newNsppDBInstance(sqlfile, IMySQL.class, h2_rec); // 数据库应用客户端
			final var tables = datafileXls.sheetS().map(e -> e.getSheetName()).toArray(String[]::new); // 基础数据表
			// 数据初始化
			jdbcApp.withTransaction(imports(e -> datafileXls.autoDetect(e).collect(DFrame.dfmclc2), tables));

			return jdbcApp;
		});
	}

	public static final String datafile = Globals.WS_HOME
			+ "/gitws/malonylcoa/src/test/java/gbench/webapps/mymall/api/model/data/acct_data.xlsx";
	public static final String sqlfile = Globals.WS_HOME
			+ "/gitws/malonylcoa/src/test/java/gbench/webapps/mymall/api/model/sqls/acct.sql";
	public static Map<String, IMySQL> jdbcApps = new ConcurrentHashMap<>(); // jdbcs缓存

	final protected IMySQL jdbcApp; // 数据库
	final protected IRecord params; // 构建参数
}
