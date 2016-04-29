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

import java.io.*;		// IOException, InputStream
import java.net.*;	// MalformedURLException, URL, URLConnection
import java.util.*;	// Properties, Vector, WeakHashMap

//import java.util.Collection;
//import java.util.Iterator;
//import java.util.WeakHashMap;	// JBuilder bug
import javax.servlet.*;	// ServletConfig, ServletException
import javax.servlet.http.*;	// HttpServletRequest, HttpServletResponse
import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.tagext.BodyContent;

import static com.dsp.util.BZCast._boolean;
import static com.dsp.util.BZCast._String;
import static com.dsp.util.BZText.base64Decode;
import static com.dsp.util.BZText.normalizePath;

public class DspProp implements DspObject
{
	public static final String NAME = "prop";

//	public static final String DEFAULT_DB				= "DB,Default";
	public static final int		 DEF_CON_INT		= -1;
	public static final String FOLDER_NAME			= "folderName";
	public static final String FOLDER_PARENT		= "folderParent";
	public static final String FOLDER_PATH			= "folderPath";
	public static final String AUTHORIZING			= "com.dsp.authorizing";
	public static final String COOKIE				= "DSP_Authorization";
	public static final String AUTHORIZED			= "d_authorized";
//	public static final String GET_CONNECTIONS	= "connections";
//	public static final String DEF_CONNECTIONS	= String.valueOf(DEF_CON_INT);
	public static final String PROPERTY_FILE		= "WEB-INF/dsp.properties";

	private static final int AUTH_BASIC		= 0;
	private static final int AUTH_COOKIE	= 1;
	private static final int AUTH_SESSION	= 2;

	private DspServlet	servlet;
	private DspProp		parent;	// used when this folder has no properties file
	private Database	firstDB;
	private boolean		debug = false, trace = false, checkAuth = false;

	private Hashtable<String, Database>	dbs;
	private Properties	props;
	private File		file;		// cached File object
	private URL			url, propsUrl;
	private long		timestamp;
	private String		authPage;
	private int			authMethod;
	private ProxLoader	loader;

	public DspProp(URL url, DspServlet servlet) throws IOException
	{
		this.servlet = servlet;
		this.url = url;
		propsUrl = new URL(url, PROPERTY_FILE);
		load();
		if (Mode.CONTEXT_LOADER) {
			loader = new ProxLoader(getFile());
		}
		if (trace) ThreadState.logln("DspProp(" + url + ')');
	} // DspProp()

	private Object addDB(String database/*, String conStr*/) throws DspException
	{
		if (parent != null) return parent.addDB(database);
		if (trace) ThreadState.logln("DspProp.addDB(" + database + /*", " + conStr +*/ ")");

		String lower = database != null ? database.toLowerCase() : null;
		Database db = lower != null ? dbs.get(lower) : null;
		if (db != null) return db;
		Object result;
		db = new DspDatabase(database, this, servlet);
		result = db;
		dbs.put(db.getName().toLowerCase(), db);
		if (firstDB == null) firstDB = db;
		return result;
	} // addDB()

