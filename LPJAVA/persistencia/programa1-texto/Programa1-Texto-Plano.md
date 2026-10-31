# Programa 1: Persistencia en texto plano con Java 21

## Objetivo de la clase

Al terminar esta práctica el estudiante será capaz de:

- Modelar una entidad de negocio (`Producto`) con un `record` de Java 21 que valida sus propios datos.
- Guardar y recuperar información en un **archivo de texto plano delimitado** (`productos.txt`).
- Implementar las cuatro operaciones básicas de persistencia: **crear**, **añadir**, **modificar** y **eliminar** (más **listar** para verificar los resultados).
- Usar la API moderna de archivos `java.nio.file` (`Path`, `Files`) con codificación UTF-8.
- Proteger el archivo contra corrupción mediante la técnica de **escritura en archivo temporal + reemplazo atómico**.

## La estructura producto

| Campo | Tipo Java | Regla de validación | ¿Se ingresa? |
|---|---|---|---|
| `codigo` | `String` | No vacío, **sin espacios** (regex `\S+`). Se guarda en mayúsculas. Es la clave única. | Sí |
| `nombre` | `String` | No vacío, **puede contener espacios**. No puede contener `;` (es el separador del archivo). | Sí |
| `cantidad` | `int` | Entero `>= 0` | Sí |
| `precio` | `double` | Decimal `>= 0.00`, se redondea a 2 decimales | Sí |
| `total` | `double` | **Calculado**: `cantidad * precio` | No |

`total` es un **campo derivado**: nunca se le pide al usuario y, aunque se escribe en el archivo para que sea legible, al leerlo se vuelve a calcular. Así se evita que un archivo editado a mano tenga totales inconsistentes.

## Requisitos

| Elemento | Versión |
|---|---|
| JDK | 21 o superior (Temurin, Oracle u OpenJDK) |
| Bibliotecas externas | Ninguna |
| Sistema operativo | Windows 11 (también funciona en Linux/macOS) |

Verifique la instalación en PowerShell:

```powershell
java -version
```

Debe mostrar `21` o superior.

## Estructura del proyecto

```text
programa1-texto/
├── Programa1Texto.java          <- código fuente (un solo archivo)
├── Programa1-Texto-Plano.md     <- este documento
└── productos.txt                <- se crea al ejecutar la opción 1
```

## Formato del archivo productos.txt

Cada línea es un producto; los campos van separados por punto y coma. La primera línea, que empieza con `#`, es una cabecera informativa que el programa ignora al leer.

```text
#codigo;nombre;cantidad;precio;total
P002;Aceite vegetal 1 L;5;9.90;49.50
P003;Leche evaporada;24;3.80;91.20
```

Decisiones de diseño del formato:

- **Separador `;`**: permite que el nombre tenga espacios y comas. Por eso el nombre no puede contener `;`.
- **Punto decimal siempre** (`Locale.ROOT`): si se usara la configuración regional del equipo, en algunos países se escribiría `4,50`, lo que rompería la lectura en otra máquina.
- **UTF-8 explícito**: las tildes y la ñ se guardan igual en Windows y Linux.

## Arquitectura del programa

El programa está en un solo archivo, pero organizado en **capas** mediante clases anidadas estáticas, para que el estudiante identifique la responsabilidad de cada parte:

```text
┌──────────────────────────────────────────────┐
│ main + métodos del menú   (interfaz usuario) │
├──────────────────────────────────────────────┤
│ Consola              (lectura validada)      │
├──────────────────────────────────────────────┤
│ ServicioProductos    (reglas CRUD)           │
├──────────────────────────────────────────────┤
│ RepositorioTexto     (leer/escribir archivo) │
├──────────────────────────────────────────────┤
│ record Producto      (modelo + validación)   │
└──────────────────────────────────────────────┘
                     │
                     ▼
               productos.txt
```

Esta separación es la que permite que los programas 2, 3 y 4 **reutilicen casi todo el código** y solo cambien la capa de persistencia.

## Explicación detallada del código

### 1. El modelo: record Producto

```java
record Producto(String codigo, String nombre, int cantidad, double precio) {
    Producto {               // constructor compacto
        ... validaciones ...
        codigo = codigo.toUpperCase();
        nombre = nombre.strip();
        precio = Math.round(precio * 100.0) / 100.0;
    }
    double total() { return cantidad * precio; }
}
```

Puntos clave:

