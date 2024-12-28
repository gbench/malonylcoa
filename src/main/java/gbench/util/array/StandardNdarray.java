package gbench.util.array;

import java.util.LinkedHashMap;
import java.util.List;
import gbench.util.lisp.Tuple2;

/**
 * NDarray 的标准实现
 * 
 * @author gbench
 *
 * @param <T> 元素类型
 */
public class StandardNdarray<T> extends AbstractNdarray<T> {

	/**
	 * 构造函数
	 *
	 * @param start   开始索引从0开始,inclusive
	 * @param end     结束索引，exlusive
	 * @param strides 分区索引跨度
	 */
	public StandardNdarray(final T[] data, final int start, final int end,
			final LinkedHashMap<List<Integer>, Tuple2<Integer, Integer>> strides) {
		super(data, start, end, strides);
	}

}