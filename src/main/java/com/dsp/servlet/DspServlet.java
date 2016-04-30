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
package com.dsp.servlet;

import com.dsp.DspConnect;
import com.dsp.DebugLog;
import com.dsp.DspException;
import com.dsp.DspFactory;
import com.dsp.DspNull;
import com.dsp.DspObject;
import com.dsp.DspPageContext;
import com.dsp.DspReadOnlyException;
import com.dsp.ThreadState;
import com.dsp.DspStatementLog;
import com.dsp.TargetWriter;

import java.io.*;			// IOException, PrintWriter
import java.net.*;		// URL, MalformedURLException
import java.sql.SQLException;  // ResultSet, SQLException
import java.util.*;

import javax.servlet.*;
import javax.servlet.http.*;
import javax.servlet.jsp.*;	// HttpJspPage, JspWriter

import static com.dsp.util.BZText.normalizePath;
import static com.dsp.util.BZCast._boolean;
import static com.dsp.util.BZCast._int;

public class DspServlet extends HttpServlet implements DspObject
{
	public static final String NAME					= "init";
	public static final String CONFIG_FILE			= "configFile";
    public static final String INIT_STMT			= "initStatement";
	public static final String PREFIX				= "com.dsp.init.";
	public static final String FLUSH_PAGES			= "flushPageCache";
	public static final String LOG_THRESHOLD		= "logThreshold";
	public static final String KILL_TIMEOUT			= "killTimeout";
	public static final String SPILL_SOURCE			= "spillSource";
	public static final String SPILL_JAVA			= "spillJava";
	public static final String VIRTUAL_DOMAINS      = "virtualDomains";	// no longer used
	public static final String TEMP_FOLDER			= "tempFolder";
	public static final String END_HTML				= "></table></body></html>\r\n";

	private static final long 		serialVersionUID		= -8107912546787200054L;
	
	private static WeakHashMap<URL, DspCompile>	pageCache;
	private static DspFactory		factory;
	private static Properties		data;

	private ServletConfig	config;
	private boolean				trace, debug, flushPages, spillSource, spillJava, virtualDomains;
//	private Object				sessionContext;
//	private boolean				noSession;
	private String				tempFolder;

	public void destroy()
	{
		if (trace) ThreadState.logln("DspServlet.destroy()");
        if (config == null) throw new NullPointerException("config is null when it shouldn't be.");
		if (factory != null) {
			factory.destroy();
			factory = null;
		}
		pageCache = null;
		config = null;
		DspStatementLog.close();
	} // destroy()

	public void doGet(HttpServletRequest req, HttpServletResponse res)
			throws ServletException, IOException
	{
		if (trace) System.out.println("DspServlet.doGet(" + req + ", " + res + ")");

		doPage(req, res);
	} // doGet()

