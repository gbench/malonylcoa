package gbench.util.type;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.Date;
import java.util.Objects;
import java.util.function.Function;
import java.util.regex.Pattern;

/**
 * 基本时间操作函数
 */
public class Times {
	/**
	 * 
	 * @param localDate 日期值
	 * @return 日期对象
	 */
	public static Date ld2dt(final LocalDate localDate) {
		if (localDate == null)
			return null;
		ZoneId zoneId = ZoneId.systemDefault();
		ZonedDateTime zdt = localDate.atStartOfDay(zoneId);
		return Date.from(zdt.toInstant());
	}

	/**
	 * localDateTime2Date 的别名
	 * 
	 * @param localDateTime,null 返回null
	 * @return 日期对象
	 */
	public static Date ldt2dt(LocalDateTime localDateTime) {
		if (localDateTime == null)
			return null;
		return localDateTime2date(localDateTime);
	}

	/**
	 * 
	 * @param localDateTime
	 * @return 日期对象
	 */
	public static Date localDateTime2date(LocalDateTime localDateTime) {
		if (localDateTime == null)
			return null;
		ZoneId zoneId = ZoneId.systemDefault();
		ZonedDateTime zdt = localDateTime.atZone(zoneId);
		return Date.from(zdt.toInstant());
	}

	/**
	 * date2LocalDateTime的别名
	 * 
	 * @param date 时间
	 * @return 本地日期时间
	 */
	public static LocalDateTime dt2ldt(Date date) {
		if (date == null)
			return null;
		return date2localDateTime(date);
	}

	/**
	 * date -> localDate
	 * 
	 * @param date
	 * @return 日期时间
	 */
	public static LocalDateTime date2localDateTime(final Date date) {
		if (date == null)
			return null;
		Instant instant = date.toInstant();
		ZoneId zoneId = ZoneId.systemDefault();
		return instant.atZone(zoneId).toLocalDateTime();
	}

	/**
	 * 日期Date 转 LocalDate
	 * 
	 * @param date
	 * @return 日期对象
	 */
	public static LocalDate dt2ld(final Date date) {
		if (date == null)
			return null;
		return date2localDate(date);
	}

	/**
	 * date -> localDate
	 * 
	 * @param date
	 * @return 日期对象
	 */
	public static LocalDate date2localDate(final Date date) {
		if (date == null)
			return null;
		Instant instant = date.toInstant();
		ZoneId zoneId = ZoneId.systemDefault();
		return instant.atZone(zoneId).toLocalDateTime().toLocalDate();
	}

	/**
	 * 默认为当日日期
	 * 
	 * @param lt 本地实间
	 * @return 日期
	 */
	public static Date ld2dt(final LocalTime lt) {
		return localTime2date(lt);
	}

	/**
	 * 默认为当日日期
	 * 
	 * @param lt 本地时间
	 * @param ld 本地日期,null 表示当日
	 * @return 日期
	 */
	public static Date ld2dt(final LocalTime lt, final LocalDate ld) {
		return localTime2date(lt, ld);
	}

	/**
	 * 默认为当日日期
	 * 
	 * @param lt 本地时间
	 * @param dt 日期,null 表示当日
	 * @return 日期
	 */
	public static Date ld2dt(final LocalTime lt, final Date dt) {
		return localTime2date(lt, dt2ld(dt));
	}

	/**
	 * 日期转LocalTime
	 * 
	 * @param lt 本地时间
	 * @return LocalTime
	 */
	public static Date lt2dt(final LocalTime lt) {
		return localTime2date(lt);
	}

	/**
	 * 默认为当日日期
	 * 
	 * @param lt 本地时间
	 * @return 日期
	 */
	public static Date localTime2date(final LocalTime lt) {
		return localTime2date(lt, null);
	}

	/**
	 * 默认为当日日期
	 * 
	 * @param localTime
	 * @param ld        补填出的日期,null 表示当日
	 * @return 日期
	 */
	public static Date localTime2date(final LocalTime localTime, final LocalDate ld) {
		LocalDate localDate = ld == null ? LocalDate.now() : ld;
		LocalDateTime localDateTime = LocalDateTime.of(localDate, localTime);
		ZoneId zone = ZoneId.systemDefault();
		Instant instant = localDateTime.atZone(zone).toInstant();
		Date date = Date.from(instant);
		return date;
	}

