package gbench.util.jdbc;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import gbench.util.jdbc.Jdbc.SQL_MODE;
import gbench.util.jdbc.function.ExceptionalFunction;
import gbench.util.jdbc.kvp.DFrame;
import gbench.util.jdbc.kvp.IRecord;
import gbench.util.jdbc.sql.SQL;
import gbench.util.jdbc.sql.Term;

/**
 * 一次事务性的会话：共享一个数据库连接，并且出现操作失败（sql操作)，将给予先前的动作回滚．<br>
 * IJdbcSession 将作为一个Monad来出现。它内部保存由会话过程的状态数据。D 类型的data <br>
 * 用来作为链式编程的 特有编写方法。<br>
 * 
 * IJdbcSession 专门定义了一套jdbc的操作函数。这些函数的操作不会关闭数据库连接。所以 <br>
 * 当需要进行一些列共享数据库连接的操作的时候，可以使用 带有IJdbcSession 的函数来进行操作 <br>
 * 比如Jdbc.withTransaction的系列函数。
 *
 * @author xuqinghua
 *
 * @param <T> transCode，即SessionId的类型．又叫事务编号
 * @param <D> Data，Session 中的数据类型。又叫Monad session 的状态数据。
 */
public interface IJdbcSession<T, D> extends IManagedStreams {

	/**
	 * 获取数据库连接
	 *
	 * @return 数据库连接
	 */
	Connection getConnection();

	/**
	 * 获得交易代码
	 *
	 * @return 返回一个UUID, 交易代码，用于标识一次会话过程．
	 */
	T transCode();

	/**
	 * 会话中的数据内容
	 *
	 * @return session中的数据类型。
	 */
	D getData();

	/**
	 * 会话中的数据内容
	 *
	 * @return session中的数据类型。
	 */
	D setData(final D data);

	/**
	 * 返回会话属性集合
	 *
	 * @return 绘画属性
	 */
	Map<Object, Object> getAttributes();

	/**
	 * 查询结果集合
	 *
	 * @param sql    prepared sql 语句
	 * @param params prepared sql 语句中占位符参数的值集合，位置从1开始
	 * @return 查询结果集合
	 * @throws SQLException
	 */
	Stream<IRecord> psql2recordS(final String sql, final Map<Integer, ?> params) throws SQLException;

	/**
	 * 更新数据:execute 与update 同属于与 update
	 *
	 * @param sql    prepared sql 语句
	 * @param params 占位符的对应值的Map,位置从1开始
	 * @return 更新结果集合函数有 generatedKeys 键值
	 * @throws SQLException
	 */
	List<IRecord> psql2update(final String sql, final Map<Integer, Object> params) throws SQLException;

	/**
	 * 获得SessionId, 默认采用transCode来实现．
	 *
	 * @return 返回一个UUID, 交易代码，用于标识一次会话过程．
	 */
	default T getSessionId() {
		return transCode();
	}

	/**
	 * 设置属性
	 * 
	 * @param key   属性的键值
	 * @param value 属性的值
	 * @return 自身的 IJdbcSession&lt;T,D&gt; 的实例，用以实现链式编程
	 */
	default IJdbcSession<T, D> setAttribute(final Object key, final Object value) {
		this.getAttributes().put(key, value);
		return this;
	}

	/**
	 * 设置返回值
	 * 
	 * @param value 属性的值
	 * @return 自身的 IJdbcSession&lt;T,D&gt; 的实例，用以实现链式编程
	 */
	default IJdbcSession<T, D> setResult(final Object value) {
		this.getAttributes().put("result", value);
		return this;
	}

	/**
	 * 获取result属性的值
	 * 
	 * @param <R> 值类型
	 * @return 自身的 IJdbcSession&lt;T,D&gt; 的实例，用以实现链式编程
	 */
	@SuppressWarnings("unchecked")
	default <R> R getResult() {
		return (R) this.getAttributes().get("result");
	}

	/**
	 * 设置属性
	 * 
	 * @param attributes 待设置的属性集合
	 * @return 自身的 IJdbcSession&lt;T,D&gt; 的实例，用以实现链式编程
	 */
	default IJdbcSession<T, D> setAttributes(final Map<Object, Object> attributes) {
		this.getAttributes().putAll(attributes);
		return this;
	}

	/**
	 * 清空属性集合
	 * 
	 * @return 自身的 IJdbcSession&lt;T,D&gt; 的实例，用以实现链式编程
	 */
	default IJdbcSession<T, D> clearAttributes() {
		this.getAttributes().clear();
		return this;
	}

	/**
	 * 设置属性
	 * 
	 * @param key 属性的键值，当key为class类型的时候，若依据键名检索的结果为null，则尝试进行findOne操作即从attributes的值中提取key类型类的对象
	 * @return 属性的值，如果不存在返回空
	 */
	default Object getAttribute(final Object key) {
		return Optional.ofNullable(this.getAttributes().get(key)).orElseGet(() -> {
			if (key instanceof Class<?> clazz) { // 尝试做findOne操作
				final var valOpt = this.getAttributes().values().stream()
						.filter(e -> clazz.isAssignableFrom(e.getClass())).findFirst(); // 值对象
				return valOpt.orElse(null);
			} else { //
				return null;
			}
		});
	}

	/**
	 * 状态迁移绑定<br>
	 * 把 mapper 绑定到当前的 IJdbcSession sess, 进而把sess的当前状态(D) 推进到一个新的状态(X)<br>
	 * 注意 :<br>
	 * IJdbcSession&lt;T,D&gt;。bind 的实现需要首先检测当前自身的数据状态<br>
	 * 对于无效的数据状态 getData()==null 则拒绝执行绑定传递函数:mapper,并返回 null<br>
	 * 对于 空值仍旧需要进行bind 的情况 请使用 mapper 函数 返回 Optional.empty() 来代替。
	 * 
	 * 借助于 monad 的 可以实现IJdbcSession的级联状态迁移. <br>
	 * 例如 :monad(d->x).bind(x->y).bind(y->z) ... 进而 实现 数据状态上的级联。
	 * 
	 * @param <X>    目标容器的参数类型
	 * @param mapper 状态迁移函数: d -> x, 表示从d转换成d1状态，2是to的简写
	 * @return 状态为X类型的数据容器:IJdbcSession&lt;T,X&gt;
	 * @throws SQLException
	 * @throws Exception
	 */
	default <X> IJdbcSession<T, X> bind(final ExceptionalFunction<D, X> mapper) throws SQLException, Exception {

		final var d = this.getData();// 提取monad中的数据
		return d == null ? null : this.fmap(d, mapper);// 这句是关键如果 当前数据为null 则不再做继续传递的操作。
	}

	/**
	 * 状态迁移绑定<br>
	 * 把 mapper 绑定到当前的 IJdbcSession sess, 进而把sess的当前状态(D) 推进到一个新的状态(X)<br>
	 * 注意 :<br>
	 * IJdbcSession&lt;T,D&gt;。bind 的实现需要首先检测当前自身的数据状态<br>
	 * 对于无效的数据状态 getData()==null 则拒绝执行绑定传递函数:mapper,并返回 null<br>
	 * 
	 * 借助于 monad 的 可以实现IJdbcSession的级联状态迁移. <br>
	 * 例如 :monad(d->x).bind(x->y).bind(y->z) ... 进而 实现 数据状态上的级联。
	 * 
	 * @param <X>    目标容器的参数类型
	 * @param mapper 状态迁移函数: d -> x, 表示从d转换成d1状态，2是to的简写
	 * @return 状态为X类型的数据容器:Optional&lt;IJdbcSession&lt;T,X&gt;&gt;
	 * @throws SQLException
	 * @throws Exception
	 */
	default <X> Optional<IJdbcSession<T, X>> bindOpt(final ExceptionalFunction<D, X> mapper)
			throws SQLException, Exception {
		return Optional.ofNullable(this.bind(mapper));
	}

	/**
	 * Monad fmap 状态单子 ( fmap 的别名函数 ,强调 IJdbcSession 将作为一个 状态单子出现) <br>
	 * 相当于 : fmap(this.getData(), d->x);
	 * 
	 * @param <X> 目标容器中的元素的类型。
	 * @param x   新的状态值
	 * @return 状态数据为X类型的IJdbcSession
	 * @throws SQLException
	 * @throws Exception
	 */
	default <X> IJdbcSession<T, X> monad(final X x) throws SQLException, Exception {

		return this.fmap(this.getData(), d -> x);
	}

	/**
	 * Monad fmap 状态单子 ( fmap 的别名函数 ,强调 IJdbcSession 将作为一个 状态单子出现) <br>
	 * 相当于 : fmap(this.getData(), mapper);
	 * 
	 * @param <X>    目标容器中的元素的类型。
	 * @param mapper d:当前数据状态->x:目标状态 的 状态迁移函数
	 * @return 状态数据为X类型的IJdbcSession
	 * @throws SQLException
	 * @throws Exception
	 */
	default <X> IJdbcSession<T, X> monad(final ExceptionalFunction<D, X> mapper) throws SQLException, Exception {

		return fmap(this.getData(), mapper);
	}

