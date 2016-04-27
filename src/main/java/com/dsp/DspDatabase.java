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

import com.dsp.servlet.DspServlet;

import java.sql.SQLException;
import java.util.*;	// Hashtable, Properties

public class DspDatabase implements Database
{
	static final String DEF_USERNAME	= "";
	static final String DEF_PASSWORD	= "";
	static final String DEF_DRIVER		= null;
	static final String DEF_DATABASE	= null;
	static final String DEF_TIMEOUT	= null;

	static final int PERIOD = 1000 * 60 * 10;	// Time between checks for too many connection
	static final int MAX_CONNS = 40;
	static final int STEP_BY = 8;

	String		name;
	String 		driver, database, username, password;
	boolean		trace, debug;

	private Pool	pool;	// = new Pool();
	private int		built;	// count of connections built
	private long	nextTime = Long.MIN_VALUE;
	private int		timeout;

	DspDatabase(String name, DspProp prop, DspServlet servlet) throws DspException
	{
		try {	// init timeout from servlet params
			timeout = DspPage._int(servlet.get(INIT_TIMEOUT));
		} catch (Exception e) {
			timeout = 0;
		}
		if (name == null)
		{
			this.name = "database";
			set(GET_DRIVER, prop.get(GET_DRIVER, DEF_DRIVER));
			set(GET_DATABASE, prop.get(GET_DATABASE, DEF_DATABASE));
			set(GET_USERNAME, prop.get(GET_USERNAME, DEF_USERNAME));
			set(GET_USERNAME, prop.get(GET_PASSWORD, DEF_PASSWORD));
			set(GET_TIMEOUT, prop.get(GET_TIMEOUT, DEF_TIMEOUT));
		}
		else
		{
			this.name = name;
		}
		prop.preSet(this, this.name);
		prop.preSet(this, DspOpen.NAME);	// get open.debug and open.trace
		if (driver == null) throw new DspException(name + ".driver property must be set");
		if (database == null) throw new DspException(name + ".database property must be set");
		pool = new Pool(this.name);
		if (trace) ThreadState.logln("DspDatabase(" + this.name + ", " + prop + ')');
	} // DspDatabase()

	/** Return a connection back to the pool.  If there are too many connections
	 *  it will periodically close a few of them.
	 */
	public synchronized void checkIn(Connect conn)
	{
		if (trace) ThreadState.logln("DspDatabase.checkIn(" + conn + ')');
		pool.checkIn(conn);
		long now;
		int avail = pool.available();
			// Periodically check if there are too many available connections.
			// If so, delete some of them but never all of them.
		if (avail > STEP_BY * 2 && (now = System.currentTimeMillis()) >= nextTime)
		{
			nextTime = now + PERIOD;
			int target = built - STEP_BY;
			while (built > target)
			{
				conn = (Connect)pool.checkOut();
				conn.close();
				built--;
			}
			if (debug) ThreadState.logln(name + " connection checkIn: " + built + " built");
		}
		if (debug) ThreadState.logln(name + " connection checkIn: " + avail + " available");
	} // checkIn()

	/** Get a connection to the database.  If there are none available, and I haven't
	 *  created too many then it will create a few more connections, and return one of them.
	 */
	public synchronized Connect checkOut() throws DspException, SQLException
	{
		if (trace) ThreadState.logln("DspDatabase.checkOut()");
		int avail = pool.available();
		if (avail == 0 && built < MAX_CONNS)
		{		// create a fixed number of connections, but abort some of them if it takes too long
			long timeout = System.currentTimeMillis() + 4 * 1000;
			while (System.currentTimeMillis() < timeout && avail < STEP_BY)
			{
				pool.checkIn(new DspConnect(this));
				built++;
				avail++;
			}
			if (debug) ThreadState.logln(name + " connection checkOut: " + built + " built");
		}
		Connect conn = (Connect)pool.checkOut();
		if (debug) ThreadState.logln(name + " connection checkOut: " + avail + " available");
		return conn;
	} // checkOut()

	public void close()
	{
		if (trace) ThreadState.logln("DspDatabase.close()");
			// This closes all checked in connections.  I can't close non-checked in connections,
			// because they may never get checked in the case of a blocked or eternally looped thread.
		int avail = pool.available();
		if (debug) System.out.println(name + " database close: closing " + avail + " connections");
		while (avail > 0)
		{
			Connect conn = (Connect)pool.checkOut();
			conn.close();
			built--;
			avail--;
		}
		if (built > 0) System.out.println(built + " connections were in-use, and could not be clcosed");
	} // close()

