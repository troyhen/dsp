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

class DspSave extends DspToken implements Output
{
	static final String NAME = "save";

	protected boolean append = false;
//	protected String	name;

	DspSave(String eat, List<DspArg> args)
	{
		super(eat, args);
//		name = DspFactory.getUnique("_save");
	} // DspSave()

	public int doJava(DspCompile comp, StringBuilder buf, int level) throws DspParseException
	{
		DspCompile.doTabs(buf, level);
		buf.append("pageContext.save(");
		StringBuilder buf2 = new StringBuilder();
		boolean more = false, needComma = true;
		int ix = 0, ixz = args.size();
		for (; ix < ixz; ix++)
		{
			DspArg arg = args.get(ix);
			String text = arg.getText(), next = null;
			if (text != null && text.trim().length() > 0)
			{
				if (needComma)
				{
					int comma = text.indexOf(',');
					if (comma >= 0)
					{
						next = text.substring(comma + 1).trim();
						text = text.substring(0, comma).trim();
						if (text.length() > 0) {
							if (more) buf2.append(" + ");
							buf2.append('"');
							buf2.append(text);
							buf2.append('"');
						}
						buf2.append(", ");
						text = null;
						needComma = false;
						more = false;
						if (next.length() > 0) {
							buf2.append('"');
							buf2.append(next);
							buf2.append('"');
							more = true;
						}
					}
				}
				if (text != null) {
					if (more) buf2.append(" + ");
					else more = true;
					buf2.append('"');
					buf2.append(text);
					buf2.append('"');
				}
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
		if (needComma) throw new DspParseException(",", "end of " + (append ? "append" : "save"), getTokenIndex(), comp);
		buf.append(fix(comp, buf2.toString(), level));
		buf.append(", ");
		buf.append(append);
		buf.append(");\r\n");
		return level;
	} // doJava()

	public String toString()
	{
		return toString(NAME);
	} // toString()

} // DspSave
