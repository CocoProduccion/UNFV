# Programa 3: Persistencia en archivo binario con Java 21

## Objetivo de la clase

Al terminar esta práctica el estudiante será capaz de:

- Diferenciar un **archivo de texto** (caracteres legibles) de un **archivo binario** (bytes que representan directamente tipos de datos).
- Escribir y leer tipos primitivos con `DataOutputStream` y `DataInputStream`.
- Diseñar un **formato binario propio** con cabecera, número mágico, versión y registros.
- Detectar archivos ajenos, de versión incorrecta o truncados.
- Implementar las operaciones **crear**, **añadir**, **modificar** y **eliminar** (más **listar**) sobre `productos.dat`.

## La estructura producto

| Campo | Tipo Java | Representación binaria | Regla de validación | ¿Se ingresa? |
|---|---|---|---|---|
| `codigo` | `String` | `writeUTF` (2 bytes de longitud + bytes) | No vacío, **sin espacios**, en mayúsculas, clave única | Sí |
| `nombre` | `String` | `writeUTF` | No vacío, **puede tener espacios** | Sí |
| `cantidad` | `int` | `writeInt` (4 bytes) | Entero `>= 0` | Sí |
| `precio` | `double` | `writeDouble` (8 bytes IEEE 754) | Decimal `>= 0.00`, a 2 decimales | Sí |
| `total` | `double` | `writeDouble` (8 bytes) | **Calculado**: `cantidad * precio` | No |

## Requisitos

| Elemento | Versión |
|---|---|
| JDK | 21 o superior |
| Bibliotecas externas | Ninguna |
| Sistema operativo | Windows 11 (también Linux/macOS) |

## Estructura del proyecto

```text
programa3-binario/
├── Programa3Binario.java    <- código fuente
├── Programa3-Binario.md     <- este documento
└── productos.dat            <- se crea al ejecutar la opción 1
```

## Diseño del formato binario productos.dat

```text
CABECERA (10 bytes)
┌──────────────┬─────────────┬──────────────┐
│ MAGIA (int)  │ VERSION     │ N (int)      │
│ 0x50524F44   │ (short) = 1 │ nº registros │
│ "PROD"       │             │              │
│ 4 bytes      │ 2 bytes     │ 4 bytes      │
└──────────────┴─────────────┴──────────────┘

REGISTRO (repetido N veces, longitud variable)
┌──────────────┬──────────────┬──────────┬──────────┬──────────┐
│ codigo (UTF) │ nombre (UTF) │ cantidad │ precio   │ total    │
│ 2 + n bytes  │ 2 + n bytes  │ int (4)  │ dbl (8)  │ dbl (8)  │
└──────────────┴──────────────┴──────────┴──────────┴──────────┘
```

| Elemento | Para qué sirve |
|---|---|
| **Número mágico** `0x50524F44` | Son los códigos ASCII de `P`, `R`, `O`, `D`. Permite rechazar un archivo que no sea de productos (por ejemplo, una imagen renombrada a `.dat`). Los formatos reales hacen lo mismo: un PDF empieza con `%PDF` y un PNG con `‰PNG`. |
| **Versión** | Si en el futuro se agrega un campo, el programa puede saber con qué formato fue escrito el archivo. |
| **N** | Indica cuántos registros leer; así se detecta un archivo truncado. |
| `writeUTF` | Guarda primero 2 bytes con la longitud del texto y luego los bytes en "UTF-8 modificado"; por eso los nombres pueden tener espacios, tildes y cualquier carácter. |

Todos los números se guardan en orden **big-endian** (el byte más significativo primero), que es el estándar de Java independientemente del procesador.

### Volcado hexadecimal real

Con los productos `P002 / Aceite vegetal 1 L / 5 / 9.90` y `X1 / Leche evaporada / 7 / 2.56`:

