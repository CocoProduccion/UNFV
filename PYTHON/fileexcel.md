# Explicación de `fileexcel.py`

Programa de consola que administra un inventario de productos guardado en un libro de Excel. Permite grabar, listar, modificar y eliminar registros. El archivo vive en la misma carpeta que el script.

La lógica del menú es la de `filetexto.py`: el código identifica al producto, el nombre admite espacios y comas, y el total lo calcula el programa. Lo que cambia es el almacenamiento. Aquí cada producto es una fila de una hoja de cálculo. La primera fila es el encabezado y las siguientes son los productos.

Por defecto el archivo se llama `producto.xlsx`. Si en el código se cambia la extensión a `.xls`, el mismo programa usa el formato antiguo de Excel 97-2003.

Para ejecutarlo, abra una terminal en esa carpeta y escriba:

```powershell
python fileexcel.py
```

El programa muestra un menú y espera una opción del `1` al `5`. Permanece abierto hasta que se elige **Salir**.

Antes de usarlo hacen falta librerías que no vienen con Python:

```powershell
pip install openpyxl
```

Esa alcanza para `producto.xlsx`. Para `producto.xls` también hacen falta las otras dos:

```powershell
pip install xlrd xlwt
```

---

## Qué problema resuelve

Cada producto ocupa una fila con cinco celdas:

| Columna | Celda | Tipo en el programa | Rol |
| --- | --- | --- | --- |
| A | `codigo` | texto sin espacios | Identifica al producto. No se repite. |
| B | `Nombre` | texto | Puede llevar espacios, comas y tildes. |
| C | `Cantidad` | entero `>= 0` | Unidades en inventario. |
| D | `precio` | decimal `>= 0` | Precio unitario. |
| E | `total` | decimal | Lo calcula el programa: `Cantidad * precio`. El usuario no lo escribe. |

El código es la clave de búsqueda. Modificar y eliminar piden ese código y recorren la lista hasta encontrar el producto.

Una hoja, después de grabar dos productos, se ve así:

| | A | B | C | D | E |
| --- | --- | --- | --- | --- | --- |
| 1 | codigo | Nombre | Cantidad | precio | total |
| 2 | P001 | Arroz extra | 10 | 3.5 | 35 |
| 3 | A-2 | Leche, entera | 2 | 4.25 | 8.5 |

La fila 1 es el encabezado. El nombre `Leche, entera` cabe en una sola celda: la coma no separa columnas, porque cada dato vive en su propia celda. Al listar en la consola, el precio y el total se muestran con dos decimales aunque en la hoja Excel guarde `3.5` y `35`.

---

## Los dos formatos de Excel

La extensión de `ARCHIVO` elige el formato y, con él, las librerías:

| Extensión | Formato | Lectura | Escritura |
| --- | --- | --- | --- |
| `.xlsx` | Excel actual (Office Open XML) | `openpyxl` | `openpyxl` |
| `.xls` | Excel 97-2003 (BIFF) | `xlrd` | `xlwt` |

`.xlsx` es un paquete comprimido de archivos XML. Es el formato que Excel crea hoy. `.xls` es un binario más antiguo. Un lector de `.xlsx` no abre `.xls`, y al revés. Por eso el programa tiene dos caminos de lectura y dos de escritura, y el menú es el mismo en los dos.

El archivo por defecto está en esta línea:

```python
ARCHIVO = Path(__file__).resolve().parent / "producto.xlsx"
```

Para el formato antiguo se cambia solo el nombre:

```python
ARCHIVO = Path(__file__).resolve().parent / "producto.xls"
```

`main` rechaza cualquier otra extensión antes de mostrar el menú: `El archivo debe tener extensión .xls o .xlsx.`

Las librerías se importan cuando hacen falta, dentro de la función del formato elegido. Si falta el paquete, el programa avisa con el `pip install` correspondiente y termina.

---

## Por qué cada cambio reescribe la hoja

En `producto.txt` se puede abrir el archivo en modo adición y pegar una línea al final. Un libro de Excel no es una secuencia de líneas sueltas. Es un paquete con la hoja, los tipos de cada celda y la estructura del libro. Agregar bytes al final lo dejaría inválido.

Por eso grabar, modificar y eliminar hacen el mismo recorrido:

1. Leen todas las filas de datos.
2. Cambian la lista en memoria: agregan, actualizan o quitan un diccionario.
3. Crean un libro nuevo y vuelven a escribir el encabezado y todas las filas.

