package gbench.webapps.crawler.api.model.decomposer;

import gbench.util.data.DataApp.IRecord;

/**
 * 分词器:解构 把一个 流式子结构的文档给 分解成 索引字段结构 <br>
 * REC(// 加入字段分析结果 <br>
 * "symbol", symbol, // 关键词 <br>
 * "statement", statement, // 关键词所在的上下文语句 <br>
 * "file", fname, // 文件名称 <br>
 * "type", "text", // 文本类型 , 图片类型 等 <br>
 * "position", position, // 关键字的位置{rownum,start,end} <br>
 * "snapfile", snapfile // 快照文件位置路径 <br>
 * "tags", tags // 词汇标签 <br>
 * );// 索引字段记录 <br>
 *
 * @author gbench
 */
public interface IDecomposer {
	/**
	 * 文本index
	 *
	 * @param entry 记录需要包含一下字段 <br>
	 *              - File file 待分词的文件 <br>
	 *              - YuhuanAnalyzer analyzer 分词器 <br>
	 *              - ConcurrentLinkedQueue&lt;IRecord&gt;tokens [out] 类型参数
	 *              ,tokens的元素是一个IRecord对象需要包括:<br>
	 *              symbol:关键词,<br>
	 *              statement:上下文语句摘要, <br>
	 *              file:文件名, 以及 type:媒体类型 四个字段 特殊补充字段 <br>
	 *              position 关键词的索引位置:{rownum,sart,end}的IRecord<br>
	 *              snapfile 快照文件的位置:{rownum,sart,end}的IRecord<br>
	 *              - ConcurrentLinkedQueue&lt;IRecord&gt;files[out] 类型参数处理的文件集合
	 *              <br>
	 */
	public void decompose(IRecord entry);
}
