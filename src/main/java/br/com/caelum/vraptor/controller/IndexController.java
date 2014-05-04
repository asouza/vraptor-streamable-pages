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

@Controller
public class IndexController {

	private final Result result;
	@Inject
	private HttpServletResponse response;

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
		Set<ListenableFuture<?>> pagelets = new HashSet<>();
		AsyncHttpClient start = new AsyncHttpClient();
		final HttpServletResponse unproxifiedResponse = CDIProxies.unproxifyIfPossible(response);
		BoundRequestBuilder startGet = start.prepareGet(
				"http://localhost:8080/vraptor-blank-project/index/start");
		
		ListenableFuture<Void> waitingStart = startGet.execute(
				new AsyncCompletionHandler<Void>() {

					@Override
					public Void onCompleted(Response asyncResponse) throws Exception {
						System.out.println("Start ta pronto "+asyncResponse.getResponseBody());
						unproxifiedResponse.getOutputStream().println(asyncResponse.getResponseBody());
						unproxifiedResponse.flushBuffer();
						return null;
					}
				});
				
					
			waitingStart.get();
			System.out.println("pegou o start...");
			AsyncHttpClient page1Client = new AsyncHttpClient();
			ListenableFuture<Void> waitingPage1 = page1Client.prepareGet(
					"http://localhost:8080/vraptor-blank-project/index/page1").execute(
					new AsyncCompletionHandler<Void>() {

						@Override
						public Void onCompleted(Response asyncResponse) throws Exception {
							System.out.println("Page1 ta pronto");
							unproxifiedResponse.getOutputStream().println(asyncResponse.getResponseBody());
							unproxifiedResponse.flushBuffer();
							return null;
						}
					});
			pagelets.add(waitingPage1);
			AsyncHttpClient page2Client = new AsyncHttpClient();
			ListenableFuture<Void> waitingPage2 = page2Client.prepareGet(
					"http://localhost:8080/vraptor-blank-project/index/page2").execute(
					new AsyncCompletionHandler<Void>() {

						@Override
						public Void onCompleted(Response asyncResponse) throws Exception {
							System.out.println("Page2 ta pronto");							
							unproxifiedResponse.getOutputStream().println(asyncResponse.getResponseBody());
							unproxifiedResponse.flushBuffer();
							return null;
						}
					});
			
			pagelets.add(waitingPage2);
			
		while(true){
			boolean done = true;
			for (ListenableFuture<?> pagelet : pagelets) {
				done = pagelet.isDone() && done;
			}
			if(done) break;
		}
		
		AsyncHttpClient end = new AsyncHttpClient();
		ListenableFuture<Void> waitingEnd = end.prepareGet(
				"http://localhost:8080/vraptor-blank-project/index/end").execute(
				new AsyncCompletionHandler<Void>() {

					@Override
					public Void onCompleted(Response asyncResponse) throws Exception {
						System.out.println("end ta pronto");
						unproxifiedResponse.getOutputStream().println(asyncResponse.getResponseBody());
						unproxifiedResponse.flushBuffer();
						return null;
					}
				});
		
		waitingEnd.get();		
		System.out.println("mandando resposta para o navegador");
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