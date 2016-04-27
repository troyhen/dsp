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

import javax.servlet.ServletException;

/**
 * The root class of all DSP exceptions
 */
public class DspException extends ServletException
{
	private static final long serialVersionUID = 2945355115547931962L;

	/** Create an exception without any message. */
	public DspException() {}

	/** Create an exception with a message. */
	public DspException(String msg) { super(msg); }

	/** Create an exception with an root cause Throwable. */
	public DspException(Throwable cause) { super(cause); }

	/** Create an exception with a message and a root cause Throwable. */
	public DspException(String msg, Throwable cause) { super(msg, cause); }

} // DspException

