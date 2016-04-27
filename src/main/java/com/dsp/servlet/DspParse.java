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

/*
DSP Syntax
	'{' ['!' <tag>] [<dspc> [';' <dspc>]...] '}'
	<dspc>
			( '[' <expr> ']' (output)
			| '{' <code> '}' (code)
			| <word> ['#' <id>] [<stmt>]
			| <tag>:<opt> [<attr> ['=' <value>]]...
			)
	<tag>		<word>
	<expr>	... | '\' ']' | '[' ... ']'
	<code>	... | '\' '}' | '{' ... '}'
	<cmt>		<word>
	<word>	[_a-zA-Z][_0-9a-zA-Z]*
	<id>		[_0-9a-zA-Z]+
	<stmt>	(... | '[' <expr> ']' | '<' '%' '=' <expr> '%' '>')
	<cmnt>	('/' '/' ... /(';'| '}') | '/' '*' ... '*' '/') (anywhere)

	DSP Conversions:
		(contiguous whitespace)
						:' '
		&nbsp;	:' '
		&gt;		:'<'
		&guot;	:'"'
		&semi;	:';'
		&lt;		:'>'
		&amp;		:'&'
		<br>		:'\r\n'
		'\' 'r'	:'\r'
		'\' 'n' :'\n'
		'\' '\' :'\\'
		'`'			:'"'
		'\'' <two or more chars> '\''
						:'"' <two or more chars> '"'

JSP Syntax
	'<' '%' ('=' | '!' | '@' | ) ... '%' '>'
	<cmnt>	'<' '%' '-' '-' ... '-' '-' '%' '>' (anywhere)

Tag Library Syntax
	'<' <tag> ':' <opt> [<attr> ['=' <value>]]... '>'
	'{' ['!' <tag>] <tag> ':' <opt> [<attr> ['=' <value>]]... [; <dsp>]...'}'
	<opt>		:<word>
	<attr>	:<word>
	<value>	:<id> | '"' (... | '\\' '"') '"'
*/

package com.dsp.servlet;

import com.dsp.ThreadState;

import java.util.ArrayList;
import java.io.*;	// File, IOException: for testing only

import javax.servlet.*; // ServletContext, ServletException

class DspParse
{
	private byte[]				buf;
	private int					index, bufLen, phase = START, line = 1;
	private StringBuffer		phrase = new StringBuffer();
	private boolean				htmlComment;
	private String				dspEat, dspCommand, dspName;
	private ArrayList<DspArg>	dspArgs = new ArrayList<DspArg>();
	private ArrayList<Token>	list = new ArrayList<Token>();
	private DspCompile			compile;

	private String				prefix = null;		// set to the tag prefix when a tag extension is being parsed
	private ArrayList<TagAttr>	attrs = null;		// list of attributes for tag extensions
	private TagAttr				attr = null;		// used for each attribute of a tag extension
	private boolean				tagEnd1 = false,	// set when this tag extension is the tail
								tagEnd2 = false;	// set what this tag extension has no tail

private boolean printFlag = false;	// for debugging only

	DspParse(byte[] data, DspCompile compile)
	{
		bufLen = data.length;
		buf = data;
		this.compile = compile;
	} // DspParse()

	private char get(int index)
	{
		return (char)(buf[index++] & 0xff);
	} // get()

