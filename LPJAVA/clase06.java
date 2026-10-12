import java.util.Scanner;

// ax2+bx+c=0
public class clase06 {
    public static void main(String[] args) {
        double a, b, c, x1, x2, discriminante;
        Scanner sc = new Scanner(System.in);
        System.out.print("Ingrese a : ");
        a = sc.nextDouble();
        System.out.print("Ingrese b : ");
        b = sc.nextDouble();
        System.out.print("Ingrese c : ");
        c = sc.nextDouble();
        if (a == 0) {
            System.out.println("El coeficiente 'a' no puede ser 0 para una ecuación cuadrática.");
        } else {
            // b * b es ligeramente más rápido que Math.pow(b, 2)
            discriminante = (b * b) - (4 * a * c);
            
            // Calculamos 2 * a una sola vez para evitar repetirlo en las divisiones
            double divisor = 2 * a;

            if (discriminante >= 0) {
                System.out.println("La Ecuacion tiene Solucion Real");
                
                // Calculamos la raíz una sola vez
                double raiz = Math.sqrt(discriminante);
                x1 = (-b + raiz) / divisor;
                x2 = (-b - raiz) / divisor;
                
                System.out.println("El valor de x1 es " + String.format("%17.5f", x1));
                System.out.println("El valor de x2 es " + String.format("%17.5f", x2));
            } else {
                System.out.println("La Ecuacion no tiene Solucion Real es imaginaria");
                
                // Si discriminante es menor a 0, -discriminante lo vuelve positivo sin llamar a Math.abs
                double raiz = Math.sqrt(-discriminante);
                x1 = (-b) / divisor;
                x2 = raiz / divisor;
                
                System.out.println(
                        "El valor de x1 es " + String.format("%17.5f", x1) + " + " + String.format("%17.5f", x2) + "i");
                System.out.println(
                        "El valor de x2 es " + String.format("%17.5f", x1) + " - " + String.format("%17.5f", x2) + "i");
            }
        }

        sc.close();

    }
}
