import java.util.*;

public class clase28 {
    public static void main(String[] args) {
        int numero[] = new int[5];

        for (int i = 0; i < 5; i++) {
            numero[i] = i + 1;
        }
        try {
            for (int i = 0; i < 10; i++) {
                System.out.println(numero[i]);
            }
        } catch (ArrayIndexOutOfBoundsException e) {
            System.out.println("Error: Indice fuera del Intervalo " + e.getMessage());
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
    }
}