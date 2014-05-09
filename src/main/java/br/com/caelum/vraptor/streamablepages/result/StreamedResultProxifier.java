package br.com.caelum.vraptor.streamablepages.result;

import java.lang.reflect.Method;

import br.com.caelum.vraptor.http.route.Router;
import br.com.caelum.vraptor.proxy.MethodInvocation;
import br.com.caelum.vraptor.proxy.SuperMethod;
import br.com.caelum.vraptor.view.PathResolver;

public class StreamedResultProxifier<T> implements MethodInvocation<T> {

	private Class<T> controllerClass;
	private String url;
	private Router router;
	
	public StreamedResultProxifier(Router router,
			Class<T> controllerClass) {
		this.router = router;
		this.controllerClass = controllerClass;
	}

	@Override
	public Object intercept(T proxy, Method method, Object[] args,
			SuperMethod superMethod) {
		this.url = router.urlFor(controllerClass, method, args);
		return null;
	}
	
	public String getUrl() {
		return url;
	}

}
