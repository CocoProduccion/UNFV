import java.util.regex.*;

public class clase33 {
    public static void main(String[] args) {
        String texto = "Factura : Valor venta : S/ 1300.00, IGV : S/ 234.00,Total : S/ 1534.00";
        String patron = "S/\\s\\d+(\\.\\d{2})?";
        Pattern pat = Pattern.compile(patron);
        Matcher mat = pat.matcher(texto);
        while (mat.find()) {
            System.out.println("Encontrado:" + mat.group() + " en la posicion:" + mat.start());
        }

    }
}