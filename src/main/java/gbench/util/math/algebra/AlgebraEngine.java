package gbench.util.math.algebra;

import static gbench.util.math.algebra.BinaryOp.*;
import static gbench.util.math.algebra.Comma.COMMA_TEST;
import static gbench.util.math.algebra.MyRecord.REC;
import static gbench.util.math.algebra.Node.*;
import static gbench.util.math.algebra.Ops.*;
import static gbench.util.math.algebra.Tuple2.TUP2;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Stack;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 二维元组 (t,u) 仅含有两个元素的数据结构，形式简单 但是 内涵极为丰富
 *
 * @param <T> 第一位置 元素类型
 * @param <U> 第而位置 元素类型
 */
class Tuple2<T, U> {

	/**
	 * @param _1 第一位置元素
	 * @param _2 第二位置元素
	 */
	public Tuple2(final T _1, final U _2) {
		this._1 = _1;
		this._2 = _2;
	}

	/**
	 * 返回第一元素
	 *
	 * @return 第一元素
	 */
	public T _1() {
		return this._1;
	}

	/**
	 * 返回 第二元素
	 *
	 * @return 第二元素
	 */
	public U _2() {
		return this._2;
	}

	/**
	 * 数据格式化
	 * 
	 * @return 数据格式化
	 */
	public String toString() {
		return "(" + this._1 + "," + this._2 + ")";
	}

	/**
	 * 构造一个二元组
	 * 
	 * @param _1  第一元素
	 * @param _2  第二元素
	 * @param <T> 第一元素类型
	 * @param <U> 第二元素类型
	 * @return 二元组对象的构造
	 */
	public static <T, U> Tuple2<T, U> TUP2(final T _1, final U _2) {
		return new Tuple2<>(_1, _2);
	}

	protected final T _1; // 第一位置元素
	protected final U _2; // 第二位置元素

}

/**
 * 数据记录对象
 * 
 * @author gbench
 *
 */
interface IRecord {

	/**
	 * 根据键名进行取值
	 * 
	 * @param key 键名
	 * @return 键key的值
	 */
	Object get(final String key);

	/**
	 * 增加新键，若 key 与 老的 键 相同则 覆盖 老的值
	 * 
	 * @param key   新的 键名
	 * @param value 键值
	 * @return 对象本身
	 */
	IRecord add(String key, Object value);

	/**
	 * 数据复制
	 * 
	 * @return 当前对象的拷贝
	 */
	IRecord duplicate();

	/**
	 * 键名序列
	 * 
	 * @return 键名列表
	 */
	List<String> keys();

	/**
	 * 返回一个 Map 结构
	 * 
	 * @return 一个 键值 对儿 的 列表 [(key,map)]
	 */
	Map<String, Object> toMap();

	/**
	 * 返回 key 所对应的 值
	 * 
	 * @param <T>    目标类型
	 * @param key    键名
	 * @param tclass key 所标定的 值 的 类型
	 * @return T 类型的数值
	 */
	@SuppressWarnings("unchecked")
	default <T> T get(final String key, final Class<T> tclass) {
		return (T) this.get(key);
	}

	/**
	 * 返回 key 所对应的 键值, Double 类型
	 * 
	 * @param key 键名
	 * @return key 所标定的 值
	 */
	default Double dbl(String key) {
		return BinaryOp.obj2dbl().apply(this.get(key));
	}

	/**
	 * 返回 key 所对应的 键值, Integer 类型
	 * 
	 * @param key 键名
	 * @return key 所标定的 值
	 */
	default Integer i4(String key) {
		return this.dbl(key).intValue();
	}

	/**
	 * 返回 key 所对应的 键值, Integer 类型
	 * 
	 * @param key          键名
	 * @param defaultValue 默认的值
	 * @return key 所标定的 值
	 */
	default Integer i4(String key, Integer defaultValue) {
		return BinaryOp.obj2dbl(defaultValue).apply(this.get(key)).intValue();
	}

	/**
	 * 返回 key 所对应的 键值, String 类型
	 * 
	 * @param key 键名
	 * @return key 所标定的 值
	 */
	default String str(String key) {
		final var obj = this.get(key);
		return obj instanceof String ? (String) obj : obj + "";
	}

	/**
	 * IRecord 数据的 结构 变换 <br>
	 * 
	 * 把 IRecord 转换 另一种结构的 数据 <br>
	 * 
	 * @param <T>    目标类型结构
	 * @param mapper 结构变换函数 rec->T
	 * @return T 类型的结构
	 */
	default <T> T mutate(final Function<IRecord, T> mapper) {
		return mapper.apply(this);
	}

	/**
	 * 生成 构建器
	 * 
	 * @param keys 键名序列,用逗号分隔
	 * @return keys 为格式的 构建器
	 */
	public static Builder rb(final String keys) {
		return new Builder(Arrays.asList(keys.split(",")));
	}

	/**
	 * 数据滑动<br>
	 * 例如 [1,2,3,4,5] 按照 width=2, step=1 进行滑动 <br>
	 * flag false: [1, 2][2, 3][3, 4][4, 5][5] 如果 flag = true 则 返回 <br>
	 * flag true: [1, 2][2, 3][3, 4][4, 5] <br>
	 * 按照 width=2, step=2 进行滑动 <br>
	 * flag false: [1, 2][3, 4] <br>
	 * flag true: [1, 2][3, 4] <br》
	 * 
	 * @param <T>        数据元素类型
	 * @param collection 数据集合
	 * @param size       窗口大小
	 * @param step       步长
	 * @param flag       是否返回等长记录
	 * @return 滑动窗口列表
	 */
	public static <T> Stream<List<T>> slidingS(final Collection<T> collection, final int size, final int step,
			final boolean flag) {

		final var n = collection.size();
		final var arrayList = collection instanceof ArrayList ? (ArrayList<T>) collection
				: new ArrayList<T>(collection);

		// 当flag 为true 的时候 i的取值范围是: [0,n-size] <==> [0,n+1-size)
		return Stream.iterate(0, i -> i < (flag ? n + 1 - size : n), i -> i + step)
				.map(i -> arrayList.subList(i, (i + size) > n ? n : (i + size)));
	}

	/**
	 * IRecord 构建器
	 * 
	 * @author gbench
	 *
	 */
	class Builder {

		/**
		 * 构造IRecord构造器
		 * 
		 * @param keys 键名列表
		 */
		public Builder(final List<String> keys) {
			this.keys = new ArrayList<>(keys);
		}

		/**
		 * 
		 * @param objs
		 * @return
		 */
		public IRecord get(Object... objs) {
			final int n = objs.length;
			final var size = this.keys.size();
			final Map<String, Object> data = new LinkedHashMap<String, Object>();

			for (int i = 0; i < size; i++) {
				final var key = keys.get(i);
				final var value = objs[i % n];
				data.put(key, value);
			}

			return new MyRecord(data);
		}

		List<String> keys = new LinkedList<String>();
	}
}

/**
 * 数据记录对象的实现
 * 
 * @author gbench
 *
 */
class MyRecord implements IRecord {

	/**
	 * 构造数据记录对象
	 * 
	 * @param data IRecord数据
	 */
	public MyRecord(final Map<String, Object> data) {
		this.data.putAll(data);
	}

	/**
	 * 构造空数据记录
	 * 
	 */
	public MyRecord() {
		this(new LinkedHashMap<String, Object>());
	}

	@Override
	public IRecord add(String key, Object value) {
		this.data.put(key, value);
		return this;
	}

	@Override
	public Map<String, Object> toMap() {
		return this.data;
	}

	@Override
	public List<String> keys() {
		return new ArrayList<>(this.data.keySet());
	}

	/**
	 * 
	 */
	@Override
	public Object get(String key) {
		return this.data.get(key);
	}

	@Override
	public IRecord duplicate() {
		final var _rec = new MyRecord();
		_rec.data.putAll(this.data);
		return _rec;
	}

	/**
	 * 标准版的记录生成器, map 生成的是LinkedRecord
	 * 
	 * @param kvs 键,值序列:key0,value0,key1,value1,....
	 * @return IRecord对象
	 */
	static IRecord REC(final Object... kvs) {
		final var n = kvs.length;
		final var data = new LinkedHashMap<String, Object>();

		if (n == 1) { // 单一参数情况
			final var obj = kvs[0];
			if (obj instanceof Map) { // Map情况的数据处理
				((Map<?, ?>) obj).forEach((k, v) -> { // 键值的处理
					data.put(k + "", v);
				}); // forEach
			} // if
		} else { //
			for (int i = 0; i < n - 1; i += 2) {
				data.put(kvs[i].toString(), kvs[i + 1]);
			}
		} // if

		return new MyRecord(data);
	}

	private LinkedHashMap<String, Object> data = new LinkedHashMap<String, Object>();
}

/**
 * 数据框对象的实现,列式的组织方式
 * 
 * @author gbench
 *
 */
class DFrame {

	/**
	 * 
	 * @param data
	 */
	public DFrame(final List<IRecord> data) {
		this.dataRows.addAll(data);
		this.initialize().ifPresent(columns -> {
			this.dataColumns.putAll(columns);
		});
	}

	/**
	 * 
	 * @return
	 */
	public Optional<List<String>> header() {
		if (this.dataRows.size() > 0) {
			return Optional.of(this.dataRows.get(0).keys());
		} else {
			return Optional.empty();
		}
	}

	/**
	 * 列数据
	 * 
	 * @param name 列名
	 * @return 列数据
	 */
	public List<Object> col(final String name) {
		return this.dataColumns.get(name);
	}

	/**
	 * 列数据
	 * 
	 * @param name 列名
	 * @return 列数据的流
	 */
	public Stream<Object> colS(final String name) {
		return this.col(name).stream();
	}

	/**
	 * 转换成列数据数组
	 * 
	 * @return
	 */
	public Optional<LinkedHashMap<String, ArrayList<Object>>> initialize() {
		return this.header().map(keys -> {
			final var map = new LinkedHashMap<String, ArrayList<Object>>();
			this.dataRows.forEach(e -> {
				keys.forEach(k -> {
					map.compute(k, (_key, _value) -> _value == null ? new ArrayList<Object>() : _value).add(e.get(k));
				});
			});
			return map;
		});
	}

	/**
	 * 数据内容格式化
	 */
	public String toString() {
		if (this.dataRows.size() > 0) {
			final var buffer = new StringBuffer();
			final var first = this.dataRows.get(0);
			final var keys = first.keys();
			final var header = keys.stream().collect(Collectors.joining("\t"));
			final var body = this.dataRows.stream()
					.map(e -> keys.stream().map(e::str).collect(Collectors.joining("\t")))
					.collect(Collectors.joining("\n"));
			buffer.append(header + "\n");
			buffer.append(body);
			return buffer.toString();
		} else {
			return "DFrame(empty)";
		}
	}

	public static Collector<IRecord, ?, DFrame> dfmclc = Collector.of((Supplier<List<IRecord>>) ArrayList::new,
			List::add, (left, right) -> {
				left.addAll(right);
				return left;
			}, e -> {
				return new DFrame(e);
			});

	final ArrayList<IRecord> dataRows = new ArrayList<IRecord>();
	final LinkedHashMap<String, ArrayList<Object>> dataColumns = new LinkedHashMap<>();
}

