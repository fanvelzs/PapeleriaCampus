package com.example.papeleriacampus;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;

public final class ProductoValidator {
    public static final List<String> CATEGORIAS = List.of("Papelería", "Tecnología", "Accesorios");
    private static final BigDecimal MAX_PRECIO = new BigDecimal("99999999.99");
    private ProductoValidator() { }

    public static Producto validar(Integer id, String codigo, String nombre, String categoria,
                                   String precioTexto, String stockTexto) {
        codigo = clean(codigo).toUpperCase(Locale.ROOT);
        nombre = clean(nombre);
        if (length(codigo) < 1 || length(codigo) > 10)
            throw new IllegalArgumentException("El código debe tener de 1 a 10 caracteres.");
        if (length(nombre) < 3 || length(nombre) > 80)
            throw new IllegalArgumentException("El nombre debe tener de 3 a 80 caracteres.");
        if (categoria == null || !CATEGORIAS.contains(categoria))
            throw new IllegalArgumentException("Seleccione Papelería, Tecnología o Accesorios.");
        String rawPrecio = clean(precioTexto);
        if (!rawPrecio.matches("-?[0-9]+(?:\\.[0-9]+)?"))
            throw new IllegalArgumentException("Ingrese un precio numérico usando punto decimal, por ejemplo 65.00.");
        BigDecimal precio = new BigDecimal(rawPrecio);
        if (precio.scale() > 2)
            throw new IllegalArgumentException("El precio admite como máximo dos decimales; no se redondea.");
        if (precio.signum() <= 0 || precio.compareTo(MAX_PRECIO) > 0)
            throw new IllegalArgumentException("El precio debe estar entre 0.01 y 99999999.99.");
        String rawStock = clean(stockTexto);
        if (!rawStock.matches("-?[0-9]+"))
            throw new IllegalArgumentException("Las existencias deben ser un número entero.");
        int stock;
        try { stock = Integer.parseInt(rawStock); }
        catch (NumberFormatException e) {
            throw new IllegalArgumentException("Las existencias deben estar entre 0 y 2147483647.");
        }
        if (stock < 0)
            throw new IllegalArgumentException("Las existencias deben estar entre 0 y 2147483647.");
        return new Producto(id, codigo, nombre, categoria, precio.setScale(2), stock);
    }

    private static String clean(String value) { return value == null ? "" : value.trim(); }
    private static int length(String value) { return value.codePointCount(0, value.length()); }
}
