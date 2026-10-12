import java.util.StringTokenizer;

public class clase15 {
    public static void main(String[] args) {
        String datosAlumno = "20261001;Coco Pastor;Java Basico;18";

        StringTokenizer separador = new StringTokenizer(datosAlumno, ";");

        System.out.println("Cantidad de datos: " + separador.countTokens());

        String codigo = separador.nextToken();
        String nombre = separador.nextToken();
        String curso = separador.nextToken();
        String notaTexto = separador.nextToken();

        int nota = Integer.parseInt(notaTexto);

        System.out.println("----------------------------");
        System.out.println("DATOS DEL ALUMNO");
        System.out.println("----------------------------");
        System.out.println("Código : " + codigo);
        System.out.println("Nombre : " + nombre);
        System.out.println("Curso  : " + curso);
        System.out.println("Nota   : " + nota);

        if (nota >= 11) {
            System.out.println("Estado : Aprobado");
        } else {
            System.out.println("Estado : Desaprobado");
        }

    }
}
