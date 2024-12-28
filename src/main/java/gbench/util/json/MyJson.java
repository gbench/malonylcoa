package gbench.util.json;

import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.json.JsonReadFeature;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.ser.std.DateSerializer;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalTimeSerializer;

import gbench.util.data.xls.DataMatrix;
import gbench.util.json.jackson.DFrameModule;
import gbench.util.json.jackson.DataMatrixModule;
import gbench.util.lisp.DFrame;
import gbench.util.lisp.IRecord;
import gbench.util.json.jackson.IRecordModule;
import gbench.util.json.jackson.IRecordModule2;

/**
 * Json 工具类
 *
 * @author xuqinghua
 */
public class MyJson {

	/**
	 * 使用指定 注册模块 来 构造 ObjectMapper
	 *
	 * @param modules Json 模块列表
	 * @return ObjectMapper
	 */
	public static ObjectMapper of(final Module... modules) {
		final JsonMapper jm = JsonMapper.builder() //
				.configure(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true) // 允许省略字段名的引号
				.configure(JsonReadFeature.ALLOW_TRAILING_COMMA, true) // 允许末尾保留逗号
				.configure(JsonReadFeature.ALLOW_MISSING_VALUES, true) // 允许空值
				.configure(JsonReadFeature.ALLOW_YAML_COMMENTS, true) // 允许yaml 注释
				.configure(JsonReadFeature.ALLOW_JAVA_COMMENTS, true) // 允许java 注释
				.configure(JsonParser.Feature.ALLOW_SINGLE_QUOTES, true) // 允许单引号
				.build(); //

		// 模块去重并注册
		Arrays.stream(modules).distinct().forEach(jm::registerModule); // forEach

		return jm;
	}

	/**
	 * 带有 IRecord 类型解析功能的 Mapper
	 *
	 * @param key objM 的缓存键名
	 * @return 默认的 注册了 time 与 IRecord的 ObjectMapper
	 */
	public static ObjectMapper recM(final Object key) {
		return objMCache.computeIfAbsent(key, k -> {
			// 日期序列化设置
			final JavaTimeModule javaTimeModule = new JavaTimeModule();
			javaTimeModule.addSerializer(Date.class,
					new DateSerializer(false, new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")));
			javaTimeModule.addSerializer(LocalDateTime.class,
					new LocalDateTimeSerializer(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
			javaTimeModule.addSerializer(LocalDate.class,
					new LocalDateSerializer(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
			javaTimeModule.addSerializer(LocalTime.class,
					new LocalTimeSerializer(DateTimeFormatter.ofPattern("HH:mm:ss")));

			// lisp.IRecord 序列化设置
			final IRecordModule recModule = new IRecordModule();
			// DataApp.IRecord 序列化设置
			final IRecordModule2 recModule2 = new IRecordModule2();

			// DFrame 序列化设置
			final DFrameModule dfmModule = new DFrameModule();

			// DataMatrix 序列化设置
			final DataMatrixModule dmxModule = new DataMatrixModule();

			return of(recModule, recModule2, javaTimeModule, dfmModule, dmxModule);
		}); // computeIfAbsent
	}

	/**
	 * 默认的mapper
	 *
	 * @return ObjectMapper
	 */
	public static ObjectMapper recM() {
		return recM("default_mapper"); // 默认的mapper
	}

	/**
	 * 生成 json 字符串
	 *
	 * @param obj 目标对象
	 * @return 生成 json 字符串
	 */
	public static String toJson(final Object obj) {
		try {
			return toJson2(obj);
		} catch (JsonProcessingException e) {
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * 生成 json 字符串, 带有 异常抛出
	 *
	 * @param obj 目标对象
	 * @return 生成 json 字符串
	 */
	public static String toJson2(final Object obj) throws JsonProcessingException {
		return recM().writeValueAsString(obj);
	}

	/**
	 * 返回美化后的json字符串
	 *
	 * @param obj 目标对象
	 * @return 美化后的json字符串
	 */
	public static String pretty(final Object obj) {
		try {
			return MyJson.recM().writerWithDefaultPrettyPrinter().writeValueAsString(obj);
		} catch (JsonProcessingException e) {
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * 把json字符串转成IRecord 对象
	 *
	 * @param json json字符串转
	 * @return IRecord 对象
	 */
	public static IRecord fromJson(final String json) {
		IRecord rec = null;

		try {
			rec = fromJson2(json);
		} catch (JsonProcessingException e) {
			e.printStackTrace();
		}

		return rec;
	}

	/**
	 * 把json字符串转成IRecord 对象 带有 异常抛出
	 *
	 * @param json json字符串转
	 * @return IRecord 对象
	 */
	public static IRecord fromJson2(final String json) throws JsonProcessingException {
		return recM().readValue(json, IRecord.class);
	}

	/**
	 * 把json字符串转成IRecord 对象
	 *
	 * @param <T>  结果类型: Map类的父类对昂
	 * @param json json字符串转
	 * @return Map 对象: T 由T餐宿进行具体指定
	 */
	@SuppressWarnings("unchecked")
	public static <T> Optional<T> optMap(final String json) {
		return Optional.ofNullable((T) MyJson.toMap(json));
	}

	/**
	 * 把json字符串转成IRecord 对象
	 *
	 * @param json json字符串转
	 * @return IRecord 对象, 对于 非法 json 对象放回null
	 */
	public static Map<?, ?> toMap(final String json) {
		Map<?, ?> map = null;

		try {
			map = toMap2(json);
		} catch (JsonProcessingException e) {
			// e.printStackTrace();
		}

		return map;
	}

	/**
	 * 把json字符串转成Map 对象 带有 异常抛出
	 *
	 * @param json json字符串转
	 * @return IRecord 对象
	 */
	public static Map<?, ?> toMap2(final String json) throws JsonProcessingException {
		return recM().readValue(json, Map.class);
	}

	/**
	 * 判断对象是否是json
	 *
	 * @param json json字符串转
	 * @return IRecord 对象
	 */
	public static boolean isJson(final String json) {
		boolean b = true;
		try {
			MyJson.toMap2(json);
		} catch (Exception e) {
			b = false;
		}
		return b;
	}

	/**
	 * 把json字符串转成DFrame对象
	 *
	 * @param json json字符串转
	 * @return DFrame 对象
	 */
	public static DFrame dfmFromJson(final String json) {
		DFrame dfm = null;
		try {
			dfm = dfmFromJson2(json);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return dfm;
	}

	/**
	 * 把json字符串转成DataMatrix对象
	 *
	 * @param json json字符串转
	 * @return DFrame 对象
	 */
	public static DataMatrix<Object> dmxFromJson(final String json) {
		DataMatrix<Object> dmx = null;
		try {
			dmx = dmxFromJson2(json);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return dmx;
	}

	/**
	 * 把json字符串转成DFrame对象 带有 异常抛出
	 *
	 * @param json json字符串转
	 * @return DFrame 对象
	 */
	public static DFrame dfmFromJson2(final String json) throws JsonProcessingException {
		return recM().readValue(json, DFrame.class);
	}

	/**
	 * 把json字符串转成DataMatrix对象 带有 异常抛出
	 *
	 * @param json json字符串转
	 * @return DataMatrix 对象
	 */
	@SuppressWarnings("unchecked")
	public static DataMatrix<Object> dmxFromJson2(final String json) throws JsonProcessingException {
		return recM().readValue(json, DataMatrix.class);
	}

	public static Map<Object, ObjectMapper> objMCache = new ConcurrentHashMap<Object, ObjectMapper>(); // objM 对象的缓存

}
