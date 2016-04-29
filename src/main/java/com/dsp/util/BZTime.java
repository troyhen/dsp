package com.dsp.util;

import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import com.dsp.ThreadState;

import static com.dsp.util.BZCast.*;
import static com.dsp.util.BZText.*;

public class BZTime
{
	public static final boolean DEBUG_MODE = false;
	
		/** The word "day", used in dateAdd(), dateSub(), instantAdd(), and instantSub() */
	private static final String DAY			= "day";
		/** The word "days", used in dateAdd(), dateSub(), instantAdd(), and instantSub() */
	private static final String DAYS		= "days";
	private static final String MONTH		= "month";
	private static final String MONTHS		= "months";
	private static final String LONG		= "long";
	private static final String SHORT		= "short";
	private static final String YEAR		= "year";
	private static final String YEARS		= "years";
		/** The words 'sql' and 'mil', used in date(), time(), and instant() */
	private static final String SQL			= "sql";
	private static final String MIL			= "mil";
	private static final String	ACCESS		= "access";
	
	static final Calendar cal = Calendar.getInstance();
	private static final DateFormat dform = DateFormat.getDateInstance(DateFormat.SHORT);
	private static final DateFormat dateAmer = new SimpleDateFormat("MM/dd/yyyy");
	private static final DateFormat datelong = new SimpleDateFormat("EEEE, MMMM dd, yyyy");
	private static final DateFormat instantAmer = new SimpleDateFormat("MM/dd/yyyy hh:mm a");
	private static final DateFormat instantAccessForm = new SimpleDateFormat("MM/dd/yy HH:mm:ss");
	private static final DateFormat instantlong = new SimpleDateFormat("EEEE, MMMM dd, yyyy hh:mm:ss.SSSS a");
	private static final DateFormat timelong = new SimpleDateFormat("hh:mm:ss.SSSS a");
	private static final DateFormat timeshort = new SimpleDateFormat("hh:mm:ss a");
	private static final DateFormat timeshort2 = new SimpleDateFormat("hh:mm a");
	private static final DateFormat timeshort3 = new SimpleDateFormat("hh:mma");
	private static final DateFormat timeshort4 = new SimpleDateFormat("hh:mm:ssa");
	private static final DateFormat timeshort5 = new SimpleDateFormat("HH:mm:ss");
	private static final DateFormat timeshort6 = new SimpleDateFormat("HH:mm");
	private static final DateFormat zonedate = new SimpleDateFormat("yyyyMMdd");
	private static final DateFormat zoneinstant = new SimpleDateFormat("yyyyMMddHHmmssSSSS");
	private static final DateFormat zonetime = new SimpleDateFormat("HHmmss");

	private static final DecimalFormat tens = new DecimalFormat("#,##0.#");

	/**
	 * Convert to Date. This function converts the data to a Date object.
	 */
	@SuppressWarnings("deprecation")
	public static Date _Date(Object obj) throws NumberFormatException
	{
		if (obj == null) return null;
		try {
			return (Date)obj;
		} catch (Exception e) {
			try {
				return new Date(((java.util.Date)obj).getTime());
			} catch (Exception e1) {
				try {
					int date = ((Number)obj).intValue();
					return new Date(date / 10000, date / 100 % 100 - 1, date % 100);
				} catch (Exception e2) {
					String value = _String(obj).trim();
					if (value.length() == 0) return null;
					try {
						return Date.valueOf(value);
					} catch (Exception e3) {
						try {
							return new Date(dform.parse(value).getTime());
						} catch (Exception e4) {
							try {
								return new Date(zonedate.parse(value).getTime());
							} catch (Exception e5) {
								throw new NumberFormatException("Cannot convert '" + obj + "' to a date");
							}
						}
					}
				}
			}
		}
	} // _Date()