	/**
	 * Monad fmap 状态单子 ( fmap 的别名函数 ,强调 IJdbcSession 将作为一个 状态单子出现)
	 * 
	 * @param <X>    目标容器中的元素的类型。
	 * @param <S>    输入参数的类型:初始入参的类型:Start 开始点入口数据的准备
	 * @param start  初始数据 start
	 * @param mapper s->x 的状态迁移函数
	 * @return 状态数据为X类型的IJdbcSession
	 * @throws SQLException
	 * @throws Exception
	 */
	default <X, S> IJdbcSession<T, X> monad(final S start, final ExceptionalFunction<S, X> mapper)
			throws SQLException, Exception {

		return fmap(start, mapper);
	}

	/**
	 * Monad fmap 状态单子
	 * 
	 * @param <X>    目标容器中的元素的类型。
	 * @param <S>    输入参数的类型:初始入参的类型:Start 开始点入口数据的准备
	 * @param start  初始数据 start
	 * @param mapper s->x 的状态迁移函数
	 * @return 状态数据为X类型的IJdbcSession
	 * @throws SQLException
	 * @throws Exception
	 */
	default <X, S> IJdbcSession<T, X> fmap(final S start, final ExceptionalFunction<S, X> mapper)
			throws SQLException, Exception {

		// s2d 是对 new IJdbcSession<T, X>() 中的数据 data 字段的初始化。
		final var outer_data = mapper.apply(start);// 计算映射结果. 然后把这个d 设置到一个新的 IJdbcSession 容器之中.
		final var outer_attributes = new HashMap<>(this.getAttributes());// 会话属性
		final var outer_uuid = this.getSessionId();// 重用当前的 SessionId 以保证Session 共享

		// 创建一个新的 IJdbcSession <T,X>
		return new IJdbcSession<>() {// 这就是一个容器的类型的生成过程。注意D是元素类型。IJdbcSession 是容器类型。

			/**
			 * 
			 * new IJdbcSession<>(){} 的 psql2recordS 的方法实现 <br>
			 * 
			 * 对于短路的流，注意调用 stream.close() 来释放数据库连接, 或者 是 调用 jdbc.clear 来给与清空。<br>
			 * 
			 * 查询结果集合 <br>
			 */
			@Override
			public Stream<IRecord> psql2recordS(final String sqlpattern, final Map<Integer, ?> params)
					throws SQLException {

				final var stream = IJdbcSession.psql2recordS(this.getConnection(), sqlparse_unescape(sqlpattern),
						params, false, false);
				this.add(stream); // 加入托管流

				return stream;
			}

			/**
			 * 更新数据:execute 与update 同属于与 update <br>
			 */
			@Override
			public List<IRecord> psql2update(final String sqlpattern, final Map<Integer, Object> params)
					throws SQLException {

				final var pstmt = Jdbc.pstmt(this.getConnection(), SQL_MODE.UPDATE, sqlparse_unescape(sqlpattern),
						params);
				final var recs = Jdbc.readlines(pstmt.getGeneratedKeys(), true); // 获取生成主键信息，字段名称为：GENERATED_KEY
				pstmt.close();// 读取数据并关闭语句

				return recs;
			}

			// 创建session 会话
			@Override
			public Connection getConnection() {
				return IJdbcSession.this.getConnection();
			}

			@Override
			public Set<Stream<?>> getActiveStreams() {
				return managedStream.getActiveStreams();
			}

			@Override
			public T transCode() {
				return uuid;
			}// 交易编码的实现

			@Override
			public X getData() {
				return this.data;
			}// 会话中的数据

			@Override
			public X setData(X data) {
				return this.data = data;
			}// 会话中的数据

			@Override
			public Map<Object, Object> getAttributes() {
				return this.attributes;
			}// 返回会话属性

			/**
			 * sqlpattern 的解析 并 对解析后的语句进行 unescape <br>
			 * 
			 * @param sqlpattern sql 模板,可以使是一个shparp变量,比如 :#actIn 这样的语句标号
			 * @return 解析之后的sql
			 */
			private String sqlparse_unescape(final String sqlpattern) {
				return Jdbcs.format(this.sqlparse(sqlpattern, null));
			}

			private X data = outer_data; // 计算映射结果
			private final T uuid = outer_uuid;// 重用当前的 SessionId 以保证Session 共享
			private final Map<Object, Object> attributes = outer_attributes;// 会话属性,继承前会话属性数据
			final IManagedStreams managedStream = new ManagedStreams();
		};// IJdbcSession

	}// fmap

	/**
	 * 查询结果并给予归集<br>
	 * 执行sql语句查询出结果集合。不会尝试调用spp 进行sql解析 <br>
	 * u 结果 会被 保存到 session 的 result 的 属性 之中。
	 *
	 * @param sql sql 语句
	 * @return UDFrame 类型的结果
	 * @throws SQLException
	 */
	default DFrame sql2dframe(final String sql) throws SQLException {
		@SuppressWarnings("unchecked")
		final var collector = (Collector<IRecord, ?, DFrame>) this.getAttribute(Collector.class);
		return this.sql2u(sql, IRecord.REC(), collector);
	}

	/**
	 * 查询结果并给予归集<br>
	 * 执行sql语句查询出结果集合。不会尝试调用spp 进行sql解析 <br>
	 * u 结果 会被 保存到 session 的 result 的 属性 之中。
	 *
	 * @param sql    sql 语句
	 * @param params 语句参数
	 * @return DFrame 类型的结果
	 * @throws SQLException
	 */
	default DFrame sql2dframe(final String sql, final IRecord params) throws SQLException {
		return this.sql2u(sql, params);
	}

	/**
	 * 查询结果并给予归集<br>
	 * 执行sql语句查询出结果集合。不会尝试调用spp 进行sql解析 <br>
	 * u 结果 会被 保存到 session 的 result 的 属性 之中。
	 *
	 * @param sql    sql 语句
	 * @param params 语句参数,键,值间隔序列
	 * @return DFrame 类型的结果
	 * @throws SQLException
	 */
	default DFrame sql2dframe(final String sql, final Object... params) throws SQLException {
		return this.sql2u(sql, IRecord.REC(params));
	}

	/**
	 * 查询结果并给予归集<br>
	 * 执行sql语句查询出结果集合。不会尝试调用spp 进行sql解析 <br>
	 * u 结果 会被 保存到 session 的 result 的 属性 之中。
	 *
	 * @param <U>    归集器结果类型
	 * @param sql    sql 语句
	 * @param params 语句参数
	 * @return U 类型的结果
	 * @throws SQLException
	 */
	default <U> U sql2u(final String sql, final IRecord params) throws SQLException {
		@SuppressWarnings("unchecked")
		final var collector = (Collector<IRecord, ?, U>) this.getAttribute(Collector.class);
		return this.sql2u(sql, params, collector);
	}

	/**
	 * 查询结果并给予归集<br>
	 * 执行sql语句查询出结果集合。不会尝试调用spp 进行sql解析 <br>
	 * u 结果 会被 保存到 session 的 result 的 属性 之中。
	 *
	 * @param <U>       归集器结果类型
	 * @param sql       sql 语句
	 * @param collector 归集器 [rec]-&gt;u
	 * @return U 类型的结果
	 * @throws SQLException
	 */
	default <U> U sql2u(final String sql, final Collector<IRecord, ?, U> collector) throws SQLException {
		return this.sql2u(sql, IRecord.REC(), collector);
	}

	/**
	 * 查询结果并给予归集<br>
	 * 执行sql语句查询出结果集合。不会尝试调用spp 进行sql解析 <br>
	 * u 结果 会被 保存到 session 的 result 的 属性 之中。
	 *
	 * @param <U>       归集器结果类型
	 * @param sql       sql 语句, 调用 sql.format() 生成 查询文本
	 * @param collector 归集器 [rec]-&gt;u
	 * @return U 类型的结果
	 * @throws SQLException
	 */
	default <U> U sql2u(final SQL sql, final Collector<IRecord, ?, U> collector) throws SQLException {
		return this.sql2u(sql.format(), collector);
	}

	/**
	 * 查询结果并给予归集<br>
	 * 使用自身带有的spp(sqlpattern
	 * 的解释器,前提需要设置Class&lt;SqlPatternPreprocessor&gt;为键值的属性)执行sql语句查询出结果集合.<br>
	 * 比如：传入与sharp标号语句：让spp 从其语句库中检索。<br>
	 * final var recs = sess.sql2records("#getCytokines",REC("name",name)); <br>
	 * 或者是直接传入带有sharp变量编号(比如下图的“#name”)的语句让spp解析。<br>
	 * final var recs = sess.sql2records( <br>
	 * "MATCH (n)-[:Secrete]-&gt;(b) where b.name=#name RETURN n.name as host",<br>
	 * REC("name",name)); <br>
	 * 
	 * <br>
	 * 对于短路的流，注意调用 stream.close() 来释放数据库连接, 或者 是 调用 jdbc.clear 来给与清空。<br>
	 * u 结果 会被 保存到 session 的 result 的 属性 之中。
	 *
	 * @param <U>        归集器结果类型
	 * @param sqlpattern sql模板：可以是一个sharp语句标记：比如#getUser,用于从模板文件中提取脚本，也可以是函数有sharp变量的语句模板。
	 * @param params     sharp变量的形参与实际参数的对应关系
	 * @param collector  归集器 [rec]-&gt;u
	 * @return U 类型的结果
	 * @throws SQLException SQLException
	 */
	@SuppressWarnings("unchecked")
	default <U> U sql2u(final String sqlpattern, final IRecord params, final Collector<IRecord, ?, U> collector)
			throws SQLException {

		try {
			final var result = this.sql2recordS(sqlpattern, params).collect(collector);
			this.setAttribute("result", result); // 设置结果状态值
		} catch (Exception e) {
			this.setAttribute("result", error("sql2u", sqlpattern, params)); // 设置结果状态值
			throw e;
		}

		return (U) this.getAttribute("result");
	}

