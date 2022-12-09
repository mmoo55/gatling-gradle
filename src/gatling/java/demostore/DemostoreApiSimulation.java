package demostore;

import java.time.Duration;
import java.util.*;

import io.gatling.javaapi.core.*;
import io.gatling.javaapi.http.*;

import static io.gatling.javaapi.core.CoreDsl.*;
import static io.gatling.javaapi.http.HttpDsl.*;
/*
* Siempre debe extender de la clase Simulation que es propia de Gatling
*
* Dividir en 3 pasos:
* primer paso: definir las cabeceras (Protocolo, Url, etc)
* segundo paso: separar todo lo que se va a utilizar por endpoints
* tercer paso: Scenario, tener el orden en que se ejecutara todo y cuantas veces
* cuarto paso: Hacer la simulacion (Cantidad de usuarios, cuanto tiempo sera la simulacion, etc)
* */
public class DemostoreApiSimulation extends Simulation {

    /*PASO 1: Definir cabeceras*/
    /*
    * Configurar la URL;
    * Depende si es json o puede ser xml o txt, se cambia ese apartado
    * */
    private HttpProtocolBuilder httpProtocol = http
            .baseUrl("https://demostore.gatling.io")
            .header("Cache-Control", "no-cache")
            .contentTypeHeader("application/json")
            .acceptHeader("application/json");


    /*
    * La autorizacion no siempre existira, depende si es necesario autenticarse
    * */
    private static Map<CharSequence, String> authorizationHeaders = Map.ofEntries(
            Map.entry("authorization", "Bearer #{jwt}")
    );

    /*PASO 2: Separar codigo en endpoints*/
    private static ChainBuilder initSession = exec(session -> session.set("authenticated", false));

    private static class Authentication {
        /*
        * El token se guarda en jwt
        * Assert = Check
        * Inicio de sesion en authenticated
        *
        * Para utilizar en otras clases sacar el Authentication en otro Java Class
        * */
        private static ChainBuilder authenticate =
                doIf(session -> !session.getBoolean("authenticated")).then(
                        exec(http("Authenticate")
                                .post("/api/authenticate")
                                .body(StringBody("{\"username\": \"admin\",\"password\": \"admin\"}"))
                                .check(status().is(200))
                                .check(jmesPath("token").saveAs("jwt")))
                                .exec(session -> session.set("authenticated", true)));
    }

    private static class Categories {

        private static FeederBuilder.Batchable<String> categoriesFeeder =
                csv("data/categories.csv").random();

        private static ChainBuilder list =
                exec(http("List categories")
                        .get("/api/category")
                        .check(jmesPath("[? id == `6`].name").ofList().is(List.of("For Her"))));

        private static ChainBuilder update =
                feed(categoriesFeeder)
                        .exec(Authentication.authenticate)
                        .exec(http("Update category")
                                .put("/api/category/#{categoryId}")
                                .headers(authorizationHeaders)
                                .body(StringBody("{\"name\": \"#{categoryName}\"}"))
                                .check(jmesPath("name").isEL("#{categoryName}")));
    }

    private static class Products {

        private static FeederBuilder.Batchable<String> productsFeeder =
                csv("data/products.csv").circular();

        private static ChainBuilder list =
                exec(http("List products")
                        .get("/api/product?category=7")
                        .check(jmesPath("[? categoryId != '7']").ofList().is(Collections.emptyList()))
                        .check(jmesPath("[*].id").ofList().saveAs("allProductIds")));

        private static ChainBuilder get =
                exec(session -> {
                    List<Integer> allProductIds = session.getList("allProductIds");
                    return session.set("productId", allProductIds.get(new Random().nextInt(allProductIds.size())));
                })
                        .exec(http("Get product")
                                .get("/api/product/#{productId}")
                                .check(jmesPath("id").ofInt().isEL("#{productId}"))
                                .check(jmesPath("@").ofMap().saveAs("product")));


