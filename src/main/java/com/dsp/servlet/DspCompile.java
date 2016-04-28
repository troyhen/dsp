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

import com.dsp.DspCookies;
import com.dsp.DspException;
import com.dsp.DspFactory;
import com.dsp.DspOpen;
import com.dsp.DspPage;
import com.dsp.DspPageContext;
import com.dsp.DspProp;
import com.dsp.DspRequest;
import com.dsp.DspStatement;
import com.dsp.DspUser;

import java.io.*;			// IOException, FileNotFoundException
import java.net.*;			// URL, URLConnection
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.HttpJspPage;

import sun.tools.javac.*;

class DspCompile
{
	static final boolean			DEBUG = false;

	private static DspLoader		loader = null;

	private DspServlet				servlet;
	private ArrayList<Token>		tokens;
	private String					pkg, fileName, className, fullName, classRoot;
	private File					javaFile, classFile;
	private URL						pageUrl;
//	private String					pagePath;
	private PrintWriter				out;
	private int						level;
	private ByteArrayOutputStream	errors;
	private long					pageDate, newDate;
	private DspPage				page;
	private DspProp				prop;
	private ArrayList<String>		specials;
	private boolean					flush;	// one time flag to force recompile at start-up
	private URLConnection			conn;
	private ArrayList<DspCompile>	inserts;
	private DspParse				parse;
	private String					isErrorPage;

	DspCompile(DspServlet servlet, HttpServletRequest req, URL url,
			boolean flush) throws IOException, DspException
	{
		if (DEBUG) System.out.println("DSP compiler page: " + url);
		this.servlet = servlet;
		pageUrl = url;
		this.flush = flush;
		String fullPath = url.getFile();
		int ix = fullPath.lastIndexOf('/');
		if (ix >= 0)
		{
			fileName = fullPath.substring(ix + 1);
			fullPath = DspPage.replace(fullPath.substring(0, ix).toLowerCase(), ":", "_");
		}
		else
		{
			fileName = fullPath;
			fullPath = "";
		}
		pkg = pathToPackage(fullPath);
		className = fileToClass(fileName);
		fullName = pkg + '.' + className;
		if (DEBUG) System.out.println("DSP compiler class: " + fullName);
		pageDate = Long.MIN_VALUE;
		String tempDir = servlet.getTempFolder();
		String javaRoot = tempDir + File.separator + "dsp" + File.separator + "source";
		javaFile = new File(javaRoot + fullPath + File.separator + className + ".java");
		if (DEBUG) System.out.println("DSP compiler source: " + javaFile);
		classRoot = tempDir + File.separator + "dsp" + File.separator + "class";
		classFile = new File(classRoot + fullPath + File.separator + className + ".class");
		if (DEBUG) System.out.println("DSP compiler class: " + classFile);
		String propUrl = pageUrl.toString();
		ix = propUrl.lastIndexOf('/');
		propUrl = propUrl.substring(0, ix + 1);
		prop = DspFactory.getDefaultFactory().getProp(new URL(propUrl));
	} // DspCompile()

	/** Add a name from the specials list.  This list specifies which objects are of type
	 * DspObject and need special treatment when parsing and converting to java.  User's
	 * can create dynamic objects using the #name syntax in any statement.  These objects
	 * are only in scope until the {else} or {end} for the declarinig statement.
	 * @see addSpecial(String)
	 */
	void addSpecial(String name, int index) throws DspParseException
	{
		if (specials.indexOf(name) >= 0)
		{
			throw new DspParseException("The name #" + name + " is already in use", index, this);
		}
		specials.add(name);
	} // addSpecial()