	String getError(String name, int index)
	{
		StringBuffer buf = new StringBuffer();
		buf.append("Error in file ");
		buf.append(name);
		Token token;
		int size = list.size();
		if (index < size)
		{
			token = (Token)list.get(index++);
		}
		else
		{
			if (size > 0)
			{
				token = (Token)list.get(index - 1);
			}
			else token = null;
		}
		int line = token != null ? token.getSourceLine() : 1;
		buf.append(" near line ");
		buf.append(line);
		buf.append(".\r\n");
		int ix = index - 5;
		if (ix < 0) ix = 0;
		for (; ix < index; ix++)
		{
			token = (Token)list.get(ix);
			buf.append("Line ");
			buf.append(token.getSourceLine());
			buf.append(": ");
			buf.append(token);
			buf.append("\r\n");
		}
		return buf.toString();
	} // getError()
/*
	private void insertFile(String path) throws IOException
	{
		int add = 0;
		char c = get(index - 1);
		if (c == ';')
		{
			add = 1;
		}
		int oldLen = bufLen;
		byte addBuf[] = compile.loadFile(path);
		int addLen = addBuf.length;
		byte[] newBuf = new byte[bufLen += addLen + add];
		System.arraycopy(buf, 0, newBuf, 0, index);
		System.arraycopy(addBuf, 0, newBuf, index, addLen);
			// if insert ended with a semicolon then fix the next part to start a new command
		if (add != 0)
		{
			buf[index + addLen] = (byte)'{';
			phase = START;
		}
		System.arraycopy(buf, index, newBuf, index + addLen + add, oldLen - index);
		buf = newBuf;
	} // insertFile()
*/
	private boolean match(int index, String find)
	{
		for (int ix = 0, end1 = buf.length, end2 = find.length();
				index < end1; index++, ix++)
		{
			if (ix >= end2) return true;
			if (Character.toLowerCase(get(index))
					!= Character.toLowerCase(find.charAt(ix))) return false;
		}
		return false;
	} // match()

	@SuppressWarnings("unchecked")
	ArrayList<Token> parse() throws IOException, DspParseException
	{
		for (;;)
		{
			int token = scan();
			if (token == START) break;
/*
			if (token == DSP && dspCommand != null && dspCommand.equals("insert"))
			{
				if (dspArgs.size() != 1) throw new DspParseException("file name", "an expression", list.size());
				DspArg arg = (DspArg) dspArgs.get(0);
				String expr = arg.getExpr();
				if (arg.getExpr() != null) throw new DspParseException("file name", arg.getExpr(), list.size());
				insertFile(arg.getText());
				continue;
			}
*/
			Token tok = null;
			switch (token)
			{
				case HTML:
					tok = new DspText(phrase.toString());
					break;
				case IGNORE:
					break;
				case DSP:
					if (prefix == null)
					{
						tok = DspToken.makeToken(compile, dspEat, dspCommand, dspName,
								(ArrayList<DspArg>) dspArgs.clone(), list.size());
					}
					else if (prefix.length() == 0 && dspCommand.equals(PageTag.NAME))
					{
						tok = new PageTag(dspEat, attrs, list.size(), compile);
					}
					else
					{
						tok = new DspTagExt(dspEat, prefix, dspCommand, attrs, tagEnd1, tagEnd2, list.size());
					}
//System.out.println("token: " + tok);
					break;
				case JSP_DECL:
					tok = new JspDeclaration(phrase.toString());
					break;
				case JSP_EXPR:
					tok = new JspExpression(phrase.toString());
					break;
				case JSP_DIRECT:
					tok = new JspDirective(phrase.toString());
					break;
				case JSP_FRAG:
					tok = new JspScriptlet(phrase.toString());
					break;
				default: throw new DspParseException("Unknown token: " + token, list.size(), compile);
			}
			if (tok != null)
			{
				tok.setSourceLine(line);
				tok.setTokenIndex(list.size());
				list.add(tok);
			}
		}
		ArrayList<Token> result = list;
		buf = null;
		phrase = null;
		dspEat = null;
		dspCommand = null;
		dspName = null;
		dspArgs = null;
		line = 0;
		return result;
	} // parse()

	private static final int START = 0;
	private static final int HTML = 1;
	private static final int IGNORE = 2;
	private static final int DSP = 3;
	private static final int JSP_DECL = 5;
	private static final int JSP_EXPR = 6;
	private static final int JSP_DIRECT = 7;
	private static final int JSP_FRAG = 8;
	private static final int JSP_COMMENT = 9;

