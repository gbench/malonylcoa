package gbench.util.data;

import java.io.*;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ParameterMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import javax.sql.DataSource;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.*;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static gbench.util.data.DataApp.IRecord.REC;

/**
 * 数据应用: All In One 的版本的库文件
 *
 * @author gbench
 */
public class DataApp {

	/**
	 * 数据应用
	 */
	public DataApp() {
		// do nothing
	}

	/**
	 * 数据应用
	 *
	 * @param dataSource 数据源
	 */
	public DataApp(final DataSource dataSource) {
		this.dataSource = dataSource;
	}

	/**
	 * 返回数据源
	 * 
	 * @return DataSource
	 */
	public DataSource ds() {
		return this.dataSource;
	}

	/**
	 * 获取数据库连接
	 *
	 * @return 数据库连接
	 */
	public Connection getConnection() {

		Connection conn = null;

		try {
			conn = this.dataSource.getConnection();
		} catch (final SQLException e) {
			e.printStackTrace();
		}

		return conn;
	}

	/**
	 * maybe 查询
	 *
	 * @param sql SQL语句
	 * @return maybe 查询
	 */
	@SuppressWarnings("unchecked")
	public Optional<IRecord> sqlmaybe(final String sql) {
		return (Optional<IRecord>) this.withTransaction(sess -> sess.setData(sess.sql2maybe(sql)));
	}

	/**
	 * maybe结果: 执行类
	 *
	 * @param sql SQL语句
	 * @return maybe结果: 执行类
	 */
	@SuppressWarnings("unchecked")
	public Optional<IRecord> sqlmaybe2(final String sql) {
		return (Optional<IRecord>) this.withTransaction(sess -> sess.setData(sess.sql2maybe2(sql)));
	}

	/**
	 * 查询出数据框
	 *
	 * @param sql sql 语句
	 * @return DFrame
	 */
	public DFrame sqldframe(final String sql) {
		final Object obj = this.withTransaction(sess -> {
			final DFrame dfm = sess.sql2x(sql);
			sess.setData(dfm); // 设置返回值
		});

		if (obj instanceof DFrame) {
			return (DFrame) obj;
		} else if (obj instanceof IRecord) {
			return DFrame.of(Arrays.asList((IRecord) obj));
		} else {
			return null;
		}
	}

	/**
	 * 查询出数据框
	 *
	 * @param sb sql语句构建器
	 * @return DFrame
	 */
	public DFrame sqldframe(final SqlBuilder sb) {
		return this.sqldframe(sb.toString());
	}

	/**
	 * 执行类数据框
	 *
	 * @param sql sql 语句
	 * @return DFrame
	 */
	public DFrame sqldframe2(final String sql) {
		final Object obj = this.withTransaction(sess -> {
			final DFrame dfm = sess.sql2executeS(sql).collect(DFrame.dfmclc);
			sess.setData(dfm); // 设置返回值
		});

		if (obj instanceof DFrame) {
			return (DFrame) obj;
		} else if (obj instanceof IRecord) {
			return DFrame.of(Arrays.asList((IRecord) obj));
		} else {
			return null;
		}
	}

	/**
	 * 查询出数据框
	 *
	 * @param sql sql 语句
	 * @return DFrame
	 */
	@SuppressWarnings("unchecked")
	public DFrame sqlexecute(final String sql) {
		Object ret = this.withTransaction(sess -> {
			final var rs = sess.sql2execute(sql);
			sess.setData(rs);
		});
		if (ret instanceof List<?> rs && rs.size() > 0) {
			final var r = rs.get(0);
			if (r instanceof IRecord) {
				return DFrame.of((List<IRecord>) rs);
			} else {
				return null;
			}
		} else if (ret instanceof IRecord rec) {
			return DFrame.of(rec);
		} else {
			return null;
		}
	}

	/**
	 * 查询出数据框
	 *
	 * @param sql sql 语句
	 * @return DFrame
	 */
	public Optional<DFrame> sqlexecuteopt(final String sql) {
		return Optional.ofNullable(this.sqlexecute(sql));
	}

	/**
	 * 查询出数据框
	 *
	 * @param sb sql语句构建器
	 * @return DFrame
	 */
	public Optional<DFrame> sqlexecuteopt(final SqlBuilder sb) {
		return this.sqlexecuteopt(sb.toString());
	}

	/**
	 * 表是否存在（需要注意，此处没有指定catalog，也就是默认tblname是哦正式数据库服务器中唯一，若是需要查询特定服务事情使用,带有catalog的版本）
	 * 该API会出现在A数据库中不存在而补B数据库中存在的table被误认为A数据库存在的情况，所以catalog必要时还需要给予指定
	 *
	 * @param tbl 数据表名
	 * @return true 存在,false 不存在。
	 */
	public Boolean tblExists(final String tbl) {
		return (Boolean) this.tblExists(tbl, null);
	}

	/**
	 * 表是否存在
	 *
	 * @param tbl     数据表名
	 * @param catalog 数据库名
	 * @return true 存在,false 不存在。
	 */
	public Boolean tblExists(final String tbl, String catalog) {
		return this.tblExists(tbl, null, catalog);
	}

	/**
	 * 表是否存在
	 *
	 * @param tbl     数据表名
	 * @param schema  表模式在数据库catalog只下的一个table分组,mysql一般为null,postgresql需要设置
	 * @param catalog 数据库名
	 * @return true 存在,false 不存在。
	 */
	public Boolean tblExists(final String tbl, String schema, String catalog) {
		return (Boolean) this.withTransaction(sess -> sess.setData(sess.isTablePresent(tbl, schema, catalog)));
	}

	/**
	 * 以事务的形式执行 action
	 *
	 * @param <T>    结果类型
	 * @param action 回调函数 sess->{}
	 * @return sess 中的数据对象 强制转换类型为 T, 当遭遇错误信息时,
	 *         会生成错误记录:{$error,$exception,$attributes,$sess_data}
	 */
	public synchronized <T> T withTransaction(final ExceptionalConsumer<IJdbcSession<Object, DFrame>> action) {
		final Connection conn = DataApp.this.getConnection();

		final IJdbcSession<Object, DFrame> session = new AbstractJdbcSession<Object, DFrame>() { // 创建会话session
			@Override
			public Connection getConnection() {
				return conn;
			}

			@Override
			public Collector<IRecord, ?, DFrame> collectorX() {
				return DFrame.dfmclc;
			}
		};

		try {
			conn.setAutoCommit(false);
			action.accept(session);
			if (!conn.isClosed()) {
				if (null != debug) {
					debug.accept(String.format("%s", IRecord.rb("msg").get("tx:conn.close")));
				}
				conn.commit();
			}
		} catch (final Exception e) {
			System.err.println("sess attributes" + session.getAttributes()); // 打印会话属性
			e.printStackTrace();

			final IRecord error_rec = REC("$error", e.getMessage(), "$exception", e, "$attributes",
					session.getAttributes(), "$sess_data", session.getData());
			final String error_line = String.format("Encounter exception:%s, Dump the session data:%s", //
					e.getMessage(), session.getData());
			System.err.println(error_line);
			session.setData(error_rec); // 记录错误信息

			try { // 数据回滚
				conn.rollback();
			} catch (final SQLException e1) {
				e1.printStackTrace();
			}
		} finally {
			session.clear(); // 清空所有流
			try {
				if (!conn.isClosed()) {
					conn.close();
					// System.err.println("连接关闭");
				} // if
			} catch (final SQLException e) {
				e.printStackTrace();
			} // try
		} // try

		@SuppressWarnings("unchecked")
		final T t = (T) session.getData(); // 强制类型转换为目标类型

		return t;
	}

	/**
	 * SqlBuilder
	 *
	 * @return SqlBuilder
	 */
	public static SqlBuilder SB() {
		return new SqlBuilder();
	}

	/**
	 * JOIN
	 *
	 * @param path 路径
	 * @param flds 字段
	 * @param mode 连接模式
	 * @return SqlBuilder
	 */
	public static SqlBuilder SB_JOIN(final String path, final String flds, final String mode) {
		return new SqlBuilder().join(path, flds, mode);
	}

	/**
	 * LEFT JOIN
	 *
	 * @param path 路径
	 * @param flds 字段
	 * @return SqlBuilder
	 */
	public static SqlBuilder SB_LEFT(final String path, final String flds) {
		return SB_JOIN(path, flds, "left");
	}

	/**
	 * RIGHT JOIN
	 *
	 * @param path 路径
	 * @param flds 字段
	 * @return SqlBuilder
	 */
	public static SqlBuilder SB_RIGHT(final String path, final String flds) {
		return SB_JOIN(path, flds, "right");
	}

	/**
	 * INNER JOIN
	 *
	 * @param path 路径
	 * @param flds 字段
	 * @return SqlBuilder
	 */
	public static SqlBuilder SB_INNER(final String path, final String flds) {
		return SB_JOIN(path, flds, "inner");
	}

	/**
	 * Tuple2
	 * 
	 * @param <T> 第一元素
	 * @param <U> 第二元素
	 * @author xuqinghua
	 */
	public static class Tuple2<T, U> implements Comparable<Tuple2<T, U>> {
		/**
		 * 构造函数
		 * 
		 * @param _1 第一元素
		 * @param _2 第二元素
		 */
		public Tuple2(final T _1, final U _2) {
			this._1 = _1;
			this._2 = _2;
		}

		/**
		 * 元素位置互换
		 *
		 * @return (u, t)
		 */
		public Tuple2<U, T> swap() {
			return TUP2(this._2, this._1);
		}

		/**
		 * 元素位置互换
		 *
		 * @param <X>    元素类型
		 * @param mapper 元祖变换函数 (u,t)->X
		 * @return X 类型结果
		 */
		public <X> X swap(final Function<Tuple2<U, T>, X> mapper) {
			return mapper.apply(this.swap());
		}

		/**
		 * 扁平化
		 *
		 * @param <X>    元素类型
		 * @param mapper 值变换函数 o->x
		 * @return X类型额数据流
		 */
		public <X> Stream<X> flatS(final Function<Object, X> mapper) {
			return Tuple2.flatS(this).map(mapper);
		}

		/**
		 * flatS 扁平化
		 * 
		 * @return 对象数据流
		 */
		public Stream<Object> flatS() {
			return flat(this).stream();
		}

		/**
		 * 一号元素变换
		 *
		 * @param <X>     变化函数值类型
		 * @param mapper1 t->x 一号元素变换函数
		 * @return (x, u) 元组类型
		 */
		public <X> Tuple2<X, U> fmap1(final Function<T, X> mapper1) {
			return Tuple2.of(mapper1.apply(_1), _2);
		}

		/**
		 * 二号元素变换
		 *
		 * @param <X>     变化函数值类型
		 * @param mapper2 u->x 二号元素变换函数
		 * @return (t, x)
		 */
		public <X> Tuple2<T, X> fmap2(final Function<U, X> mapper2) {
			return Tuple2.of(_1, mapper2.apply(_2));
		}

		/**
		 * 数值变换
		 *
		 * @param <X>    结果类型
		 * @param mapper 数值变换函数 tup->x
		 * @return X X类型的数据
		 */
		public <X> X mutate(final Function<Tuple2<T, U>, X> mapper) {
			return mapper.apply(this);
		}

		/**
		 * 生成数组 生成数组 [_1,_2]
		 *
		 * @return [_1, _2]
		 */
		public Object[] toArray() {
			return this.toArray(Object[]::new);
		}

		/**
		 * 生成数组 [_1,_2]
		 *
		 * @param generator 数组生成器 n->[t]
		 * @param <T>       元素类型
		 * @return [_1, _2]
		 */
		@SuppressWarnings("hiding")
		public <T> T[] toArray(final IntFunction<T[]> generator) {
			return Stream.of(this._1, this._2).toArray(generator);
		}

		/**
		 * 数组格式应用
		 * 
		 * @param <X>    结果类型
		 * @param mapper 概念换函数 [_1,_2]-&gt;x
		 * @return X 类型的结构
		 */
		public <X> X arrayOf(final Function<Object[], X> mapper) {
			return mapper.apply(this.toArray());
		}

		@Override
		public int hashCode() {
			return Objects.hash(this._1, this._2);
		}

		@Override
		public boolean equals(final Object obj) {
			if (obj instanceof Tuple2) {
				final BiPredicate<Object, Object> eql = (a, b) -> a != null ? a.equals(b) : b == null;
				@SuppressWarnings("unchecked")
				final Tuple2<Object, Object> another = (Tuple2<Object, Object>) obj;
				return eql.test(this._1, another._1) && eql.test(this._2, another._2);
			} else {
				return false;
			}
		}

		@Override
		@SuppressWarnings("unchecked")
		public int compareTo(final Tuple2<T, U> o) {
			if (o == null)
				return 1;
			final Optional<Comparable<T>> l1 = asCP(this._1);
			final Optional<Comparable<T>> r1 = asCP(o._1);
			final Optional<Comparable<U>> l2 = asCP(this._2);
			final Optional<Comparable<U>> r2 = asCP(o._2);

			final int t = l1.map(t1 -> r1.map(t2 -> t1.compareTo((T) t2)).orElse(1)).orElse(r1.isPresent() ? 0 : -1);
			if (t == 0) {
				final int u = l2.map(u1 -> r2.map(u2 -> u1.compareTo((U) u2)).orElse(1))
						.orElse(r2.isPresent() ? 0 : -1);
				return u;
			} else {
				return t;
			}
		}

		/**
		 * 格式化
		 */
		public String toString() {
			return IRecord.FT("($0,$1)", this._1, this._2);
		}

		/**
		 * 健名，键值 序列 (默认采用空格 逗号 /进行间隔)
		 *
		 * @param line 健名，键值 序列
		 * @return 健名，键值 序列
		 */
		public static final Stream<Tuple2<String, String>> tupleS(final String line) {
			return Tuple2.tupleS(line, "[\\s,;/]+");
		}

		/**
		 * 健名，键值 序列
		 *
		 * @param line    健名，键值序列
		 * @param pattern 分隔符模式
		 * @return 健名，键值 序列
		 */
		public static final Stream<Tuple2<String, String>> tupleS(final String line, final String pattern) {
			return Tuple2.tupleS(line.split(pattern));
		}

		/**
		 * 扁平化
		 * 
		 * @param tup 二元组
		 * @return 对象流
		 */
		public static Stream<Object> flatS(final Tuple2<?, ?> tup) {
			return flat(tup).stream();
		}

		/**
		 * 扁平化
		 * 
		 * @param tup 二元组
		 * @return 列表
		 */
		public static List<Object> flat(final Tuple2<?, ?> tup) {
			final List<Object> ll = new LinkedList<Object>();
			final Stack<Object> stack = new Stack<Object>();

			stack.push(tup);
			while (!stack.isEmpty()) {
				final Object p = stack.pop();
				if (p == null)
					continue;

				if (p instanceof Tuple2) {
					@SuppressWarnings("unchecked")
					final Tuple2<Object, Object> _tup = (Tuple2<Object, Object>) p;
					Stream.of(_tup._2, _tup._1).forEach(stack::push);
				} else {
					ll.add(p);
				}
			}

			return ll;
		}

		/**
		 * 健名，键值 序列
		 *
		 * @param <T> tt的元素类型
		 * @param tt  健名，键值 序列
		 * @return 健名，键值 序列
		 */
		@SafeVarargs
		public static <T> Stream<Tuple2<T, T>> tupleS(final T... tt) {
			return Stream.iterate(0, i -> i < tt.length, i -> i + 2)
					.map(i -> i + 1 >= tt.length ? null : new Tuple2<>(tt[i], tt[i + 1])).filter(Objects::nonNull);
		}

		/**
		 * 1 位 zipper
		 *
		 * @param <T> 第一元素
		 * @param <U> 第二元素
		 * @param itr 第一元素遍历器
		 * @return t->(t,u)
		 */
		public static <T, U> Function<U, Tuple2<T, U>> zipper1(final Iterator<T> itr) {
			final List<T> data = new ArrayList<T>();
			final AtomicInteger ai = new AtomicInteger();
			final Supplier<T> tgetter = () -> {
				final int i = ai.getAndIncrement();
				if (itr.hasNext()) {
					data.add(itr.next());
				}
				return data.get(i % data.size());
			};
			return u -> Tuple2.of(tgetter.get(), u);
		}

		/**
		 * 1 位 zipper
		 *
		 * @param <T> 第一元素
		 * @param <U> 第二元素
		 * @param tt  第一元素遍历器
		 * @return t->(t,u)
		 */
		public static <T, U> Function<U, Tuple2<T, U>> zipper1(final Iterable<T> tt) {
			return zipper1(tt.iterator());
		}

		/**
		 * 1 位 zipper
		 *
		 * @param <T> 第一元素
		 * @param <U> 第二元素
		 * @param tt  第一元素遍历器
		 * @return t->(t,u)
		 */
		public static <T, U> Function<U, Tuple2<T, U>> zipper1(final T[] tt) {
			return zipper1(Arrays.asList(tt));
		}

		/**
		 * 1 位 zipper
		 *
		 * @param <T> 第一元素
		 * @param <U> 第二元素
		 * @param stm 第一元素流
		 * @return t->(t,u)
		 */
		public static <T, U> Function<U, Tuple2<T, U>> zipper1(final Stream<T> stm) {
			return zipper1(stm.iterator());
		}

		/**
		 * 2 位 zipper
		 *
		 * @param <T> 第一元素
		 * @param <U> 第二元素
		 * @param itr 第二元素遍历器
		 * @return t->(t,u)
		 */
		public static <T, U> Function<T, Tuple2<T, U>> zipper2(final Iterator<U> itr) {
			final List<U> data = new ArrayList<U>();
			final AtomicInteger ai = new AtomicInteger();
			final Supplier<U> ugetter = () -> {
				final int i = ai.getAndIncrement();
				if (itr.hasNext()) {
					data.add(itr.next());
				}
				final int n = data.size();
				return n < 1 ? null : data.get(i % n);
			};
			return t -> Tuple2.of(t, ugetter.get());
		}

		/**
		 * 2 位 zipper
		 *
		 * @param <T> 第一元素
		 * @param <U> 第二元素
		 * @param uu  第二元素遍历器
		 * @return t->(t,u)
		 */
		public static <T, U> Function<T, Tuple2<T, U>> zipper2(final Iterable<U> uu) {
			return zipper2(uu.iterator());
		}

		/**
		 * 2 位 zipper
		 *
		 * @param <T> 第一元素
		 * @param <U> 第二元素
		 * @param uu  第二元素遍历器
		 * @return t->(t,u)
		 */
		public static <T, U> Function<T, Tuple2<T, U>> zipper2(final U[] uu) {
			return zipper2(Arrays.asList(uu));
		}

		/**
		 * 2 位 zipper
		 *
		 * @param <T> 第一元素
		 * @param <U> 第二元素
		 * @param stm 第二元素流
		 * @return t->(t,u)
		 */
		public static <T, U> Function<T, Tuple2<T, U>> zipper2(final Stream<U> stm) {
			return zipper2(stm.iterator());
		}

		/**
		 * snbuilder 的简写 <br>
		 * 键名，键值 生成器 <br>
		 * 开始号码0
		 *
		 * @param <T>   元素
		 * @param start 开始号码
		 * @return t->(int,t) 的标记函数
		 */
		public static <T> Function<T, Tuple2<Integer, T>> snb(final Integer start) {
			return snbuilder(start);
		}

		/**
		 * snb 的 键名，键值的调转形式 键名，键值 生成器 <br>
		 * 开始号码0
		 *
		 * @param <T>   元素
		 * @param start 开始号码
		 * @return t->(t,integer) 的标记函数
		 */
		public static <T> Function<T, Tuple2<T, Integer>> snb2(final Integer start) {
			final AtomicInteger sn = new AtomicInteger(start);
			return t -> TUP2(t, sn.getAndIncrement());
		}

		/**
		 * 序列号生成器 <br>
		 * Serial Number Builder
		 *
		 * @param <T>   元素
		 * @param start 开始号码
		 * @return t->(int,t) 的标记函数
		 */
		public static <T> Function<T, Tuple2<Integer, T>> snbuilder(final int start) {
			final AtomicInteger sn = new AtomicInteger(start);
			return t -> TUP2(sn.getAndIncrement(), t);
		}

		/**
		 * 键名，键值 生成器 <br>
		 * 开始号码0
		 * 
		 * @param <T> 元素类型
		 * @return t->(int,t) 的标记函数
		 */
		public static <T> Function<T, Tuple2<Integer, T>> snbuilder() {
			return snbuilder(0);
		}

		/**
		 * 构造一个二元组
		 *
		 * @param _1  第一元素
		 * @param _2  第二元素
		 * @param <T> 第一元素类型
		 * @param <U> 第二元素类型
		 * @return 二元组对象的构造
		 */
		public static <T, U> Tuple2<T, U> TUP2(final T _1, final U _2) {
			return Tuple2.of(_1, _2);
		}

		/**
		 * 构造一个二元组 <br>
		 * 提取tt前两个元素组成 Tuple2
		 *
		 * @param tt  数组元素,提取tt前两个元素组成 Tuple2, (tt[0],tt[1])
		 * @param <T> 第一元素类型,第二元素类型
		 * @return 二元组对象的构造
		 */
		public static <T> Tuple2<T, T> TUP2(final T[] tt) {
			if (tt == null || tt.length < 1) {
				return null;
			}

			final T _1 = tt.length >= 1 ? tt[0] : null;
			final T _2 = tt.length >= 2 ? tt[0] : null;

			return new Tuple2<>(_1, _2);
		}

		/**
		 * 二元组(t,u) 生成
		 * 
		 * @param <T> 第一元素类型
		 * @param <U> 第二元素类型
		 * @param t   第一元素
		 * @param u   第二元素
		 * @return 二元组(t,u)
		 */
		public static <T, U> Tuple2<T, U> of(final T t, final U u) {
			return new Tuple2<>(t, u);
		}

		/**
		 * Pair 成对儿函数 <br>
		 * 二元组(t,u) 生成
		 * 
		 * @param <T> 第一元素类型
		 * @param <U> 第二元素类型
		 * @param t   第一元素
		 * @param u   第二元素
		 * @return 二元组(t,u)
		 */
		public static <T, U> Tuple2<T, U> P(final T t, final U u) {
			return Tuple2.of(t, u);
		}

		/**
		 * 构造一个二元组
		 *
		 * @param iterable 可遍历对象
		 * @param <T>      iterable的元素类型
		 * @return 二元组对象的构造(提取可便利对象的前面两个元素)
		 */
		public static <T> Tuple2<T, T> from(final Iterable<T> iterable) {
			final Iterator<T> itr = iterable.iterator();
			final T t1 = itr.hasNext() ? itr.next() : null;
			final T t2 = itr.hasNext() ? itr.next() : null;
			return new Tuple2<T, T>(t1, t2);
		}

		/**
		 * 强制转换
		 *
		 * @param t   值对象
		 * @param <T> 元素类型
		 * @return Optional Comparable T
		 */
		@SuppressWarnings("unchecked")
		public static <T> Optional<Comparable<T>> asCP(final T t) {
			return Optional.ofNullable(t instanceof Comparable ? (Comparable<T>) t : null);
		}

		/**
		 * 元组比较 次序为 1号元素比较，较大的返回，若是 1号元素相等才比较二号元素。。
		 * 
		 * @param tup1 第一元组
		 * @param tup2 第二元组
		 * @param <T>  1号位置元素
		 * @param <U>  2号位置元素
		 * @return 最大的元组
		 */
		public static <T, U> Tuple2<T, U> max(final Tuple2<T, U> tup1, final Tuple2<T, U> tup2) {
			final Optional<Tuple2<T, U>> opt1 = Optional.ofNullable(tup1);
			final Optional<Tuple2<T, U>> opt2 = Optional.ofNullable(tup2);
			return opt1.map(t1 -> opt2.map(t2 -> t1.compareTo(t2) > 0 ? t1 : t2).orElse(t1))
					.orElse(opt2.isPresent() ? tup1 : tup2);
		}

		/**
		 * 元组比较 次序为 1号元素比较，较小的返回，若是 1号元素相等才比较二号元素。
		 * 
		 * @param tup1 第一元组
		 * @param tup2 第二元组
		 * @param <T>  1号位置元素
		 * @param <U>  2号位置元素
		 * @return 最小的元组
		 */
		public static <T, U> Tuple2<T, U> min(final Tuple2<T, U> tup1, final Tuple2<T, U> tup2) {
			final Optional<Tuple2<T, U>> opt1 = Optional.ofNullable(tup1);
			final Optional<Tuple2<T, U>> opt2 = Optional.ofNullable(tup2);
			return opt1.map(t1 -> opt2.map(t2 -> t1.compareTo(t2) < 0 ? t1 : t2).orElse(t1))
					.orElse(opt2.isPresent() ? tup1 : tup2);
		}

		/**
		 * 二元函数
		 * 
		 * @param <X>    1号元素类型
		 * @param <Y>    2号元素类型
		 * @param <Z>    结果类型
		 * @param mapper (x,y)->z
		 * @return (x,y)->z
		 */
		public static <X, Y, Z> Function<Tuple2<X, Y>, Z> bifun(final BiFunction<X, Y, Z> mapper) {
			return tup -> mapper.apply(tup._1, tup._2);
		}

