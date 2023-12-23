package gbench.util.jdbc.kvp;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ser.std.DateSerializer;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalTimeSerializer;

import java.util.Optional;
import java.util.Map;
import java.util.List;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.LinkedHashMap;

/**
 * Json 的帮助类
 * 
 * @author gbench
 *
 */
public class Json {

	/**
	 * 把一个对象转换成json对象
	 * 
	 * @param obj 对象
	 * @return json 结构的对象
	 */
	public static String obj2json(final Object obj) {
		if (obj == null)
			return "{}";
		String jsn = ""; // jsn 结构的对象
		try {
			Object o = obj;// 对象结构
			// if(o instanceof IRecord) o = ((IRecord)o).toEscapedMap();// 映射对象
			jsn = objM.writeValueAsString(o);
		} catch (JsonProcessingException e) {
			e.printStackTrace();
		} // try

		return jsn;
	}

	/**
	 * 把一个对象转换成json对象 当cls为Map.class 时候结果不会返回null,空值使用一个长度为0的HashMap代替
	 * 
	 * @param <T> 结果类型
	 * @param jsn 对象 json的字符串表示结构，之所以使用Object是便于从Map这样的容器中取值进行直接传送。
	 * @return Map 结构
	 */
	public static <T> Map<String, Object> json2map(final Object jsn) {
		@SuppressWarnings("unchecked")
		final Map<String, Object> map = json2obj(jsn, Map.class, true);
		return map;
	}

	/**
	 * 把一个对象转换成json对象 当cls为Map.class 时候结果不会返回null,空值使用一个长度为0的HashMap代替
	 * 
	 * @param <T> 结果类型
	 * @param jsn 对象 json的字符串表示结构，之所以使用Object是便于从Map这样的容器中取值进行直接传送。
	 * @return json 结构的对象
	 */
	public static <T> IRecord json2rec(final Object jsn) {
		@SuppressWarnings("unchecked")
		final Map<String, Object> map = json2obj(jsn, Map.class, true);
		return new LinkedRecord(map);
	}

	/**
	 * 把一个对象转换成json对象 当cls为Map.class 时候结果不会返回null,空值使用一个长度为0的HashMap代替
	 * 
	 * @param <T> 结果类型
	 * @param jsn 对象 json的字符串表示结构，之所以使用Object是便于从Map这样的容器中取值进行直接传送。
	 * @param cls 目标类型类型
	 * @return json 结构的对象
	 */
	public static <T> T json2obj(final Object jsn, final Class<T> cls) {
		return json2obj(jsn, cls, true);
	}

	/**
	 * 把一个对象转换成json对象 当cls为Map.class 时候结果不会返回null,空值使用一个长度为0的HashMap代替
	 * 
	 * @param <T> 结果类型
	 * @param jsn 对象 json的字符串表示结构，之所以使用Object是便于从Map这样的容器中取值进行直接传送。
	 * @param cls 对象类型
	 * @param b   是否打印异常信息
	 * @return json 结构的对象
	 */
	@SuppressWarnings("unchecked")
	public static <T> T json2obj(final Object jsn, final Class<T> cls, final Boolean b) {

		ObjectMapper objM = new ObjectMapper();
		T obj = null;
		try {
			obj = objM.readValue(jsn + "", cls);
		} catch (Exception e) {
			if (b)
				e.printStackTrace();
			if (Map.class.isAssignableFrom(cls)) {// 保证当cls为Map的时候不会返回空
				obj = (T) new LinkedHashMap<>();
			} // if
		} // try

		return obj;
	}

	/**
	 * 把一个对象转换成json对象 <br>
	 * 当cls为Map.class 时候结果不会返回null,空值使用一个长度为0的HashMap代替
	 * 
	 * @param jsn     对象 json的字符串表示结构，之所以使用Object是便于从Map这样的容器中取值进行直接传送。
	 * @param typeRef 类型信息
	 * @return json 结构的对象
	 */
	public static <T> T json2obj(final Object jsn, TypeReference<T> typeRef) {

		final var objM = new ObjectMapper();
		T obj = null;
		try {
			obj = objM.readValue(jsn + "", typeRef);
		} catch (Exception e) {
			e.printStackTrace();
		} // try

		return obj;
	}

	/**
	 * 生成 record 列表 <br>
	 * json2recs
	 * 
	 * @param jsn 一个jsn数组[{key:value,key2:value2,...},{key:value,key2:value2,...},...]
	 * @return IRecord 列表
	 */
	public static List<IRecord> json2recs(final Object jsn) {
		return Json.json2obj(jsn, new TypeReference<>() {
		});// 注意这里 不能用 json2list，必须指定List<IRecord>类型
	}

