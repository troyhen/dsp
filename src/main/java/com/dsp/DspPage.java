/* Copyright 2015 Troy D. Heninger
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.dsp;

import java.io.IOException;
import java.io.File;
import java.lang.reflect.*;	// Field, Method, Modifier
import java.sql.*;
import java.text.*;	// DateFormat
import java.util.Calendar;
import java.util.Collection;
import java.util.Iterator;
import java.util.Random;

import javax.servlet.*;
import javax.servlet.http.*;
import javax.servlet.jsp.*;
import javax.servlet.jsp.tagext.*;

/**
 * This class is the default class that all DSP pages extend.  When a page
 * is written to extend another class then an object of this class still
 * be created as the page object.  Functions of this class are treated special
 * in the DSP parser.  Any arguments passed to functions which do not start
 * with an underscore '_', will be converted to the proper type automatically.
 * Functions that accept multiple arguments accept Object arrays.  These functions
 * call allow any number of arguments and the DSP parser will send all arguments
 * as elements of the array.
 */
public abstract class DspPage extends HttpServlet implements HttpJspPage
{
	private static final long serialVersionUID = 6527415972891184043L;
		/** The empty string */
	public static final String EMPTY		= "";
		/** The word NULL */
	public static final String NULL			= "NULL";
		/** The word "day", used in dateAdd(), dateSub(), instantAdd(), and instantSub() */
	public static final String DAY			= "day";
		/** The word "days", used in dateAdd(), dateSub(), instantAdd(), and instantSub() */
	public static final String DAYS			= "days";
	public static final String MONTH		= "month";
	public static final String MONTHS		= "months";
	public static final String LONG			= "long";
	public static final String SHORT		= "short";
	public static final String YEAR			= "year";
	public static final String YEARS		= "years";
		/** The words 'sql' and 'mil', used in date(), time(), and instant() */
	public static final String SQL			= "sql";
	public static final String MIL			= "mil";

	public static final String	ACCESS	= "access";
	public static final String	HTML		= "html";
	public static final String	DOMAIN	= "domain";
	public static final String	LINKS		= "links";
	public static final String	SPACE		= "space";

	public static final int SQL_STMT		= 0;
	public static final int ARRAY_STMT	= 1;
	public static final int DIR_STMT		= 2;
	public static final int SCAN_STMT		= 3;

	static final boolean DEBUG_MODE = false;
//	static final boolean TRACE_MODE = false;

	static final Calendar cal = Calendar.getInstance();
	static final DateFormat dform = DateFormat.getDateInstance(DateFormat.SHORT);
	static final DateFormat dateAmer = new SimpleDateFormat("MM/dd/yyyy");
	static final DateFormat datelong = new SimpleDateFormat("EEEE, MMMM dd, yyyy");
	static final DateFormat instantAmer = new SimpleDateFormat("MM/dd/yyyy hh:mm a");
	static final DateFormat instantAccessForm = new SimpleDateFormat("MM/dd/yy HH:mm:ss");
	static final DateFormat instantlong = new SimpleDateFormat("EEEE, MMMM dd, yyyy hh:mm:ss.SSSS a");
	static final DateFormat timelong = new SimpleDateFormat("hh:mm:ss.SSSS a");
	static final DateFormat timeshort = new SimpleDateFormat("hh:mm:ss a");
	static final DateFormat timeshort2 = new SimpleDateFormat("hh:mm a");
	static final DateFormat timeshort3 = new SimpleDateFormat("hh:mma");
	static final DateFormat timeshort4 = new SimpleDateFormat("hh:mm:ssa");
	static final DateFormat timeshort5 = new SimpleDateFormat("HH:mm:ss");
	static final DateFormat timeshort6 = new SimpleDateFormat("HH:mm");
	static final DateFormat zonedate = new SimpleDateFormat("yyyyMMdd");
	static final DateFormat zoneinstant = new SimpleDateFormat("yyyyMMddHHmmssSSSS");
	static final DateFormat zonetime = new SimpleDateFormat("HHmmss");
	static final DecimalFormat tens = new DecimalFormat("#,##0.#");
	static final DecimalFormat percent = new DecimalFormat("#,##0.#%");

	private static String[] types = {"<a", "</a", ".com", ".net", ".org", ".edu", ".gov", ".mil",};

	protected DspProp prop;

	/**
	 * Integer absolute value.  Returns the absolute value of <b>arg</b>.
	 */
	public static int abs(int arg)
	{
		return arg < 0 ? -arg : arg;
	} // abs()

	/**
	 * Integer addition.  Returns the summation of all arguments.  Each argument
	 * is converted to an <b>int</b> type.  An empty or null array <b>arg</b> evalutes to 0.
	 * Any null arguments are ignored.
	 */
	public static int add(Object[] args)
	{
		if (args == null || args.length == 0) return 0;
		int psize = args.length;
		if (DEBUG_MODE) ThreadState.logln("add(" + psize + " args)");
		int result = 0;
		for (int ix = 0, end = psize; ix < end; ix++)
		{
			try {
				result += _int(args[ix]);
			} catch (NullPointerException e) {
			}
		}
		return result;
	} // add()

	/**
	 * Add anchor tags around text that looks like a hyperlink or an email addresses.
	 * Called from html().
	 * @see html()
	 */
	private static StringBuilder addAnchors(StringBuilder buf)
	{
		char c;
		boolean inTag = false, mailto = false;
		for (int ix = 0, end = buf.length(); ix < end; ix++)
		{
			c = buf.charAt(ix);
			if (c == '<' && !inTag)
			{
				inTag = true;
				if (ix + 2 < end && Character.toLowerCase(buf.charAt(ix + 1)) == 'a'
						&& buf.charAt(ix + 2) <= ' ') return buf;	// done if any anchor tags found
			}
			if (c == '>' && inTag) inTag = false;
			if (c == '.' && !inTag)
			{
				int iz = ix + 1;
				mailto = false;
				for (; iz < end; iz++)
				{
					c = buf.charAt(iz);
					if (c <= ' ' || c == '"' || c == '\'' || c == ';' || c == ','
							|| c == '<' || c == '>' || c > 126)
					{
						break;
					}
					if (iz == ix + 1 && c == '&') break;
					if (c == '@') mailto = true;
				}
				String url = buf.substring(ix, iz).toLowerCase();
				if (url.startsWith(".exe") || url.startsWith(".doc")
						|| url.startsWith(".htm") || url.startsWith(".txt")
						|| url.startsWith(".dll") || url.startsWith(".zip")
						|| url.startsWith(".gif") || url.startsWith(".jpg")
						|| url.startsWith("..."))
				{
					ix += 2;
					continue;	// these are only files
				}
//				if (!(url.startsWith("com") || url.startsWith("org") || url.startsWith("net")
//						|| url.startsWith("mil") || url.startsWith("gov"))) continue;	// only these allowed
				c = buf.charAt(iz - 1);
				while (c == '.')
				{
					iz--;       	// ignore ending periods
					if (iz <= ix) break;
					c = buf.charAt(iz - 1);
				}
				if (iz <= ix + 1) continue;	// too short
				int iy = ix - 1;
				for (; iy > 0; iy--)
				{
					c = buf.charAt(iy);
					if (c <= ' ' || c == '"' || c == '\'' || c == ';' || c == ','
							|| c == '<' || c == '>' || c > 126)
					{
						iy++;
						break;
					}
					if (c == '@') mailto = true;
				}
				if (iz < iy + 4 || iy < 0) continue;	// too short
				int ij = iy;
				for (; ij < iz; ij++)
				{
					c = buf.charAt(ij);
					if (c != '-' && c != '+' && c != '.' && c != 'e' && c != 'E'
							&& c != '$' && (c < '0' || c > '9')) break;
				}
				if (ij >= iz) continue;	// ignore floating point numbers
				url = buf.substring(iy, iz);

					// weed out things that can't be a URL
				String lower = url.toLowerCase();
				if (!mailto && lower.indexOf(".com") < 0 && lower.indexOf(".net") < 0
						&& lower.indexOf("www") < 0 && lower.indexOf(".edu") < 0
						&& lower.indexOf(".gov") < 0 && lower.indexOf(".org") < 0
						&& lower.indexOf(".web") < 0 && lower.indexOf(".htm") < 0) continue;

//System.out.println("ix =  " + ix + ", iy = " + iy + ", iz = " + iz + ", end = " + end
//		+ "\n\t'" + url + "'");
				buf.insert(iz, "</a>");
				if (mailto)
				{
					buf.insert(iy, "<a href=\"mailto:" + url + "\">");
				}
				else
				{
					if (url.toLowerCase().startsWith("http"))
					{
						buf.insert(iy, "<a href=\"" + url + "\">");
					}
					else
					{
						buf.insert(iy, "<a href=\"http://" + url + "\">");
					}
				}
				int inc = buf.length() - end;
				end += inc;
				ix = iz + inc - 1;
			}
		}
		return buf;
	} // addAnchors()

	/**
	 * Logical And.  Calculates the logical <b>and</b> of all arguments.
	 * Each argument is converted to a boolean as it is used.  An empty or null
	 * array arg evaluates to 0.  Note, this function cannot do short-circut
	 * evalation, as Java dose when you use the || operator.
	 * @see or()
	 */
	public static boolean and(Object[] args)
	{
		if (args == null || args.length == 0) return false;
		int psize = args.length;
		if (DEBUG_MODE) ThreadState.logln("and(" + psize + " args)");
		boolean result = true;
		for (int ix = 0, end = psize; result && ix < end; ix++)
		{
			try {
				result &= _boolean(args[ix]);
			} catch (NullPointerException e) {
			}
		}
		return result;
	} // and()

	static final String base64Chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/";

	/**
	 * Decode a Base64 String.  This decodes a String that has been encoded with Base64
	 * encoding, as defined in RFC 1521.
	 */
	public static String base64Decode(CharSequence data)
	{
		String result = null;
		if (data != null)
		{
			int len = data.length();
			byte[] buf = new byte[len * 3 / 4];
			int bits = 0, accum = 0, iy = 0;
			boolean trunc = false;
			for (int ix = 0; ix < len; ix++)
			{
				char c = data.charAt(ix);
				if (c == '=')
				{
					trunc = true;	// ignore the last 8 bits because the input had a non multiple of 6 bits
					break;	// If I hit '=' then I know I'm done
				}
				if (c >= 'A' && c <= 'Z')
				{
					accum = (accum << 6) | (c - 'A');
				}
				else
				if (c >= 'a' && c <= 'z')
				{
					accum = (accum << 6) | (c - 'a' + 26);
				}
				else
				if (c >= '0' && c <= '9')
				{
					accum = (accum << 6) | (c - '0' + 52);
				}
				else if (c == '+')
				{
					accum = (accum << 6) | 62;
				}
				else if (c == '/')
				{
					accum = (accum << 6) | 63;
				}
				else continue;	// ignore all other characters; don't increment bits
				bits += 6;
				if (bits >= 24)
				{
					buf[iy++] = (byte)(accum >>> 16);
					buf[iy++] = (byte)(accum >>> 8);
					buf[iy++] = (byte)accum;
					bits = 0;
					accum = 0;
				}
			}
			if (bits > 0)
			{
				int bitsOver = bits;
				while (bitsOver < 24)
				{
					accum <<= 6;
					bitsOver += 6;
				}
				if (trunc) bits -= 8;	// ignore the last byte since it will be 0
				while (bits > 0)
				{
					buf[iy++] = (byte)(accum >>> 16);
					accum <<= 8;
					bits -= 8;
				}
			}
			try { result = new String(buf, 0, iy, "UTF-8"); } catch (java.io.UnsupportedEncodingException e) {}
		}
		if (DEBUG_MODE) ThreadState.logln("base64Decode(" + data + ") -> " + result);
		return result;
	} // base64Decode()

	/**
	 * Encode a Base64 String.  This encodes a String using Base64 encoding, as defined in
	 * RFC 1521.
	 */
	public static String base64Encode(CharSequence data)
	{
		String result;
		if (data == null) result = null;
		else
		{
			byte[] utf8 = null;
			try { utf8 = data.toString().getBytes("UTF-8"); } catch (java.io.UnsupportedEncodingException e) {}
			int len = utf8.length;
			StringBuilder buf = new StringBuilder(len * 5 / 3);
			int bits = 0, accum = 0, line = 0;
			for (int ix = 0; ix < len; ix++)
			{
				accum = (accum << 8) | (utf8[ix] & 0xff);	// always positive
				bits += 8;
				if (bits >= 24)
				{
					buf.append(base64Chars.charAt(accum >>> 18));
					buf.append(base64Chars.charAt((accum >>> 12) & 0x3f));
					buf.append(base64Chars.charAt((accum >>> 6) & 0x3f));
					buf.append(base64Chars.charAt(accum & 0x3f));
					bits = 0;
					accum = 0;
					line += 4;
					if (line >= 76)
					{
						buf.append("\r\n");
						line = 0;
					}
				}
			}
			if (bits > 0)
			{
				int bitsOver = bits;
				while (bitsOver < 24)
				{
					accum <<= 8;
					bitsOver += 8;
				}
				buf.append(base64Chars.charAt(accum >>> 18));
				bits -= 6;
				buf.append(base64Chars.charAt((accum >>> 12) & 0x3f));
				bits -= 6;
				if (bits > 0)
				{
					buf.append(base64Chars.charAt((accum >>> 6) & 0x3f));
				}
				else
				{
					buf.append('=');
				}
				buf.append('=');
			}
			result = buf.toString();
		}
		if (DEBUG_MODE) ThreadState.logln("base64Encode(" + data + ") -> " + result);
		return result;
	} // base64Encode()

