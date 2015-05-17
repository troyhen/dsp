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
package com.dsp.util;

import java.io.*;	// File, IOException, InputStream
import java.sql.Timestamp;
import java.util.*;	// TreeSet, Vector

public class BZFile //implements DspObject
{
	private byte[]					buf;
	private boolean					trace, debug;
	private PrintStream			log;
	private File						base, file;
	private BufferedReader	in;
	private PrintWriter 		out;
	private String					relPath;

	public BZFile(File base) throws IOException
	{
		this(base, false, false, null);
	} // BZFile()

	public BZFile(File base, PrintStream log) throws IOException
	{
		this(base, true, false, log);
	} // BZFile()

	public BZFile(File base, boolean debug, boolean trace, PrintStream log) throws IOException
	{
		this.base = base.getCanonicalFile();
		this.debug = debug;
		this.trace = trace;
		this.log = log;
		if ((debug || trace) && log == null) log = System.out;
		if (trace) log.println("BZFile()");
		if (debug) log.println("BZFile(" + base + ')');
	} // BZFile()

	public void finalize() throws Throwable
	{
		if (trace) log.println("BZFile.finalize()");
		close();
	} // finalize()

	public void close()
	{
		if (trace) log.println("BZFile.close()");
		if (in != null) try { in.close(); } catch (IOException e) {}
		in = null;
		if (out != null) out.close();
		out = null;
		if (debug) log.println("file.close(" + this + ')');
	} // close()

	public void copyFile(String targetPath) throws IOException
	{
		if (trace) log.println("BZFile.copy(" + targetPath + ')');
		File to;
		if (targetPath.startsWith("/") || targetPath.startsWith(File.separator))
		{
			to = new File(targetPath);
		}
		else
		{
			to = new File(base, targetPath);
		}
		writeLog("Copying " + file + " to " + to, debug, log);
		if (!file.exists())
		{
			writeLog("File doesn't exist, nothing copied", debug, log);
			return;
		}
		if (!file.isFile()) throw new IOException(file + " is not a file");
		to.getParentFile().mkdirs();	// make sure the folders are there
//		try {
			copyFile(file, to);
//		} catch (IOException e) {
//			throw new IOException("Error copying: " + e);
//		}
	} // copyFile()

	private synchronized void copyFile(File from, File to) throws IOException
	{
		if (trace) log.println("BZFile.copyFile(" + from + ", " + to + ')');
		close();
		long modified = from.lastModified();
		if (to.exists() && from.length() == to.length()
				&& modified == to.lastModified()) return;
		FileInputStream in = new FileInputStream(from);
		FileOutputStream out = new FileOutputStream(to);
		if (buf == null) buf = new byte[32768];
//		int ix = 0;
		for (;;)
		{
			int read = in.read(buf);
			if (read <= 0) break;
			out.write(buf, 0, read);
//			ix += read;
		}
		try { in.close(); } catch (IOException e) {}
		try { out.close(); } catch (IOException e) {}
		to.setLastModified(from.lastModified());
		writeLog("Copied " + from.getName(), debug, log);
	} // copyFile()

	public void copyFolder(String targetPath) throws IOException
	{
		if (trace) log.println("BZFile.copyFolder(" + targetPath + ')');
		close();
		File to;
		if (targetPath.startsWith("/") || targetPath.startsWith(File.separator))
		{
			to = new File(targetPath);
		}
		else
		{
			to = new File(base, targetPath);
		}
		writeLog("Copying " + file + "/ to " + to + '/', debug, log);
		if (!file.exists())
		{
			writeLog("Folder doesn't exist, nothing copied", debug, log);
			return;
		}
		if (!file.isDirectory()) throw new IOException(file + " is not a folder");
//		try {
			copyFolder(file, to);
//		} catch (IOException e) {
//			throw new BZException("Error copying: " + e);
//		} catch (NullPointerException e) {
//			log.println(e);
//			e.printStackTrace();
//			throw e;
//		}
	} // copyFolder()

