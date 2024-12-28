package gbench.webapps.crawler.api.model.srch;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TotalHits;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import gbench.util.data.DataApp.IRecord;
import static gbench.util.data.DataApp.IRecord.*;

/**
 * 这是从Luke 拷贝过来的代码 Holder for a search result page.
 */
public final class PageData {

	/**
	 * 构造函数
	 * 
	 * @throws IOException
	 */
	private PageData(final TotalHits totalHits, final ScoreDoc[] docs, final int offset, final IndexSearcher searcher,
			final Set<String> fieldsToLoad) throws IOException {
		Objects.requireNonNull(docs);
		Objects.requireNonNull(searcher);

		this.offset = offset;
		this.totalHits = Objects.requireNonNull(totalHits);
		final var sfs = searcher.storedFields();
		for (final var sd : docs) {
			final var luceneDoc = (fieldsToLoad == null) ? sfs.document(sd.doc) : sfs.document(sd.doc, fieldsToLoad);
			this.hits.add(Doc.of(sd.doc, sd.score, luceneDoc));

		}
	}

	/**
	 * Creates a search result page for the given raw Lucene hits.
	 *
	 * @param totalHits    - total number of hits for this query
	 * @param docs         - array of hits
	 * @param offset       - offset of the current page
	 * @param searcher     - index searcher
	 * @param fieldsToLoad - fields to load
	 * @return the search result page
	 * @throws IOException
	 */
	public static PageData of(final TotalHits totalHits, final ScoreDoc[] docs, final int offset,
			final IndexSearcher searcher, final Set<String> fieldsToLoad) throws IOException {
		Objects.requireNonNull(docs);
		Objects.requireNonNull(searcher);
		return new PageData(totalHits, docs, offset, searcher, fieldsToLoad);
	}

	/**
	 * Returns the total number of hits for this query.
	 */
	public TotalHits getTotalHits() {
		return totalHits;
	}

	/**
	 * Returns the offset of the current page.
	 */
	public int getOffset() {
		return offset;
	}

	/**
	 * Returns the documents of the current page.
	 */
	public List<Doc> getHits() {
		return Collections.unmodifiableList(hits);
	}

	/**
	 * Returns the documents of the current page.
	 */
	public List<IRecord> getHits2() {
		return this.hitsStream().toList();
	}

	/**
	 * Returns the documents of the current page.
	 */
	public Stream<IRecord> hitsStream() {
		return this.getHits().stream().map(Doc::record);
	}

	/**
	 * 遍历查询的当前页面中结果数据
	 * 
	 * @param cs hits 的遍历函数
	 */
	public void forEach(final Consumer<IRecord> cs) {
		this.hitsStream().forEach(cs);
	}

	/**
	 * 遍历查询的当前页面中结果数据
	 * 
	 * @param cs hits 的遍历函数
	 */
	public void forEach2(final Consumer<Doc> cs) {
		this.getHits().forEach(cs);
	}

	/**
	 * Returns the size of the current page.
	 */
	public int size() {
		return hits.size();
	}

	/**
	 * Holder for a hit.
	 */
	public static class Doc {

		/**
		 * Doc
		 * 
		 * @param docId
		 * @param score
		 * @param luceneDoc
		 */
		private Doc(final int docId, final float score, final Document luceneDoc) {
			this.docId = docId;
			this.score = score;
			final Set<String> fields = luceneDoc.getFields().stream().map(IndexableField::name)
					.collect(Collectors.toSet());
			for (String f : fields) {
				this.fieldValues.put(f, luceneDoc.getValues(f));
			}
		}

		/**
		 * Creates a hit.
		 *
		 * @param docId     - document id
		 * @param score     - score of this document for the query
		 * @param luceneDoc - raw Lucene document
		 * @return the hit
		 */
		static Doc of(final int docId, final float score, final Document luceneDoc) {
			Objects.requireNonNull(luceneDoc);
			return new Doc(docId, score, luceneDoc);
		}

		/**
		 * 转换成文档记录:对于docid,和score采用
		 * 
		 * @param prefix 对于 Doc 字段采用prefix指定的前缀进行区别，当 设为null 默认为 "",表示不进行却别
		 * @return IRecord
		 */
		public IRecord record(final String prefix) {
			final var final_prefix = (prefix == null ? "" : prefix);
			final var rec = REC("docid", final_prefix + docId, "score", final_prefix + score);// 基础Doc 成员。
			return rec.derive(fldvals());
		}

		/**
		 * 把fieldValues 转换成 IRecord 记录
		 * 
		 * @return IRecord
		 */
		public IRecord fldvals() {
			final var rec = REC();// 基础Doc 成员。
			fieldValues.forEach((k, v) -> {
				if (v.length < 2) {
					rec.add(k, v[0]);
				} else {
					rec.add(k, v);
				} // if
			});// forEach
			return rec;
		}

		/**
		 * 转换成文档记录:对于docid,和score采用 对于 Doc 字段采用默认前缀，即不带有前缀。不进行却别
		 * 
		 * @return IRecord
		 */
		public IRecord record() {
			return record(null);
		}

		/**
		 * Returns the document id.
		 */
		public int getDocId() {
			return docId;
		}

		/**
		 * Returns the score of this document for the current query.
		 */
		public float getScore() {
			return score;
		}

		/**
		 * Returns the field data of this document.
		 */
		public Map<String, String[]> getFieldValues() {
			return Collections.unmodifiableMap(fieldValues);
		}

		private final int docId;
		private final float score;
		private final Map<String, String[]> fieldValues = new HashMap<>();
	}

	private final TotalHits totalHits;
	private final int offset;
	private final List<Doc> hits = new ArrayList<>();
}