public class clase44 {
    static int totaloperaciones = 0;
    static final double IGV = 0.18;
    String nombre;
    double precio;

    clase44(String nombre, double precio) {
        this.nombre = nombre;
        this.precio = precio;
        totaloperaciones++;
    }

    double precioConIgv() {
        return precio * (1 + IGV);
    }

    public static void main(String[] args) {
        System.out.println("Cuantos Objetos existen =" + clase44.totaloperaciones);
        System.out.println("Cual es el valor del IGV =" + clase44.IGV);
        System.out.println();
        clase44 pernos = new clase44("Pernos", 15.5);
        clase44 Abrazaderas = new clase44("Abrazaderas", 20.5);
        System.out.println("Cuantos Objetos existen =" + clase44.totaloperaciones);
        System.out.println(pernos.nombre + " -> S/. " + pernos.precioConIgv());
        System.out.println(Abrazaderas.nombre + " -> S/. " + Abrazaderas.precioConIgv());
        System.out.println("Transacciones Realizadas =" + clase44.totaloperaciones);
        System.out.println("Transacciones Realizadas =" + pernos.totaloperaciones);
        System.out.println("Transacciones Realizadas =" + Abrazaderas.totaloperaciones);

        pernos.totaloperaciones = 500;
        System.out.println("Transacciones Realizadas =" + clase44.totaloperaciones);
        System.out.println("Transacciones Realizadas =" + pernos.totaloperaciones);
        System.out.println("Transacciones Realizadas =" + Abrazaderas.totaloperaciones);

    }
}
