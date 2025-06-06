package gbench.webapps.myfuture.xchg.model.match;

public interface IMatchModel {
	
	/**
	 * 
	 */
	public void pollAndMatchOrders();
	
	public void start();
	
	public void destroy();
}
