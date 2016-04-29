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

import java.io.IOException;
import java.lang.reflect.*;	// Field, Method, Modifier

import javax.servlet.*;
import javax.servlet.http.*;
import javax.servlet.jsp.*;
import javax.servlet.jsp.tagext.*;

import static com.dsp.util.BZCast._boolean;

/**
 * This class is the default class that all DSP pages extend.  When a page
 * is written to extend another class then an object of this class still
 * be created as the page object.  Functions of this class are treated special
 * in the DSP parser.  Any arguments passed to functions which do not start
 * with an underscore '_', will be converted to the proper type automatically.
 * Functions that accept multiple arguments accept Object arrays.  These functions
 * call allow any number of arguments and the DSP parser will send all arguments
 * as elements of the array.
 */
public abstract class DspPage extends HttpServlet implements HttpJspPage
{
	private static final long serialVersionUID = 6527415972891184043L;

	public static final int SQL_STMT		= 0;
	public static final int ARRAY_STMT	= 1;
	public static final int DIR_STMT		= 2;
	public static final int SCAN_STMT		= 3;

	static final boolean DEBUG_MODE = false;
//	static final boolean TRACE_MODE = false;

	protected DspProp prop;

//	public void doGet(HttpServletRequest request, HttpServletResponse response)
//			throws ServletException, IOException
//	{
//		_jspService(request, response);
//	} // doGet()

//	public void doPost(HttpServletRequest request, HttpServletResponse response)
//			throws ServletException, IOException
//	{
//		_jspService(request, response);
//	} // doPost()

	/**
	 * Execute a statement.  The first word specifies the type of statement: "array", "dir", "scan", or "sql".
	 * If none of those are found the statement is assumed to be of type "sql".  The db option to switch
	 * databases is allowed at the beginning of sql statements.
	 */
	public DspStatement execute(String typeStr, Object obj) throws DspException
	{
		int type = SQL_STMT;
		if (typeStr != null && typeStr.length() > 0)
		{
			if (typeStr.equals("scan"))
			{
				type = SCAN_STMT;
			}
			else
			if (typeStr.equals("array"))
			{
				type = ARRAY_STMT;
			}
			else
			if (typeStr.equals("dir"))
			{
				type = DIR_STMT;
			}
			else
			if (!typeStr.equals("sql")) throw new IllegalArgumentException("Invalid statement type: " + typeStr);
		}
		return execute("_t999", type, obj);
	} // execute()

	/**
	 * Execute a statement.  Used by DspDo, DspIf, DspSql, and DspWhile.
	 */
	protected DspStatement execute(String name, int type, Object obj)
			throws DspException
	{
		DspStatement state = makeStatement(name, type);
		state.execute(obj);
		return state;
	} // execute()

	/**
	 * Execute a statement and return the first column of the first row if statement produces
	 * a result set.  Otherwise, return the number of rows modified.  The first word specifies
	 * the type of statement: "array", "dir", "scan", or "sql".  If none of those are found the
	 * statement is assumed to be of type "sql".  The db option to switch databases is allowed
	 * at the beginning of sql statements.
	 */
	public Object executeGet1st(String typeStr, Object obj) throws DspException
	{
		int type = SQL_STMT;
		if (typeStr != null && typeStr.length() > 0)
		{
			if (typeStr.equals("scan"))
			{
				type = SCAN_STMT;
			}
			else
			if (typeStr.equals("array"))
			{
				type = ARRAY_STMT;
			}
			else
			if (typeStr.equals("dir"))
			{
				type = DIR_STMT;
			}
			else
			if (!typeStr.equals("sql")) throw new IllegalArgumentException("Invalid statement type: " + typeStr);
		}
		return executeGet1st("_t998", type, obj);
	} // executeGet1st()

	/**
	 * Execute a statement and return the first column of the first row if statement produces
	 * a result set.  Otherwise, return the number of rows modified.  Used by DspSet and DspDefault.
	 */
	protected Object executeGet1st(String name, int type, Object obj)
			throws DspException
	{
		DspStatement state = makeStatement(name, type);
		try {
			state.execute(obj);
			if (state.hasResults())
			{
				state.next();
				return state.getObject(1);
			}
			int result = state.getResult();
			if (result == 0 || result == Integer.MIN_VALUE) return null;
			return new Integer(result);
    } finally {
      state.close();
		}
	} // executeGet1st()

	/**
	 * Returns a new object instance.
	 */
	public static Object getBean(String className) throws DspException
	{
		try {
			return Class.forName(className).newInstance();
		} catch (Exception e) {
			throw new DspException("Couldn't get bean " + className, e);
		}
	} // getBean()

	/**
	 * Returns the value of the named member of the object.  If the object is a DspObject
	 * get() is called.  Otherwise, it uses reflection to look up and get the field.
	 */
	public static Object getMember(Object obj, String member) throws DspException
	{
		try {
			return ((DspObject)obj).get(member);
		} catch (ClassCastException e) {
			Class<?> c = obj.getClass();
			try {
				Field f = c.getField(member);
	//			if (Modifier.isPublic(f.getModifiers())
	//			{
					return f.get(obj);
	//			}
			} catch (NoSuchFieldException e1) {
				throw new DspException("Could not find field '" + member + '\'', e1);
			} catch (IllegalAccessException e1) {
				throw new DspException("Could not access field '" + member + '\'', e1);
			}
		}
	} // getMember()

