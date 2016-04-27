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

class DspTagExt extends DspToken implements Output
{
//	static final String NAME = "tag:ext";

	private String			prefix, action, name;
	private List<TagAttr>	attrs;
	private boolean			end1, end2;
	private DspTagExt		link, parent;
	private DspTagLib		lib;
	private int				index;

	DspTagExt(String eat, String prefix, String action, List<TagAttr> attrs,
			boolean end1, boolean end2, int index)
	{
		super(eat, null);
		this.prefix = prefix;
		this.action = action;
		this.attrs = attrs;
		this.end1 = end1;
		this.end2 = end2;
		this.index = index;
		this.name = '_' + prefix + '_' + action + index;
	} // DspTagExt()

	public int doJava(DspCompile comp, StringBuffer buf, int level) throws DspParseException
	{
		String className = lib.getClassName();
		DspCompile.doTabs(buf, level);
		buf.append(className);
		buf.append(' ');
		buf.append(name);
		buf.append(" = (");
		buf.append(className);
		buf.append(")\r\n");
		DspCompile.doTabs(buf, level + 2);
		buf.append("getTag(temp, \"");
		buf.append(prefix);
		buf.append("\", \"");
		buf.append(action);
		buf.append("\", \"");
		buf.append(className);
		buf.append("\");\r\n");
		DspCompile.doTabs(buf, level++);
		buf.append("try {\r\n");
		if (!end1)
		{
	/*		comp.doTabs(buf, level);
			buf.append(name);
			buf.append(".setPageContext(pageContext);\r\n");
			comp.doTabs(buf, level);
			buf.append(name);
			buf.append(".setParent(");
			if (parent == null)
			{
				buf.append("null");
			}
			else
			{
				buf.append("(Tag).temp.get(\"");
				buf.append(parent.action);
				buf.append("\");\r\n");
			}
			buf.append(");\r\n");
	*/		int ixz;
			if (attrs != null && (ixz = attrs.size()) > 0)
			{
				for (int ix = 0; ix < ixz; ix++)
				{
					TagAttr attr = (TagAttr)attrs.get(ix);
					DspCompile.doTabs(buf, level);
					buf.append(name);
					attr.doJava(comp, buf, level);
				}
			}
			DspCompile.doTabs(buf, level);
			buf.append(name);
			buf.append(".doStartTag();\r\n");
		}
		if (end1 || end2)
		{
			DspCompile.doTabs(buf, level);
			buf.append(name);
			buf.append(".doEndTag();\r\n");
			DspCompile.doTabs(buf, level - 1);
			buf.append("} finally {\r\n");
			DspCompile.doTabs(buf, level);
			buf.append("releaseTag(temp, \"");
			buf.append(prefix);
			buf.append("\", \"");
			buf.append(action);
			buf.append("\", ");
			buf.append(name);
			buf.append(");\r\n");
			DspCompile.doTabs(buf, --level);
			buf.append("}\r\n");
		}
		else if (link == null) throw new DspParseException("No end tag found for " + prefix + ':' + action, index, comp);
		return level;
	} // doJava()

	@Override
	public void postParse(DspCompile comp, List<Token> tokens, int index) throws DspParseException
	{
		super.postParse(comp, tokens, index);
		int level = 0;
		for (int ix = index - 1; ix >= 0 && (link == null || !end1) && parent == null && lib == null; ix--)
		{
			Object token = tokens.get(ix);
			try {
				DspTagExt tag = (DspTagExt)token;
				if (!(tag.end1 || tag.end2))
				{
					if (parent == null && --level == 0) parent = tag;
					if (end1 && link == null && tag.prefix.equalsIgnoreCase(prefix)
							&& tag.action.equalsIgnoreCase(action) && tag.link == null)
					{
						link = tag;
						tag.link = this;
					}
				}
				else if (tag.end1) level++;
			} catch (ClassCastException e) {
				try {
					DspTagLib tagLib = (DspTagLib)token;
					if (lib == null && tagLib.getPrefix().equals(prefix)) lib = tagLib;
				} catch (ClassCastException e1) {
				}
			}
		}
		if (lib == null) throw new DspParseException("Did not find taglib with prefix=\"" + prefix + "\"", index, comp);
	} // postParse()

	public String toString()
	{
		return toString(prefix + ':' + action);
	} // toString()

} // DspTagExt
