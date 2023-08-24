package gbench.util.json.jackson;

import java.io.IOException;
import java.util.Map;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

/**
 * 抽象序列化器
 * 
 * @author xuqinghua
 *
 * @param <T> 数据对象类型
 */
public abstract class AbstractSerializer<T> extends StdSerializer<T> {
	/**
	 * 序列号
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * 构造函数
	 * 
	 * @param tclass 目标类
	 */
	protected AbstractSerializer(final Class<T> tclass) {
		super(tclass);
	}

	@Override
	public void serializeWithType(final T value, final JsonGenerator generator, final SerializerProvider provider,
			final TypeSerializer typeSer) throws IOException {
		this.serialize(value, generator, provider);
	}

	@Override
	public void serialize(final T value, final JsonGenerator generator, final SerializerProvider provider)
			throws IOException {
		generator.writeStartObject();

		final Map<String, Object> data = this.dataOf(value); // 提取数据
		if (data != null) {
			for (final Map.Entry<String, Object> e : data.entrySet()) {
				generator.writeObjectField(e.getKey(), e.getValue());
			} // for
		} // if

		generator.writeEndObject();
	}

	/**
	 * 提取数据元结构
	 * 
	 * @param t 值对象
	 * @return 数据元结构
	 */
	public abstract Map<String, Object> dataOf(final T t);

}
