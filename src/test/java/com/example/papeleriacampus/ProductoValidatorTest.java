package com.example.papeleriacampus;

import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import static org.junit.jupiter.api.Assertions.*;

class ProductoValidatorTest {
    @Test void normalizaYConservaLimites() {
        Producto p = ProductoValidator.validar(7, " p001 ", " Cuaderno ", "Papelería", "0.01", "0");
        assertEquals("P001", p.codigo()); assertEquals("Cuaderno", p.nombre());
        assertEquals(7, p.idProducto()); assertEquals(0, p.stock());
        Producto max = ProductoValidator.validar(null, "X".repeat(10), "N".repeat(80), "Accesorios", "99999999.99", "2147483647");
        assertEquals(new BigDecimal("99999999.99"), max.precio());
    }

    @Test void v1AV8RechazanTodosLosSubcasosAlCrearYActualizar() {
        String[][] casos = {
            {"", "Nombre", "Papelería", "10", "1"}, {"   ", "Nombre", "Papelería", "10", "1"},
            {"X".repeat(11), "Nombre", "Papelería", "10", "1"},
            {"X001", "   ", "Papelería", "10", "1"}, {"X001", "AB", "Papelería", "10", "1"},
            {"X001", "N".repeat(81), "Papelería", "10", "1"},
            {"X001", "Nombre", null, "10", "1"}, {"X001", "Nombre", "Otra", "10", "1"},
            {"X001", "Nombre", "Papelería", "abc", "1"},
            {"X001", "Nombre", "Papelería", "0", "1"}, {"X001", "Nombre", "Papelería", "-1", "1"},
            {"X001", "Nombre", "Papelería", "12.345", "1"},
            {"X001", "Nombre", "Papelería", "100000000.00", "1"},
            {"X001", "Nombre", "Papelería", "10", "abc"}, {"X001", "Nombre", "Papelería", "10", "2.5"},
            {"X001", "Nombre", "Papelería", "10", "-1"}, {"X001", "Nombre", "Papelería", "10", "2147483648"}
        };
        for (Integer id : new Integer[]{null, 1}) for (String[] c : casos) {
            IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                    () -> ProductoValidator.validar(id, c[0], c[1], c[2], c[3], c[4]));
            assertFalse(e.getMessage().isBlank());
        }
    }

    @Test void noAceptaNotacionCientificaNiComas() {
        for (String precio : new String[]{"1e2", "12,50", "NaN", "Infinity"})
            assertThrows(IllegalArgumentException.class,
                    () -> ProductoValidator.validar(null, "X001", "Producto", "Accesorios", precio, "0"));
    }
}
