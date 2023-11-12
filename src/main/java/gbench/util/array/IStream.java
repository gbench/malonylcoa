package gbench.util.array;

import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collector;
import java.util.stream.Stream;

import gbench.util.lisp.Lisp;
import gbench.util.lisp.Tuple2;
import gbench.util.type.Types;

/**
 * 序列接口
 * 
 * @author gbench
 *
 * @param <T> 序列元素类型
 */
public interface IStream<T> extends Iterable<T> {

	/**
	 * T类型的元素流 [t]
	 *
	 * @return T类型的元素流 [t]
	 */
	Stream<T> stream();

	/**
	 * 编号化的T类型的元素流 [(i,t)]
	 *
	 * @return T类型的元素流 [(i,t)],i从0开始
	 */
	default Stream<Tuple2<Integer, T>> stream2() {
		return this.stream(Tuple2.snb(0));
	}

	/**
	 * 编号化的T类型的元素流 [(i,t)]
	 * 
	 * @param <U>    结果类型
	 * @param mapper (i,t)->u 结果变换类型
	 * @return T类型的元素流 [(i,t)],i从0开始
	 */
	default <U> Stream<U> stream2(final Function<Tuple2<Integer, T>, U> mapper) {
		return this.stream2().map(mapper);
	}

	/**
	 * U类型的元素流 [t]
	 *
	 * @param mapper 元素变换器 t->u
	 * @param <U>    元素类型
	 * @return U类型的元素流 [u]
	 */
	default <U> Stream<U> stream(final Function<T, U> mapper) {
		return this.map(mapper);
	}

	/**
	 * Returns a stream consisting of the results of replacing each element of this
	 * stream with the contents of a mapped stream produced by applyingthe provided
	 * mapping function to each element. Each mapped stream is closed after its
	 * contentshave been placed into this stream. (If a mapped stream is nullan
	 * empty stream is used, instead.)
	 * <p>
	 * This is an intermediateoperation.
	 * 
	 * @param <R>    The element type of the new stream
	 * @param mapper a non-interfering, statelessfunction to apply to each element
	 *               which produces a streamof new values
	 * @return the new stream
	 */
	default <R> Stream<R> flatMap(Function<? super T, ? extends Stream<? extends R>> mapper) {
		return this.stream().flatMap(mapper);
	}

	/**
	 * U类型的元素流 [t]
	 *
	 * @param mapper 元素变换器 t->u
	 * @param <U>    元素类型
	 * @return U类型的元素流 [u]
	 */
	default <U> Stream<U> map(final Function<T, U> mapper) {
		return stream().map(mapper);
	}

	/**
	 * U类型的元素流 [t]
	 *
	 * @param mapper 元素变换器 (i:索引从0开始,t:元素内容)->u
	 * @param <U>    元素类型
	 * @return U类型的元素流 [u]
	 */
	default <U> Stream<U> map(final BiFunction<Integer, T, U> mapper) {
		return stream().map(Tuple2.snb(0)).map(Tuple2.bifun(mapper));
	}

	/**
	 * Returns a stream consisting of the elements of this stream that matchthe
	 * given predicate.
	 * <p>
	 * This is an intermediateoperation.
	 * 
	 * @param predicate a non-interfering, statelesspredicate to apply to each
	 *                  element to determine if itshould be included
	 * @return T类型的元素流 [u]
	 */
	default Stream<T> filter(final Predicate<? super T> predicate) {
		return stream().filter(predicate);
	}

	/**
	 * Returns a stream consisting of the elements of this stream that matchthe
	 * given predicate.
	 * <p>
	 * This is an intermediateoperation.
	 * 
	 * @param predicate a non-interfering, statelesspredicate to apply to each
	 *                  element to determine if itshould be included
	 * @return T类型的元素流 [u]
	 */
	default Stream<T> filter(final BiPredicate<Integer, ? super T> predicate) {
		return stream().map(Tuple2.snb(0)).filter(tp -> predicate.test(tp._1, tp._2)) //
				.map(tp -> tp._2);
	}

