package gbench.util.jdbc.json;

import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Calendar;
import java.util.regex.Pattern;

/**
 * 基本时间操作函数
 */
public class CronTime {
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
		return CronTime.ldt(year, mon, day, hour, min, sec, 0);
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
		return CronTime.ldt(year, mon, day, hour, min, 0, 0);
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
		return CronTime.ldt(year, mon, day, hour, 0, 0, 0);
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
		return CronTime.ldt(year, mon, day, 0, 0, 0, 0);
	}

	/**
	 * date2LocalDateTime的别名
	 *
	 * @param year 年份
	 * @param mon  月份
	 * @return 日期时间
	 */
	public static LocalDateTime ldt(final int year, final int mon) {
		return CronTime.ldt(year, mon, 0, 0, 0, 0, 0);
	}

	/**
	 * date2LocalDateTime的别名
	 *
	 * @param year 年份
	 * @return 日期时间
	 */
	public static LocalDateTime ldt(final int year) {
		return CronTime.ldt(year, 0, 0, 0, 0, 0, 0);
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

	public static final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	public static final SimpleDateFormat sdf2 = new SimpleDateFormat("yyyy-MM-dd");
} // class CronTime
