package gbench.webapps.mymall.api.model.finance.trading;

import gbench.webapps.mymall.api.model.finance.acct.entity.AcctEntityBuilder;
import gbench.webapps.mymall.api.model.finance.builder.AbstractFinBuilder;

/**
 * FinAcctBuilder 财务会计构建器
 */
public class FinTraderBuilder extends AcctEntityBuilder {

	@Override
	public FinTrader build() {
		return new FinTrader(this.jdbcApp, this.params.lng("id"));
	}

	/**
	 * 创建一个存货对象
	 * 
	 * @param kvs 参数列表
	 * @return Inventory
	 */
	public static FinTrader build(final Object... kvs) {
		return (FinTrader) AbstractFinBuilder.of(FinTraderBuilder.class).add(kvs).build();
	}
}
