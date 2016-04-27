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

abstract class JspToken implements Token
{
	private String text;
	private int index, line;

	JspToken(String text)
	{
		this.text = text;
	} // JspToken()

	public String getComment()
	{
		return null;
	} // getComment()

	public int getSourceLine() { return line; }
	public int getTokenIndex() { return index; }
	public void postParse(DspCompile comp, List<Token> tokens, int index) throws DspParseException {}
	public void setSourceLine(int line) { this.line = line; }
	public void setTokenIndex(int index) { this.index = index; }

	public String toString(String name)
	{
		return name + text + "%>";
	} // toString()

} // JspDeclaration
