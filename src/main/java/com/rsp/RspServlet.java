package com.rsp;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class RspServlet extends HttpServlet {

	private static final int BUF_SIZE = 32768;
	private static final boolean DEBUG = true;
	private static final long serialVersionUID = 1L;

	private Map<String, String> mimes = new HashMap<>();	
	private Collection<Route> routes = new ArrayList<>();

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		String path = req.getPathInfo();
		if (path == null) path = req.getServletPath();
		setContentType(path, resp);
		String filePath = getServletContext().getRealPath(path);
		if (filePath != null) {
			int len = (int) new File(filePath).length();
			resp.setContentLength(len);
		}
		InputStream in = getServletContext().getResourceAsStream(path);
		if (in != null) {
			try {
				byte[] buf = new byte[BUF_SIZE];
				OutputStream out = resp.getOutputStream();
				try {
					for (;;) {
						int read = in.read(buf);
						if (read <= 0) break;
						out.write(buf, 0, read);
					}
				} finally {
					out.close();
				}
			} finally {
				in.close();
			}
			return;
		}
		resp.sendError(HttpServletResponse.SC_NOT_FOUND,
				"Resource not found: " + path);
	}

	@Override
	public void init() throws ServletException {
		initMimeTypes();
		String routeFile = getInitParameter("routeFile");
		if (routeFile == null) throw new ServletException("Missing routeFile");
		ServletContext context = getServletContext();
		try {
			BufferedReader in = new BufferedReader(new InputStreamReader(
					context.getResourceAsStream(routeFile)));
			try {
				parse(in);
			} finally {
				in.close();
			}
		} catch (Exception e) {
			throw new ServletException(e);
		}
	}
	
	private void initMimeTypes() {
		mimes.put("css", "text/css");
		mimes.put("gif", "image/gif");
		mimes.put("htm", "text/html");
		mimes.put("html", "text/html");
		mimes.put("jpg", "image/jpeg");
		mimes.put("js", "application/x-javascript");
		mimes.put("mp3", "audio/mpeg");
		mimes.put("mpeg", "video/mpeg");
		mimes.put("png", "image/png");
		mimes.put("swf", "application/x-shockwave-flash");
		mimes.put("tiff", "image/tiff");
		mimes.put("txt", "text/plain");
		mimes.put("wav", "audio/wav");
		mimes.put("xml", "application/xml");
		mimes.put("zip", "application/zip");
	}

	private void parse(BufferedReader in) throws ServletException {
		int count = 1;
		try {
			for (;;) {
				String line = in.readLine();
				if (line == null) break;
				Route route = Route.parse(line);
				if (route != null) {
					routes.add(route);
				}
				count++;
			}
		} catch (Exception e) {
			throw new ServletException("Problem on line " + count, e);
		}
	}

	@Override
	public void service(ServletRequest req, ServletResponse resp) throws ServletException, IOException {
		if (req instanceof HttpServletRequest) {
			HttpServletRequest hreq = (HttpServletRequest) req;
			String newPath = translate(hreq);
			if (newPath != null) {
				if (DEBUG) getServletContext().log("Rsp: " + hreq.getMethod() + ' ' + newPath);
			    RequestDispatcher rd = req.getRequestDispatcher(newPath);
			    if (rd != null) {
			    	rd.forward(req, resp);
			    	return;
			    }
			}
		}
		super.service(req, resp);
	}

	/*
	 *
.au	audio/basic
.avi	video/msvideo, video/avi, video/x-msvideo
.bmp	image/bmp
.bz2	application/x-bzip2
.dtd	application/xml-dtd
.doc	application/msword
.docx	application/vnd.openxmlformats-officedocument.wordprocessingml.document
.dotx	application/vnd.openxmlformats-officedocument.wordprocessingml.template
.es	application/ecmascript
.exe	application/octet-stream
.gz	application/x-gzip
.hqx	application/mac-binhex40
.jar	application/java-archive
.midi	audio/x-midi
.ogg	audio/vorbis, application/ogg
.pdf	application/pdf
.pl	application/x-perl
.potx	application/vnd.openxmlformats-officedocument.presentationml.template
.ppsx	application/vnd.openxmlformats-officedocument.presentationml.slideshow
.ppt	application/vnd.ms-powerpointtd>
.pptx	application/vnd.openxmlformats-officedocument.presentationml.presentation
.ps	application/postscript
.qt	video/quicktime
.ra	audio/x-pn-realaudio, audio/vnd.rn-realaudio
.ram	audio/x-pn-realaudio, audio/vnd.rn-realaudio
.rdf	application/rdf, application/rdf+xml
.rtf	application/rtf
.sgml	text/sgml
.sit	application/x-stuffit
.sldx	application/vnd.openxmlformats-officedocument.presentationml.slide
.svg	image/svg+xml
.tar.gz	application/x-tar
.tgz	application/x-tar
.tsv	text/tab-separated-values
.xlam	application/vnd.ms-excel.addin.macroEnabled.12
.xls	application/vnd.ms-excel
.xlsb	application/vnd.ms-excel.sheet.binary.macroEnabled.12
.xlsx	application/vnd.openxmlformats-officedocument.spreadsheetml.sheet
.xltx	application/vnd.openxmlformats-officedocument.spreadsheetml.template
	 */
	private void setContentType(String path, HttpServletResponse resp) {
		path = path.toLowerCase();
		int dot = path.lastIndexOf('.');
		if (dot >= 0) {
			String ext = path.substring(dot + 1);
			String mimeType = mimes.get(ext);
			if (mimeType != null) {
				resp.setContentType(mimeType);
				return;
			} else {
				if (DEBUG) getServletContext().log("Rsp: unknown file type: ." + ext);
				return;
			}
		}
		if (DEBUG) getServletContext().log("Rsp: no file extension: " + path);
	}

	private String translate(HttpServletRequest req) {
		for (Route route : routes) {
			String path = route.route(req);
			if (path != null) return path;
		}
		return null;
	}
}