- Un `record` (estable desde Java 16) genera automáticamente el constructor, los métodos de acceso (`codigo()`, `nombre()`, etc.), `equals()`, `hashCode()` y `toString()`.
- Es **inmutable**: para "modificar" un producto se crea un objeto nuevo. Esto evita cambios accidentales.
- El **constructor compacto** valida todos los campos. Si un dato es inválido lanza `IllegalArgumentException`; por lo tanto, **es imposible que exista un `Producto` inválido en memoria**, venga del teclado o del archivo.
- `total()` es un método, no un campo: el total siempre es coherente con cantidad y precio.

### 2. La persistencia: RepositorioTexto

Es la única clase que conoce el archivo. Tiene cuatro responsabilidades:

| Método | Qué hace |
|---|---|
| `existe()` | `Files.exists(archivo)` |
| `crear()` | Escribe un archivo nuevo solo con la cabecera |
| `cargarTodos()` | Lee línea por línea y construye un `LinkedHashMap<String, Producto>` |
| `guardarTodos(...)` | Reescribe el archivo completo |

Lectura (`cargarTodos`):

1. Abre el archivo con `Files.newBufferedReader(archivo, StandardCharsets.UTF_8)` dentro de un **try-with-resources**, que cierra el archivo automáticamente aunque ocurra un error.
2. Ignora líneas vacías y las que empiezan con `#`.
3. Convierte cada línea con `desdeLinea()`: `linea.split(";", -1)` separa los campos. El `-1` conserva campos vacíos al final.
4. Si una línea está dañada, la ignora y muestra un aviso con el número de línea en lugar de detener el programa.
5. Usa `LinkedHashMap` porque permite buscar por código en tiempo constante y **conserva el orden** de inserción.

Escritura segura (`guardarTodos`):

```java
Path temporal = archivo.resolveSibling(archivo.getFileName() + ".tmp");
// 1) escribir todo en productos.txt.tmp
// 2) Files.move(temporal, archivo, REPLACE_EXISTING, ATOMIC_MOVE);
```

Si el programa se interrumpe mientras escribe, el archivo original queda intacto: solo se pierde el `.tmp`. Si el sistema de archivos no soporta movimiento atómico, se hace un movimiento normal.

Conversión de datos:

```java
// Producto -> texto
String.format(Locale.ROOT, "%.2f", p.precio())   // "4.50" siempre con punto
// texto -> Producto (el total del archivo se ignora y se recalcula)
new Producto(c[0], c[1], Integer.parseInt(c[2]), Double.parseDouble(c[3]));
```

### 3. El servicio: ServicioProductos

Implementa las reglas de negocio del CRUD. Como un archivo de texto no permite modificar una línea "en su lugar" de forma sencilla, se usa el patrón **leer todo, cambiar en memoria, guardar todo**:

| Operación | Pasos |
|---|---|
| Crear | `repo.crear()` deja el archivo vacío (solo cabecera) |
| Añadir | cargar mapa, verificar que el código **no** exista, `put`, guardar |
| Modificar | cargar mapa, verificar que el código **sí** exista, `put` (reemplaza), guardar |
| Eliminar | cargar mapa, `remove`, si se eliminó algo, guardar |

Este patrón es simple y correcto para archivos pequeños y medianos (miles de registros). Para volúmenes grandes o muchos usuarios simultáneos se usa una base de datos (programa 4).

### 4. La consola: Consola

Centraliza la lectura del teclado con `Scanner` y repite la pregunta hasta obtener un dato válido:

- `leerCodigo`: exige `\S+` (uno o más caracteres que no sean espacio).
- `leerNombre`: no vacío y sin `;`.
- `leerEnteroNoNegativo`: captura `NumberFormatException` y exige `>= 0`.
- `leerDecimalNoNegativo`: acepta `4,50` o `4.50` (reemplaza la coma por punto) y exige `>= 0.00`.
- `FinDeEntrada`: si se cierra la entrada estándar (Ctrl+Z en Windows), el programa termina limpiamente.

### 5. El programa principal

- Usa un **bloque de texto** (`"""`, Java 15+) para dibujar el menú.
- Usa un **switch con flechas** (`case "1" -> ...`, Java 14+), sin `break` y sin caídas accidentales entre casos.
- Captura `IllegalArgumentException` (dato inválido), `IllegalStateException` (código duplicado o inexistente) y `UncheckedIOException` (error de disco), de modo que un error no cierra el programa.
- Al **modificar**, el usuario puede presionar ENTER para conservar el valor actual de cada campo. El código no se modifica porque es la clave.
- Al **eliminar** y al **recrear** el archivo se pide confirmación.