/**
 * 二元算符结构 <br>
 * 
 * 需要 注意 结构不是 功能, 结构 是一种组织方式 并不代表 什么意义，<br>
 * 意义是一种 映射关系 a->b, 它是 把一个对象a与另一个对象b 之间 进行的 关联, 换句话说 <br>
 * 意义的常见解释 就是 把 结构 -> 实现特定目的功能用途 . <br>
 * 所以 意义 是一种 在 什么情况下 使用结构的 习惯约定。 它由设计 决定。 <br>
 * 就像 房子 可以 使用 砖木结构 也可以 钢筋结构 两种不同的 结构 却常常用来完成 同样的 功能 <br>
 * 对应形同的意义。同样 的 钢筋结构 不仅可以 建房子 还可以 键大船, 也就是 同样的 结构 可以用于实现不同的 功能 <br>
 * 即 结构 和 功能 是一种 多对多的 关系。 <br>
 * 
 * BinaryOp 不仅可以 用来 当做 运算对象来使用 还可以 当做 存储结构 即 数据节点(二叉树的节点来使用) 比如<br>
 * 求导 操作的时候 就是把 BinaryOp 当属 数据节点来 用的。<br>
 * 还有 Token 就是 把 BinaryOp 单做 一个 单值 容器来使用的 <br>
 * 结构 与 意义 之间的 关系是 非常灵活的 要在 实际 使用 中 灵活把握 同时 需要 区分 结构概念 和 意义观念 <br>
 * 比如 运算Op结构 和 运算Op意义 虽然都叫 运算Op 但还是不一样的 <br>
 * 1) 运算Op结构: 强调 运算Op 是如何 构造的，即 由那些 基本的 小构件，<br>
 * 用 什么 方式 联系到一起，那些些事动态可变的那些事静态固定的<br>
 * 2) 预算Op意义：强调 输入Input 经过 一系列的 黑盒 运算 之后 变成了 怎样的 输出Ouput, 具体 是如何 计算的<br>
 * 意义是不关心的，这也是说他黑盒的 原因,相反 对 Input 和 Output 到是 极为 有兴趣， <br>
 * 即 什么样 Input 可以 得到 Output, Output 出现的时机，形态，方式，和 性质有哪些 ， 这就像 一位 美食家 <br>
 * 他对食材和菜肴极为注重 但是 对 烹饪 却 不慎 关心（比如如何颠大勺,切黄瓜之类，厨艺功夫他是不理会的) 。<br>
 * 所以 意义 是一种 强调 结果 的 概念 而 结构 是一种强调过程 或者说 机制的概念。
 * 
 * 代数(符号计算)的 基本算符 <br>
 * 灵感来源于 LISP 的 CONS,CAR,CDR 的设计 <br>
 * <br>
 * LISP 的基本 逻辑就是 世界的 一切都是 列表(LIST) 结构,<br>
 * 列表以 CONS 为基本单元, 如果 一个不行，那就 多来几个，特别是 递归一下 的 多来几个<br>
 * (LIST是一个 以CONS:(CAR,CDR) 为基本单元的结构,CONS通过CDR进行彼此串联，<br>
 * 数据存放在CAR中或是由CAR指向)<br>
 * 
 * <br>
 * 所谓 CONS 是一个列表元素结构, <br>
 * 简单说就是一个二元组:第一个元素是 CAR,第二个元素是 CDR. <br>
 * 其中 CAR,CDR 可以任何类型。并且允许递归嵌套。<br>
 * CONS = (CAR,CDR) ，特别留意一下 ，CAR,CDR 不元素而是指针,<br>
 * CAR 指向一个元素 ,CDR 执行一个尾列表,例如： <br>
 * (a,(b,(c,d))) = (CAR:a,CDR:(b,(c,d))) <br>
 * 我这里采用 Tuple2 来表 代表 CONS, _1 代表 CAR,_2 代表 CDR <br>
 * <br>
 * 多元函数可以被视为 一个 参数类型是 多元列表的 UninaryOp。 <br>
 * UninaryOp 可以被视为 第二 参数是 null 的 BinaryOp <br>
 * 所以 BinaryOp 是最为基本的类型,这就是为何 把 BinaryOp 作为第一个类的原因。 <br>
 * 多元列表：采用 逗号 表达式用来构造。 <br>
 * 所以 a + b , 可以 `+` ((a,b),null) 来表示 <br>
 * 
 * <br>
 * 
 * BinaryOp 结构性 合理理解 是 一种 类似于 抗体 的 Y型结构 (Fab,(a,b)) 而不是 把作为一种二元函数结构，因为 有时候 我们 可以
 * 仅仅使用 Fab 的值得功能，比如 把 BinaryOp <br>
 * 作为一种常量容器，即 常量函数来使用。或者 运算的 结果 就是保持 Fab 不变 <br>
 * 
 * @author gbench
 *
 * @param <T> 算符 的 第一参数类型 CAR
 * @param <U> 算符 的 第二参数类型 CDR
 */
class BinaryOp<T, U> extends Tuple2<String, Tuple2<T, U>> {

	/**
	 * 二元算符的结构
	 * 
	 * @param name 操作符的名称
	 * @param args 操作符的参数 (left, right)
	 */
	public BinaryOp(String name, Tuple2<T, U> args) {
		super(name, args);
	}

	/**
	 * 操作的名称
	 * 
	 * @return
	 */
	public String getName() {
		return this._1;
	}

	/**
	 * 算符的参数
	 * 
	 * @return
	 */
	public Tuple2<T, U> getArgs() {
		return this._2;
	}

	/**
	 * 把参数组装成流式结构
	 * 
	 * @return [_1:第一个参数,_2:第二个参数]
	 */
	public Stream<Object> getArgsS() {
		if (this._2 == null) {
			return Stream.of();
		} else {
			return Stream.of(this._2._1, this._2._2);
		}
	}

	/**
	 * 把把参数扁平化之后的 流
	 * 
	 * 比如 (a,(b,(c,d)),(e,(f,g))) 扁平化 之后 返回 [b,c,d,e,f,g]
	 * 
	 * @return 把把参数扁平化之后的 流
	 */
	public Stream<Object> flatArgsS() {
		return flat(this.getArgs()).stream();
	}

	/**
	 * 运算的元数<br>
	 * 一个参数返回为一元运算,比如 正弦函数 sin(x)<br>
	 * 两个参数为二元运算,比如幂函数pow(x,n)
	 * 
	 * @return
	 */
	public int getAry() {
		return 2;
	}

	/**
	 * 1#参数位置组合 <br>
	 * 
	 * 一元函数继承的时候 需要 实现 自己的compose1, 另外compose1是设计为二元函数使用的函数，<br>
	 * 一元函数请使用自己的一元compose <br>
	 * 当然 一元函数可以自己先实现compose1，<br>
	 * 然后用自己compose调用自己先实现compose1
	 * 
	 * @param <X> 参数类型
	 * @param x   被组合的参数
	 * @return 新生成的组合形式
	 */
	public <X> BinaryOp<X, U> compose1(final X x) {
		return this._2 == null ? null : new BinaryOp<>(this._1, TUP2(x, this._2._2()));
	}

	/**
	 * 2#参数位置组合
	 * 
	 * @param <X> 参数类型
	 * @param x   被组合的参数
	 * @return 新生成的组合形式
	 */
	public <X> BinaryOp<T, X> compose2(final X x) {
		return this._2 == null ? null : new BinaryOp<>(this._1, TUP2(this._2._1(), x));
	}

	/**
	 * 
	 * 1#,2#位置的参数组合
	 * 
	 * @param <X> 1#参数的类型
	 * @param <Y> 2#参数的类型
	 * @param x   1#参数
	 * @param y   2#参数
	 * @return 新生成的组合形式
	 */
	public <X, Y> BinaryOp<X, Y> compose(final X x, final Y y) {
		return this._2 == null ? null : new BinaryOp<>(this._1, TUP2(x, y));
	}

	/**
	 * 二元函数的求值
	 * 
	 * @return 二元函数计算的结果, 数值 或者 BinaryOp 对象（当含有未知数的时候）
	 */
	public Object evaluate() {
		return this.evaluate(new HashMap<>());
	}

	/**
	 * 二元函数的求值
	 * 
	 * @param bindings 变量参数的数据绑定
	 * @return 二元函数计算的结果, 数值 或者 BinaryOp 对象（当含有未知数的时候）
	 */
	public Object evaluate(final IRecord bindings) {
		return this.evaluate(bindings.toMap());
	}

	/**
	 * 二元函数的求值
	 * 
	 * @param bindings 变量参数的数据绑定
	 * @return 二元函数计算的结果, 数值 或者 BinaryOp 对象（当含有未知数的时候）
	 */
	public Object evaluate(final Map<String, Object> bindings) {

		final var dataStream = this.getArgsS().map(e -> {
			if (e instanceof BinaryOp) { // op 算符类型
				return ((BinaryOp<?, ?>) e).evaluate(bindings);
			} else if (e instanceof Node) { // node 数据糖衣类型
				try {
					/**
					 * 正常情况 生成运算对象会 进行 unpack , 需要 运算时移除糖衣的情况很少， 但是还要给予保留，<br>
					 * 以应对不测 比如 要保证 某些不经意的,没有剔除干净掉数据糖衣的代码，恶意运行 <br>
					 */
					throw new Exception("语句中出现了数据糖衣，请检查运算的数据生成逻辑,在运算执行前给予糖衣unpack\n" + e);
				} catch (Exception ex) {
					ex.printStackTrace();
				} // try

				final var node = (Node) e;
				return node.isOp() ? node.getOp().evaluate(bindings) : node.tokenValue();
			} else { // 值类型
				return e;
			} // if
		}).map(e -> { // 上下文数据绑定
			if (e instanceof String) { // 字符串类型的数据 尝试 使用 绑定上下文 进行内容解析
				final var key = (String) e; // 把 参数值 解析为 变量符号(bindings 中的键名)
				final var value = bindings.getOrDefault(key, e); // 尝试从 bindings数据集合中提取符号的的具体的值
				return value; // 返回解析后的结果
			} else { // 非字符串类型的 参数 直接返回原来的数据
				return e;
			} // if
		}); // dataStream

		if (COMMA_TEST(this.getName())) { // 逗号表达式
			final var dd = dataStream.toArray();
			final var ret = TUP2(dd[0], dd[1]);
			return ret;
		} else { // 非 逗号表达式
			final var args = new LinkedList<Object>();
			final var dbls = dataStream.peek(args::add).map(BinaryOp.obj2dbl()).toArray(Double[]::new);

			// 二元函数的参数调整
			if (this.getAry() == 2 && Stream.of(dbls).filter(Objects::nonNull).count() < 2) {
				final var _dbls = args.stream().filter(Objects::nonNull)
						.flatMap(e -> e instanceof Tuple2 ? flatS((Tuple2<?, ?>) e) : Stream.of(e))
						.map(BinaryOp.obj2dbl()).toArray(Double[]::new);
				for (int i = 0; i < Math.min(dbls.length, _dbls.length); i++) { // 拷贝数据到 dbls
					dbls[i] = _dbls[i];
				} // for
			} // if

			final var x = dbls.length < 1 ? null : dbls[0];
			final var y = dbls.length < 2 ? null : dbls[1];
			final var left = args.size() < 1 ? null : args.get(0);
			final var right = args.size() < 2 ? null : args.get(1);

			if (this instanceof UnaryOp && x != null) { // 一元函数
				switch (this._1) { // 1 元运算
				case "sinh":
					return Math.sinh(x);
				case "sin":
					return Math.sin(x);
				case "csc":
					return 1d / Math.sin(x);
				case "cosh":
					return Math.cosh(x);
				case "cos":
					return Math.cos(x);
				case "sec":
					return 1d / Math.cos(x);
				case "tan":
					return Math.tan(x);
				case "cot":
					return 1d / Math.tan(x);
				case "exp":
					return Math.exp(x);
				case "neg":
					return -x;
				case "identity":
					return x;
				case "ln":
					return Math.log(x);
				case "sqrt":
					return Math.sqrt(x);
				case "square":
					return Math.pow(x, 2);
				default:
					return this.duplicate().compose1(x);
				} // switch
			} else if (this instanceof BinaryOp && x != null && y != null) { // 二元运算
				switch (this._1) {
				case "+":
					return x + y;
				case "-":
					return x - y;
				case "*":
					return x * y;
				case "/":
					return x / y;
				case "pow":
					return Math.pow(x, y);
				case "log":
					return Math.log(y) / Math.log(x);
				default:
					return this.duplicate().compose(x, y);
				} // switch
			} else { // 非法函数
				switch (this.getAry()) {
				case 1:
					return this.duplicate().compose1(left);
				case 2:
					return this.duplicate().compose(left, right);
				default:
					return this.duplicate();
				} // switch
			} // if UnaryOps
		} // if comma
	}

