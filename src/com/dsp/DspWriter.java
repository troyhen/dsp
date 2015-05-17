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
import javax.servlet.http.HttpServletResponse;
import javax.servlet.jsp.JspWriter;

public class DspWriter extends JspWriter
{
	public static final String NAME = "out";
	public static final int DEFAULT_SIZE = 32768;

	private boolean				grow;
	private HttpServletResponse response;
	private OutputStream	out;
	private byte[]				buf;
	private int						index;
	private boolean				flushed;

	public DspWriter(int buffer, boolean autoflush, HttpServletResponse res)
	{
		super(buffer, autoflush);
		grow = buffer == UNBOUNDED_BUFFER;
		if (buffer == DEFAULT_BUFFER || grow) buffer = DEFAULT_SIZE;
		if (buffer != NO_BUFFER) buf = new byte[buffer];
		this.response = res;
	} // DspWriter

  public void clear() throws IOException
	{
		synchronized (lock) {
			if (flushed) throw new IOException("Buffer was previosly flushed");
			index = 0;
		}
	} // clear()

  public void clearBuffer() throws IOException
	{
		synchronized (lock) {
			index = 0;
		}
	} // clearBuffer()

	/**
	 * Close the stream, flushing it first.  Once a stream has been closed,
	 * further write() or flush() invocations will cause an IOException to be
	 * thrown.  Closing a previously-closed stream, however, has no effect.
	 *
	 * @exception  IOException  If an I/O error occurs
	 */
	public synchronized void close() throws IOException
	{
		synchronized (lock) {
			if (!flushed)
			{
				response.setContentLength(index);
			}
			flush();
			out.close();
			response = null;
			out = null;
			buf = null;
			index = 0;
		}
	} // close()

  /**
   * Flush the stream.  If the stream has saved any characters from the
   * various write() methods in a buffer, write them immediately to their
   * intended destination.  Then, if that destination is another character or
   * byte stream, flush it.  Thus one flush() invocation will flush all the
   * buffers in a chain of Writers and OutputStreams.
   *
   * @exception  IOException  If an I/O error occurs
   */
  public void flush() throws IOException
  {
  	synchronized (lock) {
			OutputStream out = getOut();
			if (index > 0 && buf != null)
			{
				out.write(buf, 0, index);
				index = 0;
			}
			out.flush();
			flushed = true;
		}
	} // flush()

	private OutputStream getOut() throws IOException
	{
		synchronized (lock) {
			if (out == null) out = response.getOutputStream();
		}
		return out;
	} // getOut()

  public int getRemaining()
	{
		synchronized (lock) {
			return buf == null ? 0 : buf.length - index;
		}
	} // getRemaining()

  public void newLine() throws IOException
	{
		synchronized (lock) {
			write('\r');
			write('\n');
		}
	} // newLine()

  public void print(boolean p0) throws IOException
	{
		synchronized (lock) {
			print(String.valueOf(p0));
		}
	} // print(boolean)

  public void print(char p0) throws IOException
	{
		synchronized (lock) {
			write(p0);
		}
	} // print(char)

  public void print(int p0) throws IOException
	{
		synchronized (lock) {
			print(String.valueOf(p0));
		}
	} // print(int)

  public void print(long p0) throws IOException
	{
		synchronized (lock) {
			print(String.valueOf(p0));
		}
	} // print(long)

  public void print(float p0) throws IOException
	{
		synchronized (lock) {
			print(String.valueOf(p0));
		}
	} // print(float)

  public void print(double p0) throws IOException
	{
		synchronized (lock) {
			print(String.valueOf(p0));
		}
	} // print(double)

  public void print(char[] p0) throws IOException
	{
		synchronized (lock) {
			if (p0 == null) write("null", 0, 4);
			else write(p0, 0, p0.length);
		}
	} // print(char[])

  public void print(String p0) throws IOException
	{
		synchronized (lock) {
			if (p0 == null) write("null", 0, 4);
			else write(p0, 0, p0.length());
		}
	} // print(String)

  public void print(Object p0) throws IOException
	{
		synchronized (lock) {
			if (p0 == null) write("null", 0, 4);
			else print(p0.toString());
		}
	} // print(Object)

  public void println() throws IOException
 	{
		synchronized (lock) {
			newLine();
		}
	} // println()

	public void println(boolean p0) throws IOException
	{
		synchronized (lock) {
			println(String.valueOf(p0));
		}
	} // println(boolean)

