# Explicación de `filebinario.py`

Programa de consola que administra un inventario de productos guardado en el archivo binario `productos.bin`. Permite grabar, listar, modificar y eliminar registros. El archivo vive en la misma carpeta que el script.

La lógica del menú es la de `filetexto.py`: el código identifica al producto, el nombre admite espacios y el total lo calcula el programa. Lo que cambia es el almacenamiento. Aquí no hay líneas ni comas. Cada producto ocupa un bloque de **140 bytes** de longitud fija.

Para ejecutarlo, abra una terminal en esa carpeta y escriba:

```powershell
python filebinario.py
```

El programa muestra un menú y espera una opción del `1` al `5`. Permanece abierto hasta que se elige **Salir**.

El comentario inicial del script menciona `python gestion_productos_bin.py`. El archivo real se llama `filebinario.py`, así que el comando correcto es el de arriba.

---

## Qué problema resuelve

Cada producto es un registro con cinco datos:

| Campo | En memoria | En el archivo | Bytes |
| --- | --- | --- | --- |
| `codigo` | texto sin espacios | cadena de bytes, rellenada hasta 20 | 20 |
| `Nombre` | texto con espacios y tildes | cadena de bytes, rellenada hasta 100 | 100 |
| `Cantidad` | entero `>= 0` | entero con signo de 32 bits (`i`) | 4 |
| `precio` | decimal `>= 0` | flotante de doble precisión (`d`) | 8 |
| `total` | decimal | flotante de doble precisión (`d`) | 8 |

`20 + 100 + 4 + 8 + 8 = 140`. Todos los productos ocupan exactamente ese tamaño, esté el nombre corto o largo. Los bytes que el texto no usa se rellenan con el byte nulo `\x00`.

El total no se pide. Siempre es `Cantidad * precio` en el momento de grabar o de modificar.

El código es la clave de búsqueda. No puede repetirse y, en esta versión, además tiene que caber en 20 bytes. Modificar y eliminar piden ese código y recorren la lista hasta encontrar el producto.

`productos.bin` no se abre con el Bloc de notas para leerlo: los números están en binario, no escritos como dígitos. Hace falta el programa (o un visor hexadecimal) para interpretarlo.

---

## El formato del registro

El módulo `struct` empaqueta y desempaqueta esos 140 bytes. El formato está en una sola cadena:

```python
REGISTRO = struct.Struct("<20s100sidd")
```

`Struct` compila el formato una vez. Después, `REGISTRO.pack(...)` convierte un producto en bytes y `REGISTRO.unpack(...)` hace el camino inverso. `REGISTRO.size` vale `140`.

Cada pieza de `"<20s100sidd"` tiene un significado:

| Fragmento | Significado |
| --- | --- |
| `<` | Little-endian y tamaño estándar. El byte menos significativo de cada número va primero. No se insertan bytes de relleno entre campos. El mismo archivo se lee igual en Windows y en Linux. |
| `20s` | 20 bytes para el código. El tipo `s` trabaja con `bytes`, no con `str`. |
| `100s` | 100 bytes para el nombre. |
| `i` | Entero con signo de 4 bytes. Va de `-2 147 483 648` a `2 147 483 647`. El programa solo acepta de `0` a `2 147 483 647`. |
| `d` | Flotante de 8 bytes (doble precisión, el `float` de Python). El primero es el precio y el segundo es el total. |

Little-endian importa en los números, no en el texto. El entero `10` no se guarda como los caracteres `'1'` y `'0'`. Se guarda como cuatro bytes, con el `10` en el primero y ceros en los otros tres:

```text
0A 00 00 00
```

El decimal `3.5` ocupa ocho bytes en formato IEEE 754, también con el byte menos significativo al inicio:

```text
00 00 00 00 00 00 0C 40
```

El texto sí se guarda como los bytes de su codificación UTF-8, seguidos de ceros hasta completar el campo.

Un registro, visto de izquierda a derecha, queda así:

```text
|<------------ 20 bytes ----------->|<---------------- 100 bytes --------------->|<- 4 ->|<----- 8 ----->|<----- 8 ----->|
| codigo  +  \x00 \x00 ...          | Nombre  +  \x00 \x00 ...                    | cantidad | precio        | total         |
0                                  20                                           120        124             132             140
```

El archivo completo es la sucesión de esos bloques, sin encabezado y sin separador entre uno y otro:

