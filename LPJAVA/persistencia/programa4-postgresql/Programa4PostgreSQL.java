import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Scanner;

/**
 * PROGRAMA 4 - Persistencia en PostgreSQL con JDBC (Java 21)
 *
 * Gestiona productos (crear tabla, añadir, listar, modificar y eliminar)
 * guardándolos en la tabla "producto" de una base de datos PostgreSQL.
 *
 * Ejecución (Java 21, sin compilar previamente):
 *     Windows : java -cp lib\postgresql-42.7.4.jar Programa4PostgreSQL.java
 *     Linux   : java -cp lib/postgresql-42.7.4.jar Programa4PostgreSQL.java
 */
public class Programa4PostgreSQL {

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
            if (codigo.length() > 20)
                throw new IllegalArgumentException("El código admite como máximo 20 caracteres.");
            if (nombre == null || nombre.isBlank())
                throw new IllegalArgumentException("El nombre no puede estar vacío.");
            if (nombre.strip().length() > 100)
                throw new IllegalArgumentException("El nombre admite como máximo 100 caracteres.");
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
    // 2. PERSISTENCIA: acceso a PostgreSQL con JDBC
    // =====================================================================
    /**
     * Datos de conexión. Se leen de variables de entorno para no "quemar" la
     * contraseña en el código fuente; si no existen se usan valores por defecto.
     *
     *   PG_URL      (defecto: jdbc:postgresql://localhost:5432/inventario)
     *   PG_USER     (defecto: postgres)
     *   PG_PASSWORD (defecto: postgres)
     */
    record ConfigBD(String url, String usuario, String clave) {
        static ConfigBD desdeEntorno() {
            return new ConfigBD(
                    System.getenv().getOrDefault("PG_URL", "jdbc:postgresql://localhost:5432/inventario"),
                    System.getenv().getOrDefault("PG_USER", "postgres"),
                    System.getenv().getOrDefault("PG_PASSWORD", "postgres"));
        }
    }

    static class RepositorioPostgres {

        /** DDL de la tabla. "total" es una COLUMNA GENERADA: la calcula PostgreSQL. */
        private static final String SQL_CREAR_TABLA = """
                CREATE TABLE IF NOT EXISTS producto (
                    codigo   VARCHAR(20)   PRIMARY KEY
                             CHECK (codigo ~ '^\\S+$'),
                    nombre   VARCHAR(100)  NOT NULL
                             CHECK (btrim(nombre) <> ''),
                    cantidad INTEGER       NOT NULL DEFAULT 0
                             CHECK (cantidad >= 0),
                    precio   NUMERIC(12,2) NOT NULL DEFAULT 0.00
                             CHECK (precio >= 0),
                    total    NUMERIC(14,2) GENERATED ALWAYS AS (cantidad * precio) STORED
                )
                """;
        private static final String SQL_BORRAR_TABLA = "DROP TABLE IF EXISTS producto";
        private static final String SQL_EXISTE_TABLA = "SELECT to_regclass('public.producto') IS NOT NULL";
        private static final String SQL_INSERTAR =
                "INSERT INTO producto (codigo, nombre, cantidad, precio) VALUES (?, ?, ?, ?)";
        private static final String SQL_LISTAR =
                "SELECT codigo, nombre, cantidad, precio, total FROM producto ORDER BY codigo";
        private static final String SQL_BUSCAR =
                "SELECT codigo, nombre, cantidad, precio, total FROM producto WHERE codigo = ?";
        private static final String SQL_MODIFICAR =
                "UPDATE producto SET nombre = ?, cantidad = ?, precio = ? WHERE codigo = ?";
        private static final String SQL_ELIMINAR = "DELETE FROM producto WHERE codigo = ?";
        private static final String SQL_TOTAL_INVENTARIO = "SELECT COALESCE(SUM(total), 0) FROM producto";

        /** Código SQLSTATE estándar de "violación de clave única". */
        private static final String SQLSTATE_DUPLICADO = "23505";

