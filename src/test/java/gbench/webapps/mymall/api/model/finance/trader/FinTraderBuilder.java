package gbench.webapps.mymall.api.model.finance.trader;

import gbench.util.jdbc.IMySQL;
import gbench.util.jdbc.kvp.IRecord;
import gbench.webapps.mymall.api.model.finance.builder.AbstractFinBuilder;

/**
 * FinAcctBuilder 财务会计构建器
 */
public class FinTraderBuilder extends AbstractFinBuilder<FinTraderBuilder, FinTrader> {
	
	/**
	 * FinAcctBuilder
	 * @param jdbcApp 数据库
	 */
	public FinTraderBuilder(final IMySQL jdbcApp) {
		super(jdbcApp, IRecord.REC());
	}

	@Override
	public FinTrader build() {
		return this.ft(this.params.str("id"));
	}

	/**
	 * 创建一个 FinTrader 对象
	 * 
	 * @param id 策略名称
	 * @return FinTrader 对象
	 */
	private FinTrader ft(final String id) {
		return null;
	}

}
