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

import java.util.*;	// Hashtable, Vector
import javax.servlet.http.*;

import static com.dsp.util.BZCast._boolean;
import static com.dsp.util.BZCast._int;
import static com.dsp.util.BZCast._String;

/**
 * <p>This is the cookie manager class.  All cookies for a request and response are handled
 * by this class.  On a DSP page this is the type for the implicit <b>cookie</b> object.
 * Incomming cookies from the <b>request</b> are initially loaded into this object at the top of
 * the page. Subsequent <b>cookie</b> changes or additions are automatically reflected
 * in the <b>response</b>.</p>
 *
 * <p>Besides the standard debugMode and traceMode properties, this object supports a few more
 * in the WEB-INF/dsp.properties file.  These specify default values for newly created
 * cookies:</p>
 * <ul>
 * <li>_maxage <i>The default maxAge; defaults to 1,048,576 seconds</i></li>
 * <li>_domain <i>The default domain name; defaults to <b>null</b></i></li>
 * <li>_path <i>The default path for newly created cookies; defaults to the project path</i></li>
 * </ul>
 */
public class DspCookies implements DspObject
{
	public static final String NAME	= "cookie";
	
	static final String MAX_AGE		= "_maxAge";
	static final String DOMAIN		= "_domain";
	static final String PATH		= "_path";

	private Hashtable<String, Cookie>	cookies = new Hashtable<String, Cookie>();
	private boolean			debug, trace;
	private String			path, domain;
	private int				maxAge;
	private DspPageContext	pageContext;

	DspCookies(DspPageContext pageContext, DspProp prop) //throws DspException
	{
		this.pageContext = pageContext;
		unset(DOMAIN);	// init domain
		unset(MAX_AGE);	// init maxAge
		prop.preSet(this, NAME);

		if (trace) ThreadState.logln("DspCookies()");
		HttpServletRequest request = (HttpServletRequest)pageContext.getRequest();
		Cookie[] c = request.getCookies();
		if (c != null)
		{
			for (int ix = 0, iz = c.length; ix < iz; ix++)
			{
				Cookie cookie = c[ix];
				String name = cookie.getName();
				if (cookie.getVersion() == 0)
				{
					cookie.setMaxAge(maxAge);
					if (domain != null) cookie.setDomain(domain);
					if (path != null) cookie.setPath(path);
				}
				if (debug) ThreadState.logln(NAME + '.' + name + " => " + cookie.getValue());
//				if (debug)
//				{
//					ThreadState.logln("version: " + cookie.getVersion()
//							+ ", maxAge: " + cookie.getMaxAge()
//							+ ", domain: " + cookie.getDomain()
//							+ ", path: " + cookie.getPath());
//				}
				cookies.put(name.toLowerCase(), cookie);
			}
		}
	} // DspCookies()

	/**
	 * Closes and erases contents. DSP calls this automatically at the
	 * end of your _jspService() function.
	 */
	public void close()
	{
		if (trace) ThreadState.logln("DspCookies.close()");
		cookies = null;
	} // close()

	/**
	 * Returns true if <b>name</b> is a cookie.
	 */
	public boolean exists(String name)
	{
		Cookie cookie = findCookie(name);
		return cookie != null;
	} // exists()

	private Cookie findCookie(String name)
	{
		Cookie cookie = (Cookie)cookies.get(name.toLowerCase());
		if (trace) ThreadState.logln("DspCookies.findCookie(" + name + ") -> "
				+ (cookie == null ? "(not found)" : cookie.getValue().toString()));
		return cookie;
	} // findCookie()

	/**
	 * Returns the cookie value specified by <b>name</b>.
	 */
	public Object get(String name)
	{
		return get(name, null);
	} // get()

	Object get(String name, Object defaultValue)
	{
		if (trace) ThreadState.logln("DspCookies.get(" + name + ", " + defaultValue + ')');
		Object result = null;
		if (name.equalsIgnoreCase(TRACE)) {
			result = new Boolean(trace);
		} else if (name.equalsIgnoreCase(DEBUG)) {
			result = new Boolean(debug);
		} else if (name.equalsIgnoreCase(MAX_AGE)) {
			result = new Integer(maxAge);
		} else if (name.equalsIgnoreCase(DOMAIN)) {
			result = domain;
		} else if (name.equalsIgnoreCase(PATH)) {
			result = path;
		} else {
			Cookie cookie = findCookie(name);
			if (cookie != null) result = cookie.getValue();
			if (result == null) result = defaultValue;
		}
		if (debug) ThreadState.logln("cookie." + name + " => " + result);
		return result;
	} // get()

