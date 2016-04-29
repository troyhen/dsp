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

import java.sql.*;	// Date, SQLException, Time
import java.util.*;	// Hashtable, Vector

import static com.dsp.DspNull.NULL;
import static com.dsp.util.BZCast._boolean;
import static com.dsp.util.BZCast._Integer;
import static com.dsp.util.BZCast._String;
import static com.dsp.util.BZTime._Date;
import static com.dsp.util.BZTime._Time;

abstract public class DspStatement implements DspObject
{
	public static final String	NAME = "row";
	public static final int		DONE = Integer.MIN_VALUE;

	protected boolean		debug, trace;
	protected String		name;

	private Hashtable<String, Object>		cache = new Hashtable<String, Object>();
	private int					row = DONE;
	private String			statement;

	public DspStatement(String name, boolean debug, boolean trace) throws DspException
	{
		this.debug = debug;
		this.trace = trace;
		if (trace) ThreadState.logln("DspStatement(" + name + ")");
		this.name = name;
	} // DspStatement()

	public DspStatement close()
	{
		if (trace) ThreadState.logln("DspStatement.close()");
		cache = null;
		row = DONE;
		return ThreadState.getOpen().remove(this);
	} // close()

	/**
	 * Copies all of the row's values by name to the target object.
	 */
	public void copyTo(DspObject target) throws DspException
	{
		copyTo(target, null);
	} // copyTo()

	/**
	 * Copies all of the row's values by name to the target object.  If prefix is set
	 * this string will be prepended to each variable name in the target.
	 */
	public void copyTo(DspObject target, String prefix) throws DspException
	{
		if (trace) ThreadState.logln("DspStatement.copy(" + target + ')');
		int ix = 1;
		if (prefix == null) prefix = "";
		try {
			for (;; ix++)
			{
				String name = getColumnName(ix);
				Object value = getObject(ix);
				target.set(prefix + name, value);
				if (debug) ThreadState.logln(this.name + " copied column " + name);
			}
		} catch (SQLException e) {
				// I expect to get here on after I've gone past the last column.
				// I don't have a way to know how many columns there are so this is the way out of the loop.
				// if the first column errors, however, maybe there was some other problem so I'll throw an exception
			if (ix == 1) throw new DspException("Couldn't copy even one column", e);
		}
	} // copyTo()

	public boolean exists(String variable) throws DspException
	{
		if (trace) ThreadState.logln(name + ".exists(" + variable + ")");
		boolean result;
		String lower = variable.toLowerCase();
		if (cache.get(lower) != null) return true;
		try {
			try {
				result = exists0(Integer.parseInt(variable));
			}
			catch (NumberFormatException e)
			{
				result = exists0(lower);
			}
		} catch (SQLException e) {
			throw new DspException(e);
		}
		if (debug) ThreadState.logln(name + '.' + variable + (result ? " exists" : " does not exist"));
		return result;
	} // exists()

	protected abstract boolean exists0(int index) throws DspException, SQLException;
	protected abstract boolean exists0(String name) throws DspException, SQLException;

	public final boolean execute(Object obj) throws DspException
	{
		if (trace) ThreadState.logln("DspStatement.execute(" + obj + ")");
		cache.clear();
		if (debug) ThreadState.logln(name + " Executing: " + obj);
		ThreadState.getOpen().setLastExecuted(statement = _String(obj));
		DspStatementLog.logStatement(name, statement);
		try {
			row = execute0(obj);
		} catch (SQLException e) {
			DspStatementLog.logError(e);
			throw new DspException("Error executing " + obj, e);
		} finally {
			DspStatementLog.logRows(row);
			DspStatementLog.logTime();
		}
		if (hasResults())
		{
			ThreadState.getOpen().add(this);
			return true;
		}
		return false;
	} // execute()

	protected abstract int execute0(Object obj) throws DspException, SQLException;

	public Object get(String variable) throws DspException
	{
		if (trace) ThreadState.logln("DspStatement.get(" + variable + ")");
//		try {
			return getObject(variable);
//		} catch (SQLException e) {
//			throw new DspException("Couldn't get value of " + variable, e);
//		}
	} // get()

	public abstract String getColumnName(int index) throws SQLException;

	public java.sql.Date getDate(int index) throws DspException
	{
//		try {
			Object obj = getObject(index);
			if (obj == null) return null;
			return _Date(obj);
//		} catch (SQLException e) {
//			throw new DspException("Couldn't get date value of " + index, e);
//		}
	} // getDate()

	public java.sql.Date getDate(String name) throws DspException, SQLException
	{
//		try {
			Object obj = getObject(name);
			if (obj == null) return null;
			return _Date(obj);
//		} catch (SQLException e) {
//			throw new DspException("Couldn't get date value of " + name, e);
//		}
	} // getDate()

//	public long getId() { return id; }

	public Integer getInteger(int index) throws NumberFormatException, DspException
	{
//		try {
			Object obj = getObject(index);
			if (obj == null) return null;
			return _Integer(obj);
//		} catch (SQLException e) {
//			throw new DspException("Couldn't get integer value of " + index, e);
//		}
	} // getInteger()

