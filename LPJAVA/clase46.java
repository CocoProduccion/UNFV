public class clase46 {
    static int totalObjetos = 0;
    static final String[] CATEGORIA = new String[3];
    static {
        System.out.println("Inicializando las Categorias");
        CATEGORIA[0] = "Productos Quimicos";
        CATEGORIA[1] = "Medicinas";
        CATEGORIA[2] = "Herramientas";
        System.out.println("Categorias preparadas : " + CATEGORIA.length);
    }
    String nombre;

    clase46(String nombre) {
        this.nombre = nombre;
        totalObjetos++;

    }

    public static void main(String[] args) {
        new clase46("Cloro Liquido");
        new clase46("Pernos");
        System.out.println("Total de Objetos =" + clase46.totalObjetos);

    }
}
