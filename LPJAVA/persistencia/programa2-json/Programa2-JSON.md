# Programa 2: Persistencia en JSON con Java 21 y Gson

## Objetivo de la clase

Al terminar esta práctica el estudiante será capaz de:

- Explicar qué es **JSON** y por qué es el formato estándar de intercambio de datos en la web y en las API REST.
- **Serializar** (objeto Java → JSON) y **deserializar** (JSON → objeto Java) una colección de productos con la biblioteca **Gson**.
- Usar un `record` como **DTO** (objeto de transferencia de datos) para controlar exactamente qué campos se escriben en el archivo.
- Resolver el problema del **borrado de tipos** de los genéricos con `TypeToken`.
- Implementar las operaciones **crear**, **añadir**, **modificar** y **eliminar** (más **listar**) sobre `productos.json`.

## La estructura producto

| Campo | Tipo Java | Tipo JSON | Regla de validación | ¿Se ingresa? |
|---|---|---|---|---|
| `codigo` | `String` | string | No vacío, **sin espacios** (`\S+`), se guarda en mayúsculas, clave única | Sí |
| `nombre` | `String` | string | No vacío, **puede tener espacios** y cualquier carácter | Sí |
| `cantidad` | `int` | number | Entero `>= 0` | Sí |
| `precio` | `double` | number | Decimal `>= 0.00`, redondeado a 2 decimales | Sí |
| `total` | `double` | number | **Calculado**: `cantidad * precio` | No |

A diferencia del texto plano, en JSON el nombre **sí puede contener `;`, comillas o comas**, porque Gson se encarga de "escapar" los caracteres especiales.

## Requisitos

| Elemento | Versión |
|---|---|
| JDK | 21 o superior |
| Biblioteca | Gson 2.11.0 (`gson-2.11.0.jar`, incluida en `lib/`) |
| Sistema operativo | Windows 11 (también Linux/macOS) |

