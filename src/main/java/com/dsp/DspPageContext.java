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
import com.dsp.util.BZText;

import java.io.*;	// IOException, PrintStream
import java.net.URL;
import java.lang.reflect.InvocationTargetException;
import java.util.*;					// Hashtable, Stack, Vector

import javax.el.ELContext;
import javax.servlet.*;			// Servlet, ServletRequest, ServletResponse
import javax.servlet.http.*;// HttpSession
import javax.servlet.jsp.*;	// JspWriter, PageContext
import javax.servlet.jsp.el.*;	// JSP 2.x APR
import javax.servlet.jsp.tagext.*;	// BodyContent

import static com.dsp.util.BZCast._boolean;
import static com.dsp.util.BZText.replace;

public class DspPageContext extends PageContext implements DspObject
{
	public static final String NAME					= "temp";
	public static final String COOKIES			= "com.dsp.cookies";
	public static final String FORWARD			= "com.dsp.forward";
	public static final String OPEN					= "com.dsp.open";
//	public static final String OUT					= "com.dsp.out";
//	public static final String PROP					= "com.dsp.prop";
//	public static final String DSP					= "com.dsp.dspFlag";
	public static final String ROOT					= "com.dsp.rootPath";
  public static final String TARGET				= "com.dsp.target";
  public static final String THROWN				= "com.dsp.thrown";
	public static final String EXCEPTION		= "javax.servlet.jsp.jspException";
	public static final String PATH_INFO		= "javax.servlet.include.path_info";
	public static final String SERVLET_PATH	= "javax.servlet.include.servlet_path";
//	public static final String CONTEXT_PATH	= "javax.servlet.include.context_path";
	public static final String QUERY_STRING	= "javax.servlet.include.query_string";
	public static final String REQUEST_URI	= "javax.servlet.include.request_uri";

	private boolean debug, trace;

	private Hashtable<String, Object>	vars = new Hashtable<String, Object>();
	private DspServlet					servlet;
	private ServletConfig				config;
	private ServletContext				application;
	private HttpServletRequest			request;
	private HttpServletResponse			response;
	private HttpSession					session;
	private Stack<JspWriter>			outStack = new Stack<JspWriter>();
	private JspWriter					out;
	private DspPage					page;
	private Exception					exception;
	private String						errorPageURL;
	private DspOpen					open;
	private DspRequest					var = new DspRequest();
	private DspUser					user;
	private DspCookies					cookie;
	private final boolean				insertType;

	public DspPageContext(DspServlet servlet, boolean insert)
	{
		this.servlet = servlet;
		this.insertType = insert;
			// 'open' is shared from normal threads to inserted threads,
			// so only the normal threads need to allocate 'open'.
		if (!insertType) this.open = new DspOpen();
	} // DspPageContext()

	public boolean exists(String name)
	{
		if (trace) ThreadState.logln("DspPageContext.exists(" + name + ')');
		return get(name) != null;
	} // exists()

	public Object findAttribute(String name)
	{
		return null;
	} // findAttribute()

	public void forward(String relativeUrlPath) throws ServletException, IOException
	{
		forward(relativeUrlPath, false, false);
	} // forward()

	public void forward(String relativeUrlPath, boolean dspFlag)
			throws ServletException, IOException
	{
		forward(relativeUrlPath, dspFlag, false);
	} // forward()

