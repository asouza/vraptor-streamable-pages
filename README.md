# VRaptor streamable pages

This projects aims to enable async rendering of htmls using VRaptor. The
inspiration came from this Linkedin
talk(http://engineering.linkedin.com/play/composable-and-streamable-play-apps).

#Example    

Take a look on this example:

```java
@Controller
public class IndexController {

	private final Result result;
	@Inject
	private Streamer streamer;

	/**
	 * @deprecated CDI eyes only
	 */
	public IndexController() {
		this(null);
	}

	@Inject
	public IndexController(Result result) {
		this.result = result;
	}

	@Path("/")
	public void index() throws IOException, InterruptedException, ExecutionException {
		streamer.order("http://localhost:8080/vraptor-blank-project/index/start")
				.unOrder("http://localhost:8080/vraptor-blank-project/header",
						"http://localhost:8080/vraptor-blank-project/body",
						"http://localhost:8080/vraptor-blank-project/footer")
				.order("http://localhost:8080/vraptor-blank-project/end");
		result.nothing();
	}

    @Path("/start")
	public void start() {
		result.include("variable", "VRaptor!");
	}

    @Path("/header")
	public void header() throws InterruptedException {
		Thread.sleep(2000);
	}

    @Path("/body")
	public void body() {
	}

    @Path("/footer")
	public void footer() {
	}

    @Path("/path")
	public void end() {

	}

}
```

# Example using jsp

You can also use our taglib to achieve the same result. Suppose that your `/WEB-INF/jsp/index/inidex.jsp` is like this:


```jsp
<%@taglib prefix="streamer" uri="http://vraptor.org/jsp/taglib/streamer" %>
<html>
    <head>
        <link src="css/styles.css" rel="stylesheet">
    </head>
    <body>
        <div id="header-pagelet"></div>
        <div id="body-pagelet"></div>
        <div id="footer-pagelet"></div>

        <streamer:stream>
            <streamer:page url="header"/>
            <streamer:page url="body"/>
            <streamer:page url="footer"/>
        </streamer:stream>
    </body>
</html>
```