	/**
	 * 提取所有的叶端元素
	 * 
	 * @return 返回叶子节点的Set,没有重复
	 */
	@SuppressWarnings("unchecked")
	public <X extends Collection<Object>> X getLeafs(final Supplier<X> sup) {
		final X leafs = sup.get();
		final var stack = new Stack<Object>();

		this.getArgsS().forEach(stack::push);
		while (!stack.empty()) {
			final var o = Node.UNPACK(stack.pop());
			if (o instanceof BinaryOp) {
				((BinaryOp<Object, Object>) o).getLeafs(sup).forEach(leafs::add);
			} else if (Objects.nonNull(o)) {
				leafs.add(o);
			} // if
		} // while

		return leafs;
	}

	/**
	 * 
	 * @param variable
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public boolean hasLeaf(final Object variable) {
		final var stack = new Stack<Object>();
		this.getArgsS().forEach(stack::push);
		while (!stack.empty()) {
			final var o = Node.UNPACK(stack.pop());
			if (o instanceof BinaryOp) {
				if (((BinaryOp<Object, Object>) o).hasLeaf(variable))
					return true;
			} else if (Objects.nonNull(o)) {
				if (o.equals(variable))
					return true;
			} // if
		} // while

		return false;
	}

	/**
	 * 构造一个复制品
	 * 
	 * @return BinaryOp 浅层拷贝
	 */
	public BinaryOp<T, U> duplicate() {
		return this._2 == null ? BinaryOp.of(_1, null) : BinaryOp.of(_1, this._2._1(), this._2._2());
	}

	/**
	 * 运算节点的遍历<br>
	 * 需要注意：forEach 方法 只遍历 算符结构 对于 非 算符结构，即 算符的参数数据 并不给予 遍历 <br>
	 * 如果需要 遍历 算符的参数 数据，需要 利用 binaryop 的实际情况 来 进行处理 <br>
	 * 
	 * @param cons 回调函数 (level:层级从0开始, binaryop:运算节点)->{}
	 */
	@SuppressWarnings("unchecked")
	public void forEach(final BiConsumer<Integer, BinaryOp<?, ?>> cons) {
		final var stack = new Stack<BinaryOp<?, ?>>();
		final Map<Object, Integer> stateLevel = new HashMap<>();

		stateLevel.put(this, 0); // 层级初始化
		stack.push(this); // 根节点如栈

		while (!stack.empty()) {
			final var binaryop = stack.pop();
			final var level = stateLevel.get(binaryop);

			cons.accept(level, binaryop); // 方法回调

			binaryop.getArgsS() // 一次处理 餐数节点
					.map(Node::UNPACK) // Node 类型去包装
					.filter(e -> e instanceof BinaryOp) // 仅提取 BinaryOp 类型的数据
					.filter(Objects::nonNull) // 过滤掉空值
					.map(e -> (BinaryOp<Object, Object>) e) // 统一声明为 BinaryOp
					.forEach(e -> { // 计算层级加入 stack
						stateLevel.put(e, level + 1);
						stack.push(e);
					}); // forEach
		} // while
	}

	/**
	 * 数据格式化
	 */
	public String toString() {
		final String s1 = this._2._1() == null ? "null" : this._2._1().toString();
		final String s2 = this._2._2() == null ? "null" : this._2._2().toString();
		final var op = COMMA_TEST(this._1) ? "'" + this._1 + "'" : this._1;

		return this.getAry() == 2 ? MessageFormat.format("({0},{1},{2})", op, s1, s2)
				: MessageFormat.format("({0},{1})", op, s1);
	}

	/**
	 * 
	 * @param tup
	 * @return
	 */
	public static Stream<Object> flatS(final Tuple2<?, ?> tup) {
		return flat(tup).stream();
	}

	/**
	 * 
	 * @param tup
	 * @return
	 */
	public static List<Object> flat(final Tuple2<?, ?> tup) {
		final var ll = new LinkedList<Object>();
		final var stack = new Stack<Object>();

		stack.push(tup);
		while (!stack.isEmpty()) {
			final var p = stack.pop();
			if (p == null)
				continue;

			if (p instanceof Tuple2) {
				@SuppressWarnings("unchecked")
				final var _tup = (Tuple2<Object, Object>) p;
				Stream.of(_tup._2(), _tup._1()).forEach(stack::push);
			} else {
				ll.add(p);
			}
		}

		return ll;
	}

	/**
	 * 考虑到 BinaryOp 会覆盖掉name类派生类的特性，比如 一元函数 sin , 转换成 二元 函数 的情况 <br>
	 * 因此 建议 实际使用中 尽量不用 BinaryOp.of ， 可以的化 使用 BinaryOp.duplicate().compose 系列函数<br>
	 * 
	 * @param <T>   参数区第一元素类型
	 * @param <U>   参数区第二元素类型
	 * @param name  名称
	 * @param left  参数区第一元素类型 left参数
	 * @param right 参数区第二元素类型 right参数
	 * @return BinaryOp
	 */
	public static <T, U> BinaryOp<T, U> of(final String name, final T left, final U right) {
		return of(name, TUP2(left, right));
	}

	/**
	 * 考虑到 BinaryOp 会覆盖掉name类派生类的特性，比如 一元函数 sin , 转换成 二元 函数 的情况 <br>
	 * 因此 建议 实际使用中 尽量不用 BinaryOp.of ， 可以的化 使用 BinaryOp.duplicate().compose 系列函数<br>
	 * 
	 * @param <T>  参数区第一元素类型
	 * @param <U>  参数区第二元素类型
	 * @param name 算符名称
	 * @param args 算符的参数
	 * @return BinaryOp
	 */
	public static <T, U> BinaryOp<T, U> of(final String name, Tuple2<T, U> args) {
		return new BinaryOp<T, U>(name, args);
	}

	/**
	 * 把一个数据对象转换为浮点数<br>
	 * 对于 非法的数字类型 返回 null
	 * 
	 * @param <T> 函数的参数类型
	 * @return t->dbl
	 */
	static <T> Function<T, Double> obj2dbl() {
		return BinaryOp.obj2dbl(null);
	}

	/**
	 * 把一个数据对象转换为浮点数<br>
	 * 对于 非法的数字类型 返回 defaultValue
	 * 
	 * @param <T>          函数的参数类型
	 * @param defaultValue 非法的数字类型 返回 的默认值
	 * @return t->dbl
	 */
	static <T> Function<T, Double> obj2dbl(final Number defaultValue) {
		return (T obj) -> {
			if (obj instanceof Number)
				return ((Number) obj).doubleValue();
			Double dbl = defaultValue == null ? null : defaultValue.doubleValue();
			try {
				dbl = Double.parseDouble(obj.toString());
			} catch (Exception e) {
				// e.printStackTrace();
			}
			return dbl;
		};
	}

	/**
	 * 判断t 是否是运算符
	 */
	final static Predicate<Object> predicate_op = t -> {
		return ((t instanceof BinaryOp) || (t instanceof Node && ((Node) t).isOp()));
	};

	/**
	 * 当 t 为 null 的时候 返回 false 判断t 是否是词素 是 predicate_op 的 取反
	 */
	final static Predicate<Object> predicate_token = t -> {
		return t == null ? false : !predicate_op.test(t);
	};

	/**
	 * 
	 */
	final static Predicate<Object> predicate_var = t -> {
		Object _t = Node.UNPACK(t);
		if (t instanceof IRecord) { // 对于 IRecord 类型的数据尝试读取其value字段的的值
			_t = ((IRecord) t).get("value");
		}
		if (predicate_op.test(_t))
			return false;
		else if (_t instanceof String)
			return true;
		else
			return false;
	};

	/**
	 * 判断t是否是常量
	 */
	final static Predicate<Object> predicate_const = t -> {
		Object _t = Node.UNPACK(t);
		if (t instanceof IRecord) { // 对于 IRecord 类型的数据尝试读取其value字段的的值
			_t = ((IRecord) t).get("value");
		}
		return BinaryOp.obj2dbl().apply(_t) != null;
	};

	/**
	 * 获取 指定节点 op 的 在parent中的家庭排行,即 是 parent 的 第几个子节点。 <br>
	 * 当 parent 为 null 的 时候 返回 0 <br>
	 * 需要注意:对于 op 即是 parent的第一个参数也是第二个参数的时候, 即 parent(op,op) <br>
	 * rank_eval 是无法区分的具体排行的，同一返回最小的排行。<br>
	 * 此时 rank_eval 排行计算就会出现混乱，即 op 即是老大也是老二, <br>
	 * 会造成 相关方法比如: Node.dumpAST方法计算换乱, 为了避难 此种情况发生，<br>
	 * 请在构造节点的时候 对op 做一个浅拷贝。比如:parent(op,op.duplicate) <br>
	 * 参见 Node.derivate 方法的 微分的除法法则部分。 <br>
	 * MUL( ynode, ynode.duplicate() ) <br>
	 * <br>
	 * 
	 * return 排行序号从0开始,非子节点返回-1 <br>
	 */
	final static BiFunction<BinaryOp<?, ?>, BinaryOp<?, ?>, Integer> rank_eval = (parent, op) -> {
		if (parent == null) {
			return 0;
		} else {
			final var optional = parent.getArgsS().map(kvp_int()).map(sibling -> { // 添加序号并脱壳
				final var o = Node.UNPACK(sibling._2()); // 对象脱壳
				return TUP2(sibling._1(), o instanceof BinaryOp ? o : null);
			}).filter(e -> e._2() == op).findAny(); // 尝试从脱壳之后的args中查询结果项为op的节点

			return optional.map(e -> e._1()).orElse(-1);
		} // if
	};

}

/**
 * 
 * @author gbench
 *
 * @param <T>
 */
class UnaryOp<T> extends BinaryOp<T, Object> {

	public UnaryOp(String _1, T t) {
		super(_1, TUP2(t, null));
	}

	/**
	 * 
	 */
	@Override
	public int getAry() {
		return 1;
	}

	/**
	 * 构造一个复制品
	 * 
	 * @return UnaryOp 浅层拷贝
	 */
	@Override
	public UnaryOp<T> duplicate() {
		return new UnaryOp<>(this._1, this._2._1());
	}

	/**
	 * 1#参数位置组合 <br>
	 * 
	 * 一元函数继承的时候 需要 实现 自己的compose1, 另外compose1是设计为二元函数使用的函数，<br>
	 * 一元函数请使用自己的一元compose <br>
	 * 当然 一元函数可以自己先实现compose1，<br>
	 * 然后用自己compose调用自己先实现compose1
	 * 
	 * @param <X> 参数类型
	 * @param x   被组合的参数
	 * @return 新生成的组合形式
	 */
	public <X> BinaryOp<X, Object> compose1(final X x) {
		return new UnaryOp<>(this._1, x);
	}

	/**
	 * 2#参数位置组合
	 * 
	 * @param <X> 参数类型
	 * @param x   被组合的参数
	 * @return 新生成的组合形式, UnaryOp 无法做 compose2(X) 组合
	 */
	public <X> BinaryOp<T, X> compose2(final X x) {

		try {
			throw new Exception("UnaryOp 无法做 compose2(X) 组合");
		} catch (Exception e) {
			e.printStackTrace();
		}

		return null;
	}

	/**
	 * 
	 * 1#,2#位置的参数组合
	 * 
	 * @param <X> 1#参数的类型
	 * @param <Y> 2#参数的类型
	 * @param x   1#参数
	 * @param y   2#参数
	 * @return null , UnaryOp 无法做 compose(X,Y) 组合
	 */
	public <X, Y> BinaryOp<X, Y> compose(final X x, final Y y) {

		try {
			throw new Exception("UnaryOp 无法做 compose(X,Y) 组合");
		} catch (Exception e) {
			e.printStackTrace();
		}

		return null;
	}

	/**
	 * 1#参数位置组合
	 * 
	 * @param <X> 参数类型
	 * @param x   被组合的参数
	 * @return 新生成的组合形式
	 */
	public <X> BinaryOp<X, Object> compose(final X x) {
		return this.compose1(x);
	}

	/**
	 * 
	 * @param <T>
	 * @param name
	 * @param t
	 * @return
	 */
	public static <T> UnaryOp<T> of(String name, T t) {
		return new UnaryOp<>(name, t);
	}