	public Integer getInteger(String name) throws NumberFormatException, DspException
	{
//		try {
			Object obj = getObject(name);
			if (obj == null) return null;
			return _Integer(obj);
//		} catch (SQLException e) {
//			throw new DspException("Couldn't get int value of " + name, e);
//		}
	} // getInteger()

	public String getName() { return name; }
//	public DspStatement getNext() { return next; }

	public Object getObject(int index) throws DspException
	{
		if (trace) ThreadState.logln("DspStatement.getObject(" + index + ')');
		if (cache == null) throw new DspException("Statement " + name + " has been closed");
		try {
			String variable = String.valueOf(index);	//getColumnName(index);
			//if (variable == null) return null;
			String lower = variable.toLowerCase();
			Object result = cache.get(lower);
			if (result == null)
			{
				result = getObject0(index);
				if (result == null) cache.put(lower, NULL);
				else cache.put(lower, result);
			}
			else
			if (result == NULL) result = null;
			if (debug) ThreadState.logln(name + '.' + variable + " => " + (result == null ? null : result.toString()));
			return result;
		} catch (SQLException e) {
			throw new DspException("Can't get column " + index, e);
		}
	} // getObject()

	public Object getObject(String variable) throws DspException
	{
		if (trace) ThreadState.logln("DspStatement.getObject(" + variable + ')');
		if (cache == null) throw new DspException("Statement " + name + " has been closed");
		Object result = null;
		if (variable.equals(DEBUG)) result = new Boolean(debug);
		else
		if (variable.equals(TRACE)) result = new Boolean(trace);
		else
		if (variable.equals(NAME)) result = new Integer(row);
		else
		try {
			result = getObject(Integer.parseInt(variable));
		}
		catch (NumberFormatException e)
		{
			if (trace) ThreadState.logln("DspStatement.getObject(" + variable + ")");
			String lower = variable.toLowerCase();
			result = cache.get(lower);
			if (result == null)
			{
				try {
					result = getObject0(variable);
				} catch (SQLException e1) {
					throw new DspException("Can't get column " + variable, e1);
				}
				if (result == null) cache.put(lower, NULL);
				else cache.put(lower, result);
			}
			else
			if (result == NULL) result = null;
		}
		if (debug) ThreadState.logln(name + '.' + variable + " => " + result);
		return result;
	} // getObject()

	protected abstract Object getObject0(int index) throws DspException, SQLException;
	protected abstract Object getObject0(String name) throws DspException, SQLException;

	public int getResult()
	{
		if (debug) ThreadState.logln(name + ".result -> " + row);
		return row;
	} // getResult()

	public int getRow()
	{
		if (debug) ThreadState.logln(name + ".row -> " + row);
		return row;
	} // getRow()

	public String getStatement() {
		return statement;
	} // getStatement()

	public String getString(int index) throws DspException
	{
//		try {
			Object obj = getObject(index);
			if (obj == null) return null;
			return obj.toString();
//		} catch (SQLException e) {
//			throw new DspException("Couldn't get string value of " + index, e);
//		}
	} // getString()

	public String getString(String name) throws DspException
	{
//		try {
			Object obj = getObject(name);
			if (obj == null) return null;
			return obj.toString();
//		} catch (SQLException e) {
//			throw new DspException("Couldn't get string value of " + name, e);
//		}
	} // getString()

	public Time getTime(int index) throws DspException
	{
//		try {
			Object obj = getObject(index);
			if (obj == null) return null;
			return _Time(obj);
//		} catch (SQLException e) {
//			throw new DspException("Couldn't get time value of " + index, e);
//		}
	} // getTime()

	public Time getTime(String name) throws DspException
	{
//		try {
			Object obj = getObject(name);
			if (obj == null) return null;
			return _Time(obj);
//		} catch (SQLException e) {
//			throw new DspException("Couldn't get time value of " + name, e);
//		}
	} // getTime()

//	public int getTokenIndex() { return tokenIndex; }

	public abstract boolean hasResults();

	public Iterator<String> names()
	{
		ArrayList<String> list = new ArrayList<String>();
		try {
			for (int ix = 1; ; ix++)
			{
				String variable = getColumnName(ix);
				if (variable == null) break;
				list.add(variable);
			}
		} catch (SQLException e) {
		}
		return list.iterator();
	} // names()

	public final boolean next() throws DspException
	{
		if (trace) ThreadState.logln("DspStatement.next()");
		if (cache == null) throw new DspException("Statement " + name + " has been closed");
		cache.clear();
		DspStatementLog.logRows(row);
		row++;
		try {
			boolean result = next0();
			if (!result) row = DONE;
			if (debug)
			{
				if (result) ThreadState.logln(name + " Got row " + row);
				else ThreadState.logln(name + " No more rows");
			}
			return result;
		} catch (SQLException e) {
			throw new DspException("Couldn't get row " + row, e);
		}
	} // next()

