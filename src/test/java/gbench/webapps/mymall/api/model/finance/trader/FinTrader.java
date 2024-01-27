package gbench.webapps.mymall.api.model.finance.trader;

import gbench.util.jdbc.IMySQL;
import gbench.util.jdbc.kvp.DFrame;
import gbench.webapps.mymall.api.model.finance.builder.AbstractFinBase;

public class FinTrader extends AbstractFinBase<FinTrader> {

	/**
	 * 
	 * @param id
	 */
	public FinTrader(IMySQL jdbcApp, final long id) {
		super(jdbcApp);
		this.id = id;
	}

	public long getId() {
		return this.id;
	}

	public DFrame getProducts() {
		return null;
	}

	private final long id;
}
