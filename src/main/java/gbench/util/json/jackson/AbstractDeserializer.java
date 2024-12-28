package gbench.util.json.jackson;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import gbench.util.lisp.IRecord;

/**
 * IRecord 反序列化 <br>
 * 
 * @author xuqinghua
 *
 * @param <T> 数据对象类型
 */
public abstract class AbstractDeserializer<T> extends StdDeserializer<T> {

	private static final long serialVersionUID = 637227298143614828L;

	/**
	 * 反序列化 构造函数
	 * 
	 * @param tclass 目标类名
	 */
	public AbstractDeserializer(final Class<T> tclass) {
		super(tclass);
	}

	/**
	 * 单层节点转换的ObjectNode变为IRecord
	 * 
	 * @param node 节点对象
	 * @return IRecord 对象
	 */
	public T objnode2t(final ObjectNode node) {
		final Map<String, Object> data = new LinkedHashMap<>();
		node.fieldNames().forEachRemaining(name -> {
			final JsonNode jsnode = node.get(name);
			final Object value = jsnode2value(jsnode);
			data.put(name, value);
		});
		return this.create(data);
	}

	/**
	 * 把 jsnode 转换成值类型 jsnode2value
	 * 
	 * @param jsnode JsonNode 类型的数据
	 * @return 值对象
	 */
	public Object jsnode2value(final JsonNode jsnode) {
		Object value = jsnode;

		if (jsnode instanceof ObjectNode) {
			final ObjectNode objnode = (ObjectNode) jsnode;
			value = this.objnode2t(objnode);
		} else if (jsnode instanceof ArrayNode) {
			final ArrayNode arrNode = (ArrayNode) jsnode;
			final ArrayList<Object> aa = new ArrayList<Object>(); // 数组容器
			arrNode.forEach(e -> {
				final Object obj = jsnode2value(e);
				aa.add(obj);
			}); // forEach
			value = aa;
		} else { // 基础类型
			if (jsnode.isTextual()) { // 文本
				value = jsnode.asText();
			} else if (jsnode.isBoolean()) { // BOOL类型
				value = jsnode.asBoolean();
			} else if (jsnode.isInt()) { // 整形
				value = jsnode.asInt();
			} else if (jsnode.isBigInteger()) { // 空值
				value = jsnode.bigIntegerValue();
			} else if (jsnode.isIntegralNumber()) { // 整形数值
				value = jsnode.asInt();
			} else if (jsnode.isFloat()) { // 浮点数
				value = jsnode.floatValue();
			} else if (jsnode.isFloatingPointNumber()) { // 浮点数
				value = jsnode.doubleValue();
			} else if (jsnode.isDouble()) { // 双精度
				value = jsnode.asDouble();
			} else if (jsnode.isLong()) { // 长整型
				value = jsnode.asLong();
			} else if (jsnode.isBinary()) { // 二进制类型
				try {
					value = jsnode.binaryValue();
				} catch (IOException e) {
					e.printStackTrace();
				} // try
			} else if (jsnode.isBigDecimal()) { // 大数字
				value = jsnode.decimalValue(); //
			} else if (jsnode.isNumber()) { // 数字类型
				value = jsnode.numberValue(); //
			} else if (jsnode.isEmpty()) { // 空值
				value = IRecord.REC("empty", true); // 空值类型
			} else if (jsnode.isNull()) { // 空值类型
				value = null; //
			} else {
				value = jsnode;
			} //
		} // if
		return value;
	}

	@Override
	public T deserialize(final JsonParser jp, final DeserializationContext ctxt)
			throws IOException, JsonProcessingException {
		final JsonNode node = jp.getCodec().readTree(jp);
		final Map<String, Object> tuples = new LinkedHashMap<String, Object>();

		node.fieldNames().forEachRemaining(name -> {
			final JsonNode jsnode = node.get(name);
			final Object value = jsnode2value(jsnode);
			tuples.put(name, value);
		}); // if forEachRemaining

		return this.create(tuples);
	}

	/**
	 * T 类型构造函数
	 * 
	 * @param data 中间数据结构
	 * @return T类型的值
	 */
	public abstract T create(final Map<String, Object> data);
}