	/**
	 * Bit-wise And.  Calculates the bit-wise <b>and</b> of all arguments.
	 * Each argument is converted to an int as it is used.  An empty or null
	 * array evaluates to 0.
	 */
	public static int bitAnd(Object[] args)
	{
		if (args == null || args.length == 0) return 0;
		int psize = args.length;
		int result = -1;
		for (int ix = 0, end = psize; ix < end; ix++)
		{
			try {
				result &= _int(args[ix]);
			} catch (NullPointerException e) {
			}
		}
		if (DEBUG_MODE) ThreadState.logln("bitAnd(" + psize + " args) -> " + result);
		return result;
	} // bitAnd()

	/**
	 * Bit-wise Or.  Calculates the bit-wise <b>or</b> of all arguments.
	 * Each argument is converted to an int as it is used.  An empty or null
	 * array <b>arg</b> evaluates to 0.
	 */
	public static int bitOr(Object[] args)
	{
		if (args == null || args.length == 0) return 0;
		int psize = args.length;
		int result = 0;
		for (int ix = 0, end = psize; ix < end; ix++)
		{
			try {
				result |= _int(args[ix]);
			} catch (NullPointerException e) {
			}
		}
		if (DEBUG_MODE) ThreadState.logln("bitOr(" + psize + " args) -> " + result);
		return result;
	} // bitOr()

	/**
	 * Bit-wise Exclusive-Or.  Calculates the bit-wise <b>exclusive or</b> of all arguments.
	 * Each argument is converted to an int as it is used.  An empty or null
	 * array <b>arg</b> evaluates to 0.
	 */
	public static int bitXor(Object[] args)
	{
		if (args == null || args.length == 0) return 0;
		int psize = args.length;
		int result = 0;
		for (int ix = 0, end = psize; ix < end; ix++)
		{
			try {
				result ^= _int(args[ix]);
			} catch (NullPointerException e) {
			}
		}
		if (DEBUG_MODE) ThreadState.logln("bitXor(" + psize + " args) -> " + result);
		return result;
	} // bitXor()

	/**
	 * Concatenates Strings.  This simply concatenates the arguments and returns the String.
	 * Each argument is converted to a String as it is used.  Null arguments convert to the empty
	 * String "".  An empty or null array <b>arg</b> evaluates to the empty string "".
	 */
	public static String concat(Object[] args)
	{
		StringBuilder result = new StringBuilder();
		int psize = 0;
		if (args != null)
		{
			psize = args.length;
			for (int ix = 0; ix < psize; ix++)
			{
				String s = _String(args[ix]);
				if (s != null)
				{
//					s = replace(s, "<br>", "\r\n");
					result.append(s);
				}
			}
		}
		if (DEBUG_MODE) ThreadState.logln("concat(" + psize + " args) -> " + result);
		return result.toString();
	} // concat()

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
	 * Returns an integer that has it's bits scrambled.  If the same resulting value is again sent
	 * through this function the original value will be returned.
	 */
	public static int descramble(int value, String key)
	{
		byte[] shifts = new byte[32];
		for (int ix = 0; ix < 32; ix++)
		{
			shifts[ix] = (byte)ix;
		}
		int hash = key.hashCode();
		Random rand = new Random((long)hash << 32 | hash);
		for (int ix = 0; ix < 32; ix++)
		{
			byte temp = shifts[ix];
			int r = (int)(rand.nextFloat() * 32);
			shifts[ix] = shifts[r];
			shifts[r] = temp;
		}
//System.out.println("descramble [");
//for (int ix = 0; ix < 32; ix++)
//{
//	if (ix > 0) System.out.print(", ");
//	System.out.print(shifts[ix]);
//}
//System.out.println("]");
		int result = 0;
		for (int ix = 0; ix < 32; ix++)
		{
			int shift = shifts[ix];
			result |= ((value >>> shift) & 1) << ix;
		}
		result ^= hash;
//System.out.println("value " + Integer.toHexString(value) + ", hash " + Integer.toHexString(value) + ", result " + Integer.toHexString(result));
		if (DEBUG_MODE) ThreadState.logln("descramble(" + value + ", " + key + ") -> " + result);
		return result;
	} // descramble()

	/**
	 * Integer division.  Returns the first argument divided by each of the reast.  Each argument
	 * is converted to an <b>int</b> type.  An empty or null array <b>arg</b> evalutes to 0.
	 * Any null arguments are ignored.
	 */
	public static int div(Object[] args)
	{
		if (args == null || args.length == 0) return 0;
		int psize = args.length;
		if (DEBUG_MODE) ThreadState.logln("div(" + psize + " args)");
		int result = 1;
		try {
			result = _int(args[0]);
		} catch (NullPointerException e) {
		}
		for (int ix = 1, end = psize; ix < end; ix++)
		{
			try {
				result /= _int(args[ix]);
			} catch (NullPointerException e) {
			}
		}
		return result;
	} // div()

	/**
	 * Integer division with round-off.  This works the similar to div(), except that
	 * when the resulting fractional part is 1/2 or greater the next higher integer (or lower
	 * in case of a negative result) is returned.  Also, this only works with two arguments.
	 * @see div(Object[])
	 * @see divRoundUp(int, int)
	 */
	public static int divRoundOff(int i1, int i2) throws IllegalArgumentException
	{
		int result;
		if (i2 == 0) throw new IllegalArgumentException("Cannot divide by 0.");
		if ((i1 >= 0 && i2 > 0) || (i1 < 0 && i2 < 0)) result = (i1 + (i2 >> 1)) / i2;
		else result = (i1 + (i2 >> 1)) / i2;
		return result;
	} // divRoundOff()

	/**
	 * Integer division with round-up.  This works the similar to div(), except that
	 * when their would be any remainder the next higher integer (or lower
	 * in case of a negative result) is returned.  Also, this only works with two arguments.
	 * @see div(Object[])
	 * @see divRoundOff(int, int)
	 */
	public static int divRoundUp(int i1, int i2) throws IllegalArgumentException
	{
		int result;
		if (i2 == 0) throw new IllegalArgumentException("Cannot divide by 0.");
		if ((i1 >= 0 && i2 > 0) || (i1 < 0 && i2 < 0)) result = (i1 + i2 - 1) / i2;
		else result = (i1 + i2 - 1) / i2;
		return result;
	} // divRoundUp()

//	public void doGet(HttpServletRequest request, HttpServletResponse response)
//			throws ServletException, IOException
//	{
//		_jspService(request, response);
//	} // doGet()

//	public void doPost(HttpServletRequest request, HttpServletResponse response)
//			throws ServletException, IOException
//	{
//		_jspService(request, response);
//	} // doPost()

	/**
	 * Returns true if arg1 and arg2 are equals, false otherwise.  Arg2 is converted to
	 * the same time as arg1, if the types to not match.  If arg2 can't be converted the
	 * result is false.  Case is ignored when comparing Strings.
	 */
	public boolean eq(Object arg1, Object arg2) //throws Exception
	{
		boolean result = false;
		if (arg1 == null && arg2 == null) result = true;
		else
		if (arg1 != null && arg2 != null)
		{
			try {
				if (arg1 instanceof String && ((String)arg1).equalsIgnoreCase(_String(arg2))) result = true;
				else
				if (arg1 instanceof Date && arg1.equals(_Date(arg2))) result = true;
				else
				if (arg1 instanceof Time && arg1.equals(_Time(arg2))) result = true;
				else
				if (arg1 instanceof Boolean && arg1.equals(_Boolean(arg2))) result = true;
				else
				if (arg1 instanceof Double && arg1.equals(_Double(arg2))) result = true;
				else
				if (arg1 instanceof Integer && arg1.equals(_Integer(arg2))) result = true;
				else
				if (arg1 instanceof Number && _Double(arg1).equals(_Double(arg2))) result = true;
				else
				if (_String(arg1).equalsIgnoreCase(_String(arg2))) result = true;
			} catch (Exception e) {
			}
		}
		if (DEBUG_MODE) ThreadState.logln("eq(" + arg1 + ", " + arg2 + ") -> " + result);
		return result;
	} // eq()

	/**
	 * Make object ready for use in an SQL where clause.  Outputs an '=' if value is set
	 * or 'is' if not, then converts the value to an Integer and outputs the value.
	 * @see quote(CharSequence)
	 */
	public static String eqInteger(Object value)
	{
		return (value == null || (value instanceof CharSequence && ((CharSequence) value).length() == 0)
				? "is " : "= ") + sqlInteger(value);
	} // eqInteger()

	/**
	 * Make String ready for use in an SQL where clause.  Outputs an '=' if value is set
	 * or 'is' if not, then quotes and outputs the value.
	 * @see quote(CharSequence)
	 */
	public static String eqQuote(CharSequence value)
	{
		return (value == null || value.length() == 0 ? " is " : " = ") + quote(value);
	} // eqQuote()

	/**
	 * Make object ready for use in an SQL where clause.  Outputs an '=' if value is set
	 * or 'is' if not, then quotes and outputs the value.
	 * @see quote(CharSequence)
	 */
	public static String eqSql(Object value)
	{
		if (value == null || value instanceof CharSequence) return eqQuote((CharSequence)value);
		if (value instanceof Object[] || value instanceof String[]
				|| value instanceof Number[] || value instanceof Collection) return " in " + sql(value);
		return " = " + sql(value);
	} // eqSql()

	/**
	 * Execute a statement.  The first word specifies the type of statement: "array", "dir", "scan", or "sql".
	 * If none of those are found the statement is assumed to be of type "sql".  The db option to switch
	 * databases is allowed at the beginning of sql statements.
	 */
	public DspStatement execute(String typeStr, Object obj) throws DspException
	{
		int type = SQL_STMT;
		if (typeStr != null && typeStr.length() > 0)
		{
			if (typeStr.equals("scan"))
			{
				type = SCAN_STMT;
			}
			else
			if (typeStr.equals("array"))
			{
				type = ARRAY_STMT;
			}
			else
			if (typeStr.equals("dir"))
			{
				type = DIR_STMT;
			}
			else
			if (!typeStr.equals("sql")) throw new IllegalArgumentException("Invalid statement type: " + typeStr);
		}
		return execute("_t999", type, obj);
	} // execute()

	/**
	 * Execute a statement.  Used by DspDo, DspIf, DspSql, and DspWhile.
	 */
	protected DspStatement execute(String name, int type, Object obj)
			throws DspException
	{
		DspStatement state = makeStatement(name, type);
		state.execute(obj);
		return state;
	} // execute()

	/**
	 * Execute a statement and return the first column of the first row if statement produces
	 * a result set.  Otherwise, return the number of rows modified.  The first word specifies
	 * the type of statement: "array", "dir", "scan", or "sql".  If none of those are found the
	 * statement is assumed to be of type "sql".  The db option to switch databases is allowed
	 * at the beginning of sql statements.
	 */
	public Object executeGet1st(String typeStr, Object obj) throws DspException
	{
		int type = SQL_STMT;
		if (typeStr != null && typeStr.length() > 0)
		{
			if (typeStr.equals("scan"))
			{
				type = SCAN_STMT;
			}
			else
			if (typeStr.equals("array"))
			{
				type = ARRAY_STMT;
			}
			else
			if (typeStr.equals("dir"))
			{
				type = DIR_STMT;
			}
			else
			if (!typeStr.equals("sql")) throw new IllegalArgumentException("Invalid statement type: " + typeStr);
		}
		return executeGet1st("_t998", type, obj);
	} // executeGet1st()

	/**
	 * Execute a statement and return the first column of the first row if statement produces
	 * a result set.  Otherwise, return the number of rows modified.  Used by DspSet and DspDefault.
	 */
	protected Object executeGet1st(String name, int type, Object obj)
			throws DspException
	{
		DspStatement state = makeStatement(name, type);
		try {
			state.execute(obj);
			if (state.hasResults())
			{
				state.next();
				return state.getObject(1);
			}
			int result = state.getResult();
			if (result == 0 || result == Integer.MIN_VALUE) return null;
			return new Integer(result);
    } finally {
      state.close();
		}
	} // executeGet1st()

	/**
	 * Real number absolute value.  Returns the absolute value of <b>arg</b>.
	 */
	public static double fabs(double arg)
	{
		return arg < 0.0 ? -arg : arg;
	} // fabs()

	/**
	 * Real numebr addition.  Returns the summation of all arguments.  Each argument
	 * is converted to an <b>int</b> type.  An empty or null array <b>arg</b> evalutes to 0.
	 * Any null arguments are ignored.
	 */
	public static double fadd(Object[] args)
	{
		if (args == null || args.length == 0) return 0.0;
		int psize = args.length;
		if (DEBUG_MODE) ThreadState.logln("fadd(" + psize + " args)");
		double result = 0.0;
		for (int ix = 0, end = psize; ix < end; ix++)
		{
			try {
				result += _double(args[ix]);
			} catch (NullPointerException e) {
			}
		}
		return result;
	} // fadd()