	private void doPage(HttpServletRequest req, HttpServletResponse res)
			throws ServletException, IOException
	{
		if (trace) System.out.println("DspServlet.doPage(" + req + ", " + res + ")");
        if (config == null) throw new NullPointerException("config is null when it shouldn't be.");

		boolean topLevel = req.getAttribute(DspPageContext.OPEN) == null;
		String virtualPath = getVirtualPath(req);
//System.out.println(virtualPath);
		if (debug) System.out.println("DSP Request " + virtualPath);
		try {
//			res.setContentType("text/html;charset=8859_1");
			DspCompile comp = getCompiler(req, virtualPath);
			if (spillSource && req.getParameter(SPILL_SOURCE) != null)
			{
				returnSource(comp, res);
			}
			else if (spillJava && req.getParameter(SPILL_JAVA) != null)
			{
				returnJava(comp, req, res);
			}
			else
			{
				runPage(comp, virtualPath, req, res);
			}
		} catch (DspCompileException e) {
			req.setAttribute(DspPageContext.THROWN, e);
			DebugLog log = ThreadState.getLog();
			log.print("Error Compiling ");
			log.println(virtualPath);
			log.print(e.getMessage());
//getServletContext().log("Compile exception", e);
		} catch (DspParseException e) {
			req.setAttribute(DspPageContext.THROWN, e);
			DebugLog log = ThreadState.getLog();
			log.print("Error Parsing ");
			log.println(virtualPath);
			log.print(e.getMessage());
//getServletContext().log("Parse exception", e);
		} catch (FileNotFoundException e) {
			req.setAttribute(DspPageContext.THROWN, e);
			res.setStatus(HttpServletResponse.SC_NOT_FOUND);
			DebugLog log = ThreadState.getLog();
			log.println("File Not Found: " + e.getMessage());
			getServletContext().log("File Not Found in " + virtualPath, e);
		} catch (Exception e) {
			req.setAttribute(DspPageContext.THROWN, e);
			DebugLog log = ThreadState.getLog();
			log.print("Internal Exception Processing ");
			log.print(virtualPath);
			log.print(": ");
			log.println(e);
			getServletContext().log("Caught Exception in " + virtualPath, e);
//			e.printStackTrace(log);
		} catch (Throwable e) {
			req.setAttribute(DspPageContext.THROWN, new ServletException(e));
			DebugLog log = ThreadState.getLog();
			log.print("Internal Error Processing ");
			log.print(virtualPath);
			log.print(": ");
			log.println(e);
			getServletContext().log("Caught Throwable in " + virtualPath, e);
//			e.printStackTrace(log);
		} finally {
				// if this request was not an include or forward
			if (topLevel)
			{
					// I need to append the log to the output
				DebugLog log = ThreadState.getLog();
				DspPageContext pageContext = null;
				try {
					pageContext = ThreadState.getPageContext();
					if (pageContext != null)
					{
						ThreadState.setPageContext(null);
						JspWriter out = pageContext.getOut();
						log.flush(out);
						if (!res.isCommitted())
						{
							res.setContentLength(out.getBufferSize() - out.getRemaining());
						}
						try { out.close(); } catch (IOException e1) {}
					} else {
						ServletOutputStream out1 = res.getOutputStream();
						log.flush(out1);
						try { out1.close(); } catch (Exception e1) {}
					}
				} catch (Exception e) {
//					log.flush(System.out);
					System.out.println("Error flushing output stream: " + e);
				} finally {
					if (pageContext != null) factory.releaseFinal(pageContext);
				}
			}
		}
	} // doPage()

	public void doPost(HttpServletRequest req, HttpServletResponse res)
			throws ServletException, IOException
	{
		if (trace) System.out.println("DspServlet.doPost(" + req + ", " + res + ")");

		doPage(req, res);
	} // doPost()

	public boolean exists(String variable)
	{
		if (trace) ThreadState.logln("DspServlet.exists(" + variable + ")");
        if (config == null) throw new NullPointerException("config is null when it shouldn't be.");
		boolean result = getServletContext().getAttribute(PREFIX + variable) != null;
		if (!result)
		{
			result = config.getInitParameter(variable) != null;
		}
		if (debug) ThreadState.logln(NAME + '.' + variable + (result ? " exists" : " does not exist"));
		return result;
	} // exists()

	public Object get(String variable) throws DspException
	{
		if (trace) ThreadState.logln("DspServlet.get(" + variable + ")");
        if (config == null) throw new NullPointerException("config is null when it shouldn't be.");
		return get(variable, null);
	} // get()

	public Object get(String variable, Object defValue) throws DspException
	{
		if (trace) ThreadState.logln("DspServlet.get(" + variable + ", " + defValue + ')');
        if (config == null) throw new NullPointerException("config is null when it shouldn't be.");
		Object result = getServletContext().getAttribute(PREFIX + variable);
		if (result == null)
		{
			result = config.getInitParameter(variable);
		}
		if (result == null) result = defValue;
		if (debug) ThreadState.logln(NAME + '.' + variable + " => " + result);
		if (result == DspNull.NULL) result = null;
		return result;
	} // get()

