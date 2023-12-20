package gbench.util.type;

import java.lang.reflect.Array;
import java.nio.ByteBuffer;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.stream.*;

/**
 * 类型强转
 * 
 * @author Administrator
 *
 */
public class Types {

	/**
	 * 类型强转
	 * 
	 * @param <T>         函数参数类型
	 * @param <U>         函数结果类型
	 * @param placeholder 占位符
	 * @return 类型强转函数
	 */
	@SuppressWarnings("unchecked")
	public static <T, U> Function<T, U> cast(final U placeholder) {
		return o -> (U) o;
	}

	/**
	 * 类型强转
	 * 
	 * @param <T>    函数参数类型
	 * @param <U>    函数结果类型
	 * @param tclass 占位符
	 * @return 类型强转函数
	 */
	@SuppressWarnings("unchecked")
	public static <T, U> Function<T, U> cast(final Class<U> tclass) {
		return o -> (U) o;
	}

	/**
	 * 类型转换:非强转
	 * 
	 * @param <T>   目标类型
	 * @param value 数据值
	 * @param type  目标类型
	 * @return 类型转换
	 */
	@SuppressWarnings("unchecked")
	public static <T> T corece(final String value, final Class<T> type) {
		if (value == null) {
			return (T) null;
		} else if (type == Integer.class) {
			return (T) (Object) Integer.parseInt(value);
		} else if (type == Double.class) {
			return (T) (Object) Double.parseDouble(value);
		} else if (type == Long.class) {
			return (T) (Object) Long.parseLong(value);
		} else if (type == LocalDateTime.class) {
			return (T) Times.asLocalDateTime(value);
		} else if (type == LocalDate.class) {
			return (T) Times.asLocalDate(value);
		} else if (type == LocalTime.class) {
			return (T) Optional.ofNullable(Times.asLocalDateTime(value)) //
					.map(e -> e.toLocalTime()).orElse(null);
		} else {
			return (T) value;
		} // if
	}

	/**
	 * 数组归集器
	 * 
	 * @param <T> 规约类型
	 * @param <U> 结果类型
	 * @return 归集器
	 */
	@SuppressWarnings("unchecked")
	public static <T, U> Collector<T, ?, U[]> arrayclc() {
		return arrayclc(cc -> (Class<U>) getSharedSuperClasses(cc).stream().findFirst().orElse(Object.class));
	}

	/**
	 * 数组归集器
	 * 
	 * @param <T>                规约类型
	 * @param <U>                结果类型
	 * @param shared_super_class 共享父类
	 * @return U类型的数组
	 */
	@SuppressWarnings("unchecked")
	public static <T, U> Collector<T, ?, U[]> arrayclc(final Function<Class<?>[], Class<U>> shared_super_class) {
		return Collector.of(ArrayList::new, ArrayList::add, (aa, bb) -> {
			aa.add(bb);
			return aa;
		}, aa -> {
			final Class<?>[] cc = aa.stream().map(e -> e == null ? null : e.getClass()).distinct()
					.filter(Objects::nonNull).toArray(Class[]::new);
			final Class<U> uclazz = null == shared_super_class
					? (Class<U>) Stream.of(cc).findFirst().orElse(Object.class)
					: shared_super_class.apply(cc);
			return aa.stream().toArray(aagen(uclazz));
		});
	}

	/**
	 * 获取所有父类
	 * 
	 * @param clazz JAVA类结构
	 * @return 所有父类列表
	 */
	@SuppressWarnings("unchecked")
	public static List<Class<?>> getClassHierarchy(final Class<?> clazz) {
		return (List<Class<?>>) (Object) Stream.iterate((Class<Object>) clazz, Objects::nonNull, c -> c.getSuperclass())
				.collect(Collectors.toList());
	}

	/**
	 * 获取共同父类
	 * 
	 * @param clazzS 类元素流
	 * @return 共同父类
	 */
	public static List<Class<?>> getSharedSuperClasses(final Stream<Class<?>> clazzS) {
		return clazzS.map(Types::getClassHierarchy).reduce((a, b) -> {
			return a.stream().filter(b::contains).collect(Collectors.toList());
		}).orElse(Arrays.asList(Object.class));
	}