```text
byte 0     registro 0
byte 140   registro 1
byte 280   registro 2
...
```

Como todos miden lo mismo, la posición del registro número `i` (contando desde cero) es siempre `i * 140`. Este programa no usa esa posición para saltar con `seek`: lee el archivo de principio a fin. La longitud fija igual es lo que permite saber dónde termina un producto y empieza el siguiente.

---

## Estructura del programa

El archivo está dividido en cinco bloques:

1. **Formato.** Ruta del archivo, `Struct` y los topes de código y nombre.
2. **Conversión.** `producto_a_bytes` y `bytes_a_producto`.
3. **Lectura y escritura.** `leer_productos` y `reescribir_productos`.
4. **Validación.** `pedir_codigo`, `pedir_texto`, `pedir_entero` y `pedir_float`.
5. **Operaciones y menú.** Grabar, listar, modificar, eliminar y `main`.

Mapa de llamadas:

```text
main
 ├── grabar_producto
 │    ├── leer_productos
 │    ├── pedir_codigo
 │    ├── pedir_texto
 │    ├── pedir_entero
 │    ├── pedir_float
 │    └── producto_a_bytes
 ├── listar_productos
 │    └── leer_productos
 ├── modificar_producto
 │    ├── leer_productos
 │    ├── pedir_codigo
 │    ├── pedir_texto
 │    ├── pedir_entero
 │    ├── pedir_float
 │    └── reescribir_productos
 └── eliminar_producto
      ├── leer_productos
      ├── pedir_codigo
      └── reescribir_productos
```

`leer_productos` convierte cada bloque con `bytes_a_producto`. `reescribir_productos` convierte cada producto con `producto_a_bytes`.

Grabar no reescribe el archivo. Abre `productos.bin` en modo adición binaria (`"ab"`) y agrega 140 bytes al final. Modificar y eliminar sí reescriben el archivo completo, porque hay que cambiar o quitar un bloque que puede estar en el medio.

---

## Constantes

```python
ARCHIVO = Path(__file__).resolve().parent / "productos.bin"
REGISTRO = struct.Struct("<20s100sidd")
MAX_CODIGO = 20
MAX_NOMBRE = 100
```

`Path` (del módulo `pathlib`) representa una ruta de forma que funciona igual en Windows y en Linux.

- `__file__` es la ruta del propio script.
- `.resolve()` la convierte en ruta absoluta.
- `.parent` es la carpeta donde está `filebinario.py`.
- `/ "productos.bin"` une esa carpeta con el nombre del archivo de datos.

Así, `productos.bin` se crea junto al programa aunque la terminal se haya abierto en otra carpeta.

`MAX_CODIGO` y `MAX_NOMBRE` coinciden con los `20` y `100` del formato. La validación los usa antes de empaquetar: si el texto, ya convertido a UTF-8, supera ese tope, `struct` lanzaría un error al intentar meterlo en el campo.

---

## De diccionario a bytes: `producto_a_bytes`

Recibe un producto y devuelve los 140 bytes de su registro:

```python
return REGISTRO.pack(
    p["codigo"].encode("utf-8"),
    p["Nombre"].encode("utf-8"),
    p["Cantidad"],
    p["precio"],
    p["total"],
)
```

El orden de los argumentos es el orden del formato: código, nombre, cantidad, precio, total.

`.encode("utf-8")` pasa de `str` a `bytes`. `pack` exige `bytes` en los campos `s`. Si el resultado mide menos que el campo, `struct` completa con `\x00` por la derecha. `"P001"` son 4 bytes y el campo mide 20, así que el código guardado es:

```text
50 30 30 31 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00
P  0  0  1  \0 ...
```

`50 30 30 31` es la codificación ASCII, y también UTF-8, de `P001`. Los 16 bytes que siguen son relleno.

Cantidad, precio y total entran como números. `pack` los escribe en los 4 y 8 bytes de `i` y `d`. No hace falta convertirlos a texto.

---

## De bytes a diccionario: `bytes_a_producto`

Hace el camino inverso con un bloque de exactamente 140 bytes:

```python
codigo_b, nombre_b, cantidad, precio, total = REGISTRO.unpack(raw)
```

`unpack` devuelve cinco valores. Los dos primeros siguen siendo `bytes`, todavía con el relleno de ceros. Cantidad llega como `int`. Precio y total llegan como `float`.

Para recuperar el texto:

