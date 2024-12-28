package gbench.util.math.algebra.op;

import static gbench.util.math.algebra.tuple.MyRecord.REC;
import static gbench.util.math.algebra.tuple.Tuple2.P;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import gbench.util.math.algebra.symbol.Node;
import gbench.util.math.algebra.tuple.IRecord;
import gbench.util.math.algebra.tuple.Tuple2;

/**
 * 运算工厂类<br>
 * 
 * 一系列便捷操作的辅助函数 <br>
 * 
 * @author gbench
 *
 */
public class Ops {

	/**
	 * 可有空的ADD算符运算
	 * 
	 * @param <T>
	 * @param <U>
	 * @param t
	 * @param u
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public static <T, U> BinaryOp<T, U> ADD_NULLABLE(T t, U u) {
		// 借用filter实现过滤，用 reduce 实现 加法
		final var op = Stream.of(t, u).filter(Objects::nonNull).reduce((a, b) -> Ops.ADD(a, b).simplify()).orElse(null);
		return (BinaryOp<T, U>) op;
	}

	public static <T, U> BinaryOp<T, U> ADD(T t, U u) {
		return new Add<>(t, u);
	}

	public static <T, U> BinaryOp<T, U> SUB(T t, U u) {
		return new Subtract<>(t, u);
	}

	public static <T, U> BinaryOp<T, U> MINUS(T t, U u) {
		return new Minus<>(t, u);
	}

	public static <U> BinaryOp<Double, U> MINUS(U u) {
		return new Minus<>(0d, u);
	}

	public static <T, U> BinaryOp<T, U> MUL(T t, U u) {
		return new Mul<>(t, u);
	}

	public static <T, U> BinaryOp<T, U> DIV(T t, U u) {
		return new Div<>(t, u);
	}

	public static <T, U> BinaryOp<T, U> POW(T t, U u) {
		return new Pow<>(t, u);
	}

	public static <T, U> BinaryOp<T, U> COMMA(T t, U u) {
		return new Comma<>(t, u);
	}

	public static <T, U> BinaryOp<T, U> LOG(T t, U u) {
		return new Log<>(t, u);
	}

	public static <T> UnaryOp<T> SIN(T t) {
		return new Sin<>(t);
	}

	public static <T> UnaryOp<T> SINH(T t) {
		return new Sinh<>(t);
	}

	public static <T> UnaryOp<T> CSC(T t) {
		return new Csc<>(t);
	}

	public static <T> UnaryOp<T> COS(T t) {
		return new Cos<>(t);
	}

	public static <T> UnaryOp<T> COSH(T t) {
		return new Cosh<>(t);
	}

	public static <T> UnaryOp<T> SEC(T t) {
		return new Sec<>(t);
	}

	public static <T> UnaryOp<T> TAN(T t) {
		return new Tan<>(t);
	}

	public static <T> UnaryOp<T> COT(T t) {
		return new Cot<>(t);
	}

	public static <T> UnaryOp<T> NEG(T t) {
		return new Neg<>(t);
	}

	public static <T> UnaryOp<T> EXP(T t) {
		return new Exp<>(t);
	}

	public static <T> UnaryOp<T> ID(T t) {
		return new Identity<>(t);
	}

	public static <T> UnaryOp<T> LN(T t) {
		return new Ln<>(t);
	}

	public static <T> UnaryOp<T> SQRT(T t) {
		return new Sqrt<>(t);
	}

	public static <T> UnaryOp<T> SQUARE(T t) {
		return new Square<>(t);
	}

	public static <T> UnaryOp<T> FACT(T t) {
		return new Fact<>(t);
	}

	public static <T> UnaryOp<T> FACT2(T t) {
		return new UnaryOp<>("!", t);
	}

	public static <T, U> BinaryOp<T, U> POW2(T t, U u) {
		return BinaryOp.of("^", t, u);
	}

	public static <T, U> BinaryOp<T, U> EXPA(T t, U u) {
		return BinaryOp.of("expa", t, u);
	}

	public static <T, U> UnaryOp<T> ARCSIN(T t) {
		return new Arcsin<>(t);
	}

	public static <T, U> UnaryOp<T> ARCCOS(T t) {
		return new Arccos<>(t);
	}

	public static <T, U> UnaryOp<T> ARCTAN(T t) {
		return new Arctan<>(t);
	}

	public static <T, U> UnaryOp<T> ARCCOT(T t) {
		return new Arccot<>(t);
	}

	public static Token TOKEN(final String token) {
		return new Token(token);
	}

	public static Token TOKEN(final Number token) {
		return new Token(token);
	}

	/**
	 * 根据运算名称查找运算对象
	 * 
	 * @param name 运算对象的名称
	 * @return 当name所指定的运算不存在的时候，返回null
	 */
	@SuppressWarnings("unchecked")
	public static BinaryOp<Object, Object> of(String name) {
		return (BinaryOp<Object, Object>) registry.get(name);
	}

	/**
	 * 根据运算名称查找运算对象
	 * 
	 * @param name 运算对象的名称
	 * @return optional 的 运算对象
	 */
	public static Optional<BinaryOp<Object, Object>> lookup(String name) {
		return Optional.ofNullable(of(name));
	}

