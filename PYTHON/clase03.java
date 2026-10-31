/**
 * Bloque static en Java 21.
 *
 * El bloque static se ejecuta una sola vez, cuando la JVM carga la clase,
 * antes de main y antes de cualquier constructor. Sirve para preparar
 * variables estáticas cuando la inicialización no cabe en una sola asignación.
 *
 * Ejecute con:
 *   javac clase03.java
 *   java clase03
 */
public class clase03 {

    static int totalCreados = 0;

    /** Se llena en el bloque static: hace falta un ciclo, no una sola expresión. */
    static final String[] CATEGORIAS = new String[3];

    static {
        System.out.println("1. Bloque static. La clase se carga y esto corre una sola vez.");
        CATEGORIAS[0] = "Abarrotes";
        CATEGORIAS[1] = "Lácteos";
        CATEGORIAS[2] = "Bebidas";
        System.out.println("   Categorías preparadas: " + CATEGORIAS.length);
    }

    String nombre;

    clase03(String nombre) {
        this.nombre = nombre;
        totalCreados++;
        System.out.println("3. Constructor de " + nombre
                + ". El bloque static no se repite. Creados: " + totalCreados);
    }

    public static void main(String[] args) {
        System.out.println("2. main empieza después del bloque static.");
        System.out.println("   Primera categoría: " + CATEGORIAS[0]);
        System.out.println();

        new clase03("Arroz extra");
        new clase03("Leche entera");

        System.out.println();
        System.out.println("Fin. El mensaje del bloque static apareció una sola vez.");
    }
}
