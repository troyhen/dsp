/* Copyright 2016 Troy D. Heninger
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
import java.io.Reader;
import java.util.ArrayList;
import java.util.Iterator;

import javax.json.Json;
import javax.json.JsonNumber;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.JsonString;
import javax.servlet.http.HttpServletRequest;

import static com.dsp.util.BZCast._boolean;

public class DspJson implements DspObject
{
	public static final String NAME = "json";
	//public static final String PREFIX = "com.dsp.json.";

	private boolean				trace, debug;
	private HttpServletRequest	req;
	private JsonObject			obj;
	private boolean 			isParsed;
	
	public DspJson(DspPageContext context) {
		req = (HttpServletRequest) context.getRequest();
	}
	
	@Override
	public boolean exists(String variable) throws DspException {
		if (trace) ThreadState.logln(NAME + ".exists(" + variable + ")");
		JsonObject jo = getObject();
		boolean result = false;
		if (jo != null) {
			result = jo.containsKey(variable);
		}
		if (debug) ThreadState.logln(NAME + '.' + variable + (result ? " exists" : " does not exist"));
		return result;
	}

	@Override
	public Object get(String name) throws DspException {
		Object result = get(name, null);
//		if (result == null) throw new DspException(DspExpRef.JSON + '.' + name + " doesn't exist");
		return result;
	} // get()

	public Object get(String name, Object defaultValue) {
		if (trace) ThreadState.logln(NAME + ".get(" + name + ", " + defaultValue + ")");
		JsonObject jo = getObject();
		Object result = null;
		if (jo != null) {
			result = jo.get(name);
		}
		if (result == null) result = defaultValue;
		result = unwrap(result);
		if (debug) ThreadState.logln(NAME + '.' + name +" => " + result);
		return result;
	}

	public JsonObject getObject() {
		if (!isParsed) parse();
		return obj;
	}
	
	@Override
	public Iterator<String> names() {
		if (trace) ThreadState.logln(NAME + ".names()");
		JsonObject jo = getObject();
		if (jo == null) {
			return new ArrayList<String>().iterator();
		}
		return jo.keySet().iterator();
	}
	
	private void parse() {
		isParsed = true;
		try (Reader rdr = req.getReader()) {
			JsonReader jr = Json.createReader(rdr);
			obj = jr.readObject();
		} catch (IOException e) {
			if (debug) ThreadState.logln(e);
		}
	}
	
	@Override
	public void set(String name, Object value) throws DspException {
		if (trace) ThreadState.logln(NAME + ".set(" + name + ", " + value + ")");
		if (value == null) {
			unset(name);
			return;
		}
		else
		if (name.equals(DEBUG)) debug = _boolean(value);
		else
		if (name.equals(TRACE)) trace = _boolean(value);
		else throw new DspReadOnlyException(NAME, name);
		if (debug) ThreadState.logln(NAME + '.' + name +" <= " + value);
	}
	
	@Override
	public void unset(String name) throws DspException {
		if (trace) ThreadState.logln(NAME + ".unset(" + name + ')');
		if (name.equals(DEBUG)) debug = false;
		else
		if (name.equals(TRACE)) trace = false;
		else throw new DspReadOnlyException(NAME, name);
		if (debug) ThreadState.logln("unset " + NAME + '.' + name);
	}
	
	public static Object unwrap(Object value) {
		if (value instanceof JsonString) return ((JsonString) value).getString();
		if (value instanceof JsonNumber) {
			JsonNumber num = (JsonNumber) value;
			if (num.isIntegral()) return num.longValue();
			return num.doubleValue();
		}
		return value;
	}
}
