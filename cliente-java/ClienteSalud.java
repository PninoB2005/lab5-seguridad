
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class ClienteSalud {

    public static void main(String[] args) throws Exception {

        String url = "https://localhost:8443/api/v1/salud";

        HttpClient client = HttpClient.newHttpClient();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .build();

        HttpResponse<String> response = client.send(
                request,
                HttpResponse.BodyHandlers.ofString()
        );

        System.out.println("Estado HTTP: " + response.statusCode());
        System.out.println("Respuesta: " + response.body());
    }
}