```text
00000000: 5052 4f44 0001 0000 0002 0004 5030 3032  PROD........P002
00000010: 0012 4163 6569 7465 2076 6567 6574 616c  ..Aceite vegetal
00000020: 2031 204c 0000 0005 4023 cccc cccc cccd   1 L....@#......
00000030: 4048 c000 0000 0000 0002 5831 000f 4c65  @H........X1..Le
00000040: 6368 6520 6576 6170 6f72 6164 6100 0000  che evaporada...
00000050: 0740 047a e147 ae14 7b40 31eb 851e b851  .@.z.G..{@1....Q
00000060: ec                                       .
```

Lectura del volcado byte a byte:

| Bytes | Significado |
|---|---|
| `50 52 4f 44` | Número mágico "PROD" |
| `00 01` | Versión 1 |
| `00 00 00 02` | N = 2 registros |
| `00 04` + `50 30 30 32` | Longitud 4 + "P002" |
| `00 12` + 18 bytes | Longitud 18 + "Aceite vegetal 1 L" |
| `00 00 00 05` | cantidad = 5 |
| `40 23 cc cc cc cc cc cd` | precio = 9.9 (IEEE 754) |
| `40 48 c0 00 00 00 00 00` | total = 49.5 |

Para ver el volcado en Windows 11 (PowerShell 7): `Format-Hex .\productos.dat`.

## Arquitectura del programa

```text
┌──────────────────────────────────────────────┐
│ main + métodos del menú   (interfaz usuario) │
├──────────────────────────────────────────────┤
│ Consola              (lectura validada)      │
├──────────────────────────────────────────────┤
│ ServicioProductos    (reglas CRUD)           │
├──────────────────────────────────────────────┤
│ RepositorioBinario   (DataInput/OutputStream)│
├──────────────────────────────────────────────┤
│ record Producto      (modelo + validación)   │
└──────────────────────────────────────────────┘
                     │
                     ▼
               productos.dat
```

## Explicación detallada del código

### 1. El modelo: record Producto

```java
record Producto(String codigo, String nombre, int cantidad, double precio) {
    Producto { ... validaciones y normalización ... }
    double total() { return cantidad * precio; }
}
```

- El `record` es inmutable y genera constructor, accesores, `equals`, `hashCode` y `toString`.
- El constructor compacto valida: código sin espacios, nombre no vacío, cantidad `>= 0`, precio `>= 0.00` y finito. También pasa el código a mayúsculas, limpia el nombre y redondea el precio a 2 decimales.
- Como la lectura del archivo también usa este constructor, un archivo con datos inválidos **nunca** produce objetos inválidos.

### 2. La persistencia: RepositorioBinario

Composición de flujos (patrón **Decorator**):

```java
new DataOutputStream(                 // sabe escribir int, double, UTF...
    new BufferedOutputStream(         // agrupa bytes en un búfer (rendimiento)
        Files.newOutputStream(temporal)))   // escribe bytes en el disco
```

Cada capa añade una capacidad. Sin `BufferedOutputStream`, cada `writeInt` sería una llamada al sistema operativo.

Escritura (`guardarTodos`):

```java
out.writeInt(MAGIA);
out.writeShort(VERSION);
out.writeInt(productos.size());
for (Producto p : productos) {
    out.writeUTF(p.codigo());
    out.writeUTF(p.nombre());
    out.writeInt(p.cantidad());
    out.writeDouble(p.precio());
    out.writeDouble(p.total());
}
```

Se escribe en `productos.dat.tmp` y luego se reemplaza el original con `Files.move(..., ATOMIC_MOVE)`: si el programa falla durante la escritura, el archivo anterior sigue intacto.

Lectura (`cargarTodos`):

```java
if (in.readInt() != MAGIA) throw ...;          // ¿es nuestro archivo?
short version = in.readShort();                // ¿versión conocida?
int n = in.readInt();
for (int i = 0; i < n; i++) {
    String codigo = in.readUTF();
    String nombre = in.readUTF();
    int cantidad  = in.readInt();
    double precio = in.readDouble();
    in.readDouble();                           // total: se lee y se descarta
    ...
}
```

