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

public interface Database extends DspObject
{
	public static final String GET_USERNAME	= "username";
	public static final String GET_PASSWORD	= "password";
	public static final String GET_DRIVER		= "driver";
	public static final String GET_DATABASE	= "database";
	public static final String GET_TIMEOUT	= "timeout";
	public static final String INIT_TIMEOUT	= "dbtimeout";

	/** Return a connection back to the pool.  If there are too many connections
	 *  it will periodically close a few of them.
	 */
	public void checkIn(Connect conn);

	/** Get a connection to the database.  If there are none available, and I haven't
	 *  created too many then it will create a few more connections, and return one of them.
	 */
	public Connect checkOut() throws DspException, SQLException;

	public void close();

	public boolean getDebug();
	public String getDriver();
	public String getDatabase();
	public String getName();
	public String getUsername();
	public String getPassword();
	public boolean getTrace();
	public int getTimeout();

	public void reconnect();

} // Database