	public boolean exists(String name)
	{
		if (trace) ThreadState.logln("DspDatabase.exists(" + name + ')');
		boolean result = false;
		if (name.equals(GET_USERNAME) && username != null) result = true;
		else
		if (name.equals(GET_PASSWORD) && password != null) result = true;
		else
		if (name.equals(GET_DRIVER) && driver != null) result = true;
		else
		if (name.equals(GET_DATABASE) && database != null) result = true;
		else
		if (name.equals(TRACE) || name.equals(DEBUG)) result = true;
		if (debug) ThreadState.logln(this.name + '.' + name + (result ? " exists" : " does not exist"));
		return result;
	} // exists()

	public void finalize()
	{
		close();
	} // finalize()

	public Object get(String name)
	{
		if (trace) ThreadState.logln("DspDatabase.get(" + name + ')');
		Object result = null;
		if (name.equals(GET_USERNAME)) result = username;
		else
		if (name.equals(GET_PASSWORD)) result = password;
		else
		if (name.equals(GET_DRIVER)) result = driver;
		else
		if (name.equals(GET_DATABASE)) result = database;
		else
		if (name.equals(GET_TIMEOUT)) result = new Integer(timeout);
		else
		if (name.equals(TRACE)) result = new Boolean(trace);
		else
		if (name.equals(DEBUG)) result = new Boolean(debug);
		if (debug) ThreadState.logln(this.name + '.' + name + " => " + result);
		return result;
	} // get()

	int getConnections()
	{
		if (trace) ThreadState.logln("DspDatabase.getConnections()");
		return built;
	} // getConnections()

	public String getDatabase() { return database; }
	public boolean getDebug() { return debug; }
	public String getDriver() { return driver; }

	public String getName()
	{
		if (trace) ThreadState.logln("DspDatabase.getName()");
		return name;
	} // getName()

	public String getPassword() { return password; }
	public int getTimeout() { return timeout; }
	public boolean getTrace() { return trace; }
	public String getUsername() { return username; }

	public Iterator<String> names()
	{
		ArrayList<String> list = new ArrayList<String>();
		list.add(GET_USERNAME);
		list.add(GET_PASSWORD);
		list.add(GET_DRIVER);
		list.add(GET_DATABASE);
		list.add(GET_TIMEOUT);
		return list.iterator();
	} // names()

	public synchronized void reconnect()
	{
		if (trace) ThreadState.logln("DspDatabase.reconnect()");
		int avail = pool.available();
		if (debug) ThreadState.logln(name + " database reconnect: closing " + avail + " connections");
		while (avail > 0)
		{
			Connect conn = (Connect)pool.checkOut();
			conn.close();
			built--;
			avail--;
		}
	} // reconnect()

	public void set(String variable, Object value) throws DspException
	{
		if (trace) ThreadState.logln("DspDatabase.set(" + variable + ", " + value + ')');
		if (value == null)
		{
			unset(variable);
			return;
		}
		else
		if (variable.equals(GET_USERNAME))
		{
			username = DspPage._String(value);
			if (built > 0) reconnect();
		}
		else
		if (variable.equals(GET_PASSWORD))
		{
			password = DspPage._String(value);
			if (built > 0) reconnect();
		}
		else
		if (variable.equals(GET_DRIVER))
		{
			driver = DspPage._String(value);
			if (built > 0) reconnect();
		}
		else
		if (variable.equals(GET_DATABASE))
		{
			database = DspPage._String(value);
			if (built > 0) reconnect();
		}
		else
		if (variable.equals(GET_TIMEOUT))
		{
			timeout = DspPage._int(value);
		}
		else
		if (variable.equals(TRACE)) try { trace = DspPage._boolean(value); } catch (NumberFormatException e) {}
		else
		if (variable.equals(DEBUG)) try { debug = DspPage._boolean(value); } catch (NumberFormatException e) {}
		else throw new DspReadOnlyException(this.name, variable);
		if (debug) ThreadState.logln(this.name + '.' + variable + " <= " + value);
	} // set()

//	public void setPool(Pool pool) { this.pool = pool; }

	public String toString()
	{
		return ('[' + name + " Database]");
	} // toString()

	public void unset(String variable) throws DspException
	{
		if (trace) ThreadState.logln("DspDatabase.unset(" + variable + ')');
		if (variable.equals(GET_USERNAME))
		{
			username = null;
			if (built > 0) reconnect();
		}
		else
		if (variable.equals(GET_PASSWORD))
		{
			password = null;
			if (built > 0) reconnect();
		}
		else
		if (variable.equals(GET_DRIVER))
		{
			driver = null;
			if (built > 0) reconnect();
		}
		else
		if (variable.equals(GET_DATABASE))
		{
			database = null;
			if (built > 0) reconnect();
		}
		else
		if (variable.equals(GET_TIMEOUT)) timeout = 0;
		else
		if (variable.equals(TRACE)) trace = false;
		else
		if (variable.equals(DEBUG)) debug = false;
		else throw new DspReadOnlyException(this.name, variable);
		if (debug) ThreadState.logln("unset " + this.name + '.' + variable);
	} // unset()

} // DspDatabase

