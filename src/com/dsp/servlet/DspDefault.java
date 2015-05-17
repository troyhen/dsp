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

import java.util.List;

class DspDefault extends DspSet
{
	static final String NAME = "default";

	DspDefault(String eat, List<DspArg> args)
	{
		super(eat, args);
	} // DspDefault()

	protected void doRefClose(StringBuffer buf)
	{
		buf.append(" }");
	} // doRefClose()

	protected void doRefOpen(StringBuffer buf, String var)
	{
		buf.append("if (!isSet(");
		buf.append(var);
		buf.append(")) { ");
		buf.append(var);
		buf.append(" = ");
	} // doRefOpen()

	public String toString()
	{
		return toString(NAME);
	} // toString()

} // DspDefault
