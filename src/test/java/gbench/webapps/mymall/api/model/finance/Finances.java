package gbench.webapps.mymall.api.model.finance;

import java.util.stream.Stream;

import gbench.util.jdbc.kvp.IRecord;
import gbench.util.type.Types;

/**
 * 财物资金核算的常用工具方法
 */
public class Finances {

	/**
	 * 计算drcr,price,quanty的乘积项目 DR:代表1，CR:代表-1
	 * 
	 * @param lines数据行
	 * @return
	 */
	public static Double dpqsum(final Stream<IRecord> lines) {
		return lines.map(e -> e.dbl("drcr") * e.dbl("price") * e.dbl("quantity")).reduce(0d, Double::sum);
	}

	/**
	 * 计算drcr,price,quanty的乘积项目 DR:代表1，CR:代表-1
	 * 
	 * @param lines数据行
	 * @return
	 */
	public static Double dqsum(final Stream<IRecord> lines) {
		return lines.map(e -> e.dbl("drcr") * e.dbl("quantity")).reduce(0d, Double::sum);
	}

	/**
	 * 计算drcr,price,quanty的乘积项目 DR:代表1，CR:代表-1
	 * 
	 * @param lines数据行
	 * @return
	 */
	public static Double dpqsum(final Iterable<IRecord> lines) {
		return dpqsum(Types.itr2stream(lines));
	}

	/**
	 * 计算drcr,price,quanty的乘积项目 DR:代表1，CR:代表-1
	 * 
	 * @param lines数据行
	 * @return
	 */
	public static Double dqsum(final Iterable<IRecord> lines) {
		return dqsum(Types.itr2stream(lines));
	}

}
