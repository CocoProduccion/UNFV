public class clase25 {
    public static void main(String[] args) {
        punto p1 = new punto(10, 20);
        circulo c1 = new circulo(10, 20, 30);
        cilindrofigura cf1 = new cilindrofigura(10, 20, 30, 40);

        figurageometrica figuras[] = new figurageometrica[3];
        figuras[0] = p1;
        figuras[1] = c1;
        figuras[2] = cf1;

        for (int i = 0; i < figuras.length; i++) {
            System.out.println("Nombre: " + figuras[i].ObtenerNombre());
            System.out.println("Area: " + figuras[i].ObtenerArea());
            System.out.println("Perimetro: " + figuras[i].ObtenerPerimetro());
            System.out.println("Volumen: " + figuras[i].ObtenerVolumen());
            System.out.println("-----------------------------");
        }
    }
}
