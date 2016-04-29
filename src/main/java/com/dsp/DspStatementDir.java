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
package com.dsp;

import java.io.File;
import java.util.*;	// Vector

import static com.dsp.util.BZCast._String;

class DspStatementDir extends DspStatement
{
	static final String NAME = "dir";

	private static final String[] columns = {"file", "name", "ext", "size", "date"};
	private static final int	FIRST = 1;
	private static final int	FILE	= 1;
	private static final int	FNAME	= 2;
	private static final int	EXT		= 3;
	private static final int	SIZE	= 4;
	private static final int	DATE	= 5;
	private static final int	LAST	= 5;

	private File				dir;
	private Enumeration<File>	iter;
	private int					pend;
	private File				file;
	private String				pattern;

	DspStatementDir(/*long id,*/ String name, boolean debug, boolean trace) throws DspException
	{
		super(/*id,*/ name, debug, trace);
		if (trace) ThreadState.logln("DspStatementDir(" + name + ")");
	} // DspStatementDir()

	public DspStatement close()
	{
		if (trace) ThreadState.logln("DspStatementDir.close()");
		dir = null;
		iter = null;
		return super.close();
	} // close()

	protected boolean exists0(int index)
	{
		return index >= FIRST && index < LAST ? true : false;
	} // exists0()

	protected boolean exists0(String variable)
	{
		if (file == null || name == null) return false;
		name = name.toLowerCase();
		for (int ix = FIRST; ix <= LAST; ix++)
		{
			if (name.equals(columns[ix - FIRST])) return true;
		}
		return false;
	} // exists0()

	protected int execute0(Object obj)
			throws DspException
	{
	//	if (trace) ThreadState.logln("DspStatementDir.execute0(" + path + ")");
		String path = parse(obj);
		path = ThreadState.getPageContext().normalizePath(path);
		iter = null;
		file = null;

		dir = new File(ThreadState.getServlet().getRealPath(ThreadState.getRequest(), path));
		if (debug) ThreadState.logln("Executing: dir on '" + dir + '\'');
		String[] list = dir.list();
		if (debug)
		{
			if (list == null) ThreadState.logln("Directory doesn't exist");
			else ThreadState.logln(list.length + " files found before filtering");
		}
		int row = DONE;
		if (list != null && list.length > 0)
		{
			row = 0;
			Vector<File> files = new Vector<File>();
			for (int ix = 0, end = list.length; ix < end; ix++)
			{
				String f = list[ix];
				if (isNameOk(f))
				{
					File file = new File(dir, list[ix]);
					if (file.isFile())
					{
						files.addElement(file);
					}
				}
			}
			if (files.size() > 0)
			{
				iter = files.elements();
//				file = (File)iter.nextElement();
			}
			else row = DONE;
			if (debug) ThreadState.logln(files.size() + " files left after filtering");
		}
		return row;
	} // execute0()

	private boolean find(int pix, String name, int nix)
	{
		for (int nend = name.length(); ; pix++, nix++)
		{
			if (pix >= pend)
			{
				if (nix >= nend) return true;
				return false;
			}
			char c = pattern.charAt(pix);
			if (c == File.pathSeparatorChar)
			{
				if (nix >= nend) return true;
				return find(pix + 1, name, 0);
			}
			if (nix >= nend) return false;
			switch (c)
			{
				case '*':
					if (pix + 1 >= pend || find(pix + 1, name, nix)) return true;
					pix--;
					break;
				case '?':
					break;
				default:
					if (c != name.charAt(nix)) return false;
			}
		}
	} // find()

	public String getColumnName(int index)
	{
		if (index < FIRST || index > LAST) return null;
		return columns[index - 1];
	} // getColumnName()

	protected Object getObject0(int index)
	{
		if (trace) ThreadState.logln("DspStatementDir.getObject(" + index + ")");
		if (file == null) return null;
		String str;
		int ix;
		switch (index)
		{
			case FILE:
				return file.getName();
			case FNAME:
				str = file.getName();
				ix = str.indexOf('.');
				if (ix < 0) return str;
				return str.substring(0, ix);
			case EXT:
				str = file.getName();
				ix = str.indexOf('.');
				if (ix < 0) return null;
				return str.substring(ix + 1);
			case SIZE:
				return new Long(file.length());
			case DATE:
				return new Date(file.lastModified());
			default:
				return null;
		}
	} // getObject()

	protected Object getObject0(String name)
	{
		if (trace) ThreadState.logln("DspStatementDir.getObject(" + name + ")");
		if (file == null || name == null) return null;
		name = name.toLowerCase();
		for (int ix = FIRST; ix <= LAST; ix++)
		{
			if (name.equals(columns[ix - FIRST])) return getObject0(ix);
		}
		return null;
	} // getObject0()

	public boolean hasResults() { return iter != null; }

	private boolean isNameOk(String name)
	{
		return find(0, name, 0);
	} // isNameOk()

	protected boolean next0()
	{
		if (trace) ThreadState.logln("DspStatementDir.next0()");
		if (iter == null) return false;
		file = null;
		if (iter.hasMoreElements()) file = (File)iter.nextElement();
		boolean result = file != null;
		if (!result)
		{
			iter = null;
		}
		return result;
	} // next()

	private String parse(Object obj)
	{
		if (trace) ThreadState.logln("DspStatementDir.stripDir(" + obj + ')');
		if (obj == null) return null;
		String path = _String(obj).trim();
//		if (!path.toLowerCase().startsWith("dir")) return null;
		path = path.trim();
		int ix = path.indexOf(',');
		if (ix < 0)
		{
			pattern = "*";
		}
		else
		{
			pattern = path.substring(ix + 1).trim();
			path = path.substring(0, ix).trim();
		}
		pend = pattern.length();
		if (debug) ThreadState.logln("dir: path is '" + path + "', pattern is '" + pattern + '\'');
		return path;
	} // parse()

	public static void main(String[] arg) throws Exception
	{
		// test code
		DspStatementDir stmt = new DspStatementDir(/*1,*/ "Test", false, false);
		if (stmt.execute("c:\\Webshare\\WWWRoot\\ut-newimage\\images;*.jpg"))
		{
			do {
				for (int ix = FIRST; ix <= LAST; ix++)
				{
					if (ix > FIRST) System.out.print(", ");
					String name = stmt.getColumnName(ix);
					System.out.print(name + ": " + stmt.getObject(name) + '[' + stmt.getObject(ix).getClass().getName() + ']');
				}
				System.out.println();
			} while (stmt.next());
		}
		else
		{
			System.out.println("statement was rejected!");
		}
	} // main()

} // DspStatementDir