	/**
	 * 查询结果并给予归集<br>
	 * 执行sql语句查询出结果集合.<br>
	 * 比如：传入与sharp标号语句：让spp 从其语句库中检索。<br>
	 * final var recs = sess.sql2records("#getCytokines",REC("name",name),spp); <br>
	 * 或者是直接传入带有sharp变量编号(比如下图的“#name”)的语句让spp解析。<br>
	 * final var recs = sess.sql2records( <br>
	 * "MATCH (n)-[:Secrete]-&gt;(b) where b.name=#name RETURN n.name as host",<br>
	 * REC("name",name),spp); <br>
	 * 
	 * <br>
	 * 对于短路的流，注意调用 stream.close() 来释放数据库连接, 或者 是 调用 jdbc.clear 来给与清空。<br>
	 * u 结果 会被 保存到 session 的 result 的 属性 之中。
	 *
	 * @param <U>        归集器结果类型
	 * @param sqlpattern sql模板：可以是一个sharp语句标记：比如#getUser,用于从模板文件中提取脚本，也可以是函数有sharp变量的语句模板。
	 * @param params     sharp变量的形参与实际参数的对应关系
	 * @param spp        sqlpattern 的解释器
	 * @param collector  归集器 [rec]-&gt;u
	 * @return IRecord 类型的结果集合
	 * @throws SQLException
	 */
	default <U> U sql2u(final String sqlpattern, final IRecord params, final ISqlPatternPreprocessor spp,
			final Collector<IRecord, ?, U> collector) throws SQLException {
		return this.sql2recordS(spp.handle(null, params, sqlpattern, null)).collect(collector);
	}

	/**
	 * 执行sql语句查询出结果集合。不会尝试调用spp 进行sql解析
	 * 
	 * 注意 这是采用 sql2records 转化的 独立 流对象，不受 数据库 连接 影响。<br>
	 * 这是 sql2recordS 依赖于数据库连接 流的 重大区别，<br>
	 * 即 sql2recordS 只能在 withTranction 内使用 <br>
	 * 而 sql2records 可以 游离出 withTranction 在 外边 独立使用。
	 *
	 * @param sql sql 语句
	 * @return IRecord 类型的结果的流
	 * @throws SQLException
	 */
	default Stream<IRecord> sql2stream(final String sql) throws SQLException {
		return this.sql2recordS(sql);
	}

	/**
	 * 执行sql语句查询出结果集合。不会尝试调用spp 进行sql解析
	 * 
	 * 注意 这是采用 sql2records 转化的 独立 流对象，不受 数据库 连接 影响。<br>
	 * 这是 sql2recordS 依赖于数据库连接 流的 重大区别，<br>
	 * 即 sql2recordS 只能在 withTranction 内使用 <br>
	 * 而 sql2records 可以 游离出 withTranction 在 外边 独立使用。
	 *
	 * @param sql    sql 语句
	 * @param params sql 语句中占位符参数的值集合，位置从1开始
	 * @return IRecord 类型的结果集合
	 * @throws SQLException
	 */
	default Stream<IRecord> sql2stream(final String sql, final IRecord params) throws SQLException {
		return this.sql2recordS(sql, params);
	}

	/**
	 * 执行sql语句查询出结果集合。<br>
	 * 一般不会尝试调用spp 进行sql解析,但是 当sql为"#开头的namedsql的时候，会调用spp给予解析") <br>
	 * sql2records 的核心函数 <br>
	 * 
	 * <br>
	 * 对于短路的流，注意调用 stream.close() 来释放数据库连接, 或者 是 调用 jdbc.clear 来给与清空。<br>
	 *
	 * @param sql sql 语句
	 * @return IRecord 类型的结果集合
	 * @throws SQLException
	 */
	default List<IRecord> sql2records(final String sql) throws SQLException {
		return this.sql2recordS(sql).collect(Collectors.toList());
	}

	/**
	 * 执行sql语句查询出结果集合。<br>
	 * 一般不会尝试调用spp 进行sql解析,但是 当sql为"#开头的namedsql的时候，会调用spp给予解析") <br>
	 * sql2records 的核心函数 <br>
	 * 
	 * <br>
	 * 对于短路的流，注意调用 stream.close() 来释放数据库连接, 或者 是 调用 jdbc.clear 来给与清空。<br>
	 *
	 * @param sql sql 语句
	 * @return IRecord类型的流
	 * @throws SQLException
	 */
	default Stream<IRecord> sql2recordS(final String sql) throws SQLException {
		var _sql = sql.strip(); // 去除多余的首尾空格
		if (_sql.startsWith("#")) {
			_sql = this.sqlparse(_sql, IRecord.REC());
		}
		return this.psql2recordS(_sql, (Map<Integer, Object>) null);
	}

	/**
	 * sql2records Extend 增强版的sql2records <br>
	 * 使用内置的 SqlPatternPreprocessor(spp) 对 sql 进行预处理 比如：<br>
	 * jdbc.withTransaction(sess-&gt;{ <br>
	 * final var recs = sess.sql2records_ex("#getTables"); <br>
	 * },Jdbc.M(SqlPatternPreprocessor.class,spp)); <br>
	 * <p>
	 * 执行sql语句查询出结果集合 <br>
	 * 
	 * <br>
	 * 对于短路的流，注意调用 stream.close() 来释放数据库连接, 或者 是 调用 jdbc.clear 来给与清空。<br>
	 *
	 * @param sql sql 语句
	 * @return IRecord 类型的结果集合
	 * @throws SQLException
	 */
	default List<IRecord> sql2records_ex(final String sql) throws SQLException {
		return this.sql2records(sql, IRecord.REC());
	}

	/**
	 * 注意这里的 rec 是对SQL 中的参数补充，而不是对preparedStatement 中的参数。
	 *
	 * @param sql    sql 语句,调用 sql.string(params) 提取语句文本
	 * @param params sql 语句中占位符参数的值集合，位置从1开始
	 * @return IRecord 集合
	 * @throws SQLException
	 */
	default List<IRecord> sql2records(final SQL sql, final IRecord params) throws SQLException {
		return this.psql2records(sql.string(params), params);
	}

	/**
	 * 注意这里的 rec 是对SQL 中的参数补充，而不是对preparedStatement 中的参数。
	 * 
	 * <br>
	 * 对于短路的流，注意调用 stream.close() 来释放数据库连接, 或者 是 调用 jdbc.clear 来给与清空。<br>
	 *
	 * @param sql    sql 语句,调用 sql.format() 生成 查询文本
	 * @param params prepared sql 语句中占位符参数的值集合，位置从1开始
	 * @return IRecord 集合
	 * @throws SQLException
	 */
	default Stream<IRecord> psql2recordS(final SQL sql, final IRecord params) throws SQLException {
		return this.psql2recordS(sql.format(), params);
	}

	/**
	 * 查询结果集合
	 *
	 * @param sql 命名SQL语句,调用 sql.format() 生成 查询文本
	 * @return 查询结果集合
	 * @throws SQLException
	 */
	default List<IRecord> sql2records(final SQL sql) throws SQLException {
		return this.sql2records(sql.format());
	}

	/**
	 * 使用自身带有的spp(sqlpattern
	 * 的解释器,前提需要设置Class&lt;SqlPatternPreprocessor&gt;为键值的属性)执行sql语句查询出结果集合.<br>
	 * 比如：传入与sharp标号语句：让spp 从其语句库中检索。<br>
	 * final var recs = sess.sql2records("#getCytokines",REC("name",name)); <br>
	 * 或者是直接传入带有sharp变量编号(比如下图的“#name”)的语句让spp解析。<br>
	 * final var recs = sess.sql2records( <br>
	 * "MATCH (n)-[:Secrete]-&gt;(b) where b.name=#name RETURN n.name as host",<br>
	 * REC("name",name)); <br>
	 * 
	 * <br>
	 * 对于短路的流，注意调用 stream.close() 来释放数据库连接, 或者 是 调用 jdbc.clear 来给与清空。<br>
	 *
	 * @param sqlpattern sql模板：可以是一个sharp语句标记：比如#getUser,用于从模板文件中提取脚本，也可以是函数有sharp变量的语句模板。
	 * @param params     sharp变量的形参与实际参数的对应关系
	 * @return IRecord 类型的结果集合
	 * @throws SQLException
	 */
	default List<IRecord> sql2records(final String sqlpattern, final IRecord params) throws SQLException {

		return this.sql2recordS(sqlpattern, params).collect(Collectors.toList());
	}

