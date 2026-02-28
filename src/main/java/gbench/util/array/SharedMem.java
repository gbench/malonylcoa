package gbench.util.array;

import gbench.util.jdbc.kvp.DFrame;
import gbench.util.jdbc.kvp.IRecord;
import gbench.util.jdbc.kvp.Tuple2;
import gbench.util.jdbc.kvp.Json;

import static gbench.util.array.Partitioner.P2;

import java.nio.MappedByteBuffer;
import java.nio.charset.StandardCharsets;
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
			final var paths = slot.paths();
			final var path = paths.stream().collect(Collectors.joining("."));
			final var name = paths.get(2);
			final var type = DataType.valueOf(paths.get(1));
			final var start = slot.start();
			final var end = slot.end();
			final var length = slot.length();
			final var value = dfm.col(name);
			final var count = value.size();
			return rb.get(path, name, type, start, end, length, count, value);
		});
	}

	/**
	 * 将DFrame数据写入共享内存
	 * 
	 * @param buffer MappedByteBuffer 共享内存缓冲区
	 * @param dfm    数据框
	 */
	public static void write(MappedByteBuffer buffer, DFrame dfm) {
		var slots = slotS(dfm).collect(Collectors.toList());
		var meta = Json.obj2json(Map.of("s",
				slots.stream().map(s -> Map.of("n", s.str("name"), "t", s.str("type"), "c", s.num("count"))).toList()));
		var metaBytes = meta.getBytes(StandardCharsets.UTF_8);

		buffer.position(0);
		buffer.putInt(metaBytes.length);
		buffer.put(metaBytes);

		slots.forEach(s -> {
			var pos = s.num("start").intValue() + 4 + metaBytes.length;
			buffer.position(pos);
			var vals = s.lla("value", e -> e);
			var type = DataType.valueOf(s.str("type"));

			switch (type) {
			case INT32 -> {
				var arr = vals.stream().mapToInt(v -> ((Number) v).intValue()).toArray();
				buffer.asIntBuffer().put(arr);
			}
			case FLOAT64 -> {
				var arr = vals.stream().mapToDouble(v -> ((Number) v).doubleValue()).toArray();
				buffer.asDoubleBuffer().put(arr);
			}
			case STRING16, STRING32, STRING64, STRING128, STRING256, STRING512, STRING1024, STRING2048 -> {
				vals.forEach(v -> {
					var bytes = v.toString().getBytes(StandardCharsets.UTF_16LE);
					buffer.put(bytes);
					for (int i = bytes.length; i < type.elementSize; i++)
						buffer.put((byte) 0);
				});
			}
			default -> {
			}
			}
		});
		buffer.force();
	}

	/**
	 * 从共享内存读取数据到DFrame
	 * 
	 * @param buffer MappedByteBuffer 共享内存缓冲区
	 * @return DFrame
	 */
	public static DFrame read(MappedByteBuffer buffer) {
		buffer.position(0);
		var metaLen = buffer.getInt();
		var metaBytes = new byte[metaLen];
		buffer.get(metaBytes);
		var meta = Json.json2obj(new String(metaBytes, StandardCharsets.UTF_8), Map.class);

		var records = new ArrayList<Map<String, Object>>();
		var fields = (List<Map<String, Object>>) meta.get("s");

		for (var field : fields) {
			var name = (String) field.get("n");
			var type = DataType.valueOf((String) field.get("t"));
			var count = ((Number) field.get("c")).intValue();

			// 这里需要根据实际偏移读取数据，简化实现
			// 实际应用中需要完整的偏移计算
		}

		return null; // 返回构建的DFrame
	}
}