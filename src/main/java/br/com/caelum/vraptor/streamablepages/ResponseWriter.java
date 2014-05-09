package br.com.caelum.vraptor.streamablepages;

import java.io.IOException;

import javax.servlet.http.HttpServletResponse;

import org.jboss.netty.util.internal.StringUtil;

import br.com.caelum.vraptor.util.StringUtils;
import rx.Observer;

public class ResponseWriter implements Observer<String> {

	private HttpServletResponse response;

	public ResponseWriter(HttpServletResponse response) {
		this.response = response;
	}

	@Override
	public void onCompleted() {
		// TODO Auto-generated method stub

	}

	@Override
	public void onError(Throwable e) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onNext(String html) {
		if (html != null && !html.trim().isEmpty()) {
			try {
				response.getWriter().print(html);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
	}
}
