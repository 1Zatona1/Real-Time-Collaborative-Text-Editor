package com.example.apt_project;


import org.springframework.web.client.RestOperations;
import org.springframework.web.client.RestTemplate;

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
}