	public boolean authorize(DspPageContext pageContext) throws IOException, ServletException
	{
		if (parent != null) return parent.authorize(pageContext);
		boolean result = false;	// authorized, or no athorization needed
		HttpServletRequest request = (HttpServletRequest)pageContext.getRequest();
			// if we're authorizing now then just return
		if (request.getAttribute(AUTHORIZING) != null) return result;
		DspObject var = pageContext.getVar();
		if (!checkAuth)
		{
			authPage = (String)get("authorize.page");
			String method = (String)get("authorize.method");
			if (method != null) method = method.toLowerCase();
			if (method == null || method.equals("basic")) authMethod = AUTH_BASIC;
			else
			if (method.equals("cookie")) authMethod = AUTH_COOKIE;
			else authMethod = AUTH_SESSION;
			checkAuth = true;
		}
		HttpSession session = null;
		if (authPage != null)
		{
			switch (authMethod)
			{
			case AUTH_COOKIE:
				Cookie[] cookies = request.getCookies();
				if (cookies != null)
				{
					for (int ix = 0, ixz = cookies.length; ix < ixz; ix++)
					{
						Cookie cookie = cookies[ix];
						if (cookie.getName().equals(COOKIE))
						{
							if (debug) ThreadState.logln("Authorization: " + cookie.getValue());
							var.set(AUTHORIZED, cookie.getValue());
						}
					}
				}
				break;
			case AUTH_SESSION:
				session = request.getSession();
				Object auth1 = session.getAttribute("DSP_Authorization");
				if (auth1 != null)
				{
					if (debug) ThreadState.logln("Authorization: " + auth1);
					var.set(AUTHORIZED, auth1);
				}
				break;
			default:
				try {
					String auth = request.getHeader("Authorization");
					if (debug)
					{
						ThreadState.logln("Authorization: " + auth);
					/*Enumeration e = request.getHeaderNames();
						while (e.hasMoreElements())
						{
							String head = (String)e.nextElement();
							DspState.log(head);
							DspState.log(": ");
							Enumeration e1 = request.getHeaders(head);
							boolean comma = false;
							while (e1.hasMoreElements())
							{
								String value = (String)e1.nextElement();
								if (comma) DspState.log(", ");
								else comma = true;
								DspState.log(value);
							}
							DspState.logln("");
						}*/
					}

					String userName = null, password = null;
						// If it doesn't start with BASIC then let the user log in
					if (auth != null && auth.toLowerCase().startsWith("basic "))
					{
							// Decode the authoization string
						auth = base64Decode(auth.substring(6));
							// Colon delimits the user name from the password
						int ix = auth.indexOf(':');
						// If not colon then something's wrong
						if (ix >= 0)
						{
							// Get user name and password
							userName = auth.substring(0, ix);
							password = auth.substring(ix + 1);
						}
					}
//				if (debug)
//				{
//					DspState.logln("userName: " + userName);
//					DspState.logln("password: " + password);
//				}
					var.set("d_username", userName);
					var.set("d_password", password);
				} catch (ClassCastException e) {}
			} // switch
			request.setAttribute(AUTHORIZING, "1");
			BodyContent out2 = pageContext.pushBody();
			pageContext.include(authPage);
			JspWriter out1 = pageContext.popBody();
	//				request.removeAttribute(AUTHORIZING);
			Object auth2 = var.get(AUTHORIZED);
			if (auth2 == null)
			{
				result = true;	// authorization failed, don't execute page
				out1.print(out2.getString());	// send the authorization page output to the client
			}
			HttpServletResponse response = (HttpServletResponse)pageContext.getResponse();
			if (result)
			{
				if (authMethod == AUTH_BASIC)
				{
					String realm = _String(get("authorize.realm"));
					if (realm == null)
					{
						realm = url.getFile();
					}
					response.setHeader("WWW-Authenticate", "BASIC realm=\"" + realm + '"');
					response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
				}
			}
			else
			{
				switch (authMethod)
				{
				case AUTH_COOKIE:
					Cookie cookie = new Cookie(COOKIE, auth2.toString());
					cookie.setMaxAge(-1);
					cookie.setPath("/");
					response.addCookie(cookie);
					break;
				case AUTH_SESSION:
					session.setAttribute(COOKIE, auth2);
					break;
				}
			}
		} // if authPage
		return result;
	} // authorize()

	public void close()
	{
		if (dbs != null)
		{
			if (trace) ThreadState.logln("DspProp.close()");
			Collection<Database> col = dbs.values();
			dbs = null;
			Iterator<Database> it = col.iterator();
			while (it.hasNext())
			{
				Database db = it.next();
				db.close();
			}
		}
	} // close()

	public boolean exists(String variable) throws DspException
	{
		if (parent != null) return parent.exists(variable);
		if (trace) ThreadState.logln("DspProp.exists(" + variable + ")");
		Object result = get(variable);
		if (debug) ThreadState.logln(NAME + '.' + variable + (result != null ? " exists" : " does not exist"));
		return result != null;
	} // exists()

	public void finalize()
	{
		close();
	}	// finalize()

	public Object get(String variable) throws DspException
	{
		return get(variable, null);
	} // get()

	public Object get(String variable, Object defaultValue)
			throws DspException
	{
		if (parent != null) return parent.get(variable, defaultValue);
		if (trace) ThreadState.logln("DspProp.get(" + variable + ")");
		load();
		Object result;
		if (FOLDER_NAME.equalsIgnoreCase(variable)) result = url.getFile();	//getName();
//		else
//		if (FOLDER_PARENT.equalsIgnoreCase(variable)) result = getProp();
//		else
//		if (FOLDER_PATH.equalsIgnoreCase(variable)) result = url.getPath();	//getPath();
		else
		{
			result = props.get(variable);
			if (result == null) result = defaultValue;
		}
		if (debug) ThreadState.logln(NAME + '.' + variable + " => " + result);
		return result;
	} // get()

