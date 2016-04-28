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
/*
TODO Problems:
(DspObject) open.s.get('1') -> StringIndexOutOfBounds -1
var.email.send() won't work yet.
Including a file that doesn't compile didn't quit the calling page.
{end} -> // {end end}
+Some parsing problems stop execution but the browser gets no error message.
+{[row]} doesn't work, and causes the above problem
*/

package com.dsp.servlet;

import com.dsp.DspPage;

import java.lang.reflect.*;	// Method
import java.sql.*;	// Date, Time, Timestamp
import java.util.List;

abstract class DspToken implements Token
{
	public static final Object[] objectArray = new Object[0];
	public static final Class<?> arrayClass = objectArray.getClass();

	protected String eat;
	protected List<DspArg> args;

	private int index, line;

	DspToken(String eat, List<DspArg> args)
	{
		this.eat = eat;
		if (args != null && args.size() > 0) this.args = args;
	} // DspToken()

//	private static final int PRENAME = 0;
//	private static final int INNAME = 1;
	private static final int PRETYPE = 2;
	private static final int INTYPE = 3;
	private static final int POSTTYPE = 4;
	private static final int DONE = 5;

	protected String doStatement(DspCompile comp, StringBuilder buf, int clip, String name)
			throws DspParseException
	{
		boolean comma = false;
		if (name == null) buf.append("null, ");
		else
		{
			buf.append('"');
			buf.append(name);
			buf.append("\", ");
		}
		for (int iy = 0, iyz = args.size(); iy < iyz; iy++)
		{
			DspArg arg = args.get(iy);
			String text = arg.getText();
			if (iy == 0 && text != null && text.length() > 0)
			{
				int phase = PRETYPE, start = clip;
				for (int ix = clip, len = text.length(); phase != DONE; ix++)
				{
					if (ix >= len)
					{
						clip = ix;
						break;
					}
					char c = text.charAt(ix);
					switch (phase)
					{
						case PRETYPE:
							if (c > ' ')
							{
								phase = INTYPE;
								start = ix;
							}
							break;
						case INTYPE:
							if (c <= ' ')
							{
								String type = text.substring(start, ix);
								phase = POSTTYPE;
								if (type.equals("scan")) buf.append(DspPage.SCAN_STMT);
								else
								if (type.equals("array")) buf.append(DspPage.ARRAY_STMT);
								else
								if (type.equals("dir")) buf.append(DspPage.DIR_STMT);
								else
								{
									buf.append(DspPage.SQL_STMT);
									if (!type.equals("sql"))
									{
										phase = DONE;
										clip = start;
									}
								}
								buf.append(", ");
							}
							break;
						case POSTTYPE:
							if (c > ' ')
							{
								phase = DONE;
								clip = ix;
							}
							break;
					}
				}
				switch (phase)
				{
					case PRETYPE:
					case INTYPE:
						throw new DspParseException("Statement not found", getTokenIndex(), comp);
				}
				if (clip > 0)
				{
					text = text.substring(clip);	//.trim();
				}
			} // iy == 0
			if (text != null && text.length() > 0)
			{
				if (comma) buf.append(" + ");
				comma = true;
				buf.append('"');
				buf.append(text);
				buf.append("\"");
			}
			text = arg.getExpr();
			if (text != null && text.length() > 0)
			{
				if (comma) buf.append(" + _String(");
				buf.append(text);
				if (comma) buf.append(")");
				comma = true;
			}
		}
		return buf.toString();
	} // doStatement()

