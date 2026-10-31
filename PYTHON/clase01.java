/**
 * Variables estáticas en Java 21.
 *
 * Una variable estática pertenece a la clase. Existe una sola copia,
 * compartida por todos los objetos. Se crea al cargar la clase, incluso
 * antes de construir el primer objeto.
 *
 * Una variable de instancia pertenece a cada objeto: cada uno tiene la suya.
 *
 * Ejecute con:
 *   javac clase01.java
 *   java clase01
 */
public class clase01 {

    /** Compartida: cuenta cuántos productos se han creado. */
    static int totalCreados = 0;

    /**
     * Constante de clase. static final se asigna una vez y no cambia.
     * Por convención el nombre va en mayúsculas.
     */
    static final double IGV = 0.18;

    /** De instancia: cada producto tiene su propio nombre y precio. */
    String nombre;
    double precio;

    /**
     * El bloque static se ejecuta una sola vez, cuando la JVM carga la clase.
     * Sirve para preparar variables estáticas antes de usarlas.
     */
    static {
        System.out.println("Clase cargada. totalCreados empieza en " + totalCreados);
    }

    clase01(String nombre, double precio) {
        this.nombre = nombre;
        this.precio = precio;
        totalCreados++;
    }

    double precioConIgv() {
        return precio * (1 + IGV);
    }

    public static void main(String[] args) {
        System.out.println("Aún no hay objetos, y la variable ya existe: "
                + clase01.totalCreados);
        System.out.println("IGV de la clase: " + clase01.IGV);
        System.out.println();

        clase01 arroz = new clase01("Arroz extra", 3.50);
        clase01 leche = new clase01("Leche entera", 4.25);

        System.out.println(arroz.nombre + " -> S/ " + arroz.precioConIgv());
        System.out.println(leche.nombre + " -> S/ " + leche.precioConIgv());
        System.out.println();

        System.out.println("Leído desde la clase:     " + clase01.totalCreados);
        System.out.println("Leído desde arroz:        " + arroz.totalCreados);
        System.out.println("Leído desde leche:        " + leche.totalCreados);

        arroz.totalCreados = 10;
        System.out.println("Tras cambiarla en arroz, leche también ve "
                + leche.totalCreados + ": es la misma variable.");
    }
}
