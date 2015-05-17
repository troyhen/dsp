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

class DspElse extends DspEnd
{
	static final String NAME = "else";

	DspElse(String eat, List<DspArg> args)
	{
		super(eat, args);
	} // DspElse()

	public int doJava(DspCompile comp, StringBuffer buf, int level) throws DspParseException
	{
		return ((DspIf)doTag).doElse(comp, buf, level);
	} // doJava()

	public void postParse(DspCompile comp, List<Token> tokens, int index) throws DspParseException
	{
		super.postParse(comp, tokens, index);
		if (args != null && (args.size() > 1 || args.get(0).getExpr() != null))
				throw new DspParseException("Expressions not allowed in 'else'", getTokenIndex(), comp);
		for (--index; index >= 0; index--)
		{
			try {
				DspIf ifTag = (DspIf)tokens.get(index);
				if (ifTag.setElse(this))
				{
					this.doTag = ifTag;
					break;
				}
			} catch (ClassCastException e) {
			}
		}
		if (doTag == null) throw new DspParseException("Else with no matching if or while", getTokenIndex(), comp);
	} // postParse()

	public String toString()
	{
		return toString(NAME);
	} // toString()

} // DspElse
