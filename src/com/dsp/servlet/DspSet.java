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

import com.dsp.DspFactory;

import java.util.List;

class DspSet extends DspToken implements Output
{
	static final String NAME = "set";

	private String name;

	DspSet(String eat, List<DspArg> args)
	{
		super(eat, args);
		name = DspFactory.getUnique("_set");
	} // DspSet()

	private static final int VAR = 0;
	private static final int EQUALS = 1;
	private static final int EXPR = 2;
	private static final int STMT = 3;

	public int doJava(DspCompile comp, StringBuffer buf, int level) throws DspParseException
	{
		if (args.size() == 0) throw new DspParseException(
				"set and default require a variable reference and statement or expression",
				getTokenIndex(), comp);
		StringBuffer buf2 = new StringBuffer();
		int phase = VAR;
		DspArg arg0 = args.get(0);
		String ref = arg0.getText();
		String var = ref;
		int ix = 0;
		for (int iz = ref.length(); ix < iz; ix++)
		{
			char c = ref.charAt(ix);
			switch (phase)
			{
				case VAR:
					if (c == '=')
					{
						phase = EXPR;
						var = ref.substring(0, ix);
					}
					else
					if (c <= ' ')
					{
						phase = EQUALS;
						var = ref.substring(0, ix);
					}
					break;
				case EQUALS:
					if (c <= ' ') break;
					if (c == '=') phase = EXPR;
					else phase = STMT;
					break;
				case EXPR:
					if (c <= ' ') break;
					phase = STMT;
					break;
			}
			if (phase == STMT) break;
		}
		doRefOpen(buf2, var);
		if (phase != STMT && args.size() > 1) phase = STMT;
		if (phase == STMT)
		{
			buf2.append("executeGet1st(");
			doStatement(comp, buf2, ix, name);
			buf2.append(");");
		}
		else
		{
			buf2.append(arg0.getExpr());
			buf2.append(";");
		}
		doRefClose(buf2);
		DspCompile.doTabs(buf, level);
		buf.append(fix(comp, buf2.toString(), level));
		buf.append("\r\n");
		return level;
	} // doJava()

	protected void doRefClose(StringBuffer buf)
	{
	} // doRefClose()

	protected void doRefOpen(StringBuffer buf, String var)
	{
		buf.append(var);
		buf.append(" = ");
	} // doRefOpen()

/*
	protected String parseRef(String ref) throws DspParseException
	{
		if (ref == null || ref.length() == 0) throw new DspParseException("variable reference", "null", getTokenIndex());
		String lower = ref.toLowerCase();
		int dot = lower.indexOf('.');
		if (dot < 0) throw new DspParseException(".", ref, getTokenIndex());
		String obj = lower.substring(0, dot);
		int dot2 = lower.indexOf('.', dot + 1);
		if (dot2 < 0) dot2 = lower.indexOf(' ', dot);
		//if (lower.startsWith("var"))
//		{
//			return "var.get(\""
		return null;
	} // parseRef()
*/
	public String toString()
	{
		return toString(NAME);
	} // toString()

} // DspSet