Grabar también reescribe. La palabra «adicionar» describe el efecto sobre el inventario: el producto nuevo se suma a la lista y el libro se genera otra vez, con los productos anteriores incluidos.

---

## Estructura del programa

El archivo está dividido en cuatro bloques:

1. **Constantes.** Ruta del libro, nombres de columna y nombre de la hoja.
2. **Lectura y escritura.** Funciones internas de cada formato, más `leer_productos` y `reescribir_productos`.
3. **Validación.** `pedir_codigo`, `pedir_entero` y `pedir_float`.
4. **Operaciones y menú.** Grabar, listar, modificar, eliminar y `main`.

Las funciones que empiezan con `_` son de apoyo. El menú no las llama por su nombre: las usan `leer_productos` y `reescribir_productos`.

Mapa de llamadas:

```text
main
 ├── grabar_producto
 │    ├── leer_productos
 │    │    ├── _leer_xlsx    o    _leer_xls
 │    │    └── _fila_a_producto
 │    ├── pedir_codigo
 │    ├── pedir_entero
 │    ├── pedir_float
 │    └── reescribir_productos
 │         └── _escribir_xlsx    o    _escribir_xls
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

---

## Constantes

```python
ARCHIVO = Path(__file__).resolve().parent / "producto.xlsx"
CAMPOS = ["codigo", "Nombre", "Cantidad", "precio", "total"]
HOJA = "productos"
```

`Path` (del módulo `pathlib`) representa una ruta de forma que funciona igual en Windows y en Linux.

- `__file__` es la ruta del propio script.
- `.resolve()` la convierte en ruta absoluta, sin `..` ni enlaces.
- `.parent` es la carpeta donde está `fileexcel.py`.
- `/ "producto.xlsx"` une esa carpeta con el nombre del libro.

Así, el Excel se crea junto al programa aunque la terminal se haya abierto en otra carpeta.

`CAMPOS` es la fila de encabezado y también el orden en que se escribe cada producto: columna A, B, C, D y E.

`HOJA` es el nombre de la pestaña que el programa crea al guardar. En la barra de hojas de Excel aparece `productos`.

---

## Cargar la librería: `_requerir`

```python
def _requerir(modulo, paquete):
    try:
        return __import__(modulo)
    except ImportError:
        print(f"   Falta instalar {paquete}. Ejecute: pip install {paquete}")
        raise SystemExit(1)
```

`__import__("openpyxl")` hace lo mismo que `import openpyxl` y devuelve el módulo. Si Python no lo encuentra, `ImportError` se convierte en un aviso y `SystemExit(1)` cierra el programa.

El import ocurre en el momento de leer o escribir, no al arrancar el menú. Quien use solo `.xlsx` no necesita tener instalados `xlrd` ni `xlwt`.

---

## De celda a texto: `_como_texto`

Excel a veces guarda como número un dato que el programa trata como texto. Un código `10` puede volver de la hoja como el flotante `10.0`. Si se convirtiera con `str` sin más, el código quedaría `"10.0"` y dejaría de coincidir con `"10"`.

```python
def _como_texto(valor):
    if isinstance(valor, float) and valor.is_integer():
        return str(int(valor))
    return str(valor).strip()
