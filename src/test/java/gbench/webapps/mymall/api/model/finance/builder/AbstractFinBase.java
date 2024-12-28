package gbench.webapps.mymall.api.model.finance.builder;

import gbench.util.jdbc.IMySQL;

/**
 * 财流核算对象
 * 
 * @param <S>
 */
public class AbstractFinBase<S> {

	/**
	 * 数据库应用
	 * 
	 * @param jdbcApp 数据库应用
	 */
	public AbstractFinBase(final IMySQL jdbcApp) {
		this.jdbcApp = jdbcApp;
	}

	/**
	 * 本身元素
	 * 
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public S self() {
		return (S) this;
	}

	final protected IMySQL jdbcApp;
}
