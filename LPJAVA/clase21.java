public class clase21 {
    public static void main(String[] args) {
        int M[][] = new int[3][];
        M[0] = new int[2];
        M[1] = new int[5];
        M[2] = new int[4];

        for (int i = 0; i < M.length; i++) {
            for (int j = 0; j < M[i].length; j++) {
                M[i][j] = (int) (Math.random() * 10);
            }
        }

        for (int i = 0; i < M.length; i++) {
            for (int j = 0; j < M[i].length; j++) {
                System.out.print(M[i][j] + " ");
            }
            System.out.println();
        }
    }
}
