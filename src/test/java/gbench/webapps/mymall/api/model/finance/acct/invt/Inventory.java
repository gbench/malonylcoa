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
import java.util.function.BiConsumer;
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
	 * 通过对checkins和checkouts进行时间排序，再结合correspondS就可以实现: <br>
	 * FIFO(checkins的按照时间正序排序),LIFO(checkins的按照时间逆向排序)等会计记账需要的存货成本核算策略。
	 * 
	 * @param checkins  入库单的各个入库数量
	 * @param checkouts 发货单的各个出库数量
	 * @return 发货方案列表,逐项罗列各个发货单对应的入库单的中所需货物数量:
	 *         [{index:发货单单号,requires:发货单中要求的数量,provides:的回应的入库单列表,即[{
	 *         index:入库单索引,quantity:入库单的对应与出库单的数量,lacks:该入库单缺少的数量,lacks_total:整个发货批次的累计缺货数量,eror:缺货信息
	 *         }]}]
	 */
	public static DFrame correspondfm(final double[] checkins, final double[] checkouts) {
		return correspondS(checkins, checkouts).collect(DFrame.dfmclc);
	}

	/**
	 * 将发货单与存货单进行匹配对应(数据流版) <br>
	 * 通过对checkins和checkouts进行时间排序，再结合correspondS就可以实现: <br>
	 * FIFO(checkins的按照时间正序排序),LIFO(checkins的按照时间逆向排序)等会计记账需要的存货成本核算策略。
	 * 
	 * @param checkins  入库单的各个入库数量
	 * @param checkouts 发货单的各个出库数量
	 * @return 发货方案列表,逐项罗列各个发货单对应的入库单的中所需货物数量:
	 *         [{index:发货单单号,requires:发货单中要求的数量,provides:的回应的入库单列表,即[{
	 *         index:入库单索引,quantity:入库单的对应与出库单的数量,lacks:该入库单缺少的数量,lacks_total:整个发货批次的累计缺货数量,eror:缺货信息
	 *         }]}]
	 */
	public static Stream<IRecord> correspondS(final double[] checkins, final double[] checkouts) {
		return corresponds(checkins, checkouts).stream();
	}

	/**
	 * 将发货单与存货单进行匹配对应(列表版本) <br>
	 * 通过对checkins和checkouts进行时间排序，再结合correspondS就可以实现: <br>
	 * FIFO(checkins的按照时间正序排序),LIFO(checkins的按照时间逆向排序)等会计记账需要的存货成本核算策略。
	 * 
	 * @param checkins  入库单的各个入库数量
	 * @param checkouts 发货单的各个出库数量
	 * @return 发货方案列表,逐项罗列各个发货单对应的入库单的中所需货物数量:
	 *         [{index:发货单单号,requires:发货单中要求的数量,provides:的回应的入库单列表,即[{
	 *         index:入库单索引,quantity:入库单的对应与出库单的数量,lacks:该入库单缺少的数量,lacks_total:整个发货批次的累计缺货数量,eror:缺货信息
	 *         }]}]
	 */
	public static List<IRecord> corresponds(final double[] checkins, final double[] checkouts) {
		final Function<double[], double[]> cumsum = origin -> { // 帕累托累计和函数
			final var data = Arrays.copyOf(origin, origin.length); // 复制原来的数量
			for (int i = 1; i < data.length; i++) { // 逐次向后累计递增
				data[i] += data[i - 1]; // 累计递增
			} // for 逐次向后累计递增
			return data; // 累计和
		}; // 帕累托累计和函数
		final var lines = new LinkedList<IRecord>(); // 结果数据行
		final var incums = cumsum.apply(checkins); // 帕累托累计和-入库单累计和
		final var outcums = cumsum.apply(checkouts); // 帕累托累计和-出库单累计和
		int i = 0; // 收货单/入库单索引：读取索引
		int j = 0; // 发货单/出库单所索引：读取索引
		for (; j < outcums.length; j++) { // 逐个发货单进行匹配,发货单读取索引逐次向后递增
			final var provides = new LinkedList<IRecord>(); // 由入库单向发货单提供的发货方案项目明细。[{index:入库单索引,quantity:checkins[index]中需要发货的数量}]
			final var i0 = i; // 记录收货单索引的初始位置
			while (i < incums.length && incums[i] < outcums[j]) { // 直到拓展到收货单的数量累计和大于发货单的数量累计和
				i++; // 继续拓展到下一个收货单读取索引
			} // 当收货单索引拓展到收货单最大数量后就不再增加了
			final var initial = i0 >= incums.length // 收货单索引是否有效
					? -1 // 收货单索引i0非法,用-1标识非法数量
					: j == 0 // 是否是首个发货单
							? Math.min(checkins[0], checkouts[0]) // 首个：收货单数量与发货单数量中的较小者
							: Math.min(incums[i0] - outcums[j - 1], checkouts[j]); // 非首个：初始的发货单所对应的收货单中的数目上次发货剩余与本次发货需求之间的较小者
			final var line = REC("index", j, "requires", checkouts[j], "provides", provides); // 发货方案,index是checkouts的索引，requires是checkouts的需求熟练
			final var iqrb = IRecord.rb("index,quantity"); // checkin的对应项构建器:{index,quantity}的结构
			final BiConsumer<Integer, Function<Integer, Double>> calculate_provides = (pos, qty) -> { // 登记到可库存发货的商品集合。pos:读写位置,qty:数量计算器
				for (int k = i0; k <= pos; k++) { // 遍历checkins从[i0,到pos]这个范围的入库单都需要为checkouts[j]提供发货产品
					final var quantity = qty.apply(k); // 根据索引提取对应checkin入库单的发货数量
					if (quantity > 0) { // 初始数量大于0时才给予写入
						provides.add(iqrb.get(k, quantity)); // 为checkouts[j]匹配checkins[k]的发货数量
					} // if
				} // for
			}; // calculate_provides
			final var pos = i; // 常量当前库存的读写位置
			if (i >= incums.length) { // 库存不足, 需要考虑i==0也就是checkins中没有数据的情况
				calculate_provides.accept(pos, k -> k == i0 ? initial : k >= incums.length ? -1 : checkins[k]);
				final var lacks_total = outcums[j] - (i <= 0 ? 0 : incums[i - 1]); // 缺货累计,整个发货批次的累计缺货数量
				final var lacks = Math.min(checkouts[j], lacks_total); // 当前发货单中的缺货数量,系统没有二外的入库单来提供发货产品了。
				final var error = String.format("空间不足,缺少:%f", lacks_total); //
				lines.add(line.derive("lacks", lacks, "lacks_total", lacks_total, "eror", error));
				// break;
			} else {// 库存足够
				final var unused = incums[i] - outcums[j]; // 多余部分未使用的数量
				final var used = checkins[i] - unused; // 实际使用的部分,最终数量
				calculate_provides.accept(pos, k -> k == i0 ? initial : k == pos ? used : checkins[k]);
				lines.add(line);
			} // if i >= incums.length
		} // for j 逐个发货单进行匹配,发货单索引逐次向后递增

		return lines;
	}

	final long companyId;

}
