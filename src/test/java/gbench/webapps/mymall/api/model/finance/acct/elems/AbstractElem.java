package gbench.webapps.mymall.api.model.finance.acct.elems;

import gbench.util.jdbc.IMySQL;

/**
 * 存货
 * 
 * @param <S> 自身对象
 */
public class AbstractElem<S> {

	/**
	 * 
	 * @param jdbcApp
	 */
	public AbstractElem(final IMySQL jdbcApp) {
		this.jdbcApp = jdbcApp;
	}

	/**
	 * 
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public S self() {
		return (S) this;
	}

	final protected IMySQL jdbcApp;

}
