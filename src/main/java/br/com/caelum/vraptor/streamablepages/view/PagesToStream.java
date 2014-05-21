package br.com.caelum.vraptor.streamablepages.view;

import java.util.ArrayList;
import java.util.List;

import javax.enterprise.context.RequestScoped;

@RequestScoped
public class PagesToStream {

	private List<String> urls = new ArrayList<>();
	
	public void add(String url) {
		urls.add(url);
	}
	
	public List<String> getUrls() {
		return urls;
	}

	public void clear() {
		urls.clear();
	}
	
}
