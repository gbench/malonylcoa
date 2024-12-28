package gbench.webapps.crawler.api.model.srch;

import static gbench.util.data.DataApp.IRecord.*;
import static gbench.util.data.DataApp.Tuple2.*;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexOptions;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.FuzzyQuery;
import org.apache.lucene.search.PrefixQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.WildcardQuery;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.BytesRefBuilder;

import gbench.util.data.DataApp.IRecord;
import gbench.util.data.DataApp.Tuple2;

/**
 * 查询辅助类:用于根据
 *
 * @author gbench
 */
public class SrchUtils {

	/**
	 * 一个数据项目字段
	 *
	 * @param termline term行的字符串 的结构 [BooleanClause.Occur][字段名][Query的类型] <br>
	 *                 + 开头表示 BooleanClause.Occur.MUST <br>
	 *                 - 开头表示 BooleanClause.Occur.MUST_NOT <br>
	 *                 # 开头表示 BooleanClause.Occur.FILTER <br>
	 *                 空开头 即默认 表示 BooleanClause.Occur.SHOULD <br>
	 * @return {name:名称String,op:操作符BooleanClause.Ocuur,type:类型string}
	 */
	public static IRecord parseTerm(final String termline) {
		final var term_matcher = Pattern.compile(TERM_PATTERN).matcher(termline);// term行模式
		String nameline = termline; // 默认全行作为字段名
		String type = "="; // 默认采用精准匹配

		if (term_matcher.matches()) {// 行模式
			nameline = term_matcher.group(1);
			type = term_matcher.group(2);
		}

		final var tup = parseName(nameline);
		if (tup == null) {
			return null;
		}

		return REC("name", tup._1, "type", type, "op", tup._2);
	}

	/**
	 * parseName
	 * 
	 * @param nameline 名称行 [occur][symbol]
	 * @return (String,Occur)
	 */
	final static Tuple2<String, BooleanClause.Occur> parseName(final String nameline) {
		final var matcher = Pattern.compile(NAMELINE_PATTERN).matcher(nameline.strip());
		if (!matcher.matches()) {
			return null;
		}
		final var grp1 = matcher.group(1); // 第一组
		final var op = switch (grp1 == null ? "" : grp1) {// 操作符选择
		case "+" -> BooleanClause.Occur.MUST;
		case "-" -> BooleanClause.Occur.MUST_NOT;
		case "#" -> BooleanClause.Occur.FILTER;
		default -> BooleanClause.Occur.SHOULD;
		};// switch

		return TUP2(matcher.group(2), op);
	}

	/**
	 * dsl 解析
	 * 
	 * @param dsl 领域特定语言
	 * @return Query
	 */
	public static Query parseDsl(final IRecord dsl) {
		return parseDsl(dsl, null, new BooleanQuery.Builder());
	}

	/**
	 * dsl 解析
	 * 
	 * @param dsl     领域特定语言
	 * @param key     当前键名
	 * @param builder BooleanQuery 构建器
	 * @return Query
	 */
	public static Query parseDsl(final IRecord dsl, final String key, BooleanQuery.Builder builder) {
		@SuppressWarnings("unchecked")
		final var subqueries = dsl.tupleS().map(p -> p.fmap2(e -> {
			final var rec = parseTerm(p._1);
			final var type = rec.str("type");
			final var op = (BooleanClause.Occur) rec.get("op");

			if (e instanceof IRecord r) {
				return TUP2(op, parseDsl(r, p._1, new BooleanQuery.Builder()));
			} else if (e instanceof Tuple2<?, ?> tup) {
				return tup;
			} else {
				return TUP2(op, term2query(type, kvp2term(p)));
			} // if
		})).collect(IRecord.mapclc2(e -> (Tuple2<String, Tuple2<BooleanClause.Occur, Query>>) (Object) e));

		Optional.ofNullable(key).map(k -> OCCURS.get(key.toUpperCase())).ifPresentOrElse(occur -> { // 键名有效
			subqueries.values().forEach(p -> builder.add(p._2, occur));
		}, () -> { // 贱名无效, key == null
			if (subqueries.size() > 1) { // 包含多了子组件
				subqueries.forEach((k, p) -> { // 子组件
					Optional.ofNullable(k).map(e -> OCCURS.get(e.toUpperCase())).ifPresentOrElse(occur1 -> {
						builder.add(p._2, occur1);
					}, () -> {
						builder.add(p._2, p._1);
					}); // ifPresentOrElse
				}); // forEach
			} // if subqueries.size() > 1
		}); // ifPresentOrElse

		return subqueries.size() <= 1 //
				? subqueries.values().iterator().next()._2 //
				: builder.build();
	}