	@SuppressWarnings("deprecation")
	private boolean buildClass() throws DspException, IOException, NoClassDefFoundError
	{
		if (DEBUG) System.out.println("DSP Compiling " + classFile);
		String as[];
		as = new String[5];
		int ix = 0;
		as[ix++] = "-classpath";
		as[ix++] = classPath();
		if (DEBUG) System.out.println("DSP Compiler classpath: " + as[ix - 1]);
//System.out.println("DSP System classpath: " + System.getProperty("java.class.path"));
		as[ix++] = "-d";
		as[ix++] = classRoot;
		as[ix++] = javaFile.getAbsolutePath();	//path + '/' + javaFile.getName();
		errors = new ByteArrayOutputStream();
		Main main = null;
//		try
//		{
			main = new Main(errors, "DSP Compiler");
/*		}
		catch(NoClassDefFoundError e)
		{
			System.out.println("DSP Servlet exception: " + e);
			e.printStackTrace();
			ServletOutputStream servletoutputstream1 = httpservletresponse.getOutputStream();
			servletoutputstream1.println("<html><head><title>Error getting compiled page</title></head>");
			servletoutputstream1.println("<body><h1>Error getting compiled page</h1>");
			servletoutputstream1.println("<p>The class sun.tools.javac.Main could not be found.");
			servletoutputstream1.println("<p>Verify you have a JDK installed and if you are using JDK 1.2 then verify ");
			servletoutputstream1.println("tools.jar has been added to the classpath as described in the User Guide.");
			servletoutputstream1.println("</body></html>");
			return false;
		}*/
		new File(classFile.getParent()).mkdirs();
		if (!main.compile(as))
		{
//			ServletOutputStream servletoutputstream = httpservletresponse.getOutputStream();
//			servletoutputstream.println("<html><head><title>Error compiling page</title></head>");
//			servletoutputstream.println("<body><h1>Error compiling page</h1><pre>");
//			servletoutputstream.println(out.toString());
//			servletoutputstream.println("</pre></body></html>");
			return false;
		}
//		if(out.size() > 0)
//			System.out.print(out.toString());
		return true;
	} // buildClass()

	private void buildJava(HttpServletRequest req) throws IOException, DspException
	{
		if (DEBUG) System.out.println("DSP compiler file: " + javaFile);
//		tempCount = 1;
		new File(javaFile.getParent()).mkdirs();
		out = new PrintWriter(new FileOutputStream(javaFile));
		try {
			do1Top();
			do2Imports();
			do3Class();
			do4FuncTop();
			out.print(do5Tokens(req));
			do6FuncEnd();
			out.print(do7Members());
			do8Close();
		} finally {
			out.close();
			out = null;
		}
	} // buildJava()

	private String classPath() throws DspException {
		StringBuilder buf = new StringBuilder();
		String sep = File.pathSeparator;
		URL[] urls = ((URLClassLoader) DspServlet.class.getClassLoader()).getURLs();
		for (URL url : urls) {
			if ("file".equalsIgnoreCase(url.getProtocol())) {
				if (buf.length() > 0) buf.append(sep);
				buf.append(url.getPath());
			}
		}
		if (DEBUG) System.out.println("-cp " + buf);
		return buf.toString();
//		StringBuilder buf = new StringBuilder();
//		buf.append(servlet.get("classPath", System.getProperty("java.class.path")));
//		if (pageUrl.getProtocol().equalsIgnoreCase("file")) {
//			File file = new File(new File(pageUrl.getFile()).getParentFile(), "WEB-INF/classes");
//			if (file.exists() && file.isDirectory()) {
//				buf.append(File.pathSeparatorChar);
//				buf.append(file);
//			}
//			file = new File(file.getParentFile(), "lib");
//			if (file.exists() && file.isDirectory()) {
//				File[] list = file.listFiles();
//				for (int ix = 0, ixz = list.length; ix < ixz; ix++) {
//					buf.append(File.pathSeparatorChar);
//					buf.append(list[ix]);
//				}
//			}
//		}
//		if (DEBUG) System.out.println("-cp " + buf);
//		return buf.toString();
	} // classPath()

