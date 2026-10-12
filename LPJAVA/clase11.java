public class clase11 {

    public static void main(String[] args) {

        String texto = "programacion en java";
        // String texto = new String("programacion en java");
        System.out.println("Texto original: " + texto);

        System.out.println("Cantidad de caracteres: " + texto.length());

        System.out.println("Primer carácter: " + texto.charAt(2));

        System.out.println("Texto en mayúsculas: " + texto.toUpperCase());

        System.out.println("Texto en minúsculas: " + texto.toLowerCase());

        System.out.println("Desde la posición 12: " + texto.substring(12, 15));

        System.out.println("¿Contiene la palabra java?: " + texto.contains("java"));

        System.out.println("Reemplazo: " + texto.replace("java", "Python"));
        System.out.println("Imprimiendo Reemplazo: " + texto);
        String texto2 = "programacion en java";
        System.out.println("¿Los textos son iguales?: " + texto.equals(texto2));
        String texto3 = "  programacion en rust  ";
        System.out.println("Imprimiendo Texto 3 :" + texto3);
        System.out.println("Imprimiendo Texto 3 sin espacios :" + texto3.trim());
        System.out.println("Imprimiendo Texto 3 :" + texto3);
    }
}
