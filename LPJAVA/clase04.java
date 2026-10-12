import java.util.Scanner;
import java.text.NumberFormat; // clase para el formato numérico
import java.util.Locale; // clase para información específica de cada país

public class clase04 {
    public static void main(String[] args) {
        double radio, altura, arealateral, areatotal, volumen;
        NumberFormat nf = NumberFormat.getInstance(Locale.GERMANY);
        NumberFormat nm = NumberFormat.getCurrencyInstance(Locale.CHINESE);
        nf.setMaximumFractionDigits(2);
        Scanner sc = new Scanner(System.in);
        System.out.print("Ingrese radio : ");
        radio = sc.nextDouble();
        System.out.print("Ingrese altura : ");
        altura = sc.nextDouble();
        arealateral = 2 * Math.PI * radio * altura;
        areatotal = 2 * arealateral + 2 * Math.PI * Math.pow(radio, 2);
        volumen = Math.PI * Math.pow(radio, 2) * altura;
        System.out.println("El area lateral es " + nf.format(arealateral));
        System.out.println("El area total es " + nm.format(areatotal));
        System.out.println("El volumen es " + String.format("%17.3f", volumen));
        sc.close();

    }
}
