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

import java.io.*;
import javax.servlet.ServletContext;

public class ServletLog extends PrintStream
{
	public static final String NAME = "log";

	private static LogStream log;

	public ServletLog(ServletContext context)
	{
		super(log = new LogStream(context));
	} // ServletLog()

	/**
	 * Flush the log to the stream.
	 *
	 * @exception  IOException  If an I/O error occurs
	 */
	public void flush(OutputStream stream) throws IOException
	{
		flush();
		log.flush();
	} // flush()

	/**
	 * Flush the log to the stream.
	 *
	 * @exception  IOException  If an I/O error occurs
	 */
	public void flush(Writer stream) throws IOException
	{
		flush();
		log.flush();
	} // flush()

} // ServletLog

