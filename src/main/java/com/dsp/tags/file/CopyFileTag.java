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
package com.dsp.tags.file;

import com.dsp.ThreadState;
import com.dsp.util.BZFile;

import java.io.IOException;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.*;	// Tag, TagSupport

public class CopyFileTag extends TagSupport
{
	private static final long serialVersionUID = -2228355639107805759L;
	
	private String			from, to;
	private BZFile			file;
	private boolean			debug;

	public int doStartTag() throws JspException
	{
		try {
			if (file == null) file = new BZFile(ThreadState.getProp().getFile(), debug, false, ThreadState.getLog());
//ThreadState.logln("from " + from);
			file.setPath(from);
//ThreadState.logln("to " + from);
			file.copyFile(to);
		} catch (IOException e) {
			throw new JspException("Could not copy " + from + " to " + to + ": " + e.toString());
		}
		return SKIP_BODY;
	} // doStartTag()

	public boolean getDebug() { return debug; }
	public String getFrom() { return from; }
	public String getTo() { return to; }
	public void setDebug(String value) { debug = true; }
	public void setFrom(String value) { from = value; }
	public void setTo(String value) { to = value; }

} // CopyFileTag