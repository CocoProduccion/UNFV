import java.io.IOException;

public class clase29 {
    public static void main(String[] args) {

        try {
            System.out.println("Ingrese un caracter(letra)");
            int codigoAscii = System.in.read();
            char letra = (char) codigoAscii;
            System.out.println("La letra es: " + letra);
        } catch (IOException e) {
            System.out.println("Error: " + e.getMessage());
        }

    }
}