	public void forward(String relativeUrlPath, boolean dspFlag, boolean force)
			throws ServletException, IOException
	{
			// switch to the real 'out', in case of call void ...
		while (outStack.size() > 0)
		{
			popBody();
		}
		if (force) out.clearBuffer();
		else
		{
			try {
				out.clear();
			} catch(IOException e) {
				throw new IllegalStateException("forward failed because some data has already been written to the client");
			}
		}
		Object pathSave = request.getAttribute(SERVLET_PATH);
		Object uriSave = request.getAttribute(REQUEST_URI);
//		Object rootSave = swapAttribute(request, ROOT, servlet.getResource(request, "/"));
		boolean rootPath = relativeUrlPath.startsWith("//");
		Object rootSave = null;
		if (rootPath) {
			rootSave = swapAttribute(request, ROOT, "true");
			dspFlag = true;
		}
/*		dspFlag |= request.getAttribute(DSP) != null || rootPath;
		Object dspSave = null;
		if (dspFlag) dspSave = swapAttribute(request, DSP, "true");*/
		relativeUrlPath = normalizePath(relativeUrlPath);
		if (getProp().getDebug()) ThreadState.logln("forward " + (dspFlag ? "dsp " : "") + relativeUrlPath);
		if (!insertType)
		{
			request.setAttribute(OPEN, open);
			request.setAttribute(OUT, out);
		}
		Database dbSave = open.getDatabase();
		try {
			request.removeAttribute(FORWARD);	// cancel any previous forward
			request.removeAttribute(PATH_INFO);
			request.removeAttribute(QUERY_STRING);
/*			request.removeAttribute(REQUEST_URI);
			request.removeAttribute(SERVLET_PATH);*/
			request.setAttribute(SERVLET_PATH, relativeUrlPath);
			request.setAttribute(REQUEST_URI, relativeUrlPath);
			if (dspFlag)
			{
/*				request.setAttribute(SERVLET_PATH, relativeUrlPath);
				request.setAttribute(REQUEST_URI, relativeUrlPath);*/
				servlet.runPage(servlet.getCompiler(request, relativeUrlPath),
						relativeUrlPath, request, response);
			}
			else
			{
				RequestDispatcher dispatch = getDispatch(relativeUrlPath);
				dispatch.forward(request, response);
			}
		} finally {
			ThreadState.setPageContext(this);
//			request.setAttribute(PROP, getProp());
			swapAttribute(request, SERVLET_PATH, pathSave);
			swapAttribute(request, REQUEST_URI, uriSave);
			if (rootPath) swapAttribute(request, ROOT, rootSave);
			request.setAttribute(FORWARD, relativeUrlPath);
//ThreadState.logln(FORWARD + " = " + request.getAttribute(FORWARD));
/*			if (dspFlag) swapAttribute(request, DSP, dspSave);*/
			open.setDatabase(dbSave);
			if (!insertType)
			{
				request.removeAttribute(OPEN);
				request.removeAttribute(OUT);
			}
		}
	} // forward()

	public Object get(String name)
	{
		if (trace) ThreadState.logln("DspPageContext.get(" + name + ')');
		return getAttribute(name);
	} // get()

	public Object getAttribute(String name)
	{
		if (trace) ThreadState.logln("DspPageContext.getAttribute(" + name + ')');
		Object result;
		if (name.equals(DEBUG)) result = Boolean.valueOf(debug);
		else
		if (name.equals(TRACE)) result = Boolean.valueOf(trace);
		else result = vars.get(name);
		if (debug) ThreadState.logln(NAME + '.' + name + " => " + result);
		return result;
	} // getAttribute()

	public Object getAttribute(String name, int scope)
	{
		if (trace) ThreadState.logln("DspPageContext.getAttribute(" + name + ", " + scope + ')');
		if (name == null) throw new NullPointerException("Attribute name is null");
		switch (scope)
		{
			case APPLICATION_SCOPE:
				if (name.equals(APPLICATION)) return application;
				return application.getAttribute(name);
			case SESSION_SCOPE:
				if (name.equals(SESSION)) return session;
				if (session != null) return session.getAttribute(name);
				break;
			case PAGE_SCOPE:
				if (name.equals(RESPONSE)) return response;
				if (name.equals(PAGECONTEXT)) return this;
				if (name.equals(OUT)) return out;
				if (name.equals(CONFIG)) return config;
				if (name.equals(PAGE)) return page;
				if (name.equals(EXCEPTION)) return getException();
				return getAttribute(name);
			case REQUEST_SCOPE:
				if (name.equals(REQUEST)) return request;
				return request.getAttribute(name);
			default: throw new IllegalArgumentException("Invalid Scope");
		}
		return null;
	} // getAttribute(scope)

