public class clase12 {
    public static void main(String[] args) {

        StringBuilder texto = new StringBuilder("Java");

        System.out.println("Texto inicial: " + texto);

        texto.append(" es un lenguaje");
        System.out.println("Después de append: " + texto);

        texto.insert(4, " moderno");
        System.out.println("Después de insert: " + texto);

        texto.replace(5, 12, "potente");
        System.out.println("Después de replace: " + texto);

        texto.delete(5, 13);
        System.out.println("Después de delete: " + texto);

        texto.append(" ABC");
        texto.reverse();
        System.out.println("Después de reverse: " + texto);

        System.out.println("Cantidad de caracteres: " + texto.length());

        System.out.println("Capacidad disponible: " + texto.capacity());

        String newcadena;
        newcadena = texto.toString();
        System.out.println("Texto convertido a String: " + newcadena);
    }
}
