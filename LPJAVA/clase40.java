import java.util.Scanner;

public class clase40 {
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        MatriculaEstado me = null;
        System.out.println("Muestrame los Estado de Matricula Autorizados");
        for (MatriculaEstado est : MatriculaEstado.values()) {
            System.out.println(est);
        }

        while (me == null) {
            try {
                System.out.println("Ingrese un Estado de Matricula: ");
                String est = sc.nextLine().trim().toUpperCase();
                me = MatriculaEstado.valueOf(est);
            } catch (IllegalArgumentException e) {
                System.out.println("Estado de Matricula no valido");
            }
        }

        sc.close();
    }
}

enum MatriculaEstado {
    PREMATRICULADO, MATRICULADO, RETIRADO, CERRADO
}