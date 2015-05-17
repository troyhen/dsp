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

import com.dsp.DspPage;

import java.util.ArrayList;
import java.util.List;

class TagAttr
{
	private String name;
	private List<DspArg> args;

	TagAttr(String name)
	{
		this.name = name;
	} // TagAttr()

	void doJava(DspCompile comp, StringBuffer buf, int level) throws DspParseException
	{
		buf.append(".set");
		buf.append(Character.toUpperCase(name.charAt(0)));
		if (name.length() > 1)
		{
			buf.append(name.substring(1));
		}
		buf.append('(');
		int ixz;
		if (args != null && (ixz = args.size()) > 0)
		{
			boolean plus = false;
			for (int ix = 0; ix < ixz; ix++)
			{
				DspArg arg = args.get(ix);
				String text = arg.getText();
				if (text != null && text.length() > 0)
				{
					if (plus) buf.append(" + ");
					buf.append(DspPage.quotesScript(text));
					plus = true;
				}
				String expr = arg.getExpr();
				if (expr != null && expr.length() > 0)
				{
					if (plus) buf.append(" + ");
					buf.append(DspToken.fix(comp, expr, level));
					plus = true;
				}
			}
		} else {
			buf.append("null");
		}
		buf.append(");\r\n");
	} // doJava()

	String getName() { return name; }

	String getText(int index, DspCompile comp) throws DspParseException
	{
		if (args.size() != 1) throw new DspParseException("arguments must be static", index, comp);
		DspArg arg = (DspArg)args.get(0);
		if (arg.getExpr() != null) throw new DspParseException("arguments must be static", index, comp);
		return arg.getText();
	} // getText()

	@SuppressWarnings("unchecked")
	void setArgs(ArrayList<DspArg> args)
	{
		this.args = (List<DspArg>) args.clone();
		args.clear();
	} // setArgs()

} // TagAttr