	/**
	 * 使用自身带有的spp(sqlpattern
	 * 的解释器,前提需要设置Class&lt;SqlPatternPreprocessor&gt;为键值的属性)执行sql语句查询出结果集合.<br>
	 * 比如：传入与sharp标号语句：让spp 从其语句库中检索。<br>
	 * final var recs = sess.sql2records("#getCytokines",REC("name",name)); <br>
	 * 或者是直接传入带有sharp变量编号(比如下图的“#name”)的语句让spp解析。<br>
	 * final var recs = sess.sql2records( <br>
	 * "MATCH (n)-[:Secrete]-&gt;(b) where b.name=#name RETURN n.name as host",<br>
	 * REC("name",name)); <br>
	 * 
	 * <br>
	 * 对于短路的流，注意调用 stream.close() 来释放数据库连接, 或者 是 调用 jdbc.clear 来给与清空。<br>
	 *
	 * @param sqlpattern sql模板：可以是一个sharp语句标记：比如#getUser,用于从模板文件中提取脚本，也可以是函数有sharp变量的语句模板。
	 * @param params     sharp变量的形参与实际参数的对应关系
	 * @return IRecord类型的结果的流
	 * @throws SQLException
	 */
	default Stream<IRecord> sql2recordS(final String sqlpattern, final IRecord params) throws SQLException {

		return this.sql2recordS(this.sqlparse(sqlpattern, params));
	}

	/**
	 * 执行sql语句查询出结果集合.<br>
	 * 比如：传入与sharp标号语句：让spp 从其语句库中检索。<br>
	 * final var recs = sess.sql2records("#getCytokines",REC("name",name),spp); <br>
	 * 或者是直接传入带有sharp变量编号(比如下图的“#name”)的语句让spp解析。<br>
	 * final var recs = sess.sql2records( <br>
	 * "MATCH (n)-[:Secrete]-&gt;(b) where b.name=#name RETURN n.name as host",<br>
	 * REC("name",name),spp); <br>
	 * 
	 * <br>
	 * 如果没有配置spp,则使用 Term.FT做变量填充,用FT 填充变量的时候不需要
	 * 使用sharpPattern,当然也是可以用sharpPattern来标识变量的<br>
	 * 前提REC中的变量需要用#开头 <br>
	 * 比如:sess.sqlexecute("insert into t_num(num) values(#num)",REC("#num",e)); <br>
	 * 
	 * <br>
	 * 对于短路的流，注意调用 stream.close() 来释放数据库连接, 或者 是 调用 jdbc.clear 来给与清空。<br>
	 *
	 * @param sqlpattern sql模板：可以是一个sharp语句标记：比如#getUser,用于从模板文件中提取脚本，也可以是函数有sharp变量的语句模板。
	 * @param params     sharp变量的形参与实际参数的对应关系
	 * @param spp        sqlpattern 的解释器
	 * @return IRecord 类型的结果集合
	 * @throws SQLException SQLException
	 */
	default List<IRecord> sql2records(final String sqlpattern, final IRecord params, final ISqlPatternPreprocessor spp)
			throws SQLException {
		return this.sql2records(this.sqlparse(sqlpattern, params, spp));
	}

	/**
	 * 从结果集中提取一行数据。
	 *
	 * @param <U>         返回值类型
	 * @param sql         sql 语句
	 * @param targetClass 目标结果类型,当targetClass 为Boolean.class,boolean.class 的时候 返回结果
	 *                    为null的时候回被 视为 false
	 * @return 一条结果的数据集合，单行数据或者没有数据。
	 * @throws SQLException
	 */
	@SuppressWarnings("unchecked")
	default <U> U sql2get(final String sql, final Class<U> targetClass) throws SQLException {
		final var _targetClass = targetClass == null ? (Class<U>) Object.class : targetClass;
		return this.sql2maybe(sql, _targetClass)
				.orElse((Boolean.class.isAssignableFrom(_targetClass) || boolean.class.isAssignableFrom(_targetClass)) // Maybe
						// 没有值的时候
						? (U) (Object) false // targetClass 类型为布尔类型的时候 返回 false
						: null); // targetClass 为其他类型的时候 返回 null
	}

	/**
	 * 使用 spp 的 sql2get 使用自身带有的spp(sqlpattern
	 * 的解释器,前提需要设置Class&lt;SqlPatternPreprocessor&gt;为键值的属性)执行sql语句查询出结果集合.<br>
	 * 比如：传入与sharp标号语句：让spp 从其语句库中检索。<br>
	 * final var recs = sess.sql2records("#getCytokines",REC("name",name)); <br>
	 * 或者是直接传入带有sharp变量编号(比如下图的“#name”)的语句让spp解析。<br>
	 * final var recs = sess.sql2records( <br>
	 * "MATCH (n)-[:Secrete]-&gt;(b) where b.name=#name RETURN n.name as host",<br>
	 * REC("name",name)); <br>
	 * 
	 * <br>
	 * 如果没有配置spp,则使用 Term.FT做变量填充,用FT 填充变量的时候不需要
	 * 使用sharpPattern,当然也是可以用sharpPattern来标识变量的<br>
	 * 前提REC中的变量需要用#开头 <br>
	 * 比如:sess.sqlexecute("insert into t_num(num) values(#num)",REC("#num",e)); <br>
	 *
	 * @param <U>        返回值类型
	 * @param sqlpattern sql模板：可以是一个sharp语句标记：比如#getUser,用于从模板文件中提取脚本，也可以是函数有sharp变量的语句模板。
	 * @param params     sharp变量的形参与实际参数的对应关系
	 * @return U 类型的结果
	 * @throws SQLException
	 */
	default <U> U sql2get(final String sqlpattern, final IRecord params, final Class<U> targetClass)
			throws SQLException {
		final var sql = this.sqlparse(sqlpattern, params);
		return this.sql2maybe(sql, targetClass).orElse(null);
	}

	/**
	 * 从结果集中提取一行数据。
	 *
	 * @param <U>         返回值类型
	 * @param sql         sql 语句,调用sql.format()提取语句文本
	 * @param targetClass 目标结果类型,当targetClass 为Boolean.class,boolean.class 的时候 返回结果
	 *                    为null的时候回被 视为 false
	 * @return 一条结果的数据集合，单行数据或者没有数据。
	 * @throws SQLException
	 */
	default <U> U sql2get(final SQL sql, final Class<U> targetClass) throws SQLException {
		return sql2get(sql.format(), targetClass);
	}

	/**
	 * 从结果集中提取一行数据。
	 *
	 * @param sql sql 语句
	 * @return IRecord
	 * @throws SQLException
	 */
	default IRecord sql2get(final String sql) throws SQLException {
		return this.sql2maybe(sql, IRecord.class).orElse(null);
	}

	/**
	 * 从结果集中提取一行数据。
	 *
	 * @param sql sql 语句
	 * @return 一条结果的数据集合，单行数据或者没有数据。
	 * @throws SQLException
	 */
	default Optional<IRecord> sql2maybe(final String sql) throws SQLException {
		return this.sql2recordS(sql).findFirst();
	}

	/**
	 * 从结果集中提取一行数据。
	 *
	 * @param sql sql语句, 调用 sql.format() 来提取文本内容
	 * @return 一条结果的数据集合，单行数据或者没有数据。
	 * @throws SQLException
	 */
	default Optional<IRecord> sql2maybe(final SQL sql) throws SQLException {
		return this.sql2maybe(sql.format());
	}

	/**
	 * 从结果集中提取一行数据。
	 *
	 * @param sqlpattern sql 语句模版
	 * @param params     被替换数据的 键值对
	 * @return 单行数据或者没有数据。
	 * @throws SQLException
	 */
	default Optional<IRecord> sql2maybe(final String sqlpattern, final IRecord params) throws SQLException {
		return this.sql2maybe(this.sqlparse(sqlpattern, params));
	}

	/**
	 * 从结果集中提取一行数据。
	 *
	 * @param <U>         返回值类型
	 * @param sqlpattern  sql 语句模版
	 * @param params      被替换数据的 键值对
	 * @param targetClass U类型类
	 * @return U类型的结果，单行数据或者没有数据。
	 * @throws SQLException
	 */
	default <U> Optional<U> sql2maybe(final String sqlpattern, final IRecord params, final Class<U> targetClass)
			throws SQLException {
		final var sql = this.sqlparse(sqlpattern, params);
		return this.sql2maybe(sql, targetClass);
	}

	/**
	 * 从结果集中提取一行数据。
	 * 
	 * @param <U>         返回值类型
	 * @param sql         sql 语句
	 * @param targetClass U类型类
	 * @return U类型的结果，单行数据或者没有数据。
	 * @throws SQLException
	 */
	default <U> Optional<U> sql2maybe(final String sql, final Class<U> targetClass) throws SQLException {

		final var stream = this.sql2recordS(sql);
		final var ret = stream.findFirst().map(e -> e.mutate(targetClass));
		this.clear(stream);

		return ret;
	}

