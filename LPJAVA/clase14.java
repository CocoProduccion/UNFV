import java.util.StringTokenizer;

public class clase14 {
    public static void main(String[] args) {
        String texto = "Java es un lenguaje muy utilizado para desarrollar aplicaciones";

        StringTokenizer tokens = new StringTokenizer(texto, " ");

        int contador = 1;

        while (tokens.hasMoreTokens()) {

            String palabra = tokens.nextToken();

            System.out.println("Token " + contador + ": " + palabra);

            contador++;
        }

        System.out.println("Total de tokens: " + (contador - 1));

    }
}
