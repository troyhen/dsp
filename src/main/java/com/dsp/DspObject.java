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

import java.util.Iterator;

public interface DspObject
{
	static final String DEBUG = "debugMode";
	static final String TRACE = "traceMode";

	boolean exists(String variable) throws DspException;

	Object get(String variable) throws DspException;

	Iterator<String> names();

//	Object run(String function, Object[] params) throws DspException;

	void set(String variable, Object value) throws DspException;

	void unset(String variable) throws DspException;

} // DspObject