	private int scan() throws DspParseException
	{
		phrase.setLength(0);
		int ix = index;
		boolean literal = false;
		int token = START;
		char c = 0, last;

		while (token == START)
		{
			if (ix >= bufLen)
			{
				switch (phase)
				{
					case START:
						token = START;
						break;
					case HTML:
						token = HTML;
						phase = START;
						break;
					default:
						throw new DspParseException("Invalid ending phase: " + phase, list.size(), compile);
				}
				break;
			}
			last = c;
			c = get(ix++);
			if (c == '\r') line++;
			else
			if (c == '\n' && last != '\r') line++;
			if (literal)
			{
				if (phase != START) phrase.append('\\');
				phrase.append(c);
				literal = false;
				continue;
			}
			if (c == '\\' && !htmlComment)
			{
				literal = true;
				continue;
			}
			else
			if (!htmlComment && c == '<' && match(ix, "!--")) htmlComment = true;
			else
			if (htmlComment && c == '-' && match(ix, "->")) htmlComment = false;
			switch (phase)
			{
				case START:
					if (c == '}' && !htmlComment) throw new DspParseException("{", "}", list.size(), compile);
					else
					if (c == '{' && !htmlComment) phase = DSP;
					else
					if (c == '<')
					{
						if (match(ix, "%--"))
						{
							phase = JSP_COMMENT;
							ix += 3;
							phrase.setLength(0);
							continue;
						}
						else
						if (match(ix, "%!") && !htmlComment)
						{
							phase = JSP_DECL;
							ix += 2;
							phrase.setLength(0);
							continue;
						}
						else
						if (match(ix, "%="))
						{
							phase = JSP_EXPR;
							ix += 2;
							phrase.setLength(0);
							continue;
						}
						else
						if (match(ix, "%@") && !htmlComment)
						{
							phase = JSP_DIRECT;
							ix += 2;
							phrase.setLength(0);
							continue;
						}
						else
						if (match(ix, "%") && !htmlComment)
						{
							phase = JSP_FRAG;
							ix++;
							phrase.setLength(0);
							continue;
						}
						else phase = HTML;
					}
					else phase = HTML;
					break;
				case HTML:
					if (c == '}' && !htmlComment) throw new DspParseException("{", "}", list.size(), compile);
					else
					if (c == '{' && !htmlComment)
					{
						phase = DSP;
						token = HTML;
					}
					else
					if (c == '<')
					{
						if (match(ix, "%--"))
						{
							phase = JSP_COMMENT;
							token = HTML;
							ix += 3;
						}
						else
						if (match(ix, "%!") && !htmlComment)
						{
							phase = JSP_DECL;
							token = HTML;
							ix += 3;
						}
						else
						if (match(ix, "%="))
						{
							phase = JSP_EXPR;
							token = HTML;
							ix += 2;
						}
						else
						if (match(ix, "%@") && !htmlComment)
						{
							phase = JSP_DIRECT;
							token = HTML;
							ix += 2;
						}
						else
						if (match(ix, "%") && !htmlComment)
						{
							phase = JSP_FRAG;
							token = HTML;
							ix++;
						}
					}
					break;
				case DSP:
				case DSP_PRECMD:
					ix = scanDsp(ix - 1);
					token = htmlComment ? IGNORE : DSP;
					break;
				case JSP_COMMENT:
					if (c == '-' && match(ix, "-%>"))
					{
						phase = START;
						token = IGNORE;
					}
					break;
				case JSP_DECL:
				case JSP_EXPR:
				case JSP_DIRECT:
				case JSP_FRAG:
					if (c == '%' && match(ix, ">"))
					{
						token = phase;
						phase = START;
						ix++;
					}
					break;
				default:
					throw new DspParseException("Invalid phase: " + phase, list.size(), compile);
			}
			if (token == START) phrase.append(c);
		}
		index = ix;
		return token;
	} // scan()

