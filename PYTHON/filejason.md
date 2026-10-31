# Explicación de `filejason.py`

Programa de consola que administra un inventario de productos guardado en el archivo `producto.json`. Permite grabar, listar, modificar y eliminar registros. El archivo vive en la misma carpeta que el script.

La lógica del menú es la de `filetexto.py`: el código identifica al producto, el nombre admite espacios y comas, y el total lo calcula el programa. Lo que cambia es el almacenamiento. Aquí el inventario es una lista de objetos JSON, un formato de texto que ya distingue cadenas, enteros y decimales.

Para ejecutarlo, abra una terminal en esa carpeta y escriba:

```powershell
python filejason.py
```

El programa muestra un menú y espera una opción del `1` al `5`. Permanece abierto hasta que se elige **Salir**.

---

## Qué problema resuelve

Cada producto es un objeto con cinco datos:

| Campo | Tipo en JSON | Tipo en el programa | Rol |
| --- | --- | --- | --- |
| `codigo` | cadena | `str` | Identifica al producto. No se repite y no lleva espacios. |
| `Nombre` | cadena | `str` | Puede llevar espacios, comas y tildes. |
| `Cantidad` | número entero | `int` `>= 0` | Unidades en inventario. |
| `precio` | número | `float` `>= 0` | Precio unitario. |
| `total` | número | `float` | Lo calcula el programa: `Cantidad * precio`. El usuario no lo escribe. |

El código es la clave de búsqueda. Modificar y eliminar piden ese código y recorren la lista hasta encontrar el producto.

Un ejemplo de `producto.json` después de grabar dos productos:

```json
[
  {
    "codigo": "P001",
    "Nombre": "Arroz extra",
    "Cantidad": 10,
    "precio": 3.5,
    "total": 35.0
  },
  {
    "codigo": "A-2",
    "Nombre": "Leche, entera",
    "Cantidad": 2,
    "precio": 4.25,
    "total": 8.5
  }
]
```

El archivo entero es un solo valor: una lista. Cada elemento de la lista es un objeto, escrito entre llaves. Dentro del objeto, el nombre del campo y su valor van separados por dos puntos. Las comas separan campos y también separan un producto del siguiente. La coma que está dentro de `"Leche, entera"` no separa nada, porque va entre comillas y forma parte del nombre.

`json.dump` escribe el decimal con los dígitos necesarios. `3.5` queda como `3.5`, no como `3.50`. Al listar en pantalla, el formato `.2f` sí muestra dos decimales.

---

## Cómo está escrito un JSON

JSON (JavaScript Object Notation) es texto. Se puede abrir `producto.json` con el Bloc de notas y leer los productos. El módulo `json` de la biblioteca estándar convierte ese texto en listas y diccionarios de Python, y al revés.

Estas son las reglas que usa este archivo:

| En el archivo | En Python | Ejemplo |
| --- | --- | --- |
| Lista, entre `[` y `]` | `list` | la lista de productos |
| Objeto, entre `{` y `}` | `dict` | un producto |
| Texto entre comillas dobles | `str` | `"P001"`, `"Arroz extra"` |
| Número sin punto | `int` | `10` |
| Número con punto | `float` | `3.5` |

Otras reglas del formato, aunque este programa no grabe esos valores:

- Las comillas del texto son siempre dobles. `'P001'` no es JSON válido.
- Un objeto no puede llevar una coma después del último campo, ni una lista después del último elemento.
- El archivo no admite comentarios.
- `true`, `false` y `null` son los booleanos y el nulo. En Python serían `True`, `False` y `None`.

Las cadenas de este programa salen con tildes reales (`Ñoño`) porque al escribir se usa `ensure_ascii=False`. Sin ese argumento, `json` cambiaría la eñe por la secuencia `\u00f1`.

---

## Por qué cada cambio reescribe el archivo

En `producto.txt` se puede abrir el archivo en modo adición y pegar una línea al final. Un JSON válido es un único valor, cerrado con `]` . Pegar otro objeto después de ese corchete dejaría el archivo inválido:

