package gbench.webapps.mymall.api.model.finance.acct.core;

import static gbench.util.jdbc.kvp.IRecord.getter;

import gbench.util.jdbc.IMySQL;
import gbench.util.jdbc.kvp.DFrame;
import gbench.util.jdbc.kvp.IRecord;
import gbench.util.jdbc.kvp.Tuple2;
import gbench.webapps.mymall.api.model.finance.builder.AbstractFinBuilder;

/**
 * FinAcctBuilder 财务会计构建器
 */
public class FinAcctBuilder extends AbstractFinBuilder<FinAcctBuilder, FinAcct> {
	
	/**
	 * FinAcctBuilder
	 * @param jdbcApp 数据库
	 */
	public FinAcctBuilder(final IMySQL jdbcApp) {
		super(jdbcApp, IRecord.REC());
	}

	@Override
	public FinAcct build() {
		return this.fa(this.params.str("policy"));
	}

	/**
	 * 创建一个 FinAcct 对象
	 * 
	 * @param policy 策略名称
	 * @return FinAcct 对象
	 */
	private FinAcct fa(final String policy) {
		@SuppressWarnings("unchecked")
		final var params = (Tuple2<DFrame, IRecord>) jdbcApp.withTransaction(sess -> {
			final var coa = sess.sql2dframe("select * from t_coa") //
					.forEachBy(e -> { // 将账号改为long格式
						e.set("acctnum", e.lng("acctnum"));
					}); // 会计科目表
			final var policies = sess.sql2dframe("select * from t_acct_policy")
					.pivotTable1("name,bill_type,position,drcr", getter("acctnum")); // 会计策略
			sess.setResult(Tuple2.of(coa, policies)); // 在会话的result属性中设置返回值以向外输出
		}).get("result"); // 提取sess保存的结果值

		return new FinAcct(this.jdbcApp, params._1(), params._2().rec(policy));
	}

}