```python
codigo_b.rstrip(b"\x00").decode("utf-8")
```

`rstrip(b"\x00")` quita los nulos del final y deja los bytes que sí forman el código o el nombre. `decode("utf-8")` los convierte otra vez en `str`. Los ceros de relleno no forman parte del texto: se agregaron al guardar y se retiran al leer.

El diccionario resultante tiene la misma forma que en la versión de texto:

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

## Por qué el límite se mide en bytes y no en letras

UTF-8 usa un byte para las letras sin tilde (`A`, `0`, espacio, coma) y dos o más para muchas letras del español:

| Texto | Caracteres | Bytes UTF-8 |
| --- | --- | --- |
| `P001` | 4 | 4 |
| `Arroz extra` | 11 | 11 |
| `Ñoño` | 4 | 6 (`Ñ` y `ñ` ocupan 2 cada una) |
| diez eñes | 10 | 20 |

Un código de 20 letras cabe solo si cada letra ocupa un byte. Diez eñes llenan justo los 20 bytes del campo. Once eñes ocupan 22 bytes y `pedir_codigo` los rechaza, con el aviso `El código admite hasta 20 bytes.`

El nombre tiene 100 bytes de cupo. El mensaje de `pedir_texto` lo recuerda: una palabra con tilde usa más bytes que letras. `len(texto)` contaría caracteres; `len(texto.encode("utf-8"))` cuenta lo que realmente se va a guardar. El programa usa la segunda medida.

---

## Lectura: `leer_productos`

Devuelve una lista de diccionarios, uno por producto. Si `productos.bin` todavía no existe, devuelve una lista vacía. El resto del programa trata esa lista como «aún no hay productos».

Si el archivo existe, lo abre en binario y lee de 140 en 140 bytes:

```python
with ARCHIVO.open("rb") as f:
    while True:
        raw = f.read(REGISTRO.size)
        if len(raw) < REGISTRO.size:
            break
        productos.append(bytes_a_producto(raw))
```

| Detalle | Para qué sirve |
| --- | --- |
| `"rb"` | Lectura binaria. Python no traduce saltos de línea ni aplica una codificación de texto al archivo entero. |
| `f.read(140)` | Pide el siguiente registro completo. |
| `len(raw) < 140` | No quedaban 140 bytes. Es el fin del archivo, o un resto incompleto. El ciclo se detiene y ese resto no se convierte en producto. |
| `with` | Cierra el archivo al salir del bloque, también si ocurre un error. |

No hay encabezado que saltar. El primer `read` ya es el primer producto. Un archivo vacío existe pero mide 0 bytes: el primer `read` devuelve `b""`, la condición de fin se cumple y la función regresa `[]`.

---

## Escritura completa: `reescribir_productos`

Recibe la lista ya modificada y vuelve a crear `productos.bin` desde cero:

```python
with ARCHIVO.open("wb") as f:
    for p in productos:
        f.write(producto_a_bytes(p))
```

El modo `"wb"` vacía el archivo si ya existía y luego escribe cada registro, uno detrás de otro, sin separadores. Modificar y eliminar arman primero la lista definitiva en memoria y solo entonces llaman a esta función.

Si después de eliminar no queda ningún producto, el ciclo no escribe nada. El archivo queda en disco con tamaño 0. La próxima lectura lo interpreta como inventario vacío.

---

## Validación de lo que escribe el usuario

Las funciones de entrada repiten la pregunta hasta recibir un valor que quepa en el registro. Ninguna operación del menú continúa con un dato inválido.

### `pedir_codigo`

Pide un texto, le quita espacios al inicio y al final con `strip()`, y lo acepta solo si se cumplen las tres condiciones:

1. Queda algún carácter.
2. Ningún carácter interno es un espacio (`isspace` cubre espacio, tabulador y saltos de línea).
3. Su UTF-8 mide como máximo 20 bytes.

Válidos: `P001`, `A-2`, `SKU_10`.  
Rechazados: cadena vacía, `P 001`, un código cuya codificación supere 20 bytes.

El código identifica al producto. Si pudiera contener espacios, sería más fácil confundirlo al escribirlo de nuevo en modificar o eliminar. El tope de bytes evita que `pack` falle al guardarlo.

Al modificar, el código solo sirve para buscar. Esta función no tiene un valor «actual» que se conserve con ENTER: hay que escribir el código que se busca.

