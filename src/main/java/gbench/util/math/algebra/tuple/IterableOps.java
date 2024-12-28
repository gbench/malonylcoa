package gbench.util.math.algebra.tuple;

import static gbench.util.math.algebra.tuple.Tuple2.P;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.Predicate;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 
 * 模仿scala 的 IterableOps <br>
 * 略作改动 以 满足 java 语言
 * 
 * @author Administrator
 * @param <A> the element collection
 * @param <C> type of the collection (e.g. List[Int], String, BitSet).
 *            Operations returning a collection with the same type of element
 *            (e.g. drop, filter) return a C.
 */
public interface IterableOps<A, C extends IterableOps<A, C>> extends Iterable<A> {

	/**
	 * 基本元素的流化处理
	 * 
	 * @return 数据元素的流
	 */
	public A[] data();

	/**
	 * type constructor of the collection (e.g. List, Set). Operations returning a
	 * collection with a different type of element B (e.g. map) return a CC[B].
	 * 
	 * @param as 元素序列
	 * @return 构造一个容器类型
	 */
	public C CC(final List<A> as);

	/**
	 * type constructor of the collection (e.g. List, Set). Operations returning a
	 * collection with a different type of element B (e.g. map) return a CC[B].
	 * 
	 * @param as 元素序列
	 * @return 构造一个容器类型
	 */
	default C CC(final A[] as) {
		return this.CC(Arrays.asList(as));
	}

	/**
	 * 基本元素的流化处理
	 * 
	 * @return 数据元素的流
	 */
	default Stream<A> dataS() {
		return Arrays.stream(this.data());
	}

	/**
	 * The initial part of the collection without its last element.
	 * 
	 * @return The initial part of the collection without its last element.
	 */
	default C init() {
		final var as = this.data();
		return as.length > 1 ? this.CC(Arrays.copyOfRange(as, 0, as.length - 1)) : this.empty();
	}

	/**
	 * The initial part of the collection without its last element.
	 * 
	 * @return a list of C
	 */
	default List<C> inits() {
		final List<C> ll = new ArrayList<C>();
		C c = this.init();
		while (c.count() > 0) {
			ll.add(c);
			c = c.init();
		}
		ll.add(c);
		return ll;
	}

	/**
	 * The rest of the collection without its first element.
	 * 
	 * @return The rest of the collection without its first element.
	 */
	default C tail() {
		final var as = this.data();
		return as.length > 1 ? this.CC(Arrays.copyOfRange(as, 1, as.length)) : this.empty();
	}

	/**
	 * tails: Iterator[C] Iterates over the tails of this iterable collection. The
	 * first value will be this iterable collection and the final one will be an
	 * empty iterable collection, with the intervening values the results of
	 * successive applications of tail.
	 * 
	 * @return an iterator over all the tails of this iterable collection
	 */
	default List<C> tails() {
		final List<C> ll = new ArrayList<C>();
		C c = this.tail();
		while (c.count() > 0) {
			ll.add(c);
			c = c.tail();
		}
		ll.add(c);
		return ll;
	}

	/**
	 * Selects the first element of this iterable collection.
	 * 
	 * @return the first element of this iterable collection.
	 */
	default Optional<A> headOption() {
		final var dd = this.data();
		return Optional.ofNullable(dd != null && dd.length > 0 ? dd[0] : null);
	}

	/**
	 * Selects the first element of this iterable collection.
	 * 
	 * @return Selects the first element of this iterable collection.
	 */
	default A head() {
		return this.headOption().orElse(null);
	}

	/**
	 * Selects the first element of this iterable collection.
	 * 
	 * @return the first element of this iterable collection.
	 */
	default Optional<A> lastOption() {
		final var dd = this.data();
		return Optional.ofNullable(dd != null && dd.length > 0 ? dd[dd.length - 1] : null);
	}

	/**
	 * Selects the last element of this iterable collection.
	 * 
	 * @return Selects the last element of this iterable collection.
	 */
	default A last() {
		return this.lastOption().orElse(null);
	}

	/**
	 * Selects the first n elements.
	 * 
	 * @param n the number of elements to take from this iterable collection.
	 * @return a iterable collection consisting only of the first n elements of this
	 *         iterable collection, or else the whole iterable collection, if it has
	 *         less than n elements. If n is negative, returns an empty iterable
	 *         collection.
	 */
	default C take(final int n) {
		final var dd = this.data();
		final var len = dd.length;
		final var as = Arrays.copyOfRange(dd, 0, n >= len ? len : n);
		return this.CC(as);
	}

	/**
	 * Selects the last n elements.
	 * 
	 * @param n the number of elements to take from this iterable collection.
	 * @return a a iterable collection consisting only of the last n elements of
	 *         this iterable collection, or else the whole iterable collection, if
	 *         it has less than n elements. If n is negative, returns an empty
	 *         iterable collection.
	 */
	default C takeRight(final int n) {
		final var dd = this.data();
		final var len = dd.length;
		final var start = Optional.of(len - n).map(e -> e >= 0 ? e : 0).get();

		return this.CC(Arrays.copyOfRange(dd, start > len ? len : start, len));
	}

