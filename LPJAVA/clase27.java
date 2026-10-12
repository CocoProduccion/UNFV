import java.util.InputMismatchException;
import java.util.Scanner;

public class clase27 {
    public static void main(String[] args) {
        int numero1, numero2, resultado;
        Scanner sc = new Scanner(System.in);
        try {
            System.out.println("Ingrese un numero: ");
            numero1 = sc.nextInt();
            System.out.println("Ingrese otro numero: ");
            numero2 = sc.nextInt();
            resultado = numero1 / numero2;
        } catch (InputMismatchException e) {
            System.out.println("Error: Ingresa un numero valido " + e.getMessage());
            resultado = 0;
        } catch (ArithmeticException e) {
            System.out.println("Error: Se ha producido una division entre cero " + e.getMessage());
            resultado = 0;
        } catch (Exception e) {
            System.out.println("Error: No definido" + e.getMessage());
            resultado = 0;
        }

        System.out.println("El resultado de la division es: " + resultado);
        sc.close();
    }
}
