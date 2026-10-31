import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;

public class clase41 {

    public static void main(String[] args) {

        ArrayList<Producto> productos = new ArrayList<>();

        titulo("1. isEmpty(): verificar si la lista está vacía");
        System.out.println("¿Está vacía? " + productos.isEmpty());

        titulo("2. add(producto): agregar al final");
        productos.add(new Producto(1, "Teclado", 85.00));
        productos.add(new Producto(2, "Mouse", 45.00));
        productos.add(new Producto(3, "Monitor", 650.00));
        mostrar(productos);

        titulo("3. add(indice, producto): insertar en una posición");
        productos.add(1, new Producto(4, "Parlantes", 120.00));
        mostrar(productos);

        titulo("4. size(): obtener la cantidad de elementos");
        System.out.println("Cantidad: " + productos.size());

        titulo("5. get(indice): obtener un producto");
        Producto primero = productos.get(0);
        System.out.println("Producto en la posición 0:");
        System.out.println(primero);

        titulo("6. set(indice, producto): reemplazar un producto");
        Producto anterior = productos.set(
                1,
                new Producto(5, "Audífonos", 95.00));
        System.out.println("Producto reemplazado: " + anterior);
        mostrar(productos);

        titulo("7. Modificar un atributo del producto obtenido con get()");
        productos.get(0).setPrecio(90.00);
        System.out.println(productos.get(0));

        titulo("8. contains(producto): comprobar si existe");
        Producto buscado = new Producto(2, "Mouse", 45.00);

        System.out.println(
                "¿Existe un producto con código 2? "
                        + productos.contains(buscado));

        titulo("9. indexOf(producto): buscar su posición");
        int posicion = productos.indexOf(buscado);

        if (posicion != -1) {
            System.out.println("Encontrado en el índice: " + posicion);
            System.out.println(productos.get(posicion));
        } else {
            System.out.println("Producto no encontrado.");
        }

        titulo("10. addAll(coleccion): agregar varios productos");
        productos.addAll(Arrays.asList(
                new Producto(6, "Impresora", 800.00),
                new Producto(7, "Webcam", 150.00)));
        mostrar(productos);

        titulo("11. forEach(): recorrer la lista");
        productos.forEach(producto -> System.out.println(producto));

        titulo("12. sort(): ordenar por precio, de menor a mayor");
        productos.sort(Comparator.comparingDouble(Producto::getPrecio));
        mostrar(productos);

        titulo("13. sort(): ordenar por nombre");
        productos.sort(Comparator.comparing(Producto::getNombre));
        mostrar(productos);

        titulo("14. remove(indice): eliminar por posición");
        Producto eliminado = productos.remove(0);
        System.out.println("Producto eliminado: " + eliminado);
        mostrar(productos);

        titulo("15. remove(objeto): eliminar por igualdad");
        boolean seElimino = productos.remove(
                new Producto(2, "Mouse", 45.00));
        System.out.println("¿Se eliminó el producto con código 2? " + seElimino);
        mostrar(productos);

        titulo("16. removeIf(): eliminar según una condición");
        boolean huboCambios = productos.removeIf(
                producto -> producto.getPrecio() > 600.00);
        System.out.println("¿Se eliminaron productos? " + huboCambios);
        mostrar(productos);

        titulo("17. toArray(): convertir la lista en un arreglo");
        Producto[] arreglo = productos.toArray(new Producto[0]);

        for (Producto producto : arreglo) {
            System.out.println(producto);
        }

        titulo("18. Crear una copia de la lista");
        ArrayList<Producto> copia = new ArrayList<>(productos);
        mostrar(copia);

        titulo("19. clear(): eliminar todos los elementos");
        productos.clear();
        System.out.println("Cantidad en la lista original: " + productos.size());
        System.out.println("¿Está vacía? " + productos.isEmpty());

        titulo("20. Comprobar que la copia conserva sus elementos");
        mostrar(copia);
    }

    private static void titulo(String texto) {
        System.out.println("\n--------------------------------------------------");
        System.out.println(texto);
        System.out.println("--------------------------------------------------");
    }

    private static void mostrar(ArrayList<Producto> lista) {
        if (lista.isEmpty()) {
            System.out.println("(Lista vacía)");
            return;
        }

        for (int i = 0; i < lista.size(); i++) {
            System.out.println("Índice " + i + " -> " + lista.get(i));
        }

        System.out.println("Total: " + lista.size());
    }
}