	/**
	 * 运算的优先级比较
	 * 
	 * @param op1 运算符1
	 * @param op2 运算符2
	 * @return 优先级的比较结果
	 */
	public final static int priority_cmp(final String op1, final String op2) {
		final var n = priorities.keys().stream().map(priorities::get) // 提取最大的优先级数
				.collect(Collectors.summarizingInt(e -> (Integer) e)).getMax() + 1;

		return priorities.i4(op1, 10) - priorities.i4(op2, n);
	}

	/**
	 * 运算的优先级比较
	 * 
	 * @param op1 运算符1
	 * @param op2 运算符1
	 * @return
	 */
	public final static int priority_cmp(final Node op1, final Node op2) {
		return priority_cmp(op1.getName(), op2.getName());
	}

	/**
	 * 索引化生成器
	 * 
	 * @param <T>
	 * @return
	 */
	public final static <T> Function<T, Tuple2<Integer, T>> kvp_int() {
		return kvp_int(0, e -> e);
	}

	/**
	 * 索引化生成器
	 * 
	 * @param <T>
	 * @param start 开始位置
	 * @return
	 */
	public final static <T> Function<T, Tuple2<Integer, T>> kvp_int(final Number start) {
		return kvp_int(start, e -> e);
	}

	/**
	 * 索引化生成器
	 * 
	 * @param <T>
	 * @param <K>
	 * @param start         开始位置
	 * @param key_generator 索引生成器
	 * @return
	 */
	public final static <T, K> Function<T, Tuple2<K, T>> kvp_int(final Number start,
			final Function<Integer, K> key_generator) {
		final var atom = new AtomicInteger(start.intValue());// 状态缓存：用于生成序号
		return t -> P(key_generator.apply(atom.getAndIncrement()), t);
	}

	/**
	 * flat_mapper
	 */
	public final static Function<Object, Stream<?>> flat_mapper = o -> Optional.of(o).map(e -> {
		if (e instanceof Collection) {
			return ((Collection<?>) e).stream();
		} else if (e instanceof Iterable) {
			return StreamSupport.stream(((Iterable<?>) e).spliterator(), true);
		} else {
			return Stream.of(e);
		}
	}).get();

	/**
	 * 简单的打印输出函数(自带有换行)
	 * 
	 * @param <T>  元素类型
	 * @param objs 打印输入的数据，数据值之间采用"\t"分隔符 进行分隔
	 * @return 打印输出的数内容
	 */
	@SafeVarargs
	public static <T> String println(final T... objs) {

		final var line = print(objs);
		if (debug)
			System.out.println();

		return line;
	}

	/**
	 * 简单的打印输出函数(不自带有换行)
	 * 
	 * @param objs 打印输入的数据，数据值之间采用"\t"分隔符 进行分隔
	 * @return 打印输出的数内容
	 */
	public static String print(final Object... objs) {

		final var line = Arrays.stream(objs).map(e -> {
			if (e instanceof Object[][]) { // 二维数组
				final var oo = (Object[][]) e;
				return Arrays.stream(oo).map(Arrays::deepToString).collect(Collectors.joining("\n"));
			}
			if (e instanceof Object[]) { // 一维数组
				return Arrays.deepToString((Object[]) e);
			} else { // 其他数据
				return e + "";
			}
		}).collect(Collectors.joining("\t")).replaceAll("\n\\s+", "\n"); // 把换行符之后的空格给取消掉,即顶格显示

		if (debug)
			System.out.print(line);

		return line;
	}

	/**
	 * 算符的优先级定义列表,算符的值越高表示其优先级越高,比如 “*”的值是2>“+”的值1,即 乘法运算 优先于 加法运算
	 */
	public static final IRecord priorities = REC("*", 2, "/", 2, "+", 1, "-", 1, ",", 0); // 优先级表, 逗号优先级最低

	/**
	 * 内置的函数库列表,每个内置函数 有一个字符串的名字做 key标记 其 值是一个 BinaryOP 或是 UnaryOp ,<br>
	 * UnaryOp 是一种特殊 的 BinaryOP 即 right 参数 或者说 第二个参数 为null 的 BinaryOP
	 */
	private static Map<String, BinaryOp<?, ?>> registry = new HashMap<>();

	public static boolean debug = false; // 调试标记

	/**
	 * registry 的 初始化过程, 注册系统函数
	 */
	static { // 注册系统函数
		Stream.of(ADD(null, null), SUB(null, null), MINUS(null, null), MINUS(null), MUL(null, null), DIV(null, null),
				POW(null, null), COMMA(null, null), LOG(null, null), SIN(null), SINH(null), CSC(null), COS(null),
				COSH(null), SEC(null), TAN(null), COT(null), NEG(null), EXP(null), ID(null), LN(null), SQRT(null),
				SQUARE(null), FACT(null), FACT2(null), POW2(null, null), EXPA(null, null), ARCSIN(null), ARCCOS(null),
				ARCTAN(null), ARCCOT(null)).forEach(e -> {
					registry.put(e.getName(), e); // 注册函数
				}); // forEach
	} // static

}