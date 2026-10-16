package com.example.papeleriacampus;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public final class ProductoDAO {
    private static final Logger LOG = Logger.getLogger(ProductoDAO.class.getName());
    @FunctionalInterface
    public interface ConnectionFactory { Connection open() throws SQLException; }
    private final ConnectionFactory connections;
    public ProductoDAO() { this(DatabaseConnection::getConnection); }
    public ProductoDAO(ConnectionFactory connections) { this.connections = connections; }

    public List<Producto> buscar(String texto, String categoria, boolean soloExistencias) throws SQLException {
        String sql = "SELECT id_producto, codigo, nombre, categoria, precio, stock FROM producto "
                + "WHERE (nombre ILIKE ? ESCAPE '!' OR codigo ILIKE ? ESCAPE '!') "
                + "AND (CAST(? AS VARCHAR) IS NULL OR categoria = ?) AND (? = FALSE OR stock > 0) ORDER BY codigo ASC";
        String patron = "%" + (texto == null ? "" : texto.trim())
                .replace("!", "!!").replace("%", "!%").replace("_", "!_") + "%";
        try (Connection c = connections.open(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, patron); ps.setString(2, patron);
            ps.setString(3, categoria); ps.setString(4, categoria); ps.setBoolean(5, soloExistencias);
            try (ResultSet rs = ps.executeQuery()) {
                List<Producto> productos = new ArrayList<>();
                while (rs.next()) productos.add(new Producto(rs.getInt("id_producto"), rs.getString("codigo"),
                        rs.getString("nombre"), rs.getString("categoria"), rs.getBigDecimal("precio"), rs.getInt("stock")));
                return productos;
            }
        } catch (SQLException e) { throw registrar("buscar", e); }
    }

    public int insertar(Producto p) throws SQLException {
        String sql = "INSERT INTO producto (codigo, nombre, categoria, precio, stock) VALUES (?, ?, ?, ?, ?)";
        try (Connection c = connections.open();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            bind(ps, p);
            if (ps.executeUpdate() != 1) throw new SQLException("Inserción sin fila afectada.", "P0001");
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) return rs.getInt(1);
                throw new SQLException("No se recibió el identificador.", "P0001");
            }
        } catch (SQLException e) { throw registrar("insertar", e); }
    }

    public boolean actualizar(Producto p) throws SQLException {
        String sql = "UPDATE producto SET codigo=?, nombre=?, categoria=?, precio=?, stock=? WHERE id_producto=?";
        try (Connection c = connections.open(); PreparedStatement ps = c.prepareStatement(sql)) {
            bind(ps, p); ps.setInt(6, p.idProducto());
            return ps.executeUpdate() == 1;
        } catch (SQLException e) { throw registrar("actualizar", e); }
    }

    public boolean eliminar(int id) throws SQLException {
        try (Connection c = connections.open();
             PreparedStatement ps = c.prepareStatement("DELETE FROM producto WHERE id_producto=?")) {
            ps.setInt(1, id);
            return ps.executeUpdate() == 1;
        } catch (SQLException e) { throw registrar("eliminar", e); }
    }

    private static void bind(PreparedStatement ps, Producto p) throws SQLException {
        ps.setString(1, p.codigo()); ps.setString(2, p.nombre()); ps.setString(3, p.categoria());
        ps.setBigDecimal(4, p.precio()); ps.setInt(5, p.stock());
    }

    private static SQLException registrar(String operacion, SQLException e) {

        LOG.warning("Operación=" + operacion + "; SQLState=" + e.getSQLState());
        return e;
    }
}