	/**
	 * Clean up after the compile and call all of the inserted page compilers to do the same.
	 * Called by getPage().
	 */
	private void closeInserts()
	{
		parse = null;
		int ixz;
		if (inserts != null && (ixz = inserts.size()) > 0)
		{
			for (int ix = 0; ix < ixz; ix++)
			{
				((DspCompile)inserts.get(ix)).closeInserts();
			}
		}
	} // closeInserts()

	private void do1Top()
	{
		level = 0;
		out.println("// Do not edit this file.  It is automatically generated from the page " + fileName);
		out.println();
		out.println("package " + pkg + ';');
		out.println();
	} // do1Top()

	private void do2Imports()
	{
		out.println("import com.dsp.*;");
		out.println("import java.io.*;");
		out.println("import javax.servlet.*;");
		out.println("import javax.servlet.http.*;");
		out.println("import javax.servlet.jsp.*;");
		out.println("import javax.servlet.jsp.tagext.BodyContent;");
		for (int ix = 0, ixz = tokens.size(); ix < ixz; ix++)
		{
			try {
				Page page = (Page)tokens.get(ix);
				String importVal = page.getImport();
				if (importVal != null && importVal.length() > 0)
				{
					StringTokenizer tok = new StringTokenizer(importVal, ",");
					while (tok.hasMoreTokens())
					{
						out.print("import ");
						out.print(tok.nextToken());
						out.println(";");
					}
				}
			} catch (ClassCastException e) {
			}
		}
		out.println();
	} // do2Imports()

	private void do3Class()
	{
		out.println("public class " + className + " extends DspPage {");
		level++;
		doTabs();
		out.println("static DspFactory _factory = DspFactory.getDefaultFactory();");
		out.println();
/*		doTabs();
		out.print("public long getId() { return ");
		out.print(random.nextLong());
		out.println("L; }");
		out.println();*/
	} // do3Class()

	private void do4FuncTop()
	{
		String errorPageURL = null;
		String needsSession = null;
		String bufferSize = null;
		String autoFlush = null;
		String contentType = null;
		isErrorPage = null;
		for (int ix = 0, iz = tokens.size(); ix < iz; ix++)
		{
			try {
				Page token = (Page)tokens.get(ix);
				if (autoFlush == null) autoFlush = token.getAutoFlush();
				if (bufferSize == null) bufferSize = token.getBuffer();
				if (errorPageURL == null)
				{
					errorPageURL = token.getErrorPage();
					if (errorPageURL != null) errorPageURL = '"' + errorPageURL + '"';
				}
				if (needsSession == null) needsSession = token.getSession();
				if (contentType == null)
				{
					contentType = token.getContentType();
					if (contentType != null) contentType = '"' + contentType + '"';
				}
				if (isErrorPage == null) isErrorPage = token.isErrorPage();
			} catch (ClassCastException e) {
			}
		}
		if (errorPageURL == null) errorPageURL = "null";
		if (needsSession == null) needsSession = "true";
		if (bufferSize == null) bufferSize = "JspWriter.DEFAULT_BUFFER";
		if (autoFlush == null) autoFlush = "true";
		doTabs();
		out.println("public void _jspService(HttpServletRequest request, HttpServletResponse response)");
		doTabs();
		out.println("\t\tthrows ServletException, IOException {\r\n");
		level++;
		doTabs();
		out.println("\t// JSP Implicit Objects");
		doTabs();
		out.println("final DspPageContext pageContext = (DspPageContext)_factory.getPageContext(this, request, ");
		doTabs();
		out.print("\t\tresponse, ");
		out.print(errorPageURL);
		out.print(", ");
		out.print(needsSession);
		out.print(", ");
		out.print(bufferSize);
		out.print(", ");
		out.print(autoFlush);
		out.println(");");
		if (contentType != null)
		{
			doTabs();
			out.print("if (!pageContext.isCalled()) response.setContentType(");
			out.print(contentType);
			out.println(");");
		}
		doTabs();
		out.println("JspWriter out = pageContext.getOut();");
		doTabs();
		out.println("final HttpSession session = pageContext.getSession();");
		doTabs();
		out.println("final ServletConfig config = pageContext.getServletConfig();");
		doTabs();
		out.println("final ServletContext application = pageContext.getServletContext();");
		doTabs();
		out.println("Exception exception = pageContext.getException();");
		doTabs();
		out.println("DspPage page = this;\r\n");
		doTabs();
		out.println("\t// DSP Implicit Objects");
		doTabs();
		out.println("final DspObject cookie = pageContext.getCookie();");
		doTabs();
		out.println("PrintStream debugLog = pageContext.getDebugLog();");
		doTabs();
		out.println("final DspObject init = pageContext.getInit();");
		doTabs();
		out.println("final DspOpen open = pageContext.getOpen();");
		doTabs();
		out.print("final String pageFile = \"");
		out.print(fileName);
		out.println("\";");
		doTabs();
		out.print("final String pageName = \"");
		int ix = fileName.indexOf('.');
		if (ix < 0) ix = fileName.length();
		out.print(fileName.substring(0, ix));
		out.println("\";");
		doTabs();
		out.println("DspStatement row = pageContext.getRow();");
		doTabs();
		out.println("PrintStream servletLog = _factory.getServletLog();");
		doTabs();
		out.println("final DspObject temp = pageContext;");
		doTabs();
		out.println("final DspObject user = pageContext.getUser();");
		doTabs();
		out.println("final DspObject var = pageContext.getVar();\r\n");
		doTabs();
		out.println("try {");
		level++;
		doTabs();
		out.println("if (prop.authorize(pageContext)) return;");
	} // do4FuncTop()

