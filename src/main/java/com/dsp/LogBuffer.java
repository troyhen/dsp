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

class LogBuffer extends OutputStream
{
	static final int DEFAULT_SIZE = 32768;
	static final String CTOP = "<html><body><hr><h2>Debug Log</h2><plaintext>";
	static byte[] BTOP;
	static final String CWRAPPED = "... log overflow deleted ...\r\n";
	static byte[] BWRAPPED;

	static {
		try {
			BTOP = CTOP.getBytes("ISO-8859-1");
			BWRAPPED = CWRAPPED.getBytes("ISO-8859-1");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
	}

	private Object				lock;
	private byte[]				buf = new byte[DEFAULT_SIZE];
	private int						index;
	private boolean				wrapped;

	LogBuffer()
	{
		lock = this;
	} // LogBuffer()

	LogBuffer(Object lock)
	{
		this.lock = lock;
	} // LogBuffer()

	/**
	 * Close this object.  It can no longer be used after this call.
	 */
	public void close()
	{
		synchronized (lock) {
			buf = null;
			index = 0;
			wrapped = false;
		}
	} // close()

	public void flush()
	{
		// Does nothing
	} // flush()

  /**
   * Flush the log to the stream.
   *
   * @exception  IOException  If an I/O error occurs
   */
  void flush(OutputStream out) throws IOException
  {
	  if (buf == null) throw new IOException("The log was closed");
  	synchronized (lock) {
			if ((wrapped || index > 0))
			{
//System.out.println("Debug Log");
				out.write(BTOP);
				if (wrapped)
				{
//System.out.write(buf, index, buf.length - index);
					out.write(BWRAPPED);
					out.write(buf, index, buf.length - index);
					wrapped = false;
				}
//System.out.write(buf, 0, index);
				out.write(buf, 0, index);
				index = 0;
				out.flush();
			}
		}
	} // flush()

  /**
   * Flush the log to the stream.
   *
   * @exception  IOException  If an I/O error occurs
   */
  void flush(Writer out) throws IOException
  {
	  if (buf == null) throw new IOException("The log was closed");
  	synchronized (lock) {
			if (wrapped || index > 0)
			{
//System.out.println("Debug Log");
				out.write(CTOP);
				if (wrapped)
				{
					out.write(CWRAPPED);
					for (int ix = index; ix < DEFAULT_SIZE; ix++)
					{
//System.out.write(buf[ix]);
						out.write(buf[ix]);
//					out.write(buf, index, buf.length - index);
					}
					wrapped = false;
				}
				for (int ix = 0; ix < index; ix++)
				{
//System.out.write(buf[ix]);
					out.write(buf[ix]);
				}
//			out.write(buf, 0, index);
				index = 0;
				out.flush();
			}
		}
	} // flush()

  void reset()
	{
		synchronized (lock) {
			index = 0;
			wrapped = false;
		}
	} // reset()

	public String toString()
	{
		if (buf == null) throw new RuntimeException("The log was closed");
		StringBuffer buffer = new StringBuffer(wrapped ? buf.length + CWRAPPED.length() : index);
  	synchronized (lock) {
			if (wrapped || index > 0)
			{
//System.out.println("Debug Log");
				if (wrapped)
				{
					buffer.append(CWRAPPED);
					buffer.append(new String(buf, index, buf.length - index));
				}
				buffer.append(new String(buf, 0, index));
			}
		}
		return buffer.toString();
	} // toString()

	/**
	 * Write a single character.  The character to be written is contained in
	 * the 16 low-order bits of the given integer value; the 16 high-order bits
	 * are ignored.
	 */
	public void write(int c) throws IOException
	{
		if (buf == null) throw new IOException("The log was closed");
		synchronized (lock) {
			c = (char)c;
			if (c < 256)
			{
				buf[index++] = (byte)c;
				if (index >= DEFAULT_SIZE)
				{
					index = 0;
					wrapped = true;
				}
			}
			else
			{
				write('&');
				write('#');
				int d5 = c / 10000;
				if (d5 > 0) write(d5 + '0');
				int d4 = c / 1000 % 10;
				if (d4 > 0) write(d4 + '0');
				int d3 = c / 100 % 10;
				if (d3 > 0) write(d3 + '0');
				int d2 = c / 10 % 10;
				if (d2 > 0) write(d2 + '0');
				write((c % 10) + '0');
				write(';');
			}
		}
	} // write()

	/**
	 * Write a portion of an array of characters.
	 *
	 * @param  cbuf  Array of characters
	 * @param  off   Offset from which to start writing characters
	 * @param  len   Number of characters to write
	 */
	public void write(byte buf[], int off, int len) throws IOException
	{
		if (buf == null) throw new IOException("The log was closed");
		synchronized (lock) {
			for (; off < len; off++)
			{
				write(buf[off]);
			}
		}
	} // write()

} // LogBuffer

