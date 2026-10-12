public class cilindro {
    private double radio;
    private double altura;

    public cilindro() {
    }

    public cilindro(double radio) {
        this.radio = radio;
    }

    public cilindro(double radio, double altura) {
        this.radio = radio;
        this.altura = altura;
    }

    public double getRadio() {
        return this.radio;
    }

    public double getAltura() {
        return this.altura;
    }

    public void setRadio(double radio) {
        this.radio = radio;
    }

    public void setAltura(double altura) {
        this.altura = altura;
    }

    public double calcularAreaBase() {
        return Math.PI * radio * radio;
    }

    public double calcularArea() {
        return 2 * calcularAreaBase() + calcularAreaLateral();
    }

    public double calcularVolumen() {
        return Math.PI * radio * radio * altura;
    }

    public double calcularAreaLateral() {
        return 2 * Math.PI * radio * altura;
    }

    public void imprimir(double radio, double altura) {
        System.out.println("Radio: " + radio);
        System.out.println("Altura: " + altura);
    }

    public void imprimir() {
        imprimir(this.radio, this.altura);
        System.out.println("Area: " + calcularArea());
        System.out.println("Volumen: " + calcularVolumen());
    }
}
