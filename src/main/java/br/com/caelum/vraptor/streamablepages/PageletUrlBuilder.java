package br.com.caelum.vraptor.streamablepages;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;

@Dependent
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
		return String.format(template,url);
	}

	public void local(int port) {
		String contextPath = request.getContextPath();
		contextPath = contextPath.startsWith("/") ? contextPath : "/"+contextPath;
		this.template = "http://localhost:"+port+""+contextPath+"/%s";
	}

}