```

Si el valor es un flotante sin parte decimal, lo pasa a entero y después a texto: `10.0` termina en `"10"`. En cualquier otro caso usa el texto de la celda y le quita espacios de los extremos. `"Arroz extra"` sigue igual. Un precio no pasa por aquí: código y nombre sí.

Un código guardado como número pierde los ceros de la izquierda. La celda numérica `1` vuelve como `"1"`, no como `"001"`. Cuando el propio programa escribe el código, lo escribe como texto (`"P001"`, `"A-2"`), así que ese caso aparece si la celda se editó en Excel y quedó con formato numérico.

---

## De fila a producto: `_fila_a_producto`

Recibe los valores de una fila y devuelve un diccionario, o `None` si esa fila no es un producto.

Hay dos motivos para devolver `None`:

1. La fila no existe o todas sus celdas están vacías (`None` o texto en blanco). Es una fila en blanco al final de la hoja.
2. Tiene menos de cinco celdas, o alguna de las cinco primeras está vacía. Falta un dato y no se puede armar el producto.

Si la fila sirve, toma solo las cinco primeras celdas y arma el diccionario:

```python
codigo, nombre, cantidad, precio, total = fila[:5]
return {
    "codigo": _como_texto(codigo),
    "Nombre": _como_texto(nombre),
    "Cantidad": int(float(cantidad)),
    "precio": float(precio),
    "total": float(total),
}
```

`int(float(cantidad))` unifica los dos formatos. `openpyxl` puede devolver la cantidad como `int` (`10`). `xlrd` devuelve los números como `float` (`10.0`). `float` acepta los dos, e `int` deja el entero `10` que el resto del programa espera. Precio y total quedan como `float`.

El diccionario tiene la misma forma que en las otras versiones:

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

## Lectura y escritura de `.xlsx`

Estas funciones usan `openpyxl`.

### `_leer_xlsx`

```python
libro = openpyxl.load_workbook(ARCHIVO, data_only=True)
hoja = libro.active
```

`load_workbook` abre el libro. `data_only=True` pide el valor calculado de la celda. Este programa escribe números y textos, no fórmulas; el argumento cubre el caso de una celda que alguien haya convertido en fórmula dentro de Excel.

`libro.active` es la hoja activa, la que Excel muestra al abrir el archivo. El programa, al guardar, deja activa la hoja `productos`. Si después se crea otra hoja y se la marca como activa, la lectura tomará esa otra hoja.

Las filas se recorren desde la 2, porque la 1 es el encabezado:

```python
for fila in hoja.iter_rows(min_row=2, values_only=True):
    producto = _fila_a_producto(fila)
    if producto is not None:
        productos.append(producto)
libro.close()
```

`values_only=True` entrega una tupla de valores (`"P001"`, `"Arroz extra"`, `10`, `3.5`, `35.0`) en lugar de objetos celda. Las filas que `_fila_a_producto` descarta no entran en la lista. `close()` suelta el archivo. En Windows, dejar el libro abierto impide que otro programa lo vuelva a guardar.

### `_escribir_xlsx`

Crea un libro nuevo, no reabre el anterior para parchar celdas:

```python
libro = openpyxl.Workbook()
hoja = libro.active
hoja.title = HOJA
hoja.append(CAMPOS)
for p in productos:
    hoja.append([p["codigo"], p["Nombre"], p["Cantidad"], p["precio"], p["total"]])
libro.save(ARCHIVO)
libro.close()
```

`Workbook()` empieza con una hoja en blanco. `title` le pone el nombre `productos`. El primer `append` escribe el encabezado en la fila 1. Cada `append` siguiente escribe un producto en la próxima fila libre, en el orden de `CAMPOS`. `save` reemplaza `producto.xlsx` en disco.

Cantidad se guarda como entero de Python. Precio y total se guardan como `float`. El código y el nombre se guardan como texto, así que una coma dentro del nombre sigue siendo parte de esa celda.

---

## Lectura y escritura de `.xls`

`xlrd` solo lee. `xlwt` solo escribe. Por eso hacen falta los dos paquetes, y por eso modificar un `.xls` también significa leer todo con uno y crear el archivo de nuevo con el otro.

### `_leer_xls`

```python
libro = xlrd.open_workbook(ARCHIVO)
hoja = libro.sheet_by_index(0)
```

`sheet_by_index(0)` es la primera pestaña del libro. En el archivo que escribe este programa, esa pestaña se llama `productos`.

Las filas de `xlrd` se numeran desde 0. La fila 0 es el encabezado, así que el ciclo empieza en 1 y llega hasta `nrows` sin incluirlo:

```python
for rx in range(1, hoja.nrows):
    fila = [hoja.cell_value(rx, cx) for cx in range(min(5, hoja.ncols))]
    producto = _fila_a_producto(fila)
```

`cell_value` devuelve el valor de una celda. De un texto devuelve `str`. De un número devuelve `float`, también cuando la cantidad es `10`: llega como `10.0` y `_fila_a_producto` lo convierte a `10`.

`min(5, hoja.ncols)` pide como máximo las columnas A a E. Si la hoja tuviera menos de cinco columnas, se leen las que hay y `_fila_a_producto` descarta la fila por incompleta.

### `_escribir_xls`

```python
libro = xlwt.Workbook(encoding="utf-8")
hoja = libro.add_sheet(HOJA)
```

`encoding="utf-8"` permite guardar tildes y eñes en el formato antiguo. `add_sheet("productos")` crea la única hoja.

El encabezado se escribe celda por celda en la fila 0:

```python
for col, campo in enumerate(CAMPOS):
    hoja.write(0, col, campo)
