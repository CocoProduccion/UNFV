import java.util.Scanner;

public class clase02 {
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        int s1, s2, suma;
        System.out.print("Sumando 1 : ");
        s1 = sc.nextInt();
        System.out.print("Sumando 2 : ");
        s2 = sc.nextInt();
        suma = s1 + s2;
        System.out.println("La suma es " + s1 + "+" + s2 + "=" + suma);
        sc.close();

    }
}