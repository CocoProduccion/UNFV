import java.util.Scanner;

public class clase07 {

    public static void main(String[] args) {

        Scanner scanner = new Scanner(System.in);
        int opcion;

        do {
            System.out.println("\n==============================");
            System.out.println("   EJEMPLOS DE BUCLES EN JAVA");
            System.out.println("==============================");
            System.out.println("1. Ejemplo con while");
            System.out.println("2. Ejemplo con do...while");
            System.out.println("3. Ejemplo con for");
            System.out.println("0. Salir");
            System.out.print("Seleccione una opción: ");

            opcion = scanner.nextInt();

            switch (opcion) {

                case 1:
                    System.out.println("\n--- Ejemplo usando while ---");
                    System.out.println("Sumaremos los números del 1 al 5.");

                    int numero = 1;
                    int suma = 0;

                    while (numero <= 5) {
                        System.out.println("Sumando: " + numero);

                        suma = suma + numero;
                        numero++;
                    }

                    System.out.println("La suma total es: " + suma);
                    break;

                case 2:
                    System.out.println("\n--- Ejemplo usando do...while ---");

                    int claveCorrecta = 1234;
                    int claveIngresada;
                    int intentos = 0;

                    do {
                        System.out.print("Ingrese la clave: ");
                        claveIngresada = scanner.nextInt();

                        intentos++;

                        if (claveIngresada != claveCorrecta) {
                            System.out.println("Clave incorrecta. Intente nuevamente.");
                        }

                    } while (claveIngresada != claveCorrecta);

                    System.out.println("Acceso permitido.");
                    System.out.println("Número de intentos: " + intentos);
                    break;

                case 3:
                    System.out.println("\n--- Ejemplo usando for ---");
                    System.out.println("Tabla de multiplicar del 7:");

                    for (int i = 1; i <= 12; i++) {
                        System.out.println("7 x " + i + " = " + (7 * i));
                    }
                    break;

                case 0:
                    System.out.println("Programa finalizado.");
                    break;

                default:
                    System.out.println("Opción no válida. Intente nuevamente.");
            }

        } while (opcion != 0);

        scanner.close();
    }
}