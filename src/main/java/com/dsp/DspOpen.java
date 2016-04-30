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

import java.sql.SQLException;
import java.util.*;					// Hashtable, Stack, Vector

import static com.dsp.util.BZCast._boolean;

/**
 * <p>Manager of all open statements.  Each statament that opens multiple rows is stored
 * as in a stack arrangement, with the most recently added being the first found.  Statements
 * will have a unique name assigned to them, if the programmer does not assign one using the
 * #name syntax.  Statements can be found in this object by using their name.</p>
 *
 * <p>This also keeps a cache of connections used during the request so DSP can reuse
 * the same connections.  This is a requirement for handling database transactions.  Connections
 * are freed once the request has completed.</p>
 */
public class DspOpen implements DspObject
{
	public static final String NAME	= "open";

	private boolean					debug, trace;
	private Connect					conn;
	private Database				db;
	private List<Connect>			transQue;			// connections with open transactions
	private List<DspStatement>		openStatements;		// statements that haven't closed yet
	private Map<String, Connect>	connections;		// connections used in this transaction
	private String					lastExecuted;		// last statement executed

	/**
	 * Closes all open statements, and rolls back all uncommitted transactions.
	 * Use when forwarding to an error handling page.  Used by release().
	 */
	public void abortAll()
	{
		if (openStatements != null)
		{
			int size = openStatements.size();
			while (size-- > 0)
			{
				DspStatement statement = openStatements.get(size);
				statement.close();
			}
			openStatements = null;
		}
		if (transQue != null)
		{
			while (transQue.size() > 0)
			{
				Connect conn = transQue.get(0);
				try {
					conn.rollbackTransaction();
					if (debug) ThreadState.logln("Rolled back the transaction");
				} catch (Exception e1) {
					if (debug) ThreadState.logln("Error: Could not roll back the transaction.  See server log for more details.");
					ThreadState.logln(e1);
				}
				while (transQue.remove(conn));	// remove all instances of this connection
			}
			transQue = null;
		}
	} // abortAll()

	void add(DspStatement statement)
	{
		if (trace) ThreadState.logln("DspOpen.add(" + statement + ')');
		if (statement == null) throw new NullPointerException("statement is null");
		if (openStatements == null) openStatements = new ArrayList<DspStatement>();
		openStatements.add(statement);
	} // add()

	/**
	 * Used by SQL Statements to notify this object of open transactions.  This is
	 * to ensure rollbacks in case the transactions are not committed.
	 */
	boolean beginTransaction(Connect conn)
	{
		if (trace) ThreadState.logln("DspOpen.beginTransaction()");
		boolean result = true;
		if (transQue == null) transQue = new ArrayList<Connect>();
		else if (transQue.contains(conn)) result = false;
		transQue.add(conn);
		if (debug) ThreadState.logln("Opening transaction");
		return result;
	} // beginTransaction()

	/**
	 * Used by SQL Statements to notify this object of committed transactions, so they
	 * will not be rolled back when the request completes.
	 */
	boolean endTransaction(Connect conn)
	{
		if (trace) ThreadState.logln("DspOpen.commitTransaction()");
		boolean result = false;
		if (debug) ThreadState.logln("Closing transaction");
		if (transQue != null)
		{
			transQue.remove(conn);
			result = !transQue.contains(conn);
		}
		return result;
	} // endTransaction()

	/**
	 * Returns true if the object (statament name) exists.
	 */
	public boolean exists(String name)
	{
		if (trace) ThreadState.logln("DspOpen.exists(" + name + ')');
		if (openStatements == null) return false;
		for (int ix = openStatements.size() - 1; ix >= 0; ix--)
		{
			DspStatement stmt = openStatements.get(ix);
			if (stmt.getName().equals(name)) return true;
		}
		return false;
	} // exists()

	/**
	 * Returns the named object (Statement) or null if not found.
	 * Statement names are search in reverse order of their opening, like a Stack.
	 */
	public Object get(String name)
	{
		if (trace) ThreadState.logln("DspOpen.get(" + name + ')');
		Object result = null;
		if (name.equals(DEBUG)) result = new Boolean(debug);
		else
		if (name.equals(TRACE)) result = new Boolean(trace);
		DspStatement stmt = null;
		if (openStatements != null)
		{
//ThreadState.logln(openStatements);
			for (int ix = openStatements.size() - 1; ix >= 0; ix--)
			{
				stmt = openStatements.get(ix);
				String stName = stmt.getName();
//ThreadState.logln(" name " + stName);
				if (name.equals(stName))
				{
					result = stmt;
					break;
				}
			}
		}
		if (debug) ThreadState.logln(NAME + '.' + name + " => " + result);
		return result;
	} // get()

	/**
	 * Returns the current connection.
	 */
	public Connect getConnection()
	{
		if (trace) ThreadState.logln("DspOpen.getConnection() -> " + conn);
		return conn;
	} // getConnection()

	/**
	 * Returns the connection associated with name.  If none is found in the list of used connections
	 * then the group is searched for the database and a new connection is checked out.  This does not
	 * modify the current stored connection in this object.
	 */
	public Connect getConnection(String name) throws DspException
	{
		if (connections == null) connections = new HashMap<String, Connect>();
		Connect conn = (Connect)connections.get(name);
		if (conn == null)
		{
			Database db = ThreadState.getProp().getDatabase(name);
			try {
				conn = db.checkOut();
			} catch (SQLException e) {
				throw new DspException(e);
			}
			connections.put(name, conn);
		}
		if (trace) ThreadState.logln("DspOpen.getConnection(" + name + ") -> " + conn);
		return conn;
	} // getConnection()

