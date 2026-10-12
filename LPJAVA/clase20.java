import java.util.Scanner;

public class clase20 {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        int filas = 2;
        int columnas = 2;

        int[][] matrizA = new int[filas][columnas];
        int[][] matrizB = new int[filas][columnas];

        int[][] suma = new int[filas][columnas];
        int[][] resta = new int[filas][columnas];
        int[][] multiplicacion = new int[filas][columnas];

        // Ingreso de matriz A
        System.out.println("Ingrese los valores de la matriz A:");

        for (int i = 0; i < filas; i++) {
            for (int j = 0; j < columnas; j++) {
                System.out.print("A[" + i + "][" + j + "]: ");
                matrizA[i][j] = scanner.nextInt();
            }
        }

        // Ingreso de matriz B
        System.out.println("\nIngrese los valores de la matriz B:");

        for (int i = 0; i < filas; i++) {
            for (int j = 0; j < columnas; j++) {
                System.out.print("B[" + i + "][" + j + "]: ");
                matrizB[i][j] = scanner.nextInt();
            }
        }

        // Suma y resta de matrices
        for (int i = 0; i < filas; i++) {
            for (int j = 0; j < columnas; j++) {
                suma[i][j] = matrizA[i][j] + matrizB[i][j];
                resta[i][j] = matrizA[i][j] - matrizB[i][j];
            }
        }

        // Multiplicación de matrices
        for (int i = 0; i < filas; i++) {
            for (int j = 0; j < columnas; j++) {
                multiplicacion[i][j] = 0;

                for (int k = 0; k < columnas; k++) {
                    multiplicacion[i][j] = multiplicacion[i][j]
                            + matrizA[i][k] * matrizB[k][j];
                }
            }
        }

        // Mostrar matriz A
        System.out.println("\nMatriz A:");

        for (int i = 0; i < filas; i++) {
            for (int j = 0; j < columnas; j++) {
                System.out.print(matrizA[i][j] + "\t");
            }
            System.out.println();
        }

        // Mostrar matriz B
        System.out.println("\nMatriz B:");

        for (int i = 0; i < filas; i++) {
            for (int j = 0; j < columnas; j++) {
                System.out.print(matrizB[i][j] + "\t");
            }
            System.out.println();
        }

        // Mostrar suma
        System.out.println("\nSuma de matrices:");

        for (int i = 0; i < filas; i++) {
            for (int j = 0; j < columnas; j++) {
                System.out.print(suma[i][j] + "\t");
            }
            System.out.println();
        }

        // Mostrar resta
        System.out.println("\nResta de matrices:");

        for (int i = 0; i < filas; i++) {
            for (int j = 0; j < columnas; j++) {
                System.out.print(resta[i][j] + "\t");
            }
            System.out.println();
        }

        // Mostrar multiplicación
        System.out.println("\nMultiplicación de matrices:");

        for (int i = 0; i < filas; i++) {
            for (int j = 0; j < columnas; j++) {
                System.out.print(multiplicacion[i][j] + "\t");
            }
            System.out.println();
        }

        scanner.close();

    }
}
