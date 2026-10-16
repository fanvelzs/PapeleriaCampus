package com.example.papeleriacampus;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.*;
import java.util.List;
import java.util.logging.*;
import static org.junit.jupiter.api.Assertions.*;

@EnabledIfEnvironmentVariable(named="CAMPUS_TEST_DB_URL", matches=".+")
class ProductoDAOIntegrationTest {
    static Connection connection() throws SQLException {
        String url = System.getenv("CAMPUS_TEST_DB_URL");
        if (url == null || !url.matches("jdbc:postgresql://(?:localhost|127\\.0\\.0\\.1):[0-9]+/papeleria_campus_pruebas"))
            throw new IllegalStateException("Las pruebas requieren una base local llamada papeleria_campus_pruebas.");
        return DriverManager.getConnection(url, System.getenv().getOrDefault("CAMPUS_TEST_DB_USER", "postgres"),
                System.getenv().getOrDefault("CAMPUS_TEST_DB_PASSWORD", ""));
    }
    static ProductoDAO dao() { return new ProductoDAO(ProductoDAOIntegrationTest::connection); }
    static void preparar() throws Exception {
        try (Connection c = connection(); Statement s = c.createStatement()) {
            s.execute("DROP TABLE IF EXISTS producto");
            s.execute(Files.readString(Path.of("sql/estructura.sql"), StandardCharsets.UTF_8));
            s.execute(Files.readString(Path.of("sql/datos.sql"), StandardCharsets.UTF_8));
        }
    }
    @BeforeEach void reset() throws Exception { preparar(); }
    @AfterEach void restaurar() throws Exception { preparar(); }
    private static List<String> codes(List<Producto> p) { return p.stream().map(Producto::codigo).toList(); }
    static Producto prueba() {
        return ProductoValidator.validar(null, "X001", "Producto de prueba", "Accesorios", "10.00", "1");
    }

    @Test void b1AB7ConsultaRealYRecargaExterna() throws Exception {
        ProductoDAO d = dao();
        assertEquals(List.of("P001", "P002"), codes(d.buscar("CUADERNO", null, false)));
        assertEquals(List.of("T001", "T002", "T003", "T004"), codes(d.buscar("T00", null, false)));
        assertEquals(List.of("P001", "P002", "P003", "P004"), codes(d.buscar("", "Papelería", false)));
        List<Producto> stock = d.buscar("", null, true);
        assertEquals(9, stock.size()); assertTrue(stock.stream().allMatch(p -> p.stock() > 0));
        assertEquals(List.of("T001", "T003"), codes(d.buscar("USB", "Tecnología", true)));
        assertTrue(d.buscar("ZZZ999", null, false).isEmpty());
        assertEquals(12, d.buscar("", null, false).size());
        assertTrue(d.buscar("' OR 1=1 --", null, false).isEmpty());
        assertTrue(d.buscar("%", null, false).isEmpty());
        try (Connection c = connection(); PreparedStatement s = c.prepareStatement(
                "INSERT INTO producto(codigo,nombre,categoria,precio,stock) VALUES('EXT','Producto externo','Accesorios',1,1)")) {
            assertEquals(1, s.executeUpdate());
        }
        assertEquals(13, d.buscar("", null, false).size());
    }

    @Test void cicloCrudPersistenciaYFiltros() throws Exception {
        ProductoDAO d = dao();
        int id = d.insertar(prueba());
        assertEquals(13, dao().buscar("", null, false).size());
        assertEquals(new BigDecimal("10.00"), dao().buscar("X001", null, false).getFirst().precio());
        Producto modificado = ProductoValidator.validar(id, "X001", "Producto de prueba", "Accesorios", "15.50", "3");
        assertTrue(d.actualizar(modificado));
        assertEquals(modificado, dao().buscar("X001", null, false).getFirst());
        assertEquals(13, dao().buscar("", null, false).size());
        assertTrue(d.actualizar(ProductoValidator.validar(id,"X001","Producto de prueba","Accesorios","15.50","0")));
        assertTrue(d.buscar("X001", "Accesorios", true).isEmpty());
        assertTrue(d.eliminar(id)); assertEquals(12, dao().buscar("", null, false).size());
        assertFalse(d.actualizar(modificado)); assertFalse(d.eliminar(id));
    }

