package gbench.webapps.mymall.api.model.finance.acct.invt;

import gbench.util.jdbc.IMySQL;
import gbench.util.jdbc.Jdbcs;
import gbench.util.jdbc.kvp.DFrame;
import gbench.util.jdbc.kvp.IRecord;
import gbench.webapps.mymall.api.model.finance.acct.core.AbstractAcct.Drcr;
import gbench.webapps.mymall.api.model.finance.builder.AbstractFinBase;

import static gbench.util.jdbc.kvp.IRecord.REC;
import static gbench.util.jdbc.sql.SQL.sql;
import java.time.LocalDateTime;

/**
 * 存货对象
 */
public class Inventory extends AbstractFinBase<Inventory> {

	/**
	 * 
	 * @param jdbcApp
	 */
	public Inventory(final IMySQL jdbcApp, final Number companyId) {
		super(jdbcApp);
		this.companyId = companyId.longValue();
	}

	/**
	 * 签发出入库单据
	 * 
	 * @param stock
	 * @return
	 */
	public IRecord billof(final Drcr drcr, final Iterable<IRecord> items, long warehouseId, long orderId,
			long freightId) {
		if (drcr.equals(Drcr.NONE)) {
			return null;
		}

		final var tbl = "t_billof_product";
		final var billType = switch (drcr) {
		case DR -> "receipt";
		case CR -> "invoice";
		default -> "";
		};
		final var bill = REC("bill_type", billType, "issuer_id", this.companyId, "warehouse_id", warehouseId,
				"order_id", orderId, "freight_order_id", freightId, "details", REC("items", items), "creator_id", -1,
				"time", LocalDateTime.now());
		final var id = jdbcApp.withTransaction(sess -> {
			sess.sql2execute2maybe(sql(tbl, bill).insql()).ifPresent(e -> sess.setResult(e.get(0)));
		}).i4("result"); // 提取插入的数据生成的id字段
		System.out.println("INVT#" + id);
		return jdbcApp.getById(tbl, id).orElse(null);
	}

	/**
	 * 签发出入库单据
	 * 
	 * @param stock
	 * @return
	 */
	public IRecord billof(final Drcr drcr, final Iterable<IRecord> items, long warehouseId, long orderId) {
		return this.billof(drcr, items, warehouseId, orderId, -1);
	}

	/**
	 * 签出发票：发货单据
	 * 
	 * @param stock
	 * @return
	 */
	public IRecord invoice(final Iterable<IRecord> items, long warehouseId, long orderId) {
		return this.billof(Drcr.CR, items, warehouseId, orderId, -1);
	}

	/**
	 * 签出收票：收货单据
	 * 
	 * @param stock
	 * @return
	 */
	public IRecord receipt(final Iterable<IRecord> items, long warehouseId, long orderId) {
		return this.billof(Drcr.DR, items, warehouseId, orderId, -1);
	}

	/**
	 * 先进先出
	 * 
	 * @return 先进先出
	 */
	public DFrame fifo() {
		final var sql = String.format("select * from t_billof_product where issuer_id=%d", this.companyId);
		final var dfm = jdbcApp.sqldframe(sql).forEachBy(Jdbcs.h2_json_processor("details"));
		final var cpdfm = jdbcApp
				.sqldframe("select cp.id,p.name from t_company_product cp left join t_product p on cp.product_id=p.id");
		final var lines = dfm.rowS() // 转换成数据行
				.flatMap(e -> e.path2llS("details/items", IRecord::REC) // 展开行项目
						.map(REC("drcr", switch (e.str("bill_type")) { // drcr 根据类型改名
						case "invoice" -> -1; // 入库单
						case "receipt" -> 1; // 出库单
						default -> 0;
						})::derive).map(p -> p.add("name", cpdfm.one2one("id", p.i4("id"), "cp"))))
				.collect(DFrame.dfmclc);
		return lines;
	}

	final long companyId;

}
