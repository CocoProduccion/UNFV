public class clase45 {
    static int totalObjetos = 0;
    static final double IGV = 0.18;
    String nombre;
    double precio;

    clase45(String nombre, double precio) {
        this.nombre = nombre;
        this.precio = precio;
        totalObjetos++;
    }

    static double aplicaIgv(double precio) {
        return precio * (1 + IGV);
    }

    static int numeroObjetos() {
        return totalObjetos;
    }

    String ficha() {
        return nombre + " -> S/. " + String.format("%.2f", aplicaIgv(precio));
    }

    public static void main(String[] args) {
        clase45 producto1 = new clase45("Laptop", 2500);
        clase45 producto2 = new clase45("Mouse", 50);
        double precio = 1586.00;

        System.out.println(producto1.ficha());
        System.out.println(producto2.ficha());
        System.out.println("Total productos: " + numeroObjetos());
        System.out.println("Total productos: " + clase45.numeroObjetos());
        System.out.println("Total productos: " + producto1.numeroObjetos());
        System.out.println("Total productos: " + producto2.numeroObjetos());
        System.out.println("Total venta: " + clase45.aplicaIgv(precio));
    }

}
