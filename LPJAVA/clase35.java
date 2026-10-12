import java.util.Scanner;

public class clase35 {
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        String dni;
        boolean ok;
        do {
            System.out.println("Ingrese un DNI: ");
            dni = sc.nextLine().trim();
            ok = dni.matches("\\d{8}");
        } while (!ok);
        System.out.println("DNI valido: " + dni);
        sc.close();
    }

}
