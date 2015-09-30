package br.com.caelum.vraptor.streamablepages;

import java.util.LinkedList;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.annotation.PreDestroy;
import javax.enterprise.inject.Vetoed;
import javax.inject.Inject;

import br.com.caelum.vraptor.streamablepages.writer.ClientWriter;

import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.ListenableFuture;

@Vetoed
public class PipelineExecutor {
	
	//just a guess...
	private static final ExecutorService WAITING_RESPONSE_POOL = Executors.newFixedThreadPool(5);
	private AsyncHttpClient client = new AsyncHttpClient();	
	private LinkedList<CompletableFuture<Integer>> pipeline = new LinkedList<>();
	private CountDownLatch requestsCount = new CountDownLatch(0);
	private PageletRequester pageletRequester;
	private PageletUrlBuilder pageletUrlBuilder;
	private ClientWriter clientWriter;	
	
	@Deprecated
	public PipelineExecutor() {}

	@Inject
	public PipelineExecutor(PageletRequester pageletRequester, PageletUrlBuilder pageletUrlBuilder,
			ClientWriter clientWriter) {
		super();
		this.pageletRequester = pageletRequester;
		this.pageletUrlBuilder = pageletUrlBuilder;
		this.clientWriter = clientWriter;
	}

	private class ResponseWriter implements Runnable {

		private CompletableFuture<Integer> waitingRequestPromise;
		private ListenableFuture<String> listener;
		private CompletableFuture<Integer> externalBlockingPromise;
		private Runnable afterComplete;

		public ResponseWriter(CompletableFuture<Integer> promise, ListenableFuture<String> listener) {
			this(null, promise, listener, new Runnable() {
				public void run() {
					requestsCount.countDown();
				}
			});
		}

		public ResponseWriter(CompletableFuture<Integer> externalBlockingPromise, CompletableFuture<Integer> promise,
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
				waitingRequestPromise.complete(1);
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

			CompletableFuture<Integer> blockingPromise = externalBlockingPromise == null ? pipeline.get(index - 1)
					: externalBlockingPromise;
			blockingPromise.thenAccept(result -> {
				completeAndWrite();
			});
		}
	}
	
	public PipelineExecutor order(final String url) {
		ListenableFuture<String> waitingResponse = pageletRequester.get(pageletUrlBuilder.build(url));

		// podia ser qualquer coisa aqui
		final CompletableFuture<Integer> myPromise = CompletableFuture.completedFuture(1);
		waitingResponse.addListener(new ResponseWriter(myPromise, waitingResponse), WAITING_RESPONSE_POOL);
		incRequestsCount();
		return this;

	}

	private void incRequestsCount() {
		this.requestsCount = new CountDownLatch((int) (requestsCount.getCount() + 1));
	}
	
	public PipelineExecutor unorder(String... urls) {
		CompletableFuture<Integer> blockingPromise = CompletableFuture.completedFuture(1);
		
		if (!pipeline.isEmpty()) {
			blockingPromise = pipeline.getLast();
		}

		final CountDownLatch asyncRequestsBlockCounter = new CountDownLatch(urls.length);

		final CompletableFuture<Integer> asyncRequestsBlocker = new CompletableFuture<Integer>();

		for (String url : urls) {	
			incRequestsCount();
			ListenableFuture<String> executing = pageletRequester.get(pageletUrlBuilder.build(url));
			CompletableFuture<Integer> promise = new CompletableFuture<Integer>();
			ResponseWriter writer = new ResponseWriter(blockingPromise, promise, executing, new Runnable() {

				@Override
				public void run() {
					asyncRequestsBlockCounter.countDown();
					requestsCount.countDown();
					if (asyncRequestsBlockCounter.getCount() == 0) {
						asyncRequestsBlocker.complete(1);
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
	}	
	
	@PreDestroy
	public void release() {
		client.close();
	}	
}
