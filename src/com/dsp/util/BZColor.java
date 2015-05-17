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

import java.awt.Color;
import java.io.PrintStream;
//import java.util.Vector;

public class BZColor //implements DspObject
{
/*
public static final String COLOR		= "color";
	public static final String HEXCOLOR	= "hexcolor";
	public static final String INTCOLOR	= "intcolor";
	public static final String RED			= "red";
	public static final String GRE			= "gre";
	public static final String BLU			= "blu";
	public static final String HUE			= "hue";
	public static final String SAT			= "sat";
	public static final String BRI			= "bri";
*/
	private boolean		needRGB, needHSB;
	private float[]		hsb = new float[3], rgb = new float[3];
	private int			red, green, blue;
	PrintStream			log;

	public BZColor()
	{
		this(false, false, null);
	} // BZColor()

	public BZColor(PrintStream log)
	{
		this(true, false, log);
	} // BZColor()

	public BZColor(boolean debug, boolean trace, PrintStream log)
	{
		this.log = log;
		if (log == null) this.log = System.out;
	} // BZColor()

	private void calcHSB()
	{
		Color.RGBtoHSB(red, green, blue, hsb);
		needHSB = false;
	} // calcHSB()

	private void calcRGB()
	{
		int val = Color.HSBtoRGB(hsb[0], hsb[1], hsb[2]);
		red   = (val >>> 16) & 255;
		green = (val >>> 8) & 255;
		blue  = val & 255;
		rgb[0] = red / (float)255;
		rgb[1] = green / (float)255;
		rgb[2] = blue / (float)255;
		needRGB = false;
	} // calcRGB()
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
		name = name.toLowerCase();
		if (name.startsWith(RED))
		{
			if (needRGB) calcRGB();
			result = new Float(rgb[0]);
		}
		else
		if (name.startsWith(GRE))
		{
			if (needRGB) calcRGB();
			result = new Float(rgb[1]);
		}
		else
		if (name.startsWith(BLU))
		{
			if (needRGB) calcRGB();
			result = new Float(rgb[2]);
		}
		else
		if (name.startsWith(HUE))
		{
			if (needHSB) calcHSB();
			result = new Float(hsb[0]);
		}
		else
		if (name.startsWith(SAT))
		{
			if (needHSB) calcHSB();
			result = new Float(hsb[1]);
		}
		else
		if (name.startsWith(BRI))
		{
			if (needHSB) calcHSB();
			result = new Float(hsb[2]);
		}
		else
		if (name.equals(HEXCOLOR))
		{
			if (needRGB) calcRGB();
			String val =  Integer.toHexString((red << 16) | (green << 8) | blue);
			while (val.length() < 6) val = "0" + val;	// Make sure it's 6 digits
			result = val;
		}
		else
		if (name.equals(INTCOLOR))
		{
			if (needRGB) calcRGB();
			result = new Integer((red << 16) | (green << 8) | blue);
		}
		else throw new IllegalArgumentException(COLOR + '.' + name + " is not defined");
		if (debug) log.println(COLOR + '.' + name + " -> " + result);
		return result;
	} // get()
*/
	public float getBlue()
	{
		if (needRGB) calcRGB();
		return rgb[2];
	} // getBlue()

	public float getBri()
	{
		if (needHSB) calcHSB();
		return hsb[2];
	} // getBri()

	public float getBrightness()
	{
		if (needHSB) calcHSB();
		return hsb[2];
	} // getBrightness()

	public float getGreen()
	{
		if (needRGB) calcRGB();
		return rgb[1];
	} // getGreen()

	public String getHexColor()
	{
		if (needRGB) calcRGB();
		String val =  Integer.toHexString((red << 16) | (green << 8) | blue);
		while (val.length() < 6) val = "0" + val;	// Make sure it's 6 digits
		return val;
	} // getHexColor()

	public float getHue()
	{
		if (needHSB) calcHSB();
		return hsb[0];
	} // getHue()

	public int getIntColor()
	{
		if (needRGB) calcRGB();
		return (red << 16) | (green << 8) | blue;
	} // getIntColor()

	public float getRed()
	{
		if (needRGB) calcRGB();
		return rgb[0];
	} // getRed()

	public float getSat()
	{
		if (needHSB) calcHSB();
		return hsb[1];
	} // getSat()

	public float getSaturation()
	{
		if (needHSB) calcHSB();
		return hsb[1];
	} // getSaturation()

