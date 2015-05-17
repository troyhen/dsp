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

/**
 * <p>This class handles the collection and output of debugging information.  All debug
 * data printed to this object is held in a fixed sized buffer.  If the buffer fills
 * then older data will be thrown away to make room for new data.  The contents of the
 * log are appended to the bottom web page after the requested page has completed.
 * This is different from using System.out.println() or application.log() in that those
 * write to a servlet engine specific log, which must be viewed via the servlet engine
 * admin pages.  The log produced by using this object are directly visible at the bottom
 * of the requested page.</p>
 *
 * <p>When you use debugMode or traceMode in the WEB-INF/dsp.properties file the DSP
 * object use this log for their output.</p>
 */
public class DebugLog extends PrintStream
{
	public static final String NAME = "debugLog";

	private LogBuffer buf;

	/** Create a buffered debug log.  DSP creates this automatically for each page.
	 */
	public DebugLog(LogBuffer buf)
	{
		super(buf);
		this.buf = buf;
	} // DebugLog()

	/**
	 * Flush the log to the stream.
	 *
	 * @exception  IOException  If an I/O error occurs
	 */
	public void flush(OutputStream stream) throws IOException
	{
		flush();
		buf.flush(stream);
	} // flush()

	/**
	 * Flush the log to the stream.
	 *
	 * @exception  IOException  If an I/O error occurs
	 */
	public void flush(Writer stream) throws IOException
	{
		flush();
		buf.flush(stream);
	} // flush()

	/**
	 * Resets the log stream.
	 */
  public void reset()
	{
		buf.reset();
	} // reset()

	public String toString()
	{
		return buf.toString();
	} // toString()

} // DebugLog

