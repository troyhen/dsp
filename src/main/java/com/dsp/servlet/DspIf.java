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

class DspIf extends DspDo
{
	static final String NAME = "if";

	protected boolean elseTag, statement, whileTag;

	DspIf(String eat, List<DspArg> args, String name, int index)
	{
		super(eat, args, name, index);
	} // DspIf()

	int doElse(DspCompile comp, StringBuffer buf, int level)
	{
		return doElse(comp, buf, level, getName());
	} // doElse()

	protected int doElse(DspCompile comp, StringBuffer buf, int level, String name)
	{
		if (statement)
		{
			DspCompile.doTabs(buf, level);
			buf.append("row = ");
			buf.append(name);
			buf.append(".close();\r\n");
		}
		DspCompile.doTabs(buf, level - 1);
		buf.append("} else {\r\n");
		if (name != null && name.charAt(0) != '_') comp.removeSpecial(name);
		return level;
	} // doElse()

	int doEnd(DspCompile comp, StringBuffer buf, int level)
	{
		String name = getName();
		if (statement)
		{
			DspCompile.doTabs(buf, --level);
			buf.append("}\r\n");
			DspCompile.doTabs(buf, level - 1);
			buf.append("} finally {\r\n");
			DspCompile.doTabs(buf, level);
			buf.append("row = ");
			buf.append(name);
			buf.append(".close();\r\n");
			DspCompile.doTabs(buf, --level);
			buf.append("}\r\n");
		}
		else
		{
			DspCompile.doTabs(buf, --level);
			buf.append("}\r\n");
		}
		if (!elseTag && name != null && name.charAt(0) != '_') comp.removeSpecial(name);
		return level;
	} // doEnd()

	public int doJava(DspCompile comp, StringBuffer buf, int level) throws DspParseException
	{
		StringBuffer buf2 = new StringBuffer();
		int oldLevel = level;
		String name = getName();
		if (statement)
		{
			DspCompile.doTabs(buf2, level);
			buf2.append("DspStatement ");
			buf2.append(name);
			buf2.append(" = execute(");
			doStatement(comp, buf2, 0, name);
			buf2.append(");\r\n");
			DspCompile.doTabs(buf2, level++);
			buf2.append("try {\r\n");
			DspCompile.doTabs(buf2, level++);
			buf2.append("if (");
			buf2.append(name);
			if (whileTag)
			{
				buf2.append(".hasResults()) {\r\n");
			}
			else
			{
				buf2.append(".getResult() > 0 || ");
				buf2.append(name);
				buf2.append(".next()) {\r\n");
			}
			DspCompile.doTabs(buf2, level);
			buf2.append("row = ");
			buf2.append(name);
			buf2.append(";\r\n");
		}
		else
		{
			DspArg arg0 = args.get(0);
			DspCompile.doTabs(buf2, level++);
			buf2.append("if (_boolean(");
			buf2.append(arg0.getExpr());
			buf2.append(")) {\r\n");
		}
		buf.append(fix(comp, buf2.toString(), oldLevel));
		if (name != null && name.charAt(0) != '_') comp.addSpecial(name, getTokenIndex());
		return level;
	} // doJava()

	public void postParse(DspCompile comp, List<Token> tokens, int index) throws DspParseException
	{
		super.postParse(comp, tokens, index);
		if (this.getClass() == DspIf.class)
		{
			if (args == null || args.size() == 0)
			{
				throw new DspParseException("if requires a statement or expression", getTokenIndex(), comp);
			}
			DspArg arg0 = args.get(0);
			String text = arg0.getText();
			statement = text != null && text.length() > 0;
			if (!statement && args.size() > 1)
			{
				throw new DspParseException("Text not allowed following 'if [expr]'", getTokenIndex(), comp);
			}
		}
	} // postParse()

	boolean setElse(DspElse tag)
	{
		if (!getEnd() && !elseTag)
		{
			elseTag = true;
			return true;
		}
		return false;
	} // setElse()

	boolean setWhile(DspWhile tag)
	{
		if (statement && !whileTag)
		{
			whileTag = true;
			return true;
		}
		return false;
	} // setWhile()

	public String toString()
	{
		return toString(NAME);
	} // toString()

} // DspIf

/*
	{if select * from Table}
		{[row.1]}
	{else if}
		Not found
	{end if}
<!-- translation -->
	DspStatement st1234 = execute("select * from Table");
	try {
		if (st1234.next() || st1234.getResult() != 0) {
			out.print(row.get("1"));
			st1234.close();
		} else {
			out.print("Not found");
		}
	} finally {
		st1234.close();
	}
--------------------------
	{if [var.flag}
		{[row.1]}
	{else if}
		Not found
	{end if}
<!-- translation -->
	if (_boolean(var.get("flag"))) {
		out.print(row.get("1"));
	} else {
		out.print("Not found");
	}
*/