	/**
	 * 获取共同父类
	 * 
	 * @param classes 类列表
	 * @return 共同父类
	 */
	public static List<Class<?>> getSharedSuperClasses(final Class<?>... classes) {
		return Types.getSharedSuperClasses(Arrays.stream(classes));
	}

	/**
	 * 获取共同父类
	 * 
	 * @param itr 类列表
	 * @return 共同父类
	 */
	public static List<Class<?>> getSharedSuperClasses(final Iterable<Object> itr) {
		final Class<?> classes[] = itr2stream(itr).filter(Objects::nonNull)
				.map(e -> e instanceof Class ? (Class<?>) e : e.getClass()).toArray(Class<?>[]::new);
		return Types.getSharedSuperClasses(classes);
	}

	/**
	 * iterable换为数组
	 * 
	 * @param <T> 元素类型
	 * @param itr 可遍历数据源
	 * @return T类型的数组，maxSize 最大数组容量,默认 1G
	 */
	public static <T> T[] itr2array(final Iterable<T> itr) {
		return itr2array(itr, MAX_SIZE);
	}

	/**
	 * iterable换为数组
	 * 
	 * @param <T>     元素类型
	 * @param itr     元素迭代器
	 * @param maxSize 最大数组容量
	 * @return T类型的数组
	 */
	@SuppressWarnings("unchecked")
	public static <T> T[] itr2array(final Iterable<T> itr, final int maxSize) {
		if (itr instanceof List) {
			final Object[] objs = ((List<T>) itr).toArray();
			if (objs != null) {
				final Class<?> objclazz = objs.getClass().getComponentType();
				if (!Objects.equals(objclazz, Object.class)) {
					return (T[]) objs;
				} // if
			} // if
			final HashSet<Class<?>> clazzes = new HashSet<Class<?>>();
			for (int i = 0; i < objs.length; i++) {
				if (Objects.nonNull(objs[i])) {
					clazzes.add(objs[i].getClass());
				} // if
			} // for
			final List<Class<?>> scs = Types.getSharedSuperClasses(clazzes.stream());
			if (scs.size() > 0) { // 存在共享类
				final Class<?> tclass = scs.get(0);
				final T[] ts = (T[]) Array.newInstance(tclass, objs.length);
				System.arraycopy(objs, 0, ts, 0, objs.length);
				return ts;
			} else {
				return Arrays.stream(objs).collect(Types.arrayclc());
			}
		}
		return stream2array(itr2stream(itr), maxSize);
	}

	/**
	 * iterable换为数据流
	 * 
	 * @param <T> 元素类型
	 * @param itr 元素迭代器
	 * @return T类型的数组
	 */
	public static <T> Stream<T> itr2stream(final Iterable<T> itr) {
		return StreamSupport.stream(itr.spliterator(), false);
	}

	/**
	 * 流变换为数组
	 * 
	 * @param <T>    元素类型
	 * @param stream 元素流
	 * @return T类型的数组
	 */
	public static <T> T[] stream2array(final Stream<T> stream) {
		return Types.stream2array(stream, MAX_SIZE);
	}

	/**
	 * 流变换为数组
	 * 
	 * @param <T>     元素类型
	 * @param stream  元素流
	 * @param maxSize 最大数组容量
	 * @return T类型的数组
	 */
	public static <T> T[] stream2array(final Stream<T> stream, final int maxSize) {
		return stream.limit(maxSize).collect(arrayclc()); //
	}

	/**
	 * 数组生成函数
	 * 
	 * @param <T>    元素类型
	 * @param tclazz 数组元素的类
	 * @return 数组生成函数
	 */
	@SuppressWarnings("unchecked")
	public static <T> IntFunction<T[]> aagen(final Class<T> tclazz) {
		return n -> (T[]) Array.newInstance(tclazz, n);
	}