	/**
	 * Real number division.  Returns the first argument divided by each of the reast.  Each argument
	 * is converted to an <b>int</b> type.  An empty or null array <b>arg</b> evalutes to 0.
	 * Any null arguments are ignored.
	 */
	public static double fdiv(Object[] args)
	{
		if (args == null || args.length == 0) return 0.0;
		int psize = args.length;
		if (DEBUG_MODE) ThreadState.logln("fdiv(" + psize + " args)");
		double result = 1.0;
		try {
			result = _double(args[0]);
		} catch (NullPointerException e) {
		}
		for (int ix = 1, end = psize; ix < end; ix++)
		{
			try {
				result /= _double(args[ix]);
			} catch (NullPointerException e) {
			}
		}
		return result;
	} // div()

	/**
	 * Returns the maximum (most positive) of the double arguments.
	 * Any null arguments are ignored.
	 */
	public static double fmax(Object[] args)
	{
		int psize;
		if (args == null || ((psize = args.length) == 0)) return 0.0;
		double result = 0.0;
		try {
			result = _double(args[0]);
		} catch (NullPointerException e) {
		}
		for (int ix = 1; ix < psize; ix++)
		{
			try {
				double temp = _double(args[ix]);
				if (temp > result) result = temp;
			} catch (NullPointerException e) {
			}
		}
		return result;
	} // fmax()

	/**
	 * Returns the minimum (most negative) of the double arguments.
	 * Any null arguments are ignored.
	 */
	public static double fmin(Object[] args)
	{
		int psize;
		if (args == null || ((psize = args.length) == 0)) return 0.0;
		double result = 0.0;
		try {
			result = _double(args[0]);
		} catch (NullPointerException e) {
		}
		for (int ix = 1; ix < psize; ix++)
		{
			try {
				double temp = _double(args[ix]);
				if (temp < result) result = temp;
			} catch (NullPointerException e) {
			}
		}
		return result;
	} // fmin()

	/**
	 * Real number multiplication.  Returns the product of all arguments.  Each argument
	 * is converted to an <b>int</b> type.  An empty or null array <b>arg</b> evalutes to 0.
	 * Any null arguments are ignored.
	 */
	public static double fmul(Object[] args)
	{
		if (args == null || args.length == 0) return 0.0;
		int psize = args.length;
		if (DEBUG_MODE) ThreadState.logln("fmul(" + psize + " args)");
		double result = 1.0;
		try {
			result = _double(args[0]);
		} catch (NullPointerException e) {
		}
		for (int ix = 1, end = psize; ix < end; ix++)
		{
			try {
				result *= _double(args[ix]);
			} catch (NullPointerException e) {
			}
		}
		return result;
	} // fmul()

	/**
	 * Real number negation.  Returns the negative of the argument.
	 */
	public static double fneg(double arg)
	{
		double result = -arg;
		if (DEBUG_MODE) ThreadState.logln("fneg(" + arg + ") => " + result);
		return result;
	} // fneg()

	/**
	 * Formats the value as a percentage, and returns the String.
	 */
	public static String formatPercent(double value)
	{
		return percent.format(value);
	} // formatPercent()

	/**
	 * Formats raw phone numbers both with and without area codes as follows:
	 * (aaa) eee-nnnn or eee-nnnn
	 */
	public static String formatPhone(String phone)
	{
		String phoneOrig = phone;
		if (phone == null || phone.length() == 0) return phone;
		phone = stripNonNumerics(phone.trim());
		if (phone.length() == 0) return phone;
		String prefix = "";
		char c;
		while ((c = phone.charAt(0)) == '0' || c == '1') {
			prefix += c;
			phone = phone.substring(1);
			if (phone.length() == 0) break;
		}
		if (prefix.length() > 0) prefix += ' ';
		int len = phone.length();
		if (len == 7) return prefix + phone.substring(0, 3) + '-' + phone.substring(3);
		if (len == 10) return prefix + '(' + phone.substring(0, 3) + ") " + phone.substring(3, 6)
				+ '-' + phone.substring(6);
		return phoneOrig;
	} // formatPhone()

	/**
	 * Convert from HTML to Unicode text.  This function converts many of the encoded HTML
	 * characters to normal Unicode text.  Example: &amp;lt&semi; to &lt;.  This is the opposite
	 * of showHtml().
	 * @see showHtml(CharSequence)
	 */
	public static String fromHtml(CharSequence text)
	{
		if (text == null) return null;
		int ixz;
		String string = text.toString();
		if ((ixz = text.length()) == 0) return string;
		StringBuilder buf = new StringBuilder(ixz);
		String rep = null;
		for (int ix = 0; ix < ixz; ix++)
		{
			char c = text.charAt(ix);
			if (c == '&');
			{
				String sub = string.substring(ix + 1).toLowerCase();
				if (sub.startsWith("lt;"))
				{
					c = '<';
					ix += 3;
				}
				else
				if (sub.startsWith("gt;"))
				{
					c = '>';
					ix += 3;
				}
				else
				if (sub.startsWith("amp;"))
				{
					c = '&';
					ix += 4;
				}
				else
				if (sub.startsWith("nbsp;"))
				{
					c = ' ';
					ix += 5;
				}
				else
				if (sub.startsWith("semi;"))
				{
					c = ';';
					ix += 5;
				}
				else
				if (sub.startsWith("#"))
				{
					char c2 = 0;
					for (int iy = ix + 1; iy < ixz; iy++)
					{
						char c1 = text.charAt(iy);
						if (c1 >= '0' && c1 <= '9')
						{
							c2 = (char)(c2 * 10 + c1);
							continue;
						}
						if (c1 == ';')
						{
							c = c2;
							ix = iy;
						}
						break;
					}
				} else if (c == '<' && ix + 3 <= ixz && Character.toLowerCase(text.charAt(ix + 1)) == 'b'
					&& Character.toLowerCase(text.charAt(ix + 2)) == 'r' && text.charAt(ix + 3) == '>') {
					c = '\n';
				}
			}
			if (rep != null)
			{
				buf.append(rep);
				rep = null;
			}
			else buf.append(c);
		}
		return buf.toString();
	} // fromHtml()

	/**
	 * Real number subtraction.  Each argument is subtracted from the first.  If no arguments
	 * are submitted then the result is 0.
	 * Any null arguments are ignored.
	 */
	public static double fsub(Object[] args)
	{
		if (args == null || args.length == 0) return 0.0;
		int psize = args.length;
		if (DEBUG_MODE) ThreadState.logln("fsub(" + psize + " args)");
		double result = 0.0;
		try {
			result = _double(args[0]);
		} catch (NullPointerException e) {
		}
		for (int ix = 1, end = psize; ix < end; ix++)
		{
			try {
				result -= _double(args[ix]);
			} catch (NullPointerException e) {
			}
		}
		return result;
	} // fsub()

	/**
	 * Returns the sign of the argument as a real number: -1.0 if negative, 0.0 if 0.0, and
	 * 1.0 if positive.
	 */
	public static double fsgn(double arg)
	{
		if (arg < 0.0) return -1.0;
		else
		if (arg > 0.0) return 1.0;
		else return 0.0;
	} // fsgn()

	/**
	 * Returns true if arg1 >= arg2, false otherwise.
	 */
	public static boolean ge(double arg1, double arg2)
	{
		return arg1 >= arg2;
	} // ge()

	/**
	 * Returns a new object instance.
	 */
	public static Object getBean(String className) throws DspException
	{
		try {
			return Class.forName(className).newInstance();
		} catch (Exception e) {
			throw new DspException("Couldn't get bean " + className, e);
		}
	} // getBean()

	/**
	 * Returns the value of the named member of the object.  If the object is a DspObject
	 * get() is called.  Otherwise, it uses reflection to look up and get the field.
	 */
	public static Object getMember(Object obj, String member) throws DspException
	{
		try {
			return ((DspObject)obj).get(member);
		} catch (ClassCastException e) {
			Class<?> c = obj.getClass();
			try {
				Field f = c.getField(member);
	//			if (Modifier.isPublic(f.getModifiers())
	//			{
					return f.get(obj);
	//			}
			} catch (NoSuchFieldException e1) {
				throw new DspException("Could not find field '" + member + '\'', e1);
			} catch (IllegalAccessException e1) {
				throw new DspException("Could not access field '" + member + '\'', e1);
			}
		}
	} // getMember()

	/**
	 * Returns the result of calling a named method of the object, passing args as the arguments
	 * of the function.  If the object is a DspObject run() is called.  Otherwise, it uses
	 * reflection to look up and call the method.
	 */
	public static Object getMember(Object obj, String member, Object[] args) throws DspException
	{
//		try {
//			return ((DspObject) obj).run(member, args);
//		} catch (ClassCastException e) {
			Class<?> c = obj.getClass();
			Class<?>[] types = null;
			int len;
			if (args != null && (len = args.length) > 0)
			{
				types = new Class[len];
				for (int ix = 0; ix < len; ix++)
				{
					if (args[ix] != null)
					{
						types[ix] = args[ix].getClass();
					}
				}
			}
			try {
				int close = 0;
				Method[] meths = c.getMethods();
				Method found = null;
//int choice = -1;
				for (int ix = 0, ixz = meths.length; ix < ixz; ix++)
				{
					Method meth = meths[ix];
					if (Modifier.isPublic(meth.getModifiers()) && meth.getName().equals(member))
					{
						Class<?>[] pTypes = meth.getParameterTypes();
						if ((pTypes.length == 0 && args == null) || (args != null && pTypes.length == args.length))
						{
							int close1 = 1;
							if (args != null)
							{
								for (int iy = 0, iyz = pTypes.length; iy < iyz; iy++)
								{
									if (pTypes[iy] == types[iy])
									{
										close1 += 2;
//ThreadState.logln(ix + " - " + pTypes[iy]);
									}
									else
									if (pTypes[iy].isAssignableFrom(types[iy])) close1++;
								} // for iy
							} // if args
							if (close1 > close)
							{
//ThreadState.logln(ix + " got " + close1 + " points");
								found = meth;
//								choice = ix;
								close = close1;
							}
						} // if name matches
					} // for
				}
				if (found == null)
				{
					throw new DspException("Could not find method " + member + " in " + obj);
				}
				else
				{
//ThreadState.logln("Choice " + choice);
					return found.invoke(obj, args);
				}
//			} catch (NoSuchMethodException e1) {
//				throw new DspException("Could not find method " + member + " in " + obj, e1);
			} catch (IllegalAccessException e1) {
				throw new DspException("Could not invoke method " + member + " in " + obj, e1);
			} catch (InvocationTargetException e1) {
				throw new DspException("Exception in method " + member + " in " + obj, e1.getTargetException());
			} catch (Throwable e1) {
				throw new DspException("Exception in method " + member + " in " + obj, e1);
			}
//		}
	} // getMember(args)

	/**
	 * Returns an instantiation of a tag extenion object.
	 */
	public static Tag getTag(DspObject temp, String prefix, String action, String className) throws DspException
	{
		Tag tag = (Tag)temp.get('_' + prefix + '_' + action);
		if (tag == null)
		{
			try {
				tag = (Tag)Class.forName(className).newInstance();
			} catch (Exception e) {
				throw new DspException("Could not create " + className);
			}
		}
		return tag;
	} // getTag()

//	abstract public long getId();
	/**
	 * Returns the prop object belonging to this group or pages.
	 */
	public DspProp getProp() { return prop; }

	/**
	 * Returns true if arg1 > arg2.
	 */
	public static boolean gt(double arg1, double arg2)
	{
		boolean result = arg1 > arg2;
		if (DEBUG_MODE) ThreadState.logln("gt(" + arg1 + ", " + arg2 + ") => " + result);
		return result;
	} // gt()