	private void copyFolder(File from, File to) throws IOException
	{
		String name = from.getName();
		if (name.equals(".") || name.equals("..")) return;
		if (trace) log.println("BZFile.copyFolder(" + from + ", " + to + ')');
		if (to.exists() && !to.isDirectory())
		{
			if (!to.delete())
			{
				throw new IOException("Couldn't delete file " + to);
			}
		}
		if (!to.exists())
		{
			if (!to.mkdirs())
			{
				throw new IOException("Couldn't create folder " + to);
			}
		}
		TreeSet<String> fromFiles = listFiles(from, trace, debug);
		TreeSet<String> toFiles = listFiles(to, trace, debug);
		if (fromFiles.size() == 0) return;
		Iterator<String> fromIt = fromFiles.iterator();
		Iterator<String> toIt = toFiles.iterator();
		File fromFile = null, toFile = null;
		String fromName = null, toName = null;
		for (;;)
		{
			if (fromFile == null && fromIt.hasNext())
			{
				fromName = fromIt.next();
				fromFile = new File(from, fromName);
			}
			if (toFile == null && toIt.hasNext())
			{
				toName = toIt.next();
				toFile = new File(to, toName);
			}
			if (fromFile == null && toFile == null) break;
			if (fromFile == null)
			{
				if (toFile.isDirectory()) deleteFolder(toFile);
				else deleteFile(toFile);
				toFile = null;
			}
			else
			if (toFile == null)
			{
				File tof = new File(to, fromName);
				if (fromFile.isDirectory()) copyFolder(fromFile, tof);
				else copyFile(fromFile, tof);
				fromFile = null;
			}
			else
			{
				int comp = fromName.compareTo(toName);
				if (comp < 0)
				{
					File tof = new File(to, fromName);
					if (fromFile.isDirectory()) copyFolder(fromFile, tof);
					else copyFile(fromFile, tof);
					fromFile = null;
				}
				else if (comp > 0)
				{
					if (toFile.isDirectory()) deleteFolder(toFile);
					else deleteFile(toFile);
					toFile = null;
				}
				else
				{
					if (fromFile.isDirectory()) copyFolder(fromFile, toFile);
					else copyFile(fromFile, toFile);
					fromFile = toFile = null;
				}
			}
		}
		writeLog("Copied " + name + '/', debug, log);
	} // copyFolder()

	public boolean deleteFile() throws IOException
	{
		if (trace) log.println("BZFile.delete()");
		close();
		writeLog("Deleting " + file, true, debug, log);
		if (!file.exists())
		{
			writeLog("File was already deleted", debug, log);
			return true;
		}
		if (!file.isFile()) throw new IOException(file + " is not a file");
		try {
			deleteFile(file);
		} catch (IOException e) {
			throw new IOException("Error deleting " + this.file + ": " + e);
		}
		return true;
	} // deleteFile()

	private void deleteFile(File file) throws IOException
	{
		if (trace) log.println("BZFile.delete(" + file + ')');
		close();
		if (!file.exists()) return;
		if (!file.delete()) throw new IOException("Couldn't delete " + file);
		writeLog("Deleted " + file.getName(), debug, log);
	} // deleteFile()

	public void deleteFolder() throws IOException
	{
		if (trace) log.println("BZFile.deleteFolder()");
		writeLog("Deleting " + file + '/', true, debug, log);
		if (!file.exists())
		{
			writeLog("Folder was already deleted", debug, log);
		}
		if (!file.isDirectory()) throw new IOException(file + " is not a folder");
		try {
			deleteFolder(file);
		} catch (IOException e) {
			throw new IOException("Error deleting " + this.file + "/: " + e);
		}
	} // deleteFolder()

	private void deleteFolder(File file) throws IOException
	{
		if (!file.exists()) return;
		if (!file.isDirectory()) throw new IOException(file + " is not a folder");
		String name = file.getName();
		if (name.equals(".") || name.equals("..")) return;
		if (trace) log.println("BZFile.deleteFolder(" + file + ')');
		File[] files = file.listFiles();
		if (files == null) throw new IOException(file + " is not a directory");
		for (int ix = 0, ixz = files.length; ix < ixz; ix++)
		{
			File f = files[ix];
			if (f.isDirectory()) deleteFolder(f);
			else deleteFile(f);
		}
		file.delete();
		writeLog("Deleted " + name + '/', debug, log);
	} // deleteFolder()

	public boolean exists()
	{
		boolean result = file.exists();
		if (debug) log.println("exists(" + file + ") => " + result);
		return result;
	} // exists()