	/**
	 * Takes longest prefix of elements that satisfy a predicate.
	 * 
	 * @param p The predicate used to test elements.
	 * @return the longest prefix of this iterable collection whose elements all
	 *         satisfy the predicate p.
	 */
	default C takeWhile(final Predicate<A> p) {
		final var as = this.dataS().takeWhile(p).collect(Collectors.toList());
		return this.CC(as);
	}

	/**
	 * Selects all elements except first n ones.
	 * 
	 * @param n the number of elements to take from this iterable collection.
	 * @return a iterable collection consisting of all elements of this iterable
	 *         collection except the first n ones, or else the empty iterable
	 *         collection, if this iterable collection has less than n elements. If
	 *         n is negative, don't drop any elements.
	 */
	default C drop(final int n) {
		final var dd = this.data();
		final var as = Arrays.copyOfRange(dd, n > dd.length ? dd.length : n, dd.length);
		return this.CC(as);
	}

	/**
	 * Selects all elements except last n ones.
	 * 
	 * @param n the number of elements to drop from this iterable collection.
	 * @return a iterable collection consisting of all elements of this iterable
	 *         collection except the last n ones, or else the empty iterable
	 *         collection, if this iterable collection has less than n elements. If
	 *         n is negative, don't drop any elements.
	 */
	default C dropRight(final int n) {
		final var dd = this.data();
		final var len = dd.length;
		final var end = Optional.of(len - n).map(e -> e > 0 ? e : 0).get();

		return this.CC(Arrays.copyOfRange(dd, 0, end));
	}

	/**
	 * Drops longest prefix of elements that satisfy a predicate.
	 * 
	 * @param p The predicate used to test elements.
	 * @return the longest suffix of this iterable collection whose first element
	 *         does not satisfy the predicate p.
	 */
	default C dropWhile(final Predicate<A> p) {
		final var as = this.dataS().dropWhile(p).collect(Collectors.toList());
		return this.CC(as);
	}

	/**
	 * Partitions this sequence into a map of sequences according to some
	 * discriminator function.
	 * 
	 * Note: Even when applied to a view or a lazy collection it will always force
	 * the elements.
	 * 
	 * @param <K> the type of keys returned by the discriminator function.
	 * @param f   the discriminator function.
	 * @return A map from keys to sequences such that the following invariant holds:
	 *         (xs groupBy f)(k) = xs filter (x => f(x) == k)
	 */
	default <K> Map<K, List<A>> groupBy(final Function<A, K> f) {
		return this.groupMap(f, e -> e);
	}

	/**
	 * Partitions this sequence into a map of sequences according to some
	 * discriminator function.
	 * 
	 * Note: Even when applied to a view or a lazy collection it will always force
	 * the elements.
	 * 
	 * @param <K> the type of keys returned by the discriminator function.
	 * @param <B> the type of values returned by the transformation function
	 * @param key the discriminator function
	 * @param f   the element transformation function
	 * @return A map from keys to sequences such that the following invariant holds:
	 *         (xs groupBy f)(k) = xs filter (x => f(x) == k)
	 */
	default <K, B> Map<K, List<B>> groupMap(final Function<A, K> key, final Function<A, B> f) {
		final var map = new LinkedHashMap<K, List<B>>();
		for (var a : this.data()) {
			final var ll = map.computeIfAbsent(key.apply(a), (k) -> new ArrayList<B>());
			ll.add(f.apply(a));
		}
		return map;
	}

	/**
	 * Partitions this iterable collection into a map according to a discriminator
	 * function key. All the values that have the same discriminator are then
	 * transformed by the f function and then reduced into a single value with the
	 * reduce function.
	 * 
	 * It is equivalent to groupBy(key).mapValues(_.map(f).reduce(reduce)), but more
	 * efficient.
	 * 
	 * def occurrences[A](as: Seq[A]): Map[A, Int] = as.groupMapReduce(identity)(_
	 * => 1)(_ + _) Note: Even when applied to a view or a lazy collection it will
	 * always force the elements.
	 * 
	 * @param <K>    the type of keys returned by the discriminator function.
	 * @param <B>    the type of values returned by the transformation function
	 * @param key    the discriminator function
	 * @param reduce the reduce function for [A]
	 * @return A map from keys to sequences such that the following invariant holds:
	 *         (xs groupBy f)(k) = xs filter (x => f(x) == k)
	 */
	default <K, B> Map<K, A> groupMapReduce(final Function<A, K> key, final BinaryOperator<A> reduce) {
		return this.groupMapReduce(key, e -> e, reduce);
	}

