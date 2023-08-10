/*
 * Copyright (c) 2023 Joanna Szczesna
 * All rights reserved
 */

package pl.szczesnaj.generator;

import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

public class CustomerGenerator {
    private final Map<Enum<Gender>, List<String>> names;
    private final Map<Enum<Gender>, List<String>> surnames;
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
        this.names = Map.of(
                Gender.FEMALE, getDataFromFile("names_woman.csv", NameCSV.class),
                Gender.MALE, getDataFromFile("names_man.csv", NameCSV.class));
        this.surnames = Map.of(
                Gender.FEMALE, getDataFromFile("surnames_woman.csv", SurnameCSV.class),
                Gender.MALE, getDataFromFile("surnames_man.csv", SurnameCSV.class));
    }

    List<String> getDataFromFile(String fileName, Class<?> type) {
        CsvMapper mapper = new CsvMapper();
        CsvSchema schema = CsvSchema.emptySchema().withHeader();

        ObjectReader oReader = mapper.readerFor(type).with(schema);
        List<CSV> attributes = new ArrayList<>();

        try (Reader reader = new FileReader(fileName)) {
            MappingIterator<CSV> iterator = oReader.readValues(reader);
            while (iterator.hasNext() && attributes.size() < 100) {
                CSV current = iterator.next();
                attributes.add(current);
                System.out.println(current);
            }
        } catch (FileNotFoundException e) {
            if (type.getName().equals("pl.szczesnaj.generator.SurnameCSV")) {
                return Arrays.asList(
                        "Kot",
                        "Mot",
                        "Nowak",
                        "Kowal",
                        "Szklany",
                        "Rad"
                );
            } else if (type.getName().equals("pl.szczesnaj.generator.NameCSV")) {
                return Arrays.asList(
                        "Ala",
                        "Tola",
                        "Ewa",
                        "Natalka",
                        "Jan",
                        "Franek",
                        "Bartek",
                        "Jerzy"
                );
            }

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return attributes.stream().map(CSV::getAttribute).collect(Collectors.toList());
    }

    public void generate(int customersNumber) {
        for (int i = 0; i < customersNumber; i++) {
            String peselNum = generatePeselNumber(i);
            Gender gender = getGenderFromPeselNumber(peselNum);
            final Map<String, String> person = Map.of(
                    "peselNumber", peselNum,
                    "name", generateName(gender),
                    "surname", generateSurname(gender));

            String customerPayload = makePayload(person);

            String location = addCustomer(customerPayload);
            System.out.printf("Added customer: %s%n", location);

            int quantity = getRandomNumber(2, 5);
            String contactPayload = addContacts(quantity);
            int statusCode = addContactsMethods(location, contactPayload);
            System.out.printf("Added methods. Status Code: %s%n", statusCode);
        }
    }
    Gender getGenderFromPeselNumber(String peselNumber){
        int orderNumber = Integer.parseInt(peselNumber.substring(9, 10));
        return orderNumber %2 ==0? Gender.FEMALE : Gender.MALE;
    }

    private String addContacts(int quantity) {
        Set<Integer> numbers = new HashSet<>();
        while (numbers.size() < quantity) {
            numbers.add(getRandomNumber(0, allowedContactsMethods.size()));
        }
        Map<String, String> methods = generateMethods(numbers);

        return makePayload(methods);
    }

    String makePayload(Map<String, String> payload) {
        return payload.entrySet().stream()
                .map(e ->
                        """
                                "%s": "%s\"""".formatted(
                                e.getKey(), e.getValue()))
                .collect(Collectors.joining(
                        ",",
                        "{",
                        "}"
                ));
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
        return "customer" + getRandomNumber(0, 99999) + "@example.com";
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
        int nextDigits = getRandomNumber(1000, 999999);
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

    private String generateName(Gender gender) {
        int nameNumber = getRandomNumber(0, names.get(gender).size());
        return names
                .get(gender)
                .get(nameNumber);
    }

    private String generateSurname(Gender gender) {
        int surnameNumber = getRandomNumber(0, surnames.get(gender).size());
        return surnames
                .get(gender)
                .get(surnameNumber);
    }

    private int getRandomNumber(int start, int bound) {
        return ThreadLocalRandom.current().nextInt(start, bound);
    }

    private String addCustomer(String payload) {
        var customerUri = URI.create("http://localhost:8080/customers");

        return httpPost(customerUri, payload)
                .headers().allValues("location").get(0);
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
}

