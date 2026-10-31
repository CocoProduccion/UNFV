/**
 * Métodos estáticos en Java 21.
 *
 * Un método static pertenece a la clase. Se llama con el nombre de la clase
 * y no necesita un objeto. Solo puede usar variables y métodos estáticos:
 * no tiene this, así que no ve nombre ni precio de un producto concreto.
 *
 * Un método de instancia pertenece a un objeto. Sí puede usar this y también
 * puede llamar a un método estático.
 *
 * Ejecute con:
 *   javac clase02.java
 *   java clase02
 */
public class clase02 {

    static int totalCreados = 0;
    static final double IGV = 0.18;

    String nombre;
    double precio;

    clase02(String nombre, double precio) {
        this.nombre = nombre;
        this.precio = precio;
        totalCreados++;
    }

    /**
     * Estático: el precio llega como parámetro. No consulta ningún objeto.
     */
    static double aplicarIgv(double precio) {
        return precio * (1 + IGV);
    }

    /** Estático: lee la variable compartida totalCreados. */
    static int cuantosHay() {
        return totalCreados;
    }

    /**
     * De instancia: usa el nombre y el precio de ESTE producto,
     * y delega el cálculo en el método estático.
     */
    String ficha() {
        return nombre + " -> S/ " + String.format("%.2f", aplicarIgv(precio));
    }

    public static void main(String[] args) {
        System.out.println("Sin objetos, aplicarIgv(10) = "
                + String.format("%.2f", clase02.aplicarIgv(10)));
        System.out.println("Sin objetos, cuantosHay() = " + clase02.cuantosHay());
        System.out.println();

        clase02 arroz = new clase02("Arroz extra", 3.50);
        clase02 leche = new clase02("Leche entera", 4.25);

        System.out.println(arroz.ficha());
        System.out.println(leche.ficha());
        System.out.println();

        System.out.println("Desde la clase: " + clase02.cuantosHay());
        System.out.println("Desde arroz:    " + arroz.cuantosHay());
        System.out.println("Desde leche:    " + leche.cuantosHay());
    }
}
