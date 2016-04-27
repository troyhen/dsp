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

class DspUnset extends DspToken implements Output
{
	static final String NAME = "unset";

	DspUnset(String eat, List<DspArg> args)
	{
		super(eat, args);
	} // DspUnset()

	public int doJava(DspCompile comp, StringBuffer buf, int level) throws DspParseException
	{
		if (args.size() != 1) throw new DspParseException(
				"unset requires a variable reference", getTokenIndex(), comp);
		DspArg arg0 = args.get(0);
		if (arg0.getExpr() != null) throw new DspParseException(
				"unset does not allow an expression", getTokenIndex(), comp);
		StringBuffer buf2 = new StringBuffer();
		buf2.append(arg0.getText());
		buf2.append(" = null;");
		DspCompile.doTabs(buf, level);
		buf.append(fix(comp, buf2.toString(), level));
		buf.append("\r\n");
		return level;
	} // doJava()

/*
	protected String parseRef(String ref) throws DspParseException
	{
		if (ref == null || ref.length() == 0) throw new DspParseException("variable reference", "null", getTokenIndex());
		String lower = ref.toLowerCase();
		int dot = lower.indexOf('.');
		if (dot < 0) throw new DspParseException(".", ref, getTokenIndex());
		String obj = lower.substring(0, dot);
		int dot2 = lower.indexOf('.', dot + 1);
		if (dot2 < 0) dot2 = lower.indexOf(' ', dot);
		//if (lower.startsWith("var"))
//		{
//			return "var.get(\""
		return null;
	} // parseRef()
*/
	public String toString()
	{
		return toString(NAME);
	} // toString()

} // DspUnset
