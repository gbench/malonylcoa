package gbench;

import gbench.util.array.*;
import gbench.util.data.MyDataApp;

import static gbench.util.lisp.Lisp.*;
import static gbench.util.array.INdarray.*;
import static gbench.util.data.DataApp.IRecord.REC;
import static gbench.util.function.Functions.*;
import static gbench.util.math.Maths.*;
import static java.lang.Math.*;

import java.util.function.Function;

import org.junit.jupiter.api.Test;

public class DemoTest {

	@Test
	public void foo() {

		// 数据库配置,请指定正确的数据库url,driver,user和password
		final var mysql_rec = REC("url", "jdbc:mysql://127.0.0.1:3309/hitler?serverTimezone=UTC", "driver",
				"com.mysql.cj.jdbc.Driver", "user", "root", "password", "123456");
		final var h2_rec = REC("url",
				String.format("jdbc:h2:mem:%s2;MODE=MYSQL;DB_CLOSE_DELAY=-1;database_to_upper=false;", "malonylcoa"),
				"driver", "org.h2.Driver", "user", "root", "password", "123456");

		// 查看数据表
		final var dataApp = new MyDataApp(mysql_rec);
		dataApp.sqldframe("show tables");
		dataApp.withTransaction(sess -> sess.setData(sess.sql2pdS("show tables").fmap2(t -> nd(t.toList()))));

		// h2数据库
		final var dataApp2 = new MyDataApp(h2_rec);
		dataApp2.sqldframe2(
				"create table t_user(id int auto_increment,name varchar(128),password varchar(32),phone varchar(64),sex varchar)");
		dataApp2.sqldframe2(
				"insert into t_user(name,password,phone,sex) values ('zhangsan','123456','18120751773','man')");
		dataApp2.sqldframe("select * from t_user");

		// pivotTable
		cph(RPTA(nats(2).data(), 10)).map(INdarray::nd).map(INdarray::dupdata).collect(ndclc()).pivotTable(
				INdarray::length,
				nats(10).reverse().head(4).fmap(i -> (Function<INdarray<Integer>, Integer>) nd -> nd.get(i)));

		// 泰勒级数
		final var mysin = identity(0d).andThen(
				x -> nats(7).fmap(n -> (n % 2 == 0 ? 1 : -1) * 1d / fact(2 * n + 1) * pow(x, 2 * n + 1)).sum());
		final var z1 = nats(10).add(1).fmap(n -> 3.1415926 / n).fmap(mysin);
		final var z2 = nats(10).add(1).fmap(n -> 3.1415926 / n).fmap(Math::sin);
		z1.sub(z2);

		// 实际利率
		final var pmts = nd(59, 59, 59, 59, 59 + 1250); // 现金流
		final var price = 1000; // 现值(价格)
		final var formula = identity(0d).andThen(rate -> pmts.fmap((i, pmt) -> pmt * pow(1 + rate, -(1 + i))))
				.andThen(pvs -> pvs.sum() - price); // 实际利率
		final var eff_rate = bisect(formula, 0d, 1, pow(10, -6)); // 实际利率
		System.out.println(String.format("eff_rate percent:%.02f%%, real:%s", eff_rate * 100, eff_rate));

		// 序列分解
		nd(i -> pow(2, i), 10).fmap(n -> nats(n, 2 * n));

	}

}