### `pedir_texto`

Pide el nombre, que debe caber en `MAX_NOMBRE` (100 bytes).

1. Si el usuario pulsa ENTER y se pasó un valor `actual`, devuelve ese nombre sin cambiarlo. Eso ocurre al modificar.
2. Al grabar no se pasa `actual`. ENTER, o un texto que `strip()` deja vacío, se rechaza con `Este campo no puede estar vacío.`
3. Si el UTF-8 supera `max_bytes`, avisa y vuelve a preguntar.

El nombre sí puede llevar espacios interiores, comas y tildes: `Leche, entera` y `Ñoño` son válidos mientras quepan en 100 bytes.

### `pedir_entero`

Pide la cantidad.

1. ENTER conserva la cantidad actual cuando se está modificando. Al grabar hay que escribir un número.
2. Intenta `int(texto)`. Si el texto no es un entero (`12.5`, `abc`), avisa y vuelve a preguntar.
3. Acepta solo el rango `0 <= valor <= 2_147_483_647`.

Ese máximo es el mayor entero positivo del campo `i` de 32 bits. En Python se puede escribir `2_147_483_647`: los guiones bajos son solo visuales y el número es `2147483647`. Un entero más grande no cabe en 4 bytes. El cero sí es válido: un producto puede quedar sin stock. Los negativos se rechazan con el mismo mensaje de rango.

### `pedir_float`

Pide el precio.

1. ENTER durante una modificación conserva el precio actual.
2. Intenta `float(texto)`. Acepta `12.50` y también `12`.
3. Rechaza los negativos.

El punto es el separador decimal, porque `float` de Python usa el punto. Una coma (`12,50`) se rechaza. El campo `d` tiene rango de sobra para un precio de inventario; el control que hace el programa es que no sea negativo.

---

## Operación 1: grabar

`grabar_producto` agrega un producto nuevo sin borrar los que ya están.

Pasos:

1. Lee todos los registros y arma la lista de códigos existentes.
2. Pide el código. Si ya está en esa lista, informa el duplicado y regresa al menú.
3. Pide el nombre con `pedir_texto`, limitado a 100 bytes.
4. Pide cantidad y precio.
5. Calcula `total = cantidad * precio`.
6. Abre `productos.bin` en modo `"ab"` (append binario). Ese modo escribe al final. Si el archivo no existía, lo crea.
7. Empaqueta el producto y escribe esos 140 bytes.
8. Muestra el código grabado y el detalle de la multiplicación.

No hay encabezado, así que la primera grabación y las siguientes hacen lo mismo: agregan un bloque al final. En la versión de texto había que escribir la línea de columnas solo la primera vez. Aquí el formato ya está implícito en los 140 bytes.

Grabar lee el archivo completo solo para saber si el código existe. La escritura en sí es de un registro, no de todo el inventario.

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
| `<12` y `<24` | Código y nombre alineados a la izquierda, en un ancho fijo. |
| `>9`, `>10`, `>12` | Números alineados a la derecha. |
| `.2f` | Precio y total con dos decimales. |

Esos anchos son solo de presentación. En el archivo el código sigue ocupando 20 bytes y el nombre 100, con relleno incluido. Un nombre más largo que 24 caracteres se imprime completo y desplaza las columnas de esa fila de la pantalla.

Listar muestra el `total` guardado. No lo vuelve a calcular.

---

## Operación 3: modificar

`modificar_producto` busca por código y reemplaza nombre, cantidad y precio. El código no se cambia: es la identidad del registro.

Pasos:

1. Lee la lista completa.
2. Pide el código. `next(...)` recorre la lista y devuelve el primer diccionario cuyo `codigo` coincide. Si no hay ninguno, `next` devuelve `None`, se avisa y se regresa al menú.
3. Muestra el nombre, la cantidad y el precio actuales.
4. Pide el nuevo nombre pasando el nombre actual. ENTER lo conserva. Un nombre nuevo tiene que caber en 100 bytes.
5. Pide cantidad y precio pasando los valores actuales, de modo que ENTER los conserve.
6. Actualiza el diccionario con `update` y recalcula `total`. El diccionario modificado es el mismo objeto que está dentro de la lista `productos`, así que la lista ya queda con el cambio.
7. `reescribir_productos` graba todos los registros otra vez. Los demás productos salen con los mismos datos.