	private void eatAfter(List<Token> tokens, int ix)
	{
//		if (TRACE) DspVar.logln("DspTokenTag.stripHead(" + token.text + ')');
		try {
			Token token;
			for (;;) {
				token = tokens.get(++ix);
				if (token instanceof DspToken && ((DspToken)token).eat != null) break;
				if (token.getClass() == DspText.class)
				{
					DspText ttok = (DspText)token;
					String text = ttok.getText();
					if (text != null)
					{
						int index1 = text.indexOf("</" + eat);
						int index2 = text.indexOf("</" + eat.toUpperCase());
						if (index2 > 0 && index2 < index1) index1 = index2;
						if (index1 < 0)
						{
							ttok.setText(null);
//System.out.println("</" + eat + " not found, eating everything");
							continue;
						}
						index1 = text.indexOf('>', index1 + 2 + eat.length());
						if (index1 < 0)
						{
							ttok.setText(null);
//System.out.println("</" + eat + " ...> not found, eating everything");
							continue;
						}
						boolean ws = false;
						for (int end = text.length() - 1; index1 < end; )
						{
							char c = text.charAt(++index1);
							if (c > ' ')
							{
								index1--;
								break;
							}
							ws = true;
						}
						ttok.setText(text.substring(++index1));
//System.out.println("eatAfter(" + ix + ") ws = " + ws);
							// ignore white space at the end of the file
						if (ws && ix < tokens.size() - 1) ws = false;
							// ignore white space if the next token will also eat tags
						if (ws && (token = (Token)tokens.get(ix + 1)) instanceof DspToken
								&& ((DspToken)token).eat != null) ws = false;
							// if white space followed the token add some back in
						if (ws) ttok.setText("\r\n\t" + ttok.getText());
		//				if (DEBUG) DspVar.logln("stripped off " + (index1 + 1) + " letters");
						break;
					}
				}
			}
		} catch (IndexOutOfBoundsException e) {
		}
	} // eatAfter()

	private void eatBefore(List<Token> tokens, int ix)
	{
		try {
			Token token;
			for (;;) {
				token = tokens.get(--ix);
				if (token instanceof DspToken && ((DspToken)token).eat != null) break;
				if (token.getClass() == DspText.class)
				{
					DspText ttok = (DspText)token;
					String text = ttok.getText();
					if (text != null)
					{
						int index1 = text.lastIndexOf("<" + eat);
						int index2 = text.lastIndexOf("<" + eat.toUpperCase());
						if (index2 > index1) index1 = index2;
						if (index1 < 0)
						{
							ttok.setText(null);
//							text = null;
							continue;
						}
						boolean ws = false;
						while (index1 >= 1)
						{
							char c = text.charAt(--index1);
							if (c > ' ')
							{
								index1++;
								break;
							}
							ws = true;
						}
	//						if (DEBUG) DspVar.logln("stripped off " + (token.text.length() - index1) + " letters");
						ttok.setText(text.substring(0, index1));
//System.out.println("eatBefore(" + ix + ") ws = " + ws);
							// ignore white space at the beginning of the file
						if (ws && ix == 0) ws = false;
							// ignore white space if the previous token will also eat tags
						if (ws && (token = (Token)tokens.get(ix - 1)) instanceof DspToken
								&& ((DspToken)token).eat != null) ws = false;
							// if white space preceeded the token add some back in
						if (ws) ttok.setText(ttok.getText() + ' ');
						break;
					}
				}
			}
		} catch (IndexOutOfBoundsException e) {
		}
	} // eatBefore()

	/** Fix both function calls and special object references.
	 */
	static String fix(DspCompile comp, String expr, int level)
	{
		if (expr == null || expr.length() == 0) return expr;
//try {
//com.dsp.ThreadState.logln("Before: " + expr);
			expr = fixSpecials(comp, expr, level);
//com.dsp.ThreadState.logln("After: " + expr);
			return fixCalls(comp, expr, level);
//} catch (Exception e) {
//	e.printStackTrace();
//}
//return null;
	} // fix()