	public File getBase() { return base; }
	public Timestamp getDate() { return new Timestamp(file.lastModified()); }
	public File getFile() { return file; }
	public long getLength() { return file.length(); }
	public String getName() { return file.getName(); }
	public String getRelPath() { return relPath; }
	public String getPath() { return file.getPath(); }
	public boolean isOpen() { return in != null || out != null; }
	public boolean isDir() { return file.isDirectory(); }
	public boolean isFile() { return file.isFile(); }
	public long length() { return file.length(); }

	private TreeSet<String> listFiles(File dir, boolean trace, boolean debug) throws IOException
	{
		if (trace) log.println("BZFile.listFiles(" + dir + ')');
		String[] files = dir.list();
		if (files == null) throw new IOException(dir + " is not a directory");
		int ixz = files.length;
		TreeSet<String> set = new TreeSet<String>();
		if (ixz > 0)
		{
			for (int ix = 0; ix < ixz; ix++)
			{
				set.add(files[ix]);
			}
		}
		if (debug) log.println(dir.getName() + " contains " + set.size() + " items");
		return set;
	} // listFiles()

	public boolean makeDirs()
	{
		if (trace) log.println("BZFile.makeDirs()");
		close();
		boolean result = file.mkdirs();
		if (debug) log.println("file.makeDirs(" + this + ") -> " + result);
		return result;
	} // makeDirs()

	public void open(String type) throws IllegalArgumentException, IOException
	{
		if (trace) log.println(this + "BZFile.open(" + type + ')');
		close();
		if (type == null || type.length() != 1) throw new IllegalArgumentException(
				"file.open() only accepts a single letter code (r, w, or a)");
		char c = type.charAt(0);
		if (c != 'r' && c != 'w' && c != 'a') throw new IllegalArgumentException(
				"file.open() only accepts r, w, or a");
		if (c == 'r')
		{
			in = new BufferedReader(new FileReader(file));
		}
		else
		{
			boolean append = c == 'a';
			out = new PrintWriter(new FileWriter(file.getPath(), append), true);
		}
		if (debug) log.println("file.open(" + this + ')');
	} // open()

	/**
	 * Calculates the relative path from combined path back to base.
	 * @return relative path back to target folder, in Posix format
	 */
	public String pathBack() throws IOException
	{
		return pathBack(base, relPath);
	} // pathBack()

	/**
	 * Calculates the path back to a folder.  Given a full path to a folder, and a relative path to
	 * another file, compute a relative path from back to the target folder.
	 * @param target absolute path to target folder
	 * @param path relative path from target folder, in Posix format
	 * @return relative path back to target folder, in Posix format
	 */
	public static String pathBack(File target, String path) throws IOException
	{
		return pathBack(target.getCanonicalPath(), path);
	} // pathBack()

	/**
	 * Calculates the path back to a folder.  Given a full path to a folder, and a relative path to
	 * another file, compute a relative path from back to the target folder.
	 * @param target full path to the target folder, in OS format
	 * @param path relative path from target folder, in Posix format
	 * @return relative path back to target folder, in Posix format
	 */
	public static String pathBack(String target, String path)
	{
//		int iz = target.lastIndexOf(File.separatorChar);    // remove 'webedit'
//		target = target.substring(0, iz);
//		int len = path.length();
		StringBuffer buf = new StringBuffer();
		int ix0 = -1, ix = 0;
		int iy0 = target.length(), iy = 0;
		String tfolder = null;
		for (;;) {
			while (tfolder == null)
			{
				if (iy0 < 0) throw new IllegalArgumentException("path is not relative to target in pathBack()");
				iy = target.lastIndexOf(File.separatorChar, iy0 - 1);
				tfolder = iy < 0 ? target.substring(0, iy0) : target.substring(iy + 1, iy0);
				if (tfolder.length() == 0) tfolder = null;
				iy0 = iy;
			}
			ix = path.indexOf('/', ++ix0);
			String folder = ix < 0 ? path.substring(ix0) : path.substring(ix0, ix);
			if (folder.length() > 0)
			{
				if (folder.equals(".."))
				{
					if (buf.length() > 0) buf.insert(0, '/');
					buf.insert(0, tfolder);
					tfolder = null;
				}
				else if (!folder.equals("."))
				{
					if (buf.length() > 0) buf.insert(0, '/');
					buf.insert(0, "..");
				}
			}
			if (ix < 0) break;
      ix0 = ix;
		}
//System.out.println("BZFile.pathBack(" + target + ", " + path + ") -> " + buf);
		return buf.toString();
	} // pathBack()

