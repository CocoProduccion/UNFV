import java.util.Scanner;

public class clase10 {

    public static void main(String[] args) {

        Scanner scanner = new Scanner(System.in);

        char caracter;
        int opcion;

        do {
            System.out.println("\n==================================");
            System.out.println("    MANEJO DE CHAR Y CHARACTER");
            System.out.println("==================================");
            System.out.println("1. Analizar un carácter");
            System.out.println("2. Convertir mayúscula / minúscula");
            System.out.println("3. Mostrar código Unicode del carácter");
            System.out.println("4. Comparar dos caracteres");
            System.out.println("5. Validar una contraseña simple");
            System.out.println("0. Salir");
            System.out.print("Seleccione una opción: ");

            opcion = scanner.nextInt();
            scanner.nextLine(); // Limpia el salto de línea pendiente.

            switch (opcion) {

                case 1:
                    System.out.print("Ingrese un carácter: ");
                    String texto = scanner.nextLine();

                    if (texto.length() == 0) {
                        System.out.println("No ingresó ningún carácter.");
                        break;
                    }

                    caracter = texto.charAt(0);

                    System.out.println("\nCarácter ingresado: " + caracter);

                    if (Character.isLetter(caracter)) {
                        System.out.println("Es una letra.");
                    } else {
                        System.out.println("No es una letra.");
                    }

                    if (Character.isDigit(caracter)) {
                        System.out.println("Es un dígito.");
                    } else {
                        System.out.println("No es un dígito.");
                    }

                    if (Character.isLetterOrDigit(caracter)) {
                        System.out.println("Es una letra o un dígito.");
                    } else {
                        System.out.println("No es letra ni dígito.");
                    }

                    if (Character.isUpperCase(caracter)) {
                        System.out.println("Está en mayúscula.");
                    }

                    if (Character.isLowerCase(caracter)) {
                        System.out.println("Está en minúscula.");
                    }

                    if (Character.isWhitespace(caracter)) {
                        System.out.println("Es un espacio o carácter de separación.");
                    }

                    if (!Character.isLetterOrDigit(caracter)
                            && !Character.isWhitespace(caracter)) {
                        System.out.println("Es un símbolo especial.");
                    }

                    break;

                case 2:
                    System.out.print("Ingrese un carácter: ");
                    texto = scanner.nextLine();

                    if (texto.length() == 0) {
                        System.out.println("No ingresó ningún carácter.");
                        break;
                    }

                    caracter = texto.charAt(0);

                    System.out.println("Carácter original: " + caracter);
                    System.out.println("En mayúscula: " +
                            Character.toUpperCase(caracter));
                    System.out.println("En minúscula: " +
                            Character.toLowerCase(caracter));

                    break;

                case 3:
                    System.out.print("Ingrese un carácter: ");
                    texto = scanner.nextLine();

                    if (texto.length() == 0) {
                        System.out.println("No ingresó ningún carácter.");
                        break;
                    }

                    caracter = texto.charAt(0);

                    System.out.println("Carácter: " + caracter);
                    System.out.println("Código decimal: " + (int) caracter);
                    System.out.println("Código hexadecimal: " +
                            Integer.toHexString((int) caracter).toUpperCase());

                    break;

                case 4:
                    System.out.print("Ingrese el primer carácter: ");
                    String texto1 = scanner.nextLine();

                    System.out.print("Ingrese el segundo carácter: ");
                    String texto2 = scanner.nextLine();

                    if (texto1.length() == 0 || texto2.length() == 0) {
                        System.out.println("Debe ingresar ambos caracteres.");
                        break;
                    }

                    char caracter1 = texto1.charAt(0);
                    char caracter2 = texto2.charAt(0);

                    System.out.println("\nPrimer carácter: " + caracter1);
                    System.out.println("Segundo carácter: " + caracter2);

                    if (caracter1 == caracter2) {
                        System.out.println("Los caracteres son iguales.");
                    } else {
                        System.out.println("Los caracteres son diferentes.");
                    }

                    Character objetoCaracter1 = Character.valueOf(caracter1);
                    Character objetoCaracter2 = Character.valueOf(caracter2);

                    int comparacion = objetoCaracter1.compareTo(objetoCaracter2);

                    if (comparacion < 0) {
                        System.out.println(
                                caracter1 + " aparece antes que " +
                                        caracter2 + " según Unicode.");
                    } else if (comparacion > 0) {
                        System.out.println(
                                caracter1 + " aparece después que " +
                                        caracter2 + " según Unicode.");
                    } else {
                        System.out.println(
                                "Ambos tienen el mismo valor Unicode.");
                    }

                    break;

                case 5:
                    System.out.print("Ingrese una contraseña: ");
                    String clave = scanner.nextLine();

                    boolean tieneMayuscula = false;
                    boolean tieneMinuscula = false;
                    boolean tieneDigito = false;
                    boolean tieneSimbolo = false;

                    for (int i = 0; i < clave.length(); i++) {
                        caracter = clave.charAt(i);

                        if (Character.isUpperCase(caracter)) {
                            tieneMayuscula = true;
                        } else if (Character.isLowerCase(caracter)) {
                            tieneMinuscula = true;
                        } else if (Character.isDigit(caracter)) {
                            tieneDigito = true;
                        } else if (!Character.isWhitespace(caracter)) {
                            tieneSimbolo = true;
                        }
                    }

                    System.out.println("\nResultado de validación:");

                    if (clave.length() >= 8) {
                        System.out.println("✓ Tiene al menos 8 caracteres.");
                    } else {
                        System.out.println("✗ Debe tener al menos 8 caracteres.");
                    }

                    if (tieneMayuscula) {
                        System.out.println("✓ Contiene una letra mayúscula.");
                    } else {
                        System.out.println("✗ No contiene letra mayúscula.");
                    }

                    if (tieneMinuscula) {
                        System.out.println("✓ Contiene una letra minúscula.");
                    } else {
                        System.out.println("✗ No contiene letra minúscula.");
                    }

                    if (tieneDigito) {
                        System.out.println("✓ Contiene un número.");
                    } else {
                        System.out.println("✗ No contiene número.");
                    }

                    if (tieneSimbolo) {
                        System.out.println("✓ Contiene un símbolo especial.");
                    } else {
                        System.out.println("✗ No contiene símbolo especial.");
                    }

                    if (clave.length() >= 8
                            && tieneMayuscula
                            && tieneMinuscula
                            && tieneDigito
                            && tieneSimbolo) {

                        System.out.println(
                                "\nContraseña válida y suficientemente segura.");
                    } else {
                        System.out.println(
                                "\nLa contraseña no cumple todos los requisitos.");
                    }

                    break;

                case 0:
                    System.out.println("Programa finalizado.");
                    break;

                default:
                    System.out.println("Opción no válida.");
            }

        } while (opcion != 0);

        scanner.close();
    }
}