	/**
	 * 日期转LocalTime
	 * 
	 * @param date 日期值
	 * @return LocalTime
	 */
	public static LocalTime dt2lt(final Date date) {
		if (date == null)
			return null;
		Instant instant = date.toInstant();
		ZoneId zone = ZoneId.systemDefault();
		LocalDateTime localDateTime = LocalDateTime.ofInstant(instant, zone);
		return localDateTime.toLocalTime();
	}

	/**
	 * 
	 * @param date
	 * @return
	 */
	public static LocalTime date2localTime(final Date date) {
		if (date == null)
			return null;
		Instant instant = date.toInstant();
		ZoneId zone = ZoneId.systemDefault();
		LocalDateTime localDateTime = LocalDateTime.ofInstant(instant, zone);
		return localDateTime.toLocalTime();
	}

	/**
	 * localDateTime now
	 * 
	 * @return localDateTime now
	 */
	public static LocalDateTime now() {
		return LocalDateTime.now();
	}

	/**
	 * localDateTime now
	 * 
	 * @return localDateTime now
	 */
	public static LocalDateTime ldtnow() {
		return LocalDateTime.now();
	}

	/**
	 * localDate now
	 * 
	 * @return localDate now
	 */
	public static LocalDate ldnow() {
		return LocalDate.now();
	}

	/**
	 * Date now
	 * 
	 * @return Date now
	 */
	public static Date dtnow() {
		return new Date();
	}

	/**
	 * LocalTime Now
	 * 
	 * @return LocalTime Now
	 */
	public static LocalTime ltnow() {
		return LocalTime.now();
	}

	/**
	 * date2LocalDateTime的别名
	 *
	 * @param year 年份
	 * @param mon  月份
	 * @param day  日期
	 * @param hour 小时
	 * @param min  分钟
	 * @param sec  秒
	 * @param ns   纳秒
	 * @return 日期时间
	 */
	public static LocalDateTime ldt(int year, int mon, int day, int hour, int min, int sec, int ns) {
		return LocalDateTime.of(year, mon, day, hour, min, sec, ns);
	}

	/**
	 * date2LocalDateTime的别名
	 * 
	 * @param year 年份
	 * @param mon  月份
	 * @param day  日期
	 * @param hour 小时
	 * @param min  分钟
	 * @param sec  秒
	 * @return 日期时间
	 */
	public static LocalDateTime ldt(final int year, final int mon, final int day, final int hour, final int min,
			final int sec) {
		return Times.ldt(year, mon, day, hour, min, sec, 0);
	}

	/**
	 * date2LocalDateTime的别名
	 *
	 * @param year 年份
	 * @param mon  月份
	 * @param day  日期
	 * @param hour 小时
	 * @param min  分钟
	 * @return 日期时间
	 */
	public static LocalDateTime ldt(int year, int mon, int day, int hour, int min) {
		return Times.ldt(year, mon, day, hour, min, 0, 0);
	}

	/**
	 * date2LocalDateTime的别名
	 *
	 * @param year 年份
	 * @param mon  月份
	 * @param day  日期
	 * @param hour 小时
	 * @return 日期时间
	 */
	public static LocalDateTime ldt(final int year, final int mon, final int day, final int hour) {
		return Times.ldt(year, mon, day, hour, 0, 0, 0);
	}

	/**
	 * date2LocalDateTime的别名
	 *
	 * @param year 年份
	 * @param mon  月份
	 * @param day  日期
	 * @return 日期时间
	 */
	public static LocalDateTime ldt(int year, int mon, int day) {
		return Times.ldt(year, mon, day, 0, 0, 0, 0);
	}

	/**
	 * date2LocalDateTime的别名
	 *
	 * @param year 年份
	 * @param mon  月份
	 * @return 日期时间
	 */
	public static LocalDateTime ldt(final int year, final int mon) {
		return Times.ldt(year, mon, 0, 0, 0, 0, 0);
	}