	public static int getContentLength(URLConnection conn)
	{
		URL url = conn.getURL();
		if (url.getProtocol().equals("file"))
		{
			File file = new File(url.getFile());
			return (int)file.length();
		}
		return conn.getContentLength();
	} // getContentLength

	public Database getDatabase(String name) throws DspException
	{
		if (parent != null) return parent.getDatabase(name);
		if (trace) ThreadState.logln("DspProp.getDatabase(" + name + ")");
		load();
		Database result = dbs.get(name.toLowerCase());
		if (result == null)
		{
//DspState.logln("Databases: " + dbs);
			throw new DspException("Database " + name + " cannot be found");
		}
		return result;
	} // getDatabase()

	public boolean getDebug() { return debug; }
	public boolean getTrace() { return trace; }

	public File getFile() throws IllegalStateException
	{
//		if (parent != null) return parent.getFile();
//System.err.println("Prop File URL: " + url);
//System.err.println("URL Host: " + url.getHost());
//System.err.println("URL File: " + url.getFile());
		if (file != null) return file;
		if (url.getProtocol().equals("file"))
		{
			return file = new File(url.getFile());
		}
		if (url.getProtocol().equals("jndi")) {
			String uri = url.toString();
			int s2 = uri.indexOf("/", 6);
			String host;
			if (s2 > 0) {
				host = uri.substring(6, s2);
			} else {
				host = uri.substring(6);
			}
			String domainPath = servlet.getDomainPath(host);
			if (domainPath != null) return new File(domainPath);
		}
		throw new IllegalStateException("prop URL is an unknown protocol; " + url);
	} // getFile()

	/** Returns a file object from the requested property.  It will convert relative
	 * paths to absolute paths.  Absolute paths begin with a slash.
	 */
	public File getFile(String name) throws DspException
	{
		String path = (String)get(name);
		if (path == null) return null;
		if (path.startsWith("/")) return new File(path);
		if (parent != null) return new File(parent.getFile(), path);
		return new File(getFile(), path);
	} // getFile()

/*	public File getFile() throws IllegalStateException
	{
//		if (parent != null) return parent.getFile();
System.err.println("Prop File URL: " + url);
		if (file != null) return file;
		if (url.getProtocol().equals("file"))
		{
			return file = new File(url.getFile());
		}
		throw new IllegalStateException("prop is not using FILE: protocol");
	} // getFile()
*/
	public Database getFirstDatabase()
	{
		if (trace) ThreadState.logln("DspProp.getFirstDatabase()");
		if (parent != null) return parent.getFirstDatabase();
		load();
		if (debug) ThreadState.logln("prop.getFirstDatabase() -> " + firstDB);
		return firstDB;
	} // getFirstDatabase()

/*
	public DspProp getProp() throws DspException
	{
		if (trace) DspState.logln("DspProp.getProp()");
		if (parent == null)
		{
			String p = this.getParent();
			if (p != null)
			{
				try { parent = DspFactory.getDefaultFactory().getProp(p); }
				catch (IOException e) { throw new DspException(e.toString()); }
			}
		}
		return parent;
	} // getProp()
*/
	public static long getLastModified(URLConnection conn)
	{
		URL url = conn.getURL();
//System.out.println("getLastModified url " + url);
		long result;
		if (url.getProtocol().equals("file"))
		{
			File file = new File(url.getFile());
//System.out.println("getLastModified file " + file);
			result = file.lastModified();
		}
		else result = conn.getLastModified();
//System.out.println("getLastModified -> " + result);
		return result;
	} // getLastModified()

/*
	public String getParam(String name)
	{
		return getParam(name, null);
	} // getParam()

	public String getParam(String name, String defaultValue)
	{
		if (trace) DspState.logln("DspProp.getParam(" + name + ", " + defaultValue + ")");
//		load();
		String param = props.getProperty(name.toLowerCase());
		if (param == null) param = defaultValue;
		if (debug) DspState.logln(NAME + '.' + name + " => " + param);
		return param;
	} // getParam()
*/
	public Properties getParams()
	{
		if (parent != null) return parent.getParams();
		if (trace) ThreadState.logln("DspProp.getParams()");
//		load();
		return props;
	} // getParams()

	public String getPath() { return url.getFile(); }