	/**
	 * Convert Object to Timestamp.
	 */
	@SuppressWarnings("deprecation")
	public static Timestamp _Instant(Object obj) throws NumberFormatException
	{
		if (obj == null) return null;
		try {
			return (Timestamp)obj;
		} catch (Exception e) {
			try {
				return new Timestamp(((java.util.Date)obj).getTime());
			} catch (Exception e1) {
				try {
					long time = ((Number)obj).longValue();
					int msec = (int)(time % 10000);
					time /= 10000;
					int sec = (int)(time % 100);
					time /= 100;
					int min = (int)(time % 100);
					time /= 100;
					int hr = (int)(time % 100);
					time /= 100;
					int day = (int)(time % 100);
					time /= 100;
					int mon = (int)(time % 100);
					int year = (int)(time / 100);
					return new Timestamp(year, mon - 1, day, hr, min, sec, msec);
				} catch (Exception e2) {
					String value = _String(obj).trim();
					if (value.length() == 0) return null;
					try {
						return Timestamp.valueOf(value);
					} catch (Exception e3) {
						try {
							return new Timestamp(zoneinstant.parse(value).getTime());
						} catch (Exception e4) {
                            try {
                                return new Timestamp(_Date(obj).getTime());
                            } catch (Exception e5) {
    							throw new NumberFormatException("Cannot convert '" + obj + "' to instant");
                            }
						}
					}
				}
			}
		}
	} // _Instant()

	/**
	 * Convert to Time. This function converts the data to a Time object.
	 */
	@SuppressWarnings("deprecation")
	public static Time _Time(Object obj) throws NumberFormatException
	{
		if (obj == null) return null;
		try {
			return (Time)obj;
		} catch (Exception e) {
			try {
				return new Time(((java.util.Date)obj).getTime());
			} catch (Exception e1) {
				try {
					int time = ((Number)obj).intValue();
					return new Time(time / 10000, time / 100 % 100, time % 100);
				} catch (Exception e2) {
					String value = _String(obj).trim();
					if (value.length() == 0) return null;
					try {
						return new Time(timeshort.parse(value).getTime());
					} catch (Exception e3) {
						try {
							return new Time(timeshort2.parse(value).getTime());
						} catch (Exception e4) {
							try {
								return new Time(timeshort3.parse(value).getTime());
							} catch (Exception e5) {
								try {
									return new Time(timeshort4.parse(value).getTime());
								} catch (Exception e6) {
									try {
										return new Time(timeshort5.parse(value).getTime());
									} catch (Exception e7) {
										try {
											return new Time(timeshort6.parse(value).getTime());
										} catch (Exception e8) {
											try {
												return new Time(timelong.parse(value).getTime());
											} catch (Exception e9) {
												try {
													return Time.valueOf(value);
												} catch (Exception e10) {
													try {
														return new Time(zonetime.parse(value).getTime());
													} catch (Exception e11) {
														throw new NumberFormatException("Cannot convert '" + obj + "' to time");
													}
												}
											}
										}
									}
								}
							}
						}
					}
				}
			}
		}
	} // _Time()