	/**
	 * Returns the connection associated with the database.  If none is found in the list of used
	 * connections then a new connection is checked out.  This does not modify the current stored
	 * connection in this object.
	 */
	public Connect getConnection(Database db) throws DspException
	{
		Connect conn = null;
		if (db != null)
		{
			if (connections == null) connections = new HashMap<String, Connect>();
			String name = db.getName();
			conn = connections.get(name);
			if (conn == null)
			{
				try {
					conn = db.checkOut();
				} catch (SQLException e) {
					throw new DspException(e);
				}
				connections.put(name, conn);
			}
		}
		if (trace) ThreadState.logln("DspOpen.getConnection(" + db + ") -> " + conn);
		return conn;
	} // getConnection()

	/**
	 * Returns the current database.
	 */
	public Database getDatabase() { return db; }

	/**
	 * Returns the last statement executed.
     * @see #setLastExecuted(String)
	 */
	public String getLastExecuted() { return lastExecuted; }

	/**
	 * Returns the last open statement, this should match the DSP row object.
	 */
	public DspStatement getRow()
	{
		int len;
		DspStatement result = null;
		if (openStatements != null && (len = openStatements.size()) > 0)
		{
			result = openStatements.get(len - 1);
		}
		if (trace) ThreadState.logln("DspOpen.getRow() -> " + result);
		return result;
	} // getRow()

	/**
	 * Returns the names of the open statements, in reverse order.
	 */
	public Iterator<String> names()
	{
		ArrayList<String> list = new ArrayList<String>(openStatements.size());
		for (int ix = openStatements.size() - 1; ix >= 0; ix--)
		{
			DspStatement stmt = openStatements.get(ix);
			list.add(stmt.getName());
		}
		return list.iterator();
	} // names()

	/**
	 * Releases this object, to make it available to another thread.  It will close all open statements
	 * and rollback all uncommitted transactions.
	 */
	public void release()
	{
		if (trace) ThreadState.logln("DspOpen.release()");
		abortAll();
		if (connections != null)
		{
			Iterator<Connect> it = connections.values().iterator();
			while (it.hasNext())
			{
				Connect conn = (Connect)it.next();
				conn.checkIn();
			}
			connections = null;
		}
	} // release()

	DspStatement remove(DspStatement statement)
	{
		if (trace) ThreadState.logln("DspOpen.remove(" + statement + ')');
		if (openStatements != null)
		{
			openStatements.remove(statement);
			int size = openStatements.size();
			if (size > 0) return openStatements.get(size - 1);
		}
		return null;
	} // remove()

	/**
	 * Set member to a value.  This only works for <b>debugMode</b> and <b>traceMode</b>.  All
	 * other members must me added by opening statement.
	 * @see DspPage#execute(String, Object)
	 */
	public void set(String variable, Object value) throws DspException, DspReadOnlyException
	{
		if (trace) ThreadState.logln("DspOpen.set(" + variable + ", " + value + ')');
		if (value == null)
		{
			unset(variable);
			return;
		}
		else
		if (variable.equals(DEBUG)) debug = _boolean(value);
		else
		if (variable.equals(TRACE)) trace = _boolean(value);
		else throw new DspReadOnlyException(NAME, variable);
		if (debug) ThreadState.logln(NAME + '.' + variable + " <= " + value);
	} // set()

	/**
	 * Sets the internal database and connection to the one associated with the name.
	 * This is used internally by the <b>db</b> command.
	 */
	public void setDatabase(String name) throws DspException
	{
		if (trace) ThreadState.logln("DspOpen.setDatabase(" + name + ')');
		Database old = this.db;
		this.conn = getConnection(name);
		this.db = conn.getDatabase();
		if (debug && old != db && db != null && old != null) ThreadState.logln("Switching to db: " + db.getName());
	} // setDatabase()

	/**
	* Sets the current database and connection.  This is used internally to set up the initial
	* database for each page.
	*/
	public void setDatabase(Database db) throws DspException
	{
		if (trace) ThreadState.logln("DspOpen.setDatabase(" + db + ')');
		Database old = this.db;
		this.db = db;
		this.conn = getConnection(db);
		if (debug && old != db && db != null && old != null) ThreadState.logln("Switching to db: " + db.getName());
	} // setDatabase()

	/**
	 * Records the last statement executed.
	 * @see #getLastExecuted()
	 */
	void setLastExecuted(String sql)
	{
		lastExecuted = sql;
	} // setLast()

	/**
	 * Commit the transaction so far, then reopen it.
	 */
	public void subcommit() throws DspException, SQLException
	{
		subcommit(0);
	} // subcommit()

	/**
	 * Commit the transaction so far, pause, then reopen it.
	 */
	public void subcommit(int msec) throws DspException, SQLException
	{
		Connect conn = transQue != null && transQue.size() > 0 ? (Connect)transQue.get(0) : null;
		if (conn != null)
		{
			conn.commitTransaction();
		}
		try { Thread.sleep(msec); } catch (InterruptedException e) {}
		if (conn != null)
		{
			conn.beginTransaction();
		}
	} // subcommit()

	/**
	 * Unset (remove) a member's value.  This only works with debugMode and traceMode, since other
	 * memebers are removed when the statement is closed, usually when an <b>else</b> or <b>end</b>
	 * command is executed.
	 */
	public void unset(String variable) throws DspException
	{
		if (trace) ThreadState.logln("DspOpen.unset(" + variable + ')');
		if (variable.equals(DEBUG)) debug = false;
		else
		if (variable.equals(TRACE)) trace = false;
		else throw new DspReadOnlyException(NAME, variable);
		if (debug) ThreadState.logln("unset " + NAME + '.' + variable);
	} // unset()

} // DspOpen