        private final ConfigBD config;

        RepositorioPostgres(ConfigBD config) {
            this.config = config;
        }

        /** Abre una conexión nueva. Siempre se usa dentro de try-with-resources. */
        private Connection conectar() throws SQLException {
            return DriverManager.getConnection(config.url(), config.usuario(), config.clave());
        }

        boolean existeTabla() {
            try (Connection cn = conectar();
                 Statement st = cn.createStatement();
                 ResultSet rs = st.executeQuery(SQL_EXISTE_TABLA)) {
                return rs.next() && rs.getBoolean(1);
            } catch (SQLException e) {
                throw new ErrorBD("No se pudo verificar la tabla", e);
            }
        }

        /**
         * CREACIÓN: crea la tabla. Si recrear = true, primero la elimina.
         * Ambas sentencias van en UNA transacción: o se hacen las dos o ninguna.
         */
        void crearTabla(boolean recrear) {
            try (Connection cn = conectar()) {
                cn.setAutoCommit(false);
                try (Statement st = cn.createStatement()) {
                    if (recrear) st.executeUpdate(SQL_BORRAR_TABLA);
                    st.executeUpdate(SQL_CREAR_TABLA);
                    cn.commit();
                } catch (SQLException e) {
                    cn.rollback();
                    throw e;
                }
            } catch (SQLException e) {
                throw new ErrorBD("No se pudo crear la tabla", e);
            }
        }

        /** AÑADIR: INSERT con PreparedStatement (evita inyección SQL). */
        void insertar(Producto p) {
            try (Connection cn = conectar();
                 PreparedStatement ps = cn.prepareStatement(SQL_INSERTAR)) {
                ps.setString(1, p.codigo());
                ps.setString(2, p.nombre());
                ps.setInt(3, p.cantidad());
                ps.setBigDecimal(4, aDinero(p.precio()));
                ps.executeUpdate();
            } catch (SQLException e) {
                if (SQLSTATE_DUPLICADO.equals(e.getSQLState()))
                    throw new IllegalStateException("Ya existe un producto con código " + p.codigo());
                throw new ErrorBD("No se pudo insertar", e);
            }
        }

        List<Producto> listar() {
            List<Producto> lista = new ArrayList<>();
            try (Connection cn = conectar();
                 PreparedStatement ps = cn.prepareStatement(SQL_LISTAR);
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) lista.add(desdeFila(rs));
            } catch (SQLException e) {
                throw new ErrorBD("No se pudo listar", e);
            }
            return lista;
        }

