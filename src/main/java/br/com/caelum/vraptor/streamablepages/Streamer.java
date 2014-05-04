package br.com.caelum.vraptor.streamablepages;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import br.com.caelum.vraptor.proxy.CDIProxies;

import com.ning.http.client.AsyncCompletionHandler;
import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.AsyncHttpClient.BoundRequestBuilder;
import com.ning.http.client.ListenableFuture;
import com.ning.http.client.Response;

@RequestScoped
public class Streamer {

	private HttpServletResponse response;
	private AsyncHttpClient client = new AsyncHttpClient();
	private Set<ListenableFuture<?>> unorderedPagelets = new HashSet<>();
	private static final Logger logger = LoggerFactory.getLogger(Streamer.class);

	public Streamer() {
	}

	@Inject
	public Streamer(HttpServletResponse response) {
		super();
		this.response = CDIProxies.unproxifyIfPossible(response);
	}

	public Streamer order(final String url) {
		ListenableFuture<Void> firstRequest = asyncGet(url);
		try {
			firstRequest.get();
		} catch (InterruptedException | ExecutionException e) {
			throw new RuntimeException(e);
		}
		return this;

	}

	private ListenableFuture<Void> asyncGet(final String url) {
		try {
			BoundRequestBuilder startGet = client.prepareGet(url);

			ListenableFuture<Void> firstRequest = startGet.execute(new AsyncCompletionHandler<Void>() {

				@Override
				public Void onCompleted(Response asyncResponse) throws Exception {
					logger.debug("Serving response from url {}", url);
					response.getOutputStream().println(asyncResponse.getResponseBody());
					response.flushBuffer();
					return null;
				}
			});
			return firstRequest;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public Streamer unOrder(String... urls) {
		for (String url : urls) {			
			unorderedPagelets.add(asyncGet(url));
		}
		while(true){
			boolean done = true;
			for (ListenableFuture<?> pagelet : unorderedPagelets) {
				done = pagelet.isDone() && done;
			}
			if(done) break;
		}		
		return this;
	}

}