```

`enumerate` da el índice y el nombre: `(0, "codigo")`, `(1, "Nombre")`, y así hasta la columna 4.

Los productos empiezan en la fila 1:

```python
for fila, p in enumerate(productos, start=1):
    hoja.write(fila, 0, p["codigo"])
    hoja.write(fila, 1, p["Nombre"])
    hoja.write(fila, 2, p["Cantidad"])
    hoja.write(fila, 3, p["precio"])
    hoja.write(fila, 4, p["total"])
libro.save(str(ARCHIVO))
```

`hoja.write(fila, columna, valor)` coloca un dato. La columna 0 es la A. `save` recibe la ruta como texto: `str(ARCHIVO)` convierte el `Path` en una cadena, que es lo que espera `xlwt`.

---

## Elegir el formato: `leer_productos` y `reescribir_productos`

Estas dos funciones son las que usa el menú. Miran la extensión y delegan.

```python
def leer_productos():
    if not ARCHIVO.exists():
        return []
    extension = ARCHIVO.suffix.lower()
    if extension == ".xlsx":
        return _leer_xlsx()
    if extension == ".xls":
        return _leer_xls()
    raise ValueError("Use un archivo con extensión .xls o .xlsx.")
```

Si el libro todavía no existe, devuelven una lista vacía. El resto del programa lo trata como «aún no hay productos» y la primera grabación crea el archivo.

`ARCHIVO.suffix` es la extensión, incluido el punto. `.lower()` acepta `.XLSX` y `.XLS` igual que las minúsculas.

`reescribir_productos` hace el mismo reparto hacia `_escribir_xlsx` o `_escribir_xls`. Cualquier otra extensión lanza `ValueError`. `main` ya impide llegar al menú con una extensión distinta, así que ese error cubre el caso de que la ruta se cambiara en el código a algo que no sea Excel.

---

## Validación de lo que escribe el usuario

Las tres funciones de entrada repiten la pregunta hasta recibir un valor aceptable. Ninguna operación del menú continúa con un dato inválido.

Excel no impone aquí un tamaño fijo de campo. Un código o un nombre pueden ser tan largos como el usuario los escriba. El control está en la forma del dato.

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

El punto es el separador decimal de lo que se escribe en la consola, porque `float` de Python usa el punto. Una coma (`12,50`) se rechaza. En la hoja, ese precio queda como número de Excel, no como texto.

---

## Operación 1: grabar

`grabar_producto` agrega un producto nuevo y conserva los que ya están.

Pasos:

1. Lee toda la hoja y arma la lista de códigos existentes.
2. Pide el código. Si ya está en esa lista, informa el duplicado y regresa al menú. El código identifica al producto, así que no puede repetirse.
3. Pide el nombre y le aplica `strip()`. Un nombre vacío o hecho solo de espacios se rechaza y la función termina. El nombre puede contener espacios interiores y comas: `Arroz extra` y `Leche, entera` son válidos.
4. Pide cantidad y precio con las funciones de validación.
5. Calcula `total = cantidad * precio`.
6. Agrega un diccionario al final de la lista que está en memoria, con `append`.
7. `reescribir_productos` crea el libro de nuevo: fila 1 con el encabezado y, debajo, todos los productos, incluido el nuevo.
8. Muestra el código grabado y el detalle de la multiplicación.

La primera grabación crea el archivo. Las siguientes leen las filas anteriores, suman una y reemplazan el libro. El producto nuevo queda en la última fila de datos.

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

Un nombre de más de 22 caracteres se imprime completo y desplaza las columnas de esa fila de la consola. El ancho `22` es solo de presentación. En la hoja el nombre ocupa la celda completa.

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
7. `reescribir_productos` graba el libro entero. Los demás productos salen en las mismas filas, con los mismos datos.

Si el usuario pulsa ENTER en el nombre, el anterior se mantiene. Si escribe solo espacios, `strip()` también lo convierte en vacío y se conserva el nombre anterior.

---

## Operación 4: eliminar

`eliminar_producto` quita un producto y conserva el resto.

Pasos:

1. Lee la lista y busca el código igual que al modificar.
2. Si no existe, avisa y regresa al menú.
3. Pide confirmación: `s` elimina y cualquier otra respuesta cancela. La respuesta se pasa a minúsculas, así `S` también confirma.
4. Arma una lista nueva con todos los productos cuyo código es distinto del elegido.
5. Reescribe el libro con esa lista.

Quitar una fila del medio y dejar las de abajo corridas hacia arriba es justo lo que hace crear la hoja de nuevo: el encabezado vuelve a la fila 1 y los productos que se quedan ocupan las filas 2, 3, y así sucesivamente, sin un hueco donde estaba el eliminado.

Si se elimina el único producto, el libro queda solo con la fila de encabezado. La próxima lectura no encuentra filas de datos y el listado muestra `(sin productos registrados)`.

La confirmación evita borrar un registro por un código mal escrito. Cancelar no toca el archivo.

---

## Menú: `main`

Antes del ciclo, `main` mira la extensión:

```python
if ARCHIVO.suffix.lower() not in (".xls", ".xlsx"):
    print("  El archivo debe tener extensión .xls o .xlsx.")
    return
