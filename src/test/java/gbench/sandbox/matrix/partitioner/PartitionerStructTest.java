package gbench.sandbox.matrix.partitioner;

import org.junit.jupiter.api.Test;

import static gbench.util.array.INdarray.with_index;
import static gbench.util.array.INdarray.with_key;
import static gbench.util.array.Partitioner.P2;
import static gbench.util.io.Output.println;
import static gbench.util.lisp.IRecord.A;

import java.util.Arrays;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Consumer;
import java.util.stream.Stream;

import gbench.util.array.Tensor;
import gbench.util.array.INdarray;
import gbench.util.array.Partitioner;
import static gbench.util.type.Types.Str2Bytes;
import static gbench.util.type.Types.Bytes2Str;

/**
 * Partitioner 做为数据结构的应用
 * 
 * @author gbench
 *
 */
public class PartitionerStructTest {

	@Test
	public void foo() {
		final var pdef = P2("name", P2("firstname", 10, "lastname", 10), "address",
				P2("province", 5, "city", 4, "street", 3)); // 实现类似于C语言的struct结构, partition define
		final var root = pdef.root(); // 获取根路径
		final var ndata = Tensor.nd(i -> i, pdef); // 申请数据空间

		println("--------------------------------");
		root.forEach(e -> {
			println("#", e._2().paths(), e._2.key(), e._2.evaluate(ndata::create));
		});

		println("--------------------------------");
		println("[]", root.getElem(new String[] {}));
		println("[address,province]", root.getElem(new String[] { "address", "province" }));
		println("--------------------------------");
		final var name = root.getElem("name");
		final var province = root.getElem("address.province");
		println("pdef.evaluate(\"address/province\", ndata::create))",
				pdef.evaluate("address/province", ndata::create));
		println("province.evaluate(ndata::create)", province.evaluate(ndata::create));
		println("pdef.evaluate(\"name.firstname\", ndata::create)", pdef.evaluate("name.firstname", ndata::create));
		println("name.evaluate(ndata::create)", name.evaluate(ndata::create));
		println("--------------------------------");
	}

	@Test
	public void bar() {
		final var pdef = P2("name", P2("firstname", 10, "lastname", 10), "address",
				P2("province", 5, "city", 4, "street", 3)); // 实现类似于C语言的struct结构, partition define
		final var root = pdef.root(); // 获取根路径
		println("[]", root.getElem(new Integer[] {}));
		println("[0]", root.getElem(A(0)));
		println("[0,0]", root.getElem(A(0, 0)));
		println("[0,1]", root.getElem(A(0, 1)));
		println("--------------------------------");
		final var ndata = Tensor.nd(i -> i, pdef); // 申请数据空间
		println("[0,1]", pdef.evaluate(A(0, 1), ndata::create));
		println("[-1,1]", pdef.evaluate(A(-1, 1), ndata::create));
	}

	/**
	 * partitioner 可以实现一个类似于c语言的结构体的结构。这一个编译器的底层数据结构模型。
	 */
	@Test
	public void qux() {

		/**
		 * 基础数据类型定义。
		 */
		int BOOLEAN = 1;
		int BYTE = 1;
		int SHORT = 2;
		int CHAR = 2;
		int INTEGER = 4;
		int LONG = 8;
		int FLOAT = 4;
		int DOUBLE = 8;

		final var user_struct = P2( // 定义用户结构
				/* 0 */"id", INTEGER, //
				/* 1 */"name", P2("firstname", CHAR * 8, "lastname", CHAR * 16), //
				/* 2 */"sex", BOOLEAN, //
				/* 3 */"address", P2("province", CHAR * 32, "city", CHAR * 32, "street", CHAR * 32), //
				/* 4 */"weight", FLOAT, //
				/* 5 */"salary", DOUBLE, //
				/* 6 */"avatar", BYTE * 1024, //
				/* 7 */"grade", SHORT, //
				/* 8 */"birth", LONG //
		);
		final var ts = Tensor.of(new Byte[user_struct.length()]); // 定义数据空间
		final var nd = ts.nd(); // 数据内容放访问器
		final Function<INdarray<Byte>, Consumer<Integer>> nd_init = ndarray -> i -> {
			ndarray.set(i, (byte) (i.byteValue() % 256));
		};
		// 数据空间初始化
		Stream.iterate(0, i -> i < ts.length(), i -> i + 1).forEach(nd_init.apply(nd));
		println("-----------------------------------------");
		println("ts", ts);
		// 结构成员的数据元素:path与索引两种方法访问
		println("-----------------------------------------");
		println("currying");
		println("-----------------------------------------");
		final Function<Partitioner, Function<INdarray<Byte>, Function<String, Optional<INdarray<Byte>>>>> _with_key = p -> ndarray -> key -> {
			return p.evaluate(key, ndarray::create);
		};
		final Function<Partitioner, Function<INdarray<Byte>, Function<Integer[], Optional<INdarray<Byte>>>>> _with_index = p -> ndarray -> index -> {
			return p.evaluate(index, ndarray::create);
		};
		final var with_nd_key = _with_key.apply(user_struct).apply(nd);
		final var with_nd_index = _with_index.apply(user_struct).apply(nd);
		// currying
		println("--id[0]");
		println("id", with_nd_key.apply("id"));
		println("[0]", with_nd_index.apply(A(0)));
		println("--name[1]");
		println("name", with_nd_key.apply("name"));
		println("[1]", with_nd_index.apply(A(1)));
		// 直接调用
		println("--name.lastname[1.1]");
		println("name.lastname", user_struct.evaluate("name.lastname", nd::create));
		println("with_nd_key:name.lastname", with_nd_key.apply("name.lastname"));
		println("with_key:name.lastname", with_key(user_struct, nd).get("name.lastname"));
		println("[1.1]", user_struct.evaluate(A(1, 1), nd::create));
		println("with_nd_index:[1,1]", with_nd_index.apply(A(1, 1)));
		println("with_index:name.lastname", with_index(user_struct, nd).get(A(1, 1)));
		println("--birth[8]");
		println("birth", user_struct.evaluate("birth", nd::create));
		println("with_nd_key:birth", with_nd_key.apply("birth"));
		println("[8]", user_struct.evaluate(A(8), nd::create));
		println("with_nd_index:[8]", with_nd_index.apply(A(8)));
		// 赋值
		println("with_nd_key:name.firstname", with_nd_key.apply("name.firstname"));
		println("with_nd_key:name.firstname", with_nd_key.apply("name.firstname") //
				.map(e -> e.assign(Str2Bytes //
						.andThen(Arrays::asList) //
						.apply("abcdefghijklmnopq"), 3)));
		println("with_nd_key:name.firstname", with_nd_key.apply("name.firstname") //
				.map(e -> e.mutate((INdarray<Byte> s) -> s.arrayOf(Bytes2Str))));
	}

}
