import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

public class clase42 {

    public static void main(String[] args) {

        Set<Producto> productos = new LinkedHashSet<>();

        titulo("1. isEmpty(): verificar si el conjunto está vacío");
        System.out.println("¿Está vacío? " + productos.isEmpty());

        titulo("2. add(): agregar productos");
        productos.add(new Producto(1, "Teclado", 85.00));
        productos.add(new Producto(2, "Mouse", 45.00));
        productos.add(new Producto(3, "Monitor", 650.00));
        mostrar(productos);

        titulo("3. add(): intentar agregar un duplicado");

        boolean agregado = productos.add(
                new Producto(2, "Mouse diferente", 99.00));

        System.out.println("¿Se agregó el producto repetido? " + agregado);
        System.out.println("El producto original no se reemplaza.");
        mostrar(productos);

        titulo("4. size(): obtener la cantidad de productos");
        System.out.println("Cantidad: " + productos.size());

        titulo("5. contains(): comprobar si existe un producto");

        Producto buscado = new Producto(2, "Mouse", 45.00);

        System.out.println(
                "¿Existe el producto con código 2? "
                        + productos.contains(buscado));

        titulo("6. Recorrer y buscar un producto por código");

        Producto encontrado = buscarPorCodigo(productos, 2);

        if (encontrado != null) {
            System.out.println("Producto encontrado:");
            System.out.println(encontrado);

            encontrado.setPrecio(50.00);

            System.out.println("Después de modificar su precio:");
            System.out.println(encontrado);
        } else {
            System.out.println("Producto no encontrado.");
        }

        titulo("7. addAll(): agregar varios productos");

        Set<Producto> adicionales = new LinkedHashSet<>(Arrays.asList(
                new Producto(3, "Monitor", 650.00),
                new Producto(4, "Parlantes", 120.00),
                new Producto(5, "Webcam", 150.00)));

        boolean cambio = productos.addAll(adicionales);

        System.out.println("¿Cambió el conjunto? " + cambio);
        System.out.println("El código 3 ya existía y no se duplica.");
        mostrar(productos);

        titulo("8. containsAll(): comprobar varios productos");

        Set<Producto> requeridos = new LinkedHashSet<>(Arrays.asList(
                new Producto(1, "Teclado", 85.00),
                new Producto(5, "Webcam", 150.00)));

        System.out.println(
                "¿Contiene todos los productos requeridos? "
                        + productos.containsAll(requeridos));

        titulo("9. forEach(): recorrer todos los productos");
        productos.forEach(producto -> System.out.println(producto));

        titulo("10. iterator(): recorrer usando un iterador");

        Iterator<Producto> iterador = productos.iterator();

        while (iterador.hasNext()) {
            Producto producto = iterador.next();
            System.out.println(producto);
        }

        titulo("11. toArray(): convertir el conjunto en un arreglo");

        Producto[] arreglo = productos.toArray(new Producto[0]);

        for (Producto producto : arreglo) {
            System.out.println(producto);
        }

        titulo("12. Crear una copia del conjunto");

        Set<Producto> copia = new LinkedHashSet<>(productos);
        mostrar(copia);

        titulo("13. equals(): comparar dos conjuntos");

        Set<Producto> otroOrden = new LinkedHashSet<>(Arrays.asList(
                new Producto(5, "Webcam", 150.00),
                new Producto(4, "Parlantes", 120.00),
                new Producto(3, "Monitor", 650.00),
                new Producto(2, "Mouse", 50.00),
                new Producto(1, "Teclado", 85.00)));

        System.out.println(
                "¿Tienen los mismos productos aunque cambie el orden? "
                        + productos.equals(otroOrden));

        titulo("14. addAll(): unión de conjuntos");

        Set<Producto> otroCatalogo = new LinkedHashSet<>(Arrays.asList(
                new Producto(3, "Monitor", 650.00),
                new Producto(5, "Webcam", 150.00),
                new Producto(6, "Impresora", 800.00)));

        Set<Producto> union = new LinkedHashSet<>(productos);
        union.addAll(otroCatalogo);

        System.out.println("Productos presentes en uno o ambos conjuntos:");
        mostrar(union);

        titulo("15. retainAll(): intersección de conjuntos");

        Set<Producto> interseccion = new LinkedHashSet<>(productos);
        interseccion.retainAll(otroCatalogo);

        System.out.println("Productos presentes en ambos conjuntos:");
        mostrar(interseccion);

        titulo("16. removeAll(): diferencia de conjuntos");

        Set<Producto> diferencia = new LinkedHashSet<>(productos);
        diferencia.removeAll(otroCatalogo);

        System.out.println(
                "Productos del conjunto original que no están en el otro:");
        mostrar(diferencia);

        titulo("17. remove(): eliminar un producto");

        boolean eliminado = productos.remove(
                new Producto(2, "Mouse", 50.00));

        System.out.println("¿Se eliminó el código 2? " + eliminado);
        mostrar(productos);

        titulo("18. removeIf(): eliminar según una condición");

        boolean huboCambios = productos.removeIf(
                producto -> producto.getPrecio() > 600.00);

        System.out.println(
                "¿Se eliminaron productos de más de S/ 600? " + huboCambios);
        mostrar(productos);

        titulo("19. clear(): eliminar todos los productos");

        productos.clear();

        System.out.println("Cantidad: " + productos.size());
        System.out.println("¿Está vacío? " + productos.isEmpty());

        titulo("20. Comprobar que la copia conserva sus elementos");
        mostrar(copia);
    }

    private static Producto buscarPorCodigo(
            Set<Producto> productos,
            int codigo) {
        for (Producto producto : productos) {
            if (producto.getCodigo() == codigo) {
                return producto;
            }
        }

        return null;
    }

    private static void titulo(String texto) {
        System.out.println("\n--------------------------------------------------");
        System.out.println(texto);
        System.out.println("--------------------------------------------------");
    }

    private static void mostrar(Set<Producto> productos) {
        if (productos.isEmpty()) {
            System.out.println("(Conjunto vacío)");
            return;
        }

        for (Producto producto : productos) {
            System.out.println(producto);
        }

        System.out.println("Total: " + productos.size());
    }
}