	/**
	 * Converts the String to HTML format.  It looks for certain characters and converts them
	 * to the proper HTML tags.  If the String already contains HTML tags, these will be left
	 * intact.  You will probably use this function heavily, unless you store HTML directly in
	 * your database.  The following table lists the conversions that take place within this function:
	 * <table>
	 *   <tr><td>From</td><td>To</td><td>Conditions and Comments</td></tr>
	 *   <tr><td>\r, \n, or \r\n</td><td>&ltbr&gt</td><td>&nbsp;</td></tr>
	 *   <tr><td>\t</td><td>&amp;nbsp&semi;&amp;nbsp&semi;&amp;nbsp&semi;&amp;nbsp&semi;</td><td>&nbsp;</td></tr>
	 *   <tr><td>&lt;space&gt;&lt;space&gt;...</td><td>&amp;nbsp&semi;...&lt;space&gt;</td><td>&lt;space&gt; means a space character</td></tr>
	 *   <tr><td>&amp;</td><td>&amp;amp&semi;</td><td>&nbsp;</td></tr>
	 *   <tr><td>&semi;</td><td>&amp;semi&semi;</td><td>&nbsp</td></td>
	 *   <tr><td>*</td><td>&lt;ul&gt&lt;li&gt;</td><td>Unordered list&semi; must begin a line</td></tr>
	 *   <tr><td>**...</td><td>&lt;ul&gt&lt;ul&gt;...&lt;li&gt;</td><td>Higher level lists&semi; must begin a line</td></tr>
	 *   <tr><td>#</td><td>&lt;ol&gt&lt;li&gt;</td><td>Ordered list&semi; must begin a line</td></tr>
	 *   <tr><td>##...</td><td>&lt;ol&gt&lt;ol&gt...&lt;li&gt;</td><td>Higher level lists&semi; must begin a line</td></tr>
	 *   <tr><td>^</td><td>&lt;blockquote&gt;</td><td>Indented block&semi; must begin a line</td></tr>
	 *   <tr><td>^^...</td><td>&lt;blockquote&gt<br>&lt;blockquote&gt...</td><td>Higher level indents&semi; must begin a line</td></tr>
	 *   <tr><td>(c) or (C)</td><td>&amp;copy&semi;</td><td>&copy;</td></tr>
	 *   <tr><td>(r) or (R)</td><td>&amp;reg&semi;</td><td>&reg;</td></tr>
	 *   <tr><td>(tm) or (TM)</td><td>&lt;small&gt;&lt;sub&gt;TM&lt;/small&gt;&lt;/sub&gt;</td><td><small><sup>TM</small></sup></td></tr>
	 *   <tr><td>(sm) or (SM)</td><td>&lt;small&gt;&lt;sub&gt;SM&lt;/small&gt;&lt;/sub&gt;</td><td><small><sup>SM</small></sup></td></tr>
	 *	 <tr><td>{</td><td>&amp;#123&semi;</td><td>To prevent DSP from parsing published pages</td></tr>
	 *	 <tr><td>}</td><td>&amp;#125&semi;</td><td>same as above</td></tr>
	 * </table>
	 */
	public static String html(CharSequence text)
	{
		if (text == null) return null;
		String org = text.toString();
		String result;
		if (text == null || isHtml(text)) result = org;
		else
		{
			int ix;

			StringBuilder buf = new StringBuilder(text);
			buf.ensureCapacity(text.length() * 5 / 4);
			addAnchors(buf);
			htmlCharsBuf(buf);
			text = buf.toString();
			int end = text.length();
			buf.setLength(0);
			int level0 = 0, level = 0, tab0 = 0, tab = 0;
			char c1 = '\n', c;
			boolean bar = false;
			for (ix = 0; ix <= end; ix++)
			{
				c = c1;
				c1 = ix < end ? text.charAt(ix) : (char)-1;
				String app = null;
				switch (c)
				{
					case '\r':
						if (c1 == '\n')
						{
							c = c1;
							c1 = ++ix < end ? text.charAt(ix) : (char)-1;
						}
					case '\n':
						level = tab = 0;
						if (c1 == '^')
						{
							while (c1 == '^')
							{
								c = c1;
								c1 = ++ix < end ? text.charAt(ix) : (char)-1;
								tab++;
							}
						}
						if (c1 == '|')
						{
							bar = true;
							while (c1 == '|')
							{
								c = c1;
								c1 = ++ix < end ? text.charAt(ix) : (char)-1;
								tab++;
							}
						}
						if (c1 == '*')
						{
							while (c1 == '*')
							{
								c = c1;
								c1 = ++ix < end ? text.charAt(ix) : (char)-1;
								level++;
							}
						}
						else
						if (c1 == '#')
						{
							while (c1 == '#')
							{
								c = c1;
								c1 = ++ix < end ? text.charAt(ix) : (char)-1;
								level--;
							}
						}
	//System.out.println("level " + level0 + " -> " + level);
	//System.out.println("tab " + tab0 + " -> " + tab);
						while (level0 > level && level0 > 0)
						{
							if (app != null) buf.append(app);
							app = "</ul>";
							level0--;
						}
						while (level0 < level && level0 < 0)
						{
							if (app != null) buf.append(app);
							app = "</ol>";
							level0++;
						}
						boolean br = ix > 0;
						while (tab0 > tab)
						{
							br = false;
							if (app != null) buf.append(app);
							app = "</blockquote>";
							tab0--;
						}
						while (tab0 < tab)
						{
							br = false;
							if (app != null) buf.append(app);
							if (bar) app = "<blockquote dir=ltr style=\"PADDING-RIGHT: 0px; PADDING-LEFT: 5px; MARGIN-LEFT: 5px; BORDER-LEFT: #000000 2px solid; MARGIN-RIGHT: 0px\">";
							else app = "<blockquote>";
							tab0++;
						}
						bar = false;
						while (level0 < level)
						{
							if (app != null) buf.append(app);
							app = "<ul>";
							level0++;
						}
						while (level0 > level)
						{
							if (app != null) buf.append(app);
							app = "<ol>";
							level0--;
						}
						if (level != 0)
						{
							if (app != null) buf.append(app);
							app = "<li>";
						}
						else if (br)
						{
							if (app != null) buf.append(app);
							app = "<br>";
						}
						break;
					case '\t':
						app = "&nbsp;&nbsp;&nbsp;&nbsp;";
						break;
					case ' ':
						if (c1 == ' ') app = "&nbsp;";
						break;
				}
				if (app != null) buf.append(app);
				else if (c > 0) buf.append(c);
			}
			while (level0 != 0)
			{
				if (level0 < 0)
				{
					buf.append("</ol>");
					level0++;
				}
				else
				{
					buf.append("</ul>");
					level0--;
				}
			}
			result = buf.toString();
		}
		if (DEBUG_MODE) ThreadState.logln("html(" + org + ") -> " + result);
		return result;
	} // html()

	/**
	 * Converts the String to HTML format.  It looks for certain characters and converts them
	 * to the proper HTML tags.  If the String already contains HTML tags, these will be left
	 * intact.  You will probably use this function heavily, unless you store HTML directly in
	 * your database.  The following table lists the conversions that take place within this function:
	 * <table>
	 *   <tr><td>From</td><td>To</td><td>Conditions and Comments</td></tr>
	 *   <tr><td>(c) or (C)</td><td>&amp;copy&semi;</td><td>&copy;</td></tr>
	 *   <tr><td>(r) or (R)</td><td>&amp;reg&semi;</td><td>&reg;</td></tr>
	 *   <tr><td>(tm) or (TM)</td><td>&lt;small&gt;&lt;sub&gt;TM&lt;/small&gt;&lt;/sub&gt;</td><td><small><sup>TM</small></sup></td></tr>
	 *   <tr><td>(sm) or (SM)</td><td>&lt;small&gt;&lt;sub&gt;SM&lt;/small&gt;&lt;/sub&gt;</td><td><small><sup>SM</small></sup></td></tr>
	 *	 <tr><td>{</td><td>&amp;#123&semi;</td><td>To prevent DSP from parsing published pages</td></tr>
	 *	 <tr><td>}</td><td>&amp;#125&semi;</td><td>same as above</td></tr>
	 * </table>
	 */
	public static String htmlChars(CharSequence text)
	{
		StringBuilder buf = new StringBuilder(text);
		buf.ensureCapacity(text.length() * 5 / 4);
		htmlCharsBuf(buf);
		String result = buf.toString();
		if (DEBUG_MODE) ThreadState.logln("htmlChars(" + text + ") -> " + result);
		return result;
	} // htmlChars()

	private static void htmlCharsBuf(StringBuilder buf)
	{
		replaceBuf(buf, "(TM)", "<small><small><sup>TM</sup></small></small>");
		replaceBuf(buf, "(tm)", "<small><small><sup>TM</sup></small></small>");
		replaceBuf(buf, "(SM)", "<small><small><sup>SM</sup></small></small>");
		replaceBuf(buf, "(sm)", "<small><small><sup>SM</sup></small></small>");
		replaceBuf(buf, "(R)", "&reg;");
		replaceBuf(buf, "(r)", "&reg;");
		replaceBuf(buf, "(C)", "&copy;");
		replaceBuf(buf, "(c)", "&copy;");
		if (buf != null)
		{
			for (int ix = 0, ixz = buf.length(); ix < ixz; ix++)
			{
				char c = buf.charAt(ix);
				if (c > 0x7f || c == '{' || c == '}')
				{
					String str = "&#" + (int)c + ';';
					buf.replace(ix, ix + 1, str);
					ix += str.length() - 1;
				}
			}
		}
		if (DEBUG_MODE) ThreadState.logln("htmlCharsBuf()");
	} // htmlCharsBuf()

	/**
	 * Join a sequence into a single string, separated by sep.
	 * Items that are null are skipped.
	 * @param list list of items
	 * @param sep separator
	 * @return joined string
	 */
	public static CharSequence join(Collection<?> list, String sep) {
	    StringBuilder result = new StringBuilder();
	    for (Object item : list) {
	    	if (item != null) {
		        if (result.length() > 0) result.append(sep);
		        result.append(item);
	    	}
	    }
	    return result;
	}

	/**
	 * Join a sequence into a single string, separated by sep.
	 * Items that are null are skipped.
	 * @param list list of items
	 * @param sep separator
	 * @return joined string
	 */
	public static CharSequence join(Object[] list, String sep) {
	    StringBuilder result = new StringBuilder();
	    for (Object item : list) {
	    	if (item != null) {
		        if (result.length() > 0) result.append(sep);
		        result.append(item);
	    	}
	    }
	    return result;
	}
	
	/**
	 * Binary multiplexer,  If the first argument evaluates to true then the second argument
	 * is returned. If false, the third argument is return, or null if there was no third argument.
	 */
	public static Object iff(Object[] args) throws IllegalArgumentException
	{
		int psize = args.length;
		if (!(psize == 2 || psize == 3))
				throw new IllegalArgumentException("iff() can only accept 2 or 3 parameters");
		Object param = args[0];
//		ThreadState.log("iff(" + args + ") -> ");
		if (_boolean(param))
		{
			if (DEBUG_MODE) ThreadState.logln(args[1]);
			return args[1];
		}
		else
		if (psize == 3)
		{
			if (DEBUG_MODE) ThreadState.logln(args[2]);
			return args[2];
		}
		else
		{
			if (DEBUG_MODE) ThreadState.logln("null");
			return null;
		}
	} // iff()

	/**
	 * Return the first index of a substring within the String.  This function returns the index of
	 * the second element as a String if found within the first as a String. If not found then -1 is returned.
	 * If three elements are sent, it is starting index for the search.
	 * @see lastIndexOf(Object[])
	 */
	public static int indexOf(Object[] args) throws IllegalArgumentException, NumberFormatException
	{
		int psize = args.length;
		if (!(psize == 2 || psize == 3))
				throw new IllegalArgumentException("indexOf() can only accept 2 or 3 arguments");
		String data = _String(args[0]);
		String find = _String(args[1]);
		int result;
		if (data == null || find == null) result = -1;
		else
		{
			int index = 0;
			if (psize > 2)
			{
				index = _int(args[2]);
			}
			result = data.indexOf(find, index);
		}
		if (DEBUG_MODE) ThreadState.logln("indexOf(" + args + ") -> " + result);
		return result;
	} // indexOf()

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
	 * Returns true if the object is a real number or can be converted to one.
	 */
	public static boolean isFloat(Object obj)
	{
		boolean result = false;
		try {
			result = _Float(obj) != null;
		} catch (Exception e) {
		}
		if (DEBUG_MODE) ThreadState.logln("isFloat(" + obj + ") -> " + result);
		return result;
	} // isFloat()

	/**
	 * Returns true if the String begins and ends with HTML tags.
	 */
	public static boolean isHtml(CharSequence text)
	{
		boolean result = false;
		if (text != null)
		{
			String temp = text.toString().trim();
			int end = temp.length();
			if (end >= 3 && temp.charAt(0) == '<'
					&& temp.charAt(end - 1) == '>') result = true;
		}
		if (DEBUG_MODE) ThreadState.logln("isHtml(" + text + ") -> " + result);
		return result;
	} // isHtml()

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
	 * Returns true if the object is a integral number or can be converted to one.
	 */
	public static boolean isInt(Object obj)
	{
		boolean result = false;
		try {
			result = _Integer(obj) != null;
		} catch (Exception e) {
		}
		if (DEBUG_MODE) ThreadState.logln("isInt(" + obj + ") -> " + result);
		return result;
	} // isInt()

	/**
	 * Same as isInt(Object)
	 */
	public static boolean isInteger(Object obj)
	{
		return isInt(obj);
	} // isInteger()

	/**
	 * Returns true if the object is a null.
	 */
	public static boolean isNull(Object obj)
	{
		boolean result = (obj == null) || (obj instanceof DspNull);
		if (DEBUG_MODE) ThreadState.logln("isNull(" + obj + ") -> " + result);
		return result;
	} // isNull()

	/**
	 * Returns true if obj is not null or if obj is a String returns true if it is not empty.
	 */
	public static boolean isSet(Object obj)
	{
		if (obj == null) return false;
		String str = _String(obj);
		return str.length() > 0;
	} // isSet()

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

	public void jspInit() {}
	public void jspDestroy() {}

	/**
	 * Return the last index of a substring within the String.  This function returns the index of
	 * the second element as a String if found within the first as a String. If not found then -1 is returned.
	 * If three elements are sent, it is starting index for the search.
	 * @see indexOf(Object[])
	 */
	public static int lastIndexOf(Object[] args) throws IllegalArgumentException, NumberFormatException
	{
		int psize = args.length;
		if (!(psize == 2 || psize == 3))
				throw new IllegalArgumentException("lastIndexOf() can only accept 2 or 3 arguments");
		String data = _String(args[0]);
		String find = _String(args[1]);
		int result;
		if (data == null || find == null) result = -1;
		else
		{
			int index = data.length();
			if (psize > 2)
			{
				index = _int(args[2]);
			}
			result = data.lastIndexOf(find, index);
		}
		if (DEBUG_MODE) ThreadState.logln("lastIndexOf(" + args + ") -> " + result);
		return result;
	} // lastIndexOf()

