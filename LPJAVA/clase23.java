public class clase23 {
    public static void main(String[] args) {
        cuadratica x = new cuadratica();
        cuadratica y = new cuadratica(1, 1, 1);
        x.setA(1);
        x.setB(5);
        x.setC(1);
        System.out.println("Imprimiendo Objelo X");
        x.imprimir();
        System.out.println("Imprimiendo Objelo Y");
        y.imprimir();
        // eliminado los objhetos de la memoria
        x = null;
        y = null;
        System.gc();
    }
}
