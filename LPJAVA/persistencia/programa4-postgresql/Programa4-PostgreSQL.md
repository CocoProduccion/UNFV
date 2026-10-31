# Programa 4: Persistencia en PostgreSQL con Java 21 y JDBC

## Objetivo de la clase

Al terminar esta práctica el estudiante será capaz de:

- Conectar un programa Java 21 a **PostgreSQL** mediante **JDBC** (Java Database Connectivity).
- Crear una tabla con **restricciones** (`PRIMARY KEY`, `CHECK`, `NOT NULL`) y una **columna generada** para el total.
- Ejecutar sentencias SQL seguras con `PreparedStatement` (protección contra **inyección SQL**).
- Implementar **crear** (`CREATE TABLE`), **añadir** (`INSERT`), **modificar** (`UPDATE`), **eliminar** (`DELETE`) y **listar** (`SELECT`).
- Usar **transacciones** (`commit` / `rollback`) y manejar errores por su código **SQLState**.
- Comparar la persistencia en archivos (programas 1 a 3) con la persistencia en un gestor de base de datos.

## La estructura producto

| Campo | Tipo Java | Tipo PostgreSQL | Restricción en la base de datos | ¿Se ingresa? |
|---|---|---|---|---|
| `codigo` | `String` | `VARCHAR(20)` | `PRIMARY KEY`, `CHECK (codigo ~ '^\S+$')` (sin espacios) | Sí |
| `nombre` | `String` | `VARCHAR(100)` | `NOT NULL`, `CHECK (btrim(nombre) <> '')` | Sí |
| `cantidad` | `int` | `INTEGER` | `NOT NULL`, `CHECK (cantidad >= 0)` | Sí |
| `precio` | `double` | `NUMERIC(12,2)` | `NOT NULL`, `CHECK (precio >= 0)` | Sí |
| `total` | `double` | `NUMERIC(14,2)` | `GENERATED ALWAYS AS (cantidad * precio) STORED` | No |

Las reglas se validan **dos veces**:

1. En Java (constructor del `record Producto` y clase `Consola`): da mensajes amigables al usuario.
2. En PostgreSQL (`CHECK`, `PRIMARY KEY`): protege los datos aunque otro programa, o alguien desde `psql`, intente insertar datos inválidos.

`total` es una **columna generada almacenada** (PostgreSQL 12+): la base de datos la calcula y la actualiza sola cada vez que cambia `cantidad` o `precio`. El programa nunca la escribe; si lo intentara, PostgreSQL daría error.

¿Por qué `NUMERIC(12,2)` y no `DOUBLE PRECISION`? Porque `NUMERIC` guarda decimales **exactos**, como se requiere para dinero. En Java se convierte el `double` a `BigDecimal` con 2 decimales antes de enviarlo.

## Requisitos

| Elemento | Versión |
|---|---|
| JDK | 21 o superior |
| PostgreSQL | 12 o superior (probado con PostgreSQL 18) |
| Driver JDBC | `postgresql-42.7.4.jar` (incluido en `lib/`) |
| Sistema operativo | Windows 11 (también Linux/macOS) |