	/** Fix function calls special functions of DspPage to pass arrays of objects.
	 */
	private static String fixCalls(DspCompile comp, String expr, int level)
	{
		StringBuilder buf = new StringBuilder();
		int count = 0;
		boolean literal = false, string = false, cstring = false;
		char c = 0;
		Method[] methods = DspPage.class.getMethods();
		for (int ix = 0, iz = methods.length; ix < iz; ix++)
		{
			Method method = methods[ix];
			int mods = method.getModifiers();
			if (!(Modifier.isPublic(mods) || Modifier.isProtected(mods))) continue;
			String spec = method.getName();
				// treat certain functions special; don't process them
			if (spec.startsWith("_") || spec.equals("getMember") || spec.equals("setMember")
					|| spec.equals("sql")) continue;
			int iy = 0;
		wloop:
			while ((iy = expr.indexOf(spec, iy)) >= 0)
			{
					// if I'm in a string this can't be one
				if (inString(expr, iy))
				{
					iy++;
					continue;
				}
					// if it's the first letter then it's a likely candidate
				if (iy > 0)
				{
						// if the preceding letter is part of the name, then I didn't find one
					c = expr.charAt(iy - 1);
					if (c > ' ' && c != '+' && c != '-' && c != '*' && c != '/'
							&& c != ';' && c != '!' && c != '"' && c != '\'' && c != '&'
							&& c != '|' && c != ',' && c != ':' && c != '<' && c != '>'
							&& c != '=' && c != '?' && c != '[' && c != '{' && c != '('
							&& c != '~')
					{
						iy++;
						continue;
					}
				}
/*					// see if there is a type cast
				String cast = null;
				int castStart = -1, castEnd = -1;
				for (int ik = iy - 1; ik > 0; ik--)
				{
					c = expr.charAt(ik);
					if (castEnd < 0)
					{
						if (c <= ' ') continue;
						if (c != ')') break;
						castEnd = ik + 1;
System.out.println("castEnd " + castEnd);
					}
					else
					{
						if (c == '(')
						{
							castStart = ik;
System.out.println("castStart " + castStart);
							cast = expr.substring(ik + 1, castEnd - 1).trim();
System.out.println("cast " + cast);
							break;
						}
						if (!(c <= ' ' || c == '_' || (c >= '0' && c <= '9')
								|| (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z'))) break;
					}
				}
*/					// make sure this is a function call
				int len = expr.length();
				int ij = iy + spec.length();
				for (; ij < len; ij++)
				{
					c = expr.charAt(ij);
					if (c <= ' ') continue;
					if (c == '(') break;
					iy++;
					continue wloop;
				}
				if (c != '(') {
					iy++;
					continue wloop;
				}
				ij++;
				count = 0;
				literal = string = cstring = false;
				Class<?>[] argTypes = method.getParameterTypes();
				int argIx = 0;
				boolean newArg = true;
				boolean multi = argTypes.length > 0 && argTypes[0] == arrayClass;
//				if (cast == null)
//				{
					if (ij < len) buf.append(expr.substring(0, ij));
					else buf.append(expr);
/*				}
				else
				{
					if (cast.equals("boolean") || cast.equals("byte")
							|| cast.equals("char") || cast.equals("double")
							|| cast.equals("float") || cast.equals("int")
							|| cast.equals("long"))
					{
						buf.append(expr.substring(0, castStart));
						buf.append("_");
						buf.append(cast);
					}
					else
					{
						buf.append(expr.substring(0, castEnd));
					}
					buf.append('(');
					buf.append(expr.substring(castEnd, ij));
				}
*/				if (multi)
				{
					buf.append("new Object[] {\r\n");
					DspCompile.doTabs(buf, level + 2);
					buf.append("_Object(");
				}
				for (; ij < len; ij++)
				{
					if (newArg && !multi)
					{
						if (argIx >= argTypes.length)
						{
							buf.setLength(0);
							iy++;
							continue wloop;
						}
						newArg = false;
						Class<?> cl = argTypes[argIx++];
						if (cl == boolean.class)
						{
							buf.append("_boolean(");
						}
						else
						if (cl == byte.class)
						{
							buf.append("_byte(");
						}
						else
						if (cl == char.class)
						{
							buf.append("_char(");
						}
						else
						if (cl == double.class)
						{
							buf.append("_double(");
						}
						else
						if (cl == float.class)
						{
							buf.append("_float(");
						}
						else
						if (cl == int.class)
						{
							buf.append("_int(");
						}
						else
						if (cl == long.class)
						{
							buf.append("_long(");
						}
						else
						if (cl == short.class)
						{
							buf.append("_short(");
						}
						else
						if (cl == String.class)
						{
							buf.append("_String(");
						}
						else
						if (cl == Date.class)
						{
							buf.append("_Date(");
						}
						else
						if (cl == Time.class)
						{
							buf.append("_Time(");
						}
						else
						if (cl == Timestamp.class)
						{
							buf.append("_Instant(");
						}
						else
						if (cl == Object.class)
						{
							buf.append("_Object(");
						}
						else
						{
							buf.append("(");
						}
					}
					c = expr.charAt(ij);
					if (literal) literal = false;
					else
					if (c == '\\') literal = true;
					else
					if (c == '"' && !cstring) string = !string;
					else
					if (c == '\'' && !string) cstring = !cstring;
					else
					if ((c == '(' || c == '{') && !(string || cstring)) count++;
					else
					if ((c == ')' || c == '}') && !(string || cstring))
					{
						count--;
						if (c == ')' && count < 0)	break;
					}
					else
					if (c == ',' && count == 0 && !(string || cstring))
					{
						if (multi)
						{
							buf.append("),\r\n");
							DspCompile.doTabs(buf, level + 2);
							buf.append("_Object(");
						}
						else
						{
							newArg = true;
							buf.append("), ");
						}
						continue;
					}
					buf.append(c);
				}
				buf.append(')');
				if (multi)
				{
					buf.append("\r\n");
					DspCompile.doTabs(buf, level + 1);
					buf.append("}");
				}
				buf.append(')');
//				if (cast != null) buf.append(')');
//System.out.println("ij = " + ij + ", len = " + expr.length());
				if (ij + 1 < len) buf.append(expr.substring(ij + 1));
				expr = buf.toString();
				buf.setLength(0);
				iy += 2;
			}
		}
		return expr;
	} // fixCalls()

