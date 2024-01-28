package gbench.webapps.mymall.api.model.finance.trading;

import static gbench.util.jdbc.Jdbcs.h2_json_processor;
import static gbench.util.jdbc.kvp.IRecord.REC;
import static gbench.util.jdbc.sql.SQL.sql;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Random;

import gbench.util.jdbc.IMySQL;
import gbench.util.jdbc.kvp.DFrame;
import gbench.util.jdbc.kvp.IRecord;
import gbench.webapps.mymall.api.model.finance.acct.core.AbstractAcct.Position;
import gbench.webapps.mymall.api.model.finance.acct.entity.AcctEntity;
import gbench.webapps.mymall.api.model.finance.acct.invt.Inventory;
import gbench.webapps.mymall.api.model.finance.acct.invt.InventoryBuilder;

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
		super(jdbcApp, id);
	}

	/**
	 * 公司产品信息
	 * 
	 * @return 公司产品信息
	 */
	public DFrame getProvidables() {
		return this.getLines("t_company_product", "company_id").forEachBy(h2_json_processor("attrs"));
	}

	/**
	 * 公司产品信息
	 * 
	 * @return 公司产品信息
	 */
	public DFrame rpvds(final int size) {
		return rnd(getProvidables(), size);
	}

	/**
	 * 公司产品信息
	 * 
	 * @return 公司产品信息
	 */
	public DFrame roffers(final int size) {
		return this.rpvds(size).fmap(e -> REC( //
				"id", e.i4("id"), // 公司产品
				"price", e.rec("attrs").dbl("price") * (1 + rndgen.nextDouble(1)), // 产品价格 上浮基准价格10%
				"quantity", rndgen.nextInt(10) // 产品数量
		));
	}

	/**
	 * 创建订单
	 * 
	 * @param position      订单持有头寸
	 * @param counterpartId
	 * @param products
	 * @return
	 */
	public IRecord purchaseOrder(final long counterpartId, final Iterable<IRecord> products) {
		return this.buildOrder(Position.LONG, counterpartId, products);
	}

	/**
	 * 创建订单
	 * 
	 * @param position      订单持有头寸
	 * @param counterpartId
	 * @param products
	 * @return
	 */
	public IRecord saleOrder(final long counterpartId, final Iterable<IRecord> products) {
		return this.buildOrder(Position.SHORT, counterpartId, products);
	}

	/**
	 * 存货对象
	 * 
	 * @return
	 */
	public Inventory getInventory() {
		final var inventory = InventoryBuilder.build("entityId", this.id);
		return inventory;
	}

	/**
	 * 签出发票：发货单据
	 * 
	 * @param stock
	 * @return
	 */
	public IRecord invoice(final Iterable<IRecord> items, long warehouseId, long orderId) {
		return this.getInventory().invoice(items, warehouseId, orderId);
	}

	/**
	 * 签出发票：发货单据
	 * 
	 * @param stock
	 * @return
	 */
	public IRecord invoice(final DFrame itemdfm, long warehouseId, long orderId) {
		return this.getInventory().invoice(itemdfm.rows(), warehouseId, orderId);
	}

	/**
	 * 签出收票：收货单据
	 * 
	 * @param stock
	 * @return
	 */
	public IRecord receipt(final Iterable<IRecord> items, long warehouseId, long orderId) {
		return this.getInventory().receipt(items, warehouseId, orderId);
	}

	/**
	 * 签出收票：收货单据
	 * 
	 * @param stock
	 * @return
	 */
	public IRecord receipt(final DFrame itemdfm, long warehouseId, long orderId) {
		return this.getInventory().receipt(itemdfm.rows(), warehouseId, orderId);
	}

	/**
	 * 创建订单
	 * 
	 * @param position      订单持有头寸
	 * @param counterpartId 对手方
	 * @param products      订单产品
	 * @return
	 */
	public IRecord buildOrder(final Position position, final long counterpartId, final Iterable<IRecord> products) {
		if (position.equals(Position.NONE)) {
			return null;
		}

		final var parta_id = position.equals(Position.LONG) ? this.id : counterpartId; // 甲方
		final var partb_id = position.equals(Position.SHORT) ? this.id : counterpartId; // 乙方
		final var items = Optional.ofNullable(products).orElseGet(() -> this.roffers(5).rows());
		final var order = REC("parta_id", parta_id, "partb_id", partb_id, "details", REC("items", items), "creator_id",
				-1, "time", LocalDateTime.now());
		final var id = jdbcApp.withTransaction(sess -> {
			sess.sql2execute2maybe(sql("t_order", order).insql()).ifPresent(e -> sess.setResult(e.get(0)));
		}).i4("result"); // 提取插入的数据生成的id字段

		return jdbcApp.sqldframe(String.format("select * from t_order where id=%s", id))
				.forEachBy(h2_json_processor("details")).head();
	}

	/**
	 * 
	 * @param dfm
	 * @param size
	 * @return
	 */
	public static DFrame rnd(final DFrame dfm, final int size) {
		return dfm.shuffle().head(new Random().nextInt(size));
	}

	final Random rndgen = new Random(); // 随机发生器
}