	/**
	 * 从结果集中提取一行数据。<br>
	 *
	 * @param <U> 返回值类型
	 * @param sql sql 语句，会调用sql.format()生成语句内容
	 * @return 一条结果的数据集合，单行数据或者没有数据。
	 * @throws SQLException
	 */
	default <U> Optional<U> sql2maybe(final SQL sql, final Class<U> targetClass) throws SQLException {

		final var stream = this.sql2recordS(sql.format());
		final var ret = stream.findFirst().map(e -> e.mutate(targetClass));
		this.clear(stream);

		return ret;
	}

	/**
	 * 执行sql语句不产生结果集合.<br>
	 *
	 * @param sql SQL 语句
	 * @return 结果是否为一个resultset
	 * @throws SQLException
	 */
	default boolean sqlexecute(final String sql) throws SQLException {
		var _sql = sql.strip(); // 去除多余的首尾空格
		if (_sql.startsWith("#")) {
			_sql = this.sqlparse(_sql, IRecord.REC());
		}
		return psqlexecute(_sql, (Object[]) null); // 直接执行SQL语句
	}

	/**
	 * 执行sql语句不产生结果集合.<br>
	 *
	 * @param sql SQL 语句,会调用 sql.toString()来提取文本语句内容，即 尝试
	 *            返回sqltpl,若sqltpl为空则返回insert()语句
	 * @return 结果是否为一个resultset
	 * @throws SQLException
	 */
	default boolean sqlexecute(final SQL sql) throws SQLException {
		final var sqline = sql.format();
		return sqlexecute(sqline == null ? sql.toString() : sqline);
	}

	/**
	 * 使用自身带有的spp(sqlpattern
	 * 的解释器,前提需要设置Class&lt;SqlPatternPreprocessor&gt;为键值的属性)执行sql语句不产生结果集合.<br>
	 * 比如：传入与sharp标号语句：让spp 从其语句库中检索。<br>
	 * final var recs = sess.sql2records("#getCytokines",REC("name",name)); <br>
	 * 或者是直接传入带有sharp变量编号(比如下图的“#name”)的语句让spp解析。<br>
	 * final var recs = sess.sql2records( <br>
	 * "MATCH (n)-[:Secrete]-&gt;(b) where b.name=#name RETURN n.name as host",<br>
	 * REC("name",name)); <br>
	 * <br>
	 * 如果没有配置spp,则使用 Term.FT做变量填充,用FT 填充变量的时候不需要
	 * 使用sharpPattern,当然也是可以用sharpPattern来标识变量的<br>
	 * 前提REC中的变量需要用#开头 <br>
	 * 比如:sess.sqlexecute("insert into t_num(num) values(#num)",REC("#num",e)); <br>
	 *
	 * @param sqlpattern sql模板：可以是一个sharp语句标记：比如#getUser,用于从模板文件中提取脚本，也可以是函数有sharp变量的语句模板。
	 * @param params     sharp变量的形参与实际参数的对应关系
	 * @return IRecord 类型的结果集合
	 * @throws SQLException
	 */
	default boolean sql2execute(final String sqlpattern, final IRecord params) throws SQLException {
		final var spp = (ISqlPatternPreprocessor) this.getAttribute(ISqlPatternPreprocessor.class);
		final var _sql = this.sqlparse(sqlpattern, params);// 解释sql
		final var sql = spp != null && spp.name().equals("namedsql_processor_escape_brace") //
				? Jdbcs.format(_sql) // 把采用了 namedsql_processor_escape_brace 转义的 字符给转义回来
				: _sql;
		return this.sqlexecute(sql);
	}

	/**
	 * 执行sql语句不产生结果集合.<br>
	 * 比如：传入与sharp标号语句：让spp 从其语句库中检索。<br>
	 * final var recs = sess.sql2records("#getCytokines",REC("name",name),spp); <br>
	 * 或者是直接传入带有sharp变量编号(比如下图的“#name”)的语句让spp解析。<br>
	 * final var recs = sess.sql2records( <br>
	 * "MATCH (n)-[:Secrete]-&gt;(b) where b.name=#name RETURN n.name as host",<br>
	 * REC("name",name),spp); <br>
	 *
	 * @param sqlpattern sql模板：可以是一个sharp语句标记：比如#getUser,用于从模板文件中提取脚本，也可以是函数有sharp变量的语句模板。
	 * @param params     sharp变量的形参与实际参数的对应关系
	 * @param spp        sqlpattern 的解释器：把sqlpattern转换成sql语句。
	 * @return 执行的sql语句是否返回了一个结果集合
	 * @throws SQLException
	 */
	default boolean sqlexecute(final String sqlpattern, final IRecord params, final ISqlPatternPreprocessor spp)
			throws SQLException {
		return this.sqlexecute(this.sqlparse(sqlpattern, params, spp));
	}

	/**
	 * sql语句执行等同于 sql2update
	 *
	 * @param sql SQL语句
	 * @return IRecord List
	 * @throws SQLException
	 */
	default List<IRecord> sql2execute(final String sql) throws SQLException {
		return sqlupdate(sql);
	}

	/**
	 * sql语句执行
	 *
	 * @param sql sql 语句
	 * @return 带有 生成的主键 GENERATED_KEY的字段记录
	 * @throws SQLException
	 */
	default Optional<IRecord> sql2execute2maybe(final String sql) throws SQLException {
		return this.sql2update(sql).stream().findFirst();
	}

	/**
	 * sql语句执行
	 *
	 * @param sql sql 语句
	 * @param <U> 目录结果的类型
	 * @return 带有：生成的主键 GENERATED_KEY的字段记录
	 * @throws SQLException
	 */
	default <U> Optional<U> sql2execute2maybe(final String sql, final Class<U> targetClass) throws SQLException {
		return this.sql2update(sql).stream().findFirst().map(e -> IRecord.rec2obj(e, targetClass));
	}

	/**
	 * sql语句执行
	 *
	 * @param sql，sql 语句模板，参数采用 #xxx 字符串类型, ##xxx 数值类型,或是 ${xxx} 类型 来进行占位,其中
	 * @param rec     sql 语句模板:命名参数集合，key->value的键值对
	 * @return 带有：生成的主键 GENERATED_KEY的字段记录
	 * @throws SQLException
	 */
	default Optional<IRecord> sql2execute2maybe(final SQL sql, final IRecord rec) throws SQLException {
		return this.sql2update(sql.string(rec)).stream().findFirst();
	}

	/**
	 * sql语句执行
	 *
	 * @param sql insert 或 update 语句
	 * @return 生成的主键 GENERATED_KEY,如果没有生成 GENERATED_KEY比如update语句，返回空值
	 * @throws SQLException
	 */
	default Integer sql2execute2int(final String sql) throws SQLException {
		final Number num = this.sql2execute2num(sql);
		return num == null ? null : num.intValue();
	}

	/**
	 * sql语句执行
	 *
	 * @param sql insert 或 update 语句
	 * @return 生成的主键 GENERATED_KEY，如果没有生成 GENERATED_KEY比如update语句，返回空值 对于非mysql
	 *         数据库比如不会生成GENERATED_KEY,但杀局插入成功返回-1
	 * @throws SQLException
	 */
	default Number sql2execute2num(final String sql) throws SQLException {
		final Optional<IRecord> maybe = this.sql2execute2maybe(sql);
		final Number num = maybe.isPresent() ? maybe.map(e -> {
			Number gk = null;
			// GENERATED_KEY:MySQL, id : H2
			for (final var k : "GENERATED_KEY,id".split(",")) { // 获取主键名称
				if ((gk = e.num(k)) != null) { // 尝试提取键名内容
					break;
				} // if
			} // for
			if (gk == null) { // 尝试获取第一个
				gk = e.num(0); // 读取第一个字段内容
			} // if
			return gk == null ? -1 : gk;
		}).get() : null;

		return num;
	}

	/**
	 * sql语句执行
	 *
	 * @param sql sql 语句模板，参数采用 #xxx 字符串类型, ##xxx 数值类型,或是 ${xxx} 类型 来进行占位,其中
	 * @param rec sql 语句模板:命名参数集合，key->value的键值对
	 * @return 生成的主键 GENERATED_KEY
	 * @throws SQLException
	 */
	default int sql2execute2int(final SQL sql, final IRecord rec) throws SQLException {
		return sql2execute2num(sql, rec).intValue();
	}

	/**
	 * sql语句执行
	 *
	 * @param sql sql 语句模板,会调用 sql.format()来提取文本语句内容
	 * @return 生成的主键 GENERATED_KEY
	 * @throws SQLException
	 */
	default int sql2execute2int(final SQL sql) throws SQLException {
		return sql2execute2num(sql).intValue();
	}

	/**
	 * sql语句执行
	 *
	 * @param sql sql 语句模板，参数采用 #xxx 字符串类型, ##xxx 数值类型,或是 ${xxx} 类型 来进行占位
	 * @param rec sql 语句模板:命名参数集合，key->value的键值对
	 * @return 生成的主键 GENERATED_KEY
	 * @throws SQLException
	 */
	default Number sql2execute2num(final SQL sql, final IRecord rec) throws SQLException {
		return sql2execute2num(sql.string(rec));
	}

