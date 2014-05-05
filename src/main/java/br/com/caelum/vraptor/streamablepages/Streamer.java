package br.com.caelum.vraptor.streamablepages;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
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
	private Set<com.ning.http.client.cookie.Cookie> ningCookies = new HashSet<>();
	private static final Logger logger = LoggerFactory.getLogger(Streamer.class);

	public Streamer() {
	}

	@Inject
	public Streamer(HttpServletResponse response, HttpServletRequest request) {
		super();
		this.response = CDIProxies.unproxifyIfPossible(response);
		CDIProxies.unproxifyIfPossible(request);
		saveCookiesToUseInRequests(request.getCookies());
	}

	private void saveCookiesToUseInRequests(Cookie[] cookies) {
		for (Cookie cookie : cookies) {
			//FIXME I did not find a way to get the expires information from servlet cookie. Maybe reading request header?
			ningCookies.add(new com.ning.http.client.cookie.Cookie(cookie.getName(), cookie.getValue(), cookie.getValue(),
					cookie.getDomain(), cookie.getPath(), cookie.getMaxAge(), cookie.getMaxAge(), cookie.getSecure(),
					cookie.isHttpOnly()));
		}
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
			//FIXME maybe there is another way to do this. Associate these cookies with the client instead to copy to every request.
			mergeCookies(startGet);
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

	private void mergeCookies(BoundRequestBuilder startGet) {
		for (com.ning.http.client.cookie.Cookie cookie : ningCookies) {
			startGet.addCookie(cookie);				
		}
	}

	public Streamer unOrder(String... urls) {
		for (String url : urls) {
			unorderedPagelets.add(asyncGet(url));
		}
		while (true) {
			boolean done = true;
			for (ListenableFuture<?> pagelet : unorderedPagelets) {
				done = pagelet.isDone() && done;
			}
			if (done)
				break;
			Thread.yield();
		}
		return this;
	}

}
