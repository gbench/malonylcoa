package gbench.util.jdbc.kvp;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.util.Map;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.function.Function;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

/**
 * IRecord 反序列化
 * 
 * @author gbench
 *
 */
public class IRecordDeserializer extends StdDeserializer<IRecord> {

	/**
	 * 构造函数
	 */
	protected IRecordDeserializer() {
		super(IRecord.class);
	}

	@Override
	public IRecord deserialize(final JsonParser jp, final DeserializationContext ctxt)
			throws IOException, JsonProcessingException {

		final JsonNode node = jp.getCodec().readTree(jp);
		final Map<String, Object> mm = new LinkedHashMap<>();
		node.fieldNames().forEachRemaining(name -> {
			final JsonNode jsnode = node.get(name);
			final Object value = jsnode2value(jsnode);
			mm.put(name, value);
		}); // if forEachRemaining

		return IRecord.REC(mm);
	}

	/**
	 * 单层节点转换的ObjectNode变为IRecord
	 * 
	 */
	public static final Function<ObjectNode, IRecord> node2rec = IRecordDeserializer::objnode2rec;

	/**
	 * 单层节点转换的ObjectNode变为IRecord
	 * 
	 * @param node
	 * @return IRecord 对象
	 */
	public static IRecord objnode2rec(final ObjectNode node) {
		final Map<String, Object> mm = new LinkedHashMap<>();
		node.fieldNames().forEachRemaining(name -> {
			final JsonNode jsnode = node.get(name);
			final Object value = jsnode2value(jsnode);
			mm.put(name, value);
		});
		return IRecord.REC(mm);
	}

	/**
	 * 把 jsnode 转换成值类型 jsnode2value
	 * 
	 * @param jsnode JsonNode 类型的数据
	 * @return 值对象
	 */
	public static Object jsnode2value(final JsonNode jsnode) {

		Object value = jsnode;

		if (jsnode instanceof ObjectNode) {
			final ObjectNode objnode = (ObjectNode) jsnode;
			value = objnode2rec(objnode);
		} else if (jsnode instanceof ArrayNode) {
			final ArrayNode arrNode = (ArrayNode) jsnode;
			final ArrayList<Object> aa = new ArrayList<Object>(); // 数组容器
			arrNode.forEach(e -> {
				final Object obj = jsnode2value(e);
				aa.add(obj);
			}); // forEach
			value = aa;
		} else { // 基础类型
			if (jsnode.isTextual()) {
				value = jsnode.asText();
			} else if (jsnode.isDouble()) {
				value = jsnode.asDouble();
			} else if (jsnode.isInt()) {
				value = jsnode.asInt();
			} else if (jsnode.isBoolean()) {
				value = jsnode.asBoolean();
			} else if (jsnode.isFloat()) {
				value = jsnode.asDouble();
			} else if (jsnode.isLong()) {
				value = jsnode.asLong();
			} else {
				value = jsnode;
			} //
		} // if
		return value;
	}

	private static final long serialVersionUID = 637227298143614828L;

}