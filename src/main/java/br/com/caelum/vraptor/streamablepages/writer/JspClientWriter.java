package br.com.caelum.vraptor.streamablepages.writer;

import java.io.IOException;

import javax.enterprise.inject.Vetoed;
import javax.servlet.jsp.JspWriter;

@Vetoed
public class JspClientWriter implements ClientWriter {

	private JspWriter jspWriter;

	public JspClientWriter(JspWriter jspWriter) {
		super();
		this.jspWriter = jspWriter;
	}

	@Override
	public void write(String html) {
		try {
			jspWriter.print(html);
			jspWriter.flush();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		
	}

}
