package com.example.papeleriacampus;

import java.math.BigDecimal;


public record Producto(Integer idProducto, String codigo, String nombre,
                       String categoria, BigDecimal precio, int stock) { }
