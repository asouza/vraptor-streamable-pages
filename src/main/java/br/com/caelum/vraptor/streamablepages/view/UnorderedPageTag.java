package br.com.caelum.vraptor.streamablepages.view;

import javax.enterprise.context.RequestScoped;
import javax.enterprise.inject.spi.CDI;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.BodyTagSupport;

public class UnorderedPageTag extends BodyTagSupport {
	
	private PagesToStream pages;
	private String url;

	@Override
	public int doStartTag() throws JspException {
		pages = CDI.current().select(PagesToStream.class).get();
		pages.add(url);
		return super.doStartTag();
	}
	
	public void setUrl(String url) {
		this.url = url;
	}
	
}
