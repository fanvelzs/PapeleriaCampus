package com.example.papeleriacampus;

import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.WritableImage;
import javafx.stage.Stage;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.file.*;
import java.sql.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.zip.*;
import static org.junit.jupiter.api.Assertions.*;

/** Ejecuta el FXML y el controlador reales contra PostgreSQL local; capturas de Scene.snapshot. */
@EnabledIfEnvironmentVariable(named="CAMPUS_TEST_DB_URL", matches=".+")
class InterfazIntegrationTest {
    PapeleriaCampusController c;
    Stage stage;
    Parent root;
    boolean confirmar;
    List<String> avisos = new ArrayList<>();

    @BeforeAll static void javafx() throws Exception {
        CountDownLatch started = new CountDownLatch(1);
        Platform.startup(() -> { Platform.setImplicitExit(false); started.countDown(); });
        assertTrue(started.await(15, TimeUnit.SECONDS));
    }
    @AfterAll static void terminar() { Platform.exit(); }
    static <T> T fx(Callable<T> action) throws Exception {
        FutureTask<T> task = new FutureTask<>(action); Platform.runLater(task); return task.get(20,TimeUnit.SECONDS);
    }
    void accion(Runnable action) throws Exception { fx(() -> { action.run(); return null; }); esperar(); }
    void esperar() throws Exception {
        long fin = System.nanoTime() + TimeUnit.SECONDS.toNanos(20);
        while (fx(() -> c.estaOcupado())) {
            if (System.nanoTime() > fin) fail("Tiempo de espera JDBC agotado");
            Thread.sleep(25);
        }
    }
    void abrir(ProductoDAO dao) throws Exception {
        fx(() -> {
            FXMLLoader loader = new FXMLLoader(HelloApplication.class.getResource("hello-view.fxml"));
            loader.setControllerFactory(type -> new PapeleriaCampusController(dao, avisos::add, p -> confirmar));
            root = loader.load(); c = loader.getController();
            Scene scene = new Scene(root,1180,780);
            scene.getStylesheets().add(HelloApplication.class.getResource("campus.css").toExternalForm());
            stage = new Stage(); stage.setScene(scene); stage.setTitle("Papelería Campus · Pruebas"); stage.show();
            return null;
        });
        esperar();
    }
    @BeforeEach void preparar() throws Exception { ProductoDAOIntegrationTest.preparar(); abrir(ProductoDAOIntegrationTest.dao()); }
    @AfterEach void limpiar() throws Exception {
        fx(() -> { stage.close(); return null; }); ProductoDAOIntegrationTest.preparar();
    }
    void filas(int total) throws Exception {
        fx(() -> { assertEquals(total,c.tabla.getItems().size()); assertEquals(total+" registros visibles",c.contador.getText()); return null; });
    }
    void llenar(String codigo, String precio, String stock) {
        c.codigo.setText(codigo); c.nombre.setText("Producto de prueba"); c.categoria.setValue("Accesorios");
        c.precio.setText(precio); c.stock.setText(stock);
    }
    void seleccionar(String codigo) {
        c.tabla.getSelectionModel().select(c.tabla.getItems().stream().filter(p -> p.codigo().equals(codigo)).findFirst().orElseThrow());
    }
    void captura(String nombre) throws Exception {
        fx(() -> {
            root.applyCss(); root.layout();
            escribirPNG(root.snapshot(null,null),Path.of("evidencias",nombre+".png")); return null;
        });
    }

