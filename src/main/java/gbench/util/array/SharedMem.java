package gbench.util.array;

import gbench.util.jdbc.kvp.DFrame;
import gbench.util.jdbc.kvp.IRecord;
import gbench.util.jdbc.kvp.Tuple2;

import static gbench.util.array.Partitioner.P2;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 共享内存分区器 - 将Partitioner数据结构映射到共享内存 实现Java与R的零拷贝数据共享
 */
public class SharedMem {

	/**
	 * 数据类型枚举
	 */
	public enum DataType {
		INT8(1), INT16(2), INT32(4), INT64(8), FLOAT32(4), FLOAT64(8), STRING16(2), // UTF-16
		STRING32(32 * 2), STRING64(64 * 2), STRING128(128 * 2), STRING256(256 * 2), STRING512(512 * 2),
		STRING1024(1024 * 2), STRING2048(2048 * 2), BYTE(1), NULL(0);

		public final int elementSize;

		DataType(final int size) {
			this.elementSize = size;
		}
	}

	@SuppressWarnings("unchecked")
	public static <T, X extends List<?>> Tuple2<String, DataType> parse(final Tuple2<String, X> p) {
		String key = p._1();
		String type = p._2().stream().filter(Objects::nonNull).findFirst().map(Object::getClass).map(e -> (Class<T>) e)
				.orElse((Class<T>) Object.class).getSimpleName();
		DataType size = switch (type) {
		case "Byte", "byte" -> DataType.BYTE;
		case "Boolean", "boolean" -> DataType.BYTE;
		case "Short", "short" -> DataType.INT16;
		case "Integer", "int" -> DataType.INT32;
		case "Long", "long" -> DataType.INT64;
		case "Float", "float" -> DataType.FLOAT32;
		case "Double", "double" -> DataType.FLOAT64;
		case "String" -> {
			final var max = p._2().stream().filter(Objects::nonNull).map(s -> ((String) s).length())
					.max(Integer::compare).orElse(32);
			if (max < 16)
				yield DataType.STRING16;
			else if (max < 32)
				yield DataType.STRING32;
			else if (max < 64)
				yield DataType.STRING64;
			else if (max < 128)
				yield DataType.STRING128;
			else if (max < 256)
				yield DataType.STRING256;
			else if (max < 512)
				yield DataType.STRING512;
			else if (max < 1024)
				yield DataType.STRING1024;
			else if (max < 2048)
				yield DataType.STRING2048;
			else
				yield DataType.NULL;
		}
		default -> DataType.NULL;
		};
		return Tuple2.of(key, size);
	}

	/**
	 * 
	 * @param <T>
	 * @param dfm
	 * @return
	 */
	public static <T> Partitioner schemaOf(final DFrame dfm) {
		final var n = dfm.shape()._1();
		@SuppressWarnings("unused")
		var schema = dfm.colS((k, v) -> Tuple2.of(k, v)).map(SharedMem::parse).reduce(P2(), (acc, a) -> {
			final var key = a._1();
			final var type = a._2();
			final var size = type.elementSize;
			((Partitioner) acc.computeIfAbsent(type.name(), k -> P2())).put(key, size * n);
			return acc;
		}, (a, b) -> {
			a.putAll(b);
			return a;
		});
		return P2("root", schema);
	}

	public static Stream<IRecord> slotS(final DFrame dfm) {
		final var rb = IRecord.rb("path,name,type,start,end,length,count,value");
		return SharedMem.schemaOf(dfm).leafS().map(slot -> {
			final var paths = slot.paths(); // 路径路劲
			final var path = paths.stream().collect(Collectors.joining(".")); // 路径拼接
			final var name = paths.get(2); // 字段名称
			final var type = DataType.valueOf(paths.get(1)); // 字段类型
			final var start = slot.start(); // 起始偏移（包含）
			final var end = slot.end(); // 位置结尾索引（不报考）
			final var length = slot.length(); // (end-start) 长度
			final var value = dfm.col(name); // 数据值
			final var count = value.size(); // 数据数量
			return rb.get(path, name, type, start, end, length, count, value);
		});
	}

}