	/**
	 * sql语句执行
	 *
	 * @param sql sql语句对象,会调用 sql.format()来提取文本语句内容
	 * @return 生成的主键 GENERATED_KEY
	 * @throws SQLException
	 */
	default Number sql2execute2num(final SQL sql) throws SQLException {
		return sql2execute2num(sql.format());
	}

	/**
	 * sql语句执行 这是对sqlupdate的别名
	 *
	 * @param sql sql 语句
	 * @return 查询结果集合
	 * @throws SQLException
	 */
	default List<IRecord> sql2update(final String sql) throws SQLException {
		return this.sqlupdate(sql);
	}

	/**
	 * 更新数据:execute 与update 同属于与 update
	 *
	 * @param sql SQL语句 这里是采用 preparedStatement 来执行。
	 * @return sql 语句的执行记过
	 * @throws SQLException
	 */
	default List<IRecord> sqlupdate(final String sql) throws SQLException {
		return psql2update(sql, (Map<Integer, Object>) null);
	}

	/**
	 * 执行sql语句更新
	 *
	 * @param sql sql 语句
	 * @param rec 语句参数
	 * @return 返回结果列表
	 * @throws SQLException
	 */
	default List<IRecord> sqlupdate(final SQL sql, final IRecord rec) throws SQLException {
		return this.sqlupdate(sql.string(rec));
	}

	/**
	 * 执行sql语句更新
	 *
	 * @param sql sql 语句,会调用 sql.format()来提取文本语句内容
	 * @return 返回结果列表
	 * @throws SQLException
	 */
	default List<IRecord> sqlupdate(final SQL sql) throws SQLException {
		return this.sqlupdate(sql, sql.getSqlCtxAsOneRecord());
	}

	/**
	 * 批量執行sql语句集合
	 *
	 * @param sqls sql 语句集合
	 * @throws SQLException
	 */
	default List<List<IRecord>> sql2batch(final List<String> sqls) throws SQLException {
		if (sqls == null)
			return null;
		final List<List<IRecord>> rr = new LinkedList<>();// 语句链表
		for (String sql : sqls) {
			if (sql == null || sql.length() <= 0 || sql.matches("^[\\s]*$"))
				continue;
			rr.add(sql2update(sql));
		} // for
		return rr;
	}

	/**
	 * 批量执行目前只能够执行 插入更新语句，而不能自动判断出是 QUERY 还是 UPDATE
	 *
	 * @param sqls 需要批量执行的语句集合
	 * @return 批量执行的结果
	 * @throws SQLException
	 */
	default List<List<IRecord>> sql2batch(final String... sqls) throws SQLException {
		return sql2batch(Arrays.asList(sqls));
	}

	/**
	 * sql语句的模版参数填充 <br>
	 * 如果没有配置spp,则使用 Term.FT做变量填充,用FT 填充变量的时候不需要
	 * 使用sharpPattern,当然也是可以用sharpPattern来标识变量的<br>
	 * 前提REC中的变量需要用#开头 <br>
	 * 比如:sess.sqlexecute("insert into t_num(num) values(#num)",REC("#num",e)); <br>
	 * 
	 * @param sqlpattern sql模版,占位符用#标记,#name表示字符串类项目即值被引号括起来,##name,表示数值项目,即值不会被引号括起来引号
	 * @param params     sqlpattern的模版参数
	 * @return 使用params填充 sqlpattern 的字符串
	 */
	default String sqlparse(final String sqlpattern, final IRecord params) {
		final var spp = (ISqlPatternPreprocessor) this.getAttribute(ISqlPatternPreprocessor.class);
		return this.sqlparse(sqlpattern, params, spp);
	}

	/**
	 * sql语句的模版参数填充 <br>
	 * 如果没有配置spp,则使用 Term.FT做变量填充,用FT 填充变量的时候不需要
	 * 使用sharpPattern,当然也是可以用sharpPattern来标识变量的<br>
	 * 前提REC中的变量需要用#开头 <br>
	 * 比如:sess.sqlexecute("insert into t_num(num) values(#num)",REC("#num",e)); <br>
	 * 
	 * @param sqlpattern sql模版,占位符用#标记,#name表示字符串类项目即值被引号括起来,##name,表示数值项目,即值不会被引号括起来引号
	 * @param params     sqlpattern的模版参数
	 * @param spp        SqlPatternPreprocesso模版参数 模版的预处理参数
	 * @return 使用params填充 sqlpattern 的字符串
	 */
	default String sqlparse(final String sqlpattern, final IRecord params, final ISqlPatternPreprocessor spp) {
		if (spp == null) {
			return Term.FT(sqlpattern, params); // 默认使用Term.FT来进行填充
		} else {
			var _sqlpattern = sqlpattern;// sql语句的模式
			if (params != null && spp != null && "namedsql_processor_escape_brace".equals(spp.name())) { // 手动完成参数
				// _sqlpattern
				// 的预处理
				for (int i = 0; i < params.size(); i++) {
					final var v = params.get(i);
					if (v instanceof String)
						params.set(i, Jdbcs.format_escape((String) v));
				} // for
				_sqlpattern = Jdbcs.format_escape(sqlpattern); // 对 SQL模版进行预处理
			} // if
			final var sql = spp.handle(null, params, _sqlpattern, null); // 参数填充,调用spp
			// 进行参数填充的时候需要对_sqlpattern预处理
			return sql;
		}
	}

	/**
	 * 查询结果并给予归集<br>
	 * 注意这里的 rec 是对SQL 中的参数补充，而不是对preparedStatement 中的参数。
	 * 
	 * <br>
	 * 对于短路的流，注意调用 stream.close() 来释放数据库连接, 或者 是 调用 jdbc.clear 来给与清空。<br>
	 * 
	 * @param <U>       归集器结果类型
	 * @param sql       sql 语句,调用 sql.format() 生成 查询文本
	 * @param params    prepared sql 语句中占位符参数的值集合，位置从1开始
	 * @param collector 归集器 [rec]-&gt;u
	 * @return U 类型的结果
	 * @throws SQLException
	 */
	default <U> U psql2u(final SQL sql, final IRecord params, final Collector<IRecord, ?, U> collector)
			throws SQLException {

		return this.psql2recordS(sql, params).collect(collector);
	}

	/**
	 * 查询结果并给予归集<br>
	 * 使用自身带有的spp(sqlpattern
	 * 的解释器,前提需要设置Class&lt;SqlPatternPreprocessor&gt;为键值的属性)执行sql语句查询出结果集合.<br>
	 * 比如：传入与sharp标号语句：让spp 从其语句库中检索。<br>
	 * final var recs = sess.sql2records("#getCytokines",REC("name",name)); <br>
	 * 或者是直接传入带有sharp变量编号(比如下图的“#name”)的语句让spp解析。<br>
	 * final var recs = sess.sql2records( <br>
	 * "MATCH (n)-[:Secrete]-&gt;(b) where b.name=#name RETURN n.name as host",<br>
	 * REC("name",name)); <br>
	 * 
	 * <br>
	 * 对于短路的流，注意调用 stream.close() 来释放数据库连接, 或者 是 调用 jdbc.clear 来给与清空。<br>
	 *
	 * @param <U>        归集器结果类型
	 * @param sqlpattern sql模板：可以是一个sharp语句标记：比如#getUser,用于从模板文件中提取脚本，也可以是函数有sharp变量的语句模板。
	 * @param params     prepared sql 语句中占位符参数的值集合，位置从1开始
	 * @param collector  归集器 [rec]-&gt;u
	 * @return U 类型的结果
	 * @throws SQLException SQLException
	 */
	default <U> U psql2u(final String sqlpattern, final Map<Integer, ?> params,
			final Collector<IRecord, ?, U> collector) throws SQLException {

		return this.psql2recordS(sqlpattern, params).collect(collector);
	}

	/**
	 * 查询结果集合
	 *
	 * @param <U>       归集器结果类型
	 * @param sql       prepared sql 语句
	 * @param params    prepared sql 语句中占位符参数的值集合，位置从1开始
	 * @param collector 归集器 [rec]-&gt;u
	 * @return 查询结果集合
	 * @throws SQLException
	 */
	default <U> U psql2u(final String sql, final IRecord params, final Collector<IRecord, ?, U> collector)
			throws SQLException {

		final var stream = this.psql2recordS(sql, params);
		final var ret = stream.collect(collector);
		this.clear(stream);

		return ret;
	}

	/**
	 * 查询结果集合
	 *
	 * @param <X>       参数类型
	 * @param <U>       结果类型
	 * @param sql       prepared sql 语句
	 * @param params    prepared sql 语句中占位符参数的值序列
	 * @param collector 归集器 [rec]-&gt;u
	 * @return 查询结果集合
	 * @throws SQLException
	 */
	default <X, U> List<IRecord> psql2u(final String sql, final List<X> params,
			final Collector<IRecord, ?, U> collector) throws SQLException {

		return psql2records(sql, params == null ? new Object[] {} : params.toArray());
	}

