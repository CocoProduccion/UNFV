import java.util.Scanner;

public class clase39 {
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        String tarea, estado;
        Prioridad[] pri = Prioridad.values();
        System.out.println("Ingrese una tarea");
        tarea = sc.nextLine();
        System.out.println("Ingrese el estado de la Tarea");
        estado = sc.nextLine();
        for (int i = 0; i < pri.length; i++) {
            System.out.println(pri[i]);
        }
        for (Tamano tam : Tamano.values())
            System.out.println(tam);
        sc.close();
    }
}

enum Prioridad {
    BAJA, MEDIA, ALTA, URGENTE
}

enum Tamano {
    PEQUEÑO, MEDIANO, GRANDE
}