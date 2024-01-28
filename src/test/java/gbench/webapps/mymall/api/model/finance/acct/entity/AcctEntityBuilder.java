package gbench.webapps.mymall.api.model.finance.acct.entity;

import gbench.webapps.mymall.api.model.finance.builder.AbstractFinBuilder;

/**
 * FinAcctBuilder 财务会计构建器
 */
public class AcctEntityBuilder extends AbstractFinBuilder<AcctEntityBuilder, AcctEntity> {

	@Override
	public AcctEntity build() {
		return new AcctEntity(this.jdbcApp, this.params.lng("id"));
	}

	/**
	 * 创建一个存货对象
	 * 
	 * @param kvs 参数列表
	 * @return Inventory
	 */
	public static AcctEntity build(final Object... kvs) {
		return AbstractFinBuilder.of(AcctEntityBuilder.class).add(kvs).build();
	}

}
