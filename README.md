# VRaptor streamable pages

This projects aims to enable async rendering of htmls using VRaptor. The
inspiration came from this Linkedin
talk(http://engineering.linkedin.com/play/composable-and-streamable-play-apps).

#Example

Take a look on this example:

```
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
				.unOrder("http://localhost:8080/vraptor-blank-project/index/page1",
						"http://localhost:8080/vraptor-blank-project/index/page2")
				.order("http://localhost:8080/vraptor-blank-project/index/end");
		result.nothing();
	}

	public void start() {
		result.include("variable", "VRaptor!");
	}

	public void page1() throws InterruptedException {
		Thread.sleep(2000);
	}

	public void page2() {

	}

	public void end() {

	}

}
```