	public DspCompile getCompiler(HttpServletRequest req, String virtualPath) throws DspException, IOException
	{
		if (trace) ThreadState.logln("DspServlet.getCompiler(" + virtualPath + ')');
        if (config == null) throw new NullPointerException("config is null when it shouldn't be.");
		URL url = getResource(req, virtualPath);
//System.out.println("DspServlet getCompiler Page URL: " + url);
		if (url == null) throw new FileNotFoundException(virtualPath + " (context " + getRealPath(req, "/") + ')');
		DspCompile comp = null;
		synchronized (pageCache) {
			comp = (DspCompile)pageCache.get(url);
			if (comp == null)
			{
				comp = new DspCompile(this, req, url, flushPages);
				pageCache.put(url, comp);
			}
		}
		return comp;
	} // getCompiler()

	private static final String[] types = {
		".db.htm", ".dsp", ".htm", ".html", ".jsp", ".txt"
	};
	private static final String[] mimes = {
		"text/html", "text/html", "text/html", "text/html", "text/html", "text/plain"
	};

	public String getDomainPath(String domain) {
        if (config == null) throw new NullPointerException("config is null when it shouldn't be.");
		if (data != null) {
			return data.getProperty(domain);
		}
		return null;
	} // getDomainPath()

	public String getMimeType(String path)
	{
        if (config == null) throw new NullPointerException("config is null when it shouldn't be.");
		path = path.toLowerCase();
		String mime = null;
		if (data != null)
		{
			int ix = path.lastIndexOf('.');
			String ext = path.substring(ix);
			mime = data.getProperty(ext);
		}
		if (mime == null)
		{
			mime = getServletContext().getMimeType(path);
		}
		if (mime == null)
		{
			for (int ix = 0, ixz = types.length; ix < ixz; ix++)
			{
				if (path.endsWith(types[ix]))
				{
					mime = mimes[ix];
					break;
				}
			}
		}
		if (mime == null) mime = "unknown/unknown";
		return mime;
	} // getMimeType()

	public String getRealPath(HttpServletRequest req, String path)
	{
        if (config == null) throw new NullPointerException("config is null when it shouldn't be.");
		String result = null;
		try {
			if (path.startsWith("//"))
			{
				result = new File(path.substring(1)).getCanonicalPath();
			}
			if (result == null && virtualDomains && data != null) {
				String host = req.getHeader("Host");
				if (host != null)
				{
					host = host.toLowerCase();
					int ix = host.indexOf(':');
					if (ix > 0) host = host.substring(0, ix);
					String domainPath = data.getProperty(host);
					if (domainPath != null)
					{
						result = new File(domainPath, path).getCanonicalPath();
					}
				}
			}
			if (result == null) {
				result = getServletContext().getRealPath(path);
			}
		} catch (IOException e) {
			ThreadState.logln(e);
		}
		if (debug) ThreadState.logln("DspServlet.getRealPath(" + path + ") -> " + result);
		return result;
	} // getRealPath()

	@SuppressWarnings("deprecation")
	public URL jndi2file(URL url) {
        if (config == null) throw new NullPointerException("config is null when it shouldn't be.");
			// Convert jndi: URLs to file:
		if (url.getProtocol().equals("jndi") && data != null) {
			String path = url.getFile();
			String domainName, pathRest;
			int ix = path.indexOf("/", 1);
			if (ix > 0) {
				domainName = path.substring(1, ix);
				pathRest = path.substring(ix);
			} else {
				domainName = path.substring(1);
				pathRest = "";
			}
System.out.println("jndi2file path " + path);
System.out.println("jndi2file domain " + domainName);
System.out.println("getResource pathRest " + pathRest);
			String domainPath = data.getProperty(domainName);
System.out.println("jndi2file domainPath " + domainPath);
			if (domainPath != null) {
				try {
					url = new File(domainPath, pathRest).toURL();
				} catch (MalformedURLException e) {
					e.printStackTrace();
				}
			}
		}
System.out.println("jndi2file -> " + url);
		return url;
	} // jndi2file()