	private StringBuilder do5Tokens(HttpServletRequest req) throws IOException, DspException
	{
		StringBuilder buf = new StringBuilder();
		for (int ix = 0, iz = tokens.size(); ix < iz; ix++)
		{
			Token token = (Token)tokens.get(ix);
			String comment = token.getComment();
			if (comment != null)
			{
				int iy = comment.indexOf("\r");
				if (iy >= 0)
				{
					int iy1 = comment.indexOf("\n");
					if (iy1 >= 0 && iy1 < iy) iy = iy1;
					comment = comment.substring(0, iy) + "...";
				}
				doTabs(buf, level + 1);
				buf.append("// ");
				buf.append(comment);
				buf.append("\r\n");
			}
//System.out.println("token " + ix + ": " + token + " (" + token.getClass() + ')');
			try {
				level = ((Output)token).doJava(this, buf, level);
			} catch (ClassCastException e) {
				try {
					Insert insert = (Insert)token;
					String path = insert.getPath(this, ix);
					DspCompile comp = insert(req, path);
					if (inserts == null) inserts = new ArrayList<DspCompile>();
					if (!inserts.contains(comp))
					{
						inserts.add(comp);
					}
            // Note: comp.hasChanged() must be first in the following conditional expression
					if (comp.hasChanged() || comp.tokens == null)
					{
						comp.specials = specials;
						comp.parse();
						comp.specials = null;
					}
					doTabs(buf, level++);
					buf.append("{\r\n");
					comp.level = level;
					buf.append(comp.do5Tokens(req));
					doTabs(buf, --level);
					buf.append("}\r\n");
				} catch (ClassCastException e1) {
				}
			}
		}
		return buf;
	} // do5Tokens()

	private void do6FuncEnd()
	{
		if (isErrorPage == null || isErrorPage.equalsIgnoreCase("false"))
		{
			level--;
			doTabs();
			out.println("} catch (Exception _pageExc) {");
			level++;
//		doTabs();
//		out.println("out.clearBuffer();");
			doTabs();
			out.println("pageContext.handlePageException(_pageExc);");
		}
		level--;
		doTabs();
		out.println("} finally {");
		level++;
		doTabs();
		out.println("_factory.releasePageContext(pageContext);");
		while (level-- > 1)
		{
			doTabs();
			out.println("}");
		}
	} // do6FuncEnd()