	public int getAttributesScope(String name)
	{
		return -1;
	} // getAttributesScope()

	public Enumeration<String> getAttributeNamesInScope(int scope)
	{
		return null;
	} // getAttributeNamesInScope()

	public DspObject getCookie()
	{
		if (cookie == null)
		{
			cookie = (DspCookies)request.getAttribute(COOKIES);
		}
		if (cookie == null)
		{
			cookie = new DspCookies(this, page.prop);
			request.setAttribute(COOKIES, cookie);
		}
		return cookie;
	} // getCookie()

	private RequestDispatcher getDispatch(String relativeUrl) throws FileNotFoundException
	{
		RequestDispatcher dispatch = application.getRequestDispatcher(relativeUrl);
		if (dispatch == null)
		{
			throw new FileNotFoundException("Failed to get dispatcher for: " + relativeUrl
					+ "\r\nMake sure the file exists and that the servlet engine is configured to handle the file type");
		}
		return dispatch;
	} // getDispatch()

	public PrintStream getDebugLog() { return ThreadState.getLog(); }
	public Exception getException() { return exception; }
	boolean getInsertType() { return insertType; }
	public DspObject getInit() { return servlet; }
	public DspOpen getOpen() { return open; }
	public JspWriter getOut() { return out; }
	public Object getPage() { return page; }
  public DspProp getProp() { return page.prop; }
	public ServletRequest getRequest() { return request; }
	public ServletResponse getResponse() { return response; }
	public DspStatement getRow() { return open.getRow(); }
	DspServlet getServlet() { return servlet; }
	public ServletConfig getServletConfig() { return config; }
	public ServletContext getServletContext() { return application; }
	public HttpSession getSession() { return session; }
	public DspObject getUser() { return user; }
	public DspObject getVar() { return var; }

    public void handlePageException(Exception e)
            throws ServletException, IOException
    {
        handlePageException((Throwable)e);
    }

	public void handlePageException(Throwable e)
			throws ServletException, IOException
	{
		Throwable obj = e;
		for (;;)
		{
			Throwable temp;
			if (obj instanceof InvocationTargetException)
			{
				temp = ((InvocationTargetException)obj).getTargetException();
				if (temp != null) obj = temp;
				else break;
			}
			else
			if (obj instanceof ServletException)
			{
				temp = ((ServletException)obj).getRootCause();
				if (temp != null) obj = temp;
				else break;
			}
			else break;
		}
		if (errorPageURL != null && errorPageURL.length() > 0 && !errorPageURL.equalsIgnoreCase("null"))
		{
			request.removeAttribute(SERVLET_PATH);
			request.removeAttribute(THROWN);
			request.setAttribute(EXCEPTION, obj);
			try {
				forward(errorPageURL, true, true);
			} catch (Exception e1) {
				e1.printStackTrace();
				throw new ServletException(obj);
			}
			request.setAttribute(THROWN, obj);
		}
		else
		{
			request.setAttribute(THROWN, obj);
			throw new ServletException(obj);
		}
	} // handlePageException()


    public void include(String relativeUrlPath) throws ServletException, IOException
    {
        include(relativeUrlPath, false);
    } // include()

