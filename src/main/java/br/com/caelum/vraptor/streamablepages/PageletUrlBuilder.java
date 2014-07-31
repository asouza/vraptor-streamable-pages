package br.com.caelum.vraptor.streamablepages;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;

@RequestScoped
public class PageletUrlBuilder {

	private HttpServletRequest request;
	private String template = "%s";

	@Deprecated
	public PageletUrlBuilder() {
	}

	@Inject
	public PageletUrlBuilder(HttpServletRequest request) {
		super();
		this.request = request;
	}

	public String build(String url) {
		return String.format(template,url.startsWith("/") ? url.replaceFirst("/","") : url);
	}

	public void local(int port) {
		String contextPath = request.getContextPath();
		this.template = "http://localhost:"+port+""+contextPath+"/%s";
	}

}