	/**
	 * Date Conversion and Extraction. This multipurpose function can convert a Date object
	 * to a String in several formats, or can extract parts of the date.  This function only
	 * accepts one or two arg elements.  If two, the first is the requested function
	 * to perform, and the second is converted to java.sql.Date.  If one, the request is assumed
	 * to be "short". The following are the types of functions performed based on the request:
	 * <table><tr><td>Request (arg1)</td><td>Returned</td><td>Type</td><td>Example</td></tr>
	 * <tr><td>short</td><td>Short Date</td><td>String</td><td>2000-11-20</td></tr>
	 * <tr><td>long</td><td>Long Date</td><td>String</td><td>Monday, November 20, 2000</td></tr>
	 * <tr><td>sql</td><td>SQL Date</td><td>String</td><td>{d '2000-11-20'}</td></tr>
	 * <tr><td>year</td><td>Year</td><td>Integer</td><td>2000</td></tr>
	 * <tr><td>month</td><td>Month</td><td>Integer</td><td>11</td></tr>
	 * <tr><td>day</td><td>Day of Month</td><td>Integer</td><td>20</td></tr></table>
	 * @deprecated
	 */
	public static Object date(Object[] args) throws IllegalArgumentException
	{
		int psize = args.length;
		if (!(psize == 1 || psize == 2))
				throw new IllegalArgumentException("date() can only accept 1 or 2 arguments");
		String type = null;
		Date date;
		if (psize == 2)
		{
			type = _String(args[0]);
			date = _Date(args[1]);
		}
		else date = _Date(args[0]);
		Object result;
		if (psize == 1 || SHORT.equalsIgnoreCase(type)) result = dateShort(date);
		else
		if (LONG.equalsIgnoreCase(type)) result = dateLong(date);
		else
		if (SQL.equalsIgnoreCase(type)) result = dateSql(date);
		else
		if (YEAR.equalsIgnoreCase(type)) result = new Integer(dateYear(date));
		else
		if (MONTH.equalsIgnoreCase(type)) result = new Integer(dateMonth(date));
		else
		if (DAY.equalsIgnoreCase(type)) result = new Integer(dateDay(date));
		else
			throw new IllegalArgumentException("date(" + type + ", " + date + ") error, unknown date type");
		if (DEBUG_MODE) ThreadState.logln("date(" + psize + " args) -> " + result);
		return result;
	} // date()

	/**
	 * Add date units to a Date.  This function adds years, months, weeks, or days to a Date.
	 * This function accepts either two or three arg elements.  The first element
	 * is converted to a java.sql.Date.  The second is converted to an int.  The last
	 * if present is converted to a String and specifies the type of addition to perform.
	 * If only two elements are sent the type is assumed to be "days".  Here are
	 * the allowed addition requests: year, years, month, months, week, weeks, day, or days.
	 */
	public static Date dateAdd(Object[] args) throws IllegalArgumentException
	{
		int psize = args.length;
		if (DEBUG_MODE) ThreadState.logln("dateAdd(" + psize + " args)");
		if (!(psize == 2 || psize == 3))
				throw new IllegalArgumentException("dateAdd() can only accept 2 or 3 arguments");
		Date date = _Date(args[0]);
		int value = _int(args[1]);
		String str = "days";
		if (psize == 3)
		{
			str = _String(args[2]);
		}
		int type = 'd';
		if (str.length() > 0) type = Character.toLowerCase(str.charAt(0));
		Calendar c = Calendar.getInstance();
		c.setTime(date);
		switch (type)
		{
			case 'd':
				c.add(Calendar.DATE, value);
				break;
			case 'm':
				c.add(Calendar.MONTH, value);
				break;
			case 'w':
				c.add(Calendar.WEEK_OF_YEAR, value);
				break;
			case 'y':
				c.add(Calendar.YEAR, value);
				break;
			default:
				throw new IllegalArgumentException("Unknown value type: " + str);
		}
		return new Date(c.getTime().getTime());
	} // dateAdd()

	/**
	 * Returns the date in the American standar format MM/DD/YYYY
	 */
	public static String dateAmerican(Date date)
	{
		String result;
		if (date == null) result = null;
		else result = dateAmer.format(date);
		if (DEBUG_MODE) ThreadState.logln("dateAmerican(" + date + ") -> " + result);
		return result;
	} // dateAmerican()

	/**
	 * Return the day of the month of the Date.
	 */
	public static int dateDay(Date date)
	{
		int result;
		if (date == null) result = 0;
		else
		{
			Calendar cal = Calendar.getInstance();
			cal.setTime(date);
			result = cal.get(Calendar.DAY_OF_MONTH);
		}
		if (DEBUG_MODE) ThreadState.logln("dateDay(" + date + ") -> " + result);
		return result;
	} // dateDay()

