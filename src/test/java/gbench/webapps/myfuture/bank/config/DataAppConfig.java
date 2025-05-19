package gbench.webapps.myfuture.bank.config;

import gbench.util.data.DataApp.DFrame;
import gbench.util.data.MyDataApp;
import gbench.util.data.xls.SimpleExcel;
import gbench.util.lisp.IRecord;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

import static gbench.util.lisp.IRecord.REC;
import static gbench.webapps.myfuture.api.model.DataModel.*;

@Configuration
public class DataAppConfig {

	/**
	 * @return
	 */
	@Bean
	public DataSource dataSource(@Value("${gbench.jdbc.driver:org.h2.Driver}") String driver, //
			@Value("${gbench.jdbc.url:jdbc:h2:mem:hitler;MODE=MYSQL;DB_CLOSE_DELAY=-1;database_to_upper=false;}") String url, //
			@Value("${gbench.jdbc.user:sa}") String user, //
			@Value("${gbench.jdbc.password:}") String password //
	) {
		final var db_rec = IRecord.rb("driver,url,user,password").get(driver, url, user, password);

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
			final @Value("${gbench.datafile:F:/slicef/ws/gitws/malonylcoa/src/test/java/gbench/webapps/world/api/model/data/datafile.xlsx}") String datafile) {

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