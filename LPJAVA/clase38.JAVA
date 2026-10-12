import java.util.Scanner;
@FunctionalInterface
interface Igv {
    double calcular(double BaseImponible,double porcentaje);
}

public class clase38 {
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        double BaseImp,por;
        System.out.println("Ingrese la Base Imponible =");
        BaseImp =Double.parseDouble(sc.nextLine());
        System.out.println("Ingrese porcentaje del Impuesto =");
        por =Double.parseDouble(sc.nextLine());
        Igv igv=(base,porcentaje)->{
            return base*porcentaje/100;
        };
        double igv=igv.calcular(BaseImp,por);
        double total=BaseImp+igv;
        System.out.println("El igv es =",igv);
        System.out.println("El total es =",total);
        sc.close();
    }
}
