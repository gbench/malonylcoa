# malonylcoa

#### 介绍
丙二酰辅酶A是一种辅酶A衍生物，可用于脂肪酸和聚酮合成，以及胯线粒体膜α-转移酮戊二酸。丙二酰辅酶A是由乙酰辅酶A羧化酶介导的羧化反应生成。葡萄糖代谢也会产生丙二酰辅酶A。它会变构阻断肉碱棕榈酰转移酶1的作用，从而影响长链脂肪酸向线粒体的转移。脂肪酸合酶失活会导致丙二酰辅酶A过量，导致厌食。

#### 软件架构
软件架构说明


#### 安装教程

1.  xxxx
2.  xxxx
3.  xxxx

#### 使用说明

# 启动 jshell
``` batch
set mvn_repos_home=D:/sliced/mvn_repos
jshell --class-path %mvn_repos_home%/gbench/pubchem/malonylcoa/0.0.1-SNAPSHOT/malonylcoa-0.0.1-SNAPSHOT.jar;%mvn_repos_home%/com/mysql/mysql-connector-j/8.3.0/mysql-connector-j-8.3.0.jar;%mvn_repos_home%/com/h2database/h2/2.2.224/h2-2.2.224.jar
```

``` java
// 导入数据库
import gbench.util.array.*;
import gbench.util.data.MyDataApp;
import static gbench.util.lisp.Lisp.*
import static gbench.util.array.INdarray.*
import static gbench.util.data.DataApp.*;
import static gbench.util.data.DataApp.IRecord.REC;
import static gbench.util.data.DataApp.IRecord.rb;
import static gbench.util.data.DataApp.Tuple2.*;
import static gbench.util.function.Functions.*;
import static gbench.util.io.Output.println;
import static gbench.util.math.Maths.*;
import static gbench.util.math.FinanceMaths.*;
import static java.lang.Math.*;
import static gbench.util.math.algebra.Algebras.evaluate;
import static gbench.util.math.algebra.Algebras.analyze;

// 数据库配置,请指定正确的数据库url,driver,user和password
final var mysql_rec = REC("url", "jdbc:mysql://127.0.0.1:3309/hitler?serverTimezone=UTC", "driver", "com.mysql.cj.jdbc.Driver", "user", "root", "password", "123456");
final var h2_rec = REC("url", String.format("jdbc:h2:mem:%s2;MODE=MYSQL;DB_CLOSE_DELAY=-1;database_to_upper=false;", "malonylcoa"), "driver", "org.h2.Driver","user", "root", "password", "123456");

// 查看数据表
final var dataApp = new MyDataApp(mysql_rec);
dataApp.sqldframe("show tables");
dataApp.withTransaction(sess -> sess.setData(sess.sql2pdS("show tables").fmap2(t -> nd(t.toList()))));

// h2数据库
final var dataApp2 = new MyDataApp(h2_rec);
dataApp2.sqldframe2("create table t_user(id int auto_increment,name varchar(128),password varchar(32),phone varchar(64),sex varchar)");
dataApp2.sqldframe2("insert into t_user(name,password,phone,sex) values ('zhangsan','123456','18120751773','man')");
dataApp2.sqldframe("select * from t_user");

// pivotTable
cph(RPTA(nats(2).data(), 10)).map(INdarray::nd2).collect(ndclc()).pivotTable(INdarray::length, nats(10).reverse().head(4).fmap(i -> (Function<INdarray<Integer>, Integer>) nd -> nd.get(i)));

// 泰勒级数
final var mysin = identity(0d).andThen(x -> nats(7).fmap(n -> (n % 2 == 0 ? 1 : -1) * 1d / fact(2 * n + 1) * pow(x, 2 * n + 1)).sum());
final var z1 = nats(10).add(1).fmap(n -> 3.1415926 / n).fmap(mysin);
final var z2 = nats(10).add(1).fmap(n -> 3.1415926 / n).fmap(Math::sin);

// 实际利率
final var pmts = nd(59, 59, 59, 59, 59 + 1250); // 现金流
final var price = 1000; // 现值(价格)
final var formula = identity(0d).andThen(rate -> pmts.fmap((i, pmt) -> pmt * pow(1 + rate, -(1 + i)))).andThen(pvs -> pvs.sum() - price); // 实际利率
final var eps = pow(10, -6); // 最小值
final var eff_rate = bisect(formula, 0d, 1, eps); // 实际利率
println(String.format("eff_rate percent:%.02f%%, real:%s", eff_rate * 100, eff_rate));

// 序列分解
nd(i -> pow(2, i), 10).fmap(n -> nats(n, 2 * n));

// algebra
nats(8).fmap(n -> evaluate("sin x", "x", PI * 2 / 8 * n));
nats(8).fmap(n -> evaluate("sin x ^ 2 + cos x ^ 2", "x", PI * 2 / 8 * n));
analyze("x+(x+2*x+x)+x*sin(x+(x+2*x+x))").simplify(); // 符号计算化简
println(analyze("sin x ^ 2 + cos x ^ 2").dumpAST()); // 解析成语法树
println(analyze("1/(sigma*sqrt(2*pi))*exp(neg(square((x-mu)/sigma)/2))").dumpAST()); // 正态分布概率密度
analyze("sin x ^ 2 + cos x ^ 2").dx(); // 对x求导的微分运算
analyze("sin x ^ 2 + cos x ^ 2").dx().simplify(); // 微分化简:结果0
Stream.iterate(analyze("sin x"), e -> e.derivate()).map(e -> e.simplify()).limit(10).collect(ndclc()); // sin 高阶导数 公式
Stream.iterate(analyze("cos x"), e -> e.derivate()).map(e -> e.simplify()).limit(10).collect(ndclc()); // cos 高阶导数 公式
Stream.iterate(analyze("sin x"), e -> e.derivate()).map(e -> e.eval("x", 0)).limit(10).collect(ndclc()); // sin 高阶导数 数值
Stream.iterate(analyze("cos x"), e -> e.derivate()).map(e -> e.eval("x", 0)).limit(10).collect(ndclc()); // cos 高阶导数 数值

```
# 启动H2控制台 : 在eclipse debugshell
org.h2.tools.Server.createWebServer("-web").start()

#### 参与贡献

1.  Fork 本仓库
2.  新建 Feat_xxx 分支
3.  提交代码
4.  新建 Pull Request


#### 特技

1.  使用 Readme\_XXX.md 来支持不同的语言，例如 Readme\_en.md, Readme\_zh.md
2.  Gitee 官方博客 [blog.gitee.com](https://blog.gitee.com)
3.  你可以 [https://gitee.com/explore](https://gitee.com/explore) 这个地址来了解 Gitee 上的优秀开源项目
4.  [GVP](https://gitee.com/gvp) 全称是 Gitee 最有价值开源项目，是综合评定出的优秀开源项目
5.  Gitee 官方提供的使用手册 [https://gitee.com/help](https://gitee.com/help)
6.  Gitee 封面人物是一档用来展示 Gitee 会员风采的栏目 [https://gitee.com/gitee-stars/](https://gitee.com/gitee-stars/)