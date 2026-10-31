import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/** Pruebas repetibles, sin JUnit ni bibliotecas externas. */
public class PruebasPrograma5Csv {
    private static int comprobaciones;

    private static void verificar(boolean condicion, String mensaje) {
        comprobaciones++;
        if (!condicion) throw new AssertionError(mensaje);
    }

    private static void rechazar(Runnable accion, String mensaje) {
        boolean fallo = false;
        try { accion.run(); }
        catch (IllegalArgumentException | IllegalStateException e) { fallo = true; }
        verificar(fallo, mensaje);
    }

    public static void main(String[] args) throws Exception {
        Path carpeta = Files.createTempDirectory("pruebas-csv-");
        Path archivo = carpeta.resolve("productos.csv");
        var repo = new Programa5Csv.RepositorioCsv(archivo);
        var servicio = new Programa5Csv.ServicioProductos(repo);
        try {
            verificar(!repo.existe(), "Inicialmente no existe el archivo");
            verificar(servicio.listar().isEmpty(), "Archivo ausente se lee como lista vacía");
            servicio.crearArchivo();
            verificar(repo.existe(), "Crear genera el archivo");
            verificar(Files.readString(archivo).equals(
                    "codigo,nombre,cantidad,precio,total\r\n"), "Cabecera y CRLF");
            String nombre = "Aceite \"Premium\", botella; 1 L";
            var p = new Programa5Csv.Producto("p001", nombre, 5, 9.90);
            servicio.anadir(p);
            verificar(servicio.buscar("p001").orElseThrow().nombre().equals(nombre),
                    "Ida y vuelta de comas, comillas y punto y coma");
            verificar(Files.readString(archivo).contains(
                    "\"Aceite \"\"Premium\"\", botella; 1 L\""), "Comillas duplicadas");
            verificar(servicio.buscar("P001").orElseThrow().total() == 49.5, "Total inicial");
            rechazar(() -> servicio.anadir(p), "Código repetido rechazado");
            servicio.modificar(new Programa5Csv.Producto("P001", "Arroz extra", 8, 2.555));
            var modificado = servicio.buscar("P001").orElseThrow();
            verificar(modificado.precio() == 2.56, "Precio redondeado a 2 decimales");
            verificar(Math.abs(modificado.total() - 20.48) < 1e-9, "Total modificado");
            verificar(servicio.buscar("NO").isEmpty(), "Búsqueda inexistente");
            rechazar(() -> servicio.modificar(
                    new Programa5Csv.Producto("NO", "No existe", 0, 0)), "Modificar inexistente");
            verificar(!servicio.eliminar("NO"), "Eliminar inexistente devuelve false");
            verificar(servicio.eliminar("p001"), "Eliminar normaliza código");
            verificar(servicio.listar().isEmpty(), "Eliminar deja la colección vacía");
            servicio.anadir(new Programa5Csv.Producto("Ñ1", "Línea uno\r\nLínea dos", 0, 0));
            verificar(servicio.buscar("ñ1").orElseThrow().nombre().contains("\r\n"),
                    "Texto multilínea y Unicode");
            servicio.anadir(new Programa5Csv.Producto("A,\"B", "Café, azúcar y té", 1, 0.1));
            verificar(servicio.buscar("a,\"b").isPresent(), "CSV escapa también el código");
            var recargado = new Programa5Csv.RepositorioCsv(archivo);
            verificar(recargado.cargarTodos().size() == 2, "Persistencia tras reabrir");
            rechazar(() -> new Programa5Csv.Producto("A 1", "x", 1, 1), "Espacio en código");
            rechazar(() -> new Programa5Csv.Producto("A\u00A01", "x", 1, 1),
                    "Espacio Unicode en código");
            rechazar(() -> new Programa5Csv.Producto("", "x", 1, 1), "Código vacío");
            rechazar(() -> new Programa5Csv.Producto("A1", "   ", 1, 1), "Nombre vacío");
            rechazar(() -> new Programa5Csv.Producto("A1", "x", -1, 1), "Cantidad negativa");
            rechazar(() -> new Programa5Csv.Producto("A1", "x", 1, -0.01), "Precio negativo");
            rechazar(() -> new Programa5Csv.Producto("A1", "x", 1, Double.NaN), "NaN");
            rechazar(() -> new Programa5Csv.Producto("A1", "x", 1, Double.POSITIVE_INFINITY),
                    "Infinito");
            rechazar(() -> new Programa5Csv.Producto("A1", "x", 2, Double.MAX_VALUE),
                    "Total desbordado");
            verificar(Programa5Csv.RepositorioCsv.analizar("a,b,\r\n").getFirst()
                    .equals(List.of("a", "b", "")), "Campo final vacío");
            verificar(Programa5Csv.RepositorioCsv.analizar("\"\"\n").getFirst()
                    .equals(List.of("")), "Campo vacío entrecomillado");
            rechazar(() -> Programa5Csv.RepositorioCsv.analizar("\"sin cerrar"),
                    "Comillas sin cierre");
            rechazar(() -> Programa5Csv.RepositorioCsv.analizar("\"a\"x,b"),
                    "Texto después del cierre");
            rechazar(() -> Programa5Csv.RepositorioCsv.analizar("a\"b,c"),
                    "Comilla dentro de campo simple");
            String cabecera = "codigo,nombre,cantidad,precio,total\n";
            Files.writeString(archivo, "\uFEFF" + cabecera + "P9,Arroz,2,1.50,999\n",
                    StandardCharsets.UTF_8);
            verificar(servicio.buscar("P9").orElseThrow().total() == 3, "BOM, LF y total derivado");
            Files.writeString(archivo, cabecera + "P9,Arroz,2,1.5,3\np9,Otro,1,1,1\n");
            String antes = Files.readString(archivo);
            rechazar(() -> servicio.anadir(p), "Duplicado importado aborta la carga");
            verificar(Files.readString(archivo).equals(antes), "No sobrescribir CSV inválido");
            Files.writeString(archivo, cabecera + "P9,Arroz,-1,1.5,0\n");
            rechazar(repo::cargarTodos, "Cantidad importada inválida");
            Files.writeString(archivo, cabecera + "P9,Arroz,1,1.5\n");
            rechazar(repo::cargarTodos, "Número de columnas incorrecto");
            Files.writeString(archivo, "codigo,nombre\n");
            rechazar(repo::cargarTodos, "Cabecera incorrecta");
            Files.writeString(archivo, "");
            rechazar(repo::cargarTodos, "Archivo vacío sin cabecera");
            repo.crear();
            verificar(servicio.listar().isEmpty(), "Recrear vacía el archivo");
            try (var rutas = Files.list(carpeta)) {
                verificar(rutas.noneMatch(r -> r.toString().endsWith(".tmp")),
                        "Temporales eliminados");
            }
            System.out.println("PRUEBAS SUPERADAS: " + comprobaciones + " comprobaciones.");
        } finally {
            Files.deleteIfExists(archivo);
            Files.deleteIfExists(carpeta);
        }
    }
}
