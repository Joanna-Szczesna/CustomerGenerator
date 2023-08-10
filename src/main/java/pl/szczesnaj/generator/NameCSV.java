package pl.szczesnaj.generator;

class NameCSV implements CSV {
    private String name;
    private String gender;
    private int number;

    NameCSV() {
    }

    NameCSV(String name, String gender, int number) {
        this.name = name;
        this.gender = gender;
        this.number = number;
    }

    public String getName() {
        return name;
    }

    public String getGender() {
        return gender;
    }

    public int getNumber() {
        return number;
    }

    @Override
    public String toString() {
        return "NameCSV{" +
                "name='" + name + '\'' +
                ", gender='" + gender + '\'' +
                ", number=" + number +
                '}';
    }

    @Override
    public String getAttribute() {
        return name;
    }
}
