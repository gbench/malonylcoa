package gbench.webapps.mymall.api.model.finance.trader;

import static gbench.util.jdbc.Jdbcs.h2_json_processor;

import gbench.util.jdbc.IMySQL;
import gbench.util.jdbc.kvp.DFrame;
import gbench.util.jdbc.kvp.IRecord;
import gbench.webapps.mymall.api.model.finance.builder.AbstractFinBase;

/**
 * 交易主体
 */
public class FinTrader extends AbstractFinBase<FinTrader> {

	/**
	 * 财流的交易主体
	 * 
	 * @param id
	 */
	public FinTrader(final IMySQL jdbcApp, final long id) {
		super(jdbcApp);
		this.id = id;
		entity = jdbcApp.getLines("t_company", 1).get(0);
	}

	/**
	 * 公司id
	 * 
	 * @return
	 */
	public long getId() {
		return this.id;
	}

	/**
	 * 公司id
	 * 
	 * @return 交易实体
	 */
	public String getName() {
		return this.entity.str("name");
	}

	/**
	 * 公司产品信息
	 * 
	 * @return 公司产品信息
	 */
	public DFrame getProducts() {
		final var sql = String.format("select * from t_company_product where company_id=%s", this.id);
		return jdbcApp.sqldframe(sql).forEachBy(h2_json_processor("attrs"));
	}

	private final long id;
	private final IRecord entity;
}
