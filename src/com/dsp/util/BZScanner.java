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
package com.dsp.util;

import java.io.PrintStream;
import java.util.Vector;

import org.apache.oro.text.perl.Perl5Util;

public class BZScanner //implements DspObject
{
/*
	public static final String AFTER				= "after";
	public static final String BEFORE				= "before";
	public static final String BEGINOFFSET	= "beginOffset";
	public static final String ENDOFFSET		= "endOffset";
	public static final String MATCH				= "match";
	public static final String SPLIT				= "split";
	public static final String SUBSTITUTE		= "substitute";
*/
	private Perl5Util perl = new Perl5Util();
	private boolean		trace, debug;
	private PrintStream log;

	public BZScanner()
	{
		this(false, false, null);
	} // BZScanner()

	public BZScanner(PrintStream log)
	{
		this(true, false, log);
	} // BZScanner()

	public BZScanner(boolean debug, boolean trace, PrintStream log)
	{
		this.trace = trace;
		this.debug = debug;
		this.log = log;
		if (log == null) this.log = System.out;
	} // BZScanner()
/*
	public boolean exists(String name)
	{
		Object obj = get(name);
		return obj != null;
	} // exists()

	public Object get(String name) throws IllegalArgumentException
	{
		if (trace) log.println(this + ".get(" + name + ')');
		Object result = null;
		try {
			int ix = Integer.parseInt(name);
			result = perl.group(ix);
		} catch (NumberFormatException e) {
			if (BEFORE.equalsIgnoreCase(name)) result = perl.preMatch();
			else
			if (AFTER.equalsIgnoreCase(name)) result = perl.postMatch();
			else
			if (MATCH.equalsIgnoreCase(name)) result = perl.group(0);
			else
			if (BEGINOFFSET.equalsIgnoreCase(name)) result = new Integer(perl.beginOffset(0));
			else
			if (ENDOFFSET.equalsIgnoreCase(name)) result = new Integer(perl.endOffset(0));
			else throw new IllegalArgumentException(BZExpRef.SCAN + '.' + name + " is not defined");
		}
		if (debug) log.println(BZExpRef.SCAN + '.' + name + " -> " + result);
		return result;
	} // get()
*/
	public String getAfter()
	{
		if (trace) log.println("BZScanner.getAfter()");
		String result = perl.postMatch();
		if (debug) log.println("scanner.getAfter() -> " + result);
		return result;
	} // getAfter()

	public String getBefore()
	{
		if (trace) log.println("BZScanner.getBefore()");
		String result = perl.preMatch();
		if (debug) log.println("scanner.getBefore() -> " + result);
		return result;
	} // getBefore()

	public int getBeginOffset()
	{
		if (trace) log.println("BZScanner.getBeginOffset()");
		int result = perl.beginOffset(0);
		if (debug) log.println("scanner.getBeginOffset() -> " + result);
		return result;
	} // getBeginOffset()

	public int getEndOffset()
	{
		if (trace) log.println("BZScanner.getEndOffset()");
		int result = perl.endOffset(0);
		if (debug) log.println("scanner.getEndOffset() -> " + result);
		return result;
	} // getEndOffset()

	public String getGroup(int index)
	{
		if (trace) log.println("BZScanner.get()");
		String result = perl.group(index);
		if (debug) log.println("scanner.get(" + index + ") -> " + result);
		return result;
	} // getGroup()

	public String getMatch()
	{
		if (trace) log.println("BZScanner.getMatch()");
		String result = perl.group(0);
		if (debug) log.println("scanner.getMatch() -> " + result);
		return result;
	} // getMatch()
/*
	public Object run(String function, Object[] args) throws IllegalArgumentException
	{
		if (trace) log.println(this + ".run(" + function + ')');
		if (MATCH.equalsIgnoreCase(function)) return runMatch(args);
		if (SPLIT.equalsIgnoreCase(function)) return runSplit(args);
		if (SUBSTITUTE.equalsIgnoreCase(function)) return runSubstitute(args);
		throw new IllegalArgumentException(BZExpRef.SCAN + '.' + function + "() is not defined");
	} // run()
*/
	public boolean match(String pat, String str)
	{
		if (trace) log.println("BZScanner.match()");
		boolean result = perl.match(pat, str);
		if (debug) log.println("scanner.match('" + pat + "', '" + str + "') -> " + result);
		return result;
	} // match()

	public Vector<String> split(String pat, String str)
	{
		if (trace) log.println("BZScanner.runSplit()");
		Vector<String> result = new Vector<String>();
		perl.split(result, pat, str);
		if (debug) log.println("scanner.split('" + pat + "', '" + str + "') -> " + result.size() + " matches");
		return result;
	} // split()

	public String substitute(String pat, String str)
	{
		if (trace) log.println("BZScanner.substitute()");
		pat = 's' + pat;
		String result = perl.substitute(pat, str);
		if (debug) log.println("substitute('" + pat + "', '" + str + "') -> " + result);
		return result;
	} // substitute()
/*
	public void set(String name, Object value) throws IllegalArgumentException
	{
		if (trace) log.println(this + ".set(" + name + ", " + value + ')');
		throw new IllegalArgumentException(BZExpRef.SCAN + " doesn't support set()");
	} // set()
*/
	public String toString()
	{
		return "BZScanner";
	} // toString()
/*
	public void unset(String name) throws IllegalArgumentException
	{
		if (trace) log.println(this + ".unset(" + name + ')');
		throw new IllegalArgumentException(BZExpRef.SCAN + " doesn't support unset()");
	} // unset()
*/
} // BZScanner()
