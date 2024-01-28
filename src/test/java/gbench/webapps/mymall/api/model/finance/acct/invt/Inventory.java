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
	public IRecord issue(final Drcr drcr, final Iterable<IRecord> items, long warehouseId, long orderId,
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
		System.out.println(String.format("%s#%s", billType, id));
		return jdbcApp.getById(tbl, id).orElse(null);
	}

	/**
	 * 签发出入库单据
	 * 
	 * @param stock
	 * @return
	 */
	public IRecord issue(final Drcr drcr, final Iterable<IRecord> items, long warehouseId, long orderId) {
		return this.issue(drcr, items, warehouseId, orderId, -1);
	}

	/**
	 * 签出发票：发货单据
	 * 
	 * @param stock
	 * @return
	 */
	public IRecord checkout(final Iterable<IRecord> items, long warehouseId, long orderId) {
		return this.issue(Drcr.CR, items, warehouseId, orderId, -1);
	}

	/**
	 * 签出收票：收货单据
	 * 
	 * @param stock
	 * @return
	 */
	public IRecord checkin(final Iterable<IRecord> items, long warehouseId, long orderId) {
		return this.issue(Drcr.DR, items, warehouseId, orderId, -1);
	}

	/**
	 * 签发的单据
	 * 
	 * @return 先进先出
	 */
	public DFrame bills() {
		final var bill_sql = String.format("select * from t_billof_product where issuer_id=%d order by time",
				this.companyId);
		return jdbcApp.sqldframe(bill_sql);
	}

	/**
	 * 先进先出
	 * 
	 * @return 先进先出
	 */
	public DFrame fifo() {
		final var bill_sql = String.format("select * from t_billof_product where issuer_id=%d order by time",
				this.companyId);
		final var cp_sql = "select cp.id,p.name from t_company_product cp left join t_product p on cp.product_id=p.id";
		final var bldfm = jdbcApp.sqldframe(bill_sql).forEachBy(Jdbcs.h2_json_processor("details"));
		final var cpdfm = jdbcApp.sqldframe(cp_sql); // 公司产品
		final var lines = bldfm.rowS().flatMap(e -> e.path2llS("details/items", IRecord::REC) // 展开行项目
				.map(REC("drcr", switch (e.str("bill_type")) { // drcr 根据类型改名
				case "receipt" -> 1; // 入库单
				case "invoice" -> -1; // 出库单
				default -> 0;
				})::derive).map(p -> {
					final var bill_id = e.lng("id"); // 单据id
					final var name = cpdfm.one2one("id", p.i4("id"), "cp").str("name"); // 货物名称
					final var time = e.ldt("time"); // 单据时间
					final var line = p.add("bill_id", bill_id, "name", name, "time", time); // 补充单据信息
					return line;
				})).collect(DFrame.dfmclc);
		return lines;
	}

	final long companyId;

}