	/**
	 * Returns the difference between the two dates as a String.
	 */
	public static String dateDiff(Date date1, Date date2)
	{
		return msecDiff(date1.getTime(), date2.getTime());
	} // dateDiff()

	/**
	 * Return the Date as a String in long format.
	 */
	public static String dateLong(Date date)
	{
		String result;
		if (date == null) result = null;
		else result = datelong.format(date);
		if (DEBUG_MODE) ThreadState.logln("dateLong(" + date + ") -> " + result);
		return result;
	} // dateLong()

	/**
	 * Return the month of the Date, as a number 1-12.
	 */
	public static int dateMonth(Date date)
	{
		int result;
		if (date == null) result = 0;
		else
		{
			Calendar cal = Calendar.getInstance();
			cal.setTime(date);
			result = cal.get(Calendar.MONTH) + 1;
		}
		if (DEBUG_MODE) ThreadState.logln("dateMonth(" + date + ") -> " + result);
		return result;
	} // dateMonth()

	/**
	 * Return the Date as a String in short format.
	 */
	public static String dateShort(Date date)
	{
		String result;
		if (date == null) result = null;
		else result = date.toString();
		if (DEBUG_MODE) ThreadState.logln("dateShort(" + date + ") -> " + result);
		return result;
	} // dateShort()

	/**
	 * Return the Date as a String formated for use in an SQL statement.
	 */
	public static String dateSql(Date date)
	{
		String result;
		if (date == null) result = NULL;
		else result = "{d '" + date + "'}";
		if (DEBUG_MODE) ThreadState.logln("dateSql(" + date + ") -> " + result);
		return result;
	} // dateSql()

	/**
	 * Subtract date units from a Date.  This function subtracts years, months, weeks, or days from a Date.
	 * This function accepts either two or three arg elements.  The first element
	 * is converted to a java.sql.Date.  The second is the value converted to an int.  The last
	 * if present is converted to a String and specifies the type of subtraction to perform.
	 * If only two elements are sent the type is assumed to be "days".  Here are
	 * the allowed subtraction requests: year, years, month, months, week, weeks, day, or days.
	 */
	public static Date dateSub(Object[] args) throws IllegalArgumentException
	{
		int psize = args.length;
		if (DEBUG_MODE) ThreadState.logln("dateSub(" + psize + " args)");
		if (!(psize == 2 || psize == 3))
				throw new IllegalArgumentException("dateSub() can only accept 2 or 3 arguments");
		int value = _int(args[1]);
		args[1] = new Integer(-value);
		return dateAdd(args);
	} // dateSub()

	/**
	 * Return the year of the Date.
	 */
	public static int dateYear(Date date)
	{
		int result;
		if (date == null) result = 0;
		else
		{
			Calendar cal = Calendar.getInstance();
			cal.setTime(date);
			result = cal.get(Calendar.YEAR);
		}
		if (DEBUG_MODE) ThreadState.logln("dateYear(" + date + ")");
		return result;
	} // dateYear()

