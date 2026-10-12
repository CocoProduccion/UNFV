public class clase22 {
    public static void main(String[] args) {
        cilindro objeto_cilindro = new cilindro();
        cilindro objeto_cilindro_1 = new cilindro(5);
        cilindro objeto_cilindro_2 = new cilindro(5, 2);
        objeto_cilindro_1.setAltura(70);
        objeto_cilindro.setRadio(10);
        objeto_cilindro.setAltura(20);
        System.out.println("Imprimiendo el objeto 1 ");
        objeto_cilindro.imprimir();
        System.out.println("\nImprimiendo el objeto 2 ");
        objeto_cilindro_1.imprimir();
        System.out.println("\nImprimiendo el objeto 3 ");
        objeto_cilindro_2.imprimir();

    }
}
