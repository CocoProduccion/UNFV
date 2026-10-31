"""
Gestión de productos en un archivo Excel: producto.xlsx (o producto.xls)
=======================================================================

Misma lógica que la versión de texto (producto.txt). El inventario se
guarda en la primera hoja, con una fila de encabezado y una fila por
producto:

    codigo   : string sin espacios (identifica al producto)
    Nombre   : string con espacios
    Cantidad : entero
    precio   : float
    total    : float, calculado por el programa como Cantidad * precio

Un libro de Excel no es una secuencia de líneas: no se puede adicionar
un producto al final como en el CSV. Cada cambio reescribe la hoja
completa para que el archivo siga siendo válido.

La extensión de ARCHIVO elige el formato:
    .xlsx  Excel actual     ->  pip install openpyxl
    .xls   Excel 97-2003    ->  pip install xlrd xlwt

Por defecto se usa producto.xlsx. Para el formato antiguo, cambie
ARCHIVO a producto.xls.

Ejecute con:  python fileexcel.py
"""

from pathlib import Path

# Archivo de datos junto al script (funciona igual en Windows y Linux)
ARCHIVO = Path(__file__).resolve().parent / "producto.xlsx"
CAMPOS = ["codigo", "Nombre", "Cantidad", "precio", "total"]
HOJA = "productos"


# -------------------------------------------------------------------
# Lectura y escritura del archivo
# -------------------------------------------------------------------
def _requerir(modulo, paquete):
    """Importa la librería del formato elegido e indica cómo instalarla."""
    try:
        return __import__(modulo)
    except ImportError:
        print(f"   Falta instalar {paquete}. Ejecute: pip install {paquete}")
        raise SystemExit(1)


def _como_texto(valor):
    """Excel guarda algunos textos numéricos como float (10.0 -> '10')."""
    if isinstance(valor, float) and valor.is_integer():
        return str(int(valor))
    return str(valor).strip()


def _fila_a_producto(fila):
    """Convierte una fila de la hoja en un producto, o None si está vacía."""
    if not fila or all(celda is None or str(celda).strip() == "" for celda in fila):
        return None
    if len(fila) < 5 or any(celda is None or str(celda).strip() == "" for celda in fila[:5]):
        return None
    codigo, nombre, cantidad, precio, total = fila[:5]
    return {
        "codigo": _como_texto(codigo),
        "Nombre": _como_texto(nombre),
        "Cantidad": int(float(cantidad)),
        "precio": float(precio),
        "total": float(total),
    }


def _leer_xlsx():
    openpyxl = _requerir("openpyxl", "openpyxl")
    libro = openpyxl.load_workbook(ARCHIVO, data_only=True)
    hoja = libro.active
    productos = []
    for fila in hoja.iter_rows(min_row=2, values_only=True):
        producto = _fila_a_producto(fila)
        if producto is not None:
            productos.append(producto)
    libro.close()
    return productos


def _escribir_xlsx(productos):
    openpyxl = _requerir("openpyxl", "openpyxl")
    libro = openpyxl.Workbook()
    hoja = libro.active
    hoja.title = HOJA
    hoja.append(CAMPOS)
    for p in productos:
        hoja.append([p["codigo"], p["Nombre"], p["Cantidad"], p["precio"], p["total"]])
    libro.save(ARCHIVO)
    libro.close()


def _leer_xls():
    xlrd = _requerir("xlrd", "xlrd")
    libro = xlrd.open_workbook(ARCHIVO)
    hoja = libro.sheet_by_index(0)
    productos = []
    for rx in range(1, hoja.nrows):
        fila = [hoja.cell_value(rx, cx) for cx in range(min(5, hoja.ncols))]
        producto = _fila_a_producto(fila)
        if producto is not None:
            productos.append(producto)
    return productos


def _escribir_xls(productos):
    xlwt = _requerir("xlwt", "xlwt")
    libro = xlwt.Workbook(encoding="utf-8")
    hoja = libro.add_sheet(HOJA)
    for col, campo in enumerate(CAMPOS):
        hoja.write(0, col, campo)
    for fila, p in enumerate(productos, start=1):
        hoja.write(fila, 0, p["codigo"])
        hoja.write(fila, 1, p["Nombre"])
        hoja.write(fila, 2, p["Cantidad"])
        hoja.write(fila, 3, p["precio"])
        hoja.write(fila, 4, p["total"])
    libro.save(str(ARCHIVO))


def leer_productos():
    """Devuelve la lista de productos leída del archivo Excel."""
    if not ARCHIVO.exists():
        return []                             # archivo aún no creado
    extension = ARCHIVO.suffix.lower()
    if extension == ".xlsx":
        return _leer_xlsx()
    if extension == ".xls":
        return _leer_xls()
    raise ValueError("Use un archivo con extensión .xls o .xlsx.")


def reescribir_productos(productos):
    """Reescribe la hoja completa con la lista de productos."""
    extension = ARCHIVO.suffix.lower()
    if extension == ".xlsx":
        _escribir_xlsx(productos)
    elif extension == ".xls":
        _escribir_xls(productos)
    else:
        raise ValueError("Use un archivo con extensión .xls o .xlsx.")


# -------------------------------------------------------------------
# Validación de entradas
# -------------------------------------------------------------------
def pedir_codigo(mensaje):
    """Pide un código sin espacios."""
    while True:
        codigo = input(mensaje).strip()
        if codigo and not any(caracter.isspace() for caracter in codigo):
            return codigo
        print("   El código no puede estar vacío ni contener espacios.")


