package gbench.sandbox.matrix.array;

import org.junit.jupiter.api.Test;

import static gbench.util.array.INdarray.pts;
import static gbench.util.array.INdarray.is_modrem;
import static gbench.util.array.INdarray.nd;
import static gbench.util.array.INdarray.ndclc;
import static gbench.util.array.Partitioner.P2;
import static gbench.util.function.Functions.identity;
import static gbench.util.io.Output.println;
import static gbench.util.lisp.IRecord.A;
import static gbench.util.lisp.Lisp.RPTA;
import static gbench.util.lisp.Lisp.cph;
import static gbench.util.lisp.Tuple2.bifun;
import static gbench.util.type.Types.Bytes2Bool;
import static gbench.util.type.Types.Bytes2Int;
import static gbench.util.type.Types.Bytes2Str;
import static gbench.util.type.Types.Bytes2Dbl;
import static gbench.util.type.Types.Bytes2Char;
import static gbench.util.type.Types.int2Bytes;
import static gbench.util.type.Types.str2Bytes;
import static gbench.util.type.Types.dbl2Bytes;
import static gbench.util.type.Types.bool2Bytes;
import static gbench.util.type.Types.char2Bytes;

import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import gbench.util.array.INdarray;
import gbench.util.lisp.IRecord;
import gbench.util.lisp.Tuple2;
import gbench.util.math.algebra.Algebras;
import gbench.util.matrix.Matrix;

public class Ndarray3Test {

	@Test
	public void foo() {
		final var ndata = INdarray.of(i -> Byte.valueOf((byte) (int) -i), 256);
		println(ndata);
		final var user_struct = ndata.struct( //
				"name", 32, "sex", 1, "age", 4, "weight", 64, "grade", 2, //
				"address", P2("city", 16, "district", 16, "street", 16));
		Stream.of("address.city,address.street".split(",")).map(user_struct).forEach(e -> {
			println(e, "--------->", e.start(), e.end());
		});
		println("----", user_struct.get("age"));
		println("age", user_struct.get("age").assign(int2Bytes(34)).arrayOf(Bytes2Int));
		println("sex", user_struct.get("sex").assign(bool2Bytes(false)).arrayOf(Bytes2Bool));
		println("name", user_struct.get("name").assign(str2Bytes("zhangsan")).arrayOf(Bytes2Str));
		println("weight", user_struct.get("weight").assign(dbl2Bytes(63.5)).arrayOf(Bytes2Dbl));
		println("grade", user_struct.get("grade").assign(char2Bytes('A')).arrayOf(Bytes2Char));
		println("address.city", user_struct.get("address.city").assign(str2Bytes("shanghai/")).arrayOf(Bytes2Str));
		println("address.district",
				user_struct.get("address.district").assign(str2Bytes("changning/")).arrayOf(Bytes2Str));
		println("address.street", user_struct.get("address.street").assign(str2Bytes("fahuazhen/")).arrayOf(Bytes2Str));
		println("address", user_struct.get("address").arrayOf(Bytes2Str));
		println("----");
		println(ndata);
	}

	@Test
	public void bar() {
		final var mx = Matrix.of((i, j) -> i * 5 + j, 5, 5);
		println(mx);
		final var ndd = Stream.iterate(2, i -> i + mx.nrow()).limit(mx.ncol()) //
				.map(i -> INdarray.of(mx.data(), i, i + 1)) //
				.collect(INdarray.ndclc());
		println(ndd);

		println("----------------------------------------------------");
		println(mx.vcol(1));

		mx.ndcols().forEach(col -> {
			col.get(0).set(0, 1111);
			println(col);
		});
		println("----------------------------------------------------");
		mx.ndrows().forEach(row -> {
			println(row);
		});
	}

	@Test
	public void quz() {
		final var nd = INdarray.of(i -> i, 50).subarray(10);
		nd.assign((_d, i, t) -> _d.set(i, -1 * t));
		println(nd);
		println(nd.slidings(3, 3).assign((d, i, t) -> t.set(-t.get())));
		println(nd);
	}

