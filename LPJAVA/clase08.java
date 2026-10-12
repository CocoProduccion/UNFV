import java.util.Scanner;

public class clase08 {
    public static void main(String[] args) {
        int n, f0, f = 0, contador, factorial;
        Scanner sc = new Scanner(System.in);
        do {
            System.out.print("Ingrese el valor de N=");
            n = sc.nextInt();
        } while (n < 0);
        if (n == 0) {
            System.out.println("El valor de la funcion es = 1");
        } else {
            f0 = 1;
            contador = 1;
            while (contador <= n) {
                // calcular el factorial de contador
                factorial = 1;
                for (int j = 1; j <= f0; j++)
                    factorial = factorial * j;
                f = f0 * f0 + factorial;
                f0 = f;
                contador++;
            }
            System.out.println("El valor de la funcion es f(" + n + ") = " + f);
        }
        sc.close();
    }
}