	@SuppressWarnings("deprecation")
	public URL getResource(HttpServletRequest req, String path)
	{
		if (trace) ThreadState.logln("DspServlet.getResource(" + path + ")");
        if (config == null) throw new NullPointerException("config is null when it shouldn't be.");
		URL result = null;
		try {
			String realPath = getRealPath(req, path);
			if (realPath != null) {
				result = new File(realPath).toURL();
			}
		} catch (MalformedURLException e) {
			ThreadState.logln(e);
		}
		if (result == null) {
			try {
				result = getServletContext().getResource(path);
			} catch (MalformedURLException e) {
				ThreadState.logln(e);
			}
		}
/*		URL result = null;
		try {
			DspProp prop = (DspProp) req.getAttribute(DspPageContext.PROP);
			if (path.startsWith("//"))
			{
System.out.println("getResource 1");
				result = new File(path.substring(1)).toURL();
			}
			else
			if (prop != null && !path.startsWith("/"))
			{
System.out.println("getResource 2");
				result = new URL(prop.getURL(), path);
			}
			else
			{
				if (data != null)
				{
					String host = req.getHeader("Host");
					if (host != null)
					{
						host = host.toLowerCase();
						int ix = host.indexOf(':');
						if (ix > 0) host = host.substring(0, ix);
						String domainPath = data.getProperty(host);
						if (domainPath != null)
						{
System.out.println("getResource 3");
							result = new File(domainPath, path).toURL();
						}
					}
				}
				if (result == null)
				{
					if (virtualDomains)
					{
System.out.println("getResource 4");
						result = new File(req.getRealPath(path)).toURL();
					}
					else
					{
System.out.println("getResource 5");
						result = getServletContext().getRealPath(path);
						if (result == null) {
							result = getServletContext().getResource(path);
						}
					}
				}
			}
		} catch (MalformedURLException e) {
			ThreadState.logln(e);
		} catch (IOException e) {
			ThreadState.logln(e);
		}
*/
		if (debug) {
			ThreadState.logln("getResource(" + path + ") -> " + result);
			System.out.println("DspServlet.getResource(" + path + ") -> " + result);
		}
		return result;
	} // getResource()

	public String getServletInfo()
	{
		if (trace) System.out.println("DspServlet.getServletInfo()");

		return "DSP - Database Server Pages\r\nCopyright 2015 Troy Heninger, All Rights Reserved";
	} // getServletInfo()

	public String getTempFolder() {
        if (config == null) throw new NullPointerException("config is null when it shouldn't be.");
 		if (tempFolder == null) {
			tempFolder = getServletContext().getAttribute("javax.servlet.context.tempdir").toString();
		}
		return tempFolder;
	} // getTempFolder()

	public static String getVirtualPath(HttpServletRequest req)
	{
//		String contextPath = (String)req.getAttribute(DspPageContext.CONTEXT_PATH);
//System.out.println("contextPath1 " + contextPath);
//		if (contextPath == null) contextPath  = req.getContextPath();
//System.out.println("contextPath2 " + contextPath);
		String servletPath = (String)req.getAttribute(DspPageContext.SERVLET_PATH);
//System.out.println("servletPath1 " + servletPath);
		if (servletPath == null)
		{
			servletPath  = req.getServletPath();
			while (servletPath != null && servletPath.startsWith("//")) servletPath = servletPath.substring(1);
		}
//System.out.println("servletPath2 " + servletPath);
		String pathInfo = (String)req.getAttribute(DspPageContext.PATH_INFO);
//System.out.println("pathInfo1 " + pathInfo);
		if (pathInfo == null)
		{
			pathInfo  = req.getPathInfo();
			while (pathInfo != null && pathInfo.startsWith("//")) pathInfo = pathInfo.substring(1);
		}
//System.out.println("pathInfo2 " + pathInfo);
		String virtualPath;
		if (pathInfo == null || pathInfo.length() == 0) virtualPath = servletPath;
		else virtualPath = pathInfo;
		boolean rootPath = virtualPath.startsWith("//");
		if (rootPath) virtualPath = '/' + normalizePath(virtualPath.substring(1));
		else virtualPath = normalizePath(virtualPath);
		return virtualPath;
	} // getVirtualPath()

