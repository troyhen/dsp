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

import java.util.ArrayList;

class PageTag extends DspToken implements Page
{
	static final String NAME = "page";

	private String		autoFlushFlag,
						bufSize,
						contentType,
						errorPage,
						errorPageFlag,
						extendsVal,
						importVal,
						info,
						sessionFlag,
						threadSafeFlag;

	PageTag(String eat, ArrayList<TagAttr> attrs, int index, DspCompile comp) throws DspParseException
	{
		super(eat, null);
		//System.out.println("attrs = " + attrs);
		for (int ix = 0, ixz = attrs.size(); ix < ixz; ix++)
		{
			TagAttr attr = (TagAttr)attrs.get(ix);
			String name = attr.getName();
			String value = attr.getText(index, comp);
if (name.equals("autoFlush")) autoFlushFlag = String.valueOf(_boolean(name, value, index, comp));
			else
			if (name.equals("buffer"))
			{
				if (value.equals("none")) bufSize = "0";
				else
				if (value.endsWith("kb"))
				{
					bufSize = String.valueOf(Integer.parseInt(value.substring(0, value.length() - 2).trim()) * 1024);
				}
				else throw new DspParseException("Invalid buffer size: " + value, index, comp);
			}
			else
			if (name.equals("contentType")) contentType = value;
			else
			if (name.equals("errorPage")) errorPage = value;
			else
			if (name.equals("extends")) extendsVal = value;
			else
			if (name.equals("import")) importVal = value;
			else
			if (name.equals("session")) sessionFlag = String.valueOf(_boolean(name, value, index, comp));
			else
			if (name.equals("info")) info = value;
			else
			if (name.equals("isErrorPage")) errorPageFlag = String.valueOf(_boolean(name, value, index, comp));
			else
			if (name.equals("isThreadSafe")) threadSafeFlag = String.valueOf(_boolean(name, value, index, comp));
			else throw new DspParseException("Unknown page attribute: " + name, index, comp);
		}
	} // PageTag()

	private static boolean _boolean(String name, String value, int index, DspCompile comp) throws DspParseException
	{
		boolean flag = false;
		if (!(value.equals("false") || (flag = value.equals("true"))))
		{
			throw new DspParseException(name + " must be true or false: " + value, index, comp);
		}
		return flag;
	} // _boolean()

	public String getAutoFlush() { return autoFlushFlag; }
	public String getBuffer() { return bufSize; }
	public String getComment() { return null; }
	public String getContentType() { return contentType; }
	public String getErrorPage() { return errorPage; }
	public String getExtends() { return extendsVal; }
	public String getImport() { return importVal; }
	public String getSession() { return sessionFlag; }
	public String getInfo() { return info; }
	public String isErrorPage() { return errorPageFlag; }
	public String isThreadSafe() { return threadSafeFlag; }

	public String toString()
	{
		return toString(NAME);
	} // toString()

} // PageTag
