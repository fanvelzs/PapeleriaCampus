-- Ejecutar conectado a papeleria_campus. No elimina tablas existentes.
CREATE TABLE producto (
    id_producto INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    codigo VARCHAR(10) NOT NULL UNIQUE,
    nombre VARCHAR(80) NOT NULL,
    categoria VARCHAR(20) NOT NULL,
    precio NUMERIC(10,2) NOT NULL,
    stock INTEGER NOT NULL,
    CONSTRAINT ck_producto_codigo CHECK (
        char_length(codigo) BETWEEN 1 AND 10 AND codigo = btrim(codigo) AND codigo = upper(codigo)
    ),
    CONSTRAINT ck_producto_nombre CHECK (
        char_length(btrim(nombre)) BETWEEN 3 AND 80 AND nombre = btrim(nombre)
    ),
    CONSTRAINT ck_producto_categoria CHECK (categoria IN ('Papelería', 'Tecnología', 'Accesorios')),
    CONSTRAINT ck_producto_precio CHECK (precio > 0),
    CONSTRAINT ck_producto_stock CHECK (stock >= 0)
);
