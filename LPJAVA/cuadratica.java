public class cuadratica {
    private double a;
    private double b;
    private double c;

    public cuadratica() {
    }

    public cuadratica(double a, double b, double c) {
        this.a = a;
        this.b = b;
        this.c = c;
    }

    public double getA() {
        return this.a;
    }

    public double getB() {
        return this.b;
    }

    public double getC() {
        return this.c;
    }

    public void setA(double a) {
        this.a = a;
    }

    public void setB(double b) {
        this.b = b;
    }

    public void setC(double c) {
        this.c = c;
    }

    private double discriminante() {
        return b * b - 4 * a * c;
    }

    private double calcularRaiz1() {
        return (-b + Math.sqrt(discriminante())) / (2 * a);
    }

    private double calcularRaiz2() {
        return (-b - Math.sqrt(b * b - 4 * a * c)) / (2 * a);
    }

    private double calcularReal() {
        return (-b / (2 * a));
    }

    private double calcularImaginario() {
        return (Math.sqrt(Math.abs(discriminante())) / (2 * a));
    }

    public void imprimir() {
        if (discriminante() > 0) {
            System.out.println("Raiz 1: " + calcularRaiz1());
            System.out.println("Raiz 2: " + calcularRaiz2());
        } else {
            System.out.println("Raiz 1: " + calcularReal() + " + " + calcularImaginario() + "i");
            System.out.println("Raiz 2: " + calcularReal() + " - " + calcularImaginario() + "i");
        }
    }

    protected void finalize() {
        System.out.println("Nos fuimos Adios");
    }
}
