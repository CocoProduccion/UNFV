# Explicación de `filetexto.py`

Programa de consola que administra un inventario de productos guardado en el archivo de texto `producto.txt`. Permite grabar, listar, modificar y eliminar registros. El archivo vive en la misma carpeta que el script.

Para ejecutarlo, abra una terminal en esa carpeta y escriba:

```powershell
python filetexto.py
```

El programa muestra un menú y espera una opción del `1` al `5`. Permanece abierto hasta que se elige **Salir**.

El comentario inicial del script menciona `python gestion_productos.py`. El archivo real se llama `filetexto.py`, así que el comando correcto es el de arriba.

---

## Qué problema resuelve

Cada producto es un registro con cinco datos:

| Campo | Tipo en el programa | Rol |
| --- | --- | --- |
| `codigo` | texto sin espacios | Identifica al producto. No se repite. |
| `Nombre` | texto | Puede llevar espacios y comas. |
| `Cantidad` | entero `>= 0` | Unidades en inventario. |
| `precio` | decimal `>= 0` | Precio unitario. |
| `total` | decimal | Lo calcula el programa: `Cantidad * precio`. El usuario no lo escribe. |

El código es la clave de búsqueda. Modificar y eliminar piden ese código y recorren la lista hasta encontrar el producto.

Un ejemplo de `producto.txt` después de grabar dos productos:

```text
codigo,Nombre,Cantidad,precio,total
P001,Arroz extra,10,3.5,35.0
A-2,"Leche, entera",2,4.25,8.5
```

La primera línea es el encabezado. Cada línea siguiente es un producto. El nombre `Leche, entera` va entre comillas porque contiene una coma: así el lector sabe que esa coma pertenece al nombre y no separa columnas.

---

## Por qué se usa el módulo `csv`

En un archivo de texto plano las columnas suelen separarse con coma. Si el nombre se partiera con `linea.split(",")`, un nombre como `Leche, entera` se rompería en dos campos y el resto de la fila quedaría desalineado.

El módulo `csv` de la biblioteca estándar se encarga de:

- escribir el encabezado y cada fila con el mismo orden de columnas;
- poner comillas cuando un texto contiene coma, comillas o saltos de línea;
- leer cada fila como un diccionario cuyas claves son los nombres del encabezado.

`DictReader` produce diccionarios. `DictWriter` los vuelve a escribir. Las claves son exactamente las de la lista `CAMPOS`.

Todo lo que entra y sale de un archivo de texto es texto. Por eso, al leer, el programa convierte `Cantidad` con `int` y `precio` y `total` con `float`. Al escribir, `csv` convierte esos números otra vez a texto.

---

## Estructura del programa

El archivo está dividido en cuatro bloques:

1. **Constantes.** Dónde está el archivo y cuáles son las columnas.
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
 │    └── pedir_float
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

Grabar no usa `reescribir_productos`. Abre el archivo en modo adición (`"a"`) y agrega una sola fila al final. Modificar y eliminar sí reescriben el archivo completo, porque hay que cambiar o quitar una fila que ya está en el medio.

---

## Constantes

```python
ARCHIVO = Path(__file__).resolve().parent / "producto.txt"
CAMPOS = ["codigo", "Nombre", "Cantidad", "precio", "total"]
```

`Path` (del módulo `pathlib`) representa una ruta de forma que funciona igual en Windows y en Linux.

- `__file__` es la ruta del propio script.
- `.resolve()` la convierte en ruta absoluta, sin `..` ni enlaces.
- `.parent` es la carpeta donde está `filetexto.py`.
- `/ "producto.txt"` une esa carpeta con el nombre del archivo de datos.

Así, `producto.txt` se crea junto al programa aunque la terminal se haya abierto en otra carpeta.

`CAMPOS` fija el orden de las columnas. `DictWriter` lo usa como `fieldnames`: el encabezado y cada fila salen siempre en ese orden.

---

## Lectura: `leer_productos`

Devuelve una lista de diccionarios, uno por producto. Si `producto.txt` todavía no existe, devuelve una lista vacía. El resto del programa trata esa lista vacía como «aún no hay productos» y no se cae al arrancar por primera vez.

Si el archivo existe, lo abre así:

```python
with ARCHIVO.open("r", newline="", encoding="utf-8") as f:
```

| Argumento | Para qué sirve |
| --- | --- |
| `"r"` | Solo lectura. |
| `newline=""` | Le indica a `csv` que él controle los saltos de línea. En Windows una línea termina en `\r\n` y en Linux en `\n`. Sin `newline=""`, el módulo puede interpretar mal esos finales. |
| `encoding="utf-8"` | Lee tildes, eñes y otros caracteres como Unicode. El mismo archivo se lee igual en Windows y en Linux. |
| `with` | Cierra el archivo al salir del bloque, también si ocurre un error. |

`csv.DictReader(f)` toma la primera línea como nombres de columna y cada línea siguiente como un diccionario de textos. El bucle convierte los tres campos numéricos y acumula la fila en `productos`.

Ejemplo de lo que queda en memoria a partir de la fila `P001,Arroz extra,10,3.5,35.0`:

```python
{
    "codigo": "P001",
    "Nombre": "Arroz extra",
    "Cantidad": 10,
    "precio": 3.5,
    "total": 35.0,
}
```

`codigo` y `Nombre` se quedan como texto. `Cantidad`, `precio` y `total` pasan a ser números para poder compararlos, sumarlos y recalcular el total.

---

## Escritura completa: `reescribir_productos`

Recibe la lista ya modificada y vuelve a crear `producto.txt` desde cero:

```python
with ARCHIVO.open("w", newline="", encoding="utf-8") as f:
    escritor = csv.DictWriter(f, fieldnames=CAMPOS)
    escritor.writeheader()
    escritor.writerows(productos)
```

