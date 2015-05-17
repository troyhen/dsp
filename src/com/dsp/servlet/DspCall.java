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

class DspCall extends DspToken implements Output
{
	static final String NAME = "call";

	private String	first;	// holds the first argument after stripping mode flags
	private boolean	dspFlag, tryFlag, voidFlag;
//	private String	name;

	DspCall(String eat, List<DspArg> args)
	{
		super(eat, args);
//		name = DspFactory.getUnique("_call");
	} // DspCall()

	public int doJava(DspCompile comp, StringBuffer buf, int level) throws DspParseException
	{
		if (voidFlag)
		{
			DspCompile.doTabs(buf, level);
			buf.append("out = pageContext.pushBody();\r\n");
		}
		DspCompile.doTabs(buf, level);
		buf.append("pageContext.include(");
		StringBuffer buf2 = new StringBuffer();
		boolean more = false;
		for (int ix = 0, ixz = args.size(); ix < ixz; ix++)
		{
			DspArg arg = args.get(ix);
			String text = arg.getText();
			if (text != null && text.length() > 0)
			{
				if (more) buf2.append(" + ");
				else
				{
					more = true;
					text = first;
				}
				buf2.append('"');
				buf2.append(text);
				buf2.append('"');
			}
			text = arg.getExpr();
			if (text != null && text.length() > 0)
			{
				if (more) buf2.append(" + ");
				else more = true;
				buf2.append("_String(");
				buf2.append(text);
				buf2.append(")");
			}
		}
		buf.append(fix(comp, buf2.toString(), level));
		buf2 = null;
		if (dspFlag) buf.append(", true");
		buf.append(");\r\n");
		if (voidFlag)
		{
			DspCompile.doTabs(buf, level);
			buf.append("out = pageContext.popBody();\r\n");
		}
		if (tryFlag)
		{
			DspCompile.doTabs(buf, level);
			buf.append("var.set(\"error\", request.getAttribute(pageContext.THROWN);\r\n");
			DspCompile.doTabs(buf, level);
			buf.append("request.removeAttribute(pageContext.THROWN);\r\n");
		}
		DspCompile.doTabs(buf, level);
		buf.append("if (request.getAttribute(pageContext.FORWARD) != null");
		if (!tryFlag)
		{
			buf.append(" || request.getAttribute(pageContext.THROWN");
		}
		buf.append(") != null) return;\r\n");
		return level;
	} // doJava()

	@Override
	public void postParse(DspCompile comp, List<Token> tokens, int index) throws DspParseException
	{
		int size = args != null ? args.size() : 0;
		if (size == 0) throw new DspParseException("dsp, try, void, or page path", "end of call", index, comp);
		first = setModes(args.get(0).getText(), index, comp);
		super.postParse(comp, tokens, index);
	} // postParse()

	private String setModes(String text, int index, DspCompile comp) throws DspParseException
	{
		if (text == null) return text;
		if (text.length() == 0) return text;
		int iy = 0;
		boolean skipWhite = true;
		for (int ix = 0, ixz = text.length(); ix < ixz; ix++)
		{
			char c = text.charAt(ix);
			if (!skipWhite && c <= ' ' && ix > iy)
			{
				String val = text.substring(iy, ix).toLowerCase();
				if (val.equals("sync")) throw new DspParseException("call sync is not supported", index, comp);
				else
				if (val.equals("dsp")) dspFlag = true;
				else
				if (val.equals("try")) tryFlag = true;
				else
				if (val.equals("void")) voidFlag = true;
				else break;
				skipWhite = true;
			}
			else if (skipWhite && c > ' ')
			{
				iy = ix;
				skipWhite = false;
			}
		}
		if (skipWhite && (dspFlag || tryFlag || voidFlag)) iy = text.length();
		if (iy > 0) text = text.substring(iy).trim();
		return text;
	} // setModes()

	public String toString()
	{
		return toString(NAME);
	} // toString()

} // DspCall
