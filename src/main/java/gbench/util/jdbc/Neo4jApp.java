package gbench.util.jdbc;

import java.util.*;
import java.math.BigInteger;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.security.MessageDigest;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.BiConsumer;
import java.lang.annotation.Annotation;
import java.util.concurrent.atomic.AtomicReference;

import gbench.util.jdbc.kvp.IRecord;
import gbench.util.jdbc.kvp.KVPair;
import gbench.util.jdbc.kvp.PVec;
import gbench.util.jdbc.kvp.Tuple2;
import gbench.util.jdbc.kvp.SimpleRecord;
import gbench.util.jdbc.annotation.JdbcConfig;

import static gbench.util.jdbc.Jdbcs.ANNO;
import static gbench.util.jdbc.Jdbcs.LIST;
import static gbench.util.jdbc.kvp.IRecord.*;
import static gbench.util.jdbc.kvp.SimpleRecord.REC2;
import static gbench.util.jdbc.Jdbcs.SET_FIELD_OF_ANNOTATION;

/**
 * 我们把图视为一种 边的结构{edges}：而顶点是一种特殊的边。只有一个顶点的边。或者说是终顶点为null的边。<br>
 * 一般的（最简单与最基本）边就是 由（开始顶点与，终止顶点）构成的边，属性数据附着于
 * 之两个顶点即边之上。这既是图的本质:边（含有属性和单顶点边）的集合<br>
 * 
 * 数据的本质：key2value,辩证的基础是 动态的many2many，阴阳：不对称，不静态的二元分解，借助于 单向的形式到内容的
 * key2value的one2many即语言逻辑<br>
 * 来构造many2many: many2one->one2many. 即用 多对一/一对多 的关系 来构造 <br>
 * 
 * 关系/谓词与运算图：(关系的动词化特征就是谓词,可以表示判断)
 * 要把关系上升到数据运算,tensorflow的运算图。与程序与系统的层面上进行思考。<br>
 * 关系就是 表达式 就是结构化系统：<br>
 * 关系可以视为一种顶点之间的计算，即关系可以理解为一个以其内含顶点为参数的函数 即 我们可以把<br>
 * {"x/y",REC("0#vlblk","X", "1#vlblk","Y")} 视为 f(x:X,y:Y)的运算结果。<br>
 * {"x/y/z",REC("0#vlblk","X", "1#vlblk","Y","2#vlblk","Z")} 视为
 * f(x:X,y:Y,"Z")的运算结果。<br>
 * 即{"formula",REC(变量符号,变量类型)}。由此，我们可以把关系即顶点序列理解成 一个表达式。所有关系与表达式（以顶点或是边为构成项)的结构
 * 是等价的。关系即表达式：<br>
 * 此时我们就把关系视为：语句、命令模版了，于是就有了一下的结构设计：关系谓词(顶点1，顶点2). 即 REC(关系谓词,顶点1,顶点2)
 * 的谓词(命题)逻辑。即把关系视作谓词判断。
 * 
 * <br>
 * 构造原子：即顶点概念是 一切实体结构的基础。<br>
 * <br>
 * 顶点 又叫原子顶点，就是不可以再进一步给予分解（细分）的 数据概念。一般用于表达 数据实体。<br>
 * 顶点就相当于一条数据记录,标签label就像相当于 关系数据库中的表名，只不过 这了 这条记录表名 是多对多的关系，更合理的解释 label 是
 * 顶点的分组,顶点属性 相当于的记录的字段。<br>
 * 区别在于： 关系型数据库一条数据记录只能存在于一张表中，一张表可以包含多条记录，即记录与表的关系是 （many2one），<br>
 * 而 非关系数据库,一个顶点可以 隶属于多个标签 ，一个标签可以包含多个顶点, 即 顶点与标签的关系是
 * (many2many),所以顶点标签是对记录表关系的泛化 <br>
 * <br>
 * 构造边：即 关系概念，这是高阶的虚拟顶点概念（这需要思考一下：边与顶点是一种概念，注意领会这里把边进行原子化即不予考虑其构成结构），即设计的基础。
 * <br>
 * 对于边可以把边理解为 复合顶点：即由顶点构成的顶点(两个或是多个顶点即顶点序列)构成的顶点结构。最小的边 就是一个由两个原子顶点 构成的数据结构。<br>
 * 边拥有丰富的结构化信息表达能力,它用于表达那些 依附于在其构成顶点（成分顶点)之上的数据特征的集合，所以边是一种 比顶点更为高级的数据概念。<br>
 * 我们把这样数据信息称为关系信息。关系信息能够提供更为灵活生动信息能力，它还可以实现 以边为顶点 高阶/递归构造，进而实现 仅通过 基本的 原子顶点数据
 * <br>
 * 就能够实现 无穷于天地的 高阶信息，即 概念想法的 表现力。这也是 图结构的 辩证内涵。<br>
 * 之所以把 它称为高阶顶点，是为了利用 最为基本顶点对标签的 many2many的数据关系，而这种 Value2Label即 many2many
 * 是我们对数据进行的操作算法基础。 <br>
 * Value 一般被称为 值对象，Label 一般被称为 Key对象/符号对象。于是 又可以通过基本的(key物质,value:意识)的
 * (认,知)或称为阴阳辩证关系。<br>
 * 所谓阴阳辩证 就是这是many2many 是一种可以 通过 拆分成: key(符号形式, one)-&gt;value(内容意义 many),即通过
 * 通过符号来 启发意义 的 认知逻辑。<br>
 * 亦可以反过来通过:内容意义的value即心有所想one-&gt;key(表达手段与方式many）的 实践逻辑。而这二者的合并 就会构成
 * 认知实践循环：<br>
 * many2one-&gt;one2many 其阴阳互变的动态结构，说到阴阳就是强调这种动态过程。<br>
 * 这里采用 key-&gt;value作为一种记录的文法规则:（旨在表达一种正处于向其对立面即 辩证的二 元变化的动态过程：阳到阴 或是
 * 阴到阳)。表示我们对这个变换过程的观察角度：<br>
 * 这其实是一种思路的表达逻辑注意这非实际事实（事实事实是无法单向表达的，需要辩证论述），它只是为现象的方便理解，而构造出的语言或表达，即我们愿意如此相信或是想象而已。<br>
 * 这是一种以key开始,用value结束的记法约定：这里的key与value就像会计的复式记账法一样,仅作为一种 方向标记、占位符,而已：<br>
 * 而key,value到底是什么,即key到是代表符号形式还是内容意义，则是取决于我们能的表达需要。即我们当前的 状态：是认知还是实践。<br>
 * 这里需要有用目的即主观的方向性来做解释。即用什么养的内容value来解释key的这种形式。<br>
 * - 当我们是处于认知方向:key 就是符号形式，手段方法 ，value 是 内容意义。<br>
 * - 当我们处于 实践表达方向：key 则是内心的观念想法， value 则是 表达方式与活动行为。<br>
 * 
 * <br>
 * 图结构的创建App： <br>
 * 顶点采用 KVPair&lt;String,Object*gt;来表示。 <br>
 * 演示示例如下：Map&lt;IRecord,Record&gt; edge2attributes 的结构创建 <br>
 * 
 * <pre>{@code
 * 
 *	public void foo0() { 
 *       	final var app = new Neo4jApp(true); 
 *       	final var g = app.graph(REC2( 
 *       		"张三/李四", R("rel","喜欢","张三.address","上海", 
 *       		"张三.father","lisi", 
 *       		"张三.birth",LocalDateTime.now(), 
 *       		"李四.phone","1812074620"
 *       		), 
 *       		"李四/王五/赵六", R("rel","喜欢","2#address","北京","1#age",25,"2#height",1.98), 
 *       			"赵六/王八", R("rel","喜欢","2#address","北京"), 
 *       		"陈七/王八", R("rel","喜欢","$address","中国") 
 *       	).applyForKvs(IRecord::STRING2REC,e->(IRecord)e),PVec::new); 
 *       	System.out.println(g); 
 * 
 *       	// 数据库主要构件的创建。 
 *       	final var database = IJdbcApp.newNspebDBInstance("neo4j.sql", Database.class);
 *       	// 根据SQL语句模板文件neo4j.sql 生成代理数据库 
 *       	final var proxy = database.getProxy();// 提取代理对象 
 *       	final var jdbc = proxy.findOne(Jdbc.class); // 提取jdbc  
 *       	final var spp = proxy.findOne(ISqlPatternPreprocessor.class);// 提取SQLPattern 处理器 
 *       	final var line = spp.handle(null, null, "#createLine", null); //提取数sql语句定义
 *       	final var jdbc = Neo4jApp.getJdbc();
 *       	jdbc.withTransaction(sess -> { 
 *       		sess.sqlexecute("match (a:Vertex)-[e]->(b:Vertex) delete a,e,b"); 
 *       		sess.sqlexecute(format("create {0}",g)); <br>
 *       		final var mm = sess.sql2records("match (a:Vertex)-[e]->(b:Vertex) return a,e,b");
 *       		System.out.println(FMT(mm)); 
 *       	}); 
 *	} // foo0
 *
 *}</pre>
 * 
 * Stream&lt;IRecord&gt; 的结构创建 <br>
 * 
 * <pre>{@code
 * 
 * public void foo1() {
 * 	final var lines = lines = Stream.of("1,2,3,4,5,6,7,8,9", //
 * 			"起床/穿衣服/洗脸/刷牙/吃早饭/出门/乘地铁/到公司/开电脑/收发邮件/开始一天的工作", //
 * 			"起床/开电视/听新闻/接电话/喝牛奶/出门" //
 * 	).map(IRecord::STRING2REC);
 * 
 * 	final var neo4jApp = new Neo4jApp();
 * 	final var g = neo4jApp.graph(lines, false);
 * 	g.setVertex_name_renderer(e -> "n" + e.value());
 * 	// 设置图的属性 <br>
 * 	g.addVertexAttributeSet("n起床", REC("time", "早上", "人物", "张小宝"));
 * 	g.addEdgeAttributeSet(REC(0, "起床", 1, "开电视"), REC("time", "早上", "人物", "张小宝"));
 * 	g.addEdgeAttributeSet("n起床-n开电视", REC("time0", "早上", "人物0", "张小宝"));
 * 	System.out.println(g);
 * }
 * 
 * }</pre>
 * 
 * @author gbench
 *
 */
public class Neo4jApp {

	/**
	 *
	 */
	public Neo4jApp() {

	}

	/**
	 * 是否在创建顶点名的时候忽略顶点的所在阶层。即 a/b/a,这样的序列，是表示一个环，而不是一个直线。<br>
	 * 即是 a_ b_ a_ (注意此处的_后缀标注仅用于展示，而不是实际意义，_表示忽略掉顶点的层级) 还是<br>
	 * a0 b1 a2 (此处额0,1,2分别哦表示顶点所在的层级)<br>
	 * 
	 * @param discardLevel true忽略顶点所在的层级序号,false 保留顶点的层级序号。
	 */
	public Neo4jApp(boolean discardLevel) {
		if (discardLevel)
			this.set_intialize_handler(g -> g.setVertex_name_renderer(e -> Jdbcs.format("v{0}", e.value())));
	}

