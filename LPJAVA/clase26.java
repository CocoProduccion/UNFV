import java.util.Scanner;

public class clase26 {
    public static void main(String[] args) {
        int numero1, numero2, resultado;
        Scanner sc = new Scanner(System.in);
        try {
            System.out.println("Ingrese un numero: ");
            numero1 = sc.nextInt();
            System.out.println("Ingrese otro numero: ");
            numero2 = sc.nextInt();
            resultado = numero1 + numero2;
            System.out.println("El resultado de la suma es: " + resultado);
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        } finally {
            System.out.println("Siempre se ejecuta el finally!");
        }
        sc.close();
    }
}