    @Test void v9DuplicadosYEdicionPropia() throws Exception {
        ProductoDAO d = dao();
        Producto dup = ProductoValidator.validar(null," p001 ","Duplicado","Papelería","1","0");
        assertEquals("23505", assertThrows(SQLException.class, () -> d.insertar(dup)).getSQLState());
        Producto propio = d.buscar("P001",null,false).getFirst();
        assertTrue(d.actualizar(propio));
        Producto otro = d.buscar("P002",null,false).getFirst();
        assertEquals("23505", assertThrows(SQLException.class, () -> d.actualizar(
                new Producto(otro.idProducto(),"P001",otro.nombre(),otro.categoria(),otro.precio(),otro.stock()))).getSQLState());
        assertEquals(12,d.buscar("",null,false).size());
    }

    @Test void e2RestriccionesDirectasDelDao() throws Exception {
        ProductoDAO d = dao();
        Producto[] invalidos = {
            new Producto(null,"","Nombre","Accesorios",BigDecimal.ONE,1),
            new Producto(null," x ","Nombre","Accesorios",BigDecimal.ONE,1),
            new Producto(null,"x","Nombre","Accesorios",BigDecimal.ONE,1),
            new Producto(null,"X001","AB","Accesorios",BigDecimal.ONE,1),
            new Producto(null,"X001","Nombre","Otra",BigDecimal.ONE,1),
            new Producto(null,"X001","Nombre","Accesorios",BigDecimal.ZERO,1),
            new Producto(null,"X001","Nombre","Accesorios",BigDecimal.ONE,-1),
            new Producto(null,null,"Nombre","Accesorios",BigDecimal.ONE,1)
        };
        for (Producto p : invalidos) {
            assertTrue(assertThrows(SQLException.class, () -> d.insertar(p)).getSQLState().startsWith("23"));
            assertEquals(12,d.buscar("",null,false).size());
        }
    }

    @Test void e1E3E4ConexionCeroFilasYDiagnosticoSeguro() throws Exception {
        StringBuilder log = new StringBuilder();
        Logger logger = Logger.getLogger(ProductoDAO.class.getName());
        Handler h = new Handler() {
            public void publish(LogRecord r) { log.append(r.getMessage()); }
            public void flush() { }
            public void close() { }
        };
        logger.addHandler(h);
        try {
            ProductoDAO sinConexion = new ProductoDAO(() -> DriverManager.getConnection(
                    "jdbc:postgresql://127.0.0.1:1/papeleria_campus_pruebas?connectTimeout=1", "campus_test", ""));
            SQLException error = assertThrows(SQLException.class, () -> sinConexion.buscar("",null,false));
            assertTrue(error.getSQLState().startsWith("08"));
            assertTrue(PapeleriaCampusController.mensajeSeguro(error).contains("PostgreSQL"));
            assertTrue(log.toString().contains("Operación=buscar; SQLState=08"));
            ProductoDAO d = dao(); int id = d.insertar(prueba());
            try (Connection c=connection(); PreparedStatement s=c.prepareStatement("DELETE FROM producto WHERE id_producto=?")) {
                s.setInt(1,id); assertEquals(1,s.executeUpdate());
            }
            assertFalse(d.actualizar(new Producto(id,"X001","Producto de prueba","Accesorios",BigDecimal.TEN,1)));
            assertFalse(d.eliminar(id));
            assertEquals(12,d.buscar("",null,false).size());
        } finally { logger.removeHandler(h); }
    }
}
