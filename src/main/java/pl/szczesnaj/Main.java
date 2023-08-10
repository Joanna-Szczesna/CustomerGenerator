package pl.szczesnaj;

import pl.szczesnaj.generator.CustomerGenerator;

public class Main {
    public static void main(String[] args) {
        int customerNumber = 25;
        for (String e : args) {
            try {
                customerNumber = Integer.parseInt(e);
            } catch (NumberFormatException exc) {
                System.out.println("no customers number");
            }
        }
        CustomerGenerator generator = new CustomerGenerator();
        generator.generate(customerNumber);
    }
}
