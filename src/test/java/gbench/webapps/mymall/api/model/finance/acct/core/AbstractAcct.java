package gbench.webapps.mymall.api.model.finance.acct.core;

import gbench.util.jdbc.IMySQL;
import gbench.webapps.mymall.api.model.finance.builder.AbstractFinBase;

/**
 * 会计对象
 * 
 * @param <S> 自身对象
 */
public abstract class AbstractAcct<S> extends AbstractFinBase<S> {

	/**
	 * 数据库应用
	 * 
	 * @param jdbcApp 数据库应用
	 */
	protected AbstractAcct(final IMySQL jdbcApp) {
		super(jdbcApp);
	}

	/**
	 * 初始化
	 */
	public S intialize() {
		return this.self();
	}

	/**
	 * 交易/订单头寸
	 */
	public enum Position {
		/**
		 * 交易头寸：多头买方
		 */
		LONG,
		/**
		 * 交易头寸：空头卖方
		 */
		SHORT,
		/**
		 * 非交易头寸
		 */
		NONE,
	}; // 订单头寸

	/**
	 * 会计借贷
	 */
	public enum Drcr {
		/**
		 * 借方
		 */
		DR,
		/*
		 * 贷方
		 */
		CR,
		/*
		 * 非会计借贷
		 */
		NONE
	}; // 会计借贷

}
