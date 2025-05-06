package com.example.apt_project;


import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestOperations;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;

import java.util.List;

public class HttpHelper {
    private static final RestTemplate restTemplate= new RestTemplate();
    private static final String baseUrl = "http://localhost:8080/";

    public static String createDocument() {

        String url = baseUrl + "api/documents";
        String mySessionDetails = "";
        try {
            mySessionDetails = restTemplate.postForObject(url,"" ,String.class);
            System.out.println(mySessionDetails);
        } catch (Exception e) {
            System.out.println("Failed to create session with error " + e.getMessage());
            System.out.println("Exiting..");
            System.exit(0);
        }
        return mySessionDetails;
    }

    public static String getDocumentIdByCode(String code) {
        String url = baseUrl + "api/documents/" + code + "/getid";
        try {
            ResponseEntity<String> sessionId = restTemplate.getForEntity(url, String.class, code);
            return sessionId.getBody();
        }
        catch (Exception e) {
            System.out.println("Failed to get session id with error " + e.getMessage());
        }
        return null;
    }

    public static List<String> getListOfOperation(String code) {
        String url = baseUrl + "api/documents/" + code + "/state";
        try {
            ResponseEntity<List<String>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<List<String>>() {}
            );
            return response.getBody();
        } catch (Exception e) {
            System.out.println("Failed to get session id with error " + e.getMessage());
            return null;
        }
    }


}