## Recursos de Java 21 utilizados

| Recurso | Dónde se usa |
|---|---|
| `record` con constructor compacto | `Producto` |
| `var` (inferencia de tipos) | `main`, `anadir`, `modificar` |
| Bloques de texto `"""` | menú |
| `switch` con flechas | menú principal |
| `Optional<T>` | `buscar()` |
| try-with-resources | lectura y escritura de archivos |
| `String.strip()`, `isBlank()`, `repeat()` | validaciones e impresión |
| `java.nio.file` (`Path`, `Files`) | persistencia |

## Cómo ejecutar en Windows 11

Java 21 puede ejecutar un archivo fuente directamente, sin compilar antes:

```powershell
cd C:\clases\persistencia\programa1-texto
chcp 65001                       # consola en UTF-8 para ver tildes y ñ
java -Dstdout.encoding=UTF-8 Programa1Texto.java
```

Forma clásica (compilar y luego ejecutar):

```powershell
javac Programa1Texto.java
java Programa1Texto
```

## Ejemplo de sesión

```text
=== PROGRAMA 1: Persistencia en TEXTO PLANO ===
Archivo: C:\clases\persistencia\programa1-texto\productos.txt

---------- MENÚ ----------
1. Crear archivo (nuevo / vacío)
2. Añadir producto
3. Listar productos
4. Modificar producto
5. Eliminar producto
0. Salir
--------------------------
Opción: 1
Archivo creado: C:\clases\persistencia\programa1-texto\productos.txt

Opción: 2
Código (sin espacios): P001
Nombre: Arroz extra superior
Cantidad (>= 0): 10
Precio (>= 0.00): 4.50
Producto añadido. Total = 45.00

Opción: 2
Código (sin espacios): P 03
  Código inválido: no debe estar vacío ni tener espacios.
Código (sin espacios): P003
Nombre: Leche evaporada
Cantidad (>= 0): -2
  Ingrese un número entero mayor o igual a 0.
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

(Para abreviar, se omitió el menú que se repite después de cada operación.)

## Casos de prueba sugeridos

| N.º | Acción | Entrada | Resultado esperado |
|---|---|---|---|
| 1 | Añadir | código `A 1` | Rechaza: tiene espacio |
| 2 | Añadir | código ya existente | "Ya existe un producto con código ..." |
| 3 | Añadir | nombre `Pan; integral` | Rechaza: contiene `;` |
| 4 | Añadir | cantidad `-1` o `abc` | Vuelve a pedir la cantidad |
| 5 | Añadir | precio `3,5` | Acepta y guarda `3.50` |
| 6 | Modificar | ENTER en todos los campos | El producto queda igual |
| 7 | Modificar | código inexistente | "No existe el producto ..." |
| 8 | Eliminar | responder `n` | "Operación cancelada." |
| 9 | Archivo | editar a mano una línea con `abc` en cantidad | Aviso "línea N ignorada" y el resto carga bien |
| 10 | Archivo | editar el total a un valor falso | Al listar, el total se muestra recalculado |

## Ventajas y limitaciones del texto plano

| Ventajas | Limitaciones |
|---|---|
| Legible y editable con el Bloc de notas | Hay que reescribir todo el archivo para cambiar una línea |
| No requiere bibliotecas | El separador no puede aparecer en los datos (o hay que "escaparlo") |
| Fácil de importar en Excel o en otros sistemas | No hay tipos: todo es texto que hay que convertir |
| Portable entre sistemas operativos (con UTF-8) | Sin control de acceso concurrente ni transacciones |

## Ejercicios propuestos

1. Agregar la opción **6. Buscar por nombre** que muestre los productos cuyo nombre contenga un texto (sin distinguir mayúsculas).
2. Cambiar el separador a tabulador (`\t`) y explicar qué validación cambia.
3. Guardar en formato **CSV estándar**, permitiendo `;` en el nombre si se encierra entre comillas dobles.
4. Agregar una opción para **exportar** solo los productos con `cantidad == 0` a `sin_stock.txt`.
5. Reemplazar `double` por `BigDecimal` en el precio y discutir por qué es preferible para dinero.

## Preguntas de repaso

1. ¿Por qué `total` no se pide al usuario y se recalcula al leer el archivo?
2. ¿Qué problema evita escribir primero en `productos.txt.tmp`?
3. ¿Por qué se usa `Locale.ROOT` al formatear el precio?
4. ¿Qué ventaja da `LinkedHashMap` frente a `ArrayList` y frente a `HashMap` en este programa?
5. ¿Qué ocurriría si el nombre de un producto contuviera `;` y no se validara?

## Código fuente completo

```java
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
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
 * PROGRAMA 1 - Persistencia en TEXTO PLANO (Java 21)
 *
 * Gestiona productos (crear archivo, añadir, listar, modificar y eliminar)
 * guardándolos en un archivo de texto delimitado por punto y coma (;).
 *
 * Ejecución (Java 21, sin compilar previamente):
 *     java Programa1Texto.java
 */
