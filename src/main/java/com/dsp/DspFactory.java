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

import java.io.*;		// IOException, FileNotFoundException
import java.net.URL;
import java.util.*;	// Collection, Hashtable, Iterator

import javax.servlet.*;	// Servlet, ServletRequest, ServletResponse
import javax.servlet.jsp.*;	// JspEngineInfo, JspFactory

/**
 * This is used to seed and initialize each page request with the required PageContext,
 * similar to JspFactory.
 */
public class DspFactory
{
	static final int		THREADS = 20;
	static final int		INSERTS = 200;
	static final boolean	DEBUG_MODE = false;
	static final boolean	TRACE_MODE = false;

//	static {
//		setDefaultFactory(new DspFactory());
//	}

	private static DspFactory	factory;
	private static char					letter1 = 'Z', letter2 = 'Z';	// used to uniqly name local variables
	private static StringBuffer buf = new StringBuffer(10);		// ditto

	private DspServlet					servlet;
	private DspVersionInfo				info = new DspVersionInfo();
	private	Pool					threadPool = new Pool("Thread");
	private Pool					insertPool = new Pool("Call");
	private ServletLog				servletLog;
	private Hashtable<URL, DspProp>	propCache;

	/**
	 * Constructor. Used only by DspServlet.
	 */
	public DspFactory(DspServlet servlet)
	{
		factory = this;
		this.servlet = servlet;
		for (int ix = 0; ix < THREADS; ix++)
		{
			threadPool.checkIn(new DspPageContext(servlet, false));
		}
		for (int ix = 0; ix < INSERTS; ix++)
		{
			insertPool.checkIn(new DspPageContext(servlet, true));
		}
		servletLog = new ServletLog(servlet.getServletContext());
	} // DspFactory()

	/**
	 * Called by DspServlet when it's destroy() is called.
	 */
	public void destroy()
	{
		if (propCache != null)
		{
			Collection<DspProp> col = propCache.values();
			propCache = null;
			Iterator<DspProp> it = col.iterator();
			while (it.hasNext())
			{
				DspProp prop = it.next();
				prop.close();
			}
		}
	} // destroy()

	/**
	 * Find the factory object.  This is called automatically by each DSP page.
	 */
	public static DspFactory getDefaultFactory()
	{
		if (factory == null) throw new IllegalStateException("The DspFactory object has not been created yet");
		return factory;
	} // getDefaultFactory()

	/**
	 * Return information about this JSP/DSP engine.
	 */
	public JspEngineInfo getEngineInfo() { return info; }

	/**
	 * Return the prop object for a given URL.  This is called automatically by
	 * each DSP page.
	 */
	public DspProp getProp(URL url) throws DspException, IOException
	{
//System.out.println("DspFactory getProp " + url);
		if (TRACE_MODE) ThreadState.logln("DspFactory.getProp(" + url + ')');
		if (propCache == null) propCache = new Hashtable<URL, DspProp>();
		synchronized (propCache) {
			DspProp prop = propCache.get(url);
			if (prop == null)
			{
				try {
					prop = new DspProp(url, servlet);
				} catch (IOException e) {
					String s = e.toString();
					if (s.indexOf("Directory") > 0 && s.indexOf("doesn't exist") > 0)
					{
						throw new FileNotFoundException(s);
					}
					else throw e;
				}
				propCache.put(url, prop);
			}
			return prop;
		}
	} // getProp()

	/**
	 * Return a new PageContext object for the page.  This is automatically called
	 * by each DSP page.
	 */
	public PageContext getPageContext(Servlet servlet, ServletRequest request,
			ServletResponse response, String errorPageURL, boolean needsSession,
      int buffer, boolean autoflush)
  {
		DspPageContext pc;
		Pool pool;
		if (request.getAttribute(DspPageContext.OPEN) != null)
		{
			pool = insertPool;
			if (pool.available() == 0) throw new RuntimeException("Recursion too deep");
		} else {
			pool = threadPool;
		}
		pc = (DspPageContext)pool.checkOut();
		ThreadState.setPageContext(pc);
		try {
			pc.initialize(servlet, request, response, errorPageURL,
					needsSession, buffer, autoflush);
		} catch (IOException e) {
			throw new RuntimeException(e.toString());
//			ThreadState.setPageContext(null);
//			pool.checkIn(pc);
//			e.printStackTrace();
//			throw new
		}
		return pc;
	} // getPageContext()

	/* *
	 * Returns the ServletLog object.
	 */
	public ServletLog getServletLog() { return servletLog; }

	/**
	 * Return a unique identifier using a 2 character rotating algorithm.
	 * This is used to create unique variable names during compilation of DSP pages.
	 */
	public synchronized static String getUnique(String prefix)
	{
		if (letter1 == '9')
		{
			letter1 = 'a';
			if (letter2 == '9') letter2 = 'a';
			else
			if (letter2 == 'z') letter2 = 'A';
			else
			if (letter2 == 'Z') letter2 = '0';
			else letter2++;
		}
		else
		if (letter1 == 'z') letter1 = 'A';
		else
		if (letter1 == 'Z') letter1 = '0';
		else letter1++;
		buf.setLength(0);
		buf.append(prefix);
		buf.append(letter2);
		buf.append(letter1);
		return buf.toString();
	} // getUnique()

	/**
	 * Release the PageContext.  Called automatically by each DSP page to place the
	 * PageContext back to the pool, so it may be used on future requests.
	 */
	public void releasePageContext(DspPageContext pc)
	{
		pc.release();
		if (pc.getInsertType())
		{
			insertPool.checkIn(pc);
		}
	} // releasePageContext()

	/**
	 * Used internally by DspServlet for top level requests only.
	 */
	public void releaseFinal(DspPageContext pc)
	{
		if (pc.getInsertType()) throw new IllegalStateException("Insert pageContext's are not allowed here");
		pc.releaseFinal();
		threadPool.checkIn(pc);
	} // releasePageContext()

} // DspFactory
