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

import java.util.*;	// ArrayList, Hashtable, Vector

import javax.servlet.http.*;

import static com.dsp.util.BZCast._boolean;

public class DspRequest implements DspObject
{
	public static final String NAME = "var";
	public static final String PREFIX = "com.dsp.var.";

	private boolean				trace, debug;
	private HttpServletRequest	req;

/*
	public static String decode(String val)
	{
		if (val == null || val.length() == 0) return val;
		byte buf[] = new byte[val.length()];
		int phase = 0, iy = 0;
		boolean decimal;
		byte num = 0;
		for (int ix = 0, ixz = val.length(); ix < ixz; ix++)
		{
			char c = val.charAt(ix);
			switch (phase)
			{
				case 0:
					if (c == '&') phase++;
					else if (c > 127)
					{
						System.out.print(c);
						buf[iy++] = (byte)c;
					}
					else buf[iy++] = (byte)c;
					break;
				case 1:
					if (c == '#')
					{
						phase++;
						num = 0;
					}
					else
					{
						phase = 0;
						buf[iy++] = (byte)'&';
						buf[iy++] =  (byte)c;
					}
					break;
				case 2:
					if (c >= '0' && c <= '9')
					{
						num = (byte)(num * 10 + c - '0');
					}
					else if (c == ';')
					{
						phase = 0;
						buf[iy++] = num;
					}
					break;
			} // switch
		} // for
		try {
			val = new String(buf, 0, iy);
//			val = new String(buf, 0, iy, "UTF8");
System.out.println("\na " + buf.length + ": " + buf);
System.out.println("b " + iy + ": " + new String(buf, 0, iy));
System.out.println("c " + val.length() + ": " + val);
		} catch (java.io.UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		return val;
	} // decode()
*/
	public static String decode(String val)
	{
		if (val == null || val.length() == 0) return val;
		StringBuffer buf = new StringBuffer(val.length());
		int phase = 0;
//		boolean decimal;
		char ch = 0;
		for (int ix = 0, ixz = val.length(); ix < ixz; ix++)
		{
			char c = val.charAt(ix);
			switch (phase)
			{
				case 0:
					if (c == '&') phase++;
					else buf.append(c);
					break;
				case 1:
					if (c == '#')
					{
						phase++;
						ch = 0;
					}
					else
					{
						phase = 0;
						buf.append('&');
						buf.append(c);
					}
					break;
				case 2:
					if (c >= '0' && c <= '9')
					{
						ch = (char)(ch * 10 + c - '0');
					}
					else if (c == ';')
					{
						phase = 0;
						buf.append(ch);
					}
					break;
			} // switch
		} // for
		return buf.toString();
	} // decode()

	public boolean exists(String variable)
	{
		if (trace) ThreadState.logln(NAME + ".exists(" + variable + ")");
		Object att = req.getAttribute(PREFIX + variable);
		boolean result = att != null;
		if (!result)
		{
			String[] values = req.getParameterValues(variable);
			result = values != null;
		}
		if (debug) ThreadState.logln(NAME + '.' + variable + (result ? " exists" : " does not exist"));
		return result;
	} // exists()

	public Object get(String name) throws DspException
	{
		Object result = get(name, null);
//		if (result == null) throw new DspException(DspExpRef.VAR + '.' + name + " doesn't exist");
		return result;
	} // get()

	public Object get(String name, Object defaultValue)
	{
		if (trace) ThreadState.logln("DspRequest.get(" + name + ", " + defaultValue + ")");
		Object result = null;
		String attName = PREFIX + name;
		result = req.getAttribute(attName);
		if (result == null) result = req.getHeader(name);
		if (result == null) result = defaultValue;
		if (debug) ThreadState.logln(NAME + '.' + name +" => " + result);
		if (result == DspNull.NULL) result = null;
		return result;
	} // get()

	public HttpServletRequest getRequest() { return req; }

	public Iterator<String> names()
	{
		ArrayList<String> list = new ArrayList<String>();
        Enumeration<String> it;
        try { // ServletExec 5.0 bug
            it = req.getAttributeNames();
            while (it.hasMoreElements())
            {
                String name = it.nextElement();
                if (name.startsWith(PREFIX) && req.getAttribute(name) != DspNull.NULL)
                {
                    list.add(name.substring(PREFIX.length()));
                }
            }
        } catch (NullPointerException e) {
            System.out.println("ServletExec 5.0 req.getAttributeNames bug!");
            e.printStackTrace();
        }
        it = req.getParameterNames();
		while (it.hasMoreElements())
		{
			String name = it.nextElement();
			if (!list.contains(name))
			{
				list.add(name);
			}
		}
		return list.iterator();
	} // names()

	public void release()
	{
		if (trace) ThreadState.logln("DspRequest.release()");
		req = null;
	} // release()

	public void set(String name, Object value) //throws DspException
	{
		if (trace) ThreadState.logln("DspRequest.set(" + name + ", " + value + ")");
		if (value == null)
		{
			unset(name);
			return;
		}
		else
		if (name.equals(DEBUG)) debug = _boolean(value);
		else
		if (name.equals(TRACE)) trace = _boolean(value);
		req.setAttribute(PREFIX + name, value);
		if (debug) ThreadState.logln(NAME + '.' + name +" <= " + value);
	} // set()

	public void setRequest(HttpServletRequest req)
	{
		this.req = req;
		debug = false;
		trace = false;
		DspProp group = ThreadState.getProp();
		group.preSet(this, NAME);
			// Move all of the parameters to attributes so they won't get lost in calls and forwards
		Enumeration<String> en = req.getParameterNames();
		while (en.hasMoreElements())
		{
			String name = en.nextElement();
			String attName = PREFIX + name;
			if (req.getAttribute(attName) == null)
			{
				String[] list = req.getParameterValues(name);
				Object value;
				int len = list != null ? list.length : 0;
				if (len == 0) value = DspNull.NULL;	// parameter with no value
				else
				if (len == 1)	value = decode(list[0]);		// single value, no array used
				else
				{
					ArrayList<String> al = new ArrayList<String>(len);
					for (int ix = 0; ix < len; ix++)
					{
						al.add(decode(list[ix]));
					}
					value = al;															// mutiple values, use ArrayList
				}
				req.setAttribute(attName, value);					// cache the decoded results
				if (debug) ThreadState.logln(NAME + '.' + name + ": " + value);
			}
		}
		if (trace) ThreadState.logln("DspRequest.setRequest(" + req + ")");
	} // setRequest()

	public void unset(String name)
	{
		if (trace) ThreadState.logln("DspRequest.unset(" + name + ")");
		if (name.equals(DEBUG)) debug = false;
		else
		if (name.equals(TRACE)) trace = false;
		else req.setAttribute(PREFIX + name, DspNull.NULL);
		if (debug) ThreadState.logln("unset " + NAME + '.' + name);
	} // unset()

} // DspRequest