		/**
		 * 生成一个 (x,y) 类型的 comparator <br>
		 * (升顺序,先比较第一元素键名,然后比较键值)
		 *
		 * @param <X> 第一元素类型
		 * @param <Y> 第二元素类型
		 * @return 生成一个 comparator
		 */
		public static <X, Y> Comparator<Tuple2<X, Y>> defaultComparator() {
			@SuppressWarnings("unchecked")
			final BiFunction<Object, Object, Integer> cmp = (a, b) -> { // 生成比较函数
				if (a == null && b == null)
					return 0;
				else if (a == null) {
					return -1;
				} else if (b == null) {
					return 1;
				} else { // a b 均为 非空
					int ret = -1; // 默认返回值
					try {
						if (a instanceof String || b instanceof String) {
							throw new Exception("字符串比较,设计的跳转异常，并非错误，这是一条类似于goto 的跳转语句写法");
						} else {
							ret = ((Comparable<Object>) a).compareTo(b);
						} // if
					} catch (final Exception e) {
						final String _a = a.toString();
						final String _b = b.toString();
						try { // 尝试做数字解析
							ret = Double.compare(Double.parseDouble(_a), Double.parseDouble(_b));
						} catch (Exception p) {
							ret = _a.compareTo(_b);
						} // try
					} // try

					return ret; // 返回比较结果
				} // if
			}; // cmp 比较函数

			return (tup1, tup2) -> {
				final int a = cmp.apply(tup1._1, tup2._1);
				if (a == 0) {
					return cmp.apply(tup1._2, tup2._2);
				} else {
					return a;
				} // if
			};
		}

		/**
		 * 1#元素
		 */
		public final T _1;

		/**
		 * 2#元素
		 */
		public final U _2;

	}

	/**
	 * IRecord
	 * 
	 * @author xuqinghua
	 */
	public interface IRecord extends Iterable<Tuple2<String, Object>>, Comparable<IRecord> {

		/**
		 * 根据键名进行取值
		 *
		 * @param key 键名
		 * @return 键key的值
		 */
		Object get(final String key);

		/**
		 * 设置键，若 key 与 老的 键 相同则 覆盖 老的值
		 *
		 * @param key   新的 键名
		 * @param value 键值
		 * @return 对象本身
		 */
		IRecord set(String key, Object value);

		/**
		 * 除掉键 key 的值
		 *
		 * @param key 新的 键名
		 * @return 对象本身(移除了key)
		 */
		IRecord remove(String key);

		/**
		 * 数据复制
		 *
		 * @return 当前对象的拷贝
		 */
		IRecord duplicate();

		/**
		 * 构建一个键名键值序列 指定的 IRecord
		 *
		 * @param kvs Map结构（IRecord也是Map结构） 或是 键名,键值 序列。即 build(map) 或是
		 *            build(key0,value0,key1,vlaue1,...) 的 形式， 特别注意 build(map) 时候，当且仅当
		 *            kvs 的只有一个元素，即 build(map0,map1) 会被视为 键值序列
		 * @return 新生成的IRecord
		 */
		IRecord build(final Object... kvs);

		/**
		 * 键名序列
		 *
		 * @return 键名列表
		 */
		List<String> keys();

		/**
		 * 返回一个 Map 结构<br>
		 * 非递归进行变换
		 *
		 * @return 一个 键值 对儿 的 列表 [(key,map)]
		 */
		Map<String, Object> toMap();

		/**
		 * 根据键名进行取值
		 *
		 * @param idx 键名索引从0开始
		 * @return 键名索引idx的值
		 */
		default Object get(final int idx) {
			return this.get(this.keyOf(idx));
		}

		/**
		 * IRecord 变换
		 * 
		 * @param <T>    结果类型
		 * @param mapper rec->t , 若 mapper 为 null 则返回 this
		 * @return T 类型结果
		 */
		@SuppressWarnings("unchecked")
		default <T> T get(final Function<IRecord, T> mapper) {
			return mapper == null ? (T) this : mapper.apply(this);
		}

		/**
		 * 根据键名提取数据(pathgetS单层路径别名) <br>
		 * 可以识别的 值类型 包括: Collection,數組,Map,Stream,其他类型视为一个单个元素的流[a]。
		 *
		 * @param <T>    元素类型
		 * @param <U>    结果（流）：元素类型
		 * @param key    键名
		 * @param mapper 元素值变换函数 t-&gt;u
		 * @return U类型的流
		 */
		default <T, U> Stream<U> getS(final String key, final Function<T, U> mapper) {
			return this.pathgetS(key, mapper);
		}

		/**
		 * 根据键名索引提取数据 <br>
		 * 可以识别的 值类型 包括: Collection,數組,Map,Stream,其他类型视为一个单个元素的流[a]。
		 *
		 * @param <T>    元素类型
		 * @param <U>    结果（流）：元素类型
		 * @param idx    键名索引,从0开始
		 * @param mapper 元素值变换函数 t-&gt;u
		 * @return U类型的流
		 */
		default <T, U> Stream<U> getS(final int idx, final Function<T, U> mapper) {
			return this.getS(this.keyOf(idx), mapper);
		}

		/**
		 * 根据键名提取数据(pathgetS单层路径别名) <br>
		 * 可以识别的 值类型 包括: Collection,數組,Map,Stream,其他类型视为一个单个元素的流[a]。
		 *
		 * @param <T>    元素类型
		 * @param <U>    结果（流）：元素类型
		 * @param key    键名
		 * @param mapper 元素值变换函数 t-&gt;u
		 * @return U类型的列表
		 */
		default <T, U> List<U> gets(final String key, final Function<T, U> mapper) {
			return this.getS(key, mapper).toList();
		}

		/**
		 * 根据键名索引提取数据 <br>
		 * 可以识别的 值类型 包括: Collection,數組,Map,Stream,其他类型视为一个单个元素的流[a]。
		 *
		 * @param <T>    元素类型
		 * @param <U>    结果（流）：元素类型
		 * @param idx    键名索引,从0开始
		 * @param mapper 元素值变换函数 t-&gt;u
		 * @return U类型的列表
		 */
		default <T, U> List<U> gets(final int idx, final Function<T, U> mapper) {
			return this.gets(this.keyOf(idx), mapper);
		}

		/**
		 * 除掉键 idx 的值
		 *
		 * @param idx 键名索引从0开始
		 * @return 对象本身(移除了key)
		 */
		default IRecord remove(final int idx) {
			return this.remove(this.keyOf(idx));
		}

		/**
		 * 尝试强转数据累心为T
		 *
		 * @param key         键名
		 * @param placeholder 类型,占位符 用于表示 返回值类型
		 * @param <T>         返回值类型
		 * @return t 类型的结果
		 */
		@SuppressWarnings("unchecked")
		default <T> T as(final String key, T placeholder) {
			final T _t = (T) this.get(key);
			return _t;
		}

		/**
		 * 尝试强转数据累心为T
		 *
		 * @param idx         键名索引,从0开始
		 * @param placeholder 类型,占位符 用于表示 返回值类型
		 * @param <T>         返回值类型
		 * @return t 类型的结果
		 */
		default <T> T as(final int idx, T placeholder) {
			return this.as(this.keyOf(idx), placeholder);
		}

		/**
		 * 转换成 tuple2 的 流式结构
		 *
		 * @return tuple的流结构 [(k,v)]
		 */
		default Stream<Tuple2<String, Object>> tupleS() {
			return StreamSupport.stream(this.spliterator(), false);
		}

		/**
		 * 转换成 tuple2 的 流式结构
		 *
		 * @return tuple的流结构 [(k,v)]
		 */
		default List<Tuple2<String, Object>> tuples() {
			return this.tupleS().collect(Collectors.toList());
		}

		/**
		 * 健名流
		 *
		 * @return 健名流
		 */
		default Stream<String> keyS() {
			return this.tupleS().map(e -> e._1);
		}

		/**
		 * 键值流
		 *
		 * @return 键值流
		 */
		default Stream<Object> valueS() {
			return this.tupleS().map(e -> e._2);
		}

		/**
		 * 键值列表
		 *
		 * @return 键值列表
		 */
		default List<Object> vals() {
			return this.tupleS().map(e -> e._2).toList();
		}

		/**
		 * 这个属性对不是分词器来说不是必须，不过就像设计衣服的时候为 服装缝制个口袋，完全是为了便于 携带数据而做的设计而已。<br>
		 * 比如把 yuhuan作为参数传递时还需要附加一些关联数据的时候，可以省略一个调用导函数的参数位置。<br>
		 * 把这个数据写入到 attributes 中就可以了。<br>
		 * 这就是袋袋裤的设计思想的程序体现。<br>
		 * <p>
		 * 根据类型检索属性对象
		 *
		 * @param <T>   属性对象
		 * @param clazz 属性类型
		 * @return 属性值 或 null
		 */
		@SuppressWarnings("unchecked")
		default <T> T findOne(final Class<T> clazz) {
			return (T) valueS() //
					.filter(e -> e != null && clazz.isAssignableFrom(e.getClass())).findFirst().orElse(null);
		}

		/**
		 * 设置键，若 key 与 老的 键 相同则 覆盖 老的值
		 *
		 * @param key   键名
		 * @param value 键值
		 * @return 对象本身
		 */
		default IRecord add(String key, Object value) {

			this.set(key, value);
			return this;
		}

		/**
		 * 设置键，若 key 与 老的 键 相同则 覆盖 老的值
		 *
		 * @param tup 键值对儿
		 * @return 对象本身
		 */
		default IRecord add(final Tuple2<String, ?> tup) {

			return this.add(tup._1, tup._2);
		}

		/**
		 * 批量添加键值列表 <br>
		 * 元组信息添加到对象本身
		 *
		 * @param <T>    第一元素类型
		 * @param <U>    第二元素类型
		 * @param <TP>   元组类型
		 * @param tupItr 元组列表
		 * @return 对象本身
		 */
		default <T, U, TP extends Tuple2<T, U>> IRecord add(final Iterable<TP> tupItr) {
			tupItr.forEach(e -> {
				final Tuple2<String, ?> tup = Tuple2.of(e._1 + "", e._2);
				this.add(tup);
			});
			return this;
		}

		/**
		 * 批量添加键值列表 <br>
		 * 元组信息添加到对象本身
		 *
		 * @param <T>  参数列表元素类型
		 * @param objs Map结构(IRecord也是Map结构) 或是 键名,键值 序列。即 build(map) 或是
		 *             build(key0,value0,key1,vlaue1,...) 的 形式， 特别注意 build(map) 时候，当且仅当
		 *             kvs 的只有一个元素，即 build(map0,map1) 会被视为 键值序列
		 * @return 依据实现不同可以为新生成的IRecord或者是对象本身
		 */
		@SuppressWarnings("unchecked")
		default <T> IRecord add(final T... objs) {
			return this.add(REC(objs));
		}

		/**
		 * 批量添加键值列表 <br>
		 * 元组信息添加到对象本身
		 *
		 * @param kvs 元组列表
		 * @return 对象本身
		 */
		default IRecord add(final Map<?, ?> kvs) {
			kvs.entrySet().forEach(e -> {
				final Tuple2<String, ?> tup = Tuple2.of(e.getKey() + "", e.getValue());
				this.add(tup);
			});
			return this;
		}

		/**
		 * 批量添加键值列表 <br>
		 * 元组信息添加到对象本身
		 *
		 * @param objs Map结构（IRecord也是Map结构） 或是 键名,键值 序列。即 build(map) 或是
		 *             build(key0,value0,key1,vlaue1,...) 的 形式， 特别注意 build(map) 时候，当且仅当
		 *             kvs 的只有一个元素，即 build(map0,map1) 会被视为 键值序列
		 * @return 新生成的IRecord 或 对象本身
		 */
		default IRecord prepend(final Object... objs) {
			return REC(objs).add(this);
		}

		/**
		 * 批量添加键值列表 <br>
		 * 元组信息添加到复制的对象
		 *
		 * @param <T>    第一元素类型
		 * @param <U>    第二元素类型
		 * @param tupItr 元组列表
		 * @return 复制的新的对象
		 */
		default <T, U> IRecord derive(final Iterable<Tuple2<T, U>> tupItr) {
			final IRecord rec = this.duplicate();
			return rec.add(tupItr);
		}

		/**
		 * 批量添加键值列表 <br>
		 * 元组信息添加到对象本身
		 *
		 * @param objs Map结构(IRecord也是Map结构) 或是 键名,键值 序列。即 build(map) 或是
		 *             build(key0,value0,key1,vlaue1,...) 的 形式， 特别注意 build(map) 时候，当且仅当
		 *             kvs 的只有一个元素，即 build(map0,map1) 会被视为 键值序列
		 * @return 复制的新的对象
		 */
		default IRecord derive(final Object... objs) {
			return this.duplicate().add(REC(objs));
		}

		/**
		 * 把idx转key
		 *
		 * @param idx 键名索引 从0开始
		 * @return 索引转键名
		 */
		default String keyOf(final int idx) {
			final List<String> kk = this.keys();
			return idx < kk.size() ? kk.get(idx) : null;
		}

		/**
		 * 根据路径提取数据 (pathget 的别名 ) <br>
		 * <p>
		 * 可以识别的值类型IRecord,Map,Collection,Array其中Collection和Array 的 key为索引序号，从开始
		 *
		 * @param <X>          元素类型
		 * @param <Y>          元素类型Y 需要为
		 *                     IRecord,Map,Collection,Array其中Collection和Array任一
		 * @param <T>          元素类型
		 * @param <U>          结果类型
		 * @param path         键名路径
		 * @param preprocessor 预处理器 x-&gt;y
		 * @param mapper       值变换函数 t-&gt;u
		 * @return U类型的值
		 */
		default <X, Y, T, U> U pget(final String path, final BiFunction<String, X, Y> preprocessor,
				final Function<T, U> mapper) {
			return this.pathget(path, mapper);
		}

		/**
		 * 根据路径提取数据 (pathget 的别名 ) <br>
		 * <p>
		 * 可以识别的值类型IRecord,Map,Collection,Array其中Collection和Array 的 key为索引序号，从开始
		 *
		 * @param <X>    元素类型
		 * @param <Y>    元素类型Y 需要为 IRecord,Map,Collection,Array其中Collection和Array任一
		 * @param <T>    元素类型
		 * @param <U>    结果类型
		 * @param path   键名路径
		 * @param mapper 值变换函数 t-&gt;u
		 * @return U类型的值
		 */
		default <X, Y, T, U> U pget(final String path, final Function<T, U> mapper) {
			return this.pathget(path, mapper);
		}

		/**
		 * 根据路径设置数据 (pathget 的别名 ) <br>
		 * <p>
		 * 可以识别的值类型IRecord,Map,Collection,Array其中Collection和Array 的 key为索引序号，从开始
		 *
		 * @param <X>   元素类型
		 * @param <Y>   元素类型Y 需要为 IRecord,Map,Collection,Array其中Collection和Array任一
		 * @param <T>   元素类型
		 * @param <U>   结果类型
		 * @param path  键名路径
		 * @param value 数据值
		 * @return IRecord 对象本省
		 */
		default <X, Y, T, U> IRecord pset(final String path, final Object value) {
			final String[] pp = path.split("[/,]+");
			final int n = pp.length;

			if (n < 2) {
				this.set(path, value);
			} else {
				final String[] parent = Arrays.copyOfRange(pp, 0, n - 1);
				final IRecord r = this.pathget(parent, (k, e) -> e, e -> (IRecord) e);
				if (null != r) {
					r.set(pp[n - 1], value);
				} // if
			} // if
			return this;
		}

		/**
		 * 键名索引
		 *
		 * @param key 键名
		 * @return 键名索引 从0开始, key 为null 时返回null
		 */
		default Integer indexOf(final String key) {

			if (key == null) {
				return null;
			}

			final List<String> kk = this.keys();

			for (int i = 0; i < kk.size(); i++) {
				final String _key = kk.get(i);
				if (key.equals(_key)) {
					return i;
				}
			}

			return null;
		}

		/**
		 * 根据路径提取数据
		 *
		 * @param <X>          元素类型
		 * @param <Y>          元素类型，需要为IRecord,Map,Collection,Array其中Collection和Array任一
		 * @param <T>          元素类型
		 * @param <U>          结果类型
		 * @param path         键名路径
		 * @param preprocessor 预处理器 x-&gt;y
		 * @param mapper       值变换函数 t-&gt;u
		 * @return U类型的值
		 */
		@SuppressWarnings("unchecked")
		default <X, Y, T, U> U pathget(final String[] path, final BiFunction<String, X, Y> preprocessor,
				final Function<T, U> mapper) {
			final String key = path[0].trim();
			final int len = path.length;

			if (len > 1) {
				final IRecord _rec = this.rec(key, preprocessor);
				final String[] _path = Arrays.copyOfRange(path, 1, len);
				return _rec != null ? _rec.pathget(_path, preprocessor, mapper) : null;
			} else {
				return mapper.apply((T) this.get(key));
			} // if
		}

		/**
		 * 根据路径提取数据 <br>
		 * <p>
		 * 可以识别的值类型IRecord,Map,Collection,Array其中Collection和Array 的 key为索引序号，从开始
		 *
		 * @param <X>          元素类型
		 * @param <Y>          元素类型Y 需要为
		 *                     IRecord,Map,Collection,Array其中Collection和Array任一
		 * @param <T>          元素类型
		 * @param <U>          结果类型
		 * @param path         键名路径
		 * @param preprocessor 预处理器 x-&gt;y
		 * @param mapper       值变换函数 t-&gt;u
		 * @return U类型的值
		 */
		default <X, Y, T, U> U pathget(final String path, final BiFunction<String, X, Y> preprocessor,
				final Function<T, U> mapper) {
			return this.pathget(path.split("[/,]+"), preprocessor, mapper);
		}

		/**
		 * 根据路径提取数据
		 *
		 * @param <T>    元素类型
		 * @param <U>    结果类型
		 * @param path   键名路径 如 a/b/c
		 * @param mapper 值变换函数 t-&gt;u
		 * @return U类型的值
		 */
		default <T, U> U pathget(final String path, final Function<T, U> mapper) {
			return this.pathget(path, (k, e) -> e, mapper);
		}

		/**
		 * 根据路径提取数据 <br>
		 * 可以识别的 值类型 包括: Collection,數組,Map,Stream,其他类型视为一个单个元素的流[a]。
		 *
		 * @param <T>    元素类型
		 * @param <U>    结果（流）：元素类型
		 * @param path   键名路径 如 a/b/c
		 * @param mapper 元素值变换函数 t-&gt;u
		 * @return U类型的流
		 */
		@SuppressWarnings("unchecked")
		default <T, U> Stream<U> pathgetS(final String path, final Function<T, U> mapper) {
			return this.pathget(path, (e) -> {
				return Optional.ofNullable(e).map(_e -> {
					final Class<?> tclass = _e.getClass(); // 提取元素类型
					if (tclass.isArray()) { // 数组结构
						return Stream.of((T[]) _e);
					} else if (e instanceof Collection) { // Collection 结构
						return ((Collection<T>) _e).stream();
					} else if (e instanceof Stream) {// Stream 结构
						return (Stream<T>) _e;
					} else if (e instanceof Map) { // map结构
						return (Stream<T>) ((Map<Object, Object>) _e).entrySet().stream();
					} else { // 其他结构
						return Stream.of((T) _e);
					} // if
				}).map(stream -> stream.map(mapper)).orElse(Stream.empty());
			}); // pathget
		}

		/**
		 * 根据路径提取数据 <br>
		 * 可以识别的 值类型 包括: Collection,數組,Map,Stream,其他类型视为一个单个元素的流[a]。
		 *
		 * @param <T>        结果（流）：元素类型
		 * @param path       键名路径 如 a/b/c
		 * @param typeholder 结果类型占位符可以为 (T)null
		 * @return U类型的流
		 */
		@SuppressWarnings("unchecked")
		default <T> Stream<T> pathgetS(final String path, final T typeholder) {
			return this.pathgetS(path, e -> (T) e);
		}

		/**
		 * 根据路径提取数据 <br>
		 * 可以识别的 值类型 包括: Collection,數組,Map,Stream,其他类型视为一个单个元素的流[a]。
		 *
		 * @param path 键名路径 如 a/b/c
		 * @return Object类型的流
		 */
		default Stream<Object> pathgetS(final String path) {
			return this.pathgetS(path, e -> (Object) e);
		}

		/**
		 * 根据路径提取数据
		 *
		 * @param path 键名路径 如 a/b/c
		 * @return U类型的值
		 */
		default String pathstr(final String path) {
			return this.pathget(path, e -> Optional.ofNullable(e).map(Object::toString).orElse(null));
		}

		/**
		 * 根据路径提取数据
		 *
		 * @param path 键名路径 如 a/b/c
		 * @return U类型的值
		 */
		default Boolean pathbool(final String path) {
			return this.pathget(path, e -> REC("0", e).bool(0));
		}

		/**
		 * 根据路径提取数据
		 *
		 * @param path 键名路径 如 a/b/c
		 * @return U类型的值
		 */
		default Integer pathi4(final String path) {
			return this.pathget(path,
					e -> Optional.ofNullable(IRecord.obj2dbl(null).apply(e)).map(Number::intValue).orElse(null));
		}

		/**
		 * 根据路径提取数据
		 *
		 * @param path 键名路径 如 a/b/c
		 * @return U类型的值
		 */
		default Long pathlng(final String path) {
			return this.pathget(path,
					e -> Optional.ofNullable(IRecord.obj2dbl(null).apply(e)).map(Number::longValue).orElse(null));
		}

		/**
		 * 根据路径提取数据
		 *
		 * @param path 键名路径 如 a/b/c
		 * @return U类型的值
		 */
		default LocalTime pathlt(final String path) {
			return this.pathget(path,
					e -> Optional.ofNullable(IRecord.asLocalDateTime(e)).map(p -> p.toLocalTime()).orElse(null));
		}

		/**
		 * 根据路径提取数据
		 *
		 * @param path 键名路径 如 a/b/c
		 * @return U类型的值
		 */
		default Double pathdbl(final String path) {
			return this.pathget(path, IRecord.obj2dbl(null));
		}

		/**
		 * 根据路径提取数据
		 *
		 * @param path 键名路径 如 a/b/c
		 * @return U类型的值
		 */
		default LocalDateTime pathldt(final String path) {
			return this.pathget(path, IRecord::asLocalDateTime);
		}

		/**
		 * 根据路径提取数据
		 *
		 * @param path 键名路径 如 a/b/c
		 * @return U类型的值
		 */
		default LocalDate pathld(final String path) {
			return Optional.ofNullable(this.pathldt(path)).map(e -> e.toLocalDate()).orElse(null);
		}

		/**
		 * 根据路径提取数据
		 *
		 * @param path 键名路径 如 a/b/c
		 * @return U类型的值
		 */
		default Timestamp pathts(final String path) {
			return this.pathget(path, e -> REC("0", e).timestamp(0));
		}

		/**
		 * 根据路径提取数据
		 *
		 * @param path 键名路径 如 a/b/c
		 * @return U类型的值
		 */
		default Date pathdate(final String path) {
			return this.pathts(path);
		}

		/**
		 * 提取record类型的结果 <br>
		 * <p>
		 * 可以识别的值类型IRecord,Map,Collection,Array其中Collection和Array 的 key为索引序号，从开始
		 *
		 * @param <X>          源数据类型
		 * @param <Y>          目标数据类型,即 IRecord,Map,Collection,Array 其中之一
		 * @param key          键名
		 * @param preprocessor 预处理器
		 * @return IRecord类型的值
		 */
		@SuppressWarnings("unchecked")
		default <X, Y> IRecord rec(final String key, final BiFunction<String, X, Y> preprocessor) {
			final Y value = Optional.ofNullable(preprocessor)
					.orElse((BiFunction<String, X, Y>) (k, x) -> (Y) IRecord.REC(x)).apply(key, (X) this.get(key));
			final Function<Collection<?>, IRecord> clcn2rec = tt -> {
				int i = 0;
				final IRecord rec = this.build(); // 创建一个空IRecord
				for (final Object t : tt) {
					rec.add("" + (i++), t);
				} // for
				return rec;
			}; // clc2rec

			if (value instanceof IRecord) {
				return (IRecord) value;
			} else if (value instanceof Map) {
				return this.build((Map<?, ?>) value);
			} else if (value instanceof Collection || (value != null && value.getClass().isArray())) {
				final Collection<Object> tt = value instanceof Collection // 值类型判断
						? (Collection<Object>) value // 集合类型
						: (Collection<Object>) Arrays.asList((Object[]) value); // 数组类型
				return clcn2rec.apply(tt);
			} else if (value instanceof Iterable) {
				final Stream<?> stream = StreamSupport.stream(((Iterable<?>) value).spliterator(), false).limit(1024);
				final List<?> ll = stream.collect(Collectors.toList());
				return clcn2rec.apply(ll);
			} else if (value instanceof String) { // json 结构的数据处理
				return IRecord.REC(value);
			} else {
				return null;
			}
		}

		/**
		 * 返回 key 所对应的 键值, IRecord 类型
		 *
		 * @param key 键名
		 * @return key 所标定的 值
		 */
		default IRecord rec(final String key) {
			return this.rec(key, null);
		}

		/**
		 * 返回 键名索引 所对应的 键值, IRecord 类型
		 *
		 * @param idx 键名索引 从0开始
		 * @return key 所标定的 值
		 */
		default IRecord rec(final int idx) {
			return this.rec(this.keyOf(idx));
		}

		/**
		 * 取键值
		 *
		 * @param name 键名
		 * @return 键值Optional
		 */
		default public Optional<Object> opt(final String name) {
			return Optional.ofNullable(this.get(name));
		}

		/**
		 * 取键值
		 *
		 * @param idx 键名索引从0开始
		 * @return 键值Optional
		 */
		default public Optional<Object> opt(final int idx) {
			final String key = this.keyOf(idx);
			return Optional.ofNullable(this.get(key));
		}

		/**
		 * 返回 key 所对应的 键值, String 类型
		 *
		 * @param key 键名
		 * @return key 所标定的 值
		 */
		default String str(final String key) {

			final Object obj = this.get(key);

			if (obj == null) {
				return null;
			}

			return obj instanceof String ? (String) obj : obj + "";
		}

		/**
		 * 返回 idx 所对应的 键值, String 类型
		 *
		 * @param idx 键名索引 從 0开始
		 * @return idx 所标定的 值
		 */
		default String str(final int idx) {
			return this.str(this.keyOf(idx));
		}