```text
[ { "codigo": "P001", ... } ]
{ "codigo": "A-2", ... }
```

Por eso grabar, modificar y eliminar hacen el mismo recorrido:

1. Leen la lista completa.
2. La cambian en memoria: agregan, actualizan o quitan un diccionario.
3. Vuelven a escribir `producto.json` desde el primer `[` hasta el último `]`.

Grabar también reescribe. La palabra «adicionar» describe el efecto sobre el inventario, no un modo `"a"` del archivo.

---

## Estructura del programa

El archivo está dividido en cuatro bloques:

1. **Constante.** Dónde está `producto.json`.
2. **Lectura y escritura.** `leer_productos` y `reescribir_productos`.
3. **Validación.** `pedir_codigo`, `pedir_entero` y `pedir_float`.
4. **Operaciones y menú.** Grabar, listar, modificar, eliminar y `main`.

Mapa de llamadas:

```text
main
 ├── grabar_producto
 │    ├── leer_productos
 │    ├── pedir_codigo
 │    ├── pedir_entero
 │    ├── pedir_float
 │    └── reescribir_productos
 ├── listar_productos
 │    └── leer_productos
 ├── modificar_producto
 │    ├── leer_productos
 │    ├── pedir_codigo
 │    ├── pedir_entero
 │    ├── pedir_float
 │    └── reescribir_productos
 └── eliminar_producto
      ├── leer_productos
      ├── pedir_codigo
      └── reescribir_productos
```

Las tres operaciones que cambian datos terminan en `reescribir_productos`. Listar solo lee.

---

## La ruta del archivo

```python
ARCHIVO = Path(__file__).resolve().parent / "producto.json"
```

`Path` (del módulo `pathlib`) representa una ruta de forma que funciona igual en Windows y en Linux.

- `__file__` es la ruta del propio script.
- `.resolve()` la convierte en ruta absoluta, sin `..` ni enlaces.
- `.parent` es la carpeta donde está `filejason.py`.
- `/ "producto.json"` une esa carpeta con el nombre del archivo de datos.

Así, `producto.json` se crea junto al programa aunque la terminal se haya abierto en otra carpeta.

---

## Lectura: `leer_productos`

Devuelve una lista de diccionarios, uno por producto.

Si el archivo no existe, o existe pero mide 0 bytes, devuelve `[]`. El resto del programa trata esa lista vacía como «aún no hay productos» y no se cae al arrancar por primera vez. Un archivo vacío no es JSON válido: `json.load` fallaría al intentar leerlo, y por eso se descarta antes de abrirlo.

Si hay contenido, lo abre así:

```python
with ARCHIVO.open("r", encoding="utf-8") as f:
    datos = json.load(f)
```

| Argumento | Para qué sirve |
| --- | --- |
| `"r"` | Solo lectura. |
| `encoding="utf-8"` | Interpreta tildes, eñes y el resto del texto como Unicode. |
| `json.load` | Lee el archivo completo y lo convierte en un valor de Python. |
| `with` | Cierra el archivo al salir del bloque, también si ocurre un error. |

Después comprueba que ese valor sea una lista:

```python
if not isinstance(datos, list):
    raise ValueError("producto.json debe contener una lista de productos.")
```

Un objeto suelto, un número o un texto en la raíz del archivo no sirven como inventario. `isinstance` reconoce el tipo real del valor que devolvió `json.load`.

El bucle arma de nuevo cada producto con los tipos que el resto del programa espera:

```python
productos.append({
    "codigo": str(fila["codigo"]),
    "Nombre": str(fila["Nombre"]),
    "Cantidad": int(fila["Cantidad"]),
    "precio": float(fila["precio"]),
    "total": float(fila["total"]),
})
```

JSON ya entrega las cadenas como `str` y los números como `int` o `float`. Estas conversiones dejan el diccionario uniforme aunque el archivo se haya editado a mano. Un código guardado como número (`100`) pasa a `"100"`. Una cantidad `10` sigue siendo el entero `10`. Un precio escrito como `4` (entero) pasa a `4.0`. `int` y `float` también aceptan el número si alguien lo guardó entre comillas, por ejemplo `"10"`.

