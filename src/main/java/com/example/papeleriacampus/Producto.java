package com.example.papeleriacampus;

import java.math.BigDecimal;

/** El ID lo asigna PostgreSQL; el precio nunca se representa con double. */
public record Producto(Integer idProducto, String codigo, String nombre,
                       String categoria, BigDecimal precio, int stock) { }