		/**
		 * 返回 建明索引 所对应的 键值, String 类型
		 *
		 * @param idx 键名索引 从0开始
		 * @return idx 所标定的 值 Optional
		 */
		default Optional<String> strOpt(final int idx) {
			return this.opt(idx).map(Object::toString);
		}

		/**
		 * 返回 key 所对应的 键值, String 类型
		 *
		 * @param key 键名
		 * @return key 所标定的 值 Optional
		 */
		default Optional<String> strOpt(final String key) {
			return this.opt(key).map(Object::toString);
		}

		/**
		 * 返回 key 所对应的 键值, Double 类型
		 *
		 * @param key 键名
		 * @return key 所标定的 值
		 */
		default Double dbl(final String key) {
			final Object obj = this.get(key);
			return IRecord.obj2dbl(null).apply(obj);
		}

		/**
		 * 返回 idx 所对应的 键值, Double 类型
		 *
		 * @param idx 键名索引 從 0开始
		 * @return idx 所标定的 值
		 */
		default Double dbl(final int idx) {
			return this.dbl(this.keyOf(idx));
		}

		/**
		 * 返回 建明索引 所对应的 键值, Double 类型
		 *
		 * @param idx 键名索引 从0开始
		 * @return idx 所标定的 值 Optional
		 */
		default Optional<Double> dblOpt(final int idx) {
			return this.dblOpt(this.keyOf(idx));
		}

		/**
		 * 返回 key 所对应的 键值, Double 类型
		 *
		 * @param key 键名
		 * @return key 所标定的 值 Optional
		 */
		default Optional<Double> dblOpt(final String key) {
			return this.opt(key).map(IRecord.obj2dbl(null));
		}

		/**
		 * 返回 idx 所对应的 键值, Integer 类型
		 *
		 * @param idx 键名索引 從 0开始
		 * @return idx 所标定的 值
		 */
		default Integer i4(final int idx) {
			return this.i4(this.keyOf(idx));
		}

		/**
		 * 返回 key 所对应的 键值, Integer 类型
		 *
		 * @param key 键名
		 * @return key 所标定的 值
		 */
		default Integer i4(final String key) {

			final Object obj = this.get(key);

			return Optional.ofNullable(IRecord.obj2dbl(null).apply(obj)).map(Number::intValue).orElse(null);
		}

		/**
		 * 返回 建明索引 所对应的 键值, Integer 类型
		 *
		 * @param idx 键名索引 从0开始
		 * @return idx 所标定的 值 Optional
		 */
		default Optional<Integer> i4Opt(final int idx) {
			return this.i4Opt(this.keyOf(idx));
		}

		/**
		 * 返回 key 所对应的 键值, Integer 类型
		 *
		 * @param key 键名
		 * @return key 所标定的 值 Optional
		 */
		default Optional<Integer> i4Opt(final String key) {
			return Optional.ofNullable(this.i4(key));
		}

		/**
		 * 返回 idx 所对应的 键值, Long 类型
		 *
		 * @param idx 键名索引 從 0开始
		 * @return idx 所标定的 值
		 */
		default Long lng(final int idx) {
			return this.lng(this.keyOf(idx));
		}

		/**
		 * 返回 key 所对应的 键值, Long 类型
		 *
		 * @param key 键名
		 * @return key 所标定的 值
		 */
		default Long lng(final String key) {
			final Object obj = this.get(key);

			return Optional.ofNullable(IRecord.obj2dbl(null).apply(obj)).map(Number::longValue).orElse(null);
		}

		/**
		 * 返回 建明索引 所对应的 键值, Long 类型
		 *
		 * @param idx 键名索引 从0开始
		 * @return idx 所标定的 值 Optional
		 */
		default Optional<Long> lngOpt(final int idx) {
			return this.lngOpt(this.keyOf(idx));
		}

		/**
		 * 返回 key 所对应的 键值, Long 类型
		 *
		 * @param key 键名
		 * @return key 所标定的 值 Optional
		 */
		default Optional<Long> lngOpt(final String key) {
			return Optional.ofNullable(this.lng(key));
		}

		/**
		 * 返回 key 所对应的 键值, LocalDateTime 类型
		 *
		 * @param key          键名
		 * @param defaultValue 默认值
		 * @return key 所标定的 值
		 */
		default LocalDateTime ldt(final String key, final LocalDateTime defaultValue) {
			final Object value = this.get(key);

			if (value == null) { // 空结构
				return defaultValue;
			} else { // 非空值
				final LocalDateTime ldt = IRecord.asLocalDateTime(value);
				return Optional.ofNullable(ldt).orElse(defaultValue);
			} // if
		}

		/**
		 * 返回 key 所对应的 键值, LocalDateTime 类型
		 *
		 * @param key 键名
		 * @return key 所标定的 值
		 */
		default LocalDateTime ldt(final String key) {
			return this.ldt(key, null);
		}

		/**
		 * 返回 建明索引 所对应的 键值, LocalDateTime 类型
		 *
		 * @param idx 键名索引 从0开始
		 * @return idx 所标定的 值 Optional
		 */
		default Optional<LocalDateTime> ldtOpt(final int idx) {
			return this.ldtOpt(this.keyOf(idx));
		}

		/**
		 * 返回 key 所对应的 键值, LocalDateTime 类型
		 *
		 * @param key 键名
		 * @return key 所标定的 值 Optional
		 */
		default Optional<LocalDateTime> ldtOpt(final String key) {
			return this.opt(key).map(IRecord::asLocalDateTime);
		}

		/**
		 * 把key键名转换成逻是日期值
		 *
		 * @param key 键名
		 * @return Date 值
		 */
		default Date date(final String key) {
			final LocalDateTime localDateTime = this.ldt(key);
			if (localDateTime == null)
				return null;
			final ZoneId zoneId = ZoneId.systemDefault();
			final ZonedDateTime zdt = localDateTime.atZone(zoneId);
			return Date.from(zdt.toInstant());
		}

		/**
		 * 把idx键名索引转换成逻是日期值
		 *
		 * @param idx 键名索引
		 * @return Date 值
		 */
		default Date date(final int idx) {
			return this.date(this.keyOf(idx));
		}

		/**
		 * 把key列转换成逻是时间戳
		 *
		 * @param key 键名
		 * @return Timestamp 时间错
		 */
		default Timestamp timestamp(final String key) {
			final Object o = this.get(key);
			if (o == null)
				return null;
			if (o instanceof Timestamp)
				return (Timestamp) o;
			final Date time = this.date(key);
			if (time == null)
				return null;
			return new Timestamp(time.getTime());
		}

		/**
		 * 把idx键名索引转换成逻是时间戳
		 *
		 * @param idx 键名索引从0开始
		 * @return Timestamp 时间错
		 */
		default Timestamp timestamp(final int idx) {
			return this.timestamp(this.keyOf(idx));
		}

		/**
		 * 把key列转换成逻辑值
		 *
		 * @param idx 键名索引,从0开始
		 * @return 布尔类型
		 */
		default Boolean bool(final int idx) {
			return this.bool(this.keyOf(idx));
		}

		/**
		 * 把key列转换成逻辑值
		 *
		 * @param key 键名
		 * @return 布尔类型
		 */
		default Boolean bool(final String key) {
			final Object o = this.get(key);
			boolean b = false;

			if (o instanceof Number) {
				b = ((Number) o).intValue() != 0;
			} else {
				try {
					b = Boolean.parseBoolean(o + "");
				} catch (final Exception e) {
					// do nothing
				} // try
			} // if
			return b;
		}

		/**
		 * 返回 建明索引 所对应的 键值, Boolean 类型
		 *
		 * @param idx 键名索引 从0开始
		 * @return idx 所标定的 值 Optional
		 */
		default Optional<Boolean> boolOpt(final int idx) {
			return this.boolOpt(this.keyOf(idx));
		}

		/**
		 * 返回 key 所对应的 键值, Boolean 类型
		 *
		 * @param key 键名
		 * @return key 所标定的 值 Optional
		 */
		default Optional<Boolean> boolOpt(final String key) {
			return Optional.ofNullable(this.bool(key));
		}

		/**
		 * 对键 key 应用函数 mapper
		 *
		 * @param <T>    键值类型
		 * @param <U>    结果类型
		 * @param key    键名
		 * @param mapper 键值变换函数 t->u
		 * @return 对键 key 应用函数 mapper
		 */
		@SuppressWarnings("unchecked")
		default <T, U> U invoke(final String key, Function<T, U> mapper) {
			return mapper.apply((T) this.get(key));
		}

		/**
		 * 值变换
		 *
		 * @param <T>    映射结果类型
		 * @param mapper this-&gt;t
		 * @return T 类型的结果
		 */
		default <T> T mutate(final Function<IRecord, T> mapper) {
			return mapper.apply(this);
		}

		/**
		 * IRecord 变换
		 * 
		 * @param <T>    结果类型
		 * @param mapper [(k,v)]->t , 若 mapper 为 null 则返回 this
		 * @return T 类型结果
		 */
		@SuppressWarnings("unchecked")
		default <T> T mutate2(final Function<? super Map<?, ?>, T> mapper) {
			return mapper == null ? (T) this : mapper.apply(this.toMap());
		}

		/**
		 * 智能版的数组转换 <br>
		 * 视键值对儿kvp的值为单值类型(非集合类型[比如List,Set,HashMap等]),比如 <br>
		 * Integer,Long,Double等，把当前集合中的值集合转换成 一维数组<br>
		 * <p>
		 * 使用示例：<br>
		 * IRecord.rb("name,birth,marry") // 档案结构 <br>
		 * .get("zhangsan,19810713,20011023".split(",")) // 构建张三的数据记录 <br>
		 * .arrayOf("birth,marry",IRecord::asLocalDate, // 把 出生日期和结婚日期转换为日期类型 <br>
		 * ldts->ldts[0].until(ldts[1]).getYears()); // 计算张三的结婚年龄 <br>
		 *
		 * @param <T>    数组的元素类型
		 * @param <U>    mapper 目标元素的类型
		 * @param mapper [t]->u 数组变换函数
		 * @return U类型结果
		 */
		@SuppressWarnings("unchecked")
		default <T, U> U arrayOf(final Function<T[], U> mapper) {
			final Object[] oo = this.toArray(e -> e);
			U u = null;
			try {
				u = mapper.apply((T[]) oo);
			} catch (final Exception e) {
				// do nothing
			} // try

			return u;
		}

		/**
		 * 把 IRecord 值转换成 数组结构
		 *
		 * @param <X>    mapper 参数类型
		 * @param <T>    mapper 值类型
		 * @param mapper 值的变换函数 x->t
		 * @return T类型的一维数组
		 */
		@SuppressWarnings("unchecked")
		default <X, T> T[] toArray(final Function<X, T> mapper) {
			final List<Class<?>> classes = this.tupleS().map(x -> mapper.apply((X) x._2)).filter(Objects::nonNull)
					.map(e -> (Class<Object>) e.getClass()).distinct().collect(Collectors.toList());
			final Class<?> clazz = classes.size() > 1 // 类型不唯一
					? classes.stream().allMatch(Number.class::isAssignableFrom) // 数字类型
							? Number.class // 数字类型
							: Object.class // 节点类型
					: classes.get(0); // 类型唯一
			return this.tupleS().map(x -> mapper.apply((X) x._2)).toArray(n -> (T[]) Array.newInstance(clazz, n));
		}

		/**
		 * 带有键值对儿间隔数组 [key1,value1,key2,value2]
		 *
		 * @param generator 数组生成器
		 * @param <T>       数组元素类型
		 * @return 数组
		 */
		default <T> T[] toArray2(final IntFunction<T[]> generator) {
			return this.tupleS().flatMap(e -> Arrays.stream(e.toArray(Object[]::new))).toArray(generator);
		}

		/**
		 * 带有键值对儿间隔数组 [key1,value1,key2,value2]
		 *
		 * @return 数组
		 */
		default Object[] toArray2() {
			return this.toArray2(Object[]::new);
		}

		/**
		 * 肯定过滤
		 *
		 * @param predicate 谓词判断函数 tuple2->boolean
		 * @return 新生的IRecord
		 */
		default IRecord filter(final Predicate<Tuple2<String, Object>> predicate) {
			return this.tupleS().filter(predicate).collect(IRecord.recclc(e -> e));
		}

		/**
		 * 肯定过滤
		 *
		 * @param keys 键名列表
		 * @return 新生的IRecord
		 */
		default IRecord filter(final List<String> keys) {
			return this.filter(e -> keys.contains(e._1));
		}

		/**
		 * 肯定过滤
		 *
		 * @param keys 键名列表
		 * @return 新生的IRecord
		 */
		default IRecord filter(String keys[]) {
			return this.filter(Arrays.asList(keys));
		}

		/**
		 * 肯定过滤
		 *
		 * @param keys 键名列表，用英文逗号分隔
		 * @return 新生的IRecord
		 */
		default IRecord filter(final String keys) {
			return this.filter(Arrays.asList(keys.split("[,]+")));
		}

		/**
		 * 否定过滤
		 *
		 * @param predicate 谓词判断函数 tuple2->boolean
		 * @return 新生的IRecord
		 */
		default IRecord filterNot(final Predicate<Tuple2<String, Object>> predicate) {
			return this.filter(e -> !predicate.test(e));
		}

		/**
		 * 否定过滤
		 *
		 * @param keys 键名列表
		 * @return 新生的IRecord
		 */
		default IRecord filterNot(final List<String> keys) {
			return this.filterNot(e -> keys.contains(e._1));
		}

		/**
		 * 否定过滤
		 *
		 * @param keys 键名列表
		 * @return 新生的IRecord
		 */
		default IRecord filterNot(String keys[]) {
			return this.filterNot(Arrays.asList(keys));
		}

		/**
		 * 否定过滤
		 *
		 * @param keys 键名列表 用英文逗号分隔
		 * @return 新生的IRecord
		 */
		default IRecord filterNot(final String keys) {
			return this.filterNot(Arrays.asList(keys.split("[,]+")));
		}

		/**
		 * Haskell Functor : 改变键名,也改变键值
		 *
		 * @param mapper (k,v) -> (k1,v1)
		 * @return IRecord
		 */
		default IRecord fmap(final BiFunction<String, Object, Tuple2<String, Object>> mapper) {
			return this.tupleS().map(e -> mapper.apply(e._1, e._2)).collect(IRecord.recclc());
		}

		/**
		 * Haskell Functor : 不改变键名,只改变键值
		 *
		 * @param mapper value -> new_value
		 * @return 变换后者IRecord
		 */
		default IRecord fmap(final Function<Object, Object> mapper) {
			return this.tupleS().map(e -> Tuple2.of(e._1, mapper.apply(e._2))).collect(IRecord.recclc());
		}

		/**
		 * Haskell Functor : 不改变键名,只改变键值
		 *
		 * @param mapper (k,v) -> new_value
		 * @return IRecord
		 */
		default IRecord fmap1(final BiFunction<String, Object, Object> mapper) {
			return this.tupleS().collect(IRecord.recclc(e -> e.fmap2(x -> mapper.apply(e._1, x))));
		}

		/**
		 * 重新计算(不管是否存在)
		 *
		 * @param <T>    值类型1
		 * @param <V>    值类型2
		 * @param key    健名
		 * @param mapper 健名映射 t->v
		 * @return T 类型的结果
		 */
		@SuppressWarnings("unchecked")
		default <T, V> V compute(final String key, final Function<T, V> mapper) {
			final T t = (T) this.get(key);
			final V v = mapper.apply(t);
			this.set(key, v);
			return v;
		}

		/**
		 * 重新计算(不管是否存在)
		 *
		 * @param <T>    值类型1
		 * @param <V>    值类型2
		 * @param index  健名
		 * @param mapper 健名映射 t->v
		 * @return T 类型的结果
		 */
		default <T, V> V compute(final int index, final Function<T, V> mapper) {
			return this.compute(this.keyOf(index), mapper);
		}

		/**
		 * 重新计算(不管是否存在)
		 *
		 * @param <T>    值类型1
		 * @param <V>    值类型2
		 * @param key    健名
		 * @param mapper 健名映射 (s,t)->v
		 * @return T 类型的结果
		 */
		@SuppressWarnings("unchecked")
		default <T, V> V compute(final String key, final BiFunction<String, T, V> mapper) {
			final T t = (T) this.get(key);
			final V v = mapper.apply(key, t);
			this.set(key, v);
			return v;
		}

		/**
		 * 重新计算
		 *
		 * @param <T>    值类型1
		 * @param <V>    值类型2
		 * @param index  健名索引
		 * @param mapper 健名映射 (s,t)->v
		 * @return T 类型的结果
		 */
		default <T, V> V compute(final int index, final BiFunction<String, T, V> mapper) {
			return this.compute(index, mapper);
		}

		/**
		 * 不存在则计算
		 *
		 * @param <T>    值类型
		 * @param key    健名
		 * @param mapper 健名映射 k->t
		 * @return T 类型的结果
		 */
		@SuppressWarnings("unchecked")
		default <T> T computeIfAbsent(final String key, final Function<String, T> mapper) {
			return this.opt(key).map(e -> (T) e).orElseGet(() -> {
				final T value = mapper.apply(key);
				this.add(key, value);
				return value;
			});
		}

		/**
		 * 不存在则计算
		 *
		 * @param <T>    值类型
		 * @param idx    键名索引，从0开始
		 * @param mapper 健名映射 k->t
		 * @return T 类型的结果
		 */
		default <T> T computeIfAbsent(final Integer idx, final Function<String, T> mapper) {
			return this.computeIfAbsent(this.keyOf(idx), mapper);
		}

		/**
		 * 不存在则计算
		 *
		 * @param <T>    mapper 结果类型
		 * @param key    健名
		 * @param mapper 健名映射 k->t
		 * @return T 类型的结果
		 */
		default <T> T computeIfPresent(final String key, final Function<String, T> mapper) {
			return this.opt(key).map(v -> {
				final T t = mapper.apply(key);
				this.add(key, t);
				return t;
			}).orElse(null);
		}

		/**
		 * 不存在则计算
		 *
		 * @param <T>    mapper 结果类型
		 * @param idx    键名索引，从0开始
		 * @param mapper 健名映射 k->t
		 * @return T 类型的结果
		 */
		default <T> T computeIfPresent(final Integer idx, final Function<String, T> mapper) {
			return this.computeIfPresent(this.keyOf(idx), mapper);
		}

		/**
		 * Haskell Functor
		 *
		 * @param mapper (k,v)->obj
		 * @return IRecord
		 */
		default IRecord update(final BiFunction<String, Object, Object> mapper) {
			return this.fmap((k, v) -> new Tuple2<>(k, mapper.apply(k, v)));
		}

		/**
		 * Indicates whether some other object is "equal to" this one.
		 * <p>
		 * The {@code equals} method implements an equivalence relation on non-null
		 * object references:
		 * <ul>
		 * <li>It is <i>reflexive</i>: for any non-null reference value {@code x},
		 * {@code x.equals(x)} should return {@code true}.
		 * <li>It is <i>symmetric</i>: for any non-null reference values {@code x} and
		 * {@code y}, {@code x.equals(y)} should return {@code true} if and only if
		 * {@code y.equals(x)} returns {@code true}.
		 * <li>It is <i>transitive</i>: for any non-null reference values {@code x},
		 * {@code y}, and {@code z}, if {@code x.equals(y)} returns {@code true} and
		 * {@code y.equals(z)} returns {@code true}, then {@code x.equals(z)} should
		 * return {@code true}.
		 * <li>It is <i>consistent</i>: for any non-null reference values {@code x} and
		 * {@code y}, multiple invocations of {@code x.equals(y)} consistently return
		 * {@code true} or consistently return {@code false}, provided no information
		 * used in {@code equals} comparisons on the objects is modified.
		 * <li>For any non-null reference value {@code x}, {@code x.equals(null)} should
		 * return {@code false}.
		 * </ul>
		 * <p>
		 * The {@code equals} method for class {@code Object} implements the most
		 * discriminating possible equivalence relation on objects; that is, for any
		 * non-null reference values {@code x} and {@code y}, this method returns
		 * {@code true} if and only if {@code x} and {@code y} refer to the same object
		 * ({@code x == y} has the value {@code true}).
		 * <p>
		 * Note that it is generally necessary to override the {@code hashCode} method
		 * whenever this method is overridden, so as to maintain the general contract
		 * for the {@code hashCode} method, which states that equal objects must have
		 * equal hash codes.
		 *
		 * @param rec the reference object with which to compare.
		 * @return {@code true} if this object is the same as the obj argument;
		 *         {@code false} otherwise.
		 * @see #hashCode()
		 * @see java.util.HashMap
		 */
		default boolean equals(final IRecord rec) {
			return this == rec || this.compareTo(rec) == 0;
		}

		/**
		 * IRececord 之间的比较大小,比较的keys选择当前对象的keys,当 null 小于任何值,即null值会排在牵头
		 */
		@Override
		default int compareTo(final IRecord o) {

			if (o == null) {
				return 1;
			} else if (this.keys().equals(o.keys())) {
				return IRecord.cmp(this.keys()).compare(this, o);
			} else {
				final Set<String> hashSet = new LinkedHashSet<String>(this.keys());
				hashSet.addAll(o.keys());// 归并
				final String[] kk = hashSet.stream().sorted().toArray(String[]::new);
				System.err.println(
						"比较的两个键名序列(" + this.keys() + "," + o.keys() + ")不相同,采用归并键名序列进行比较:" + Arrays.deepToString(kk));
				return IRecord.cmp(kk, true).compare(this, o);
			} // if
		}

		/**
		 * IRecord Builder
		 *
		 * @return rb
		 */
		default Builder rb() {
			return IRecord.rb(this.keys());
		}

		/**
		 * 生成一个 同结构的对象,健名使用新值
		 *
		 * @param <T>        占位符类型：name1类型，
		 * @param <U>        占位符类型：alias_name1 类型
		 * @param name_pairs [(name1,alias_name1),(name2,alias_name2)]
		 * @return 生成一个 同结构的对象,健名使用新值 {alias_name1,alias_name2}
		 */
		default <T, U> IRecord alias(final Iterable<Tuple2<T, U>> name_pairs) {
			return StreamSupport.stream(name_pairs.spliterator(), false)
					.map(e -> Tuple2.of(e._2 + "", this.get(e._1 + ""))).collect(IRecord.recclc());
		}

		/**
		 * 生成一个 同结构的对象,健名使用新值
		 *
		 * @param kvps 健名映射序列, name1,alias_name1,name2,alias_name2 ...
		 * @return 生成一个 同结构的对象,健名使用新值 {alias_name1,alias_name2}
		 */
		default IRecord alias(final Object... kvps) {
			return this.alias(REC(kvps));
		}

		/**
		 * 生成一个 同结构的对象,健名使用新值
		 *
		 * @param kvps 健名映射序列, name1,alias_name1,name2,alias_name2 ... , 键名间采用英文 逗号',',
		 *             分号';'分隔
		 * @return 生成一个 同结构的对象,健名使用新值 {alias_name1,alias_name2}
		 */
		default IRecord alias(final String kvps) {
			return this.alias((Object[]) kvps.split("[,;]+"));
		}

		/**
		 * json 字符串
		 * 
		 * @return json 字符串
		 */
		default String json() {
			return JSON.toJson(this);
		}

		/**
		 * 元素个数
		 *
		 * @return 元素个数
		 */
		default int size() {
			return this.toMap().size();
		}

		/**
		 * Record 搜集器
		 * 
		 * @param <A>       累加器的元素 类型 中间结果类型,用于暂时存放 累加元素的中间结果的集合。
		 * @param <R>       返回结果类型
		 * @param collector 搜集 KVPair&lt;String,Object&gt;类型的 搜集器
		 * @return 规约的结果 R
		 */
		default <A, R> R collect(final Collector<Tuple2<String, Object>, A, R> collector) {
			return this.tupleS().collect(collector);
		}

		/**
		 * 比较器,需要 键名序列keys中的每个值对象带有比较能力:Comparable
		 *
		 * @param keys 键名序列
		 * @param asc  是否升序,true 表示升序,小值在前,false 表示降序,大值在前
		 * @return keys 序列的比较器
		 */
		static Comparator<IRecord> cmp(final String[] keys, final boolean asc) {
			return cmp(keys, null, asc);
		}

		/**
		 * 比较器,需要 键名序列keys中的每个值对象带有比较能力:Comparable
		 *
		 * @param keys 键名序列
		 * @return keys 序列的比较器
		 */
		static Comparator<IRecord> cmp(final List<String> keys) {
			return IRecord.cmp(keys.toArray(new String[0]), true);
		}

		/**
		 * 比较器,需要 键名序列keys中的每个值对象带有比较能力:Comparable
		 *
		 * @param <T>    元素类型
		 * @param <U>    具有比较能力的类型
		 * @param keys   键名序列
		 * @param mapper (key:键名,t:键值)->u 比较能力变换器
		 * @param asc    是否升序,true 表示升序,小值在前,false 表示降序,大值在前
		 * @return keys 序列的比较器
		 */
		@SuppressWarnings("unchecked")
		static <T, U extends Comparable<?>> Comparator<IRecord> cmp(final String[] keys,
				final BiFunction<String, T, U> mapper, final boolean asc) {

			final BiFunction<String, T, U> final_mapper = mapper == null
					? (String i, T o) -> o instanceof Comparable ? (U) o : (U) (o + "")
					: mapper;

			return (a, b) -> {
				final Queue<String> queue = new LinkedList<String>();
				for (String k : keys)
					queue.offer(k);// 压入队列
				while (!queue.isEmpty()) {
					final String key = queue.poll(); // 提取队首元素
					final Comparable<Object> ta = (Comparable<Object>) a.invoke(key,
							(T t) -> final_mapper.apply(key, t));
					final Comparable<Object> tb = (Comparable<Object>) b.invoke(key,
							(T t) -> final_mapper.apply(key, t));

					if (ta == null && tb == null)
						return 0;
					else if (ta == null)
						return -1;
					else if (tb == null)
						return 1;
					else {
						int ret = 0;

						try {
							ret = ta.compareTo(tb);// 进行元素比较
						} catch (final Exception e) {
							final String[] aa = Stream.of(ta, tb)
									.map(o -> o != null ? o.getClass().getName() + o : "null").toArray(String[]::new);
							ret = aa[0].compareTo(aa[1]);// 进行元素比较
						} // try

						if (ret != 0) {
							return (asc ? 1 : -1) * ret; // 返回比较结果,如果不相等直接返回,相等则继续比计较
						} // if
					} // if
				} // while

				return 0;// 所有key都比较完毕,则认为两个元素相等
			};
		}