	/**
	 * date2LocalDateTime的别名
	 *
	 * @param year 年份
	 * @return 日期时间
	 */
	public static LocalDateTime ldt(final int year) {
		return Times.ldt(year, 0, 0, 0, 0, 0, 0);
	}

	/**
	 * 
	 * @param year       年份
	 * @param month      月份
	 * @param dayOfMonth 月中的天分数
	 * @return
	 */
	public static LocalDate ld(final int year, final int month, final int dayOfMonth) {
		return LocalDate.of(year, month, dayOfMonth);
	}

	/**
	 * 本地日期
	 * 
	 * @param datestr 日期字符串 yyyy-MM-dd HH:mm:ss
	 * @return 日期对象
	 */
	public static LocalDate ld(final String datestr) {
		Date date = date(datestr);
		return dt2ld(date);
	}

	/**
	 * 
	 * @param hour         小时
	 * @param minute       分钟
	 * @param second       秒
	 * @param nanoOfSecond 纳秒
	 * @return 时间
	 */
	public static LocalTime lt(int hour, int minute, int second, int nanoOfSecond) {
		return LocalTime.of(hour, minute, second, nanoOfSecond);
	}

	/**
	 * 
	 * @param hour   小时
	 * @param minute 分钟
	 * @param second 纳秒
	 * @return 时间
	 */
	public static LocalTime lt(int hour, int minute, int second) {
		return LocalTime.of(hour, minute, second);
	}

	/**
	 * 
	 * @param hour   小时
	 * @param minute 分钟
	 * @return 时间
	 */
	public static LocalTime lt(int hour, int minute) {
		return LocalTime.of(hour, minute);
	}

	/**
	 * 
	 * @param hour 小时
	 * @return 时间
	 */
	public static LocalTime lt(int hour) {
		return LocalTime.of(hour, 0);
	}

	/**
	 * 整型
	 * 
	 * @param s 字符串
	 * @return 整长整型
	 */
	public static Integer i4(final String s) {
		Integer i = null;
		try {
			i = Integer.parseInt(s);
		} catch (Exception e) {
			e.printStackTrace();
		}

		return i;
	}

	/**
	 * 长整型
	 * 
	 * @param s 字符串
	 * @return 长整型
	 */
	public static Long lng(String s) {
		Long i = null;
		try {
			i = Long.parseLong(s);
		} catch (Exception e) {
			e.printStackTrace();
		}

		return i;
	}

	/**
	 * 
	 * @param s
	 * @return
	 */
	public static Double dbl(String s) {
		Double i = null;
		try {
			i = Double.parseDouble(s);
		} catch (Exception e) {
			e.printStackTrace();
		}

		return i;
	}