	/**
	 * @author Bean_bag 进行Hash运算
	 * 
	 * @param input 参数字符串
	 * @return 生成的hash值
	 */
	public static String md5(String input) {
		try {
			// 参数校验
			if (null == input) {
				return null;
			}
			MessageDigest md = MessageDigest.getInstance("MD5");
			md.update(input.getBytes());
			byte[] digest = md.digest();
			BigInteger bi = new BigInteger(1, digest);
			StringBuilder hashText = new StringBuilder(bi.toString(16));
			while (hashText.length() < 32) {
				hashText.insert(0, "0");
			}
			return hashText.toString();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * 图结构: 所谓图 ,理论上 就是一个二元组(V,E)即 Vertex 顶点集 与 Edge边集 的元组 .<br>
	 * <br>
	 * Graph 的实现结构，采用了 (V,E,A)的结果对二元组结果进行了扩展，加入了除了顶点与边，增加属性库集合,Attributes <br>
	 * 其中顶点集与边集 中的顶点与边 都拥有一个唯一的名字。用于标识自己， 属性库集合 A用于存放 顶点与边的属性信息。 <br>
	 * 保存结构是一个键值对集合IRecord.(Key/String:Value/Object) <br>
	 * 对于一个顶点或是边：Graph 是通过其name 在 属性仓库中进行检索/索引访问的。<br>
	 * 每个顶点或是边都拥有一个可以自定义的 vertex/edge_name_render 用于生成器唯一名称/属性集合的名称的索引。 <br>
	 * 默认的的 edge_name_render是采用 :md5(开始顶点名-结束顶点名) 的形式来给予编码的。。当然这个可以自行设置的。<br>
	 * 默认的的 vertex_name_render是采用 :直接返回原始的顶点名称。当然这个也是可以自行设置的。<br>
	 * 名称非常重要，设置vertex/edge_name_render的需要谨慎,一定要保持名称唯一性,边名不能与顶点相同,这也是为何默认实现的时候,边名左md5加密,而顶点名不做
	 * md5加密的原因。<br>
	 * 
	 * @author gbench
	 *
	 */
	public static class Graph {

		/**
		 * 获取一个顶点的Next顶点
		 * 
		 * @param predicate 顶点的等价判定
		 * @return 顶点的后继顶点
		 */
		public List<VertexInfo> getPrevs(final Predicate<VertexInfo> predicate) {
			return this.getEdges().stream().filter(e -> predicate.test(e.endNode())).map(EdgeInfo::startNode)
					.collect(Collectors.toList());
		}

		/**
		 * 根据顶点值进行前驱顶点获取 获取一个顶点的Next顶点
		 * 
		 * @param v 顶点名
		 * @return 顶点的后继顶点
		 */
		public List<VertexInfo> getPrevs2(final Object v) {
			return this.getPrevs(VEQS(v));
		}

		/**
		 * 获取一个顶点的Next顶点
		 * 
		 * @param predicate 顶点的等价判定
		 * @return 顶点的后继顶点
		 */
		public List<VertexInfo> getNexts(final Predicate<VertexInfo> predicate) {
			return this.getEdges().stream().filter(e -> predicate.test(e.startNode())).map(EdgeInfo::endNode)
					.collect(Collectors.toList());
		}

		/**
		 * 根据顶点值进行后续顶点获取 获取一个顶点的Next顶点
		 * 
		 * @param v 顶点的等价判定
		 * @return 顶点的后继顶点
		 */
		public List<VertexInfo> getNexts2(final Object v) {
			return this.getNexts(VEQS(v));
		}

		/**
		 * 获得输入的边
		 * 
		 * @param predicate 顶点的等价判定
		 * @return 顶点的后继顶点
		 */
		public List<EdgeInfo> getOutEdges(final Predicate<VertexInfo> predicate) {
			return this.getEdges().stream().filter(e -> predicate.test(e.startNode())).collect(Collectors.toList());
		}

		/**
		 * 获得输入的边
		 * 
		 * @param v 顶点的等价判定
		 * @return 顶点的后继顶点
		 */
		public List<EdgeInfo> getOutEdges2(final Object v) {
			return this.getOutEdges(VEQS(v));
		}

		/**
		 * 获得输入的边
		 * 
		 * @param predicate 顶点的等价判定
		 * @return 顶点的后继顶点
		 */
		public List<EdgeInfo> getInEdges(final Predicate<VertexInfo> predicate) {
			return this.getEdges().stream().filter(e -> predicate.test(e.endNode())).collect(Collectors.toList());
		}

		/**
		 * 获得输入的边
		 * 
		 * @param v 顶点的等价判定
		 * @return 顶点的后继顶点
		 */
		public List<EdgeInfo> getInEdges2(final Object v) {
			return this.getInEdges(VEQS(v));
		}

		/**
		 * 或如入口 顶点集合
		 * 
		 * @return 顶点的后继顶点
		 */
		public List<VertexInfo> getStarts() {
			return LIST(this.getVertexes().stream()
					.filter(e -> this.getPrevs(p -> getVertexName(e).equals(getVertexName(p))).size() == 0));
		}

		/**
		 * 获取前驱顶点集合
		 * 
		 * @return 顶点的后继顶点
		 */
		public List<VertexInfo> getEnds() {
			return LIST(this.getVertexes().stream()
					.filter(e -> this.getNexts(p -> getVertexName(e).equals(getVertexName(p))).size() == 0));
		}

		/**
		 * 顶点渲染器 <br>
		 * 顶点 又叫原子顶点，就是不可以给予在进一步细分的 数据概念。一般用于表达 数据实体。<br>
		 * 顶点就相当于一条数据记录,标签就像相当于表,顶点属性就就于记录字段。<br>
		 * 区别在于： 关系型数据库一条数据记录只能存在于一张表中，一张表可以包含多条记录，即记录与表的关系是 （many2one），<br>
		 * 而 菲关系数据库,一个顶点可以 隶属于多个标签 ，一个标签可以包含多个顶点, 即 顶点与标签的关系是
		 * (many2many),所以顶点标签是对记录表关系的泛化 <br>
		 * 
		 * @param vertex_name_renderer  顶点名渲染器
		 * @param vertex_label_renderer 顶点标签的渲染器
		 * @param vertex_attrs_renderer 顶点属性的渲染器
		 * @return 渲染顶点的cql
		 */
		public Function<VertexInfo, String> cql_vertex_renderer(Function<VertexInfo, String> vertex_name_renderer,
				Function<VertexInfo, String> vertex_label_renderer,
				Function<VertexInfo, String> vertex_attrs_renderer) {

			return v -> Jdbcs.format("({0} :{1} '{'{2}})", vertex_name_renderer.apply(v),
					vertex_label_renderer.apply(v), vertex_attrs_renderer.apply(v));// 顶点描绘器。
		}

		/**
		 * 边的渲染器
		 * 
		 * 对于边可以把边理解为 复合顶点：即由顶点构成的顶点(两个或是多个顶点即顶点序列)构成的顶点结构。最小的边 就是一个由两个原子顶点 构成的数据结构。<br>
		 * 边拥有丰富的结构化信息表达能力,它用于表达那些 依附于在其构成顶点（成分顶点)之上的数据特征的集合，所以边是一种 比顶点更为高级的数据概念。<br>
		 * 我们把这样数据信息称为关系信息。关系信息能够提供更为灵活生动信息能力，它还可以实现 以边为顶点 高阶/递归构造，进而实现 仅通过 基本的 原子顶点数据
		 * <br>
		 * 就能够实现 无穷于天地的 高阶信息，即 概念想法的 表现力。这也是 图结构的 辩证内涵。<br>
		 * 之所以把 它称为高阶顶点，是为了利用 最为基本顶点对标签的 many2many的数据关系，而这种 Value2Label即 many2many
		 * 是我们对数据进行的操作算法基础。 <br>
		 * Value 一般被称为 值对象，Label 一般被称为 Key对象/符号对象。于是 又可以通过基本的(key物质,value:意识)的
		 * (认,知)或称为阴阳辩证关系。<br>
		 * 所谓阴阳辩证 就是这是many2many 是一种可以 通过 拆分成: key(符号形式, one)->value(内容意义 many),即通过 通过符号来
		 * 启发意义 的 认知逻辑。<br>
		 * 亦可以反过来通过:内容意义的value即心有所想one->key(表达手段与方式many）的 实践逻辑。而这二者的合并 就会构成 认知实践循环：<br>
		 * many2one->one2many 其阴阳互变的动态结构，说到阴阳就是强调这种动态过程。<br>
		 * 这里采用 key->value作为一种记录的文法规则:（旨在表达一种正处于向其对立面即 辩证的二 元变化的动态过程：阳到阴 或是
		 * 阴到阳)。表示我们对这个变换过程的观察角度：<br>
		 * 这其实是一种思路的表达逻辑注意这非实际事实（事实事实是无法单向表达的，需要辩证论述），它只是为现象的方便理解，而构造出的语言或表达，即我们愿意如此相信或是想象而已。<br>
		 * 这是一种以key开始,用value结束的记法约定：这里的key与value就像会计的复式记账法一样,仅作为一种 方向标记、占位符,而已：<br>
		 * 而key,value到底是什么,即key到是代表符号形式还是内容意义，则是取决于我们能的表达需要。即我们当前的 状态：是认知还是实践。<br>
		 * 这里需要有用目的即主观的方向性来做解释。即用什么养的内容value来解释key的这种形式。<br>
		 * - 当我们是处于认知方向:key 就是符号形式，手段方法 ，value 是 内容意义。<br>
		 * - 当我们处于 实践表达方向：key 则是内心的观念想法， value 则是 表达方式与活动行为。<br>
		 * 
		 * @param vertex_name_renderer 顶点名渲染器
		 * @param edge_name_renderer   边名渲染器
		 * @param edge_label_renderer  边标签渲染器
		 * @param edge_attrs_renderer  边属性渲染器
		 * @return 边渲染的cql
		 */
		public Function<EdgeInfo, String> cql_edge_renderer(Function<VertexInfo, String> vertex_name_renderer,
				Function<EdgeInfo, String> edge_name_renderer, Function<EdgeInfo, String> edge_label_renderer,
				Function<EdgeInfo, String> edge_attrs_renderer) {

			return tup -> Jdbcs.format("({0})-[{1} :{2} '{'{3}}]->({4})", vertex_name_renderer.apply(tup.startNode()),
					edge_name_renderer.apply(tup), edge_label_renderer.apply(tup), edge_attrs_renderer.apply(tup),
					vertex_name_renderer.apply(tup.endNode()));
		}

		/**
		 * 顶点描绘
		 * 
		 * @return 顶点的渲染器
		 */
		public Function<VertexInfo, String> getVertexRenderer() {
			return this.cql_vertex_renderer(vertex_name_renderer, vertex_label_renderer, vertex_attrs_renderer);
		}

		/**
		 * 边渲染
		 * 
		 * @return 边渲染的CQL
		 */
		public Function<EdgeInfo, String> getEdgeRenderer() {
			return this.cql_edge_renderer(vertex_name_renderer, edge_name_renderer, edge_label_renderer,
					edge_attrs_renderer);
		}

		/**
		 * 获取顶点名称
		 * 
		 * @param vertex 顶点对象
		 * @return 顶点的名称
		 */
		public String getVertexName(VertexInfo vertex) {
			return this.vertex_name_renderer.apply(vertex);
		}

		/**
		 * 获取顶点标签
		 * 
		 * @param vertex 顶点对象
		 * @return 顶点的标签
		 */
		public String getVertexLabel(VertexInfo vertex) {
			return this.vertex_label_renderer.apply(vertex);
		}

		/**
		 * 获取顶点名称
		 * 
		 * @param edge 边对象
		 * @return 边的名称
		 */
		public String getEdgeName(EdgeInfo edge) {
			return this.edge_name_renderer.apply(edge);
		}

		/**
		 * 获取边的标签
		 * 
		 * @param edge 边对象
		 * @return 边的标签
		 */
		public String getEdgeLabel(EdgeInfo edge) {
			return this.edge_label_renderer.apply(edge);
		}

		/**
		 * 顶点合并
		 * 
		 * @return 属性合并之后的顶点列表。
		 */
		public Stream<VertexInfo> vertices_coalesce() {
			final var name1vtxes = this.vertices_workingcache.stream()
					.collect(Collectors.groupingBy(this::getVertexName));// 罗列历史出现的顶点。
			return name1vtxes.values().stream().map(vertexInfos -> vertexInfos.stream().reduce((a, b) -> {
				final var vtx = new VertexInfo(KVPair.KVP("-1", a.value()));// 创建顶点对象
				vtx.attributes.add(a.attributes).add(b.attributes);
				return vtx;
			}).orElse(null)).peek(vtx -> {
				final var vtxname = this.getVertexName(vtx);
				final var attrs = this.getAttributeSet(vtxname);
				if (vtx != null)
					vtx.setAttributes(attrs);// 使用图表属性集合中的顶点属性。
			});// map collect/reduce
		}

		/**
		 * 顶点合并
		 * 
		 * @return 属性合并之后的顶点列表。
		 */
		public Stream<EdgeInfo> edges_coalesce() {
			final var name1edgs = this.edges_workingcache.stream().collect(Collectors.groupingBy(this::getEdgeName));// 罗列历史出现的顶点。
			return name1edgs.values().stream().map(edgeInfos -> edgeInfos.stream().reduce((a, b) -> {
				final var edge = new EdgeInfo(a);
				edge.attributes.add(a.attributes).add(b.attributes);
				return edge;
			}).orElse(null)).peek(edg -> {
				final var edgename = this.getEdgeName(edg);// 提取边名称
				final var attrs = this.getAttributeSet(edgename);// 获得边属性
				if (edg != null)
					edg.setAttributes((IRecord) attrs);// 使用图表属性集合中的顶点属性。
			});// map collect/reduce
		}

		/**
		 * 顶点集合
		 * 
		 * @return 去重后的顶点集合
		 */
		public List<VertexInfo> getVertexes() {
			return LIST(vertices_coalesce());// 顶点合并
		}

		/**
		 * 边集合
		 * 
		 * @return 去重后的边集合
		 */
		public List<EdgeInfo> getEdges() {
			return LIST(this.edges_coalesce());
		}

		/**
		 * 为顶点或边增加一个属性
		 * 
		 * @param key   顶点名 或者 顶点名 用于唯一标注 图元素的对象
		 * @param name  属性名
		 * @param value 舒总的值
		 * @return 图对象本身，用以实现链式编程
		 */
		public Graph setAttribute(final String key, final String name, final Object value) {
			this.attr_ops.accept(attributeSets, REC("key", key, "name", name, "value", value));
			return this;
		}

		/**
		 * 为顶点或边增加一个属性
		 * 
		 * @param key 顶点名 或者 边名 用于唯一标注 图元素的 对象
		 * @param rec 属性名 ,属性名称,[(key0,value0),(key1,value1),...]的序列
		 * @return 图对象本身，用以实现链式编程
		 */
		public Graph setAttributeSet(final String key, final IRecord rec) {
			rec.foreach((name, value) -> this.setAttribute(key, name, value));
			return this;
		}

		/**
		 * 为顶点或边增加一个属性
		 * 
		 * @param key 顶点名 或者 边名 用于唯一标注 图元素的 对象
		 * @param rec 属性名 ,属性名称,[(key0,value0),(key1,value1),...]的序列
		 * @return 图对象本身，用以实现链式编程
		 */
		public Graph addAttributeSet(final String key, final IRecord rec) {
			return this.setAttributeSet(key, rec);
		}

		/**
		 * 为顶点或边增加一个属性
		 * 
		 * @param vertex 顶点名 用于唯一标注 图元素的对象
		 * @param rec    属性名 ,属性名称,[(key0,value0),(key1,value1),...]的序列
		 * @return 图对象本身，用以实现链式编程
		 */
		public Graph addVertexAttributeSet(final String vertex, final IRecord rec) {
			return this.addAttributeSet(vertex, rec);
		}

		/**
		 * 为顶点增加一个属性
		 * 
		 * @param vertices 顶点结合
		 * @param rec      属性名 ,属性名称,[(key0,value0),(key1,value1),...]的序列
		 * @return 图对象本身，用以实现链式编程
		 */
		public Graph addVertexAttributeSet(IRecord vertices, IRecord rec) {
			vertices.kvS().forEach(vertex -> this.addVertexAttributeSet(vertex, rec));
			return this;
		}

		/**
		 * 为顶点增加一个属性
		 * 
		 * @param v   顶点对象
		 * @param rec 属性名 ,属性名称,[(key0,value0),(key1,value1),...]的序列
		 * @return 图对象本身，用以实现链式编程
		 */
		public Graph addVertexAttributeSet(VertexInfo v, IRecord rec) {
			rec.foreach((name, value) -> this.setAttribute(this.vertex_name_renderer.apply(v), name, value));
			return this;
		}

		/**
		 * 为边增加属性
		 * 
		 * @param edge 边的定义
		 * @param rec  属性的集合
		 * @return 图对象本身，用以实现链式编程
		 */
		public Graph addEdgeAttributeSet(IRecord edge, IRecord rec) {
			edge.sliding2().map(e -> new EdgeInfo(e, rec)).forEach(tup -> this.addEdgeAttributeSet(tup, rec));
			return this;
		}

		/**
		 * 为边增加属性
		 * 
		 * @param edgeName 边的定义
		 * @param rec      属性的集合
		 * @return 图对象本身，用以实现链式编程
		 */
		public Graph addEdgeAttributeSet(String edgeName, IRecord rec) {
			return this.addAttributeSet(Jdbcs.format("e{0}", md5(edgeName)), rec);
		}

		/**
		 * 为顶点增加一个属性
		 * 
		 * @param tup 边的开始与结束顶点。
		 * @param rec 属性名 ,属性名称,[(key0,value0),(key1,value1),...]的序列
		 * @return 图对象本身，用以实现链式编程
		 */
		public Graph addEdgeAttributeSet(EdgeInfo tup, IRecord rec) {
			rec.foreach((name, value) -> this.setAttribute(this.edge_name_renderer.apply(tup), name, value));
			return this;
		}

		/**
		 * 获取顶点属性：若顶点属性不存在会返回一个空的IRecord，不会返回空值。
		 * 
		 * @param name 属性名
		 * @return 顶点属性
		 */
		public IRecord getAttributeSet(String name) {
			return attributeSets.getOrDefault(name, REC());
		}

		/**
		 * 根据顶点名称获取顶点对象
		 * 
		 * @param name 顶点名称
		 * @return 顶点对象,顶点名称不存在返回null
		 */
		public VertexInfo getVertexByName(String name) {
			return this.getVertexes().stream().filter(v -> v.getName().equals(name)).findFirst().orElse(null);
		}

		/**
		 * 根据边名称获取边对象
		 * 
		 * @param name 边名称
		 * @return 边对象,边名称不存在返回null
		 */
		public EdgeInfo getEdgeByName(String name) {
			return this.getEdges().stream().filter(edge -> edge.getName().equals(name)).findFirst().orElse(null);
		}

		/**
		 * 边信息结构
		 * 
		 * @author gbench
		 *
		 */
		public class EdgeInfo extends Tuple2<KVPair<String, Object>, KVPair<String, Object>> {

			/**
			 * 构造函数
			 * 
			 * @param tup2 开始顶点,结束顶点
			 */
			public EdgeInfo(Tuple2<KVPair<String, Object>, KVPair<String, Object>> tup2) {
				super(tup2._1(), tup2._2());
				this.attributes = REC();
			}

			/**
			 * 构造函数:双顶点边
			 * 
			 * @param tup2       (开始顶点,结束顶点)
			 * @param attributes 边属性
			 */
			public EdgeInfo(Tuple2<KVPair<String, Object>, KVPair<String, Object>> tup2, IRecord attributes) {
				super(tup2._1(), tup2._2());
				this.attributes = attributes;
			}

			/**
			 * 获取指定名称的属性信息,带有默认值 ：强制转换成T的目标结果类型。
			 * 
			 * @param name          属性名
			 * @param default_value 默认值
			 * @return 属性值
			 */
			public Object attr(String name, Object default_value) {
				final var value = attributes.get(name);

				return (value == null ? default_value : value);
			}

			/**
			 * 获取指定名称的属性信息,带有默认值 ：强制转换成T的目标结果类型。
			 * 
			 * @param <T>           目标结果类型
			 * @param name          属性名
			 * @param default_value 默认值
			 * @return 属性值
			 */
			@SuppressWarnings("unchecked")
			public <T> T attr2(String name, T default_value) {
				final var value = attributes.get(name);
				return (T) (value == null ? default_value : value);
			}

			/**
			 * 用mapper 对属性值name 进行变换，并返回结果
			 * 
			 * @param <T>    原键值类型
			 * @param <U>    mapper 的目标结果变换类型
			 * @param name   属性名
			 * @param mapper 属性变换器
			 * @return 属性值
			 */
			public <T, U> U attr(String name, Function<T, U> mapper) {
				return attributes.get(name, mapper);
			}

			/**
			 * 返回边的属性值
			 * 
			 * @param attr_name 属性名
			 * @return 属性值
			 */
			public Object getAttribute(String attr_name) {
				return this.attributes.get(attr_name);
			}

			/**
			 * 获取边的属性集合
			 * 
			 * @return 边的属性集合
			 */
			public IRecord getAttributes() {
				return attributes;
			}

			/**
			 * 设置边的属性集合
			 * 
			 * @param attributes 属性集合
			 */
			public void setAttributes(IRecord attributes) {
				this.attributes = attributes;
			}

			/**
			 * 起始顶点
			 * 
			 * @return 起始顶点
			 */
			public VertexInfo startNode() {
				final var g = this.getGraph();
				final var vtx = new VertexInfo(this._1());
				final var attribute = g.getAttributeSet(g.getVertexName(vtx));
				vtx.setAttributes(attribute);
				return vtx;
			}

			/**
			 * 终止顶点
			 * 
			 * @return 终止顶点
			 */
			public VertexInfo endNode() {
				final var g = this.getGraph();
				final var vtx = new VertexInfo(this._2());
				final var attribute = g.getAttributeSet(g.getVertexName(vtx));
				vtx.setAttributes(attribute);
				return vtx;
			}

			/**
			 * 获取边名称
			 * 
			 * @return 边名称
			 */
			public String getName() {
				return getEdgeName(this);
			}

			/**
			 * 获取边标签
			 * 
			 * @return 边标签
			 */
			public String getLabel() {
				return getEdgeLabel(this);
			}

			/**
			 * Graph.this.getVertexByName(name); 的简写 <br>
			 * 根据顶点名称获取顶点对象
			 * 
			 * @param name 顶点名称
			 * @return 顶点对象,顶点名称不存在返回null
			 */
			public VertexInfo getVertexByName(String name) {
				return Graph.this.getVertexByName(name);
			}

			/**
			 * 根据边名称获取边对象
			 * 
			 * @param name 边名称
			 * @return 边对象,边名称不存在返回null
			 */
			public EdgeInfo getEdgeByName(String name) {
				return Graph.this.getEdgeByName(name);
			}

			/**
			 * 获取边所隶属的图对象
			 * 
			 * @return 图对象
			 */
			public Graph getGraph() {
				return Graph.this;
			}

			/**
			 * 返回边属性值:字符串值的属性值
			 * 
			 * @param attr_name 属性名
			 * @return 属性值
			 */
			public String getProperty(String attr_name) {
				return this.attributes.str(attr_name);
			}

			/**
			 * 数据格式化
			 */
			public String toString() {
				return Jdbcs.format("{0}-->{1} with {2}", this.startNode(), this.endNode(), this.attributes);
			}

			private static final long serialVersionUID = 1L;
			private IRecord attributes;// 边属性
		}

		/**
		 * 顶点信息结构
		 * 
		 * @author gbench
		 *
		 */
		public class VertexInfo extends KVPair<String, Object> {

			/**
			 * 构造函数
			 * 
			 * @param kvp 顶点元素（层号，顶点名称）
			 */
			public VertexInfo(KVPair<String, Object> kvp) {
				super(kvp == null ? null : kvp.key(), kvp == null ? null : kvp.value());
				this.attributes = REC();
			}

			/**
			 * 构造函数
			 * 
			 * @param kvp        顶点元素（层号，顶点名称）
			 * @param attributes 顶点的属性
			 */
			public VertexInfo(KVPair<String, Object> kvp, IRecord attributes) {
				super(kvp == null ? null : kvp.key(), kvp == null ? null : kvp.value());
				this.attributes = attributes;
			}

			/**
			 * 获取指定名称的属性信息,带有默认值 ：强制转换成T的目标结果类型。
			 * 
			 * @param name          属性名
			 * @param default_value 默认值
			 * @return 属性值
			 */
			public Object attr(String name, Object default_value) {
				final var value = attributes.get(name);

				return (value == null ? default_value : value);
			}

			/**
			 * 获取指定名称的属性信息,带有默认值 ：强制转换成T的目标结果类型。
			 * 
			 * @param <T>           目标结果类型
			 * @param name          属性名
			 * @param default_value 默认值
			 * @return 属性值
			 */
			@SuppressWarnings("unchecked")
			public <T> T attr2(String name, T default_value) {
				final var value = attributes.get(name);

				return (T) (value == null ? default_value : value);
			}

			/**
			 * 用mapper 对属性值name 进行变换，并返回结果
			 * 
			 * @param <T>  原键值类型
			 * @param <U>  mapper 的目标结果变换类型
			 * @param name 属性名
			 * @return 属性值
			 */
			public <T, U> U attr(String name, Function<T, U> mapper) {
				return attributes.get(name, mapper);
			}

			/**
			 * 获取顶点的属性集合
			 * 
			 * @return 顶点的属性集合
			 */
			public IRecord getAttributes() {
				return attributes;
			}

			/**
			 * 设置顶点的属性集合
			 * 
			 * @param attributes 属性集合
			 */
			public void setAttributes(IRecord attributes) {
				this.attributes = attributes;
			}

			/**
			 * Graph.this.getVertexByName(name); 的简写 根据顶点名称获取顶点对象
			 * 
			 * @param name 顶点名称
			 * @return 顶点对象,顶点名称不存在返回null
			 */
			public VertexInfo getVertexByName(String name) {
				return Graph.this.getVertexByName(name);
			}

			/**
			 * 顶点跳转，借助于当前顶点的位置信息，根据jumpLogic的逻辑,进行目标顶点跳转。<br>
			 * <br>
			 * 一旦提出这种 jumpTo &amp; jumpLogic 跳转逻辑 的概念，我们就为顶点 提供了一种 对边(即关系)的进行抽象视角，可以这样的理解
			 * 顶点，顶点是一种依托于 jumpLogic的动态的边。<br>
			 * 即它可以根据需要（jumpLogic描述）来构建出 当前节点(a) 与 目标顶点 (b) 之间的
			 * 边(关系:a-b),注意这个跳转可以是向前:a-&gt;b,也可以向后:b-&gt;a <br>
			 * 也就是:jumpLogic不但 给出/引出 了目标节点,还给出了 这条 引出边(渲染的边)的方向性。即jumpTo 就是通过 jumpLogic
			 * 给出了完整的 双顶点边 的定义&amp;生成。<br>
			 * 所以 我们对于顶点的理解又有了一种 新的视角<br>
			 * &nbsp;&nbsp;新视角：边是基本单元,顶点是只有一个点的边(或者说是起始点均相同的边),<br>
			 * &nbsp;&nbsp;&nbsp;&nbsp;也是一个可以被jumpLogic渲染出的动态边。即虚拟边：需要借助于jumpLogic给予实体化<br>
			 * &nbsp;&nbsp;&nbsp;&nbsp;所谓一般常说的 边(关系):a-&gt;b,只是jumpLogic 为常值函数 v-&gt;"b"
			 * 的特定函数:常值函数<br>
			 * &nbsp;&nbsp;&nbsp;&nbsp;的渲染的结果。即 a-&gt;b === a.jumpTo(v-&gt;b)的特例而已。<br>
			 * VS <br>
			 * &nbsp;&nbsp;经典视角：顶点是基本单元，边是顶点的序列，是一个高阶的顶点对象。<br>
			 * 
			 * @param jumpLogic 跳转逻辑, 一个以自身顶点 为参数,目标顶点为返回值的的一元函数, this顶点对象 -&gt; 目标顶点对象
			 * @return 目标顶点
			 */
			public VertexInfo jumpTo(Function<VertexInfo, VertexInfo> jumpLogic) {
				return jumpLogic.apply(this);
			}

			/**
			 * 根据边名称获取边对象
			 * 
			 * @param name 边名称
			 * @return 边对象,边名称不存在返回null
			 */
			public EdgeInfo getEdgeByName(String name) {
				return Graph.this.getEdgeByName(name);
			}

			/**
			 * 获取顶点所隶属的图对象
			 * 
			 * @return 图对象
			 */
			public Graph getGraph() {
				return Graph.this;
			}

			/**
			 * 返回顶点属性值
			 * 
			 * @param attr_name 属性名
			 * @return 属性值
			 */
			public Object getAttribute(String attr_name) {
				return this.attributes.get(attr_name);
			}

			/**
			 * 返回顶点属性值:字符串值的属性值
			 * 
			 * @param attr_name 属性名
			 * @return 属性值
			 */
			public String getProperty(String attr_name) {
				return this.attributes.str(attr_name);
			}

			/**
			 * 获取顶点名称
			 * 
			 * @return 顶点名称
			 */
			public String getName() {
				return getVertexName(this);
			}

			/**
			 * 获取顶点标签
			 * 
			 * @return 顶点标签
			 */
			public String getLabel() {
				return getVertexLabel(this);
			}

			/**
			 * 根据前驱顶点的名称获后继顶点
			 * 
			 * @param name 后继顶点的名称
			 * @return 前驱为name的顶点对象。如果名称不存在返回null
			 */
			public VertexInfo getPrevByName(String name) {
				return this.getPrevs().stream().filter(v -> name.equals(v.getName())).findFirst().orElse(null);
			}

			/**
			 * 获取前驱顶点
			 * 
			 * @return 前驱顶点
			 */
			public List<VertexInfo> getPrevs() {
				return Graph.this.getPrevs2(this);
			}

			/**
			 * 根据后继顶点的名称获后继顶点
			 * 
			 * @param name 后继顶点的名称
			 * @return 后继为name的顶点对象。如果名称不存在返回null
			 */
			public VertexInfo getNextByName(String name) {
				return this.getNexts().stream().filter(v -> name.equals(v.getName())).findFirst().orElse(null);
			}

			/**
			 * 获取后继顶点
			 * 
			 * @return 前驱顶点
			 */
			public List<VertexInfo> getNexts() {
				return Graph.this.getNexts2(this);
			}

			/**
			 * 获取输入的边集合
			 * 
			 * @return 前驱顶点
			 */
			public List<EdgeInfo> getInEdges() {
				return Graph.this.getInEdges2(this);
			}

			/**
			 * 获取输出的边集合
			 * 
			 * @return 前驱顶点
			 */
			public List<EdgeInfo> getOutEdges() {
				return Graph.this.getOutEdges2(this);
			}

			private static final long serialVersionUID = 1L;
			private IRecord attributes;// 顶点属性。

		}

		/**
		 * @return the vertex_name_renderer
		 */
		public Function<VertexInfo, String> getVertex_name_renderer() {
			return vertex_name_renderer;
		}

		/**
		 * @param vertex_name_renderer the vertex_name_renderer to set
		 */
		public void setVertex_name_renderer(Function<VertexInfo, String> vertex_name_renderer) {
			this.vertex_name_renderer = vertex_name_renderer;
		}

		/**
		 * @return the edge_name_renderer
		 */
		public Function<EdgeInfo, String> getEdge_name_renderer() {
			return edge_name_renderer;
		}

		/**
		 * @param edge_name_renderer the edge_name_renderer to set
		 */
		public void setEdge_name_renderer(Function<EdgeInfo, String> edge_name_renderer) {
			this.edge_name_renderer = edge_name_renderer;
		}

		/**
		 * @return the vertex_label_renderer
		 */
		public Function<VertexInfo, String> getVertex_label_renderer() {
			return vertex_label_renderer;
		}

		/**
		 * @param vertex_label_renderer the vertex_label_renderer to set
		 */
		public void setVertex_label_renderer(Function<VertexInfo, String> vertex_label_renderer) {
			this.vertex_label_renderer = vertex_label_renderer;
		}

		/**
		 * @return the edge_label_renderer
		 */
		public Function<EdgeInfo, String> getEdge_label_renderer() {
			return edge_label_renderer;
		}

		/**
		 * @param edge_label_renderer the edge_label_renderer to set
		 */
		public void setEdge_label_renderer(Function<EdgeInfo, String> edge_label_renderer) {
			this.edge_label_renderer = edge_label_renderer;
		}

		/**
		 * @return the vertex_attrs_renderer
		 */
		public Function<VertexInfo, String> getVertex_attrs_renderer() {
			return vertex_attrs_renderer;
		}

		/**
		 * @param vertex_attrs_renderer the vertex_attrs_renderer to set
		 */
		public void setVertex_attrs_renderer(Function<VertexInfo, String> vertex_attrs_renderer) {
			this.vertex_attrs_renderer = vertex_attrs_renderer;
		}

		/**
		 * @return the edge_attrs_renderer
		 */
		public Function<EdgeInfo, String> getEdge_attrs_renderer() {
			return edge_attrs_renderer;
		}

		/**
		 * @param edge_attrs_renderer the edge_attrs_renderer to set
		 */
		public void setEdge_attrs_renderer(Function<EdgeInfo, String> edge_attrs_renderer) {
			this.edge_attrs_renderer = edge_attrs_renderer;
		}

		/**
		 * @return the vertices_workingcache
		 */
		public List<VertexInfo> getVertices_workingcache() {
			return vertices_workingcache;
		}

		/**
		 * @param vertices_workingcache the vertices_workingcache to set
		 */
		public void setVertices_workingcache(List<VertexInfo> vertices_workingcache) {
			this.vertices_workingcache = vertices_workingcache;
		}

		/**
		 * @return the edges_workingcache
		 */
		public List<EdgeInfo> getEdges_workingcache() {
			return edges_workingcache;
		}

		/**
		 * @param edges_workingcache the edges_workingcache to set
		 */
		public void setEdges_workingcache(List<EdgeInfo> edges_workingcache) {
			this.edges_workingcache = edges_workingcache;
		}

		/**
		 * @return the attributeSets
		 */
		public Map<String, IRecord> getAttributeSets() {
			return attributeSets;
		}

		/**
		 * @param attributeSets the attributeSets to set
		 * @return Graph 对象本身
		 */
		public Graph setAttributeSets(Map<String, IRecord> attributeSets) {
			this.attributeSets = attributeSets;
			return this;
		}

		/**
		 * 获取 图形结构属性的设置操作方法
		 * 
		 * @return the attr_ops 图形结构属性的设置操作方法 函数
		 */
		public BiConsumer<Map<String, IRecord>, IRecord> getAttr_ops() {
			return attr_ops;
		}

		/**
		 * @param attr_ops 属性设置算法 (attributeSets,rec)->{}, 例如：<br>
		 *                 (attributeSets,rec)->{<br>
		 *                 final var key = rec.str("key"); // 边名/顶点名 <br>
		 *                 final var name = rec.str("name"); // 属性名 <br>
		 *                 final var value = rec.get("value");// 属性值 <br>
		 *                 final var as = attributeSets.compute(key,
		 *                 (k,v)->(v==null?REC():v)); // 边名/顶点名key的属性集合 <br>
		 *                 as.add(name,value); //
		 *                 设置key的具体属性name的值value，默认为单值设置，即新值会覆盖老的值 <br>
		 *                 }<br>
		 * @return Graph 图对象本身
		 */
		public Graph setAttr_ops(final BiConsumer<Map<String, IRecord>, IRecord> attr_ops) {
			this.attr_ops = attr_ops;
			return this;
		}

		/**
		 * 数据格式化
		 */
		public String toString() {
			final var stream = Stream.of(// 流化处理
					distinct(vertices_workingcache.stream(), vertex_name_renderer).map(this.getVertexRenderer()),
					distinct(edges_workingcache.stream(), edge_name_renderer).map(this.getEdgeRenderer()))
					.flatMap(e -> e);
			return stream.collect(Collectors.joining(",\n"));
		}

		/**
		 * 根据 obj的运行时类型对obj对象进行引用
		 * 
		 * @param obj 待引用的对象
		 * @return 除了数值(Number的子类)，其他一律添加 “"”给括起来。
		 */
		public static String quote(Object obj) {
			if (obj instanceof Number)
				return obj + "";
			else
				return Jdbcs.format("\"{0}\"", obj + "");
		}

		/**
		 * VALUE EQs 与 obj 生成一个值相等的比较器。
		 * 
		 * @param obj 待检测的对象
		 * @return obj 是否与一个 对象的 kvp.value 相等价的判定
		 */
		public static Predicate<VertexInfo> VEQS(Object obj) {
			return (kvp) -> obj instanceof Tuple2 ? kvp.value().equals(((Tuple2<?, ?>) obj)._2())
					: kvp.value().equals(obj);
		}

		/**
		 * 判断一个模式字符串namepattern是否符合一个顶点的属性名的结构。
		 * 
		 * @param namepattern 名称结构字符串
		 */
		private final Predicate<String> is_vertex_attribute_name = namepattern -> // 判断字符串namepattern
		VERTEX_ATTRIBUTE_NAME_PATTERN.matcher(namepattern).matches();

		/**
		 * 属性的设置操作方法 rec(key:边名/顶点名,name:属性名,value:属性值)
		 */
		private BiConsumer<Map<String, IRecord>, IRecord> attr_ops = (attributeSets, rec) -> {
			final var key = rec.str("key"); // 边名/顶点名
			final var name = rec.str("name"); // 属性名
			final var value = rec.get("value"); // s属性值
			final var as = attributeSets.compute(key, (k, v) -> (v == null ? REC() : v)); // 边名/顶点名key的属性集合
			as.add(name, value); // 设置key的具体属性name的值value,默认为单值设置，即新值会覆盖老的值
		};

		/**
		 * 顶点明渲染器
		 * 
		 * @param v (顶点层号,顶点名)
		 */
		private Function<VertexInfo, String> vertex_name_renderer = (v) -> // 顶点名称生成器:
		Jdbcs.format("v{0}{1}", v.key(), v.value());

		/**
		 * 顶点标签渲染器
		 * 
		 * @param v (顶点层号,顶点名)
		 */
		private Function<VertexInfo, String> vertex_label_renderer = (v) -> // 顶点标签序列生成器 :
		String.join(":", Collections.singletonList(Jdbcs.format("{0}", "Vertex"))); // 默认顶点名为 Vertex

		/**
		 * 顶点属性渲染器
		 * 
		 * @param v (顶点层号,顶点名)
		 */
		private Function<VertexInfo, String> vertex_attrs_renderer = (v) -> // 顶点属性
		Stream.of(Jdbcs.format("level:{0}", quote(v.key())), // 顶点阶层
				Jdbcs.format("name:{0}", quote(vertex_name_renderer.apply(v))), // 顶点名称
				Jdbcs.format("{0}", this.getAttributeSet(vertex_name_renderer.apply(v)).kvs().stream()
						.map(p -> Jdbcs.format("{0}:{1}", p.key(), quote(p.value()))).collect(Collectors.joining(",")))// 顶点属性
		).filter(e -> e.length() > 0).collect(Collectors.joining(","));

		/**
		 * 边名渲染器
		 * 
		 * @param tup 边结构,(起始点(顶点层号:顶点名),终止点:(顶点层号:顶点名))
		 */
		private Function<EdgeInfo, String> edge_name_renderer = (tup) -> Jdbcs.format("e{0}",
				md5(Jdbcs.format("{0}-{1}", vertex_name_renderer.apply(tup.startNode()),
						vertex_name_renderer.apply(tup.endNode()))));

		/**
		 * 边标签渲染器
		 * 
		 * @param tup 边结构,(起始点(顶点层号:顶点名),终止点:(顶点层号:顶点名))
		 */
		private Function<EdgeInfo, String> edge_label_renderer = (tup) -> String.join(":",
				Collections.singletonList(Jdbcs.format("{0}", "Edge")));

		/**
		 * 边属性渲染器
		 * 
		 * @param tup 边结构,(起始点(顶点层号:顶点名),终止点:(顶点层号:顶点名))
		 */
		private Function<EdgeInfo, String> edge_attrs_renderer = (tup) -> Stream.of(
				Jdbcs.format("from:{0}", quote(vertex_name_renderer.apply(tup.startNode()))),
				Jdbcs.format("to:{0}", quote(vertex_name_renderer.apply(tup.endNode()))),
				Jdbcs.format("{0}", this.getAttributeSet(edge_name_renderer.apply(tup)).kvs().stream()
						.filter(e -> !this.is_vertex_attribute_name.test(e.key()))// 过滤掉顶点属性
						.map(p -> Jdbcs.format("{0}:{1}", p.key(), quote(p.value()))).collect(Collectors.joining(",")))// Jdbcs.format顶点属性
		).filter(e -> e.length() > 0).collect(Collectors.joining(","));// edge_attrs_renderer

		/**
		 * 图的顶点集合: 顶点名为路径中的(层号名,顶点名), 会出现多个同名顶点。每条字段代表边的一部分数据。
		 */
		private List<VertexInfo> vertices_workingcache = new LinkedList<>();

		/**
		 * 图的边：每个边名 [{层号名,顶点名}]的序列,会出现多个同名边，每条字段代表边的一部分数据。
		 */
		private List<EdgeInfo> edges_workingcache = new LinkedList<>();

		/**
		 * 顶点或是边的属性集合
		 */
		private Map<String, IRecord> attributeSets = new HashMap<>(); // 属性集容器

		/**
		 * 顶点属性名的 模式结构。<br>
		 * 通过 VERTEX_ATTRIBUTE_NAME_PATTERN 来识别是边属性还是顶点属性 <br>
		 * 属性结构为:节点名[$#.]属性名 <br>
		 */
		public static final Pattern VERTEX_ATTRIBUTE_NAME_PATTERN = Pattern
				.compile("(\\s*[^$#.\\s]*)\\s*([$#.]+)\\s*([^\\s]+)\\s*");

		/**
		 * EDGEINFO属性信息的标志key
		 */
		public static final String EDGE_ATTR_KEY = "$attributes";// 需要以$开头
	}

	/**
	 * 生成一个图结构 <br>
	 * 
	 * 图是边路径的集合 ,图是边路径的集合 ,边路径 是一个IRecord 结构，每个 IRecord 表示一条 顶点路径 即 IRecord 的每个
	 * KVPair(顶点阶层号,顶点名) 代表一个顶点。 <br>
	 * 比如STRING2REC(“A/B/C”) 表示KVP("0","A"),KVP("1","B"),KVP("2","C") 三个顶点 ：第0层的A，
	 * 第1层的B，第2层的C <br>
	 * 边路径属性信息一般为 边得属性 或是 构成的顶点属性 定义 路径中的顶点索引号（从0开始）#订单顶点属性名 的 IRecord <br>
	 * 
	 * 由于一条路径可以通过一个字符串来表示(比如"A/B/C"),于是 我们把 路径作为 key,而 用一个存储该路径的属性以及顶点信息的IRecord作为
	 * 值value即:构造一个 <br>
	 * [(边路径,边路径的属性信息)] 的KVPair集合 即 IRecord 来定义一个图 结构<br>
	 * 
	 * <br>
	 * 边路径的属性信息分为两类:<br>
	 * 
	 * 1)边属性 : 属性名不需要用前缀标识,比如 REC("张三/苹果",REC("name","购买")) <br>
	 * 表示 边 “张三/苹果” 的 name属性为 购买 , 即 顶点 张三 与顶点 苹果 是购买关系 <br>
	 * <br>
	 * 2)顶点属性 : 顶点属性需要用 前缀标识,比如 REC("苹果/水蜜桃", <br>
	 * REC("1#origin","烟台","2#origin","南汇","$type","商品")) 表示 <br>
	 * 苹果的产品origin产地是 烟台,水蜜桃的origin产地是南汇, 注意这里 没有设置 边“苹果/水蜜桃” 的属性<br>
	 * #用于根据 路径中的顶点序号(阶层号,从0开始)设置顶点属性,<br>
	 * $则为所有顶点都设置属性 <br>
	 * 
	 * 边属性与顶点属性可以在一个IRecord 同时设置边 与 顶点属性性 比如: <br>
	 * REC("张三/苹果",REC("name","购买","quantity",5,"date","2020-07-25","0#sex","男","0#birth","1982-6-11","1#price",5.8,"1#quality","大个"))
	 * <br>
	 * 表示:<br>
	 * 张三 性别男 出生日期为 1982-6-11 在 2020-07-25 购买了 5斤 苹果，苹果的价格是5.8元,等级为大个。 <br>
	 * <br>
	 * 
	 * 在定义边的时候 需要注意：边属性与 顶点属性名称的 命名区分：<br>
	 * 单顶点属性 :注意单顶点边的属性，名需要符合定性属性名的命名规范。例如:'$name','0#name' 或是 '苹果.price' 这样的式样。
	 * 这里的单引号'用于分隔，实际使用 不要输入。<br>
	 * 
	 * 代码示例： // 构建数据关系图 ，注意此处使用了REC2 以保证可以使用重名的键 final var g = neo4jApp.graph( REC2
	 * ( <br>
	 * // 双顶点边,注意张三买了两次苹果,但是每次的交易码 transcode 不一样。通过交易码进行边的区分。 <br>
	 * "张三/苹果",
	 * purchaseProto.derive("quality",3,"unit","斤","0#friend","王五","transcode",1),//
	 * 张三买了5斤苹果 ，站三有个朋友属性：王五 <br>
	 * "张三/苹果",
	 * purchaseProto.derive("quality",5,"unit","斤","0#friend","王五","transcode",2),//
	 * 张三买了5斤苹果 ，站三有个朋友属性：王五 <br>
	 * "张三/葡萄", purchaseProto.derive("quality",3,"unit","斤","transcode",1),//
	 * 张三买了3斤葡萄 <br>
	 * "张三/张三", teachProto.derive("subject","数学"), // 张三自学数学 "李四/张三",
	 * teachProto.derive("subject","语文"), // 李四教张三语文 // 单顶点边。<br>
	 * // 单顶点属性 :注意单顶点边的属性，名需要符合定性属性名的命名规范。例如:'$name','0#name' 或是 '苹果.price' 这样的式样。
	 * 这里的单引号'用于分隔，实际使用 不要输入。<br>
	 * "张三", profileProto.derive("$sex","女","$birth","1980-8-12"), // 张三的属性数据 <br>
	 * "苹果", productProto.derive("$price",5.8,"$title","红富士苹果","$sku","F0001"),//
	 * 苹果的属性数据 <br>
	 * "葡萄", productProto.derive("$price",9.8,"$title","龙眼葡萄","$sku","F0002") //
	 * 葡萄的属性数据。 <br>
	 * )); // graph
	 * 
	 * @param edgeLineInfos       数据行
	 *                            IRecord的流:边定义-&gt;边属性,比如：STRING2REC("zhansan/lisi")->REC(“relation",”同学“)
	 * @param reverse             是否对lines 中的数据 做reverse，即调换顺序，把"a/b/c"调换成"c/b/a"
	 * @param onVertex            接收到顶点结构的回调数据。
	 * @param onEdge              接收到边数据的回调函数
	 * @param autoconvert_sve2vtx 单顶点边是否把边属性复制到顶点上。auto_convert_single_vertex_edge2_vertex
	 * @return Graph 图结构
	 */
	public Graph graph(final PVec<IRecord, IRecord> edgeLineInfos, final boolean reverse,
			final BiConsumer<Graph.VertexInfo, Neo4jApp> onVertex, final BiConsumer<Graph.EdgeInfo, Neo4jApp> onEdge,
			boolean autoconvert_sve2vtx) {

		final var edgeInfoLines = new LinkedList<IRecord>();// 图边结构，即顶点序列
		edgeLineInfos.forEach((edge, attrs) -> {// 拼接成边结构信息:edgeInfoLine,即引入了 $attribute的键值结构。
			edgeInfoLines.add(edge.duplicate().add(Graph.EDGE_ATTR_KEY, attrs));// 为顶点数据添加边属性信息。
		});// 提取图的边结构信息
		final var g = this.graph(edgeInfoLines);// 生成一个图结构框架,即边结构图

		// 图边结构，即顶点序列的内容解析。
		edgeLineInfos.forEach((edgeInfoline, edge_attributes) -> {

			// 图边结构的解析:拆分成基本单元 双顶点边EdgeInfo而后 逐一处理。
			edgeInfoline.sliding2()// 把边结构edgeInfoline 分解成双顶点边
					.map(tup -> g.new EdgeInfo(tup, edge_attributes))// 转换双定顶点边:EdgeInfo
					.map(g.getEdge_name_renderer())// 提取双顶点边EdgeInfo的边名
					.forEach(edgeName -> {
						// 设置边的属性。
						g.addAttributeSet(edgeName, edge_attributes
								// 过滤掉顶点属性,通过 VERTEX_ATTRIBUTE_NAME_PATTERN 来识别是否是边属性。
								.filter(kvp -> !g.is_vertex_attribute_name.test(kvp._1())));// g.addAttribute 设置边属性
					});// sliding2 边流

			// 拆分成双顶点边，进而实现对边内的 顶点属性的提取。
			if (edge_attributes != null && edge_attributes.size() > 0)
				edgeInfoline.sliding2(false) // 把边结构拆分成双顶点边的。并以此依次处理。
						.forEach(edge -> {// edge 是一个包含有两个顶点的元组，第二个顶点可能为null
							// 边处理回调
							if (this.on_edge_event != null)
								this.on_edge_event.accept(g.new EdgeInfo(edge, edge_attributes), GraphEvent.PHASE2);// 搜集顶点信息,通知回调

							// 定义在边上的各个属性值的处理：函数有边属性，也有顶点属性。
							edge_attributes.foreach((atrribute_name_pattern, atrribute_value) -> {// 属性名与属性值

								if (autoconvert_sve2vtx && edge._2() == null) {// 单顶点边，表示这是一个顶点数据，所有边属性均复制到顶点中区。
									if (!g.is_vertex_attribute_name.test(atrribute_name_pattern) && // 直接不满足顶点属性
											g.is_vertex_attribute_name
													.test(Jdbcs.format("${0}", atrribute_name_pattern))) {// 添加上$前缀满足顶点属性。
										atrribute_name_pattern = Jdbcs.format("${0}", atrribute_name_pattern);// 为atrribute_name_pattern
										// 添加上$前缀，自动生成为顶点属性。
									} // 把边属性转换成顶点属性。
								} // 单顶点的边

								final var matcher = Graph.VERTEX_ATTRIBUTE_NAME_PATTERN.matcher(atrribute_name_pattern);// 顶点属性名识别器。
								if (matcher.matches()) {// 通过 matcher 来区分是否是顶点属性。

									final var vertex_name = matcher.group(1);// 属性 pattern 里面的 顶点名
									final var delim = matcher.group(2); // 属性 pattern 里面的分隔符
									final var attr_name = matcher.group(3);// 属性 pattern里面的 属性名：属性键名
									final var v0 = g.new VertexInfo(edge._1());// 开始顶点 属性集合为空。
									final var v1 = edge._2() == null ? null : g.new VertexInfo(edge._2());// 终止顶点 属性集合为空
									final var vv = new LinkedList<Graph.VertexInfo>();// namepattern 符合的顶点集合，即需要把
																						// namepattern 属性添加到的目标顶点集合。
									if (delim.matches("#+")) {// 使用序号代替顶点名,# 表示vertex_name是一个标号格式。 编号式顶点属性。
										try {
											final var no = Integer.parseInt(vertex_name);// 顶点编号
											if (v0 != null) {// 顶点编号识别,开始顶点
												final var v0_no = Integer.parseInt(v0.key());// 开始顶点的编号
												if (no == v0_no)
													vv.add(v0); // 开始顶点 判断
											}
											if (v1 != null) {// 顶点编号识别,终止顶点判断
												final var v1_no = Integer.parseInt(v1.key());// 结束顶点的编号
												if (no == v1_no)
													vv.add(v1);// 终止顶点
											} // v1
										} catch (Exception e) {
											e.printStackTrace();
										} // 使用try 防止parseInt 异常。
									} else if (delim.matches("\\.+")) {// vertex_name, 名称式 顶点属性。
										if (edge._1() != null && vertex_name.equals(edge._1().value()))
											vv.add(v0); // 开始顶点
										if (edge._2() != null && vertex_name.equals(edge._2().value()))
											vv.add(v1); // 结束顶点
									} else {// 一般用$引导，表示共同的属性,即两个顶点都有的属性。 其他(全体)式 顶点属性。即非边属性。注意此处没有设置v,v==null,这样下面的
											// if(v!==null) 就不会执行了。
										if (edge._1() != null && v0 != null)
											vv.add(v0);
										if (edge._2() != null && v1 != null)
											vv.add(v1);
									} // 点点属性的解释。

									if (vv.size() > 0) {
										vv.forEach(v -> {
											// System.out.println(Jdbcs.format("为顶点:{0} 添加 属性 {1},
											// @边[{2}]",v,REC(attr_name,atrribute_value),edge) );
											g.addVertexAttributeSet(v, REC(attr_name, atrribute_value));// 仅当名称有效。
											if (this.on_vertex_event != null)
												this.on_vertex_event.accept(v, GraphEvent.PHASE2);// 搜集顶点信息,通知回调
										});
									} // 顶点的属性添加
								} // if matcher.matches() // 顶点属性解析
							});// edge_attributes.foreach
						});// sliding2(false).forEach
		}); // edgeLineInfos.forEach

		return g;// 返回图结构
	}

	/**
	 * 生成一个图结构 <br>
	 * 
	 * 图是边路径的集合 ,边路径 是一个IRecord 结构，每个 IRecord 表示一条 顶点路径 即 IRecord 的每个
	 * KVPair(顶点阶层号,顶点名) 代表一个顶点。 <br>
	 * 比如STRING2REC(“A/B/C”) 表示KVP("0","A"),KVP("1","B"),KVP("2","C") 三个顶点 ：第0层的A，
	 * 第1层的B，第2层的C <br>
	 * 边路径属性信息一般为 边得属性 或是 构成的顶点属性 定义 路径中的顶点索引号（从0开始）#订单顶点属性名 的 IRecord <br>
	 * 
	 * 由于一条路径可以通过一个字符串来表示(比如"A/B/C"),于是 我们把 路径作为 key,而 用一个存储该路径的属性以及顶点信息的IRecord作为
	 * 值value即:构造一个 <br>
	 * [(边路径,边路径的属性信息)] 的KVPair集合 即 IRecord 来定义一个图 结构<br>
	 * 
	 * <br>
	 * 边路径的属性信息分为两类:<br>
	 * 
	 * 1)边属性 : 属性名不需要用前缀标识,比如 REC("张三/苹果",REC("name","购买")) <br>
	 * 表示 边 “张三/苹果” 的 name属性为 购买 , 即 顶点 张三 与顶点 苹果 是购买关系 <br>
	 * <br>
	 * 2)顶点属性 : 顶点属性需要用 前缀标识,比如 REC("苹果/水蜜桃", <br>
	 * REC("1#origin","烟台","2#origin","南汇","$type","商品")) 表示 <br>
	 * 苹果的产品origin产地是 烟台,水蜜桃的origin产地是南汇, 注意这里 没有设置 边“苹果/水蜜桃” 的属性<br>
	 * #用于根据 路径中的顶点序号(阶层号,从0开始)设置顶点属性,<br>
	 * $则为所有顶点都设置属性 <br>
	 * 
	 * 边属性与顶点属性可以在一个IRecord 同时设置边 与 顶点属性性 比如: <br>
	 * REC("张三/苹果",REC("name","购买","quantity",5,"date","2020-07-25","0#sex","男","0#birth","1982-6-11","1#price",5.8,"1#quality","大个"))
	 * <br>
	 * 表示:<br>
	 * 张三 性别男 出生日期为 1982-6-11 在 2020-07-25 购买了 5斤 苹果，苹果的价格是5.8元,等级为大个。 <br>
	 * <br>
	 * 
	 * 在定义边的时候 需要注意：边属性与 顶点属性名称的 命名区分： 单顶点属性
	 * :注意单顶点边的属性，名需要符合定性属性名的命名规范。例如:'$name','0#name' 或是 '苹果.price' 这样的式样。
	 * 这里的单引号'用于分隔，实际使用 不要输入。 生成一个图结构
	 * 
	 * @param edgeLineInfos 数据行
	 *                      IRecord的流:边定义-&gt;边属性,比如：STRING2REC("zhansan/lisi")->REC(“relation",”同学“)
	 * @param reverse       是否对lines 中的数据 做reverse，即调换顺序，把"a/b/c"调换成"c/b/a"
	 * @param onVertex      接收到顶点结构的回调数据。
	 * @param onEdge        接收到边数据的回调函数
	 * @return Graph 图结构
	 */
	public Graph graph(final PVec<IRecord, IRecord> edgeLineInfos, final boolean reverse,
			final BiConsumer<Graph.VertexInfo, Neo4jApp> onVertex, final BiConsumer<Graph.EdgeInfo, Neo4jApp> onEdge) {
		return this.graph(edgeLineInfos, reverse, onVertex, onEdge, false);// 不进行自动转换。
	}

	/**
	 * 
	 * 生成一个图结构 <br>
	 * 
	 * 图是边路径的结合，边路径边路径 是一个IRecord 结构，每个 IRecord 表示一条 顶点路径 即 IRecord 的每个
	 * KVPair(顶点阶层号,顶点名) 代表一个顶点。 <br>
	 * 比如STRING2REC(“A/B/C”) 表示KVP("0","A"),KVP("1","B"),KVP("2","C") 三个顶点 ：第0层的A，
	 * 第1层的B，第2层的C <br>
	 * 边路径属性信息一般为 边得属性 或是 构成的顶点属性 定义 路径中的顶点索引号（从0开始）#订单顶点属性名 的 IRecord <br>
	 * 
	 * 由于一条路径可以通过一个字符串来表示(比如"A/B/C"),于是 我们把 路径作为 key,而 用一个存储该路径的属性以及顶点信息的IRecord作为
	 * 值value即:构造一个 <br>
	 * [(边路径,边路径的属性信息)] 的KVPair集合 即 IRecord 来定义一个图 结构<br>
	 * 
	 * <br>
	 * 边路径的属性信息分为两类:<br>
	 * 
	 * 1)边属性 : 属性名不需要用前缀标识,比如 REC("张三/苹果",REC("name","购买")) <br>
	 * 表示 边 “张三/苹果” 的 name属性为 购买 , 即 顶点 张三 与顶点 苹果 是购买关系 <br>
	 * <br>
	 * 2)顶点属性 : 顶点属性需要用 前缀标识,比如 REC("苹果/水蜜桃", <br>
	 * REC("1#origin","烟台","2#origin","南汇","$type","商品")) 表示 <br>
	 * 苹果的产品origin产地是 烟台,水蜜桃的origin产地是南汇, 注意这里 没有设置 边“苹果/水蜜桃” 的属性<br>
	 * #用于根据 路径中的顶点序号(阶层号,从0开始)设置顶点属性,<br>
	 * $则为所有顶点都设置属性 <br>
	 * 
	 * 边属性与顶点属性可以在一个IRecord 同时设置边 与 顶点属性性 比如: <br>
	 * REC("张三/苹果",REC("name","购买","quantity",5,"date","2020-07-25","0#sex","男","0#birth","1982-6-11","1#price",5.8,"1#quality","大个"))
	 * <br>
	 * 表示:<br>
	 * 张三 性别男 出生日期为 1982-6-11 在 2020-07-25 购买了 5斤 苹果，苹果的价格是5.8元,等级为大个。 <br>
	 * <br>
	 * 
	 * 示例：<br>
	 * final var rel3 =
	 * app.graph(REC2((Object[])"张三/李四,喜欢1,李四/王五,喜欢2,王五/张三,喜欢3".split(",")) <br>
	 * .apply2kvs(IRecord::STRING2REC, e->REC("rel",e)));<br>
	 * 生成一个图结构 <br>
	 * 
	 * 对边属性字段,可以在定义 边属性的同时，指定顶点属性，顶点属性的语法是 (顶点名)(分隔符)(属性名)
	 * 分隔符包括：".","$","#",其中#表示边顶点的序号,0表示开始顶点,<br>
	 * 其余表示二号顶点。<br>
	 * REC2("张三/李四",R("rel","喜欢","张三.address","上海"), // 张三的address为上海 <br>
	 * "李四/王五/赵六",R("rel","喜欢","2 # address","北京"),// 赵六的address为北京 <br>
	 * "陈七/王八",R("rel","喜欢","$address","中国") // 陈七 和王八 address 都设置为中国。 <br>
	 * ).apply2keys(IRecord::STRING2REC)// 顶点定义。
	 * 
	 * @param edgeLineInfos 数据行 IRecord的流:边定义-&gt;边属性,IRecord记录的
	 *                      属性Graph.EDGE_ATTR_KEY 会被视作边属性。而给予添加到 EdgeInfo之中。<br>
	 *                      比如：STRING2REC("zhansan/lisi")->REC(“relation",”同学“)
	 * @param reverse       是否对lines 中的数据做reverse
	 * @return Graph 图结构
	 */
	public Graph graph(final PVec<IRecord, IRecord> edgeLineInfos, final boolean reverse) {
		return graph(edgeLineInfos, reverse, null, null);
	}

	/**
	 * 
	 * 生成一个图结构 <br>
	 * 
	 * 图是边路径的集合 ,边路径边路径 是一个IRecord 结构，每个 IRecord 表示一条 顶点路径 即 IRecord 的每个
	 * KVPair(顶点阶层号,顶点名) 代表一个顶点。 <br>
	 * 比如STRING2REC(“A/B/C”) 表示KVP("0","A"),KVP("1","B"),KVP("2","C") 三个顶点 ：第0层的A，
	 * 第1层的B，第2层的C <br>
	 * 边路径属性信息一般为 边得属性 或是 构成的顶点属性 定义 路径中的顶点索引号（从0开始）#订单顶点属性名 的 IRecord <br>
	 * 
	 * 由于一条路径可以通过一个字符串来表示(比如"A/B/C"),于是 我们把 路径作为 key,而 用一个存储该路径的属性以及顶点信息的IRecord作为
	 * 值value即:构造一个 <br>
	 * [(边路径,边路径的属性信息)] 的KVPair集合 即 IRecord 来定义一个图 结构<br>
	 * 
	 * <br>
	 * 边路径的属性信息分为两类:<br>
	 * 
	 * 1)边属性 : 属性名不需要用前缀标识,比如 REC("张三/苹果",REC("name","购买")) <br>
	 * 表示 边 “张三/苹果” 的 name属性为 购买 , 即 顶点 张三 与顶点 苹果 是购买关系 <br>
	 * <br>
	 * 2)顶点属性 : 顶点属性需要用 前缀标识,比如 REC("苹果/水蜜桃", <br>
	 * REC("1#origin","烟台","2#origin","南汇","$type","商品")) 表示 <br>
	 * 苹果的产品origin产地是 烟台,水蜜桃的origin产地是南汇, 注意这里 没有设置 边“苹果/水蜜桃” 的属性<br>
	 * #用于根据 路径中的顶点序号(阶层号,从0开始)设置顶点属性,<br>
	 * $则为所有顶点都设置属性 <br>
	 * 
	 * 边属性与顶点属性可以在一个IRecord 同时设置边 与 顶点属性性 比如: <br>
	 * REC("张三/苹果",REC("name","购买","quantity",5,"date","2020-07-25","0#sex","男","0#birth","1982-6-11","1#price",5.8,"1#quality","大个"))
	 * <br>
	 * 表示:<br>
	 * 张三 性别男 出生日期为 1982-6-11 在 2020-07-25 购买了 5斤 苹果，苹果的价格是5.8元,等级为大个。 <br>
	 * <br>
	 * 
	 * 示例：<br>
	 * final var rel3 =
	 * app.graph(REC2((Object[])"张三/李四,喜欢1,李四/王五,喜欢2,王五/张三,喜欢3".split(",")) <br>
	 * .apply2kvs(IRecord::STRING2REC, e->REC("rel",e)));<br>
	 * 生成一个图结构 <br>
	 * 
	 * 对边属性字段,可以在定义 边属性的同时，指定顶点属性，顶点属性的语法是 (顶点名)(分隔符)(属性名)
	 * 分隔符包括：".","$","#",其中#表示边顶点的序号,0表示开始顶点,<br>
	 * 其余表示二号顶点。<br>
	 * REC2("张三/李四",R("rel","喜欢","张三.address","上海"), // 张三的address为上海 <br>
	 * "李四/王五/赵六",R("rel","喜欢","2 # address","北京"),// 赵六的address为北京 <br>
	 * "陈七/王八",R("rel","喜欢","$address","中国") // 陈七 和王八 address 都设置为中国。 <br>
	 * ).apply2keys(IRecord::STRING2REC)// 顶点定义。
	 * 
	 * 不对边进行倒转。reverse 默认为false <br>
	 * 
	 * @param edgeLineInfos 数据行 IRecord的流:IRecord记录的 属性Graph.EDGE_ATTR_KEY
	 *                      会被视作边属性。而给予添加到 EdgeInfo之中。<br>
	 *                      边定义->边属性,比如：STRING2REC("zhansan/lisi")->REC(“relation",”同学“)
	 * @return Graph 图结构
	 */
	public Graph graph(final PVec<IRecord, IRecord> edgeLineInfos) {
		return graph(edgeLineInfos, false, null, null);
	}

	/**
	 * 
	 * 生成一个图结构 <br>
	 * 
	 * 图是边路径的集合 ,边路径边路径 是一个IRecord 结构，每个 IRecord 表示一条 顶点路径 即 IRecord 的每个
	 * KVPair(顶点阶层号,顶点名) 代表一个顶点。 <br>
	 * 比如STRING2REC(“A/B/C”) 表示KVP("0","A"),KVP("1","B"),KVP("2","C") 三个顶点 ：第0层的A，
	 * 第1层的B，第2层的C <br>
	 * 边路径属性信息一般为 边得属性 或是 构成的顶点属性 定义 路径中的顶点索引号（从0开始）#订单顶点属性名 的 IRecord <br>
	 * 
	 * 由于一条路径可以通过一个字符串来表示(比如"A/B/C"),于是 我们把 路径作为 key,而 用一个存储该路径的属性以及顶点信息的IRecord作为
	 * 值value即:构造一个 <br>
	 * [(边路径,边路径的属性信息)] 的KVPair集合 即 IRecord 来定义一个图 结构<br>
	 * 
	 * <br>
	 * 边路径的属性信息分为两类:<br>
	 * 
	 * 1)边属性 : 属性名不需要用前缀标识,比如 REC("张三/苹果",REC("name","购买")) <br>
	 * 表示 边 “张三/苹果” 的 name属性为 购买 , 即 顶点 张三 与顶点 苹果 是购买关系 <br>
	 * <br>
	 * 2)顶点属性 : 顶点属性需要用 前缀标识,比如 REC("苹果/水蜜桃", <br>
	 * REC("1#origin","烟台","2#origin","南汇","$type","商品")) 表示 <br>
	 * 苹果的产品origin产地是 烟台,水蜜桃的origin产地是南汇, 注意这里 没有设置 边“苹果/水蜜桃” 的属性<br>
	 * #用于根据 路径中的顶点序号(阶层号,从0开始)设置顶点属性,<br>
	 * $则为所有顶点都设置属性 <br>
	 * 
	 * 边属性与顶点属性可以在一个IRecord 同时设置边 与 顶点属性性 比如: <br>
	 * REC("张三/苹果",REC("name","购买","quantity",5,"date","2020-07-25","0#sex","男","0#birth","1982-6-11","1#price",5.8,"1#quality","大个"))
	 * <br>
	 * 表示:<br>
	 * 张三 性别男 出生日期为 1982-6-11 在 2020-07-25 购买了 5斤 苹果，苹果的价格是5.8元,等级为大个。 <br>
	 * <br>
	 * 
	 * 示例：<br>
	 * final var rel3 =
	 * app.graph(REC2((Object[])"张三/李四,喜欢1,李四/王五,喜欢2,王五/张三,喜欢3".split(",")) <br>
	 * .apply2kvs(IRecord::STRING2REC, e->REC("rel",e)));<br>
	 * 生成一个图结构 <br>
	 * 
	 * 对边属性字段,可以在定义 边属性的同时，指定顶点属性，顶点属性的语法是 (顶点名)(分隔符)(属性名)
	 * 分隔符包括：".","$","#",其中#表示边顶点的序号,0表示开始顶点,<br>
	 * 其余表示二号顶点。<br>
	 * REC2("张三/李四",R("rel","喜欢","张三.address","上海"), // 张三的address为上海 <br>
	 * "李四/王五/赵六",R("rel","喜欢","2 # address","北京"),// 赵六的address为北京 <br>
	 * "陈七/王八",R("rel","喜欢","$address","中国") // 陈七 和王八 address 都设置为中国。 <br>
	 * ).apply2keys(IRecord::STRING2REC)// 顶点定义。
	 * 
	 * 不对边进行倒转。reverse 默认为false <br>
	 * 
	 * @param edgeLineInfos 数据行 IRecord的流:IRecord记录的 属性Graph.EDGE_ATTR_KEY
	 *                      会被视作边属性。而给予添加到 EdgeInfo之中。<br>
	 *                      边定义->边属性,比如：STRING2REC("zhansan/lisi")->REC(“relation",”同学“)
	 * @return Graph 图结构
	 */
	public <U extends Tuple2<IRecord, IRecord>> Graph graph(final Iterable<U> edgeLineInfos) {
		return graph(new PVec<>(edgeLineInfos), false, null, null);
	}

	/**
	 * 生成一个图结构<br>
	 * 
	 * 图是边路径的集合 ,边路径边路径 是一个IRecord 结构，每个 IRecord 表示一条 顶点路径 即 IRecord 的每个
	 * KVPair(顶点阶层号,顶点名) 代表一个顶点。 <br>
	 * 比如STRING2REC(“A/B/C”) 表示KVP("0","A"),KVP("1","B"),KVP("2","C") 三个顶点 ：第0层的A，
	 * 第1层的B，第2层的C <br>
	 * 
	 * 所谓数据行lines 是一个类似于 如下的列表数据 <br>
	 * lines =Stream.of(<br>
	 * "1,2,3,4,5,6,7,8,9", <br>
	 * "起床/穿衣服/洗脸/刷牙/吃早饭/出门/乘地铁/到公司/开电脑/收发邮件/开始一天的工作",<br>
	 * "起床/开电视/听新闻/接电话/喝牛奶/出门")<br>
	 * .map(IRecord::STRING2REC);<br>
	 * 
	 * @param edgeInfoLines 数据行 IRecord的流,IRecord记录的 属性Graph.EDGE_ATTR_KEY
	 *                      会被视作边属性。而给予添加到 EdgeInfo之中。
	 * @param reverse       是否对lines 中的数据做reverse
	 * @param onVertex      接收到顶点结构的回调数据。
	 * @param onEdge        接收到边数据的回调函数
	 * @return Graph 图结构
	 */
	public Graph graph(final Stream<IRecord> edgeInfoLines, final boolean reverse,
			final BiConsumer<Graph.VertexInfo, Neo4jApp> onVertex, final BiConsumer<Graph.EdgeInfo, Neo4jApp> onEdge) {

		final var g = new Graph();// 图结构数据
		this.on_initialize_handler.accept(g);// 初始化准备
		edgeInfoLines.forEach(e -> {
			final var edge_rec = (reverse ? e.reverse() : e).filter(kvp -> !kvp._1().startsWith("$"));// 去除以符号开头的属性，剩下的就是边路径记录:edge
																										// record。
			final var edge_attributes = e.rec(Graph.EDGE_ATTR_KEY);// 提取边/顶点属性,每个 kvp 表示一个属性:key,value。

			edge_rec.sliding2(false) // 边路径记录 分解成 首尾相连的 首尾端点对儿span。
					.map(span -> g.new EdgeInfo(span, // 构造成一个边信息结构
							edge_attributes != null // 首尾端点对儿span 是否包含有属性
									? edge_attributes.filter(kvp -> !g.is_vertex_attribute_name.test(kvp.key())) // 过滤掉非边的属性（顶点属性),即提取边属性。
									: REC()))
					.forEach(edge -> {
						final List<KVPair<String, Object>> vertices = edge.tt();// 提取边的顶点结构
						vertices.stream().filter(Objects::nonNull)
								.map(v -> g.new VertexInfo(v, edge_attributes == null ? REC() // 空属性值
										: edge_attributes.filter(kvp -> g.is_vertex_attribute_name.test(kvp.key()))))// 此处添加的顶点可能包含非本顶点属性。因为没有做顶点编号判断。
								.forEach(vertex -> {// 记录顶点属性
									g.vertices_workingcache.add(vertex);
									if (this.on_vertex_event != null)
										this.on_vertex_event.accept(vertex, GraphEvent.PHASE1);// 搜集顶点信息,通知回调
								});// forEach

						// 正式把边纳入边工作缓存。
						if (edge._2() != null)
							g.edges_workingcache.add(edge);// 搜集边信息,仅仅接收双顶点的边，单顶点为 顶点定义，不予放置于edges
						if (this.on_edge_event != null)
							this.on_edge_event.accept(edge, GraphEvent.PHASE1);// 搜集边信息，可能包含单顶点边信息。通知回调
					});// sliding2
		});// forEach
		this.on_complete_handler.accept(g);

		return g;
	}

	/**
	 * 生成一个图结构<br>
	 * 
	 * 图是边路径的结合，边路径边路径 是一个IRecord 结构，每个 IRecord 表示一条 顶点路径 即 IRecord 的每个
	 * KVPair(顶点阶层号,顶点名) 代表一个顶点。 <br>
	 * 比如STRING2REC(“A/B/C”) 表示KVP("0","A"),KVP("1","B"),KVP("2","C") 三个顶点 ：第0层的A，
	 * 第1层的B，第2层的C <br>
	 * <br>
	 * 所谓数据行lines 是一个类似于 如下的列表数据 <br>
	 * lines =Stream.of(<br>
	 * "1,2,3,4,5,6,7,8,9", <br>
	 * "起床/穿衣服/洗脸/刷牙/吃早饭/出门/乘地铁/到公司/开电脑/收发邮件/开始一天的工作",<br>
	 * "起床/开电视/听新闻/接电话/喝牛奶/出门")<br>
	 * .map(IRecord::STRING2REC);<br>
	 * 
	 * @param edgeInfoLines 数据行 IRecord的流
	 * @param reverse       是否对lines 中的数据做reverse
	 * @return Graph 图结构
	 */
	public Graph graph(final Stream<IRecord> edgeInfoLines, final boolean reverse) {
		return graph(edgeInfoLines, reverse, null, null);
	}

	/**
	 * 生成一个图结构<br>
	 * 
	 * 边路径edgeInfoLines 是一个IRecord 集合，每个 IRecord 表示一条 顶点路径 即 IRecord 的每个
	 * KVPair(顶点阶层号,顶点名) 代表一个顶点。 <br>
	 * 比如STRING2REC(“A/B/C”) 表示KVP("0","A"),KVP("1","B"),KVP("2","C") 三个顶点 ：第0层的A，
	 * 第1层的B，第2层的C <br>
	 * <br>
	 * 所谓数据行lines 是一个类似于 如下的列表数据 <br>
	 * lines =Stream.of(<br>
	 * "1,2,3,4,5,6,7,8,9", <br>
	 * "起床/穿衣服/洗脸/刷牙/吃早饭/出门/乘地铁/到公司/开电脑/收发邮件/开始一天的工作",<br>
	 * "起床/开电视/听新闻/接电话/喝牛奶/出门")<br>
	 * .map(IRecord::STRING2REC);<br>
	 * 
	 * @param edgeInfoLines 数据行 IRecord的流
	 * @return Graph 图结构
	 */
	public Graph graph(final Stream<IRecord> edgeInfoLines) {
		return graph(edgeInfoLines, false, null, null);
	}

	/**
	 * 生成一个图结构<br>
	 * <br>
	 * 边路径edgeInfoLines 是一个IRecord 集合，每个 IRecord 表示一条 顶点路径 即 IRecord 的每个
	 * KVPair(顶点阶层号,顶点名) 代表一个顶点。 <br>
	 * 比如STRING2REC(“A/B/C”) 表示KVP("0","A"),KVP("1","B"),KVP("2","C") 三个顶点 ：第0层的A，
	 * 第1层的B，第2层的C <br>
	 * <br>
	 * 所谓数据行lines 是一个类似于 如下的列表数据 <br>
	 * lines =Stream.of(<br>
	 * "1,2,3,4,5,6,7,8,9", <br>
	 * "起床/穿衣服/洗脸/刷牙/吃早饭/出门/乘地铁/到公司/开电脑/收发邮件/开始一天的工作",<br>
	 * "起床/开电视/听新闻/接电话/喝牛奶/出门")<br>
	 * .map(IRecord::STRING2REC);<br>
	 * 
	 * @param edgeLineInfos 数据行 IRecord的流, 边路径集合
	 * @param reverse       是否对lines 中的数据做reverse
	 * @return Graph 图结构
	 */
	public Graph graph(final List<IRecord> edgeLineInfos, final boolean reverse) {
		return graph(edgeLineInfos.stream(), reverse, null, null);
	}

	/**
	 * 
	 * 生成一个图结构<br>
	 * <br>
	 * 图是边路径集合，边路径 是一个IRecord 结构，每个 IRecord 表示一条 顶点路径 即 IRecord 的每个
	 * KVPair(顶点阶层号,顶点名) 代表一个顶点。 <br>
	 * 比如STRING2REC(“A/B/C”) 表示KVP("0","A"),KVP("1","B"),KVP("2","C") 三个顶点 ：第0层的A，
	 * 第1层的B，第2层的C <br>
	 * <br>
	 * 所谓数据行lines 是一个类似于 如下的列表数据 <br>
	 * lines =Stream.of(<br>
	 * "1,2,3,4,5,6,7,8,9", <br>
	 * "起床/穿衣服/洗脸/刷牙/吃早饭/出门/乘地铁/到公司/开电脑/收发邮件/开始一天的工作",<br>
	 * "起床/开电视/听新闻/接电话/喝牛奶/出门")<br>
	 * .map(IRecord::STRING2REC);<br>
	 * 
	 * @param edgeInfoLines 数据行 IRecord的流，边路径集合
	 * @return Graph 图结构
	 */
	public Graph graph(final List<IRecord> edgeInfoLines) {
		return graph(edgeInfoLines.stream(), false, null, null);
	}

	/**
	 * 创建一个图结构 <br>
	 * 
	 * 图是边路径的集合 ,边路径 是一个IRecord 概念，每个 IRecord 表示一条 顶点路径 即 IRecord 的每个
	 * KVPair(顶点阶层号,顶点名) 代表一个顶点。 <br>
	 * 比如STRING2REC(“A/B/C”) 表示KVP("0","A"),KVP("1","B"),KVP("2","C") 三个顶点 ：第0层的A，
	 * 第1层的B，第2层的C <br>
	 * <br>
	 * 
	 * 由于一条路径可以通过一个字符串来表示(比如"A/B/C"),于是 我们把 路径作为 key,而 用一个存储该路径的属性以及顶点信息的IRecord作为
	 * 值value即:构造一个 <br>
	 * [(边路径,边路径的属性信息)] 的KVPair集合 即 IRecord 来定义一个图 结构<br>
	 * 
	 * <br>
	 * 边路径的属性信息分为两类:<br>
	 * 
	 * 1)边属性 : 属性名不需要用前缀标识,比如 REC("张三/苹果",REC("name","购买")) <br>
	 * 表示 边 “张三/苹果” 的 name属性为 购买 , 即 顶点 张三 与顶点 苹果 是购买关系 <br>
	 * <br>
	 * 2)顶点属性 : 顶点属性需要用 前缀标识,比如 REC("苹果/水蜜桃", <br>
	 * REC("1#origin","烟台","2#origin","南汇","$type","商品")) 表示 <br>
	 * 苹果的产品origin产地是 烟台,水蜜桃的origin产地是南汇, 注意这里 没有设置 边“苹果/水蜜桃” 的属性<br>
	 * #用于根据 路径中的顶点序号(阶层号,从0开始)设置顶点属性,<br>
	 * $则为所有顶点都设置属性 <br>
	 * 
	 * 边属性与顶点属性可以在一个IRecord 同时设置边 与 顶点属性性 比如: <br>
	 * REC("张三/苹果",REC("name","购买","quantity",5,"date","2020-07-25","0#sex","男","0#birth","1982-6-11","1#price",5.8,"1#quality","大个"))
	 * <br>
	 * 表示:<br>
	 * 张三 性别男 出生日期为 1982-6-11 在 2020-07-25 购买了 5斤 苹果，苹果的价格是5.8元,等级为大个。 <br>
	 * 
	 * <br>
	 * 示例：<br>
	 * final var purchaseProto = REC( "0#vlblk","User", "1#vlblk","Product",
	 * "elblk","Purchase", // 基本关系结构 <br>
	 * &nbsp;&nbsp;&nbsp;&nbsp;"createtime",LocalDateTime.now(),"shop","华联超市");//
	 * 购买关系 <br>
	 * final var profileProto = REC("0#vlblk","User");// 个人基本信息 <br>
	 * final var teachProto = REC( "0#vlblk","User", "1#vlblk","User",
	 * "elblk","Teach");// 教学关系<br>
	 * final var g = neo4jApp.graph( REC( <br>
	 * &nbsp;&nbsp;&nbsp;&nbsp;// 双顶点边 <br>
	 * &nbsp;&nbsp;&nbsp;&nbsp;"张三/苹果",
	 * purchaseProto.derive("quality",5,"unit","斤","0#friend","王五"),// 张三买了5斤苹果
	 * ，站三有个朋友属性：王五 <br>
	 * &nbsp;&nbsp;&nbsp;&nbsp;"张三/葡萄",
	 * purchaseProto.derive("quality",3,"unit","斤"),// 张三买了3斤葡萄 <br>
	 * &nbsp;&nbsp;&nbsp;&nbsp;"张三/张三", teachProto.derive("subject","数学"), // 张三自学数学
	 * <br>
	 * &nbsp;&nbsp;&nbsp;&nbsp;"李四/张三", teachProto.derive("subject","语文"), //
	 * 李四教张三语文 <br>
	 * &nbsp;&nbsp;&nbsp;&nbsp;// 单顶点边。 <br>
	 * &nbsp;&nbsp;&nbsp;&nbsp;"张三",
	 * profileProto.derive("$sex","女","$birth","1980-8-12") // 张三的属性数据 <br>
	 * ),false ); // graph <br>
	 * 
	 * @param edgeLineInfos 数据行的IRecord映射:边定义->边属性,比如：{"zhansan/lisi",REC(“relation",”同学“)}
	 * @param reverse       是否对edgeLineInfos中的 边数据 lines 做 reverse 首尾调转:true
	 *                      调转，false 不调转
	 * @return Graph 图结构
	 */
	public Graph graph(final IRecord edgeLineInfos, final boolean reverse) {
		final var edgeInfoLines = edgeLineInfos
				.collect(PVec.pveclc(e -> e.map((k, v) -> new Tuple2<>(STRING2REC(k), (IRecord) v)))); // 生成边的定义
		return graph(edgeInfoLines, reverse, null, null);
	}

	/**
	 * 生成一个图结构 <br>
	 * 
	 * 图是边路径的集合 ,边路径 是一个IRecord 概念，每个 IRecord 表示一条 顶点路径 即 IRecord 的每个
	 * KVPair(顶点阶层号,顶点名) 代表一个顶点。 <br>
	 * 比如STRING2REC(“A/B/C”) 表示KVP("0","A"),KVP("1","B"),KVP("2","C") 三个顶点 ：第0层的A，
	 * 第1层的B，第2层的C <br>
	 * 
	 * 由于一条路径可以通过一个字符串来表示(比如"A/B/C"),于是 我们把 路径作为 key,而 用一个存储该路径的属性以及顶点信息的IRecord作为
	 * 值value即:构造一个 <br>
	 * [(边路径,边路径的属性信息)] 的KVPair集合 即 IRecord 来定义一个图 结构<br>
	 * 
	 * <br>
	 * 边路径的属性信息分为两类:<br>
	 * 
	 * 1)边属性 : 属性名不需要用前缀标识,比如 REC("张三/苹果",REC("name","购买")) <br>
	 * 表示 边 “张三/苹果” 的 name属性为 购买 , 即 顶点 张三 与顶点 苹果 是购买关系 <br>
	 * <br>
	 * 2)顶点属性 : 顶点属性需要用 前缀标识,比如 REC("苹果/水蜜桃", <br>
	 * REC("1#origin","烟台","2#origin","南汇","$type","商品")) 表示 <br>
	 * 苹果的产品origin产地是 烟台,水蜜桃的origin产地是南汇, 注意这里 没有设置 边“苹果/水蜜桃” 的属性<br>
	 * #用于根据 路径中的顶点序号(阶层号,从0开始)设置顶点属性,<br>
	 * $则为所有顶点都设置属性 <br>
	 * 
	 * 边属性与顶点属性可以在一个IRecord 同时设置边 与 顶点属性性 比如: <br>
	 * REC("张三/苹果",REC("name","购买","quantity",5,"date","2020-07-25","0#sex","男","0#birth","1982-6-11","1#price",5.8,"1#quality","大个"))
	 * <br>
	 * 表示:<br>
	 * 张三 性别男 出生日期为 1982-6-11 在 2020-07-25 购买了 5斤 苹果，苹果的价格是5.8元,等级为大个。 <br>
	 * 
	 * 示例：<br>
	 * final var purchaseProto = REC( "0#vlblk","User", "1#vlblk","Product",
	 * "elblk","Purchase", // 基本关系结构 <br>
	 * &nbsp;&nbsp;&nbsp;&nbsp;"createtime",LocalDateTime.now(),"shop","华联超市");//
	 * 购买关系 <br>
	 * final var profileProto = REC("0#vlblk","User");// 个人基本信息 <br>
	 * final var teachProto = REC( "0#vlblk","User", "1#vlblk","User",
	 * "elblk","Teach");// 教学关系<br>
	 * final var g = neo4jApp.graph( REC( <br>
	 * &nbsp;&nbsp;&nbsp;&nbsp;// 双顶点边 <br>
	 * &nbsp;&nbsp;&nbsp;&nbsp;"张三/苹果",
	 * purchaseProto.derive("quality",5,"unit","斤","0#friend","王五"),// 张三买了5斤苹果
	 * ，站三有个朋友属性：王五 <br>
	 * &nbsp;&nbsp;&nbsp;&nbsp;"张三/葡萄",
	 * purchaseProto.derive("quality",3,"unit","斤"),// 张三买了3斤葡萄 <br>
	 * &nbsp;&nbsp;&nbsp;&nbsp;"张三/张三", teachProto.derive("subject","数学"), // 张三自学数学
	 * <br>
	 * &nbsp;&nbsp;&nbsp;&nbsp;"李四/张三", teachProto.derive("subject","语文"), //
	 * 李四教张三语文 <br>
	 * &nbsp;&nbsp;&nbsp;&nbsp;// 单顶点边。 <br>
	 * &nbsp;&nbsp;&nbsp;&nbsp;"张三",
	 * profileProto.derive("$sex","女","$birth","1980-8-12") //
	 * 张三的属性数据,要注意这里的顶点属性用$为前缀,否则会会作为边属性给予忽略 <br>
	 * )); // graph <br>
	 * 
	 * @param edgeLineInfos 数据行的IRecord映射:边定义->边属性,比如：{"zhansan/lisi",REC(“relation",”同学“)}<br>
	 *                      reverse 不对edgeLineInfos中的 边数据 lines 做 reverse 首尾调转
	 * @return Graph 图结构
	 */
	public Graph graph(final IRecord edgeLineInfos) {
		return graph(edgeLineInfos, false);
	}

	/**
	 * 顶点事件监听器
	 * 
	 * @param vertex_listener 顶点处理器
	 * @return Neo4jApp 对象本身
	 */
	public Neo4jApp set_vertex_listener(final BiConsumer<Graph.VertexInfo, GraphEvent> vertex_listener) {
		this.on_vertex_event = vertex_listener;
		return this;
	}

	/**
	 * 边事件监听器
	 * 
	 * @param edge_listener 边处理器
	 * @return Neo4jApp 对象本身
	 */
	public Neo4jApp set_edge_listener(final BiConsumer<Graph.EdgeInfo, GraphEvent> edge_listener) {
		this.on_edge_event = edge_listener;
		return this;
	}

	/**
	 * 初始化处理器
	 * 
	 * @param on_initialize_handler 初始化处理器
	 * @return Neo4jApp 对象本身
	 */
	public Neo4jApp set_intialize_handler(final Consumer<Graph> on_initialize_handler) {
		this.set_intialize_handler(on_initialize_handler, true);
		return this;
	}

	/**
	 * 初始化处理器 <br>
	 * 
	 * @param on_initialize_handler 初始化处理器
	 * @param replace               对于老的 on_initialize_handler 是否给予替换：<br>
	 *                              true 表示替换模式 也就 直接设置 on_initialize_handler
	 *                              作为新的图初始化器。<br>
	 *                              false 表示给予增量覆盖模式，即先执行老的，然后再执行新的。<br>
	 * @return Neo4jApp 对象本身
	 */
	public Neo4jApp set_intialize_handler(final Consumer<Graph> on_initialize_handler, final boolean replace) {
		final var prev = this.on_initialize_handler;
		this.on_initialize_handler = (g) -> {
			if (!replace)
				if (prev != null)
					prev.accept(g);// 增量覆盖模式
			on_initialize_handler.accept(g);
		};// on_initialize_handler
		return this;
	}

	/**
	 * 尾处理处理器
	 * 
	 * @param on_complete_handler 尾处理处理器
	 * @return Neo4jApp 对象本身
	 */
	public Neo4jApp set_complete_handler(final Consumer<Graph> on_complete_handler) {
		this.on_complete_handler = on_complete_handler;
		return this;
	}

	/**
	 * 尾处理处理器
	 * 
	 * @param on_complete_handler 尾处理处理器
	 * @param replace             对于老的 on_complete_handler 是否给予替换： true 表示替换模式 也就
	 *                            直接设置 on_complete_handler 作为新的图初始化器。<br>
	 *                            false 表示给予增量覆盖模式，即先执行老的，然后再执行新的。 <br>
	 * @return Neo4jApp 对象本身
	 */
	public Neo4jApp set_complete_handler(final Consumer<Graph> on_complete_handler, final boolean replace) {
		final var prev = this.on_complete_handler;
		this.on_complete_handler = (g) -> {
			if (!replace)
				if (prev != null)
					prev.accept(g); // 增量覆盖模式
			on_complete_handler.accept(g);
		};// on_complete_handler
		return this;
	}

	/**
	 * 特殊情况:外部 初始化图结构
	 * 
	 * @param g 图结构
	 * @return Neo4jApp 对象本身
	 */
	public Neo4jApp initialize(final Graph g) {
		if (on_initialize_handler != null)
			this.on_initialize_handler.accept(g);
		return this;
	}

	/**
	 * 特殊情况:外部 销毁图结构
	 * 
	 * @param g 图结构
	 * @return Neo4jApp 对象本身
	 */
	public Neo4jApp finalized(final Graph g) {
		if (on_complete_handler != null)
			this.on_complete_handler.accept(g);
		return this;
	}

	/**
	 * 去除重复数据
	 * 
	 * @param <T>  元素类型
	 * @param <U>  ID类型
	 * @param cc   数据源
	 * @param t2id 唯一值 id
	 * @return 唯一值的数据源流
	 */
	public static <T, U> Stream<T> distinct(final Collection<T> cc, final Function<T, U> t2id) {
		return distinct(cc.stream(), t2id);
	}

	/**
	 * 去除重复数据
	 * 
	 * @param <T>  元素类型
	 * @param <U>  ID类型
	 * @param t2id 唯一值 id
	 * @return 唯一值的数据源流
	 */
	public static <T, U> Stream<T> distinct(final Stream<T> stream, final Function<T, U> t2id) {
		@SuppressWarnings("unchecked")
		final var collector = Collector.of(() -> new AtomicReference<T>(null), (atom, a) -> atom.set((T) a),
				(aa, bb) -> aa.get() == null ? bb : aa);
		final var mm = stream.collect(Collectors.groupingBy(t2id, collector));
		return mm.values().stream().map(AtomicReference::get);
	}

	/**
	 * 测试数据库系统
	 * 
	 * @author gbench
	 *
	 */
	@JdbcConfig(url = "jdbc:neo4j:bolt://localhost/mydb?nossl", driver = "org.neo4j.jdbc.Driver", user = "neo4j")
	public interface INeo4j extends IMySQL {
	}

	/**
	 * 修改注解中的字段内容
	 * 
	 * @param <T>             注解的类的类型
	 * @param targetClass     目标位置
	 * @param annotationClass 注解类型
	 * @param field           注解中的字段名称
	 * @param value           注解中的字段值
	 */
	public static <T extends Annotation> void updateAnnotaion(final Class<?> targetClass,
			final Class<T> annotationClass, String field, String value) {
		SET_FIELD_OF_ANNOTATION(ANNO(targetClass, annotationClass), field, value);
	}

	/**
	 * 修改注解中的字段内容
	 * 
	 * @param <T>             注解的类的类型
	 * @param targetClass     目标位置
	 * @param annotationClass 注解类型
	 * @param fld2values      field/value 注解中的字段名称与字段值序列
	 */
	public static <T extends Annotation> void updateAnnotaion(final Class<?> targetClass,
			final Class<T> annotationClass, final Object... fld2values) {

		final var fvs = REC(fld2values);
		fvs.foreach((field, v) -> {
			var value = v + "";
			SET_FIELD_OF_ANNOTATION(ANNO(targetClass, annotationClass), field, value);
		});// fvs
	}

	/**
	 * 修改注解中的字段内容
	 * 
	 * @param <T>   注解的类的类型 targetClass 目标位置 默认为Database.class annotationClass 注解类型
	 *              默认为JdbcConfig.class
	 * @param field 注解中的字段名称
	 * @param value 注解中的字段值
	 */
	public static <T extends Annotation> void updateDatabaseAnnotaion(final String field, final String value) {
		SET_FIELD_OF_ANNOTATION(ANNO(INeo4j.class, JdbcConfig.class), field, value);
	}

	/**
	 * 创建一个Neo4jApp对象 <br>
	 * pov: prefix of vertex 顶点的前缀默认为 : V <br>
	 * vlblk: vetertex label key 顶点label所对应的顶点属性名称。 默认为 : vlblk, <br>
	 * 如果不设置vlblk属性即顶点的label属性,默认的顶点label 为 : “Vertex” <br>
	 * elblk: edge label key 边label所对应的边属性名称。 默认为 : elblk, <br>
	 * 如果不设置elblk属性即边的label属性,默认为边label 为 : “Edge” <br>
	 * 
	 * @param pov   顶点的前缀 Prefix Of Vertex,mo认为V
	 * @param vlblk 顶l点abel所对应的顶点属性名称(key)。 默认为vlblk, vetertex label key, <br>
	 *              注意：当在路径为顶点添加属性的时候，需要使用$,#或是.做顶点标记，<br>
	 *              否则属性会被添加到,边上去，而不是顶点上。<br>
	 *              例如：REC("a/b/c",REC($+"vlblk","Person","elblk","purchase")); //
	 *              即$+"vlblk"，才是顶点属性，表示这是一个人物顶点。 而elblk是边属性。表示一个买东西的关系。 <br>
	 * @param elblk 边label所对应的边属性名称(key)。 默认为elblk, edge label key
	 * @return Neo4jApp 对象
	 */
	public static Neo4jApp newInstance(final String pov, final String vlblk, final String elblk) {
		final var neo4jApp = new Neo4jApp();
		final var final_pov = pov == null ? "V" : pov;// 默认顶点前缀为V
		final var final_elblk = elblk == null ? "elblk" : elblk;
		final var final_vlblk = vlblk == null ? "vlblk" : vlblk;
		neo4jApp.set_intialize_handler(g -> {
			g.setVertex_name_renderer(e -> Jdbcs.format(final_pov + e.value()));// 顶点名称采用V做前缀
			g.setVertex_label_renderer(e -> {
				final var vname = g.getVertexName(e); // 顶点的名称
				final var lbls = g.getAttributeSet(vname).lla(final_vlblk, s -> s + "");// 提取顶点的label属性
				Object[] vlbls = new String[] { "Vertex" };// 默认顶点label
				if (lbls != null && lbls.size() > 0) {
					vlbls = lbls.stream().filter(s -> !s.matches("\\s*")).toArray(String[]::new); // 顶点的label过滤空白
				} // if

				return Arrays.stream(vlbls).filter(Objects::nonNull).map(Object::toString).distinct() // 过滤空值并去重
						.collect(Collectors.joining(":")); // 生成顶点label集合
			});
			g.setEdge_label_renderer(edge -> {
				final var name = g.getEdge_name_renderer().apply(edge);// 边的名称
				final List<String> elbls = g.getAttributeSet(name) //
						.path2lls(final_elblk, e -> Optional.ofNullable(e).map(String::valueOf).orElse("Edge")); // 提取边的label属性
				final var elbl = elbls != null && elbls.size() > 0 ? elbls.get(0) : null;// 把边标签映射到 属性
																							// elblk，注意边的label只取第一个属性值.
				return (elbl == null || elbl.matches("\\s*")) ? "Edge" : elbl;
			});// setEdge_label_renderer
		});// set_intialize_handler
		return neo4jApp;
	}

	/**
	 * 创建一个Neo4jApp对象 <br>
	 * pov,prefixOfVertex 顶点的前缀默认为V <br>
	 * vlblk: vetertex label key 顶点label所对应的顶点属性名称(key)。 默认为 <br>
	 * vlblk,如果不设置vlblk属性即顶点的label属性,默认为顶点 label 为 Vertex <br>
	 * elblk: edge label key 边label所对应的边属性名称(key)。 默认为 elblk, <br>
	 * 如果不设置elblk属性即边的label属性,默认为边 label 为 Edge <br>
	 * 
	 * @return Neo4jApp 对象本身
	 */
	public static Neo4jApp newInstance() {
		return newInstance(null, null, null);
	}

	/**
	 * 创建数据库对象，并提取其中的Jdbc对象。这是一个简化操作。 获取jdbc 对象
	 * 
	 * @return jdbc 对象
	 */
	public static Jdbc getJdbc() {
		final var db = Jdbc.newInstance(INeo4j.class);
		final var jdbc = db.getProxy().findOne(Jdbc.class);
		return jdbc;
	}

	/**
	 * 生成边关系信息edgeLineInfos <br>
	 * 路径 path 的模式为:<br>
	 * a/b 一对一<br>
	 * A[a,b,c] 一对多 <br>
	 * [A,B,C]a 多对一 <br>
	 * [A,B,C][a,b,c] 多对多
	 * 
	 * @param delim 分隔符模式:null表示使用逗号分隔
	 * @param rec   键值序列的IRecord形式
	 * @return SimpleRecord
	 */
	public static SimpleRecord _EDGINFOS(final String delim, final IRecord rec) {

		final var DELIM_REGEX = delim == null ? "," : delim; // 分隔符的模式
		final var PATH_SEP = "/"; // 二元关系的分隔符,开始点与终止点之间的分隔符号
		return rec.tupleS().filter(e -> !(e.key() == null || e.key().matches("\\s*"))) // 过滤掉无效的key,即 空白key
				.flatMap(kvp -> {
					final var path = kvp.key().trim(); // 提取路径key
					final var value = kvp.value(); // 提取
					final var one2many_matcher = Pattern.compile("([^\\[]+)\\s*\\[([^\\]]+)\\]").matcher(path);
					final var many2one_matcher = Pattern.compile("\\[([^\\]]+)\\]\\s*([^\\[]+)").matcher(path);
					final var many2many_matcher = Pattern.compile("\\[([^\\]]+)\\]\\s*\\[([^\\]]+)\\]").matcher(path);
					if (one2many_matcher.matches()) { // one to many
						final var partA = one2many_matcher.group(1); // 第一方
						final var partBs = one2many_matcher.group(2); // 第二方
						return Arrays.stream(partBs.split(DELIM_REGEX)).map(String::strip)
								.map(partB -> KVPair.KVP(partA + PATH_SEP + partB, value));
					} else if (many2one_matcher.matches()) { // many to one
						final var partAs = many2one_matcher.group(1); // 第一方
						final var partB = many2one_matcher.group(2); // 第二方
						return Arrays.stream(partAs.split(DELIM_REGEX)).map(String::strip)
								.map(partA -> KVPair.KVP(partA + PATH_SEP + partB, value));
					} else if (many2many_matcher.matches()) {// many to many
						final var partAs = many2many_matcher.group(1); // 第一方
						final var partBs = many2many_matcher.group(2); // 第二方
						return Arrays.stream(partAs.split(DELIM_REGEX)).map(String::strip)
								.flatMap(partA -> Arrays.stream(partBs.split(DELIM_REGEX)).map(String::strip)
										.map(partB -> KVPair.KVP(partA, partB)))
								.map(partab -> KVPair.KVP(partab._1() + PATH_SEP + partab._2(), value));
					} else { // 默认一对一的关系
						return Stream.of(kvp);
					} // if
				}).collect(IRecord.llclc(SimpleRecord::KVS2REC));// 注意一定要归并成SimpleRecord,这样才有保证不会出现同名key被覆盖的情况发生
	}

	/**
	 * 生成边关系信息edgeLineInfos <br>
	 * 路径 path 的模式为:<br>
	 * a/b 一对一<br>
	 * A[a,b,c] 一对多 <br>
	 * [A,B,C]a 多对一 <br>
	 * [A,B,C][a,b,c] 多对多
	 * 
	 * @param delim 分隔符模式:null表示使用逗号分隔
	 * @param objs  键值序列
	 * @return SimpleRecord
	 */
	public static SimpleRecord EDGINFOS(final String delim, final Object... objs) {
		final var rec = REC2(objs);
		return _EDGINFOS(delim, rec);
	}

	/**
	 * EDGINFOS 的别名 <br>
	 * 生成边关系信息edgeLineInfos <br>
	 * 生成结构关系数组 <br>
	 * <br>
	 * 默认1对1的关系 <br>
	 * A[a,b,c] 一对多 <br>
	 * [A,B,C]a 多对一 <br>
	 * [A,B,C][a,b,c] 多对多 <br>
	 * 
	 * @param objs 键值序列
	 * @return SimpleRecord
	 */
	public static SimpleRecord EIS(final Object... objs) {
		return EDGINFOS(null, objs);
	}

	/**
	 * 视边定义为(path,path属性集)的键值对儿,并对其进行归集处理,path的各个成分采用delim进行分隔<br>
	 * 把一个kvs序列归集成SimpleRecord,并对键名使用边关系EDGINFOS (EIS)进行分解处理 <br>
	 * 生成边关系信息edgeLineInfos <br>
	 * 路径 path 的模式为:<br>
	 * a/b 一对一<br>
	 * A[a,b,c] 一对多 <br>
	 * [A,B,C]a 多对一 <br>
	 * [A,B,C][a,b,c] 多对多
	 * 
	 * @param delim 分隔符,当delim 为null 时候默认为 ","
	 * @return SimpleRecord 的edgeLineInfos
	 */
	public static Collector<KVPair<String, ?>, ?, SimpleRecord> eisclc(final String delim) {
		return IRecord.llclc(kvps -> _EDGINFOS(delim, SimpleRecord.KVS2REC(kvps)));
	};

	/**
	 * 视边定义为(path,path属性集)的键值对儿,并对其进行归集处理,path的各个成分采用delim进行分隔<br>
	 * 把一个kvs序列归集成SimpleRecord<br>
	 * 并对键名使用边关系EDGINFOS (EIS)进行分解处理 <br>
	 * 键名分隔符候默认为 "," <br>
	 * <br>
	 * 生成边关系信息edgeLineInfos <br>
	 * 路径 path 的模式为:<br>
	 * a/b 一对一<br>
	 * A[a,b,c] 一对多 <br>
	 * [A,B,C]a 多对一 <br>
	 * [A,B,C][a,b,c] 多对多
	 * 
	 * @return SimpleRecord 的edgeLineInfos
	 */
	public static Collector<KVPair<String, ?>, ?, SimpleRecord> eisclc() {
		return eisclc(",");
	};

	private BiConsumer<Graph.VertexInfo, GraphEvent> on_vertex_event = // 顶点事件的处理
			(kvp, event) -> {
			};
	private BiConsumer<Graph.EdgeInfo, GraphEvent> on_edge_event = // 边事件的处理
			(kvp, event) -> {
			};
	private Consumer<Graph> on_initialize_handler = // 初始处理
			(graph) -> {
			};
	private Consumer<Graph> on_complete_handler = // 收尾处理
			(graph) -> {
			};

	public enum GraphEvent {
		PHASE1, PHASE2
	}// 图事件
}