	/**
	 * Partitions this iterable collection into a map according to a discriminator
	 * function key. All the values that have the same discriminator are then
	 * transformed by the f function and then reduced into a single value with the
	 * reduce function.
	 * 
	 * It is equivalent to groupBy(key).mapValues(_.map(f).reduce(reduce)), but more
	 * efficient.
	 * 
	 * def occurrences[A](as: Seq[A]): Map[A, Int] = as.groupMapReduce(identity)(_
	 * => 1)(_ + _) Note: Even when applied to a view or a lazy collection it will
	 * always force the elements.
	 * 
	 * @param <K>       the type of keys returned by the discriminator function.
	 * @param <B>       the type of values returned by the transformation function
	 * @param key       the discriminator function
	 * @param collector the element transformation function
	 * @return A map from keys to sequences such that the following invariant holds:
	 *         (xs groupBy f)(k) = xs filter (x => f(x) == k)
	 */
	default <K, B> Map<K, B> groupMapReduce(final Function<A, K> key, final Collector<A, ?, B> collector) {
		final var lhm = new HashMap<K, B>();
		this.groupMap(key, e -> e).forEach((k, aa) -> {
			lhm.put(k, aa.stream().collect(collector));
		});
		return lhm;
	}

	/**
	 * Partitions this iterable collection into a map according to a discriminator
	 * function key. All the values that have the same discriminator are then
	 * transformed by the f function and then reduced into a single value with the
	 * reduce function.
	 * 
	 * It is equivalent to groupBy(key).mapValues(_.map(f).reduce(reduce)), but more
	 * efficient.
	 * 
	 * def occurrences[A](as: Seq[A]): Map[A, Int] = as.groupMapReduce(identity)(_
	 * => 1)(_ + _) Note: Even when applied to a view or a lazy collection it will
	 * always force the elements.
	 * 
	 * @param <K>     the type of keys returned by the discriminator function.
	 * @param <B>     the type of values returned by the transformation function
	 * @param key     the discriminator function
	 * @param summary the summary function [a]-&gt;b
	 * @return A map from keys to sequences such that the following invariant holds:
	 *         (xs groupBy f)(k) = xs filter (x =&gt; f(x) == k)
	 */
	default <K, B> Map<K, B> groupMapReduce(final Function<A, K> key, final Function<List<A>, B> summary) {
		final var lhm = new HashMap<K, B>();
		this.groupMap(key, e -> e).forEach((k, aa) -> {
			lhm.put(k, summary.apply(aa));
		});
		return lhm;
	}

	/**
	 * Partitions this iterable collection into a map according to a discriminator
	 * function key. All the values that have the same discriminator are then
	 * transformed by the f function and then reduced into a single value with the
	 * reduce function.
	 * 
	 * It is equivalent to groupBy(key).mapValues(_.map(f).reduce(reduce)), but more
	 * efficient.
	 * 
	 * def occurrences[A](as: Seq[A]): Map[A, Int] = as.groupMapReduce(identity)(_
	 * =&gt; 1)(_ + _) Note: Even when applied to a view or a lazy collection it
	 * will always force the elements.
	 * 
	 * @param <K>    the type of keys returned by the discriminator function.
	 * @param <B>    the type of values returned by the transformation function
	 * @param key    the discriminator function
	 * @param f      the element transformation function
	 * @param reduce the reduce function for [B]
	 * @return A map from keys to sequences such that the following invariant holds:
	 *         (xs groupBy f)(k) = xs filter (x =&gt; f(x) == k)
	 */
	default <K, B> Map<K, B> groupMapReduce(final Function<A, K> key, Function<A, B> f,
			final BinaryOperator<B> reduce) {
		final var ret = new LinkedHashMap<K, B>();
		this.groupMap(key, f).forEach((k, bs) -> {
			ret.put(k, bs.stream().reduce(reduce).orElse(null));
		});
		return ret;
	}

	/**
	 * Partitions this iterable collection into a map according to a discriminator
	 * function key. All the values that have the same discriminator are then
	 * transformed by the f function and then reduced into a single value with the
	 * reduce function.
	 * 
	 * It is equivalent to groupBy(key).mapValues(_.map(f).reduce(reduce)), but more
	 * efficient.
	 * 
	 * def occurrences[A](as: Seq[A]): Map[A, Int] = as.groupMapReduce(identity)(_
	 * =&gt; 1)(_ + _) Note: Even when applied to a view or a lazy collection it
	 * will always force the elements.
	 * 
	 * @param <K>    the type of keys returned by the discriminator function.
	 * @param <B>    the type of values returned by the transformation function
	 * @param <R>    the reduce result type
	 * @param key    the discriminator function
	 * @param f      the element transformation function
	 * @param reduce the reduce function for [B],[b]->r
	 * @return the summary of the reduce result {(k,r)}
	 */
	default <K, B, R> Map<K, R> groupMapReduce(final Function<A, K> key, final Function<A, B> f,
			final Function<List<B>, R> reduce) {
		return this.groupMapReduce(key, f, reduce, e -> e);
	}

