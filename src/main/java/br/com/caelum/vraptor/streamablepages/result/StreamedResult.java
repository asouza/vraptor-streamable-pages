package br.com.caelum.vraptor.streamablepages.result;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import br.com.caelum.vraptor.http.route.Router;
import br.com.caelum.vraptor.proxy.Proxifier;
import br.com.caelum.vraptor.streamablepages.PageletUrlBuilder;
import br.com.caelum.vraptor.streamablepages.Streamer;

public class StreamedResult {
	
	private final Proxifier proxifier;
	private final List<StreamedResultProxifier<?>> invocations;
	private final Streamer streamer;
	private final Router router;
	private PageletUrlBuilder pageletUrlBuilder;
	
	@Deprecated
	StreamedResult() {
		this(null, null, null, null);
	}

	@Inject
	public StreamedResult(Proxifier proxifier, Router router, Streamer streamer, PageletUrlBuilder pageletUrlBuilder) {
		this.proxifier = proxifier;
		this.router = router;
		this.streamer = streamer;
		this.pageletUrlBuilder = pageletUrlBuilder;
		this.invocations = new ArrayList<>();
	}

	public <T> T unorder(Class<T> controller) {
		StreamedResultProxifier<T> invocation = new StreamedResultProxifier<T>(router, controller);
		invocations.add(invocation);
		return proxifier.proxify(controller, invocation);
	}

	public void startStream() {
		ArrayList<String> paths = new ArrayList<String>();
		for (StreamedResultProxifier<?> invocation : invocations) {
			paths.add(pageletUrlBuilder.build(invocation.getUrl()));
		}
		streamer.unorder(paths.toArray(new String[paths.size()]));
	}

}