	protected abstract boolean next0() throws DspException, SQLException;

//	public Object run(String function, Object[] args) throws DspException
//	{
//		if (trace) DspState.logln("DspStatement.run(" + function + ", " + args.length + " args");
//		if (function.equals("copyTo")) return runCopyTo(args);
//		else throw new DspException(NAME + '.' + function + "() is not defined");
//	} // run()

//	private Object runCopyTo(Object[] args) throws IllegalArgumentException
//	{
//		int psize;
//		if (args == null || (psize = args.length) != 1) throw new IllegalArgumentException("row.copyTo() requires 1 parameter");
//		try {
//			copyTo((DspObject) args[0]);
//		} catch (ClassCastException e) {
//			throw new IllegalArgumentException("row.copyTo() requires a DspObject paramater");
//		}
//		return null;
//	} // runCopyTo()

	public void set(String variable, Object value) throws DspException, DspReadOnlyException
	{
		if (trace) ThreadState.logln("DspStatement.set(" + variable + ", " + value + ')');
		if (value == null)
		{
			unset(variable);
			return;
		}
		else
		if (name.equals(DEBUG)) try { debug = _boolean(value); } catch (NumberFormatException e) {}
		else
		if (name.equals(TRACE)) try { trace = _boolean(value); } catch (NumberFormatException e) {}
		else throw new DspReadOnlyException(NAME, variable);
		if (debug) ThreadState.logln(name + '.' + variable + " <= " + value);
	} // set()

/*
	private String stripDB(DspTransaction trans, String sql) throws DspException
	{
		if (trace) DspState.logln("DspStatement.stripDB(" + sql + ')');
		sql = sql.trim();
		String dbName = null;
		if (sql.length() > 4 && sql.toLowerCase().startsWith("db") && sql.charAt(2) <= ' ')
		{
			int mark = 0;
			for (int ix = 3, end = sql.length(); ix < end; ix++)
			{
				char c = sql.charAt(ix);
				if (mark == 0 && c > ' ') mark = ix;
				else
				if (mark > 0 && c <= ' ')
				{
					dbName = sql.substring(mark, ix);
					sql = sql.substring(ix + 1).trim();
					break;
				}
			}
		}
		if (dbName == null)
		{
			db = trans.getDB();
//			pool = null;
		}
		else
		{
			db = trans.getProgram().getProp().getDB(dbName);
//			Object pool = trans.program.getProp().getPool(dbName);
//			try {
//				this.pool = (DspPool) pool;
//				db = (Database)this.pool.checkOut();
//			} catch (ClassCastException e) {
//				db = (Database)pool;
//				pool = null;
//			}
		}
		return sql;
	} // stripDB()
*/
/*
	public static java.sql.Date _Date(Object obj) throws DspParseException
	{
		if (obj instanceof java.sql.Date) return (java.sql.Date)obj;
		if (obj instanceof java.util.Date) return new java.sql.Date(((java.util.Date)obj).getTime());
		if (obj instanceof Number)
		{
			int ymd = ((Integer)obj).intValue();
			int day = ymd % 100;
			ymd /= 100;
			int month = ymd % 100 - 1;
			int year = ymd / 100;
			return new java.sql.Date(year, month, day);
		}
		throw new DspParseException("Date type", obj.getClass().getName());
	} // _Date()

	public static int _int(Object obj) throws DspParseException
	{
		if (obj instanceof Number) return ((Number)obj).intValue();
		try {
			return Integer.parseInt(obj.toString());
		} catch (NumberFormatException e) {
			throw new DspParseException("Integer type", obj.getClass().getName());
		}
	} // _int()

	public static Integer _Integer(Object obj) throws DspParseException
	{
		if (obj instanceof Integer) return (Integer)obj;
		if (obj instanceof Number) return new Integer(((Number)obj).intValue());
		try {
			return new Integer(obj.toString());
		} catch (NumberFormatException e) {
			throw new DspParseException("Integer type", obj.getClass().getName());
		}
	} // _Integer()
*/
	public String toString()
	{
		if (debug) ThreadState.logln(name + " => " + getRow());
		return Integer.toString(getRow());
	} // toString()
/*
	public static Time toTime(Object obj) throws DspParseException
	{
		if (obj instanceof Time) return (Time)obj;
		if (obj instanceof java.util.Date) return new Time(((java.util.Date)obj).getTime());
		if (obj instanceof Number)
		{
			int hms = ((Integer)obj).intValue();
			int secs = hms % 100;
			hms /= 100;
			int mins = hms % 100 - 1;
			int hrs = hms / 100;
			return new Time(hrs, mins, secs);
		}
		throw new DspParseException("Time type", obj.getClass().getName());
	} // toTime()
*/
	public void unset(String variable) throws DspException
	{
		if (trace) ThreadState.logln("DspStatement.unset(" + variable + ')');
		if (name.equals(DEBUG)) debug = false;
		else
		if (name.equals(TRACE)) trace = false;
		else throw new DspReadOnlyException(NAME, variable);
		if (debug) ThreadState.logln(name + " unset " + NAME + '.' + variable);
	} // unset()

} // DspStatement
