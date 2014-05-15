package br.com.caelum.vraptor.streamablepages.writer;

import java.io.IOException;

import javax.enterprise.inject.Vetoed;
import javax.inject.Inject;
import javax.servlet.http.HttpServletResponse;

import br.com.caelum.vraptor.proxy.CDIProxies;

@Vetoed
public class DefaultWriter implements ClientWriter {

	private HttpServletResponse response;

	@Inject
	public DefaultWriter(HttpServletResponse response) {
		super();
		this.response = CDIProxies.unproxifyIfPossible(response);
	}

	@Override
	public void write(String html) {
		try {
			response.getOutputStream().print(html);
			response.flushBuffer();

		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

}
