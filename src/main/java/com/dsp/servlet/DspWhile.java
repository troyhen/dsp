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

class DspWhile extends DspIf
{
	static final String NAME = "while";

	private DspIf	ifTag;

	DspWhile(String eat, List<DspArg> args, String name, int index)
	{
		super(eat, args, name, index);
	} // DspWhile()

	int doElse(DspCompile comp, StringBuilder buf, int level)
	{
//		if (statement)
//		{
			DspCompile.doTabs(buf, --level);
			buf.append("}\r\n");
//		}
		return super.doElse(comp, buf, level, ifTag != null ? ifTag.getName() : getName());
	} // doElse()

	int doEnd(DspCompile comp, StringBuilder buf, int level)
	{
		if (ifTag != null)
		{
			String name = ifTag.getName();
			DspCompile.doTabs(buf, --level);
			buf.append("}\r\n");
			DspCompile.doTabs(buf, level);
			buf.append("row = ");
			buf.append(name);
			buf.append(".close();\r\n");
			if (!elseTag && name != null && name.charAt(0) != '_') comp.removeSpecial(name);
		}
		else
		{
//			if (statement && elseTag)
//			{
//				comp.doTabs(buf, --level);
//				buf.append("}\r\n");
//			}
			return super.doEnd(comp, buf, level);
		}
		return level;
	} // doEnd()

	public int doJava(DspCompile comp, StringBuilder buf, int level) throws DspParseException
	{
		StringBuilder buf2 = new StringBuilder();
		int oldLevel = level;
		String name;
		if (ifTag != null)
		{
			name = ifTag.getName();
			DspCompile.doTabs(buf2, level++);
			buf2.append("while (");
			buf2.append(name);
			buf2.append(".next()) {\r\n");
		}
		else if (statement)
		{
			name = getName();
			DspCompile.doTabs(buf2, level);
			buf2.append("DspStatement ");
			buf2.append(name);
			buf2.append(" = execute(");
			doStatement(comp, buf2, 0, name);
			buf2.append(");\r\n");
			DspCompile.doTabs(buf2, level++);
			buf2.append("try {\r\n");
			if (elseTag)
			{
				DspCompile.doTabs(buf2, level++);
				buf2.append("if (");
				buf2.append(name);
				buf2.append(".hasResults()) {\r\n");
			}
			DspCompile.doTabs(buf2, level++);
			buf2.append("while (");
			buf2.append(name);
			buf2.append(".next()) {\r\n");
			DspCompile.doTabs(buf2, level);
			buf2.append("row = ");
			buf2.append(name);
			buf2.append(";\r\n");
		}
		else
		{
			name = null;
			DspArg arg0 = args.get(0);
			if (elseTag)
			{
				DspCompile.doTabs(buf, level++);
				buf2.append("if (_boolean(");
				buf2.append(arg0.getExpr());
				buf2.append(")) {\r\n");
			}
			DspCompile.doTabs(buf, level++);
			buf2.append("while (_boolean(");
			buf2.append(arg0.getExpr());
			buf2.append(")) {\r\n");
		}
		buf.append(fix(comp, buf2.toString(), oldLevel));
		if (name != null && name.charAt(0) != '_' && ifTag == null) comp.addSpecial(name, getTokenIndex());
		return level;
	} // doJava()

	@Override
	public void postParse(DspCompile comp, List<Token> tokens, int index) throws DspParseException
	{
		super.postParse(comp, tokens, index);
		if (args == null || args.size() == 0)
		{
			for (--index; index >= 0; index--)
			{
				try {
					DspIf ifTag = (DspIf)tokens.get(index);
					if (ifTag.setWhile(this))
					{
						this.ifTag = ifTag;
						break;
					}
				} catch (ClassCastException e) {
				}
			}
			if (ifTag == null) throw new DspParseException("Empty while with no matching if", getTokenIndex(), comp);
		}
		else
		{
			DspArg arg0 = args.get(0);
			String text = arg0.getText();
			statement = text != null && text.length() > 0;
			if (!statement && args.size() > 1)
			{
				throw new DspParseException("Text not allowed following 'while [expr]'", getTokenIndex(), comp);
			}
		}
	} // postParse()

	public String toString()
	{
		return toString(NAME);
	} // toString()

} // DspWhile

/*
	{while [(int)var.count < 50]}
		{set var.count [(int)var.count + 1]}
	{end while}
<!-- translation -->
	while (_boolean(_int(var.get("count")) < 50)) {
		var.set("count", _Object(_int(var.get("count")) + 1));
	}
	------------------------
	{while [(int)var.count < 50]}
		{set var.count [(int)var.count + 1]}
	{else while}
		None found
	{end while}
<!-- translation -->
	if (_boolean(toInt(var.get("count")) < 50)) {
		while (_boolean(_int(var.get("count")) < 50)) {
			var.set("count", _Object(_int(var.get("count")) + 1));
		}
	} else {
		out.print("None found\r\n");
	}
	------------------------
	{while select * from Table}
		{[row.1]}
	{else while}
		None found
	{end while}
<!-- translation -->
	DspStatement st1234 = execute("select * from Table");
	try {
		if (st1234.hasResults()) {
			while (st1234.next()) {
				out.print(row.get("1"));
			}
			st1234.close();
		} else {
			out.print("None found\r\n");
		}
	} finally {
		st1234.close();
	}
	------------------------
	{if select * from Table}
	  List
		{while}
			{[row.1]}
		{end while}
	{else if}
		None found
	{end if}
<!-- translation -->
	DspStatement st1234 = execute("select * from Table");
	try {
		if (st1234.hasResults()) {
			while (st1234.next()) {
				out.print(row.get("1"));
				out.print("\r\n");
			}
			st1234.close();
		} else {
			out.print("None found\r\n");
		}
	} finally {
		st1234.close();
	}
*/