Si a un objeto le falta `codigo`, `Nombre`, `Cantidad`, `precio` o `total`, la expresión `fila["..."]` lanza `KeyError` y la lectura se detiene.

Ejemplo de lo que queda en memoria a partir del primer objeto del archivo:

```python
{
    "codigo": "P001",
    "Nombre": "Arroz extra",
    "Cantidad": 10,
    "precio": 3.5,
    "total": 35.0,
}
```

---

## Escritura: `reescribir_productos`

Recibe la lista ya modificada y vuelve a crear `producto.json` desde cero:

```python
with ARCHIVO.open("w", encoding="utf-8") as f:
    json.dump(productos, f, ensure_ascii=False, indent=2)
    f.write("\n")
```

El modo `"w"` vacía el archivo si ya existía y luego escribe la lista completa.

| Argumento | Efecto |
| --- | --- |
| `productos` | La lista de diccionarios que se va a convertir en JSON. |
| `f` | El archivo abierto donde se escribe. |
| `ensure_ascii=False` | Conserva `á`, `ñ` y el resto de caracteres Unicode. Con el valor por defecto, `json` los cambiaría por secuencias `\uXXXX`. |
| `indent=2` | Parte la lista en varias líneas y sangra cada nivel con dos espacios. Sin sangría, todo el inventario saldría en una sola línea. |
| `f.write("\n")` | Agrega un salto de línea al final. `json.dump` no lo pone, y muchas herramientas esperan que un archivo de texto termine así. |

El orden de las claves dentro de cada objeto es el orden en que se armó el diccionario: `codigo`, `Nombre`, `Cantidad`, `precio`, `total`. Python conserva ese orden al escribir.

`json.dump` escapa lo que rompería la cadena. Un nombre con comillas, como `Yogur "light"`, se guarda así:

```json
"Nombre": "Yogur \"light\""
```

Al leer, `json.load` devuelve otra vez `Yogur "light"`, sin la barra invertida.

Si se eliminan todos los productos, la lista que se escribe es `[]`. El archivo no queda vacío: queda un JSON válido que significa inventario sin registros. La próxima lectura obtiene esa lista vacía y el listado muestra `(sin productos registrados)`.

---

## Validación de lo que escribe el usuario

Las tres funciones de entrada repiten la pregunta hasta recibir un valor aceptable. Ninguna operación del menú continúa con un dato inválido.

JSON no impone un tamaño fijo de campo. Un código o un nombre pueden ser tan largos como el usuario los escriba. El control está en la forma del dato, no en una cantidad de bytes.

### `pedir_codigo`

Pide un texto, le quita espacios al inicio y al final con `strip()`, y lo acepta solo si queda algo y ningún carácter interno es un espacio (`isspace` cubre espacio, tabulador y saltos de línea).

Válidos: `P001`, `A-2`, `SKU_10`.  
Rechazados: cadena vacía, solo espacios, `P 001`.

El código se usa como identificador. Si pudiera contener espacios, sería más fácil confundirlo al escribirlo de nuevo en modificar o eliminar.

### `pedir_entero`

Pide la cantidad.

1. Si el usuario pulsa ENTER y se pasó un valor `actual`, devuelve ese valor sin cambiarlo. Eso se usa al modificar: ENTER conserva la cantidad que ya tenía el producto. Al grabar no se pasa `actual`, así que ENTER no sirve como respuesta y hay que escribir un número.
2. Intenta `int(texto)`. Si el texto no es un entero (`12.5`, `abc`, vacío al grabar), avisa y vuelve a preguntar.
3. Rechaza los negativos. El cero sí es válido: un producto puede quedar sin stock.

### `pedir_float`

Hace lo mismo para el precio, con `float`. Acepta `12.50` y también `12`. Rechaza negativos y textos que no sean un número decimal. ENTER durante una modificación conserva el precio actual.

