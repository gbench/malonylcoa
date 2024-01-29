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
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;

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

	/**
	 * 将发货单与存货单进行匹配对应(数据流版) <br>
	 * 通过排序对receipts和invoices个这种排序再结合correspondS就可以实现: <br>
	 * FIFO(receipts的按照时间正序排序),LIFO(receipts的按照时间逆向排序)等会计记账需要的存货成本核算策略。
	 * 
	 * @param receipts 入库单的各个入库数量
	 * @param invoices 发货单的各个出库数量
	 * @return 发货方案列表,逐项罗列各个发货单对应的入库单的中所需货物数量:
	 *         [{id:发货单单号,requires:发货单中要求的数量,provides:的回应的入库单列表,即[{
	 *         index:入库单索引,quantity:入库单的对应与出库单的数量,lacks:该入库单缺少的数量,eror:缺货信息 }]}]
	 */
	public static DFrame correspondfm(final double[] receipts, final double[] invoices) {
		return correspondS(receipts, invoices).collect(DFrame.dfmclc);
	}

	/**
	 * 将发货单与存货单进行匹配对应(数据流版) <br>
	 * 通过排序对receipts和invoices个这种排序再结合correspondS就可以实现: <br>
	 * FIFO(receipts的按照时间正序排序),LIFO(receipts的按照时间逆向排序)等会计记账需要的存货成本核算策略。
	 * 
	 * @param receipts 入库单的各个入库数量
	 * @param invoices 发货单的各个出库数量
	 * @return 发货方案列表,逐项罗列各个发货单对应的入库单的中所需货物数量:
	 *         [{id:发货单单号,requires:发货单中要求的数量,provides:的回应的入库单列表,即[{
	 *         index:入库单索引,quantity:入库单的对应与出库单的数量,lacks:该入库单缺少的数量,eror:缺货信息 }]}]
	 */
	public static Stream<IRecord> correspondS(final double[] receipts, final double[] invoices) {
		return corresponds(receipts, invoices).stream();
	}

	/**
	 * 将发货单与存货单进行匹配对应(列表版本) <br>
	 * 通过排序对receipts和invoices个这种排序再结合correspondS就可以实现: <br>
	 * FIFO(receipts的按照时间正序排序),LIFO(receipts的按照时间逆向排序)等会计记账需要的存货成本核算策略。
	 * 
	 * @param receipts 入库单的各个入库数量
	 * @param invoices 发货单的各个出库数量
	 * @return 发货方案列表,逐项罗列各个发货单对应的入库单的中所需货物数量:
	 *         [{id:发货单单号,requires:发货单中要求的数量,provides:的回应的入库单列表,即[{
	 *         index:入库单索引,quantity:入库单的对应与出库单的数量,lacks:该入库单缺少的数量,eror:缺货信息 }]}]
	 */
	public static List<IRecord> corresponds(final double[] receipts, final double[] invoices) {
		final Function<double[], double[]> cumsum = origin -> { // 帕累托累计和函数
			double[] data = Arrays.copyOf(origin, origin.length); // 复制原来的数量
			for (int i = 1; i < data.length; i++) { // 逐次向后累计递增
				data[i] += data[i - 1]; // 累计递增
			} // for 逐次向后累计递增
			return data; // 累计和
		}; // 帕累托累计和函数
		final List<IRecord> lines = new LinkedList<IRecord>();
		final double[] rcpcums = cumsum.apply(receipts); // 帕累托累计和
		final double[] ivccums = cumsum.apply(invoices); // 帕累托累计和
		int i = 0; // 收货单/入库单索引
		int j = 0; // 发货单/出库单所索引
		for (; j < ivccums.length; j++) { // 逐个发货单进行匹配,发货单索引逐次向后递增
			final var provides = new LinkedList<IRecord>();
			final var i0 = i; // 记录初始位置
			while (i < rcpcums.length && rcpcums[i] < ivccums[j]) { // 直到拓展到收货单的数量累计和大于发货单的数量累计和
				i++; // 继续拓展到下一个收货单
			} // 当收货单索引拓展到收货单最大数量后就不再增加了
			final var initial = i0 >= rcpcums.length // 发货单索引是否有效
					? -1 // 发货单索引非法,用-1标识非法数量
					: j == 0 // 是否时首个发货单
							? Math.min(receipts[0], invoices[0]) // 首个：收货单数量与发货单数量中的较小者
							: Math.min(rcpcums[i0] - ivccums[j - 1], invoices[j]); // 非首个：初始的发货单所对应的收货单中的数目上次发货剩余与本次发货需求之间的较小着
			final var line = REC("id", j, "requires", invoices[j], "provides", provides);
			if (i >= rcpcums.length) { // 库存不足
				for (int k = i0; k < i; k++) {
					final var quantity = k == i0 ? initial : receipts[k];
					if (quantity > 0) { // 初始数量大于0时才给予写入
						provides.add(REC("index", k, "quantity", quantity));
					} // if
				}
				final var lacks = ivccums[j] - rcpcums[i - 1]; // 缺失数量
				lines.add(
						line.derive("lacks", lacks, "eror", String.format("空间不足,缺少:%f", ivccums[j] - rcpcums[i - 1])));
				// break;
			} else {// 库存足够
				final var unused = rcpcums[i] - ivccums[j]; // 多余部分未使用的数量
				final var used = receipts[i] - unused; // 实际使用的部分,最终数量
				for (int k = i0; k <= i; k++) {
					final var quantity = k == i0 ? initial : k == i ? used : receipts[k];
					if (quantity > 0) { // 初始数量大于0时才给予写入
						provides.add(REC("index", k, "quantity", quantity));
					} // if
				}
				lines.add(line);
			} // if
		}

		return lines;
	}

	final long companyId;

}
