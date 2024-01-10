package gbench.webapps.mymall.api.model.finance.acct;

import gbench.util.jdbc.IMySQL;

/**
 * 会计对象
 * 
 * @param <SELF> 自身对象
 */
public abstract class AbstractAcct<SELF> {

	/**
	 * 数据库应用
	 * 
	 * @param jdbcApp 数据库应用
	 */
	protected AbstractAcct(final IMySQL jdbcApp) {
		this.jdbcApp = jdbcApp;
	}

	/**
	 * 初始化
	 */
	@SuppressWarnings("unchecked")
	public SELF intialize() {
		return (SELF) this;
	}

	/**
	 * 交易/订单头寸
	 */
	enum Position {
		/**
		 * 长头买方
		 */
		LONG,
		/**
		 * 空头卖方
		 */
		SHORT
	}; // 订单头寸

	/**
	 * 会计借贷
	 */
	enum Drcr {
		/**
		 * 借方
		 */
		DR,
		/*
		 * 贷方
		 */
		CR
	}; // 会计借贷

	final IMySQL jdbcApp;
}
