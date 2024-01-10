package gbench.webapps.mymall.api.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import gbench.util.jdbc.IMySQL;
import gbench.webapps.mymall.api.model.finance.acct.FinAcctBuilder;

@Configuration
public class MyMallConfig {

	/**
	 * 
	 * @param jdbcApp
	 * @return
	 */
	@Bean
	FinAcctBuilder faBuilder(final IMySQL jdbcApp) {
		return new FinAcctBuilder(jdbcApp);
	}
}
