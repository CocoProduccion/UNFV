class EmpleadoPorHoras extends Empleado {
    private double tarifaPorHora;
    private int horasTrabajadas;

    public EmpleadoPorHoras() {
    }

    public EmpleadoPorHoras(String nombre, String apellido, String dni, double tarifaPorHora, int horasTrabajadas) {
        super(nombre, apellido, dni);
        if (tarifaPorHora < 0.00)
            this.tarifaPorHora = 0.00;
        else
            this.tarifaPorHora = tarifaPorHora;
        if (horasTrabajadas < 0.00)
            this.horasTrabajadas = 0;
        else
            this.horasTrabajadas = horasTrabajadas;
    }

    public double getTarifaPorHora() {
        return tarifaPorHora;
    }

    public void setTarifaPorHora(double tarifaPorHora) {
        if (tarifaPorHora < 0.00)
            this.tarifaPorHora = 0.00;
        else
            this.tarifaPorHora = tarifaPorHora;
    }

    public int getHorasTrabajadas() {
        return horasTrabajadas;
    }

    public void setHorasTrabajadas(int horasTrabajadas) {
        if (horasTrabajadas < 0.00)
            this.horasTrabajadas = 0;
        else
            this.horasTrabajadas = horasTrabajadas;
    }

    @Override
    public double calcularSalario() {
        return tarifaPorHora * horasTrabajadas;
    }

    @Override
    public void mostrarInformacion() {
        super.imprimir();
        System.out.println("Tarifa por hora: " + tarifaPorHora);
        System.out.println("Horas trabajadas: " + horasTrabajadas);
        System.out.println("Sueldo: " + calcularSalario());
    }

}