	public void include(String relativeUrlPath, boolean dspFlag) throws ServletException, IOException
	{
		Object pathSave = request.getAttribute(SERVLET_PATH);
		Object uriSave = request.getAttribute(REQUEST_URI);
		boolean rootPath = relativeUrlPath.startsWith("//");
		Object rootSave = null;
		if (rootPath) {
			rootSave = swapAttribute(request, ROOT, "true");
			dspFlag = true;
		}
/*		dspFlag |= request.getAttribute(DSP) != null || rootPath;
		Object dspSave = null;
		if (dspFlag) dspSave = swapAttribute(request, DSP, "true");*/
		Object forwardSave = swapAttribute(request, FORWARD, null);
		relativeUrlPath = normalizePath(relativeUrlPath);
		if (getProp().getDebug()) ThreadState.logln("call " + (dspFlag ? "dsp " : "") + relativeUrlPath);
		if (!insertType)
		{
			request.setAttribute(OPEN, open);
			request.setAttribute(OUT, out);
		}
		request.setAttribute(SERVLET_PATH, relativeUrlPath);
		request.setAttribute(REQUEST_URI, relativeUrlPath);
		Database dbSave = open.getDatabase();
		try {
			if (dspFlag)
			{
/*				request.setAttribute(SERVLET_PATH, relativeUrlPath);
				request.setAttribute(REQUEST_URI, relativeUrlPath);*/
				servlet.runPage(servlet.getCompiler(request, relativeUrlPath), relativeUrlPath, request, response);
			}
			else
			{
				RequestDispatcher dispatch = getDispatch(relativeUrlPath);
        if (dispatch == null) throw new FileNotFoundException("Could not include " + relativeUrlPath);
				dispatch.include(request, response);
			}
		} finally {
			ThreadState.setPageContext(this);
//			request.setAttribute(PROP, getProp());
			swapAttribute(request, SERVLET_PATH, pathSave);
			swapAttribute(request, REQUEST_URI, uriSave);
			if (rootPath) swapAttribute(request, ROOT, rootSave);
/*			if (dspFlag) swapAttribute(request, DSP, dspSave);*/
			if (request.getAttribute(FORWARD) == null) swapAttribute(request, FORWARD, forwardSave);
			open.setDatabase(dbSave);
			if (!insertType)
			{
				request.removeAttribute(OPEN);
				request.removeAttribute(OUT);
			}
		}
	} // include()

	public void initialize(Servlet page, ServletRequest request,
			ServletResponse response, String errorPageURL, boolean needsSession,
			int bufferSize, boolean autoFlush) throws IOException,
			IllegalStateException, IllegalArgumentException
	{
		this.page = (DspPage)page;
		this.config = servlet.getServletConfig();
		this.application = config.getServletContext();
		this.request = (HttpServletRequest)request;
		this.response = (HttpServletResponse)response;
		this.exception = (Exception)request.getAttribute(EXCEPTION);
		request.removeAttribute(EXCEPTION);
		this.errorPageURL = errorPageURL;
		if (needsSession)
		{
			session = (HttpSession)this.request.getAttribute(SESSION);
			if (session == null) session = this.request.getSession();
			user = new DspUser(session);
		}
		this.var.setRequest((HttpServletRequest)request);
		DspProp prop = this.page.prop;
//		this.request.setAttribute(PROP, prop);
		if (errorPageURL == null)
		{
			try {
				URL errorURL = prop.getURL("errorPage");
				if (errorURL != null) this.errorPageURL = '/' + errorURL.getFile();
			} catch (DspException e) {
				e.printStackTrace();
			}
		}
		if (insertType)
		{
			open = (DspOpen)request.getAttribute(OPEN);
			out = (JspWriter)request.getAttribute(OUT);
		}
		else
		{
			out = new DspWriter(bufferSize, autoFlush, this.response);
			try {
				open.unset(DEBUG);
				open.unset(TRACE);
			} catch (DspException e) {}
			prop.preSet(open, DspOpen.NAME);
		}
		prop.preSet(this, NAME);
		try {
			open.setDatabase(prop.getFirstDatabase());
		} catch (DspException e) {
			ThreadState.logln(e);
			throw new IOException("Could not connect to the initial database");
		}
	} // initialize()

	public boolean isCalled() { return insertType; }