Regla de oro de los archivos binarios: **se debe leer exactamente en el mismo orden y con los mismos tipos con que se escribió**. Un archivo binario no tiene "etiquetas"; si se lee un `int` donde se escribió un `double`, se obtienen valores sin sentido.

Manejo de errores:

| Situación | Excepción | Mensaje al usuario |
|---|---|---|
| El archivo no empieza con "PROD" | `IllegalStateException` | "... no es un archivo de productos." |
| Versión distinta de 1 | `IllegalStateException` | "Versión de archivo no soportada" |
| El archivo termina antes de lo esperado | `EOFException` | "... está truncado o dañado." |
| Error de disco | `UncheckedIOException` | "Error de E/S: ..." |

### 3. El servicio: ServicioProductos

Como los registros tienen **longitud variable** (los textos miden distinto), no se puede saltar directamente al registro *k*. Por eso se usa el patrón **leer todo, modificar en memoria, guardar todo**:

| Operación | Pasos |
|---|---|
| Crear | escribir solo la cabecera con N = 0 |
| Añadir | cargar, verificar que el código no exista, `put`, guardar |
| Modificar | cargar, verificar que exista, `put` (reemplaza), guardar |
| Eliminar | cargar, `remove`, guardar si se eliminó |

Se usa `LinkedHashMap<String, Producto>` para buscar por código rápidamente y conservar el orden.

### 4. La consola y el menú

- `Consola` repite cada pregunta hasta obtener un valor válido; acepta `3,80` y `3.80`.
- Menú con **bloque de texto** y **switch con flechas**.
- Modificar: ENTER conserva el valor actual; el código no se cambia.
- Eliminar y recrear piden confirmación.

## Recursos de Java 21 utilizados

| Recurso | Dónde se usa |
|---|---|
| `record` con constructor compacto | `Producto` |
| `DataInputStream` / `DataOutputStream` | persistencia |
| Flujos con búfer (patrón Decorator) | persistencia |
| Literales hexadecimales `0x50524F44` | número mágico |
| try-with-resources | apertura y cierre de flujos |
| Bloques de texto, `switch` con flechas, `var`, `Optional` | menú y servicio |

## Cómo ejecutar en Windows 11

```powershell
cd C:\clases\persistencia\programa3-binario
chcp 65001
java -Dstdout.encoding=UTF-8 Programa3Binario.java
```

Forma clásica:

```powershell
javac Programa3Binario.java
java Programa3Binario
```

## Ejemplo de sesión

```text
=== PROGRAMA 3: Persistencia en archivo BINARIO ===
Archivo: C:\clases\persistencia\programa3-binario\productos.dat

Opción: 1
Archivo creado: C:\clases\persistencia\programa3-binario\productos.dat

Opción: 2
Código (sin espacios): P002
Nombre: Aceite vegetal 1 L
Cantidad (>= 0): 5
Precio (>= 0.00): 9.90
Producto añadido. Total = 49.50

Opción: 2
Código (sin espacios): X1
Nombre: Leche evaporada
Cantidad (>= 0): 3
Precio (>= 0.00): 2.555
Producto añadido. Total = 7.68

Opción: 4
Código del producto a modificar: X1
Datos actuales: Producto[codigo=X1, nombre=Leche evaporada, cantidad=3, precio=2.56]
(Presione ENTER para conservar el valor actual)
Nombre [Leche evaporada]:
Cantidad [3]: 7
Precio [2.56]:
Producto modificado. Nuevo total = 17.92

Opción: 3
CÓDIGO     NOMBRE                           CANTIDAD       PRECIO          TOTAL
--------------------------------------------------------------------------------
P002       Aceite vegetal 1 L                      5         9.90          49.50
X1         Leche evaporada                         7         2.56          17.92
--------------------------------------------------------------------------------
           TOTAL INVENTARIO                                                67.42
```

