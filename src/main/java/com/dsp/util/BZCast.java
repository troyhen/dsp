package com.dsp.util;

import java.sql.Date;
import java.sql.Time;
import java.util.Calendar;

import static com.dsp.util.BZText.*;
import static com.dsp.util.BZTime.cal;

import static com.dsp.util.BZMath.isNull;

public class BZCast
{
	public static final boolean DEBUG_MODE = false;

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
		if (isNull(obj)) return Boolean.FALSE;
		if (obj instanceof Boolean) return (Boolean) obj;
		if (obj instanceof Number) {
			return Boolean.valueOf(((Number) obj).doubleValue() != 0.0);
		}
		return Boolean.valueOf(_boolean(_String(obj)));
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
		if (isNull(obj)) return null;
		if (obj instanceof Character) return (Character) obj;
		String str = _String(obj);
		if (str.length() == 0) return null;
		return Character.valueOf(str.charAt(0));
	} // _Character()

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
		if (isNull(obj)) return null;
		if (obj instanceof Double) return (Double) obj;
		if (obj instanceof Number) {
			return Double.valueOf(((Number) obj).doubleValue());
		}
		try {
			String s = stripNonNumerics(_String(obj));
			if (s.length() == 0) return null;
			return Double.valueOf(s);
		} catch (NumberFormatException e3) {
			throw new NumberFormatException("Cannot convert '" + obj + "' to a double");
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
		if (isNull(obj)) return null;
		if (obj instanceof Float) return (Float) obj;
		if (obj instanceof Number) {
			return Float.valueOf(((Number) obj).floatValue());
		}
		try {
			String s = stripNonNumerics(_String(obj));
			if (s.length() == 0) return null;
			return Float.valueOf(s);
		} catch (NumberFormatException e3) {
			throw new NumberFormatException("Cannot convert '" + obj + "' to a float");
		}
	} // _Float()

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
		if (isNull(obj)) return null;
		if (obj instanceof Integer) return (Integer) obj;
		if (obj instanceof Number) {
			return Integer.valueOf(((Number) obj).intValue());
		}
		if (obj instanceof Date) {
			cal.setTime((Date) obj);
			return Integer.valueOf(cal.get(Calendar.YEAR) * 10000 + (cal.get(Calendar.MONTH) + 1) * 100 + cal.get(Calendar.DAY_OF_MONTH));
		}
		if (obj instanceof Time) {
			cal.setTime((Time) obj);
			return new Integer(cal.get(Calendar.HOUR_OF_DAY) * 10000 + cal.get(Calendar.MINUTE) * 100 + cal.get(Calendar.SECOND));
		}
		if (obj instanceof Boolean) {
			return Integer.valueOf(((Boolean) obj) ? 1 : 0);
		}
		try {
			String s = stripNonNumerics(_String(obj));
			if (s.length() == 0) return null;
			return Integer.valueOf(s);
		} catch (NumberFormatException e4) {
			try {
				return Integer.valueOf(_Double(obj).intValue());
			} catch (NumberFormatException e5) {
				throw new NumberFormatException("Cannot convert '" + obj + "' to an int");
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
		if (isNull(obj)) return null;
		if (obj instanceof Long) return (Long) obj;
		if (obj instanceof Number) {
			return Long.valueOf(((Number) obj).longValue());
		}
		if (obj instanceof java.util.Date) {
			cal.setTime((java.util.Date) obj);
			return new Long(cal.get(Calendar.YEAR)	* 10000000000l
					+ (cal.get(Calendar.MONTH) + 1)	* 100000000
					+ cal.get(Calendar.DAY_OF_MONTH)* 1000000
					+ cal.get(Calendar.HOUR_OF_DAY)	* 10000
					+ cal.get(Calendar.MINUTE)		* 100
					+ cal.get(Calendar.SECOND));
		}
		if (obj instanceof Boolean) {
			return new Long(((Boolean) obj) ? 1l : 0l);
		}
		try {
			String s = stripNonNumerics(_String(obj));
			if (s.length() == 0) return null;
			return Long.valueOf(s);
		} catch (NumberFormatException e4) {
			try {
				return Long.valueOf(_Double(obj).longValue());
			} catch (Exception e5) {
				throw new NumberFormatException("Cannot convert '" + obj + "' to an long");
			}
		}
	} // _Long()

	/**
	 * Convert to Object. This function converts the data to an Object object.
	 */
	public static Object _Object(boolean val)
	{
		return val ? Boolean.TRUE : Boolean.FALSE;
	} // _Object()

	/**
	 * Convert to Object. This function converts the data to an Object object.
	 */
	public static Object _Object(char val)
	{
		return new Character(val);
	} // _Object()

	/**
	 * Convert to Object. This function converts the data to an Object object.
	 */
	public static Object _Object(short val)
	{
		return new Short(val);
	} // _Object()

	/**
	 * Convert to Object. This function converts the data to an Object object.
	 */
	public static Object _Object(int val)
	{
		return new Integer(val);
	} // _Object()

	/**
	 * Convert to Object. This function converts the data to an Object object.
	 */
	public static Object _Object(long val)
	{
		return new Long(val);
	} // _Object()

	/**
	 * Convert to Object. This function converts the data to an Object object.
	 */
	public static Object _Object(float val)
	{
		return new Float(val);
	} // _Object()

	/**
	 * Convert to Object. This function converts the data to an Object object.
	 */
	public static Double _Object(double val)
	{
		return new Double(val);
	} // _Object()

	/**
	 * Convert to Object. This function converts the data to an Object object.
	 */
	public static Object _Object(Object val)
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

}
