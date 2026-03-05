package gbench.util.array;

import gbench.util.array.SharedMem.Schema.ChanBuff;
import gbench.util.jdbc.function.ExceptionalFunction;
import gbench.util.jdbc.kvp.DFrame;
import gbench.util.jdbc.kvp.IRecord;
import gbench.util.jdbc.kvp.Tuple2;
import gbench.util.jdbc.kvp.Json;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 共享内存分区器 - 将Partitioner数据结构映射到共享内存 实现Java与R的零拷贝数据共享
 */
public class SharedMem {

	public static class Schema extends Partitioner {

		private static final long serialVersionUID = -4217509027616888918L;

		/**
		 * 
		 */
		public static class ChanBuff implements AutoCloseable {

			public ChanBuff(FileChannel chan, MappedByteBuffer buf) {
				this.chan = chan;
				this.buff = buf;
			}

			@Override
			public void close() throws Exception {
				buff.force();
				chan.close();
				Optional.ofNullable(this.rafile()).ifPresent(rafile -> {
					try {
						rafile.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				});
			}

			/**
			 * 
			 * @param key
			 * @param value
			 * @return
			 */
			public ChanBuff put(final String key, final Object value) {
				cachedata.put(key, value);
				return this;
			}

			/**
			 * 
			 * @param file
			 * @return
			 */
			public ChanBuff rafile(final RandomAccessFile rafile) {
				return this.put(Constant.RAND_FILE.name(), rafile);
			}

			/**
			 * 
			 * @param file
			 * @return
			 */
			public RandomAccessFile rafile() {
				return (RandomAccessFile) this.cachedata.get(Constant.RAND_FILE.name());
			}

			/**
			 * 
			 * @param path
			 * @return
			 */
			public ChanBuff pathname(final File file) {
				return this.put(Constant.PATH_NAME.name(), file);
			}

			/**
			 * 
			 * @return
			 */
			public String pathname() {
				return Optional.ofNullable(cachedata.get(Constant.PATH_NAME.name())) //
						.map(e -> switch (e) {
						case File file -> file.getAbsolutePath();
						default -> String.valueOf(e);
						}).orElse(null);
			}

			/**
			 * 
			 * @return
			 */
			public String getName() {
				return this.pathname();
			}

			/**
			 * 
			 * @param channel
			 * @param size
			 * @return
			 * @throws IOException
			 */
			public static ChanBuff of(final FileChannel channel, final int size) throws IOException {
				return new ChanBuff(channel, channel.map(FileChannel.MapMode.READ_WRITE, 0, size));
			}

			enum Constant {
				RAND_FILE, PATH_NAME
			}

			@Override
			public String toString() {
				return "%s".formatted(this.pathname());
			}

			public Map<String, Object> cachedata = new ConcurrentHashMap<>();
			public final FileChannel chan;
			public final MappedByteBuffer buff;
		}

		@SuppressWarnings("unchecked")
		public static <T, X extends List<?>> Tuple2<String, DataType> typeof(final Tuple2<String, X> p) {
			String key = p._1();
			String type = p._2().stream().filter(Objects::nonNull).findFirst().map(Object::getClass)
					.map(e -> (Class<T>) e).orElse((Class<T>) Object.class).getSimpleName();
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
			// 添加日期时间类型的处理
			case "LocalDateTime" -> DataType.DATETIME;
			case "LocalDate" -> DataType.DATE;
			case "Date" -> DataType.DATETIME; // java.util.Date 也存储为 DATETIME 类型
			case "Timestamp" -> DataType.DATETIME; // java.sql.Timestamp
			default -> DataType.NULL;
			};
			return Tuple2.of(key, size);
		}

		/**
		 * 
		 * @param slots
		 * @return
		 */
		public static int sizeof(final List<IRecord> slots) {
			final int dataSize = slots.stream().mapToInt(slot -> slot.i4("end")).max().orElse(0);
			final var meta = Json.obj2json(Map.of("s", slots.stream().map(
					s -> Map.of("n", s.str("name"), "t", s.str("type"), "c", s.num("count"), "start", s.num("start")))
					.toList()));
			final int metaSize = 4 + meta.getBytes(StandardCharsets.UTF_8).length;

			return metaSize + dataSize;
		}

