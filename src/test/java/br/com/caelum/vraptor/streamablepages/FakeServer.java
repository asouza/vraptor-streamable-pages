package br.com.caelum.vraptor.streamablepages;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import io.undertow.Handlers;
import io.undertow.Undertow;
import io.undertow.server.handlers.PathHandler;
import io.undertow.servlet.Servlets;
import io.undertow.servlet.api.DeploymentInfo;
import io.undertow.servlet.api.DeploymentManager;

public class FakeServer {

	public static Undertow start(Class<? extends HttpServlet> streamerServlet) {
		DeploymentInfo servletBuilder = Servlets.deployment()
		        .setClassLoader(FakeServer.class.getClassLoader())
		        .setContextPath("/test")
		        .setDeploymentName("test.war")
		        .addServlets(
		                Servlets.servlet("headerServlet", HeaderServlet.class)
		                        .addMapping("/header"),
		                Servlets.servlet("pagelet1Servlet", Pagelet1Servlet.class)
		                        .addMapping("/pagelet1"),
				        Servlets.servlet("pagelet2Servlet", Pagelet2Servlet.class)
		                        .addMapping("/pagelet2"),
						Servlets.servlet("footerServlet", FooterServlet.class)
						.addMapping("/footer"),
						Servlets.servlet(streamerServlet.getSimpleName(),streamerServlet)
						.addMapping("/streamer"));

		DeploymentManager manager = Servlets.defaultContainer().addDeployment(servletBuilder);
		manager.deploy();
		PathHandler path = null;
		try {
			path = Handlers.path(Handlers.redirect("/test"))
			        .addPrefixPath("/test", manager.start());
		} catch (ServletException e) {
			throw new RuntimeException(e);
		}

		Undertow server = Undertow.builder()
		        .addHttpListener(8080, "localhost")
		        .setHandler(path)
		        .build();
		server.start();		
		return server;
	}
	
	private static class HeaderServlet extends HttpServlet {
		@Override
		protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
			resp.getWriter().print("header");
		}
	}
	
	private static class Pagelet1Servlet extends HttpServlet {
		@Override
		protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
			resp.getWriter().print("pagelet1");
		}
	}
	
	private static class Pagelet2Servlet extends HttpServlet {
		@Override
		protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
			resp.getWriter().print("pagelet2");
		}
	}
	
	private static class FooterServlet extends HttpServlet {
		@Override
		protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
			resp.getWriter().print("footer");
		}
	}

	
}
