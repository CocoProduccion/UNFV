import java.util.Scanner;

public class clase36 {
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        String correo;
        boolean ok;
        do {
            System.out.println("Ingrese un correo: ");
            correo = sc.nextLine().trim();
            ok = correo.matches("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$");
        } while (!ok);
        System.out.println("Correo valido: " + correo);
        sc.close();
    }

}
