public class Main {
    public static void main(String[] args){
        Cat cat = new Cat();
        cat.setName("Cat");
        cat.setBreed("Breed");
        System.out.println(ToStrings.toString(cat));
    }
}
