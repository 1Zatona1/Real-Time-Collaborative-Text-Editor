package com.example.apt_project;


import org.springframework.web.client.RestOperations;
import org.springframework.web.client.RestTemplate;

public class HttpHelper {
    private static final RestTemplate restTemplate= new RestTemplate();
    private static final String baseUrl = "http://localhost:8080/";

    public static Session createDocument() {

        String url = baseUrl + "api/documents";
        Session mySession = new Session("", "", "");
        try {
            mySession = restTemplate.postForObject(url,"" ,Session.class);
            System.out.println(mySession);
        } catch (Exception e) {
            System.out.println("Failed to create session with error " + e.getMessage());
            System.out.println("Exiting..");
            System.exit(0);
        }
        return mySession;
    }
}