	/**
	 * Format Timestamp in various ways. This multipurpose function can convert a Timestamp object
	 * to a String in several formats.  This function only
	 * accepts one or two arg elements.  If two, the first is the requested function
	 * to perform, and the second is converted to java.sql.Time.  If one, the request is assumed
	 * to be "short". The following are the types of functions performed based on the request:
	 * <table><tr><td>Request (arg1)</td><td>Returned</td><td>Type</td><td>Example</td></tr>
	 * <tr><td>short</td><td>Short Timestamp</td><td>String</td><td>2000-11-20 12:13 PM</td></tr>
	 * <tr><td>long</td><td>Long Timestamp</td><td>String</td><td>Monday, November 20, 2000 12:13:52.239 PM MST</td></tr>
	 * <tr><td>sql</td><td>SQL Timestamp</td><td>String</td><td>{ts '2000-11-20 12:13:52.239 PM MST'}</td></tr>
	 * <tr><td>access</td><td>MS Access Timestamp</td><td>String</td><td>#2000-11-20 12:13:56 PM#</td></tr></table>
	 * @deprecated
	 */
	public static Object instant(Object[] args) throws IllegalArgumentException, NumberFormatException
	{
		int psize = args.length;
		if (!(psize == 1 || psize == 2))
				throw new IllegalArgumentException("instant() can only accept 1 or 2 arguments");
		String type = null;
		Timestamp date;
		if (psize == 2)
		{
			type = _String(args[0]);
			date = _Instant(args[1]);
		}
		else date = _Instant(args[0]);
		String result;
		if (psize == 1
				|| SHORT.equalsIgnoreCase(type)) result = instantShort(date);
		else
		if (LONG.equalsIgnoreCase(type)) result = instantLong(date);
		else
		if (SQL.equalsIgnoreCase(type)) result = instantSql(date);
		else
		if (ACCESS.equalsIgnoreCase(type)) result = instantAccess(date);
		else throw new IllegalArgumentException("instant(" + type + ", " + date + ") error, unknown instant function");
		if (DEBUG_MODE) ThreadState.logln("instant(" + args + ") -> " + result);
		return result;
	} // instant()

	/**
	 * Convert Timestamp to an MS Access SQL String.  Converts the Timestamp to a String for use
	 * in an SQL statement when using Microsoft Access(tm).  This is needed because MS Access,
	 * using the JDBC-ODBC bridge, doesn't support the standard JDBC Timestamp format.
	 */
	public static String instantAccess(Timestamp date)
	{
		String result;
		if (date == null) result = NULL;
		else result = '#' + instantAccessForm.format(date) + '#';
		if (DEBUG_MODE) ThreadState.logln("instantAccess(" + date + ") -> " + result);
		return result;
	} // instantAccess()

	/**
	 * Add units of time to a Timestamp.  Adds years, months, weeks, days, hours,
	 * minutes, seconds, milliseconds from Time.  It accepts 2 or three members of args.  The first
	 * is convert to Time.  The second is the value to be added.  The third, if present, is
	 * the units String: years, months, weeks, days, hours, mins, secs, msecs.  If no third member
	 * is present, the units are assumed to be milliseconds.
	 */
	public static Timestamp instantAdd(Object[] args) throws IllegalArgumentException, NumberFormatException
	{
		int psize = args.length;
		if (!(psize == 2 || psize == 3))
				throw new IllegalArgumentException("instantAdd() can only accept 2 or 3 arguments");
		Timestamp date = _Instant(args[0]);
		int value = _int(args[1]);
		String str = "msec";
		if (psize == 3)
		{
			str = _String(args[2]);
		}
		int type = 'i';
		if (str.length() > 0) type = Character.toLowerCase(str.charAt(0));
		if (str.length() > 1 && type == 'm')
		{
			int t2 = Character.toLowerCase(str.charAt(1));
			if (t2 == 's') type = 'i';
			else
			if (t2 == 'i') type = 'n';
		}
		Calendar c = Calendar.getInstance();
		c.setTime(date);
		switch (type)
		{
			case 'i':
				c.add(Calendar.MILLISECOND, value);
				break;
			case 's':
				c.add(Calendar.SECOND, value);
				break;
			case 'n':
				c.add(Calendar.MINUTE, value);
				break;
			case 'h':
				c.add(Calendar.HOUR, value);
				break;
			case 'd':
				c.add(Calendar.DATE, value);
				break;
			case 'm':
				c.add(Calendar.MONTH, value);
				break;
			case 'w':
				c.add(Calendar.WEEK_OF_YEAR, value);
				break;
			case 'y':
				c.add(Calendar.YEAR, value);
				break;
			default:
				throw new IllegalArgumentException("Unknown value type: " + str);
		}
		Timestamp result = new Timestamp(c.getTime().getTime());
		if (DEBUG_MODE) ThreadState.logln("instantAdd(" + args + ") -> " + result);
		return result;
	} // instantAdd()