		/**
		 * 生成 构建器
		 *
		 * @param n     键数量,正整数
		 * @param keyer 键名生成器 (i:从0开始)->key
		 * @return IRecord 构造器
		 */
		public static Builder rb(final int n, Function<Integer, ?> keyer) {

			final List<Object> keys = new ArrayList<Object>();
			for (int i = 0; i < n; i++) {
				keys.add(keyer.apply(i));
			}
			return new Builder(keys);
		}

		/**
		 * 生成 构建器
		 *
		 * @param <T>  元素类型
		 * @param keys 键名序列
		 * @return keys 为格式的 构建器
		 */
		@SafeVarargs
		public static <T> Builder rb(final T... keys) {
			final List<String> _keys = Arrays.asList(keys).stream().map(e -> e + "").collect(Collectors.toList());
			return new Builder(_keys);
		}

		/**
		 * 生成 构建器
		 *
		 * @param keys 键名序列, 用半角逗号 “,” 分隔
		 * @return keys 为格式的 构建器
		 */
		public static Builder rb(final String keys) {

			return rb(keys.split(","));
		}

		/**
		 * 生成 构建器
		 *
		 * @param keys 键名序列
		 * @return keys 为格式的 构建器
		 */
		public static Builder rb(final String[] keys) {

			return rb(Arrays.asList(keys));
		}

		/**
		 * 生成 构建器
		 *
		 * @param <T>  键名列表元素类型
		 * @param keys 键名序列
		 * @return keys 为格式的 构建器
		 */
		public static <T> Builder rb(final Iterable<T> keys) {

			return new Builder(keys);
		}

		/**
		 * 括号版的替换,括号与数字键不能出现空格, 即 ${ 0 } 是非法的, 只有 ${0} 才是合法的 <br>
		 * fill_template 的别名<br>
		 * IRecord.FT2("${0}+${1}=${2}",1,2,3) 替换后的结果为 1+2=3 <br>
		 * 健名使用${} 括起来避免对于模式串里的 "$11"出现混淆 是 $1后跟1 还是 就是$11的情况 <br>
		 *
		 * @param <T>      参数列表元素类型
		 * @param template 模版字符串，占位符${0},${1},${2},...
		 * @param tt       模版参数序列
		 * @return template 被模版参数替换后的字符串
		 */
		@SafeVarargs
		public static <T> String FT2(final String template, final T... tt) {
			final IRecord rec = IRecord.rb(tt.length, i -> "${" + i + "}").get(tt);
			return fill_template(template, rec);
		}

		/**
		 * fill_template 的别名<br>
		 * IRecord.FT("$0+$1=$2",1,2,3) 替换后的结果为 1+2=3
		 *
		 * @param <T>      参数列表元素类型
		 * @param template 模版字符串，占位符$0,$1,$2,...
		 * @param tt       模版参数序列
		 * @return template 被模版参数替换后的字符串
		 */
		@SafeVarargs
		public static <T> String FT(final String template, final T... tt) {
			final IRecord rec = IRecord.rb(tt.length, i -> "$" + i).get(tt);
			return fill_template(template, rec);
		}

		/**
		 * fill_template 的别名<br>
		 * 把template中的占位符用rec中对应名称key的值value给予替换,默认 非null值会用 单引号'括起来，<br>
		 * 对于使用$开头/结尾的占位符,比如$name或name$等不采用单引号括起来。<br>
		 * 还有对于 数值类型的值value不论占位符/key是否以$开头或结尾都不会用单引号括起来 。<br>
		 * 例如 : FT("insert into tblname$ (name,sex) values
		 * (#name,#sex)",REC("tblname$","user","#name","张三","#sex","男")) <br>
		 * 返回: insert into user (name,sex) values ('张三','男') <br>
		 *
		 * @param template           模版字符串
		 * @param placeholder2values 关键词列表:占位符/key以及与之对应的值value集合
		 * @return 把template中的占位符/key用placeholder2values中的值value给予替换
		 */
		public static String FT(final String template, final IRecord placeholder2values) {
			return fill_template(template, placeholder2values);
		}

		/**
		 * fill_template 的别名<br>
		 * 把template中的占位符用rec中对应名称key的值value给予替换,默认 非null值会用 单引号'括起来，<br>
		 * 对于使用$开头/结尾的占位符,比如$name或name$等不采用单引号括起来。<br>
		 * 还有对于 数值类型的值value不论占位符/key是否以$开头或结尾都不会用单引号括起来 。<br>
		 * 例如 : FT("insert into tblname$ (name,sex) values
		 * (#name,#sex)",REC("tblname$","user","#name","张三","#sex","男")) <br>
		 * 返回: insert into user (name,sex) values ('张三','男') <br>
		 *
		 * @param template           模版字符串
		 * @param placeholder2values 关键词列表:占位符/key以及与之对应的值value集合
		 * @param flag               是否采用单引号括起来
		 * @return 把template中的占位符/key用placeholder2values中的值value给予替换
		 */
		public static String FT(final String template, final IRecord placeholder2values, final boolean flag) {
			return fill_template(template, placeholder2values, flag);
		}

		/**
		 * fill_template 的别名<br>
		 *
		 * @param line     模板字符串
		 * @param pattern  字段模式
		 * @param replacer 变换函数 The function to be applied to the match result of this
		 *                 matcher that returns a replacement string.
		 * @return 填充后的字符串
		 */
		static String FT(final String line, final String pattern, final Function<MatchResult, String> replacer) {
			return IRecord.FT(line, Pattern.compile(pattern), replacer);
		}

		/**
		 * fill_template 的别名<br>
		 *
		 * @param line     模板字符串
		 * @param pattern  字段模式
		 * @param replacer 变换函数 The function to be applied to the match result of this
		 *                 matcher that returns a replacement string.
		 * @return 填充后的字符串
		 */
		static String FT(final String line, final Pattern pattern, final Function<MatchResult, String> replacer) {
			final StringBuilder sb = new StringBuilder();
			try {
				final Matcher matcher = pattern.matcher(line);
				final List<MatchResult> results = new ArrayList<>();
				while (matcher.find()) {
					results.add(matcher.toMatchResult());
				}

				int i = 0;
				for (final MatchResult result : results) {
					while (i < result.start()) {
						sb.append(line.charAt(i++));
					}
					final String e = replacer.apply(result);
					sb.append(e);
					i = result.end();
				} //

				while (i < line.length()) {
					sb.append(line.charAt(i++));
				} //
			} catch (final Exception e) {
				// e.printStackTrace();
				sb.append(line);
			}

			return sb.toString();
		}

		/**
		 * 把template中的占位符用rec中对应名称key的值value给予替换,默认 非null值会用 单引号'括起来，<br>
		 * 对于使用$开头/结尾的占位符,比如$name或name$等不采用单引号括起来,<br>
		 * 还有对于 数值类型的值value不论占位符/key是否以$开头或结尾都不会用单引号括起来 。<br>
		 * 例如 : fill_template("insert into tblname$ (name,sex) values
		 * (#name,#sex)",REC("tblname$","user","#name","张三","#sex","男")) <br>
		 * 返回: insert into user (name,sex) values ('张三','男') <br>
		 *
		 * @param template           模版字符串
		 * @param placeholder2values 关键词列表:占位符/key以及与之对应的值value集合
		 * @return 把template中的占位符/key用placeholder2values中的值value给予替换
		 */
		public static String fill_template(final String template, final IRecord placeholder2values) {
			return fill_template(template, placeholder2values, false);
		}

		/**
		 * 把template中的占位符用rec中对应名称key的值value给予替换,默认 非null值会用 单引号'括起来，<br>
		 * 对于使用$开头/结尾的占位符,比如$name或name$等不采用单引号括起来,<br>
		 * 还有对于 数值类型的值value不论占位符/key是否以$开头或结尾都不会用单引号括起来 。<br>
		 * 例如 : fill_template("insert into tblname$ (name,sex) values
		 * (#name,#sex)",REC("tblname$","user","#name","张三","#sex","男")) <br>
		 * 返回: insert into user (name,sex) values ('张三','男') <br>
		 *
		 * @param template           模版字符串
		 * @param placeholder2values 关键词列表:占位符/key以及与之对应的值value集合
		 * @param flag               是否采用单引号括起来
		 * @return 把template中的占位符/key用placeholder2values中的值value给予替换
		 */
		public static String fill_template(final String template, final IRecord placeholder2values,
				final boolean flag) {

			if (placeholder2values == null) { // 空值判断
				return template;
			}

			final int len = template.length(); // 模版的长度
			final StringBuilder buffer = new StringBuilder();// 工作缓存
			final List<String> keys = placeholder2values.keys().stream().sorted((a, b) -> -(a.length() - b.length())) // 按照从长到短的顺序进行排序。以保证keys的匹配算法采用的是贪婪算法。
					.collect(Collectors.toList());// 键名 也就是template中的占位符号。
			final Map<Object, List<String>> firstCharMap = keys.stream()
					.collect(Collectors.groupingBy(key -> key.charAt(0)));// keys的首字符集合，这是为了加快
			// 读取的步进速度
			int i = 0;// 当前读取的模版字符位置,从0开始。

			while (i < len) {// 从前向后[0,len)的依次读取模板字符串的各个字符
				// 注意:substring(0,0) 或是 substring(x,x) 返回的是一个长度为0的字符串 "",
				// 特别是,当 x大于等于字符串length会抛异常:StringIndexOutOfBoundsException
				final String line = template.substring(0, i);// 业已读过的模版字符串内容[0,i),当前所在位置为i等于line.length
				String placeholder = null;// 占位符号 的内容
				final List<String> kk = firstCharMap.get(template.charAt(i));// 以 i为首字符开头的keys
				if (kk != null) {// 使用firstCharMap加速步进速度读取模版字符串
					for (final String key : kk) {// 寻找 可以被替换的key
						final int endIndex = line.length() + key.length(); // 终点索引 exclusive
						final boolean b = endIndex > len // 是否匹配到placeholder
								? false // 拼装的组合串(line+key) 超长（超过模板串的长度)
								: template.substring(line.length(), endIndex).equals(key); // 当前位置之后刚好存在一个key模样的字符串
						if (b) { // 定位到一个完整的占位符：长度合适（endIndex<=len) && 内容匹配 equals
							placeholder = key; // 提取key作为占位符
							break; // 确定了占位符 ,跳出本次i位置的kk循环。
						} // if b 定位到一个完整的占位符
					} // for key:kk
				} // if kk!=null

				if (placeholder != null) {// 已经发现了占位符号，用rec中的值给予替换
					boolean isnumberic = false;// 默认不是数字格式
					final Object value = placeholder2values.get(placeholder); // 提取占位符内容
					if (flag || placeholder.startsWith("$") || placeholder.endsWith("$"))
						isnumberic = true; // value 是否是 强制 使用 数字格式，即用$作为前/后缀。
					if (value instanceof Number)
						isnumberic = true; // 如果值是数字类型就一定会不加引号的。即采用数字格式

					buffer.append((isnumberic || value == null) ? value + "" : "'" + value + "'"); // 为字符串添加引号，数字格式或是null不加引号
					i += placeholder.length();// 步进placeholder的长度，跳着前进
				} else {// 发现了占位符号则附加当前的位置的字符
					buffer.append(template.charAt(i));
					i++;// 后移一步
				} // if placeholder
			} // while

			return buffer.toString(); // 返回替换后的结果值
		}

		/**
		 * 构建一个键名键值序列 指定的 IRecord
		 *
		 * @param <T> 参数列表元素类型
		 * @param kvs Map结构（IRecord也是Map结构） 或是 键名,键值 序列。即 build(map) 或是
		 *            build(key0,value0,key1,vlaue1,...) 的 形式， 特别注意 build(map) 时候，当且仅当
		 *            kvs 的只有一个元素，即 build(map0,map1) 会被视为 键值序列
		 * @return 新生成的IRecord
		 */
		@SafeVarargs
		public static <T> IRecord REC(final T... kvs) {
			return MyRecord.REC(kvs); // 采用 MyRecord 来作为IRecord的默认实现函数
		}

		/**
		 * 把一个数据对象转换为浮点数<br>
		 * 对于 非法的数字类型 返回 defaultValue
		 * <p>
		 * 默认会尝试把时间类型也解释为数字,即 '1970-01-01 08:00:01' <br>
		 * 也会被转换成一个 0时区 的 从1970年1月1 即 epoch time 以来的毫秒数<br>
		 * 对于 中国 而言 位于+8时区, '1970-01-01 08:00:01' 会被解析为1000
		 *
		 * @param <T>          函数的参数类型
		 * @param defaultValue 非法的数字类型 返回 的默认值
		 * @return t->dbl
		 */
		static <T> Function<T, Double> obj2dbl(final Number defaultValue) {
			return (T obj) -> {
				if (obj instanceof Number) {
					return ((Number) obj).doubleValue();
				}

				Double dbl = Optional.ofNullable(defaultValue).map(Number::doubleValue).orElse(null);
				try {
					dbl = Double.parseDouble(obj.toString());
				} catch (final Exception e) { //
					// do nothing
				} // try

				return dbl;
			};
		}

		/**
		 * 把一个值对象转换成LocalDateTime
		 *
		 * @param value 值对象
		 * @return LocalDateTime
		 */
		static LocalDateTime asLocalDateTime(final Object value) {
			final Function<LocalDate, LocalDateTime> ld2ldt = ld -> LocalDateTime.of(ld, LocalTime.of(0, 0));
			final Function<LocalTime, LocalDateTime> lt2ldt = lt -> LocalDateTime.of(LocalDate.of(0, 1, 1), lt);
			final Function<Long, LocalDateTime> lng2ldt = lng -> {
				final Long timestamp = lng;
				final Instant instant = Instant.ofEpochMilli(timestamp);
				final ZoneId zoneId = ZoneId.systemDefault();
				return LocalDateTime.ofInstant(instant, zoneId);
			};

			final Function<Timestamp, LocalDateTime> timestamp2ldt = timestamp -> {
				return lng2ldt.apply(timestamp.getTime());
			};

			final Function<Date, LocalDateTime> dt2ldt = dt -> {
				return lng2ldt.apply(dt.getTime());
			};

			final Function<String, LocalTime> str2lt = line -> {
				LocalTime lt = null;
				for (String format : "HH:mm:ss,HH:mm,HHmmss,HHmm,HH".split("[,]+")) {
					try {
						lt = LocalTime.parse(line, DateTimeFormatter.ofPattern(format));
					} catch (final Exception ex) {
						// do nothing
					}
					if (lt != null)
						break;
				}
				return lt;
			};

			final Function<String, LocalDate> str2ld = line -> {
				LocalDate ld = null;
				for (String format : "yyyy-MM-dd,yyyy-M-d,yyyy/MM/dd,yyyy/M/d,yyyyMMdd".split("[,]+")) {
					try {
						ld = LocalDate.parse(line, DateTimeFormatter.ofPattern(format));
					} catch (final Exception ex) {
						// do nothing
					}
					if (ld != null)
						break;
				}

				return ld;
			};

			final Function<String, LocalDateTime> str2ldt = line -> {
				LocalDateTime ldt = null;
				final String patterns = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSSSS," //
						+ "yyyy-MM-dd'T'HH:mm:ss.SSSSSS," //
						+ "yyyy-MM-dd'T'HH:mm:ss.SSS," //
						+ "yyyy-MM-dd'T'HH:mm:ss," //
						+ "yyyy-MM-ddTHH:mm:ss.SSSSSSSSS," //
						+ "yyyy-MM-ddTHH:mm:ss.SSSSSS," //
						+ "yyyy-MM-ddTHH:mm:ss.SSS," //
						+ "yyyy-MM-ddTHH:mm:ss," //
						+ "yyyy-MM-dd HH:mm:ss," //
						+ "yyyy-MM-dd HH:mm," //
						+ "yyyy-MM-dd HH," //
						+ "yyyy-M-d H:m:s," //
						+ "yyyy-M-d H:m," //
						+ "yyyy-M-d H," //
						+ "yyyy/MM/dd HH:mm:ss," //
						+ "yyyy/MM/dd HH:mm," //
						+ "yyyy/MM/dd HH," //
						+ "yyyy/M/d H:m:s," //
						+ "yyyy/M/d H:m," //
						+ "yyyy/M/d H," //
						+ "yyyyMMddHHmmss," //
						+ "yyyyMMddHHmm," //
						+ "yyyyMMddHH"//
				; // patterns 时间的格式字符串

				for (String format : patterns.split("[,]+")) {
					try {
						ldt = LocalDateTime.parse(line, DateTimeFormatter.ofPattern(format));
					} catch (final Exception ex) {
						// do nothing
					}
					if (ldt != null)
						break;
				}

				return ldt;
			};

			if (value instanceof LocalDateTime) {
				return (LocalDateTime) value;
			} else if (value instanceof LocalDate) {
				return ld2ldt.apply((LocalDate) value);
			} else if (value instanceof LocalTime) {
				return lt2ldt.apply((LocalTime) value);
			} else if (value instanceof Number) {
				return lng2ldt.apply(((Number) value).longValue());
			} else if (value instanceof Timestamp) {
				return timestamp2ldt.apply(((Timestamp) value));
			} else if (value instanceof Date) {
				return dt2ldt.apply(((Date) value));
			} else if (value instanceof String) {
				final String line = (String) value;
				final LocalDateTime _ldt = str2ldt.apply(line);
				if (Objects.nonNull(_ldt)) {
					return _ldt;
				}
				final LocalDate _ld = str2ld.apply(line);
				if (Objects.nonNull(_ld)) {
					return ld2ldt.apply(_ld);
				}
				final LocalTime _lt = str2lt.apply(line);
				if (Objects.nonNull(_lt)) {
					return lt2ldt.apply(_lt);
				}
				return null;
			} else {
				return null;
			}
		}

		/**
		 * Record类型的T元素归集器
		 *
		 * @param <T>    元素类型
		 * @param <U>    元组的1#位置占位符元素类型
		 * @param mapper Tuple2 类型的元素生成器 t->(str,u)
		 * @return IRecord类型的T元素归集器
		 */
		static <T, U> Collector<T, ?, IRecord> recclc(final Function<T, Tuple2<String, U>> mapper) {
			return Collector.of((Supplier<List<T>>) ArrayList::new, List::add, (left, right) -> {
				left.addAll(right);
				return left;
			}, (ll) -> { // finisher
				final IRecord rec = REC(); // 空对象
				ll.stream().map(mapper).forEach(rec::add);
				return rec;
			}); // Collector.of
		}

		/**
		 * Record类型的T元素归集器
		 *
		 * @param <T> 元组值类型
		 * @return IRecord类型的T元素归集器
		 */
		static <T> Collector<Tuple2<String, T>, ?, IRecord> recclc() {
			return IRecord.recclc(e -> e);
		}

		/**
		 * 滑动窗口的T元素归集器
		 *
		 * @param <T>  流元素类型
		 * @param size 窗口长度 大于0的整数
		 * @param step 移动步长 大于0的整数 <br>
		 *             flag 是否返回齐次窗口，true:齐次窗口,false:非齐次窗口
		 * @return 滑动窗口的T元素归集器
		 */
		public static <T> Collector<T, ?, Stream<List<T>>> slidingclc(final int size, final int step) {
			return IRecord.slidingclc(size, step, true, e -> e.stream());
		}

		/**
		 * 滑动窗口的T元素归集器
		 *
		 * @param <T>  流元素类型
		 * @param size 窗口长度 大于0的整数
		 * @param step 移动步长 大于0的整数
		 * @param flag 是否返回齐次窗口，true:齐次窗口,false:非齐次窗口
		 * @return 滑动窗口的T元素归集器
		 */
		public static <T> Collector<T, ?, Stream<List<T>>> slidingclc(final int size, final int step,
				final boolean flag) {
			return IRecord.slidingclc(size, step, flag, e -> e.stream());
		}

		/**
		 * 滑动窗口的T元素归集器
		 *
		 * @param <T>      流元素类型
		 * @param <U>      结果归集器的结果类型，即窗口集合的数据类型，比如 把 List&lt;List&lt;T&gt; 变换
		 *                 List&lt;DFrame&gt; 这样的类型
		 * @param size     窗口长度 大于0的整数
		 * @param step     移动步长 大于0的整数
		 * @param flag     是否返回齐次窗口，true:齐次窗口,false:非齐次窗口
		 * @param finisher 最终结果处理器,比如 把 List&lt;List&lt;T&gt;&gt; 变换 List&lt;DFrame&gt;
		 *                 这样的类型函数
		 * @return 滑动窗口的T元素归集器，归集成U类型的窗口集合
		 */
		public static <T, U> Collector<T, ?, U> slidingclc(final int size, final int step, final boolean flag,
				final Function<List<List<T>>, U> finisher) {
			return IRecord.slidingclc(size, step, flag, finisher, e -> e);
		}

		/**
		 * 滑动窗口的T元素归集器
		 *
		 * @param <F>      帧框类型
		 * @param <T>      流元素类型
		 * @param <U>      结果归集器的结果类型，即窗口集合的数据类型，比如 把 List&lt;List&lt;T&gt; 变换
		 *                 List&lt;DFrame&gt; 这样的类型
		 * @param size     窗口长度 大于0的整数
		 * @param step     移动步长 大于0的整数
		 * @param flag     是否返回齐次窗口，true:齐次窗口,false:非齐次窗口
		 * @param finisher 最终结果处理器,比如 把 List&lt;List&lt;T&gt;&gt; 变换成 List&lt;F&gt;
		 *                 这样的类型函数
		 * @param as_frame 帧框变换函数 把 List&lt;T&gt;&gt; 变换成 F 类型的 帧框
		 * @return 滑动窗口的T元素归集器，归集成U类型的窗口集合
		 */
		public static <F, T, U> Collector<T, ?, U> slidingclc(final int size, final int step, final boolean flag,
				final Function<List<F>, U> finisher, final Function<List<T>, F> as_frame) {
			final AtomicInteger ai = new AtomicInteger(0);
			return Collector.of(() -> new ArrayList<List<T>>(), (wnds, a) -> {
				if (ai.getAndIncrement() % step == 0) { // 达到步长位置则添加一个数据窗口
					wnds.add(new ArrayList<>()); // 添加一个数据窗口
				}
				final int n = wnds.size(); // 结果集合的长度
				for (int i = n - 1; i >= 0; i--) { // 从后前向依次遍历 追加数据元素，直到长度达到 窗口长度 size 位置
					final List<T> wnd = wnds.get(i); // 窗口数据
					// 由于是从后向前，所以一旦遇到长度等于要求长度的窗口,则停止倒退(向前滑行)，因为前面也都是size 长度的
					if (wnd.size() == size) { // 窗口长度达到要求，停止倒退，结束返回。
						break;
					} else { //
						wnd.add(a);
					} // if
				} // for
			}, (aa, bb) -> {
				try {
					throw new Exception("slidingclc 不支持并发 的流处理模式！");
				} catch (final Exception e) {
					e.printStackTrace();
				}
				aa.addAll(bb);
				return aa;
			}, aa -> {
				return Optional.of(flag).map(e -> { // flag 的判断
					if (!e) { // 非齐整模式
						return aa;
					} else { // 齐整模式
						int i = aa.size() - 1; // 末尾元素的下标索引
						while (i > 0) {
							if (aa.get(i).size() >= size) { // 直到现在子列表长度等于size的的最大下标索引
								break;
							} else { // 长度小于窗口长度size 的继续向前移动
								i--;
							} // if
						} // while

						final List<List<T>> homo_aa = i == aa.size() - 1 ? aa : aa.subList(0, i + 1); // 提取齐整的列表集合
						return homo_aa; // 齐整列表
					} // if
				}).map(ll -> {
					final List<F> frames = ll.stream().map(as_frame).collect(Collectors.toList());
					return finisher.apply(frames);
				}).get(); // of
			}); // Collector.of
		}

		/**
		 * 分组归集器
		 *
		 * @param <X>   元素数据类型
		 * @param <K>   键名类型
		 * @param keyer 键名函数,分类规则&amp;依据 x-&gt;key
		 * @return Map:{(K,U)}
		 */
		public static <X, K> Collector<X, ?, Map<K, List<X>>> grpclc2(final Function<X, K> keyer) {

			return grpclc(keyer, e -> e, e -> e, e -> e);
		}

		/**
		 * 分组归集器
		 *
		 * @param <X>     元素数据类型
		 * @param <K>     键名类型
		 * @param <V>     键值类型
		 * @param <U>     中间结果类型
		 * @param keyer   键名函数,分类规则&amp;依据 x-&gt;key
		 * @param valueer 键值创建函数 x-&gt;value
		 * @param uclc    键值元素集合包装函数 vv-&gt;u
		 * @return Map:{(K,U)}
		 */
		public static <X, K, V, U> Collector<X, ?, Map<K, U>> grpclc2(final Function<X, K> keyer,
				final Function<X, V> valueer, final Collector<V, ?, U> uclc) {

			return IRecord.grpclc(keyer, valueer, uclc, e -> e);
		}

