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

import java.io.*;	// FileWriter, PrintWriter
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;	// List, Vector

import javax.servlet.http.*;

public class DspStatementLog extends Thread {

	public static final String PATH = "/C:/prop/";
	public static final String NAME = "sqlLog ";
	public static final String EXT = ".csv";
	public static final int TIMEOUT = 45 * 1000;		// 45 seconds
	public static final int SLEEP_TIME = 10 * 1000;		// 10 seconds
	public static final int FLUSH_SIZE = 500;			// size of buffer until flush
	public static final int FLUSH_TIME = 60 * 1000;		// time in log until flush
	private static DateFormat YYMMDD = new SimpleDateFormat("yy-MM-dd");
	private static DateFormat LOGDATE = new SimpleDateFormat("HH:mm:ss");

	private static DspStatementLog instance;
	private static int logThresh = 1000, killTimeout = TIMEOUT;
	private static ThreadLocal<Entry> entryHolder = new ThreadLocal<Entry>();
	private static ThreadLocal<HttpServletRequest> reqHolder = new ThreadLocal<HttpServletRequest>();

	private List<Entry> open, log;
	private PrintWriter out;

	class Entry {
		String page;
		String request;
		String name;
		String statement;
		Thread thread;
		StackTraceElement[] stack;
		int rows;
		Throwable error;
		long time;
		int msec;
		boolean killed;
	} // Entry

	private DspStatementLog() {
		open = new Vector<Entry>();
		log = new Vector<Entry>();
	} // DspStatementLog()

	public static void close() {
		System.out.println("DspStatementLog.close()");
		if (instance == null) return;
		if (instance.open.size() > 0) System.out.println("Closing the log with open statements");
		instance.flush();
		if (instance.out != null) instance.out.close();
		instance = null;
		reqHolder.set(null);
		entryHolder.set(null);
	} // close()

	public void finalize() {
		System.out.println("DspStatementLog.finalize()");
		close();
	} // finalize()

	public void flush() {
		boolean done = true;
		synchronized (log) {
			Iterator<Entry> it = log.iterator();
			while (it.hasNext()) {
				Entry entry = (Entry)it.next();
				if (entry.msec >= logThresh) {
					done = false;
					break;
				}
			}
		}
		if (done) {
			synchronized (log) {
				log.clear();
			}
			if (out != null) {
				out.close();
				out = null;
			}
			return;
		}
		if (out == null) {
			try {
				out = new PrintWriter(new FileWriter(PATH + NAME + YYMMDD.format(new Date()) + EXT, true));
				out.println("Time,Page,Arguments,Name,SQL,Msec,Rows,Error,Killed,Stack...");
			} catch (IOException e) {
				System.out.println("Can't open statement log: " + e);
				out = new PrintWriter(System.out);
			}
		}
		if (out != null) {
			synchronized (log) {
				Iterator<Entry> it = log.iterator();
				while (it.hasNext()) {
					Entry entry = (Entry)it.next();
					it.remove();
					out.print('"');
					out.print(LOGDATE.format(new Date(entry.time)));
					out.print("\",");
					out.print(entry.page);
					out.print(',');
					out.print(entry.request);
					out.print(',');
					out.print(entry.name);
					out.print(",\"");
					out.print(entry.statement);
					out.print("\",");
					out.print(entry.msec);
					out.print(',');
					if (entry.rows >= 0) out.print(entry.rows);
					out.print(",\"");
					if (entry.error != null) out.print(entry.error);
					out.print("\",");
					if (entry.killed) {
						out.print("Killed!");
					}
					for (int ix = 0, ixz = entry.stack.length; ix < ixz; ix++) {
						StackTraceElement trace = entry.stack[ix];
						String className = trace.getClassName().toLowerCase();
						if (className.indexOf("dspstatement") >= 0) continue;
						if (className.indexOf("c_") >= 0 || className.indexOf("d_") >= 0 || className.indexOf("dsp") >= 0) {
							out.print(',');
							out.print(trace.toString());
						}
					}
					out.println();
				}
			}
			out.flush();
		}
	} // flush()

	public static DspStatementLog getInstance() {
		if (instance == null) {
			instance = new DspStatementLog();
			instance.start();
		}
		return instance;
	} // getInstance()

	public static void logError(Throwable err) {
		getInstance().setError(err);
	} // logError()

	public static void logRequest(HttpServletRequest req) {
		getInstance().setRequest(req);
	} // logRequest()

	public static void logRows(int rows) {
		getInstance().setRows(rows);
	} // logError()

	public static void logStatement(String name, String stmt) {
		getInstance().setStatement(name, stmt);
	} // logRequest()

	public static void logTime() {
		getInstance().setTime();
	} // logTime()

	@SuppressWarnings("deprecation")
	public void run() {
		System.out.println("DspStatementLog thread started");
		long nextFlush = System.currentTimeMillis() + FLUSH_TIME;
		while (instance != null) {
            try {
                long time = System.currentTimeMillis();
                long timeout = time - killTimeout;
                synchronized (open) {
                    Iterator<Entry> it = open.iterator();
                    while (it.hasNext()) {
                        Entry entry = (Entry)it.next();
                        if (timeout > entry.time) {
                            it.remove();
                            entry.msec = (int)(time - entry.time);
                            entry.killed = true;
                            synchronized (log) {
                                log.add(entry);
                            }
                            entry.thread.stop(new DspException("Statement " + entry.name + " was killed because it was hung"));
                        }
                    }
                }
                if (time >= nextFlush) {
                    nextFlush += FLUSH_TIME;
                    flush();
                }
				sleep(SLEEP_TIME);
			} catch (Exception e) {
				// Don't let an exception kill this thread
                e.printStackTrace();
			}
		}
		System.out.println("DspStatementLog thread stopped");
	} // run()

	public static void setLogThreshold(int sec) {
		logThresh = sec * 1000;
	} // setLogThreshold()

	public static void setKillTimeout(int sec) {
		killTimeout = sec * 1000;
	} // setKillTimeout()

	public void setRequest(HttpServletRequest req) {
		reqHolder.set(req);
	} // setRequest()

	public void setError(Throwable err) {
		Entry entry = (Entry)entryHolder.get();
		entry.error = err;
	} // setError()

	public void setRows(int rows) {
		Entry entry = (Entry)entryHolder.get();
		if (entry != null) entry.rows = rows;
	} // setRows()

	public void setStatement(String name, String stmt) {
		Entry entry = new Entry();
		entryHolder.set(entry);
		synchronized (open) {
			open.add(entry);
		}
        entry.thread = Thread.currentThread();
        entry.time = System.currentTimeMillis();
		HttpServletRequest req = (HttpServletRequest)reqHolder.get();
		entry.page = req.getServletPath() + (req.getPathInfo() != null ? req.getPathInfo() : "");
		entry.request = req.getQueryString() != null ? req.getQueryString() : "";
		entry.name = name;
		entry.statement = stmt;
		Throwable t = new RuntimeException();
		t.fillInStackTrace();
		entry.stack = t.getStackTrace();
	} // setStatement()

	public void setTime() {
		Entry entry = (Entry)entryHolder.get();
		synchronized (open) {
			// if my timeout thread killed it, just quit
			if (!open.remove(entry) || entry.msec != 0) return;
		}
		entry.msec = (int)(System.currentTimeMillis() - entry.time);
		synchronized (log) {
			log.add(entry);
		}
		if (log.size() >= FLUSH_SIZE) flush();
	} // setTime()

} // DspStatemetLog
