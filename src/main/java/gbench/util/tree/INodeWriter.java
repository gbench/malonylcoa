package gbench.util.tree;

import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;
import gbench.util.function.TriConsumer;

/**
 * 节点书写器
 * 
 * @author gbench
 *
 * @param <N> 节点类型
 */
public interface INodeWriter<N> {

	/**
	 * 节点转换成Json
	 * 
	 * @param pre_processor  前处理 (sb:操作缓存,e:节点元素)-&gt;s
	 * @param post_processor 后处理 (sb:操作缓存,e:节点元素)-&gt;s
	 * @return json字符串
	 */
	public String json(final BiFunction<StringBuilder, N, String> pre_processor,
			final BiFunction<StringBuilder, N, String> post_processor);

	/**
	 * 节点转换成Json
	 * 
	 * @param <N>            节点类型
	 * @param node           树形元素节点
	 * @param get_children   获取子节点
	 * @param pre_processor  前处理 (sb:操作缓存,e:节点元素)-&gt;s
	 * @param post_processor 后处理 (sb:操作缓存,e:节点元素)-&gt;s
	 * @return json字符串
	 */
	public static <N> String writeJson(final N node, final Function<N, Iterable<N>> get_children,
			final BiFunction<StringBuilder, N, String> pre_processor,
			final BiFunction<StringBuilder, N, String> post_processor) {
		return writeJson(null, node, get_children, (sb, e) -> sb.append(pre_processor.apply(sb, e)),
				(sb, e) -> sb.append(post_processor.apply(sb, e)));
	}

	/**
	 * 节点转换成Json
	 * 
	 * @param <N>            节点类型
	 * @param node           树形元素节点
	 * @param get_children   获取子节点
	 * @param pre_processor  前处理 (sb:操作缓存,e:节点元素)-&gt;{}
	 * @param post_processor 后处理 (sb:操作缓存,e:节点元素)-&gt;{}
	 * @return json字符串
	 */
	public static <N> String writeJson(final N node, final Function<N, Iterable<N>> get_children,
			final BiConsumer<StringBuilder, N> pre_processor, final BiConsumer<StringBuilder, N> post_processor) {
		return writeJson(null, node, get_children, pre_processor, post_processor);
	}

	/**
	 * 节点转换成Json
	 * 
	 * @param <N>            节点类型
	 * @param builder        字符串构建器
	 * @param node           树形元素节点
	 * @param get_children   获取子节点
	 * @param pre_processor  前处理 (sb:操作缓存,e:节点元素)-&gt;{}
	 * @param post_processor 后处理 (sb:操作缓存,e:节点元素)-&gt;{}
	 * @return json字符串
	 */
	public static <N> String writeJson(final StringBuilder builder, final N node,
			final Function<N, Iterable<N>> get_children, final BiConsumer<StringBuilder, N> pre_processor,
			final BiConsumer<StringBuilder, N> post_processor) {
		return writeJson(null, node, get_children, pre_processor, (sb, prev, c) -> sb.append(null != prev ? "," : ""),
				post_processor);
	}

	/**
	 * 节点转换成Json
	 * 
	 * @param <N>            节点类型
	 * @param builder        字符串构建器
	 * @param node           树形元素节点
	 * @param get_children   获取子节点
	 * @param pre_processor  前处理 (sb:操作缓存,e:节点元素)-&gt;{}
	 * @param processor      数据处理节点 (sb:操作缓存,prev:previous前驱节点,c:current当前节点)-&gt;{}
	 * @param post_processor 后处理 (sb:操作缓存,e:节点元素)-&gt;{}
	 * @return json字符串
	 */
	public static <N> String writeJson(final StringBuilder builder, final N node,
			final Function<N, Iterable<N>> get_children, final BiConsumer<StringBuilder, N> pre_processor,
			final TriConsumer<StringBuilder, N, N> processor, final BiConsumer<StringBuilder, N> post_processor) {
		final var sb = Optional.ofNullable(builder).orElse(new StringBuilder());
		if (null != pre_processor) {
			pre_processor.accept(sb, node);
		} // if
		final var cc = get_children.apply(node);
		if (cc != null) {
			N prev = null;
			if (processor != null) { // 存在中间处理器
				for (final N c : cc) {
					processor.accept(sb, prev, c);
					writeJson(sb, c, get_children, pre_processor, processor, post_processor);
					prev = c;
				} // for
			} else { // 不存在中间处理器
				for (final N c : cc) {
					writeJson(sb, c, get_children, pre_processor, null, post_processor);
					prev = c;
				} // for
			} // if
		} // if
		if (null != post_processor) {
			post_processor.accept(sb, node);
		} // if

		return sb.toString();
	}

}
