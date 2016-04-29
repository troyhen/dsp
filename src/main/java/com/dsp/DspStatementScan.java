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

import java.util.*;	// Hashtable, Vector

import static com.dsp.util.BZCast._String;

public class DspStatementScan extends DspStatement
{
	public static final String COLUMN = "word";
	public static final String DEF_DELIM = " \t\n\r";

	private StringTokenizer scan;
	private String item, delims = DEF_DELIM;

	public DspStatementScan(/*long id,*/ String name, boolean debug, boolean trace) throws DspException
	{
		super(/*id,*/ name, debug, trace);
		if (trace) ThreadState.logln("DspStatementScan(" + name + ")");
	} // DspStatementScan()

	public DspStatement close()
	{
		if (trace) ThreadState.logln("DspStatementScan.close()");
		item = null;
		scan = null;
		return super.close();
	} // close()

	public boolean exists0(int variable)
	{
		return variable == 1;
	} // count0()

	public boolean exists0(String variable)
	{
		return variable.toLowerCase().equals(COLUMN);
	} // count0()

	protected int execute0(Object data)
			throws DspException
	{
		if (trace) ThreadState.logln("DspStatementScan.execute(" + data + ")");
		scan = parse(data);
		int row = 0;
		item = null;
/*		try { item = scan.nextToken().trim(); } catch (NoSuchElementException e) {}
//		if (scan.hasMoreTokens()) delim = scan.nextToken();
//		else delim = null;
		if (item == null)
		{
			item = null;
			scan = null;
			row = DONE;
		}
*/		return row;
	} // execute0()

	public String getColumnName(int index)
	{
		if (index == 1) return COLUMN;
		return null;
	} // getColumnName()

	protected Object getObject0(int index)
	{
		if (trace) ThreadState.logln("DspStatementScan.getObject(" + index + ")");
		if (index == 1) return item;
		return null;
	} // getObject()

	protected Object getObject0(String name)
	{
		if (trace) ThreadState.logln("DspStatementScan.getObject(" + name + ")");
		if (COLUMN.equalsIgnoreCase(name)) return item;
		return null;
	} // getObject0()

	public boolean hasResults() { return true; }

	protected boolean next0()
	{
		if (trace) ThreadState.log("DspStatementScan.next0()");
		if (scan == null) return false;
		item = null;
		if (scan.hasMoreTokens())
		{
			item = scan.nextToken().trim();
//			if (scan.hasMoreTokens()) delim = scan.nextToken();
//			else delim = null;
			return true;
		}
		scan = null;
		return false;
	} // next0()

	private StringTokenizer parse(Object obj)
	{
		if (trace) ThreadState.logln("DspStatementScan.stripScan(" + obj + ')');
		String data = _String(obj);
		if (data.length() == 0) return null;
		int ix = 0, end = data.length();
		char c = 0;
			// look for the first character (non-whitespace)
		for (; ix < end; ix++)
		{
			c = Character.toLowerCase(data.charAt(ix));
			if (c > ' ') break;
		}
		int start = ix;
			// look for an impossible delimiter (alpha-numeric)
		if ((c >= '0' && c <= '9') || (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z'))
		{
				// alpha-numeric means no delimiter
			if (debug) ThreadState.logln("Scanning '" + data + "'");
			return new StringTokenizer(data, delims, false);
		}
			// look for whitespace after the delimiters
		for (; ix < end; ix++)
		{
			c = data.charAt(ix);
			if (c <= ' ')
			{
					// whitespace means we found a delimiter
				delims = data.substring(start, ix);
				if (delims.startsWith("_")) delims = " \n\r\t" + delims.substring(1);
				data = data.substring(ix + 1).trim();
				if (data.startsWith("[") && data.endsWith("]")) data = data.substring(1, data.length() - 1);
				if (debug) ThreadState.logln("Scanning '" + data + "' with delimeters of '" + delims + "'");
				return new StringTokenizer(data, delims, false);
			}
		}
			// no whitespace, so there is no delimiter
		if (debug) ThreadState.logln("Scanning '" + data + "'");
		return new StringTokenizer(data, delims, false);
	} // parse()

	public static void main(String[] arg) throws Exception
	{
		// test code
		DspStatement stmt = new DspStatementScan(/*1,*/ "Test", false, false);
		if (stmt.execute("scan ?.! How are you today? I'm fine, Thank you."))
		{
			do {
				int ix = 1;
				String name = stmt.getColumnName(ix);
				System.out.print(name + ": " + stmt.getObject(name) + '[' + stmt.getObject(ix).getClass().getName() + ']');
				System.out.println();
			} while (stmt.next());
		}
		else
		{
			System.out.println("statement was rejected!");
		}
	} // main()

} // DspStatementScan

