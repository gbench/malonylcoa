package gbench.sandbox.data;

import org.junit.jupiter.api.Test;

import gbench.util.data.DataApp.IRecord;
import gbench.util.data.DataApp.Tuple2;
import gbench.util.data.MyDataApp;

import static gbench.sandbox.data.pg.Pgdb.*;

/**
 * PgSQLTest
 */
public class PgSQLTest {

	@Test
	public void foo() {
		final var dataApp = new MyDataApp(pgsql_rec);
		final var datafile = "f:/slicef/ws/gitws/malonylcoa/src/test/java/gbench/sandbox/data/datafile.xlsx";

		dataApp.withTransaction(sess -> {
			imports("t_company,t_product,t_company_product".split(",")).accept(Tuple2.of(datafile, sess));
		});
	}

	final IRecord pgsql_rec = IRecord.REC( //
			"url", "jdbc:postgresql://localhost:5432/latinus?currentSchema=economics", //
			"driver", "com.mysql.cj.jdbc.Driver", //
			"user", "postgres", "password", "123456");

}
