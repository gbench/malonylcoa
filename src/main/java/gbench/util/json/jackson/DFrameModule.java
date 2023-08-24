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
import gbench.util.lisp.DFrame;
import gbench.util.lisp.IRecord;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * DFrame 数据模块
 */
public class DFrameModule extends SimpleModule {

	/**
	 * 
	 */
	private static final long serialVersionUID = -7266350024200887114L;

	/**
	 * IterableSerializer
	 * 
	 * @param <T> Iterable的类型结构
	 */
	public static class IterableSerializer<T extends Iterable<?>> extends StdSerializer<T> {

		/**
		 * 
		 */
		private static final long serialVersionUID = -8293652782470526341L;

		/**
		 * 构造函数
		 *
		 * @param tclass 类Class
		 */
		protected IterableSerializer(final Class<T> tclass) {
			super(tclass);
		}

		@Override
		public void serialize(final T t, final JsonGenerator jsonGenerator, final SerializerProvider serializerProvider)
				throws IOException {
			final Object[] oo = StreamSupport.stream(t.spliterator(), false).toArray(Object[]::new);
			jsonGenerator.writeStartArray();
			for (final Object o : oo) {
				jsonGenerator.writeObject(o);
			}
			jsonGenerator.writeEndArray();
		}
	}

	/**
	 * IterableDeserializer
	 * 
	 * @param <T> Iterable的类型结构
	 */
	public static abstract class IterableDeserializer<T extends Iterable<?>> extends StdDeserializer<T> {

		/**
		 * 
		 */
		private static final long serialVersionUID = -1501276456923937018L;

		/**
		 * 构造函数
		 *
		 * @param tclass 类型类
		 */
		protected IterableDeserializer(final Class<T> tclass) {
			super(tclass);
		}

		@Override
		public T deserialize(final JsonParser jsonParser, final DeserializationContext deserializationContext)
				throws IOException, JacksonException {
			final JsonNode node = jsonParser.getCodec().readTree(jsonParser);
			if (node.isArray()) { // 数组类型
				final ArrayNode arrayNode = (ArrayNode) node;
				final ArrayList<Map<String, Object>> entries = new ArrayList<Map<String, Object>>();
				for (final JsonNode jsnode : arrayNode) {
					final Map<String, Object> mm = objM.convertValue(jsnode, new TypeReference<Map<String, Object>>() {
					});
					entries.add(mm);
				}
				return this.create(entries);
			} else { //
				System.err.println("非数组类型");
				return null;
			} // if
		}

		/**
		 * 由可遍历对象创建T
		 *
		 * @param itr 可遍历对象
		 * @return T 类型的对象
		 */
		public T create(final Iterable<?> itr) {
			final Object[] oo = StreamSupport.stream(itr.spliterator(), false).toArray(Object[]::new);
			return this.create(oo);
		}

		/**
		 * 由数组对象创建T
		 *
		 * @param dd 数组对象
		 * @return T 类型的对象
		 */
		public abstract T create(final Object[] dd);
	}

	/**
	 * objM
	 */
	public static ObjectMapper objM = new ObjectMapper();

	/**
	 * DFrameModule 构造函数
	 */
	public DFrameModule() {
		super("DFrameModule", new Version(0, 0, 1, "0.0.1-SNAPSHOT", "gbench.tartarus", "moxi"));
		this.addDeserializer(DFrame.class, new IterableDeserializer<DFrame>(DFrame.class) {
			private static final long serialVersionUID = 9020796813986599839L;

			public DFrame create(final Object[] oo) {
				return Stream.of(oo).map(IRecord::REC).collect(DFrame.dfmclc);
			}
		});
		this.addSerializer(new IterableSerializer<>(DFrame.class));
	}
}