	/**
	 * 
	 */
	public String toString() {
		String s1 = this._2._1() == null ? "null" : this._2._1().toString();
		return MessageFormat.format("({0},{1})", this._1, s1);
	}

}

/**
 * 
 * @author gbench
 *
 * @param <T>
 * @param <U>
 */
class Add<T, U> extends BinaryOp<T, U> {

	public Add(T t, U u) {
		super("+", TUP2(t, u));
	}

}

/**
 * 
 * @author gbench
 *
 * @param <T>
 * @param <U>
 */
class Minus<T, U> extends BinaryOp<T, U> {

	public Minus(T t, U u) {
		super("-", TUP2(t, u));
	}

}

/**
 * 
 * @author gbench
 *
 * @param <T>
 * @param <U>
 */
class Mul<T, U> extends BinaryOp<T, U> {

	public Mul(T t, U u) {
		super("*", TUP2(t, u));
	}

}

/**
 * 
 * @author gbench
 *
 * @param <T>
 * @param <U>
 */
class Div<T, U> extends BinaryOp<T, U> {

	public Div(T t, U u) {
		super("/", TUP2(t, u));
	}

}

/**
 * 
 * @author gbench
 *
 * @param <T>
 * @param <U>
 */
class Pow<T, U> extends BinaryOp<T, U> {

	public Pow(T t, U u) {
		super("pow", TUP2(t, u));
	}

}

/**
 * 
 * @author gbench
 *
 * @param <T>
 * @param <U>
 */
class Comma<T, U> extends BinaryOp<T, U> {

	public Comma(T t, U u) {
		super(",", TUP2(t, u));
	}

	/**
	 * 检测 target 是否是一个 逗号"," 字符串
	 * 
	 * @param target 待检测的对象
	 */
	public static boolean COMMA_TEST(final Object target) {
		return ",".equals((target + "").strip());
	}

}

/**
 * 
 * @author gbench
 *
 * @param <T>
 * @param <U>
 */
class Log<T, U> extends BinaryOp<T, U> {

	public Log(T t, U u) {
		super("log", TUP2(t, u));
	}

}

/**
 * 
 * @author gbench
 *
 * @param <T>
 */
class Sin<T> extends UnaryOp<T> {

	public Sin(T t) {
		super("sin", t);
	}

}

/**
 * 
 * @author gbench
 *
 * @param <T>
 */
class Sinh<T> extends UnaryOp<T> {

	public Sinh(T t) {
		super("sinh", t);
	}

}

/**
 * 
 * @author gbench
 *
 * @param <T>
 */
class Csc<T> extends UnaryOp<T> {

	public Csc(T t) {
		super("csc", t);
	}

}

/**
 * 
 * @author gbench
 *
 * @param <T>
 */
class Cos<T> extends UnaryOp<T> {

	public Cos(T t) {
		super("cos", t);
	}

}

/**
 * 
 * @author gbench
 *
 * @param <T>
 */
class Cosh<T> extends UnaryOp<T> {

	public Cosh(T t) {
		super("cosh", t);
	}

}

/**
 * 
 * @author gbench
 *
 * @param <T>
 */
class Sec<T> extends UnaryOp<T> {

	public Sec(T t) {
		super("sec", t);
	}

}

/**
 * 
 * @author gbench
 *
 * @param <T>
 */
class Tan<T> extends UnaryOp<T> {

	public Tan(T t) {
		super("tan", t);
	}

}

/**
 * 
 * @author gbench
 *
 * @param <T>
 */
class Cot<T> extends UnaryOp<T> {

	public Cot(T t) {
		super("cot", t);
	}

}

/**
 * 
 * @author gbench
 *
 * @param <T>
 */
class Neg<T> extends UnaryOp<T> {

	public Neg(T t) {
		super("neg", t);
	}

}

/**
 * 
 * @author gbench
 *
 * @param <T>
 */
class Exp<T> extends UnaryOp<T> {

	public Exp(T t) {
		super("exp", t);
	}

}

/**
 * 
 * @author gbench
 *
 * @param <T>
 */
class Sqrt<T> extends UnaryOp<T> {

	public Sqrt(T t) {
		super("sqrt", t);
	}

}

/**
 * 
 * @author gbench
 *
 * @param <T>
 */
class Square<T> extends UnaryOp<T> {

	public Square(T t) {
		super("square", t);
	}

}

/**
 * 
 * @author gbench
 *
 * @param <T>
 */
class Identity<T> extends UnaryOp<T> {

	public Identity(T t) {
		super("identity", t);
	}

}

/**
 * 
 * @author gbench
 *
 * @param <T>
 */
class Ln<T> extends UnaryOp<T> {

	public Ln(T t) {
		super("ln", t);
	}

}

/**
 * 数据糖衣结构 (语法节点)
 * 
 * 语法树的节点, 即 数据糖衣 是 一种 包装 <br>
 * 包装 就像 刷 为 药物 刷一层糖衣 一样,Node 以后的 target 会 更加方便 操作。<br>
 * 因为 Node 提供一系列 对 被包裹值 的 的 便捷 访问与操作函数 <br>
 * 
 * Node 的 本质就是 一个 value 的 wrapper, <br>
 * 为 value 提供一个 方法壳, 以方便对 对value的 处理 <br>
 * 对于 语法树 它的内容 主要分为： <br>
 * 1) op 运算节点 <br>
 * 2) token 词素节点 <br>
 * 
 * @author gbench
 *
 */
class Node implements gbench.util.math.algebra.AlgebraEngine.INode {

	/**
	 * 数据糖衣
	 * 
	 * @param value
	 */
	public Node(Object value) {
		this.value = value;
	}

	/**
	 * 是否是运算节点,<br>
	 * 此时的 Op 是一种 运算意义 而 不是结构， 也就是 <br>
	 * 尽管 value 是 一种 BinaryOp 但是 由于 它没有 运算功能 <br>
	 * 比如 isConstant的 常量的 情况, 它就 不是 运算意义下得Op,它 也是 要返回 false 的
	 * 
	 * @return 是否是一种运算意义上的Op
	 */
	public boolean isOp() {
		return this.value instanceof BinaryOp;
	}

	/**
	 * 是否词素节点
	 * 
	 * @return
	 */
	public boolean isToken() {
		return !this.isOp();
	}

	/**
	 * 获取Node的名称 对于 op 类型的 Node name 就是 op 算符名 <br>
	 * 对于 token 类型的 name, name 就是 其Record的结构的 value字段 的字符串表示。 <br>
	 * 
	 * @return Node 的名字
	 */
	public String getName() {
		return this.isOp() ? this.getOp().getName() : this.getToken().str("value");
	}

	/**
	 * 获取内容物 即 被包裹的值
	 * 
	 * @return Object
	 */
	public Object getValue() {
		return this.value;
	}

	/**
	 * 从 Node 中脱壳 <br>
	 * 所谓 脱壳 就是 从 Node 中给 拆解出 value, <br>
	 * 若 target 不是 Node类型 则保持不变 <br>
	 * 尝试接续解包的次数为100
	 * 
	 * @param target 目标对象
	 * @return 脱壳后的对象
	 */
	public Object unpack() {
		return UNPACK(this);
	}

	/**
	 * 从 Node 中脱壳 <br>
	 * 所谓 脱壳 就是 从 Node 中给 拆解出 value, <br>
	 * 若 target 不是 Node类型 则保持不变
	 * 
	 * @param target 目标对象
	 * @param n      尝试继续解包的次数
	 * @return 脱壳后的对象
	 */
	public Object unpack(final int n) {
		return UNPACK(this, n);
	}

	@SuppressWarnings("unchecked")
	public BinaryOp<Object, Object> getOp() {
		return this.isOp() ? (BinaryOp<Object, Object>) value : null;
	}

	/**
	 * 生成词素对象
	 * 
	 * @return
	 */
	public IRecord getToken() {
		if (this.isToken()) {
			if (this.value instanceof IRecord) {
				return (IRecord) this.value;
			} else { // 生成一个类型为virtual的词素对象
				return REC("type", "virtual", "value", this.value);
			} // if
		} else {
			return null;
		} // if
	}

	/**
	 * 读取token 的值
	 * 
	 * @return token 的 值
	 */
	public Object tokenValue() {
		final var token = this.getToken();
		return token == null ? null : token.get("value");
	}

	/**
	 * 节点结算
	 * 
	 * @return 节点计算后的结果
	 */
	public Object evaluate() {
		return this.evaluate(REC());
	}

	/**
	 * 节点计算
	 * 
	 * @param bindings 变量参数的数据绑定
	 * @return 节点计算后的结果
	 */
	public Object evaluate(final IRecord bindings) {
		return this.evaluate(bindings.toMap());
	}

	/**
	 * 节点计算
	 * 
	 * @param bindings 变量参数的数据绑定
	 * @return 节点计算后的结果
	 */
	public Object evaluate(final Map<String, Object> bindings) {
		return this.isOp() ? this.getOp().evaluate(bindings) : this.tokenValue();
	}

	/**
	 * 按照x变量进行方法求导
	 * 
	 * @return 求导之后的语法树结构
	 */
	public Node derivate() {
		return this.derivate("x");
	}

	/**
	 * 结构求导 (尚未把求导规则补充完成)
	 * 
	 * @param variable 求导变量
	 * @return 求导之后的计算结构
	 */
	public Node derivate(final String variable) {
		if (this.isToken()) { // 词素类型节点
			if (predicate_var.test(this.value)) { // 变量检测
				return variable.equals(this.getName()) ? PACK(1) : PACK(0);
			} else if (predicate_const.test(this.value)) { // 常量检测
				return PACK(0);
			} else {
				return null;
			}
		} else { // 运算类型节点
			if (!this.getOp().hasLeaf(variable)) { // 算符的叶端节点不包含微分变量则返回0
				return PACK(0);
			} // if

			final var theOp = this.getOp(); // 当前的操作符号
			final var name = this.getName(); // 获取运算名称
			final var nodes = this.getOp().getArgsS().limit(theOp.getAry()).map(Node::PACK).toArray(Node[]::new); // 提取方法参数并Node糖衣化
			final var xnode = nodes.length < 1 ? null : nodes[0]; // 第一个参数的node
			final var ynode = nodes.length < 2 ? null : nodes[1]; // 第二个参数的node
			final var x = xnode == null ? null : xnode.unpack(); // 第一个参数
			final var y = ynode == null ? null : ynode.unpack(); // 第二个参数
			final Supplier<IRecord> comma_args_adjust = () -> { // 逗号表达式的情况
				final var rb = IRecord.rb("xnode,ynode,x,y"); // 基本数据的命名结构
				if (y == null && COMMA_TEST(xnode.getOp().getName())) { // xnode 是一个逗号表达式
					final var comma_args = xnode.getOp().flatArgsS().map(Node::UNPACK).toArray(Object[]::new); // 提取逗号表达式的参数列表
					final var _x = comma_args[0]; // comma 的一号位置元素
					final var _y = comma_args[1]; // comma 的二号位置元素
					final var _xnode = PACK(_x);
					final var _ynode = PACK(_y);
					return rb.get(_xnode, _ynode, _x, _y);
				} else { // 非逗号表达式 不予 调整参数 原样返回
					return rb.get(xnode, ynode, x, y);
				} // if
			}; // comma_args_adjust
			final Function<Node, Node> derivate_rule = prototye -> { // 求导规则
				if (xnode != null && xnode.isOp()) { // 复合函数情况
					return PACK(MUL(prototye.unpack(), xnode.derivate(variable).unpack()));
				} else { // 基本函数情况
					return prototye;
				}
			};

			switch (name) {
			case "+":
			case "-": { // 加减法规则 +(x,y) 或 -(x,y) 就 x+y 或 x-y
				final var node = Stream.of(nodes).filter(Objects::nonNull) // 去除空值节点
						.map(e -> {
							final var p = e.derivate(variable);
							return p;
						}).reduce((node1, node2) -> {
							return PACK(theOp.duplicate().compose(node1.unpack(), node2.unpack()));
						}).orElse(null);
				return node;
			}
			case "*": { // 乘法运算规则 *(x,y) 即 x*y
				final var op = ADD(MUL(xnode.derivate(variable).unpack(), y), // x'*y
						MUL(x, ynode.derivate(variable).unpack())); // x*y'
				return PACK(op);
			}
			case "/": { // 除法运算规则 /(x,y) 即 x/y
				final var op = DIV(
						MINUS(MUL(xnode.derivate(variable).unpack(), ynode.unpack()),
								MUL(xnode.unpack(), ynode.derivate(variable).unpack())),
						// 为了避免 dumpAST出现混乱[会出现兄弟排行错误,即一个元素即是老大也是老二]，
						// 兄弟节点的元素不要使用相同的对象结构。所以 这里第二个ynode给予复制
						MUL(ynode.unpack(), ynode.duplicate().unpack()));
				return PACK(op);
			}
			case "identity": { // 常量函数
				return xnode.derivate(variable);
			}
			case "neg": { // 取反函数 neg(x)
				return PACK(MINUS("0", xnode.derivate(variable).unpack()));
			}
			case "sin": { // 正弦函数 sin(x)
				return derivate_rule.apply(PACK(COS(x)));
			}
			case "cos": { // 余弦函数 cos(x)
				return derivate_rule.apply(PACK(NEG(SIN(x))));
			}
			case "tan": { // 余弦函数 tan(x)
				return derivate_rule.apply(PACK(SQUARE(SEC(x))));
			}
			case "exp": { // 指数函数 exp(x)
				return derivate_rule.apply(PACK(EXP(x)));
			}
			case "ln": { // 指数函数 ln(x)
				return derivate_rule.apply(PACK(DIV(1, x)));
			}
			case "log": { // 指数函数 log(x,y) 其中 x 常数底数, y 是变量
				final var _args = comma_args_adjust.get(); // 参数调整
				final var _a = _args.get("x"); // 底数 命名为 数学表达习惯
				final var _x = _args.get("y"); // 微分变量 命名为 数学表达习惯
				final var _xnode = _args.get("ynode", Node.class); // 底数

				if (_xnode != null && _xnode.isOp()) { // 复合函数情况
					return PACK(MUL(MUL(LN(_a), DIV(1, _x)), _xnode.derivate(variable).unpack()));
				} else { // 基本函数情况
					return PACK(MUL(LN(_a), DIV(1, _x)));
				}
			}
			case "pow": { // 指数函数 pow (x,y), 由于 pow 函数 是蚕蛹逗号表达式来提供参数操作数的，所以这里需要对操作数进行调整
				final var _args = comma_args_adjust.get();
				final var _x = UNPACK(_args.get("x"));
				final var _y = UNPACK(_args.get("y"));
				final var _xnode = _args.get("xnode", Node.class);

				if (_xnode != null && _xnode.isOp()) { // 复合函数情况
					return PACK(MUL(_y, MUL(POW(_x, MINUS(_y, 1)), _xnode.derivate(variable).unpack())));
				} else { // 基本函数情况
					return PACK(MUL(_y, POW(_x, MINUS(_y, 1))));
				}
			}
			default: { // 其他函数不予处理
				return null;
			}
			} // switch
		}
	}