	private StringBuilder do7Members() throws DspParseException
	{
		StringBuilder buf = new StringBuilder();
		buf.append("\r\n");
		for (int ix = 0, iz = tokens.size(); ix < iz; ix++)
		{
			try {
				Declaration token = (Declaration)tokens.get(ix);
//System.out.println("token " + ix + ": " + token + " (" + token.getClass() + ')');
				token.doMember(this, buf);
				buf.append("\r\n\r\n");
			} catch (ClassCastException e) {
			}
		}
		int ixz;
		if (inserts != null && (ixz = inserts.size()) > 0)
		{
			for (int ix = 0; ix < ixz; ix++)
			{
				DspCompile comp = (DspCompile)inserts.get(ix);
				buf.append(comp.do7Members());
			}
		}
		return buf;
	} // do7Members()

	private void do8Close()
	{
		level--;
		doTabs();
		out.println("}");
	} // do8Close()

	private void doTabs()
	{
		for (int ix = 0; ix < level; ix++)
		{
			out.print('\t');
		}
	} // doTabs()

	public static void doTabs(StringBuilder buf, int level)
	{
		for (int ix = 0; ix < level; ix++)
		{
			buf.append('\t');
		}
	} // doTabs()

	static String fileToClass(String name)
	{
		name = Character.toUpperCase(name.charAt(0)) + name.substring(1);
		int iz = name.length();
		StringBuilder buf = new StringBuilder(iz);
		for (int ix = 0; ix < iz; ix++)
		{
			char c = name.charAt(ix);
			if (ix == 0)
			{
				c = Character.toUpperCase(c);
				if (c >= '0' && c <= '9')
				{
					buf.append('A');
				}
			}
			else c = Character.toLowerCase(c);
			if (c <= '.' || (c >= ':' && c <= '?') || (c >= '[' && c <= '`') ||  c >= '{')
			{
				c = '_';
			}
			buf.append(c);
		}
		return buf.toString();
	} // fileToClass()

	String getClassName() { return className; }

	synchronized InputStream getJava(HttpServletRequest req) throws DspException, IOException
	{
		try {
			getPage(req);
		} catch (DspCompileException e) {
		}
		return new FileInputStream(javaFile);
	} // getJava()

	File getJavaFile() { return javaFile; }

	/**
	 * Returns the associated page object.  It will compile the file and reload
	 * the class if it notices that page date has change.
	 */
	synchronized HttpJspPage getPage(HttpServletRequest req) throws DspException, IOException
	{
		if (hasChanged())
		{
			ByteArrayOutputStream err0 = new ByteArrayOutputStream();
			PrintWriter err = new PrintWriter(err0);
			page = null;
			try {
				parse();
				buildJava(req);
				javaFile.setLastModified(newDate);
				boolean result = buildClass();
				if (errors.size() > 0)
				{
					err.println(errors);
				}
				if (result)
				{
					classFile.setLastModified(newDate);
					loader = new DspLoader(classRoot, Mode.CONTEXT_LOADER ? null : servlet.getClass().getClassLoader());	// need a new loader when redefining a class
				}
				else
				{
					err.flush();
					throw new DspCompileException(err0.toString());
				}
			} catch (DspParseException e) {
//e.printStackTrace();
//				err.print(parse.getError(pageFile.getName(), e.getTokenIndex()));
				err.print(e.getCompiler().parse.getError(e.getCompiler().fileName, e.getTokenIndex()));
				err.flush();
				throw new DspCompileException(e.getMessage() + "\r\n" + err0.toString());
			} finally {
				if (errors != null) errors.close();
				errors = null;
				tokens = null;
				specials = null;
				closeInserts();
			}
		}
		if (page == null)
		{
			if (loader == null) loader = new DspLoader(classRoot, Mode.CONTEXT_LOADER ? null : servlet.getClass().getClassLoader());
			try {
//				for (int tries = 0; tries < 2; tries++)
//				{
//					try {
						Class<?> cl = loader.loadClass(fullName);
						page = (DspPage) cl.newInstance();
//						break;
//					} catch (LinkageError e) {
//						loader = new DspLoader();	// need a new loader
//								// because a class loader can't redefine a class
//					}
//				}
			} catch (ClassNotFoundException e) {
				throw new FileNotFoundException(DspServlet.getVirtualPath(req));
			} catch (Exception e) {
				throw new DspException(e);
			}
			page.setProp(prop);
			flush = false;
			pageDate = newDate;
		}
		return page;
	} // getPage()

//	String getPath() { return pageUrl.getPath(); }

