package gbench.util.function;

import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import gbench.util.array.INdarray;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * 读取器：Key-Value Pair的动态结构
 *
 * @param <K> 参数类型
 * @param <T> 结果类型
 * @author gbench
 */
@FunctionalInterface
public interface Getter<K, T> extends Function<K, T> {

	/**
	 * 根据键名获取键值
	 *
	 * @param key 键值名
	 * @return 键值
	 */
	T get(final K key);

	@Override
	default T apply(final K key) {
		return this.get(key);
	}

	/**
	 * 对象流
	 *
	 * @param ks 键序列
	 * @return 对象流
	 */
	@SuppressWarnings("unchecked")
	default Stream<T> stream(final K... ks) {
		return Stream.of(ks).map(this::get);
	}

	/**
	 * 提取对象序列流
	 *
	 * @param ks 键序列
	 * @return 提取对象序列流
	 */
	default Stream<T> stream(final Iterable<K> ks) {
		return StreamSupport.stream(ks.spliterator(), false).map(this::get);
	}

	/**
	 * 提取对象序列流
	 *
	 * @param k    开始元素
	 * @param next 键序列
	 * @return 提取对象序列流
	 */
	default Stream<T> stream(final K k, final UnaryOperator<K> next) {
		return Stream.iterate(k, next).map(this::get);
	}

	/**
	 * 提取对象序列流
	 *
	 * @param <R>    结果元素里欸选哪个
	 * @param mapper 映射函数
	 * @param ks     键序列
	 * @return 提取对象序列流
	 */
	default <R> Stream<R> map(final Function<T, R> mapper, final Iterable<K> ks) {
		return this.stream(ks).map(mapper);
	}

	/**
	 * 提取对象序列流
	 *
	 * @param <R>    结果元素里欸选哪个
	 * @param mapper 映射函数
	 * @param ks     键序列
	 * @return 提取对象序列流
	 */
	default <R> Stream<R> map(final BiFunction<Integer, T, R> mapper, final Iterable<K> ks) {
		final AtomicInteger ai = new AtomicInteger();
		return this.stream(ks).map(t -> mapper.apply(ai.getAndIncrement(), t));
	}

	/**
	 * 提取对象序列流
	 *
	 * @param <R>    结果元素里欸选哪个
	 * @param mapper 映射函数
	 * @param ks     键序列
	 * @return 提取对象序列流
	 */
	@SuppressWarnings("unchecked")
	default <R> Stream<R> map(final Function<T, R> mapper, final K... ks) {
		return this.stream(ks).map(mapper);
	}

	/**
	 * 提取对象序列流
	 *
	 * @param <R>    结果元素里欸选哪个
	 * @param mapper 变换器
	 * @param ks     键序列
	 * @return 提取对象序列流
	 */
	default <R> Stream<R> flatMap(final Function<? super T, ? extends Stream<? extends R>> mapper,
			final Iterable<K> ks) {
		return this.stream(ks).flatMap(mapper);
	}

	/**
	 * 提取对象序列流
	 *
	 * @param <R>    结果元素里欸选哪个
	 * @param mapper 变换器
	 * @param ks     键序列
	 * @return 提取对象序列流
	 */
	@SuppressWarnings("unchecked")
	default <R> Stream<R> flatMap(final Function<? super T, ? extends Stream<? extends R>> mapper, final K... ks) {
		return this.stream(ks).flatMap(mapper);
	}

	/**
	 * 提取对象列表 List ItemS
	 *
	 * @param <R>    结果元素里欸选哪个
	 * @param mapper 映射函数
	 * @param ks     键序列
	 * @return 提取对象序列流
	 */
	default <R> List<R> lis(final Function<T, R> mapper, final Iterable<K> ks) {
		return this.map(mapper, ks).collect(Collectors.toList());
	}

	/**
	 * 提取对象列表 List ItemS
	 *
	 * @param <R>    结果元素里欸选哪个
	 * @param mapper 映射函数
	 * @param ks     键序列
	 * @return R 类型的列表
	 */
	default <R> List<R> lis(final BiFunction<Integer, T, R> mapper, final Iterable<K> ks) {
		return this.map(mapper, ks).collect(Collectors.toList());
	}

	/**
	 * 提取对象序列流
	 *
	 * @param <R>    结果元素里欸选哪个
	 * @param mapper 映射函数
	 * @param ks     键序列
	 * @return R 类型的nd
	 */
	default <R> INdarray<R> nd(final Function<T, R> mapper, final Iterable<K> ks) {
		return this.map(mapper, ks).collect(INdarray.ndclc());
	}

	/**
	 * 提取对象序列流
	 *
	 * @param <R>    结果元素里欸选哪个
	 * @param mapper 映射函数
	 * @param ks     键序列
	 * @return R 类型的nd
	 */
	default <R> INdarray<R> nd(final BiFunction<Integer, T, R> mapper, final Iterable<K> ks) {
		return this.map(mapper, ks).collect(INdarray.ndclc());
	}

	/**
	 * 提取对象序列流
	 *
	 * @param <U>  归集结果类型
	 * @param tclc 归集器
	 * @param ks   键序列
	 * @return 提取对象序列流
	 */
	default <U> U collect(final Collector<T, ?, U> tclc, final Iterable<K> ks) {
		return this.stream(ks).collect(tclc);
	}

	/**
	 * 提取对象序列流
	 *
	 * @param <U>  归集结果类型
	 * @param tclc 归集器
	 * @param ks   键序列
	 * @return 提取对象序列流
	 */
	@SuppressWarnings("unchecked")
	default <U> U collect(final Collector<T, ?, U> tclc, final K... ks) {
		return this.stream(ks).collect(tclc);
	}

	/**
	 * 提取对象序列流
	 *
	 * @param accumulator 规约器
	 * @param ks          键序列
	 * @return 提取对象序列流
	 */
	default Optional<T> reduce(final BinaryOperator<T> accumulator, final Iterable<K> ks) {
		return this.stream(ks).reduce(accumulator);
	}

	/**
	 * 提取对象序列流
	 *
	 * @param accumulator 规约器
	 * @param ks          键序列
	 * @return 提取对象序列流
	 */
	@SuppressWarnings("unchecked")
	default Optional<T> reduce(final BinaryOperator<T> accumulator, final K... ks) {
		return this.stream(ks).reduce(accumulator);
	}
}