package com.dsp.util;

import com.dsp.DspNull;
import com.dsp.ThreadState;

import java.sql.Date;
import java.sql.Time;
import java.util.Collection;
import java.util.Random;

import static com.dsp.util.BZCast._Boolean;
import static com.dsp.util.BZCast._Double;
import static com.dsp.util.BZCast._Float;
import static com.dsp.util.BZCast._Integer;
import static com.dsp.util.BZCast._String;
import static com.dsp.util.BZCast._boolean;
import static com.dsp.util.BZCast._double;
import static com.dsp.util.BZCast._int;
import static com.dsp.util.BZTime._Date;
import static com.dsp.util.BZTime._Time;

//import static com.dsp.util.BZ
public class BZMath
{
	public static final boolean DEBUG_MODE	= false;

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

	/**
	 * Returns true if arg1 and arg2 are equals, false otherwise.  Arg2 is converted to
	 * the same time as arg1, if the types to not match.  If arg2 can't be converted the
	 * result is false.  Case is ignored when comparing Strings.
	 */
	public static boolean eq(Object arg1, Object arg2) //throws Exception
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
	 * Returns true if arg1 > arg2.
	 */
	public static boolean gt(double arg1, double arg2)
	{
		boolean result = arg1 > arg2;
		if (DEBUG_MODE) ThreadState.logln("gt(" + arg1 + ", " + arg2 + ") => " + result);
		return result;
	} // gt()

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
		boolean result = (obj == null) || (obj == DspNull.NULL);
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
	 * Returns true if the first argument is less than the second, compared as real numbers.
	 */
	public static boolean lt(double arg1, double arg2)
	{
		boolean result = arg1 < arg2;
		if (DEBUG_MODE) ThreadState.logln("lt(" + arg1 + ", " + arg2 + ") => " + result);
		return result;
	} // lt()

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
	public static boolean ne(Object arg1, Object arg2) //throws Exception
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
	 * Return the boolean negative of the argument.
	 */
	public static boolean not(boolean arg)
	{
		boolean result = !arg;
		if (DEBUG_MODE) ThreadState.logln("not(" + arg + ") => " + result);
		return result;
	} // not()

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

}