	/**
	 * Returns the page source as a stream.
	 */
	synchronized InputStream getSource() throws IOException
	{
		return getSource(pageUrl.openConnection());
//		try {
//			return new FileInputStream(pageFile);
//		} catch (FileNotFoundException e) {
//			return context.getResourceAsStream(pagePath);
//		}
	} // getSource()

	/**
	 * Returns the page source as a stream.
	 */
	synchronized InputStream getSource(URLConnection conn) throws IOException
	{
		return conn.getInputStream();
//		try {
//			return new FileInputStream(pageFile);
//		} catch (FileNotFoundException e) {
//			return context.getResourceAsStream(pagePath);
//		}
	} // getSource()

	List<String> getSpecials()
	{
		if (specials == null)
		{
			specials = new ArrayList<String>();
			specials.add(DspCookies.NAME);
			specials.add(DspServlet.NAME);
			specials.add(DspOpen.NAME);
			specials.add(DspStatement.NAME);
			specials.add(DspProp.NAME);
//			specials.add(DspScan.NAME);
			specials.add(DspPageContext.NAME);
			specials.add(DspRequest.NAME);
			specials.add(DspUser.NAME);
		}
		return specials;
	} // getSpecials()

	/**
	 * Return true if the page, java, or class file has changed dates or needs to be recompiled
	 * for any reason.
	 */
	private boolean hasChanged() throws IOException
	{
		if (conn == null) conn = pageUrl.openConnection();
		newDate = DspProp.getLastModified(conn);
		long javaDate = javaFile.lastModified();
		long classDate = classFile.lastModified();
//System.out.println("hasChanged url " + pageUrl);
//System.out.println("hasChanged flush " + flush);
//System.out.println("hasChanged pageDate " + pageDate);
//System.out.println("hasChanged newDate " + newDate);
//System.out.println("hasChanged javaDate " + javaDate);
//System.out.println("hasChanged classDate " + classDate);
		boolean result = flush || (pageDate != Long.MIN_VALUE && newDate != pageDate) || javaDate != newDate || classDate != newDate;
//System.out.println("hasChanged -> " + result);
		return result;
	} // hasChanged()

	/**
   * Returns the compiler for another file relative to this one.
	 */
	private DspCompile insert(HttpServletRequest req, String path) throws DspException, IOException
	{
//		Object propSave = DspPageContext.swapAttribute(req, DspPageContext.PROP, prop);
		path = DspPageContext.normalizePath(req, path);
		DspCompile comp = servlet.getCompiler(req, path);
//		DspPageContext.swapAttribute(req, DspPageContext.PROP, propSave);
		return comp;
	} // insert()

	byte[] loadFile(String path) throws IOException
	{
		try {
			URL url = new URL(pageUrl, path);
			return loadFile(url.openConnection());
		} catch (java.net.MalformedURLException e) {
			throw new IOException("Could not load " + path + ": " + e);
		}
	} // loadFile()