	/**
	 * Partitions this iterable collection into a map according to a discriminator
	 * function key. All the values that have the same discriminator are then
	 * transformed by the f function and then reduced into a single value with the
	 * reduce function.
	 * 
	 * It is equivalent to groupBy(key).mapValues(_.map(f).reduce(reduce)), but more
	 * efficient.
	 * 
	 * def occurrences[A](as: Seq[A]): Map[A, Int] = as.groupMapReduce(identity)(_
	 * =&gt; 1)(_ + _) Note: Even when applied to a view or a lazy collection it
	 * will always force the elements.
	 * 
	 * @param <K>      the type of keys returned by the discriminator function.
	 * @param <B>      the type of values returned by the transformation function
	 * @param <R>      the reduce result type
	 * @param <FR>     the final result type
	 * @param key      the discriminator function
	 * @param f        the element transformation function
	 * @param reduce   the reduce function for [B],[b]->r
	 * @param finisher {(k,r)}->fr
	 * @return the summary of the reduce result FR
	 */
	default <K, B, R, FR> FR groupMapReduce(final Function<A, K> key, final Function<A, B> f,
			final Function<List<B>, R> reduce, final Function<Map<K, R>, FR> finisher) {
		final var ret = new LinkedHashMap<K, R>();
		this.groupMap(key, f).forEach((k, bs) -> {
			ret.put(k, reduce.apply(bs));
		});
		return finisher.apply(ret);
	}

	/**
	 * Tests whether a predicate holds for all elements of this sequence.
	 * 
	 * 
	 * @param <T>
	 * @param p   the predicate used to test elements.
	 * @return true if this sequence is empty or the given predicate p holds for all
	 *         elements of this sequence, otherwise false.
	 */
	default <T> boolean forall(final Predicate<A> p) {
		return this.dataS().allMatch(p);
	}

	/**
	 * Counts the number of elements in the iterable collection which satisfy a
	 * predicate.
	 * 
	 * @param p the predicate used to test elements.
	 * @return the number of elements satisfying the predicate p.
	 */
	default long count(final Predicate<A> p) {
		return this.dataS().filter(p).count();
	}

	/**
	 * 计算指定位点的元素值
	 * 
	 * @param idx    元素索引从0开始，有效范围内为[0,size),超出范围的数据 返回C对象本身的复制品
	 * @param mapper a-&gt;_a 元素变换函数
	 * @return C 类型的复制品
	 */
	default C computeAt(final Integer idx, final Function<A, A> mapper) {
		final var as = this.dataS().collect(Collectors.toList());
		if (as.size() > idx) {
			final var a = as.get(idx);
			final var _a = mapper.apply(a);
			as.set(0, _a);
		}
		return this.CC(as);
	}

	/**
	 * 计算第一个值，返回 C 类型的复制品
	 * 
	 * @param mapper a-&gt;_a 元素变换函数
	 * @return C 类型的复制品
	 */
	default C computeFirst(final Function<A, A> mapper) {
		final var as = this.dataS().collect(Collectors.toList());
		if (as.size() > 0) {
			final var first = as.get(0);
			final var _first = mapper.apply(first);
			as.set(0, _first);
		}
		return this.CC(as);
	}

	/**
	 * 计算最后一个值，返回 C 类型的复制品
	 * 
	 * @param mapper a-&gt;_a 元素变换函数
	 * @return C 类型的复制品
	 */
	default C computeLast(final Function<A, A> mapper) {
		final var as = this.dataS().collect(Collectors.toList());
		if (as.size() > 0) {
			final var _last_idx = as.size() - 1;
			final var last = as.get(_last_idx);
			final var _last = mapper.apply(last);
			as.set(_last_idx, _last);
		}
		return this.CC(as);
	}

	/**
	 * Counts the number of elements in the iterable collection
	 * 
	 * @return number of elements in the iterable collection
	 */
	default long count() {
		return this.data().length;
	}

	/**
	 * Tests whether a predicate holds for at least one element of this iterable
	 * collection.
	 * 
	 * @param p the predicate used to test elements.
	 * @return true if the given predicate p is satisfied by at least one element of
	 *         this iterable collection, otherwise false
	 */
	default boolean exists(final Predicate<A> p) {
		return this.dataS().filter(p).findFirst().isPresent();
	}

	/**
	 * Selects all elements of this iterable collection which satisfy a predicate.
	 * 
	 * @param pred the predicate used to test elements.
	 * @return a new iterable collection consisting of all elements of this iterable
	 *         collection that satisfy the given predicate p. The order of the
	 *         elements is preserved.
	 */
	default C filter(final Predicate<A> pred) {
		final var aa = this.dataS().filter(pred).collect(Collectors.toList());
		return this.CC(aa);
	}

