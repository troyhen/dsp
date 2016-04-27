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

class DspArg
{
	private String text, expr;

	DspArg(String text, String expr)
	{
		this.text = text;
		this.expr = parse(expr);
	} // DspArg()

	String getExpr() { return expr; }
	String getText() { return text; }

	private static String parse(String expr)
	{
		if (expr == null) return expr;
		int len = expr.length();
		StringBuffer buf = new StringBuffer(len);
		boolean string = false, backquote = false;
		int quote = -1;
		for (int ix = 0; ix < len; ix++)
		{
			char c = expr.charAt(ix);
			if (c == '"' && !backquote && quote < 0)
			{
				string = !string;
				c = '"';
			}
			else
			if (c == '`' && !string && quote < 0)
			{
				backquote = !backquote;
				c = '"';
			}
/*			else
			if (c == '\'' && !string && !backquote)
			{
				if (quote < 0) quote = ix;
				else
				{
					if (ix - quote > 2)
					{
						buf.setCharAt(quote, '"');
						quote = -1;
						c = '"';
					}
				}
			}*/
			buf.append(c);
		}
		return buf.toString();
	} // parse()

} // DspArg