	byte[] loadFile(URLConnection conn) throws IOException
	{
		int size = DspProp.getContentLength(conn);
		byte[] buf = new byte[size];
		InputStream in = getSource(conn);
		try {
			for (int ix = 0; ix < size;)
			{
				int read = in.read(buf, ix, size - ix);
				if (read <= 0) //throw new IOException("File " + pageFile + " is truncated at " + ix + " of " + size + " bytes");
						throw new IOException(pageUrl + " is truncated at " + ix + " of " + size + " bytes");
				ix += read;
			}
		} finally {
			in.close();
			in = null;
		}
		return buf;
	} // loadFile()

//	int nextTemp() { return tempCount++; }

	/**
	 * Load a parse the page.  If the page hasn't been modified since the last time then
	 * the previous tokens are just returned.
	 */
	private void parse() throws DspParseException, IOException
	{
		try {
			byte[] buf = loadFile(conn);
//			DspFactory.nextLetter();	// use another letter for unnamed statements, so we don't get a name conflict
			parse = new DspParse(buf, this);
		} finally {
			conn = null;
		}
		tokens = parse.parse();
		postParse();
	} // parse()

	private static String[] reserved = {
		"abstract", "boolean", "break", "byte", "byvalue", "case", "cast", "catch", "char",
		"class", "const", "continue", "default", "do", "double", "else", "extends", "false",
		"final", "finally", "float", "for", "future", "generic", "goto", "if", "implements",
		"import", "inner", "instanceof", "int", "interface", "long", "native", "new", "null",
		"operator", "outer", "package", "private", "protected", "public", "rest", "return",
		"short", "static", "super", "switch", "synchronized", "this", "throw", "throws",
		"transient", "true", "try", "var", "void", "volatile", "while"
	};

	private static void pathFix(String word, StringBuilder buf)
	{
//ThreadState.log("pathFix(" + word + ") -> " + word);
		for (int iy = 0, iyz = reserved.length; iy < iyz; iy++)
		{
			if (word.equals(reserved[iy]))
			{
				buf.append('_');
//ThreadState.log("_");
				break;
			}
		}
//ThreadState.logln("");
	} // pathFix()

	/**
	 * Convert a folder path into a package name.  This currently assumes the path has been converted to
	 * lower case.
	 */
	static String pathToPackage(String path)
	{
		int ixz = path.length();
		StringBuilder buf = new StringBuilder(ixz);
		int last = 0;
		for (int ix = 0; ix < ixz; ix++)
		{
			char c = path.charAt(ix);
			if (c == '/')
			{
				if (ix == 0 || ix + 1 == ixz) continue;	// don't need an initial or final dot
				pathFix(path.substring(last + 1, ix), buf);
				last = ix;
				c = '.';
			}
			else
			if (!((c >= '0' && c <= '9') || (c >= 'a' && c <= 'z') || c == '_'))
			{
				c = '_';
			}
			buf.append(c);
		}
		pathFix(path.substring(last + 1), buf);
		return buf.toString();
	} // pathToPackage()

	private void postParse() throws DspParseException
	{
		for (int ix = 0, iz = tokens.size(); ix < iz; ix++)
		{
			try {
				Token token = (Token)tokens.get(ix);
				token.postParse(this, tokens, ix);
			} catch (ClassCastException e) {
			}
		}
	} // postParse()

	/** Remove a name from the specials list.
	 * @see addSpecial(String)
	 */
	void removeSpecial(String name)
	{
		specials.remove(name);
	} // removeSpecial()

	public String toString()
	{
		return "DspCompile[" + pageUrl + ']';
	} // toString()

/*
	public static void main(String args[]) throws IOException, ServletException
	{
		new DspFactory(null);
		javaRoot = "/c:/dsp/dos";
		classRoot = "/c:/dsp/dos";
		String realPath = "c:\\WebShare\\wwwroot\\webedit\\DspTest.dsp";
		DspCompile compile = new DspCompile(null, "/wwwroot/webedit/dspTest.dsp", realPath, true);
		HttpJspPage page = compile.get();
		if (page != null) page._jspService(null, null);
	} // main()
*/
} // DspCompile
