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

import java.io.*;	// File, FileInputStream
//import java.util.HashMap;

class DspLoader extends ClassLoader
{
	private String classRoot;

	DspLoader(String classPath, ClassLoader parent)
	{
		super(Mode.CONTEXT_LOADER ? Thread.currentThread().getContextClassLoader() : parent);
		this.classRoot = classPath;
//System.out.println("DspLoader classRoot " + classPath);
	} // DspLoader()

	protected Class<?> findClass(String name) throws ClassNotFoundException
	{
		File file = new File(classRoot + File.separator + packageToPath(name) + ".class");
		FileInputStream in = null;
		try {
			in = new FileInputStream(file);
			int size = (int)file.length();
			byte[] buf = new byte[size];
			for (int ix = 0; ix < size;)
			{
				int read = in.read(buf, ix, size - ix);
				if (read <= 0) throw new ClassNotFoundException(name);
				ix += read;
			}
			return defineClass(name, buf, 0, size);
		} catch (IOException e) {
e.printStackTrace();
			throw new ClassNotFoundException(name);
		} finally {
			try { if (in != null) in.close(); } catch (IOException e) {}
		}
	} // findClass()

	static String packageToPath(String pkg)
	{
		StringBuffer buf = new StringBuffer(pkg.length());
		int last = -1, ix;
		while ((ix = pkg.indexOf(".", ++last)) >= 0)
		{
			if (buf.length() > 0) buf.append('/');
			buf.append(pkg.substring(last, ix));
			last = ix;
		}
		if (buf.length() > 0) buf.append('/');
		buf.append(pkg.substring(last));
		return buf.toString();
	} // packageToPath()

} // ClassLoader
