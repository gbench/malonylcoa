package gbench.util.jdbc.kvp;

import java.io.IOException;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

/**
 * IRecord序列化
 * 
 * @author gbench
 *
 */
public class IRecordSerializer extends StdSerializer<IRecord> {

	/**
	 * 序列化
	 */
	protected IRecordSerializer() {
		super(IRecord.class);
	}

	@Override
	public void serializeWithType(final IRecord value, final JsonGenerator generator, final SerializerProvider provider,
			TypeSerializer typeSer) throws IOException {
		this.serialize(value, generator, provider);
	}

	@Override
	public void serialize(final IRecord value, final JsonGenerator generator, final SerializerProvider provider)
			throws IOException {
		generator.writeStartObject();
		for (KVPair<String, Object> kvp : value.kvs())
			generator.writeObjectField(kvp.key(), kvp.value());
		generator.writeEndObject();
	}

	private static final long serialVersionUID = -6713069486531158400L;

}