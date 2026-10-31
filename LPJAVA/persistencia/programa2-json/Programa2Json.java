import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Scanner;

/**
 * PROGRAMA 2 - Persistencia en JSON (Java 21)
 *
 * Gestiona productos (crear archivo, añadir, listar, modificar y eliminar)
 * guardándolos en un archivo JSON mediante la biblioteca Gson.
 *
 * Ejecución (Java 21, sin compilar previamente):
 *     Windows : java -cp lib\gson-2.11.0.jar Programa2Json.java
 *     Linux   : java -cp lib/gson-2.11.0.jar Programa2Json.java
 */
public class Programa2Json {

    // =====================================================================
    // 1. MODELO: record Producto (inmutable, con validación)
    // =====================================================================
    /**
     * Un record genera automáticamente: constructor, getters (codigo(), nombre()...),
     * equals(), hashCode() y toString(). El "constructor compacto" valida los datos.
     */
    record Producto(String codigo, String nombre, int cantidad, double precio) {

        Producto {
            if (codigo == null || !codigo.matches("\\S+"))
                throw new IllegalArgumentException("El código no puede estar vacío ni contener espacios.");
            if (nombre == null || nombre.isBlank())
                throw new IllegalArgumentException("El nombre no puede estar vacío.");
            if (cantidad < 0)
                throw new IllegalArgumentException("La cantidad debe ser un entero >= 0.");
            if (Double.isNaN(precio) || Double.isInfinite(precio) || precio < 0.0)
                throw new IllegalArgumentException("El precio debe ser un número >= 0.00.");
            codigo = codigo.toUpperCase();   // normalizamos el código
            nombre = nombre.strip();          // quitamos espacios sobrantes
            precio = Math.round(precio * 100.0) / 100.0;   // dinero: 2 decimales
        }

        /** Campo calculado: NO se pide al usuario, se deriva de cantidad * precio. */
        double total() {
            return cantidad * precio;
        }
    }

    // =====================================================================
    // 2. PERSISTENCIA: lectura / escritura del archivo JSON (Gson)
    // =====================================================================
    /**
     * Objeto de transferencia (DTO) que define cómo se ve cada producto en el JSON.
     * Incluye "total" para que el archivo sea legible, pero al leerlo se RECALCULA.
     */
    record ProductoJson(String codigo, String nombre, int cantidad, double precio, double total) {

        static ProductoJson desde(Producto p) {
            double totalRedondeado = Math.round(p.total() * 100.0) / 100.0;
            return new ProductoJson(p.codigo(), p.nombre(), p.cantidad(), p.precio(), totalRedondeado);
        }

        Producto aProducto() {
            return new Producto(codigo, nombre, cantidad, precio); // valida en el constructor
        }
    }

    static class RepositorioJson {
        /** Gson configurado para generar JSON "bonito" (con sangrías) y sin escapar tildes. */
        private static final Gson GSON = new GsonBuilder()
                .setPrettyPrinting()
                .disableHtmlEscaping()
                .create();

        /** Tipo genérico List<ProductoJson>: necesario por el "borrado de tipos" de Java. */
        private static final Type TIPO_LISTA = new TypeToken<List<ProductoJson>>() { }.getType();

        private final Path archivo;

        RepositorioJson(Path archivo) {
            this.archivo = archivo;
        }

        boolean existe() {
            return Files.exists(archivo);
        }

        /** CREACIÓN: crea (o reinicia) el archivo con un arreglo vacío: [] */
        void crear() {
            guardarTodos(List.of());
        }

        /** Lee el arreglo JSON completo y lo convierte en mapa codigo -> producto. */
        Map<String, Producto> cargarTodos() {
            Map<String, Producto> productos = new LinkedHashMap<>();
            if (!existe()) return productos;

            try (Reader lector = Files.newBufferedReader(archivo, StandardCharsets.UTF_8)) {
                List<ProductoJson> lista = GSON.fromJson(lector, TIPO_LISTA);
                if (lista == null) return productos;          // archivo vacío
                int i = 0;
                for (ProductoJson pj : lista) {
                    i++;
                    try {
                        Producto p = pj.aProducto();
                        productos.put(p.codigo(), p);
                    } catch (RuntimeException e) {
                        System.err.printf("Aviso: elemento %d ignorado (%s)%n", i, e.getMessage());
                    }
                }
            } catch (JsonParseException e) {
                throw new IllegalStateException("El archivo " + archivo + " no es un JSON válido: "
                        + e.getMessage(), e);
            } catch (IOException e) {
                throw new UncheckedIOException("No se pudo leer " + archivo, e);
            }
            return productos;
        }

        /** Serializa toda la colección; escribe primero en .tmp y luego reemplaza el original. */
        void guardarTodos(Collection<Producto> productos) {
            List<ProductoJson> lista = productos.stream().map(ProductoJson::desde).toList();
            Path temporal = archivo.resolveSibling(archivo.getFileName() + ".tmp");
            try (Writer escritor = Files.newBufferedWriter(temporal, StandardCharsets.UTF_8)) {
                GSON.toJson(lista, TIPO_LISTA, escritor);
            } catch (IOException e) {
                throw new UncheckedIOException("No se pudo escribir " + temporal, e);
            }
            reemplazar(temporal, archivo);
        }

