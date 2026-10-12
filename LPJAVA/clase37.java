import java.util.Scanner;

@FunctionalInterface
interface Operacion {
    double Calcular(double a, double b);
}

public class clase37 {
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        double a, b, resultado = 0;
        int op;
        System.out.println("Ingrese el primer numero: ");
        a = sc.nextDouble();
        System.out.println("Ingrese el segundo numero: ");
        b = sc.nextDouble();
        System.out.println("1. Suma");
        System.out.println("2. Resta");
        System.out.println("3. Multiplicacion");
        System.out.println("4. Division");
        System.out.println("Seleccione una operacion: ");
        op = sc.nextInt();
        Operacion operacion = (x, y) -> {
            switch (op) {
                case 1:
                    return x + y;
                case 2:
                    return x - y;
                case 3:
                    return x * y;
                case 4:
                    return x / y;
                default:
                    return 0;
            }
        };
        resultado = operacion.Calcular(a, b);
        System.out.println("El resultado es: " + resultado);
        sc.close();
    }

}
