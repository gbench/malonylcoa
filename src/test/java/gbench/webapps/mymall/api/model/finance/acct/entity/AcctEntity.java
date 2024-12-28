package gbench.webapps.mymall.api.model.finance.acct.entity;

import gbench.util.jdbc.IMySQL;
import gbench.util.jdbc.kvp.DFrame;
import gbench.util.jdbc.kvp.IRecord;
import gbench.webapps.mymall.api.model.finance.builder.AbstractFinBase;

/**
 * 会计主体
 */
public class AcctEntity extends AbstractFinBase<AcctEntity> {

	/**
	 * 财流的会计主体
	 * 
	 * @param jdbcApp 数据库
	 * @param id      会计主体id
	 */
	public AcctEntity(final IMySQL jdbcApp, final long id) {
		super(jdbcApp);
		this.id = id;
		masterdata = jdbcApp.getLines("t_company", 1).get(0);
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
		return this.masterdata.str("name");
	}

	/**
	 * 公司产品信息
	 * 
	 * @param tbl       数据表
	 * @param entityKey 主体key
	 * @return 公司产品信息
	 */
	public DFrame getLines(final String tbl, final String entityKey) {
		final var sql = String.format("select * from %s where %s=%s", tbl, entityKey, this.id);
		return jdbcApp.sqldframe(sql);
	}

	protected final long id;
	protected final IRecord masterdata;
}