	/**
	 * Returns true if the first argument is less than or equal to the second, comparison
	 * is done with real numbers.
	 */
	public static boolean le(double arg1, double arg2)
	{
		boolean result = arg1 <= arg2;
		if (DEBUG_MODE) ThreadState.logln("le(" + arg1 + ", " + arg2 + ") => " + result);
		return result;
	} // le()

	/**
	 * Return the length of the String.
	 */
	public static int length(CharSequence data)
	{
		int result;
		if (data == null) result = 0;
		else
		{
			result = data.length();
		}
		if (DEBUG_MODE) ThreadState.logln("length(" + data + ") -> " + result);
		return result;
	} // length()

	/**
	 * Truncate the String to lim characters, from the left.
	 * @see rlimit()
	 */
	public static String limit(CharSequence data, int lim)
	{
		String result = null;
		if (lim > 0)
		{
			result = data.toString();
			if (data != null && data.length() > lim)
			{
				result = result.substring(0, lim);
			}
		}
		if (DEBUG_MODE) ThreadState.logln("limit(" + data + ", " + lim + ") -> " + result);
		return result;
	} // limit()

	/**
	 * Convert String to lower case.
	 */
	public static String lowerCase(CharSequence arg)
	{
		String result;
		if (arg == null) result = null;
		else result = arg.toString().toLowerCase();
		if (DEBUG_MODE) ThreadState.logln("lowerCase(" + arg + ") -> " + arg);
		return result;
	} // lowerCase()

	/**
	 * Returns true if the first argument is less than the second, compared as real numbers.
	 */
	public static boolean lt(double arg1, double arg2)
	{
		boolean result = arg1 < arg2;
		if (DEBUG_MODE) ThreadState.logln("lt(" + arg1 + ", " + arg2 + ") => " + result);
		return result;
	} // lt()

	private DspStatement makeStatement(String name, int type)
			throws DspException, IllegalArgumentException
	{
		DspStatement state;
		boolean debug = false, trace = false;
		try { debug = _boolean(ThreadState.getOpen().get(DspObject.DEBUG)); } catch (NumberFormatException e) {}
		try { trace = _boolean(ThreadState.getOpen().get(DspObject.TRACE)); } catch (NumberFormatException e) {}
		switch (type)
		{
			case ARRAY_STMT:
				state = new DspStatementArray(name, debug, trace);
				break;
			case DIR_STMT:
				state = new DspStatementDir(name, debug, trace);
				 break;
			case SCAN_STMT:
				state = new DspStatementScan(name, debug, trace);
				break;
			case SQL_STMT:
				state = new DspStatementSql(name, debug, trace);
				break;
			default: throw new IllegalArgumentException("Invalid statement type: " + type);
		}
		return state;
	} // makeStatement()

	/**
	 * Returns the maximum (most positive) of the int arguments.
	 * Any null arguments are ignored.
	 */
	public static int max(Object[] args)
	{
		int psize;
		if (args == null || ((psize = args.length) == 0)) return 0;
		int result = 0;
		try {
			result = _int(args[0]);
		} catch (NullPointerException e) {
		}
		for (int ix = 0; ix < psize; ix++)
		{
			try {
				int temp = _int(args[ix]);
				if (temp > result) result = temp;
			} catch (NullPointerException e) {
			}
		}
		return result;
	} // max()

	/**
	 * Returns the minimum (most negative) of the int arguments.
	 * Any null arguments are ignored.
	 */
	public static int min(Object[] args)
	{
		int psize;
		if (args == null || ((psize = args.length) == 0)) return 0;
		int result = 0;
		try {
			result = _int(args[0]);
		} catch (NullPointerException e) {
		}
		for (int ix = 0; ix < psize; ix++)
		{
			try {
				int temp = _int(args[ix]);
				if (temp < result) result = temp;
			} catch (NullPointerException e) {
			}
		}
		return result;
	} // min()

	/**
	 * Returns the remainder of num / den
	 */
	public static int mod(int num, int den)
	{
		return num % den;
	} // mod()

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
	 * Integer multiplication.  Returns the product of all arguments.  Each argument
	 * is converted to an <b>int</b> type.  An empty or null array <b>arg</b> evalutes to 0.
	 * Any null arguments are ignored.
	 */
	public static int mul(Object[] args)
	{
		if (args == null || args.length == 0) return 0;
		int psize = args.length;
		if (DEBUG_MODE) ThreadState.logln("mul(" + psize + " args)");
		int result = 1;
		try {
			result = _int(args[0]);
		} catch (NullPointerException e) {
		}
		for (int ix = 1, end = psize; ix < end; ix++)
		{
			try {
				result *= _int(args[ix]);
			} catch (NullPointerException e) {
			}
		}
		return result;
	} // mul()

	/**
	 * Return one of several arguments depending the first.  The first argument is compared
	 * with each odd indexed argument.  If a match is found then the following argument is returned.
	 * If no match is found then the last argument is return as the default, or null if there
	 * were an odd number of arguments.  If the first argument is a string, then the comparisons
	 * by converting the objects to strings and and ignoring the case.  Otherwise, Object.equals()
	 * is used for comparison and the programmer must ensure type and value match.
	 */
	public static Object multiplex(Object[] args) throws IllegalArgumentException
	{
		int psize = args.length;
		if (!(psize >= 3))
				throw new IllegalArgumentException("multiplex() requires 3 or more arguments");
		Object obj = args[0];
		Object def = (psize & 1) == 0 ? args[psize - 1] : null;
		Object result = def;
		if (obj != null)
		{
			String str = obj instanceof String ? (String)obj : null;
			for (int ix = 1, end = psize - 1; ix < end; ix += 2)
			{
				Object test = args[ix];
				if (test != null)
				{
					if ((str != null && str.equalsIgnoreCase(_String(test)))
							|| obj.equals(test))
					{
						result = args[ix + 1];
						break;
					}
				}
			}
		}
		if (DEBUG_MODE) ThreadState.logln("multiplex(" + args + ") -> " + result);
		return result;
	} // multiplex()

	/**
	 * Not Equal. Return true if the two objects are not equal.  This calls eq() and returns the
	 * boolean negative.
	 * @see eq(Object, Object)
	 */
	public boolean ne(Object arg1, Object arg2) //throws Exception
	{
		boolean result = !eq(arg1, arg2);
		if (DEBUG_MODE) ThreadState.logln("ne(" + arg1 + ", " + arg2 + ") -> " + result);
		return result;
	} // ne()

	/**
	 * Integer Negation. Return the integer negative of the argument.
	 */
	public static int neg(int arg)
	{
		int result = -arg;
		if (DEBUG_MODE) ThreadState.logln("neg(" + arg + ") => " + result);
		return result;
	} // neg()

	/**
	 * Returns a path with the /./, and // removed and /folder/../ simplified.  Note: paths
	 * are converted to POSIX/Unix standard.
	 */
	public static String normalizePath(String path)
	{
		if (path == null || path.length() < 2) return path;
		int ix = 0;
		StringBuilder buf = new StringBuilder(path);
		char sep = File.separatorChar;
			// Convert non Posix file separators to slashes
		if (sep != '/')
		{
			boolean changed = false;
			while ((ix = path.indexOf(sep, ix)) >= 0)
			{
				buf.setCharAt(ix++, '/');
				changed = true;
			}
				// If it starts with a DOS drive letter, preceed the path with a '/'
			if (sep == '\\' && buf.charAt(1) == ':')
			{
				char c = buf.charAt(0);
				if ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z'))
				{
					buf.insert(0, '/');
					changed = true;
				}
			}
			if (changed) path = buf.toString();
		}
			// loop until no changes happen
		String temp = null;
		do {
			temp = path;
				// convert // to /
			while ((ix = path.indexOf("//")) >= 0)
			{
				buf.deleteCharAt(ix + 1);
				path = buf.toString();
			}
				// convert /./ to /
			while ((ix = path.indexOf("/./")) >= 0)
			{
				buf.delete(ix + 1, ix + 3);
				path = buf.toString();
			}
				// convert /folder/../ to /
			ix = -1;
			while ((ix = path.indexOf("/../", ix + 1)) >= 0)
			{
				int iy = path.lastIndexOf('/', ix - 1);
				if (iy >= 0 && (ix <= 2 || !buf.toString().substring(ix - 2, ix).equals("..")))
				{
					buf.delete(iy + 1, ix + 4);
					path = buf.toString();
					ix = iy - 1;
				}
			}
		} while (!temp.equals(path));
		return path;
	} // normalizePath()

	/**
	 * Return the boolean negative of the argument.
	 */
	public static boolean not(boolean arg)
	{
		boolean result = !arg;
		if (DEBUG_MODE) ThreadState.logln("not(" + arg + ") => " + result);
		return result;
	} // not()

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
	 * Logical Or.  Calculates the logical <b>or</b> of all arguments.
	 * Each argument is converted to a boolean as it is used.  An empty or null
	 * array arg evaluates to 0.  Note, this function cannot do short-circut
	 * evalation, as Java dose when you use the || operator.
	 * @see and()
	 */
	public static boolean or(Object[] args)
	{
		if (args == null || args.length == 0) return false;
		int psize = args.length;
		boolean result = false;
		for (int ix = 0, end = psize; !result && ix < end; ix++)
		{
			try {
				result |= _boolean(args[ix]);
			} catch (NullPointerException e) {
			}
		}
		if (DEBUG_MODE) ThreadState.logln("or(" + psize + " args) -> " + result);
		return result;
	} // or()

	/**
	 * Make String ready for use in an SQL statement.  Adds single quotes around the string
	 * and quotes embeded quotes.
	 * @see sql(CharSequence)
	 */
	public static String quote(CharSequence value)
	{
		int len;
		boolean unicode = false;
		if (value == null || (len = value.length()) == 0) return NULL;
		StringBuilder buf = new StringBuilder(len + len / 10 + 5);
		buf.append('\'');
		for (int ix = 0, iz = value.length(); ix < iz; ix++)
		{
			char c = value.charAt(ix);
			if (c > 127 && !unicode)
			{
				buf.insert(0, 'N');	// MS SQL Server 7 specific
				unicode = true;
				buf.append(c);
			}
			else
			if (c == '\'')
			{
				buf.append("''");
			}
			else buf.append(c);
		}
		buf.append('\'');
		String result = buf.toString();
		if (DEBUG_MODE) ThreadState.logln("quote(" + value + ") -> " + result);
		return result;
	} // quote()

	/**
	 * Make String ready for use as a double quoted HTML attribute.  Converts embeded quotes to
	 * &quot;, and <> to &lt; and &gt;.  It will enclose the resulting string in double quotes if
     * the second parameter is true.
	 */
	public static String quoteHtml(CharSequence value, boolean enclose)
	{
		int len;
		if (value == null || (len = value.length()) == 0)
		{
			if (enclose) return "\"\"";
			return "";
		}
		StringBuilder buf = new StringBuilder(len + len / 10 + 5);
		if (enclose) buf.append('"');
		for (int ix = 0, iz = value.length(); ix < iz; ix++)
		{
			char c = value.charAt(ix);
            switch (c) {
                case '"':
    				buf.append("&quot;");
                    break;
                case '<':
                    buf.append("&lt;");
                    break;
                case '>':
                    buf.append("&gt;");
                    break;
                default:
                    buf.append(c);
            }
		}
		if (enclose) buf.append('"');
		String result = buf.toString();
		if (DEBUG_MODE) ThreadState.logln("quoteHtml(" + value + ", " + enclose + ") -> " + result);
		return result;
	} // quoteHtml()

	/**
	 * Make String ready for use as a double quoted HTML attribute.  Converts embeded quotes to
	 * &quot;.  It will not enclose the resulting string in double quotes.
	 */
	public static String quotes(CharSequence value)
	{
		return quoteHtml(value, false);
	} // quotes()

	/**
	 * Make String ready for use in a JavaScript expression.  Adds single quotes around the string
	 * and backslashes embeded quotes.
	 * @see _script(CharSequence)
	 */
	public static String quoteScript(CharSequence value)
	{
		int len;
		if (value == null) return NULL;
		if ((len = value.length()) == 0) return "''";
		StringBuilder buf = new StringBuilder(len + len / 10 + 5);
		buf.append('\'');
		for (int ix = 0, iz = value.length(); ix < iz; ix++)
		{
			char c = value.charAt(ix);
            if (c == '<') {
                buf.append("&lt;");
                continue;
            }
            if (c == '>') {
                buf.append("&gt;");
                continue;
            }
            if (c == '"') {
                buf.append("&quot;");
                continue;
            }
			if (c == '\'' || c == '\\') buf.append("\\");
			buf.append(c);
		}
		buf.append('\'');
		String result = buf.toString();
		if (DEBUG_MODE) ThreadState.logln("quoteScript(" + value + ") -> " + result);
		return result;
	} // quoteScript()

	/**
	 * Make String ready for use in a JavaScript expression.  Adds double quotes around the string
	 * and backslashes embeded quotes.
	 * @see quotes(CharSequence)
	 */
	public static String quotesScript(CharSequence value)
	{
		int len;
		if (value == null) return NULL;
		if ((len = value.length()) == 0) return "''";
		StringBuilder buf = new StringBuilder(len + len / 10 + 5);
		buf.append('"');
		for (int ix = 0, iz = value.length(); ix < iz; ix++)
		{
			char c = value.charAt(ix);
            if (c == '<') {
                buf.append("&lt;");
                continue;
            }
            if (c == '>') {
                buf.append("&gt;");
                continue;
            }
			if (c == '"' || c == '\\')
			{
				buf.append("\\");
			}
			else buf.append(c);
		}
		buf.append('"');
		String result = buf.toString();
		if (DEBUG_MODE) ThreadState.logln("quotesScript(" + value + ") -> " + result);
		return result;
	} // quotesScript()

