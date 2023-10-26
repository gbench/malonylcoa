package gbench.webapps.world.api.config;

import gbench.util.data.MyDataApp;
import gbench.util.lisp.IRecord;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

import static gbench.util.lisp.IRecord.REC;

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
	 * @param ds
	 * @param dbreset
	 * @param datafile
	 * @return MyDataApp
	 */
	@Bean
	public MyDataApp dataApp(final DataSource ds, final @Value("${gbench.dbreset:true}") Boolean dbreset,
			final @Value("${gbench.datafile:F:/slicef/ws/gitws/gcloud/projs/fumarate/src/test/java/gbench/sandbox/mall/data/data.xlsx}") String datafile) {
		final var dataApp = new MyDataApp(ds);
		return dataApp;
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