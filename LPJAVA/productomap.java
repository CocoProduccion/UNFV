import java.util.Objects;

class Productomap {
    private final int codigo;
    private final String nombre;
    private double precio;

    public Productomap(int codigo, String nombre, double precio) {
        this.codigo = codigo;
        this.nombre = nombre;
        this.precio = precio;
    }

    public int getCodigo() {
        return codigo;
    }

    public String getNombre() {
        return nombre;
    }

    public double getPrecio() {
        return precio;
    }

    public void setPrecio(double precio) {
        this.precio = precio;
    }

    // Para comparar valores Producto, usamos todos sus atributos.
    @Override
    public boolean equals(Object objeto) {
        if (this == objeto) {
            return true;
        }

        if (!(objeto instanceof Productomap)) {
            return false;
        }

        Productomap otro = (Productomap) objeto;

        return codigo == otro.codigo
                && Objects.equals(nombre, otro.nombre)
                && Double.compare(precio, otro.precio) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(codigo, nombre, precio);
    }

    @Override
    public String toString() {
        return String.format(
                "Código: %d | Nombre: %-12s | Precio: S/ %.2f",
                codigo, nombre, precio);
    }
}