	/**
	 * Stores the tag extension object for use later in the same request.
	 */
	public static void releaseTag(DspObject temp, String prefix, String action, Tag tag) throws DspException
	{
		tag.release();
		temp.set('_' + prefix + '_' + action, tag);
	} // releaseTag()

	/**
	 * Substring Replacer. For each instance of <b>sub</b> found in <b>str</b>, it is replaced
	 * by <b>rep</b>.  The resulting String is returned.
	 */
	public static String replace(CharSequence str, String sub, String rep)
	{
		if (str == null) return null;
		String string = str.toString();
		StringBuilder buf = null;
		int lenS = sub.length();
		for (int last = 0;;)
		{
			int ix = string.indexOf(sub, last);
			if (ix < 0)
			{
				if (buf != null)
				{
					buf.append(string.substring(last));
					string = buf.toString();	// return string as result
				}
				break;
			}
			if (buf == null) buf = new StringBuilder(string.length() * 3 / 2);
			buf.append(string.substring(last, ix));
			buf.append(rep);
			last = ix + lenS;
		}
		return string;
	} // replace()

	/**
	 * Substring Replacer. For each instance of <b>sub</b> found in <b>str</b>, it is replaced
	 * by <b>rep</b>.  The buffer argument itself is modifed and returned.  This is faster than
	 * replace(), especially useful when called multiple times for various replacements.
	 */
	public static StringBuilder replaceBuf(StringBuilder buf, String sub, String rep)
	{
		String str = buf.toString();
		int lenS = sub.length();
		int diff = rep.length() - lenS;
		int offset = 0;
		for (int last = 0;;)
		{
			int ix = str.indexOf(sub, last);
			if (ix < 0) break;
			buf.replace(ix + offset, ix + offset + lenS, rep);
			last = ix + lenS;
			offset += diff;
		}
		return buf;
	} // replaceBuf()

	/**
	 * Return the Timestamp of this instant.
	 */
	public static Timestamp rightNow()
	{
		return new Timestamp(System.currentTimeMillis());
	} // rightNow()

	/**
	 * Truncate the String to lim characters, from the right.
	 * @see limit()
	 */
	public static String rlimit(CharSequence data, int lim)
	{
		String result = null;
		if (data != null && lim > 0)
		{
			result = data.toString();
			int len;
			if (data != null && (len = data.length()) > lim)
			{
				result = result.substring(len - lim);
			}
		}
		if (DEBUG_MODE) ThreadState.logln("rlimit(" + data + ", " + lim + ") -> " + result);
		return result;
	} // rlimit()

	private static final double[] mulDig = {1, 10, 100, 1000, 10000, 100000,
		1e6, 1e7, 1e8, 1e9, 1e10, 1e11, 1e12};
	private static final double[] divDig = {1, .1, .01, .001, .0001, .00001,
		1e-6, 1e-7, 1e-8, 1e-9, 1e-10, 1e-11, 1e-12};

	/**
	 * Round value off to a specified number of digists.
	 * @param value the value to round off
	 * @param digits the number of digits to keep, 0-12
	 */
	public static double round(double value, int digits) throws IllegalArgumentException
	{
		if (digits < 0 || digits >= mulDig.length)
				throw new IllegalArgumentException("Only 0 to 12 digits allowed for round()");
		return Math.round(value * mulDig[digits]) * divDig[digits];
	} // round()

	/**
	 * Returns an integer that has it's bits scrambled.  If the same resulting value is again sent
	 * through this function the original value will be returned.
	 */
	public static int scramble(int value, String key)
	{
		byte[] shifts = new byte[32];
		for (int ix = 0; ix < 32; ix++)
		{
			shifts[ix] = (byte)ix;
		}
		int hash = key.hashCode();
		int v = value ^ hash;
		Random rand = new Random((long)hash << 32 | hash);
		for (int ix = 0; ix < 32; ix++)
		{
			byte temp = shifts[ix];
			int r = (int)(rand.nextFloat() * 32);
			shifts[ix] = shifts[r];
			shifts[r] = temp;
		}
//System.out.print("scramble [");
//for (int ix = 0; ix < 32; ix++)
//{
//	if (ix > 0) System.out.print(", ");
//	System.out.print(shifts[ix]);
//}
//System.out.println("]");
		int result = 0;
		for (int ix = 0; ix < 32; ix++)
		{
			int shift = shifts[ix];
			result |= ((v >>> ix) & 1) << shift;
		}
//System.out.println("value " + Integer.toHexString(value) + ", hash " + Integer.toHexString(value) + ", result " + Integer.toHexString(result));
		if (DEBUG_MODE) ThreadState.logln("scramble(" + value + ", " + key + ") -> " + result);
		return result;
	} // scramble()

	/**
	 * Sets the value of the named member of the object to value.  If the object is a DspObject
	 * set() is called.  Otherwise, it uses reflection to look up and set the field.
	 */
	public static Object setMember(Object obj, String member, Object value) throws DspException
	{
		try {
			((DspObject)obj).set(member, value);
		} catch (ClassCastException e) {
			Class<?> c = obj.getClass();
			try {
				Field f = c.getField(member);
	//			if (Modifier.isPublic(f.getModifiers())
	//			{
					f.set(obj, value);
	//			}
			} catch (NoSuchFieldException e1) {
				throw new DspException("Could not find field " + member, e1);
			} catch (IllegalAccessException e1) {
				throw new DspException("Could not access field " + member, e1);
			}
		}
		return value;
	} // setMember()

	/**
	 * Set the prop object.  Used internally to set up the initial prop object.
	 */
	public void setProp(DspProp value)
	{
		prop = value;
	} // setProp()

	/**
	 * Return the sign of the argument: -1 negative, 0 for 0, or 1 for positive.
	 */
	public static int sgn(int arg)
	{
		if (arg < 0) return -1;
		else
		if (arg > 0) return 1;
		else return 0;
	} // sgn()

	/**
	 * Convert characters not allowed in HTML to allowable characters.  Example:
	 * &lt; to &amp;lt&semi;.  This is the opposite of fromHtml().
	 * @see fromHtml(CharSequence)
	 */
	public static String showHtml(CharSequence text)
	{
		if (text == null) return null;
		int end = text.length();
		StringBuilder buf = new StringBuilder(end * 5 / 4);
//	loop:
		for (int ix = 0; ix < end; ix++)
		{
			char c = text.charAt(ix);
			String app = null;
			switch (c)
			{
				case '<':
					app = "&lt;";
					break;
				case '>':
					app = "&gt;";
					break;
				case '&':
					app = "&amp;";
					break;
				case '"':
					app = "&quot;";
					break;
			}
			if (app != null) buf.append(app);
			else if (c > 0) buf.append(c);
		}
		return buf.toString();
	} // showHtml()

	/**
	 * Format boolean for use in SQL. This function converts the data to a String suitable for use in an SQL statement.
	 */
	public static String sql(boolean value)
	{
		return value ? "1" : "0";
	} // sql(boolean)

	/**
	 * Format Boolean for use in SQL. This function converts the data to a String suitable for use in an SQL statement.
	 */
	public static String sql(Boolean value)
	{
		if (value == null) return NULL;
		return value.booleanValue() ? "1" : "0";
	} // sql(Boolean)

	/**
	 * Format value for use in SQL. This function converts the data to a String suitable for use in an SQL statement.
	 */
	public static String sql(byte value)
	{
		return Byte.toString(value);
	} // sql(byte)

	/**
	 * Format char for use in SQL. This function converts the data to a String suitable for use in an SQL statement.
	 */
	public static String sql(char value)
	{
		return quote(String.valueOf(value));
	} // sql(char)

	/**
	 * Format Character for use in SQL. This function converts the data to a String suitable for use in an SQL statement.
	 */
	public static String sql(Character value)
	{
		if (value == null) return NULL;
		return quote(String.valueOf(value));
	} // sql(Character)

	/**
	 * Format a list of objects as an paren enclosed comma delimited list.
	 */
  public static String sql(Collection<Object> value) {
		Iterator<Object> it = value.iterator();
		StringBuilder buf = new StringBuilder("(");
		while (it.hasNext()) {
			if (buf.length() > 1) buf.append(", ");
			buf.append(sql(it.next()));
		}
		buf.append(')');
		return buf.toString();
	} // sql(Collection)

	/**
	 * Format Date for use in SQL. This function converts the data to a String suitable for use in an SQL statement.
	 */
	public static String sql(Date value)
	{
		return dateSql(value);
	} // sql(Date)

	/**
	 * Format Date for use in SQL. This function converts the data to a String suitable for use in an SQL statement.
	 */
	public static String sql(java.util.Date value)
	{
		if (value == null) return NULL;
		return instantSql(new Timestamp(value.getTime()));
	} // sql(java.util.Date)

	/**
	 * Format value for use in SQL. This function converts the data to a String suitable for use in an SQL statement.
	 */
	public static String sql(double value)
	{
		return Double.toString(value);
	} // sql(double)

	/**
	 * Format value for use in SQL. This function converts the data to a String suitable for use in an SQL statement.
	 */
	public static String sql(float value)
	{
		return Float.toString(value);
	} // sql(float)

	/**
	 * Format value for use in SQL. This function converts the data to a String suitable for use in an SQL statement.
	 */
	public static String sql(int value)
	{
		return Integer.toString(value);
	} // sql(int)

	/**
	 * Format Number for use in SQL. This function converts the data to a String suitable for use in an SQL statement.
	 */
	public static String sql(Number value)
	{
		return value == null ? NULL : value.toString();
	} // sql(Number)

	/**
	 * Format value for use in SQL. This function converts the data to a String suitable for use in an SQL statement.
	 */
	public static String sql(long value)
	{
		return Long.toString(value);
	} // sql(long)

	/**
	 * Format value for use in SQL. This function converts the data to a String suitable for use in an SQL statement.
	 */
	public static String sql(short value)
	{
		return Short.toString(value);
	} // sql(String)

	/**
	 * Format String for use in SQL. This function converts the data to a String suitable for use in an SQL statement.
	 */
	public static String sql(CharSequence value)
	{
		return quote(value);
	} // sql(CharSequence)

	/**
	 * Format Time for use in SQL. This function converts the data to a String suitable for use in an SQL statement.
	 */
	public static String sql(Time value)
	{
		return timeSql(value);
	} // sql(Time)

	/**
	 * Format Timestamp for use in SQL. This function converts the data to a String suitable for use in an SQL statement.
	 */
	public static String sql(Timestamp value)
	{
		return instantSql(value);
	} // sql(Timestamp)

	/**
	 * Format Object for use in SQL. This function converts the data to a String suitable for use in an SQL statement.
	 */
	@SuppressWarnings("unchecked")
	public static String sql(Object value)
	{
		if (value == null) return NULL;
		try {
			return sql((Number) value);
		} catch (ClassCastException e) {
			try {
				java.util.Date date = (java.util.Date) value;
				try {
					return sql((Time) date);
				} catch (ClassCastException e1) {
					try {
						return sql((Date) date);
					} catch (ClassCastException e2) {
						try {
							return sql((Timestamp) date);
						} catch (ClassCastException e3) {
							return sql(date);
						}
					}
				}
			} catch (ClassCastException e7) {
				try {
					return sql((Boolean) value);
				} catch (ClassCastException e4) {
					try {
						return sql((Character) value);
					} catch (ClassCastException e5) {
						try {
							return sql((Collection<Object>) value);
						} catch (ClassCastException e6) {
							try {
								return sql((Object[]) value);
							} catch (ClassCastException e8) {
								return sql(value.toString());
							}
						}
					}
				}
			}
		}
	} // sql()

	/**
	 * Format an array of numbers as an paren enclosed comma delimited list.
	 */
/*  public static String sql(Number[] value) {
		StringBuilder buf = new StringBuilder("(");
		for (int ix = 0, ixz = value.length; ix < ixz; ix++) {
			if (buf.length() > 1) buf.append(", ");
			buf.append(sql(value[ix]));
		}
		buf.append(')');
		return buf.toString();
	} // sql(Number[])
*/
	/**
	 * Format an array of objects as an paren enclosed comma delimited list.
	 */
  public static String sql(Object[] value) {
		StringBuilder buf = new StringBuilder("(");
		for (int ix = 0, ixz = value.length; ix < ixz; ix++) {
			if (buf.length() > 1) buf.append(", ");
			buf.append(sql(value[ix]));
		}
		buf.append(')');
		return buf.toString();
	} // sql(Object[])

	/**
	 * Format an array of strings as an paren enclosed comma delimited list.
	 */
/*  public static String sql(String[] value) {
		StringBuilder buf = new StringBuilder("(");
		for (int ix = 0, ixz = value.length; ix < ixz; ix++) {
			if (buf.length() > 1) buf.append(", ");
			buf.append(sql(value[ix]));
		}
		buf.append(')');
		return buf.toString();
	} // sql(String[])
*/
	/**
	 * Convert object to a Boolean and format it suitable for use in an SQL statement.
	 */
	public static String sqlBoolean(boolean arg)
	{
		return sql(arg);
	} // sqlBoolean()