	/**
	 * 本地时间
	 * 
	 * @param datestr
	 * @return
	 */
	public static LocalTime lt(String datestr) {
		if (datestr == null)
			return null;
		var p1 = Pattern.compile("(\\d+):(\\d+)").matcher(datestr.strip());
		var p2 = Pattern.compile("(\\d+):(\\d+):(\\d+)").matcher(datestr.strip());
		var p3 = Pattern.compile("(\\d+):(\\d+):(\\d+)\\.(\\d+)").matcher(datestr.strip());
		LocalTime lt = null;
		try {
			if (p3.matches()) {
				lt = lt(i4(p3.group(1)), i4(p3.group(2)), i4(p3.group(3)), i4(p3.group(4)));
			} else if (p2.matches()) {
				lt = lt(i4(p2.group(1)), i4(p2.group(2)), i4(p2.group(3)));
			} else if (p1.matches()) {
				lt = lt(i4(p1.group(1)), i4(p1.group(2)));
			} else {
				// do nothing
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		return lt;
	}

	/**
	 * 提取毫秒
	 * 
	 * @param date
	 * @return
	 */
	public static int MILLISECOND(Date date) {
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(date);
		return calendar.get(Calendar.MILLISECOND);
	}

	/**
	 * 提取秒数
	 * 
	 * @param date
	 * @return
	 */
	public static int SECOND(Date date) {
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(date);
		return calendar.get(Calendar.SECOND);
	}

	/**
	 * 提取分钟
	 * 
	 * @param date
	 * @return
	 */
	public static int MINUTE(Date date) {
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(date);
		return calendar.get(Calendar.MINUTE);
	}

	/*
	 * 提取小时
	 * 
	 * @reutrn 0-23
	 */
	public static int HOUR(Date date) {
		final Calendar calendar = Calendar.getInstance();
		calendar.setTime(date);
		return calendar.get(Calendar.HOUR_OF_DAY);
	}

	/**
	 * 提取日期
	 * 
	 * @param date
	 * @return
	 */
	public static int DATEOFYEAR(Date date) {
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(date);
		return calendar.get(Calendar.DAY_OF_MONTH);
	}

	/**
	 * 提取月份
	 * 
	 * @param date
	 * @return 月份数值
	 */
	public static int MONTH(Date date) {
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(date);
		return calendar.get(Calendar.MONTH);
	}

	/**
	 * 日期的解析
	 * 
	 * @param datestr
	 * @return 日期对象
	 */
	public static Date date(String datestr) {
		Date d = null;
		if (datestr == null)
			return null;
		var s = datestr.strip();
		try {
			if (s.matches("\\d+-\\d+-\\d+")) {
				d = sdf2.parse(s);
			} else {
				d = sdf.parse(s);
			}
		} catch (Exception e) {
			//
		}

		if (d == null) {
			final var fmts = new String[] { "yy-MM-dd HH:mm:ss.SSS", "yy-MM-dd HH:mm:ss", "yy-MM-dd HH:mm",
					"yy-MM-dd HH", "yy-MM-dd", "yy-MM", "yy", "yyyy-MM-dd HH:mm:ss.SSS", "yyyy-MM-dd HH:mm:ss",
					"yyyy-MM-dd HH:mm", "yyyy-MM-dd HH", "yyyy-MM-dd", "yyyy-MM", "yyyy", "yy-MM-ddTHH:mm:ss.SSS",
					"yy-MM-ddTHH:mm:ss", "yy-MM-ddTHH:mm", "yy-MM-ddTHH", "yyyy-MM-ddTHH:mm:ss.SSS",
					"yyyy-MM-ddTHH:mm:ss", "yyyy-MM-ddTHH:mm", "yyyy-MM-ddTHH", };// fmts
			for (String fmt : fmts) {
				try {
					final var sdf = new SimpleDateFormat(fmt);
					d = sdf.parse(datestr);
				} catch (Exception e) {
					//
				}
				if (d != null)
					return d;
			} // for
		} // d==null

		return d;
	}

	/**
	 * 日期的即使
	 * 
	 * @param datestr
	 * @return 本地时间
	 */
	public static LocalDateTime ldt(String datestr) {
		return dt2ldt(date(datestr));
	}

	/**
	 * 日期格式化式样：yyyy-MM-dd HH:mm:ss
	 * 
	 * @param pattern
	 * @return
	 */
	public static DateTimeFormatter dtf(String pattern) {
		return DateTimeFormatter.ofPattern(pattern);
	}

	/**
	 * 把一个值对象转换成LocalDateTime
	 *
	 * @param value 值对象
	 * @return LocalDateTime
	 */
	public static LocalDate asLocalDate(final Object value) {
		final LocalDateTime ldt = asLocalDateTime(value);
		return ldt != null ? ldt.toLocalDate() : null;
	}

	/**
	 * 把一个值对象转换成LocalDateTime
	 *
	 * @param value 值对象
	 * @return LocalDateTime
	 */
	public static LocalDateTime asLocalDateTime(final Object value) {
		final Function<LocalDate, LocalDateTime> ld2ldt = ld -> LocalDateTime.of(ld, LocalTime.of(0, 0));
		final Function<LocalTime, LocalDateTime> lt2ldt = lt -> LocalDateTime.of(LocalDate.of(0, 1, 1), lt);
		final Function<Long, LocalDateTime> lng2ldt = lng -> {
			final Long timestamp = lng;
			final Instant instant = Instant.ofEpochMilli(timestamp);
			final ZoneId zoneId = ZoneId.systemDefault();
			return LocalDateTime.ofInstant(instant, zoneId);
		};

		final Function<Timestamp, LocalDateTime> timestamp2ldt = timestamp -> lng2ldt.apply(timestamp.getTime());

		final Function<Date, LocalDateTime> dt2ldt = dt -> lng2ldt.apply(dt.getTime());

		final Function<String, LocalTime> str2lt = line -> {
			LocalTime lt = null;
			for (String format : "HH:mm:ss,HH:mm,HHmmss,HHmm,HH".split("[,]+")) {
				try {
					lt = LocalTime.parse(line, DateTimeFormatter.ofPattern(format));
				} catch (Exception ex) {
					// do nothing
				}
				if (lt != null)
					break;
			}
			return lt;
		};

		final Function<String, LocalDate> str2ld = line -> {
			LocalDate ld = null;
			for (String format : "yyyy-MM-dd,yyyy-M-d,yyyy/MM/dd,yyyy/M/d,yyyyMMdd".split("[,]+")) {
				try {
					ld = LocalDate.parse(line, DateTimeFormatter.ofPattern(format));
				} catch (Exception ex) {
					// do nothing
				}
				if (ld != null)
					break;
			}

			return ld;
		};

		final Function<String, LocalDateTime> str2ldt = line -> {
			LocalDateTime ldt = null;
			final String patterns = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSSSS," //
					+ "yyyy-MM-dd'T'HH:mm:ss.SSSSSS," //
					+ "yyyy-MM-dd'T'HH:mm:ss.SSS," //
					+ "yyyy-MM-dd'T'HH:mm:ss," //
					+ "yyyy-MM-ddTHH:mm:ss.SSSSSSSSS," //
					+ "yyyy-MM-ddTHH:mm:ss.SSSSSS," //
					+ "yyyy-MM-ddTHH:mm:ss.SSS," //
					+ "yyyy-MM-ddTHH:mm:ss," //
					+ "yyyy-MM-dd HH:mm:ss," //
					+ "yyyy-MM-dd HH:mm," //
					+ "yyyy-MM-dd HH," //
					+ "yyyy-M-d H:m:s," //
					+ "yyyy-M-d H:m," //
					+ "yyyy-M-d H," //
					+ "yyyy/MM/dd HH:mm:ss," //
					+ "yyyy/MM/dd HH:mm," //
					+ "yyyy/MM/dd HH," //
					+ "yyyy/M/d H:m:s," //
					+ "yyyy/M/d H:m," //
					+ "yyyy/M/d H," //
					+ "yyyyMMddHHmmss," //
					+ "yyyyMMddHHmm," //
					+ "yyyyMMddHH"//
			; // patterns 时间的格式字符串

			for (String format : patterns.split("[,]+")) {
				try {
					ldt = LocalDateTime.parse(line, DateTimeFormatter.ofPattern(format));
				} catch (Exception ex) {
					// do nothing
				}
				if (ldt != null)
					break;
			}

			return ldt;
		};

		if (value instanceof LocalDateTime) {
			return (LocalDateTime) value;
		} else if (value instanceof LocalDate) {
			return ld2ldt.apply((LocalDate) value);
		} else if (value instanceof LocalTime) {
			return lt2ldt.apply((LocalTime) value);
		} else if (value instanceof Number) {
			return lng2ldt.apply(((Number) value).longValue());
		} else if (value instanceof Timestamp) {
			return timestamp2ldt.apply(((Timestamp) value));
		} else if (value instanceof Date) {
			return dt2ldt.apply(((Date) value));
		} else if (value instanceof String) {
			final String line = (String) value;
			final LocalDateTime _ldt = str2ldt.apply(line);
			if (Objects.nonNull(_ldt)) {
				return _ldt;
			}
			final LocalDate _ld = str2ld.apply(line);
			if (Objects.nonNull(_ld)) {
				return ld2ldt.apply(_ld);
			}
			final LocalTime _lt = str2lt.apply(line);
			if (Objects.nonNull(_lt)) {
				return lt2ldt.apply(_lt);
			}
			return null;
		} else {
			return null;
		}
	}

	public static final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	public static final SimpleDateFormat sdf2 = new SimpleDateFormat("yyyy-MM-dd");
} // class CronTime