	/**
	 * 每个类的默认值
	 * 
	 * @param <T>   类类型
	 * @param clazz 类对象
	 * @return clazz 的默认值
	 */
	@SuppressWarnings("unchecked")
	public static <T> T defaultValue(final Class<T> clazz) {
		if (clazz == Byte.class) {
			return (T) (Object) (byte) 0;
		} else if (clazz == Short.class) {
			return (T) (Object) (short) 0;
		} else if (clazz == Integer.class) {
			return (T) (Object) (int) 0;
		} else if (clazz == Long.class) {
			return (T) (Object) (long) 0;
		} else if (clazz == Float.class) {
			return (T) (Object) (float) 0;
		} else if (clazz == Double.class) {
			return (T) (Object) 0d;
		} else if (clazz == String.class) {
			return (T) (Object) "-";
		} else {
			return (T) null;
		}
	}

	/**
	 * 把一个数据对象转换为浮点数<br>
	 * 对于 非法的数字类型 返回 defaultValue
	 * <p>
	 * 默认会尝试把时间类型也解释为数字,即 '1970-01-01 08:00:01' <br>
	 * 也会被转换成一个 0时区 的 从1970年1月1 即 epoch time 以来的毫秒数<br>
	 * 对于 中国 而言 位于+8时区, '1970-01-01 08:00:01' 会被解析为1000
	 *
	 * @param <T>          函数的参数类型
	 * @param defaultValue 非法的数字类型 返回 的默认值
	 * @return t-&gt;dbl
	 */
	public static <T> Function<T, Double> obj2dbl(final Number defaultValue) {
		return (T obj) -> {
			Double dbl = defaultValue == null ? null : defaultValue.doubleValue();
			if (obj == null) {
				return dbl;
			} else if (obj instanceof Number) {
				return ((Number) obj).doubleValue();
			}

			dbl = defaultValue == null ? null : defaultValue.doubleValue();
			try {
				dbl = Double.parseDouble(obj.toString());
			} catch (Exception e) { //
				// do nothing
			} // try
			return dbl;
		}; //
	}

	public static Function<byte[], Byte[]> bytes2Bytes = bytes -> {
		final int n = bytes.length;
		final Byte[] _bytes = new Byte[n];
		for (int i = 0; i < n; i++)
			_bytes[i] = bytes[i];
		return _bytes;
	};

	public static Function<Byte[], byte[]> Bytes2bytes = _bytes -> {
		final int n = _bytes.length;
		final byte[] bytes = new byte[n];
		for (int i = 0; i < n; i++)
			bytes[i] = _bytes[i];
		return bytes;
	};

	public static Function<char[], Character[]> chars2Chars = chars -> {
		final int n = chars.length;
		final Character[] _characters = new Character[n];
		for (int i = 0; i < n; i++)
			_characters[i] = chars[i];
		return _characters;
	};

	public static Function<Character[], char[]> Chars2chars = _chars -> {
		final int n = _chars.length;
		final char[] chars = new char[n];
		for (int i = 0; i < n; i++)
			chars[i] = _chars[i];
		return chars;
	};

	public static Function<short[], Short[]> shorts2Shorts = shorts -> {
		final int n = shorts.length;
		final Short[] _shorts = new Short[n];
		for (int i = 0; i < n; i++)
			_shorts[i] = shorts[i];
		return _shorts;
	};

	public static Function<Short[], short[]> Shorts2shorts = _shorts -> {
		final int n = _shorts.length;
		final short[] shorts = new short[n];
		for (int i = 0; i < n; i++)
			shorts[i] = _shorts[i];
		return shorts;
	};

	public static Function<int[], Integer[]> ints2Ints = ints -> {
		return IntStream.of(ints).boxed().toArray(Integer[]::new);
	};

	public static Function<Integer[], int[]> Ints2ints = ints -> {
		return Stream.of(ints).mapToInt(e -> e).toArray();
	};

	public static Function<long[], Long[]> lngs2Lngs = lngs -> {
		return LongStream.of(lngs).boxed().toArray(Long[]::new);
	};

