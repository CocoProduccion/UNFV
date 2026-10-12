import java.util.InputMismatchException;
import java.util.Scanner;

public class clase30 {
    public static void main(String[] args) {
        int numerador, denominador, resultado;
        Scanner sc = new Scanner(System.in);
        System.out.println("Calculadora de divisiones");
        try {
            System.out.println("Ingrese el numerador: ");
            numerador = sc.nextInt();
            System.out.println("Ingrese el denominador: ");
            denominador = sc.nextInt();
            rango(numerador, denominador);
            resultado = numerador / denominador;
        } catch (InputMismatchException e) {
            System.out.println("Error: Ingresa un numero valido " + e.getMessage());
            resultado = 0;
        } catch (ArithmeticException e) {
            System.out.println("Error: Se ha producido una division entre cero " + e.getMessage());
            resultado = 0;
        } catch (ExcepcionIntervalo e) {
            System.out.println("Error: El numero esta fuera del intervalo " + e.getMessage());
            resultado = 0;
        } catch (Exception e) {
            System.out.println("Error: No definido" + e.getMessage());
            resultado = 0;
        }
        System.out.println("El resultado de la division es: " + resultado);
        sc.close();
    }

    static void rango(int num, int den) throws ExcepcionIntervalo {
        if (num < 0 || num > 100 || den < 0 || den > 100) {
            throw new ExcepcionIntervalo("El numero esta fuera del intervalo");
        }
    }
}
