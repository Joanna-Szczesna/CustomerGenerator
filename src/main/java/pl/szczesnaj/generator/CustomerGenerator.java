/*
 * Copyright (c) 2023 Joanna Szczesna
 * All rights reserved
 */

package pl.szczesnaj.generator;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class CustomerGenerator {
    private final Random random;
    private List<String> names;
    private List<String> surnames;
    private final List<String> allowedContactsMethods;
    private final HttpClient httpClient;

    public CustomerGenerator() {
        this.allowedContactsMethods = Arrays.asList(
                "emailAddress",
                "residenceAddress",
                "registeredAddress",
                "privatePhoneNumber",
                "businessPhoneNumber");
        this.httpClient = HttpClient.newHttpClient();
        this.names = Arrays.asList(
                "Ala",
                "Tola",
                "Ewa",
                "Natalka",
                "Jan",
                "Franek",
                "Bartek",
                "Jerzy"
        );
        this.surnames = Arrays.asList(
                "Kot",
                "Mot",
                "Nowak",
                "Kowal",
                "Szklany",
                "Rad"
        );
        random = new Random();
    }

    public void generate(int customersNumber) {
        for (int i = 0; i < customersNumber; i++) {

            String name = generateName();
            String surname = generateSurname();
            String pesel = generatePeselNumber(i);
            String customerPayload = makeCustomerPayload(pesel, name, surname);

            String location = addCustomer(customerPayload);
            System.out.printf("Added customer: %s%n", location);

            int quantity = getRandomNumber(2, 5);
            String contactPayload = addContacts(quantity);
            int statusCode = addContactsMethods(location, contactPayload);
            System.out.printf("Added methods. Status Code: %s%n User: %s%n", statusCode, location);
        }
    }

    private String addContacts(int quantity) {
        Set<Integer> numbers = new HashSet();
        while (numbers.size() < quantity) {
            numbers.add(getRandomNumber(0, allowedContactsMethods.size()));
        }
        Map<String, String> methods = generateMethods(numbers);

        return makeContactPayload(methods);
    }

    private Map<String, String> generateMethods(Set<Integer> numbers) {
        Map<String, String> methods = new HashMap<>();
        List<String> methodsKey = numbers.stream().
                map(allowedContactsMethods::get)
                .toList();
        for (String key : methodsKey) {
            if (key.toLowerCase().contains("email")) {
                methods.put(key, generateEmail());
            } else if (key.toLowerCase().contains("phone")) {
                methods.put(key, generatePhoneNumber());
            } else if (key.toLowerCase().contains("address")) {
                methods.put(key, generateAddress(key));
            }
        }
        return methods;
    }

    private String generateEmail() {
        return "customer" + getRandomNumber(0, 99999) + "@generator.com";
    }

    private int addContactsMethods(String location, String contactPayload) {
        String contactAddress = location + "/methods";
        var contactUri = URI.create(contactAddress);

        return httpPost(contactUri, contactPayload).statusCode();
    }

    String generatePeselNumber(int component) {
        String year = String.format("%02d", getRandomNumber(0, 100));
        String month = genMonthNum();
        String days = String.format("%02d", getRandomNumber(1, 32));
        String fiveControlDigits = genControlNumber(component);

        return year + month + days + fiveControlDigits;
    }

    String generatePhoneNumber() {
        int firstFiveDigits = getRandomNumber(10000, 99999);
        int nextDigits = getRandomNumber(100, 999999);
        return "" + firstFiveDigits + nextDigits;
    }

    private String generateAddress(String type) {
        return type + " " + UUID.randomUUID();
    }

    private String genMonthNum() {
        int monthNum = getRandomNumber(1, 33);
        if (monthNum >= 13 && monthNum <= 20) {
            monthNum -= 10;
        }
        return String.format("%02d", monthNum);
    }

    String genControlNumber(int component) {
        if (component < 99999) {
            return String.format("%05d", component);
        }
        return String.valueOf(component).substring(0, 5);
    }

    private String generateName() {
        int nameNumber = getRandomNumber(0, names.size());
        return names.get(nameNumber);
    }

    private String generateSurname() {
        int surnameNumber = getRandomNumber(0, surnames.size());
        return surnames.get(surnameNumber);
    }

    private int getRandomNumber(int start, int bound) {
        return ThreadLocalRandom.current().nextInt(start, bound);
    }

    String makeCustomerPayload(String peselNum, String name, String surname) {
        return """
                {
                "peselNumber": "%s",
                "name": "%s",
                "surname": "%s"
                }
                """.formatted(peselNum, name, surname);
    }

    private String addCustomer(String payload) {
        var customerUri = URI.create("http://localhost:8080/customers");

        return Optional.of(httpPost(customerUri, payload)
                .headers().allValues("location").get(0)).orElseThrow();
    }

    private HttpResponse<Void> httpPost(URI uri, String payload) {
        var request = HttpRequest.newBuilder()
                .POST(HttpRequest.BodyPublishers.ofString(payload))
                .header("Content-Type", "application/json")
                .uri(uri).build();
        try {
            return httpClient.send(request, HttpResponse.BodyHandlers.discarding());
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    String makeContactPayload(String email, String residence,
                              String registered, String privatePhone,
                              String businessPhone) {
        return """
                {
                    "emailAddress": "%s",
                    "residenceAddress": "%s",
                    "registeredAddress": "%s",
                    "privatePhoneNumber": "%s",
                    "businessPhoneNumber": "%s"
                }                      
                """.formatted(email, residence, registered, privatePhone, businessPhone);
    }

    String makeContactPayload(Map<String, String> payload) {
        ObjectMapper mapper = new ObjectMapper();
        String contactJson = "";

        try {
            contactJson = mapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
        // payload.entrySet().stream().map(Map.Entry::getKey).collect(Collectors.joining(","));

        return contactJson;
    }
}
