public class clase18 {
    public static void main(String[] args) {
        final int LONGITUD_ARREGLO = 10; // constante
        int arreglo[]; // referencia a un arreglo int
        arreglo = new int[LONGITUD_ARREGLO]; // crear el arreglo

        // calcular el valor para cada elemento del arreglo
        for (int contador = 0; contador < arreglo.length; contador++)
            arreglo[contador] = 2 + 2 * contador;

        String salida = "Índice\tValor\n";

        for (int contador = 0; contador < arreglo.length; contador++)
            salida += contador + "\t" + arreglo[contador] + "\n";

        System.out.println(salida);

    }
}
