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

import com.dsp.DspException;

public class DspParseException extends DspException
{
	private static final long serialVersionUID = -1477696220896626805L;

	private DspCompile comp;
	private int index;
/*
	public DspParseException() {}
	public DspParseException(String msg) { super(msg); }
*/
	public DspParseException(String msg, int index, DspCompile comp)
	{
		super(msg + ", found on token " + index);
		this.index = index;
		this.comp = comp;
	} // DspParseException()
/*
	public DspParseException(String expected, String got)
	{
		super("Expected " + expected + ", found " + got);
	} // DspParseException()

	public DspParseException(char expected, String got)
	{
		super("Expected '" + expected + "', found " + got);
	} // DspParseException()

	public DspParseException(String expected, char got)
	{
		super("Expected " + expected + ", found '" + got + "'");
	} // DspParseException()

	public DspParseException(char expected, char got)
	{
		super("Expected '" + expected + "', found '" + got + "'");
	} // DspParseException()
*/
	public DspParseException(String expected, String got, int index, DspCompile comp)
	{
		super("Expected " + expected + " on token " + index + ", found " + got);
		this.index = index;
		this.comp = comp;
	} // DspParseException()

	public DspCompile getCompiler() { return comp; }
	public int getTokenIndex() { return index; }

} // DspParseException
