package gbench.webapps.mymall.api.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import gbench.util.jdbc.IMySQL;
import gbench.webapps.mymall.api.model.finance.acct.core.FinAcctBuilder;

@Configuration
public class MyMallConfig {

	/**
	 * 会计对象的创建者
	 * 
	 * @param jdbcApp 数据库应用
	 * @return FinAcctBuilder
	 */
	@Bean
	public FinAcctBuilder fabuilder(final IMySQL jdbcApp) {
		return new FinAcctBuilder(jdbcApp);
	}
}