	/**
	 * Convert value to char and format it suitable for use in an SQL statement.
	 */
	public static String sqlChar(char arg)
	{
		return sql(arg);
	} // sqlChar()

	/**
	 * Convert Object to Character and format it suitable for use in an SQL statement.
	 */
	public static String sqlCharacter(Object arg)
	{
		return sql(_Character(arg));
	} // sqlCharacter()

	/**
	 * Convert object to a Date and format it suitable for use in an SQL statement.
	 */
	public static String sqlDate(Date arg)
	{
		return sql(arg);
	} // sqlDate()

	/**
	 * Convert object to an Double and format it suitable for use in an SQL statement.
	 */
	public static String sqlDouble(Object arg)
	{
		return sql(_Double(arg));
	} // sqlDouble()

	/**
	 * Convert object to an Float and format it suitable for use in an SQL statement.
	 */
	public static String sqlFloat(Object arg)
	{
		return sql(_Float(arg));
	} // sqlFloat()

	/**
	 * Convert object to a Timestamp and format it suitable for use in an SQL statement.
	 */
	public static String sqlInstant(Timestamp arg)
	{
		return sql(arg);
	} // sqlInstant()

	/**
	 * Convert object to an int and format it suitable for use in an SQL statement.
	 */
	public static String sqlInt(int arg)
	{
		return sql(arg);
	} // sqlInt()

	/**
	 * Convert object to an Integer and format it suitable for use in an SQL statement.
	 */
	public static String sqlInteger(Object arg)
	{
		return sql(_Integer(arg));
	} // sqlInteger()

	/**
	 * Convert object to a String and format it suitable for use in an SQL statement.
	 * Same as calling quote().
	 * @see quote(CharSequence)
	 */
	public static CharSequence sqlString(CharSequence arg)
	{
		return quote(arg);
	} // sqlString()

	/**
	 * Convert object to a Time and format it suitable for use in an SQL statement.
	 */
	public static String sqlTime(Time arg)
	{
		return sql(arg);
	} // sqlTime()

	/**
	 * Strip various types of text from the String.
	 * @deprecated
	 */
	public static String strip(String type, String data) throws IllegalArgumentException
	{
		String result;
		if (data == null) result = null;
		if (HTML.equalsIgnoreCase(type)) result = stripHtml(data);
		else
		if (LINKS.equalsIgnoreCase(type)) result = stripLinks(data);
		else
		if (DOMAIN.equalsIgnoreCase(type)) result = stripDomain(data);
		else
		if (SPACE.equalsIgnoreCase(type)) result = stripSpace(data);
		else throw new IllegalArgumentException("strip(" + type + ") invalid parameter");
		if (DEBUG_MODE) ThreadState.logln("strip(" + type + ", " + data + ") -> " + result);
		return result;
	} // strip()

	private static String stripContig(String text, int offset)
	{
		if (DEBUG_MODE) ThreadState.logln("stripContig(" + text + ", " + offset + ')');
		int start = offset;
		String before = "#ERROR#";
		for (; start > 0; start--)
		{
			if (text.charAt(start) <= ' ')
			{
				before = text.substring(0, ++start) + before;
				break;
			}
		}
		int end = text.length();
		for (; offset < end; offset++)
		{
			char c = text.charAt(offset);
			if (c <= ' ' || c == ';' || c == '<') return before + text.substring(offset);
		}
		return before;
	} // stripContig()

	/**
	 * Strips out all but the domain name from the URL string.
	 */
	public static String stripDomain(String text)
	{
		String result;
		if (text == null || text.length() < 3) result = text;
		else
		{
			int start = text.indexOf("//");
			int end = text.indexOf("/", start >= 0 ? start : 0);
			if (start >= 0 && end >= 0) result = text.substring(start + 2, end);
			else
			if (start >= 0) result = text.substring(start + 2);
			else
			if (end > 0) result = text.substring(0, end);
			else result = text;
		}
		if (DEBUG_MODE) ThreadState.logln("stripDomain(" + text + ") -> " + result);
		return result;
	} // stripDomain()

	/**
	 * Strips out all HTML tags from the String.
	 */
	public static String stripHtml(CharSequence text)
	{
		String result;
		if (text == null) result = null;
		else
		if (text.length() < 3) result = text.toString();
		else
		{
			boolean inHtml = false, space = false;
			int iz = text.length();
			StringBuilder buf = new StringBuilder(iz);
			for (int ix = 0; ix < iz; ix++)
			{
				char c = text.charAt(ix);
				if (inHtml)
				{
					if (c == '>') inHtml = false;
					continue;
				}
				else
				if (space)
				{
					if (c > ' ') space = false;
					else continue;
				}
				if (c == '<')
				{
					inHtml = true;
				}
				else
				if (c <= ' ')
				{
					space = true;
					buf.append(' ');
				}
				else buf.append(c);
			}
			result = buf.toString();
		}
		if (DEBUG_MODE) ThreadState.logln("stripHtml(" + text + ") -> " + result);
		return result;
	} // stripHtml()

	/**
	 * String specified letters from the text String.
	 */
	private static String stripLetters(String text, String letters)
	{
		int len = 0;
		if (text == null || (len = text.length()) == 0) return text;
		StringBuilder buf = new StringBuilder(len);
		for (int ix = 0; ix < len; ix++)
		{
			char c = text.charAt(ix);
			if (letters.indexOf(c) < 0) buf.append(c);
		}
		return buf.toString();
	} // stripLetters()

	/**
	 * Strips out all anchor tags and references to an internet server.
	 */
	public static String stripLinks(CharSequence text)
	{
		String result;
		if (text == null) result = null;
		else
		if (text.length() < 4) result = text.toString();
		else
		{
			String string = text.toString();
			for (int ix = 0, end = types.length; ix < end; ix++)
			{
//				if (DEBUG_MODE) BZVar.logln("ix " + ix);
				String type = types[ix];
				for (int iy = 0; iy < 2; iy++)	// 0 = lower case, 1 == upper case
				{
//					if (DEBUG_MODE) BZVar.logln("iy " + iy);
					int index = 0;
					for (;;)
					{
						index = string.indexOf(type, index);
						if (index < 0) break;
//						if (DEBUG_MODE) BZVar.logln("index " + index);
						char c = type.charAt(0);
						if (c == '<')
						{
							int iend = index + type.length();
							if (string.length() > iend)
							{
								c = string.charAt(iend);
								if (c < '0'
										|| (c > '9' && c < 'A')
										|| (c > 'Z' && c < 'a')
										|| c > 'z')
								{
									string = stripTag(string, index);
								}
							}
							else index++;
						}
						else string = stripContig(string, index);
					}
					type = type.toUpperCase();
				}
			}
			result = string;
		}
		if (DEBUG_MODE) ThreadState.logln("stripLinks(" + text + ") -> " + result);
		return result;
	} // stripLinks()

	/**
	 * Strips dollar sign and percent symbols from the String.
	 */
	public static String stripNonNumerics(String text)
	{
		String neg = (text != null && text.startsWith("-")) ? "-" : "";
		return neg + stripLetters(text.trim(), "$%,-() ");
	} // stripNonNumerics()

	/**
	 * Strips all whitespace from the string.
	 */
	public static String stripSpace(CharSequence text)
	{
		String result;
		int ixz;
		if (text == null) result = null;
		else
		if ((ixz = text.length()) == 0) result = text.toString();
		else
		{
			StringBuilder buf = new StringBuilder(ixz);
			for (int ix = 0; ix < ixz; ix++)
			{
				char c = text.charAt(ix);
				if (c > ' ') buf.append(c);
			}
			result = buf.toString();
		}
		if (DEBUG_MODE) ThreadState.logln("stripSpace(" + text + ") -> " + result);
		return result;
	} // stripSpace()

	private static String stripTag(String text, int offset)
	{
		if (DEBUG_MODE) ThreadState.logln("stripTag(" + text + ", " + offset + ')');
		String before = text.substring(0, offset) + ' ';
		@SuppressWarnings("unused")
		int start = offset++;
		int end = text.length();
		for (; offset < end; offset++)
		{
			if (text.charAt(offset) == '>') return before + text.substring(++offset);
		}
		return before;
	} // stripTag()

	/**
	 * Integer Subtraction.  Each argument is subtracted from the first.  If no arguments
	 * are submitted then the result is 0.
	 * Any null arguments are ignored.
	 */
	public static int sub(Object[] args)
	{
		if (args == null || args.length == 0) return 0;
		int psize = args.length;
		if (DEBUG_MODE) ThreadState.logln("sub(" + psize + " args)");
		int result = 0;
		try {
			result = _int(args[0]);
		} catch (NullPointerException e) {
		}
		for (int ix = 1, end = psize; ix < end; ix++)
		{
			try {
				result -= _int(args[ix]);
			} catch (NullPointerException e) {
			}
		}
		return result;
	} // sub()

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

	/**
	 * All possible chars for representing a number as a String
	 */
	private final static char[] digits = {
		'0' , '1' , '2' , '3' , '4' , '5' ,
		'6' , '7' , '8' , '9' , 'A' , 'B' ,
		'C' , 'D' , 'E' , 'F'
	};

	/**
	 * Convert arg to a hexadecimal.  If two arguments are sent, the second specifies the number
	 * of digits desired.
	 */
	public static String toHex(Object[] args) throws IllegalArgumentException
	{
		int psize = args.length;
		if (!(psize == 1))
				throw new IllegalArgumentException("toHex() can only accept 1 or 2 arguments");
		Object param = args[0];
		if (param == null || (param instanceof String && ((String)param).trim().length() == 0)) return null;
		int i = _int(param);
		String result;
		if (psize == 1) result = Integer.toHexString(i);
		else
		{
			int dig = _int(args[1]);
			char[] buf = new char[dig];
			int charPos = dig;
			int shift = 4;
			int radix = 1 << shift;
			int mask = radix - 1;
			while (dig-- > 0) {
				buf[--charPos] = digits[i & mask];
				i >>>= shift;
			}
			result = new String(buf);
		}
		if (DEBUG_MODE) ThreadState.logln("toHex(" + args + ") -> " + result);
		return result;
	} // toHex()

	/**
	 * Returns the value as a hexidecimal digit.
	 */
	private static char toHexDigit(int value)
	{
		value &= 0xf;
		if (value <= 9) return (char)('0' + value);
		return (char)('A' + value - 10);
	} // toHexDigit()

	/**
	 * Trims the whitespace from the beginning and end of the String.
	 */
	public static String trim(CharSequence arg)
	{
		String result;
		if (arg == null) result = null;
		else result = arg.toString().trim();
		if (DEBUG_MODE) ThreadState.logln("trim(" + arg + ") -> " + result);
		return result;
	} // trim()

	/**
	 * Convert String to upper case. This function converts all characters of arg to upper case.
	 */
	public static String upperCase(CharSequence arg)
	{
		String result;
		if (arg == null) result = null;
		else result = _String(arg).toUpperCase();
		if (DEBUG_MODE) ThreadState.logln("upperCase(" + arg + ") -> " + result);
		return result;
	} // upperCase()

	/**
	 * Used by both url() and urlFull() to do their jobs, since the are so similar.
	 */
	private static String url(CharSequence value, boolean partial)
	{
		if (value == null) return null;
		StringBuilder buf = new StringBuilder();
		for (int ix = 0, end = value.length(); ix < end; ix++)
		{
			char c = value.charAt(ix);
//			if (c <= ' ') buf.append('+');
//			else
			if ((c < '0' || (c > '9' && c < '@') || c > 'z')
					&& !(c == '-' || c == '/' || c == ':' || c == '.')
					&& (partial || !(c == '?' || c == '&' || c == '=' || c == '#')))
			{
				buf.append('%');
				buf.append(toHexDigit(c >> 4));
				buf.append(toHexDigit(c));
			}
			else buf.append(c);
		}
		return buf.toString();
	} // url()

	/**
	 * Format data for use in a URL String. This function formats arg as a String with
	 * characters that are not allowed in a URL converted to %hex.  Use this when you
	 * need to put a field value into an HTML URL, on the right side of the ? character.
	 * @see urlFull(String)
	 */
	public static String url(CharSequence value)
	{
		String result = url(value, true);
		if (DEBUG_MODE) ThreadState.logln("url(" + value + ") -> " + result);
		return result;
	} // url()

	/**
	 * Format data for use as a URL String. This function formats arg as a String with
	 * characters that are not allowed in a full URL converted to %hex.  Use this when you
	 * need to format a String to be the entire URL.
	 * @see url(String)
	 */
	public static String urlFull(CharSequence value)
	{
		String result = url(value, false);
		if (DEBUG_MODE) ThreadState.logln("urlFull(" + value + ") -> " + result);
		return result;
	} // url()

	/**
	 * Convert to boolean. This function converts the data to a boolean type.
	 */
	public static boolean _boolean(boolean arg)
	{
		return arg;
	} // _boolean(boolean)

	/**
	 * Convert to boolean. This function converts the data to a boolean type.
	 */
	public static boolean _boolean(double arg)
	{
		return arg != 0.0;
	} // _boolean(double)

	/**
	 * Convert to boolean. This function converts the data to a boolean type.
	 */
	public static boolean _boolean(float arg)
	{
		return arg != 0.0f;
	} // _boolean(float)

