package pl.szczesnaj;

import pl.szczesnaj.generator.CustomerGenerator;

public class Main {
    public static void main(String[] args)
    {
        CustomerGenerator generator = new CustomerGenerator();
        generator.generate(2);
    }
}