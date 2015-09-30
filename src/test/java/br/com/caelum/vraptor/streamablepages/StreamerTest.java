package br.com.caelum.vraptor.streamablepages;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.ExecutionException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.Test;

import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.ListenableFuture;
import com.ning.http.client.Response;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import io.undertow.Undertow;

public class StreamerTest {

	private static class OrderStreamerServlet extends HttpServlet {
		@Override
		protected void service(HttpServletRequest request,
				HttpServletResponse response) throws ServletException,
				IOException {
			Streamer streamer = new Streamer(new PageletRequester(request),
					request, response);
			streamer.order("http://localhost:8080/test/header")
					.order("http://localhost:8080/test/footer").await();

		}
	}

	private static class UnorderStreamerServlet extends HttpServlet {
		@Override
		protected void service(HttpServletRequest request,
				HttpServletResponse response) throws ServletException,
				IOException {
			Streamer streamer = new Streamer(new PageletRequester(request),
					request, response);
			streamer.order("http://localhost:8080/test/header")
					.unorder("http://localhost:8080/test/pagelet1",
							"http://localhost:8080/test/pagelet2")
					.order("http://localhost:8080/test/footer").await();

		}
	}

	@Test
	public void shouldRespectTheOrder() throws IOException,
			InterruptedException, ExecutionException {
		Undertow server = FakeServer.start(OrderStreamerServlet.class);
		String response = request();
		assertEquals("headerfooter", response);
		server.stop();

	}

	private String request() throws IOException, InterruptedException,
			ExecutionException {
		try (AsyncHttpClient client = new AsyncHttpClient()) {
			ListenableFuture<Response> execute = client.prepareGet(
					"http://localhost:8080/test/streamer").execute();
			String response = execute.get().getResponseBody();
			return response;
		}
	}

	@Test
	public void shouldRespectTheUnorder() throws IOException,
			InterruptedException, ExecutionException {
		Undertow server = FakeServer.start(UnorderStreamerServlet.class);
		String response = request();
		assertTrue(response.startsWith("header"));
		assertTrue(response.endsWith("footer"));
		assertTrue(response.contains("pagelet1")
				&& response.contains("pagelet2"));
		server.stop();

	}
}