	public static Function<Long[], long[]> Lngs2lngs = lngs -> {
		return Stream.of(lngs).mapToLong(e -> e).toArray();
	};

	public static Function<float[], Float[]> floats2Floats = floats -> {
		final int n = floats.length;
		final Float[] _floats = new Float[n];
		for (int i = 0; i < n; i++)
			_floats[i] = floats[i];
		return _floats;
	};

	public static Function<Float[], float[]> Floats2floats = _floats -> {
		final int n = _floats.length;
		final float[] floats = new float[n];
		for (int i = 0; i < n; i++)
			floats[i] = _floats[i];
		return floats;
	};

	public static Function<double[], Double[]> dbls2Dbls = dbls -> {
		return DoubleStream.of(dbls).boxed().toArray(Double[]::new);
	};

	public static Function<Double[], double[]> Dbls2dbls = dbls -> {
		return Stream.of(dbls).mapToDouble(e -> e).toArray();
	};

	/**
	 * int到byte[] 由高位到低位
	 * 
	 * @param i 需要转换为byte数组的整行值。
	 * @return byte数组
	 */
	public static Byte[] int2Bytes(int i) {
		final Byte[] _bytes = Int2Bytes.apply(i);
		return _bytes;
	}

	/**
	 * int到byte[] 由高位到低位
	 * 
	 * @param i 需要转换为byte数组的整行值。
	 * @return byte数组
	 */
	public static byte[] int2bytes(final int i) {
		byte[] result = new byte[4];
		result[0] = (byte) ((i >> 24) & 0xFF);
		result[1] = (byte) ((i >> 16) & 0xFF);
		result[2] = (byte) ((i >> 8) & 0xFF);
		result[3] = (byte) (i & 0xFF);
		return result;
	}

	/**
	 * 
	 * @param line 行内容
	 * @return Byte[]
	 */
	public static Byte[] lng2Bytes(final Long line) {
		return Types.bytes2Bytes.apply(Types.lng2bytes(line));
	}

	/**
	 * 
	 * @param dbl
	 * @return
	 */
	public static byte[] dbl2bytes(final double dbl) {
		long value = Double.doubleToRawLongBits(dbl);
		byte[] byteRet = new byte[8];
		for (int i = 0; i < 8; i++) {
			byteRet[i] = (byte) ((value >> 8 * i) & 0xff);
		}
		return byteRet;
	}

	/**
	 * 布尔转字节数组
	 * 
	 * @param b 布尔标记
	 * @return 字节数组
	 */
	public static byte[] bool2bytes(final boolean b) {

		return new byte[] { (byte) (b ? 0x01 : 0x00) };
	}

	/**
	 * 布尔转字节数组
	 * 
	 * @param b 布尔转字节数组
	 * @return 字节数组
	 */
	public static Byte[] bool2Bytes(final boolean b) {

		return Bool2Bytes.apply(b);
	}

	/**
	 * bytes2bool
	 * 
	 * @param bytes bytes
	 * @return bool
	 */
	public static boolean bytes2bool(final byte[] bytes) {

		return bytes[0] == 0x00 ? false : true;
	}

	/**
	 * bytes2char
	 * 
	 * @param bytes bytes
	 * @return bytes2char
	 */
	public static char bytes2char(final byte[] bytes) {
		final int hi = (bytes[0] & 0xff) << 8;
		final int lo = bytes[1] & 0xff;
		return (char) (hi | lo);
	}

	/**
	 * char2bytes
	 * 
	 * @param c c
	 * @return char2bytes
	 */
	public static byte[] char2bytes(char c) {
		final byte[] bytes = new byte[2];
		bytes[0] = (byte) ((c & 0xff00) >> 8);
		bytes[1] = (byte) (c & 0xff);
		return bytes;
	}

	/**
	 * dbl2Bytes
	 * 
	 * @param dbl dbl
	 * @return dbl2Bytes
	 */
	public static Byte[] dbl2Bytes(final double dbl) {
		return Dbl2Bytes.apply(dbl);
	}