	/**
	 * 查询结果集合
	 *
	 * @param <U>       结果类型
	 * @param sql       prepared sql 数据操作语句
	 * @param params    prepared sql 语句中占位符参数的值数组
	 * @param collector 归集器 [rec]-&gt;u
	 * @return 查询结果集合
	 * @throws SQLException
	 */
	default <U> U psql2u(final String sql, final Object[] params, final Collector<IRecord, ?, U> collector)
			throws SQLException {

		final var stream = psql2recordS(sql, params);
		final var ret = stream.collect(collector);
		this.clear(stream);

		return ret;
	}

	/**
	 * 查询结果集合
	 *
	 * @param sql prepared sql 语句
	 * @return 查询结果集合
	 * @throws SQLException
	 */
	default Stream<IRecord> psql2recordS(final String sql) throws SQLException {

		return this.psql2recordS(sql, (Map<Integer, Object>) null);
	}

	/**
	 * 查询结果集合
	 * 
	 * <br>
	 * 对于短路的流，注意调用 stream.close() 来释放数据库连接, 或者 是 调用 jdbc.clear 来给与清空。<br>
	 *
	 * @param sql    prepared sql 语句
	 * @param params prepared sql 语句中占位符参数的值集合，位置从1开始
	 * @return 查询结果集合
	 * @throws SQLException
	 */
	default Stream<IRecord> psql2recordS(final String sql, IRecord params) throws SQLException {

		final Map<Integer, Object> _params = params.aoks(e -> (Integer) IRecord.obj2num.apply(e).intValue());
		return this.psql2recordS(sql, _params);
	}

	/**
	 * 查询结果集合
	 *
	 * @param sql    prepared sql 语句
	 * @param params prepared sql 语句中占位符参数的值集合，位置从1开始
	 * @return 查询结果集合, IRecord的流 [rec]
	 * @throws SQLException
	 */
	default List<IRecord> psql2records(final String sql, final Map<Integer, Object> params) throws SQLException {

		return this.psql2recordS(sql, params).collect(Collectors.toList());
	}

	/**
	 * 查询结果集合
	 *
	 * @param sql    prepared sql 语句
	 * @param params prepared sql 语句中占位符参数的值集合，位置从1开始
	 * @return 查询结果集合，IRecord的列表 [rec]
	 * @throws SQLException
	 */
	default List<IRecord> psql2records(final String sql, final IRecord params) throws SQLException {

		final var mm = new HashMap<Integer, Object>();
		params.filter(kvp -> kvp._1().matches("\\+?\\d+")) // 过滤掉非数字key
				.foreach((k, v) -> {
					try {
						final var i = Integer.parseInt(k);
						mm.put(i, v);
					} catch (Exception e) {
						e.printStackTrace();
					} // try
				});// foreach

		return psql2records(sql, mm);
	}

	/**
	 * 查询结果集合
	 *
	 * @param <U>    参数类型
	 * @param sql    prepared sql 语句
	 * @param params prepared sql 语句中占位符参数的值集合序列
	 * @return 查询结果集合，IRecord的流 [rec]
	 * @throws SQLException
	 */
	default <U> List<IRecord> psql2records(final String sql, final List<U> params) throws SQLException {

		return psql2records(sql, params == null ? new Object[] {} : params.toArray());
	}

	/**
	 * 查询结果集合
	 *
	 * @param sql    prepared sql 语句
	 * @param params prepared sql 语句中占位符参数的值数组
	 * @return 查询结果集合，IRecord的流 [rec]
	 * @throws SQLException
	 */
	default List<IRecord> psql2records(final String sql, final Object[] params) throws SQLException {

		final Map<Integer, Object> _params = new LinkedHashMap<>();
		if (_params != null) {
			for (int i = 0; i < params.length; i++) {
				_params.put(i + 1, params[i]);// pareparedstatement 占位符 从1开始
			} // for
		} // if

		return psql2records(sql, _params);
	}

	/**
	 * 查询结果集合
	 *
	 * @param sql    prepared sql 语句
	 * @param params prepared sql 语句中占位符参数的值数组
	 * @return 查询结果集合 IRecord 的 流
	 * @throws SQLException
	 */
	default Stream<IRecord> psql2recordS(final String sql, final Object[] params) throws SQLException {

		final Map<Integer, Object> _params = new LinkedHashMap<>();
		if (params != null) {
			for (int i = 0; i < params.length; i++) {
				_params.put(i + 1, params[i]);// pareparedstatement 占位符 从1开始
			} // for
		} // if

		return psql2recordS(sql, _params);
	}

	/**
	 * 查询结果集合
	 *
	 * @param <U>         返回值的类型
	 * @param sql         prepared sql 语句
	 * @param params      prepared sql 语句中占位符参数的值集合，位置从1开始
	 * @param targetClass 结果的目标雷西给
	 * @return U类型的对象
	 * @throws SQLException
	 */
	default <U> U psql2get(final String sql, final IRecord params, final Class<U> targetClass) throws SQLException {

		return psql2maybe(sql, params).map(e -> IRecord.rec2obj(e, targetClass)).get();
	}

	/**
	 * 从结果集中提取一行数据。
	 *
	 * @param sql    sql语句模版，调用 sql.format() 生成 查询文本
	 * @param params prepared sql 语句中占位符参数的值集合，位置从1开始
	 * @return 单行数据或者没有数据。
	 * @throws SQLException
	 */
	default Optional<IRecord> psql2maybe(final SQL sql, final IRecord params) throws SQLException {

		return this.psql2maybe(sql.format(), params);
	}

	/**
	 * 查询结果集合
	 *
	 * @param sql    prepared sql 语句
	 * @param params prepared sql 语句中占位符参数的值集合，位置从1开始
	 * @return 查询结果Optional
	 * @throws SQLException
	 */
	default Optional<IRecord> psql2maybe(final String sql, final IRecord params) throws SQLException {

		return psql2records(sql, params).stream().findFirst();
	}

	/**
	 * 查询结果集合
	 *
	 * @param sql prepared sql 语句
	 * @param oo  prepared sql 语句中占位参数的值数组
	 * @return 查询结果集：Optional
	 * @throws SQLException
	 */
	default Optional<IRecord> psql2maybe(final String sql, final Object[] oo) throws SQLException {

		return psql2records(sql, oo).stream().findFirst();
	}

	/**
	 * 查询结果集合
	 *
	 * @param <U>         返回值的类型
	 * @param sql         prepared sql 语句
	 * @param params      prepared sql 语句中占位参数的值数组
	 * @param targetClass 结果的目标雷西给
	 * @return U 类型的对象
	 * @throws SQLException
	 */
	default <U> U psql2get(final String sql, final Object[] params, Class<U> targetClass) throws SQLException {

		return psql2maybe(sql, params).map(e -> IRecord.rec2obj(e, targetClass)).get();
	}

	/**
	 * 执行SQL语句（直接使用）
	 *
	 * @param sql    prepared sql语句
	 * @param params prepared sql 语句中的参数的值数组
	 * @return 结果是否为一个resultset
	 * @throws SQLException
	 */
	default boolean psqlexecute(final String sql, final Object[] params) throws SQLException {

		final Map<Integer, Object> _params = new LinkedHashMap<>();
		if (params != null) {// 把数据参数调制成 位置参数Map
			for (int i = 0; i < params.length; i++) {
				_params.put(i + 1, params[i]);// pareparedstatement 占位符 从1开始
			} // for
		} // if oo!=null

		final var pstmt = Jdbc.pstmt(this.getConnection(), SQL_MODE.UPDATE, sql, _params);
		try {
			final var b = pstmt.execute();
			pstmt.close();// 关闭语句
			return b;
		} catch (Exception e) {
			System.err.println("error sql:" + sql);
			this.setAttribute("error_sql", sql);
			throw e;
		}
	}

	/**
	 * 更新数据:execute 与update 同属于与 update
	 *
	 * @param sql    sql 语句
	 * @param params 占位符的对应值列表
	 * @return 更新结果集合函数有 generatedKeys 键值
	 * @throws SQLException
	 */
	default <U> List<IRecord> psql2update(final String sql, final List<U> params) throws SQLException {

		return this.psql2update(sql, params == null ? new Object[] {} : params.toArray());
	}

	/**
	 * 更新数据:execute 与update 同属于与 update
	 *
	 * @param sql    prepared sql 语句
	 * @param params prepared sql 语句中的参数的值数组
	 * @return 更新结果集合函数有 generatedKeys 键值
	 * @throws SQLException
	 */
	default List<IRecord> psql2update(final String sql, final Object[] params) throws SQLException {

		final Map<Integer, Object> _params = new LinkedHashMap<>();
		if (params != null) {
			for (int i = 0; i < params.length; i++) {
				_params.put(i + 1, params[i]);// pareparedstatement 占位符 从1开始
			} // for
		} // if

		return this.psql2update(sql, _params);
	}

