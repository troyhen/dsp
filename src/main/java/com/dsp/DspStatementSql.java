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

import java.sql.*;	// DriverManager, Connection, ...

import static com.dsp.util.BZCast._String;

public class DspStatementSql extends DspStatement
{
	public static final String NAME = "sql";
	public static final int TRANSACTION = DONE + 1;

//private static int openCount;

	private Statement			statement;
	private ResultSet			results;
	private ResultSetMetaData	meta;
	private Connect				conn;
	private boolean				transaction, gotFirst;

	public DspStatementSql(String name, boolean debug, boolean trace) throws DspException
	{
		super(name, debug, trace);
		if (trace) ThreadState.logln("DspStatementSql(" + name + ")");
	} // DspStatementSql()

	public DspStatement close()
	{
		if (trace) ThreadState.logln("DspStatementSql.close()");
		DspOpen open = ThreadState.getOpen();
		if (transaction && ThreadState.getRequest().getAttribute(DspPageContext.THROWN) == null)
		{
			if (open.endTransaction(conn))
			{
				try {
					statement = conn.getConnection().createStatement();
					String sql = "commit transaction";
					if (debug) ThreadState.logln(name + " Executing: " + sql);
					statement.execute(sql);
					transaction = false;
				} catch (Exception e) {
					ThreadState.logln(name + " Couldn't commit transaction");
					ThreadState.logln(e);
				} finally {
					if (statement != null)
					{
						try {statement.close(); } catch (SQLException e) {}
						statement = null;
					}
				}
			}
		}
		meta = null;
		conn = null;
		if (results != null)
		{
			try { results.close(); } catch (SQLException e) {}
			results = null;
		}
		if (statement != null)
		{
			try {
//ThreadState.logln("Close: " + (--openCount) + " statements open");
				statement.close();
			} catch (SQLException e) {}
			statement = null;
		}
		return super.close();
	} // close()

	public boolean exists0(int index) throws SQLException
	{
		try {
			Object obj = getObject0(index);
			return obj != null;
		} catch (SQLException e) {
		}
		return false;
	} // exists0()

	public boolean exists0(String variable) throws SQLException
	{
		try {
			Object obj = getObject0(variable);
			return obj != null;
		} catch (SQLException e) {
		}
		return false;
	} // exists0()

	protected int execute0(Object obj)
			throws DspException, SQLException
	{
		if (trace) ThreadState.logln("DspStatementSql.execute(" + obj + ")");
		String sql = stripDB(obj);
		if (sql == null || sql.length() == 0) throw new DspException("Empty statement");

		String sqlLower = sql.toLowerCase();
		int b1 = sqlLower.indexOf("begin");
		int b2 = sqlLower.indexOf("transaction");
		if (b2 >= 0 || b1 >= 0)
		{
			int b3 = sqlLower.indexOf("commit");
			int b4 = sqlLower.indexOf("rollback");
			if (b3 == 0 || b4 == 0)
			{
				if (!ThreadState.getOpen().endTransaction(conn))
				{
					return 0; // Early return
				}
			}
			else if (b1 == 0 || b2 == 0)
			{
				transaction = true;
				if (!ThreadState.getOpen().beginTransaction(conn))
				{
					return -2;		// Early return
				}
				sql = "begin transaction";
			}
		}

//ThreadState.logln("Open: " + (++openCount) + " statements open");
		boolean hasResults = false;
		for (int tries = 0; ; tries++)
		{
			try {
				statement = null;
				statement = conn.getConnection().createStatement();
				hasResults = statement.execute(sql);
				break;
			}	catch (SQLException e) {
				if (statement != null) try { statement.close(); } catch (SQLException e1) {}
				String msg = e.getMessage().toLowerCase();
				if (tries < 2) {
					if (msg.indexOf("connection reset") >= 0
							|| msg.indexOf("socket write error") >= 0
							|| msg.indexOf("connection is closed") >= 0) {
						conn.reconnect();
						continue;
					}
				}
				throw e;
			}
		}
		int row = DONE;
		if (hasResults)
		{
			results = statement.getResultSet();
			if (!results.next())
			{		// If there's no first row, then I close the results and report that I'm done
					// (Did you know that it is possible to get a result set without any rows?)
				try { results.close(); } catch (SQLException e) {}
				results = null;
			}
			else
			{
				row = 0;
				gotFirst = true;
				if (debug)
				{
					ThreadState.log(name + " Columns:");
					getMetaData();
					for (int ix = 1, end = meta.getColumnCount(); ix <= end; ix++)
					{
						ThreadState.log(" " + meta.getColumnName(ix));
					}
					ThreadState.logln("");
				}
			}
//			if (!results.next())
//			{
//				conn = null;
//				try { results.close(); } catch (SQLException e) {}
//				results = null;
////ThreadState.logln("Close: " + (--openCount) + " statements open");
//				try { statement.close(); } catch (SQLException e) {}
//				statement = null;
//				row = 0;
//			}
		}
		else
		{
			row = statement.getUpdateCount();
//ThreadState.logln("Close: " + (--openCount) + " statements open");
			try { statement.close(); } catch (SQLException e) {}
			statement = null;

//			if (pool != null) pool.checkIn(conn);
//			pool = null;
			if (!transaction) conn = null;
//			if (row < 0) row = 0;	// negative values mean other things, so I can't allow them
			if (debug) ThreadState.logln(name + " " + row + " rows affected");
		}
		return transaction ? TRANSACTION : row;
	} // execute()