	/**
	 * Convert to boolean. This function converts the data to a boolean type.
	 */
	public static boolean _boolean(int arg)
	{
		return arg != 0;
	} // _boolean(int)

	/**
	 * Convert to boolean. This function converts the data to a boolean type.
	 */
	public static boolean _boolean(long arg)
	{
		return arg != 0L;
	} // _boolean(long)

	/**
	 * Convert to boolean. This function converts the data to a boolean type.
	 */
	public static boolean _boolean(String arg)
	{
		return arg != null && arg.length() > 0 && !arg.equalsIgnoreCase("false") && !arg.equalsIgnoreCase("0");
	} // _boolean(String)

	/**
	 * Convert to boolean. This function converts the data to a boolean type.
	 */
	public static boolean _boolean(Object obj)
	{
		return _Boolean(obj).booleanValue();
	} // _boolean(Object)

	/**
	 * Convert to Boolean. This function converts the data to a Boolean object.
	 */
	public static Boolean _Boolean(Object obj)
	{
		if (obj == null) return Boolean.FALSE;
		try {
			return (Boolean)obj;
		} catch (Exception e1) {
			try {
				return ((Number)obj).doubleValue() != 0.0 ? Boolean.TRUE : Boolean.FALSE;
			} catch (Exception e) {	// ClassCastException
				return new Boolean(_boolean(_String(obj)));
			}
		}
	} // _Boolean()

	/**
	 * Convert value to char.
	 */
	public static char _char(int val)
	{
		return (char)val;
	} // _char()

	/**
	 * Convert Object to char.
	 */
	public static char _char(Object obj) throws IllegalArgumentException
	{
		Character c = _Character(obj);
		if (c == null) throw new IllegalArgumentException("Cannot convert " + obj + " to a char");
		return c.charValue();
	} // _char()

	/**
	 * Convert Object to Character.
	 */
	public static Character _Character(Object obj)
	{
		try {
			return (Character)obj;
		} catch (ClassCastException e) {
			String str = _String(obj);
			if (str.length() == 0) return null;
			return new Character(str.charAt(0));
		}
	} // _Character()

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
	 * Convert to double. This function converts the data to a double type.
	 */
	public static double _double(boolean arg)
	{
		return arg ? 1.0 : 0.0;
	} // _double(boolean)

	/**
	 * Convert to double. This function converts the data to a double type.
	 */
	public static double _double(double arg)
	{
		return arg;
	} // _double(double)

	/**
	 * Convert to double. This function converts the data to a double type.
	 */
	public static double _double(int arg)
	{
		return (double)arg;
	} // _double(int)

	/**
	 * Convert to double. This function converts the data to a double type.
	 */
	public static double _double(long arg)
	{
		return (double)arg;
	} // _double(long)

	/**
	 * Convert to double. This function converts the data to a double type.
	 */
	public static double _double(String arg) throws NumberFormatException
	{
		return Double.parseDouble(stripNonNumerics(arg));
	} // _double(String)

	/**
	 * Convert to double. This function converts the data to a double type.
	 */
	public static double _double(Object obj) throws NumberFormatException
	{
		Double d = _Double(obj);
		return d == null ? 0.0 : d.doubleValue();
	} // _double(Object)

	/**
	 * Convert to double. This function converts the data to a double type.
	 */
	public static Double _Double(Object obj) throws NumberFormatException
	{
		try {
			return (Double)obj;
		} catch (Exception e0) {
			try {
				return new Double(((Number)obj).doubleValue());
			} catch (Exception e) {
				try {
					String s = stripNonNumerics(obj.toString());
					if (s.length() == 0) return null;
					return new Double(s);
				} catch (Exception e3) {
					throw new NumberFormatException("Cannot convert '" + obj + "' to a double");
				}
			}
		}
	} // _Double()

	/**
	 * Convert to float. This function converts the data to a floating point number.
	 */
	public static float _float(boolean arg)
	{
		return arg ? 1.0f : 0.0f;
	} // _float(boolean)

	/**
	 * Convert to float. This function converts the data to a floating point number.
	 */
	public static float _float(double arg)
	{
		return (float)arg;
	} // _float(double)

	/**
	 * Convert to float. This function converts the data to a floating point number.
	 */
	public static float _float(float arg)
	{
		return arg;
	} // _float(float)

	/**
	 * Convert to float. This function converts the data to a floating point number.
	 */
	public static float _float(int arg)
	{
		return (float)arg;
	} // _float(int)

	/**
	 * Convert to float. This function converts the data to a floating point number.
	 */
	public static float _float(long arg)
	{
		return (float)arg;
	} // _float(long)

	/**
	 * Convert to float. This function converts the data to a floating point number.
	 */
	public static float _float(String arg) throws NumberFormatException
	{
		return Float.parseFloat(stripNonNumerics(arg));
	} // _float(String)

	/**
	 * Convert to float. This function converts the data to a floating point number.
	 */
	public static float _float(Object obj) throws NumberFormatException
	{
		Float f = _Float(obj);
		return f == null ? 0f : f.floatValue();
	} // _float()

	/**
	 * Convert to Float. This function converts the data to a Float object.
	 */
	public static Float _Float(Object obj) throws NumberFormatException
	{
		try {
			return (Float)obj;
		} catch (Exception e0) {
			try {
				return new Float(((Number)obj).floatValue());
			} catch (Exception e) {
				try {
					String s = stripNonNumerics(obj.toString());
					if (s.length() == 0) return null;
					return new Float(s);
				} catch (Exception e3) {
					throw new NumberFormatException("Cannot convert '" + obj + "' to a float");
				}
			}
		}
	} // _Float()

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
	 * Convert to int. This function converts the data to a int type.
	 */
	public static int _int(boolean arg)
	{
		return arg ? 1 : 0;
	} // _int(boolean)

	/**
	 * Convert to int. This function converts the data to a int type.
	 */
	public static int _int(double arg)
	{
		return (int)arg;
	} // _int(double)

	/**
	 * Convert to int. This function converts the data to a int type.
	 */
	public static int _int(float arg)
	{
		return (int)arg;
	} // _int(float)

	/**
	 * Convert to int. This function converts the data to a int type.
	 */
	public static int _int(int arg)
	{
		return arg;
	} // _int(int)

	/**
	 * Convert to int. This function converts the data to a int type.
	 */
	public static int _int(long arg)
	{
		return (int)arg;
	} // _int(long)

	/**
	 * Convert to int. This function converts the data to a int type.
	 */
	public static int _int(String arg) throws NumberFormatException
	{
		return Integer.parseInt(stripNonNumerics(arg));
	} // _int(String)

	/**
	 * Convert to int. This function converts the data to a int type.
	 */
	public static int _int(Object obj) throws NumberFormatException
	{
		Integer i = _Integer(obj);
		return i == null ? 0 : i.intValue();
	} // _int()

	/**
	 * Convert to Integer. This function converts the data to an Integer object.
	 */
	public static Integer _Integer(Object obj) throws NumberFormatException
	{
		try {
			return (Integer)obj;
		} catch (Exception e0) {
			try {
				return new Integer(((Number)obj).intValue());
			} catch (Exception e) {
				try {
					cal.setTime((Date)obj);
					return new Integer(cal.get(Calendar.YEAR) * 10000 + (cal.get(Calendar.MONTH) + 1) * 100 + cal.get(Calendar.DAY_OF_MONTH));
				} catch (Exception e1) {
					try {
						cal.setTime((Time)obj);
						return new Integer(cal.get(Calendar.HOUR_OF_DAY) * 10000 + cal.get(Calendar.MINUTE) * 100 + cal.get(Calendar.SECOND));
					} catch (Exception e2) {
						try {
							Boolean b = (Boolean)obj;
							return new Integer(b.booleanValue() ? 1 : 0);
						} catch (Exception e3) {
							try {
								String s = stripNonNumerics(obj.toString());
								if (s.length() == 0) return null;
								return new Integer(s);
							} catch (Exception e4) {
								try {
									return new Integer(_Double(obj).intValue());
								} catch (Exception e5) {
									throw new NumberFormatException("Cannot convert '" + obj + "' to an int");
								}
							}
						}
					}
				}
			}
		}
	} // _Integer()

	/**
	 * Convert to long. This function converts the data to a long type.
	 */
	public static long _long(boolean arg)
	{
		return arg ? 1l : 0l;
	} // _long(boolean)

	/**
	 * Convert to long. This function converts the data to a long type.
	 */
	public static long _long(double arg)
	{
		return (long)arg;
	} // _long(double)

	/**
	 * Convert to long. This function converts the data to a long type.
	 */
	public static long _long(float arg)
	{
		return (long)arg;
	} // _long(float)

	/**
	 * Convert to long. This function converts the data to a long type.
	 */
	public static long _long(int arg)
	{
		return arg;
	} // _long(int)

	/**
	 * Convert to long. This function converts the data to a long type.
	 */
	public static long _long(long arg)
	{
		return arg;
	} // _long(long)

	/**
	 * Convert to long. This function converts the data to a long type.
	 */
	public static long _long(String arg) throws NumberFormatException
	{
		return Long.parseLong(stripNonNumerics(arg));
	} // _long(String)

	/**
	 * Convert to long. This function converts the data to a long type.
	 */
	public static long _long(Object obj) throws NumberFormatException
	{
		Long i = _Long(obj);
		return i == null ? 0 : i.longValue();
	} // _long()

	/**
	 * Convert to Long. This function converts the data to an Integer object.
	 */
	public static Long _Long(Object obj) throws NumberFormatException
	{
		try {
			return (Long)obj;
		} catch (Exception e0) {
			try {
				return new Long(((Number)obj).longValue());
			} catch (Exception e1) {
				try {
					cal.setTime((java.util.Date)obj);
					return new Long(cal.get(Calendar.YEAR) * 10000000000l
							+ (cal.get(Calendar.MONTH) + 1)		* 100000000
							+ cal.get(Calendar.DAY_OF_MONTH)		* 1000000
							+ cal.get(Calendar.HOUR_OF_DAY)		* 10000
							+ cal.get(Calendar.MINUTE)					* 100
							+ cal.get(Calendar.SECOND));
				} catch (Exception e2) {
					try {
						Boolean b = (Boolean)obj;
						return new Long(b.booleanValue() ? 1l : 0l);
					} catch (Exception e3) {
						try {
							String s = stripNonNumerics(obj.toString());
							if (s.length() == 0) return null;
							return new Long(s);
						} catch (Exception e4) {
							try {
								return new Long(_Double(obj).longValue());
							} catch (Exception e5) {
								throw new NumberFormatException("Cannot convert '" + obj + "' to an long");
							}
						}
					}
				}
			}
		}
	} // _Long()

	/**
	 * Convert to Object. This function converts the data to an Object object.
	 */
	protected static Object _Object(boolean val)
	{
		return val ? Boolean.TRUE : Boolean.FALSE;
	} // _Object()

	/**
	 * Convert to Object. This function converts the data to an Object object.
	 */
	protected static Object _Object(char val)
	{
		return new Character(val);
	} // _Object()

	/**
	 * Convert to Object. This function converts the data to an Object object.
	 */
	protected static Object _Object(short val)
	{
		return new Short(val);
	} // _Object()

	/**
	 * Convert to Object. This function converts the data to an Object object.
	 */
	protected static Object _Object(int val)
	{
		return new Integer(val);
	} // _Object()

	/**
	 * Convert to Object. This function converts the data to an Object object.
	 */
	protected static Object _Object(long val)
	{
		return new Long(val);
	} // _Object()

	/**
	 * Convert to Object. This function converts the data to an Object object.
	 */
	protected static Object _Object(float val)
	{
		return new Float(val);
	} // _Object()

	/**
	 * Convert to Object. This function converts the data to an Object object.
	 */
	protected static Double _Object(double val)
	{
		return new Double(val);
	} // _Object()

	/**
	 * Convert to Object. This function converts the data to an Object object.
	 */
	protected static Object _Object(Object val)
	{
		return val;
	} // _Object()

	/**
	 * Convert to String. This function converts the data to a String object.
	 */
	public static String _String(boolean val)
	{
		return String.valueOf(val);
	} // _String()

	/**
	 * Convert to String. This function converts the data to a String object.
	 */
	public static String _String(char val)
	{
		return String.valueOf(val);
	} // _String()

	/**
	 * Convert to String. This function converts the data to a String object.
	 */
	public static String _String(short val)
	{
		return String.valueOf(val);
	} // _String()

	/**
	 * Convert to String. This function converts the data to a String object.
	 */
	public static String _String(int val)
	{
		return String.valueOf(val);
	} // _String()

	/**
	 * Convert to String. This function converts the data to a String object.
	 */
	public static String _String(long val)
	{
		return String.valueOf(val);
	} // _String()

	/**
	 * Convert to String. This function converts the data to a String object.
	 */
	public static String _String(float val)
	{
		return String.valueOf(val);
	} // _String()

	/**
	 * Convert to String. This function converts the data to a String object.
	 */
	public static String _String(double val)
	{
		return String.valueOf(val);
	} // _String()

	/**
	 * Convert to String. This function converts the data to a String object.
	 */
	public static String _String(Object val)
	{
		if (val == null) return EMPTY;
		return val.toString();
	} // _String()

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
	 * Main DSP page function.  This function is created by the DSP engine for each page
	 * and is called by the DSP servlet for each request to the web page.
	 */
	public abstract void _jspService(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException;

} // DspPage
