package gbench.util.array;

import java.util.function.*;
import java.util.Arrays;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;

import gbench.util.lisp.Tuple2;

/**
 * AbstractNdarray 方便由于 各种 INdarray 的实现
 * 
 * @author gbench
 *
 * @param <T> 元素类型
 */
public abstract class AbstractNdarray<T> implements INdarray<T> {

	/**
	 * 构造函数
	 *
	 * @param data    源数据数值
	 * @param start   开始索引从0开始,inclusive
	 * @param end     结束索引，exlusive
	 * @param strides 分区索引跨度
	 */
	public AbstractNdarray(final T[] data, final int start, final int end,
			final LinkedHashMap<List<Integer>, Tuple2<Integer, Integer>> strides) {
		this.data = data;
		this.start = start;
		this.end = end;
		this.strides = strides;
	}

	/**
	 * 数据数组
	 *
	 * @return data
	 */
	@Override
	public T[] data() {
		return this.data;
	}

	/**
	 * 开始索引
	 *
	 * @return start 从0开始,inclusive
	 */
	@Override
	public int start() {
		return this.start;
	}

	/**
	 * 结束索引
	 *
	 * @return end exclusive
	 */
	@Override
	public int end() {
		return this.end;
	}

	/**
	 * 设置指定索引位置的元素值
	 * 
	 * @param i 位置索引从0喀什
	 * @param t 元素值
	 * @return 区段对象
	 */
	@Override
	public INdarray<T> set(final int i, final T t) {
		return this.checkStatus(i, index -> {
			data[index] = t;
			// 加入拦截函数
			this.handle_method_call("set", new Object[] { i, t });
			// System.out.println(String.format("i:%s,t:%s", i, t));
			return this;
		});
	}

	/**
	 * 这里用了Ndarray的标准实现
	 */
	@Override
	public INdarray<T> create(final T[] data, final int start, final int end,
			LinkedHashMap<List<Integer>, Tuple2<Integer, Integer>> strides) {
		final StandardNdarray<T> nd = new StandardNdarray<>(data, start, end, strides);
		if (null != this.interceptors && this.interceptors.size() > 0) {
			nd.interceptors.putAll(this.interceptors);
		} // if
		return nd;
	}

	/**
	 * 分区索引跨度
	 */
	@Override
	public LinkedHashMap<List<Integer>, Tuple2<Integer, Integer>> strides() {
		return this.strides;
	}

	@Override
	public String toString() {
		return this.toString(e -> Optional.ofNullable(e).map(o -> {
			if (this.dtype() == Double.class) { // 浮点保留两位小数
				return String.format("%.2f", o);
			} else { // 默认格式化
				return o + "";
			} // if
		}).orElse("null"));
	}

	@Override
	public int hashCode() {
		return Arrays.hashCode(data);
	}

	@SuppressWarnings("unchecked")
	@Override
	public boolean equals(final Object o) {
		if (o instanceof INdarray)
			return this.compareTo((INdarray<T>) o) == 0;
		else
			return false;
	}

	/**
	 * 处理会话拦截
	 * 
	 * @param method 拦截方法
	 * @param args   方法参数
	 */
	private void handle_method_call(final String method, final Object[] args) {
		Optional.ofNullable(interceptors.get(method)).ifPresent(cons -> {
			cons.accept(this, args);
		});
	}

	/**
	 * 添加拦截器
	 * 
	 * @param method      回调函数
	 * @param interceptor 回调方法
	 * @return Ndarray 对象本身
	 */
	public AbstractNdarray<T> add_method_interceptor(final String method,
			final BiConsumer<INdarray<T>, Object[]> interceptor) {
		this.interceptors.put(method, interceptor);
		return this;
	}

	private final T[] data;
	private final int start;
	private final int end;
	private final LinkedHashMap<List<Integer>, Tuple2<Integer, Integer>> strides; // 分区索引跨度
	/**
	 * 方法拦截器
	 */
	protected final Map<String, BiConsumer<INdarray<T>, Object[]>> interceptors = new LinkedHashMap<>(); // 方法拦截器

}