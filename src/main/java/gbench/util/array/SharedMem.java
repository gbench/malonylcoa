package gbench.util.array;

import gbench.util.jdbc.kvp.Json;

import java.io.File;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.ByteOrder;
import java.nio.DoubleBuffer;
import java.nio.LongBuffer;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * 共享内存分区器 - 将Partitioner数据结构映射到共享内存 实现Java与R的零拷贝数据共享
 */
public class SharedMem {

	// 共享内存文件描述符
	public final String shmPath;
	public final MappedByteBuffer buffer;
	public final Partitioner schema;
	public final int size;

	// 字段访问器缓存
	private final Map<String, Slot> slots = new LinkedHashMap<>();

	/**
	 * 字段访问器 - 封装对特定字段的读写操作
	 */
	public static class Slot {
		public final String path;
		public final int offset;
		public final int size;
		public final DataType type;
		public final int count; // 元素数量（对于数组）

		public Slot(String path, int offset, int size, DataType type, int count) {
			this.path = path;
			this.offset = offset;
			this.size = size;
			this.type = type;
			this.count = count;
		}

		// 读取长整型数组
		public long[] lng(MappedByteBuffer buffer) {
			buffer.position(offset);
			LongBuffer lb = buffer.asLongBuffer();
			lb.limit(count);
			long[] result = new long[count];
			lb.get(result);
			return result;
		}

		// 写入长整型数组
		public void lng(MappedByteBuffer buffer, long[] values) {
			if (values.length != count)
				throw new IllegalArgumentException("数组长度不匹配: 期望 " + count + ", 实际 " + values.length);
			buffer.position(offset);
			LongBuffer lb = buffer.asLongBuffer();
			lb.put(values);
		}

		// 读取双精度数组
		public double[] dbl(MappedByteBuffer buffer) {
			buffer.position(offset);
			DoubleBuffer db = buffer.asDoubleBuffer();
			db.limit(count);
			double[] result = new double[count];
			db.get(result);
			return result;
		}

		// 写入双精度数组
		public void dbl(MappedByteBuffer buffer, double[] values) {
			if (values.length != count)
				throw new IllegalArgumentException("数组长度不匹配: 期望 " + count + ", 实际 " + values.length);
			buffer.position(offset);
			DoubleBuffer db = buffer.asDoubleBuffer();
			db.put(values);
		}

		// 读取字符串数组 (UTF-16)
		public String[] str(MappedByteBuffer buffer, int strLen) {
			buffer.position(offset);
			String[] result = new String[count];
			byte[] bytes = new byte[strLen * 2]; // UTF-16每个字符2字节

			for (int i = 0; i < count; i++) {
				buffer.get(bytes);
				// 去除末尾的空字符
				int validLen = 0;
				while (validLen < bytes.length && bytes[validLen] != 0) {
					validLen++;
				}
				result[i] = new String(bytes, 0, validLen, StandardCharsets.UTF_16LE);
			}
			return result;
		}

		// 写入字符串数组
		public void str(MappedByteBuffer buffer, String[] values, int strLen) {
			if (values.length != count)
				throw new IllegalArgumentException("数组长度不匹配");

			buffer.position(offset);
			byte[] bytes = new byte[strLen * 2];

			for (int i = 0; i < count; i++) {
				Arrays.fill(bytes, (byte) 0);
				if (values[i] != null) {
					byte[] strBytes = values[i].getBytes(StandardCharsets.UTF_16LE);
					System.arraycopy(strBytes, 0, bytes, 0, Math.min(strBytes.length, bytes.length));
				}
				buffer.put(bytes);
			}
		}
	}

	/**
	 * 数据类型枚举
	 */
	public enum DataType {
		INT8(1), INT16(2), INT32(4), INT64(8), FLOAT32(4), FLOAT64(8), STRING16(2), // UTF-16
		STRING32(32 * 2), STRING64(64 * 2), STRING128(128 * 2), STRING256(256 * 2), STRING512(512 * 2),
		STRING1024(1024 * 2), STRING2048(2048 * 2), BYTES(1);

		public final int elementSize;

		DataType(final int size) {
			this.elementSize = size;
		}
	}

	/**
	 * 创建共享内存
	 * 
	 * @param schema Partitioner模式定义
	 * @param name   共享内存名称 (如 "/acct_entries")
	 */
	private SharedMem(final Partitioner schema, final String name) {
		this.schema = schema;
		this.size = schema.length();
		final var tempDirPath = System.getProperty("java.io.tmpdir");
		this.shmPath = "%s/dev/shm".formatted(tempDirPath, name); // Linux共享内存路径
		final var home = new File(this.shmPath).getParentFile();

		if (!home.exists()) {
			home.mkdirs();
		}

		try {
			// 创建共享内存文件
			Path path = Paths.get(shmPath);
			if (!Files.exists(path)) {
				Files.createFile(path);
			}

			// 映射共享内存
			try (RandomAccessFile file = new RandomAccessFile(shmPath, "rw")) {
				file.setLength(size);
				FileChannel channel = file.getChannel();
				this.buffer = channel.map(FileChannel.MapMode.READ_WRITE, 0, size);
				this.buffer.order(ByteOrder.nativeOrder());
			}

			// 初始化字段访问器
			init();

		} catch (Exception e) {
			throw new RuntimeException("创建共享内存失败: " + shmPath, e);
		}
	}

