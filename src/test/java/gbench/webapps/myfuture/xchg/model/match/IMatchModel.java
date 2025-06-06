package gbench.webapps.myfuture.xchg.model.match;

import gbench.util.lisp.DFrame;

public interface IMatchModel {

	/**
	 * 模型启动
	 */
	public void start();

	/**
	 * 订单处理
	 * 
	 * @param ordfrm
	 */
	public void handleOrders(final DFrame ordfrm);

	/**
	 * 模型销毁
	 */
	public void destroy();
}
