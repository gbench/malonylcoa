package gbench.util.json.jackson;

import java.util.Map;

import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.module.SimpleModule;
import gbench.util.lisp.*;

/**
 * IRecord 序列号与反序列模块
 * 
 * @author xuqinghua
 *
 */
public class IRecordModule extends SimpleModule {

	/**
	 * 
	 */
	private static final long serialVersionUID = -1149634327472603779L;

	/**
	 * IRecord 反序列化 <br>
	 * 
	 * @author gbench
	 *
	 */
	public static class IRecordDeserializer extends AbstractDeserializer<IRecord> {

		private static final long serialVersionUID = 637227298143614828L;

		/**
		 * 反序列化 <br>
		 * 构造函数
		 */
		public IRecordDeserializer() {
			super(IRecord.class);
		}

		@Override
		public IRecord create(final Map<String, Object> tuples) {
			return IRecord.REC(tuples);
		}

	}

	/**
	 * IRecord 序列化
	 * 
	 * @author gbench
	 *
	 */
	public static class IRecordSerializer extends AbstractSerializer<IRecord> {

		private static final long serialVersionUID = -6713069486531158400L;

		/**
		 * 序列化 <br>
		 * 构造函数
		 */
		public IRecordSerializer() {
			super(IRecord.class);
		}

		@Override
		public Map<String, Object> dataOf(final IRecord t) {
			return t.toMap();
		}

	}

	/**
	 * 构造函数
	 */
	public IRecordModule() {
		super("IRecordModule", new Version(0, 0, 1, "0.0.1-SNAPSHOT", "gbench.pubchem", "malonylcoa"));
		this.addDeserializer(IRecord.class, new IRecordDeserializer());
		this.addSerializer(new IRecordSerializer());
	}

}
