package com.example.papeleriacampus;

/** Proceso JVM independiente: comprueba lectura persistente tras salir del proceso de escritura. */
public final class PersistenciaProbe {
    public static void main(String[] args) throws Exception {
        var lista = ProductoDAOIntegrationTest.dao().buscar("", null, false);
        if (lista.size() != Integer.parseInt(args[0])) throw new IllegalStateException("Conteo inesperado");
        var temporal = lista.stream().filter(p -> p.codigo().equals("X001")).toList();
        if (args.length == 3) {
            if (temporal.size() != 1 || !temporal.getFirst().precio().toPlainString().equals(args[1])
                    || temporal.getFirst().stock() != Integer.parseInt(args[2]))
                throw new IllegalStateException("Producto no persistido");
        } else if (!temporal.isEmpty()) throw new IllegalStateException("Eliminación no persistida");
        System.out.println("PERSISTENCIA_OK");
    }
}