	/**
	 * Returns the result of calling a named method of the object, passing args as the arguments
	 * of the function.  If the object is a DspObject run() is called.  Otherwise, it uses
	 * reflection to look up and call the method.
	 */
	public static Object getMember(Object obj, String member, Object[] args) throws DspException
	{
//		try {
//			return ((DspObject) obj).run(member, args);
//		} catch (ClassCastException e) {
			Class<?> c = obj.getClass();
			Class<?>[] types = null;
			int len;
			if (args != null && (len = args.length) > 0)
			{
				types = new Class[len];
				for (int ix = 0; ix < len; ix++)
				{
					if (args[ix] != null)
					{
						types[ix] = args[ix].getClass();
					}
				}
			}
			try {
				int close = 0;
				Method[] meths = c.getMethods();
				Method found = null;
//int choice = -1;
				for (int ix = 0, ixz = meths.length; ix < ixz; ix++)
				{
					Method meth = meths[ix];
					if (Modifier.isPublic(meth.getModifiers()) && meth.getName().equals(member))
					{
						Class<?>[] pTypes = meth.getParameterTypes();
						if ((pTypes.length == 0 && args == null) || (args != null && pTypes.length == args.length))
						{
							int close1 = 1;
							if (args != null)
							{
								for (int iy = 0, iyz = pTypes.length; iy < iyz; iy++)
								{
									if (pTypes[iy] == types[iy])
									{
										close1 += 2;
//ThreadState.logln(ix + " - " + pTypes[iy]);
									}
									else
									if (pTypes[iy].isAssignableFrom(types[iy])) close1++;
								} // for iy
							} // if args
							if (close1 > close)
							{
//ThreadState.logln(ix + " got " + close1 + " points");
								found = meth;
//								choice = ix;
								close = close1;
							}
						} // if name matches
					} // for
				}
				if (found == null)
				{
					throw new DspException("Could not find method " + member + " in " + obj);
				}
				else
				{
//ThreadState.logln("Choice " + choice);
					return found.invoke(obj, args);
				}
//			} catch (NoSuchMethodException e1) {
//				throw new DspException("Could not find method " + member + " in " + obj, e1);
			} catch (IllegalAccessException e1) {
				throw new DspException("Could not invoke method " + member + " in " + obj, e1);
			} catch (InvocationTargetException e1) {
				throw new DspException("Exception in method " + member + " in " + obj, e1.getTargetException());
			} catch (Throwable e1) {
				throw new DspException("Exception in method " + member + " in " + obj, e1);
			}
//		}
	} // getMember(args)

	/**
	 * Returns an instantiation of a tag extension object.
	 */
	public static Tag getTag(DspObject temp, String prefix, String action, String className) throws DspException
	{
		Tag tag = (Tag)temp.get('_' + prefix + '_' + action);
		if (tag == null)
		{
			try {
				tag = (Tag)Class.forName(className).newInstance();
			} catch (Exception e) {
				throw new DspException("Could not create " + className);
			}
		}
		return tag;
	} // getTag()

//	abstract public long getId();
	/**
	 * Returns the prop object belonging to this group or pages.
	 */
	public DspProp getProp() { return prop; }

	public void jspInit() {}
	public void jspDestroy() {}

	private DspStatement makeStatement(String name, int type)
			throws DspException, IllegalArgumentException
	{
		DspStatement state;
		boolean debug = false, trace = false;
		try { debug = _boolean(ThreadState.getOpen().get(DspObject.DEBUG)); } catch (NumberFormatException e) {}
		try { trace = _boolean(ThreadState.getOpen().get(DspObject.TRACE)); } catch (NumberFormatException e) {}
		switch (type)
		{
			case ARRAY_STMT:
				state = new DspStatementArray(name, debug, trace);
				break;
			case DIR_STMT:
				state = new DspStatementDir(name, debug, trace);
				 break;
			case SCAN_STMT:
				state = new DspStatementScan(name, debug, trace);
				break;
			case SQL_STMT:
				state = new DspStatementSql(name, debug, trace);
				break;
			default: throw new IllegalArgumentException("Invalid statement type: " + type);
		}
		return state;
	} // makeStatement()

	/**
	 * Stores the tag extension object for use later in the same request.
	 */
	public static void releaseTag(DspObject temp, String prefix, String action, Tag tag) throws DspException
	{
		tag.release();
		temp.set('_' + prefix + '_' + action, tag);
	} // releaseTag()

	/**
	 * Sets the value of the named member of the object to value.  If the object is a DspObject
	 * set() is called.  Otherwise, it uses reflection to look up and set the field.
	 */
	public static Object setMember(Object obj, String member, Object value) throws DspException
	{
		try {
			((DspObject)obj).set(member, value);
		} catch (ClassCastException e) {
			Class<?> c = obj.getClass();
			try {
				Field f = c.getField(member);
	//			if (Modifier.isPublic(f.getModifiers())
	//			{
					f.set(obj, value);
	//			}
			} catch (NoSuchFieldException e1) {
				throw new DspException("Could not find field " + member, e1);
			} catch (IllegalAccessException e1) {
				throw new DspException("Could not access field " + member, e1);
			}
		}
		return value;
	} // setMember()

	/**
	 * Set the prop object.  Used internally to set up the initial prop object.
	 */
	public void setProp(DspProp value)
	{
		prop = value;
	} // setProp()

	/**
	 * Main DSP page function.  This function is created by the DSP engine for each page
	 * and is called by the DSP servlet for each request to the web page.
	 */
	public abstract void _jspService(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException;

} // DspPage
