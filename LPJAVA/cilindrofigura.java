public class cilindrofigura extends circulo {
    private double altura;

    public cilindrofigura() {
    }

    public cilindrofigura(double x, double y, double radio, double altura) {
        super(x, y, radio);
        this.altura = altura;
    }

    public double getAltura() {
        return altura;
    }

    public void setAltura(double altura) {
        this.altura = altura;
    }

    @Override
    public String ObtenerNombre() {
        return "Cilindro";
    }

    @Override
    public double ObtenerArea() {
        return 2 * Math.PI * getRadio() * getRadio() + 2 * Math.PI * getRadio() * altura;
    }

    @Override
    public double ObtenerPerimetro() {
        return 0;
    }

    @Override
    public double ObtenerVolumen() {
        return Math.PI * getRadio() * getRadio() * altura;
    }
}