	/**
	 * term2query 将term包装成Query
	 * 
	 * 类型符号(type)说明 <br>
	 * '=' TermQuery 精确查询 <br>
	 * '*' WildcardQuery 通配符查询 <br>
	 * '[' TermRangeQuery 查询一个范围 没有实现 <br>
	 * '-' PrefixQuery 前缀匹配查询 <br>
	 * '#' PhraseQuery 短语查询 <br>
	 * '~' FuzzyQuery 模糊查询 <br>
	 * '@' Queryparser 万能查询（上面的都可以用这个来查询到） 没有实现 <br>
	 * 
	 * @param type Query 类型
	 * @param term 检索项
	 * @return Query
	 */
	public static Query term2query(final String type, final Term term) {
		// switch
		final Query qry = switch (type) {// term 类型
		case "=" -> new TermQuery(term); //
		case "*" -> new WildcardQuery(term);
		case "-" -> new PrefixQuery(term);
		case "~" -> new FuzzyQuery(term);
		default -> new TermQuery(term);
		};// 查询选项
		return qry;
	}

	/**
	 * 把一个rec转换成一个 Term的集合
	 *
	 * @param rec IREcord记录
	 * @return Term的集合
	 */
	public static Stream<Term> rec2terms(final IRecord rec) {
		return rec.tupleS().map(p -> {
			if (p._2 == null) {
				return null;
			} else if (p._2 instanceof Term) {
				return (Term) p._2;
			} else if (p._2 instanceof BytesRef) {
				return new Term(p._1, (BytesRef) p._2);
			} else if (p._2 instanceof BytesRefBuilder) {
				return new Term(p._1, (BytesRefBuilder) p._2);
			} else {// 默认为String类型
				return new Term(p._1, p._2 + "");
			} // if
		}).filter(Objects::nonNull);
	}

	/**
	 * 把一个记录对象转换成文档对象 <br>
	 * <p>
	 * 一 、field 前缀说明 即 类型前缀 参见VBA的类型简写 <br>
	 * $ = TextField 文本字符串(需要进行文本索引分词 ) 已经实现 <br>
	 * & = LongField 长文本 没有实现 <br>
	 * % = IntegerField 整数 没有实现 <br>
	 * ! = SingleField 单精度 没有实现 <br>
	 * # = DoubleField 双精度 没有实现 <br>
	 * @ = CurrencyField 不包含前引号 货币 没有实现 <br>
	 * 无 = 代表StringField 字符串(不需要索引分词) 已经实现 <br>
	 * <br>
	 * 二 、field 后缀（Store标记）说明: <br>
	 * &nbsp; ? 不存储 <br>
	 * &nbsp; 空 存储 <br>
	 *
	 * @param rec 记录对象: field的集合
	 * @return 把记录对象转换成 索引文件
	 */
	public static Document rec2doc(final IRecord rec) {
		final Document doc = new Document();// 创建索引文件

		// 为文档依次添加字段Field
		rec.tupleS().filter(p -> p._2 != null).map(p -> { // 依次遍历各个字段定义:kvpair(p)
			final var key = p._1;// 提取字段键名
			final var nosave = key.endsWith("?");// 问号的后缀代表这是一个临时字段
			final var field_type_and_name = nosave ? key.substring(0, key.length() - 1) : key;// 去除store标记，获取字段定义：类型+字段名

			// 定义字段的参数：name 与 value
			final var name = field_type_and_name.matches("^\\s*[a-zA-Z].*\\s*") //
					? field_type_and_name // 空类型前缀 即 默认类型
					: field_type_and_name.substring(1); // 去除类型前缀 保留 name 部分
			final var value = p._2;// 字段值

			return switch (field_type_and_name.charAt(0)) {
			case '$' -> nosave // 直接return 因此就不需要 break了
					? NOSAVE_TXTFLD(name, value) // 不存储
					: TXTFLD(name, value); // 存储
			default -> nosave // 直接return 因此就不需要 break了
					? NOSAVE_STRFLD(name, value) // 不存储
					: STRFLD(name, value); // 存储
			};// switch
		}).forEach(doc::add);// 为文档依次添加字段Field

		// System.out.println(FMT(doc));
		return doc; // 返回结果文档
	}

