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

import java.util.*;	// Enumeration, Hashtable, Vector

import javax.servlet.http.*;

public class DspUser implements DspObject//, HttpSession
{
	public static final String NAME = "user";
	public static final String SESSION_ID = "session_id";
	public static final String CREATION_TIME = "creation_time";
	public static final String LAST_ACCESS_TIME = "last_access_time";

	private boolean debug, trace;
	private HttpSession session;

	DspUser(HttpSession session)
	{
		this.session = session;
		if (session.isNew())
		{
			DspProp group = ThreadState.getProp();
			group.preSet(this, NAME);
//			try {
//				try { debug = DspPage._boolean(group.getParam(NAME + ".debug")); } catch (NumberFormatException e) {}
//				try { trace = DspPage._boolean(group.getParam(NAME + ".trace")); } catch (NumberFormatException e) {}
//			} catch (DspException e) {
//			}
		}
		else
		{
			try { debug = DspPage._boolean(session.getAttribute(DEBUG)); } catch (NumberFormatException e) {}
			try { trace = DspPage._boolean(session.getAttribute(TRACE)); } catch (NumberFormatException e) {}
		}
		if (trace) ThreadState.logln("DspUser(" + session + ")");
		if (debug)
		{
			ThreadState.logln(NAME + ".isNew() => " + session.isNew());
			ThreadState.logln(NAME + '.' + SESSION_ID + " => " + session.getId());
			ThreadState.logln(NAME + '.' + CREATION_TIME + " => " + new Date(session.getCreationTime()));
		}
	} // DspUser()

	public void close()
	{
		if (trace) ThreadState.logln(NAME + ".close()");
		session = null;
	} // close()

	public boolean exists(String name)
	{
		if (trace) ThreadState.logln(NAME + ".exists(" + name + ')');
		boolean result = session.getAttribute(name) != null;
		if (debug) ThreadState.logln(NAME + '.' + name + (result ? " exists" : " does not exist"));
		return result;
	} // exists()

	public Object get(String name)
	{
		if (trace) ThreadState.logln(NAME + ".get(" + name + ')');
		return get(name, null);
	} // get()

	public Object get(String name, Object defaultValue)
	{
		if (trace) ThreadState.logln(NAME + ".get(" + name + ", " + defaultValue + ')');
		if (name.equals(SESSION_ID)) return session.getId();
		if (name.equals(CREATION_TIME)) return new java.sql.Timestamp(session.getCreationTime());
		if (name.equals(LAST_ACCESS_TIME)) return new java.sql.Timestamp(session.getLastAccessedTime());
		Object result = session.getAttribute(name);
		if (result == null) result = defaultValue;
		if (debug) ThreadState.logln(NAME + '.' + name + " => " + result);
		if (result == DspStatement.NULL) result = null;
		return result;
	} // get()

//	public Enumeration getAttributeNames() { return session.getAttributeNames(); }
//	public Object getAttribute(String name) { return session.getAttribute(name); }
//	public long getCreationTime() { return session.getCreationTime(); }
//	public String getId() { return session.getId(); }
//	public long getLastAccessedTime() { return session.getLastAccessedTime(); }
//	public int getMaxInactiveInterval() { return session.getMaxInactiveInterval(); }
//	/** @deprecated */
//	public HttpSessionContext getSessionContext() { return null/*session.getSessionContext()*/; }
//	/** @deprecated */
//	public Object getValue(String name) { return session.getAttribute(name); }
//	/** @deprecated */
//	public String[] getValueNames() { return session.getValueNames(); }
//	public void invalidate() { session.invalidate(); }
//	public boolean isNew() { return session.isNew(); }
//	/** @deprecated */

	public Iterator<String> names()
	{
		ArrayList<String> list = new ArrayList<String>();
		@SuppressWarnings("unchecked")
		Enumeration<String> it = session.getAttributeNames();
		while (it.hasMoreElements())
		{
			list.add(it.nextElement());
		}
		return list.iterator();
	} // names()

//	public void putValue(String name, Object val) { session.setAttribute(name, val); }
//	public void removeAttribute(String name) { session.removeAttribute(name); }
//	/** @deprecated */
//	public void removeValue(String name)
//	{
//		if (trace) DspState.logln(NAME + ".removeValue(" + function + ')');
//		session.removeAttribute(name);
//	} // removeValue()

//	public Object run(String function, Object[] args) throws DspException
//	{
//		if (trace) DspState.logln(NAME + ".run(" + function + ')');
//		throw new DspException(NAME + '.' + function + "() does not exist");
//	} // DspException

	public void set(String name, Object value)
	{
		if (trace) ThreadState.logln(NAME + ".set(" + name + ", " + value + ')');
		if (value == null)
		{
			unset(name);
			return;
		}
		else
		if (name.equals(DEBUG)) try { debug = DspPage._boolean(value); } catch (NumberFormatException e) {}
		else
		if (name.equals(TRACE)) try { trace = DspPage._boolean(value); } catch (NumberFormatException e) {}
		session.setAttribute(name, value);
		if (debug) ThreadState.logln(NAME + '.' + name + " <= " + value);
	} // set()

//	public void setAttribute(String name, Object val) { session.setAttribute(name, val); }
//	public void setMaxInactiveInterval(int time) { session.setMaxInactiveInterval(time); }

	public String toString()
	{
		return "DspUser[" + session.getId() + ']';
	} // toString()

	public void unset(String name)
	{
		if (trace) ThreadState.logln(NAME + ".unset(" + name + ')');
		if (name.equals(DEBUG)) debug = false;
		else
		if (name.equals(TRACE)) trace = false;
		session.removeAttribute(name);
		if (debug) ThreadState.logln("unset " + NAME + '.' + name);
	} // unset()

} // DspUser

