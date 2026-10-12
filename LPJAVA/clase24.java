public class clase24 {
    public static void main(String[] args) {
        Empleado[] empleados = new Empleado[5];
        empleados[0] = new EmpleadoPorHoras("Juan", "Perez", "12345678", 10, 40);
        empleados[1] = new EmpleadoPorHoras("Maria", "Gomez", "87654321", 20, 40);
        empleados[2] = new EmpleadoTiempoCompleto("Pedro", "Ramirez", "12345678", 1000);
        empleados[3] = new EmpleadoTiempoCompleto("Ana", "Flores", "87654321", 2000);
        empleados[4] = new Gerente("Carlos", "Lopez", "12345678", 3000, 500);

        for (int i = 0; i < empleados.length; i++) {
            empleados[i].mostrarInformacion();
            System.out.println("-----------------------------");
        }
    }
}