public class Programa1Texto {

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
            if (nombre.contains(RepositorioTexto.SEPARADOR))
                throw new IllegalArgumentException("El nombre no puede contener el carácter '"
                        + RepositorioTexto.SEPARADOR + "'.");
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
    // 2. PERSISTENCIA: lectura / escritura del archivo de texto
    // =====================================================================
    static class RepositorioTexto {
        static final String SEPARADOR = ";";
        static final String CABECERA = "#codigo;nombre;cantidad;precio;total";

        private final Path archivo;

        RepositorioTexto(Path archivo) {
            this.archivo = archivo;
        }

        boolean existe() {
            return Files.exists(archivo);
        }

        /** CREACIÓN: crea (o reinicia) el archivo dejando solo la cabecera. */
        void crear() {
            guardarTodos(List.of());
        }

        /** Lee todo el archivo y lo devuelve como mapa codigo -> producto (mantiene el orden). */
        Map<String, Producto> cargarTodos() {
            Map<String, Producto> productos = new LinkedHashMap<>();
            if (!existe()) return productos;

            try (BufferedReader br = Files.newBufferedReader(archivo, StandardCharsets.UTF_8)) {
                String linea;
                int numLinea = 0;
                while ((linea = br.readLine()) != null) {
                    numLinea++;
                    if (linea.isBlank() || linea.startsWith("#")) continue; // cabecera o vacía
                    try {
                        Producto p = desdeLinea(linea);
                        productos.put(p.codigo(), p);
                    } catch (RuntimeException e) {
                        System.err.printf("Aviso: línea %d ignorada (%s)%n", numLinea, e.getMessage());
                    }
                }
            } catch (IOException e) {
                throw new UncheckedIOException("No se pudo leer " + archivo, e);
            }
            return productos;
        }

        /**
         * Reescribe el archivo completo. Primero escribe en un archivo temporal y luego
         * lo renombra: si el programa falla a mitad, el archivo original no se corrompe.
         */
        void guardarTodos(Iterable<Producto> productos) {
            Path temporal = archivo.resolveSibling(archivo.getFileName() + ".tmp");
            try (BufferedWriter bw = Files.newBufferedWriter(temporal, StandardCharsets.UTF_8)) {
                bw.write(CABECERA);
                bw.newLine();
                for (Producto p : productos) {
                    bw.write(aLinea(p));
                    bw.newLine();
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

        /** Producto -> "P001;Arroz extra;10;4.50;45.00" (Locale.ROOT => punto decimal). */
        private static String aLinea(Producto p) {
            return String.join(SEPARADOR,
                    p.codigo(),
                    p.nombre(),
                    String.valueOf(p.cantidad()),
                    String.format(Locale.ROOT, "%.2f", p.precio()),
                    String.format(Locale.ROOT, "%.2f", p.total()));
        }

        /** "P001;Arroz extra;10;4.50;45.00" -> Producto. El total se RECALCULA, no se confía en él. */
        private static Producto desdeLinea(String linea) {
            String[] c = linea.split(SEPARADOR, -1);
            if (c.length < 4) throw new IllegalArgumentException("faltan campos");
            return new Producto(c[0], c[1], Integer.parseInt(c[2].strip()),
                    Double.parseDouble(c[3].strip()));
        }
    }

    // =====================================================================
    // 3. SERVICIO: operaciones CRUD sobre el repositorio
    // =====================================================================
    static class ServicioProductos {
        private final RepositorioTexto repo;

        ServicioProductos(RepositorioTexto repo) {
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
                if (s.isBlank()) System.out.println("  El nombre no puede estar vacío.");
                else if (s.contains(RepositorioTexto.SEPARADOR))
                    System.out.println("  El nombre no puede contener ';'.");
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
    private static final Path ARCHIVO = Path.of("productos.txt");

    public static void main(String[] args) {
        var servicio = new ServicioProductos(new RepositorioTexto(ARCHIVO));
        var consola = new Consola();

        System.out.println("=== PROGRAMA 1: Persistencia en TEXTO PLANO ===");
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