Descarga del driver si hace falta: [PostgreSQL JDBC 42.7.4 en Maven Central](https://repo1.maven.org/maven2/org/postgresql/postgresql/42.7.4/postgresql-42.7.4.jar).

Dependencia Maven equivalente:

```xml
<dependency>
    <groupId>org.postgresql</groupId>
    <artifactId>postgresql</artifactId>
    <version>42.7.4</version>
</dependency>
```

## Estructura del proyecto

```text
programa4-postgresql/
├── Programa4PostgreSQL.java     <- código fuente
├── Programa4-PostgreSQL.md      <- este documento
├── schema.sql                   <- script SQL (base de datos + tabla + datos de prueba)
└── lib/
    └── postgresql-42.7.4.jar    <- driver JDBC
```

## Preparación de la base de datos

### Paso 1: crear la base de datos

Desde PowerShell, con el cliente `psql` de PostgreSQL:

```powershell
psql -U postgres -c "CREATE DATABASE inventario;"
```

También se puede ejecutar el script completo, que crea la base de datos, la tabla y dos productos de prueba:

```powershell
psql -U postgres -f schema.sql
```

O desde pgAdmin: clic derecho en Databases → Create → Database → `inventario`.

### Paso 2: la tabla

No es obligatorio crearla a mano: la **opción 1 del menú** la crea. El DDL que ejecuta el programa es:

```sql
CREATE TABLE IF NOT EXISTS producto (
    codigo   VARCHAR(20)   PRIMARY KEY
             CHECK (codigo ~ '^\S+$'),
    nombre   VARCHAR(100)  NOT NULL
             CHECK (btrim(nombre) <> ''),
    cantidad INTEGER       NOT NULL DEFAULT 0
             CHECK (cantidad >= 0),
    precio   NUMERIC(12,2) NOT NULL DEFAULT 0.00
             CHECK (precio >= 0),
    total    NUMERIC(14,2) GENERATED ALWAYS AS (cantidad * precio) STORED
);
```

`~ '^\S+$'` es una expresión regular de PostgreSQL: desde el inicio (`^`) hasta el final (`$`) solo hay uno o más caracteres que no son espacio (`\S+`).

### Paso 3: configurar la conexión

El programa lee tres **variables de entorno** para no escribir la contraseña en el código fuente:

| Variable | Valor por defecto |
|---|---|
| `PG_URL` | `jdbc:postgresql://localhost:5432/inventario` |
| `PG_USER` | `postgres` |
| `PG_PASSWORD` | `postgres` |

En PowerShell (solo para la sesión actual):

```powershell
$env:PG_URL      = "jdbc:postgresql://localhost:5432/inventario"
$env:PG_USER     = "postgres"
$env:PG_PASSWORD = "mi_clave"
```

Formato de la URL JDBC: `jdbc:postgresql://<servidor>:<puerto>/<base_de_datos>`. Si PostgreSQL corre en WSL o en un servidor Linux de la red, se cambia `localhost` por la IP correspondiente.

## Arquitectura del programa

```text
┌──────────────────────────────────────────────┐
│ main + métodos del menú   (interfaz usuario) │
├──────────────────────────────────────────────┤
│ Consola              (lectura validada)      │
├──────────────────────────────────────────────┤
│ ServicioProductos    (reglas CRUD)           │
├──────────────────────────────────────────────┤
│ RepositorioPostgres  (JDBC + SQL)            │
│ record ConfigBD      (URL, usuario, clave)   │
│ ErrorBD              (excepción propia)      │
├──────────────────────────────────────────────┤
│ record Producto      (modelo + validación)   │
└──────────────────────────────────────────────┘
                     │  JDBC (driver PostgreSQL)
                     ▼
        PostgreSQL → BD inventario → tabla producto
```

Diferencia fundamental con los programas 1 a 3: ya **no se lee y reescribe todo el archivo**. Cada operación envía **una sentencia SQL** que afecta solo a la fila correspondiente, y la base de datos garantiza integridad, concurrencia y durabilidad.

## Explicación detallada del código

### 1. El modelo: record Producto

```java
record Producto(String codigo, String nombre, int cantidad, double precio) {
    Producto { ... validaciones ... }
    double total() { return cantidad * precio; }
}
```

- Valida lo mismo que la tabla: código sin espacios y de máximo 20 caracteres, nombre no vacío de máximo 100, cantidad `>= 0`, precio `>= 0.00`.
- Normaliza: código en mayúsculas, nombre sin espacios sobrantes, precio a 2 decimales (igual que `NUMERIC(12,2)`), de modo que el total mostrado en Java coincide con el calculado por PostgreSQL.

### 2. La configuración: record ConfigBD

```java
record ConfigBD(String url, String usuario, String clave) {
    static ConfigBD desdeEntorno() {
        return new ConfigBD(
            System.getenv().getOrDefault("PG_URL", "jdbc:postgresql://localhost:5432/inventario"),
            ...);
    }
}
```

`System.getenv()` devuelve un `Map`; `getOrDefault` usa el valor por defecto si la variable no existe. Es una buena práctica: el mismo programa funciona en distintos equipos sin recompilar y la contraseña no queda en el código.

### 3. La persistencia: RepositorioPostgres

#### Conexión

```java
private Connection conectar() throws SQLException {
    return DriverManager.getConnection(config.url(), config.usuario(), config.clave());
}
```

Desde JDBC 4 no hace falta `Class.forName("org.postgresql.Driver")`: el driver se registra solo al estar en el classpath. Cada operación abre una conexión dentro de un **try-with-resources**, que cierra automáticamente `Connection`, `PreparedStatement` y `ResultSet`, en orden inverso, aunque ocurra un error. En aplicaciones grandes se usaría un **pool de conexiones** (por ejemplo, HikariCP).

#### Sentencias SQL como constantes

Todas las sentencias se declaran como constantes `private static final String`; el DDL usa un **bloque de texto** (`"""`) para conservar el formato legible del SQL.

#### Crear: transacción

```java
cn.setAutoCommit(false);
try (Statement st = cn.createStatement()) {
    if (recrear) st.executeUpdate("DROP TABLE IF EXISTS producto");
    st.executeUpdate(SQL_CREAR_TABLA);
    cn.commit();            // confirma ambas sentencias
} catch (SQLException e) {
    cn.rollback();          // deshace todo si algo falla
    throw e;
}
```

En PostgreSQL, a diferencia de otros gestores, el DDL (`CREATE`, `DROP`) **también es transaccional**: si el `CREATE` fallara, el `DROP` se deshace y no se pierde la tabla.

Para saber si la tabla ya existe se usa `SELECT to_regclass('public.producto') IS NOT NULL`, que devuelve `true` o `false` sin generar errores.

#### Añadir: INSERT con PreparedStatement

```java
INSERT INTO producto (codigo, nombre, cantidad, precio) VALUES (?, ?, ?, ?)

ps.setString(1, p.codigo());
ps.setString(2, p.nombre());
ps.setInt(3, p.cantidad());
ps.setBigDecimal(4, aDinero(p.precio()));
ps.executeUpdate();
```

- Los `?` son **parámetros**. El driver envía los valores separados del SQL, por lo que un nombre como `x'); DROP TABLE producto; --` se guarda como texto y **no** se ejecuta. Esto es la protección contra **inyección SQL**. Nunca se debe construir SQL concatenando cadenas con datos del usuario.
- No se envía `total`: lo calcula la base de datos.
- Si el código ya existe, PostgreSQL rechaza la fila por la `PRIMARY KEY` con **SQLState `23505`** (violación de unicidad). El programa detecta ese código y muestra "Ya existe un producto con código ...".

#### Listar y buscar: SELECT y ResultSet

```java
SELECT codigo, nombre, cantidad, precio, total FROM producto ORDER BY codigo

while (rs.next()) lista.add(desdeFila(rs));
```

`ResultSet` funciona como un cursor: `next()` avanza a la siguiente fila y devuelve `false` al terminar. `desdeFila` lee las columnas por nombre (`rs.getString("codigo")`, `rs.getBigDecimal("precio")`) y crea un `Producto`. `buscar()` devuelve un `Optional<Producto>`: vacío si no hay fila.

El **total del inventario** se pide a la base de datos con una función de agregación:

```sql
SELECT COALESCE(SUM(total), 0) FROM producto
```

`COALESCE` devuelve 0 cuando la tabla está vacía (en ese caso, `SUM` devolvería `NULL`).

#### Modificar: UPDATE

```java
UPDATE producto SET nombre = ?, cantidad = ?, precio = ? WHERE codigo = ?
return ps.executeUpdate() > 0;
```

`executeUpdate()` devuelve el **número de filas afectadas**. Si es 0, el código no existía y el servicio lanza "No existe el producto ...". El total se recalcula solo en PostgreSQL.

#### Eliminar: DELETE

```java
DELETE FROM producto WHERE codigo = ?
return ps.executeUpdate() > 0;
```

Mismo principio: 1 fila afectada significa que se eliminó; 0 significa que no existía.

#### Conversión double → BigDecimal

```java
BigDecimal.valueOf(valor).setScale(2, RoundingMode.HALF_UP);
```

`BigDecimal.valueOf(double)` usa la representación decimal "visible" del número (`9.9` → `9.9`), a diferencia de `new BigDecimal(9.9)`, que expone el error binario (`9.9000000000000003552...`).

#### Excepción propia ErrorBD

Envuelve la `SQLException` en una excepción no comprobada con un mensaje que incluye el **SQLState**, útil para diagnosticar:

| SQLState | Significado típico |
|---|---|
| `08001` | No se puede conectar (servidor apagado, puerto o IP incorrectos) |
| `28P01` | Contraseña incorrecta |
| `3D000` | La base de datos no existe |
| `42P01` | La tabla no existe (falta ejecutar la opción 1) |
| `23505` | Clave primaria duplicada |
| `23514` | Violación de una restricción `CHECK` |

### 4. El servicio: ServicioProductos

Delega cada operación en el repositorio y traduce resultados en reglas de negocio (por ejemplo, `modificar` lanza `IllegalStateException` si no se actualizó ninguna fila). Mantiene los mismos métodos que en los programas 1 a 3, por lo que el menú casi no cambia.

### 5. La consola y el menú

- `Consola` valida cada dato antes de enviarlo a la base de datos (acepta `3,80` o `3.80`).
- Menú con **bloque de texto** y **switch con flechas**; la opción 1 ahora es "Crear tabla".
- Si la tabla ya existe, la opción 1 pide confirmación antes de borrarla y recrearla.
- Modificar: ENTER conserva el valor actual. Eliminar pide confirmación.
- Los errores de base de datos (`ErrorBD`) se muestran sin cerrar el programa.

## Recursos de Java 21 utilizados

| Recurso | Dónde se usa |
|---|---|
| `record` | `Producto`, `ConfigBD` |
| Bloque de texto `"""` | DDL `CREATE TABLE` y menú |
| JDBC: `Connection`, `PreparedStatement`, `ResultSet` | repositorio |
| try-with-resources con varios recursos | cada operación SQL |
| Transacciones `setAutoCommit(false)`, `commit`, `rollback` | crear tabla |
| `BigDecimal` y `RoundingMode` | precio exacto |
| `Optional<T>` | `buscar()` |
| `switch` con flechas, `var` | menú |

## Cómo ejecutar en Windows 11

```powershell
cd C:\clases\persistencia\programa4-postgresql
chcp 65001
$env:PG_PASSWORD = "mi_clave"
java -Dstdout.encoding=UTF-8 -cp lib\postgresql-42.7.4.jar Programa4PostgreSQL.java
```

Forma clásica (en Windows el separador del classpath es `;`):

```powershell
javac -cp lib\postgresql-42.7.4.jar Programa4PostgreSQL.java
java -cp "lib\postgresql-42.7.4.jar;." Programa4PostgreSQL
```

En Linux o macOS: `java -cp "lib/postgresql-42.7.4.jar:." Programa4PostgreSQL`.

## Ejemplo de sesión

```text
=== PROGRAMA 4: Persistencia en PostgreSQL (JDBC) ===
Base de datos: jdbc:postgresql://localhost:5432/inventario  (usuario: postgres)

Opción: 1
Tabla 'producto' creada.

Opción: 2
Código (sin espacios): P001
Nombre: Arroz extra superior
Cantidad (>= 0): 10
Precio (>= 0.00): 4.50
Producto añadido. Total = 45.00

Opción: 2
Código (sin espacios): p001
Ya existe un producto con código P001

Opción: 2
Código (sin espacios): P003
Nombre: Leche evaporada
Cantidad (>= 0): 12
Precio (>= 0.00): 3,80
Producto añadido. Total = 45.60

Opción: 4
Código del producto a modificar: P003
Datos actuales: Producto[codigo=P003, nombre=Leche evaporada, cantidad=12, precio=3.8]
(Presione ENTER para conservar el valor actual)
Nombre [Leche evaporada]:
Cantidad [12]: 24
Precio [3.8]:
Producto modificado. Nuevo total = 91.20

Opción: 5
Código del producto a eliminar: P001
Se eliminará: Producto[codigo=P001, nombre=Arroz extra superior, cantidad=10, precio=4.5]
¿Confirma la eliminación? (s/n): s
Producto eliminado.

Opción: 3
CÓDIGO     NOMBRE                           CANTIDAD       PRECIO          TOTAL
--------------------------------------------------------------------------------
P003       Leche evaporada                        24         3.80          91.20
--------------------------------------------------------------------------------
           TOTAL INVENTARIO                                                91.20
```

(Se omitió el menú que se repite después de cada operación.)

Verificación desde `psql`:

```text
inventario=# SELECT * FROM producto;
 codigo |     nombre      | cantidad | precio | total
--------+-----------------+----------+--------+-------
 P003   | Leche evaporada |       24 |   3.80 | 91.20
(1 row)
```

## Casos de prueba sugeridos

| N.º | Acción | Entrada | Resultado esperado |
|---|---|---|---|
| 1 | Cualquiera | servidor PostgreSQL apagado | "Error de base de datos ... [SQLState 08001]" |
| 2 | Cualquiera | contraseña incorrecta | "... [SQLState 28P01]" |
| 3 | Listar | antes de crear la tabla | "... [SQLState 42P01]" |
| 4 | Añadir | código duplicado (también en minúsculas) | "Ya existe un producto con código ..." (se verifica antes de pedir el resto; si otro usuario lo inserta en ese intervalo, lo detecta el SQLState `23505`) |
| 5 | Añadir | nombre `x'); DROP TABLE producto; --` | Se guarda como texto; la tabla no se borra |
| 6 | Añadir | precio `2.555` | Se guarda `2.56` |
| 7 | psql | `INSERT INTO producto VALUES ('A 1','x',1,1);` | Error: viola `producto_codigo_check` |
| 8 | psql | `UPDATE producto SET cantidad = -1;` | Error: viola `producto_cantidad_check` |
| 9 | psql | `UPDATE producto SET total = 0;` | Error: la columna `total` es generada |
| 10 | Crear | opción 1 con tabla existente y respuesta `s` | La tabla queda vacía |

## Archivos frente a base de datos

| Característica | Texto / JSON / Binario | PostgreSQL |
|---|---|---|
| Modificar un registro | Reescribir todo el archivo | `UPDATE` de una fila |
| Búsqueda | Cargar todo en memoria | Índice de la clave primaria |
| Validación | Solo en el programa | En el programa y en la base de datos |
| Varios usuarios a la vez | Riesgo de pérdida de datos | Control de concurrencia integrado |
| Transacciones | No | Sí (`COMMIT` / `ROLLBACK`) |
| Campo calculado | Calculado en Java | Columna generada en la BD |
| Consultas e informes | Programarlos a mano | SQL (`SUM`, `WHERE`, `GROUP BY`), herramientas como BIRT |
| Instalación | Ninguna | Servidor PostgreSQL y driver JDBC |

## Ejercicios propuestos

1. Agregar la opción **Buscar por nombre** con `WHERE nombre ILIKE ?` y el parámetro `"%" + texto + "%"`.
2. Crear una tabla `movimiento` (entrada/salida) con clave foránea a `producto` y actualizar la cantidad dentro de una **transacción**.
3. Reemplazar `DriverManager` por un pool de conexiones **HikariCP**.
4. Agregar un **índice** sobre `nombre` y comparar el plan de ejecución con `EXPLAIN ANALYZE`.
5. Crear una **vista** `v_inventario_valorizado` con el total general y usarla desde un reporte.
6. Escribir un programa que **migre** los datos de `productos.json` (programa 2) a la tabla `producto`.

## Preguntas de repaso

1. ¿Qué ventaja tiene una columna generada frente a calcular el total en Java y guardarlo?
2. ¿Por qué `PreparedStatement` evita la inyección SQL y la concatenación de cadenas no?
3. ¿Qué devuelve `executeUpdate()` y cómo lo aprovecha el programa?
4. ¿Por qué se valida en Java y también en la base de datos?
5. ¿Por qué se usa `NUMERIC(12,2)` para el precio en lugar de `DOUBLE PRECISION`?
6. ¿Qué pasaría con el `DROP TABLE` si el `CREATE TABLE` fallara dentro de la transacción?

## Código fuente completo

```java
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
```

## Script SQL (schema.sql)

```sql
-- =====================================================================
-- Programa 4 - Script de base de datos (PostgreSQL 12 o superior)
-- Ejecutar con:  psql -U postgres -f schema.sql
-- =====================================================================

-- 1) Crear la base de datos (ejecutar conectado a la BD "postgres")
CREATE DATABASE inventario;

-- 2) Conectarse a la nueva base de datos
\c inventario

-- 3) Crear la tabla producto
CREATE TABLE IF NOT EXISTS producto (
    codigo   VARCHAR(20)   PRIMARY KEY
             CHECK (codigo ~ '^\S+$'),               -- sin espacios
    nombre   VARCHAR(100)  NOT NULL
             CHECK (btrim(nombre) <> ''),            -- no vacío
    cantidad INTEGER       NOT NULL DEFAULT 0
             CHECK (cantidad >= 0),                  -- entero >= 0
    precio   NUMERIC(12,2) NOT NULL DEFAULT 0.00
             CHECK (precio >= 0),                    -- >= 0.00
    total    NUMERIC(14,2) GENERATED ALWAYS AS (cantidad * precio) STORED
);

-- 4) Datos de prueba (opcional)
INSERT INTO producto (codigo, nombre, cantidad, precio) VALUES
    ('P001', 'Arroz extra superior', 10, 4.50),
    ('P002', 'Aceite vegetal 1 L',    5, 9.90)
ON CONFLICT (codigo) DO NOTHING;

-- 5) Consulta de verificación
SELECT codigo, nombre, cantidad, precio, total FROM producto ORDER BY codigo;
```