	/**
	 * 更新数据:execute 与update 同属于与 update
	 *
	 * @param sql    sql 语句
	 * @param params sql语句中占位参数的值集合，位置参数从1开始
	 * @return 更新结果集合函数有 generatedKeys 键值
	 * @throws SQLException
	 */
	default List<IRecord> psql2update(final String sql, final IRecord params) throws SQLException {

		final var _params = params.toMap2(Integer::parseInt);
		return this.psql2update(sql, _params);
	}

	/**
	 * Function Table Get <br>
	 * 字段内容提取值:返回单条记录 <br>
	 * 其实就是这行一条简单 sql 查询：根据主键获取指定的数据记录 <br>
	 * MFT("select * from {0} where {1}={2}",table, x,idfld)) <br>
	 *
	 * @param <X>   源类型
	 * @param table 提取表名
	 * @param idfld id字段名
	 * @return 字段值的提取函数
	 */
	default <X> Function<X, IRecord> ftblget(final String table, final String idfld) {
		return FTBLGET(this, table, idfld);
	}

	/**
	 * Function Table Get <br>
	 * 字段内容提取值:返回多条记录 <br>
	 * 其实就是这行一条简单 sql 查询：根据主键获取指定的数据记录 <br>
	 * MFT("select * from {0} where {1}={2}",table, x,idfld)) <br>
	 *
	 * @param <X>   源类型
	 * @param table 提取表名
	 * @param idfld id字段名
	 * @return 字段值的提取函数
	 */
	default <X> Function<X, List<IRecord>> ftblgets(final String table, final String idfld) {
		return FTBLGETS(this, table, idfld);
	}

	/**
	 * 判断数据库表是否存在 <br>
	 * <p>
	 * 对于 返回数据库所有表 的异常情况的处理 <br>
	 * mysql8.0的驱动，在5.5之前nullCatalogMeansCurrent属性默认为true,8.0中默认为false， <br>
	 * 所以导致DatabaseMetaData.getTables()加载了全部的无关表。<br>
	 *
	 * @param tableName 表名(表名区分大小写)
	 * @return 表是否存在
	 */
	default boolean isTablePresent(final String tableName) {
		return this.isTablePresent(tableName, null, null);
	}

	/**
	 * 判断数据库表是否存在 <br>
	 * <p>
	 * 对于 返回数据库所有表 的异常情况的处理 <br>
	 * mysql8.0的驱动，在5.5之前nullCatalogMeansCurrent属性默认为true,8.0中默认为false， <br>
	 * 所以导致DatabaseMetaData.getTables()加载了全部的无关表。<br>
	 *
	 * @param tableName 表名(表名区分大小写)
	 * @param schema    表模式（表分组）
	 * @param catalog   数据库
	 * @return 表是否存在
	 */
	default boolean isTablePresent(final String tableName, final String schema, final String catalog) {
		final Connection conn = this.getConnection();
		ResultSet tables = null;
		boolean flag = false;

		try {
			final DatabaseMetaData databaseMetaData = conn.getMetaData();
			final String[] JDBC_METADATA_TABLE_TYPES = { "TABLE" };
			tables = databaseMetaData.getTables(catalog, schema, tableName, JDBC_METADATA_TABLE_TYPES);
			flag = tables.next();
		} catch (final Exception e) {
			e.printStackTrace();
		} finally {
			try {
				tables.close();
			} catch (final Exception e) {
				System.err.println("Error closing meta data tables:" + e);
				e.printStackTrace();
			}
		}

		return flag;
	}

	/**
	 * 查询结果集合 <br>
	 * 
	 * 对于短路的流，注意调用 stream.close() 来释放数据库连接, 或者 是 调用 jdbc.clear 来给与清空。<br>
	 * 
	 * @param connection  数据库连接
	 * @param sql         查询语句
	 * @param params      查询语句 的占位参数, params 为空的时候 不予 进行 sql 语句填充
	 * @param update_flag 是否更新
	 * @param close_conn  结束时候是否关闭 connection 数据连接
	 * @return IRecord 的 数据流 [rec]
	 * @throws SQLException
	 */
	static public Stream<IRecord> psql2recordS(final Connection connection, final String sql,
			final Map<Integer, ?> params, final boolean update_flag, final boolean close_conn) throws SQLException {

		PreparedStatement _pstmt = null;
		ResultSet _rs = null;
		try {
			_pstmt = Jdbc.pstmt(connection, update_flag ? SQL_MODE.UPDATE : SQL_MODE.QUERY, sql, params); // 创建查询语句
			if (update_flag) {
				_pstmt.execute();
				_rs = _pstmt.getGeneratedKeys();
				if (_rs == null)
					_rs = _pstmt.getResultSet();
			} else {
				_rs = _pstmt.executeQuery();
			}
		} catch (SQLException e) {
			System.out.println("error sql:%s".formatted(sql));
			throw e;
		}

		final var pstmt = _pstmt;
		final var rs = _rs;
		final Runnable closeHandler = () -> { // 关闭操作的回调函数
			try {
				if (!rs.isClosed()) { // 关闭结果集
					rs.close();
				} // !rs.isClosed()

				if (!pstmt.isClosed()) { // 关闭语句集合
					pstmt.close();
				} // !pstmt.isClosed())

				if (close_conn && !connection.isClosed()) { // 关闭数据库连接
					connection.close();
				} // close_conn && !connection.isClosed()
			} catch (Exception e) {
				e.printStackTrace();
			} // try
		}; // 关闭操作的回调函数

		return rs == null ? Stream.empty() : Jdbc.readlineS(rs, closeHandler).onClose(closeHandler); // 加入关闭处理子

	}

	/**
	 * 提取 GENERATED_KEY 字段的数据
	 *
	 * @param <U>    返回结果的元素的类型,mapper 的变换结果类型
	 * @param res    sql 的查询结果集合
	 * @param mapper 把GENERATED_KEY 字段转换成目标类型。
	 * @return GENERATED_KEY 生成的数据列表
	 */
	static <U> List<U> gkeys(final List<IRecord> res, Function<Object, U> mapper) {
		return res.stream().map(e -> e.str("GENERATED_KEY")).map(mapper).collect(Collectors.toList());
	}

	/**
	 * 生成一个 GENERATED_KEY 字段的数据
	 *
	 * @param res sql 的查询结果集合
	 * @return GENERATED_KEY 生成的数据列表
	 */
	static List<Long> ids(final List<IRecord> res) {
		return gkeys(res, e -> Long.parseLong(e + ""));
	}

	/**
	 * GENERATED_KEY 字段生成的数据
	 *
	 * @param res SQL 语句查询出来的额结果集合
	 * @return id值，从res中提取一个数值
	 */
	static Long id(final List<IRecord> res) {
		return ids(res).stream().findAny().get();
	}

	/**
	 * Function Table Get <br>
	 * 字段内容提取值: 返回单条记录<br>
	 * 其实就是这行一条简单 sql 查询：根据主键获取指定的数据记录 <br>
	 * MFT("select * from {0} where {1}={2}",table, x,idfld)) <br>
	 *
	 * @param <T>   transCode，即SessionId的类型．又叫事务编号
	 * @param <D>   Data，Session 中的数据类型。又叫Monad session 的状态数据。
	 * @param <X>   结果函数的 参数类型
	 * @param sess  会话对象
	 * @param table 提取表名
	 * @param idfld id字段名
	 * @return 字段值的提取函数
	 */
	static <T, D, X> Function<X, IRecord> FTBLGET(IJdbcSession<T, D> sess, String table, String idfld) {
		return Jdbcs.trycatch(x -> sess.sql2get(
				Jdbcs.format(x == null ? "select * from {0} where isnull({1})" : "select * from {0} where {1}=''{2}''",
						table, idfld, x.toString().replace("\\", "\\\\") // 转义"\n"
				))); // trycatch
	}

	/**
	 * Function Table Get <br>
	 * 字段内容提取值:返回多条记录<br>
	 * 其实就是这行一条简单 sql 查询：根据主键获取指定的数据记录 <br>
	 * MFT("select * from {0} where {1}={2}",table, x,idfld)) <br>
	 *
	 * @param <T>   transCode，即SessionId的类型．又叫事务编号
	 * @param <D>   Data，Session 中的数据类型。又叫Monad session 的状态数据。
	 * @param <X>   结果函数的 参数类型
	 * @param sess  会话对象
	 * @param table 提取表名
	 * @param idfld id字段名
	 * @return 字段值的提取函数
	 */
	static <T, D, X> Function<X, List<IRecord>> FTBLGETS(IJdbcSession<T, D> sess, String table, String idfld) {
		return Jdbcs.trycatch(x -> sess.sql2records(
				Jdbcs.format(x == null ? "select * from {0} where isnull({1})" : "select * from {0} where {1}=''{2}''",
						table, idfld, x.toString().replace("\\", "\\\\") // 转义"\n"
				))); // trycatch
	}

	/**
	 * 生成错误记录
	 * 
	 * @param method 方法名称
	 * @param sql    sql语句
	 * @param params 语句参数
	 * @return 错误记录
	 */
	static IRecord error(final String method, final String sql, final Object params) {
		final IRecord line = IRecord.REC("method", method, "sql", sql, "params", params, "time", LocalDateTime.now());
		return line;
	}

}// IJdbcSession