	public void init(ServletConfig config) throws ServletException
	{
		if (trace) ThreadState.logln("DspServlet.init(" + config + ")");

		super.init(this.config = config);
        if (config == null) throw new NullPointerException("config is null when it shouldn't be.");

		synchronized (getClass()) {
			if (factory == null) factory = new DspFactory(this);
			if (pageCache == null) pageCache = new WeakHashMap<URL, DspCompile>();
		}
		if (config.getInitParameter(DEBUG) != null) debug = true;
		if (config.getInitParameter(TRACE) != null) trace = true;
		if (config.getInitParameter(FLUSH_PAGES) != null) flushPages = true;
		if (config.getInitParameter(LOG_THRESHOLD) != null) {
			try {
				int logThresh = _int(config.getInitParameter(LOG_THRESHOLD));
				DspStatementLog.setLogThreshold(logThresh);
			} catch (NumberFormatException e) {
				// Oh well
			}
		}
		if (config.getInitParameter(KILL_TIMEOUT) != null) {
			try {
				int killTimeout = _int(config.getInitParameter(KILL_TIMEOUT));
				DspStatementLog.setKillTimeout(killTimeout);
			} catch (NumberFormatException e) {
				// Oh well
			}
		}
		if (config.getInitParameter(SPILL_SOURCE) != null) spillSource = true;
		if (config.getInitParameter(SPILL_JAVA) != null) spillJava = true;
		if (config.getInitParameter(VIRTUAL_DOMAINS) != null) virtualDomains = true;
		String path;
		if ((path = config.getInitParameter(CONFIG_FILE)) != null) initConfig(path);
        String initStmt;
        if ((initStmt = config.getInitParameter(INIT_STMT)) != null) DspConnect.setInit(initStmt);
		tempFolder = config.getInitParameter(TEMP_FOLDER);
	} // init()

	private void initConfig(String path)
	{
        if (config == null) throw new NullPointerException("config is null when it shouldn't be.");
		if (data != null) return;
		FileInputStream in = null;
		try {
			in = new FileInputStream(path);
			data = new Properties();
			data.load(in);
		} catch (IOException e) {
			getServletContext().log("DSP Error: Couldn't load config file " + path + ": " + e.getMessage());
			e.printStackTrace();
		} finally {
			if (in != null) try { in.close(); } catch (IOException e) {}
		}
	} // initConfig()

	public Iterator<String> names()
	{
        if (config == null) throw new NullPointerException("config is null when it shouldn't be.");
		ArrayList<String> list = new ArrayList<String>();
		Enumeration<String> it = getServletContext().getAttributeNames();
		while (it.hasMoreElements())
		{
			String name = it.nextElement();
			list.add(name.substring(PREFIX.length()));
		}
		it = config.getInitParameterNames();
		while (it.hasMoreElements())
		{
			String name = (String)it.nextElement();
			if (!list.contains(name))
			{
				list.add(name);
			}
		}
		return list.iterator();
	} // names()

	private void returnJava(DspCompile comp, HttpServletRequest req, HttpServletResponse res)
			throws IOException, DspException
	{
        if (config == null) throw new NullPointerException("config is null when it shouldn't be.");
		res.setContentType("text/plain");
		res.setHeader("Location", comp.getJavaFile().getName());
		res.setStatus(HttpServletResponse.SC_CREATED);
		returnStream(comp.getJava(req), res);
	} // returnJava()

	private void returnStream(InputStream in, HttpServletResponse res)
			throws IOException, DspException
	{
        if (config == null) throw new NullPointerException("config is null when it shouldn't be.");
		try {
			byte[] buf = new byte[8192];
			OutputStream out = res.getOutputStream();
			for (;;)
			{
				int read = in.read(buf);
				if (read <= 0) break;
				out.write(buf, 0, read);
			}
			out.close();
		} finally {
			try { in.close(); } catch (IOException e) {}
		}
	} // returnStream()