El punto es el separador decimal, porque `float` de Python usa el punto. Una coma (`12,50`) se rechaza. En el archivo, ese precio queda como número JSON (`12.5`), no como texto.

---

## Operación 1: grabar

`grabar_producto` agrega un producto nuevo y conserva los que ya están.

Pasos:

1. Lee todo el archivo y arma la lista de códigos existentes.
2. Pide el código. Si ya está en esa lista, informa el duplicado y regresa al menú. El código identifica al producto, así que no puede repetirse.
3. Pide el nombre y le aplica `strip()`. Un nombre vacío o hecho solo de espacios se rechaza y la función termina. El nombre puede contener espacios interiores: `Arroz extra` es válido. También puede contener comas: `Leche, entera`.
4. Pide cantidad y precio con las funciones de validación.
5. Calcula `total = cantidad * precio`.
6. Agrega un diccionario al final de la lista que está en memoria, con `append`.
7. `reescribir_productos` graba la lista completa, incluida la fila nueva.
8. Muestra el código grabado y el detalle de la multiplicación.

La primera grabación crea `producto.json`. Las siguientes leen la lista anterior, le suman un elemento y reemplazan el archivo. El producto nuevo queda como último objeto de la lista.

---

## Operación 2: listar

`listar_productos` no modifica el archivo.

1. Lee los productos.
2. Si la lista está vacía, imprime `(sin productos registrados)` y vuelve al menú.
3. Imprime una cabecera alineada y, debajo, una línea por producto.
4. Al final suma todos los `total` y muestra el total general del inventario.

El alineado usa el formato de f-strings:

| Formato | Efecto en el listado |
| --- | --- |
| `<10` y `<22` | Texto alineado a la izquierda, en un ancho fijo. |
| `>9`, `>10`, `>12` | Números alineados a la derecha. |
| `.2f` | Precio y total con dos decimales. |

Un nombre de más de 22 caracteres se imprime completo y desplaza las columnas de esa fila. El ancho `22` es solo de presentación. En el JSON el nombre se guarda entero.

Listar muestra el `total` guardado. No lo vuelve a calcular.

---

## Operación 3: modificar

`modificar_producto` busca por código y reemplaza nombre, cantidad y precio. El código no se cambia: es la identidad del registro.

Pasos:

1. Lee la lista completa.
2. Pide el código. `next(...)` recorre la lista y devuelve el primer diccionario cuyo `codigo` coincide. Si no hay ninguno, `next` devuelve `None`, se avisa y se regresa al menú.
3. Muestra el nombre, la cantidad y el precio actuales.
4. Pide el nuevo nombre. Si el usuario pulsa ENTER, `strip()` deja `""` y la expresión `... or producto["Nombre"]` conserva el nombre anterior. Un nombre escrito reemplaza al anterior.
5. Pide cantidad y precio pasando los valores actuales, de modo que ENTER los conserve.
6. Actualiza el diccionario con `update` y recalcula `total`. El diccionario modificado es el mismo objeto que está dentro de la lista `productos`, así que la lista ya queda con el cambio.
7. `reescribir_productos` graba toda la lista. Los demás productos salen igual que estaban.

Si el usuario pulsa ENTER en el nombre, el anterior se mantiene. Si escribe solo espacios, `strip()` también lo convierte en vacío y se conserva el nombre anterior.

---

## Operación 4: eliminar

`eliminar_producto` quita un producto y conserva el resto.

Pasos:

1. Lee la lista y busca el código igual que al modificar.
2. Si no existe, avisa y regresa al menú.
3. Pide confirmación: `s` elimina y cualquier otra respuesta cancela. La respuesta se pasa a minúsculas, así `S` también confirma.
4. Arma una lista nueva con todos los productos cuyo código es distinto del elegido.
5. Reescribe el archivo con esa lista.

Quitar un objeto del medio de un JSON obliga a volver a escribir la lista: los corchetes, las comas y la sangría tienen que quedar coherentes. La lista filtrada ya no contiene ese código, y `json.dump` genera el archivo de nuevo.

