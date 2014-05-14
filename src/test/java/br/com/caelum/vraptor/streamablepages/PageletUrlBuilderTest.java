package br.com.caelum.vraptor.streamablepages;

import javax.servlet.http.HttpServletRequest;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import static org.junit.Assert.assertEquals;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class PageletUrlBuilderTest {

	private HttpServletRequest request;
	
	@Before
	public void setup() {
		this.request = mock(HttpServletRequest.class);
		when(request.getContextPath()).thenReturn("context");
	}

	@Test
	public void shouldUseTheSameParameterUrl(){
		PageletUrlBuilder builder = new PageletUrlBuilder(request);
		String url = builder.build("http://localhost:8080/context/controller/method");
		assertEquals("http://localhost:8080/context/controller/method", url);
	}
	
	@Test
	public void shouldBuildLocalhostUrl(){
		PageletUrlBuilder builder = new PageletUrlBuilder(request);
		builder.local(8080);
		String url = builder.build("controller/method");
		assertEquals("http://localhost:8080/context/controller/method", url);
	}
}