	/**
	 * 初始化字段访问器
	 */
	private void init() {
		schema.root().forEach(e -> {
			String path = String.join(".", e._2().paths());
			@SuppressWarnings("unused")
			Integer[] indices = e._2().pathS2().toArray(Integer[]::new);

			// 根据路径推断数据类型
			DataType type = inferType(path);
			int size = e._2().length();
			int start = e._2().start();

			// 计算元素数量和元素大小
			int count = 1;
			@SuppressWarnings("unused")
			int elementSize = size;

			if (path.contains("string32")) {
				// 字符串类型：每个记录32个字符，每个字符2字节
				count = 128; // 记录数
				elementSize = 64; // 32字符 * 2字节
			} else if (path.contains("int64") || path.contains("float64")) {
				count = 128; // 128条记录
				elementSize = 8; // 8字节每记录
			}

			slots.put(path, new Slot(path, start, size, type, count));
		});
	}

	/**
	 * 根据路径推断数据类型
	 */
	private DataType inferType(final String path) {
		if (path.contains("int64"))
			return DataType.INT64;
		else if (path.contains("float64"))
			return DataType.FLOAT64;
		else if (path.contains("string32"))
			return DataType.STRING16;
		else if (path.contains("string64"))
			return DataType.STRING64;
		else if (path.contains("string128"))
			return DataType.STRING128;
		else if (path.contains("string256"))
			return DataType.STRING256;
		else if (path.contains("string512"))
			return DataType.STRING512;
		else if (path.contains("string1024"))
			return DataType.STRING1024;
		else
			return DataType.BYTES;
	}

	/**
	 * 获取字段访问器
	 */
	public Slot slot(final String path) {
		return slots.get(path);
	}

	/**
	 * 获取所有字段的元数据（用于R端解析）
	 */
	public Map<String, Object> metadata() {
		Map<String, Object> meta = new LinkedHashMap<>();
		meta.put("total_size", size);
		meta.put("record_count", 128); // 记录数

		List<Map<String, Object>> fields = new ArrayList<>();
		for (Map.Entry<String, Slot> entry : slots.entrySet()) {
			Map<String, Object> field = new LinkedHashMap<>();
			field.put("name", entry.getKey().replaceAll("^root\\.", ""));
			field.put("offset", entry.getValue().offset);
			field.put("type", entry.getValue().type.name());
			field.put("count", entry.getValue().count);
			field.put("element_size", entry.getValue().type.elementSize);
			fields.add(field);
		}
		meta.put("fields", fields);

		return meta;
	}

	/**
	 * 将元数据写入共享内存头部（前1024字节预留）
	 */
	public void writeMetadata() {
		String json = Json.obj2json(metadata());
		byte[] metaBytes = json.getBytes(StandardCharsets.UTF_8);
		buffer.position(0);
		buffer.putInt(metaBytes.length); // 前4字节存储元数据长度
		buffer.put(metaBytes);
	}

	/**
	 * 写入示例数据
	 */
	public void write() {
		this.schema.buildTreeS().forEach(e -> {
			System.out.println("%s".formatted(e.paths()));
		});

		// 写入id (1..128)
		long[] ids = new long[128];
		for (int i = 0; i < 128; i++)
			ids[i] = i + 1;
		slot("root.int64.id").lng(buffer, ids);

		// 写入drcr (交替0/1)
		long[] drcr = new long[128];
		for (int i = 0; i < 128; i++)
			drcr[i] = i % 2;
		slot("root.int64.drcr").lng(buffer, drcr);

		// 写入价格
		double[] prices = new double[128];
		for (int i = 0; i < 128; i++)
			prices[i] = 100.0 + i * 10;
		slot("root.float64.price").dbl(buffer, prices);

		// 写入数量
		double[] quantities = new double[128];
		for (int i = 0; i < 128; i++)
			quantities[i] = 10 + i;
		slot("root.float64.quantity").dbl(buffer, quantities);

		// 写入名称
		String[] names = new String[128];
		for (int i = 0; i < 128; i++)
			names[i] = "Product_" + i;
		slot("root.string32.name").str(buffer, names, 32);

		// 写入描述
		String[] descs = new String[128];
		for (int i = 0; i < 128; i++)
			descs[i] = "Description for item " + i;
		slot("root.string32.description").str(buffer, descs, 32);

		// 写入元数据
		writeMetadata();
	}

	/**
	 * 关闭共享内存
	 */
	public void close() {
		// 在Linux中，共享内存文件需要手动删除
		try {
			Files.deleteIfExists(Paths.get(shmPath));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * 从共享内存读取元数据
	 */
	@SuppressWarnings("unchecked")
	public static Map<String, Object> readMetadata(MappedByteBuffer buffer) {
		buffer.position(0);
		int metaLen = buffer.getInt();
		byte[] metaBytes = new byte[metaLen];
		buffer.get(metaBytes);
		String json = new String(metaBytes, StandardCharsets.UTF_8);
		return Json.json2obj(json, Map.class);
	}

	public static SharedMem of(final Partitioner schema, final String name) {
		return new SharedMem(schema, name);
	}
}