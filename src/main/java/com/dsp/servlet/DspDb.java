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

class DspDb extends DspToken implements Output
{
	static final String NAME = "db";

	DspDb(String eat, List<DspArg> args)
	{
		super(eat, args);
	} // DspDb()

	public int doJava(DspCompile comp, StringBuilder buf, int level) throws DspParseException
	{
		StringBuilder buf2 = new StringBuilder();
		DspCompile.doTabs(buf, level);
		buf.append("open.setDatabase(");
		boolean more = false;
		for (int ix = 0, ixz = args.size(); ix < ixz; ix++)
		{
			DspArg arg = args.get(ix);
			String text = arg.getText();
			if (text != null && text.length() > 0)
			{
				if (more) buf2.append(" + ");
				else more = true;
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
		buf.append(");\r\n");
		return level;
	} // doJava()

	public String toString()
	{
		return toString(NAME);
	} // toString()

} // DspForward
