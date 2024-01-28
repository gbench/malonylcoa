package gbench.webapps.mymall.api.model.finance.trading;

import static gbench.util.jdbc.Jdbcs.h2_json_processor;

import gbench.util.jdbc.IMySQL;
import gbench.util.jdbc.kvp.DFrame;
import gbench.webapps.mymall.api.model.finance.acct.entity.AcctEntity;

/**
 * 交易主体
 */
public class FinTrader extends AcctEntity {

	/**
	 * 财流的交易主体
	 * 
	 * @param id
	 */
	public FinTrader(final IMySQL jdbcApp, final long id) {
		super(jdbcApp,id);
	}

	/**
	 * 公司产品信息
	 * 
	 * @return 公司产品信息
	 */
	public DFrame getProvidables() {
		return this.getLines("t_company_product", "company_id").forEachBy(h2_json_processor("attrs"));
	}
}
