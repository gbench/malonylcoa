package gbench.webapps.mymall.api.model.finance.trading;

import static gbench.util.jdbc.Jdbcs.h2_json_processor;
import static gbench.util.jdbc.kvp.IRecord.REC;
import static gbench.util.jdbc.sql.SQL.sql;
import static gbench.webapps.mymall.api.model.finance.acct.core.AbstractAcct.Position.LONG;
import static gbench.webapps.mymall.api.model.finance.acct.core.AbstractAcct.Position.SHORT;

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
		inventory = this.getInventory(id);
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
				"quantity", rndgen.nextInt(10) + 1 // 产品数量
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
	 * @return Inventory
	 */
	public Inventory getInventory(final long companyId) {
		final var inventory = InventoryBuilder.build("entityId", companyId);
		return inventory;
	}

	/**
	 * 存货对象
	 * 
	 * @return Inventory
	 */
	public Inventory getInventory() {
		return this.inventory;
	}

	/**
	 * 签出发票：发货单据
	 * 
	 * @param itemdfm
	 * @param warehouseId
	 * @param orderId
	 * @return
	 */
	public IRecord invoice(final DFrame itemdfm, final long warehouseId, final long orderId) {
		return inventory.checkout(itemdfm.rows(), warehouseId, orderId);
	}

	/**
	 * 签出收票：收货单据
	 * 
	 * @param itemdfm
	 * @param warehouseId
	 * @param orderId
	 * @return
	 */
	public IRecord receipt(final DFrame itemdfm, final long warehouseId, final long orderId) {
		return inventory.checkin(itemdfm.rows(), warehouseId, orderId);
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

		final var parta_id = position.equals(LONG) ? this.id : counterpartId; // 甲方
		final var partb_id = position.equals(SHORT) ? this.id : counterpartId; // 乙方
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
	 * 随机选取
	 * 
	 * @param dfm
	 * @param maxsize
	 * @return DFrame
	 */
	public static DFrame rnd(final DFrame dfm, final int maxsize) {
		return dfm.shuffle().head(new Random().nextInt(maxsize) + 1);
	}

	final Random rndgen = new Random(); // 随机发生器
	final Inventory inventory; // 随机发生器
}