```

Con una extensión válida, entra en un ciclo infinito:

1. Muestra el título con el nombre real del archivo (`producto.xlsx` o `producto.xls`) y las cinco opciones.
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

`__name__` vale `"__main__"` solo cuando el archivo se ejecuta directamente (`python fileexcel.py`). Si otro programa lo importara para reutilizar `leer_productos` u otra función, el menú no arrancaría solo.

---

## Recorrido de una sesión

Suponga que `producto.xlsx` no existe y que `openpyxl` está instalado.

**Grabar** con código `P001`, nombre `Arroz extra`, cantidad `10` y precio `3.5`. El programa crea el libro, nombra la hoja `productos`, escribe el encabezado en la fila 1 y el producto en la fila 2. El total de esa fila es `35`.

**Grabar** otra vez con código `P001`. Lo rechaza porque ese código ya está. El archivo no cambia.

**Grabar** `A-2`, `Leche, entera`, cantidad `2`, precio `4.25`. Lee las dos filas útiles (el encabezado no cuenta), agrega el segundo diccionario en memoria y crea el libro otra vez. El total de esa fila es `8.5`. La hoja queda como la tabla del inicio de este documento.

**Listar** muestra las dos filas y el total general `43.50`.

**Modificar** `P001`, ENTER en el nombre, cantidad `8`, ENTER en el precio. El nombre y el precio se conservan, la cantidad pasa a `8` y el total se recalcula a `28`. El libro se reescribe; `A-2` sigue en la fila 3.

**Eliminar** `A-2` y responder `s`. Se reescribe el libro con el encabezado y solo la fila de `P001`.

**Salir** con la opción `5`. `producto.xlsx` permanece en disco con lo último que se guardó. Se puede abrir con Excel y ver la hoja `productos`.

Si `ARCHIVO` apuntara a `producto.xls`, la sesión sería la misma para quien usa el menú. Cambiarían el paquete que lee, el paquete que escribe y el tipo de archivo en disco.

---

## Decisiones que conviene tener presentes

**El total no se pide.** Siempre es `Cantidad * precio` en el momento de grabar o de modificar. Si alguien editara la celda del precio en Excel y no cambiara la del total, el listado mostraría el total viejo hasta la próxima modificación. Listar no lo recalcula: muestra lo que está guardado.

**Grabar, modificar y eliminar reescriben el libro completo.** Cada guardado crea un libro nuevo con el encabezado y las filas que quedan. El formato del archivo sigue siendo un Excel válido.

**La búsqueda es lineal.** Cada operación carga todos los productos y recorre la lista. Para un inventario de práctica es suficiente. El código duplicado se controla solo al grabar. La hoja no tiene un índice aparte.

**La lectura ignora filas incompletas.** Una fila en blanco, o una fila a la que le falte alguna de las cinco celdas, no entra en la lista. Al volver a guardar, esas filas no se escriben: el archivo queda solo con el encabezado y los productos completos.

**Los números de Excel no siempre llegan con el tipo de Python esperado.** `xlrd` entrega la cantidad como `10.0`. `_fila_a_producto` la convierte con `int(float(...))`. Un código numérico sin decimales vuelve a texto sin el `.0`, gracias a `_como_texto`.

**Cada formato tiene su librería.** `openpyxl` no escribe `.xls`. `xlrd` 2 no lee `.xlsx`, y `xlwt` solo crea `.xls`. La extensión de `ARCHIVO` mantiene esos caminos separados. Si el paquete no está instalado, `_requerir` indica el `pip install` y el programa termina.