	/**
	 * 这个方法不怎么有用。<br>
	 * json2list：这里的转化只是一个形式转化，并没有实际调用序列化函数。<br>
	 * 
	 * @param jsn       一个jsn数组[{key:value,key2:value2,...},{key:value,key2:value2,...},...]
	 * @param itemClass 列表项的数据类型
	 * @param <T>       返回结果LIST中的元素的类型
	 * @return T 类型的List
	 */
	@SuppressWarnings("unchecked")
	public static <T> List<T> json2list(final Object jsn, final Class<T> itemClass) {
		final TypeReference<List<T>> typeReference = Optional.of(itemClass).map(e -> {
			if (e == IRecord.class) {
				return (TypeReference<List<T>>) (Object) new TypeReference<List<IRecord>>() {
				};
			} else if (e == Map.class) {
				return (TypeReference<List<T>>) (Object) new TypeReference<List<Map<?, ?>>>() {
				};
			} else {
				return (TypeReference<List<T>>) (Object) new TypeReference<>() {
				};
			}
		}).get();

		List<T> list = null;
		try {
			list = new ObjectMapper().readValue(jsn.toString(), typeReference);
		} catch (JsonMappingException e1) {
			e1.printStackTrace();
		} catch (JsonProcessingException e1) {
			e1.printStackTrace();
		}

		return list;
	}

	/**
	 * 读取json的键值属性
	 *
	 * @param jsn json 数据
	 * @param key 键名
	 * @param cls 键的值类型
	 * @param <T> 返回结果的类型
	 * @return T类型的对象
	 */
	@SuppressWarnings("unchecked")
	public static <T> T jsnget(String jsn, String key, Class<T> cls) {
		return (T) json2obj(jsn, Map.class).get(key);
	}

	/**
	 * 序列转船成一个map <br>
	 * 创造一个Record对象,参数分别是：键名1,键值1,键名2,键值2,。。。的序列。
	 * 
	 * @return SimpleRecord
	 */
	public static Map<Object, Object> seq2map(final Object... values) {
		Map<Object, Object> rec = new LinkedHashMap<>();
		for (int i = 0; i < values.length; i += 2) {
			String key = values[i] + "";// 键值名
			Object value = (i + 1) < values.length ? values[i + 1] : null;// 如果最后一位是key则它的value为null
			rec.put(key, value);
		} // for

		return rec;
	}

	/**
	 * 创造一个Record对象,参数分别是：键名1,键值1,键名2,键值2,。。。的序列。
	 * 
	 * @param values 键值序列
	 * @return SimpleRecord
	 */
	public static String build(final Object... values) {
		Map<Object, Object> rec = new LinkedHashMap<>();
		for (int i = 0; i < values.length; i += 2) {
			String key = values[i] + "";// 键值名
			Object value = (i + 1) < values.length ? values[i + 1] : null;// 如果最后一位是key则它的value为null
			rec.put(key, value);
		} // for
		return obj2json(rec);
	}

	/**
	 * 判断一个字符串是否是json,字段需要待由双引号
	 * 
	 * @param line 待检测的对象
	 */
	public static boolean isJson(final Object line) {
		return JsonMode.OTHER != getJsonMode(line);
	}

	/**
	 * 获取一个字符串line的JsonMode
	 * 
	 * @param line 待检测的对象
	 * @return JsonMode
	 */
	public static JsonMode getJsonMode(final Object line) {
		final ObjectMapper objM = new ObjectMapper();
		var type = JsonMode.OTHER;
		final var json = line + "";
		try {
			objM.readValue(json, Map.class);
			type = JsonMode.MAP;
		} catch (Exception e) {
			try {
				objM.readValue(json, List.class);
				type = JsonMode.LIST;
			} catch (Exception ee) {
				// do nothing
			} // try
		} // try

		return type;
	}

	public enum JsonMode {
		MAP // 键值对儿格式
		, LIST // 列表类型
		, OTHER // 其他类型，非json格式结构
	} // Json模式

	public static final ObjectMapper objM = new ObjectMapper();// 默认实现的ObjectMapper
	static {
		final JavaTimeModule javaTimeModule = new JavaTimeModule();
		javaTimeModule.addSerializer(Date.class,
				new DateSerializer(false, new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")));
		javaTimeModule.addSerializer(LocalDateTime.class,
				new LocalDateTimeSerializer(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
		javaTimeModule.addSerializer(LocalDate.class,
				new LocalDateSerializer(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
		javaTimeModule.addSerializer(LocalTime.class, new LocalTimeSerializer(DateTimeFormatter.ofPattern("HH:mm:ss")));
		objM.registerModule(javaTimeModule);
	}
}
