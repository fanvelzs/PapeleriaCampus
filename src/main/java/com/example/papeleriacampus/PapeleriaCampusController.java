package com.example.papeleriacampus;

import javafx.beans.property.ReadOnlyIntegerWrapper;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.function.Consumer;

public final class PapeleriaCampusController {
    @FXML TableView<Producto> tabla;
    @FXML TableColumn<Producto, Number> colId, colStock;
    @FXML TableColumn<Producto, String> colCodigo, colNombre, colCategoria;
    @FXML TableColumn<Producto, BigDecimal> colPrecio;
    @FXML TextField textoBusqueda, id, codigo, nombre, precio, stock;
    @FXML ComboBox<String> filtroCategoria, categoria;
    @FXML CheckBox soloExistencias;
    @FXML Label contador, estado, mensaje, modo;
    @FXML HBox filtros;
    @FXML VBox formulario;
    private final ObservableList<Producto> productos = FXCollections.observableArrayList();
    private final ProductoDAO dao;
    private final Consumer<String> avisos;
    private final java.util.function.Predicate<Producto> confirmar;
    private Integer seleccionado;
    private boolean ocupado;
    private boolean inicializado;

    public PapeleriaCampusController() { this(new ProductoDAO(), null, null); }
    PapeleriaCampusController(ProductoDAO dao, Consumer<String> avisos,
                             java.util.function.Predicate<Producto> confirmar) {
        this.dao = dao; this.avisos = avisos; this.confirmar = confirmar;
    }