  public void println(char p0) throws IOException
	{
		synchronized (lock) {
			write(p0);
			newLine();
		}
	} // println(char)

  public void println(int p0) throws IOException
	{
		synchronized (lock) {
			println(String.valueOf(p0));
		}
	} // println(int)

  public void println(long p0) throws IOException
	{
		synchronized (lock) {
			println(String.valueOf(p0));
		}
	} // println(long)

  public void println(float p0) throws IOException
	{
		synchronized (lock) {
			println(String.valueOf(p0));
		}
	} // println(float)

  public void println(double p0) throws IOException
	{
		synchronized (lock) {
			println(String.valueOf(p0));
		}
	} // println(double)

  public void println(char[] p0) throws IOException
	{
		synchronized (lock) {
			if (p0 == null) write("null\r\n", 0, 6);
			else write(p0, 0, p0.length);
		}
	} // println(char[])

  public void println(String p0) throws IOException
	{
		synchronized (lock) {
			if (p0 == null) write("null\r\n", 0, 6);
			else
			{
				write(p0, 0, p0.length());
				newLine();
			}
		}
	} // println(String)

  public void println(Object p0) throws IOException
	{
		synchronized (lock) {
			if (p0 == null) write("null\r\n", 0, 6);
			else println(p0.toString());
		}
	} // println(Object)

	/**
	 * Write a single character.  The character to be written is contained in
	 * the 16 low-order bits of the given integer value; the 16 high-order bits
	 * are ignored.
	 *
	 * @exception  IOException  If an I/O error occurs
	 */
	public void write(int c) throws IOException
	{
		synchronized (lock) {
			if (buf == null)
			{
				getOut().write(c);
				return;
			}
			c = (char)c;
			for (;;)
			{
				int ix = index;
				try {
					if (c < 256) buf[ix++] = (byte)c;
					else
					{
						buf[ix++] = (byte)'&';
						buf[ix++] = (byte)'#';
						int d5 = c / 10000;
						if (d5 > 0) buf[ix++] = (byte)(d5 + '0');
						int d4 = c / 1000 % 10;
						if (d4 > 0) buf[ix++] = (byte)(d4 + '0');
						int d3 = c / 100 % 10;
						if (d3 > 0) buf[ix++] = (byte)(d3 + '0');
						int d2 = c / 10 % 10;
						if (d2 > 0) buf[ix++] = (byte)(d2 + '0');
						buf[ix++] = (byte)((c % 10) + '0');
						buf[ix++] = (byte)';';
					}
					index = ix;
					break;
				} catch (ArrayIndexOutOfBoundsException e) {
					if (grow)
					{
						int oldSize = buf.length;
						int newSize = oldSize < DEFAULT_SIZE
								? oldSize + DEFAULT_SIZE : oldSize << 1;
						byte[] newBuf = new byte[newSize];
						System.arraycopy(buf, 0, newBuf, 0, index);
						buf = newBuf;
					}
					else
					{
						if (autoFlush) flush();
						else throw new IOException("Buffer overflow");
					}
				}
			}
		}
	} // write()

	/**
	 * Write a portion of an array of characters.
	 *
	 * @param  cbuf  Array of characters
	 * @param  off   Offset from which to start writing characters
	 * @param  len   Number of characters to write
	 *
	 * @exception  IOException  If an I/O error occurs
	 */
	public void write(char cbuf[], int off, int len) throws IOException
	{
		synchronized (lock) {
			for (; off < len; off++)
			{
				write(cbuf[off]);
			}
		}
	} // write()

	/**
	 * Write a portion of a string.
	 *
	 * @param  str  A String
	 * @param  off  Offset from which to start writing characters
	 * @param  len  Number of characters to write
	 *
	 * @exception  IOException  If an I/O error occurs
	 */
	public void write(String str, int off, int len) throws IOException
	{
		synchronized (lock) {
			if (len > 128) super.write(str, off, len);
			else
			{
				for (int end = off + len; off < end; off++)
				{
					write(str.charAt(off));
				}
			}
		}
	} // write()

	/**
	 * Writes the buffered contents to the output stream and clears the buffer.
	 */
	public void writeOut(Writer stream) throws IOException
	{
		if (index > 0 && buf != null)
		{
			stream.write(new String(buf, 0, index), 0, index);
			index = 0;
		}
	} // writeOut()

} // DspWriter