	/**
	 * Returns the date and time in the American standar format MM/DD/YYYY HH:MM AM|PM
	 */
	public static String instantAmerican(Date date)
	{
		String result;
		if (date == null) result = null;
		else result = instantAmer.format(date);
		if (DEBUG_MODE) ThreadState.logln("instantAmerican(" + date + ") -> " + result);
		return result;
	} // instantAmerican()

	/**
	 * Returns a String with the computed difference between two Timestamps.
	 */
	public static String instantDiff(Timestamp time1, Timestamp time2)
	{
		return msecDiff(time1.getTime(), time2.getTime());
	} // instantDiff()

	/**
	 * Convert Instant to long String.  Converts the Timestamp to a long formatted String.
	 */
	public static String instantLong(Timestamp date)
	{
		String result;
		if (date == null) result = null;
		else result = instantlong.format(date);
		if (DEBUG_MODE) ThreadState.logln("instantLong(" + date + ") -> " + result);
		return result;
	} // instantLong()

	/**
	 * Convert Instant to short String.  Converts the Timestamp to a short formatted String.
	 */
	public static String instantShort(Timestamp date)
	{
		String result;
		if (date == null) result = null;
		else result = date.toString();
		if (DEBUG_MODE) ThreadState.logln("instantShort(" + date + ") -> " + result);
		return result;
	} // instantShort()

	/**
	 * Convert Timestamp to an SQL String.  Converts the Timestamp to a String for use
	 * in an SQL statement.
	 */
	public static String instantSql(Timestamp date)
	{
		String result;
		if (date == null) result = NULL;
		else result = "{ts '" + date + "'}";
		if (DEBUG_MODE) ThreadState.logln("instantSql(" + date + ") -> " + result);
		return result;
	} // instantSql()

	/**
	 * Subtract units of time from a Timestamp.  Subtracts years, months, weeks, days, hours,
	 * minutes, seconds, milliseconds from Time. It accepts 2 or three members of args.  The first
	 * is convert to Time.  The second is the value to be subtracted.  The third, if present, is
	 * the units String: years, months, weeks, days, hours, mins, secs, msecs.  If no third member
	 * is present, the units are assumed to be milliseconds.
	 */
	public static Timestamp instantSub(Object[] args) throws IllegalArgumentException, NumberFormatException
	{
		int psize = args.length;
		if (!(psize == 2 || psize == 3))
				throw new IllegalArgumentException("instantSub() can only accept 2 or 3 arguments");
		args[1] = new Integer(-_int(args[1]));
		Timestamp result = instantAdd(args);
		if (DEBUG_MODE) ThreadState.logln("instantSub(" + args + ") -> " + result);
		return result;
	} // instantSub()

	/**
	 * Returns true if the object is a java.sql.Date or can be converted to one.
	 */
	public static boolean isDate(Object obj)
	{
		boolean result = false;
		try {
			result = _Date(obj) != null;
		} catch (Exception e) {
		}
		if (DEBUG_MODE) ThreadState.logln("isDate(" + obj + ") -> " + result);
		return result;
	} // isDate()

	/**
	 * Returns true if the object is a java.sql.Timestamp or can be converted to one.
	 */
	public static boolean isInstant(Object obj)
	{
		boolean result = false;
		try {
			result = _Instant(obj) != null;
		} catch (Exception e) {
		}
		if (DEBUG_MODE) ThreadState.logln("isInstant(" + obj + ") -> " + result);
		return result;
	} // isInstant()

	/**
	 * Returns true if the object is a java.sql.Time or can be converted to one.
	 */
	public static boolean isTime(Object obj)
	{
		boolean result = false;
		try {
			result = _Time(obj) != null;
		} catch (Exception e) {
		}
		if (DEBUG_MODE) ThreadState.logln("isTime(" + obj + ") -> " + result);
		return result;
	} // isTime()

