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

public class TargetWriter extends JspWriter
{
	public static final String NAME = "out";
	public static final int DEFAULT_SIZE = 32768;

	private Object		lock;
	private String		path, realPath;
	private JspWriter	next;
	private Writer		out;

	public TargetWriter(JspWriter next, HttpServletResponse response,
			String path, String realPath) throws DspException, IOException
	{
		super(0, false);
		if (response.isCommitted()) throw new DspException("Target must execute before output stream is committed");
		this.next = next;
		this.path = path;
		this.realPath = realPath;
		out = new FileWriter(realPath);
		((DspWriter)next).writeOut(out);
		lock = this;
	} // TargetWriter

  public void clear() throws IOException
	{
		synchronized (lock) {
			out.close();
			out = new FileWriter(realPath);
		}
	} // clear()

  public void clearBuffer() throws IOException
	{
		synchronized (lock) {
			out.close();
			out = new FileWriter(realPath);
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
			if (out != null)
			{
				flush();
				out.close();
				out = null;
			}
		}
	} // close()

	/**
	 * Destructor.  Makes sure the file is closed.
	 */
	public void finalize()
	{
		try { close(); } catch (IOException e) {}
	} // finalize()

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
			out.flush();
		}
	} // flush()

	public JspWriter getNext() { return next; }
	public String getPath() { return path; }

  public int getRemaining()
	{
		synchronized (lock) {
			return next.getRemaining();
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
			c = (char)c;
			if (c < 256) out.write(c);
			else
			{
				out.write('&');
				out.write('#');
				int d5 = c / 10000;
				if (d5 > 0) out.write(d5 + '0');
				int d4 = c / 1000 % 10;
				if (d4 > 0) out.write(d4 + '0');
				int d3 = c / 100 % 10;
				if (d3 > 0) out.write(d3 + '0');
				int d2 = c / 10 % 10;
				if (d2 > 0) out.write(d2 + '0');
				out.write((c % 10) + '0');
				out.write(';');
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

} // TargetWriter