        private static ChainBuilder update =
                exec(Authentication.authenticate)
                        .exec( session -> {
                            Map<String, Object> product = session.getMap("product");
                            return session
                                    .set("productCategoryId", product.get("categoryId"))
                                    .set("productName", product.get("name"))
                                    .set("productDescription", product.get("description"))
                                    .set("productImage", product.get("image"))
                                    .set("productPrice", product.get("price"))
                                    .set("productId", product.get("id"));
                        })
                        .exec(http("Update product #{productName}")
                                .put("/api/product/#{productId}")
                                .headers(authorizationHeaders)
                                .body(ElFileBody("gatlingdemostoreapi/demostoreapisimulation/create-product.json"))
                                .check(jmesPath("price").isEL("#{productPrice}")));

        private static ChainBuilder create =
                exec(Authentication.authenticate)
                        .feed(productsFeeder)
                        .exec(http("Create product #{productName}")
                                .post("/api/product")
                                .headers(authorizationHeaders)
                                .body(ElFileBody("gatlingdemostoreapi/demostoreapisimulation/create-product.json")));
    }


    /*PASO 3: Tener el orden en que se ejecutara y cuantas veces*/
    private ScenarioBuilder scn = scenario("DemostoreApiSimulation")
            .exec(initSession)
            .exec(Categories.list)
            .pause(2)
            .exec(Products.list)
            .pause(2)
            .exec(Products.get)
            .pause(2)
            .exec(Products.update)
            .pause(2)
            .repeat(3).on(exec(Products.create))
            .pause(2)
            .exec(Categories.update);

    /*PASO 4: Hacer la simulacion (cantidad de usuarios, cuanto tiempo sera la simulacion, etc)*/
    // Open Model: Se usa para "Performance Testing"
    //              va desde 1, 2 a n usuarios
    //              se ve que los tiempos de respuesta no varien
    //              por lo general todos los sistemas son modelos abiertos

    //              por lo general en un "Performance Testing" solo se tendra la linea: rampUsers(10).during(Duration.ofSeconds(20))
    //              para saber hasta donde nuestro sistema aguanta, que seria el maximo, este sera el baseline, ya que la segunda vez se ejecuta con el mismo numero de usuarios y no deberia variar
//    {
//        setUp(
//                scn.injectOpen(
//                        atOnceUsers(3),
//                        nothingFor(Duration.ofSeconds(5)),
//                        rampUsers(10).during(Duration.ofSeconds(20)),
//                        nothingFor(Duration.ofSeconds(10)),
//                        constantUsersPerSec(1).during(Duration.ofSeconds(20))))
//                .protocols(httpProtocol);
//    }

    // Closed Model: Algo que ya esta definido
    //              para sistemas que no nos permiten hacer mas de N(20) requests por segundo, definimos solo la cantidad concurrente que decidimos
    //              sirve tambien para "Load Testing", en vez de enviar secuencialmente 1, 2, etc; enviamos N(50) cantidad de golpe

    //              para "Load Testing" solo necesitamos: constantConcurrentUsers(5).during(Duration.ofSeconds(20))))
    //              lo que encontramos con el "Performance Test", el maximo encontrado, le pasamos para el "Load Test"

    // Para "Stress Testing" combinar entre load y performance con las lineas:
    //                      constantConcurrentUsers(5).during(Duration.ofSeconds(20))))
    //                      nothingFor(Duration.ofSeconds(10))
    //                      constantConcurrentUsers(5).during(Duration.ofSeconds(20))))
    // Para "Stress Testing", se configura el tiempo en la linea: constantConcurrentUsers(5).during(Duration.ofSeconds(20))))
    // Segun el valor encontrado en el "Performance Test", se lo para en un tiempo con: nothingFor(Duration.ofSeconds(10))
    // Despues se le vuelve a mandar con: constantConcurrentUsers(5).during(Duration.ofSeconds(20))))
    // Para Estresar al sistema y ver como reacciona
    // A la par que se ejecuta el "Stress Testing" hay que monitorear los recursos del sistema que estamos testeando (Memoria, CPU, Disco, Network)
    {
        setUp(
                scn.injectClosed(
                        rampConcurrentUsers(1).to(5).during(Duration.ofSeconds(20)),
                        constantConcurrentUsers(5).during(Duration.ofSeconds(20))))
                .protocols(httpProtocol);
    }
}
