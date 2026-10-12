public class circulo extends punto {
    private double radio;

    public circulo() {
    }

    public circulo(double x, double y, double radio) {
        super(x, y);
        this.radio = radio;
    }

    public double getRadio() {
        return radio;
    }

    public void setRadio(double radio) {
        this.radio = radio;
    }

    @Override
    public String ObtenerNombre() {
        return "Circulo";
    }

    @Override
    public double ObtenerArea() {
        return Math.PI * radio * radio;
    }

    @Override
    public double ObtenerPerimetro() {
        return 2 * Math.PI * radio;
    }

    @Override
    public double ObtenerVolumen() {
        return 0;
    }
}
