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

import com.dsp.servlet.DspServlet;

import javax.servlet.http.*;	// HttpServletResponse
import javax.servlet.jsp.*;		// JspWriter

public class ThreadState
{
	private static ThreadLocal<DspPageContext>	context = new ThreadLocal<DspPageContext>();
	private static ThreadLocal<DebugLog>	logSlot = new ThreadLocal<DebugLog>();

	public static void clear()
	{
		context.set(null);
		logSlot.set(null);
	} // clear()

	public static String getClassName(Object obj)
	{
		String name = obj.getClass().getName();
		int ix = name.lastIndexOf('.');
		if (ix < 0) return name;
		return name.substring(ix + 1);
	} // getClassName()

	public static Connect getConnection()
	{
		return getOpen().getConnection();
	} // getConnection()

	public static Database getDatabase()
	{
		return getOpen().getConnection().getDatabase();
	} // getDatabase()

	public static DebugLog getLog()
	{
		DebugLog log = logSlot.get();
		if (log == null)
		{
			log = new DebugLog(new LogBuffer());
			logSlot.set(log);
		}
		return log;
	} // getLog()

	public static DspOpen getOpen()
	{
		return (DspOpen)getPageContext().getOpen();
	} // getOpen()

	public static JspWriter getOut()
	{
		return getPageContext().getOut();
	} // getOut()

	public static DspPage getPage()
	{
		DspPageContext pc = getPageContext();
		if (pc == null) return null;
		return (DspPage)pc.getPage();
	} // getPage()

	public static DspPageContext getPageContext()
	{
		return context.get();
	} // getPageContext()

	public static DspProp getProp()
	{
		return getPage().getProp();
	} // getProp()

	public static HttpServletRequest getRequest()
	{
		return (HttpServletRequest)getPageContext().getRequest();
	} // getRequest()

	public static HttpServletResponse getResponse()
	{
		return (HttpServletResponse)getPageContext().getResponse();
	} // getResponse()

	public static DspServlet getServlet()
	{
		return getPageContext().getServlet();
	} // getServlet()

	public static void log(Object obj)
	{
		getLog().print(obj);
	} // log()

	public static void logln(Throwable obj)
	{
		obj.printStackTrace(getLog());
	} // logln()

	public static void logln(Object obj)
	{
		getLog().println(obj);
	} // logln()

	public static void setPageContext(DspPageContext pc)
	{
		context.set(pc);
	} // setObject()

} // ThreadState