	private static final int DSP_EAT		= 100;
	private static final int DSP_PRECMD		= 101;
	private static final int DSP_CMD		= 102;
	private static final int DSP_PRESTMT	= 103;
	private static final int DSP_TAGEXT		= 120;
	private static final int DSP_PRETAGATTR	= 121;
	private static final int DSP_TAGATTR	= 122;
	private static final int DSP_TAGEQ		= 123;
	private static final int DSP_PRETAGVAL	= 124;
	private static final int DSP_TAGVAL		= 125;
	private static final int DSP_STMT		= 104;
	private static final int DSP_EXPR		= 105;
	private static final int DSP_SUBEXPR	= 106;
	private static final int JSP_SUBEXPR	= 107;
	private static final int DSP_NAME		= 108;
	private static final int DSP_SCRIPT		= 109;
	private static final int DSP_END		= 110;

	private static final int NO_STRING		= 0;
	private static final int DBL_STRING		= 1;
	private static final int SNGL_STRING  	= 2;
//	private static final int BACK_STRING	= 3;
	private static final int NO_COMMENT		= 0;
	private static final int SLSH_COMMENT	= 1;
	private static final int STAR_COMMENT	= 2;

	private int scanDsp(int ix) throws DspParseException
	{
		phrase.setLength(0);
		boolean literal = false/*, done = false, string = false*/;
		dspEat = dspCommand = dspName = null;
		String argText = null;
		dspArgs.clear();
		int exprCount = 0, string = NO_STRING, comment = NO_COMMENT,
				nextPhase = -1, stringSave = NO_STRING;
		char c = '{', last;
		prefix = null;		// the tag extension prefix
		attrs = null;			// list of attributes for tag extensions
		attr = null;			// used for each attribute of a tag extension
		tagEnd1 = false;	// set when this tag extension is the tail
		tagEnd2 = false;	// set what this tag extension has no tail

		while (nextPhase < 0)
		{
			if (ix >= bufLen) throw new DspParseException("Statement Closure", "EOF", list.size(), compile);
			last = c;
			c = get(ix++);
			if (c == '\r') line++;
			else
			if (c == '\n' && last != '\r') line++;
			else
			if (c == '&')
			{
				if (match(ix, "quot;"))
				{
					c = '"';
					ix += 5;
				}
				else
				{
					if (string != DBL_STRING)
					{
						if (match(ix, "lt;"))
						{
							c = '<';
							ix += 3;
						}
						else
						if (match(ix, "gt;"))
						{
							c = '>';
							ix += 3;
						}
						else
						if (match(ix, "nbsp;"))
						{
							c = ' ';
							ix += 5;
						}
						else
						if (match(ix, "semi;"))
						{
							c = ';';
							ix += 5;
						}
						else
						if (match(ix, "amp;"))
						{
							c = '&';
							ix += 4;
						}
					}
				}
			}
			else
			if (c == '`')
			{
				c = '"';
			}
			else
			if (c == '%' && match(ix, "20"))
			{
				c = ' ';
				ix += 2;
			}
if (printFlag) ThreadState.logln(c + " Phase: " + phase);
			if (literal)
			{
				phrase.append(c);
				literal = false;
				continue;	// don't append c
			}
			if (c == '\\')
			{
				phrase.append('\\');
				literal = true;
				continue;	// don't append c
			}
			// no else here, so above HTML conversions will take effect
			if (comment == SLSH_COMMENT)
			{
        if (phase == DSP_SCRIPT)
        {
          if (c == '\r' || c == '\n')
          {
            comment = NO_COMMENT;
          }
        }
				else
        {
          if (c == ';')
          {
            comment = NO_COMMENT;
            nextPhase = DSP_PRECMD;
            break;
          }
          else
          if (c == '}')
          {
            comment = NO_COMMENT;
            nextPhase = START;
            break;
          }
          continue;	// don't append c
        }
			}
			else
			if (comment == STAR_COMMENT)
			{
				if (c == '*' && match(ix, "/"))
				{
					comment = NO_COMMENT;
					if (phase != DSP_SCRIPT) ix++;
				}
				if (phase != DSP_SCRIPT) continue;	// don't append c
			}
			else
			if (c == '/' && string == NO_STRING)
			{
				if (match(ix, "/"))
				{
					comment = SLSH_COMMENT;
					if (phase != DSP_SCRIPT)
					{
						ix++;
						continue;	// don't append c
					}
				}
				if (match(ix, "*"))
				{
					comment = STAR_COMMENT;
					if (phase != DSP_SCRIPT)
					{
						ix++;
						continue;	// don't append c
					}
				}
			}
			else
			if (c <= ' ')
			{
				if (last <= ' ') continue;	// don't append c
				c = ' ';
			}
			else
			if (c == '<')
			{
				if (match(ix, "br>") || match(ix, "BR>"))
				{
					phrase.append("\r\n");
					ix += 3;
					continue;	// don't append c
				}
			}
			else
			if (c == '{')
			{
				switch (phase)
				{
					case DSP:
						throw new DspParseException("DSP Command", "{", list.size(), compile);
					case DSP_EAT:
						throw new DspParseException("!tag", "{", list.size(), compile);
					case DSP_PRECMD:
						throw new DspParseException("DSP Command", "{", list.size(), compile);
					case DSP_CMD:
						throw new DspParseException("Statement", "{", list.size(), compile);
					case DSP_PRESTMT:
						throw new DspParseException("}", "{", list.size(), compile);
					case DSP_STMT:
						throw new DspParseException("}", "{", list.size(), compile);
					case DSP_SUBEXPR:
						throw new DspParseException("]", "{", list.size(), compile);
					case JSP_SUBEXPR:
						throw new DspParseException("%>", "{", list.size(), compile);
				}
			}
			else
			if (c == '}' && phase != DSP_SCRIPT && string == NO_STRING)
			{
				nextPhase = START;
if (printFlag) System.out.println("done}");
				break;
			}
			else
			if (c == ';' && phase != DSP_SCRIPT && string == NO_STRING)
			{
				nextPhase = DSP_PRECMD;
if (printFlag) System.out.println("done;");
				break;
			}
			switch (phase)
			{
				case DSP:
					if (c == '!')
					{
						phase = DSP_EAT;
						phrase.setLength(0);
						continue;	// don't append c
					}
					else
					if (c == '[')
					{
						phase = DSP_EXPR;
						phrase.setLength(0);
						continue;	// don't append c
					}
					else
					if (c == '%')
					{
						if (match(ix, "!"))
						{
							dspCommand = DspMember.NAME;
							ix++;
						}
						else
						{
							dspCommand = DspScriptlet.NAME;;
						}
						phase = DSP_SCRIPT;
						phrase.setLength(0);
						continue;	// don't append c
					}
					else
					if (c == '/')
					{
						phase = DSP_CMD;
						tagEnd1 = true;
						continue; // don't append c
					}
					else
					if (c > ' ')
					{
						phase = DSP_CMD;
						phrase.setLength(0);
					}
					break;
				case DSP_EAT:
					if (c <= ' ')
					{
						dspEat = phrase.toString().toLowerCase();
						if (dspEat.length() == 0) throw new DspParseException("!value", "}", list.size(), compile);
						phase = DSP_PRECMD;
					}
					break;
				case DSP_EXPR:
					if (c == '[' && string == NO_STRING) exprCount++;
					else
					if (c == ']' && string == NO_STRING && exprCount-- <= 0)
					{
						phase = DSP_END;
						dspArgs.add(new DspArg(null, phrase.toString()));
						phrase.setLength(0);
						continue;	// don't append c
					}
					else
					if (c == '"')
					{
						switch (string)
						{
							case NO_STRING: string = DBL_STRING; break;
							case DBL_STRING: string = NO_STRING; break;
						}
					}
					else
					if (c == '\'')
					{
						switch (string)
						{
							case NO_STRING: string = SNGL_STRING; break;
							case SNGL_STRING: string = NO_STRING; break;
						}
					}
					break;
				case DSP_PRECMD:
					if (c == '[')
					{
						phase = DSP_EXPR;
						phrase.setLength(0);
						continue;	// don't append c
					}
					else
					if (c == '%')
					{
						if (match(ix, "!"))
						{
							dspCommand = DspMember.NAME;
							ix++;
						}
						else
						{
							dspCommand = DspScriptlet.NAME;
						}
						phase = DSP_SCRIPT;
						phrase.setLength(0);
						continue;	// don't append c
					}
					else
					if (c == '/')
					{
						phase = DSP_CMD;
						tagEnd1 = true;
						continue; // don't append c
					}
					else
					if (c > ' ')
					{
						phase = DSP_CMD;
						phrase.setLength(0);
					}
					break;
				case DSP_CMD:
					if (c == ':')
					{
						prefix = phrase.toString().toLowerCase();
						phase = DSP_TAGEXT;
						phrase.setLength(0);
						continue;	// don't append c
					}
					else
					if (c <= ' ')
					{
						if (tagEnd1) throw new DspParseException(":", "whitespace", list.size(), compile);
						dspCommand = phrase.toString().toLowerCase();
						if (dspCommand.equals(PageTag.NAME))
						{
							prefix = "";	// flag to parse page attributes
							phase = DSP_PRETAGATTR;
						}
						else if (dspCommand.equals(DspMember.NAME))
						{
							dspCommand = DspMember.NAME;
							phase = DSP_SCRIPT;
							phrase.setLength(0);
						}
						else phase = DSP_PRESTMT;
						continue;	// don't append c
					}
					break;
				case DSP_TAGEXT:
					if (c <= ' ')
					{
						dspCommand = phrase.toString().toLowerCase();
						phase = DSP_PRETAGATTR;
						continue;	// don't append c
					}
					break;
				case DSP_PRETAGATTR:
					if (c == '/' && (match(ix, ";") || match(ix, "}")))
					{
						tagEnd2 = true;
						phase = DSP_END;
					}
					else
					if (c > ' ')
					{
						phase = DSP_TAGATTR;
						phrase.setLength(0);
						break;
					}
					continue;	// don't append c
				case DSP_TAGATTR:
					if (c <= ' ' || c == '=')
					{
						attr = new TagAttr(phrase.toString());
						if (attrs == null) attrs = new ArrayList<TagAttr>();
						attrs.add(attr);
						phase = c != '=' ? DSP_TAGEQ : DSP_PRETAGVAL;
					}
					break;
				case DSP_TAGEQ:
					if (c == '=')
					{
						phase = DSP_PRETAGVAL;
					}
					else
					if (c > ' ') throw new DspParseException("=", String.valueOf(c), list.size(), compile);
					continue;	// don't append c
				case DSP_PRETAGVAL:
					if (c == '/' && (match(ix, ";") || match(ix, "}")))
					{
						tagEnd2 = true;
						phase = DSP_END;
					}
					else
					if (c == '"')
					{
						string = DBL_STRING;
						phase = DSP_TAGVAL;
						phrase.setLength(0);
					}
					else
					if (c > ' ')
					{
						phase = DSP_TAGVAL;
						phrase.setLength(0);
						break;
					}
					continue;	// don't append c
				case DSP_TAGVAL:
					if (c == '"')
					{
						string = NO_STRING;
						if (phrase.length() > 0)
						{
							dspArgs.add(new DspArg(phrase.toString(), null));
							phrase.setLength(0);
						}
						attr.setArgs(dspArgs);
						phase = DSP_PRETAGATTR;
						continue;	// don't append c
					}
					else
					if (c == '[')
					{
						stringSave = string;
						string = NO_STRING;
						phase = DSP_SUBEXPR;
						argText = phrase.toString();
						phrase.setLength(0);
						continue;	// don't append c
					}
					else
					if (c == '<' && match(ix, "%="))
					{
						stringSave = string;
						string = NO_STRING;
						ix += 2;
						phase = JSP_SUBEXPR;
						argText = phrase.toString();
						phrase.setLength(0);
						continue;	// don't append c
					}
					else
					if (c <= ' ' && string == NO_STRING)
					{
						if (phrase.length() > 0)
						{
							dspArgs.add(new DspArg(phrase.toString(), null));
							phrase.setLength(0);
							attr.setArgs(dspArgs);
						}
						phase = DSP_PRETAGATTR;
						continue;	// don't append c
					}
					break;
				case DSP_PRESTMT:
					if (c == '[')
					{
						phase = DSP_SUBEXPR;
						phrase.setLength(0);
						continue;	// don't append c
					}
					else
					if (c == '<' && match(ix, "%="))
					{
						phase = JSP_SUBEXPR;
						phrase.setLength(0);
						continue;	// don't append c
					}
					else
					if (c == '#' && dspName == null)
					{
						phase = DSP_NAME;
						phrase.setLength(0);
						continue;	// don't append c
					}
					else
					if (c > ' ')
					{
						phase = DSP_STMT;
						phrase.setLength(0);
//						dspArgs.clear();
					}
					break;
				case DSP_NAME:
					if (c <= ' ')
					{
						dspName = phrase.toString();
						phrase.setLength(0);
						phase = DSP_PRESTMT;
						continue;	// don't append c
					}
					break;
				case DSP_STMT:
					if (c == '[')
					{
						phase = DSP_SUBEXPR;
						argText = phrase.toString();
						phrase.setLength(0);
						continue;	// don't append c
					}
					else
					if (c == '<' && match(ix, "%="))
					{
						phase = JSP_SUBEXPR;
						argText = phrase.toString();
						ix += 2;
						phrase.setLength(0);
						continue;	// don't append c
					}
					break;
				case DSP_SUBEXPR:
					if (c == '[' && string == NO_STRING) exprCount++;
					else
					if (c == ']' && string == NO_STRING && exprCount-- <= 0)
					{
//ThreadState.logln("End of Expression: " + phrase);
//if (phrase.toString().equals("eq(var.wpid, var.page)")) {
//	printFlag = true;
//}
						if (prefix != null) string = stringSave;
						phase = prefix == null ? DSP_STMT : (string == NO_STRING ? DSP_PRETAGATTR : DSP_TAGVAL);
						dspArgs.add(new DspArg(argText, phrase.toString()));
						phrase.setLength(0);
						argText = null;
						continue;	// don't append c
					}
					else
					if (c == '"')
					{
						switch (string)
						{
							case NO_STRING: string = DBL_STRING; break;
							case DBL_STRING: string = NO_STRING; break;
						}
					}
					else
					if (c == '\'')
					{
						switch (string)
						{
							case NO_STRING: string = SNGL_STRING; break;
							case SNGL_STRING: string = NO_STRING; break;
						}
					}
					break;
				case JSP_SUBEXPR:
					if (c == '%' && match(ix, ">"))
					{
						if (prefix != null) string = stringSave;
						phase = prefix == null ? DSP_STMT : (string == NO_STRING ? DSP_PRETAGATTR : DSP_TAGVAL);
						ix++;
						dspArgs.add(new DspArg(argText, phrase.toString()));
						phrase.setLength(0);
						argText = null;
						continue;	// don't append c
					}
					break;
				case DSP_SCRIPT:
					if (c == '%' && comment == NO_COMMENT)
					{
							// see if the next character marks the end of the DSP statement
						for (int ix1 = ix; ix1 < bufLen; ix1++)
						{
							char c1 = get(ix);
								// ignore whitespace
							if (c1 <= ' ') continue;	// don't append c
							if (c1 == ';' || c1 == '}' || (c1 == '&' && match(ix1 + 1, "semi;")))
							{
									// found the end of the scriptlet
								if (phrase.length() > 0)
								{
									dspArgs.add(new DspArg(phrase.toString(), null));
									phrase.setLength(0);
								}
								phase = DSP_END;
							}
							break;	// always break at the first non-whitespace
						}
					}
					break;
				case DSP_END:
					if (comment == NO_COMMENT && c > ' ' && c != ';' && c != '}' && c != '<' && c != '>'
							&& c != 'b' && c != 'B' && c != 'r' && c != 'R')
					{
						throw new DspParseException("; or }", String.valueOf(c), list.size(), compile);
					}
					break;
				default:
					throw new DspParseException("Unknown DSP phase: " + phase, list.size(), compile);
			}
			if (nextPhase >= 0) break;
			phrase.append(c);
		} // while
if (printFlag) ThreadState.logln("Done " + c + " Phase: " + phase + " nextPhase: " + nextPhase);
			// clean up for the current phase
		switch (phase)
		{
//			case DSP:
//				break;
			case DSP_EAT:
				dspEat = phrase.toString().toLowerCase();
				if (dspEat.length() == 0) throw new DspParseException("!value", "}", list.size(), compile);
				break;
			case DSP_EXPR:
				throw new DspParseException("]", "}", list.size(), compile);
//			case DSP_PRECMD:
//					// no command necessary
//				break;
			case DSP_CMD:
				if (phrase.length() > 0)
				{
					dspCommand = phrase.toString().toLowerCase();
				}
				break;
//			case DSP_PRESTMT:
//					// no statement necessary
//				break;
			case DSP_NAME:
				throw new DspParseException("#name", "}", list.size(), compile);
			case DSP_STMT:
if (printFlag) ThreadState.logln("DSP_STMT " + phase);
				if (phrase.length() > 0)
				{
					dspArgs.add(new DspArg(phrase.toString(), null));
				}
				break;
			case DSP_SUBEXPR:
if (printFlag) ThreadState.logln("DSP_SUBEXPR" + phase);
				throw new DspParseException("]", "}", list.size(), compile);
			case DSP_SCRIPT:
				throw new DspParseException("%", "", list.size(), compile);
			case JSP_SUBEXPR:
				throw new DspParseException("%>", "}", list.size(), compile);
		}
			// set the next phase
		phase = nextPhase;
/*
		if (argText == null && phrase.length() > 0)
		{
			argText = phrase.toString();
			phrase.setLength(0);
		}
		if (argText != null && argText.length() > 0) dspArgs.add(new DspArg(argText, null));
*/
		return ix;
	} // scanDsp()

	public static void main(String args[]) throws IOException, ServletException
	{
		File file = new File("/c:/WebShare/WWWRoot/webedit/dspTest.dsp");
		int size = (int)file.length();
		byte[] buf = new byte[size];
		FileInputStream in = new FileInputStream(file);
		try {
			for (int ix = 0; ix < size;)
			{
				int read = in.read(buf, ix, size - ix);
				if (read <= 0) throw new IOException("File truncated at " + ix + " of " + size + " bytes");
				ix += read;
			}
		} finally {
			in.close();
		}
		DspParse parse = new DspParse(buf, null);
		ArrayList<Token> tokens = null;
		try {
			tokens = parse.parse();
			for (int ix = 0, iz = tokens.size(); ix < iz; ix++)
			{
				System.out.println(ix + ": " + tokens.get(ix));
			}
		} catch (DspParseException e) {
			System.out.print(parse.getError(file.getName(), e.getTokenIndex()));
			e.printStackTrace();
		}
	} // main()

} // DspParse
