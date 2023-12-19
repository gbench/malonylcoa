package gbench.util.jdbc.node;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import gbench.util.jdbc.kvp.Tuple2;

/**
 * 二元元素的端点元素,这是一个类似于Lisp CONS 的结构
 *
 * @param <T> 元素的值的类型
 * @author gbench
 */
public class Elem<T> extends Tuple2<T, Elem<T>> {

	/**
	 * 构造函数
	 *
	 * @param _1 元素数据
	 * @param _2 元素指针（父节点或者next节点【如何理解取决于理解视角】的元素指针）
	 */
	public Elem(T _1, Elem<T> _2) {
		super(_1, _2);
	}

	/**
	 * 元素数据
	 */
	public Elem(T _1) {
		super(_1, null);
	}

	/**
	 * 获取属性值
	 *
	 * @param key   键名
	 * @param value 兼职
	 * @return 元素对象本身
	 */
	public Elem<T> setAttribute(final Object key, Object value) {
		attributes.put(key, value);
		return this;
	}

	/**
	 * 获取属性值
	 *
	 * @param key 键名
	 * @return key对应的属性值
	 */
	public Object getAttribute(final Object key) {
		return attributes.get(key);
	}

	/**
	 * 获取属性集合
	 */
	public Map<Object, Object> getAttributes() {
		return attributes;
	}

	/**
	 * 是指属性集合
	 */
	public void setAttributes(final Map<Object, Object> attributes) {
		this.attributes = attributes;
	}

	/**
	 * 提取元素张成的路径数据
	 *
	 * @param <T>  数据元素类型
	 * @param elem 元素对象
	 * @return 元素的路径流
	 */
	public static <T> Stream<T> data(final Elem<T> elem) {
		return elem._2() == null ? Stream.of(elem._1()) : Stream.concat(data(elem._2()), Stream.of(elem._1()));
	}

	/**
	 * 提取元素张成的路径数据
	 *
	 * @return 元素的路径流
	 */
	public Stream<T> data() {
		return data(this);
	}

	/**
	 * 提取元素张成的路径数据
	 * 
	 * @param <T>  数据元素类型
	 * @param elem 元素对象
	 * @return 元素elem的路径流
	 */
	public static <T> Stream<Elem<T>> data2(final Elem<T> elem) {
		return elem._2() == null ? Stream.of(elem) : Stream.concat(data2(elem._2()), Stream.of(elem));
	}

	/**
	 * 提取元素张成的路径数据
	 * 
	 * @return 元素elem的路径流
	 */
	public Stream<Elem<T>> data2() {
		return data2(this);
	}

	/**
	 * construct 函数,拼接成 一般链表结构<br>
	 * 把t设置为新的路径的 端点元素，尾（叶子节点）节点或首节点，取决于理解实施奥
	 * 
	 * @param <T>  数据元素类型
	 * @param t    端点元素的数据
	 * @param elem t 关联的端点元素
	 * @return 数据为t的端点元素
	 */
	public static <T> Elem<T> cons2(final T t, final Elem<T> elem) {
		return new Elem<>(t, elem);
	}

	/**
	 * construct 函数，拼接成 一般链表结构<br>
	 * 把t设置为新的路径的 端点元素，尾（叶子节点）节点或首节点，取决于理解实施奥
	 * 
	 * @param t 端点元素的数据
	 * @return 数据为t的端点元素
	 */
	public Elem<T> cons2(final T t) {
		return cons2(t, this);
	}

	/**
	 * 数据全排列组合<br>
	 * final var digits = "0123456789".split(""); <br>
	 * cph(Elem::new,Arrays.asList(digits,digits,digits))<br>
	 * .map(e->e.data().collect(Collectors.toList()))<br>
	 * .forEach(System.out::println);<br>
	 * 
	 * @param <T>          元素类型
	 * @param sss          基础元素数组(SourceS arrayS), 例如
	 *                     Arrays.asList(digits,digits,digits)
	 * @param elem_creator Elem 的 创建函数,类型为： (t,parent)->Elem&lt;T&gt;
	 * @return 新的排列组合列表流
	 */
	public static <T> Stream<Elem<T>> cph(final BiFunction<T, Elem<T>, Elem<T>> elem_creator, final List<T[]> sss) {
		if (sss.size() < 1) {
			System.err.println("基础元素数组列表长度为0,至少需要包含有一个基础元素数组才能够做cph展开！");
			return Stream.of();
		}

		return sss.size() < 2 ? Stream.of(sss.get(sss.size() - 1)).map(e -> elem_creator.apply(e, null))
				: cph(elem_creator, sss.subList(0, sss.size() - 1))
						.flatMap(p -> Stream.of(sss.get(sss.size() - 1)).map(e -> elem_creator.apply(e, p)));// p:parent,e:elem
	}

