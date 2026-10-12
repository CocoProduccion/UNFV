import java.util.Scanner;

public class clase03 {
    public static void main(String[] args) {
        double radio, altura, arealateral, areatotal, volumen;
        Scanner sc = new Scanner(System.in);
        System.out.print("Ingrese radio : ");
        radio = sc.nextDouble();
        System.out.print("Ingrese altura : ");
        altura = sc.nextDouble();
        arealateral = 2 * Math.PI * radio * altura;
        areatotal = 2 * arealateral + 2 * Math.PI * Math.pow(radio, 2);
        volumen = Math.PI * Math.pow(radio, 2) * altura;
        System.out.println("El area lateral es " + arealateral);
        System.out.println("El area total es " + areatotal);
        System.out.println("El volumen es " + volumen);
        sc.close();
    }
}