	/**
	 * 规约函数
	 * 
	 * @param accumulator an associative, non-interfering, statelessfunction for
	 *                    combining two values
	 * @return Optional T类型的 Optional
	 */
	default Optional<T> reduce(final BinaryOperator<T> accumulator) {
		return this.stream().reduce(accumulator);
	}

	/**
	 * 规约函数
	 * 
	 * @param identity    the identity value for the accumulating function
	 * @param accumulator an associative, non-interfering, statelessfunction for
	 *                    combining two values
	 * @return T the result of the reduction
	 */
	default T reduce(final T identity, final BinaryOperator<T> accumulator) {
		return this.stream().reduce(identity, accumulator);
	}

	/**
	 * 规约函数
	 * 
	 * @param <U>         The type of the result
	 * @param identity    the identity value for the combiner function
	 * @param accumulator an associative, non-interfering, statelessfunction for
	 *                    incorporating an additional element into a result
	 * @param combiner    an associative, non-interfering, statelessfunction for
	 *                    combining two values, which must becompatible with the
	 *                    accumulator function
	 * @return U the result of the reduction
	 */
	default <U> U reduce(final U identity, final BiFunction<U, ? super T, U> accumulator,
			final BinaryOperator<U> combiner) {
		return this.stream().reduce(identity, accumulator, combiner);
	}

	/**
	 * 规约函数 <br>
	 * reduce(identity, accumulator, (a, b) -> a) 的简写
	 * 
	 * @param <U>         The type of the result
	 * @param identity    the identity value for the combiner function
	 * @param accumulator an associative, non-interfering, statelessfunction for
	 *                    incorporating an additional element into a result
	 * @return U the result of the reduction
	 */
	default <U> U reduce(final U identity, final BiFunction<U, ? super T, U> accumulator) {
		return this.stream().reduce(identity, accumulator, (a, b) -> a);
	}

	/**
	 * 归集函数
	 * 
	 * @param <R>       结果类型
	 * @param <A>       累加类型
	 * @param collector 归集器
	 * @return R类型结果
	 */
	default <R, A> R collect(final Collector<? super T, A, R> collector) {
		return this.stream().collect(collector);
	}

	/**
	 * 归集函数
	 * 
	 * @param <R>         结果类型
	 * @param <A>         累加类型
	 * @param supplier    结果生成器
	 * @param accumulator 累加其
	 * @param combiner    合并器
	 * @return R类型结果
	 */
	default <R, A> R collect(final Supplier<R> supplier, final BiConsumer<R, ? super T> accumulator,
			final BiConsumer<R, R> combiner) {
		return this.stream().collect(supplier, accumulator, combiner);
	}

	/**
	 * 使用param将各个元素给串联成字符串(各元素使用toString转换成字符串)
	 * 
	 * @param params 串联参数设置为null表示采用默认"",
	 *               prefix:前缀,delim:分片内元素分隔,suffix:后缀,lndelim:分片间隔
	 *               分隔,注意对于非并发流采用单分片机制不会用到lndelim
	 * @return 元素被串联后的字符串
	 */
	default String join(final String... params) {
		return this.collect(Lisp.joinclc(params));
	}

	/**
	 * 使用param将各个元素给串联成字符串(各元素使用toString转换成字符串)
	 * 
	 * @param delim 分片内元素分隔
	 * @return 元素被串联后的字符串
	 */
	default String join(final String delim) {
		return this.join(null, delim);
	}

	/**
	 * 数组应用
	 * 
	 * @param <U>    结果类型
	 * @param mapper 结果变换类型 [t]->u, [t] 为 复制数组
	 * @return U类型的结果
	 */
	default <U> U arrayOf(final Function<T[], U> mapper) {
		final T[] ts = this.stream().collect(Types.arrayclc());
		return mapper.apply(ts);
	}

}