	/**
	 * 文档转记录
	 *
	 * @param doc 索引文件
	 * @return 记录化的索引文件
	 */
	public static IRecord doc2rec(final Document doc) {
		IRecord rec = REC();
		doc.getFields().forEach(e -> rec.add(e.name(), e.stringValue()));
		return rec;
	}

	/**
	 * kvp 转 Term
	 *
	 * @param p Tuple2 对象
	 * @return Term
	 */
	public static Term kvp2term(final Tuple2<String, Object> p) {
		final var iterm = parseTerm(p._1);// 解析键名字段
		return new Term(iterm.str("name"), p._2 == null ? null : p._2.toString());
	}

	/**
	 * 生成一个字符串字段值
	 *
	 * @param name  字段名称
	 * @param value 字段值
	 * @return 字符串字段
	 */
	public static StringField STRFLD(final String name, final Object value) {
		return new StringField(name, value == null ? null : value.toString(), Field.Store.YES);
	}

	/**
	 * 生成一个字符串字段值
	 *
	 * @param name  字段名称
	 * @param value 字段值
	 * @return 字符串字段
	 */
	public static Field TXTFLD(final String name, final Object value) {
		final var fldType = new FieldType();// 字段属性
		final var charsequence_value = value == null ? null : value.toString();// 提取值信息

		// IndexOptions 控制了 postings 中记录数据的 成分：Controls how much information is stored
		// in the postings lists.
		fldType.setIndexOptions(IndexOptions.DOCS_AND_FREQS_AND_POSITIONS_AND_OFFSETS);
		fldType.setTokenized(true);
		fldType.setStored(true);
		fldType.setStoreTermVectors(true);
		fldType.setStoreTermVectorOffsets(true);
		fldType.setStoreTermVectorPayloads(true);
		fldType.setStoreTermVectorPositions(true);
		// Prevents future changes.
		fldType.freeze();// 固定属性值不可变动

		// 生成文档字段
		final var txtfld = new Field(name, charsequence_value, fldType);

		// 返回文档字段
		return txtfld;
	}

	/**
	 * 生成一个字符串字段值:不保存，用于文档标识
	 *
	 * @param name  字段名称
	 * @param value 字段值
	 * @return 字符串字段
	 */
	public static StringField NOSAVE_STRFLD(final String name, final Object value) {
		return new StringField(name, value == null ? null : value.toString(), Field.Store.NO);
	}

	/**
	 * 生成一个字符串字段值
	 *
	 * @param name  字段名称
	 * @param value 字段值
	 * @return 字符串字段
	 */
	public static TextField NOSAVE_TXTFLD(final String name, final Object value) {
		return new TextField(name, value == null ? null : value.toString(), Field.Store.NO);
	}

	/**
	 * 格式化索引文件
	 *
	 * @param doc 检索文件
	 * @return 格式化后的而索引文件版本
	 */
	public static String FMT(final Document doc) {
		return doc.getFields().stream() //
				.map(e -> e.name() + ":" + e.stringValue()) //
				.toList().toString();
	}

	/**
	 * 把一个 objects 序列构造一个查询对象
	 *
	 * @param objects 检索条件
	 * @return Query
	 */
	public static Query Q(final Object... objects) {
		return parseDsl(REC(objects));
	}

	public static String TERM_PATTERN = "(^\\s*\\S+)\\s*([?$*#!=~])$";
	final static String NAMELINE_PATTERN = "([-#+])?([^-#+].*)";// 名称行的模式文法
	final static Map<String, BooleanClause.Occur> OCCURS = Stream.of( // (name,Occur)
			BooleanClause.Occur.FILTER //
			, BooleanClause.Occur.MUST //
			, BooleanClause.Occur.MUST_NOT //
			, BooleanClause.Occur.SHOULD //
	).collect(IRecord.mapclc2(e -> TUP2(e.name(), e)));

}
