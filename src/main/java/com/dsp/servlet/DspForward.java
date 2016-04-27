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

class DspForward extends DspToken implements Output
{
	static final String NAME = "forward";
	static final String NAME2 = "redirect";

	private String	first;	// holds the first argument after stripping mode flags
	private boolean dspFlag;

	DspForward(String eat, List<DspArg> args)
	{
		super(eat, args);
	} // DspForward()

	public int doJava(DspCompile comp, StringBuffer buf, int level) throws DspParseException
	{
		DspCompile.doTabs(buf, level);
		buf.append("pageContext.forward(");
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
//		comp.doTabs(buf, level++);
//		buf.append("{\r\n");
//		comp.doTabs(buf, level);
//		buf.append("Exception _incExc = (Exception)request.getAttribute(pageContext.EXCEPTION);\r\n");
//		comp.doTabs(buf, level);
//		buf.append("if (_incExc != null) throw _incExc;\r\n");
//		comp.doTabs(buf, --level);
//		buf.append("}\r\n");
		DspCompile.doTabs(buf, level);
		buf.append("if (request.getAttribute(pageContext.FORWARD) != null) return;\r\n");
		return level;
	} // doJava()

	public void postParse(DspCompile comp, List<Token> tokens, int index) throws DspParseException
	{
		int size = args != null ? args.size() : 0;
		if (size == 0) throw new DspParseException("dsp or page path", "end of forward", index, comp);
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
				if (val.equals("sync")) throw new DspParseException("forward sync is not supported", index, comp);
				else
				if (val.equals("dsp")) dspFlag = true;
				else
				if (val.equals("try")) throw new DspParseException("forward try is not supported", index, comp);
				else
				if (val.equals("void")) throw new DspParseException("forward void is not supported", index, comp);
				else break;
				skipWhite = true;
			}
			else if (skipWhite && c > ' ')
			{
				iy = ix;
				skipWhite = false;
			}
		}
		if (skipWhite && dspFlag) iy = text.length();
		if (iy > 0) text = text.substring(iy).trim();
		return text;
	} // setModes()

	public String toString()
	{
		return toString(NAME);
	} // toString()

} // DspForward
