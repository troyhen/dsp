package com.dsp.util;

import java.io.File;
import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;
import java.text.DecimalFormat;
import java.util.Collection;
import java.util.Iterator;

import com.dsp.ThreadState;

import static com.dsp.util.BZCast._Character;
import static com.dsp.util.BZCast._Double;
import static com.dsp.util.BZCast._Float;
import static com.dsp.util.BZCast._Integer;
import static com.dsp.util.BZCast._String;
import static com.dsp.util.BZCast._int;
import static com.dsp.util.BZTime.dateSql;
import static com.dsp.util.BZTime.instantSql;
import static com.dsp.util.BZTime.timeSql;

public class BZText
{
	public static final boolean DEBUG_MODE	= false;
		/** The empty string */
	public static final String	EMPTY		= "";
		/** The word NULL */
	public static final String NULL			= "NULL";
	public static final String	DOMAIN		= "domain";
	public static final String	HTML		= "html";
	public static final String	LINKS		= "links";
	public static final String	SPACE		= "space";

	private static final DecimalFormat percent = new DecimalFormat("#,##0.#%");
	private static String[] types = {"<a", "</a", ".com", ".net", ".org", ".edu", ".gov", ".mil",};

	static final String base64Chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/";

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
	 * Decode a Base64 String.  This decodes a String that has been encoded with Base64
	 * encoding, as defined in RFC 1521.
	 */
	public static String base64Decode(String data)
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
	public static String base64Encode(String data)
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
	public static String eqQuote(String value)
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
		if (value == null || value instanceof String) return eqQuote((String)value);
		if (value instanceof Object[] || value instanceof String[]
				|| value instanceof Number[] || value instanceof Collection) return " in " + sql(value);
		return " = " + sql(value);
	} // eqSql()

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
	public static String fromHtml(String text)
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
	 * Convert from URL encoded string to Unicode text.  This function converts many 
	 * of the encoded HTML characters to normal Unicode text.  
	 * Example: &amp;lt&semi; to &lt;.  This is the opposite of url().
	 * @see showHtml(CharSequence)
	 */
	public static String fromUrl(String text)
	{
		if (text == null) return null;
		String string = text.toString();
		int ixz;
		if ((ixz = text.length()) == 0) return string;
		StringBuilder buf = new StringBuilder(ixz);
		String rep = null;
		for (int ix = 0; ix < ixz; ix++)
		{
			char c = text.charAt(ix);
			if (c == '%')
			{
				c = (char) Integer.parseInt(string.substring(ix + 1, ix + 3), 16);
				ix += 2;
			}
			if (c == '+')
			{
				c = ' ';
			}
			if (rep != null)
			{
				buf.append(rep);
				rep = null;
			}
			else buf.append(c);
		}
		return buf.toString();
	} // fromUrl()

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
	public static String html(String text)
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
	public static String htmlChars(String text)
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
	 * Returns true if the String begins and ends with HTML tags.
	 */
	public static boolean isHtml(String text)
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
	 * Return the length of the String.
	 */
	public static int length(String data)
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
	public static String limit(String data, int lim)
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
	public static String lowerCase(String arg)
	{
		String result;
		if (arg == null) result = null;
		else result = arg.toString().toLowerCase();
		if (DEBUG_MODE) ThreadState.logln("lowerCase(" + arg + ") -> " + arg);
		return result;
	} // lowerCase()

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
	 * Make String ready for use in an SQL statement.  Adds single quotes around the string
	 * and quotes embeded quotes.
	 * @see sql(CharSequence)
	 */
	public static String quote(String value)
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
	public static String quoteHtml(String value, boolean enclose)
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
	public static String quotes(String value)
	{
		return quoteHtml(value, false);
	} // quotes()

	/**
	 * Make String ready for use in a JavaScript expression.  Adds single quotes around the string
	 * and backslashes embeded quotes.
	 * @see _script(CharSequence)
	 */
	public static String quoteScript(String value)
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
	public static String quotesScript(String value)
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
	 * Substring Replacer. For each instance of <b>sub</b> found in <b>str</b>, it is replaced
	 * by <b>rep</b>.  The resulting String is returned.
	 */
	public static String replace(String str, String sub, String rep)
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
	 * Truncate the String to lim characters, from the right.
	 * @see limit()
	 */
	public static String rlimit(String data, int lim)
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

	/**
	 * Convert characters not allowed in HTML to allowable characters.  Example:
	 * &lt; to &amp;lt&semi;.  This is the opposite of fromHtml().
	 * @see fromHtml(CharSequence)
	 */
	public static String showHtml(String text)
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
	public static String sql(String value)
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
	public static CharSequence sqlString(String arg)
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
	public static String stripHtml(String text)
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
	public static String stripLinks(String text)
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
	public static String stripSpace(String text)
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
	 * Returns the value as a hexadecimal digit.
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
	public static String trim(String arg)
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
	public static String upperCase(String arg)
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
	private static String url(String value, boolean partial)
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
	public static String url(String value)
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
	public static String urlFull(String value)
	{
		String result = url(value, false);
		if (DEBUG_MODE) ThreadState.logln("urlFull(" + value + ") -> " + result);
		return result;
	} // url()

}