	/**
	 * Selects all elements of this iterable collection which do not satisfy a
	 * predicate.
	 * 
	 * @param pred the predicate used to test elements.
	 * @return
	 */
	default C filterNot(final Predicate<A> pred) {
		final var aa = this.dataS().filter(a -> !pred.test(a)).collect(Collectors.toList());
		return this.CC(aa);
	}

	/**
	 * Finds the first element of the iterable collection satisfying a predicate, if
	 * any.
	 * 
	 * @param p the predicate used to test elements.
	 * @return an option value containing the first element in the iterable
	 *         collection that satisfies p, or None if none exists.
	 */
	default Optional<A> find(final Predicate<A> p) {
		return this.dataS().filter(p).findAny();
	}

	/**
	 * Builds a new iterable collection by applying a function to all elements of
	 * this iterable collection.
	 * 
	 * @param <B> the element type of the returned iterable collection.
	 * @param f   the function to apply to each element.
	 * @return a new iterable collection resulting from applying the given function
	 *         f to each element of this iterable collection and collecting the
	 *         results.
	 */
	default <B> Stream<B> map(final Function<A, B> f) {
		return this.dataS().map(f);
	}

	/**
	 * Builds a new iterable collection by applying a function to all elements of
	 * this iterable collection and using the elements of the resulting collections.
	 * 
	 * 
	 * @param <B> the element type of the returned collection.
	 * @param f   the function to apply to each element.
	 * @return a new iterable collection resulting from applying the given
	 *         collection-valued function f to each element of this iterable
	 *         collection and concatenating the results.
	 */
	default <B> Stream<B> flatMap(final Function<A, Stream<B>> f) {
		return this.dataS().flatMap(f);
	}

	/**
	 * 带有外壳类型的内值变换，即包裹类container不变而改变其内部的值的类型,也就是 换药不换 葫芦。<br>
	 * 此函数设计为 模仿 Haskell 的 Functor <br>
	 * 
	 * @param mapper 值变换函数 a->A 的变换函数
	 * @return C 包装容器
	 */
	default C fmap(final Function<A, ? extends A> mapper) {
		return this.CC(this.map(mapper).collect(Collectors.toList()));
	}

	/*
	 * Tests whether the iterable collection is empty.
	 */
	default boolean isEmpty() {
		final var dd = this.data();
		return dd == null || dd.length < 1 ? true : false;
	}

	/**
	 * The empty iterable of the same type as this iterable
	 * 
	 * @return an empty iterable of type C.
	 */
	default C empty() {
		return this.CC(Arrays.asList());
	}

	/**
	 * Splits this iterable collection into a prefix/suffix pair according to a
	 * predicate.
	 * 
	 * @param p the test predicate
	 * @return a pair consisting of the longest prefix of this iterable collection
	 *         whose elements all satisfy p, and the rest of this iterable
	 *         collection.
	 */
	default Tuple2<C, C> span(final Predicate<A> p) {
		final var left = this.takeWhile(a -> p.test(a));
		final var len = this.count();
		final var right = this.takeRight((int) (len - left.count()));
		return P(left, right);
	}

	/**
	 * Splits this iterable collection into a prefix/suffix pair at a given
	 * position.
	 * 
	 * @param n the position at which to split.
	 * @return a pair of iterable collections consisting of the first n elements of
	 *         this iterable collection, and the other elements.
	 */
	default Tuple2<C, C> splitAt(final int n) {
		final var left = this.take(n);
		final var pos = Optional.of(this.count() - n).map(e -> e == 0 ? 0 : e).get().intValue();
		final var right = this.takeRight(pos);
		return P(left, right);
	}

	/**
	 * Groups elements in fixed size blocks by passing a "sliding window" over them
	 * (as opposed to partitioning them, as is done in grouped.) The returned
	 * iterator will be empty when called on an empty collection. The last element
	 * the iterator produces may be smaller than the window size when the original
	 * collection isn't exhausted by the window before it and its last element isn't
	 * skipped by the step before it.
	 * 
	 * @param size the number of elements per group
	 * @param step the distance between the first elements of successive groups
	 * @return An iterator producing iterable collections of size size, except the
	 *         last element (which may be the only element) will be smaller if there
	 *         are fewer than size elements remaining to be grouped.
	 */
	default List<C> slidings(final int size, final int step) {
		return this.slidingS(size, step, true).collect(Collectors.toList());
	}

	/**
	 * 数据滑动
	 * 
	 * @param size 窗口大小
	 * @param step 步长
	 * @return 子IRecord的流 [rec]
	 */
	default Stream<C> slidingS(final int size, final int step) {
		return this.slidingS(size, step, true);
	}