Si se elimina el único producto, el archivo queda así:

```json
[]
```

La confirmación evita borrar un registro por un código mal escrito. Cancelar no toca el archivo.

---

## Menú: `main`

`main` es un ciclo infinito:

1. Muestra el título y las cinco opciones.
2. Lee la opción y le quita espacios alrededor, de modo que `2` también se acepta.
3. Según el texto ingresado llama a grabar, listar, modificar o eliminar.
4. La opción `5` imprime `Fin del programa.` y hace `break`.
5. Cualquier otro texto avisa que hay que usar un número del 1 al 5 y el menú se vuelve a mostrar.

Cada operación regresa a `main` cuando termina. El ciclo es lo que mantiene el programa vivo entre una grabación y un listado.

La última línea del script es:

```python
if __name__ == "__main__":
    main()
```

`__name__` vale `"__main__"` solo cuando el archivo se ejecuta directamente (`python filejason.py`). Si otro programa lo importara para reutilizar `leer_productos` u otra función, el menú no arrancaría solo.

---

## Recorrido de una sesión

Suponga que `producto.json` no existe.

**Grabar** con código `P001`, nombre `Arroz extra`, cantidad `10` y precio `3.5`. El programa crea el archivo con una lista de un objeto. El total guardado es `35.0`.

```json
[
  {
    "codigo": "P001",
    "Nombre": "Arroz extra",
    "Cantidad": 10,
    "precio": 3.5,
    "total": 35.0
  }
]
```

**Grabar** otra vez con código `P001`. Lo rechaza porque ese código ya está. El archivo no cambia.

**Grabar** `A-2`, `Leche, entera`, cantidad `2`, precio `4.25`. Lee la lista, le agrega el segundo diccionario en memoria y reescribe el archivo entero. El total de ese producto es `8.5`. El archivo queda como el ejemplo del inicio de este documento.

**Listar** muestra las dos filas y el total general `43.50`.

**Modificar** `P001`, ENTER en el nombre, cantidad `8`, ENTER en el precio. El nombre y el precio se conservan, la cantidad pasa a `8` y el total se recalcula a `28.0`. El archivo se reescribe con los dos objetos; `A-2` sale igual.

**Eliminar** `A-2` y responder `s`. Se reescribe el archivo con una lista de un solo objeto, el de `P001`.

**Salir** con la opción `5`. `producto.json` permanece en disco con lo último que se guardó.

---

## Decisiones que conviene tener presentes

**El total no se pide.** Siempre es `Cantidad * precio` en el momento de grabar o de modificar. Si alguien editara `producto.json` a mano y cambiara el precio sin cambiar el total, el listado mostraría el total viejo hasta la próxima modificación. Listar no lo recalcula: muestra lo que está guardado.

**Grabar, modificar y eliminar reescriben el archivo completo.** Un JSON es un solo valor. Para que la lista siga siendo válida hay que generar otra vez el `[`, todos los objetos y el `]`.

**La búsqueda es lineal.** Cada operación carga todos los productos y recorre la lista. Para un inventario de práctica es suficiente. El código duplicado se controla solo al grabar. El archivo no tiene un índice aparte.

**El archivo es texto legible.** Se puede abrir con un editor y ver los productos, con sangría de dos espacios. Hay que guardarlo en UTF-8, que es la codificación con la que el programa lo lee y lo escribe. Hay que respetar las comillas, las comas y los corchetes: una coma de más o una comilla sin cerrar hacen que `json.load` lance `JSONDecodeError` y esa operación no continúa.

**La raíz tiene que ser una lista.** Si el archivo contiene un objeto suelto u otro tipo de valor, `leer_productos` lanza `ValueError` con el mensaje `producto.json debe contener una lista de productos.` Si un objeto no trae uno de los cinco campos, la lectura falla con `KeyError`. El programa espera un JSON coherente con los diccionarios que él mismo escribe.