def pedir_entero(mensaje, actual=None):
    """Pide una cantidad entera (0 o más). ENTER conserva el valor actual."""
    while True:
        texto = input(mensaje).strip()
        if texto == "" and actual is not None:
            return actual
        try:
            valor = int(texto)
        except ValueError:
            print("   Ingrese un número entero.")
            continue
        if valor < 0:
            print("   La cantidad no puede ser negativa.")
            continue
        return valor


def pedir_float(mensaje, actual=None):
    """Pide un precio decimal (0 o más). ENTER conserva el valor actual."""
    while True:
        texto = input(mensaje).strip()
        if texto == "" and actual is not None:
            return actual
        try:
            valor = float(texto)
        except ValueError:
            print("   Ingrese un número decimal (ejemplo: 12.50).")
            continue
        if valor < 0:
            print("   El precio no puede ser negativo.")
            continue
        return valor


# -------------------------------------------------------------------
# Operaciones del menú
# -------------------------------------------------------------------
def grabar_producto():
    """GRABA un producto nuevo, adicionándolo sin borrar los existentes."""
    productos = leer_productos()
    codigos = [p["codigo"] for p in productos]

    print("\n--- GRABAR PRODUCTO ---")
    codigo = pedir_codigo("   Código: ")
    if codigo in codigos:
        print(f"   Ya existe un producto con el código {codigo}.")
        return

    nombre = input("   Nombre: ").strip()
    if not nombre:
        print("   El nombre no puede estar vacío.")
        return
    cantidad = pedir_entero("   Cantidad: ")
    precio = pedir_float("   Precio: ")
    total = cantidad * precio                   # calculado por el programa

    productos.append({"codigo": codigo, "Nombre": nombre,
                      "Cantidad": cantidad, "precio": precio, "total": total})
    reescribir_productos(productos)
    print(f"   Producto {codigo} grabado. Total = {cantidad} x {precio} = {total}")


def listar_productos():
    """LISTA todos los productos en forma de tabla."""
    productos = leer_productos()
    print("\n--- LISTADO DE PRODUCTOS ---")
    if not productos:
        print("   (sin productos registrados)")
        return
    print(f"   {'Código':<10}{'Nombre':<22}{'Cantidad':>9}{'Precio':>10}{'Total':>12}")
    for p in productos:
        print(f"   {p['codigo']:<10}{p['Nombre']:<22}{p['Cantidad']:>9}"
              f"{p['precio']:>10.2f}{p['total']:>12.2f}")
    print(f"   Total general del inventario: {sum(p['total'] for p in productos):.2f}")


def modificar_producto():
    """MODIFICA los datos de un producto buscando por código."""
    productos = leer_productos()
    print("\n--- MODIFICAR PRODUCTO ---")
    codigo = pedir_codigo("   Código del producto a modificar: ")
    producto = next((p for p in productos if p["codigo"] == codigo), None)
    if producto is None:
        print(f"   No existe un producto con el código {codigo}.")
        return

    print(f"   Datos actuales -> {producto['Nombre']}, {producto['Cantidad']}, "
          f"S/ {producto['precio']:.2f}")
    print("   Presione ENTER para conservar el valor actual.")

    nuevo_nombre = input("   Nuevo nombre: ").strip() or producto["Nombre"]
    nueva_cantidad = pedir_entero("   Nueva cantidad: ", producto["Cantidad"])
    nuevo_precio = pedir_float("   Nuevo precio: ", producto["precio"])

    producto.update(Nombre=nuevo_nombre, Cantidad=nueva_cantidad,
                    precio=nuevo_precio)
    producto["total"] = nueva_cantidad * nuevo_precio             # recalculado
    reescribir_productos(productos)
    print(f"   Producto {codigo} modificado. Nuevo total = {producto['total']}")


def eliminar_producto():
    """ELIMINA un producto del archivo (los demás se conservan)."""
    productos = leer_productos()
    print("\n--- ELIMINAR PRODUCTO ---")
    codigo = pedir_codigo("   Código del producto a eliminar: ")
    producto = next((p for p in productos if p["codigo"] == codigo), None)
    if producto is None:
        print(f"   No existe un producto con el código {codigo}.")
        return

    confirmar = input(f"   ¿Eliminar {codigo} - {producto['Nombre']}? (s/n): ").lower()
    if confirmar == "s":
        restantes = [p for p in productos if p["codigo"] != codigo]
        reescribir_productos(restantes)
        print("   Producto eliminado.")
    else:
        print("   Operación cancelada.")


# -------------------------------------------------------------------
# Menú principal
# -------------------------------------------------------------------
def main():
    if ARCHIVO.suffix.lower() not in (".xls", ".xlsx"):
        print("  El archivo debe tener extensión .xls o .xlsx.")
        return
    while True:
        print(f"\n===== GESTIÓN DE PRODUCTOS ({ARCHIVO.name}) =====")
        print("  1. Grabar (adicionar) producto")
        print("  2. Listar productos")
        print("  3. Modificar producto")
        print("  4. Eliminar producto")
        print("  5. Salir")
        opcion = input("  Elija una opción: ").strip()

        if opcion == "1":
            grabar_producto()
        elif opcion == "2":
            listar_productos()
        elif opcion == "3":
            modificar_producto()
        elif opcion == "4":
            eliminar_producto()
        elif opcion == "5":
            print("  Fin del programa.")
            break
        else:
            print("  Opción no válida; use 1 a 5.")


if __name__ == "__main__":
    main()
