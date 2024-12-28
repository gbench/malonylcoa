package gbench.util.jdbc.function;

/**
 *
 * @author gbench accept 返回 false终止遍历
 * @param <T> 输入参数1类型 用在 traverse_throws T类型就是节点类型
 * @param <U> 输入参数2的类型 用在 traverse_throws U类型就是当前节点所在阶层的数值的类型那个
 */
public interface ThrowableBiConsumer<T, U> {
	/**
	 * 
	 * @param t 第一参数
	 * @param u 第二参数
	 * @return 执行结果状态
	 * @throws Exception 一场
	 */
	boolean accept(final T t, final U u) throws Exception;
}