        Optional<Producto> buscar(String codigo) {
            try (Connection cn = conectar();
                 PreparedStatement ps = cn.prepareStatement(SQL_BUSCAR)) {
                ps.setString(1, codigo);
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next() ? Optional.of(desdeFila(rs)) : Optional.empty();
                }
            } catch (SQLException e) {
                throw new ErrorBD("No se pudo buscar", e);
            }
        }

        /** MODIFICAR: devuelve true si alguna fila fue actualizada. */
        boolean modificar(Producto p) {
            try (Connection cn = conectar();
                 PreparedStatement ps = cn.prepareStatement(SQL_MODIFICAR)) {
                ps.setString(1, p.nombre());
                ps.setInt(2, p.cantidad());
                ps.setBigDecimal(3, aDinero(p.precio()));
                ps.setString(4, p.codigo());
                return ps.executeUpdate() > 0;
            } catch (SQLException e) {
                throw new ErrorBD("No se pudo modificar", e);
            }
        }

        /** ELIMINAR: devuelve true si se eliminó alguna fila. */
        boolean eliminar(String codigo) {
            try (Connection cn = conectar();
                 PreparedStatement ps = cn.prepareStatement(SQL_ELIMINAR)) {
                ps.setString(1, codigo);
                return ps.executeUpdate() > 0;
            } catch (SQLException e) {
                throw new ErrorBD("No se pudo eliminar", e);
            }
        }

        /** Total del inventario calculado por la base de datos (SUM de la columna generada). */
        double totalInventario() {
            try (Connection cn = conectar();
                 Statement st = cn.createStatement();
                 ResultSet rs = st.executeQuery(SQL_TOTAL_INVENTARIO)) {
                rs.next();
                return rs.getBigDecimal(1).doubleValue();
            } catch (SQLException e) {
                throw new ErrorBD("No se pudo calcular el total", e);
            }
        }

        /** Fila del ResultSet -> Producto (el constructor del record vuelve a validar). */
        private static Producto desdeFila(ResultSet rs) throws SQLException {
            return new Producto(
                    rs.getString("codigo"),
                    rs.getString("nombre"),
                    rs.getInt("cantidad"),
                    rs.getBigDecimal("precio").doubleValue());
        }

        /** double -> BigDecimal con 2 decimales, coherente con NUMERIC(12,2). */
        private static BigDecimal aDinero(double valor) {
            return BigDecimal.valueOf(valor).setScale(2, RoundingMode.HALF_UP);
        }
    }

    /** Excepción propia que envuelve SQLException con un mensaje comprensible. */
    static class ErrorBD extends RuntimeException {
        private static final long serialVersionUID = 1L;

        ErrorBD(String mensaje, SQLException causa) {
            super(mensaje + " -> " + causa.getMessage() + " [SQLState " + causa.getSQLState() + "]", causa);
        }
    }

    // =====================================================================
    // 3. SERVICIO: operaciones CRUD (delegan en el repositorio)
    // =====================================================================
    static class ServicioProductos {
        private final RepositorioPostgres repo;

        ServicioProductos(RepositorioPostgres repo) {
            this.repo = repo;
        }

        boolean existeTabla() { return repo.existeTabla(); }

        void crearTabla(boolean recrear) { repo.crearTabla(recrear); }

        List<Producto> listar() { return repo.listar(); }

        double totalInventario() { return repo.totalInventario(); }

        Optional<Producto> buscar(String codigo) { return repo.buscar(codigo.toUpperCase()); }

        void anadir(Producto p) { repo.insertar(p); }

        void modificar(Producto p) {
            if (!repo.modificar(p))
                throw new IllegalStateException("No existe el producto " + p.codigo());
        }

        boolean eliminar(String codigo) { return repo.eliminar(codigo.toUpperCase()); }
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
    public static void main(String[] args) {
        var config = ConfigBD.desdeEntorno();
        var servicio = new ServicioProductos(new RepositorioPostgres(config));
        var consola = new Consola();

        System.out.println("=== PROGRAMA 4: Persistencia en PostgreSQL (JDBC) ===");
        System.out.println("Base de datos: " + config.url() + "  (usuario: " + config.usuario() + ")");

        try {
            boolean salir = false;
            while (!salir) {
                mostrarMenu();
                String opcion = consola.leerLinea("Opción: ");
                try {
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
                } catch (ErrorBD e) {
                    System.out.println("Error de base de datos: " + e.getMessage());
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
                1. Crear tabla (nueva / vacía)
                2. Añadir producto
                3. Listar productos
                4. Modificar producto
                5. Eliminar producto
                0. Salir
                --------------------------""");
    }

    private static void crear(ServicioProductos s, Consola c) {
        if (s.existeTabla()) {
            if (!c.confirmar("La tabla ya existe. ¿Eliminarla y crearla vacía?")) {
                System.out.println("Operación cancelada.");
                return;
            }
            s.crearTabla(true);
        } else {
            s.crearTabla(false);
        }
        System.out.println("Tabla 'producto' creada.");
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
        for (Producto p : lista) {
            System.out.printf("%-10s %-30s %10d %12.2f %14.2f%n",
                    p.codigo(), p.nombre(), p.cantidad(), p.precio(), p.total());
        }
        double granTotal = s.totalInventario();   // SUM(total) calculado por PostgreSQL
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
