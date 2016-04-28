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

class DspText implements Output
{
	static final int WRAP = 80;
	static final String NAME = "text";

	private String text;
	private int index, line;

	DspText(String text)
	{
		this.text = text;
	} // DspText()

	public int doJava(DspCompile comp, StringBuilder buf, int level) throws DspParseException
	{
		if (text != null && text.length() > 0)
		{
			buf.append("\r\n");
			DspCompile.doTabs(buf, level);
			buf.append("out.print(\"");
			int line = 0;
			boolean literal = false;
			for (int ix = 0, iz = text.length(); ix < iz; ix++)
			{
				if (line >= WRAP)
				{
					buf.append("\");\r\n");
					DspCompile.doTabs(buf, level);
					buf.append("out.print(\"");
					line = 0;
				}
				char c = text.charAt(ix);
				if (literal)
				{
					literal = false;
					switch (c)
					{
						case '{':
						case '}':
							buf.setLength(buf.length() - 2);
							break;
					}
				}
				String rep = null;
				switch (c)
				{
					case '\r':
						rep = "\\r";
						break;
					case '\n':
						rep = "\\n";
						break;
					case '\t':
						rep = "\\t";
						break;
					case '\b':
						rep = "\\b";
						break;
					case '\f':
						rep = "\\f";
						break;
					case '"':
						rep = "\\\"";
						break;
					case '\\':
						literal = true;
						rep = "\\\\";
						break;
					default:
						if (c < ' ' || (c >= 127 && c <= 161))
						{
							rep = "\\" + octal(c, 3);
						}
						else
						if (c > 255)
						{
							rep = "\\u" + hex(c, 4);
						}
				}
				if (rep != null)
				{
					buf.append(rep);
					line += rep.length();
				}
				else
				{
					buf.append(c);
					line++;
				}
			}
			buf.append("\");\r\n\r\n");
		}
		return level;
	} // doJava()

	public static String enHtml(String text)
	{
		StringBuilder buf = new StringBuilder();
		char c = 0, last;
		for (int ix = 0, iz = text.length(); ix < iz; ix++)
		{
			String rep = null;
			last = c;
			c = text.charAt(ix);
			switch (c)
			{
/*				case '<': rep = "&lt;"; break;
				case '>': rep = "&gt;"; break;
				case '&': rep = "&amp;"; break;
				case ';'; rep = "&semi;"; break;
				case '"'; rep = "&quot;"; break;
*/				default:
					if (c <= ' ')
					{
						if (last <= ' ') rep = "";
						else rep = " ";
					}
			}
			if (rep != null) buf.append(rep);
			else buf.append(c);
		}
		return buf.toString();
	} // enHtml()

	public String getComment() { return null; }
	public int getSourceLine() { return line; }
	public String getText() { return text; }
	public int getTokenIndex() { return index; }

	public static String hex(int val, int digits)
	{
		StringBuilder buf = new StringBuilder(digits);
		while (digits-- > 0)
		{
			char d = (char)((val & 15) + '0');
			if (d > '9') d += 7;
			val >>>= 4;
			buf.insert(0, d);
		}
		return buf.toString();
	} // hex()

	public static String octal(int val, int digits)
	{
		StringBuilder buf = new StringBuilder(digits);
		while (digits-- > 0)
		{
			char d = (char)((val & 7) + '0');
			val >>>= 3;
			buf.insert(0, d);
		}
		return buf.toString();
	} // octal()

	public void postParse(DspCompile comp, List<Token> tokens, int index) throws DspParseException {}
	public void setSourceLine(int line) { this.line = line; }
	public void setText(String text) { this.text = text; }
	public void setTokenIndex(int index) { this.index = index; }

	public String toString()
	{
		String text = enHtml(this.text);
		int len = 0;
		if (text != null) len = text.length();
		if (len > 60)
		{
			text = text.substring(0, 60);
			return NAME + ": " + len + " bytes, starting with: " + text;
		}
		return NAME + ": " + len + " bytes: " + text;
	} // toString()

} // DspText
