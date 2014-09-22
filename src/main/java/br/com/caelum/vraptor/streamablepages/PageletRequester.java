package br.com.caelum.vraptor.streamablepages;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import br.com.caelum.vraptor.proxy.CDIProxies;

import com.ning.http.client.AsyncCompletionHandler;
import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.ListenableFuture;
import com.ning.http.client.Response;
import com.ning.http.client.AsyncHttpClient.BoundRequestBuilder;

@RequestScoped
public class PageletRequester {

	private static AsyncHttpClient client = new AsyncHttpClient();
	private static final Logger logger = LoggerFactory.getLogger(PageletRequester.class);
	private HttpServletRequest request;
	private Set<com.ning.http.client.cookie.Cookie> ningCookies = new HashSet<>();
	
	@Deprecated
	public PageletRequester() {}

	@Inject
	public PageletRequester(HttpServletRequest request) {
		super();
		this.request = CDIProxies.unproxifyIfPossible(request);
	}

	@PostConstruct
	public void postConstruct() {
		if(request.getCookies() == null) return;
		for (Cookie cookie : request.getCookies()) {
			// FIXME I did not find a way to get the expires information from
			// servlet cookie. Maybe reading request header?
			ningCookies.add(new com.ning.http.client.cookie.Cookie(cookie.getName(), cookie.getValue(), cookie
					.getValue(), cookie.getDomain(), cookie.getPath(), cookie.getMaxAge(), cookie.getMaxAge(), cookie
					.getSecure(), cookie.isHttpOnly()));
		}

	}

	public ListenableFuture<String> get(final String url) {
		final BoundRequestBuilder getter = client.prepareGet(url);
		mergeCookies(getter);
		try {
			ListenableFuture<String> executing = getter.execute(new AsyncCompletionHandler<String>() {

				@Override
				public String onCompleted(Response asyncResponse) throws Exception {
					logger.debug("Receiving response from url {}", url);
					String htmlContent = asyncResponse.getResponseBody();
					return htmlContent;
				}
			});
			return executing;
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private void mergeCookies(BoundRequestBuilder startGet) {
		for (com.ning.http.client.cookie.Cookie cookie : ningCookies) {
			startGet.addCookie(cookie);
		}
	}
}