	@Test
	public void dd() {
		final var a = INdarray.of(i -> i, 10);
		final var b = a.dupdata();

		println(b);
		println(a.subset(8, 2, 9, 1).assign((t) -> t.set(t.get() * -1)));
		println(a);
		println(b.slidings(2));
		a.subset();

		println(a.assign2(b, (x, y) -> x * y));
		println(a.assign2(b, (x, y) -> x * y).sum());
	}

	@Test
	public void d1() {
		final var days = nd(i -> i + 1, 370);
		final var mons = A(0, 31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31);
		nd(mons).scanls(INdarray::sum).slidings(1, 2, true).map(Tuple2::from).map(bifun(days::create))
				.map(Tuple2.snb(0)).forEach(e -> {
					println(e);
				});
		println(nd(i -> i, 10).cuts(3, true));
	}

	/**
	 * 切割
	 */
	@Test
	public void d2() {
		final var nd = nd(i -> i, 10);
		// 标砖长度切割
		println(nd.cuts(3).arrayOf(identity()));
		// 标砖长度切割的你操作
		println(nd.cuts(3).fflat(identity()));
		// 任意尺度切割
		println(nd.trieTree(3, 2, 4).leafS(e -> e.attrval(INdarray.ndint)).collect(ndclc()));
		// 任意尺度切割
		println(INdarray.from(0, 3, 2, 4).scanls(INdarray::length).slidings(1, 2, true).map(Tuple2::from)
				.map(Tuple2.bifun(nd::create)).collect(ndclc()));

	}

	/**
	 * 切割
	 */
	@Test
	public void d3() {
		final var nd = INdarray.of(i -> i * 0.1, 100);
		nd.entries().assign((i, t) -> t.set(Math.sin(i)));
		println(nd.trieTree(3, 3, 3).nodeS() //
				.filter(e -> e.getLevel() == 1) // 提取第一层
				.map(e -> e.attrval()).collect(INdarray.ndclc()));
		nd.subarray(1).entries().forEach(e -> {
			println(e.create(e.start() - 1, Math.min(e.start() + 2, e.datalen())));
		});
	}

	@Test
	public void d4() {
		final var data = INdarray.of(i -> i, 100); // 原始数据
		println(data.cuts(10).map(Object::toString).collect(Collectors.joining("\n"))); // 矩阵化显示
		println();
		println(data.subset(is_modrem(10, 9)).assign(pts(t -> -t))); // 提取矩阵的指定列
		println();
		println(data); // 数据输出
	}

	@Test
	public void d5() {
		final var data = INdarray.of(i -> i, 10); // 原始数据
		println(data.cuts(3, true).get(1)); // 第2行
		println(data.subset(is_modrem(3, 1)).assign(pts(t -> 10 * t))); // 第2列
		println(data.dupdata());
	}

	@Test
	public void d6() {

		/**
		 * 格式化器:连接器
		 */
		class Join {
			public Join(final String format, final String delim) {
				this.format = format;
				this.delim = delim;
			}

			public <K, V> String format(final Map<K, V> mm) {
				return mm.entrySet().stream().map(q -> IRecord.FT(format, q.getKey(), q.getValue())).collect(ndclc())
						.join(delim);
			}

			final String format;
			final String delim;
		}

		final var mul = new Join("pow($0,$1)", "*");
		final var add = new Join("$1*$0", "+");
		final var expression = cph(RPTA(A(1, 2, 3), 3)).map(INdarray::nd2) // (1+2+3)^3的展开式
				.map(e -> e.sorted()).collect(ndclc())
				.groupBy(e -> e.groupBy(identity(), INdarray::length, mul::format), INdarray::length, add::format);
		println(expression);
		final var result = Algebras.evaluate(expression);
		println(result);
	}

}
