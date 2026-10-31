import java.io.BufferedWriter;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Scanner;

/**
 * PROGRAMA 5 - Persistencia en CSV (Java 21)
 *
 * Gestiona productos (crear archivo, añadir, listar, modificar y eliminar)
 * guardándolos en productos.csv, con comas y comillas dobles.
 *
 * Ejecución (Java 21, sin compilar previamente):
 *     java Programa5Csv.java
 */
public class Programa5Csv {

    // =====================================================================
    // 1. MODELO: record Producto (inmutable, con validación)
    // =====================================================================
    /**
     * Un record genera automáticamente: constructor, getters (codigo(), nombre()...),
     * equals(), hashCode() y toString(). El "constructor compacto" valida los datos.
     */
    record Producto(String codigo, String nombre, int cantidad, double precio) {

        Producto {
            if (codigo == null || !codigo.matches("(?U)\\S+"))
                throw new IllegalArgumentException("El código no puede estar vacío ni contener espacios.");
            if (nombre == null || nombre.isBlank())
                throw new IllegalArgumentException("El nombre no puede estar vacío.");
            if (cantidad < 0)
                throw new IllegalArgumentException("La cantidad debe ser un entero >= 0.");
            if (Double.isNaN(precio) || Double.isInfinite(precio) || precio < 0.0)
                throw new IllegalArgumentException("El precio debe ser un número >= 0.00.");
            codigo = codigo.toUpperCase(Locale.ROOT);   // normalizamos el código
            nombre = nombre.strip();          // quitamos espacios sobrantes
            precio = BigDecimal.valueOf(precio)
                    .setScale(2, RoundingMode.HALF_UP).doubleValue();
            if (!Double.isFinite(cantidad * precio))
                throw new IllegalArgumentException("El total excede el rango de double.");
        }

        /** Campo calculado: NO se pide al usuario, se deriva de cantidad * precio. */
        double total() {
            return cantidad * precio;
        }
    }


    // =====================================================================
    // 2. PERSISTENCIA: CSV con comas, comillas dobles y campos multilínea
    // =====================================================================
    static class RepositorioCsv {
        static final List<String> CABECERA =
                List.of("codigo", "nombre", "cantidad", "precio", "total");
        private final Path archivo;

        RepositorioCsv(Path archivo) { this.archivo = archivo; }

        boolean existe() { return Files.exists(archivo); }

        /** Crear deja solo la cabecera; el menú confirma si ya hay archivo. */
        void crear() { guardarTodos(List.of()); }

        /**
         * No se omiten registros dañados: abortar evita perderlos en la
         * siguiente reescritura. La columna total se ignora y se recalcula.
         */
        Map<String, Producto> cargarTodos() {
            Map<String, Producto> datos = new LinkedHashMap<>();
            if (!existe()) return datos;
            try {
                String texto = Files.readString(archivo, StandardCharsets.UTF_8);
                // Se tolera el marcador BOM al comienzo de un archivo importado.
                if (texto.startsWith("\uFEFF")) texto = texto.substring(1);
                List<List<String>> filas = analizar(texto);
                if (filas.isEmpty() || !filas.getFirst().equals(CABECERA))
                    throw new IllegalStateException("Cabecera CSV ausente o incorrecta.");
                for (int i = 1; i < filas.size(); i++) {
                    List<String> c = filas.get(i);
                    try {
                        if (c.size() != 5)
                            throw new IllegalArgumentException("Se esperan 5 columnas.");
                        Producto p = new Producto(c.get(0), c.get(1),
                                Integer.parseInt(c.get(2)),
                                Double.parseDouble(c.get(3)));
                        if (datos.putIfAbsent(p.codigo(), p) != null)
                            throw new IllegalArgumentException("Código duplicado: " + p.codigo());
                    } catch (IllegalArgumentException e) {
                        throw new IllegalStateException("Registro CSV " + (i + 1)
                                + " inválido: " + e.getMessage(), e);
                    }
                }
                return datos;
            } catch (IOException e) {
                throw new UncheckedIOException("No se pudo leer " + archivo, e);
            }
        }