	/**
	 * 数据全排列组合<br>
	 * final var digits = "0123456789".split(""); <br>
	 * cph(Elem::new,digits,digits,digits)<br>
	 * .map(e->e.data().collect(Collectors.toList()))<br>
	 * .forEach(System.out::println);<br>
	 * 
	 * @param <T>          元素类型
	 * @param sss          基础元素数组(SourceS arrayS), 例如
	 *                     Arrays.asList(digits,digits,digits)
	 * @param elem_creator Elem 的创建函数 类型为： (t,parent)->Elem&lt;T&gt;
	 * @return 新的排列组合列表流
	 */
	@SafeVarargs
	public static <T> Stream<Elem<T>> cph(final BiFunction<T, Elem<T>, Elem<T>> elem_creator, final T[]... sss) {
		final BiFunction<T, Elem<T>, Elem<T>> default_elem_creator = Elem::new;
		return cph(elem_creator == null ? default_elem_creator : elem_creator, Arrays.asList(sss));
	}

	/**
	 * 数据全排列组合<br>
	 * final var digits = "0123456789".split(""); <br>
	 * cph(digits,digits,digits)<br>
	 * .map(e->e.data().collect(Collectors.toList()))<br>
	 * .forEach(System.out::println);<br>
	 * 
	 * @param <T> 元素类型
	 * @param sss 基础元素数组(SourceS arrayS), 例如 Arrays.asList(digits,digits,digits)
	 * @return 新的排列组合列表流
	 */
	@SafeVarargs
	public static <T> Stream<Elem<T>> cph(final T[]... sss) {
		return cph(Elem::new, Arrays.asList(sss));
	}

	/**
	 * 数据全排列组合<br>
	 * final var digits = "0123456789".split(""); <br>
	 * cph(Arrays.asList(digits,digits,digits))<br>
	 * .map(e->e.data().collect(Collectors.toList()))<br>
	 * .forEach(System.out::println);<br>
	 * 
	 * @param <T> 元素类型
	 * @param sss 基础元素数组(SourceS arrayS), 例如 Arrays.asList(digits,digits,digits)
	 * @return 新的排列组合列表流
	 */
	public static <T> Stream<Elem<T>> cph(final List<T[]> sss) {
		return cph(Elem::new, sss);
	}

	/**
	 * 数据全排列组合：<br>
	 * final var digits = "0123456789".split(""); <br>
	 * cph2(Arrays.asList(digits,digits,digits)).forEach(System.out::println);<br>
	 * 
	 * @param <T> 元素类型
	 * @param sss 基础元素数组(SourceS arrayS), 例如 Arrays.asList(digits,digits,digits)
	 * @return 新的排列组合列表流
	 */
	public static <T> Stream<List<T>> cph2(final List<T[]> sss) {
		return cph(Elem::new, sss).map(e -> e.data().collect(Collectors.toList()));
	}

	/**
	 * 数据全排列组合：<br>
	 * final var digits = "0123456789".split(""); <br>
	 * cph2(digits,digits,digits).forEach(System.out::println);<br>
	 * 
	 * @param <T> 元素类型
	 * @param sss 基础元素数组(SourceS arrayS), 例如 :digits,digits,digits
	 * @return 新的排列组合列表流
	 */
	@SafeVarargs
	public static <T> Stream<List<T>> cph2(final T[]... sss) {
		return cph(Elem::new, Arrays.stream(sss).collect(Collectors.toList()))
				.map(e -> e.data().collect(Collectors.toList()));
	}

	/**
	 * 数据全排列组合：<br>
	 * final var digits = "0123456789".split(""); <br>
	 * cph2(REC("a",digits,"b",digits) <br>
	 * .filter(p->p.value().getClass().isArray()) <br>
	 * .reduce(p->(Stream&lt;String[]&gt;)Stream.of(p._2()),Stream.of(),Stream::concat))
	 * .forEach(System.out::println);<br>
	 * 
	 * @param <T>    元素类型
	 * @param stream 基础元素数组(SourceS arrayS), 例如 :digits,digits,digits
	 * @return 新的排列组合列表流
	 */
	public static <T> Stream<List<T>> cph2(final Stream<T[]> stream) {
		return cph(Elem::new, stream.collect(Collectors.toList())).map(e -> e.data().collect(Collectors.toList()));
	}

	/**
	 * 数据全排列组合：<br>
	 * final var digits = Arrays.asList("0123456789".split("")); <br>
	 * cph2(digits,digits).forEach(System.out::println);<br>
	 * 
	 * @param <T> 元素类型
	 * @param ll  基础元素数组(SourceS List),每个元素都是一个列表, 例如 :digits,digits,digits
	 * @return 新的排列组合列表流
	 */
	@SuppressWarnings("unchecked")
	@SafeVarargs
	public static <T> Stream<List<T>> cph2(final List<T>... ll) {

		return cph2(Arrays.stream(ll).map(e -> e.toArray(n -> (T[]) Array.newInstance(componentType(e), n))));
	}

	/**
	 * 获取列表的元素类型类
	 * 
	 * @param <T> 元素类型
	 * @param ll  元素列表,当ll为空或是长度小于1的时候,返回 null
	 * @return 元素的类型类
	 */
	@SuppressWarnings("unchecked")
	public static <T> Class<T> componentType(final List<T> ll) {
		if (ll == null || ll.size() < 1)
			return null;
		final var componentType = ll.stream().filter(Objects::nonNull).findFirst().map(e -> (Class<T>) e.getClass())
				.orElse((Class<T>) Object.class);
		return componentType;
	}

	/**
	 * 序列号
	 */
	private static final long serialVersionUID = -4970370960765747394L;

	private Map<Object, Object> attributes = new HashMap<>();// 属性存储
}