	private void returnSource(DspCompile comp, HttpServletResponse res)
			throws IOException, DspException
	{
		returnStream(comp.getSource(), res);
	} // returnSource()

	public void runPage(DspCompile comp, String virtualPath, HttpServletRequest req,
			HttpServletResponse res) throws IOException, ServletException
	{
        if (config == null) throw new NullPointerException("config is null when it shouldn't be.");
		if (debug)
		{
			System.out.println(virtualPath);
			ThreadState.logln("page " + virtualPath);
		}
		HttpJspPage page = comp.getPage(req);
		if (page == null)
		{
			throw new FileNotFoundException(virtualPath);
		}
		boolean topLevel = false;
		if (req.getAttribute(DspPageContext.OPEN) == null)
		{
			topLevel = true;
			res.setContentType(getMimeType(virtualPath)/*
					+ "; charset=ISO-8859-1"*/);
			res.setHeader("Expires", "-1");
			res.setHeader("Pragma", "no-cache");
			res.setHeader("Cache-Control", "no-cache");
			res.setHeader("Cache-Control", "max-age=0");
			DspStatementLog.logRequest(req);
		}
		try {
			page._jspService(req, res);
			TargetWriter tw = null;
			if (topLevel && (tw = (TargetWriter)req.getAttribute(DspPageContext.TARGET)) != null)
			{
				tw.close();
				req.removeAttribute(DspPageContext.OUT);
				req.removeAttribute(DspPageContext.TARGET);
				DspPageContext context = ThreadState.getPageContext();
				context.setOut(tw.getNext());
				context.forward(tw.getPath(), true);
			}
		} catch (Throwable e) {
			ThreadState.logln(e.toString());
			for (;;) {
				if (e instanceof ServletException)
				{
					Throwable temp = ((ServletException)e).getRootCause();
					if (temp == null) break;
					e = temp;
					ThreadState.logln(temp.getMessage());
				}
				else break;
			}
			if (e instanceof SQLException)
			{
				SQLException temp = (SQLException)e;
				while (temp != null) {
					ThreadState.logln(temp.getMessage() + ": Error #" + temp.getErrorCode());
					temp = temp.getNextException();
				}
			}
			ThreadState.logln(e);
			JspWriter out = ThreadState.getPageContext().getOut();
			if (out != null) {
				out.println(e.toString());
				out.print(END_HTML);
			} else {
				e.printStackTrace();
			}
		} finally {
			if (topLevel) DspStatementLog.logRequest(null);
			if (debug)
			{
				System.out.println("Ended " + virtualPath);
				ThreadState.logln("end of " + virtualPath);
			}
		}
	} // runPage()

	public void set(String variable, Object value) throws DspReadOnlyException
	{
		if (trace) ThreadState.logln("DspServlet.set(" + variable + ", " + value + ')');
        if (config == null) throw new NullPointerException("config is null when it shouldn't be.");
		if (value == null) unset(variable);
		else
		if (variable.equals(DEBUG)) debug = _boolean(value);
		else
		if (variable.equals(TRACE)) trace = _boolean(value);
		getServletContext().setAttribute(PREFIX + variable, value);
		if (debug) ThreadState.logln(NAME + '.' + variable + " <= " + value);
	} // set()

	public void unset(String variable) throws DspReadOnlyException
	{
		if (trace) ThreadState.logln("DspServlet.unset(" + variable + ')');
        if (config == null) throw new NullPointerException("config is null when it shouldn't be.");
		if (variable.equals(DEBUG)) debug = false;
		else
		if (variable.equals(TRACE)) trace = false;
		getServletContext().setAttribute(PREFIX + variable, DspNull.NULL);
		if (debug) ThreadState.logln("unset " + NAME + '.' + variable);
	} // unset()

} // DspServlet

