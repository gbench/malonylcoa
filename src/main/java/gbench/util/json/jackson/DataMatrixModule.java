package gbench.util.json.jackson;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import gbench.util.data.xls.DataMatrix;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.StreamSupport;

/**
 * DFrame 数据模块
 */
public class DataMatrixModule extends SimpleModule {

	/**
	 *
	 */
	private static final long serialVersionUID = -7266350024200887114L;

	/**
	 * MySerializer
	 * 
	 * @param <T> Iterable的类型结构
	 */
	public static class MySerializer<T extends Iterable<?>> extends StdSerializer<T> {

		/**
		 *
		 */
		private static final long serialVersionUID = -8293652782470526341L;

		/**
		 * 构造函数
		 *
		 * @param tclass 类Class
		 */
		protected MySerializer(final Class<T> tclass) {
			super(tclass);
		}

		@Override
		public void serialize(final T t, final JsonGenerator jsonGenerator, final SerializerProvider serializerProvider)
				throws IOException {
			final Object[] oo = StreamSupport.stream(t.spliterator(), false).toArray(Object[]::new);
			final List<String> keys = new ArrayList<String>();
			try {
				final Method method = t.getClass().getMethod("keys", new Class<?>[] {});
				@SuppressWarnings("unchecked")
				List<String> _keys = (List<String>) method.invoke(t, new Object[] {});
				keys.addAll(_keys);
			} catch (Exception e) {
				e.printStackTrace();
			}

			jsonGenerator.writeStartObject();
			// keys
			jsonGenerator.writeFieldName("keys");
			jsonGenerator.writeObject(keys);
			// datas
			jsonGenerator.writeFieldName("datas");
			jsonGenerator.writeStartArray();
			for (final Object o : oo) {
				jsonGenerator.writeObject(o);
			}
			jsonGenerator.writeEndArray();

			jsonGenerator.writeEndObject();
		}
	}

	/**
	 * MyDeserializer
	 * 
	 * @param <T> Iterable的类型结构
	 */
	public static abstract class MyDeserializer<T extends Iterable<?>> extends StdDeserializer<T> {

		/**
		 *
		 */
		private static final long serialVersionUID = -1501276456923937018L;

		/**
		 * 构造函数
		 *
		 * @param tclass 类型类
		 */
		@SuppressWarnings("rawtypes")
		protected MyDeserializer(final Class<DataMatrix> tclass) {
			super(tclass);
		}

		@Override
		public T deserialize(final JsonParser jsonParser, final DeserializationContext deserializationContext)
				throws IOException, JacksonException {
			final JsonNode node = jsonParser.getCodec().readTree(jsonParser);

			// 提取属性
			final JsonNode keys_node = node.get("keys");
			final JsonNode datas_node = node.get("datas");

			if (datas_node.isArray()) { // 数组类型

				// datas
				final ArrayNode datas_arraynode = (ArrayNode) datas_node;
				final Object[][] datas = objM.convertValue(datas_arraynode, new TypeReference<Object[][]>() {
				});

				String[] keys = null; // 表头
				if (datas_node.isArray()) {
					// keys
					final ArrayNode keys_arraynode = (ArrayNode) keys_node;
					keys = objM.convertValue(keys_arraynode, new TypeReference<String[]>() {
					});
				} // if

				return this.create(keys, datas);
			} else { //
				System.err.println("非数组类型");
				return null;
			} // if
		}

		/**
		 * 由数组对象创建T
		 *
		 * @param keys  健名列表
		 * @param datas 数据二维数组
		 * @return T类型结果
		 */
		public abstract T create(final String[] keys, final Object[][] datas);
	}

	/**
	 * objM
	 */
	public static ObjectMapper objM = new ObjectMapper();

	/**
	 * DataMatrixModule 构造函数
	 */
	public DataMatrixModule() {

		super("DataMatrixModule", new Version(0, 0, 1, "0.0.1-SNAPSHOT", "gbench.tartarus", "moxi"));

		/**
		 * 数据矩阵 的 Deserializer
		 */
		class DataMatrixDeser extends MyDeserializer<DataMatrix<Object>> {
			/**
			 * 
			 */
			private static final long serialVersionUID = -1999053909599618642L;

			/**
			 * DataMatrixDeser
			 */
			protected DataMatrixDeser() {
				super(DataMatrix.class);
			}

			@Override
			public DataMatrix<Object> create(String[] keys, Object[][] datas) {
				return DataMatrix.of(keys, datas);
			}
		}

		this.addDeserializer(DataMatrix.class, new DataMatrixDeser());
		this.addSerializer(new MySerializer<>(DataMatrix.class));
	}
}