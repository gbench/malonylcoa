package gbench.sandbox.data;

import org.junit.jupiter.api.Test;

import gbench.util.data.DataApp.IRecord;
import gbench.global.Globals;
import gbench.util.data.MyDataApp;

import static gbench.sandbox.data.pg.Pgdb.*;
import static gbench.util.data.DataApp.Tuple2.P;

/**
 * PgSQLTest
 */
public class PgSQLTest {

	@Test
	public void foo() {
		final var dataApp = new MyDataApp(pgsql_rec);
		final var datafile = Globals.WS_HOME + "/gitws/malonylcoa/src/test/java/gbench/sandbox/data/datafile.xlsx";

		dataApp.withTransaction(sess -> {
			imports("t_company,t_product,t_company_product".split(",")).accept(P(datafile, sess));
		});
	}

	@Test
	public void bar() {
		final var dataApp = new MyDataApp(pgsql_rec);
		final var datafile = Globals.WS_HOME
				+ "/gitws/malonylcoa/src/test/java/gbench/webapps/world/api/model/data/datafile.xlsx";

		dataApp.withTransaction(sess -> {
			imports("t_maozedong,t_wenyan".split(",")).accept(P(datafile, sess));
		});
	}

	final IRecord pgsql_rec = IRecord.REC( //
			"url", "jdbc:postgresql://localhost:5432/latinus?currentSchema=economics", //
			"driver", "org.postgresql.Driver", //
			"user", "postgres", "password", "123456");

}
