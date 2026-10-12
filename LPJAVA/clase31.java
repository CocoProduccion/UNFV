public class clase31 {
    public static void main(String[] args) {
        String dni = "1234678";
        boolean EsCorrecto = dni.matches("\\d{8}");
        if (EsCorrecto) {
            System.out.println("DNI valido");
        } else {
            System.out.println("DNI invalido");
        }
    }
}
