/**
 * Clases envoltorio (wrappers) en Java 21.
 *
 * Cada tipo primitivo tiene una clase que lo envuelve:
 *
 *   int     -> Integer      double  -> Double
 *   boolean -> Boolean      char    -> Character
 *
 * El wrapper es un objeto: puede valer null, entrar en una lista y ofrece
 * métodos como parseInt. El primitivo es el valor directo y no puede ser null.
 *
 * Ejecute con:
 *   javac clase04.java
 *   java clase04
 */
import java.util.ArrayList;

public class clase04 {

    public static void main(String[] args) {
        System.out.println("--- 1. Primitivo y wrapper ---");
        int cantidad = 10;
        Integer cantidadObj = Integer.valueOf(cantidad);
        System.out.println("primitivo int:     " + cantidad);
        System.out.println("wrapper Integer:   " + cantidadObj);
        System.out.println("son el mismo dato: " + (cantidad == cantidadObj));

        System.out.println();
        System.out.println("--- 2. Autoboxing y unboxing ---");
        Double precio = 3.50;          // autoboxing: double -> Double
        double precioBase = precio;    // unboxing:   Double -> double
        double conIgv = precioBase * 1.18;
        System.out.println("Precio objeto:  " + precio);
        System.out.println("Precio con IGV: " + String.format("%.2f", conIgv));

        System.out.println();
        System.out.println("--- 3. Texto hacia número y booleano ---");
        String textoCantidad = "25";
        String textoPrecio = "4.25";
        String textoActivo = "true";
        int unidades = Integer.parseInt(textoCantidad);
        double valor = Double.parseDouble(textoPrecio);
        boolean activo = Boolean.parseBoolean(textoActivo);
        System.out.println(unidades + " unidades a S/ " + valor + ", activo = " + activo);

        System.out.println();
        System.out.println("--- 4. null: solo el wrapper puede no tener valor ---");
        Integer stock = null;
        System.out.println("stock (Integer) = " + stock);
        try {
            int copia = stock;         // unboxing de null
            System.out.println(copia);
        } catch (NullPointerException e) {
            System.out.println("Pasar null a un int lanza NullPointerException.");
        }

        System.out.println();
        System.out.println("--- 5. Una lista guarda objetos, no primitivos ---");
        ArrayList<Integer> codigos = new ArrayList<>();
        codigos.add(101);              // el int 101 se envuelve solo
        codigos.add(Integer.valueOf(205));
        int primero = codigos.get(0);  // el Integer se desempaqueta solo
        System.out.println("Códigos: " + codigos);
        System.out.println("El primero, ya como int: " + primero);

        System.out.println();
        System.out.println("--- 6. equals compara el valor; == compara el objeto ---");
        Integer bajo1 = 100;
        Integer bajo2 = 100;
        Integer alto1 = 200;
        Integer alto2 = 200;
        System.out.println("100 equals 100: " + bajo1.equals(bajo2));
        System.out.println("100 == 100:     " + (bajo1 == bajo2));
        System.out.println("200 equals 200: " + alto1.equals(alto2));
        System.out.println("200 == 200:     " + (alto1 == alto2));
        System.out.println("Para el valor use equals. == solo coincide en el rango cacheado -128..127.");

        System.out.println();
        System.out.println("--- 7. Métodos de la clase envoltorio ---");
        System.out.println("Integer.max(10, 25) = " + Integer.max(10, 25));
        System.out.println("Character.isDigit('4') = " + Character.isDigit('4'));
        System.out.println("Character.isDigit('A') = " + Character.isDigit('A'));
        System.out.println("Boolean.FALSE = " + Boolean.FALSE);
    }
}