	public Iterator<String> names()
	{
		ArrayList<String> list = new ArrayList<String>();
		Enumeration<String> it = vars.keys();
		while (it.hasMoreElements())
		{
			list.add(it.nextElement());
		}
		return list.iterator();
	} // names()

	public String normalizePath(String path) {
		return normalizePath(request, path);
	} // normalizePath()

	public static String normalizePath(HttpServletRequest request, String path)
	{
		DspPage page = ThreadState.getPage();
		boolean debug = false;
		boolean trace = false;
		if (page != null) {
			debug = page.getProp().getDebug();
			trace = page.getProp().getTrace();
		}
		if (trace) ThreadState.logln("normalizePath(" + path + ')');
    boolean rootPath = path.startsWith("//");
    if (!(rootPath || path.startsWith("/")))
    {
			File dir = new File(DspServlet.getVirtualPath(request)).getParentFile();
			if (debug) ThreadState.logln("dir = " + dir);
			File to = new File(dir, path);
			if (debug) ThreadState.logln("to = " + to);
			path = replace(to.getPath(), File.separator, "/");
			if (path.charAt(0) != '/') path = '/' + path;
			if (request.getAttribute(ROOT) != null) {
				path = '/' + path;
				rootPath = true;
			}
			if (debug) ThreadState.logln("deltaPath = " + path);
    }
    if (rootPath) path = '/' + BZText.normalizePath(path.substring(1));
		else path = BZText.normalizePath(path);
		if (debug) ThreadState.logln("normalized " + path);
		return path;
	} // normalizePath()

	public JspWriter popBody()
	{
		if (outStack.size() > 0)
		{
			out = outStack.pop();
			request.setAttribute(OUT, out);
		}
		return out;
	} // popBody()

	public BodyContent pushBody()
	{
		outStack.push(out);
		out = new DspBodyContent(out);
		request.setAttribute(OUT, out);
		return (BodyContent)out;
	} // pushBody()

	/** This frees up the pageContext so it can be used by the next request.  Note, see
	 * releaseFinal() for the reason why I don't clear 'out' here.
	 */
	public void release()
	{
		debug = trace = false;
		config = null;
		session = null;
		outStack.removeAllElements();
		errorPageURL = null;
		exception = null;
		cookie = null;
		var.release();
		vars.clear();
		user = null;
			// 'open' is only released by the originator, not by inserted contexts.
		if (!insertType) open.release();
	} // release()

	/** This is needed because the Servlet still needs to access 'out' after the page
	 * calls release().  This is because it needs to append the debug log to the page ouput.
	 * So I don't clear the out variable in release().  I clear it here instead.
	 */
	void releaseFinal()
	{
		application = null;
		out = null;
		page = null;
		request = null;
    response = null;
	} // releaseFinal()

	public void removeAttribute(String name)
	{
		if (trace) ThreadState.logln("DspPageContext.removeAttribute(" + name + ')');
		if (name == null) throw new NullPointerException("Attribute name is null");
		if (name.equals(DEBUG)) debug = false;
		else
		if (name.equals(TRACE)) trace = false;
		else vars.remove(name);
		if (debug) ThreadState.logln("unset " + NAME + '.' + name);
	} // removeAttribute()

	public void removeAttribute(String name, int scope)
	{
		if (trace) ThreadState.logln("DspPageContext.removeAttribute(" + name + ", " + scope + ')');
		if (name == null) throw new NullPointerException("Attribute name is null");
		switch (scope) {
			case APPLICATION_SCOPE:
				application.removeAttribute(name);
				break;
			case SESSION_SCOPE:
				if (session != null)
				{
					session.removeAttribute(name);
				}
				break;
			case PAGE_SCOPE:
				removeAttribute(name);
				break;
			case REQUEST_SCOPE:
				request.removeAttribute(name);
				break;
			default: throw new IllegalArgumentException("Invalid Scope");
		}
	} // removeAttribute()

