package pl.szczesnaj.generator;

class SurnameCSV implements CSV {
    private String surname;
    private int number;

    SurnameCSV() {
    }

    SurnameCSV(String surname, int number) {
        this.surname = surname;
        this.number = number;
    }

    public String getSurname() {
        return surname;
    }

    public int getNumber() {
        return number;
    }

    @Override
    public String toString() {
        return "Surname{" +
                "surname='" + surname + '\'' +
                ", number=" + number +
                '}';
    }

    @Override
    public String getAttribute() {
        return surname;
    }
}