		/**
		 * 分组归集器
		 *
		 * @param <X>        元素数据类型
		 * @param <K>        键名类型
		 * @param <V>        键值类型
		 * @param <U>        中间结果类型
		 * @param keyer      键名函数,分类规则&amp;依据 x-&gt;key
		 * @param valueer    键值创建函数 x-&gt;value
		 * @param u_finisher 键值元素集合包装函数 vv-&gt;u
		 * @return Map:{(K,U)}
		 */
		public static <X, K, V, U> Collector<X, ?, Map<K, U>> grpclc2(final Function<X, K> keyer,
				final Function<X, V> valueer, final Function<List<V>, U> u_finisher) {

			return IRecord.grpclc(keyer, valueer, u_finisher, e -> e);
		}

		/**
		 * 分组归集器
		 *
		 * @param <X>        元素数据类型
		 * @param <K>        键名类型
		 * @param <Z>        最终结果类型
		 * @param keyer      键名函数,分类规则&amp;依据 x-&gt;key
		 * @param z_finisher 最终结果包装函数 [(k,v)]->z
		 * @return Z类型的结果
		 */
		public static <X, K, Z> Collector<X, ?, Z> grpclc(final Function<X, K> keyer,
				final Function<Map<K, List<X>>, Z> z_finisher) {

			return grpclc(keyer, e -> e, e -> e, z_finisher);
		}

		/**
		 * 分组归集器
		 *
		 * @param <X>        元素数据类型
		 * @param <K>        键名类型
		 * @param <V>        键名值
		 * @param <Z>        最终结果类型
		 * @param keyer      键名函数,分类规则&amp;依据 x-&gt;key
		 * @param valuerer   键值函数 x->value
		 * @param z_finisher 最终结果包装函数 [(k,v)]->z
		 * @return Z类型的结果
		 */
		public static <X, K, V, Z> Collector<X, ?, Z> grpclc(final Function<X, K> keyer, final Function<X, V> valuerer,
				final Function<Map<K, List<V>>, Z> z_finisher) {

			return grpclc(keyer, valuerer, e -> e, z_finisher);
		}

		/**
		 * 分组归集器
		 *
		 * @param <X>        元素数据类型
		 * @param <K>        键名类型
		 * @param <V>        键值类型
		 * @param <U>        中间结果类型
		 * @param <Z>        结果类型
		 * @param keyer      键名函数,分类规则&amp;依据 x-&gt;key
		 * @param valueer    键值创建函数 x-&gt;value
		 * @param uclc       键值元素集合归集器 vv-&gt;u
		 * @param z_finisher 键值元素集合包装函数 [(k,v)]-&gt;z
		 * @return U 结果类型
		 */
		public static <X, K, V, U, Z> Collector<X, ?, Z> grpclc(final Function<X, K> keyer,
				final Function<X, V> valueer, final Collector<V, ?, U> uclc, final Function<Map<K, U>, Z> z_finisher) {

			return IRecord.grpclc(keyer, valueer, ll -> ll.stream().collect(uclc), z_finisher);
		}

		/**
		 * 分组归集器
		 *
		 * @param <X>        元素数据类型
		 * @param <K>        键名类型
		 * @param <V>        键值类型
		 * @param <U>        中间结果类型
		 * @param <Z>        结果类型
		 * @param keyer      键名函数,分类规则&amp;依据 x-&gt;key
		 * @param valueer    键值创建函数 x-&gt;value
		 * @param u_finisher 键值元素集合包装函数 vv-&gt;u
		 * @param z_finisher 键值元素集合包装函数 [(k,v)]-&gt;z
		 * @return U 结果类型
		 */
		public static <X, K, V, U, Z> Collector<X, ?, Z> grpclc(final Function<X, K> keyer,
				final Function<X, V> valueer, final Function<List<V>, U> u_finisher,
				final Function<Map<K, U>, Z> z_finisher) {

			final Collector<X, LinkedHashMap<K, List<V>>, Z> clc = Collector.of( // 创建轨迹器
					(Supplier<LinkedHashMap<K, List<V>>>) () -> new LinkedHashMap<K, List<V>>(),
					(LinkedHashMap<K, List<V>> lhm, X x) -> {
						final K k = keyer.apply(x);
						final V v = valueer.apply(x);
						lhm.computeIfAbsent(k, _k -> new ArrayList<V>()).add(v);
					}, (tuples1, tuples2) -> {
						tuples1.putAll(tuples2);
						return tuples1;
					}, lhm -> { // 最终类型包装器
						final LinkedHashMap<K, U> lhm1 = new LinkedHashMap<K, U>();

						lhm.entrySet().stream().forEach(entry -> {
							final K key = entry.getKey(); // 键名
							final U u = u_finisher.apply(entry.getValue()); // 值类型归集
							lhm1.put(key, u); // 值类型
						});

						final Z z = z_finisher.apply(lhm1);

						return z;
					}); // Collector.of

			return clc;
		}

		/**
		 * 数据透视表的归集结构
		 *
		 * @param keys 归集的键名序列,键名之间采用半全角的逗号活分号即"[,，;；]+" 进行分隔。
		 * @return 数据透视表
		 */
		public static Collector<IRecord, ?, IRecord> pvtclc(final String keys) {
			return IRecord.pvtclc(e -> e, keys.split("[,，;；]+"));
		}

		/**
		 * 数据透视表的归集结构
		 *
		 * @param keys 归集的键名序列,keys 长度为0则统一归key类别之下
		 * @return 数据透视表
		 */
		public static Collector<IRecord, ?, IRecord> pvtclc(final String... keys) {
			return IRecord.pvtclc(e -> e, keys);
		}

		/**
		 * 数据透视表的归集结构 <br>
		 * 把 [IRecord] 集合数据 归集到 由 keys 所标识的 路径集合中，并 调用evaluator 做集合数据额演算。
		 *
		 * @param <T>       路径集合的函数的计算值类型
		 * @param evaluator 路径集合计算函数 ([r]:IRecord列表)->t
		 * @param keys      归集的键名序列,键名间采用英文半角','分隔,keys 长度为0则统一归key类别之下
		 * @return 数据透视表
		 */
		public static <T> Collector<IRecord, ?, IRecord> pvtclc(final Function<List<IRecord>, T> evaluator,
				final String keys) {
			return IRecord.pvtclc(evaluator, keys == null ? new String[0] : keys.split("[,]+"));
		}

		/**
		 * 数据透视表的归集结构 <br>
		 * 把 [IRecord] 集合数据 归集到 由 keys 所标识的 路径集合中，并 调用evaluator 做集合数据额演算。
		 *
		 * @param <T>       路径集合的函数的计算值类型
		 * @param evaluator 路径集合计算函数 ([r]:IRecord列表)->t
		 * @param keys      归集的键名序列,keys 长度为0则统一归key类别之下
		 * @return 数据透视表
		 */
		public static <T> Collector<IRecord, ?, IRecord> pvtclc(final Function<List<IRecord>, T> evaluator,
				final String... keys) {
			if (keys.length > 1 && keys[0] != null) { // 键名序列长度大于0,递归归集成嵌套的IRecord
				final String[] restings = Arrays.copyOfRange(keys, 1, keys.length); // 剩余的键名
				return IRecord.grpclc((IRecord r) -> r.str(keys[0].trim()), e -> e, pvtclc(evaluator, restings),
						IRecord::REC); // 递归归集
			} else if (keys.length > 0 && keys[0] != null) { // 键名序列长度为1,键值归集成列表
				return IRecord.grpclc((IRecord r) -> r.str(keys[0].trim()), e -> e, evaluator, IRecord::REC);
			} else { // 没有键名序列,统一归key类别之下
				return IRecord.grpclc((IRecord r) -> "key", e -> e, evaluator, IRecord::REC);
			} // if
		}

		/**
		 * Map类型的T元素归集器
		 *
		 * @param <K>    键类型
		 * @param <T>    元素类型
		 * @param <U>    元组的1#位置占位符元素类型
		 * @param mapper Tuple2 类型的元素生成器 t->(k,u)
		 * @return IRecord类型的T元素归集器
		 */
		static <T, K, U> Collector<T, ?, Map<K, List<U>>> mapclc(final Function<T, Tuple2<K, U>> mapper) {
			return Collector.of((Supplier<Map<K, List<U>>>) HashMap::new, (tt, t) -> {
				final Tuple2<K, U> tup = mapper.apply(t);
				tt.computeIfAbsent(tup._1, _k -> new ArrayList<>()).add(tup._2);
			}, (left, right) -> {
				left.putAll(right);
				return left;
			}, (tt) -> { // finisher
				return tt;
			}); // Collector.of
		}

		/**
		 * Map类型的T元素归集器
		 *
		 * @param <K>    键类型
		 * @param <T>    元素类型
		 * @param <U>    元组的1#位置占位符元素类型
		 * @param mapper Tuple2 类型的元素生成器 t->(k,u)
		 * @param biop   二元运算算子 (u,u)->u
		 * @return IRecord类型的T元素归集器
		 */
		static <T, K, U> Collector<T, ?, Map<K, U>> mapclc2(final Function<T, Tuple2<K, U>> mapper,
				final BinaryOperator<U> biop) {
			return Collector.of((Supplier<Map<K, List<U>>>) HashMap::new, (tt, t) -> {
				final Tuple2<K, U> tup = mapper.apply(t);
				tt.computeIfAbsent(tup._1, _k -> new ArrayList<>()).add(tup._2);
			}, (left, right) -> {
				left.putAll(right);
				return left;
			}, (tt) -> { // finisher
				Map<K, U> map = new LinkedHashMap<K, U>();
				tt.forEach((k, uu) -> {
					final U u = uu.stream().reduce(biop).orElse(null);
					map.put(k, u);
				});
				return map;
			}); // Collector.of
		}

		/**
		 * Map类型的T元素归集器
		 *
		 * @param <K>    键类型
		 * @param <T>    元素类型
		 * @param <U>    元组的1#位置占位符元素类型
		 * @param mapper Tuple2 类型的元素生成器 t->(k,u)
		 * @return IRecord类型的T元素归集器
		 */
		static <T, K, U> Collector<T, ?, Map<K, U>> mapclc2(final Function<T, Tuple2<K, U>> mapper) {
			return IRecord.mapclc2(mapper, (a, b) -> b); // 使用新值覆盖老值。
		}

		/**
		 * 集合并集(c1包含 或者 c2包含)
		 *
		 * @param <T> 元素类型
		 * @param c1  集合1
		 * @param c2  集合2
		 * @return 集合并集
		 */
		@SuppressWarnings("unchecked")
		public static <T> LinkedHashSet<T> union(final Collection<T> c1, final Collection<T> c2) {
			return (LinkedHashSet<T>) Stream.concat(c1.stream(), c2.stream()).distinct()
					.collect(Collector.of(LinkedHashSet::new, LinkedHashSet::add, (aa, bb) -> {
						aa.add(bb);
						return aa;
					}, aa -> aa));
		}

		/**
		 * 集合交集(c1 包含 并且 c2也包含)
		 *
		 * @param <T> 元素类型
		 * @param c1  集合1
		 * @param c2  集合2
		 * @return 集合交集
		 */
		@SuppressWarnings("unchecked")
		public static <T> LinkedHashSet<T> intersect(final Collection<T> c1, final Collection<T> c2) {
			return (LinkedHashSet<T>) c1.stream().filter(c2::contains)
					.collect(Collector.of(LinkedHashSet::new, LinkedHashSet::add, (aa, bb) -> {
						aa.add(bb);
						return aa;
					}, aa -> aa));
		}

		/**
		 * 集合差集 (c1 包含 并且 c2不包含)
		 *
		 * @param <T> 元素类型
		 * @param c1  集合1
		 * @param c2  集合2
		 * @return 集合差集
		 */
		@SuppressWarnings("unchecked")
		public static <T> LinkedHashSet<T> diff(final Collection<T> c1, final Collection<T> c2) {
			return (LinkedHashSet<T>) c1.stream().filter(e -> !c2.contains(e))
					.collect(Collector.of(LinkedHashSet::new, LinkedHashSet::add, (aa, bb) -> {
						aa.add(bb);
						return aa;
					}, aa -> aa));
		}

		/**
		 * list comprehend
		 *
		 * @param <T> 元素类型
		 * @param tts 批次层级数据,[tt1,tt2,...,tt_i,...] 每个批次 tt_i 都是一个 [t]
		 * @return 流引用，元素为缓存引用
		 */
		@SuppressWarnings("unchecked")
		public static <T> Stream<T[]> cph(final T[]... tts) {
			final AtomicInteger ai = new AtomicInteger(0); // 阶层偏移
			final Class<?> tclass = tts.getClass().getComponentType().getComponentType(); // tt 的元素的类型
			final T[] buffer = (T[]) Array.newInstance(tclass, tts.length); // 工作缓存,从根节点向叶子节点逐层递进
			return Stream.of(tts).reduce(Stream.of((T) null), (parentS, tt) -> {
				final int i = ai.getAndIncrement(); // level 阶层位置，注意一定要用非对象类型:int而非Integer
				return parentS.flatMap(parent -> Stream.of(tt).map(t -> buffer[i] = t)); // 反腐刷新工作缓存输出流
			}, (a, b) -> a).map(e -> buffer); // 输出缓存状态
		}

		/**
		 * list comprehend
		 *
		 * @param <T> 元素类型
		 * @param tts 批次层级数据,[tt1,tt2,...,tt_i,...] 每个批次 tt_i 都是一个 [t]
		 * @return 流引用，元素为缓存引用
		 */
		@SuppressWarnings("unchecked")
		public static <T> Stream<T[]> cph(final Iterable<T[]> tts) {
			final T[] sample = tts.iterator().next();
			final T[][] _tts = StreamSupport.stream(tts.spliterator(), false)
					.toArray(n -> (T[][]) Array.newInstance(sample.getClass(), n));
			return cph(_tts);
		}

		/**
		 * 强制转换函数
		 *
		 * @param <T>   源类型
		 * @param <U>   目标类型
		 * @param unull U类型的占位符
		 * @return 把t类型转换成X类型的函数
		 */
		@SuppressWarnings("unchecked")
		public static <T, U> Function<T, U> nullas(final U unull) {
			return t -> (U) t;
		}

		/**
		 * 数组元素
		 *
		 * @param <T> 元素類型
		 * @param ts  元素序列
		 * @return T[]
		 */
		@SafeVarargs
		public static <T> T[] A(final T... ts) {
			return ts;
		}

		/**
		 * 列表元素
		 *
		 * @param <T> 元素類型
		 * @param ts  元素序列
		 * @return T[]
		 */
		@SafeVarargs
		public static <T> List<T> L(final T... ts) {
			return Arrays.asList(ts);
		}

		/**
		 * repeat array <br>
		 * 复制元素t,生成数组 n 个元素的数组
		 *
		 * @param <T> 数组元素类型
		 * @param t   数组元素
		 * @param n   负责次数
		 * @return T[]
		 */
		@SuppressWarnings("unchecked")
		public static <T> T[] RPTA(final T t, final int n) {
			final Class<T> clazz = (Class<T>) t.getClass();
			final T[] tt = (T[]) Array.newInstance(clazz, n);
			for (int i = 0; i < n; i++)
				tt[i] = t;
			return tt;
		}

		/**
		 * 随机生成的数据元素
		 *
		 * @param <T> 元素类型
		 * @param ts  元素列表
		 * @return 随机生成的数据元素
		 */
		@SafeVarargs
		public static <T> T rndget(T... ts) {
			return ts[new Random().nextInt(ts.length)];
		}

	}

	/**
	 * 托管的流
	 *
	 * @author gbench
	 */
	interface IManagedStreams {

		/**
		 * 获取托管数据流的集合,返回 活动流的 引用。
		 *
		 * @return 托管数据流的集合
		 */
		Set<Stream<?>> getActiveStreams();

		/**
		 * 把所有的活动的流都给予清空
		 */
		default void clear() {

			this.getActiveStreams().forEach(Stream::close); // 关闭活动流
			this.getActiveStreams().clear();// 活动流的库存清空
		}

		/**
		 * 把所有的活动的流都给予清空 清空指定的 流对象
		 *
		 * @param stream 指定的流对象
		 */
		default void clear(final Stream<?> stream) {

			if (this.getActiveStreams().contains(stream)) {
				this.getActiveStreams().remove(stream);
			}
			stream.close();
		}

		/**
		 * 托管一个流对象
		 *
		 * @param stream 托管的流对象
		 * @return IManagedStreams 本身 便于 实现链式编程
		 */
		default IManagedStreams add(final Stream<?> stream) {

			this.getActiveStreams().add(stream);
			return this;
		}

		/**
		 * 把所有的活动的流都给予清空
		 * 
		 * @return IManagedStreams 本身 便于 实现链式编程
		 */
		default IManagedStreams dump() {

			this.getActiveStreams().forEach(e -> {
				System.out.println(e);
			});

			return this;
		}

	}

	/**
	 * 会话接口
	 * <p>
	 * 接口api 带有2的版本 抛出异常,不带有2的不抛出异常 <br>
	 * 比如: sql2executeS 抛异常,sql2executeS 不抛异常
	 *
	 * @param <D> 数据类型
	 * @param <X> 归集烈性
	 * @author xuqinghua
	 */
	public interface IJdbcSession<D, X> extends IManagedStreams {

		/**
		 * 数据库连接
		 *
		 * @return Connection
		 */
		Connection getConnection();

		/**
		 * 会话归集器
		 *
		 * @return Collector
		 */
		Collector<IRecord, ?, X> collectorX();

		/**
		 * 会话数据
		 *
		 * @return D
		 */
		D getData();

		/**
		 * 会话数据
		 *
		 * @param data 会话数据
		 * @return D
		 */
		D setData(final D data);

		/**
		 * 会话属性
		 *
		 * @return 会话属性
		 */
		IRecord getAttributes();

		/**
		 * 判断数据库表是否存在 <br>
		 * 表是否存在（需要注意，此处没有指定catalog，也就是默认tblname是哦正式数据库服务器中唯一，若是需要查询特定服务事情使用,带有catalog的版本）
		 * 该API会出现在A数据库中不存在而补B数据库中存在的table被误认为A数据库存在的情况，所以catalog必要时还需要给予指定
		 * 
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
				final var metadata = tables.getMetaData();
				final var ncol = metadata.getColumnCount();
				final var keys = new ArrayList<String>();
				for (var j = 0; j < ncol; j++) {
					keys.add(metadata.getColumnName(j + 1));
				}
				final var rb = IRecord.rb(keys);
				final var lines = new ArrayList<IRecord>(10);
				while (tables.next()) {
					Object[] values = new Object[ncol];
					for (var j = 0; j < ncol; j++) {
						values[j] = tables.getObject(j + 1);
					} // for
					lines.add(rb.get(values));
				} // while

				@SuppressWarnings("unused")
				final var debugline = "tableName:%s,schema:%s,catalog:%s,lines:%s" //
						.formatted(tableName, schema, catalog, lines); // 数据debug信息行
				// println(debugline);
				flag = lines.size() > 0;
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
		 * X 查询
		 *
		 * @param path        连接列表
		 * @param flds        字段列表
		 * @param tail_clause 尾部从句
		 * @return X 结果类型
		 * @throws SQLException SQLException
		 */
		default X path2x(final String path, final String flds, final String tail_clause) throws SQLException {
			final String head_clause = SqlBuilder.LEFTJOIN(path, flds); // 头部从句
			final String sql = IRecord.FT("$0 $1", head_clause,
					tail_clause == null || tail_clause.matches("^\\s*$") ? "" : tail_clause);
			// println(sql);
			final X x = this.sql2u(sql, null, this.collectorX());

			return x;
		}

		/**
		 * X 查询
		 *
		 * @param path 连接列表
		 * @param flds 字段列表
		 * @return X 结果类型
		 * @throws SQLException SQLException
		 */
		default X path2x(final String path, final String flds) throws SQLException {

			final X x = this.path2x(path, flds, null);

			return x;
		}

		/**
		 * X 查询
		 *
		 * @param sql sql 语句
		 * @return X 结果类型，比如 DFrame 由 this.collectorX 定义
		 * @throws SQLException SQLException
		 */
		default X sql2x(final String sql) throws SQLException {

			final X x = this.sql2u(sql, null, this.collectorX());
			return x;
		}

		/**
		 * X 查询
		 *
		 * @param sb SQL 语句
		 * @return X 结果类型
		 * @throws SQLException SQLException
		 */
		default X sql2x(final SqlBuilder sb) throws SQLException {

			final X x = this.sql2x(sb.toString());
			return x;
		}

		/**
		 * sql查询
		 * 
		 * @param <U>        结果类型
		 * @param sqlpattern sqlpattern SQL 语句模式
		 * @param params     参数泪飙
		 * @param collector  collector 归集器
		 * @return u 类型结果
		 * @throws SQLException SQLException
		 */
		default <U> U sql2u(final String sqlpattern, final IRecord params, final Collector<IRecord, ?, U> collector)
				throws SQLException {

			final U result = this.sql2recordS(IRecord.FT(sqlpattern, params)).collect(collector);
			return result;
		}

		/**
		 * SQL语句查询
		 * 
		 * @param <U>       结果类型
		 * @param sql       sql语句
		 * @param collector collector 归集器
		 * @return u 类型结果
		 * @throws SQLException SQLException
		 */
		default <U> U sql2u(final String sql, final Collector<IRecord, ?, U> collector) throws SQLException {

			return this.sql2u(sql, REC(), collector);
		}

		/**
		 * 执行sql语句查询出结果集合。<br>
		 * 一般不会尝试调用spp 进行sql解析,但是 当sql为"#开头的namedsql的时候，会调用spp给予解析") <br>
		 * sql2records 的核心函数 <br>
		 *
		 * <br>
		 * 对于短路的流，注意调用 stream.close() 来释放数据库连接, 或者 是 调用 sess.clear 来给与清空。<br>
		 *
		 * @param sql sql 语句
		 * @return IRecord类型的流
		 * @throws SQLException SQLException
		 */
		default Stream<IRecord> sql2recordS(final String sql) throws SQLException {

			final String _sql = sql.trim(); // 去除多余的首尾空格
			return this.psql2recordS(_sql, (Map<Integer, Object>) null);
		}

		/**
		 * 执行sql语句查询出结果集合。<br>
		 * 一般不会尝试调用spp 进行sql解析,但是 当sql为"#开头的namedsql的时候，会调用spp给予解析") <br>
		 * sql2records 的核心函数 <br>
		 *
		 * <br>
		 * 对于短路的流，注意调用 stream.close() 来释放数据库连接, 或者 是 调用 sess.clear 来给与清空。<br>
		 *
		 * @param sb sql 语句
		 * @return IRecord类型的流
		 * @throws SQLException SQLException
		 */
		default Stream<IRecord> sql2recordS(final SqlBuilder sb) throws SQLException {

			return this.sql2executeS(sb.toString());
		}

		/**
		 * 使用连接path查询数据 path 表连接简化哈寻 a t1 left join b t2 on t1.id = t2.id 等价于 a
		 * t1;t1.id=t2.id;b t2
		 *
		 * <br>
		 * 对于短路的流，注意调用 stream.close() 来释放数据库连接, 或者 是 调用 sess.clear 来给与清空。<br>
		 *
		 * @param path 例如，例如连接路径：path： t_user u;id=userid;t_user_role
		 *             ur;roleid=id;t_role r 语句
		 * @param flds 返回列
		 * @return IRecord类型的流
		 * @throws SQLException SQLException
		 */
		default Stream<IRecord> sql2recordS(final String path, final String flds) throws SQLException {

			return this.sql2recordS(SqlBuilder.LEFTJOIN(path, flds));
		}

		/**
		 * sql2updateS
		 *
		 * @param sql sql 语句
		 * @return IRecord 的 数据流
		 * @throws SQLException SQLException
		 */
		default Stream<IRecord> sql2updateS(final String sql) throws SQLException {
			return this.psql2updateS(sql, null);
		}

		/**
		 * sql2updateS 的别名
		 *
		 * @param sql sql 语句
		 * @return IRecord 的 数据列表
		 * @throws SQLException SQLException
		 */
		default Stream<IRecord> sql2executeS(final String sql) throws SQLException {
			return this.sql2updateS(sql);
		}

		/**
		 * sql2updateS 的列表形式
		 *
		 * @param sql sql 语句
		 * @return IRecord 的 数据列表
		 * @throws SQLException SQLException
		 */
		default List<IRecord> sql2execute(final String sql) throws SQLException {
			return this.sql2updateS(sql).collect(Collectors.toList());
		}

		/**
		 * 查询结果集合
		 *
		 * @param sql sql 语句
		 * @return 查询结果集合
		 * @throws SQLException SQLException
		 */
		default IRecord sql2one(final String sql) throws SQLException {
			return this.sql2maybe(sql).orElse(null);
		}

		/**
		 * 查询类:查询结果集合
		 *
		 * @param sql sql 语句
		 * @return 查询结果集合
		 * @throws SQLException SQLException
		 */
		default Optional<IRecord> sql2maybe(final String sql) throws SQLException {
			return this.sql2recordS(sql).findFirst();
		}

		/**
		 * sql2executeS 语法糖 <br>
		 * 执行类:execute版本的sql2maybe<br>
		 * 采用sql2executeS提供基础实现
		 *
		 * @param sql sql 语句
		 * @return execute的返回结果，一般用于获取自增长的id主键
		 * @throws SQLException SQLException
		 */
		default Optional<IRecord> sql2maybe2(final String sql) throws SQLException {
			return this.sql2executeS(sql).findFirst();
		}

		/**
		 * 没有异常抛出版本的数据查询
		 *
		 * @param sql 数据查询语句
		 * @return X 类型的结果
		 */
		default X sqlquery(final String sql) {
			X x = null;
			try {
				x = this.sql2x(sql);
			} catch (SQLException e) {
				e.printStackTrace();
			}
			return x;
		}