	/**
	 * 语法树dump <br>
	 * 
	 * 所谓语法树就是一个计算结构：<br>
	 * 类似于如下结构： <br>
	 * (5*pow(2,5)*tan(45.6/255-cos(4/6))) <br>
	 * 
	 * * ----> (*,5,(*,(pow,(',',2,5),null),(tan,(-,(/,45.6,255),(cos,(/,4,6))))))
	 * <br>
	 * | 5 <br>
	 * | * ----> (*,(pow,(',',2,5),null),(tan,(-,(/,45.6,255),(cos,(/,4,6))))) <br>
	 * | | pow ----> (pow,(',',2,5),null) <br>
	 * | | | , ----> (',',2,5) <br>
	 * | | | | 2 <br>
	 * | | | | 5 <br>
	 * | | | null <br>
	 * | | tan ----> (tan,(-,(/,45.6,255),(cos,(/,4,6)))) <br>
	 * | | | - ----> (-,(/,45.6,255),(cos,(/,4,6))) <br>
	 * | | | | / ----> (/,45.6,255) <br>
	 * | | | | | 45.6 <br>
	 * | | | | | 255 <br>
	 * | | | | cos ----> (cos,(/,4,6)) <br>
	 * | | | | | / ----> (/,4,6) <br>
	 * | | | | | | 4 <br>
	 * | | | | | | 6 <br>
	 */
	public String dumpAST() {

		/**
		 * 由于 采用深度遍历，所以 一个 阶层为 i 的节点 路径为 [0,1,2,...,i-1,i]中个对应的 值序列 <br>
		 * 于是 父节点 利索应当为 i-1 所对应键值
		 */
		final var level2ops = new HashMap<Integer, BinaryOp<?, ?>>(); // 阶层对节点对象,阶层从0开始
		final var buffer = new StringBuffer(); // 数据行缓存由writeln写入
		final Consumer<String> writeln = line -> { // 数据行的写入到缓存buffer
			buffer.append(line);
			buffer.append("\n");
		};

		/**
		 * Tuple2<List<Integer>>是 处理例程routine 的数据结构 <br>
		 * routine处理例程 (rank:家族排行,task:处理过程Runnable), 根据 家族排行rank 进行依次排行,<br>
		 * 执行时,按照这样的阶层逐层递进的 方式 进行 深度遍历 (展开) : 0 > 0.1 > 0.2 > 1 > 1.1 > 1.1.1 > 1.2 >
		 * ... routines_executor 把处于同一层级的 例程 统一到一起 并 根据 其 例程的 家族序号 即 rank 按照顺序的 依次执行
		 * <br>
		 * 例子程序,用 Runnable 结构来表示.
		 */
		final Consumer<List<Tuple2<List<Integer>, Runnable>>> routines_executor = routines -> { // routine 例程
			routines.stream().sorted((a, b) -> {
				final var key1 = a._1(); // 第一元素的排序key
				final var key2 = b._1(); // 第二元素的排序key
				final var opt = Stream.iterate(0, i -> i + 1) // 生一个从0开始的自然数序列
						.limit(Math.min(key1.size(), key2.size())) // 提取公共长度
						.map(i -> key1.get(i).compareTo(key2.get(i))) // 共同长度时候按照对于位置的元素大小进行比较
						.filter(e -> e != 0) // 判断是否出现大小差异
						.findFirst(); // 获取比较结果
				if (opt.isEmpty()) { // 公共长度部分完全相同时按照数据长度进行比较，公共长度比较无效
					return key1.size() - key2.size();
				} else { // 公共长度比较有效
					return opt.get();
				}
			}).map(e -> e._2()) // 提取执行routine:Runnalbe
					.forEach(Runnable::run); // 依次运行routine

			routines.clear(); // 回调数据清空
		}; // subroutines_executor
		final var routines = new ArrayList<Tuple2<List<Integer>, Runnable>>(); // routine:执行例程的缓存
		final Function<Integer, List<Integer>> pathcode_eval = (level) -> { // 计算level阶层节点的pathcode
			final var ancestors = Stream.iterate(0, i -> i <= level, i -> i + 1).map(level2ops::get)
					.collect(Collectors.toList());// 提取祖先列表
			final var width = 2; // 滑动窗口的宽度
			final var ranks = Stream.iterate(0, i -> i <= ancestors.size() - width, i -> i + 1) // 构造以width为窗口宽度的sliding
					.map(i -> ancestors.subList(i, i + width)) // 提取数据窗口
					.map(e -> rank_eval.apply(e.get(0), e.get(1))) // 提取家庭排行
					.collect(Collectors.toList());// 按照家庭排行生成族内排行，使用族内排行代表当前level节点的pathcode
			ranks.add(0, -1); // 用-1 代表根节点的家族排行,这样的做额原因就是避免根节点的ranks为空列表[]
			return ranks;
		};

		if (this.isToken()) { // 词素
			return this.toString();
		} else { // 算符
			/**
			 * 由于 BinaryOp的forEach 只提供 非叶子结点 Op结点 的方式遍历，无法 提供 叶子结点即数据结点的参数 信息。<br>
			 * 为了 在构造语法树 AST的时候 提供 叶子结点即数据结点的 信息，需要 在 Op结点 之间插入 叶子结点 即 数据结点 的数据。<br>
			 * 单考 虑度 数据结点 与 Op结点 在 BinaryOp的 参数结点 即 子节点中的组织顺序 并非固定，若是 在 BinaryOp的forEach遍历同时
			 * <br>
			 * 给予一次性构建AST势必造成数据显示混乱，由此 这里的 AST 构建采用 构建遍历例程 带有家族排行的 routine 历程的方式进行 静态组织。<br>
			 * 家族排行 rank(如: 1, 1.1 , 1.1.1, 1.1.2 , 1.2, 1.2.1, 1.2.2 ) 代表了 routine
			 * 执行时候的时序关系, <br>
			 * 这是一种深度优先DFS的范式
			 * 
			 * 语法树结构遍历方式采用静态结构进行组织,分为2个步骤: <br>
			 * 1) 生成&归集 处理历程 routine 到 例程容器 routines, 但不予执行. <br>
			 * 2) 对 例程容器 routines 中的 历程 进行家族排序，按照家族排行键rank,按照 深度遍历的次序 给予依次执行。<br>
			 */
			this.getOp().forEach((level, op) -> {
				final var IDENT = " | "; // 缩进字符串
				level2ops.put(level, op); // 记录节点阶层
				final var pathcode = pathcode_eval.apply(level);
				routines.add(TUP2(pathcode, (Runnable) () -> { // 算符节点 的 数据 dump : 创建一个执行例程 用于在当前level打印op的信息
					final var line = IDENT.repeat(level) + op.getName() + " \t ----> " + op; // 算符节点的 信息条目
					writeln.accept(line);
				}));
				op.getArgsS() // 子节点的处理
						.limit(op.getAry()) // 提取有效参数
						.map(kvp_int()) // 增加节点的兄弟排行
						.filter(kvp -> predicate_token.test(kvp._2())) // 提取叶子节点
						.map(kvp -> { // 生成必要的叶子节点的处理例程:subroutine
							final List<Integer> child_pathcode = new ArrayList<Integer>(pathcode);
							child_pathcode.add(kvp._1()); // 补充上当前节点的家族排行
							// 数据节点 的 数据 dump : 创建一个执行例程 用于在下一个level打印叶子节点信息
							return TUP2(child_pathcode, (Runnable) () -> {
								final var line = IDENT.repeat(level + 1) + kvp._2(); // 叶子节点的 信息条目
								writeln.accept(line);
							});
						}).forEach(routines::add); // 添加处理例程
			}); // forEach

			// 把routines根据家族排行的次序给予依次执行
			routines_executor.accept(routines);
		} // if

		return buffer.toString();
	}

	/**
	 * 构造一个复制品 <br>
	 * 调用 值的 duplicate 方法来完成值复制并重新创建Node,注意:值的复制办法是采用的是浅层复制
	 * 
	 * @return 复制品,
	 */
	public Node duplicate() {
		final var _value = this.isOp() ? this.getOp().duplicate() : this.getToken().duplicate();
		return PACK(_value);
	}

	/**
	 * 数据格式化
	 */
	public String toString() {
		if (this.value == null)
			return "Null";
		return (!this.isOp() ? this.tokenValue() + "" : this.value + "").toString();
	}

	/**
	 * 从 Node 中脱壳 <br>
	 * 所谓 脱壳 就是 从 Node 中给 拆解出 value, <br>
	 * 若 target 不是 Node类型 则保持不变 <br>
	 * 尝试接续解包的次数为100
	 * 
	 * @param target 目标对象
	 * @return 脱壳后的对象
	 */
	public static Object UNPACK(final Object target) {
		return UNPACK(target, 100);
	}

	/**
	 * 从 Node 中脱壳 <br>
	 * 所谓 脱壳 就是 从 Node 中给 拆解出 value, <br>
	 * 若 target 不是 Node类型 则保持不变
	 * 
	 * @param target 目标对象
	 * @param n      尝试继续解包的次数
	 * @return 脱壳后的对象
	 */
	public static Object UNPACK(final Object target, final int n) {
		final var _t = target instanceof Node ? ((Node) target).value : target; // 脱壳
		return (_t instanceof Node && n > 0) ? UNPACK(_t, n - 1) : _t;
	}