    @FXML void initialize() {

        tabla.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        colId.setCellValueFactory(c -> new ReadOnlyIntegerWrapper(c.getValue().idProducto()));
        colCodigo.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().codigo()));
        colNombre.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().nombre()));
        colCategoria.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().categoria()));
        colPrecio.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().precio()));
        colStock.setCellValueFactory(c -> new ReadOnlyIntegerWrapper(c.getValue().stock()));
        colPrecio.setCellFactory(c -> new TableCell<>() {
            @Override protected void updateItem(BigDecimal value, boolean empty) {
                super.updateItem(value, empty);
                setText(empty || value == null ? null : "C$ " + value.toPlainString());
            }
        });
        tabla.setItems(productos);
        tabla.setPlaceholder(new Label("Cargando productos…"));
        categoria.getItems().setAll(ProductoValidator.CATEGORIAS);
        filtroCategoria.getItems().add("Todas");
        filtroCategoria.getItems().addAll(ProductoValidator.CATEGORIAS);
        filtroCategoria.setValue("Todas");
        tabla.getSelectionModel().selectedItemProperty().addListener((o, old, p) -> {
            if (p != null) cargarFormulario(p);
        });
        nuevo();
        recargar();
    }

    @FXML void nuevo() {
        if (ocupado) return;
        seleccionado = null;
        tabla.getSelectionModel().clearSelection();
        id.clear(); codigo.clear(); nombre.clear(); categoria.setValue(null); precio.clear(); stock.clear();
        modo.setText("Nuevo producto");
        mensaje.setText("Complete los cinco campos y pulse Guardar.");
    }

    private void cargarFormulario(Producto p) {
        seleccionado = p.idProducto(); id.setText(p.idProducto().toString());
        codigo.setText(p.codigo()); nombre.setText(p.nombre()); categoria.setValue(p.categoria());
        precio.setText(p.precio().toPlainString()); stock.setText(Integer.toString(p.stock()));
        modo.setText("Editar producto · " + p.codigo());
        mensaje.setText("Use Actualizar para guardar los cambios de la fila seleccionada.");
    }

    @FXML void buscar() { recargar(); }
    @FXML void limpiarFiltros() {
        if (ocupado) return;
        textoBusqueda.clear(); filtroCategoria.setValue("Todas"); soloExistencias.setSelected(false);
        recargar();
    }

    @FXML void recargar() {
        if (ocupado) return;
        String texto = textoBusqueda.getText();
        String filtro = "Todas".equals(filtroCategoria.getValue()) ? null : filtroCategoria.getValue();
        boolean existencias = soloExistencias.isSelected();
        ejecutar(() -> dao.buscar(texto, filtro, existencias), lista -> {
            mostrarLista(lista);
            estado.setText(lista.isEmpty() ? "Sin coincidencias" : "Consulta completada · PostgreSQL");
        });
    }

    private void mostrarLista(List<Producto> lista) {
        Integer conservar = seleccionado;
        tabla.getSortOrder().clear();
        productos.setAll(lista);
        contador.setText(lista.size() + " registros visibles");
        tabla.setPlaceholder(new Label("Sin coincidencias"));
        seleccionado = null;
        for (Producto p : lista) {
            if (p.idProducto().equals(conservar)) { tabla.getSelectionModel().select(p); return; }
        }
        nuevo();
    }

    @FXML void guardar() {
        if (ocupado) return;
        if (seleccionado != null) {
            avisar("Pulse Nuevo / Limpiar formulario antes de crear otro producto. Para editar use Actualizar.");
            return;
        }
        Producto p = validar();
        if (p != null) escribir(() -> { dao.insertar(p); return true; }, "Producto guardado.");
    }

    @FXML void actualizar() {
        if (ocupado || !exigirSeleccion()) return;
        Producto p = validar();
        if (p != null) escribir(() -> dao.actualizar(p), "Producto actualizado.");
    }

    @FXML void eliminar() {
        if (ocupado || !exigirSeleccion()) return;
        Producto p = tabla.getSelectionModel().getSelectedItem();
        boolean aceptar;
        if (confirmar != null) aceptar = confirmar.test(p);
        else {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION,
                    "¿Eliminar " + p.codigo() + " · " + p.nombre() + "?", ButtonType.OK, ButtonType.CANCEL);
            alert.setTitle("Confirmar eliminación"); alert.setHeaderText("Eliminar producto");
            alert.initOwner(tabla.getScene().getWindow());
            aceptar = alert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK;
        }
        if (aceptar) escribir(() -> dao.eliminar(p.idProducto()), "Producto eliminado.");
        else mensaje.setText("Eliminación cancelada. El producto se conserva.");
    }

    private boolean exigirSeleccion() {
        if (seleccionado != null) return true;
        avisar("Seleccione un producto de la tabla para actualizar o eliminar.");
        return false;
    }

    private Producto validar() {
        try {
            return ProductoValidator.validar(seleccionado, codigo.getText(), nombre.getText(),
                    categoria.getValue(), precio.getText(), stock.getText());
        } catch (IllegalArgumentException e) { avisar(e.getMessage()); return null; }
    }

    private void escribir(Callable<Boolean> operacion, String exito) {
        String texto = textoBusqueda.getText();
        String filtro = "Todas".equals(filtroCategoria.getValue()) ? null : filtroCategoria.getValue();
        boolean existencias = soloExistencias.isSelected();
        ejecutar(() -> {
            boolean cambiado = operacion.call();

            try { return new Resultado(cambiado, dao.buscar(texto, filtro, existencias), null); }
            catch (SQLException e) { return new Resultado(cambiado, null, e); }
        }, r -> {
            if (r.lista() != null) mostrarLista(r.lista());
            if (r.error() != null) {
                contador.setText("Listado sin verificar");
                estado.setText("Falló la recarga; pulse Recargar.");
                avisar(r.cambiado() ? "El cambio se guardó, pero no se pudo recargar. Pulse Recargar."
                        : "El producto ya no existe y no se pudo recargar. Pulse Recargar.");
            } else if (!r.cambiado()) {
                avisar("El producto ya no existe. Se recargó el listado; seleccione otra fila.");
            } else {
                mensaje.setText(exito); estado.setText("Listado actualizado · PostgreSQL");
            }
        });
    }

    private record Resultado(boolean cambiado, List<Producto> lista, SQLException error) { }

    private <T> void ejecutar(Callable<T> operacion, Consumer<T> alTerminar) {
        ocupado = true; filtros.setDisable(true); formulario.setDisable(true); tabla.setDisable(true);
        estado.setText("Consultando PostgreSQL…");
        Task<T> task = new Task<>() { @Override protected T call() throws Exception { return operacion.call(); } };
        task.setOnSucceeded(e -> { desbloquear(); inicializado = true; alTerminar.accept(task.getValue()); });
        task.setOnFailed(e -> {
            desbloquear();
            estado.setText("Operación fallida · revise la conexión o los datos");
            contador.setText(inicializado ? "Listado anterior · sin verificar" : "Sin conexión · sin cargar");
            if (!inicializado) tabla.setPlaceholder(new Label("No se pudo cargar. Revise la conexión y pulse Recargar."));
            avisar(mensajeSeguro(task.getException()));
        });
        Thread thread = new Thread(task, "campus-jdbc"); thread.setDaemon(true); thread.start();
    }

    private void desbloquear() {
        ocupado = false; filtros.setDisable(false); formulario.setDisable(false); tabla.setDisable(false);
    }
    boolean estaOcupado() { return ocupado; }

    static String mensajeSeguro(Throwable error) {
        if (error instanceof SQLException sql) {
            String state = sql.getSQLState() == null ? "" : sql.getSQLState();
            if (state.equals("23505")) return "Ya existe un producto con ese código. Use otro código.";
            if (state.startsWith("23") || state.startsWith("22"))
                return "Los datos incumplen una restricción. Revise código, nombre, categoría, precio y existencias.";
            if (state.equals("42P01")) return "No se encontró la tabla producto. Ejecute los scripts de estructura y datos.";
            return "No se pudo acceder a PostgreSQL. Revise el servicio, el puerto, la base y las credenciales; pulse Recargar.";
        }
        return "No se pudo completar la operación. Revise los datos y vuelva a intentarlo.";
    }

    private void avisar(String texto) {
        mensaje.setText(texto);
        if (avisos != null) avisos.accept(texto);
    }
}