	public URL getURL() { return url; }

	/** Returns a URL object from the requested property.  It will convert relative
	 * paths to absolute URLs.
	 */
	public URL getURL(String name) throws DspException
	{
		String path = (String)get(name);
		if (path == null) return null;
//		if (path.startsWith("/")) return new File(path);
		try {
			if (parent != null) return new URL(parent.getURL(), path);
			return new URL(getURL(), path);
		} catch (MalformedURLException e) {
			throw new DspException(e);
		}
	} // getURL()

	private void init() //throws DspException, IOException
	{
		if (parent != null) return;
		checkAuth = false;
		preSet(this, NAME);
//		set(DEBUG, get(NAME + ".debug"));
//		set(TRACE, get(NAME + ".trace"));
		if (trace) ThreadState.logln("DspProp.init()");
		close();	// close the current database connections before opening new ones
		dbs = new Hashtable<String, Database>();
		String databases = null, database = null;
		try {
			databases = (String)get("databases");
			database = (String)get("database");
		} catch (DspException e) {}
		if (databases == null)
		{
			if (database != null)
			{
				try {
					addDB(null);
//					dbs.put(DEFAULT_DB.toLowerCase(), addDB(null));
				} catch (DspException e) {
					ThreadState.logln("Couldn't add the database");
					ThreadState.logln(e);
				}
			}
		}
		else
		{
			StringTokenizer tok = new StringTokenizer(databases, ",");
//			boolean first = true;
			while (tok.hasMoreTokens())
			{
				database = tok.nextToken().trim();

				try {
					/*Object result =*/ addDB(database);
//					if (first)
//					{
//						database = DEFAULT_DB.toLowerCase();
//						dbs.put(DEFAULT_DB.toLowerCase(), result);
//						first = false;
//					}
				} catch (DspException e) {
					ThreadState.logln("Couldn't add the database " + database);
					ThreadState.logln(e);
				}
			}
		}
	} // init()

	private synchronized void load()
	{
		if (parent != null)
		{
			parent.load();
			return;
		}
		boolean changed = props == null;
//		boolean hasProps = propsFile.exists();
		InputStream in = null;
		try {
			URLConnection conn = propsUrl.openConnection();
//		if (hasProps)
//		{
//			long newTime = propsFile.lastModified();
			long newTime = getLastModified(conn);
			changed |= newTime != timestamp;
			timestamp = newTime;
			if (!changed) return;

			if (trace) ThreadState.logln("DspProp.load(" + /*propsFile*/ url + ')');
			props = new Properties();
//		loadProps(propsFile);
			loadProps(in = conn.getInputStream());
			init();
		} catch (IOException e) {
			if (debug) ThreadState.logln(e.toString());
			if (props.size() == 0)
			{
					// couldn't load any props.  Let's use the props from my containing folder
				try {
					if (url.getFile().length() > 1)
					{
						URL parentUrl = new URL(normalizePath(url.toString() + "/../"));
//System.out.println("loading parent " + parentUrl);
						parent = DspFactory.getDefaultFactory().getProp(parentUrl);
						parent.load();
					}
				} catch (Exception e1) {
					e1.printStackTrace();
				}
			}
		} finally {
			if (in != null) try { in.close(); } catch (IOException e) {}
		}
		if (debug && props.size() == 0) ThreadState.logln("Properties are missing from " + this);
	} // load()

//	private boolean loadProps(File parent, String file)
//	{
//		if (trace) DspState.logln("DspProp.loadProps(" + parent + ", " + file + ')');
//		if (file.startsWith("/") || file.startsWith(File.separator)) return loadProps(new File(file));
//		return loadProps(new File(parent, file));
//	} // loadProps()

//	private boolean loadProps(File file)
	private boolean loadProps(InputStream conn)
	{
		if (parent != null) return parent.loadProps(conn);
		if (trace) ThreadState.logln("DspProp.loadProps(" + /*file*/ url + ')');
		boolean found = false;
//		if (file.exists())
//		{
			BufferedReader in = null;
			try {
//				in = new DataInputStream(new BufferedInputStream(new FileInputStream(file)));
				in = new BufferedReader(new InputStreamReader(conn));
				for (;;) {
					in.mark(256);
					String line = in.readLine().trim();
//System.out.println("line: " + line);
					if (line == null) return found;
					if (line.length() == 0 || line.startsWith("#")
							|| line.startsWith("!")) continue;
					if (line.startsWith("import ") || line.startsWith("include ")
							|| line.startsWith("insert "))
					{
//						found = loadProps(file.getParentFile(), line.substring(7).trim());
						InputStream in2 = null;
						try {
							URL other;
							String path = line.substring(7).trim();
							if (path.startsWith("/")) other = new URL("file", "", path);
							else other = new URL(url, path);
							if (debug) ThreadState.logln("Including props from " + url);
							URLConnection conn2 = other.openConnection();
							in2 = conn2.getInputStream();
							found = loadProps(in2);
						} catch (IOException e) {
						} finally {
							if (in2 != null) try { in2.close(); } catch (IOException e) {}
						}
						continue;
					}
					in.reset();
					break;
				}
				props.load(in);
//				Enumeration keys = props2.keys();
//				Enumeration vals = props2.elements();
//				while (keys.hasMoreElements())
//				{
//					found = true;
//					String key = ((String)keys.nextElement()).toLowerCase();
//					props.put(key, vals.nextElement());
//				}
			} catch (IOException e) {
				ThreadState.logln(e);
			} finally {
				if (in != null)
				{
					try { in.close(); } catch (IOException e) {}
					in = null;
				}
			}
//		}
		return found;
	} // loadProps()