(Se omitió el menú que se repite después de cada operación.)

## Casos de prueba sugeridos

| N.º | Acción | Entrada | Resultado esperado |
|---|---|---|---|
| 1 | Añadir | código con espacio | Se rechaza y se vuelve a pedir |
| 2 | Añadir | código duplicado | "Ya existe un producto con código ..." |
| 3 | Añadir | precio `2.555` | Se guarda `2.56` |
| 4 | Archivo | abrir `productos.dat` en el Bloc de notas | Se ven textos mezclados con símbolos: no es legible |
| 5 | Archivo | renombrar un `.txt` a `productos.dat` | "... no es un archivo de productos." |
| 6 | Archivo | cortar los últimos bytes del archivo | "... está truncado o dañado." |
| 7 | Tamaño | comparar con el `.txt` y el `.json` de los mismos datos | El `.json` es el más grande (repite los nombres de campo). El `.dat` puede ser incluso mayor que el `.txt` con números pequeños, porque cada `double` ocupa siempre 8 bytes; a cambio, no requiere convertir texto a número |

## Ventajas y limitaciones del binario

| Ventajas | Limitaciones |
|---|---|
| Tamaño fijo y predecible por tipo: un `int` ocupa siempre 4 bytes y un `double` 8 | No es legible ni editable a mano |
| Rápido: no hay conversión texto ↔ número | Lectura y escritura deben coincidir exactamente |
| Los tipos se conservan exactos (el `double` no pierde precisión) | Cambiar el formato exige manejar versiones |
| Detecta archivos ajenos con el número mágico | Difícil de intercambiar con otros lenguajes si no se documenta el formato |

### Alternativa: serialización de objetos

Java también permite `ObjectOutputStream.writeObject(lista)` si la clase implementa `Serializable`. Es más corto de programar, pero el formato depende de Java, es frágil ante cambios de la clase y tiene riesgos de seguridad al leer archivos no confiables. Por eso, en esta clase se diseña un formato propio con `DataOutputStream`.

## Ejercicios propuestos

1. Convertir a **registros de longitud fija** (código de 10 caracteres y nombre de 40, rellenados con espacios) y usar `RandomAccessFile` con `seek()` para modificar un registro sin reescribir el archivo.
2. Crear la **versión 2** del formato con un campo `categoria` y hacer que el programa lea también archivos de versión 1.
3. Agregar una **suma de verificación** (CRC32 de `java.util.zip`) al final del archivo para detectar corrupción.
4. Implementar la variante con `ObjectOutputStream` y comparar tamaño del archivo y código.
5. Escribir un pequeño programa que muestre el contenido de `productos.dat` byte por byte, explicando cada campo.

## Preguntas de repaso

1. ¿Qué diferencia hay entre `writeInt(25)` y escribir el texto `"25"`?
2. ¿Para qué sirven el número mágico y la versión?
3. ¿Por qué no se puede modificar directamente el registro 3 en este formato?
4. ¿Qué ocurre si se cambia el orden de `writeInt(cantidad)` y `writeDouble(precio)` solo en la escritura?
5. ¿Qué función cumple `BufferedOutputStream` en la cadena de flujos?

## Código fuente completo

