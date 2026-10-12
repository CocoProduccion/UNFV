public class clase32 {
    public static void main(String[] args) {
        String codigo = "LsP-5698";
        boolean EsCorrecto = codigo.matches("[A-Z]{3}-\\d{4}");
        if (EsCorrecto) {
            System.out.println("Codigo valido");
        } else {
            System.out.println("Codigo invalido");
        }
    }
}