		/**
		 * sql2executeS 语法糖 <br>
		 * 执行类:execute版本的sql2maybe<br>
		 * 采用sql2executeS提供基础实现
		 *
		 * @param sql sql 语句
		 * @return execute的返回结果，一般用于获取自增长的id主键
		 */
		default Optional<IRecord> sqlmaybe(final String sql) {
			Optional<IRecord> maybe = Optional.empty();
			try {
				maybe = this.sql2executeS(sql).findFirst();
			} catch (final Exception e) {
				e.printStackTrace();
			}
			return maybe;
		}

		/**
		 * 没有异常抛出版本的sql语句执行
		 *
		 * @param sql 数据查询语句
		 * @return IRecord类型 数据流
		 */
		default Stream<IRecord> sqlexecuteS(final String sql) {
			Stream<IRecord> stream = null;
			try {
				stream = this.sql2executeS(sql);
			} catch (SQLException e) {
				e.printStackTrace();
			}
			return stream;
		}

		/**
		 * 直接查询数据 Pair Data(表头:字符串数组,dataS:行顺序的数据元素流)
		 * 
		 * @param sql 结果集合
		 * @return 结果集数据(列名序列:[s],值序列[d])
		 * @throws SQLException SQLException
		 */
		default Tuple2<String[], Stream<Object>> sql2pdS(final String sql) throws SQLException {
			final AtomicReference<SQLException> ar = new AtomicReference<>();
			Tuple2<String[], Stream<Object>> data = null;
			try {
				data = this.sql2pdS(sql, t -> {
					// final Connection conn = t._1;
					final Statement stmt = t._2._1;
					final ResultSet rs = t._2._2;
					try {
						/**
						 * 需要注意 rs结果集需要在语句stmt之前给予关闭,因为rs依赖于stmt,即:stmt关闭，结果集定然会关闭。
						 */
						if (null != rs && !rs.isClosed()) {
							rs.close();
							if (null != debug) {
								debug.accept(String.format("%s", IRecord.rb("msg").get("rs.close")));
							}
						} // rs

						if (null != stmt && !stmt.isClosed()) {
							stmt.close();
							if (null != debug) {
								debug.accept(String.format("%s", IRecord.rb("msg").get("stmt.close")));
							}
						} // stmt

						// conn.close(); /*连接不予关闭以便在同一个会话中重复使用*/
					} catch (SQLException e) {
						ar.set(e); // 记录内部异常
					}
				});
			} catch (SQLException e) {
				ar.set(e);
			}
			if (ar.get() != null) { // 抛出内部异常
				final IRecord line = REC("name", "sql2dataS", "sql", sql);
				this.getAttributes().add(UUID.randomUUID().toString(), line); // 随机生成异常key
				throw ar.get();
			}
			return data;
		}

		/**
		 * 直接查询数据 Pair Data(表头:字符串数组,dataS:行顺序的数据元素流)
		 * 
		 * @param sql            结果集合
		 * @param close_callback 执行结束的回调函数，比如 关闭 数据集、语句、连接 之类的 收尾操作。
		 * @return 结果集数据(列名序列:[s],值序列[d])
		 * @throws SQLException SQLException
		 */
		default Tuple2<String[], Stream<Object>> sql2pdS(final String sql,
				final SQLExceptionalBiConsumer<Statement, ResultSet> close_callback) throws SQLException {
			return this.sql2pdS(sql, t -> close_callback.accept(t._2._1, t._2._2));
		}

		/**
		 * 直接查询数据 Pair Data(表头:字符串数组,dataS:行顺序的数据元素流)
		 * 
		 * @param sql            结果集合
		 * @param close_callback 执行结束的回调函数，比如 关闭 数据集、语句、连接 之类的 收尾操作。
		 * @return 结果集数据(列名序列:[s],值序列[d])
		 * @throws SQLException SQLException
		 */
		default Tuple2<String[], Stream<Object>> sql2pdS(final String sql,
				final ExceptionalConsumer<Tuple2<Connection, Tuple2<Statement, ResultSet>>> close_callback)
				throws SQLException {
			final Connection conn = this.getConnection();
			final Statement stmt = conn.createStatement();
			final ResultSet rs = stmt.executeQuery(sql);
			final AtomicReference<SQLException> ar = new AtomicReference<SQLException>(); // 内部异常缓存
			final Runnable close = () -> {
				try {
					close_callback.accept(Tuple2.of(conn, Tuple2.of(stmt, rs)));
				} catch (SQLException e) {
					ar.set(e);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}; // close
			final Tuple2<String[], Stream<Object>> data = IJdbcSession.readDataS(rs, t -> close.run());
			this.add(data._2.onClose(close)); // 等级数据流，以便在关闭流的时候触发close_callback
			if (ar.get() != null) {
				throw ar.get();
			}

			return data;
		}

		/**
		 * X 查询(不抛异常)
		 *
		 * @param path        连接列表
		 * @param flds        字段列表
		 * @param tail_clause 尾部从句
		 * @return X 结果类型
		 */
		default X pathx(final String path, final String flds, final String tail_clause) {

			X x = null;
			try {
				x = this.path2x(path, flds, tail_clause);
			} catch (final Exception e) {
				e.printStackTrace();
			}

			return x;
		}

		/**
		 * X 查询(不抛异常)
		 *
		 * @param path 连接列表
		 * @param flds 字段列表
		 * @return X 结果类型
		 */
		default X pathx(final String path, final String flds) {

			X x = null;
			try {
				x = this.path2x(path, flds);
			} catch (final Exception e) {
				e.printStackTrace();
			}

			return x;
		}

		/**
		 * 没有异常抛出版本的sql语句执行
		 *
		 * @param sql 数据查询语句
		 * @return IRecord类型 数据列表
		 */
		default List<IRecord> sqlexecute(final String sql) {
			List<IRecord> ll = null;
			try {
				Stream<IRecord> stream = this.sql2executeS(sql);
				ll = Optional.ofNullable(stream).map(e -> e.collect(Collectors.toList())).orElse(null);
			} catch (SQLException e) {
				e.printStackTrace();
			}
			return ll;
		}

		/**
		 * 查询结果集合
		 *
		 * @param sql    prepared sql 语句
		 * @param params prepared sql 语句中占位符参数的值集合，位置从1开始
		 * @return 查询结果集合
		 * @throws SQLException SQLException
		 */
		default Stream<IRecord> psql2recordS(final String sql, final Map<Integer, ?> params) throws SQLException {
			Stream<IRecord> stream = null;

			try {
				stream = IJdbcSession.psql2recordS(getConnection(), sql, params, SQL_MODE.QUERY, false);
				this.add(stream);
			} catch (final Exception e) {
				final IRecord line = REC("name", "psql2recordS", "sql", sql, "params", params);
				this.getAttributes().add(UUID.randomUUID().toString(), line); // 随机生成异常key
				throw e; // 再次抛出异常
			} // try

			return stream;
		}

		/**
		 * 更新数据:execute 与update 同属于与 update
		 *
		 * @param sql    prepared sql 语句
		 * @param params 占位符的对应值的Map,位置从1开始
		 * @return 更新结果集合函数有 generatedKeys 键值
		 * @throws SQLException SQLException
		 */
		default Stream<IRecord> psql2updateS(final String sql, final Map<Integer, Object> params) throws SQLException {
			Stream<IRecord> stream = null;

			try {
				stream = IJdbcSession.psql2recordS(this.getConnection(), sql, params, SQL_MODE.UPDATE, false);
			} catch (final Exception e) {
				final IRecord line = REC("name", "psql2updateS", "sql", sql, "params", params);
				this.getAttributes().add(UUID.randomUUID().toString(), line); // 随机生成异常key
				throw e;
			} // try

			return stream;
		}

		/**
		 * 生成sql语句．
		 *
		 * @param conn   数据库连接
		 * @param mode   语句的类型 UPDATE ,QUERY_SCROLL
		 * @param sql    语句：含有占位符
		 * @param params sql 参数: {Key:Integer-Value:Object},Key 从1开始。 params
		 *               为null时候不予进行sql语句填充
		 * @return PreparedStatement
		 * @throws SQLException SQLException
		 */
		static PreparedStatement pstmt(final Connection conn, final SQL_MODE mode, final String sql,
				final Map<Integer, ?> params) throws SQLException {

			PreparedStatement ps = null;
			if (mode == SQL_MODE.UPDATE) {
				ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);// 生成数据主键
			} else if (mode == SQL_MODE.QUERY_SCROLL) {
				ps = conn.prepareStatement(sql, ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_READ_ONLY);
			} else {
				ps = conn.prepareStatement(sql);
			}

			final ParameterMetaData pm = ps.getParameterMetaData();// 参数元数据
			try {
				final int pcnt = pm.getParameterCount(); // paramcnt;
				if (pcnt > 0 && params != null && params.size() > 0) {
					for (final Integer paramIndex : params.keySet()) {
						if (pcnt < paramIndex) {
							continue;// 对于超出sql的参数位置范围的 参数给予舍弃
						}

						final Object value = params.get(paramIndex);
						ps.setObject(paramIndex, value);// 设置参数
					} // for
				} // if
			} catch (UnsupportedOperationException e) {
				if (params != null && params.size() > 0) {
					for (final Integer paramIndex : params.keySet()) {
						final Object value = params.get(paramIndex);
						ps.setObject(paramIndex, value);// 设置参数
					} // for
				} // if
			} // try

			return ps;
		}// pstmt

		/**
		 * 提取列标签:数组类型的返回结果
		 *
		 * @param rs 结果集合
		 * @return 标签数组
		 * @throws SQLException SQLException
		 */
		static String[] labels(final ResultSet rs) throws SQLException {

			String[] aa = null;
			if (rs == null) {
				return null;
			}

			final ResultSetMetaData rsm = rs.getMetaData();
			final int n = rsm.getColumnCount();

			aa = new String[n];

			for (int i = 1; i <= n; i++) {
				aa[i - 1] = rsm.getColumnLabel(i);
			}

			return aa;
		}

		/**
		 * 从ResultSet 当前游标位置 中读取一条数据。<br>
		 * 不对结果集合ResultSet做任何改变,游标位置需要事先设置好<br>
		 * 即不会移动ResultSet的cursor。所以第一条数据的时候需要在外部进行rs.next()<br>
		 * Record 的key 采用rs.getMetaData().getColumnLabel(索引）来获取。<br>
		 *
		 * @param rs   结果集合
		 * @param lbls 键名集合
		 * @return 结果集数据的record的表示, 出现异常则返回null
		 */
		static IRecord readline(final ResultSet rs, final String[] lbls) {

			IRecord rec = null;// 默认的非法结果

			try {
				rec = REC();
				final int n = lbls.length;
				for (int i = 0; i < n; i++) {// 读取当前行的各个字段信息。
					final String name = lbls[i]; // 提取键名
					final Object value = rs.getObject(i + 1); // 提取键值
					final Object _value = Optional.ofNullable(value)
							.map(v -> v instanceof Clob ? (Object) clob2str((Clob) v) : null).orElse(value);
					rec.add(name, _value);
				} // for
			} catch (final Exception e) {
				e.printStackTrace();
			} // try

			return rec;
		}

		/**
		 * Clob 转 字符串(Long.MAX_VALUE字节)
		 *
		 * @param clog 字符类大对象
		 * @return 字符串
		 */
		static String clob2str(final Clob clog) {

			return clob2str(clog, null);
		}

		/**
		 * Clob 转 字符串
		 *
		 * @param clog 字符类大对象
		 * @param size 最大字节数量
		 * @return 字符串
		 */
		static String clob2str(final Clob clog, final Long size) {

			if (null == clog) {
				return null;
			} else {
				String line = null;
				try {
					final Reader reader = clog.getCharacterStream();
					final long n = (Math.min(Optional.ofNullable(size).orElse(Long.MAX_VALUE), clog.length()));
					char[] buffer = new char[(int) n];
					reader.read(buffer);
					line = new String(buffer);
				} catch (final Exception e) {
					e.printStackTrace();
				} // try
				return line;
			} // if
		}

		/**
		 * 移除异常信息 ExceptionalConsumer 转 Consumer
		 * 
		 * @param <T> 参数类型
		 * @param cs  回调函数
		 * @return Consumer
		 */
		static <T> Consumer<T> trycatch(final ExceptionalConsumer<T> cs) {

			return t -> {
				try {
					cs.accept(t);
				} catch (final Exception e) {
					e.printStackTrace();
				}
			};
		}

		/**
		 * 移除异常信息 <br>
		 * 
		 * ExceptionalFunction 转 Function
		 * 
		 * @param <T>  参数类型
		 * @param <U>  结果类型
		 * @param func 异常函数类型
		 * @return Function
		 */
		static <T, U> Function<T, U> trycatch(final ExceptionalFunction<T, U> func) {

			return t -> {
				try {
					return func.apply(t);
				} catch (final Exception e) {
					e.printStackTrace();
				}

				return null;
			};// 返回一个Function
		}

		/**
		 * 从当前游标开始 (不包含)依次向后读取数据 <br>
		 * <br>
		 * Record 的key 采用rs.getMetaData().getColumnLabel(索引）来获取。<br>
		 *
		 * @param rs             结果集合
		 * @param close_callback 执行结束的回调函数，比如 关闭 数据集、语句、连接 之类的 收尾操作。
		 * @return 结果集数据的record的流 [rec]
		 * @throws SQLException SQLException
		 */
		static Stream<IRecord> readlineS(final ResultSet rs, final Runnable close_callback) throws SQLException {

			final String[] lbls = IJdbcSession.labels(rs);
			final AtomicBoolean stopflag = new AtomicBoolean(false); // 是否达到末端
			final Stream<IRecord> stream = !rs.next() // 检查是否存在有后继
					? Stream.of() // 空列表
					: Stream.iterate( // 生成流对象
							readline(rs, lbls), // 初始值
							previous -> !stopflag.get(), // 是否到达末端
							previous -> { // next
								return IJdbcSession.trycatch((ResultSet r) -> { // 读取 resultset
									if (r.next()) { // 先移动然后读取
										return readline(r, lbls); // 读取结果集
									} else { // 已经读取到了最后一条数据,返回null
										close_callback.run(); // 执行回调函数
										stopflag.set(true); // 设置结束标志
										return null; // 返回空值
									} // if
								}).apply(rs); // trycatch
							}); // iterate

			return stream;
		}

		/**
		 * 从当前游标开始 (不包含)依次向后读取数据 <br>
		 * <br>
		 * Record 的key 采用rs.getMetaData().getColumnLabel(索引）来获取。<br>
		 *
		 * @param rs             结果集合
		 * @param close_callback 执行结束的回调函数，比如 关闭 数据集、语句、连接 之类的 收尾操作。
		 * @return 结果集数据(列名序列:[s],值序列[d])
		 * @throws SQLException SQLException
		 */
		static Tuple2<String[], Stream<Object>> readDataS(final ResultSet rs,
				final ExceptionalConsumer<IRecord> close_callback) throws SQLException {

			final String[] lbls = IJdbcSession.labels(rs);
			final AtomicBoolean stopflag = new AtomicBoolean(false); // 是否达到末端
			final Supplier<Stream<Object>> readline = () -> Stream.iterate(0, i -> i < lbls.length, i -> i + 1)
					.map(trycatch((Integer i) -> rs.getObject(i + 1)));
			final Stream<Object> stream = !rs.next() // 检查是否存在有后继
					? Stream.of() // 空列表
					: Stream.iterate( // 生成流对象
							readline.get(), // 初始值
							previous -> !stopflag.get(), // 是否到达末端
							previous -> { // next
								return trycatch((ResultSet r) -> { // 读取 resultset
									if (r.next()) { // 先移动然后读取
										return readline.get();
									} else { // 已经读取到了最后一条数据,返回null
										try { // 执行回调函数
											close_callback.accept(IRecord.rb("rs").get(rs));
										} catch (final Exception e) {
											e.printStackTrace();
										} // 执行回调函数
										stopflag.set(true); // 设置结束标志
										return Stream.empty(); // 返回空值
									} // if
								}).apply(rs); // trycatch
							}).flatMap(e -> e); // iterate

			return Tuple2.of(lbls, stream);
		}

		/**
		 * 查询结果集合 <br>
		 * <p>
		 * 对于短路的流，注意调用 stream.close() 来释放数据库连接, 或者 是 调用 sess.clear 来给与清空。<br>
		 *
		 * @param connection 数据库连接
		 * @param sql        查询或更新语句
		 * @param sqlmode    sql的模式，更新还是查询
		 * @param close_conn 结束时候是否关闭 connection 数据连接
		 * @return IRecord 的 数据流 [rec]
		 * @throws SQLException SQLException
		 */
		static Stream<IRecord> psql2recordS(final Connection connection, final String sql, final SQL_MODE sqlmode,
				final boolean close_conn) throws SQLException {
			return IJdbcSession.psql2recordS(connection, sql, null, sqlmode, close_conn);
		}

		/**
		 * 查询结果集合 <br>
		 * <p>
		 * 对于短路的流，注意调用 stream.close() 来释放数据库连接, 或者 是 调用 sess.clear 来给与清空。<br>
		 *
		 * @param connection 数据库连接
		 * @param sql        查询或更新语句
		 * @param params     sql的占位参数, params 为空的时候 不予 进行 sql 语句填充
		 * @param sqlmode    sql的模式，更新还是查询
		 * @param close_conn 结束时候是否关闭 connection 数据连接
		 * @return IRecord 的 数据流 [rec]
		 * @throws SQLException SQLException
		 */
		@SuppressWarnings("unchecked")
		static Stream<IRecord> psql2recordS(final Connection connection, final String sql, final Map<Integer, ?> params,
				final SQL_MODE sqlmode, final boolean close_conn) throws SQLException {
			final AtomicReference<Runnable> ar = new AtomicReference<>(); // 回调关闭函数
			return psql2t(connection, sql, params, sqlmode, close_conn, param -> {
				final Connection conn = param._1;
				final PreparedStatement pstmt = param._2._1;
				final ResultSet rs = param._2._2;
				final Runnable closeHandler = () -> { // 关闭操作的回调函数
					try {
						if (null != rs && !rs.isClosed()) { // 关闭结果集
							if (null != debug) {
								debug.accept(String.format("%s", IRecord.rb("msg").get("rs.close")));
							}
							rs.close();
						} // !rs.isClosed()

						if (null != pstmt && !pstmt.isClosed()) { // 关闭语句集合
							if (null != debug) {
								debug.accept(String.format("%s", IRecord.rb("msg").get("pstmt.close")));
							}
							pstmt.close();
						} // !pstmt.isClosed())

						if (null != conn && close_conn && !conn.isClosed()) { // 关闭数据库连接
							debug.accept(String.format("%s", IRecord.rb("msg").get("conn.close")));
							conn.close();
						} // close_conn && !connection.isClosed()
					} catch (final Exception e) {
						e.printStackTrace();
					} // try
				}; // 关闭操作的回调函数
				ar.set(closeHandler);
				return rs == null // 结果集检查
						? (Stream<IRecord>) (Object) Stream.empty() // 空结果
						: IJdbcSession.readlineS(rs, closeHandler); // 读取数据行
			}).onClose(ar.get()); // 加入关闭处理子
		}

		/**
		 * 查询结果集合 <br>
		 * <p>
		 * 对于短路的流，注意调用 stream.close() 来释放数据库连接, 或者 是 调用 sess.clear 来给与清空。<br>
		 *
		 * @param <T>        结果类型
		 * @param connection 数据库连接
		 * @param sql        查询或更新语句
		 * @param params     sql的占位参数, params 为空的时候 不予 进行 sql 语句填充
		 * @param sqlmode    sql的模式，更新还是查询
		 * @param close_conn 结束时候是否关闭 connection 数据连接
		 * @param mapper     索引参数
		 * @return T 类型的结果
		 * @throws SQLException SQLException
		 */
		static <T> T psql2t(final Connection connection, final String sql, final Map<Integer, ?> params,
				final SQL_MODE sqlmode, final boolean close_conn,
				final SQLExceptionalFunction<Tuple2<Connection, Tuple2<PreparedStatement, ResultSet>>, T> mapper)
				throws SQLException {

			final PreparedStatement pstmt = IJdbcSession.pstmt(connection, sqlmode, sql, params); // 创建查询语句

			ResultSet _rs = null;

			if (sqlmode.equals(SQL_MODE.UPDATE)) { // 更新模式
				int n = pstmt.executeUpdate(); // 批处理的执行
				if (n > 0) { // 1 row count for SQL Data Manipulation Language (DML) statements
					_rs = pstmt.getGeneratedKeys();
				}
				if (_rs == null) {
					_rs = pstmt.getResultSet();
				} // if
			} else if (sqlmode.equals(SQL_MODE.EXECUTE)) { // 执行模式
				final boolean b = pstmt.execute(sql);
				if (b) {
					_rs = pstmt.getResultSet();
				}
			} else { // 非更新模式
				_rs = pstmt.executeQuery();
			} // if // if

			final ResultSet rs = _rs;

			return mapper.apply(Tuple2.of(connection, Tuple2.of(pstmt, rs))); // 加入关闭处理子
		}

	}

	/**
	 * 可以抛出异常的消费函数
	 *
	 * @param <T> 源数据类型
	 * @author gbench
	 */
	@FunctionalInterface
	public interface ExceptionalConsumer<T> {
		/**
		 * 数据消费函数
		 *
		 * @param t 函数参数
		 * @throws Exception 异常
		 */
		void accept(T t) throws Exception;
	}

	/**
	 * 可以抛出异常的消费函数
	 *
	 * @param <T> 源数据类型
	 * @param <U> 源数据类型
	 * @author gbench
	 */
	@FunctionalInterface
	public interface ExceptionalBiConsumer<T, U> {
		/**
		 * 数据消费函数
		 *
		 * @param t 函数参数
		 * @param u 函数参数
		 * @throws Exception 异常
		 */
		void accept(final T t, final U u) throws Exception;
	}

	/**
	 * 带有抛出异常的函数
	 *
	 * @param <T> 参数类型
	 * @param <U> 返回类型
	 * @author xuqinghua
	 */
	public interface ExceptionalFunction<T, U> {
		/**
		 * apply
		 * 
		 * @param t 函数参数
		 * @return U 类型的结果
		 * @throws Exception 异常
		 */
		U apply(final T t) throws Exception;
	}

	/**
	 * 带有抛出异常的函数
	 *
	 * @param <T> 参数类型
	 * @param <U> 返回类型
	 * @author xuqinghua
	 */
	public interface SQLExceptionalFunction<T, U> {

		/**
		 * apply
		 * 
		 * @param t 参数类型
		 * @return U类型的结果
		 * @throws SQLException SQLException
		 */
		U apply(final T t) throws SQLException;
	}

	/**
	 * 可以抛出异常的消费函数
	 *
	 * @param <T> 源数据类型
	 * @param <U> 源数据类型
	 * @author gbench
	 */
	@FunctionalInterface
	public interface SQLExceptionalBiConsumer<T, U> {
		/**
		 * 数据消费函数
		 *
		 * @param t 函数参数
		 * @param u 函数参数
		 * @throws SQLException 异常
		 */
		void accept(final T t, final U u) throws SQLException;
	}

	/**
	 * IRecord 构建器
	 *
	 * @author gbench
	 */
	public static class Builder {

		/**
		 * 构造IRecord构造器
		 *
		 * @param <T>  键名序列类型
		 * @param keys 键名列表的迭代器
		 */
		public <T> Builder(final Iterable<T> keys) {
			this.keys = new ArrayList<>(StreamSupport.stream(keys.spliterator(), false).limit(10000).map(e -> e + "")
					.collect(Collectors.toList()));
		}

		/**
		 * 构建IRecord
		 * 
		 * @param <T> 参数列表元素类型
		 * @param kvs 键值序列 key1,value1,key2,value2,...
		 * @return IRecord 对象
		 */
		@SafeVarargs
		final public <T> IRecord build(final T... kvs) {
			return MyRecord.REC(kvs);
		}

		/**
		 * 构造 IRecord <br>
		 * 按照构建器的 键名序列表，依次把objs中的元素与其适配以生成 IRecord <br>
		 * {key0:objs[0],key1:objs[1],key2:objs[2],...}
		 *
		 * @param <T>  参数列表元素类型
		 * @param objs 值序列, 若 objs 为 null 则返回null, <br>
		 *             若 objs 长度不足以匹配 keys 将采用 循环补位的仿制给予填充 <br>
		 *             若 objs 长度为0则返回一个空对象{},注意是没有元素且不是null的对象
		 * @return IRecord 对象 若 objs 为 null 则返回null
		 */
		@SafeVarargs
		final public <T> IRecord get(final T... objs) {
			if (objs == null) { // 空值判断
				return null;
			}

			final int size = this.keys.size();
			final LinkedHashMap<String, Object> data = new LinkedHashMap<String, Object>();

			Object[] oo = objs; // 参数值
			if (objs.length == 1) { // 单一值的情况
				if (objs[0] instanceof Iterable<?> itr) {
					if (itr instanceof Collection<?> coll) {
						oo = coll.toArray();
					} else {
						oo = StreamSupport.stream(itr.spliterator(), false).limit(MAX_SIZE).toArray();
					} // if
				} else if (objs[0] instanceof Stream) {
					oo = ((Stream<?>) objs[0]).toArray();
				} else {
					// do nothing
				} // if
			} // if

			final int n = oo.length;
			for (int i = 0; n > 0 && i < size; i++) {
				final String key = keys.get(i);
				final Object value = oo[i % n];
				data.put(key, value == null ? "" : value); // key 默认为 ""
			} // for

			for (int i = 0; n > 0 && i < size; i++) {
				final String key = keys.get(i);
				final Object value = oo[i % n];
				data.put(key, value == null ? "" : value); // key 默认为 ""
			} // for

			return this.build(data);
		}

		/**
		 * 健名列表
		 *
		 * @return 健名列表
		 */
		public List<String> keys() {
			return this.keys;
		}

