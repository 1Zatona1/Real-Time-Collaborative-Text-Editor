package com.example.apt_project;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class UsernameGenerator {
    private static final List<String> animals = Arrays.asList(
            "Fox", "Frog", "Camel", "Tiger", "Panda", "Eagle", "Koala", "Wolf", "Otter", "Dolphin"
    );

    private static final Random random = new Random();

    public static String generateAnonymousUsername() {
        String animal = animals.get(random.nextInt(animals.size()));
        return "Anonymous " + animal;
    }
}
