class EmpleadoTiempoCompleto extends Empleado {
    private double salarioBase;

    public EmpleadoTiempoCompleto() {
    }

    public EmpleadoTiempoCompleto(String nombre, String apellido, String dni, double salarioBase) {
        super(nombre, apellido, dni);
        if (salarioBase < 0.00)
            this.salarioBase = 0.00;
        else
            this.salarioBase = salarioBase;
    }

    public double getSalarioBase() {
        return salarioBase;
    }

    public void setSalarioBase(double salarioBase) {
        if (salarioBase < 0.00)
            this.salarioBase = 0.00;
        else
            this.salarioBase = salarioBase;
    }

    @Override
    public double calcularSalario() {
        return salarioBase;
    }

    @Override
    public void mostrarInformacion() {
        super.imprimir();
        System.out.println("Salario Base: " + salarioBase);
    }
}
