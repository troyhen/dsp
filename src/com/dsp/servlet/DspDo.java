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
//import java.util.Random;

class DspDo extends DspToken implements Output
{
	static final String NAME = "do";
//	static protected final Random random = new Random();

//	private final long id = random.nextLong();
	private int index;
	private String name;
	private boolean endTag, transaction;

	DspDo(String eat, List<DspArg> args, String name, int index)
	{
		super(eat, args);
		this.index = index;
		if (name == null) this.name = DspFactory.getUnique("_do");
		else this.name = name;
	} // DspDo()

	int doEnd(DspCompile comp, StringBuffer buf, int level)
	{
		if (transaction)
		{
			DspCompile.doTabs(buf, level - 1);
			buf.append("} catch (Throwable _e");
			buf.append(name);
			buf.append(") {\r\n");
			DspCompile.doTabs(buf, level);
			buf.append("request.setAttribute(pageContext.THROWN, _e");
			buf.append(name);
			buf.append(");\r\n");
			DspCompile.doTabs(buf, level);
			buf.append("if (_e");
			buf.append(name);
			buf.append(" instanceof ServletException) throw (ServletException)_e");
			buf.append(name);
			buf.append(";\r\n");
			DspCompile.doTabs(buf, level);
			buf.append("if (_e");
			buf.append(name);
			buf.append(" instanceof IOException) throw (IOException)_e");
			buf.append(name);
			buf.append(";\r\n");
			DspCompile.doTabs(buf, level);
			buf.append("throw new DspException(_e");
			buf.append(name);
			buf.append(");\r\n");
		}
		DspCompile.doTabs(buf, level - 1);
		buf.append("} finally {\r\n");
		DspCompile.doTabs(buf, level);
		buf.append("row = ");
		buf.append(name);
		buf.append(".close();\r\n");
		DspCompile.doTabs(buf, --level);
		buf.append("}\r\n");
		if (name != null && name.charAt(0) != '_') comp.removeSpecial(name);
		return level;
	} // doEnd()

	public int doJava(DspCompile comp, StringBuffer buf, int level) throws DspParseException
	{
		int oldLevel = level;
		DspArg arg;
		if (args != null && args.size() == 1
				&& (arg = args.get(0)).getExpr() == null
				&& arg.getText().equals("transaction"))
		{
			transaction = true;
		}
		StringBuffer buf2 = new StringBuffer();
		DspCompile.doTabs(buf2, level);
		buf2.append("DspStatement ");
		buf2.append(name);
		buf2.append(" = row = execute(");
		if (transaction)
		{
			buf2.append('"');
			buf2.append(name);
			buf2.append("\", DspPage.SQL_STMT, \"begin transaction\"");
		}
		else doStatement(comp, buf2, 0, name);
		buf2.append(");\r\n");
		buf.append(fix(comp, buf2.toString(), oldLevel));
		buf2 = null;
		DspCompile.doTabs(buf, level++);
		buf.append("try {\r\n");
		DspCompile.doTabs(buf, level);
		buf.append("if (row.hasResults()) row.next();\r\n");
		if (name != null && name.charAt(0) != '_') comp.addSpecial(name, getTokenIndex());
		return level;
	} // doJava()

	protected boolean getEnd() { return endTag; }
	protected int getIndex() { return index; }
	protected String getName() { return name; }

	boolean setEnd(DspEnd tag)
	{
		if (!endTag)
		{
			endTag = true;
			return true;
		}
		return false;
	} // setEnd()

	public String toString()
	{
		return toString(NAME);
	} // toString()

	public String toString(String command)
	{
		if (!name.startsWith("_")) return super.toString(command + " #" + name);
		return super.toString(command);
	} // toString()

} // DspDo

/*
	{do select * from Table}
		{[row.1]}
	{end do}
<!-- translation -->
	DspStatement _t1234 = execute("select * from Table");
	try {
			out.print(row.get("1"));
	} finally {
		st1234.close();
	}
-----------------------
	{do trasaction}
		...
	{end transaction}
<!-- translation -->
	executeDone("begin transaction");
	try {
		...
		executeDone("commit transaction");
	} catch (Exception _e1234) {
		executeDone("rollback transaction");
		throw new DspException(_e1234);
	}
*/