	/**
	 * Retuns the difference between two millisecond time values, as a String.
	 */
	public static String msecDiff(long date1, long date2)
	{
		double diff = date1 > date2 ? date1 - date2 : date2 - date1;
		String result = null;
		if (diff == 0) result = "no time";
		else
		{
			if (diff < 1000) result = diff + " milliseconds";
			else
			{
				diff /= 1000;
				if (diff < 60) result = tens.format(diff) + " seconds";
				else
				{
					diff /= 60;
					if (diff < 60) result = tens.format(diff) + " minutes";
					else
					{
						diff /= 60;
						if (diff < 24) result = tens.format(diff) + " hours";
						else
						{
							diff /= 24l;
							if (diff < 31) result = tens.format(diff) + " days";
							else if (diff < 365) result = tens.format(diff / 31) + " months";
							else result = tens.format(diff / 365) + " years";
						}
					}
				}
			}
		}
		if (DEBUG_MODE) ThreadState.logln("msecDiff(" + date1 + ", " + date2 + ") -> " + result);
		return result;
	} // msecDiff()

	/**
	 * Return the Time of day.
	 */
	public static Time now()
	{
		Time result = new Time(System.currentTimeMillis());
		if (DEBUG_MODE) ThreadState.logln("now() => " + result);
		return result;
	} // now()

	/**
	 * Return the Timestamp of this instant.
	 */
	public static Timestamp rightNow()
	{
		return new Timestamp(System.currentTimeMillis());
	} // rightNow()

	/**
	 * Format Time in various ways. This multipurpose function can convert a Time object
	 * to a String in several formats, or extract parts of the date.  This function only
	 * accepts one or two arg elements.  If two, the first is the requested function
	 * to perform, and the second is converted to java.sql.Time.  If one, the request is assumed
	 * to be "short". The following are the types of functions performed based on the request:
	 * <table><tr><td>Request (arg1)</td><td>Returned</td><td>Type</td><td>Example</td></tr>
	 * <tr><td>short</td><td>Short Time</td><td>String</td><td>12:13 PM</td></tr>
	 * <tr><td>long</td><td>Long Time</td><td>String</td><td>12:13:52.239 PM</td></tr>
	 * <tr><td>sql</td><td>SQL Time</td><td>String</td><td>{t '12:13:52.239 PM'}</td></tr></table>
	 */
	public static Object time(Object[] args) throws IllegalArgumentException, NumberFormatException
	{
		int psize = args.length;
		if (!(psize == 1 || psize == 2))
				throw new IllegalArgumentException("time() can only accept 1 or 2 arguments");
		String type = null;
		Time date;
		if (psize == 2)
		{
			type = _String(args[0]);
			date = _Time(args[1]);
		}
		else date = _Time(args[0]);
		Object result;
		if (psize == 1
				|| SHORT.equalsIgnoreCase(type)) result = timeShort(date);
		else
		if (LONG.equalsIgnoreCase(type)) result = timeLong(date);
		else
		if (SQL.equalsIgnoreCase(type)) result = timeSql(date);
		else
		if (MIL.equalsIgnoreCase(type)) result = timeMil(date);
		else throw new IllegalArgumentException("time(" + type + ", " + date + ") error, unknown time function");
		if (DEBUG_MODE) ThreadState.logln("time(" + args + ") -> " + result);
		return result;
	} // time()

