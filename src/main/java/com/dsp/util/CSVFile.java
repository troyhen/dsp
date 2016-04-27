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

import com.dsp.DspException;
import com.dsp.DspObject;

import java.io.*;					// File, BufferedReader, FileReader, IOException
import java.util.*;				// ArrayList, List, Map

public class CSVFile
{
	private File file;
	private BufferedReader in;
	private String line;
	private int index, columns;
	private DspObject var;
	private PrintStream log;

	public CSVFile(String path) throws IOException {
		file = new File(path);
		in = new BufferedReader(new FileReader(file));
	} // CSVFile()

	public CSVFile(File file) throws IOException {
		this.file = file;
		in = new BufferedReader(new FileReader(file));
	} // CSVFile()

	public CSVFile(File folder, String path) throws IOException {
		file = new File(folder, path);
		in = new BufferedReader(new FileReader(file));
	} // CSVFile()

	public CSVFile(InputStream ins) throws IOException {
		in = new BufferedReader(new InputStreamReader(ins));
	} // CSVFile()

	/**
	 * Close the file, if it hasn't been already.
	 */
	public void close() {
		if (log != null) log.println("CSVFile.close()");
		if (in != null) try { in.close(); } catch (IOException e) {}
		in = null;
		file = null;
		line = null;
		index = -1;
	} // close()

	/**
	 * Calls close().
	 */
	public void finalize() {
		if (log != null) log.println("CSVFile.finalize()");
		close();
	} // finalize()

	/**
	 * Returns the next field as a String, or null if it's the end.
	 */
	private String nextField() {
		if (log != null) log.print("CSVFile.nextField() -> ");
		int len;
		if (index < 0 || line == null || index >= (len = line.length())) { if (log != null) log.println("null"); return null; }
		boolean inString = false;
		StringBuffer buf = new StringBuffer();
	loop:
		while (index < len) {
			char c = line.charAt(index++);
			switch (c) {
			case '"':
					// treat two quotes as a single and don't terminate the string
				if (inString && index < len && line.charAt(index) == '"') {
					index++;
				} else {
					inString = !inString;
					continue loop;
				}
				break;
			case ',':
				if (!inString) break loop;
			} // switch
			buf.append(c);
		} // for
		if (log != null) log.println("'" + buf.toString().trim() + "', index=" + index);
		return buf.toString().trim();
	} // nextField()

	/**
	 * Reads the next record of CSV data.  Note, the line may extend to multiple lines, when the return
	 * is within quotes.
	 * @returns true if at the EOF
	 */
	private boolean readRecord() throws IOException {
		if (log != null) log.print("CSVFile.readRecord() -> ");
		index = -1;
		line = null;
		StringBuffer buf = new StringBuffer();
    boolean inQuote = false;
	  for (;;) {
			String read = in.readLine();
			if (read == null) { if (log != null) log.println("true"); return true; }
	    for (int ix = 0, ixz = read.length(); ix < ixz;) {
	      if (read.charAt(ix++) == '"') {
					if (!inQuote || ix >= ixz || read.charAt(ix) != '"') {
						inQuote = !inQuote;
					} else {
						ix++;
					}
				}
	    }
	    buf.append(read);
	    if (!inQuote) break;
	    buf.append("\r\n");
	  } // forever
	  line = buf.toString();
	  index = 0;
	  if (log != null) log.println("false"); return false;
	} // readRecord()

	/**
	 * Reads a row from the csv file, and returns it as a List of Strings, or null
	 * if at the EOF.
	 */
	public List<String> readRow() throws DspException {
		if (log != null) log.print("CSVFile.readRow() -> ");
		ArrayList<String> list = null;
		try {
			if (readRecord()) return list;
			list = columns > 0 ? new ArrayList<String>(columns) : new ArrayList<String>();
			for (;;) {
				String field = nextField();
				if (field == null) break;
				list.add(field);
			} // for
			if (columns < list.size()) columns = list.size();
		} catch (IOException e) {
			if (var != null) try { var.set("error", "Couldn't read " + file.getName() + "  " + e.toString()); } catch (Exception e1) {}
			else throw new DspException("Couldn't read " + file.getName(), e);
		}
		if (log != null) log.println(list == null ? "null" : list.size() + " columns");
		return list;
	} // readRow()

	/**
	 * Returns a List of up to max rows/records from the csv file.
	 */
	public List<List<String>> readRows(int max) throws DspException {
		if (log != null) log.print("CSVFile.readRows(" + max + ") -> ");
		List<List<String>> result = new ArrayList<List<String>>();
		for (int ix = 0; ix < max; ix++) {
			List<String> row = readRow();
			if (row != null) result.add(row);
		}
		if (result.size() < max && var != null) {
			try { var.set("msg", "The file " + file.getName() + " does not appear to contain any data"); } catch (Exception e) {}
		}
		if (log != null) log.println(result.size() + " rows");
		return result;
	} // readRows()

	/**
	 * Set the debug log output stream.  This is for debugging purposes only.
	 */
	public void setDebugLog(PrintStream log) { this.log = log; }

	/**
	 * Set the var object.  This is used for setting var.error or var.msg, instead of throwing
	 * exceptions.
	 */
	public void setVar(DspObject var) { this.var = var; }

} // CSVFile