El modo `"w"` vacía el archivo si ya existía y luego escribe el encabezado y todas las filas. Por eso modificar y eliminar arman primero la lista definitiva en memoria y solo entonces llaman a esta función. Si se escribiera fila por fila mientras se lee el mismo archivo, se perderían los registros que todavía no se han copiado.

`writeheader()` escribe `codigo,Nombre,Cantidad,precio,total`. `writerows` escribe un producto por línea, usando las claves del diccionario. Un campo que no esté en `CAMPOS` no se guarda.

---

## Validación de lo que escribe el usuario

Las tres funciones de entrada repiten la pregunta hasta recibir un valor aceptable. Ninguna operación del menú continúa con un dato inválido.

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

El punto es el separador decimal, porque `float` de Python usa el punto. Una coma (`12,50`) se rechaza.

---

## Operación 1: grabar

`grabar_producto` agrega un producto nuevo sin borrar los que ya están.

Pasos:

1. Lee todo el archivo y arma la lista de códigos existentes.
2. Pide el código. Si ya está en esa lista, informa el duplicado y regresa al menú. El código identifica al producto, así que no puede repetirse.
3. Pide el nombre y le aplica `strip()`. Un nombre vacío o hecho solo de espacios se rechaza y la función termina. A diferencia del código, el nombre puede contener espacios interiores: `Arroz extra` es válido.
4. Pide cantidad y precio con las funciones de validación.
5. Calcula `total = cantidad * precio`.
6. Abre `producto.txt` en modo `"a"` (append, adición). Ese modo escribe al final. Si el archivo no existía, lo crea.
7. Escribe el encabezado solo cuando el archivo es nuevo o está vacío (`stat().st_size == 0`). En las grabaciones siguientes el encabezado ya está y solo se agrega la fila del producto.
8. Muestra el código grabado y el detalle de la multiplicación.

La fila se arma como diccionario con las cinco claves de `CAMPOS`. `DictWriter` la serializa en el orden del encabezado.

Grabar lee el archivo completo solo para saber si el código existe. La escritura en sí es de una fila, no de todo el inventario. Modificar y eliminar no pueden hacer eso: cambiar o quitar una línea intermedia obliga a reescribir el archivo.

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

Un nombre de más de 22 caracteres se imprime completo y desplaza las columnas de esa fila. El ancho `22` es solo de presentación; en el archivo el nombre se guarda entero.

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

Si el usuario confirma un nombre vacío pulsando ENTER, el nombre anterior se mantiene. Si escribe solo espacios, `strip()` también lo convierte en vacío y se conserva el nombre anterior.

---

## Operación 4: eliminar

`eliminar_producto` quita un producto y conserva el resto.

Pasos:

1. Lee la lista y busca el código igual que al modificar.
2. Si no existe, avisa y regresa al menú.
3. Pide confirmación: `s` elimina y cualquier otra respuesta cancela. La respuesta se pasa a minúsculas, así `S` también confirma.
4. Arma una lista nueva con todos los productos cuyo código es distinto del elegido.
5. Reescribe el archivo con esa lista. Si era el único producto, el archivo queda solo con el encabezado.

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

`__name__` vale `"__main__"` solo cuando el archivo se ejecuta directamente (`python filetexto.py`). Si otro programa lo importara para reutilizar `leer_productos` u otra función, el menú no arrancaría solo.

---

## Recorrido de una sesión

Suponga que `producto.txt` no existe.

**Grabar** con código `P001`, nombre `Arroz extra`, cantidad `10` y precio `3.5`. El programa crea el archivo, escribe el encabezado y una fila, y calcula el total `35.0`.

**Grabar** otra vez con código `P001`. Lo rechaza porque ese código ya está.

**Grabar** `A-2`, `Leche, entera`, cantidad `2`, precio `4.25`. Como el archivo ya tiene contenido, solo agrega una fila. El total de esa fila es `8.5`. El archivo queda como el ejemplo del inicio de este documento.

**Listar** muestra las dos filas y el total general `43.50`.

**Modificar** `P001`, ENTER en el nombre, cantidad `8`, ENTER en el precio. El nombre y el precio se conservan, la cantidad pasa a 8 y el total se recalcula a `28.0`. El archivo se reescribe entero, con `A-2` intacto.

**Eliminar** `A-2` y responder `s`. Se reescribe el archivo con el encabezado y solo `P001`.

**Salir** con la opción `5`. `producto.txt` permanece en disco con lo último que se guardó.

---

## Decisiones que conviene tener presentes

**El total no se pide.** Siempre es `Cantidad * precio` en el momento de grabar o de modificar. Si alguien editara `producto.txt` a mano y cambiara el precio sin cambiar el total, el listado mostraría el total viejo hasta la próxima modificación. Listar no lo recalcula: muestra lo que está guardado.

**Hay dos formas de escribir.** Grabar adiciona una fila (`"a"`) para no reescribir todo el inventario en cada alta. Modificar y eliminar usan `"w"` porque el cambio no está al final del archivo.

**La búsqueda es lineal.** Cada operación carga todos los productos y recorre la lista. Para un inventario de práctica es suficiente. El código duplicado se controla solo al grabar; el archivo no tiene un índice aparte.

**El archivo es texto legible.** Se puede abrir `producto.txt` con el Bloc de notas y ver los productos. Hay que guardarlo en UTF-8 si se edita a mano, que es la codificación con la que el programa lo lee y lo escribe.

**Un archivo dañado detiene la lectura.** Si falta una columna, si `Cantidad` no es un entero o si las comillas de un nombre quedaron mal cerradas, `leer_productos` lanza una excepción y esa operación no continúa. El programa no trae un mensaje propio para ese caso: espera un CSV coherente con `CAMPOS`.
