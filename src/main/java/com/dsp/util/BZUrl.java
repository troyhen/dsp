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
package com.dsp.util;

import java.io.*;    		// input and output from and to url or page
import java.net.*;   		// URL for http and https and ftp

import com.dsp.DspPage;

/**
* This class provides support for talking to a url through various
* protocols.  It is more of a facilitator class that encapsulates the java URL and URLConnection classes.
* @see <a href="http://java.sun.com/products/jdk/1.2/docs/api/java/net/URL.html">java.net.URL</a>
* @see <a href="http://java.sun.com/products/jdk/1.2/docs/api/java/net/URLConnection.html">java.net.URLConnection</a>
*/
public class BZUrl
{
	private boolean		   		trace, debug;
	private BufferedReader	in = null;
	private PrintWriter 		out;
	private PrintStream			log;
	private String					relPath;
	private URL             base, url;
	private URLConnection   connection;

/**
* Default constructor<br>
* - sets up debugging and trace info<br>
* - sets up jsse https provider and support<br>
* - no base URL is setup
*/
	public BZUrl()
	{
		this(null, true, false, null);
	} // BZUrl()

/**
* Creates a BZUrl and sets up the base URL<br>
* - sets up debugging and trace info<br>
* - sets up jsse https provider and support<br>
*/
	public BZUrl(URL base)
	{
		this(base, true, false, null);
	} // BZUrl()

/**
* Creates a BZUrl given the base, debug, trace and log<br>
* - sets up jsse https provider and support
*/
	public BZUrl(URL base, boolean debug, boolean trace, PrintStream log)
	{
		this.base = base;
		this.trace = trace;
		this.debug = debug;
		this.log = log;
		if (log == null) this.log = System.out;
		if (trace) log.println("BZUrl()");

			// dynamicly register jsse https provider and support
		//java.security.Security.addProvider(new com.sun.net.ssl.internal.ssl.Provider());
		System.getProperties().put("java.protocol.handler.pkgs", "com.sun.net.ssl.internal.www.protocol");
	} // BZUrl()

/**
* Closes the input stream if data has been read from the URL.
* Only useful if data has been read from the URL.
*/
	public void close()
	{		// close the connection to the URL
		if (trace) log.println("BZUrl.close()");
		if (in != null) try	{ in.close(); } catch(IOException e) {}
		if (debug) log.println("url.close()");
	} // close()

/**
* Returns the base URL for this object
*/
	public URL getBase() { return base; }
/**
* Returns the Relative Path for this URL
*/
	public String getRelPath() { return relPath; }
/**
* Returns the URL object that this BZUrl object encapsulates
*/
	public URL getURL() { return url; }
/**
* Returns the specified HTTP header String based on a String key.
* @see <a href="http://java.sun.com/products/jdk/1.2/docs/api/java/net/URLConnection.html#getHeaderField(java.lang.String)">URLConnection.getHeaderField(String name)</a>
*/
	public String getHeader(String key)
	{ if (trace) log.println("BZUrl.getHeader()");
		String result = connection.getHeaderField(key);
		if (debug) log.println("url.getHeader(" + key + ") -> " + result);
		return result;
	} // getHeader()

/**
* Returns the specified HTTP header String based on an int index.
* @see <a href="http://java.sun.com/products/jdk/1.2/docs/api/java/net/URLConnection.html#getHeaderField(int)">URLConnection.getHeaderField(int n)</a>
*/
	public String getHeader(int index)
	{ if (trace) log.println("BZUrl.getHeader()");
		String result = connection.getHeaderField(index);
		if (debug) log.println("url.getHeader(" + index + ") -> " + result);
		return result;
	} // getHeader()

/**
* Returns whether the input or output stream is open.
* You must have read or posted something to/from the URL connection for this to return <b>true</b>.
*/
	public boolean isOpen()
	{ if (trace) log.println("BZUrl.isOpen()");
		boolean result = in != null || out != null;
		if (debug) log.println("url.isOpen() -> " + result);
		return result;
	} // isOpen()

/**
* Creates a connection to the given url String.<br>
* Sets up the URL object to the given url.<br>
* Creates the URLConnection from the URL created.<br>
* Sets the ability to do output for possible POSTs.<br>
* @throws IOException
* @see <a href="http://java.sun.com/products/jdk/1.2/docs/api/java/net/URL.html">URL</a>
* @see <a href="http://java.sun.com/products/jdk/1.2/docs/api/java/net/URLConnection.html">URLConnection</a>
*/
	public void open(String sUrl) throws IOException
	{  // open the connection to the URL
		if (trace) log.println("BZUrl.open()");
		try
		{ url = new URL(sUrl);
			connection = url.openConnection();
			connection.setDoOutput(true); // possibly do a post later, have to set this now
		} catch (IOException e)
		{ throw e;
		} catch(Exception e)
		{ throw new IOException(e.toString());
		}
		if(debug) log.println("url.open(" + sUrl + ')');
	} // open()

/**
* Does an HTTP POST to the URL that has already been created.<br>
* @param String parms must be in the format of "var1=val1&var2=val2"
* @throws IOException
*/
	public void post(String parms) throws IOException
	{  // post the parms to the url
		if (trace) log.println("BZurl.post()");
		DataOutputStream post = new DataOutputStream(connection.getOutputStream());
		post.writeBytes(parms);
		post.flush();
		post.close();
		if (debug) log.println("url.post(" + parms + ')');
	} // post()

/**
* Reads the entire page returned from the open(String sUrl) command.<br>
* Once you use this command, you must re-open the connection to read anything else.
* @return a String with the entire content of the resulting page.
* @throws IOException
*/
	public String readAll() throws IOException
	{  // read data from web page
		if (trace) log.println("BZUrl.read()");
		StringBuffer buf = new StringBuffer();
		String inputLine;
		if (in == null) in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
		while ((inputLine = in.readLine()) != null)
		{ buf.append(inputLine);
			buf.append("\r\n");
		}
		if (debug) log.println("url.readAll() -> " + buf.length() + " bytes");
		return buf.toString();
	} // readAll()

/**
* Reads one line from the resulting page from the URL.<br>
* This moves a pointer through the document that cannot be reset except by re-opening the connection.
* @return a String with the given line from the resulting document.
* @throws IOException
*/
	public String readLine() throws IOException
	{  // read one line of data from web page
		if (trace) log.println("BZUrl.readLine()");
		if (in == null) in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
		String result = in.readLine();
		if (debug) log.println("url.readLine() -> " + result);
		return result;
	} // readLine()

/**
* Sets HTTP Basic Authentication - takes user id and password and
* converts them to base64 format and puts them in the correct syntax for
* basic authorization request header.
* @param String uid - the userid for Basic Authentication.  This would be what you would normally type
* into the password authentication popup box on a site that requires basic auth.
* @param String pwd - the password for Basic Authentication.  This will be converted to Base64 format.
*/
	public void setBasicAuth(String uid, String pwd)
	{  // basic authorization
		if (trace) log.println("BZUrl.setBasicAuth()");
		String uidPwd = uid + ':' + pwd;
//		String authStr = "BASIC " + new sun.misc.BASE64Encoder().encode(uidPwd.getBytes());
		String authStr = "BASIC " + DspPage.base64Encode(uidPwd);
		connection.setRequestProperty("Authorization", authStr);
		if (debug) log.println("url.setBasicAuth() -> " + authStr);
	} // setBasicAuth()

/**
* Sets an HTTP Header Field designated by <b>key</b> to value of <b>val</b>.
*/
	public void setHeader(String key, String val)
	{
		if (trace) log.println("BZUrl.setHeader()");
		connection.setRequestProperty(key, val);
		if (debug) log.println("url.setHeader(" + key + ", " + val + ')');
	} // setHeader()

/**
* Sets the Base URL object.
* @param URL base - a URL object which acts as the base URL.
* @throws MalformedURLException
*/
	public void setBase(URL base) throws MalformedURLException
	{
		if (trace) log.println("BZUrl.setBase(" + base + ')');
		close();
		this.base = base;
		if (relPath != null) url = new URL(base, relPath);
		if (debug) log.println("url.url <- " + url);
	} // setBase()

/**
* Sets the relative URL path if a URL base has been set.
* If a URL base does not exist, then an attempt to create a full URL from the path will be made.
* The combination of the above creates the url private member used for the connection.
* @param String path - the path to be set, must be a valid URL path.
* @throws MalformedURLException
*/
	public void setPath(String path) throws MalformedURLException
	{
		if (trace) log.println("BZUrl.setPath(" + path + ')');
		close();
		relPath = path;
		url = base == null ? new URL(path) : new URL(base, path);
		if (debug) log.println("url.path <- " + path);
	} // setPath()

/**
* Sets the URL url private member that is used for all connections.
* @param URL url - a valid URL object containing a valid path.
*/
	public void setURL(URL url)
	{
		if (trace) log.println("BZUrl.setURL(" + url + ')');
		close();
		relPath = null;
		base = null;
		this.url = url;
		if (debug) log.println("url.url <- " + url);
	} // setURL()

}  // BZUrl class