	@Override
	public Iterator<String> names()
	{
		if (parent != null) return parent.names();
		ArrayList<String> list = new ArrayList<String>();
		Enumeration<Object> it = props.keys();
		while (it.hasMoreElements())
		{
			list.add((String) it.nextElement());
		}
		return list.iterator();
	} // names()

	void preSet(DspObject object, String name)
	{
		if (parent != null)
		{
			parent.preSet(object, name);
			return;
		}
		name += '.';
		int len = name.length();
		load();
		if (props != null)
		{
			ArrayList<Object> keys = new ArrayList<Object>();
			keys.addAll(props.keySet());
			for (int ix = 0, ixz = keys.size(); ix < ixz; ix++)
			{
				String key = (String) keys.get(ix);
//System.out.println("key " + key);
				if (key.startsWith(name))
				{
					String valStr = props.getProperty(key);
					Object val;
					try {
						if (valStr.indexOf('.') >= 0) val = Double.valueOf(valStr);
						else val = Integer.valueOf(valStr);
					} catch (NumberFormatException e) {
						val = valStr;
					}
					String var = key.substring(len);
					try {
						if (debug) ThreadState.logln(name + var + " <= " + val);
						object.set(var, val);
					} catch (DspException e) {
						ThreadState.log("Couldn't set " + var + " to " + val);
						ThreadState.logln(e);
					}
				}
			}
		}
	} // preSet()

//	public Object run(String function, Object[] args) throws DspException
//	{
//		if (trace) DspState.logln("DspProp.run(" + function + ", " + args.length + " args)");
//		throw new DspException(NAME + "." + function + "() is not defined");
//	} // run()

	public void set(String variable, Object value) throws DspReadOnlyException
	{
		if (parent != null)
		{
			parent.set(variable, value);
			return;
		}
		if (trace) ThreadState.logln("DspProp.set(" + variable + ", " + value + ')');
		if (value == null)
		{
			unset(variable);
			return;
		}
		else
		if (variable.equals(DEBUG)) try { debug = _boolean(value); } catch (NumberFormatException e) {}
		else
		if (variable.equals(TRACE)) try { trace = _boolean(value); } catch (NumberFormatException e) {}
		props.put(variable, value);
		if (debug) ThreadState.logln(NAME + '.' + variable + " <= " + value);
	} // set()

	public String toString()
	{
		return "prop [" + url + ']';
	} // toString()

	public void unset(String variable) throws DspReadOnlyException
	{
		if (parent != null)
		{
			parent.unset(variable);
			return;
		}
		if (trace) ThreadState.logln("DspProp.unset(" + variable + ')');
		if (variable.equals(DEBUG)) debug = false;
		else
		if (variable.equals(TRACE)) trace = false;
		props.remove(variable);
		if (debug) ThreadState.logln("unset " + NAME + '.' + variable);
	} // unset()

	public ClassLoader useLoader() {
		if (Mode.CONTEXT_LOADER) {
			Thread thread = Thread.currentThread();
			ClassLoader old = thread.getContextClassLoader();
			thread.setContextClassLoader(loader);
			return old;
		} else {
			return servlet.getClass().getClassLoader();
		}
	} // useLoader()

} // DspProp
