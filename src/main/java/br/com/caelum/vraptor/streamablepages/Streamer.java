package br.com.caelum.vraptor.streamablepages;

import java.io.IOException;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import br.com.caelum.blockingpromises.JPromise;
import br.com.caelum.vraptor.Result;
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
	private Set<com.ning.http.client.cookie.Cookie> ningCookies = new HashSet<>();
	private HttpServletRequest request;
	private static final Logger logger = LoggerFactory.getLogger(Streamer.class);
	private Result result;
	private LinkedList<JPromise<Integer>> queue = new LinkedList<>();

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

	private class ResponseWriter implements Runnable {

		private JPromise<Integer> waitingRequestPromise;
		private ListenableFuture<String> listener;
		private JPromise<Integer> externalBlockingPromise;
		private Runnable afterComplete;

		public ResponseWriter(JPromise<Integer> promise, ListenableFuture<String> listener) {
			this(null, promise, listener,new Runnable() {public void run() {}});
		}

		public ResponseWriter(JPromise<Integer> externalBlockingPromise, JPromise<Integer> promise,
				ListenableFuture<String> executing,Runnable afterComplete) {
			this.externalBlockingPromise = externalBlockingPromise;
			this.waitingRequestPromise = promise;
			this.listener = executing;
			this.afterComplete = afterComplete;
			queue.add(promise);
		}

		private void completeAndWrite() {
			System.out.println("escrevendo...");
			try {
				response.getOutputStream().println(listener.get());
				response.flushBuffer();
				waitingRequestPromise.success(1);
				afterComplete.run();
			} catch (Exception exception) {
				throw new RuntimeException(exception);
			}
		}

		@Override
		public void run() {
			int index = queue.indexOf(waitingRequestPromise);
			if (index == 0) {
				completeAndWrite();
				return;
			}

			JPromise<Integer> blockingPromise = externalBlockingPromise == null ? queue.get(index - 1)
					: externalBlockingPromise;

			// se tem um outra na fila, espera
			blockingPromise.onSuccess(new Runnable() {
				@Override
				public void run() {
					completeAndWrite();
				}
			});
		}
	}

	public Streamer order(final String url) {
		ListenableFuture<String> waitingResponse = asyncGet(url);

		// podia ser qualquer coisa aqui
		final JPromise<Integer> myPromise = JPromise.apply();
		waitingResponse.addListener(new ResponseWriter(myPromise, waitingResponse), Executors.newFixedThreadPool(2));
		return this;

	}

	private ListenableFuture<String> asyncGet(final String url) {
		final BoundRequestBuilder startGet = client.prepareGet(url);
		// FIXME maybe there is another way to do this. Associate these cookies
		// with the client instead to copy to every request.
		mergeCookies(startGet);
		try {
			ListenableFuture<String> executing = startGet.execute(new AsyncCompletionHandler<String>() {

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

	public Streamer unOrder(String... urls) {
		JPromise<Integer> blockingPromise = JPromise.<Integer> apply();
		if (queue.isEmpty()) {
			blockingPromise.success(1);
		} else {
			blockingPromise = queue.getLast();
		}
		
		final CountDownLatch countDownLatch = new CountDownLatch(urls.length);
		
		final JPromise<Integer> asyncRequestsBlocker = JPromise.<Integer>apply();
		
		for (String url : urls) {
			ListenableFuture<String> executing = asyncGet(url);
			JPromise<Integer> promise = JPromise.<Integer> apply();
			ResponseWriter writer = new ResponseWriter(blockingPromise, promise, executing,new Runnable() {
				
				@Override
				public void run() {
					countDownLatch.countDown();
					if(countDownLatch.getCount() == 0){
						asyncRequestsBlocker.success(1);
					}					
				}
			});
			executing.addListener(writer,
					Executors.newFixedThreadPool(2));
		}
		
		queue.add(asyncRequestsBlocker);
		return this;
	}

	public void await() {
		try {
			Thread.sleep(6000);
			System.out.println(queue.size());
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		result.nothing();
	}

}