```java
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.UncheckedIOException;
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
 * PROGRAMA 3 - Persistencia en archivo BINARIO (Java 21)
 *
 * Gestiona productos (crear archivo, añadir, listar, modificar y eliminar)
 * guardándolos en un archivo binario con DataOutputStream / DataInputStream.
 *
 * Ejecución (Java 21, sin compilar previamente):
 *     java Programa3Binario.java
 */
public class Programa3Binario {

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
    // 2. PERSISTENCIA: lectura / escritura del archivo BINARIO
    // =====================================================================
    /**
     * Formato del archivo productos.dat (todos los números en big-endian):
     *
     *   CABECERA
     *     int    MAGIA    = 0x50524F44  ("PROD" en ASCII) -> identifica el archivo
     *     short  VERSION  = 1                             -> permite evolucionar el formato
     *     int    N                                        -> cantidad de registros
     *   N REGISTROS
     *     UTF    codigo    (2 bytes de longitud + bytes en UTF-8 modificado)
     *     UTF    nombre
     *     int    cantidad  (4 bytes)
     *     double precio    (8 bytes, IEEE 754)
     *     double total     (8 bytes, solo informativo: al leer se recalcula)
     */
    static class RepositorioBinario {
        static final int MAGIA = 0x50524F44;    // 'P' 'R' 'O' 'D'
        static final short VERSION = 1;

        private final Path archivo;

        RepositorioBinario(Path archivo) {
            this.archivo = archivo;
        }

        boolean existe() {
            return Files.exists(archivo);
        }

        /** CREACIÓN: crea (o reinicia) el archivo solo con la cabecera y N = 0. */
        void crear() {
            guardarTodos(List.of());
        }

        Map<String, Producto> cargarTodos() {
            Map<String, Producto> productos = new LinkedHashMap<>();
            if (!existe()) return productos;

            try (DataInputStream in = new DataInputStream(
                    new BufferedInputStream(Files.newInputStream(archivo)))) {

                if (in.readInt() != MAGIA)
                    throw new IllegalStateException(archivo + " no es un archivo de productos.");
                short version = in.readShort();
                if (version != VERSION)
                    throw new IllegalStateException("Versión de archivo no soportada: " + version);

                int n = in.readInt();
                for (int i = 0; i < n; i++) {
                    // El orden de lectura DEBE ser idéntico al orden de escritura
                    String codigo = in.readUTF();
                    String nombre = in.readUTF();
                    int cantidad = in.readInt();
                    double precio = in.readDouble();
                    in.readDouble();                      // total guardado: se descarta
                    Producto p = new Producto(codigo, nombre, cantidad, precio);
                    productos.put(p.codigo(), p);
                }
            } catch (EOFException e) {
                throw new IllegalStateException("El archivo " + archivo + " está truncado o dañado.", e);
            } catch (IOException e) {
                throw new UncheckedIOException("No se pudo leer " + archivo, e);
            }
            return productos;
        }

        void guardarTodos(Collection<Producto> productos) {
            Path temporal = archivo.resolveSibling(archivo.getFileName() + ".tmp");
            try (DataOutputStream out = new DataOutputStream(
                    new BufferedOutputStream(Files.newOutputStream(temporal)))) {

                out.writeInt(MAGIA);
                out.writeShort(VERSION);
                out.writeInt(productos.size());
                for (Producto p : productos) {
                    out.writeUTF(p.codigo());
                    out.writeUTF(p.nombre());
                    out.writeInt(p.cantidad());
                    out.writeDouble(p.precio());
                    out.writeDouble(p.total());
                }
            } catch (IOException e) {
                throw new UncheckedIOException("No se pudo escribir " + temporal, e);
            }
            try {
                Files.move(temporal, archivo, StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.ATOMIC_MOVE);
            } catch (IOException e) {
                try {
                    Files.move(temporal, archivo, StandardCopyOption.REPLACE_EXISTING);
                } catch (IOException ex) {
                    throw new UncheckedIOException("No se pudo reemplazar " + archivo, ex);
                }
            }
        }
    }

    // =====================================================================
    // 3. SERVICIO: operaciones CRUD sobre el repositorio
    // =====================================================================
    static class ServicioProductos {
        private final RepositorioBinario repo;

        ServicioProductos(RepositorioBinario repo) {
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
    private static final Path ARCHIVO = Path.of("productos.dat");

    public static void main(String[] args) {
        var servicio = new ServicioProductos(new RepositorioBinario(ARCHIVO));
        var consola = new Consola();

        System.out.println("=== PROGRAMA 3: Persistencia en archivo BINARIO ===");
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
```
