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

class LogStream extends OutputStream
{
	private ServletContext	context;
	private Object					lock;
	private StringBuffer		buf = new StringBuffer();

	LogStream(ServletContext context)
	{
		this.context = context;
		this.lock = this;
	} // LogStream()

	LogStream(ServletContext context, Object lock)
	{
		this.context = context;
		this.lock = lock;
	} // LogStream()

	/**
	 * Close this object.  It can no longer be used after this call.
	 */
	public void close()
	{
		synchronized (lock) {
			context = null;
		}
	} // close()

	public void flush()
	{
		if (buf.length() > 0)
		{
			context.log(buf.toString());
			buf.setLength(0);
		}
	} // flush()

	/**
	 * Write a single character.  The character to be written is contained in
	 * the 16 low-order bits of the given integer value; the 16 high-order bits
	 * are ignored.
	 */
	public void write(int c)
	{
		synchronized (lock) {
			buf.append((char)c);
			if (c == '\r' || c == '\n') flush();
		}
	} // write()

	/**
	 * Write a portion of an array of characters.
	 *
	 * @param  cbuf  Array of characters
	 * @param  off   Offset from which to start writing characters
	 * @param  len   Number of characters to write
	 */
	public void write(byte buf[], int off, int len)
	{
		synchronized (lock) {
			for (; off < len; off++)
			{
				write(buf[off]);
			}
		}
	} // write()

} // LogBuffer

