public class punto extends Object implements figurageometrica {
    private double x;
    private double y;

    public punto() {
    }

    public punto(double x, double y) {
        this.x = x;
        this.y = y;
    }

    public double getX() {
        return x;
    }

    public void setX(double x) {
        this.x = x;
    }

    public double getY() {
        return y;
    }

    public void setY(double y) {
        this.y = y;
    }

    @Override
    public String ObtenerNombre() {
        return "Punto";
    }

    @Override
    public double ObtenerArea() {
        return 0;
    }

    @Override
    public double ObtenerPerimetro() {
        return 0;
    }

    @Override
    public double ObtenerVolumen() {
        return 0;
    }
}
