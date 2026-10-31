"""
Gestión de productos en un archivo JSON: producto.json
======================================================

Misma lógica que la versión de texto (producto.txt), pero el inventario
se guarda como una LISTA de objetos JSON:

    [
      {
        "codigo": "P001",
        "Nombre": "Arroz extra",
        "Cantidad": 10,
        "precio": 3.50,
        "total": 35.00
      }
    ]

    codigo   : string sin espacios (identifica al producto)
    Nombre   : string con espacios
    Cantidad : entero
    precio   : float
    total    : float, calculado por el programa como Cantidad * precio

JSON ya distingue texto, entero y decimal, así que al leer no hay que
convertir cadenas. Un archivo JSON válido es un solo valor: no se puede
adicionar un producto al final como en el CSV. Cada cambio reescribe
la lista completa.

Ejecute con:  python filejason.py
"""

import json
from pathlib import Path

# Archivo de datos junto al script (funciona igual en Windows y Linux)
ARCHIVO = Path(__file__).resolve().parent / "producto.json"


# -------------------------------------------------------------------
# Lectura y escritura del archivo
# -------------------------------------------------------------------
def leer_productos():
    """Devuelve la lista de productos leída de producto.json."""
    if not ARCHIVO.exists() or ARCHIVO.stat().st_size == 0:
        return []                             # archivo aún no creado
    with ARCHIVO.open("r", encoding="utf-8") as f:
        datos = json.load(f)
    if not isinstance(datos, list):
        raise ValueError("producto.json debe contener una lista de productos.")
    productos = []
    for fila in datos:
        productos.append({
            "codigo": str(fila["codigo"]),
            "Nombre": str(fila["Nombre"]),
            "Cantidad": int(fila["Cantidad"]),
            "precio": float(fila["precio"]),
            "total": float(fila["total"]),
        })
    return productos


def reescribir_productos(productos):
    """Reescribe el archivo completo con la lista de productos."""
    with ARCHIVO.open("w", encoding="utf-8") as f:
        json.dump(productos, f, ensure_ascii=False, indent=2)
        f.write("\n")


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
    while True:
        print("\n===== GESTIÓN DE PRODUCTOS (producto.json) =====")
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
