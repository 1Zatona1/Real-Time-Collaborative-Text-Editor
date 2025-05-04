package Network;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

/**
 * Helper class for making HTTP requests to the REST API
 */
public class HTTPHelper
{
    private static final String BASE_URL = "http://localhost:8080";
    private static final String API_DOCUMENTS_ENDPOINT = "/api/documents";
    private static final Gson gson = new Gson();

    /**
     * Creates a new document
     * @return Map containing documentId, editorCode, and viewerCode
     * @throws IOException If an I/O error occurs
     */
    public static Map<String, String> createDocument() throws IOException {
        URL url = new URL(BASE_URL + API_DOCUMENTS_ENDPOINT);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setDoOutput(true);
        connection.setConnectTimeout(10000); // 10 seconds
        connection.setReadTimeout(10000); // 10 seconds

        // Send empty POST request
        try (OutputStream os = connection.getOutputStream())
        {
            os.write("".getBytes());
            os.flush();
        }

        int responseCode = connection.getResponseCode();
        if (responseCode != 201)
        {
            throw new IOException("Failed to create document: HTTP " + responseCode);
        }

        // Read the response
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream())))
        {
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }

            // Parse the JSON response
            try {
                Map<String, String> result = gson.fromJson(response.toString(), new TypeToken<HashMap<String, String>>(){}.getType());
                return result;
            } catch (Exception e) {
                throw new IOException("Failed to parse response: " + e.getMessage(), e);
            }
        } finally {
            connection.disconnect();
        }
    }

    /**
     * Gets the text content of a document
     * @param documentId The ID of the document to get
     * @return Map containing documentId and text
     * @throws IOException If an I/O error occurs
     */
    public static Map<String, String> getDocumentText(String documentId) throws IOException {
        if (documentId == null || documentId.isEmpty())
        {
            throw new IllegalArgumentException("Document ID cannot be null or empty");
        }

        URL url = new URL(BASE_URL + API_DOCUMENTS_ENDPOINT + "/" + documentId);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.setRequestProperty("Accept", "application/json");
        connection.setConnectTimeout(10000); // 10 seconds
        connection.setReadTimeout(10000); // 10 seconds

        int responseCode = connection.getResponseCode();
        if (responseCode == 404)
        {
            throw new IOException("Document not found: " + documentId);
        }
        else if (responseCode != 200)
        {
            throw new IOException("Failed to get document: HTTP " + responseCode);
        }

        // Read the response
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null)
            {
                response.append(line);
            }

            // Parse the JSON response
            try {
                Map<String, String> result = gson.fromJson(response.toString(), new TypeToken<HashMap<String, String>>(){}.getType());
                return result;
            } catch (Exception e) {
                throw new IOException("Failed to parse response: " + e.getMessage(), e);
            }
        } finally {
            connection.disconnect();
        }
    }

    /**
     * Validates a session code (editor or viewer code) and returns the corresponding document ID
     * @param code The session code to validate
     * @return Map containing documentId and isEditor flag
     * @throws IOException If an I/O error occurs
     */
    public static Map<String, String> validateSessionCode(String code) throws IOException {
        if (code == null || code.isEmpty())
        {
            throw new IllegalArgumentException("Session code cannot be null or empty");
        }

        URL url = new URL(BASE_URL + API_DOCUMENTS_ENDPOINT + "/validate/" + code);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.setRequestProperty("Accept", "application/json");
        connection.setConnectTimeout(10000); // 10 seconds
        connection.setReadTimeout(10000); // 10 seconds

        int responseCode = connection.getResponseCode();
        if (responseCode == 404)
        {
            throw new IOException("Invalid session code: " + code);
        }
        else if (responseCode != 200)
        {
            throw new IOException("Failed to validate session code: HTTP " + responseCode);
        }

        // Read the response
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null)
            {
                response.append(line);
            }

            // Parse the JSON response
            try {
                Map<String, String> result = gson.fromJson(response.toString(), new TypeToken<HashMap<String, String>>(){}.getType());
                return result;
            } catch (Exception e) {
                throw new IOException("Failed to parse response: " + e.getMessage(), e);
            }
        } finally {
            connection.disconnect();
        }
    }
}