	/** Fix a function call to use an Object array.
	 */
	private static int fixFunction(DspCompile comp, String expr, int level, StringBuilder buf,
			int index)
	{
		int count = 0;
		boolean string = false, cstring = false, literal = false;
		boolean first = true;
		int ik = index, len = expr.length();
		for (; ik < len; ik++)
		{
			char c = expr.charAt(ik);
			if (literal) literal = false;
			else
			if (c == '\'') literal = true;
			else
			if (c == '"' && !cstring) string = !string;
			else
			if (c == '\'' && !string) cstring = !cstring;
			else
			if ((c == '(' || c == '{') && !(string || cstring)) count++;
			else
			if ((c == ')' || c == '}') && !(string || cstring))
			{
				count--;
				if (c == ')' && count < 0) break;
			}
			else
			if (c == ',' && count == 0 && !(string || cstring))
			{
				buf.append("),\r\n");
				DspCompile.doTabs(buf, level + 2);
				buf.append("_Object(");
				continue;
			}
			if (first)
			{
				buf.append("new Object[] {\r\n");
				DspCompile.doTabs(buf, level + 2);
				buf.append("_Object(");
				first = false;
			}
			buf.append(c);
		} // for
		if (first) buf.append("null)");
		else
		{
			buf.append(")\r\n");
			DspCompile.doTabs(buf, level + 1);
			buf.append("})");
		}
		return ik + (ik < len ? 1 : 0);
	} // fixFunction()