		/**
		 * 健名列表
		 *
		 * @return 健名列表
		 */
		public Stream<String> keyS() {
			return this.keys().stream();
		}

		/**
		 * 健名列表
		 *
		 * @return 健名列表
		 */
		public String[] keyA() {
			return this.toArray();
		}

		/**
		 * 注意这是一个修改Builder的方法。<br>
		 * Inserts the specified element at the specified position in thislist. Shifts
		 * the element currently at that position (if any) andany subsequent elements to
		 * the right (adds one to their indices).
		 * 
		 * @param index at which the specified element is to be inserted
		 * @param key   element to be inserted
		 * @return Builder 对象本身
		 */
		public Builder insert(final int index, final String key) {
			this.keys.add(index, key);
			return this;
		}

		/**
		 * 在头部添加key
		 * 
		 * @param keys 键名序列
		 * @return Builder 对象复制品
		 */
		public Builder prepend(final String... keys) {
			final int this_size = this.keys.size();
			final String[] kk = Arrays.copyOf(keys, keys.length + this_size);
			final int start = keys.length;
			for (int i = 0; i < this_size; i++) {
				kk[i + start] = this.keys.get(i);
			}
			return new Builder(Arrays.asList(kk));
		}

		/**
		 * 尾部追加键名
		 * 
		 * @param keys 键名序列
		 * @return 对象复制品
		 */
		public Builder append(final String... keys) {
			final Builder rb = this.clone();
			rb.keys.addAll(Arrays.asList(keys));
			return rb;
		}

		/**
		 * 构造复制品
		 * 
		 * @return 对象复制品
		 */
		public Builder duplicate() {
			return this.clone();
		}

		/**
		 * 复制品
		 * 
		 * @return 对象复制品
		 */
		@Override
		public Builder clone() {
			return new Builder(this.keys);
		}

		/**
		 * 键列表keys长度
		 * 
		 * @return 键列表keys长度
		 */
		public int size() {
			return this.keys.size();
		}

		/**
		 * 对keys进行变换
		 * 
		 * @param <T>    映射的结果类型
		 * @param mapper keys:[k] -> T 变换函数
		 * @return T 类型的结果
		 */
		public <T> T arrayOf(final Function<String[], T> mapper) {
			return mapper.apply(keys.toArray(String[]::new));
		}

		/**
		 * 键名列表
		 * 
		 * @return keys 的数组结果
		 */
		public String[] toArray() {
			return this.arrayOf(e -> e);
		}

		private ArrayList<String> keys = new ArrayList<>();

	}

	/**
	 * 数据记录对象的实现
	 *
	 * @author gbench
	 */
	public static class MyRecord extends LinkedHashMap<String, Object> implements IRecord, Serializable {

		/**
		 *
		 */
		private static final long serialVersionUID = 1L;

		/**
		 * 构造数据记录对象
		 *
		 * @param data IRecord数据
		 */
		public MyRecord(final Map<?, ?> data) {

			data.forEach((k, v) -> {
				this.put(k instanceof String ? (String) k : k + "", v);
			}); // forEach
		}

		/**
		 * 构造空数据记录
		 */
		public MyRecord() {

			this(new LinkedHashMap<String, Object>());
		}

		@Override
		public IRecord set(final String key, final Object value) {

			this.put(key, value);
			return this;
		}

		@Override
		public IRecord remove(String key) {

			super.remove(key); // 注意使用this.remove 会递归，这里要使用super
			return this;
		}

		@Override
		public IRecord build(final Object... kvs) {

			return MyRecord.REC(kvs);
		}

		@Override
		public Map<String, Object> toMap() {

			return this;
		}

		@Override
		public List<String> keys() {

			return new ArrayList<>(this.keySet());
		}

		/**
		 * 提取指定key的数据
		 */
		@Override
		public Object get(final String key) {

			return super.get(key);
		}

		@Override
		public IRecord duplicate() {

			final MyRecord _rec = new MyRecord();
			_rec.putAll(this);
			return _rec;
		}

		@Override
		public int hashCode() {

			return Objects.hash(this.entrySet());
		}

		@Override
		public Iterator<Tuple2<String, Object>> iterator() {
			return this.entrySet().stream().map(e -> new Tuple2<>(e.getKey(), e.getValue())).iterator();
		}

		/**
		 * Iterable 转 List
		 *
		 * @param <T>      元素类型
		 * @param iterable 可迭代类型
		 * @param maxSize  最大元素长度
		 * @return 元素类型
		 */
		public static <T> List<T> iterable2list(final Iterable<T> iterable, final long maxSize) {

			final ArrayList<T> aa = new ArrayList<>();
			StreamSupport.stream(iterable.spliterator(), false).limit(maxSize).forEach(aa::add);
			return aa;
		}

		/**
		 * 标准版的记录生成器, map 生成的是LinkedRecord
		 *
		 * @param <T> 类型占位符
		 * @param kvs 键,值序列:key0,value0,key1,value1,.... <br>
		 *            Map结构（IRecord也是Map结构） 或是 键名,键值 序列。即 build(map) 或是
		 *            build(key0,value0,key1,vlaue1,...) 的 形式， 特别注意 build(map) 时候，当且仅当
		 *            kvs 的只有一个元素，即 build(map0,map1) 会被视为 键值序列
		 * @return IRecord对象
		 */
		@SafeVarargs
		public static <T> IRecord REC(final T... kvs) {
			final int n = kvs.length;
			final LinkedHashMap<String, Object> data = new LinkedHashMap<String, Object>();
			final Consumer<Tuple2<String, ?>> put_tuple = tup -> { // 元组处理
				data.put(tup._1, tup._2);
			}; // put_tuple
			final Consumer<Stream<Object>> put_stream = stream -> { // 数据流的处理
				stream.map(Tuple2.snb(0)).map(tuple -> { // 编号分组
					return Optional.ofNullable(tuple._2).map(e -> {
						if (e instanceof Tuple2) { // Tuple2 元组类型
							return (Tuple2<?, ?>) e;
						} else if (e instanceof Map.Entry) { // Map.Entry 类型
							final Map.Entry<?, ?> me = (Map.Entry<?, ?>) tuple._2;
							return Tuple2.of(me.getKey(), me.getValue());
						} else { // default 默认
							return tuple;
						} // if
					}).map(e -> e.fmap1(Object::toString)).orElse(null); // key 专为字符串类型 && 默认值为null
				}).filter(Objects::nonNull).forEach(put_tuple); //
			}; // put_stream
			final Consumer<Iterable<Object>> put_iterable = iterable -> { // iterable 数据处理
				put_stream.accept(StreamSupport.stream(iterable.spliterator(), false));
			}; // put_iterable

			if (n == 1) { // 单一参数情况
				final T single = kvs[0];
				if (single instanceof Map) { // Map情况的数据处理
					((Map<?, ?>) single).forEach((k, v) -> { // 键值的处理
						data.put(k + "", v);
					}); // forEach
				} else if (single instanceof IRecord) {// IRecord 对象类型 复制对象数据
					data.putAll(((IRecord) single).toMap());
				} else if (single instanceof Collection) {// Collection 对象类型 复制对象数据
					@SuppressWarnings("unchecked")
					final Collection<Object> coll = (Collection<Object>) single;
					put_iterable.accept(coll);
				} else if (single instanceof Iterable) {// Iterable 对象类型 复制对象数据
					@SuppressWarnings("unchecked")
					final Iterable<Object> iterable = (Iterable<Object>) single;
					put_iterable.accept(iterable);
				} else if (single instanceof Iterator) {// Iterable 对象类型 复制对象数据
					@SuppressWarnings("unchecked")
					final Iterable<Object> iterable = () -> (Iterator<Object>) single;
					put_iterable.accept(iterable);
				} else if (single instanceof Stream) {// stream 对象类型 复制对象数据
					@SuppressWarnings("unchecked")
					final Stream<Object> stream = (Stream<Object>) single;
					put_stream.accept(stream);
				} else if (single instanceof String) {// json 格式结构，尝试解析
					Optional.ofNullable(JSON.asMap((String) single)).ifPresent(data::putAll);
				} else { // 其他情况尝试做javabean分解
					data.putAll(JSON.obj2lhm(single));
				} // if
			} else if (n == 2) { // 仅有两个值的情况
				AtomicReference<Iterable<?>> arkeys = new AtomicReference<>();
				AtomicReference<Iterable<?>> arvalues = new AtomicReference<>();
				Optional.ofNullable(kvs[0]).flatMap(kk -> Optional.ofNullable(kvs[1]) //
						.map(vv -> { // (键名序列,键值序列的情况), 对键,值序列给予展开,注意仅对于都是序列的情况才给予展开，只有一个是序列的情况就不展开了。
							if (kk.getClass().isArray()) {
								arkeys.set(Arrays.asList((Object[]) kk)); // 注意需要之名参数为数组类型这里是静态编译的。
							} else if (kk instanceof Iterable<?> kitr) {
								arkeys.set(kitr);
							}

							if (vv.getClass().isArray()) {
								arvalues.set(Arrays.asList((Object[]) vv)); // 注意需要之名参数为数组类型这里是静态编译的。
							} else if (vv instanceof Iterable<?> vitr) {
								arvalues.set(vitr);
							}

							if (arkeys.get() != null && arvalues.get() != null) { // 都是序列的情况
								final Iterator<?> vitr = arvalues.get().iterator();
								final Map<String, Object> _data = new LinkedHashMap<String, Object>();
								for (final Object key : arkeys.get()) {
									final Object value = vitr.hasNext() ? vitr.next() : null;
									_data.put((key instanceof String k) ? k : key + "", value);

								}
								return _data;
							} else { // 至少由一个不是序列的情况就不展开了
								return null;
							}
						})).ifPresentOrElse(data::putAll, () -> { // 其他的情况
							data.put((kvs[0] instanceof String k) ? k : kvs[0] + "", kvs[1]);
						});
			} else { // 键名减值序列
				for (int i = 0; i < n - 1; i += 2) {
					data.put(kvs[i].toString(), kvs[i + 1]);
				}
			} // if

			return new MyRecord(data);
		}

		/**
		 * 解析Json
		 * 
		 * @param json 解析Json
		 * @return IRecord
		 */
		public static IRecord fromJson(final String json) {
			return Optional.ofNullable(json).map(JSON::asMap).map(MyRecord::new).orElse(null);
		}

	}

	/**
	 * 自制的 JSON 解析器
	 *
	 * @author gbench
	 */
	public static class JSON {

		/**
		 * 默认构造函数
		 */
		public JSON() {
		}

		/**
		 * JsonException
		 * 
		 * @author gbench
		 */
		public static class JsonException extends RuntimeException {
			/**
			 * JsonException
			 */
			public JsonException() {
			}

			/**
			 * JsonException
			 * 
			 * @param message message
			 */
			public JsonException(final String message) {
				super(message);
			}

			/**
			 * JsonException
			 * 
			 * @param message message
			 * @param cause   cause
			 */
			public JsonException(final String message, final Throwable cause) {
				super(message, cause);
			}

			private static final long serialVersionUID = 7557947611199812472L;
		}

		/**
		 * 分词器
		 * 
		 * @author gbench
		 */
		public static class JsonTokenizer {

			/**
			 * 构造函数
			 * 
			 * @param line 输入行
			 */
			public JsonTokenizer(final String line) {
				this.line = line;
			}

			/**
			 * hasNext
			 * 
			 * @return 是否还有下一个元素
			 */
			public final boolean hasNext() {
				return p < line.length();
			}

			/**
			 * nextToken
			 * 
			 * @return Token
			 */
			public final Token nextToken() {
				Token token = new Token();
				char ch = line.charAt(p);
				switch (ch) {
				case '{':
					token.setType(TokenType.BEGIN_OBJECT);
					token.setVal("{");
					p++;
					break;
				case '}':
					token.setType(TokenType.END_OBJECT);
					token.setVal("}");
					p++;
					break;
				case '[':
					token.setType(TokenType.BEGIN_ARRAY);
					token.setVal("[");
					p++;
					break;
				case ']':
					token.setType(TokenType.END_ARRAY);
					token.setVal("]");
					p++;
					break;
				case ',':
					token.setType(TokenType.SEP_COMMA);
					token.setVal(",");
					p++;
					break;
				case ':':
					token.setType(TokenType.SEP_COLON);
					token.setVal(":");
					p++;
					break;
				case 't':
					if (line.charAt(++p) == 'r' && line.charAt(++p) == 'u' && line.charAt(++p) == 'e') {
						token.setType(TokenType.BOOLEN);
						token.setVal("true");
					} else {
						throw new RuntimeException();
					}
					p++;
					break;
				case 'f':
					if (line.charAt(++p) == 'a' && line.charAt(++p) == 'l' && line.charAt(++p) == 's'
							&& line.charAt(++p) == 'e') {
						token.setType(TokenType.BOOLEN);
						token.setVal("false");
					} else {
						throw new RuntimeException();
					}
					p++;
					break;
				case 'n':
					if (line.charAt(++p) == 'u' && line.charAt(++p) == 'l' && line.charAt(++p) == 'l') {
						token.setType(TokenType.NULL);
						token.setVal("null");
					} else {
						throw new RuntimeException();
					}
					p++;
					break;
				case '"':
					final StringBuilder val = new StringBuilder();
					int i = 1;
					char c = line.charAt(p + i);
					while ('"' != c) {
						if ('\\' == c) {
							i++;
							switch (line.charAt(p + i)) {
							case 'b':
								val.append('\b');
								break;
							case 'f':
								val.append('\f');
								break;
							case 'n':
								val.append('\n');
								break;
							case 'r':
								val.append('\r');
								break;
							case 't':
								val.append('\t');
								break;
							case '\\':
								val.append('\\');
								break;
							case '/':
								val.append('/');
								break;
							case '"':
								val.append('"');
								break;
							case 'u':
								i++;
								final char u1 = line.charAt(p + i);
								i++;
								final char u2 = line.charAt(p + i);
								i++;
								final char u3 = line.charAt(p + i);
								i++;
								final char u4 = line.charAt(p + i);
								if (!isDigit(u1) || !isDigit(u2) || !isDigit(u3) || !isDigit(u4)) {
									throw new RuntimeException();
								}
								final char ucode = (char) Integer
										.parseInt(String.valueOf(new char[] { u1, u2, u3, u4 }), 16);
								val.append(ucode);
								break;
							default:
								throw new RuntimeException();
							}
						} else {
							val.append(c);
						}
						i++;
						c = line.charAt(p + i);
					}
					token.setType(TokenType.STRING);
					token.setVal(val.toString());
					p = p + i + 1;
					break;
				case '-':
				case '0':
				case '1':
				case '2':
				case '3':
				case '4':
				case '5':
				case '6':
				case '7':
				case '8':
				case '9':
					final Matcher matcher = numPattern.matcher(line.substring(p));
					if (matcher.lookingAt()) {
						final int end = matcher.end();
						final String grp = matcher.group();
						token.setType(TokenType.NUMBER);
						token.setVal(grp);
						p = p + end;
						break;
					} else {
						throw new RuntimeException();
					}
				case ' ':
				case '\n':
				case '\r':
				case '\t':
					// token.setType(TokenType.WS);
					// token.setVal(ch + "");
					p++;
					token = nextToken();
					break;
				default:
					throw new RuntimeException();
				} // switch
				return token;
			}

			/**
			 * lookAhead
			 * 
			 * @return Token
			 */
			public Token lookAhead() {
				final int p1 = p;
				final Token token = nextToken();
				p = p1;
				return token;
			}

			/**
			 * isDigit
			 * 
			 * @param ch ch
			 * @return Boolean
			 */
			private Boolean isDigit(char ch) {
				return ch >= 48 || ch <= 57 || ch >= 65 || ch <= 70 || ch >= 97 || ch <= 102;
			}

			/**
			 * getPoint
			 * 
			 * @return point
			 */
			public int getPoint() {
				return p;
			}

			private final String line;
			private int p;
			private final Pattern numPattern = Pattern
					.compile("([1-9]\\d+|\\d|\\-[1-9]\\d+|\\-\\d)(\\.\\d+)?([Ee][\\+\\-]?\\d+)?");
		} // JsonTokenizer

		/**
		 * JsonWriter
		 */
		public static class JsonWriter {

			/**
			 * 默认构造函数
			 */
			public JsonWriter() {

			}

			/**
			 * 把一个对象转换成json 字符串
			 * 
			 * @param obj 目标对象
			 * @return json 字符串
			 */
			public static String toJson(final Object obj) {
				final StringBuilder json = new StringBuilder();
				if (obj == null) {
					json.append("\"\"");
				} else if (obj instanceof String || obj instanceof Integer || obj instanceof Float
						|| obj instanceof Boolean || obj instanceof Short || obj instanceof Double
						|| obj instanceof Long || obj instanceof BigDecimal || obj instanceof BigInteger
						|| obj instanceof Byte) {
					json.append("\"").append(string2json(obj.toString())).append("\"");
				} else if (obj instanceof Object[]) {
					json.append(array2json((Object[]) obj));
				} else if (obj instanceof List) {
					json.append(list2json((List<?>) obj));
				} else if (obj instanceof Map) {
					json.append(map2json((Map<?, ?>) obj));
				} else if (obj instanceof Set) {
					json.append(set2json((Set<?>) obj));
				} else {
					json.append(bean2json(obj));
				}
				return json.toString();
			}

			/**
			 * 把一个javabean转换成json
			 * 
			 * @param bean javabean
			 * @return json 对象
			 */
			public static String bean2json(final Object bean) {
				final StringBuilder json = new StringBuilder();
				json.append("{");
				final LinkedHashMap<String, Object> kvps = obj2lhm(bean);
				if (kvps.size() > 0 && kvps != null) {
					for (final Entry<String, Object> e : kvps.entrySet()) {
						final String name = toJson(e.getKey());
						final String value = toJson(e.getValue());
						json.append(name);
						json.append(":");
						json.append(value);
						json.append(",");
					}
					json.setCharAt(json.length() - 1, '}');
				} else {
					json.append("}");
				}
				return json.toString();
			}

			/**
			 * 列表对象
			 * 
			 * @param list 列表对象
			 * @return json 字符串
			 */
			public static String list2json(final List<?> list) {
				final StringBuilder json = new StringBuilder();
				json.append("[");
				if (list != null && list.size() > 0) {
					for (Object obj : list) {
						json.append(toJson(obj));
						json.append(",");
					}
					json.setCharAt(json.length() - 1, ']');
				} else {
					json.append("]");
				}
				return json.toString();
			}

			/**
			 * 数组对象 转json对象
			 * 
			 * @param array 数组对象
			 * @return json 对象
			 */
			public static String array2json(final Object[] array) {
				final StringBuilder json = new StringBuilder();
				json.append("[");
				if (array != null && array.length > 0) {
					for (final Object obj : array) {
						json.append(toJson(obj));
						json.append(",");
					}
					json.setCharAt(json.length() - 1, ']');
				} else {
					json.append("]");
				}
				return json.toString();
			}

			/**
			 * Map转json对象
			 * 
			 * @param map Map对象
			 * @return json对象
			 */
			public static String map2json(final Map<?, ?> map) {
				final StringBuilder json = new StringBuilder();
				json.append("{");
				if (map != null && map.size() > 0) {
					for (final Object key : map.keySet()) {
						json.append(toJson(key));
						json.append(":");
						json.append(toJson(map.get(key)));
						json.append(",");
					}
					json.setCharAt(json.length() - 1, '}');
				} else {
					json.append("}");
				}
				return json.toString();
			}

			/**
			 * 集合对象 转 json 对象
			 * 
			 * @param set 集合对象
			 * @return json 对象
			 */
			public static String set2json(final Set<?> set) {
				final StringBuilder json = new StringBuilder();
				json.append("[");
				if (set != null && set.size() > 0) {
					for (Object obj : set) {
						json.append(toJson(obj));
						json.append(",");
					}
					json.setCharAt(json.length() - 1, ']');
				} else {
					json.append("]");
				}
				return json.toString();
			}

			/**
			 * 字符串 转 json 对象
			 * 
			 * @param line 字符串
			 * @return json 对象
			 */
			public static String string2json(final String line) {
				if (line == null)
					return "";
				final StringBuilder sb = new StringBuilder();
				for (int i = 0; i < line.length(); i++) {
					char ch = line.charAt(i);
					switch (ch) {
					case '"':
						sb.append("\\\"");
						break;
					case '\\':
						sb.append("\\\\");
						break;
					case '\b':
						sb.append("\\b");
						break;
					case '\f':
						sb.append("\\f");
						break;
					case '\n':
						sb.append("\\n");
						break;
					case '\r':
						sb.append("\\r");
						break;
					case '\t':
						sb.append("\\t");
						break;
					case '/':
						sb.append("\\/");
						break;
					default:
						if (ch >= '\u0000' && ch <= '\u001F') {
							final String ss = Integer.toHexString(ch);
							sb.append("\\u");
							for (int k = 0; k < 4 - ss.length(); k++) {
								sb.append('0');
							}
							sb.append(ss.toUpperCase());
						} else {
							sb.append(ch);
						}
					}
				}
				return sb.toString();
			}
		} // JsonWriter

		/**
		 * JsonParser
		 * 
		 * @author gbench
		 */
		public static class JsonParser {

			/**
			 * JsonParser
			 * 
			 * @param jsonTokenizer jsonTokenizer
			 */
			public JsonParser(final JsonTokenizer jsonTokenizer) {
				this.jsonTokenizer = jsonTokenizer;
			}

			/**
			 * parseValue
			 * 
			 * @return 解析后对象
			 */
			public Object parseValue() {
				final Token token = jsonTokenizer.nextToken();
				final TokenType tt = token.getType();
				if (tt == TokenType.BEGIN_OBJECT) {
					final Map<?, ?> map = parseObj();
					return map;
				}
				if (tt == TokenType.BEGIN_ARRAY) {
					final List<?> list = parseArray();
					return list;
				}
				if (tt == TokenType.STRING) {
					final String str = token.getVal();
					return str;
				}
				if (tt == TokenType.NUMBER) {
					return new BigDecimal(token.getVal());
				}
				if (tt == TokenType.BOOLEN) {
					final String s = token.getVal();
					if ("true".equals(s)) {
						return true;
					}
					if ("false".equals(s)) {
						return false;
					}
				}
				if (tt == TokenType.NULL) {
					return null;
				}
				throw new RuntimeException("parseValue error." + prettyToken(token));
			}

			/**
			 * parseObj
			 * 
			 * @return Map对象
			 */
			private Map<?, ?> parseObj() {
				final Token lookAhead = jsonTokenizer.lookAhead();
				if (lookAhead.getType() == TokenType.END_OBJECT) {
					jsonTokenizer.nextToken();
					return new LinkedHashMap<>();
				} else if (lookAhead.getType() == TokenType.STRING) {
					final Map<?, ?> map = parseMembers();
					return map;
				} else {
					throw new RuntimeException("parseObj error." + prettyToken(lookAhead));
				}
			}

			/**
			 * parseMembers
			 * 
			 * @return Map对现象
			 */
			private Map<?, ?> parseMembers() {
				final Map<Object, Object> map = new LinkedHashMap<>();
				final Object[] objects = parseMember();
				map.put(objects[0], objects[1]);
				final Token token = jsonTokenizer.nextToken();
				final TokenType tt = token.getType();
				if (tt == TokenType.SEP_COMMA) {
					final Map<?, ?> map1 = parseMembers();
					map.putAll(map1);
				} else if (tt == TokenType.END_OBJECT) {
				} else {
					throw new RuntimeException("parseMembers error. " + prettyToken(token));
				}
				return map;
			}

			/**
			 * parseMember
			 * 
			 * @return 数组对象
			 */
			private Object[] parseMember() {
				final Token key = jsonTokenizer.nextToken();
				if (key.getType() != TokenType.STRING) {
					throw new RuntimeException("key的类型不为string:" + prettyToken(key));
				}
				final String s = key.getVal();
				final Token colon = jsonTokenizer.nextToken();
				if (colon.getType() != TokenType.SEP_COLON) {
					throw new RuntimeException("key后缺少冒号:" + prettyToken(key));
				}
				final Object o = parseValue();
				final Object[] objects = { s, o };
				return objects;
			}

			/**
			 * parseArray
			 * 
			 * @return 列表对象
			 */
			private List<?> parseArray() {
				final Token lookAhead = jsonTokenizer.lookAhead();
				if (lookAhead.getType() == TokenType.END_ARRAY) {
					jsonTokenizer.nextToken();
					return new ArrayList<>();
				} else {
					final List<?> list = parseValues();
					return list;
				}
			}

			/**
			 * parseValues
			 * 
			 * @return 列表对象
			 */
			private List<?> parseValues() {
				final List<Object> list = new ArrayList<>();
				final Object o = parseValue();
				list.add(o);
				final Token token = jsonTokenizer.nextToken();
				final TokenType tt = token.getType();
				if (tt == TokenType.SEP_COMMA) {
					final List<?> list1 = parseValues();
					list.addAll(list1);
				} else if (tt == TokenType.END_ARRAY) {
				} else {
					throw new RuntimeException("parseValues error." + prettyToken(token));
				}
				return list;
			}

			/**
			 * prettyToken
			 * 
			 * @param token token
			 * @return 格式化字符串
			 */
			private String prettyToken(final Token token) {
				final String s = " type:" + token.getType() + ",value:" + token.getVal();
				return s;
			}

			private final JsonTokenizer jsonTokenizer;
		} // JsonParser

		/**
		 * TokenType
		 * 
		 * @author gbench
		 */
		public static enum TokenType {
			/**
			 * BEGIN_OBJECT
			 */
			BEGIN_OBJECT,
			/**
			 * END_OBJECT
			 */
			END_OBJECT,

