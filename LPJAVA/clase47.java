public class clase47 {
    public static void main(String[] aStrings) {
        System.out.println("Usando Wrappers");
        int cantidad = 30;
        Integer nuevovalor = Integer.valueOf(cantidad);
        System.out.println("Cantidad: " + nuevovalor);
        System.out.println("Valor: " + nuevovalor);
        System.out.println("Son Iguales :" + (cantidad == nuevovalor));
        String textoCantidad = "20";
        String textodouble = "20.36";
        int valorentero = Integer.parseInt(textoCantidad);
        double valordoble = Double.parseDouble(textodouble);
        System.out.println("Valor Entero: " + valorentero);
        System.out.println("Valor Doble: " + valordoble);
    }
}
