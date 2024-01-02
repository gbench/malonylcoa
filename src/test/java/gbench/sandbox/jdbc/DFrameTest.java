package gbench.sandbox.jdbc;

import static gbench.util.array.INdarray.nats;
import static gbench.util.io.Output.println;
import static gbench.util.jdbc.kvp.IRecord.rb;

import org.junit.jupiter.api.Test;

import gbench.util.jdbc.kvp.DFrame;

/**
 * 数据框测试
 */
public class DFrameTest {

	@Test
	public void foo() {
		final var abc_rb = rb("a,b,c");
		final var dfm = nats(9).cuts(3).collect(DFrame.dfmclc(abc_rb::get));
		println(dfm);
		println(dfm.datas());
		println(dfm.dataS(1).toArray());
		println(dfm.summary(1));
		println(dfm.summary(2));
		println(dfm.summary());
		println(dfm.min("b"), dfm.max("b"), dfm.count("b"), dfm.avg("b"));
		println(dfm.min(1), dfm.max(1), dfm.count(1), dfm.avg(1));
	}

}
