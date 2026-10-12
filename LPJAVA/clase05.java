import java.util.Scanner;

public class clase05 {
    public static void main(String[] args) {
        String primerNumero, segundoNumero; // segunda cadena introducida por el usuario
        String resultado; // una cadena que contiene el resultado

        int numero1; // primer número para comparar
        int numero2; // segundo número para comparar
        Scanner sc = new Scanner(System.in);
        // recibir el primer número del usuario como una cadena
        System.out.println("Escriba el primer Numero");
        primerNumero = sc.nextLine();
        numero1 = Integer.parseInt(primerNumero);
        System.out.println("Escriba el Segundo Numero");
        segundoNumero = sc.nextLine();
        numero2 = Integer.parseInt(segundoNumero);
        System.out.println(primerNumero);
        System.out.println(segundoNumero);
        System.out.println(numero1);
        System.out.println(numero2);
        // inicializar resultado con cadena vacía
        resultado = "";
        if (numero1 == numero2)
            resultado = resultado + numero1 + " == " + numero2;

        if (numero1 != numero2)
            resultado = resultado + numero1 + " != " + numero2;

        if (numero1 < numero2)
            resultado = resultado + "\n" + numero1 + " < " + numero2;

        if (numero1 > numero2)
            resultado = resultado + "\n" + numero1 + " > " + numero2;

        if (numero1 <= numero2)
            resultado = resultado + "\n" + numero1 + " <= " + numero2;

        if (numero1 >= numero2)
            resultado = resultado + "\n" + numero1 + " >= " + numero2;

        // Mostrar los resultados

        System.out.println(resultado);
        sc.close();

    }

}
