package br.com.caelum.vraptor.controller;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

import javax.inject.Inject;
import javax.servlet.http.HttpSession;

import br.com.caelum.vraptor.Controller;
import br.com.caelum.vraptor.Path;
import br.com.caelum.vraptor.Result;
import br.com.caelum.vraptor.streamablepages.Streamer;

@Controller
public class IndexController {

	private final Result result;
	@Inject
	private Streamer streamer;
	@Inject
	private HttpSession session;

	/**
	 * @deprecated CDI eyes only
	 */
	public IndexController() {
		this(null);
	}

	@Inject
	public IndexController(Result result) {
		this.result = result;
	}
	
	public void createSession(){
		session.setAttribute("user","user name");
		result.nothing();
	}

	@Path("/")
	public void index() throws IOException, InterruptedException, ExecutionException {
		streamer.order("http://localhost:8080/vraptor-streamable-pages/index/start")
				.unOrder("http://localhost:8080/vraptor-streamable-pages/index/page1",
						"http://localhost:8080/vraptor-streamable-pages/index/page2")
				.order("http://localhost:8080/vraptor-streamable-pages/index/end");
		result.nothing();
	}

	public void start() {
		result.include("variable", "VRaptor!");
	}

	public void page1() throws InterruptedException {
		Thread.sleep(2000);
	}

	public void page2() {
		
	}

	public void end() {

	}

}