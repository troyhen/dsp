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

import java.util.*;	// NoSuchElementException, Vector

public class Pool
{
	private static int	id = 0;
	
	private ArrayList<Object>	list = new ArrayList<Object>();
	private int					max;
	private String				name;

	public Pool(String name)
	{
		this.name = name + (++id);
	} // Pool()

	public int available() { return list.size(); }

	public synchronized void checkIn(Object object)
	{
		list.add(object);
		notify();
		int size = list.size();
		if (max < size) max = size;
//System.out.println("checkIn " + name + ", pool has " + size + " objects");
	} // checkIn()

	public synchronized Object checkOut()
	{
		int size;
		while ((size = list.size()) == 0)
			try {
System.out.println("DSP " + name + " pool has no objects, waiting...");
				wait();
System.out.println("DSP " + name + " pool is done waiting");
			} catch (InterruptedException e) {}
		Object result = list.remove(size - 1);
//System.out.println("checkOut " + name + ", pool has " + size + " objects");
		return result;
	} // checkOut()

	public int size() { return max;	}

} // DspPool

