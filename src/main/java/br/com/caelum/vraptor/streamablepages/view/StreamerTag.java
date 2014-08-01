package br.com.caelum.vraptor.streamablepages.view;

import java.util.List;

import br.com.caelum.vraptor.streamablepages.Streamer;

import javax.enterprise.inject.spi.CDI;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.BodyTagSupport;

public class StreamerTag extends BodyTagSupport {
	
	private Streamer streamer;
	private PagesToStream pages;

	@Override
	public int doStartTag() throws JspException {
		streamer = CDI.current().select(Streamer.class).get();
		pages = CDI.current().select(PagesToStream.class).get();
		return super.doStartTag();
	}
	
	@Override
	public int doEndTag() throws JspException {
		List<String> urls = pages.getUrls();
		streamer.local(getPort()).jsp(pageContext).unorder(urls.toArray(new String[]{})).await();
		pages.clear();
		return super.doEndTag();
	}

	private int getPort() {
		String port = System.getenv("PORT");
		if (port != null) {
			return parse(port);
		}
		return 8080;
	}

	private int parse(String port) {
		try {
			return Integer.parseInt(port);
		} catch (NumberFormatException e) {
			return 8080;
		}
	}

}
