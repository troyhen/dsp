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

class DspInsert extends DspToken implements Insert
{
	static final String NAME = "insert";

	DspInsert(String eat, List<DspArg> args)
	{
		super(eat, args);
	} // DspInsert()

	public String getPath(DspCompile comp, int index) throws DspParseException
	{
		if (args.size() != 1) throw new DspParseException("file name", "an expression", index, comp);
		DspArg arg = args.get(0);
		String expr = arg.getExpr();
		if (expr != null) throw new DspParseException("file name", expr, index, comp);
		return arg.getText().trim();
	} // getPath()

	public String toString()
	{
		return toString(NAME);
	} // toString()

} // DspInsert