        private static void reemplazar(Path origen, Path destino) {
            try {
                Files.move(origen, destino, StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.ATOMIC_MOVE);
            } catch (IOException e) {
                try {
                    Files.move(origen, destino, StandardCopyOption.REPLACE_EXISTING);
                } catch (IOException ex) {
                    throw new UncheckedIOException("No se pudo reemplazar " + destino, ex);
                }
            }
        }
    }

    // =====================================================================
    // 3. SERVICIO: operaciones CRUD sobre el repositorio
    // =====================================================================
    static class ServicioProductos {
        private final RepositorioJson repo;

        ServicioProductos(RepositorioJson repo) {
            this.repo = repo;
        }

        void crearArchivo() { repo.crear(); }

        boolean existeArchivo() { return repo.existe(); }

        List<Producto> listar() { return new ArrayList<>(repo.cargarTodos().values()); }

        Optional<Producto> buscar(String codigo) {
            return Optional.ofNullable(repo.cargarTodos().get(codigo.toUpperCase()));
        }

        /** AÑADIR: falla si el código ya existe (clave única). */
        void anadir(Producto p) {
            Map<String, Producto> datos = repo.cargarTodos();
            if (datos.containsKey(p.codigo()))
                throw new IllegalStateException("Ya existe un producto con código " + p.codigo());
            datos.put(p.codigo(), p);
            repo.guardarTodos(datos.values());
        }

        /** MODIFICAR: reemplaza el producto con el mismo código. */
        void modificar(Producto p) {
            Map<String, Producto> datos = repo.cargarTodos();
            if (!datos.containsKey(p.codigo()))
                throw new IllegalStateException("No existe el producto " + p.codigo());
            datos.put(p.codigo(), p);   // put sobre clave existente = reemplazo, conserva orden
            repo.guardarTodos(datos.values());
        }

        /** ELIMINAR: devuelve true si se eliminó. */
        boolean eliminar(String codigo) {
            Map<String, Producto> datos = repo.cargarTodos();
            if (datos.remove(codigo.toUpperCase()) == null) return false;
            repo.guardarTodos(datos.values());
            return true;
        }
    }

    // =====================================================================
    // 4. CONSOLA: lectura validada de datos
    // =====================================================================
    static class Consola {
        /** Señal para terminar si se cierra la entrada estándar (Ctrl+D / Ctrl+Z). */
        static class FinDeEntrada extends RuntimeException {
            private static final long serialVersionUID = 1L;
        }

        private final Scanner sc = new Scanner(System.in, StandardCharsets.UTF_8);

        String leerLinea(String mensaje) {
            System.out.print(mensaje);
            if (!sc.hasNextLine()) throw new FinDeEntrada();
            return sc.nextLine().strip();
        }

        String leerCodigo(String mensaje) {
            while (true) {
                String s = leerLinea(mensaje);
                if (s.matches("\\S+")) return s.toUpperCase();
                System.out.println("  Código inválido: no debe estar vacío ni tener espacios.");
            }
        }

        String leerNombre(String mensaje) {
            while (true) {
                String s = leerLinea(mensaje);
                if (!s.isBlank()) return s;
                System.out.println("  El nombre no puede estar vacío.");
            }
        }

        int leerEnteroNoNegativo(String mensaje) {
            while (true) {
                String s = leerLinea(mensaje);
                try {
                    int n = Integer.parseInt(s);
                    if (n >= 0) return n;
                } catch (NumberFormatException ignored) { }
                System.out.println("  Ingrese un número entero mayor o igual a 0.");
            }
        }

        double leerDecimalNoNegativo(String mensaje) {
            while (true) {
                String s = leerLinea(mensaje).replace(',', '.'); // acepta 4,50 o 4.50
                try {
                    double d = Double.parseDouble(s);
                    if (d >= 0.0 && Double.isFinite(d)) return d;
                } catch (NumberFormatException ignored) { }
                System.out.println("  Ingrese un número decimal mayor o igual a 0.00.");
            }
        }

        boolean confirmar(String mensaje) {
            String s = leerLinea(mensaje + " (s/n): ");
            return s.equalsIgnoreCase("s") || s.equalsIgnoreCase("si") || s.equalsIgnoreCase("sí");
        }
    }

    // =====================================================================
    // 5. PROGRAMA PRINCIPAL (menú)
    // =====================================================================
    private static final Path ARCHIVO = Path.of("productos.json");

