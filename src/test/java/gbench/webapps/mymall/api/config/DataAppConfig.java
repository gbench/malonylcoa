package gbench.webapps.mymall.api.config;

import gbench.util.data.DataApp.DFrame;
import gbench.util.data.MyDataApp;
import gbench.util.data.xls.SimpleExcel;
import gbench.util.jdbc.IJdbcApp;
import gbench.util.jdbc.IMySQL;
import gbench.util.lisp.IRecord;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

import org.h2.tools.Server;

import static gbench.util.data.xls.SimpleExcel.xls;
import static gbench.util.jdbc.Jdbcs.imports;
import static gbench.util.lisp.IRecord.REC;
import static gbench.webapps.mymall.api.model.DataModel.*;

@Configuration
public class DataAppConfig {

	/**
	 * 
	 * @param port
	 */
	static void startH2Server(long port) {
		try {
			Server.createTcpServer( // 2. 关键：启动 TCP 服务，允许外部连接
					"-tcpAllowOthers", // 允许外部连接（非 localhost）
					"-tcpPort", String.valueOf(port), // 指定端口
					"-ifNotExists" // 如果服务已存在则不报错
			).start();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * @return
	 */
	@Bean
	public DataSource dataSource(@Value("${gbench.mymall.api.data.jdbc.driver:org.h2.Driver}") String driver, //
			@Value("${gbench.mymall.api.data.jdbc.url:jdbc:h2:mem:hitler;mode=mysql;db_close_delay=-1;database_to_upper=false;}") String url, //
			@Value("${gbench.mymall.api.data.jdbc.user:sa}") String user, //
			@Value("${gbench.mymall.api.data.jdbc.password:}") String password, //
			@Value("${gbench.mymall.api.data.jdbc.port:9092}") long port //
	) {
		final var db_rec = IRecord.rb("driver,url,user,password").get(driver, url, user, password);
		System.out.println("---------------\n[dataSource URL:%s, port:%s]\n-----------------\n".formatted(url, port));
		startH2Server(port);
		return MyDataApp.ds(db_rec.toArray2());
	}

	/**
	 * dataApp
	 * 
	 * @param ds       数据源
	 * @param dbreset  是否重置数据库
	 * @param datafile 数据初始文件excel全名
	 * @return MyDataApp
	 */
	@Bean
	public MyDataApp dataApp(final DataSource ds, final @Value("${gbench.dbreset:true}") Boolean dbreset,
			final @Value("${gbench.datafile:F:/slicef/ws/gitws/malonylcoa/src/test/java/gbench/webapps/mymall/api/model/data/datafile.xlsx}") String datafile) {

		final var dataApp = new MyDataApp(ds);
		if (dbreset) {
			final var excel = SimpleExcel.of(datafile);
			for (final var sheet : excel.sheets()) {
				final var tblname = sheet.getSheetName();
				final var dfm = excel.autoDetect(tblname).collect(DFrame.dfmclc2);
				final var proto = dfm
						.maxBy2((String a, String b) -> b == null || String.valueOf(a).length() > b.length()); // 提取最大长度
				final var ctsql = ctsql(tblname, proto);
				final var insql = insql(tblname, dfm);
				final var qrysql = String.format("select * from %s", tblname);
				println(ctsql);
				println(insql);
				dataApp.sqlmaybe2(ctsql);
				dataApp.sqlmaybe2(insql);
				println(dataApp.sqldframe(qrysql));
			}
		}

		return dataApp;
	}

	/**
	 * jdbcApp
	 * 
	 * @return 财务会计数据库
	 */
	@Bean
	public IMySQL jdbcApp(@Value("${gbench.mymall.api.finance.acct.jdbc.driver:org.h2.Driver}") String driver, //
			@Value("${gbench.mymall.api.finance.acct.jdbc.url:jdbc:h2:mem:hitler;mode=mysql;db_close_delay=-1;database_to_upper=false;}") String url, //
			@Value("${gbench.mymall.api.finance.acct.jdbc.user:sa}") String user, //
			@Value("${gbench.mymall.api.finance.acct.jdbc.password:}") String password, //
			@Value("${gbench.mymall.api.finance.acct.jdbc.port:9091}") long port, //
			@Value("${gbench.mymall.api.finance.acct.datafile:F:/slicef/ws/gitws/malonylcoa/src/test/java/gbench/webapps/mymall/api/model/data/acct_data.xlsx}") String datafile,
			@Value("${gbench.mymall.api.finance.acct.sqlfile:F:/slicef/ws/gitws/malonylcoa/src/test/java/gbench/webapps/mymall/api/model/sqls/acct.sql}") String sqlfile) {
		final var datafileXls = xls(datafile); // 数据-源文件
		final var h2_rec = gbench.util.jdbc.kvp.IRecord.REC("url", url, "driver", driver, "user", user, "password",
				password); // h2数据库
		System.out.println("---------------\n[dataSource URL:%s, port:%s]\n-----------------\n".formatted(url, port));
		startH2Server(port);
		final var jdbcApp = IJdbcApp.newNsppDBInstance(sqlfile, IMySQL.class, h2_rec); // 数据库应用客户端
		final var tables = datafileXls.sheetS().map(e -> e.getSheetName()).toArray(String[]::new); // 基础数据表
		jdbcApp.withTransaction(
				imports(e -> datafileXls.autoDetect(e).collect(gbench.util.jdbc.kvp.DFrame.dfmclc2), tables));

		return jdbcApp;
	}

	/**
	 * @param obj
	 */
	public static void println(final Object obj) {
		System.out.println(obj);
	}

	protected final IRecord h2_rec = REC( //
			"url", "jdbc:h2:mem:hitler;MODE=MySQL;DATABASE_TO_UPPER=false;DB_CLOSE_DELAY=-1;", //
			"driver", "org.h2.Driver", //
			"user", "sa", "password", "");
	protected final IRecord mysql_rec = IRecord.REC( //
			"url", "jdbc:mysql://127.0.0.1:3309/hitler?serverTimezone=UTC", //
			"driver", "com.mysql.cj.jdbc.Driver", //
			"user", "root", "password", "123456");

}