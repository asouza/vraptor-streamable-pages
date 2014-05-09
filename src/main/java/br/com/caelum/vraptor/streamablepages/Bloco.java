package br.com.caelum.vraptor.streamablepages;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletResponse;

import rx.Observable;

public class Bloco {

	private List<Observable<String>> observables = new ArrayList<>();

	public Bloco(Observable<String> observable) {
		observables.add(observable);
	}

	public Bloco() {
	}

	public Bloco add(Observable<String> observable) {
		observables.add(observable);
		return this;
	}

	public void subscribe(final HttpServletResponse response) {
		for (Observable<String> observable : observables) {
			observable.subscribe(new ResponseWriter(response));		
		}
	}

}
