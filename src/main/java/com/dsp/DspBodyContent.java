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
import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.tagext.BodyContent;

final class DspBodyContent extends BodyContent
{
	static final String CRLF = "\r\n";
	private StringBuffer content;

	public DspBodyContent(JspWriter jspwriter)
	{
		super(jspwriter);
		content = new StringBuffer();
	} // DspBodyContent()

	public void clear()
			throws IOException
	{
		content.setLength(0);
	} // clear()

	public void clearBuffer()
			//throws IOException
	{
		content.setLength(0);
	}

	public void close()
			throws IOException
	{
		flush();
	}

	public int getRemaining()
	{
		return 0;
	} // getRemaining()

	public Reader getReader()
	{
		return new StringReader(content.toString());
	} // getReader()

	public String getString()
	{
		return content.toString();
	} // getString()

	public void newLine()
			throws IOException
	{
		content.append(CRLF);
	} // newLine()

	public void print(boolean flag)
			throws IOException
	{
		content.append(flag);
	} // print(boolean)

	public void print(char c)
			throws IOException
	{
		content.append(c);
	} // print(char)

	public void print(int i)
			throws IOException
	{
		content.append(i);
	} // print(int)

	public void print(long l)
			throws IOException
	{
		content.append(l);
	} // print(long)

	public void print(float f)
			throws IOException
	{
		content.append(f);
	} // print(float)

	public void print(double d)
			throws IOException
	{
		content.append(d);
	} // print(double)

	public void print(char ac[])
			throws IOException
	{
		content.append(ac);
	} // print(char[])

	public void print(String s)
			throws IOException
	{
		content.append(s);
	} // print(String)

	public void print(Object obj)
			throws IOException
	{
		content.append(obj);
	} // print(Object)

	public void println()
			throws IOException
	{
		content.append(CRLF);
	} // println()

	public void println(boolean flag)
			throws IOException
	{
		content.append(flag);
		content.append(CRLF);
	} // println(boolean)

	public void println(char c)
			throws IOException
	{
		content.append(c);
		content.append(CRLF);
	} // println(char)

	public void println(int i)
			throws IOException
	{
		content.append(i);
		content.append(CRLF);
	} // println(int)

	public void println(long l)
			throws IOException
	{
		content.append(l);
		content.append(CRLF);
	} // println(long)

	public void println(float f)
			throws IOException
	{
		content.append(f);
		content.append(CRLF);
	} // println(float)

	public void println(double d)
			throws IOException
	{
		content.append(d);
		content.append(CRLF);
	} // println(double)

	public void println(char ac[])
			throws IOException
	{
		content.append(ac);
		content.append(CRLF);
	} // println(char[])

	public void println(String s)
			throws IOException
	{
		content.append(s);
		content.append(CRLF);
	} // println(String)

	public void println(Object obj)
			throws IOException
	{
		content.append(obj);
		content.append(CRLF);
	} // println(Object)

	public void write(char ac[], int i, int j)
	{
		int iz = i + j;
		if (iz > ac.length) iz = ac.length;
		for (int ix = i; ix < iz; ix++)
		{
			content.append(ac[ix]);
		}
	} // write(char[])

	public void writeOut(Writer writer)
			throws IOException
	{
		writer.write(content.toString());
	} // writeOut()

} // DspBodyContent
