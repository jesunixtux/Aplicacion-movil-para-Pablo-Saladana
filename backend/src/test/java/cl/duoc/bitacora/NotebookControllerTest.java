package cl.duoc.bitacora;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = "spring.datasource.url=jdbc:h2:mem:notebooktest;DB_CLOSE_DELAY=-1")
class NotebookControllerTest {
    @LocalServerPort int port;
    private final HttpClient client = HttpClient.newHttpClient();

    @Test
    void createListUpdateDeleteAndValidate() throws Exception {
        URI collection = URI.create("http://127.0.0.1:" + port + "/api/notebooks");
        String input = "{\"title\":\"Octubre\",\"purpose\":\"Aprender\",\"instructions\":\"Escribir\",\"teacherEmail\":\"docente@example.com\",\"teacherName\":\"Docente\"}";
        HttpResponse<String> created = send(collection, "POST", input);
        assertEquals(201, created.statusCode());
        assertTrue(created.body().contains("\"title\":\"Octubre\""));
        URI item = URI.create(collection + "/1");
        assertTrue(send(collection, "GET", null).body().contains("Octubre"));
        assertEquals(400, send(collection, "POST", "{}").statusCode());
        assertEquals(200, send(item, "PUT", input.replace("Octubre", "Noviembre")).statusCode());
        assertTrue(send(item, "GET", null).body().contains("Noviembre"));
        assertEquals(204, send(item, "DELETE", null).statusCode());
        assertEquals(404, send(item, "GET", null).statusCode());
    }

    private HttpResponse<String> send(URI uri, String method, String body) throws Exception {
        HttpRequest.Builder request = HttpRequest.newBuilder(uri);
        if (body == null) request.method(method, HttpRequest.BodyPublishers.noBody());
        else request.method(method, HttpRequest.BodyPublishers.ofString(body)).header("Content-Type", "application/json");
        return client.send(request.build(), HttpResponse.BodyHandlers.ofString());
    }
}
