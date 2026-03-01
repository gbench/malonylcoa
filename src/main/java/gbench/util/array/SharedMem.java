package gbench.util.array;

import gbench.util.jdbc.kvp.DFrame;
import gbench.util.jdbc.kvp.IRecord;
import gbench.util.jdbc.kvp.Tuple2;
import gbench.util.jdbc.kvp.Json;

import static gbench.util.array.Partitioner.P2;

import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
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
		INT8(1), INT16(2), INT32(4), INT64(8), FLOAT32(4), FLOAT64(8), STRING16(16 * 2), // 修复：32字节，可存储16个UTF-16字符
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
		if (dfm.shape()._1() < 1)
			return Stream.empty();
		final var rb = IRecord.rb("path,name,type,start,end,length,count,value");
		final var rec = dfm.head(); // 样板行
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
		}).sorted((a, b) -> rec.indexOfKey(a.str("name")) - rec.indexOfKey(b.str("name"))); // 保持dfm的列顺序
	}

	public static void write(MappedByteBuffer buffer, DFrame dfm) {
		var slots = slotS(dfm).collect(Collectors.toList());

		var meta = Json.obj2json(Map.of("s", slots.stream()
				.map(s -> Map.of("n", s.str("name"), "t", s.str("type"), "c", s.num("count"), "start", s.num("start")))
				.toList()));

		var metaBytes = meta.getBytes(StandardCharsets.UTF_8);
		var metaSize = 4 + metaBytes.length;

		buffer.position(0);
		buffer.putInt(metaBytes.length);
		buffer.put(metaBytes);

		slots.forEach(s -> {
			var type = DataType.valueOf(s.str("type"));
			var vals = s.lla("value", e -> e);
			var pos = metaSize + s.num("start").intValue();

			buffer.position(pos);

			switch (type) {
			case INT32 -> {
				// 修复：使用循环写入，确保buffer position正确推进
				vals.forEach(v -> buffer.putInt(((Number) v).intValue()));
			}
			case FLOAT64 -> {
				// 修复：使用循环写入，确保buffer position正确推进
				vals.forEach(v -> buffer.putDouble(((Number) v).doubleValue()));
			}
			case STRING16, STRING32, STRING64, STRING128, STRING256, STRING512, STRING1024, STRING2048 -> {
				vals.forEach(v -> {
					var str = v.toString();
					var bytes = str.getBytes(StandardCharsets.UTF_16LE);
					// 修复：使用Arrays.copyOf自动截断或填充到固定长度
					var fixedBytes = Arrays.copyOf(bytes, type.elementSize);
					buffer.put(fixedBytes);
				});
			}
			case INT8, BYTE -> {
				vals.forEach(v -> buffer.put(((Number) v).byteValue()));
			}
			case INT16 -> {
				vals.forEach(v -> buffer.putShort(((Number) v).shortValue()));
			}
			case INT64 -> {
				vals.forEach(v -> buffer.putLong(((Number) v).longValue()));
			}
			case FLOAT32 -> {
				vals.forEach(v -> buffer.putFloat(((Number) v).floatValue()));
			}
			default -> {
			}
			}
		});
		buffer.force();
	}

	public static MappedByteBuffer bufferOf(String filePath, int size) throws Exception {
		try (RandomAccessFile file = new RandomAccessFile(filePath, "rw")) {
			file.setLength(size);
			FileChannel channel = file.getChannel();
			return channel.map(FileChannel.MapMode.READ_WRITE, 0, size);
		}
	}

	public static MappedByteBuffer buffer2(String name, int size) throws Exception {
		String path = System.getProperty("java.io.tmpdir") + "/shm_" + name;
		return bufferOf(path, size);
	}

	@SuppressWarnings("unchecked")
	public static DFrame read(MappedByteBuffer buf) {
		buf.position(0);

		int metaLen = buf.getInt();
		byte[] metaBytes = new byte[metaLen];
		buf.get(metaBytes);

		var meta = Json.json2obj(new String(metaBytes, StandardCharsets.UTF_8), Map.class);
		var fields = (List<Map<String, Object>>) meta.get("s");
		if (fields == null || fields.isEmpty())
			return DFrame.dfm();

		int dataStart = 4 + metaLen;
		var args = new ArrayList<Object>();
		var colNames = new ArrayList<String>();
		var colData = new ArrayList<List<?>>();

		for (var f : fields) {
			var name = (String) f.get("n");
			var type = DataType.valueOf((String) f.get("t"));
			var cnt = ((Number) f.get("c")).intValue();
			var start = ((Number) f.get("start")).intValue();

			buf.position(dataStart + start);
			colNames.add(name);

			switch (type) {
			case INT32 -> {
				var list = new ArrayList<Integer>();
				for (int i = 0; i < cnt; i++)
					list.add(buf.getInt());
				colData.add(list);
			}
			case FLOAT64 -> {
				var list = new ArrayList<Double>();
				for (int i = 0; i < cnt; i++)
					list.add(buf.getDouble());
				colData.add(list);
			}
			case STRING16, STRING32, STRING64, STRING128, STRING256, STRING512, STRING1024, STRING2048 -> {
				var list = new ArrayList<String>();
				int strBytesLen = type.elementSize;
				for (int i = 0; i < cnt; i++) {
					byte[] bytes = new byte[strBytesLen];
					buf.get(bytes);

					// 找到字符串结束位置（双字节\0）
					int len = 0;
					while (len < bytes.length - 1) {
						if (bytes[len] == 0 && bytes[len + 1] == 0)
							break;
						len += 2;
					}

					list.add(new String(bytes, 0, len, StandardCharsets.UTF_16LE));
				}
				colData.add(list);
			}
			case INT8, BYTE -> {
				var list = new ArrayList<Byte>();
				for (int i = 0; i < cnt; i++)
					list.add(buf.get());
				colData.add(list);
			}
			case INT16 -> {
				var list = new ArrayList<Short>();
				for (int i = 0; i < cnt; i++)
					list.add(buf.getShort());
				colData.add(list);
			}
			case INT64 -> {
				var list = new ArrayList<Long>();
				for (int i = 0; i < cnt; i++)
					list.add(buf.getLong());
				colData.add(list);
			}
			case FLOAT32 -> {
				var list = new ArrayList<Float>();
				for (int i = 0; i < cnt; i++)
					list.add(buf.getFloat());
				colData.add(list);
			}
			default -> colData.add(new ArrayList<>());
			}
		}

		// 构建DFrame参数
		for (int i = 0; i < colNames.size(); i++) {
			args.add(colNames.get(i));
			args.add(colData.get(i));
		}

		return DFrame.dfm(args.toArray());
	}
}