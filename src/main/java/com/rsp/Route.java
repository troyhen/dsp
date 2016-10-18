package com.rsp;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;

import com.dsp.util.BZText;

public class Route {

	private static final Pattern blank = Pattern.compile("\\w*");
	private static final Pattern comment = Pattern.compile("\\w*#.*");
	private static final Pattern typed = Pattern.compile("\\s*(get|post|put|delete)\\s+(/\\S*)\\s+(/[^#\\s]*)\\s*(#.*)?");
	private static final Pattern untyped = Pattern.compile("\\s*(/\\S*)\\s+(/[^#\\s]*)\\s*(#.*)?");
	private static final Pattern colonName = Pattern.compile(":([^:/\\s]+)");
	
	private String type;
	private Pattern path;
	private String target;
	private List<String> args = new ArrayList<>();

	private Pattern makePattern(String path) {
		StringBuilder buf = new StringBuilder();
		Matcher matcher = colonName.matcher(path);
		int index = 0;
		while (matcher.find()) {
			String arg = matcher.group(1);
			args.add(arg);
			buf.append(path.substring(index, matcher.start()));
			buf.append("([^/?#]+)");
			index = matcher.end();
		}
		buf.append(path.substring(index));
		return Pattern.compile(buf.toString());
	}

	public static Route parse(String line) {
		if (comment.matcher(line).matches() || blank.matcher(line).matches()) {
			return null;
		}
		Matcher matcher = typed.matcher(line);
		if (matcher.matches()) {
			Route route = new Route();
			route.type = matcher.group(1).toUpperCase();
			route.path = route.makePattern(matcher.group(2));
			route.target = matcher.group(3);
			return route;
		}
		matcher = untyped.matcher(line);
		if (matcher.matches()) {
			Route route = new Route();
			route.type = null;
			route.path = route.makePattern(matcher.group(1));
			route.target = matcher.group(2);
			return route;
		}
		throw new RuntimeException("Invalid route: " + line);
	}

	public String route(HttpServletRequest req) {
		if (type != null && !req.getMethod().equals(type)) return null;
		String path = req.getPathInfo();
		if (path == null) path = req.getServletPath();
		Matcher matcher = this.path.matcher(path);
		if (!matcher.matches()) return null;
		StringBuilder buf = new StringBuilder();
		for (int ix = 0; ix < matcher.groupCount(); ix++) {
			String name = args.get(ix);
			String value = matcher.group(ix + 1);
			if (buf.length() == 0) {
				buf.append(target);
				buf.append('?');
			}
			buf.append(name);
			buf.append('=');
			buf.append(BZText.url(value));
		}
		if (buf.length() == 0) {
			buf.append(target);
			buf.append('?');
		}
		return buf.toString();
	}

	public String getTarget() {
		return target;
	}
}
