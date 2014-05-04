package br.com.caelum.vraptor.controller;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import javax.inject.Inject;
import javax.servlet.http.HttpServletResponse;

import org.jboss.weld.bean.builtin.ee.HttpServletRequestBean;

import com.ning.http.client.AsyncCompletionHandler;
import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.AsyncHttpClient.BoundRequestBuilder;
import com.ning.http.client.ListenableFuture;
import com.ning.http.client.Response;

import br.com.caelum.vraptor.Controller;
import br.com.caelum.vraptor.Path;
import br.com.caelum.vraptor.Result;
import br.com.caelum.vraptor.proxy.CDIProxies;
import br.com.caelum.vraptor.streamablepages.Streamer;

@Controller
public class IndexController {

	private final Result result;
	@Inject
	private Streamer streamer;

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

	@Path("/")
	public void index() throws IOException, InterruptedException, ExecutionException {
		streamer.order("http://localhost:8080/vraptor-blank-project/index/start")
				.unOrder("http://localhost:8080/vraptor-blank-project/index/page1",
						"http://localhost:8080/vraptor-blank-project/index/page2")
				.order("http://localhost:8080/vraptor-blank-project/index/end");
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