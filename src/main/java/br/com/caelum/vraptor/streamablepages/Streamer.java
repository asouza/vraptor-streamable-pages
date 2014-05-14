package br.com.caelum.vraptor.streamablepages;

import java.util.LinkedList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;

import javax.annotation.PreDestroy;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.servlet.http.HttpServletResponse;

import br.com.caelum.blockingpromises.JPromise;
import br.com.caelum.vraptor.Result;
import br.com.caelum.vraptor.proxy.CDIProxies;

import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.ListenableFuture;

@RequestScoped
public class Streamer {

	private HttpServletResponse response;
	private AsyncHttpClient client = new AsyncHttpClient();	
	private Result result;
	private LinkedList<JPromise<Integer>> queue = new LinkedList<>();
	private CountDownLatch requestsCount = new CountDownLatch(0);
	private PageletRequester pageletRequester;

	@Deprecated
	public Streamer() {

	}

	@Inject
	public Streamer(HttpServletResponse response, Result result,PageletRequester pageletRequester) {
		super();
		this.result = result;
		this.pageletRequester = pageletRequester;
		this.response = CDIProxies.unproxifyIfPossible(response);

	}

	@PreDestroy
	public void release() {
		client.close();
	}

	private class ResponseWriter implements Runnable {

		private JPromise<Integer> waitingRequestPromise;
		private ListenableFuture<String> listener;
		private JPromise<Integer> externalBlockingPromise;
		private Runnable afterComplete;

		public ResponseWriter(JPromise<Integer> promise, ListenableFuture<String> listener) {
			this(null, promise, listener, new Runnable() {
				public void run() {
					requestsCount.countDown();
				}
			});
		}

		public ResponseWriter(JPromise<Integer> externalBlockingPromise, JPromise<Integer> promise,
				ListenableFuture<String> executing, Runnable afterComplete) {
			this.externalBlockingPromise = externalBlockingPromise;
			this.waitingRequestPromise = promise;
			this.listener = executing;
			this.afterComplete = afterComplete;
			queue.add(promise);
		}

		private void completeAndWrite() {
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
		ListenableFuture<String> waitingResponse = pageletRequester.get(url);

		// podia ser qualquer coisa aqui
		final JPromise<Integer> myPromise = JPromise.apply();
		waitingResponse.addListener(new ResponseWriter(myPromise, waitingResponse), Executors.newFixedThreadPool(2));
		incRequestsCount();
		return this;

	}

	private void incRequestsCount() {
		this.requestsCount = new CountDownLatch((int) (requestsCount.getCount() + 1));
	}

	public Streamer unOrder(String... urls) {
		JPromise<Integer> blockingPromise = JPromise.<Integer> apply();
		if (queue.isEmpty()) {
			blockingPromise.success(1);
		} else {
			blockingPromise = queue.getLast();
		}

		final CountDownLatch asyncRequestsBlockCounter = new CountDownLatch(urls.length);

		final JPromise<Integer> asyncRequestsBlocker = JPromise.<Integer> apply();

		for (String url : urls) {
			incRequestsCount();
			ListenableFuture<String> executing = pageletRequester.get(url);
			JPromise<Integer> promise = JPromise.<Integer> apply();
			ResponseWriter writer = new ResponseWriter(blockingPromise, promise, executing, new Runnable() {

				@Override
				public void run() {
					asyncRequestsBlockCounter.countDown();
					requestsCount.countDown();
					if (asyncRequestsBlockCounter.getCount() == 0) {
						asyncRequestsBlocker.success(1);
					}
				}
			});
			executing.addListener(writer, Executors.newFixedThreadPool(2));
		}

		queue.add(asyncRequestsBlocker);
		return this;
	}

	public void await() {
		try {
			requestsCount.await();
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
		result.nothing();
	}

}
