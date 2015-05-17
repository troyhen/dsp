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

class DspOutput extends DspToken implements Output
{
	static final String NAME = "output";

	DspOutput(String eat, List<DspArg> args)
	{
		super(eat, args);
	} // DspOutput()

	public int doJava(DspCompile comp, StringBuffer buf, int level)
	{
		if (args != null)
		{
			for (int ix = 0, ixz = args.size(); ix < ixz; ix++)
			{
				DspArg arg = args.get(ix);
				DspCompile.doTabs(buf, level);
				buf.append("out.print(_String(");
				buf.append(fix(comp, arg.getExpr(), level));
				buf.append("));\r\n");
			}
		}
		return level;
	} // doJava()

	@Override
	public void postParse(DspCompile comp, List<Token> tokens, int index) throws DspParseException
	{
		super.postParse(comp, tokens, index);
		if (args != null && args.size() > 1)
				throw new DspParseException("Only one expression allowed in expression output", getTokenIndex(), comp);
	} // postParse()

	public String toString()
	{
		return '{' + listArgs() + '}';
	} // toString()

} // DspOutput
