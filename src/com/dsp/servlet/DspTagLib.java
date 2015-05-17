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

class DspTagLib extends DspToken
{
	static final String NAME = "taglib";

	private String prefix, uri;

	DspTagLib(String eat, List<DspArg> args, int index, DspCompile comp) throws DspParseException
	{
		super(eat, null);
		if (args == null || args.size() != 1)
				throw new DspParseException("taglib command must have static values", index, comp);
		DspArg arg = (DspArg)args.get(0);
		String text = arg.getText();
		if (text == null || arg.getExpr() != null)
				throw new DspParseException("taglib command must have static values", index, comp);
		int ix = text.indexOf(',');
		if (ix < 0) throw new DspParseException("taglib must have two parameters: prefix, uri", index, comp);
		prefix = text.substring(0, ix).trim();
		uri = text.substring(ix + 1).trim();
	} // DspTagLib()

	String getClassName() { return uri; }	// change this! only temporary!
	String getPrefix() { return prefix; }
	String getUri() { return uri; }

	public String toString()
	{
		return toString(NAME);
	} // toString()

} // DspTagLib
