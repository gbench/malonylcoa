package gbench.sandbox.jdbc.finance.acct;

import static gbench.util.data.xls.SimpleExcel.xls;
import static gbench.util.jdbc.Jdbcs.imports;
import static gbench.util.jdbc.kvp.IRecord.REC;

import gbench.util.data.xls.SimpleExcel;
import gbench.util.jdbc.IJdbcApp;
import gbench.util.jdbc.IMySQL;
import gbench.util.jdbc.kvp.DFrame;
import gbench.util.jdbc.kvp.IRecord;

/**
 * 会计对象
 * 
 * @param <SELF> 自身对象
 */
public abstract class AbstractAcct<SELF> {

	/**
	 * 初始化
	 */
	@SuppressWarnings("unchecked")
	public SELF intialize() {
		jdbcApp.withTransaction(imports(e -> datafile.autoDetect(e).collect(DFrame.dfmclc2), tables));
		return (SELF) this;
	}

	/**
	 * 交易/订单头寸
	 */
	enum Position {
		/**
		 * 长头买方
		 */
		LONG,
		/**
		 * 空头卖方
		 */
		SHORT
	}; // 订单头寸

	/**
	 * 会计借贷
	 */
	enum Drcr {
		/**
		 * 借方
		 */
		DR,
		/*
		 * 贷方
		 */
		CR
	}; // 会计借贷

	// 创造一个IJdbcApp接口应用
	final SimpleExcel datafile = xls(
			"f:/slicef/ws/gitws/malonylcoa/src/test/java/gbench/sandbox/jdbc/finance/acct/acct_data.xlsx"); // 数据-源文件
	final String sqlfile = "F:/slicef/ws/gitws/malonylcoa/src/test/java/gbench/sandbox/jdbc/sqls/mysql_test.sql"; // sql文件
	final String db = "myaccts"; // 数据库名
	final String url = String.format("jdbc:h2:mem:%s;MODE=MYSQL;DB_CLOSE_DELAY=-1;database_to_upper=false;", db); // h2连接字符串
	final IRecord h2_rec = REC("url", url, "driver", "org.h2.Driver", "user", "root", "password", "123456"); // h2数据库
	final protected IMySQL jdbcApp = IJdbcApp.newNsppDBInstance(sqlfile, IMySQL.class, h2_rec); // 数据库应用客户端
	final String[] tables = ("t_company,t_product,t_company_product," + "t_coa,t_accts,t_journal,t_bksys,t_acct_policy,"
			+ "t_user,t_order,t_warehouse").split("[,]+"); // 基础数据表
	final String[] stores = "北京,天津,重庆,上海,广州,深圳".split("[,]+"); // 仓库地址
	final String top10 = "select * from ##tbl limit 10"; // 头前10行数据
	final Integer size = 1000; // 模拟生成的数据量,t_order的数据规模
	final Integer batch_size = 10; // 插入数据时候的批次大小

}