	/**
	 * 视图包装 <br>
	 * 如果已经包装之后则不予包装，而是 直接返回, 否则 Node 包装 target
	 * 
	 * @param target 被包装对象
	 * @return Node 包装后的 target
	 */
	public static Node PACK(final Object target) {
		return target instanceof Node ? (Node) target : NODE(target);
	}

	/**
	 * 把 value 值 包装一层Node <br>
	 * 包装 就像 刷 为 药物 刷一层糖衣 一样,Node 以后的 target 会 更加方便 操作。<br>
	 * 因为 Node 提供一系列 对 被包裹值 的 的 便捷 访问与操作函数 <br>
	 * 
	 * @param target 被包装的对象
	 * @return Node 包装后的 target
	 */
	public static Node NODE(final Object target) {
		return new Node(target);
	}

	private Object value; // 被包裹的值
}

/**
 * 运算工厂类<br>
 * 
 * 一系列便捷操作的辅助函数 <br>
 * 
 * @author gbench
 *
 */
class Ops {

	public static <T, U> BinaryOp<T, U> ADD(T t, U u) {
		return new Add<>(t, u);
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
	 * @param op2 运算符1
	 * @return
	 */
	public static int priority_cmp(final String op1, final String op2) {
		return priorities.i4(op1, 10) - priorities.i4(op2, 10);
	}

	/**
	 * 运算的优先级比较
	 * 
	 * @param op1 运算符1
	 * @param op2 运算符1
	 * @return
	 */
	public static int priority_cmp(final Node op1, final Node op2) {
		return priority_cmp(op1.getName(), op2.getName());
	}

	/**
	 * 索引化生成器
	 * 
	 * @param <T>
	 * @param start
	 * @return
	 */
	public static <T> Function<T, Tuple2<Integer, T>> kvp_int() {
		return kvp_int(0, e -> e);
	}

	/**
	 * 索引化生成器
	 * 
	 * @param <T>
	 * @param start 开始位置
	 * @return
	 */
	public static <T> Function<T, Tuple2<Integer, T>> kvp_int(final Number start) {
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
	public static <T, K> Function<T, Tuple2<K, T>> kvp_int(final Number start,
			final Function<Integer, K> key_generator) {
		final var atom = new AtomicInteger(start.intValue());// 状态缓存：用于生成序号
		return t -> TUP2(key_generator.apply(atom.getAndIncrement()), t);
	}

	/**
	 * 检查 node 的 名称是否是一个有效的 算符名 <br>
	 * 
	 * param node 待检测的 对象 <br>
	 * return 检查 node 的 名称是否是一个有效的 算符名 <br>
	 */
	public static Predicate<Node> is_op_word = node -> {
		final var opt = lookup(node.getName());
		return opt.isPresent();
	};

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
	public static Map<String, BinaryOp<?, ?>> registry = new HashMap<>();

	/**
	 * registry 的 初始化过程, 注册系统函数
	 */
	static { // 注册系统函数
		Stream.of(ADD(null, null), MINUS(null, null), MINUS(null), MUL(null, null), DIV(null, null), POW(null, null),
				COMMA(null, null), LOG(null, null), SIN(null), SINH(null), CSC(null), COS(null), COSH(null), SEC(null),
				TAN(null), COT(null), NEG(null), EXP(null), ID(null), LN(null), SQRT(null), SQUARE(null)).forEach(e -> {
					registry.put(e.getName(), e); // 注册函数
				}); // forEach
	} // static
}

/***
 * 代数引擎 <br>
 * 符号计算 的 简单实现
 * 
 * @author gbench
 *
 */
public class AlgebraEngine {

	public List<IRecord> tokenize(String line) {
		println(line);
		println();
		final var digits = Pattern.compile("[\\.0-9]+", Pattern.CASE_INSENSITIVE);
		final var pattern = Pattern.compile("[\\.a-z0-9]+", Pattern.CASE_INSENSITIVE);
		final var pattern1 = Pattern.compile("^[\\.a-z0-9]+.*", Pattern.CASE_INSENSITIVE);
		final var pattern2 = Pattern.compile(".*[\\.a-z0-9]$", Pattern.CASE_INSENSITIVE);
		final var pattern3 = Pattern.compile("[^\\.a-z0-9]+", Pattern.CASE_INSENSITIVE);
		final var buffer = new StringBuffer();
		final var rb = IRecord.rb("type,value");
		final Function<String, Stream<IRecord>> keyword_handler = keyword -> {
			final var _keyword = keyword.strip(); // 去除多余的空格
			final var rec = digits.matcher(_keyword).matches() ? rb.get("number", _keyword) : rb.get("word", _keyword);
			return Stream.of(rec);
		};

		final Function<String, Stream<IRecord>> op_handler = op -> {
			return Stream.of(op.split("")).filter(e -> !e.matches("s*")).map(e -> rb.get("op", e));
		};

		final var tokens = new LinkedList<IRecord>();
		final var wordS = Stream.of(line.split("")).flatMap(c -> {
			final var word = buffer.append(c).toString();
			final var matcher = pattern.matcher(buffer);
			final var n = word.length();
			if (matcher.matches()) { // 关键词
				// do nothing
			} else {
				if (pattern1.matcher(word).matches()) {// 关键字+运算符号
					final var keyword = buffer.substring(0, n - 1);
					buffer.delete(0, n - 1);
					return keyword_handler.apply(keyword);
				} else if (pattern2.matcher(word).matches()) { // 运算符号+关键词
					final var op = buffer.substring(0, n - 1);
					buffer.delete(0, n - 1);
					return op_handler.apply(op);
				} else if (pattern3.matcher(word).matches()) { // 没有关键字
					//
				} else {
					//
				}
			} // if

			return null;
		}).filter(Objects::nonNull).filter(e -> !e.str("value").matches("\\s*")).onClose(() -> {
			final var s = buffer.toString();
			final var t = ((pattern.matcher(s).matches()) ? keyword_handler : op_handler).apply(s);
			tokens.addAll(t.collect(Collectors.toList()));
		});

		wordS.forEach(tokens::add);
		wordS.close();

		final var stateLevel = new AtomicInteger(0);
		tokens.forEach(e -> {
			if (e.get("value").equals("(")) { // 层次开始
				e.add("level", stateLevel.getAndUpdate(x -> x + 1)); // ( 不增加层次
			} else if (e.get("value").equals(")")) { // 层次结束
				e.add("level", stateLevel.updateAndGet(x -> x - 1)); // ) 需要减少层次
			} else { // 普通节点
				e.add("level", stateLevel.get()); // 直接读取层次
			}
		});

		return tokens;
	}

	/**
	 * 为节点序列 设计 算符的 比较分组
	 * 
	 * @param nodes               节点序列
	 * @param predicate_data_node 数据节点的检测函数
	 * @return (1:目标比较分组, 2:比较分组集合[(1:比较分组结果,2:比较分组元素集合[(1:元素索引位置,2:第二比较元素)]] ) <br>
	 *         比较分组元素集合[左侧比较元素 a,比较元素 b] 即 代表 a <比较> b 的 结构形式
	 */
	Tuple2<Integer, List<Tuple2<Integer, List<Tuple2<Integer, Node>>>>> compareGroups(final List<Node> nodes,
			final Predicate<Node> predicate_data_node) {

		final var opnodes = new ArrayList<Tuple2<Integer, Node>>(); // 算符 的 节点序列

		nodes.stream().map(kvp_int()) // 添加节点的位置索引序号
				.forEach(tup -> {
					final var node = tup._2(); // 提取node结构
					if (!predicate_data_node.test(node)) { // 仅 数据类型的节点 才 给予 进行 算符归集
						final var opnode = Ops.lookup(tup._2().getName()).get(); // 算符原型的对象
						opnodes.add(TUP2(tup._1(), PACK(opnode))); // 转换成算符队形给予保存
					} // if
				});

		println("\nnodes的当前状态:", nodes);
		final var node_rb = IRecord.rb("no,name,是否是算符");
		final var dfm = nodes.stream().map(kvp_int()).map(kvp -> {
			return node_rb.get(kvp._1(), kvp._2().getName(), !predicate_data_node.test(kvp._2()));
		}).collect(DFrame.dfmclc);
		println(dfm);
		println("运算符序列", opnodes);

		// 逗号表达式直接返回，这是图省事，省心,鸵鸟的办法，眼不见心不烦，物质决定意识，意识 可以 选择 见不见物质，哈哈
		if (opnodes.stream().allMatch(e -> COMMA_TEST(e._2().getName()))) {
			return null;
		} // if

		// [(比较结果,[(op1_index,op1_node),(op2_index,op2_node)])]
		final var compare_groups = IRecord.slidingS(opnodes, 2, 1, true).map(opnode_pair -> {
			final var opnode1 = opnode_pair.get(0)._2(); // 运算符的 Node结构
			final var opnode2 = opnode_pair.get(1)._2(); // 运算符的 Node结构
			final var result = Ops.priority_cmp(opnode1, opnode2); // 运算符的比较等级
			return TUP2(result, opnode_pair);
		}).collect(Collectors.toList()); // compare_groups

		// 结果排序后查看
		compare_groups.forEach(e -> { // 排序分组遍历
			final var result = e._1;
			final var opnode_pair = e._2;
			final var opnode1 = opnode_pair.get(0)._2(); // 运算符的 Node结构
			final var opnode2 = opnode_pair.get(1)._2(); // 运算符的 Node结构
			println("算符优先级比较记录:compare", TUP2(opnode_pair.get(0)._1(), opnode1.getName()),
					TUP2(opnode_pair.get(1)._1(), opnode2.getName()), "--->", result);
		}); // while

		/**
		 * 关于优先级传递问题的解决： 所谓优先级传递，看一个例子: (1,2+3*5) , <br>
		 * 依据分组规则:比较分组为 [ ( -1, (, +) ), ( -1, (+ ,*) ) ], <br>
		 * 注意到这里 出现 '+' 运算符的 优先级 传到 '*' , 也就是 优先级 不能够 读取 算符比较的 最小值, <br>
		 * 而是 要 顺延 比较组 的间 优先级 的递增顺序 给予 传导, 直到 传导链条 链条 终止。 <br>
		 * 也就是 本次使用优先级
		 */
		int i = compare_groups.stream().map(kvp_int()).filter(e -> e._2._1 < 0) // 找出第一个出现 优先级差异的
				.findAny().map(e -> e._1).orElse(0); // 目标算符 的 优先级比较分组的下标索引
		final int n = compare_groups.size(); // 比较组的列表长度

		// 比较结果<0即后续运算的优先级高度前面元素的优先级的时候才给与进行
		if (n > 0 && compare_groups.get(i)._1 < 0) {
			// 邻近的比较群的 关系处理
			while (i < n - 1) { // group 比较群 , tuple 比较对儿, 一个 比较群 中 包含 两个连续的 比较对二组
				final var group_1 = compare_groups.get(i); // 当前比较群，1#比较群
				final var group_2 = compare_groups.get(i + 1); // 当前比较群的 后继 比较群， 2#比较群
				final var tuples_1 = group_1._2(); // 当前 即 1# 比较群的 比较对儿组
				final var tuples_2 = group_2._2(); // 后继 即 2# 比较群的 比较对儿组
				final int tup1_index2 = tuples_1.get(1)._1; // 第一比较组的成员表 的 第二个元素 的 下标索引
				final int tup2_index1 = tuples_2.get(0)._1; // 第二比较组的成员表 的 第一个元素 的 下标索引
				final int tup2_index2 = tuples_2.get(1)._1; // 第二比较组的成员表 的 第二个元素 的 下标索引

				// 紧挨着的两个比较组出现了优先级的递增梯度 , 出现了 3 * sin cos x 这样的情况 '*'<'sin'='cos',并且sin的右侧紧挨着cos
				final var transit = (group_1._1() < 0 && group_2._1() < 0); // 优先级在算符从前向后依次递增的状态
				final boolean ajacent = tup1_index2 == tup2_index1; // 算符是否比邻: 1#比较对儿的2#位符号 与 2#比较对儿的1#位符号 相同
				final boolean composed = tup2_index1 + 1 == tup2_index2; // 出现sin cos tan ... 这样的循环嵌套的情况，即 符号一个接一个的出现
				if (transit && ajacent) { // 优先级 传递 以及 高级别算符 出现了 比邻显现 3 + sin cos x 需要把优先级 从 sin 传递到 cos
					i++; //
				} else if (composed) { // 参数被复合， 出现sin cos tan ... 这样的循环嵌套的情况，即 符号一个接一个的出现
					i++;
				} else { // 不做优先级传递
					break;
				} // if

			} // while

			println("目标算符优级分组 传转 到 索引位置:", i, "最优比较组 为:", compare_groups.get(i));
		} // if

		return TUP2(i, compare_groups);
	}

	/**
	 * 数据预处理 <br>
	 * 
	 * 算符的优先级处理 比如: <br>
	 * <br>
	 * 对于 节点序列 1+2*3 即 节点序列 [1,+,2,*,3 ], <br>
	 * 由于 * 比 + 优先级高，所以 * 会优先 进行 参数实装, 也就是 生成一个 运算节点 (*2,3) <br>
	 * 会 返回 [1,+,(*,2,3)] 这样的 三个 节点的 节点序列,<br>
	 * 注意 (*,2,3)是一个复合节点 或是 算符“*”的实装节点 .<br>
	 * 也就是 在 数据被规约前 事先将 优先级高的 算符 给予 提前 实装出来。<br>
	 * <br>
	 * 算符的优先处理 会 采用 递归的方式 进行处理 , 每次 只规约 当前 优先级 差别最高的算符 <br>
	 * 例如 对于逗号表达式: 2,3+5*6 由于 `,`&lt;`+`&lt;`*` 所以,<br>
	 * 这里 会 优先 规约"*"算符 实装成 (*,5,6) 得到 [2, "," , 3, +, (*,5,6) ] <br>
	 * 然后规约 “+” 得到 [2, "," , (+,3,(*,5,6)) ] <br>
	 * 这样就 只剩下了 三个元素的 节点序列 为止， 其实 这就是 一个 最简答的 逗号表达式了 <br>
	 * <br>
	 * 对于 没有优先级别差异的 节点序列 比如 1+2-3 或是 1/2*3 这样的节点序列，将不予处理，给予 直接返回
	 * 
	 * @param nodes 节点序列
	 * @return 优先为高级别的算符匹配相应的参数
	 */
	public List<Node> preProcess(final List<Node> nodes) {

		final Predicate<Node> predicate_data_node = node -> { // 节点分两类:算符节点,数据节点 即 一个 node 不是 算符 就是 数据
			if (is_op_word.test(node)) { // 是否是一个算符关键词, 是 算符关键词 需要 继续判断 是否一个 计算表达式
				if (node.isOp()) { // 合法算符的情况
					return node.getOp().getArgs()._1() != null; // 实装了参数的算符 就变成了数据 否则依旧是算符
				} else { // 没有计算能力，只是 长得像算符
					return false;
				} // if
			} else { // 不是算符关键词 则 一定是 数据node
				return true;
			} // if
		};

		/**
		 * 生成比较列表
		 */
		final var compare_result = this.compareGroups(nodes, predicate_data_node);
		if (compare_result == null) { //
			println("优先级顺滑无需处理");
			return nodes;
		}

		final var groups_data = compare_result._2; // 比较分组数据列表
		final var target_grp_index = compare_result._1; // 目标 算符比较分组 所在 索引序号, 目标算符 是 目标 分组的 第二个 元素。
		final var min = groups_data.stream().collect(Collectors.summarizingDouble(e -> e._1)).getMin(); // 确定最小值
		if (groups_data.size() < 1 || min >= 0) { // 数据优先级正序不需要处理
			println("数据优先级正序不需要处理", nodes);
			return nodes; // 返回结果
		} else { // 算符之间存在优先级差别，需要给予计算
			// 邻近的算符优先级的比较
			final var targetGroup = groups_data.get(target_grp_index); // 目标比较组
			println("\n优先级差异最大的算符对儿为", targetGroup);
			println("尝试为:" + targetGroup._2().get(1) + "确定计算参数,即 创建算符的规约结构");

			final var op_tuple = targetGroup._2().get(1);// 目标算符
			final var index = op_tuple._1(); // 目标算符的语句小标索引
			final var op = op_tuple._2().getOp(); // 提取算符原型 并 尝试 生成 算符的 运算结构
			final var tobe_removed = new ArrayList<Integer>(); // 将要移除的节点的索引位置
			final var params_installed = new AtomicInteger(2); // 实装的参数数量 默认为2 即 二元参数
			final var handle = Optional.of(op.getAry()).map(nary -> { // 算符的元数
				final Function<Node, Object> unpack = node -> { // 解包数据，元素类型
					return node.isToken() ? node.tokenValue() : node.unpack();
				};
				final Supplier<Node> unitary_routine = () -> { // 一元算符
					params_installed.set(1); // 实装参数数量标记为1
					final var arg = nodes.get(index + 1);
					tobe_removed.add(index + 1); // 记录参数索引
					if (COMMA_TEST(arg.getName()) && op.getAry() == 2) { // 逗号表达式
						final var oo = arg.getOp().getArgsS().map(Node::UNPACK).toArray(Object[]::new);
						final var left = oo.length > 0 ? oo[0] : null; // 左参数
						final var right = oo.length > 1 ? oo[1] : null; // 右参数

						println("实装" + op_tuple + "\n\t第一参数" + left + "\n\t");
						println("实装" + op_tuple + "\n\t第二参数" + right + "\n\t");
						return PACK(op.duplicate().compose(oo[0], oo[1]));
					} else {
						println("实装" + op_tuple + "\n\t第一参数" + arg + "\n\t");
						return op instanceof UnaryOp ? PACK(((UnaryOp<?>) op).compose(unpack.apply(arg)))
								: PACK((op).compose1(unpack.apply(arg)));
					} // if
				}; // unary_routine

				tobe_removed.add(index); // 记录算符的索引位置
				if (nary == 1) { // 一元函数参数在右边
					return unitary_routine.get();
				} else if (nary == 2) { // 二元运算符
					println("-----------------------------------");
					println("组装算符 op:", op);
					println("节点序列 nodes:", nodes);
					println("-----------------------------------");

					final var left = index < 1 ? null : nodes.get(index - 1); // 第一参数
					final var right = nodes.get(index + 1); // 第二参数
					final var flag = left == null || (!predicate_data_node.test(left)) // op 左侧不是一个数据而是算符
							&& COMMA_TEST(right.getName()); // 参数1是一个算符的情况是不能做方法参数的

					if (!flag) { // 有效的二元函数
						Stream.of(index - 1, index + 1).forEach(tobe_removed::add);
						println("\n实装" + op_tuple + "\n\t第一参数:" + left + "\n\t 第二参数:" + right + "\n");
						return PACK(op.duplicate().compose(unpack.apply(left), unpack.apply(right)));
					} else { // 视作一元函数
						println("由于算符" + op_tuple + "的:" + "右侧参数 " + right + " 是一个逗号表达式的元组,\n\t" + "左侧参数 " + left
								+ " 是一个算符,\n\t" + "故判断此为二元函数的前缀形式,使用右侧元组参数来实装该算符.");
						return unitary_routine.get();
					} // if
				} else { // 非法的 函数 元数
					System.err.println("函数的元数非法" + nary);
					return null;
				} // if
			}).orElse(null); // handle 规约句柄

			final var pos = params_installed.get() == 2 // handle回填到节点序列的位置,即 参数的实装状态决定
					? index - 1 // 实装了二个参数,插入位置为左侧参数位置
					: index; // 没有实装二个参数,插入位置为算符位置
			final var buffer = new LinkedList<Node>(); // 工作缓存

			println(op_tuple + "实装为:\n\t", handle);
			println("--------------------------");
			nodes.stream().map(kvp_int()) // 加入位置索引序号
					.filter(kvp -> !tobe_removed.contains(kvp._1())).map(e -> e._2()) // 提取数据项
					.forEach(buffer::add);
			buffer.add(pos, handle);

			// 一次调整之后 数据
			println("整理之后的新的节点序列状态:", buffer);
			return this.preProcess(buffer); // 递归进行数据处理
		} // 算符优先级比较
	}

	/**
	 * 层次化处理 <br>
	 * 
	 * 把一个带有层级标识（左，右括号）的词法节点序列data处理成对应的层级结构即AST语法树。<br>
	 * 把 一个 节点 序列 根据 层次化 指令节点 LEFT_PARENT "(" , RIGHT_PARENT ")" 的 指导 进行 结构规约 即 层级构建
	 * 
	 * @param data         词法节点序列
	 * @param stack        运行堆栈，当为 null 的时候进行自我规约
	 * @param reduceAction 规约算法，是一种 肖分组规约 契机, 在 reduceAction 中 算符 按照 同一优先级 从左到右 依次规约
	 * @return AST 语法树
	 */
	public Node handle(final List<Node> data, final Stack<Node> stack, final Function<List<Node>, Node> reduceAction) {

		final var _stack = stack == null ? new Stack<Node>() : stack;
		final var batchTerms = new LinkedList<Node>(); // 处理批次的表达式项目,处于同一优先级处理级别的数据项
		final var itr = data.iterator(); // 数据迭代器

		println("handle : ", data);
		while (itr.hasNext()) { // 优先级调整之后的 语法树构建
			final var currentNode = itr.next();

			// 读取到了终点 或 读取到了分组结束,需要判断 间或 条件以保证 末尾元素恶意得到处理
			if (!itr.hasNext() || name_eq.test(currentNode, RIGHT_PARENT)) { // 向前回溯，准备规约

				// 已经清空读取堆栈 或是 读回了 分组开始
				Node node = null; // 栈顶元素
				while (!_stack.empty() && !name_eq.test(node = _stack.pop(), LEFT_PARENT)) {
					batchTerms.addFirst(node);
					println(batchTerms, " <- 出栈 ", _stack);
				} // while

				// 由读取末尾条件出发的规约 : 把末尾节点加入到buffer之中来
				if (!itr.hasNext() && !name_eq.test(currentNode, RIGHT_PARENT)) {
					batchTerms.add(currentNode);
				}

				println("尝试规约:", batchTerms);
				_stack.push(reduceAction.apply(batchTerms)); // 规约节点压栈
				println("\n 规约后 stack 状态:");
				_stack.stream().map(kvp_int()).sorted((a, b) -> b._1() - a._1()).forEach(e -> {
					println(e._1(), e._2());
				});
				batchTerms.clear();
				println("----------------------\n");
			} else { // 数据暂存继续向前读取
				_stack.push(currentNode);
				println(currentNode, " 压栈 -> ", _stack);
			}
		}

		final var ret = _stack.empty() ? null : _stack.peek(); // 提取栈顶元素作为返回值
		println("规约值", ret, "\n堆栈状况:", _stack);
		if (ret == null) {
			println("规约成了一个空值，出现了数据异常");
		}

		return ret;
	}

	/**
	 * 没有 优先级识别能力的 节点项序列 算法 <br>
	 * 
	 * 扁平化的 词法节点序列 的 语法树构建 <br>
	 * reduce 没有 算符识别能力 只是 单一的 按照 算符的 出现 顺序 从左到右逐一的 进行 节点序列的 规约， <br>
	 * 如果 需要 使用 带有 算符 优先级 识别 能力的 节点规约 请使用 buildTree 算法。 <br>
	 * 
	 * @param terms 词法节点项序列
	 * @return 语法树的根节点
	 */
	public Node reduce(final Collection<Node> terms) {
		final var seq = terms.stream().map(e -> e.isToken() ? e.getName() : e.toString()) // 提取项目名称
				.map(e -> COMMA_TEST(e) ? "','" : e) // 对 "," 进行转义
				.collect(Collectors.toList());

		println("\n----------------------------");
		println("terms 项目的计算:", seq);
		println("----------------------------");

		final Function<Node, Object> node2arg = node -> {
			if (node == null)
				return null;
			return node.isOp() ? node.unpack() : node.tokenValue(); // 蜕掉糖衣转化为参数数据
		};
		final var elemStack = new Stack<Node>(); // 数据堆栈，数据项堆栈
		final var opStack = new Stack<BinaryOp<Object, Object>>(); // 运算堆栈，算符堆栈
		final var ai = new AtomicInteger(0);

		terms.forEach(term -> { // 逐项目处理
			final var name = term.getName(); // term 项目的名称
			final var i = ai.getAndIncrement(); // 下标索引
			final Optional<BinaryOp<Object, Object>> opOptional = // 尝试从 term 中 提取 算符，算符 只能只能 从 token 里提取，handle 不可以。
					term.isToken() // term.isToken()用来保证规约掉的handle(handle可以视为一种复合数据term)不会被重复计算,即只能从纯token中提取算符。
							? Ops.lookup(name) // 尝试根据term的name提取 Ops 对象
							: Optional.empty(); // 无效的算符

			println(i, name, opOptional); // 打印term 摘要

			// 语法树 的 构建 是 通过 数据 term 来 触发 算符规约 来进行 的。要知道 数据项 elem 是条件，是动力！！！
			// 这就是为何 语法树的 句柄 handle 的 构建 是位于 数据项分支 之中的 原因。
			// 数据项term又叫elem, 使用elemStack保存
			// 算符保存在opStack

			if (opOptional.isPresent()) { // 算符项分支:term词素所代表的名称是一个有效的运算操作, 读取到 算符，记录到算符栈, 等待 被数据项目term唤醒。
				opStack.push(opOptional.get()); // 加入 运算堆栈
			} else {// 数据项分支:读取到了数据元素,计算 规约关键 是通过 数据项目term 来触发 算符栈 中的 算符 来进行的。数据是 动力, 算符是 本质。 尝试 唤醒
					// 算符
				if (!opStack.isEmpty()) { // 算符栈 含有 算符 表明 该term项目可能触发出 计算规约 即 生成 计算句柄 handle 的 可能。
					final BinaryOp<Object, Object> binaryOp = opStack.peek(); // 查看栈定元素
					final var nary = binaryOp.getAry();
					if (nary == 1) { // 一元运算符
						final var arg = UNPACK(node2arg.apply(term));
						final var handle = ((UnaryOp<Object>) opStack.pop()).compose(arg); // 一元算符 规约句柄
						println("规约成 一元运算符:", handle);
						elemStack.push(PACK(handle));
					} else if (nary == 2) { // 二元运算符
						final var comma_term_flag = term.isOp() && COMMA_TEST(term.getName()); // COMMA 的条件检测
						if (!elemStack.isEmpty() || comma_term_flag) { // 二元运算 对于 除了 COMMA-逗号term以外,要求 elemStack 至少有一个元素
							final var oo = (elemStack.isEmpty() // 根据堆栈数据状态 确定算符的参数
									? term.getOp().getArgsS() // comma_term_flag,逗号表达式, 拆分逗号表达式的参数 来构造左右参数
									: Stream.of(elemStack.pop(), term).map(node2arg)).map(Node::UNPACK) //
									.toArray(); // 堆栈不为空从数据栈中提取一个数据合并当前项目term作为方法参数
							final var handle = (opStack.pop()).compose(oo[0], oo[1]); // 二元算符 规约句柄
							println("规约成 二元运算符:", handle);
							elemStack.push(PACK(handle));
						} else { // elemStack 为空
							elemStack.push(term);
						} // if elemStack
					} else {
						System.err.println("不支持[" + nary + "]元的数据运算");
					} // if ops
				} else { // opStack 中不包含算符 无法触发 计算程序：即规约节点, 将数据 保存到 elemStack 等待后续被唤醒
					elemStack.push(term);
				} // if opsStack
			} // if term
		}); // terms.forEach

		println("elemStack:", elemStack);
		println("opsStack:", opStack);

		// 计算符号栈中的尚未计算完全的运算, 注入 嵌套类 函数 需要如此 规约 比如 sin cos (tan x) 这样函数
		while (!opStack.isEmpty()) {
			final var op = opStack.pop(); // 操作符堆栈
			final BinaryOp<Object, Object> handle = Optional.of(op.getAry()) // 尝试生成规约句柄
					.map(nary -> { // 根据 参数元数 生成规约句柄
						if (nary == 2) { // 二元运算
							final var oo = Stream.iterate(0, i -> i + 1).limit(2)
									.map(i -> elemStack.empty() ? null : elemStack.pop()) // 依次从数据项堆栈弹出数据
									.map(node2arg).map(Node::UNPACK).toArray(); // oo[0] 右边的参数, oo[1] 左边
							// 对于 采用
							final var flag = (oo[1] == null && oo[0] != null); // 是否交换1,参数的位置
							final var left = flag ? oo[0] : oo[1];
							final var right = flag ? oo[1] : oo[0];

							return op.compose(left, right);
						} else if (nary == 1) {
							final var arg = elemStack.pop().unpack(); // 提取第一参数并给予 解包

							// 注意一定要用 一元函数的 compose, 以保证 调用 一个元算符 重载的 compose 否则就会更改掉 算符的 元数
							// 若是 调用 op.compose(arg,null) 就是调用二元函数的compose 生成一个 BinaryOp 类型的结构 而 非 Op的真实
							// 类型，即 op 的真实类型丢失了
							return ((UnaryOp<?>) op).compose(arg);
						} else {
							System.err.println("非法操作符" + op);
							return null;
						}
					}).orElse(null); // 规约句柄

			elemStack.push(PACK(handle));
		} // while

		final var resultNode = elemStack.pop();
		println("AST:", resultNode);

		return resultNode;
	}

	/**
	 * 
	 * 构建语法树 <br>
	 * 
	 * 非扁平化 词法节点序列 的语法树构建 即 <br>
	 * 带有优先级的识别功能的 语法树构建，为不同优先级的算符构建其对应的层级结构。 <br>
	 * 需要数据中不能包含 分组 符号:LEFT_PARENT,RIGHT_PARENT <br>
	 * 
	 * @param nodes 词法节点序列,不能含有 LEFT_PARENT,RIGHT_PARENT 两个字符
	 * @return AST 语法树
	 */
	public Node buildTree(final List<Node> nodes) {
		final var opt = nodes.stream() // 检测非法字符
				.filter(e -> name_eq.test(e, LEFT_PARENT) || name_eq.test(e, RIGHT_PARENT)).findFirst();

		if (opt.isPresent()) {
			System.err.println("buildTree 数据中不能包含分组符号:" + Arrays.asList(LEFT_PARENT, RIGHT_PARENT));
			return null;
		}

		return this.handle(this.preProcess(nodes), null, this::reduce);
	}

	/**
	 * 词素(词法符号 token) 的 结构 化分析 即 语法树的生辰 <br>
	 * 
	 * analyze 词素结构化 分成了 两个层次 <br>
	 * 1) buildTree 过程 : 把 有优先级差异 的 节点序列 分组成 一 系列的 无差异 分组，<br>
	 * 1.1) 先 深一个 阶层 做 无差异 节点 规约，<br>
	 * 1.2) 而后 再退回 来 继续 做 无优先级差异 节点。 <br>
	 * 简单的说就是 把 一个 优先级有差异的节点 序列 转换 成 无差异序列<br>
	 * 即 无差异节点序列 的 树形结构（层级）的 过程。也就是 节点层次化。 <br>
	 * 对应算法 buildTree <br>
	 * 2) reduce 过程 : 无优先级差异的 节点规约 对应 算法 reduce <br>
	 * 
	 * @param tokens 符号流
	 * @return 语法树结构 AST
	 */
	public Node analyze(final List<IRecord> tokens) {
		final var data = tokens.stream().map(Node::PACK).collect(Collectors.toList()); // 生成数据节点序列
		final var stack = new Stack<Node>(); // 节点工作栈

		/**
		 * buildTree 是一种 带有 算符 优先级 识别 能力的 规约 算法，所以
		 */
		this.handle(data, stack, this::buildTree); // 处理节点工作栈的

		if (stack.size() > 1) {
			var state = new Stack<Node>(); // 状态检测数据栈
			final var ret = this.handle(enclose(stack), state, this::buildTree);
			if (state.size() > 1) { // 正常情况 数据栈中 只有一个一个元素 超过一个 就是存在 语法错误
				try {
					throw new Exception(println("\n stack:", stack, "\n tokens:", tokens, "\n存在法语法错误"));
				} catch (Exception e) {
					e.printStackTrace();
				} // try
			} // if
			return ret;
		} else {
			return stack.peek();
		}
	}

	/**
	 * 分析一个 词法序列 生成 对应表达式 <br>
	 * (2*pow(x+1,2)+x) 对于 pow 这样的二元 函数 采用 前缀式 写法 , 即 逗号表达式的元组 来 提供参数的 情况, <br>
	 * 采用分组 提交的方式 会 逃避掉 buildTree 的 优先级的 方法调整，会造成 语法 分析失败， 所以 请去掉 分组 符号 <br>
	 * analyze("2*pow(x+1,2)+x") 就可以了。 <br>
	 * analyze("(2*pow(x+1,2)+x)") 会造成 分组混乱
	 *
	 * @param line 表达式的字符串表达
	 * @return Node
	 */
	public INode analyze(final String line) {

		final var tokens = tokenize(line); // 提取词法序列
		return this.analyze(tokens);
	}

	/**
	 * 
	 */
	public interface INode {
		/**
		 * 节点计算
		 * 
		 * @param bindings 变量参数的数据绑定
		 * @return 节点计算后的结果
		 */
		Object evaluate(final Map<String, Object> bindings);

		/**
		 * 语法树dump <br>
		 * 
		 * 所谓语法树就是一个计算结构：<br>
		 * 类似于如下结构： <br>
		 * (5*pow(2,5)*tan(45.6/255-cos(4/6))) <br>
		 * 
		 * * ----> (*,5,(*,(pow,(',',2,5),null),(tan,(-,(/,45.6,255),(cos,(/,4,6))))))
		 * <br>
		 * | 5 <br>
		 * | * ----> (*,(pow,(',',2,5),null),(tan,(-,(/,45.6,255),(cos,(/,4,6))))) <br>
		 * | | pow ----> (pow,(',',2,5),null) <br>
		 * | | | , ----> (',',2,5) <br>
		 * | | | | 2 <br>
		 * | | | | 5 <br>
		 * | | | null <br>
		 * | | tan ----> (tan,(-,(/,45.6,255),(cos,(/,4,6)))) <br>
		 * | | | - ----> (-,(/,45.6,255),(cos,(/,4,6))) <br>
		 * | | | | / ----> (/,45.6,255) <br>
		 * | | | | | 45.6 <br>
		 * | | | | | 255 <br>
		 * | | | | cos ----> (cos,(/,4,6)) <br>
		 * | | | | | / ----> (/,4,6) <br>
		 * | | | | | | 4 <br>
		 * | | | | | | 6 <br>
		 */
		String dumpAST();

		/**
		 * 节点计算
		 * 
		 * @param bindings 变量参数的数据绑定,参数名，参数值 序列，即:key1,value,key2,value2,...
		 * @return 节点计算后的结果
		 */
		default Object evaluate(final Object... bindings) {
			final var lhm = new HashMap<String, Object>();
			for (int i = 0; i < bindings.length - 1; i++) {
				lhm.put(bindings[0] + "", bindings[i + 1]);
			}
			return this.evaluate(lhm);
		}
	}

	/**
	 * 数据 统一 结构化 处理 即 统一使用 ArrayList 来管理数据
	 * 
	 * @param collection 数据结合
	 * @return 列表结构的数据集合
	 */
	final static List<Node> enclose(final Collection<Node> collection) {
		return new ArrayList<Node>(collection);
	}

	final static BiPredicate<Node, Node> name_eq = (a, b) -> a.getName().equals(b.getName()); // 名字相等
	final static Node LEFT_PARENT = REC("type", "op", "value", "(").mutate(Node::PACK);
	final static Node RIGHT_PARENT = REC("type", "op", "value", ")").mutate(Node::PACK);

}
