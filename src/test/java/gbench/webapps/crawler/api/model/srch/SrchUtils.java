package gbench.webapps.crawler.api.model.srch;

import static gbench.util.data.DataApp.IRecord.*;
import static gbench.util.data.DataApp.Tuple2.*;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collector;
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
	 * 一个数据项目字段 (
	 *
	 * @param termline term行的字符串 的结构 [BooleanClause.Occur][字段名][Query的类型] <br>
	 *                 + 开头表示 BooleanClause.Occur.MUST <br>
	 *                 - 开头表示 BooleanClause.Occur.MUST_NOT <br>
	 *                 # 开头表示 BooleanClause.Occur.FILTER <br>
	 *                 空开头 即默认 表示 BooleanClause.Occur.SHOULD <br>
	 * @return {name:名称String,op:操作符BooleanClause.Ocuur,type:类型string}
	 */
	public static IRecord parse2Term(final String termline) {
		final var term_matcher = Pattern.compile(TERM_PATTERN).matcher(termline);// term行模式
		String nameline = termline; // 默认全行作为字段名
		String type = "="; // 默认采用精准匹配

		if (term_matcher.matches()) {// 行模式
			nameline = term_matcher.group(1);
			type = term_matcher.group(2);
		}

		final var tup = namelineParser(nameline);
		if (tup == null) {
			return null;
		}

		return REC("name", tup._1, "type", type, "op", tup._2);
	}

	/**
	 * 类型符号说明 <br>
	 * '=' TermQuery 精确查询 <br>
	 * '*' WildcardQuery 通配符查询 <br>
	 * '[' TermRangeQuery 查询一个范围 没有实现 <br>
	 * '-' PrefixQuery 前缀匹配查询 <br>
	 * '#' PhraseQuery 短语查询 <br>
	 * '~' FuzzyQuery 模糊查询 <br>
	 * '@' Queryparser 万能查询（上面的都可以用这个来查询到） 没有实现 <br>
	 * <p>
	 * 生成对Term的Query 归集器
	 *
	 * @return Query 归集器
	 */
	public static Tuple2<BooleanClause.Occur, Query> parse2Query(final Tuple2<String, Term> p) {
		return Optional.ofNullable(parse2Term(p._1)) // 语句项目解析
				.map(item -> {
					final Query qry = term2query(item.str("type"), p._2);
					return TUP2((BooleanClause.Occur) item.get("op"), qry);
				}).orElse(null);
	}

	/**
	 * term2query
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
	 * kvp 转 Term
	 *
	 * @param p Tuple2 对象
	 * @return Term
	 */
	public static Term kvp2term(final Tuple2<String, Object> p) {
		final var iterm = parse2Term(p._1);// 解析键名字段
		return new Term(iterm.str("name"), p._2 == null ? null : p._2.toString());
	}

	/**
	 * 
	 * @param record
	 * @return
	 */
	public static Query getquery(final IRecord record) {
		return getquery(record, null, new BooleanQuery.Builder());
	}

	/**
	 * 
	 * @param record
	 * @param key
	 * @param builder
	 * @return
	 */
	public static Query getquery(final IRecord record, final String key, BooleanQuery.Builder builder) {
		@SuppressWarnings("unchecked")
		final var subqueries = record.tupleS().map(p -> p.fmap2(e -> {
			final var rec = parse2Term(p._1);
			final var type = rec.str("type");
			final var op = (BooleanClause.Occur) rec.get("op");
			if (e instanceof IRecord r) {
				return TUP2(op, getquery(r, p._1, new BooleanQuery.Builder()));
			} else if (e instanceof Tuple2 tup) {
				return tup;
			} else {
				return TUP2(op, term2query(type, kvp2term(p)));
			}
		})).collect(IRecord.mapclc2(e -> (Tuple2<String, Tuple2<BooleanClause.Occur, Query>>) (Object) e));

		Optional.ofNullable(key).map(k -> OCCURS.get(key.toUpperCase())).ifPresentOrElse(occur -> {
			subqueries.values().forEach(p -> {
				builder.add(p._2, occur);
			});
		}, () -> { // key == null
			if (subqueries.size() > 1) {
				subqueries.forEach((k, p) -> {
					Optional.ofNullable(k).map(e -> OCCURS.get(e.toUpperCase())).ifPresentOrElse(occur1 -> {
						builder.add(p._2, occur1);
					}, () -> {
						builder.add(p._2, p._1);
					});
				});
			}
		});

		return subqueries.size() < 1 //
				? subqueries.values().iterator().next()._2 //
				: builder.build();
	}

	/**
	 * 这是一个很难给出合适的名字的函数:它是 在一个accumulator中对一个Tuple2中的value类型为Collection的情况的
	 * 辅助函数。用以实现 递归分解。
	 *
	 * @param vv 集合类型的对象。
	 * @return Tuple2的列表
	 */
	private static List<Tuple2<String, Term>> typecase_for_collection_in_accumulator(final Collection<?> vv) {
		final var aa = new LinkedList<Tuple2<String, Term>>();
		for (var v : vv) {
			if (v instanceof IRecord rec) {
				final var aa1 = rec.collect(terms_clc(e -> e));
				aa.addAll(aa1);
			} // if
		} // for
		return aa;
	}

	public static BiConsumer<List<Tuple2<String, Term>>, Tuple2<String, Object>> accumulator = (aa, p) -> {// 元素累加
		if (p._2 == null)
			return;
		if (p._2 instanceof Collection) // 集合类结构的
			aa.addAll(typecase_for_collection_in_accumulator((Collection<?>) p._2));
		else if (p._2.getClass().isArray()) // 集合类结构的
			aa.addAll(typecase_for_collection_in_accumulator(Arrays.asList((Object[]) p._2)));
		else
			aa.add(P(p._1, kvp2term(p)));
	};

	/**
	 * qryclc
	 *
	 * @param mapper 映射函数
	 * @return Term 项目的编辑器
	 */
	public static <U> Collector<Tuple2<String, Object>, List<Tuple2<String, Term>>, U> terms_clc(
			final Function<List<Tuple2<String, Term>>, U> mapper) {
		return Collector.of(LinkedList::new, // 累加器构造
				accumulator, // 元素累加
				(aa, bb) -> {
					aa.addAll(bb);
					return aa;
				},

				// 整合器 ,归并处理,量变促成质变
				mapper);
	}

	/**
	 * qryclc
	 *
	 * @param cs 回调
	 * @return 搜集与整理
	 */
	public static Collector<Tuple2<String, Object>, List<Tuple2<String, Term>>, BooleanQuery> bool_query_clc(
			final BiConsumer<List<Tuple2<String, Term>>, BooleanQuery.Builder> cs) {
		return Collector.of(LinkedList::new, // 累加器构造
				accumulator, // 元素累加
				(aa, bb) -> {
					aa.addAll(bb);
					return aa;
				},
				// 整合器
				ll -> {// 归并处理,量变促成质变
					final var builder = new BooleanQuery.Builder();
					cs.accept(ll, builder);
					return builder.build();
				}// ll
		);
	}

	/**
	 * 生成对Term的Query 归集器
	 */
	public static Collector<Tuple2<String, Object>, List<Tuple2<String, Term>>, BooleanQuery> bool_query_clc = bool_query_clc(
			(ll, builder) -> ll.stream().map(SrchUtils::parse2Query).filter(Objects::nonNull)
					.forEach(e -> builder.add(e._2, e._1)));

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
		return doc.getFields().stream().map(e -> e.name() + ":" + e.stringValue()).toList().toString();
	}

	/**
	 * 把一个 objects 序列构造一个查询对象
	 *
	 * @param objects 检索条件
	 * @return Query
	 */
	public static Query Q(final Object... objects) {
		return rec2boolquery(REC(objects));
	}

	/**
	 * 把一个rec转换成一个 Term的集合
	 *
	 * @param rec IREcord记录
	 * @return Term的集合
	 */
	public static Stream<Term> rec2terms(final IRecord rec) {
		return rec.tupleS().map(p -> {
			if (p._2 == null)
				return null;
			if (p._2 instanceof Term) {
				return (Term) p._2;
			}
			if (p._2 instanceof BytesRef) {
				return new Term(p._1, (BytesRef) p._2);
			} else if (p._2 instanceof BytesRefBuilder) {
				return new Term(p._1, (BytesRefBuilder) p._2);
			} else {// 默认为String类型
				return new Term(p._1, p._2 + "");
			} // if

		}).filter(Objects::nonNull);
	}

	/**
	 * 把一个rec转换成一个 Query 对象
	 *
	 * @param rec IREcord记录
	 * @return Query 对象
	 */
	public static Query rec2boolquery(final IRecord rec) {
		return rec.tupleS().collect(SrchUtils.bool_query_clc);
	}

	/**
	 * 把一个记录对象转换成文档对象 <br>
	 * <p>
	 * 一 、field 前缀说明 即 类型前缀 参见VBA的类型简写 <br>
	 * &nbsp; $ = TextField 文本字符串(需要进行文本索引分词 ) 已经实现 <br>
	 * &nbsp; & = LongField 长文本 没有实现 <br>
	 * &nbsp; % = IntegerField 整数 没有实现 <br>
	 * &nbsp; ! = SingleField 单精度 没有实现 <br>
	 * &nbsp; # = DoubleField 双精度 没有实现 <br>
	 * &nbsp; @ = CurrencyField 不包含前引号 货币 没有实现 <br>
	 * &nbsp; 无 = 代表StringField 字符串(不需要索引分词) 已经实现 <br>
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
			final var name = field_type_and_name.matches("^\\s*[a-zA-Z].*\\s*") ? field_type_and_name // 空类型前缀 即 默认类型
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
	 * 
	 * @param nameline
	 * @return
	 */
	final static Tuple2<String, BooleanClause.Occur> namelineParser(final String nameline) {
		final var mth = Pattern.compile(NAMELINE_PATTERN).matcher(nameline.trim());
		if (!mth.matches()) {
			return null;
		}
		final var grp1 = mth.group(1);
		final var op = switch (grp1 == null ? "" : grp1) {// 操作符选择
		case "+" -> BooleanClause.Occur.MUST;
		case "-" -> BooleanClause.Occur.MUST_NOT;
		case "#" -> BooleanClause.Occur.FILTER;
		default -> BooleanClause.Occur.SHOULD;
		};// switch
		return TUP2(mth.group(2), op);
	};// name_parser

	public static String TERM_PATTERN = "(^\\s*\\S+)\\s*([?$*#!=~])$";
	final static String NAMELINE_PATTERN = "([-#+])?([^-#+].*)";// 名称行的模式文法
	final static Map<String, BooleanClause.Occur> OCCURS = Stream.of( // (name,Occur)
			BooleanClause.Occur.FILTER //
			, BooleanClause.Occur.MUST //
			, BooleanClause.Occur.MUST_NOT //
			, BooleanClause.Occur.SHOULD //
	).collect(IRecord.mapclc2(e -> TUP2(e.name(), e)));

}