			/**
			 * BEGIN_ARRAY
			 */
			BEGIN_ARRAY,
			/**
			 * END_ARRAY
			 */
			END_ARRAY,
			/**
			 * STRING
			 */
			STRING,
			/**
			 * NUMBER
			 */
			NUMBER,
			/**
			 * BOOLEN
			 */
			BOOLEN,
			/**
			 * NULL
			 */
			NULL,
			/**
			 * SEP_COLON
			 */
			SEP_COLON,
			/**
			 * SEP_COMMA
			 */
			SEP_COMMA,
			/**
			 * WS
			 */
			WS
		}

		/**
		 * Token
		 * 
		 * @author gbench
		 */
		public static class Token {

			/**
			 * 默认构造函数
			 */
			public Token() {
			}

			/**
			 * 参数构造函数
			 * 
			 * @param type type
			 * @param val  val
			 */
			public Token(final TokenType type, final String val) {
				this.type = type;
				this.val = val;
			}

			/**
			 * getType
			 * 
			 * @return TokenType
			 */
			public TokenType getType() {
				return type;
			}

			/**
			 * setType
			 * 
			 * @param type type
			 */
			public void setType(final TokenType type) {
				this.type = type;
			}

			/**
			 * getVal
			 * 
			 * @return 符号值
			 */
			public String getVal() {
				return val;
			}

			/**
			 * 设置符号值
			 * 
			 * @param val 符号值
			 */
			public void setVal(final String val) {
				this.val = val;
			}

			private TokenType type;
			private String val;

		}

		/**
		 * 把一个对象转换成键值对儿(javabean分解)
		 * 
		 * @param obj 目标对象
		 * @return 键值对儿列表
		 */
		public static LinkedHashMap<String, Object> obj2lhm(final Object obj) {
			final LinkedHashMap<String, Object> lhm = new LinkedHashMap<String, Object>();
			for (final Field fld : obj.getClass().getDeclaredFields()) {
				fld.setAccessible(true);
				final String key = fld.getName();
				try {
					final Object value = fld.get(obj);
					lhm.put(key, value);
				} catch (final Exception e) {
					e.printStackTrace();
				}
			} // for

			return lhm;
		}

		/**
		 * 判断对象是否是json格式
		 * 
		 * @param obj 目标对象
		 * @return 是否是json格式
		 */
		public static boolean isJson(final String obj) {
			boolean flag = false;
			try {
				final Object o = JSON.parse(obj);
				if (obj != null && !o.getClass().isPrimitive() && !(o instanceof Number)) {
					flag = true;
				} // if
			} catch (final Exception e) {
				//
			}
			return flag;
		}

		/**
		 * json 对象解析
		 * 
		 * @param <T>  结果类型
		 * @param line json 数据行
		 * @return json 对象
		 */
		@SuppressWarnings("unchecked")
		public static <T> T parse(final String line) {
			final JsonTokenizer tokenizer = new JsonTokenizer(line);
			final JsonParser parser = new JsonParser(tokenizer);
			Object o = null;
			try {
				o = parser.parseValue();
			} catch (final Exception e) {
				final int p = tokenizer.getPoint();
				final int endIndex = Math.min(p + 20, line.length());
				final String errMsg = "格式不正确位置:" + line.substring(p, endIndex) + ";" + e.getMessage();
				// System.err.println(line);
				throw new JsonException(errMsg);
			}
			return (T) o;
		}

		/**
		 * 将一个对象 转换成 json 字符串
		 * 
		 * @param obj 数据对象
		 * @return json 字符串
		 */
		public static final String toJson(final Object obj) {
			return obj == null ? null : JsonWriter.toJson(obj);
		}

		/**
		 * 把一个json字符串转换成Map对象
		 * 
		 * @param line json 字符串
		 * @return Map对象
		 */
		@SuppressWarnings("unchecked")
		public static Map<String, Object> asMap(final String line) {
			final Object ret = JSON.parse(line);
			return ret instanceof Map ? (Map<String, Object>) ret : null;
		}
	}

	/**
	 * AbstractJdbcSession
	 * 
	 * @author gbench AbstractJdbcSession
	 * @param <D> 数据类型
	 * @param <X> 归集类型
	 */
	public static abstract class AbstractJdbcSession<D, X> implements IJdbcSession<D, X> {

		/**
		 * 默认构造函数
		 */
		public AbstractJdbcSession() {
		}

		@Override
		public Set<Stream<?>> getActiveStreams() {

			return activeStreams;
		}

		/**
		 * 会话中的数据内容
		 *
		 * @return session中的数据类型。
		 */
		public D getData() {

			return this.data;
		}

		/**
		 * 会话中的数据内容
		 *
		 * @return session中的数据类型。
		 */
		public D setData(final D data) {

			return this.data = data;
		}

		@Override
		public IRecord getAttributes() {
			return this.attributes;
		}

		@Override
		public abstract Connection getConnection();

		private D data;
		private final IRecord attributes = IRecord.REC();
		private final Set<Stream<?>> activeStreams = new LinkedHashSet<>();

	}

	/**
	 * SQL 生成器
	 *
	 * @author xuqinghua
	 */
	public static class SqlBuilder {

		/**
		 * 默认构造函数
		 */
		public SqlBuilder() {

		}

		/**
		 * 左连接
		 * 
		 * @param path 连接路径
		 * @param flds 连接字段
		 * @param mode 连接模式
		 * @return SqlBuilder
		 */
		public SqlBuilder join(final String path, final String flds, final String mode) {
			final String sql = SqlBuilder.JOIN(path, flds, mode);
			this.buffer.append(sql);
			return this;
		}

		/**
		 * 左连接
		 * 
		 * @param path 路径
		 * @param flds 字段列表
		 * @return SqlBuilder
		 */
		public SqlBuilder leftjoin(final String path, final String flds) {
			final String sql = SqlBuilder.LEFTJOIN(path, flds);
			this.buffer.append(sql);
			return this;
		}

		/**
		 * rightjoin
		 * 
		 * @param path 路径
		 * @param flds 连接字段
		 * @return SqlBuilder
		 */
		public SqlBuilder rightjoin(final String path, final String flds) {
			final String sql = SqlBuilder.JOIN(path, flds, "right");
			this.buffer.append(sql);
			return this;
		}

		/**
		 * 增加数据行
		 *
		 * @param line 数据行
		 * @return SqlBuilder
		 */
		public SqlBuilder append(final Object line) {
			buffer.append(line);
			return this;
		}

		/**
		 * 别名
		 * 
		 * @param alias 别名列表
		 * @return SqlBuilder
		 */
		public SqlBuilder as(final String alias) {
			this.push(alias, this.toString());
			this.buffer.delete(0, this.buffer.length());
			return this;
		}

		/**
		 * 加入变量别名
		 *
		 * @param name 名称
		 * @param line 数据行
		 * @return SqlBuilder
		 */
		public SqlBuilder push(final String name, final String line) {
			mappings.put(name, line);
			return this;
		}

		/**
		 * 格式化输出
		 * 
		 * @param template 模板
		 * @return 格式化输出
		 */
		public String format(final String template) {
			return IRecord.FT(template, REC(this.mappings), true);
		}

		/**
		 * 格式化
		 */
		public String toString() {
			return this.format(this.buffer.toString());
		}

		/**
		 * 表连接
		 *
		 * @param path 关联序列 "t_user u;id,userid;t_user_role ur;roleid,id;t_role
		 *             r;id,permissionid;t_role_permission
		 *             rp;permissionid,id;t_permission p"; //
		 * @param flds 字段名称
		 * @param mode 连接模式
		 * @return 生成表连接查询
		 */
		public static String JOIN(final String path, final String flds, final String mode) {
			final List<DataApp.Tuple2<String, String>> tuples = Tuple2.tupleS(path, "[\\s,;/=]+")
					.collect(Collectors.toList());
			final String triples = tuples.stream().collect(IRecord.slidingclc(3, 2, true)).map(e -> {
				final Tuple2<String, String> left = e.get(0);
				final Tuple2<String, String> relation = e.get(1);
				final Tuple2<String, String> right = e.get(2);
				final String line = IRecord.FT("$6 join $4 $5 on  $1.$2 = $5.$3", left._1, left._2, relation._1,
						relation._2, right._1, right._2, Optional.ofNullable(mode).orElse("left"));
				return line;
			}).collect(Collectors.joining(" "));
			final String sql = IRecord.FT("select $3 from $0 $1 $2", tuples.get(0)._1, tuples.get(0)._2, triples,
					Optional.ofNullable(flds).map(e -> e.matches("^\\s*$") ? null : e).orElse("*"));
			return sql;
		}

		/**
		 * 表连接
		 *
		 * @param path 关联序列 "t_user u;id,userid;t_user_role ur;roleid,id;t_role
		 *             r;id,permissionid;t_role_permission
		 *             rp;permissionid,id;t_permission p"; //
		 * @param flds 字段名称
		 * @return 生成表连接查询
		 */
		public static String LEFTJOIN(final String path, final String flds) {
			return SqlBuilder.JOIN(path, flds, null);
		}

		/**
		 * 表连接
		 *
		 * @param path 关联序列 "t_user u;id,userid;t_user_role ur;roleid,id;t_role
		 *             r;id,permissionid;t_role_permission
		 *             rp;permissionid,id;t_permission p"; //
		 * @return 生成表连接查询
		 */
		public static String LEFTJOIN(final String path) {
			return SqlBuilder.JOIN(path, null, null);
		}

		private final Map<String, String> mappings = new HashMap<String, String>();
		private final StringBuilder buffer = new StringBuilder();

	}

	/**
	 * DFrame 数据框
	 *
	 * @author xuqinghua
	 */
	public static class DFrame extends LinkedList<IRecord> {

		/**
		 * 构造函数
		 *
		 * @param data 源数据
		 */
		public DFrame(final Iterable<? extends IRecord> data) {
			if (data instanceof Collection<? extends IRecord> cc) {
				this.addAll(cc);
			} else {
				this.addAll(StreamSupport.stream(data.spliterator(), false).toList());
			}
		}

		/**
		 * 构造函数
		 *
		 * @param data 源数据
		 */
		public DFrame(final IRecord... data) {
			this(Arrays.asList(data));
		}

		/**
		 * 默认构造函数
		 * 
		 */
		public DFrame() {
			super();
		}

		/**
		 * 根据索引 提取键名
		 *
		 * @param idx 键名索引从0开始
		 * @return 键名
		 */
		public String keyOf(final int idx) {
			final List<String> kk = this.keys();
			return idx < kk.size() ? kk.get(idx) : null;
		}

		/**
		 * 根据键名 提取 键名索引
		 *
		 * @param key 键名
		 * @return 键名索引
		 */
		public int indexOf(final String key) {
			int i = -1;
			for (final String _key : this.keys()) {
				i++;
				if (_key.equals(key))
					break;
			}
			return i;
		}

		/**
		 * 健名列表
		 *
		 * @return 健名列表
		 */
		public List<String> keys() {
			return new ArrayList<>(this.cols().keySet());
		}

		/**
		 * 健名列表
		 *
		 * @return 健名列表
		 */
		public Stream<String> keyS() {
			return this.cols().keySet().stream();
		}

		/**
		 * 行数据流
		 *
		 * @return 行数据流 [rec]
		 */
		public Stream<IRecord> rowS() {
			return this.stream();
		}

		/**
		 * 行数据流
		 *
		 * @param <U>    变换器的结果类型
		 * @param mapper 行记录便器 rec-&gt;u
		 * @return 行数据流 [u]
		 */
		public <U> Stream<U> rowS(final Function<IRecord, U> mapper) {
			return this.stream().map(mapper);
		}

		/**
		 * 指定行索引的记录
		 *
		 * @param idx 行名索引从0开始
		 * @return 行数据流
		 */
		public Optional<IRecord> rowOpt(final int idx) {
			return Optional.ofNullable(idx >= this.size() ? null : this.get(idx));
		}

		/**
		 * 指定行索引的记录
		 *
		 * @param idx 行名索引从0开始
		 * @return 行数据流
		 */
		public IRecord row(final int idx) {
			return this.rowOpt(idx).orElse(null);
		}

		/**
		 * 指定列名的记录
		 *
		 * @param idx 列名索引
		 * @return 列数据
		 */
		public Optional<List<Object>> colOpt(final int idx) {
			return this.colOpt(this.keyOf(idx));
		}

		/**
		 * 指定列名的记录
		 *
		 * @param idx 列名索引
		 * @return 列数据
		 */
		public List<Object> col(final int idx) {
			return this.colOpt(idx).orElse(null);
		}

		/**
		 * 指定列名的记录
		 *
		 * @param name 列名
		 * @return 列数据
		 */
		public Optional<List<Object>> colOpt(final String name) {
			return Optional.ofNullable(this.cols().get(name));
		}

		/**
		 * 指定列名的记录
		 *
		 * @param name 列名
		 * @return 列数据
		 */
		public List<Object> col(final String name) {
			return this.colOpt(name).orElse(null);
		}

		/**
		 * 列数据
		 *
		 * @param <T>    原数据类型
		 * @param <U>    目标数据类型
		 * @param name   列名
		 * @param mapper 列元素的数据变换 t-&gt;u
		 * @return 列数据的流
		 */
		@SuppressWarnings("unchecked")
		public <T, U> Stream<U> colS(final String name, final Function<T, U> mapper) {
			return this.col(name).stream().map(e -> (T) e).map(mapper);
		}

		/**
		 * 列数据
		 *
		 * @param <T>    原数据类型
		 * @param <U>    目标数据类型
		 * @param name   列名
		 * @param mapper 列元素的数据变换 t-&gt;u
		 * @return 列数据的列表
		 */
		public <T, U> List<U> cols(final String name, final Function<T, U> mapper) {
			return this.colS(name, mapper).collect(Collectors.toList());
		}

		/**
		 * 转换成列数据数组
		 *
		 * @return 列数据
		 */
		public LinkedHashMap<String, ArrayList<Object>> cols() {
			if (this.colsData == null) {
				final LinkedHashMap<String, ArrayList<Object>> _colsdata = new LinkedHashMap<String, ArrayList<Object>>();
				this.forEach(e -> {
					e.keys().forEach(k -> {
						_colsdata.compute(k, (_key, _value) -> _value == null ? new ArrayList<Object>() : _value)
								.add(e.get(k));
					}); // keys
				}); // forEach
				this.colsData = _colsdata;
			} // if

			return this.colsData;
		}

		/**
		 * 数组元素归集
		 *
		 * @param <T>       元素类型
		 * @param collector 归集器 [m] -&gt; T 归集类型
		 * @return T类型的结果
		 */
		public <T> T collect(final Collector<? super IRecord, ?, T> collector) {
			return this.rowS().collect(collector);
		}

		/**
		 * 数组元素归集
		 *
		 * @param <T>       元素类型
		 * @param collector 归集器 [m] -&gt; T 归集类型
		 * @return T类型的结果
		 */
		public <T> T collect2(final Collector<? super Map<String, Object>, ?, T> collector) {
			return this.rowS().map(e -> e.toMap()).collect(collector);
		}

		/**
		 * 结果类型
		 *
		 * @param <T>    结果类型
		 * @param mapper 结果变换 dfm -&gt; t
		 * @return T类型值
		 */
		public <T> T mutate(final Function<DFrame, T> mapper) {
			return mapper.apply(this);
		}

		/**
		 * 遍历函数(带有返回值)
		 * 
		 * @param action 遍历函数
		 * @return DFrame
		 */
		public DFrame foreach(Consumer<? super IRecord> action) {
			this.forEach(action);
			return this;
		}

		/**
		 * Haskell Functor
		 *
		 * @param mapper rec-&gt;rec_new
		 * @return DFrame
		 */
		public DFrame fmap(final Function<IRecord, IRecord> mapper) {
			return this.rowS().map(mapper).collect(DFrame.dfmclc);
		}

		/**
		 * 数组类型变换
		 *
		 * @param <T>    元素类型
		 * @param <U>    结果类型
		 * @param mapper 变换函数 obj-&gt;t
		 * @param gen    生成函数 (kk,tt)-&gt;u
		 * @return U数据类型
		 */
		public <T, U> U arrayOf(final Function<Object, T> mapper,
				final BiFunction<? super Iterable<String>, T[][], U> gen) {
			return gen.apply(this.keys(), this.toArray(mapper));
		}

		/**
		 * 二维对象数组
		 *
		 * @param <T>    数组元素类型
		 * @param mapper rec-&gt;rec_new
		 * @return 二维对象数组
		 */
		@SuppressWarnings("unchecked")
		public <T> T[][] toArray(final Function<Object, T> mapper) {
			final AtomicReference<Class<T[]>> ar = new AtomicReference<Class<T[]>>();
			this.rowS().filter(Objects::nonNull).findFirst().ifPresent(e -> {
				final T[] tt = e.toArray(mapper);
				ar.set((Class<T[]>) tt.getClass());
			});
			return this.rowS().map(e -> e.toArray(mapper)) //
					.toArray(n -> {
						final Class<T[]> tclass = ar.get() == null //
								? (Class<T[]>) (Object) Object[].class //
								: ar.get();
						return (T[][]) Array.newInstance(tclass, n);
					});
		}

		/**
		 * 键值过滤
		 *
		 * @param keys 健名列表
		 * @return DFrame
		 */
		public DFrame filter(final String keys) {
			final List<String> _keys = Arrays.asList(keys.split(","));
			return this.rowS().map(e -> e.filter(p -> _keys.contains(p._1))).collect(DFrame.dfmclc);
		}

		/**
		 * 键值过滤
		 *
		 * @param keys 健名列表
		 * @return DFrame
		 */
		public DFrame filterNot(final String keys) {
			final List<String> _keys = Arrays.asList(keys.split(","));
			return this.rowS().map(e -> e.filter(p -> !_keys.contains(p._1))).collect(DFrame.dfmclc);
		}

		/**
		 * 头前1个元素
		 *
		 * @return 头前元素
		 */
		public IRecord head() {
			return this.getFirst();
		}

		/**
		 * 头前n个元素
		 *
		 * @param n 数据长度 大于等于1,大于当前列表长度size,返回整个列表
		 * @return 头前元素
		 */
		public List<IRecord> head(final int n) {
			final int sz = this.size();
			return sz <= n ? this : this.subList(0, n);
		}

		/**
		 * 头前n个元素
		 *
		 * @param n 数据长度 大于等于1,大于当前列表长度size,返回整个列表
		 * @return 头前元素
		 */
		public Stream<IRecord> headS(final int n) {
			return this.head(n).stream();
		}

		/**
		 * 最后的元素
		 *
		 * @return 头前元素
		 */
		public IRecord last() {
			return this.getLast();
		}

		/**
		 * 后尾n个元素列表
		 *
		 * @param n 数据长度 大于等于1,大于当前列表长度size,返回整个列表
		 * @return 尾部元素列表
		 */
		public List<IRecord> tail(final int n) {
			final int sz = this.size();
			return n >= sz ? this : this.subList(sz - n, this.size());
		}

		/**
		 * 后尾n个元素列表
		 *
		 * @param n 数据长度 大于等于1,大于当前列表长度size,返回整个列表
		 * @return 尾部元素列表
		 */
		public Stream<IRecord> tailS(final int n) {
			return this.tail(n).stream();
		}

		/**
		 * 后尾1个元素列表
		 *
		 * @return 尾部元素列表
		 */
		public List<IRecord> tail() {
			final int sz = this.size();
			return sz < 1 ? null : this.tail(this.size() - 1);
		}

		/**
		 * 尾部元素列表
		 *
		 * @return 尾部元素列表
		 */
		public List<IRecord> initial() {
			return this.subList(0, this.size() - 1);
		}

		/**
		 * Returns a view of the portion of this list between the specified fromIndex,
		 * inclusive, and toIndex, exclusive. (If fromIndex and toIndex are equal, the
		 * returned list isempty.) The returned list is backed by this list, so
		 * non-structuralchanges in the returned list are reflected in this list, and
		 * vice-versa.The returned list supports all of the optional list operations
		 * supportedby this list.
		 *
		 * @param fromIndex low endpoint (inclusive) of the subList
		 * @return 字列表
		 */
		public List<IRecord> subList(final int fromIndex) {
			return this.subList(fromIndex, this.size());
		}

		/**
		 * 刷新列数据
		 * 
		 * @return DFrame
		 */
		public DFrame refresh() {
			this.colsData = null;
			return this;
		}

		/**
		 * 增加元素
		 *
		 * @param i    index at which to insert the first elementfrom the specified
		 *             collection
		 * @param rows collection containing elements to be added to this list
		 * @return this 对象本身
		 */
		public DFrame insert(final int i, final IRecord... rows) {
			this.addAll(i, Arrays.asList(rows));
			return this;
		}

		/**
		 * 增加元素
		 *
		 * @param rows collection containing elements to be added to this list
		 * @return this 对象本身
		 */
		public DFrame rbind(final Iterable<IRecord> rows) {
			rows.forEach(this::add);
			return this;
		}

		/**
		 * 使用指定比较器进行排序
		 * 
		 * @param c 比较器 (a,b)->{-1,0,1}
		 * @return this 对象本身
		 */
		public DFrame sorted(final Comparator<? super IRecord> c) {
			this.sort(c);
			return this;
		}

		/**
		 * 按照列进行数据比较
		 * 
		 * @param <T> 元素类型
		 * @param gt  行间比较函数(a:当前行,max:累计行)-&gt; a &gt; acc;
		 * @return 提取每一列的最大值
		 */
		public <T> IRecord maxBy(final BiPredicate<IRecord, IRecord> gt) {
			return this.rowS().reduce((max, a) -> gt.test(a, max) ? a : max).orElse(null);
		}

		/**
		 * 按照列进行数据比较
		 * 
		 * @param <T> 元素类型
		 * @param gt  列内元素比较函数(a:当前元素,max:累计对象)-&gt; a &gt; max;
		 * @return 提取每一列的最大值
		 */
		@SuppressWarnings("unchecked")
		public <T> IRecord maxBy2(final BiPredicate<T, T> gt) {
			return this.rowS().reduce(DataApp.IRecord.REC(), (nul, a) -> a.tupleS().reduce(nul, (max, p) -> {
				if (gt.test((T) p._2, (T) max.get(p._1)))
					max.set(p._1, p._2);
				return max;
			}, (r1, r2) -> r1.add(r2))); // 提取最大长度
		}

		/**
		 * json 字符串
		 * 
		 * @return json 字符串
		 */
		public String json() {
			return JSON.toJson(this);
		}

		/**
		 * 数据内容格式化
		 */
		public String toString() {
			if (this.size() > 0) {
				final StringBuffer buffer = new StringBuffer();
				final IRecord first = this.get(0);
				final List<String> keys = first.keys();
				final String header = keys.stream().collect(Collectors.joining("\t"));
				final String body = this.rowS().map(e -> keys.stream().map(e::str).collect(Collectors.joining("\t")))
						.collect(Collectors.joining("\n"));
				buffer.append(header + "\n");
				buffer.append(body);
				return buffer.toString();
			} else {
				return "DFrame(empty)";
			}
		}

		/**
		 * 创建 DFrame
		 *
		 * @param data 数据序列
		 * @return DFrame
		 */
		public static DFrame of(final Iterable<IRecord> data) {
			return new DFrame(data == null ? new ArrayList<>() : data);
		}

		/**
		 * 构造函数
		 *
		 * @param data 源数据
		 */
		public static DFrame of(final IRecord... data) {
			return new DFrame(data);
		}

		private LinkedHashMap<String, ArrayList<Object>> colsData;

		/**
		 * 序列号
		 */
		private static final long serialVersionUID = 3677521717607148260L;

		/**
		 * IRecord 类型的归集器 [rec:行]->dfm,行法归集
		 */
		public static Collector<IRecord, ?, DFrame> dfmclc = Collector.of((Supplier<List<IRecord>>) ArrayList::new,
				List::add, (left, right) -> {
					left.addAll(right);
					return left;
				}, e -> {
					return new DFrame(e);
				});

		/**
		 * Map 类型的归集器 [map:行]->dfm,行法归集
		 */
		public static Collector<Map<?, ?>, ?, DFrame> dfmclc2 = Collector.of((Supplier<List<IRecord>>) ArrayList::new,
				(aa, a) -> aa.add(REC(a)), (left, right) -> {
					left.addAll(right);
					return left;
				}, e -> {
					return new DFrame(e);
				});

	}

	/**
	 * 请求模式
	 */
	public enum SQL_MODE {
		/**
		 * 查询模式
		 */
		QUERY, // 查询模式
		/**
		 * 更新模式
		 */
		UPDATE, // 更新模式
		/**
		 * 执行模式
		 */
		EXECUTE, // 执行模式
		/**
		 * 带有滚动的查询模式
		 */
		QUERY_SCROLL // 带有滚动的查询模式
	}

	/**
	 * 文本格式化输出
	 *
	 * @param objects 对象序列
	 * @return 格式化输出的文本
	 */
	public static String println(final Object... objects) {
		final String line = Arrays.stream(objects).map(e -> "" + e).collect(Collectors.joining("\n"));
		System.out.println(line);
		return line;
	}

	/**
	 * 对dfm进行就按单插查错
	 * 
	 * @param dfm   sqlquery 或是 sqlexecute执行结果
	 * @param onerr 错误信息处理
	 * @return 对于 含有错误信息的dfm 返回 Optional.empty 否则返回dfm保持不变。
	 */
	public static Optional<DFrame> checkerr(final DFrame dfm, final Consumer<IRecord> onerr) {
		return Optional.ofNullable(dfm).map(rs -> {
			final var line = rs.getFirst();
			if (null != line && line.keys().contains("$error")) { // SQL直线出现了错误
				onerr.accept(line);
				return null;
			} else {
				return rs;
			}
		});
	}

	private static int MAX_SIZE = 1024 * 1024 * 1024; // 最大长度
	/**
	 * 调试输出函数
	 */
	public static Consumer<String> debug; // 调试输出函数

	private DataSource dataSource; // 注入系统的数据源

} // JdbcApp
