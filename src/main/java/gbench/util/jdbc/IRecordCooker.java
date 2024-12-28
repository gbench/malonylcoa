package gbench.util.jdbc;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;

import gbench.util.jdbc.kvp.IRecord;

/**
 * 数据烹饪大师
 *
 * @author gbench
 */
public interface IRecordCooker {
	/**
	 * 准备一下开始工作
	 * 
	 * @param n   结果集的列数量
	 * @param rs  结果集
	 * @param rsm 结果元数据
	 */
	void initialize(int n, ResultSet rs, ResultSetMetaData rsm);

	/**
	 * 对数据记录进行操作
	 * 
	 * @param rec 数据记录
	 * @return 操作后的数据记录
	 * @throws SQLException
	 */
	IRecord cook(IRecord rec) throws SQLException;// 开始工作
}