package gbench.webapps.mymall.api.model.finance.acct.elems.invt;

import gbench.util.io.Output;
import gbench.util.jdbc.IMySQL;
import gbench.util.jdbc.Jdbcs;
import gbench.util.jdbc.kvp.DFrame;
import gbench.util.jdbc.kvp.IRecord;
import gbench.webapps.mymall.api.model.finance.acct.elems.AbstractElem;

import static gbench.util.jdbc.kvp.IRecord.REC;

/**
 * 存货对象
 */
public class Inventory extends AbstractElem<Inventory> {

	/**
	 * 
	 * @param jdbcApp
	 */
	public Inventory(final IMySQL jdbcApp, final Number companyId) {
		super(jdbcApp);
		this.companyId = companyId.longValue();
	}

	/**
	 * 产品入库
	 * 
	 * @param stock
	 * @return
	 */
	public Inventory stockIn(final IRecord stock) {
		return this.self();
	}

	/**
	 * 产品出库
	 * 
	 * @param stock
	 * @return
	 */
	public Inventory stockOut(final IRecord stock) {
		return this.self();
	}

	/**
	 * 先进先出
	 * 
	 * @return 先进先出
	 */
	public DFrame fifo() {
		final var sql = String.format("select * from t_billof_product where issuer_id=%d", this.companyId);
		final var dfm = jdbcApp.sqldframe(sql).forEachBy(Jdbcs.h2_json_processor("details"));
		Output.println(dfm);
		final var lines = dfm.rowS() //
				.flatMap(e -> e.path2llS("details/items", IRecord::REC) // 展开行项目
						.map(REC("drcr", switch (e.str("bill_type")) { // drcr 根据类型改名
						case "invoice" -> -1; // 入库单
						case "receipt" -> 1; // 出库单
						default -> 0;
						})::derive))
				.collect(DFrame.dfmclc);
		return lines;
	}

	final long companyId;

}
