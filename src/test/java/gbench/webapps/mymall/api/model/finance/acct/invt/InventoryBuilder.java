package gbench.webapps.mymall.api.model.finance.acct.invt;

import gbench.webapps.mymall.api.model.finance.builder.AbstractFinBuilder;

/**
 * FinAcctBuilder 财务会计构建器
 */
public class InventoryBuilder extends AbstractFinBuilder<InventoryBuilder, Inventory> {

	/**
	 * 存货构建器对象
	 */
	public InventoryBuilder() {
		super();
	}

	/**
	 * 创建一个 Inventory 对象
	 * 
	 * entityId 会计主体id
	 * 
	 * @return Inventory
	 */
	public Inventory build() {
		return new Inventory(this.jdbcApp, this.params.num("entityId"));
	}

	/**
	 * InventoryBuilder
	 * 
	 * @return InventoryBuilder
	 */
	public static InventoryBuilder of(final Object... kvs) {
		return AbstractFinBuilder.of(InventoryBuilder.class).add(kvs);
	}

	/**
	 * 创建一个存货对象
	 * 
	 * @return Inventory
	 */
	public static Inventory build(final Object... kvs) {
		return InventoryBuilder.of(kvs).build();
	}
}
