public class ExcepcionIntervalo extends Exception {
    public ExcepcionIntervalo() {
        super("El numero esta fuera del intervalo");
    }

    public ExcepcionIntervalo(String mensaje) {
        super(mensaje);
    }
}
