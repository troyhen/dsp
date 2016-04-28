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

class DspMember extends DspToken implements Declaration
{
	static final String NAME = "member";

	DspMember(String eat, List<DspArg> args)
	{
		super(eat, args);
	} // DspMember()

	public void doMember(DspCompile comp, StringBuilder buf) throws DspParseException
	{
		DspScriptlet.rawJava(args, comp, buf, 1);
	} // doMember()

	public String getComment() { return null; }

	public String toString()
	{
		return toString(NAME);
	} // toString()

} // DspMember
