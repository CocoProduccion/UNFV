public class clase13 {
    public static void main(String[] args) {

        StringBuffer texto = new StringBuffer("Curso");

        System.out.println("Texto inicial: " + texto);

        texto.append(" de Java");
        System.out.println("Después de append: " + texto);

        texto.insert(0, "Inicio - ");
        System.out.println("Después de insert: " + texto);

        texto.replace(0, 6, "Título");
        System.out.println("Después de replace: " + texto);

        texto.delete(6, 9);
        System.out.println("Después de delete: " + texto);

        System.out.println("Carácter en posición 0: " + texto.charAt(0));

        texto.setCharAt(0, 't');
        System.out.println("Después de setCharAt: " + texto);

        texto.reverse();
        System.out.println("Texto invertido: " + texto);

        System.out.println("Longitud final: " + texto.length());

        String newString;

        newString = texto.toString();
        System.out.println("Texto convertido a String: " + newString);

    }
}
