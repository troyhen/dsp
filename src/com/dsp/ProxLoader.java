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

import java.io.*;	// File, FileInputStream
import java.util.zip.*;	// ZipEntry, ZipFile, ZipInputStream

class ProxLoader extends ClassLoader
{
	private File propDir;

	ProxLoader(File propDir)
	{
		super(Thread.currentThread().getContextClassLoader());
		this.propDir = propDir;
	} // ProxLoader()

	protected Class<?> findClass(String name) throws ClassNotFoundException
	{
		File file = new File(propDir, "WEB-INF/classes");
		String path = packageToPath(name) + ".class";
		byte[] buf = loadFromPath(file, path);
		if (buf != null) return defineClass(name, buf, 0, buf.length);
		File[] libs = new File(propDir, "WEB-INF/lib").listFiles();
		if (libs != null && libs.length > 0) {
			for (int ix = 0, ixz = libs.length; ix < ixz; ix++) {
				buf = loadFromJar(libs[ix], path);
				if (buf != null) return defineClass(name, buf, 0, buf.length);
			}
		}
		throw new ClassNotFoundException(name);
	} // findClass()

	private byte[] loadFromPath(File dir, String path) {
		File file = new File(dir, path);
		if (!(file.exists() && file.isFile() && file.canRead())) return null;
		InputStream in = null;
		byte[] buf = null;
		try {
			in = new FileInputStream(file);
			int size = (int)file.length();
			buf = new byte[size];
			for (int ix = 0; ix < size;)
			{
				int read = in.read(buf, ix, size - ix);
				if (read <= 0) {	// this should never happen
					buf = null;
					break;
				}
				ix += read;
			}
		} catch (IOException e) {
			buf = null;
		} finally {
			try { if (in != null) in.close(); } catch (IOException e) {}
		}
		return buf;
	} // loadFromPath()

	private byte[] loadFromJar(File zipFile, String path) {
		if (!(zipFile.exists() && zipFile.isFile() && zipFile.canRead())) return null;
		ZipFile file = null;
		InputStream in = null;
		byte[] buf = null;
		try {
			file = new ZipFile(zipFile);
			ZipEntry entry = file.getEntry(path);
			if (entry == null) return null;
			in = file.getInputStream(entry);
			int size = (int)entry.getSize();
			buf = new byte[size];
			for (int ix = 0; ix < size;)
			{
				int read = in.read(buf, ix, size - ix);
				if (read <= 0) {	// this should never happen
					buf = null;
					break;
				}
				ix += read;
			}
		} catch (IOException e) {
			buf = null;
		} finally {
			try { if (in != null) in.close(); } catch (IOException e) {}
			try { if (file != null) file.close(); } catch (IOException e) {}
		}
		return buf;
	} // loadFromJar()

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

} // ProxLoader