	/**
	 * 数据滑动
	 * 
	 * @param size 窗口大小
	 * @param step 步长
	 * @param flag 是否返回等长记录 true 数据等长 剔除 尾部的 不齐整（小于 size） 的元素,false 包含不齐整
	 * @return 子IRecord的流 [rec]
	 */
	default Stream<C> slidingS(final int size, final int step, final boolean flag) {
		final var arrayList = this.data();
		final var n = arrayList.length;
		// 当flag 为true 的时候 i的取值范围是: [0,n-size] <==> [0,n+1-size)
		return Stream.iterate(0, i -> i < (flag ? n + 1 - size : n), i -> i + step) // 序列生成
				.map(i -> Arrays.copyOfRange(arrayList, i, (i + size) > n ? n : (i + size))) // 转换成[a]的流
				.map(this::CC);
	}

	/**
	 * Selects an interval of elements. The returned iterable collection is made up
	 * of all elements x which satisfy the invariant: from &lt;= indexOf(x) &lt;
	 * until
	 * 
	 * @param from  the lowest index to include from this iterable collection.
	 * @param until the lowest index to EXCLUDE from this iterable collection.
	 * @return a iterable collection containing the elements greater than or equal
	 *         to index from extending up to (but not including) index until of this
	 *         iterable collection.
	 */
	default C slice(final int from, final int until) {
		return this.CC(Arrays.copyOfRange(this.data(), from, until));
	}

	/**
	 * 
	 * @param z  a neutral element for the fold operation; may be added to the
	 *           result an arbitrary number of times, and must not change the result
	 *           (e.g., Nil for list concatenation, 0 for addition, or 1 for
	 *           multiplication).
	 * @param op a binary operator that must be associative.
	 * @return the result of applying the fold operator op between all the elements
	 *         and z, or z if this iterable collection is empty.
	 */
	default A fold(final A z, final BinaryOperator<A> op) {
		return this.foldLeft(z, (b, a) -> op.apply(b, a));
	}

	/**
	 * 
	 * Applies a binary operator to a start value and all elements of this iterable
	 * collection, going left to right.
	 * 
	 * @param <B> the result type of the binary operator.
	 * @param z   the start value.
	 * @param op  the binary operator.
	 * @return the result of inserting op between consecutive elements of this
	 *         iterable collection, going left to right with the start value z on
	 *         the left: op(...op(z, x1), x2, ..., xn) where x1, ..., xn are the
	 *         elements of this iterable collection. Returns z if this iterable
	 *         collection is empty.
	 */
	default <B> B foldLeft(final B z, final BiFunction<B, A, B> op) {
		B ret = z;
		final var as = this.data();
		for (var a : as) {
			ret = op.apply(ret, a);
		}

		return ret;
	}

	/**
	 * 
	 * Applies a binary operator to all elements of this iterable collection and a
	 * start value, going right to left.
	 * 
	 * @param <B> the result type of the binary operator.
	 * @param z   the start value.
	 * @param op  the binary operator.
	 * @return the result of inserting op between consecutive elements of this
	 *         iterable collection, going right to left with the start value z on
	 *         the right: op(x1, op(x2, ... op(xn, z)...)) where x1, ..., xn are the
	 *         elements of this iterable collection. Returns z if this iterable
	 *         collection is empty.
	 */
	default <B> B foldRight(final B z, final BiFunction<B, A, B> op) {
		B ret = z;
		final var as = this.data();

		for (int i = as.length - 1; i >= 0; i--) {
			final var a = as[i];
			ret = op.apply(ret, a);
		}

		return ret;
	}

	/**
	 * Finds the largest element.
	 * 
	 * @param comparator An ordering to be used for comparing elements.
	 * @return an option value containing the largest element of this iterable
	 *         collection with respect to the ordering ord.
	 */
	default Optional<A> maxOption(final Comparator<? super A> comparator) {
		return this.dataS().sorted((a, b) -> comparator.compare(b, a)).findFirst();
	}

	/**
	 * Finds the largest element.
	 * 
	 * @param comparator An ordering to be used for comparing elements.
	 * @return Finds the largest element.
	 */
	default A max(final Comparator<? super A> comparator) {
		return this.maxOption(comparator).orElse(null);
	}

	/**
	 * Finds the smallest element.
	 * 
	 * @param comparator An ordering to be used for comparing elements.
	 * @return an option value containing the largest element of this iterable
	 *         collection with respect to the ordering ord.
	 */
	default Optional<A> minOption(final Comparator<? super A> comparator) {
		return this.dataS().sorted((a, b) -> comparator.compare(a, b)).findFirst();
	}

	/**
	 * Finds the smallest element.
	 * 
	 * @param comparator An ordering to be used for comparing elements.
	 * @return Finds the largest element.
	 */
	default A min(final Comparator<? super A> comparator) {
		return this.minOption(comparator).orElse(null);
	}

	/**
	 * same as reduceLeft <br>
	 * Reduces the elements of this iterable collection using the specified
	 * associative binary operator.
	 * 
	 * @param op A binary operator that must be associative.
	 * @return The result of applying reduce operator op between all the elements if
	 *         the iterable collection is nonempty.
	 */
	default A reduce(final BinaryOperator<A> op) {
		return this.reduceLeft(op);
	}