        /**
         * Guardar no significa añadir bytes al final: se reescribe el conjunto.
         * El temporal único se crea en la misma carpeta del archivo definitivo.
         */
        void guardarTodos(Iterable<Producto> productos) {
            Path temporal = null;
            try {
                Path destino = archivo.toAbsolutePath();
                temporal = Files.createTempFile(destino.getParent(), "productos-", ".tmp");
                try (BufferedWriter w = Files.newBufferedWriter(
                        temporal, StandardCharsets.UTF_8)) {
                    w.write(String.join(",", CABECERA) + "\r\n");
                    for (Producto p : productos) {
                        w.write(escapar(p.codigo()) + "," + escapar(p.nombre()) + ","
                                + p.cantidad() + ","
                                + String.format(Locale.ROOT, "%.2f", p.precio()) + ","
                                + String.format(Locale.ROOT, "%.2f", p.total()) + "\r\n");
                    }
                }
                try {
                    Files.move(temporal, destino, StandardCopyOption.REPLACE_EXISTING,
                            StandardCopyOption.ATOMIC_MOVE);
                } catch (AtomicMoveNotSupportedException e) {
                    // Este respaldo NO conserva la garantía de sustitución atómica.
                    Files.move(temporal, destino, StandardCopyOption.REPLACE_EXISTING);
                }
            } catch (IOException e) {
                throw new UncheckedIOException("No se pudo guardar " + archivo, e);
            } finally {
                if (temporal != null) {
                    try { Files.deleteIfExists(temporal); }
                    catch (IOException e) {
                        System.err.println("Aviso: no se pudo limpiar " + temporal);
                    }
                }
            }
        }

        /** Todo texto se entrecomilla; una comilla interior se duplica. */
        static String escapar(String valor) {
            return "\"" + valor.replace("\"", "\"\"") + "\"";
        }

        /**
         * Analizador por estados: una coma o un salto de línea solo separa
         * cuando está FUERA de un campo entrecomillado. No usar split(",").
         * Acepta CRLF o LF; no admite espacios después de una comilla de cierre.
         */
        static List<List<String>> analizar(String texto) {
            List<List<String>> filas = new ArrayList<>();
            List<String> fila = new ArrayList<>();
            StringBuilder campo = new StringBuilder();
            boolean entreComillas = false;
            boolean cerrado = false;
            boolean iniciado = false;

            for (int i = 0; i < texto.length(); i++) {
                char ch = texto.charAt(i);
                if (entreComillas) {
                    if (ch == '"') {
                        if (i + 1 < texto.length() && texto.charAt(i + 1) == '"') {
                            campo.append('"');
                            i++; // dos comillas en CSV representan una en el dato
                        } else {
                            entreComillas = false;
                            cerrado = true;
                        }
                    } else campo.append(ch); // conserva comas y saltos interiores
                } else if (ch == ',' || ch == '\r' || ch == '\n') {
                    fila.add(campo.toString());
                    campo.setLength(0);
                    iniciado = false;
                    cerrado = false;
                    if (ch != ',') {
                        filas.add(fila);
                        fila = new ArrayList<>();
                        if (ch == '\r' && i + 1 < texto.length()
                                && texto.charAt(i + 1) == '\n') i++;
                    }
                } else if (cerrado) {
                    throw new IllegalStateException(
                            "CSV inválido: carácter después de comilla de cierre.");
                } else if (ch == '"') {
                    if (iniciado)
                        throw new IllegalStateException(
                                "CSV inválido: comilla en campo sin entrecomillar.");
                    entreComillas = true;
                    iniciado = true;
                } else {
                    campo.append(ch);
                    iniciado = true;
                }
            }
            if (entreComillas)
                throw new IllegalStateException("CSV inválido: comillas sin cerrar.");
            // No crear un registro vacío ficticio después del último CRLF.
            if (iniciado || cerrado || !fila.isEmpty()) {
                fila.add(campo.toString());
                filas.add(fila);
            }
            return filas;
        }
    }

    // =====================================================================
    // 3. SERVICIO: operaciones CRUD sobre el repositorio
    // =====================================================================
    static class ServicioProductos {
        private final RepositorioCsv repo;

        ServicioProductos(RepositorioCsv repo) {
            this.repo = repo;
        }

        void crearArchivo() { repo.crear(); }

        boolean existeArchivo() { return repo.existe(); }

        List<Producto> listar() { return new ArrayList<>(repo.cargarTodos().values()); }

        Optional<Producto> buscar(String codigo) {
            return Optional.ofNullable(repo.cargarTodos().get(codigo.toUpperCase(Locale.ROOT)));
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
            if (datos.remove(codigo.toUpperCase(Locale.ROOT)) == null) return false;
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
                if (s.matches("(?U)\\S+")) return s.toUpperCase(Locale.ROOT);
                System.out.println("  Código inválido: no debe estar vacío ni tener espacios.");
            }
        }

        String leerNombre(String mensaje) {
            while (true) {
                String s = leerLinea(mensaje);
                if (s.isBlank()) System.out.println("  El nombre no puede estar vacío.");
                else return s;
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
    private static final Path ARCHIVO = Path.of("productos.csv");

    public static void main(String[] args) {
        var servicio = new ServicioProductos(new RepositorioCsv(ARCHIVO));
        var consola = new Consola();

        System.out.println("=== PROGRAMA 5: Persistencia en CSV ===");
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