	/** Fix the references to members of members of DspObjects
	 *  in expressions to use getMember() or call().
	 */
	private static int fixMember(DspCompile comp, String expr, int level, StringBuilder buf,
			int start, int end)
	{
//System.out.println("fixMember(" + expr + ", " + start + ", " + end + ')');
		int len = expr.length(), ix, iy;
//try {
		char c = 0;
			// Skip white space
		for (ix = end + 1; ix < len; ix++)
		{
			c = expr.charAt(ix);
			if (c > ' ') break;
//com.dsp.ThreadState.logln("?c = " + c);
		}
			// Find the member name
		for (iy = ix; iy < len; iy++)
		{
			c = expr.charAt(iy);
			if ((c < '0' || (c > '9' && c < 'A') || (c > 'Z' && c < 'a')
					|| c > 'z') && c != '_') break;
//com.dsp.ThreadState.logln("/c = " + c);
		}
		buf.insert(start, "getMember(");
		buf.append(", \"");
		buf.append(expr.substring(ix, iy));
		buf.append('"');
			// Skip white space
		for (; iy < len; iy++)
		{
			c = expr.charAt(iy);
			if (c > ' ') break;
//com.dsp.ThreadState.logln("+c = " + c);
		}
//com.dsp.ThreadState.logln("-c = " + c);
		int iz = iy + 1;
		if (c == '(')
		{
			buf.append(", ");
			iy = fixFunction(comp, expr, level, buf, iz);
		}
		else if (c == '=' && iz < len && expr.charAt(iz) != '=')
		{
				// change getMember to setMember
			buf.deleteCharAt(start);
			buf.insert(start, 's');
			buf.append(", ");
			int paren = 0;
			for (; iz < len; iz++)
			{
				c = expr.charAt(iz);
				if (c == '(' || c == '{') paren++;
				else
				if (c == ')' || c == '}') paren--;
				else
				if (c == ';' && paren == 0) break;
				buf.append(c);
			}
			buf.append(')');
			iy = iz;
		}
		else buf.append(')');
		return iy;
//} catch (Exception e) {
//	e.printStackTrace();
//}
//	return len;
	} // fixMember()