Aunque cada registro mide 140 bytes y se podría sobrescribir solo el bloque de ese producto, este programa no lo hace. Carga todo, cambia la lista en memoria y vuelve a escribir el archivo. El resultado es el mismo y el procedimiento coincide con el de la versión de texto.

---

## Operación 4: eliminar

`eliminar_producto` quita un producto y conserva el resto.

Pasos:

1. Lee la lista y busca el código igual que al modificar.
2. Si no existe, avisa y regresa al menú.
3. Pide confirmación: `s` elimina y cualquier otra respuesta cancela. La respuesta se pasa a minúsculas, así `S` también confirma.
4. Arma una lista nueva con todos los productos cuyo código es distinto del elegido.
5. Reescribe el archivo con esa lista.

En un archivo de registros fijos, borrar uno del medio dejaría un hueco o exigiría correr hacia adelante los que vienen después. Reescribir los que se quedan produce un archivo compacto: los bloques útiles quedan otra vez uno detrás de otro, sin huecos de 140 bytes.

Si se borra el único producto, el archivo queda con tamaño 0. La confirmación evita borrar un registro por un código mal escrito. Cancelar no toca el archivo.

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

`__name__` vale `"__main__"` solo cuando el archivo se ejecuta directamente (`python filebinario.py`). Si otro programa lo importara para reutilizar `leer_productos` u otra función, el menú no arrancaría solo.

---

## Recorrido de una sesión

Suponga que `productos.bin` no existe.

**Grabar** con código `P001`, nombre `Arroz extra`, cantidad `10` y precio `3.5`. El programa crea el archivo y escribe 140 bytes. El código ocupa 4 bytes útiles y 16 de relleno. El nombre ocupa 11 bytes útiles y 89 de relleno. La cantidad `10` ocupa 4 bytes, y el precio `3.5` y el total `35.0` ocupan 8 cada uno. El archivo mide 140 bytes.

**Grabar** otra vez con código `P001`. Lo rechaza porque ese código ya está.

**Grabar** `A-2`, `Leche, entera`, cantidad `2`, precio `4.25`. El modo `"ab"` agrega otros 140 bytes al final. El total de ese registro es `8.5`. El archivo mide 280 bytes: el primer producto empieza en el byte 0 y el segundo en el byte 140.

**Listar** muestra las dos filas y el total general `43.50`.

**Modificar** `P001`, ENTER en el nombre, cantidad `8`, ENTER en el precio. El nombre y el precio se conservan, la cantidad pasa a 8 y el total se recalcula a `28.0`. El archivo se reescribe entero, otra vez con 280 bytes, y `A-2` sale igual.

**Eliminar** `A-2` y responder `s`. Se reescribe el archivo con un solo registro. Pasa de 280 bytes a 140.

**Salir** con la opción `5`. `productos.bin` permanece en disco con lo último que se guardó.

---

## Decisiones que conviene tener presentes

**El total no se pide.** Se calcula al grabar y al modificar. Listar muestra el número que quedó empaquetado en los últimos 8 bytes del registro.

**Hay dos formas de escribir.** Grabar adiciona un bloque de 140 bytes (`"ab"`). Modificar y eliminar usan `"wb"` porque el cambio puede estar en cualquier posición del archivo.

**La búsqueda es lineal.** Cada operación carga todos los registros y recorre la lista. El tamaño fijo permitiría calcular el desplazamiento `i * 140` y leer un solo producto, pero este programa no guarda un índice de códigos: para saber cuál bloque corresponde a `P001` igual tendría que leer los códigos. Cargar todo y buscar en memoria mantiene el mismo esquema que la versión de texto.

**El archivo no es texto.** Abrirlo en un editor puede mostrar caracteres raros y, si se guarda desde ese editor, se corrompe el tamaño de los registros. La herramienta adecuada para inspeccionarlo es un visor hexadecimal, o el propio listado del programa.

**Un resto que no llega a 140 bytes se ignora.** Si el archivo se corta a la mitad de un registro, `leer_productos` se detiene en ese punto y no agrega un producto incompleto. Los registros enteros que quedaron antes sí se leen.

**Los topes existen por el formato.** Veinte y cien son bytes de UTF-8, no cantidad de teclas. El entero de cantidad tiene que entrar en 32 bits con signo. Esas comprobaciones ocurren antes de `pack`, que es donde un valor demasiado grande o un texto demasiado largo harían fallar la escritura.
