class Gerente extends EmpleadoTiempoCompleto {
    private double bonoAnual;

    public Gerente() {
    }

    public Gerente(String nombre, String apellido, String dni, double salarioBase, double bonoAnual) {
        super(nombre, apellido, dni, salarioBase);
        if (bonoAnual < 0.00)
            this.bonoAnual = 0.00;
        else
            this.bonoAnual = bonoAnual;
    }

    public double getBonoAnual() {
        return bonoAnual;
    }

    public void setBonoAnual(double bonoAnual) {
        if (bonoAnual < 0.00)
            this.bonoAnual = 0.00;
        else
            this.bonoAnual = bonoAnual;
    }

    @Override
    public double calcularSalario() {
        return super.calcularSalario() + bonoAnual;
    }

    @Override
    public void mostrarInformacion() {
        super.mostrarInformacion();
        System.out.println("Bono Anual: " + bonoAnual);
        System.out.println("Sueldo: " + calcularSalario());
    }
}