    @Test void b1AB7YSeleccion() throws Exception {
        filas(12); captura("01-listado-inicial");
        assertEquals(List.of("A001","A002","A003","A004","P001","P002","P003","P004","T001","T002","T003","T004"),
                fx(() -> c.tabla.getItems().stream().map(Producto::codigo).toList()));
        accion(() -> { c.textoBusqueda.setText("CUADERNO"); c.buscar(); }); filas(2);
        accion(() -> { c.textoBusqueda.setText("T00"); c.buscar(); }); filas(4);
        accion(() -> { c.textoBusqueda.clear(); c.filtroCategoria.setValue("Papelería"); c.buscar(); }); filas(4);
        accion(() -> { c.filtroCategoria.setValue("Todas"); c.soloExistencias.setSelected(true); c.buscar(); }); filas(9);
        accion(() -> { c.textoBusqueda.setText("USB"); c.filtroCategoria.setValue("Tecnología"); c.buscar(); }); filas(2);
        assertEquals(List.of("T001","T003"), fx(() -> c.tabla.getItems().stream().map(Producto::codigo).toList()));
        captura("02-filtro-combinado");
        accion(c::nuevo); filas(2);
        assertEquals("USB",fx(() -> c.textoBusqueda.getText()));
        accion(() -> { c.soloExistencias.setSelected(false); c.filtroCategoria.setValue("Todas"); c.textoBusqueda.setText("ZZZ999"); c.buscar(); });
        filas(0); assertEquals("Sin coincidencias",fx(() -> c.estado.getText()));
        accion(c::limpiarFiltros); filas(12);
        assertEquals("",fx(() -> c.textoBusqueda.getText())); assertFalse(fx(() -> c.soloExistencias.isSelected()));
        accion(() -> seleccionar("P001"));
        fx(() -> {
            assertFalse(c.id.isEditable()); assertEquals("P001",c.codigo.getText());
            assertEquals("Cuaderno universitario",c.nombre.getText()); assertEquals("Papelería",c.categoria.getValue());
            assertEquals("65.00",c.precio.getText()); assertEquals("25",c.stock.getText()); return null;
        });
    }

    @Test void cicloCrudConCancelacionReaperturaYFiltros() throws Exception {
        accion(() -> { llenar(" x001 ","10.00","1"); c.guardar(); }); filas(13);
        captura("04-crud-creado");
        // Cerrar y volver a cargar el FXML representa un nuevo controlador y nuevas conexiones.
        fx(() -> { stage.close(); return null; }); abrir(ProductoDAOIntegrationTest.dao()); filas(13);
        accion(() -> { seleccionar("X001"); c.precio.setText("15.50"); c.stock.setText("3"); c.actualizar(); }); filas(13);
        assertEquals("Producto actualizado.",fx(() -> c.mensaje.getText())); captura("05-crud-actualizado");
        fx(() -> { stage.close(); return null; }); abrir(ProductoDAOIntegrationTest.dao());
        accion(() -> seleccionar("X001"));
        assertEquals("15.50",fx(() -> c.precio.getText())); assertEquals("3",fx(() -> c.stock.getText()));
        confirmar=false; accion(c::eliminar); filas(13); captura("06-crud-cancelado");
        assertEquals(1,ProductoDAOIntegrationTest.dao().buscar("X001",null,false).size());
        confirmar=true; accion(c::eliminar); filas(12); captura("07-crud-eliminado");
        fx(() -> { stage.close(); return null; }); abrir(ProductoDAOIntegrationTest.dao()); filas(12);
        accion(() -> { llenar("X001","10.00","1"); c.guardar(); });
        accion(() -> { c.soloExistencias.setSelected(true); c.buscar(); }); filas(10);
        accion(() -> { seleccionar("X001"); c.stock.setText("0"); c.actualizar(); }); filas(9);
        assertTrue(ProductoDAOIntegrationTest.dao().buscar("X001",null,true).isEmpty());
    }

    @Test void v1AV10MensajesYBaseSinCambios() throws Exception {
        accion(c::actualizar); assertTrue(avisos.getLast().contains("Seleccione"));
        accion(c::eliminar); assertTrue(avisos.getLast().contains("Seleccione"));
        String[][] invalidos = {
            {"", "Nombre", "Accesorios", "10", "1"}, {"   ", "Nombre", "Accesorios", "10", "1"},
            {"X".repeat(11),"Nombre","Accesorios","10","1"},
            {"X001","   ","Accesorios","10","1"}, {"X001","AB","Accesorios","10","1"},
            {"X001","N".repeat(81),"Accesorios","10","1"},
            {"X001","Nombre",null,"10","1"},
            {"X001","Nombre","Accesorios","abc","1"}, {"X001","Nombre","Accesorios","0","1"},
            {"X001","Nombre","Accesorios","-1","1"}, {"X001","Nombre","Accesorios","12.345","1"},
            {"X001","Nombre","Accesorios","100000000.00","1"},
            {"X001","Nombre","Accesorios","10","abc"}, {"X001","Nombre","Accesorios","10","2.5"},
            {"X001","Nombre","Accesorios","10","-1"}, {"X001","Nombre","Accesorios","10","2147483648"}
        };
        List<Producto> antes=ProductoDAOIntegrationTest.dao().buscar("",null,false);
        for (boolean editar : new boolean[]{false,true}) for (String[] v : invalidos) {
            int avisosAntes=avisos.size();
            accion(() -> {
                if (editar) seleccionar("P002"); else c.nuevo();
                c.codigo.setText(v[0]); c.nombre.setText(v[1]); c.categoria.setValue(v[2]); c.precio.setText(v[3]); c.stock.setText(v[4]);
                if (editar) c.actualizar(); else c.guardar();
            });
            assertEquals(avisosAntes+1,avisos.size()); assertFalse(avisos.getLast().isBlank());
            assertEquals(antes,ProductoDAOIntegrationTest.dao().buscar("",null,false));
        }
        assertFalse(fx(() -> c.categoria.isEditable()));
        accion(() -> { c.nuevo(); llenar("X001","12.345","1"); c.guardar(); }); captura("03-validacion-precio");
        accion(() -> { llenar(" p001 ","10","1"); c.guardar(); }); assertTrue(avisos.getLast().contains("Ya existe"));
        accion(() -> { seleccionar("P002"); c.codigo.setText("P001"); c.actualizar(); }); assertTrue(avisos.getLast().contains("Ya existe"));
        accion(() -> { seleccionar("P001"); c.actualizar(); }); assertEquals("Producto actualizado.",fx(() -> c.mensaje.getText()));
        accion(() -> { c.nuevo(); llenar("X001","10","0"); c.guardar(); }); filas(13);
    }

