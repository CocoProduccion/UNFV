public class clase34 {
    public static void main(String[] args) {
        String TelefonoOriginal = "+60 (987) 959-501";
        String solonumeros = TelefonoOriginal.replaceAll("\\D", "");
        System.out.println("Telefono original: " + TelefonoOriginal);
        System.out.println("Telefono solo numeros: " + solonumeros);
    }
}