		/**
		 * 
		 * @param dfm
		 * @return
		 */
		public static int sizeof(final DFrame dfm) {
			final var slots = slotS(dfm).collect(Collectors.toList());
			return sizeof(slots);
		}

		/**
		 * 
		 * @param dfm
		 * @return
		 */
		public static List<IRecord> slots(final DFrame dfm) {
			return slotS(dfm).toList();
		}

		/**
		 * 
		 * @param dfm
		 * @return
		 */
		public static Stream<IRecord> slotS(final DFrame dfm) {
			if (dfm.shape()._1() < 1)
				return Stream.empty();
			final var rb = IRecord.rb("path,name,type,start,end,length,count,value");
			final var rec = dfm.head(); // 样板行
			return Schema.of(dfm).leafS().map(slot -> { // 遍历叶子节点slot
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

		/**
		 * 
		 * @param filePath
		 * @param slots
		 * @return
		 * @throws Exception
		 */
		public static ChanBuff slotsbuf(final String filePath, List<IRecord> slots) throws Exception {
			@SuppressWarnings("resource")
			final var file = new RandomAccessFile(filePath, "rw");
			final var size = sizeof(slots);
			file.setLength(size);
			return ChanBuff.of(file.getChannel(), size).rafile(file);
		}

		/**
		 * 
		 * @param filePath
		 * @param dfm
		 * @return
		 * @throws Exception
		 */
		public static ChanBuff dfmbuf(final String filePath, DFrame dfm) throws Exception {
			return slotsbuf(filePath, slots(dfm));
		}

		/**
		 * 
		 * @param pathname
		 * @param size
		 * @return
		 * @throws Exception
		 */
		public static ChanBuff rafbuf(final String pathname, final int size) throws Exception {
			if (pathname == null) {
				return tempbuf(null, size);
			} else {
				final var file = new File(pathname);
				final var pfile = file.getParentFile();
				if (!pfile.exists()) { // 创建目录
					pfile.mkdirs();
				}
				final var rafile = new RandomAccessFile(file.getAbsolutePath(), "rw");
				rafile.setLength(size);
				return ChanBuff.of(rafile.getChannel(), size).rafile(rafile).pathname(file);
			}
		}

		/**
		 * 
		 * @param name
		 * @param dfm
		 * @return
		 * @throws Exception
		 */
		public static ChanBuff tempbuf(final String name, final DFrame dfm) throws Exception {
			return tempbuf(name, sizeof(dfm));
		}

		/**
		 * 
		 * @param name
		 * @param size
		 * @return
		 * @throws Exception
		 */
		public static ChanBuff tempbuf(final String name, final int size) throws Exception {
			final var filename = Optional.ofNullable(name).orElse(UUID.randomUUID().toString().replace("-", ""));
			final var pathname = "%s%s%s".formatted(System.getProperty("java.io.tmpdir"), "shm_", filename);
			return rafbuf(pathname, size);
		}

		/**
		 * 构造器
		 * 
		 * @param kvps
		 * @return
		 */
		public static Schema of(final Object... kvps) {
			final var schema = new Schema();
			final var n = kvps.length;
			for (int i = 0; i < n; i += 2) {
				schema.put(String.valueOf(kvps[i]), kvps[(i + 1) % n]);
			}
			return schema;
		}

		/**
		 * schema 3 级别结构：[type:[(key,size)]]
		 * 
		 * @param <T>
		 * @param dfm
		 * @return
		 */
		public static <T> Schema of(final DFrame dfm) {
			final var n = dfm.shape()._1();
			@SuppressWarnings("unused")
			var schema = dfm.colS((k, v) -> Tuple2.of(k, v)).map(Schema::typeof).reduce(Schema.of(), (acc, a) -> {
				final var key = a._1();
				final var type = a._2();
				final var size = type.elementSize;
				((Schema) acc.computeIfAbsent(type.name(), k -> Schema.of())).put(key, size * n);
				return acc;
			}, (a, b) -> {
				a.putAll(b);
				return a;
			});
			return Schema.of("root", schema);
		}
	}

	/**
	 * 数据类型枚举 - 添加日期时间类型
	 */
	public enum DataType {
		INT8(1), INT16(2), INT32(4), INT64(8), FLOAT32(4), FLOAT64(8), STRING16(16 * 2), // 修复：32字节，可存储16个UTF-16字符
		STRING32(32 * 2), STRING64(64 * 2), STRING128(128 * 2), STRING256(256 * 2), STRING512(512 * 2),
		STRING1024(1024 * 2), STRING2048(2048 * 2), BYTE(1),
		// 添加日期时间类型
		DATE(8), // 存储为自1970-01-01以来的天数 (int64)
		DATETIME(16), // 存储为自1970-01-01T00:00:00Z以来的秒数(long) + 纳秒数(int)
		NULL(0);

		public final int elementSize;

		DataType(final int size) {
			this.elementSize = size;
		}
	}

	/**
	 * 
	 * @param <T>
	 * @param n
	 * @param reader
	 * @return
	 */
	public static <T> ArrayList<T> fetch(final int n, final Supplier<T> reader) {
		final var list = new ArrayList<T>();
		for (int i = 0; i < n; i++) {
			list.add(reader.get());
		}
		return list;
	}

	/**
	 * 写入器生成器
	 */
	public static ExceptionalFunction<String, ExceptionalFunction<DFrame, ChanBuff>> writerGen = path -> dfm -> {
		final var slots = Schema.slots(dfm);
		final var chanbuff = Schema.slotsbuf(path, slots);
		SharedMem.write(chanbuff, dfm);
		return chanbuff;
	};

	/**
	 * 
	 * @param chanbuff
	 * @param dfm
	 */
	public static void write(final ChanBuff chanbuff, final DFrame dfm) {
		write(chanbuff, Schema.slots(dfm), false);
	}

	/**
	 * 
	 * @param chanbuff
	 * @param dfm
	 */
	public static void write(final ChanBuff chanbuff, final DFrame dfm, boolean flag) {
		write(chanbuff, Schema.slots(dfm), flag);
	}

	/**
	 * 
	 * @param chanBuff
	 * @param slots
	 */
	public static void write(final ChanBuff chanBuff, final List<IRecord> slots, boolean flag) {
		final var metaJson = Json.obj2json(Map.of("slots", slots.stream().map(slot -> //
		Map.of("x", slot.str("name"), "t", slot.str("type"), "n", slot.num("count"), "s", slot.num("start")))
				.toList()));

		final var jsonBytes = metaJson.getBytes(StandardCharsets.UTF_8);
		final var metaSize = 4 + jsonBytes.length;
		final var buff = chanBuff.buff;

		buff.position(0);
		buff.putInt(jsonBytes.length);
		buff.put(jsonBytes);

		slots.forEach(slot -> {
			final var type = DataType.valueOf(slot.str("type"));
			final var value = slot.lla("value", e -> e);
			final var pos = metaSize + slot.num("start").intValue();

			buff.position(pos);

			switch (type) {
			case INT8, BYTE -> value.forEach(v -> buff.put(((Number) v).byteValue()));
			case INT32 -> value.forEach(v -> buff.putInt(((Number) v).intValue()));
			case INT16 -> value.forEach(v -> buff.putShort(((Number) v).shortValue()));
			case INT64 -> value.forEach(v -> buff.putLong(((Number) v).longValue()));
			case FLOAT32 -> value.forEach(v -> buff.putFloat(((Number) v).floatValue()));
			case FLOAT64 -> value.forEach(v -> buff.putDouble(((Number) v).doubleValue()));
			case STRING16, STRING32, STRING64, STRING128, STRING256, STRING512, STRING1024, STRING2048 -> {
				value.forEach(v -> {
					final var str = v.toString();
					final var bytes = str.getBytes(StandardCharsets.UTF_16LE);
					final var fixedBytes = Arrays.copyOf(bytes, type.elementSize);
					buff.put(fixedBytes);
				});
			}
			// 添加日期类型的写入逻辑
			case DATE -> {
				value.forEach(v -> {
					if (v == null) {
						buff.putLong(0L); // 默认值
					} else {
						long epochDay = 0L;
						if (v instanceof LocalDate) {
							epochDay = ((LocalDate) v).toEpochDay();
						} else if (v instanceof LocalDateTime) {
							epochDay = ((LocalDateTime) v).toLocalDate().toEpochDay();
						} else if (v instanceof Date) {
							epochDay = ((Date) v).toInstant().atZone(ZoneId.systemDefault()).toLocalDate().toEpochDay();
						}
						buff.putLong(epochDay);
					}
				});
			}
			// 添加日期时间类型的写入逻辑
			case DATETIME -> {
				value.forEach(v -> {
					if (v == null) {
						buff.putLong(0L); // 秒数
						buff.putInt(0); // 纳秒数
					} else {
						long epochSecond = 0L;
						int nano = 0;

						if (v instanceof LocalDateTime) {
							epochSecond = ((LocalDateTime) v).toEpochSecond(ZoneOffset.UTC);
							nano = ((LocalDateTime) v).getNano();
						} else if (v instanceof LocalDate) {
							epochSecond = ((LocalDate) v).atStartOfDay().toEpochSecond(ZoneOffset.UTC);
						} else if (v instanceof Date) {
							Instant instant = ((Date) v).toInstant();
							epochSecond = instant.getEpochSecond();
							nano = instant.getNano();
						}

						buff.putLong(epochSecond);
						buff.putInt(nano);
					}
				});
			}
			default -> {
			}
			}
		});

		if (flag) {
			buff.force();
		}
	}

	/**
	 * 
	 * @param chanBuff
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public static DFrame read(final ChanBuff chanBuff) {
		final var buf = chanBuff.buff;
		buf.position(0);

		final int metaLen = buf.getInt();
		final byte[] jsonBytes = new byte[metaLen];
		buf.get(jsonBytes);

		final var meta = Json.json2obj(new String(jsonBytes, StandardCharsets.UTF_8), Map.class);
		final var slots = (List<Map<String, Object>>) meta.get("slots");
		if (slots == null || slots.isEmpty())
			return DFrame.dfm();

		final int offset = 4 + metaLen; // 数据偏移
		final var kvps = new ArrayList<Object>();
		final var keys = new ArrayList<String>();
		final var values = new ArrayList<List<?>>();

		for (final var slot : slots) {
			final var name = (String) slot.get("x");
			final var type = DataType.valueOf((String) slot.get("t"));
			final var cnt = ((Number) slot.get("n")).intValue();
			final var start = ((Number) slot.get("s")).intValue();

			buf.position(offset + start);
			keys.add(name);

			switch (type) {
			case INT32 -> values.add(fetch(cnt, buf::getInt));
			case FLOAT64 -> values.add(fetch(cnt, buf::getDouble));
			case INT8, BYTE -> values.add(fetch(cnt, buf::get));
			case INT16 -> values.add(fetch(cnt, buf::getShort));
			case INT64 -> values.add(fetch(cnt, buf::getLong));
			case FLOAT32 -> values.add(fetch(cnt, buf::getFloat));
			case STRING16, STRING32, STRING64, STRING128, STRING256, STRING512, STRING1024, STRING2048 -> {
				final var list = new ArrayList<String>();
				final var elmsize = type.elementSize; // 类型长度
				for (int i = 0; i < cnt; i++) {
					final byte[] bytes = new byte[elmsize];
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
				values.add(list);
			}
			// 添加日期类型的读取逻辑
			case DATE -> {
				final var list = new ArrayList<LocalDate>();
				for (int i = 0; i < cnt; i++) {
					long epochDay = buf.getLong();
					if (epochDay == 0) {
						list.add(null);
					} else {
						list.add(LocalDate.ofEpochDay(epochDay));
					}
				}
				values.add(list);
			}
			// 添加日期时间类型的读取逻辑
			case DATETIME -> {
				final var list = new ArrayList<LocalDateTime>();
				for (int i = 0; i < cnt; i++) {
					long epochSecond = buf.getLong();
					int nano = buf.getInt();
					if (epochSecond == 0 && nano == 0) {
						list.add(null);
					} else {
						list.add(LocalDateTime.ofEpochSecond(epochSecond, nano, ZoneOffset.UTC));
					}
				}
				values.add(list);
			}
			default -> values.add(new ArrayList<>());
			}
		}

		// 构建DFrame参数
		for (int i = 0; i < keys.size(); i++) {
			kvps.add(keys.get(i));
			kvps.add(values.get(i));
		}

		return DFrame.dfm(kvps.toArray());
	}

}