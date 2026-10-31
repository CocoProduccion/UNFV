import java.util.LinkedHashMap;
import java.util.Map;

public class clase43 {

    public static void main(String[] args) {

        Map<Integer, Productomap> productos = new LinkedHashMap<>();

        titulo("1. isEmpty(): verificar si el mapa está vacío");
        System.out.println("¿Está vacío? " + productos.isEmpty());

        titulo("2. put(): agregar productos");
        productos.put(1, new Productomap(1, "Teclado", 85.00));
        productos.put(2, new Productomap(2, "Mouse", 45.00));
        productos.put(3, new Productomap(3, "Monitor", 650.00));
        mostrar(productos);

        titulo("3. put(): reemplazar el valor de una clave existente");

        Productomap anterior = productos.put(
                2,
                new Productomap(2, "Mouse gamer", 75.00));

        System.out.println("Valor anterior: " + anterior);
        System.out.println("Valor actual: " + productos.get(2));
        System.out.println("La clave 2 no se duplica.");
        mostrar(productos);

        titulo("4. size(): obtener la cantidad de entradas");
        System.out.println("Cantidad: " + productos.size());

        titulo("5. get(): buscar un producto por su clave");

        Productomap encontrado = productos.get(1);

        if (encontrado != null) {
            System.out.println("Producto encontrado:");
            System.out.println(encontrado);
        } else {
            System.out.println("Producto no encontrado.");
        }

        System.out.println("Buscar la clave 99: " + productos.get(99));

        titulo("6. getOrDefault(): consultar con un valor alternativo");

        Productomap alternativo = new Productomap(
                0, "No encontrado", 0.00);

        Productomap resultado = productos.getOrDefault(99, alternativo);

        System.out.println(resultado);
        System.out.println(
                "¿Se insertó la clave 99? " + productos.containsKey(99));

        titulo("7. containsKey(): comprobar si existe una clave");

        System.out.println(
                "¿Existe la clave 2? " + productos.containsKey(2));

        System.out.println(
                "¿Existe la clave 99? " + productos.containsKey(99));

        titulo("8. containsValue(): comprobar si existe un valor");

        Productomap valorBuscado = new Productomap(3, "Monitor", 650.00);

        System.out.println(
                "¿Existe ese producto? "
                        + productos.containsValue(valorBuscado));

        titulo("9. putIfAbsent(): agregar solamente si falta la clave");

        Productomap existente = productos.putIfAbsent(
                2,
                new Productomap(2, "Otro mouse", 100.00));

        System.out.println("Valor que ya existía: " + existente);

        productos.putIfAbsent(
                4,
                new Productomap(4, "Parlantes", 120.00));

        mostrar(productos);

        titulo("10. replace(): reemplazar solamente si existe la clave");

        Productomap reemplazado = productos.replace(
                4,
                new Productomap(4, "Parlantes USB", 135.00));

        System.out.println("Valor reemplazado: " + reemplazado);

        Productomap noExistia = productos.replace(
                99,
                new Productomap(99, "Desconocido", 10.00));

        System.out.println("Reemplazar clave inexistente: " + noExistia);
        mostrar(productos);

        titulo("11. replace(clave, anterior, nuevo): reemplazo condicional");

        boolean actualizado = productos.replace(
                2,
                new Productomap(2, "Mouse gamer", 75.00),
                new Productomap(2, "Mouse gamer", 80.00));

        System.out.println("¿Se realizó el reemplazo? " + actualizado);
        mostrar(productos);

        titulo("12. putAll(): agregar las entradas de otro mapa");

        Map<Integer, Productomap> adicionales = new LinkedHashMap<>();

        adicionales.put(5, new Productomap(5, "Webcam", 150.00));
        adicionales.put(6, new Productomap(6, "Impresora", 800.00));

        productos.putAll(adicionales);
        mostrar(productos);

        titulo("13. keySet(): recorrer solamente las claves");

        for (Integer codigo : productos.keySet()) {
            System.out.println("Clave: " + codigo);
        }

        titulo("14. values(): recorrer solamente los productos");

        for (Productomap producto : productos.values()) {
            System.out.println(producto);
        }

        titulo("15. entrySet(): recorrer claves y valores");

        for (Map.Entry<Integer, Productomap> entrada : productos.entrySet()) {

            System.out.println(
                    "Clave: " + entrada.getKey()
                            + " -> " + entrada.getValue());
        }

        titulo("16. forEach(): recorrer usando una expresión lambda");

        productos.forEach((codigo, producto) -> System.out.println(
                "Clave: " + codigo + " -> " + producto));

        titulo("17. computeIfAbsent(): crear un producto si falta");

        Productomap creado = productos.computeIfAbsent(
                7,
                codigo -> new Productomap(codigo, "Disco SSD", 250.00));

        System.out.println("Producto obtenido: " + creado);
        mostrar(productos);

        titulo("18. computeIfPresent(): actualizar solamente si existe");

        productos.computeIfPresent(
                7,
                (codigo, producto) -> new Productomap(
                        codigo,
                        producto.getNombre(),
                        producto.getPrecio() + 20.00));

        System.out.println(productos.get(7));

        titulo("19. replaceAll(): aplicar un cambio a todos los valores");

        // Aplicar un descuento del 10 %.
        productos.replaceAll(
                (codigo, producto) -> new Productomap(
                        codigo,
                        producto.getNombre(),
                        producto.getPrecio() * 0.90));

        mostrar(productos);

        titulo("20. Crear una copia del mapa");

        Map<Integer, Productomap> copia = new LinkedHashMap<>(productos);
        mostrar(copia);

        titulo("21. remove(clave): eliminar por clave");

        Productomap eliminado = productos.remove(1);

        System.out.println("Producto eliminado: " + eliminado);
        mostrar(productos);

        titulo("22. remove(clave, valor): eliminación condicional");

        Productomap webcam = productos.get(5);

        boolean seElimino = productos.remove(5, webcam);

        System.out.println("¿Se eliminó la webcam? " + seElimino);
        mostrar(productos);

        titulo("23. entrySet().removeIf(): eliminar según una condición");

        // Map no tiene un método removeIf propio.
        // Aplicamos removeIf sobre su vista de entradas.
        boolean huboCambios = productos.entrySet().removeIf(
                entrada -> entrada.getValue().getPrecio() > 500.00);

        System.out.println(
                "¿Se eliminaron productos de más de S/ 500? " + huboCambios);

        mostrar(productos);

        titulo("24. clear(): eliminar todas las entradas");

        productos.clear();

        System.out.println("Cantidad: " + productos.size());
        System.out.println("¿Está vacío? " + productos.isEmpty());

        titulo("25. Comprobar que la copia conserva sus entradas");
        mostrar(copia);
    }

    private static void titulo(String texto) {
        System.out.println("\n--------------------------------------------------");
        System.out.println(texto);
        System.out.println("--------------------------------------------------");
    }

    private static void mostrar(Map<Integer, Productomap> productos) {
        if (productos.isEmpty()) {
            System.out.println("(Mapa vacío)");
            return;
        }

        productos.forEach((codigo, producto) -> System.out.println(
                "Clave: " + codigo + " -> " + producto));

        System.out.println("Total de entradas: " + productos.size());
    }
}