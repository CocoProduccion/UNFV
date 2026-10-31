"""
Gestión de productos en un archivo de texto: producto.txt
=========================================================

Programa básico que GRABA (adicional), LISTA, MODIFICA y ELIMINA
registros de producto.txt, con las columnas:

    codigo   : string sin espacios (identifica al producto)
    Nombre   : string con espacios
    Cantidad : entero
    precio   : float
    total    : float, calculado por el programa como Cantidad * precio

El nombre puede contener espacios (incluso comas), por eso se usa el
módulo csv en lugar de dividir la línea a mano.

Ejecute con:  python gestion_productos.py
"""

import csv
from pathlib import Path

# Archivo de datos junto al script (funciona igual en Windows y Linux)
ARCHIVO = Path(__file__).resolve().parent / "producto.txt"
CAMPOS = ["codigo", "Nombre", "Cantidad", "precio", "total"]


# -------------------------------------------------------------------
# Lectura y escritura del archivo
# -------------------------------------------------------------------
def leer_productos():
    """Devuelve la lista de productos leída de producto.txt."""
    productos = []
    if not ARCHIVO.exists():
        return productos                      # archivo aún no creado
    with ARCHIVO.open("r", newline="", encoding="utf-8") as f:
        for fila in csv.DictReader(f):
            fila["Cantidad"] = int(fila["Cantidad"])
            fila["precio"] = float(fila["precio"])
            fila["total"] = float(fila["total"])
            productos.append(fila)
    return productos


def reescribir_productos(productos):
    """Reescribe el archivo completo con la lista de productos (modo w)."""
    with ARCHIVO.open("w", newline="", encoding="utf-8") as f:
        escritor = csv.DictWriter(f, fieldnames=CAMPOS)
        escritor.writeheader()
        escritor.writerows(productos)


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

    # Modo "a": adiciona al final; si el archivo no existe, se crea
    nuevo = not ARCHIVO.exists() or ARCHIVO.stat().st_size == 0
    with ARCHIVO.open("a", newline="", encoding="utf-8") as f:
        escritor = csv.DictWriter(f, fieldnames=CAMPOS)
        if nuevo:
            escritor.writeheader()             # encabezado solo la primera vez
        escritor.writerow({"codigo": codigo, "Nombre": nombre,
                           "Cantidad": cantidad, "precio": precio, "total": total})
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
    while True:
        print("\n===== GESTIÓN DE PRODUCTOS (producto.txt) =====")
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