	/**
	 * char2Bytes
	 * 
	 * @param c c
	 * @return char2Bytes
	 */
	public static Byte[] char2Bytes(char c) {
		return Char2Bytes.apply(c);
	}

	/**
	 * byte[]转int
	 * 
	 * @param bytes 需要转换成int的数组
	 * @return int值
	 */
	public static int bytes2int(final byte[] bytes) {
		int value = 0;
		for (int i = 0; i < 4; i++) {
			int shift = (3 - i) * 8;
			value += (bytes[i] & 0xFF) << shift;
		}
		return value;
	}

	/**
	 * lng2bytes
	 * 
	 * @param lng lng
	 * @return lng2bytes
	 */
	public static byte[] lng2bytes(final long lng) {
		ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES);
		buffer.putLong(lng);
		return buffer.array();
	}

	/**
	 * bytes2lngs
	 * 
	 * @param bytes bytes
	 * @return bytes2lngs
	 */
	public static long bytes2lngs(final byte[] bytes) {
		ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES);
		buffer.put(bytes);
		buffer.flip();// need flip
		return buffer.getLong();
	}

	/**
	 * bytes2dbls
	 * 
	 * @param bytes bytes
	 * @return bytes2dbls
	 */
	public static double bytes2dbls(final byte[] bytes) {
		long value = 0;
		for (int i = 0; i < 8; i++) {
			value |= ((long) (bytes[i] & 0xff)) << (8 * i);
		}
		return Double.longBitsToDouble(value);
	}

	/**
	 * str2Bytes
	 * 
	 * @param line 行内容
	 * @return Byte[]
	 */
	public static Byte[] str2Bytes(final String line) {
		return Str2Bytes.apply(line);
	}

	/**
	 * Integer
	 * 
	 * @param s 数字字符串
	 * @return Integer
	 */
	public static Integer i4(final String s) {
		return ((Number) Double.parseDouble(s)).intValue();
	}

	/**
	 * Long
	 * 
	 * @param s 数字字符串
	 * @return Long
	 */
	public static Long lng(final String s) {
		return ((Number) Double.parseDouble(s)).longValue();
	}

	/**
	 * sdfs
	 */
	static List<SimpleDateFormat> sdfs = null;
	static {
		String ss[] = new String[] { "yyyy-MM-dd", "yyMMdd", "yyyyMMdd", "yy-MM-dd", "yyyy-MM-dd HH",
				"yyyy-MM-dd HH:mm", "yyyy-MM-dd HH:mm:ss", "yyyy/MM/dd HH", "yyyy/MM/dd HH:mm", "yyyy/MM/dd HH:mm:ss" };
		sdfs = Arrays.stream(ss).map(SimpleDateFormat::new).collect(Collectors.toList());
	}

	/**
	 * 
	 * @param str
	 * @return
	 */
	public static Date date(final String str) {
		Date date = null;
		for (SimpleDateFormat sdf : sdfs) {
			try {
				date = sdf.parse(str);
			} catch (Exception e) {
			}
			if (date != null)
				return date;
		} // for
		return null;
	}

	public static Double dbl(String s) {
		return Double.parseDouble(s);
	}

	/**
	 * 数据初始化,使用 inits 来初始化对象obj,用inits中的key的值设置obj中的对应的字段
	 * 
	 * @param obj   待初始对象
	 * @param inits 初始源数据
	 */
	public static void OBJINIT(final Object obj, final Map<String, ?> inits) {
		Arrays.stream(obj.getClass().getDeclaredFields()).filter(e -> inits.keySet().contains(e.getName()))
				.forEach(fld -> {
					try {
						fld.setAccessible(true);
						Object value = inits.get(fld.getName());
						if (value == null)
							return;
						if (fld.getType() == value.getClass()) {
							fld.set(obj, value);
						} else if (value instanceof String) {// 对字符串类型尝试做类型转换
							if (fld.getType() == Character.class || fld.getType() == char.class) {// 数字
								fld.set(obj, (value.toString().charAt(0)));
							} else if (fld.getType() == Integer.class || fld.getType() == int.class) {// 数字
								fld.set(obj, ((Double) Double.parseDouble(value.toString())).intValue());
							} else if (fld.getType() == Double.class || fld.getType() == double.class) {// 数字
								fld.set(obj, Double.parseDouble(value.toString()));
							} else if (fld.getType() == Float.class || fld.getType() == float.class) {// 数字
								fld.set(obj, Float.parseFloat(value.toString()));
							} else if (fld.getType() == Short.class || fld.getType() == short.class) {// 数字
								fld.set(obj, Short.parseShort(value.toString()));
							} else if (fld.getType() == Boolean.class || fld.getType() == boolean.class) {// 数字
								fld.set(obj, Boolean.parseBoolean(value.toString()));
							} else if (fld.getType() == Long.class || fld.getType() == long.class) {// 数字
								fld.set(obj, ((Number) Double.parseDouble(value.toString())).longValue());
								// System.out.println(obj+"===>"+value);
							} else if (fld.getType() == Date.class) {
								if (value instanceof Number) {
									long time = ((Number) value).longValue();
									Date date = new Date(time);
									fld.set(obj, date);
								} else {
									String ss[] = "yyyy-MM-dd HH:mm:ss,yyyy-MM-dd HH:mm,yyyy-MM-dd HH,yyyy-MM-dd,yyyy-MM,yyyy-MM,yyyy"
											.split("[,]+");
									Date date = null;
									for (String s : ss) {
										try {
											date = new SimpleDateFormat(s).parse(value.toString());
										} catch (Exception ex) {
										}
										if (date != null)
											break;
									} // for
									fld.set(obj, date);
								} // date
							} // if
						}
					} catch (Exception ex) {
						ex.printStackTrace();
					}
				});
	}

	/**
	 * 转换成一个md5字符串
	 * 
	 * @param source
	 * @param symbols 字符串,null 为"0123456789abcdef"
	 * @return
	 */
	public static String md5(final byte[] source, final String symbols) {
		String s = null;
		String hexs = "0123456789abcdef";
		final int size = symbols == null ? hexs.length() : symbols.length();
		// 用来将字节转换成16进制表示的字符
		Character[] hexDigits = null;
		hexDigits = symbols == null
				? Arrays.stream(hexs.split("")).map(e -> Character.valueOf(e.charAt(0))).toArray(Character[]::new)
				: Stream.iterate(0, i -> i + 1).map(e -> new Random().nextInt(size)).map(symbols::charAt).limit(16)
						.toArray(Character[]::new);

		try {
			java.security.MessageDigest md = java.security.MessageDigest.getInstance("MD5");
			md.update(source);
			byte tmp[] = md.digest();// MD5 的计算结果是一个 128 位的长整数，
			// 用字节表示就是 16 个字节
			char str[] = new char[16 * 2];// 每个字节用 16 进制表示的话，使用两个字符， 所以表示成 16
			// 进制需要 32 个字符
			int k = 0;// 表示转换结果中对应的字符位置
			for (int i = 0; i < 16; i++) {// 从第一个字节开始，对 MD5 的每一个字节// 转换成 16
				// 进制字符的转换
				byte byte0 = tmp[i];// 取第 i 个字节
				str[k++] = hexDigits[byte0 >>> 4 & 0xf];// 取字节中高 4 位的数字转换,// >>>
				// 为逻辑右移，将符号位一起右移
				str[k++] = hexDigits[byte0 & 0xf];// 取字节中低 4 位的数字转换
			}
			s = new String(str);// 换后的结果转换为字符串
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
		return s;
	}

	/**
	 * MD5加密的字符串
	 * 
	 * @param s 源字符串
	 * @return MD5加密的字符串
	 */
	public static String md5(final String s) {
		return md5(s.getBytes(), null);
	}

	public final static Function<Byte[], Integer> Bytes2Int = _bytes -> Types.Bytes2bytes.andThen(Types::bytes2int)
			.apply(_bytes);

	public final static Function<Integer, Byte[]> Int2Bytes = i4 -> Types.bytes2Bytes.apply(int2bytes(i4));

	public final static Function<String, Byte[]> Str2Bytes = line -> Types.bytes2Bytes.apply(line.getBytes());

	public final static Function<Byte[], String> Bytes2Str = Types.Bytes2bytes.andThen(String::new);

	public final static Function<Byte[], Double> Bytes2Dbl = Types.Bytes2bytes.andThen(Types::bytes2dbls);

	public final static Function<Double, Byte[]> Dbl2Bytes = i4 -> Types.bytes2Bytes.apply(dbl2bytes(i4));

	public final static Function<Boolean, Byte[]> Bool2Bytes = b -> Types.bytes2Bytes.apply(bool2bytes(b));

	public final static Function<Byte[], Boolean> Bytes2Bool = Types.Bytes2bytes.andThen(Types::bytes2bool);

	public final static Function<Byte[], Character> Bytes2Char = Types.Bytes2bytes.andThen(Types::bytes2char);

	public final static Function<Character, Byte[]> Char2Bytes = c -> Types.bytes2Bytes.apply(char2bytes(c));

	public final static Function<?, Integer> i4 = Types.cast((Integer) null);
	public final static Function<?, Double> dbl = Types.cast((Double) null);
	public final static Function<?, Long> lng = Types.cast((Long) null);
	public final static Function<?, Float> flt = Types.cast((Float) null);
	public final static Function<?, String> str = Types.cast((String) null);
	public final static Function<?, Byte> byt = Types.cast((Byte) null);
	public final static Function<?, Boolean> bool = Types.cast((Boolean) null);
	public final static Function<?, Character> chr = Types.cast((Character) null);
	public final static Function<?, Short> shrt = Types.cast((Short) null);

	public final static Function<?, List<Integer>> lint = Types.cast((List<Integer>) null);
	public final static Function<?, List<Short>> lsht = Types.cast((List<Short>) null);
	public final static Function<?, List<Long>> llng = Types.cast((List<Long>) null);
	public final static Function<?, List<Float>> lflt = Types.cast((List<Float>) null);
	public final static Function<?, List<Double>> ldbl = Types.cast((List<Double>) null);
	public final static Function<?, List<Number>> lnum = Types.cast((List<Number>) null);
	public final static Function<?, List<Character>> lchar = Types.cast((List<Character>) null);
	public final static Function<?, List<Byte>> lbyt = Types.cast((List<Byte>) null);
	public final static Function<?, List<Boolean>> lbool = Types.cast((List<Boolean>) null);
	public final static Function<?, List<String>> lstr = Types.cast((List<String>) null);

	public final static Function<?, int[]> aint = Types.cast((int[]) null);
	public final static Function<?, Integer[]> aInt = Types.cast((Integer[]) null);

	public final static Function<?, short[]> asht = Types.cast((short[]) null);
	public final static Function<?, Short[]> aShrt = Types.cast((Short[]) null);

	public final static Function<?, double[]> adbl = Types.cast((double[]) null);
	public final static Function<?, Double[]> aDbl = Types.cast((Double[]) null);

	public final static Function<?, float[]> aflt = Types.cast((float[]) null);
	public final static Function<?, Float[]> aFlt = Types.cast((Float[]) null);

	public final static Function<?, char[]> achar = Types.cast((char[]) null);
	public final static Function<?, Double[]> aChar = Types.cast((Double[]) null);

	public final static Function<?, byte[]> abyt = Types.cast((byte[]) null);
	public final static Function<?, Byte[]> aByt = Types.cast((Byte[]) null);

	public final static Function<?, boolean[]> abool = Types.cast((boolean[]) null);
	public final static Function<?, Boolean[]> aBool = Types.cast((Boolean[]) null);

	public final static Function<?, String[]> aStr = Types.cast((String[]) null);

	public final static Integer MAX_SIZE = 1024 * 1024 * 1024; // 1G

}
