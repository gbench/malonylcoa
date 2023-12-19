package gbench.util.jdbc.json;

import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 
 * @author xuqinghua
 *
 */
public class TypeConverter {

	public static Integer i4(String s) {
		return ((Number) Double.parseDouble(s)).intValue();
	}

	public static Long lng(String s) {
		return ((Number) Double.parseDouble(s)).longValue();
	}

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
	public static Date date(String str) {
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
	public static String md5(byte[] source, final String symbols) {
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
	public static String md5(String s) {
		return md5(s.getBytes(), null);
	}

	public static void main(String args[]) {
		System.out.println(md5("123456"));
	}

}
