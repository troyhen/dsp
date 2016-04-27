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
package com.dsp;

import java.sql.*;	// DriverManager, Connection, ...
import java.util.*;	// Collection, Iterator

class DspStatementArray extends DspStatement
{
	static final String NAME = "array";

	static final String COLUMN = "item";

	private Object				object;
	private Object[] 			array;
	private Iterator<Object>	iterate;
	private int					end, index;

	DspStatementArray(String name, boolean debug, boolean trace)
			throws DspException
	{
		super(name, debug, trace);
		if (trace) ThreadState.logln("DspStatementArray(" + name + ")");
	} // DspStatementArray()

	public DspStatement close()
	{
		if (trace) ThreadState.logln("DspStatementArray.close()");
		array = null;
		iterate = null;
		object = null;
		index = end = DONE;
		return super.close();
	} // close()

	protected boolean exists0(int index)
	{
		return index == 1;
	} // exists0()

	protected boolean exists0(String variable)
	{
		variable = variable.toLowerCase();
		if (variable.equals(COLUMN)) return true;
		return false;
	} // exists0()

	protected int execute0(Object obj)
			throws DspException, SQLException
	{
		if (trace) ThreadState.logln("DspStatementArray.execute(" + obj + ")");
//		if (!stripArray(sql)) return NOT_MINE;
		if (obj == null)
		{
			index = end = DONE;
		}
		else
		{
			index = 0;
			try {
				@SuppressWarnings("unchecked")
				Collection<Object> collect = (Collection<Object>) obj;
				end = collect.size();
				iterate = collect.iterator();
//				if (iterate.hasNext()) object = iterate.next();
			} catch (ClassCastException e) {
				try {
					array = (Object[])obj;
					end = array.length;
				} catch (ClassCastException e1) {
					object = obj;
					end = 1;
				}
			}
		}
		return index;
	} // execute0()

	public String getColumnName(int index)
	{
		if (index != 1) return null;
		return COLUMN;
	} // getColumnName()

	protected Object getObject0(int index) throws SQLException
	{
		if (trace) ThreadState.logln("DspStatementArray.getObject(" + index + ")");
		if (index == 1)
		{
			if (iterate != null) return object;
			if (array != null) return array[this.index - 1];
			return object;
		}
		throw new IllegalArgumentException("Arrays only have one column");
	} // getObject()

	protected Object getObject0(String name) throws SQLException
	{
		if (trace) ThreadState.logln("DspStatementArray.getObject(" + name + ")");
		if (name.toLowerCase().equals(COLUMN)) return getObject0(1);
		throw new IllegalArgumentException("Array's only column is called 'item'");
	} // getObject0()

	public boolean hasResults() { return iterate != null || array != null || object != null; }

	protected boolean next0() throws DspException, SQLException
	{
		if (trace) ThreadState.log("DspStatementArray.next0()");
		if (index++ >= end)
		{
			close();
			return false;
		}
		if (iterate != null && iterate.hasNext()) object = iterate.next();
		return true;
	} // next0()

} // DspStatementArray