	public String getColumnName(int index) throws SQLException
	{
		getMetaData();
		if (meta == null) return null;
		return meta.getColumnName(index);
	} // getColumnName()

	public ResultSetMetaData getMetaData() throws SQLException
	{
		if (meta == null && results != null) meta = results.getMetaData();
		return meta;
	} // getMetaData()

	protected Object getObject0(int index) throws SQLException
	{
		if (trace) ThreadState.logln("DspStatementSql.getObject(" + index + ")");
		if (results == null) return null;
		return results.getObject(index);
	} // getObject()

	protected Object getObject0(String name) throws SQLException
	{
		if (trace) ThreadState.logln("DspStatementSql.getObject(" + name + ")");
		if (results == null) return null;
		return results.getObject(name);
	} // getObject0()

	public ResultSet getResultSet()
	{
		return results;
	} // getResultSet()

	public boolean hasResults()
	{
		return results != null;
	} // hasResults()

	protected boolean next0() throws SQLException
	{
		if (trace) ThreadState.logln("DspStatementSql.next0()");
		if (results == null) return false;
		if (gotFirst)
		{		// since I've already read the first row I don't need to do it again
			gotFirst = false;
			return true;
		}
		boolean result = results.next();
		if (!result)
		{
			results.close();
			results = null;
		}
		return result;
	} // next()
/*
	private String escapeSpecials(String sql)
	{
		String result = sql;
		result = DspProgram.replace(result, "\\", "\\\\");
		result = DspProgram.replace(result, "\"", "\\\"");
		result = DspProgram.replace(result, "'", "\\'");
		return result;
	} // escapeSpecials()
*/
	private String stripDB(Object obj) throws DspException
	{
		if (trace) ThreadState.logln("DspStatementSql.stripDB(" + obj + ')');
		if (obj == null) return null;
		String sql = _String(obj).trim();
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
		DspOpen open = ThreadState.getOpen();
		if (dbName == null)
		{
			conn = open.getConnection();
		}
		else
		{
			conn = open.getConnection(dbName);
			if (debug) ThreadState.logln(name + " Using db: " + (conn == null ? "null" : conn.getDatabase().getName()));
		}
		if (conn == null) throw new DspException("No database could be found. Check your properties file.");

		return sql;
	} // stripDB()

} // DspStatementSql

