import java.util.Objects;

class Producto {
    private final int codigo;
    private String nombre;
    private double precio;

    public Producto(int codigo, String nombre, double precio) {
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

    // En este ejemplo, dos productos son iguales si tienen el mismo código.
    @Override
    public boolean equals(Object objeto) {
        if (this == objeto) {
            return true;
        }

        if (!(objeto instanceof Producto)) {
            return false;
        }

        Producto otro = (Producto) objeto;
        return codigo == otro.codigo;
    }

    @Override
    public int hashCode() {
        return Objects.hash(codigo);
    }

    @Override
    public String toString() {
        return String.format(
                "Código: %d | Nombre: %-12s | Precio: S/ %.2f",
                codigo, nombre, precio);
    }
}
