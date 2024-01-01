package gbench.util.jdbc.annotation;

import java.lang.reflect.Method;
import java.util.function.Function;

import gbench.util.jdbc.Jdbc;
import gbench.util.jdbc.kvp.IRecord;

/**
 * Jdbc拦截器:SQL 的处理器
 * 
 * @author xuqinghua
 *
 */
public abstract class JdbcPatternPreprocessor {
	/**
	 * 构造函数：记得调用初始化函数
	 */
	public JdbcPatternPreprocessor() {
		this.initialize();
	}

	/**
	 * 初始化函数
	 */
	public abstract void initialize();

	/**
	 * Pattern/Preprocessor ARGS
	 * 
	 * @author gbench
	 *
	 */
	public static class PPARG {

		/**
		 * Pattern/Preprocessor ARGS
		 * 
		 * @param method     方法对象
		 * @param params     参数列表
		 * @param sqlpattern sql模版
		 * @param jdbc       jdbc对象
		 */
		public PPARG(final Method method, final IRecord params, final String sqlpattern, final Jdbc jdbc) {
			this.method = method;
			this.params = params;
			this.sqlpattern = sqlpattern;
			this.jdbc = jdbc;
		}

		/**
		 * Pattern/Preprocessor ARGS 的名称
		 * 
		 * @return PPARG 的名称
		 */
		public String getName() {
			return method.getName();
		}

		protected final Method method;
		protected final IRecord params;
		protected final String sqlpattern;
		protected final Jdbc jdbc;
	}

	/**
	 * 函数:PARG -> String
	 * 
	 * @author gbench
	 *
	 */
	public interface P2S extends Function<JdbcPatternPreprocessor.PPARG, String> {
	}

	/**
	 * 这是对 SqlPatternPreProcessor的实现
	 * 
	 * @param method     方法对象
	 * @param params     参数列表
	 * @param sqlpattern SQL语句
	 * @param jdbc       jdbc对象
	 * @return sqlpattern 实例化的SQL语句
	 */
	public String handle(final Method method, final IRecord params, final String sqlpattern, final Jdbc jdbc) {
		final var parg = new PPARG(method, params, sqlpattern, jdbc);// 把接口参数package成统一参数对象．
		final String key = parg.getName();
		if (!callbacks.has(key))
			return sqlpattern;
		return callbacks.evaluate(key, parg, (String) null);
	}

	/**
	 * 在接口函数中实例化：按照如下实现，就可以 <br>
	 * Jdbc.newInstance(xxxDatabase.class,new JdbcPatternPreprocessor(){ <br>
	 * }::handle); <br>
	 * callbacks=REC("methodName",(PPARG)pp-&gt;{return pparg.pattern}); <br>
	 */
	protected IRecord callbacks;
}