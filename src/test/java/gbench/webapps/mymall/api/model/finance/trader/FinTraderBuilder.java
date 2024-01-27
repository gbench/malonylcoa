package gbench.webapps.mymall.api.model.finance.trader;

import gbench.webapps.mymall.api.model.finance.builder.AbstractFinBuilder;

/**
 * FinAcctBuilder 财务会计构建器
 */
public class FinTraderBuilder extends AbstractFinBuilder<FinTraderBuilder, FinTrader> {

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
		return AbstractFinBuilder.of(FinTraderBuilder.class).add(kvs).build();
	}

}