	/**
	 * Add units to Time.  Adds hours, minutes, seconds, milliseconds to Time.
	 * It accepts 2 or three members of args.  The first is convert to Time.  The second
	 * is the value to be added.  The third, if present, is the units String: hours,
	 * mins, secs, msecs.  If no third member is present, the units are assumed to be seconds.
	 */
	public static Time timeAdd(Object[] args) throws IllegalArgumentException, NumberFormatException
	{
		int psize = args.length;
		if (!(psize == 2 || psize == 3))
				throw new IllegalArgumentException("timeAdd() can only accept 2 or 3 arguments");
		Time date = _Time(args[0]);
		int value = _int(args[1]);
		String str = "seconds";
		if (psize == 3)
		{
			str = _String(args[2]);
		}
		int type = 's';
		if (str.length() > 0) type = Character.toLowerCase(str.charAt(0));
		if (str.length() > 1 && type == 'm' && Character.toLowerCase(str.charAt(1)) == 's')
		{
			type = 'i';
		}
		Calendar c = Calendar.getInstance();
		c.setTime(date);
		switch (type)
		{
			case 'i':
				c.add(Calendar.MILLISECOND, value);
				break;
			case 's':
				c.add(Calendar.SECOND, value);
				break;
			case 'm':
				c.add(Calendar.MINUTE, value);
				break;
			case 'h':
				c.add(Calendar.HOUR, value);
				break;
			default:
				throw new IllegalArgumentException("Unknown value type: " + str);
		}
		Time result = new Time(c.getTime().getTime());
		if (DEBUG_MODE) ThreadState.logln("timeAdd(" + args + ") -> " + result);
		return result;
	} // timeAdd()

	/**
	 * Retuns the difference between two Timestamps, as a String.
	 */
	public static String timeDiff(Timestamp time1, Timestamp time2)
	{
		return msecDiff(time1.getTime(), time2.getTime());
	} // timeDiff()

	/**
	 * Convert Time to long String.  Returns a String with the time in long format.
	 */
	public static String timeLong(Time date)
	{
		String result;
		if (date == null) result = null;
		else result = timelong.format(date);
		if (DEBUG_MODE) ThreadState.logln("timeLong(" + date + ") -> " + result);
		return result;
	} // timeLong()

	/**
	 * Convert Time to a String.  Returns a String with the time in military format.
	 */
	public static String timeMil(Time date)
	{
		if (DEBUG_MODE) ThreadState.logln("timeShort(" + date + ") -> " + date);
		if (date == null) return null;
		return date.toString();
	} // timeMil()

	/**
	 * Convert Time to a short String.  Returns a String with the time in short format.
	 */
	public static String timeShort(Time date)
	{
		if (DEBUG_MODE) ThreadState.logln("timeShort(" + date + ") -> " + date);
		if (date == null) return null;
		return timeshort2.format(date);
	} // timeShort()

	/**
	 * Convert Time to SQL.  Returns a String with the time encoded for use in an SQL statement.
	 */
	public static String timeSql(Time date)
	{
		String result;
		if (date == null) result = NULL;
		result = "{t '" + date + "'}";
		if (DEBUG_MODE) ThreadState.logln("timeSql(" + date + " -> " + result);
		return result;
	} // timeSql()

	/**
	 * Subtract units of time from Time.  Subtracts hours, minutes, seconds, milliseconds from Time.
	 * It accepts 2 or three members of args.  The first is convert to Time.  The second
	 * is the value to be subtracted.  The third, if present, is the units String: hours,
	 * mins, secs, msecs.  If no third member is present, the units are assumed to be seconds.
	 */
	public static Time timeSub(Object[] args) throws IllegalArgumentException, NumberFormatException
	{
		int psize = args.length;
		if (!(psize == 2 || psize == 3))
				throw new IllegalArgumentException("timeSub() can only accept 2 or 3 arguments");
		args[1] = new Integer(-_int(args[1]));
		Time result = timeAdd(args);
		if (DEBUG_MODE) ThreadState.logln("timeSub(" + args + ") -> " + result);
		return result;
	} // timeSub()

	/**
	 * Return today's Date.
	 */
	public static Date today()
	{
		return new Date(System.currentTimeMillis());
	} // today()

}