	public void print(Object value)
	{
		if (trace) log.println("BZFile.print(" + value + ')');
		if (value != null) out.print(value);
		if (debug) log.println("file.print(" + value + ')');
	} // print()

	public void println(Object value)
	{
		if (trace) log.println("BZFile.println(" + value + ')');
		if (value != null) out.println(value);
		if (debug) log.println("file.println(" + value + ')');
	} // println()

	public String readLine() throws IOException
	{
		if (trace) log.println("BZFile.readLine()");
		if (in == null) return null;
		String result = null;
		result = in.readLine();
		if (result == null) close();
		if (debug) log.println("file.readLine() -> '" + result + '\'');
		return result;
	} // readLine()

	public String readAll() throws IOException
	{
		if (trace) log.println("BZFile.readAll()");
		if (in == null) return null;
		String result = null;
		String inputLine = null;
		while ((inputLine = in.readLine()) != null)
		{
			result = result.concat(inputLine);
			result = result.concat("\r\n");
		}
		if (result == null) close();
		if (debug) log.println("file.readAll() -> '" + result + '\'');
		return result;
	} // readLine()

	public boolean rename(String newPath)
	{
		if (trace) log.println("BZFile.rename(" + newPath + ')');
		close();
		if (!newPath.startsWith("/")) newPath = file.getParent() + File.separatorChar + newPath;
		File target = new File(newPath);
		boolean result = file.renameTo(target);
		if (result) file = target;
		if (debug) log.println("file.rename(" + target + ") -> " + result);
		return result;
	} // rename()

	public void setBase(File value) throws IOException
	{
		if (trace) log.println("setBase(" + value + ')');
		close();
		base = value.getCanonicalFile();
		if (relPath != null) file = new File(base, relPath).getCanonicalFile();
		if (debug) log.println("setBase(" + value + ") -> " + base);
	} // setBase()

	public void setBase(String value) throws IOException
	{
		setBase(new File(value));
	} // setBase()

	public void setPath(String path) throws IOException
	{
		if (trace) log.println("setPath(" + path + ')');
		relPath = path;
		close();
		if (path.startsWith("/")) file = new File(path);
		else file = new File(base, path);
		try { file = file.getCanonicalFile(); } catch (IOException e) {}
		if (debug) log.println("setPath(" + path + ") -> " + file);
	} // setPath()

	public String toString()
	{
		return relPath == null ? "BZFile[path not set]" : relPath;
	} // toString()

	private static void writeLog(String msg, boolean debug, PrintStream log)
	{
		writeLog(msg, false, debug, log);
	} // writeLog()

	private static synchronized void writeLog(String msg, boolean stack, boolean debug, PrintStream log)
	{
		if (debug) log.println(msg);
		PrintStream out = null;
		File file = new File("/C:/prop/FileLog.txt");
		if (file.length() > 256000)
		{
			File from = new File("/C:/prop/FileLog9.txt"), to;
			from.delete();
			int ix = 9;
			while (ix > 0)
			{
				to = from;
				if (--ix > 0) from = new File("/C:/prop/FileLog" + ix + ".txt");
				else from = file;
				from.renameTo(to);
			}
		}
//			// retry 3 times
//		for (int ix = 0; ix < 3; ix++)
//		{
			try {
				out = new PrintStream(new FileOutputStream(file.getPath(), true));
				out.print(new java.util.Date());
				out.print(" ");
				out.println(msg);
				if (stack) throw new Exception("The current stack trace for this call");
//				break;
			} catch (IOException e) {
				System.out.println(msg);
			} catch (Exception e) {
				e.printStackTrace(out);
//				break;
			} finally {
				if (out != null) out.close();
			}
//				// wait .2 seconds and try again
//			try { Thread.sleep(200); } catch (InterruptedException e) {}
//		}
	} // writeLog()

} // BZFile

