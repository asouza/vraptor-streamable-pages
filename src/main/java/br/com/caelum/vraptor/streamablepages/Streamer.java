package br.com.caelum.vraptor.streamablepages;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import rx.Observable;
import rx.Observable.OnSubscribe;
import rx.Subscriber;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.subjects.ReplaySubject;
import br.com.caelum.vraptor.Result;
import br.com.caelum.vraptor.proxy.CDIProxies;

import com.ning.http.client.AsyncCompletionHandler;
import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.AsyncHttpClient.BoundRequestBuilder;
import com.ning.http.client.Response;

@RequestScoped
public class Streamer {

	private HttpServletResponse response;
	private AsyncHttpClient client = new AsyncHttpClient();
	private Set<com.ning.http.client.cookie.Cookie> ningCookies = new HashSet<>();
	private HttpServletRequest request;
	private static final Logger logger = LoggerFactory.getLogger(Streamer.class);
	private Result result;
	private Observable<String> endOfRequest = null;
	

	@Deprecated
	public Streamer() {
		
	}

	@Inject
	public Streamer(HttpServletResponse response, HttpServletRequest request, Result result) {
		super();
		this.result = result;
		this.response = CDIProxies.unproxifyIfPossible(response);
		this.request = CDIProxies.unproxifyIfPossible(request);

	}

	@PreDestroy
	public void release() {
		client.close();
	}

	@PostConstruct
	public void postConstruct() {
		saveCookiesToUseInRequests(request.getCookies());
	}

	private void saveCookiesToUseInRequests(Cookie[] cookies) {
		for (Cookie cookie : cookies) {
			// FIXME I did not find a way to get the expires information from
			// servlet cookie. Maybe reading request header?
			ningCookies.add(new com.ning.http.client.cookie.Cookie(cookie.getName(), cookie.getValue(), cookie
					.getValue(), cookie.getDomain(), cookie.getPath(), cookie.getMaxAge(), cookie.getMaxAge(), cookie
					.getSecure(), cookie.isHttpOnly()));
		}
		
	}

	public Streamer order(final String url) {
		final Observable<String> observable = asyncGet(url);
		if (endOfRequest != null) {
			endOfRequest = observable;
			write(endOfRequest);
		} else {
			endOfRequest.map(new Func1<String, String>() {
				@Override
				public String call(String result) {
					write(observable);
					return null;
				}

			});
		}
		return this;

	}

	private Observable<String> asyncGet(final String url) {
		final BoundRequestBuilder startGet = client.prepareGet(url);
		// FIXME maybe there is another way to do this. Associate these cookies
		// with the client instead to copy to every request.
		mergeCookies(startGet);
		return Observable.create(new OnSubscribe<String>() {

			@Override
			public void call(final Subscriber<? super String> subscriber) {
				try {
					startGet.execute(new AsyncCompletionHandler<String>() {

						@Override
						public String onCompleted(Response asyncResponse) throws Exception {
							logger.debug("Receiving response from url {}", url);
							String htmlContent = asyncResponse.getResponseBody();
							subscriber.onNext(htmlContent);
							return htmlContent;
						}
					});
				} catch (Exception e) {
					subscriber.onError(e);
				}
			}
		});
	}

	private void mergeCookies(BoundRequestBuilder startGet) {
		for (com.ning.http.client.cookie.Cookie cookie : ningCookies) {
			startGet.addCookie(cookie);
		}
	}

	public Streamer unOrder(String... urls) {
		final ReplaySubject<String> subject = ReplaySubject.create();
		for (String url : urls) {
			Observable<String> observable = asyncGet(url);
			observable.subscribe(new Action1<String>() {
				@Override
				public void call(String t1) {
					subject.onNext(t1);
				}
			});
		}
		this.endOfRequest = this.endOfRequest.map(new Func1<String, String>() {
			@Override
			public String call(String t1) {
				write(subject);
				return null;
			}
		});
		return this;
	}

	public void await() {
		// TODO wait for endOfRequest completion!!!
		try {
			Thread.sleep(3000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		result.nothing();
	}
	
	private void write(Observable<String> observable) {
		observable.subscribe(new Action1<String>() {
			@Override
			public void call(String t1) {
				try {
					response.getWriter().print(t1);
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			}
		});
	}

}