	public void save(String page, String path, boolean append) throws IOException, ServletException
	{
		BodyContent out = pushBody();
		Object forwardSave = swapAttribute(request, FORWARD, null);	// suspend forward
		try {
			include(page, true);
			path = normalizePath(path);
			String realPath = servlet.getRealPath(request, path);
			if (getProp().getDebug()) ThreadState.logln("Saving " + page + " to " + path + ", real " + realPath);
			new File(realPath).getParentFile().mkdirs();
			Writer fout = new FileWriter(realPath, append);
			try {
				out.writeOut(fout);
			} finally {
				try { fout.close(); } catch (IOException e) { ThreadState.logln(e); }
			}
		} finally {
			swapAttribute(request, FORWARD, forwardSave);	// continue forward, if any
			popBody();
		}
	} // save()

	public void set(String variable, Object value) throws DspException, DspReadOnlyException
	{
		if (trace) ThreadState.logln("DspPageContext.set(" + variable + ", " + value + ')');
		if (value == null) removeAttribute(variable);
		else setAttribute(variable, value);
	} // set()

	public void setAttribute(String name, Object value)
	{
		if (trace) ThreadState.logln("DspPageContext.setAttribute(" + name + ", " + value + ')');
		if (name == null) throw new NullPointerException("Attribute name is null");
		if (value == null) throw new NullPointerException("Attribute value is null");
		if (name.equals(DEBUG)) try { debug = _boolean(value); } catch (NumberFormatException e) {}
		else
		if (name.equals(TRACE)) try { trace = _boolean(value); } catch (NumberFormatException e) {}
		else vars.put(name, value);
		if (debug) ThreadState.logln(NAME + '.' + name + " <= " + value);
	} // setAttribute()

	public void setAttribute(String name, Object value, int scope)
	{
		if (name == null) throw new NullPointerException("Attribute name is null");
		if (value == null) throw new NullPointerException("Attribute value is null");
		switch (scope)
		{
			case APPLICATION_SCOPE:
				application.setAttribute(name, value);
				break;
			case SESSION_SCOPE:
				if (session != null) session.setAttribute(name, value);
				break;
			case PAGE_SCOPE:
				setAttribute(name, value);
				break;
			case REQUEST_SCOPE:
				request.setAttribute(name, value);
				break;
			default: throw new IllegalArgumentException("Invalid Scope");
		}
	} // setAttribute(scope)

	public void setOut(JspWriter out) { this.out = out; }

	public static Object swapAttribute(HttpServletRequest req, String name, Object value)
	{
//System.out.println("swapping " + name + " to " + value);
		Object result = req.getAttribute(name);
		if (value == null) req.removeAttribute(name);
		else req.setAttribute(name, value);
		return result;
	} // swapAttribute()

	public JspWriter target(String path) throws DspException, IOException
	{
//System.out.println("target path " + path);
//System.out.println("pre-normalized path " + path);
		path = normalizePath(path);
//System.out.println("post-normalized path " + path);
		String realPath = servlet.getRealPath(request, path);
//		path = BZFile.pathBack(realPath, path);
//System.out.println("pathBack " + path);
		out = new TargetWriter(out, response, "//" + realPath, realPath);
//		out = new DspTargetWriter(out, response, path, realPath);
		request.setAttribute(OUT, out);
		request.setAttribute(TARGET, out);
		return out;
	} // target()

	public void unset(String variable) throws DspException
	{
		if (trace) ThreadState.logln("DspPageContext.unset(" + variable + ')');
		removeAttribute(variable);
	} // unset()

	@SuppressWarnings("deprecation")
	@Override
	public ExpressionEvaluator getExpressionEvaluator() {
		// TODO Auto-generated method stub
		return null;
	}

	@SuppressWarnings("deprecation")
	@Override
	public VariableResolver getVariableResolver() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ELContext getELContext() {
		// TODO Auto-generated method stub
		return null;
	}

} // DspPageContext