	/** Fix the references to special variables in expressions
	 *  to use get(), or set().  Note, do not send
	 *  comments to this code, or they will get processed also.
	 */
	private static String fixSpecials(DspCompile comp, String expr, int level)
	{
		final int GET  = 1;
		final int SET  = 2;
//		final int FUNC = 3;
		StringBuilder buf = new StringBuilder();
		//int count = 0;
//		boolean literal = false/*, string = false, cstring = false*/;
		char c;
		List<String> specials = comp.getSpecials();
		for (int ix = 0, iz = specials.size(); ix < iz; ix++)
		{
			String spec = specials.get(ix);
			int iy = 0;
		loop:
			while ((iy = expr.indexOf(spec, iy)) >= 0)
			{
					// if I'm in a string this can't be one
				if (inString(expr, iy))
				{
					iy++;
					continue;
				}
					// if it's the first letter then it's a likely candidate
				if (iy > 0)
				{
						// if the preceding letter is part of the name, then I didn't find one
					c = expr.charAt(iy - 1);
					if (c == '.' || c == '_' || (c >= '0' && c <= '9') || (c >= 'A' && c <= 'Z')
							|| (c >= 'a' && c <= 'z'))
					{
						iy++;
						continue;
					}
				}
				int ij = iy + spec.length();
				int len = expr.length();
					// skip trailing whitespace
				for (; ij < len; ij++)
				{
					c = expr.charAt(ij);
					if (c > ' ') break;
				}
					// make sure this is the beginning of a variable
				if (ij >= len)
				{		// hit expr's end so I'm done with this spec
					break;
				}
				c = expr.charAt(ij);
//				if (c == '_' (c >= '0' && c <= '9') || (c >= 'A' && c <= 'Z')
//						|| (c >= 'a' && c <= 'z'))
				if (c != '.')
				{
					iy++;
					continue;
				}
					// see if there is a type cast
				String cast = null;
				int castStart = -1, castEnd = -1;
				for (int ik = iy - 1; ik > 0; ik--)
				{
					c = expr.charAt(ik);
					if (castEnd < 0)
					{
						if (c <= ' ') continue;
						if (c != ')') break;
						castEnd = ik + 1;
//System.out.println("castEnd " + castEnd);
					}
					else
					{
						if (c == '(')
						{
							castStart = ik;
//System.out.println("castStart " + castStart);
							cast = expr.substring(ik + 1, castEnd - 1).trim();
//System.out.println("cast " + cast);
							break;
						}
						if (!(c <= ' ' || c == '_' || (c >= '0' && c <= '9')
								|| (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z'))) break;
					}
				}
					// I've found one, now get the member name
				String member = null;
				int ik = ++ij;
				for (; ik < len; ik++)
				{
					c = expr.charAt(ik);
					if (c == '_' || (c >= '0' && c <= '9') || (c >= 'A' && c <= 'Z')
							|| (c >= 'a' && c <= 'z')) continue;
					break;
				}
					// was a member found?
				if (ij == ik)
				{
					iy++;	// no
					continue;
				}
				member = expr.substring(ij, ik);

//					// is it one of the DspObject functions?
//				if (member.equals("get") || member.equals("set") || member.equals("run")
//						|| member.equals("exists") || member.equals("unset") || member.equals("equals"))
//				{
//					iy++;	// yes
//					continue;
//				}

					// I've found one, now do I need a get, set, or function call?
				int mode = GET;
				for (ij = ik; ij < len; ij++)
				{
					c = expr.charAt(ij);
					if (c <= ' ') continue;	// don't know yet
						// need func?
					if (c == '(')
					{
//						mode = FUNC;
//						break;
						iy++;		// I no longer want to call functions specially
						continue loop;
					}
					else // need set?
					if (c == '=' && ij + 1 < len && expr.charAt(ij + 1) != '=')
					{
						mode = SET;
						break;
					}
					else break;	// must need get
				}
				int il = ik;
				buf.setLength(0);
				switch (mode)
				{
					case GET:
						ij = iy;
						if (cast == null)
						{
							buf.append(expr.substring(0, iy));
							buf.append(spec);
							buf.append(".get(\"");
							buf.append(member);
							buf.append("\")");
						}
						else
						{
							ij = castStart;
							if (cast.equals("boolean") || cast.equals("byte") || cast.equals("char")
									|| cast.equals("double") || cast.equals("float") || cast.equals("int")
									|| cast.equals("long") || cast.equals("short") || cast.equals("String")
									|| cast.equals("Date") || cast.equals("Time") || cast.equals("Instant")
									|| cast.equals("Object"))
							{
								buf.append(expr.substring(0, castStart));
								buf.append('_');
								buf.append(cast);
								buf.append('(');
								buf.append(spec);
								buf.append(".get(\"");
								buf.append(member);
								buf.append("\"))");
							}
							else
							{
								buf.append(expr.substring(0, castStart));
								buf.append("((");
								buf.append(cast);
								buf.append(')');
								buf.append(spec);
								buf.append(".get(\"");
								buf.append(member);
								buf.append("\"))");
							}
						}
						for (; il < len; il++)
						{
							c = expr.charAt(il);
							if (c == '.')
							{
								il = ik = fixMember(comp, expr, level, buf, ij, ik);
							}
							if (c > ' ') break;
						}
						if (ik < len) buf.append(expr.substring(ik));
						expr = buf.toString();
						iy++;
						break;
					case SET:
						ik = expr.indexOf(";", ij + 1);
						while (ik >= 0 && inString(expr, ik))
						{
							ik = expr.indexOf(";", ik + 1);
						}
						if (ik < 0) throw new IllegalArgumentException("No semicolon found for assignment: " + expr);
						expr = expr.substring(0, iy) + spec + ".set(\"" + member + "\", _Object(" + expr.substring(ij + 1, ik).trim() + "))" + (ik < len ? expr.substring(ik) : "");
						break;
/*					case FUNC:
						//count = 0;
						literal = false;
						if (cast == null)
						{
							buf.append(expr.substring(0, iy));
						}
						else
						{
							if (cast.equals("boolean") || cast.equals("byte") || cast.equals("char")
									|| cast.equals("double") || cast.equals("float") || cast.equals("int")
									|| cast.equals("long") || cast.equals("short"))
							{
								buf.append(expr.substring(0, castStart));
								buf.append("_");
								buf.append(cast);
								buf.append('(');
							}
							else
							{
								buf.append(expr.substring(0, castEnd));
								buf.append('(');
							}
						}
						buf.append(spec);
						buf.append(".run(\"");
						buf.append(member);
						buf.append("\", ");
						ik = fixFunction(comp, expr, level, buf, ik);
						if (cast != null) buf.append(')');
						for (; il < len; il++)
						{
							c = expr.charAt(il);
							if (c == '.')
							{
								il = ik = fixMember(comp, expr, level, buf, iy, ik);
							}
							if (c > ' ') break;
						}
						if (il < len) buf.append(expr.substring(il));
						expr = buf.toString();
						buf.setLength(0);
						break;
*/				}
				iy++;
			}
		}
		return expr;
	} // fixSpecials()

	/** Fix the references to special variables in expressions
	 *  to use get(), set(), or run().  Note, do not send
	 *  comments to this code, or they will get processed also.
	 */
/*	private String fixSpecials(DspCompile comp, String expr, int level)
	{
		StringBuilder buf = new StringBuilder();
		int count = 0;
		boolean literal = false, string = false;
		char c;
		ArrayList specials = comp.getSpecials();
		for (int ix = 0, iz = specials.size(); ix < iz; ix++)
		{
			String spec = (String)specials.get(ix);
			fixObject(comp, expr, level, spec);
		}
		return expr;
	} // fixSpecials()
*/
	public String getComment()
	{
		return toString();
	} // getComment()

	public int getSourceLine() { return line; }
	public int getTokenIndex() { return index; }

	private static boolean inString(String str, int index)
	{
		boolean in = false;
//		boolean literal = false;
		for (int ix = 0; ix < index; ix++)
		{
			char c = str.charAt(ix);
			if (c == '\\') ix++;
			else
			if (c == '"') in = !in;
		}
		return in;
	} // inString()

	static DspToken makeToken(DspCompile comp, String eat, String command, String name,
			List<DspArg> args, int index) throws DspParseException
	{
		if (command == null)					return new DspOutput(eat, args);
		if (command.equals(DspAppend.NAME))	return new DspAppend(eat, args);
		if (command.equals(DspCall.NAME))		return new DspCall(eat, args);
		if (command.equals(DspDb.NAME))		return new DspDb(eat, args);
		if (command.equals(DspDefault.NAME))	return new DspDefault(eat, args);
		if (command.equals(DspDo.NAME))		return new DspDo(eat, args, name, index);
		if (command.equals(DspElse.NAME))		return new DspElse(eat, args);
		if (command.equals(DspEnd.NAME))		return new DspEnd(eat, args);
		if (command.equals(DspForward.NAME))	return new DspForward(eat, args);
		if (command.equals(DspIf.NAME))		return new DspIf(eat, args, name, index);
		if (command.equals(DspInsert.NAME))	return new DspInsert(eat, args);
		if (command.equals(DspMember.NAME))	return new DspMember(eat, args);
		if (command.equals(DspSave.NAME))		return new DspSave(eat, args);
		if (command.equals(DspScriptlet.NAME))	return new DspScriptlet(eat, args);
		if (command.equals(DspSet.NAME))		return new DspSet(eat, args);
		if (command.equals(DspSource.NAME))	return new DspSource(eat, args);
		if (command.equals(DspTagLib.NAME))	return new DspTagLib(eat, args, index, comp);
		if (command.equals(DspTarget.NAME))	return new DspTarget(eat, args);
		if (command.equals(DspUnset.NAME))		return new DspUnset(eat, args);
		if (command.equals(DspWhile.NAME))		return new DspWhile(eat, args, name, index);
		throw new DspParseException("DSP Command", command, index, comp);
	} // makeToken()

	protected String listArgs()
	{
		StringBuilder buf = new StringBuilder();
		if (args != null)
		{
			for (int ix = 0, iz = args.size(); ix < iz; ix++)
			{
				DspArg arg = args.get(ix);
				if (arg.getText() != null) buf.append(arg.getText());
				if (arg.getExpr() != null)
				{
					buf.append('[');
					buf.append(arg.getExpr());
					buf.append(']');
				}
			}
		}
		return buf.toString();
	} // listArgs()

	public void postParse(DspCompile comp, List<Token> tokens, int index) throws DspParseException
	{
//System.out.println("postParse(" + index + ")");
		if (eat != null)
		{
			if (index > 0) eatBefore(tokens, index);
			if (index < tokens.size() - 1) eatAfter(tokens, index);
		}
	} // postParse()

	public void setSourceLine(int line) { this.line = line; }
	public void setTokenIndex(int index) { this.index = index; }

	public String toString(String name)
	{
		return '{' + (eat != null ? '!' + eat + ' ' : "") + name + ' ' + listArgs() + '}';
	} // toString()

} // DspToken
