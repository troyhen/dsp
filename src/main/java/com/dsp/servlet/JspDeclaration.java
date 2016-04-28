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

class JspDeclaration extends JspToken implements Declaration
{
	static final String NAME = "<%!";

	JspDeclaration(String text)
	{
		super(text);
	} // JspDeclaration()

	public void doMember(DspCompile comp, StringBuilder buf) throws DspParseException
	{
/*		int level = 1;
		for (int ix = 0, ixz = args.size(); ix < ixz; ix++)
		{
			DspArg arg = (DspArg)args.get(ix);
			String text = arg.getText();
			if (text == null || text.length() == 0) return;
			buf.append(text);
			buf.append("; // in case progammer forgot this\r\n");
		}
*/	} // doMember()

	public String toString()
	{
		return toString(NAME);
	} // toString()

} // JspDeclaration