/*
	public Object run(String function, Object[] args) throws IllegalArgumentException
	{
		if (trace) log.println(this + ".run(" + function + ')');
		throw new IllegalArgumentException(COLOR + '.' + function + "() is not defined");
	} // run()

	public void set(String name, Object value) throws IllegalArgumentException
	{
		if (trace) log.println(this + ".set(" + name + ", " + value + ')');
		name = name.toLowerCase();
		if (INTCOLOR.equalsIgnoreCase(name))
		{
			Color color = new Color(DspPage._int(value));
			red = color.getRed();
			green = color.getGreen();
			blue = color.getBlue();
			rgb[0] = red / (float)255;
			rgb[1] = green / (float)255;
			rgb[2] = blue / (float)255;
			needHSB = true;
		}
		else
		if (HEXCOLOR.equalsIgnoreCase(name))
		{
			String hex = DspPage._String(value);
			Color color = new Color(Integer.parseInt(hex, 16));
			red = color.getRed();
			green = color.getGreen();
			blue = color.getBlue();
			rgb[0] = red / (float)255;
			rgb[1] = green / (float)255;
			rgb[2] = blue / (float)255;
			needHSB = true;
		}
		else
		if (name.startsWith(RED))
		{
			rgb[0] = DspPage._float(value);
			red = (int)(rgb[0] * 255 + .5);
			needHSB = true;
		}
		else
		if (name.startsWith(GRE))
		{
			rgb[1] = DspPage._float(value);
			green = (int)(rgb[1] * 255 + .5);
			needHSB = true;
		}
		else
		if (name.startsWith(BLU))
		{
			rgb[2] = DspPage._float(value);
			blue = (int)(rgb[2] * 255 + .5);
			needHSB = true;
		}
		else
		if (name.startsWith(HUE))
		{
			hsb[0] = DspPage._float(value);
			needRGB = true;
		}
		else
		if (name.startsWith(SAT))
		{
			hsb[1] = DspPage._float(value);
			needRGB = true;
		}
		else
		if (name.startsWith(BRI))
		{
			hsb[2] = DspPage._float(value);
			needRGB = true;
		}
		else throw new IllegalArgumentException(COLOR + " can't set " + name + " to " + value);
		if (debug) log.println("color." + name + " <- " + value);
	} // set()
*/
	public void setBlue(float value)
	{
		rgb[2] = value;
		blue = (int)(rgb[2] * 255 + .5);
		needHSB = true;
	} // setBlue()

	public void setBri(float value)
	{
		hsb[2] = value;
		needRGB = true;
	} // setBri()

	public void setBrightness(float value)
	{
		hsb[2] = value;
		needRGB = true;
	} // setBrightness()

	public void setGreen(float value)
	{
		rgb[1] = value;
		green = (int)(rgb[1] * 255 + .5);
		needHSB = true;
	} // setGreen()

	public void setHexColor(String value)
	{
		Color color = new Color(Integer.parseInt(value, 16));
		red = color.getRed();
		green = color.getGreen();
		blue = color.getBlue();
		rgb[0] = red / (float)255;
		rgb[1] = green / (float)255;
		rgb[2] = blue / (float)255;
		needHSB = true;
	} // setHexColor()

	public void setHue(float value)
	{
		hsb[0] = value;
		needRGB = true;
	} // setHue()

	public void setIntColor(int value)
	{
		Color color = new Color(value);
		red = color.getRed();
		green = color.getGreen();
		blue = color.getBlue();
		rgb[0] = red / (float)255;
		rgb[1] = green / (float)255;
		rgb[2] = blue / (float)255;
		needHSB = true;
	} // setIntColor()

	public void setRed(float value)
	{
		rgb[0] = value;
		red = (int)(rgb[0] * 255 + .5);
		needHSB = true;
	} // setRed()

	public void setSat(float value)
	{
		hsb[1] = value;
		needRGB = true;
	} // setSat()

	public void setSaturation(float value)
	{
		hsb[1] = value;
		needRGB = true;
	} // setSaturation()

	public String toString()
	{
		return "Color[r " + red + ", g " + green + ", b " + blue + ']';
	} // toString()
/*
	public void unset(String name) throws IllegalArgumentException
	{
		if (trace) log.println(this + ".unset(" + name + ')');
		name = name.toLowerCase();
		if (INTCOLOR.equalsIgnoreCase(name) || HEXCOLOR.equalsIgnoreCase(name))
		{
			rgb[0] = rgb[1] = rgb[2] = hsb[0] = hsb[1] = hsb[2] = 0;
			needHSB = false;
		}
		else
		if (name.startsWith(RED))
		{
			rgb[0] = 0;
			red = 0;
			needHSB = true;
		}
		else
		if (name.startsWith(GRE))
		{
			rgb[1] = 0;
			green = 0;
			needHSB = true;
		}
		else
		if (name.startsWith(BLU))
		{
			rgb[2] = 0;
			blue = 0;
			needHSB = true;
		}
		else
		if (name.startsWith(HUE))
		{
			hsb[0] = 0;
			needRGB = true;
		}
		else
		if (name.startsWith(SAT))
		{
			hsb[1] = 0;
			needRGB = true;
		}
		else
		if (name.startsWith(BRI))
		{
			hsb[2] = 0;
			needRGB = true;
		}
		else throw new IllegalArgumentException(COLOR + " can't unset " + name);
		if (debug) log.println("color." + name + " <- Null");
	} // unset()
*/
} // BZColor