    @Test void e1ErrorConexionYE3FilaEliminadaExternamente() throws Exception {
        accion(() -> { llenar("X001","10","1"); c.guardar(); });
        accion(() -> seleccionar("X001"));
        int id=Integer.parseInt(fx(() -> c.id.getText()));
        assertTrue(ProductoDAOIntegrationTest.dao().eliminar(id));
        accion(c::actualizar); assertTrue(avisos.getLast().contains("ya no existe")); filas(12);
        accion(() -> { c.nuevo(); llenar("X001","10","1"); c.guardar(); });
        accion(() -> seleccionar("X001"));
        int otroId=Integer.parseInt(fx(() -> c.id.getText()));
        assertTrue(ProductoDAOIntegrationTest.dao().eliminar(otroId)); confirmar=true;
        accion(c::eliminar); assertTrue(avisos.getLast().contains("ya no existe")); filas(12);
        fx(() -> { stage.close(); return null; });
        abrir(new ProductoDAO(() -> DriverManager.getConnection(
                "jdbc:postgresql://127.0.0.1:1/papeleria_campus_pruebas?connectTimeout=1","campus_test","")));
        assertTrue(fx(() -> stage.isShowing())); assertTrue(avisos.getLast().contains("PostgreSQL"));
        assertEquals("Sin conexión · sin cargar",fx(() -> c.contador.getText())); captura("08-error-conexion");
    }

    // PNG sin dependencias gráficas adicionales. Cada captura proviene de píxeles de JavaFX.
    static void escribirPNG(WritableImage image, Path path) throws IOException {
        int w=(int)image.getWidth(), h=(int)image.getHeight();
        ByteArrayOutputStream raw=new ByteArrayOutputStream();
        try (DeflaterOutputStream z=new DeflaterOutputStream(raw)) {
            for(int y=0;y<h;y++) {
                z.write(0);
                for(int x=0;x<w;x++) {
                    int argb=image.getPixelReader().getArgb(x,y);
                    z.write((argb>>16)&255); z.write((argb>>8)&255); z.write(argb&255); z.write((argb>>>24)&255);
                }
            }
        }
        Files.createDirectories(path.getParent());
        try (DataOutputStream out=new DataOutputStream(Files.newOutputStream(path))) {
            out.write(new byte[]{(byte)137,80,78,71,13,10,26,10});
            chunk(out,"IHDR",ByteBuffer.allocate(13).putInt(w).putInt(h).put((byte)8).put((byte)6).put((byte)0).put((byte)0).put((byte)0).array());
            chunk(out,"IDAT",raw.toByteArray()); chunk(out,"IEND",new byte[0]);
        }
    }
    static void chunk(DataOutputStream out,String type,byte[] data) throws IOException {
        byte[] t=type.getBytes(java.nio.charset.StandardCharsets.US_ASCII);
        out.writeInt(data.length); out.write(t); out.write(data);
        CRC32 crc=new CRC32(); crc.update(t); crc.update(data); out.writeInt((int)crc.getValue());
    }
}
