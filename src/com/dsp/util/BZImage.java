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

import java.io.*;					// File, IOException
import java.awt.*;				// Image, Toolkit
import java.awt.image.ImageObserver;

public class BZImage implements /*DspObject,*/ ImageObserver
{
/*
public static final String IMAGE		= "image";
	public static final String CLOSE		= "close";

	public static final String WIDTH		= "width";
	public static final String HEIGHT		= "height";
	public static final String AREA			= "area";
	public static final String LENGTH		= "length";
	public static final String NAME			= "name";
	public static final String PATH			= "path";
*/
	private boolean			trace, debug;
	private File				base, file;
	private int					width = -1, height = -1;
	private PrintStream	log;

	public BZImage(File base)
	{
		this(base, false, false, null);
	} // BZImage()

	public BZImage(File base, PrintStream log)
	{
		this(base, true, false, log);
	} // BZImage()

	public BZImage(File base, boolean debug, boolean trace, PrintStream log)
	{
		this.base = base;
		this.trace = trace;
		this.debug = debug;
		this.log = log;
		if (log == null) this.log = System.out;
	} // BZFile()
/*
	public boolean exists(String name)
	{
		Object obj = get(name);
		return obj != null;
	} // exists()

	public Object get(String name) throws IllegalArgumentException
	{
		if (trace) log.println(this + ".get(" + name + ')');
		Object result = null;
		if (NAME.equalsIgnoreCase(name)) result = file.getName();
		else
		if (PATH.equalsIgnoreCase(name)) result = file.getPath();
		else
		if (LENGTH.equalsIgnoreCase(name)) result = new Long(file.length());
		else
		{
			open();
			if (AREA.equalsIgnoreCase(name)) result = new Integer(width * height);
			else
			if (HEIGHT.equalsIgnoreCase(name)) result = new Integer(height);
			else
			if (WIDTH.equalsIgnoreCase(name)) result = new Integer(width);
			else throw new IllegalArgumentException(IMAGE + '.' + name + " is not defined");
		}
		if (debug) log.println(IMAGE + '.' + name + " -> " + result);
		return result;
	} // get()
*/
	public int getArea() { open(); return width * height; }
	public int getHeight() { open(); return height; }
	public int getLength() { return (int)file.length(); }
	public String getName() { return file.getName(); }
	public String getPath() { return file.getPath(); }
	public int getWidth() { open(); return width; }

	public boolean imageUpdate(Image img, int infoflags, int x, int y,
			int width, int height)
	{
		if (trace) System.out.println("BZImage.updateImage(" + infoflags + ", " + width + ", " + height + ')');
		if ((infoflags & ImageObserver.HEIGHT) != 0) this.height = height;
		if ((infoflags & ImageObserver.WIDTH) != 0) this.width = width;
		if (this.height >= 0 && this.width >= 0)
		{
			if (debug) System.out.println("image.updateImage() -> done");
			synchronized (this) { notify(); }
			return false;
		}
		if (debug) System.out.println("image.updateImage() -> need more");
		return true;
	} // imageUpdate()

	public synchronized void open()
	{
		if (width >= 0 && height >= 0) return;
		Toolkit tk = Toolkit.getDefaultToolkit();
		Image im = tk.getImage(file.getPath());
		if (debug) log.print("checkImage() -> " + tk.checkImage(im, -1, -1, this));
		if (debug) log.print("prepareImage() -> " + tk.prepareImage(im, -1, -1, this));
		else tk.prepareImage(im, -1, -1, this);
		if (debug) log.print("checkImage() -> " + tk.checkImage(im, -1, -1, this));
		try { wait(20 * 1000); } catch (InterruptedException e) {}
		im.flush();
		if (width < 0) width = 0;
		if (height < 0) height = 0;
	} // open()
/*
	public Object run(String function, Object[] args) throws IllegalArgumentException
	{
		if (trace) log.println(this + ".run(" + function + ')');
		throw new IllegalArgumentException(IMAGE + '.' + function + "() is not defined");
	} // run()

	public void set(String name, Object value) throws IllegalArgumentException
	{
		if (trace) log.println(this + ".set(" + name + ", " + value + ')');
		if (NAME.equalsIgnoreCase(name) || PATH.equalsIgnoreCase(name))
		{
			file = new File(base, DspPage._String(value));
		}
		else throw new IllegalArgumentException(IMAGE + " can't set " + name + " to " + value);
	} // set()
*/
	public void setPath(String value)
	{
		file = new File(base, value);
		width = height = -1;
	} // setPath()

	public String toString()
	{
		return "BZImage[" + file + ']';
	} // toString()
/*
	public void unset(String name) throws IllegalArgumentException
	{
		if (trace) log.println(this + ".unset(" + name + ')');
		if (NAME.equalsIgnoreCase(name) || PATH.equalsIgnoreCase(name)) file = null;
		else throw new IllegalArgumentException(IMAGE + " can't unset " + name);
	} // unset()
*/
} // BZFile

