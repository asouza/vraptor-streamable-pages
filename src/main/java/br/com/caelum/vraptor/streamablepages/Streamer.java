package br.com.caelum.vraptor.streamablepages;

import java.io.IOException;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.jsp.JspWriter;

import br.com.caelum.vraptor.Result;
import br.com.caelum.vraptor.streamablepages.writer.ClientWriter;
import br.com.caelum.vraptor.streamablepages.writer.DefaultWriter;
import br.com.caelum.vraptor.streamablepages.writer.JspClientWriter;

@RequestScoped
@Named
public class Streamer {

	private final Result result;
	private final PageletRequester pageletRequester;
	private PageletUrlBuilder pageletUrlBuilder;
	private ClientWriter clientWriter;

	public Streamer() {
		this.result = null;
		this.pageletRequester = null;
	}

	@Inject
	public Streamer(Result result, PageletRequester pageletRequester, HttpServletRequest request,
			HttpServletResponse response) {
		super();
		this.result = result;
		this.pageletRequester = pageletRequester;
		this.pageletUrlBuilder = new PageletUrlBuilder(request);
		this.clientWriter = new DefaultWriter(response);
	}

	public Streamer local(int port) {
		pageletUrlBuilder.local(port);
		return this;
	}

	public Streamer jsp(JspWriter jspWriter) {
		try {
			jspWriter.flush();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		this.clientWriter = new JspClientWriter(jspWriter);
		return this;
	}

	public PipelineExecutor unorder(String... urls) {
		PipelineExecutor pipelineExecutor = new PipelineExecutor(result, pageletRequester, pageletUrlBuilder,
				clientWriter);
		return pipelineExecutor.unorder(urls);
	}

	public PipelineExecutor order(String url) {
		PipelineExecutor pipelineExecutor = new PipelineExecutor(result, pageletRequester, pageletUrlBuilder,
				clientWriter);
		return pipelineExecutor.order(url);
	}

}
