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

import java.io.*;							// IOException, PrintWriter
import java.util.Locale;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.*;	// Cookie, HttpServletResponse

final class DspResponse implements HttpServletResponse
{
	private HttpServletResponse res;
	private DspWriter out;
	private PrintWriter writer;

	public void addCookie(Cookie cookie)
	{
		res.addCookie(cookie);
	} // addCookie()

	public void addDateHeader(String name, long date)
	{
		res.addDateHeader(name, date);
	} // addDateHeader()

	public void addHeader(String name, String value)
	{
		res.addHeader(name, value);
	} // addHeader()

	public void addIntHeader(String name, int value)
	{
		res.addIntHeader(name, value);
	} // addIntHeader()

	public boolean containsHeader(String name)
	{
		return res.containsHeader(name);
	} // containsHeader()

	public String encodeURL(String url)
	{
		return res.encodeURL(url);
	} // encodeURL()

	/** @deprecated */
	public String encodeUrl(java.lang.String url)
	{
		return res.encodeURL(url);
	} // encodeURL()

	public String encodeRedirectURL(String url)
	{
		return res.encodeRedirectURL(url);
	} // encodeRedirectURL()

	/** @deprecated */
	public String encodeRedirectUrl(String url)
	{
		return res.encodeRedirectURL(url);
	} // encodeRedirectUrl()

	public void flushBuffer() throws IOException
	{
		res.flushBuffer();
	} // flushBuffer()


    public void resetBuffer() {
        res.resetBuffer();
    }


    public int getBufferSize()
    {
        return res.getBufferSize();
    } // getBufferSize()

	public String getCharacterEncoding()
	{
		return res.getCharacterEncoding();
	} // getCharacterEncoding()

	public Locale getLocale()
	{
		return res.getLocale();
	} // getLocale()

	public ServletOutputStream getOutputStream()
	{
		return null;	//out;
	} // getOutputStream()

	public PrintWriter getWriter() throws IOException
	{
		if (writer == null) writer = new PrintWriter(out);
		return writer;
	} // getWriter()

	public boolean isCommitted()
	{
		return res.isCommitted();
	} // isCommitted()

	public void reset()
	{
		res.reset();
	} // reset()

	public void sendError(int sc) throws IOException
	{
	 res.sendError(sc);
	} // sendError()

	public void sendError(int sc, String msg)
			throws IOException
	{
		res.sendError(sc, msg);
	} // sendError()

	public void sendRedirect(String location) throws IOException
	{
		res.sendRedirect(location);
 	} // sendRedirect()

	public void setBufferSize(int size)
	{
		res.setBufferSize(size);
	} // setBufferSize()

	public void setContentLength(int len)
	{
		res.setContentLength(len);
	} // setContentLength()

	public void setContentType(String type)
	{
		res.setContentType(type);
	} // setContentType()

	public void setDateHeader(String name, long date)
	{
		res.setDateHeader(name, date);
	} // setDateHeader()

	public void setHeader(String name, String value)
	{
		res.setHeader(name, value);
	} // setHeader()

	public void setIntHeader(String name, int value)
	{
		res.setIntHeader(name, value);
	} // setIntHeader()

	public void setLocale(Locale loc)
	{
		res.setLocale(loc);
	} // setLocale()

	void setResponse(HttpServletResponse response)
	{
		res = response;
		try { out.clear(); } catch (IOException e) {}
	} // setResponse()

	public void setStatus(int sc)
	{
		res.setStatus(sc);
	} // setStatus()

    /** @deprecated */
	public void setStatus(int sc, String sm)
	{
		res.setStatus(sc, sm);
	} // setStatus()

	@Override
	public String getContentType() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setCharacterEncoding(String arg0) {
		// TODO Auto-generated method stub
	}

} // DspResponse
