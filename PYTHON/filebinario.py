"""
Gestión de productos en un archivo BINARIO: productos.bin
==========================================================

Misma lógica que la versión de texto (producto.txt), pero ahora cada
producto se guarda como un REGISTRO BINARIO de longitud fija:

    codigo   : 20 bytes  (string sin espacios)
    Nombre   : 100 bytes (string con espacios y tildes)
    Cantidad :  4 bytes  (entero, formato "i")
    precio   :  8 bytes  (float, formato "d")
    total    :  8 bytes  (float, calculado como Cantidad * precio)

Total: 140 bytes por producto. El prefijo "<" fija el formato
little-endian estándar, igual en Windows y Linux.

Operaciones: GRABAR (adicionar), LISTAR, MODIFICAR y ELIMINAR.

Ejecute con:  python gestion_productos_bin.py
"""

import struct
from pathlib import Path

# -------------------------------------------------------------------
# Archivo y formato del registro binario
# -------------------------------------------------------------------
ARCHIVO = Path(__file__).resolve().parent / "productos.bin"

# "<" little-endian estándar: 20s codigo + 100s nombre + i + d + d
REGISTRO = struct.Struct("<20s100sidd")
MAX_CODIGO = 20      # bytes disponibles para el código
MAX_NOMBRE = 100     # bytes disponibles para el nombre


# -------------------------------------------------------------------
# Conversión entre producto (dict) y bytes del registro
# -------------------------------------------------------------------
def producto_a_bytes(p):
    """Empaqueta un producto en los 140 bytes de su registro."""
    return REGISTRO.pack(
        p["codigo"].encode("utf-8"),    # struct rellena con \x00
        p["Nombre"].encode("utf-8"),
        p["Cantidad"],
        p["precio"],
        p["total"],
    )


def bytes_a_producto(raw):
    """Reconstruye el producto a partir de los bytes de su registro."""
    codigo_b, nombre_b, cantidad, precio, total = REGISTRO.unpack(raw)
    return {
        "codigo": codigo_b.rstrip(b"\x00").decode("utf-8"),
        "Nombre": nombre_b.rstrip(b"\x00").decode("utf-8"),
        "Cantidad": cantidad,
        "precio": precio,
        "total": total,
    }


# -------------------------------------------------------------------
# Lectura y escritura del archivo binario
# -------------------------------------------------------------------
def leer_productos():
    """Lee todos los registros, uno por uno, hasta el final del archivo."""
    productos = []
    if not ARCHIVO.exists():
        return productos                      # archivo aún no creado
    with ARCHIVO.open("rb") as f:              # "rb": lectura binaria
        while True:
            raw = f.read(REGISTRO.size)        # lee 140 bytes por registro
            if len(raw) < REGISTRO.size:       # fin del archivo
                break
            productos.append(bytes_a_producto(raw))
    return productos


def reescribir_productos(productos):
    """Reescribe el archivo completo con la lista de productos ("wb")."""
    with ARCHIVO.open("wb") as f:              # "wb": escritura binaria
        for p in productos:
            f.write(producto_a_bytes(p))


# -------------------------------------------------------------------
# Validación de entradas (misma lógica que la versión de texto)
# -------------------------------------------------------------------
def pedir_codigo(mensaje):
    """Pide un código sin espacios (bytes) y que quepa en el registro."""
    while True:
        codigo = input(mensaje).strip()
        if not codigo or any(c.isspace() for c in codigo):
            print("   El código no puede estar vacío ni contener espacios.")
            continue
        if len(codigo.encode("utf-8")) > MAX_CODIGO:
            print(f"   El código admite hasta {MAX_CODIGO} bytes.")
            continue
        return codigo


def pedir_texto(mensaje, max_bytes, actual=None):
    """Pide un texto que quepa en el campo. ENTER conserva el valor actual."""
    while True:
        texto = input(mensaje).strip()
        if texto == "" and actual is not None:
            return actual
        if not texto:
            print("   Este campo no puede estar vacío.")
            continue
        if len(texto.encode("utf-8")) > max_bytes:
            print(f"   Admite hasta {max_bytes} bytes; una palabra con tilde usa más bytes.")
            continue
        return texto


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
        if not 0 <= valor <= 2_147_483_647:      # límite del campo "i"
            print("   La cantidad debe estar entre 0 y 2147483647.")
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
    """GRABA un producto nuevo, adicionándolo al final del archivo."""
    productos = leer_productos()
    codigos = [p["codigo"] for p in productos]

    print("\n--- GRABAR PRODUCTO ---")
    codigo = pedir_codigo("   Código: ")
    if codigo in codigos:
        print(f"   Ya existe un producto con el código {codigo}.")
        return

    nombre = pedir_texto("   Nombre: ", MAX_NOMBRE)
    cantidad = pedir_entero("   Cantidad: ")
    precio = pedir_float("   Precio: ")
    total = cantidad * precio                  # calculado por el programa

    # Modo "ab": adiciona los 140 bytes al final sin borrar nada
    with ARCHIVO.open("ab") as f:
        f.write(producto_a_bytes({"codigo": codigo, "Nombre": nombre,
                                  "Cantidad": cantidad, "precio": precio,
                                  "total": total}))
    print(f"   Producto {codigo} grabado. Total = {cantidad} x {precio} = {total}")


def listar_productos():
    """LISTA todos los productos en forma de tabla."""
    productos = leer_productos()
    print("\n--- LISTADO DE PRODUCTOS ---")
    if not productos:
        print("   (sin productos registrados)")
        return
    print(f"   {'Código':<12}{'Nombre':<24}{'Cantidad':>9}{'Precio':>10}{'Total':>12}")
    for p in productos:
        print(f"   {p['codigo']:<12}{p['Nombre']:<24}{p['Cantidad']:>9}"
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

    nuevo_nombre = pedir_texto("   Nuevo nombre: ", MAX_NOMBRE, producto["Nombre"])
    nueva_cantidad = pedir_entero("   Nueva cantidad: ", producto["Cantidad"])
    nuevo_precio = pedir_float("   Nuevo precio: ", producto["precio"])

    producto.update(Nombre=nuevo_nombre, Cantidad=nueva_cantidad,
                   precio=nuevo_precio)
    producto["total"] = nueva_cantidad * nuevo_precio      # recalculado
    reescribir_productos(productos)
    print(f"   Producto {codigo} modificado. Nuevo total = {producto['total']}")


def eliminar_producto():
    """ELIMINA un producto: reescribe el archivo sin ese registro."""
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
    while True:
        print("\n===== GESTIÓN DE PRODUCTOS (productos.bin) =====")
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