	/**
	 * Applies a binary operator to all elements of this iterable collection, going
	 * left to right.
	 * 
	 * @param op a binary operator that must be associative.
	 * @return the result of inserting op between consecutive elements of this
	 *         iterable collection, going right to left: op(x1, op(x2, ..., op(xn-1,
	 *         xn)...)) where x1, ..., xn are the elements of this iterable
	 *         collection.
	 */
	default A reduceLeft(final BinaryOperator<A> op) {
		if (this.count() < 2) {
			return this.head();
		}

		return this.tail().fold(this.head(), op);
	}

	/**
	 * 
	 * Applies a binary operator to all elements of this iterable collection, going
	 * right to left.
	 * 
	 * @param op the binary operator.
	 * @return an option value containing the largest element of this iterable
	 *         collection with respect to the ordering ord.
	 */
	default A reduceRight(final BinaryOperator<A> op) {
		if (this.count() < 2) {
			return this.head();
		}

		final var data = this.init().data();
		final var aa = new LinkedList<A>();
		for (var d : data) {
			aa.addFirst(d);
		}
		return this.CC(aa).fold(this.last(), op);
	}

	/**
	 * same as scanLeft <br>
	 * 
	 * @param <B> element type of the resulting collection
	 * @param z   neutral element for the operator op
	 * @param op  the associative operator for the scan
	 * @return a new iterable collection containing the prefix scan of the elements
	 *         in this iterable collection
	 */
	default <B> List<B> scan(final B z, final BiFunction<B, A, B> op) {
		return this.scanLeft(z, op);
	}

	/**
	 * 
	 * @param <B> element type of the resulting collection
	 * @param z   neutral element for the operator op
	 * @param op  the associative operator for the scan
	 * @return a new iterable collection containing the prefix scan of the elements
	 *         in this iterable collection
	 */
	default <B> List<B> scanLeft(final B z, final BiFunction<B, A, B> op) {
		final var bs = new ArrayList<B>();
		final var dd = this.data();
		final var as = new ArrayList<A>();
		bs.add(z); // add first
		for (final var a : dd) {
			as.add(a);
			final var b = as.stream().reduce(z, op, (left, right) -> right);
			bs.add(b);
		}
		return bs;
	}

	/**
	 * 
	 * Produces a collection containing cumulative results of applying the operator
	 * going right to left. The head of the collection is the last cumulative
	 * result.
	 * 
	 * @param <B> element type of the resulting collection
	 * @param z   neutral element for the operator op
	 * @param op  the associative operator for the scan
	 * @return a new iterable collection containing the prefix scan of the elements
	 *         in this iterable collection
	 */
	default <B> List<B> scanRight(final B z, final BiFunction<A, B, B> op) {
		final var bs = new ArrayList<B>();
		final var dd = this.data(); // dd
		final var n = dd.length; // length size
		for (var i = 0; i < n; i++) {
			final var as = Arrays.asList(Arrays.copyOfRange(dd, i, n));
			Collections.reverse(as);// 数据调转
			final var _b = as.stream().reduce(z, (b, a) -> op.apply(a, b), (left, right) -> right);
			bs.add(_b);
		} // for
		bs.add(z); // add last
		return bs;
	}

	/**
	 * Returns a iterable collection formed from this iterable collection and
	 * another iterable collection by combining corresponding elements in pairs.
	 * 
	 * @param that    The iterable providing the second half of each result pair
	 * @param recycle whether reuse the elemment of b when that does not has the
	 *                sufficent corresponding element of this, true reuse,false use
	 *                null to fill
	 * @return a new iterable collection containing pairs consisting of
	 *         corresponding elements of this iterable collection and that. The
	 *         length of the returned collection is the minimum of the lengths of
	 *         this iterable collection and that.
	 */
	default <B> Stream<Tuple2<A, B>> zipS(final Iterable<B> that, boolean recycle) {
		final var itr = that.iterator(); // 吧的迭代器
		final var bs = new ArrayList<B>(); // B 的容器
		final var ai = new AtomicInteger(); // 计数器
		final Supplier<B> b_generator = () -> {
			int n = bs.size();
			if (itr.hasNext()) {
				final var b = itr.next();
				bs.add(b);
				return b;
			} else {
				if (recycle) {
					return n > 0 ? bs.get(ai.getAndIncrement() % n) : null;
				} else {
					return null;
				}
			}
		};
		return this.dataS().map(e -> new Tuple2<A, B>(e, b_generator.get()));
	}

