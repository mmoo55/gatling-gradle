package demo1;

import java.util.*;

import io.gatling.javaapi.core.*;
import io.gatling.javaapi.http.*;

import static io.gatling.javaapi.core.CoreDsl.*;
import static io.gatling.javaapi.http.HttpDsl.*;

public class RecordedSimulation1 extends Simulation {

  private HttpProtocolBuilder httpProtocol = http
    .baseUrl("http://demostore.gatling.io")
    .inferHtmlResources(AllowList(), DenyList(".*\\.js", ".*\\.css", ".*\\.gif", ".*\\.jpeg", ".*\\.jpg", ".*\\.ico", ".*\\.woff", ".*\\.woff2", ".*\\.(t|o)tf", ".*\\.png", ".*detectportal\\.firefox\\.com.*"))
    .acceptHeader("application/json")
    .acceptEncodingHeader("gzip, deflate")
    .acceptLanguageHeader("es-419,es;q=0.8")
    .userAgentHeader("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/108.0.0.0 Safari/537.36");
  
  private Map<CharSequence, String> headers_0 = Map.of("Sec-GPC", "1");
  
  private Map<CharSequence, String> headers_1 = Map.ofEntries(
    Map.entry("Content-Type", "application/json"),
    Map.entry("Origin", "http://demostore.gatling.io"),
    Map.entry("Sec-GPC", "1"),
    Map.entry("authorization", "Bearer eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJhZG1pbiIsImlhdCI6MTY3MDQ3MzgzOSwiZXhwIjoxNjcwNDc3NDM5fQ.AHI-LZxWVrWY368tHL4zwTEaH1gYImrlkRQEPX9jBjc")
  );
  
  private Map<CharSequence, String> headers_4 = Map.ofEntries(
    Map.entry("Accept", "image/avif,image/webp,image/apng,image/svg+xml,image/*,*/*;q=0.8"),
    Map.entry("Sec-GPC", "1")
  );


  private ScenarioBuilder scn = scenario("RecordedSimulation")
    .exec(
      http("request_0")
        .get("/api/category")
        .headers(headers_0)
    )
    .pause(17)
    .exec(
      http("request_1")
        .post("/api/category")
        .headers(headers_1)
        .body(RawFileBody("demo1/recordedsimulation/0001_request.json"))
    )
    .pause(17)
    .exec(
      http("request_2")
        .get("/api/category/0")
        .headers(headers_0)
        .check(status().is(404))
    )
    .pause(28)
    .exec(
      http("request_3")
        .get("/api/product")
        .headers(headers_0)
    )
    .pause(5)
    .exec(
      http("request_4")
        .get("/swagger-ui/favicon-32x32.png?v=3.0.4")
        .headers(headers_4)
    )
    .pause(3)
    .exec(
      http("request_5")
        .post("/api/product")
        .headers(headers_1)
        .body(RawFileBody("demo1/recordedsimulation/0005_request.json"))
    )
    .pause(5)
    .exec(
      http("request_6")
        .get("/swagger-ui/favicon-32x32.png?v=3.0.4")
        .headers(headers_4)
    )
    .pause(7)
    .exec(
      http("request_7")
        .get("/swagger-ui/favicon-32x32.png?v=3.0.4")
        .headers(headers_4)
    );

  {
	  setUp(scn.injectOpen(atOnceUsers(1))).protocols(httpProtocol);
  }
}
