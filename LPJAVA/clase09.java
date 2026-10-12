public class clase09 {

    public static void main(String[] args) {

        System.out.println("Ejemplo de break y continue");
        System.out.println("============================");

        // El bucle intentará recorrer los números del 1 al 10.
        for (int numero = 1; numero <= 10; numero++) {

            // continue: no procesa los números pares.
            if (numero % 2 == 0) {
                System.out.println("El número " + numero + " es par: se omite con continue.");
                continue;
            }

            // break: termina el bucle al llegar al número 7.
            if (numero == 7) {
                System.out.println("Se encontró el número 7: se finaliza el bucle con break.");
                break;
            }

            // Esta línea solo se ejecuta para impares distintos de 7.
            System.out.println("Procesando el número impar: " + numero);
        }

        System.out.println("============================");
        System.out.println("El programa terminó.");
    }
}