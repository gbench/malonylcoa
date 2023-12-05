package gbench.webapps.crawler.api.model.srch;

import java.io.IOException;
import java.util.Arrays;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import gbench.util.data.DataApp;

/**
 * Jdbc搜索引擎
 * 
 * @author gbench
 *
 */
public class JdbcSrchEngine extends AbstractJdbcSrchEngine {

	/**
	 * 搜索引擎
	 * 
	 * @param jdbc       数据库链接
	 * @param expireTime 索引文件的过期世家
	 */
	public JdbcSrchEngine(DataApp jdbc, int expireTime) {
		this.dataApp = jdbc;
		this.expireTime = expireTime;
	}

	/**
	 * 数据重建
	 * 
	 * @return
	 */
	public boolean isExpired() {
		final long modified = getIndexHome().getDirectory().toFile().lastModified();
		final long now = System.currentTimeMillis();// 当前系统时间
		final long elapse = now - modified;// 时间经历时长
		if (elapse <= expireTime * 1000) {// 没有超时则不予进行索引文件的重建
			return false;
		} else {
			System.out.println(String.format("%dms passes since last build,now rebuild it", elapse / 1000));
			return true;
		}
	}

	/**
	 * 创建缩影文件
	 * 
	 * @param sql     sql语句
	 * @param colname 列名称
	 */
	public void createIndex(String sql, String colname) {
		try {
			final IndexWriterConfig iwc = new IndexWriterConfig(analyzer);// 索引书写器的配置
			iwc.setOpenMode(OpenMode.CREATE_OR_APPEND);
			final IndexWriter writer = new IndexWriter(this.getIndexHome(), iwc);
			dataApp.sqldframe(sql).stream().flatMap(e -> expand2docs(e, colname)) // 索引源
					.forEach(doc -> {
						try {
							writer.addDocument(doc);
						} catch (Exception e) {
							// do nothing
						} // try
					}); // forEach
			writer.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * 对SQL 语句的执行结果进行索引创建，以提高结果集二次查询的检索速度
	 * 
	 * @param sql     数据查询额语句
	 * @param colname 进行数据冗余的字段名称（比如 一行 甲醇的数据，转换成 {"jiachun","jc",甲醇},多一次冗余，多一点便捷！
	 */
	@SuppressWarnings("resource")
	public void indexOn(String sql, String colname) {
		try {
			if (isExpired()) {// 索引文件是否过期
				Arrays.asList(this.getIndexHome().listAll()).forEach(e -> {
					try {
						this.getIndexHome().deleteFile(e);
					} catch (IOException e1) {
						// do nothing
					}
				});
			} else if (this.getIndexHome().listAll().length > 0) {
				return;// 当前系统包含有数缩影文件否在立即创建索引文件
			} // if

			this.createIndex(sql, colname);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private DataApp dataApp = null;
	private int expireTime;// 索引文件的有效时长,超过该时间,时间单位秒，索引数据将给与重建！
}