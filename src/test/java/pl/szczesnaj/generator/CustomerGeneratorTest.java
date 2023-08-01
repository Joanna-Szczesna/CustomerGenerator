/*
 * Copyright (c) 2023 Joanna Szczesna
 * All rights reserved
 */

package pl.szczesnaj.generator;

import com.google.common.collect.Range;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.*;

class CustomerGeneratorTest {

    public static final String PESEL_NUM = "11111111111";
    public static final String MIESZKO = "Mieszko";
    public static final String PIERWSZY = "Pierwszy";
    private CustomerGenerator generator;

    @BeforeEach
    void setUp() {
        generator = new CustomerGenerator();
    }

    @Nested
    class PayloadStructure {
        @Test
        void customer() {
            String returnStatement = generator.makeCustomerPayload(PESEL_NUM, MIESZKO, PIERWSZY);

            assertThat(returnStatement).contains("""
                    {
                    "peselNumber": "11111111111",
                    "name": "Mieszko",
                    "surname": "Pierwszy"
                    }
                    """);
        }

        @Test
        void allContacts() {
            String returnStatement = generator.makeContactPayload("email@name.com",
                    "residence", "registered",
                    "111111111", "222222222");

            assertThat(returnStatement).contains("""
                    {
                        "emailAddress": "email@name.com",
                        "residenceAddress": "residence",
                        "registeredAddress": "registered",
                        "privatePhoneNumber": "111111111",
                        "businessPhoneNumber": "222222222"
                    } 
                            """);
        }

        @Test
        void addOnlyTwoWaysToContact() {
            Map<String, String> contacts = new HashMap<>();
            contacts.put("privatePhoneNumber", "111111111");
            contacts.put("residenceAddress", "residence");

            String returnStatement = generator.makeContactPayload(contacts);
            assertThat(returnStatement).contains("""
                    {"residenceAddress":"residence","privatePhoneNumber":"111111111"}""");
        }
    }

    @Nested
    class Pesel {
        @Test
        void shouldContainsElevenChar() {
            int peselLength = generator.generatePeselNumber(1).length();

            assertEquals(11, peselLength);
        }

        @Test
        void shouldContainsOnlyDigits() {
            String peselNumber = generator.generatePeselNumber(1);
            boolean onlyDigits = peselNumber.chars().allMatch(Character::isDigit);

            assertTrue(onlyDigits);
        }

        @Test
        void monthDigitsInScope() {
            String peselNumber = generator.generatePeselNumber(1);
            int month = Integer.parseInt(peselNumber.substring(2, 4));

            assertThat(month).isIn(Range.closed(1, 32));
            assertThat(month).isNotIn(Range.closed(13, 20));
        }

        @Test
        void daysDigitsInScope() {
            String peselNumber = generator.generatePeselNumber(1);
            int days = Integer.parseInt(peselNumber.substring(4, 6));

            assertThat(days).isIn(Range.closed(1, 31));
        }

        @Test
        void noDuplicateAllowed() {
            String peselNumberFirst = generator.generatePeselNumber(1);
            String peselNumberSecond = generator.generatePeselNumber(2);

            assertNotEquals(peselNumberFirst, peselNumberSecond);
        }

        @Test
        void hasFiveControlDigits() {
            String controlNumber = String.valueOf(generator.genControlNumber(1));
            int sizeControlNum = controlNumber.length();

            String controlNumberSecond = String.valueOf(generator.genControlNumber(150000));
            int sizeControlNumSecond = controlNumberSecond.length();

            assertEquals(5, sizeControlNum);
            assertEquals(5, sizeControlNumSecond);
        }
    }

    @Nested
    class ContactMethods {

        @Test
        void generatedPhoneNumberNotNull() {
            String number = generator.generatePhoneNumber();

            assertNotEquals(null, number);
        }

        @Test
        void generatedPhoneNumberHasMinFiveMaxElevenChars() {
            int phoneNumberSize = generator.generatePhoneNumber().length();

            assertThat(phoneNumberSize).isIn(Range.closed(5, 11));
        }
        @Test
        void generatedPhoneNumberHasOnlyDigits() {
            String phoneNumberSize = generator.generatePhoneNumber();
            boolean onlyDigits = phoneNumberSize.chars().allMatch(Character::isDigit);

            assertTrue(onlyDigits);
        }

        @Test
        void name() {
            List<String> allowedContactsMethods = Arrays.asList(
                    "emailAddress",
                    "residenceAddress",
                    "registeredAddress",
                    "privatePhoneNumber",
                    "businessPhoneNumber");

        }
    }
}
