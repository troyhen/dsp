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
import java.util.*;	// Hashtable, Properties

public class DspConnect implements Connect
{
    private static String initStatement = null;
	public static final int MINUTES = 1000 * 60;

	private Database			db;
	private Connection		connection;
	private long					nextTime = Long.MIN_VALUE;

	public DspConnect(Database db) throws DspException, SQLException
	{
		this.db = db;
		if (db.getTrace()) ThreadState.logln("DspConnect(" + db.getDriver() + ", " + db.getDatabase() + ')');
		getConnection();
	} // DspConnect()

	public void beginTransaction() throws DspException, SQLException
	{
		if (db.getTrace()) ThreadState.logln("DspConnect.beginTransaction()");
		Statement statement = null;
		try {
			statement = getConnection().createStatement();
			String sql = "begin transaction";
			if (db.getDebug()) ThreadState.logln("Executing: " + sql);
			statement.execute(sql);
		} finally {
			if (statement != null) try { statement.close(); } catch (SQLException e2) {}
		}
	} // beginTransaction()

	public void close()
	{
		if (db.getTrace()) ThreadState.logln("DspConnect.close()");
		if (connection != null)
		{
			try { connection.close(); } catch (SQLException e) {}
			connection = null;
		}
	} // close()

	public void checkIn() { db.checkIn(this); }

	public void commitTransaction() throws DspException, SQLException
	{
		if (db.getTrace()) ThreadState.logln("DspConnect.commitTransaction()");
		Statement statement = null;
		try {
			statement = getConnection().createStatement();
			String sql = "commit transaction";
			if (db.getDebug()) ThreadState.logln("Executing: " + sql);
			statement.execute(sql);
		} finally {
			if (statement != null) try { statement.close(); } catch (SQLException e2) {}
		}
	} // commitTransaction()

	public boolean exists(String name) throws DspException
	{
		return db.exists(name);
	} // exists()

	public void finalize()
	{
		close();
	} // finalize()

	public Object get(String name) throws DspException
	{
		return db.get(name);
	} // get()

	public synchronized Connection getConnection() throws DspException, SQLException
	{
		update();
		if (connection == null)
		{
			try {
					// I have to (re)register drivers for every servlet instance since
					// each servlet (under ServeletExec) runs in a different security context
					// and JDBC only connects to drivers under the current security context.
					//  This means the drivers may be registered more than once, but this
					// isn't a problem since JDBC uses a Vector and not a Hashtable.
				Class<?> c = Class.forName(db.getDriver());
				DriverManager.registerDriver((Driver) c.newInstance());
				if (db.getDebug()) ThreadState.logln("loaded(" + db.getDriver() + ")");
			} catch (Exception e) {
				throw new DspException("Couldn't load database driver " + db.getDriver(), e);
			}
			String database = db.getDatabase();
			if (!database.toLowerCase().startsWith("jdbc:"))
			{
				database = "jdbc:" + database;
			}
			if (db.getTrace()) ThreadState.logln("DriverManager.getConnection(" + database + ", "
					+ db.getUsername() + ", " + db.getPassword() + ")");
			connection = DriverManager.getConnection(database, db.getUsername(), db.getPassword());
            if (initStatement != null && initStatement.length() > 0) {
                try {
                    Statement stmt = connection.createStatement();
                    stmt.executeUpdate(initStatement);
                    stmt.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
		}
		return connection;
	} // getConnection()

	public Database getDatabase() { return db; }

	public Iterator<String> names() { return db.names(); }

	public synchronized void reconnect()
	{
		if (db.getTrace()) ThreadState.logln("DspConnect.reconnect()");
		close();
		db.reconnect();
	} // reconnect()

	void release()
	{
		db.checkIn(this);
	} // release()

	public void rollbackTransaction() throws DspException, SQLException
	{
		if (db.getTrace()) ThreadState.logln("DspConnect.rollbackTransaction()");
		Statement statement = null;
		try {
			statement = getConnection().createStatement();
			String sql = "rollback transaction";
			if (db.getDebug()) ThreadState.logln("Executing: " + sql);
			statement.execute(sql);
		} finally {
			if (statement != null) try { statement.close(); } catch (SQLException e2) {}
		}
	} // rollbackTransaction()

	public void set(String variable, Object value) throws DspException
	{
		db.set(variable, value);
	} // set()

    public static void setInit(String sql) {
        initStatement = sql;
    } // setInit()

	public String toString()
	{
		return ('[' + db.getName() + " Connection]");
	} // toString()

	public void unset(String variable) throws DspException
	{
		db.unset(variable);
	} // unset()

	synchronized void update()
	{
		int timeout = db.getTimeout() * MINUTES;
		if (timeout <= 0) return;
		if (System.currentTimeMillis() > nextTime && connection != null)
		{
			if (db.getDebug()) ThreadState.logln(db.getName() + " connection timeout, reconnecting");
			close();
		}
		nextTime = System.currentTimeMillis() + timeout;
	} // update()

} // DspConnect