	public Iterator<String> names()
	{
		ArrayList<String> list = new ArrayList<String>();
		Enumeration<String> it = cookies.keys();
		while (it.hasMoreElements())
		{
			list.add(it.nextElement());
		}
		return list.iterator();
	} // names()

	/**
	 * Changes the value of the specified cookie.  If the value is <b>null</b> that is the
	 * same as calling unset(name).
	 */
	public void set(String name, Object value)
	{
		if (trace) ThreadState.logln("DspCookies.set(" + name + ", " + value + ')');
		if (value == null)
		{
			unset(name);
			return;
		}
		if (name.equalsIgnoreCase(TRACE)) {
			trace = _boolean(value);
		} else if (name.equalsIgnoreCase(DEBUG)) {
			debug = _boolean(value);
		} else if (name.equalsIgnoreCase(MAX_AGE)) {
			maxAge = _int(value);
		} else if (name.equalsIgnoreCase(DOMAIN)) {
			domain = _String(value);
		} else if (name.equalsIgnoreCase(PATH)) {
			path = _String(value);
		} else {
			Cookie cookie = findCookie(name);
			if (cookie == null)
			{
				cookie = new Cookie(name, value.toString());
//				System.out.println("new cookie: name: " + cookie.getName()
//						+ ", value: " + cookie.getValue()
//						+ ", version: " + cookie.getVersion()
//						+ ", maxAge: " + cookie.getMaxAge()
//						+ ", domain: " + cookie.getDomain()
//						+ ", path: " + cookie.getPath());
				cookie.setMaxAge(maxAge);
				if (domain != null) cookie.setDomain(domain);
				if (path != null) cookie.setPath(path);
				cookies.put(name.toLowerCase(), cookie);
				if (trace)
				{
					ThreadState.logln("cookie._maxAge => " + maxAge);
					ThreadState.logln("cookie._domain => " + domain);
					ThreadState.logln("cookie._path => " + path);
				}
			}
			else
			{
				cookie.setValue(value.toString());
			}
			if (cookie != null)
			{
				((HttpServletResponse)pageContext.getResponse()).addCookie(cookie);
				if (trace) ThreadState.logln("added cookie " + cookie.getName() + " to response");
//				if (debug)
//				{
//					ThreadState.logln("version: " + cookie.getVersion()
//							+ ", maxAge: " + cookie.getMaxAge()
//							+ ", domain: " + cookie.getDomain()
//							+ ", path: " + cookie.getPath());
//				}
			}
//			else if (trace) ThreadState.logln("cookie " + name + " was not added to response");
		}
		if (debug) ThreadState.logln(NAME + '.' + name + " <= " + value);
	} // set()

	/**
	 * Returns a description of this object.
	 */
	public String toString()
	{
		return "DspCookies [" + cookies.size() + " cookies]";
	} // toString()

	/**
	 * Tell browser to delete the cookie in the response and delete the cookie value.
	 */
	public void unset(String name)
	{
		if (trace) ThreadState.logln("DspCookies.unset(" + name + ')');
		if (name.equalsIgnoreCase(TRACE)) {
			trace = false;
		} else if (name.equalsIgnoreCase(DEBUG)) {
			debug = false;
		} else if (name.equalsIgnoreCase(MAX_AGE)) {
			maxAge = Integer.MAX_VALUE >> 1;
		} else if (name.equalsIgnoreCase(DOMAIN)) {
			HttpServletRequest request = (HttpServletRequest)pageContext.getRequest();
			int port = request.getServerPort();
			if (port == 80 || port == 443)
			{
				domain = null;
			}
			else
			{
				domain = request.getServerName();
				domain += ":" + port;
			}
		} else if (name.equalsIgnoreCase(PATH)) {
			path = null;
		} else {
			Cookie cookie = findCookie(name);
			if (cookie != null)
			{
				cookie.setMaxAge(0);	// cause the cookie to be deleted
				((HttpServletResponse)pageContext.getResponse()).addCookie(cookie);
				cookies.remove(name.toLowerCase());
			}
		}
		if (debug) ThreadState.logln(NAME + '.' + name + " <= NULL");
	} // unset()

} // DspCookies

