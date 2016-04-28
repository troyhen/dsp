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
package com.dsp.servlet;

import java.util.List;

class DspScriptlet extends DspToken implements Output
{
	static final String NAME = "scriptlet";

	DspScriptlet(String eat, List<DspArg> args)
	{
		super(eat, args);
	} // DspScriptlet()

	public int doJava(DspCompile comp, StringBuilder buf, int level)
	{
		return rawJava(args, comp, buf, level);
	} // doJava()

	private static boolean next(String text, int ix, char c)
	{
		if (ix + 1 >= text.length()) return false;
		return text.charAt(ix + 1) == c;
	} // next()

	static int rawJava(List<DspArg> args, DspCompile comp, StringBuilder buf, int level)
	{
		StringBuilder buf2 = new StringBuilder();
		boolean newLine = true, literal = false, string = false, cstr = false, skipWhite = true,
				slashComment = false, starComment = false;
		char c = 0;
		for (int ix = 0, ixz = args.size(); ix < ixz; ix++)
		{
			DspArg arg = (DspArg)args.get(ix);
			String text = arg.getText();
			if (text == null) return level;
			int len = text.length();
			if (len == 0) return level;
//ThreadState.logln(len + " text: " + text);
			for (int iy = 0; iy < len; iy++)
			{
				if (newLine)
				{
//ThreadState.logln("newLine level " + level);
					DspCompile.doTabs(buf2, level);
					newLine = false;
					skipWhite = true;
				}
				c = text.charAt(iy);
				if (skipWhite)
				{
					if (c <= ' ') continue;
					skipWhite = false;
				}
				if (c == '\r')
				{
					slashComment = false;
					c = 0;
					newLine = true;
					literal = false;
					if (iy + 1 < len && text.charAt(iy + 1) == '\n') iy++;
				}
				else
				if (c == '\n')
				{
					slashComment = false;
					c = 0;
					newLine = true;
					literal = false;
				}
				else
				if (literal) literal = false;
				else
				if (starComment)
				{
					if (c == '*' && next(text, iy, '/'))
					{
//ThreadState.logln("starComment ended at " + iy);
						starComment = false;
						buf2.append(c);
						c = text.charAt(++iy);
						newLine = true;
					}
				}
				else
				if (slashComment) {}
				else
				if (c == '/' && !(cstr || string))
				{
//ThreadState.logln("slash found at " + iy);
					if (next(text, iy, '/')) slashComment = true;
					else
					if (next(text, iy, '*'))
					{
//ThreadState.logln("starComment found at " + iy);
						starComment = true;
						buf2.append("\r\n");
						DspCompile.doTabs(buf2, level + 1);
					}
				}
				else
				if (c == '\\') literal = true;
				else
				if (c == '"' && !cstr) string = !string;
				else
				if (c == '\'' && !string) cstr = !cstr;
				else
				if (!(cstr || string || slashComment || starComment))
				{
					if (c == '{')
					{
						level++;
						newLine = true;
					}
					else
					if (c == '}')
					{
						level--;
						int bufEnd = buf2.length() - 1;
						if (buf2.charAt(bufEnd) == '\t') buf2.deleteCharAt(bufEnd);
						newLine = true;
					}
					else
					if (c == ';')
					{
						newLine = true;
					}
				}
				if (c != 0) buf2.append(c);
				if (newLine) buf2.append("\r\n");
			} // for iy
		} // for ix
		buf2.append("; // in case progammer forgot this\r\n");
		buf.append(fix(comp, buf2.toString(), level));
		return level;
	} // doJava()

	public String toString()
	{
		return null;
//		return "{{" + listArgs() + "}}";
	} // toString()

} // DspScriptlet