    public static void main(String[] args) {
        var servicio = new ServicioProductos(new RepositorioJson(ARCHIVO));
        var consola = new Consola();

        System.out.println("=== PROGRAMA 2: Persistencia en JSON ===");
        System.out.println("Archivo: " + ARCHIVO.toAbsolutePath());

        try {
            boolean salir = false;
            while (!salir) {
                mostrarMenu();
                String opcion = consola.leerLinea("Opción: ");
                try {
                    // switch con flechas (Java 14+): sin "break", sin caída accidental
                    switch (opcion) {
                        case "1" -> crear(servicio, consola);
                        case "2" -> anadir(servicio, consola);
                        case "3" -> listar(servicio);
                        case "4" -> modificar(servicio, consola);
                        case "5" -> eliminar(servicio, consola);
                        case "0" -> salir = true;
                        default -> System.out.println("Opción no válida.");
                    }
                } catch (IllegalArgumentException | IllegalStateException e) {
                    System.out.println("Error: " + e.getMessage());
                } catch (UncheckedIOException e) {
                    System.out.println("Error de E/S: " + e.getMessage());
                }
            }
        } catch (Consola.FinDeEntrada e) {
            System.out.println();
        }
        System.out.println("Fin del programa.");
    }

    private static void mostrarMenu() {
        System.out.println("""

                ---------- MENÚ ----------
                1. Crear archivo (nuevo / vacío)
                2. Añadir producto
                3. Listar productos
                4. Modificar producto
                5. Eliminar producto
                0. Salir
                --------------------------""");
    }

    private static void crear(ServicioProductos s, Consola c) {
        if (s.existeArchivo() && !c.confirmar("El archivo ya existe. ¿Borrar todo su contenido?")) {
            System.out.println("Operación cancelada.");
            return;
        }
        s.crearArchivo();
        System.out.println("Archivo creado: " + ARCHIVO.toAbsolutePath());
    }

    private static void anadir(ServicioProductos s, Consola c) {
        String codigo = c.leerCodigo("Código (sin espacios): ");
        if (s.buscar(codigo).isPresent()) {
            System.out.println("Ya existe un producto con código " + codigo);
            return;
        }
        String nombre = c.leerNombre("Nombre: ");
        int cantidad = c.leerEnteroNoNegativo("Cantidad (>= 0): ");
        double precio = c.leerDecimalNoNegativo("Precio (>= 0.00): ");
        var p = new Producto(codigo, nombre, cantidad, precio);
        s.anadir(p);
        System.out.printf("Producto añadido. Total = %.2f%n", p.total());
    }

    private static void listar(ServicioProductos s) {
        List<Producto> lista = s.listar();
        if (lista.isEmpty()) {
            System.out.println("No hay productos registrados.");
            return;
        }
        String formato = "%-10s %-30s %10s %12s %14s%n";
        System.out.printf(formato, "CÓDIGO", "NOMBRE", "CANTIDAD", "PRECIO", "TOTAL");
        System.out.println("-".repeat(80));
        double granTotal = 0;
        for (Producto p : lista) {
            System.out.printf("%-10s %-30s %10d %12.2f %14.2f%n",
                    p.codigo(), p.nombre(), p.cantidad(), p.precio(), p.total());
            granTotal += p.total();
        }
        System.out.println("-".repeat(80));
        System.out.printf("%-10s %-30s %10s %12s %14.2f%n", "", "TOTAL INVENTARIO", "", "", granTotal);
    }

    private static void modificar(ServicioProductos s, Consola c) {
        String codigo = c.leerCodigo("Código del producto a modificar: ");
        Optional<Producto> encontrado = s.buscar(codigo);
        if (encontrado.isEmpty()) {
            System.out.println("No existe el producto " + codigo);
            return;
        }
        Producto actual = encontrado.get();
        System.out.println("Datos actuales: " + actual);
        System.out.println("(Presione ENTER para conservar el valor actual)");

        String nombre = c.leerLinea("Nombre [" + actual.nombre() + "]: ");
        String cant = c.leerLinea("Cantidad [" + actual.cantidad() + "]: ");
        String prec = c.leerLinea("Precio [" + actual.precio() + "]: ").replace(',', '.');

        // Si el usuario deja vacío, se conserva el valor; si no, el record valida el nuevo valor.
        var nuevo = new Producto(
                actual.codigo(),
                nombre.isEmpty() ? actual.nombre() : nombre,
                cant.isEmpty() ? actual.cantidad() : parsearEntero(cant),
                prec.isEmpty() ? actual.precio() : parsearDecimal(prec));
        s.modificar(nuevo);
        System.out.printf("Producto modificado. Nuevo total = %.2f%n", nuevo.total());
    }

    private static void eliminar(ServicioProductos s, Consola c) {
        String codigo = c.leerCodigo("Código del producto a eliminar: ");
        Optional<Producto> p = s.buscar(codigo);
        if (p.isEmpty()) {
            System.out.println("No existe el producto " + codigo);
            return;
        }
        System.out.println("Se eliminará: " + p.get());
        if (c.confirmar("¿Confirma la eliminación?") && s.eliminar(codigo))
            System.out.println("Producto eliminado.");
        else
            System.out.println("Operación cancelada.");
    }

    private static int parsearEntero(String s) {
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("La cantidad debe ser un entero >= 0.");
        }
    }

    private static double parsearDecimal(String s) {
        try {
            return Double.parseDouble(s);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("El precio debe ser un número >= 0.00.");
        }
    }
}