Gson soporta `record` desde la versión 2.10. Si necesita descargar el JAR: [Gson 2.11.0 en Maven Central](https://repo1.maven.org/maven2/com/google/code/gson/gson/2.11.0/gson-2.11.0.jar).

Si trabaja con Maven, la dependencia es:

```xml
<dependency>
    <groupId>com.google.code.gson</groupId>
    <artifactId>gson</artifactId>
    <version>2.11.0</version>
</dependency>
```

## Estructura del proyecto

```text
programa2-json/
├── Programa2Json.java       <- código fuente
├── Programa2-JSON.md        <- este documento
├── lib/
│   └── gson-2.11.0.jar      <- biblioteca Gson
└── productos.json           <- se crea al ejecutar la opción 1
```

## Formato del archivo productos.json

El archivo contiene un **arreglo** de objetos; cada objeto es un producto:

```json
[
  {
    "codigo": "P002",
    "nombre": "Aceite vegetal 1 L",
    "cantidad": 5,
    "precio": 9.9,
    "total": 49.5
  },
  {
    "codigo": "P003",
    "nombre": "Leche evaporada",
    "cantidad": 24,
    "precio": 3.8,
    "total": 91.2
  }
]
```

Al crear el archivo (opción 1) su contenido es simplemente `[]`.

Conceptos de JSON que se ven en el archivo:

| Elemento | Sintaxis | Ejemplo |
|---|---|---|
| Objeto | `{ "clave": valor, ... }` | un producto |
| Arreglo | `[ valor, valor, ... ]` | la lista de productos |
| Texto | entre comillas dobles | `"P002"` |
| Número | sin comillas, punto decimal | `9.9` |
| Otros | `true`, `false`, `null` | (no se usan aquí) |

## Arquitectura del programa

```text
┌──────────────────────────────────────────────┐
│ main + métodos del menú   (interfaz usuario) │
├──────────────────────────────────────────────┤
│ Consola              (lectura validada)      │
├──────────────────────────────────────────────┤
│ ServicioProductos    (reglas CRUD)           │
├──────────────────────────────────────────────┤
│ RepositorioJson      (Gson: leer/escribir)   │
│ record ProductoJson  (DTO del archivo)       │
├──────────────────────────────────────────────┤
│ record Producto      (modelo + validación)   │
└──────────────────────────────────────────────┘
                     │
                     ▼
              productos.json
```

Comparado con el programa 1, **solo cambia la capa de persistencia**: el modelo, el servicio, la consola y el menú son los mismos. Esta es la ventaja de separar responsabilidades.

## Explicación detallada del código

### 1. El modelo: record Producto

```java
record Producto(String codigo, String nombre, int cantidad, double precio) {
    Producto { ... validaciones, normalización ... }
    double total() { return cantidad * precio; }
}
```

- El `record` genera constructor, accesores, `equals`, `hashCode` y `toString`.
- El **constructor compacto** valida cada campo y lanza `IllegalArgumentException` si algo es inválido. También normaliza: código en mayúsculas, nombre sin espacios sobrantes y precio a 2 decimales.
- `total()` es un método: el total nunca queda desactualizado.

### 2. El DTO: record ProductoJson

```java
record ProductoJson(String codigo, String nombre, int cantidad, double precio, double total) {
    static ProductoJson desde(Producto p) { ... }   // modelo -> DTO
    Producto aProducto() { return new Producto(codigo, nombre, cantidad, precio); } // DTO -> modelo
}
```

¿Por qué un segundo record? Gson serializa **campos**, no métodos. Como `total()` es un método de `Producto`, no aparecería en el JSON. El DTO tiene `total` como campo para que el archivo sea autoexplicativo; al leer, `aProducto()` **descarta el total del archivo** y crea un `Producto` que lo recalcula y vuelve a validar todo.

El total se redondea antes de escribirlo (`Math.round(total * 100) / 100.0`) para evitar valores como `0.30000000000000004`, propios de la aritmética binaria de `double`.

### 3. La persistencia: RepositorioJson

Configuración de Gson:

```java
private static final Gson GSON = new GsonBuilder()
        .setPrettyPrinting()      // JSON con sangrías, legible
        .disableHtmlEscaping()    // no convierte caracteres como = o & en \u003d
        .create();
```

El problema del borrado de tipos:

```java
private static final Type TIPO_LISTA = new TypeToken<List<ProductoJson>>() { }.getType();
```

En tiempo de ejecución Java "olvida" el tipo genérico: `List<ProductoJson>` es solo `List`. Si se llamara `GSON.fromJson(lector, List.class)`, Gson no sabría que cada elemento es un `ProductoJson` y crearía mapas genéricos. `TypeToken` (una clase anónima, por eso las llaves `{ }`) conserva esa información.

Lectura (`cargarTodos`):

1. Abre el archivo en UTF-8 con try-with-resources.
2. `GSON.fromJson(lector, TIPO_LISTA)` convierte todo el arreglo en `List<ProductoJson>`. Gson usa el **constructor canónico** del record.
3. Si el archivo está vacío, `fromJson` devuelve `null`; se trata como lista vacía.
4. Cada elemento se convierte a `Producto`; si uno es inválido (por ejemplo `"cantidad": -5`), se ignora con un aviso.
5. Si el archivo no es JSON válido (por ejemplo, falta una llave), Gson lanza `JsonParseException`, que se transforma en un mensaje claro.

Escritura (`guardarTodos`):

```java
List<ProductoJson> lista = productos.stream().map(ProductoJson::desde).toList();
GSON.toJson(lista, TIPO_LISTA, escritor);   // en productos.json.tmp
Files.move(temporal, archivo, REPLACE_EXISTING, ATOMIC_MOVE);
```

- `stream().map(...).toList()` (Java 16+) transforma cada `Producto` en su DTO.
- Se escribe primero en un archivo temporal y luego se reemplaza el original: si el programa falla a mitad, `productos.json` no queda corrupto.

### 4. El servicio: ServicioProductos

Igual que en texto plano, un archivo JSON se procesa como un **documento completo**: se lee todo, se modifica en memoria y se reescribe.

| Operación | Pasos |
|---|---|
| Crear | `repo.crear()` escribe `[]` |
| Añadir | cargar, verificar que el código no exista, `put`, guardar |
| Modificar | cargar, verificar que exista, `put` (reemplaza), guardar |
| Eliminar | cargar, `remove`, guardar si se eliminó |

Se usa `LinkedHashMap<String, Producto>`: búsqueda rápida por código y orden de inserción conservado.

### 5. La consola y el menú

- `Consola` repite cada pregunta hasta recibir un dato válido. Acepta precio con coma o punto (`3,80` o `3.80`).
- El menú usa un **bloque de texto** `"""` y un **switch con flechas**.
- Al modificar, ENTER conserva el valor actual; el código (clave) no se modifica.
- Eliminar y recrear el archivo piden confirmación.
- Los errores (`IllegalArgumentException`, `IllegalStateException`, `UncheckedIOException`) se muestran sin cerrar el programa.

## Recursos de Java 21 utilizados

| Recurso | Dónde se usa |
|---|---|
| `record` (modelo y DTO) | `Producto`, `ProductoJson` |
| Referencia a método `ProductoJson::desde` | `guardarTodos` |
| `Stream.toList()` | `guardarTodos` |
| Clase anónima para `TypeToken` | `TIPO_LISTA` |
| Bloques de texto, `switch` con flechas, `var` | menú |
| `Optional<T>` | `buscar()` |
| try-with-resources | lectura y escritura |

## Cómo ejecutar en Windows 11

Ejecución directa del archivo fuente, indicando el JAR en el classpath (`-cp`):

```powershell
cd C:\clases\persistencia\programa2-json
chcp 65001
java -Dstdout.encoding=UTF-8 -cp lib\gson-2.11.0.jar Programa2Json.java
```

Forma clásica (compilar y ejecutar). En Windows el separador de classpath es `;`:

```powershell
javac -cp lib\gson-2.11.0.jar Programa2Json.java
java -cp "lib\gson-2.11.0.jar;." Programa2Json
```

En Linux o macOS el separador es `:` → `java -cp "lib/gson-2.11.0.jar:." Programa2Json`.

## Ejemplo de sesión

```text
=== PROGRAMA 2: Persistencia en JSON ===
Archivo: C:\clases\persistencia\programa2-json\productos.json

Opción: 1
Archivo creado: C:\clases\persistencia\programa2-json\productos.json

Opción: 2
Código (sin espacios): P001
Nombre: Arroz extra superior
Cantidad (>= 0): 10
Precio (>= 0.00): 4.50
Producto añadido. Total = 45.00

Opción: 2
Código (sin espacios): P002
Nombre: Aceite vegetal 1 L
Cantidad (>= 0): 5
Precio (>= 0.00): 9.90
Producto añadido. Total = 49.50

Opción: 4
Código del producto a modificar: P002
Datos actuales: Producto[codigo=P002, nombre=Aceite vegetal 1 L, cantidad=5, precio=9.9]
(Presione ENTER para conservar el valor actual)
Nombre [Aceite vegetal 1 L]:
Cantidad [5]: 8
Precio [9.9]:
Producto modificado. Nuevo total = 79.20

Opción: 5
Código del producto a eliminar: P001
Se eliminará: Producto[codigo=P001, nombre=Arroz extra superior, cantidad=10, precio=4.5]
¿Confirma la eliminación? (s/n): s
Producto eliminado.

Opción: 3
CÓDIGO     NOMBRE                           CANTIDAD       PRECIO          TOTAL
--------------------------------------------------------------------------------
P002       Aceite vegetal 1 L                      8         9.90          79.20
--------------------------------------------------------------------------------
           TOTAL INVENTARIO                                                79.20
```

(Se omitió el menú que se repite después de cada operación.)

## Casos de prueba sugeridos

| N.º | Acción | Entrada | Resultado esperado |
|---|---|---|---|
| 1 | Añadir | nombre `Aceite "Primor"; 1 L` | Se acepta; en el JSON aparece como `"Aceite \"Primor\"; 1 L"` |
| 2 | Añadir | código duplicado | "Ya existe un producto con código ..." |
| 3 | Añadir | cantidad `-1` | Vuelve a pedir la cantidad |
| 4 | Añadir | precio `3.333` | Se guarda `3.33` |
| 5 | Archivo | borrar una llave `}` del JSON a mano | "El archivo ... no es un JSON válido" |
| 6 | Archivo | cambiar `"cantidad": -5` a mano | Aviso "elemento N ignorado" |
| 7 | Archivo | cambiar el `total` a mano | Al listar se muestra el total recalculado |
| 8 | Archivo | dejar el archivo totalmente vacío | Se interpreta como lista vacía |

## Ventajas y limitaciones del JSON

| Ventajas | Limitaciones |
|---|---|
| Estándar de las API REST y de JavaScript | Hay que reescribir el documento completo para cada cambio |
| Legible y con estructura (objetos, arreglos, tipos básicos) | Más grande que el binario (repite los nombres de los campos) |
| Los datos pueden contener cualquier carácter | Requiere una biblioteca en Java (Gson, Jackson) |
| Soporta estructuras anidadas (por ejemplo, un producto con lista de proveedores) | Sin transacciones ni acceso concurrente seguro |

## Ejercicios propuestos

1. Envolver la lista en un objeto con metadatos: `{ "version": 1, "fecha": "...", "productos": [...] }`.
2. Añadir un campo `categoria` y comprobar que los archivos antiguos (sin ese campo) se siguen leyendo.
3. Reescribir el repositorio usando **Jackson** (`ObjectMapper`) y comparar el código.
4. Agregar la opción **Exportar a CSV** a partir del JSON.
5. Usar `@SerializedName("cod")` para que el JSON use nombres de campo diferentes a los de Java.

## Preguntas de repaso

1. ¿Qué es serializar y qué es deserializar?
2. ¿Por qué se necesita `TypeToken` para leer un `List<ProductoJson>`?
3. ¿Por qué se usa un DTO en lugar de serializar directamente `Producto`?
4. ¿Qué pasa con la validación del record cuando Gson lee un producto con cantidad negativa?
5. ¿Por qué en JSON el nombre puede contener `;` y en texto plano no?

## Código fuente completo

```java
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
```
