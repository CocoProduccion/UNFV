public class clase16 {
    public static void main(String[] args) {
        // int depositos[];
        int num_depositos = 4;
        int depositos[] = new int[num_depositos];
        // depositos = new int[num_depositos]; // Igual que: int depositos[] = new
        // int[4];
        depositos[0] = depositos[1] = 300;
        depositos[2] = depositos[3] = 700;
        System.out.println("Litros del segundo deposito:" + depositos[1]);
        depositos[2] = (int) (depositos[2] * 1.5); // Incremento de un 50%
        System.out.println("Litros del tercer deposito:" + depositos[2]);
    }
}