	/**
	 * Returns a iterable collection formed from this iterable collection and
	 * another iterable collection by combining corresponding elements in pairs.
	 * 
	 * @param that The iterable providing the second half of each result pair
	 * @return a new iterable collection containing pairs consisting of
	 *         corresponding elements of this iterable collection and that. The
	 *         length of the returned collection is the minimum of the lengths of
	 *         this iterable collection and that.
	 */
	default <B> Stream<Tuple2<A, B>> zipS(final Iterable<B> that) {
		return this.zipS(that, true);
	}

	/**
	 * Returns a iterable collection formed from this iterable collection and
	 * another iterable collection by combining corresponding elements in pairs.
	 * 
	 * @param that The iterable providing the second half of each result pair
	 * @return a new iterable collection containing pairs consisting of
	 *         corresponding elements of this iterable collection and that. The
	 *         length of the returned collection is the minimum of the lengths of
	 *         this iterable collection and that.
	 */
	default <B> List<Tuple2<A, B>> zip(final Iterable<B> that) {
		return this.zipS(that).collect(Collectors.toList());
	}

	/**
	 * Returns a iterable collection formed from this iterable collection and
	 * another iterable collection by combining corresponding elements in pairs.
	 * 
	 * @return A new iterable collection containing pairs consisting of all elements
	 *         of this iterable collection paired with their index. Indices start at
	 *         0.
	 */
	default <B> List<Tuple2<A, Integer>> zipWithIndex() {
		final var n = this.count();
		final var that = Stream.iterate(0, i -> i + 1).limit(n).collect(Collectors.toList());
		return this.zip(that);
	}

	/**
	 * Converts this iterable collection of pairs into two collections of the first
	 * and second half of each pair.
	 * 
	 * @param <A1>   the type of the first half of the element pairs
	 * @param <A2>   the type of the second half of the element pairs
	 * @param asPair an conversion which asserts that the element type of this
	 *               iterable collection is a pair.
	 * @return a pair of iterable collections, containing the first, respectively
	 *         second half of each element pair of this iterable collection.
	 */
	default <A1, A2> Tuple2<List<A1>, List<A2>> unzip(final Function<A, Tuple2<A1, A2>> asPair) {
		final var a1s = new ArrayList<A1>();
		final var a2s = new ArrayList<A2>();
		final var dd = this.data();
		if (dd != null && dd.length > 0) {
			for (final var d : dd) {
				final var pair = asPair.apply(d);
				a1s.add(pair._1());
				a2s.add(pair._2());
			}
		}
		return new Tuple2<>(a1s, a2s);
	}

	/**
	 * 对元素序列进行排序
	 * 
	 * @param weighter 排序的向量的权重函数 t->num <br>
	 *                 即把 t映射成一个可以进行比较的数值权重(weight)而后按照weight进行排序：从小到大
	 * @return 排序后的元素向量
	 */
	default C sorted(final Function<A, Number> weighter) {
		final Function<A, Double> parse = x -> weighter.apply(x).doubleValue();
		final var as = this.dataS().sorted((a, b) -> a == null && b == null // 均为均值
				? 0 // 均是空值表名相等
				: a == null // 第一元素空
						? -1 // 小于
						: b == null //
								? 1 //
								: parse.apply(a).compareTo(parse.apply(b)) // 计算数值化 比较
		).collect(Collectors.toList());
		return this.CC(as);
	}

	/**
	 * 对元素序列进行排序
	 * 
	 * @param comparator 比较器 (a,b)-&gt;[-1,0,1]
	 * @return 排序后的元素向量
	 */
	default C sorted(final Comparator<A> comparator) {
		final var as = this.dataS().sorted(comparator).collect(Collectors.toList());
		return this.CC(as);
	}

	/**
	 * 
	 * Given a collection factory factory, convert this collection to the
	 * appropriate representation for the current element type A. Example uses:
	 * Example uses: xs.to(List) xs.to(ArrayBuffer) xs.to(BitSet) // for xs:
	 * Iterable[Int]
	 * 
	 * @param <C1>    the representation type
	 * @param factory the converting method
	 * @return C1
	 */
	@SuppressWarnings("unchecked")
	default <C1> C1 to(final Function<C, C1> factory) {
		return factory.apply((C) this);
	}

	/**
	 * put all element into a list
	 * 
	 * @param <B>    the element type of the returned iterable collection.
	 * @param mapper the function to apply to each element.
	 * @return a new List resulting from applying the given function f to each
	 *         element of this iterable collection and collecting the results.
	 */
	default <B> List<B> toList(final Function<A, B> mapper) {
		return this.dataS().map(mapper).collect(Collectors.toList());
	}

	@Override
	default Iterator<A> iterator() {
		return Arrays.asList(this.data()).iterator();
	}

	/**
	 * 把记录序列归集成指定类型 T
	 * 
	 * @param <T>       归集器的结果类型
	 * @param collector 归集器 [r] -&gt; T
	 * @return 归集的结果
	 */
	public default <T> T collect(final Collector<? super A, ?, T> collector) {
		return this.dataS().collect(collector);
	}

}
