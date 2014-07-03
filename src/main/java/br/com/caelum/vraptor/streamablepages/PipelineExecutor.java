package br.com.caelum.vraptor.streamablepages;

import java.util.LinkedList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.annotation.PreDestroy;
import javax.enterprise.inject.Vetoed;
import javax.inject.Inject;

import br.com.caelum.vraptor.streamablepages.jpromises.JPromise;
import br.com.caelum.vraptor.Result;
import br.com.caelum.vraptor.streamablepages.writer.ClientWriter;

import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.ListenableFuture;

@Vetoed
public class PipelineExecutor {
	
	//just a guess...
	private static final ExecutorService WAITING_RESPONSE_POOL = Executors.newFixedThreadPool(5);
	private AsyncHttpClient client = new AsyncHttpClient();	
	private Result result;
	private LinkedList<JPromise<Integer>> pipeline = new LinkedList<>();
	private CountDownLatch requestsCount = new CountDownLatch(0);
	private PageletRequester pageletRequester;
	private PageletUrlBuilder pageletUrlBuilder;
	private ClientWriter clientWriter;	
	
	@Deprecated
	public PipelineExecutor() {}

	@Inject
	public PipelineExecutor(Result result, PageletRequester pageletRequester, PageletUrlBuilder pageletUrlBuilder,
			ClientWriter clientWriter) {
		super();
		this.result = result;
		this.pageletRequester = pageletRequester;
		this.pageletUrlBuilder = pageletUrlBuilder;
		this.clientWriter = clientWriter;
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
			pipeline.add(promise);
		}

		private void completeAndWrite() {
			try {
				clientWriter.write(listener.get());
				waitingRequestPromise.success(1);
				afterComplete.run();
			} catch (Exception exception) {
				throw new RuntimeException(exception);
			}
		}

		@Override
		public void run() {
			int index = pipeline.indexOf(waitingRequestPromise);
			if (index == 0) {
				completeAndWrite();
				return;
			}

			JPromise<Integer> blockingPromise = externalBlockingPromise == null ? pipeline.get(index - 1)
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
	
	public PipelineExecutor order(final String url) {
		ListenableFuture<String> waitingResponse = pageletRequester.get(pageletUrlBuilder.build(url));

		// podia ser qualquer coisa aqui
		final JPromise<Integer> myPromise = JPromise.apply();
		waitingResponse.addListener(new ResponseWriter(myPromise, waitingResponse), WAITING_RESPONSE_POOL);
		incRequestsCount();
		return this;

	}

	private void incRequestsCount() {
		this.requestsCount = new CountDownLatch((int) (requestsCount.getCount() + 1));
	}
	
	public PipelineExecutor unorder(String... urls) {
		JPromise<Integer> blockingPromise = JPromise.<Integer> apply();
		if (pipeline.isEmpty()) {
			blockingPromise.success(1);
		} else {
			blockingPromise = pipeline.getLast();
		}

		final CountDownLatch asyncRequestsBlockCounter = new CountDownLatch(urls.length);

		final JPromise<Integer> asyncRequestsBlocker = JPromise.<Integer> apply();

		for (String url : urls) {	
			incRequestsCount();
			ListenableFuture<String> executing = pageletRequester.get(pageletUrlBuilder.build(url));
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
			executing.addListener(writer, WAITING_RESPONSE_POOL);
		}

		pipeline.add(asyncRequestsBlocker);
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
	
	@PreDestroy
	public void release() {
		client.close();
	}	
}
