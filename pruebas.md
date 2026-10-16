# Evidencias de pruebas - Papelería Campus

**Autor:** Fanor Velasquez. **Grupo:** 3. **Ejecución:** 15/10/2026, America/Managua.

Entorno: Windows, Temurin 21.0.12, JavaFX 21.0.6, PostgreSQL 18 local en 127.0.0.1:55432, JDBC 42.7.3, Maven 3.8.5 y JUnit 5.12.1. Las pruebas usan exclusivamente `papeleria_campus_pruebas`, sin intervenir otras bases. Se restauran estructura y los 12 productos antes y después de cada prueba de integración. La aplicación de demostración usa otra base: `papeleria_campus`.

## Método y resultados

Se ejecutan 12 métodos JUnit: 3 de validación, 5 de DAO/JDBC real y 4 de interfaz con FXML real. Cada método recorre varios casos del enunciado. Las capturas son píxeles obtenidos de `Scene.snapshot` durante la ejecución; no son maquetas. El resultado esperado se compara mediante aserciones. La confirmación Aceptar/Cancelar se inyecta en los tests para recorrer ambas decisiones; la aplicación final muestra un Alert modal. Los resultados de INSERT, UPDATE y DELETE se comprueban con nuevas consultas y con **tres procesos JVM independientes**, además de volver a abrir el controlador JavaFX.

Comando de verificación (con JAVA_HOME 21 y variables de prueba configuradas como indica README): `mvnw.cmd -B clean verify`. La verificación y las ocho capturas se actualizaron con la interfaz básica de JavaFX, sin CSS personalizado.

Resultado final: **12 pruebas, 0 fallos, 0 errores, 0 omitidas**. Ver [log completo](evidencias/maven-verificacion.txt) y [reportes JUnit](evidencias/). Las advertencias SQLState 23505/23514/23502/08001 corresponden a errores provocados y gestionados deliberadamente. JavaFX avisa que los tests cargan clases desde el classpath; el lanzamiento normal usa el module-path y también fue ejecutado.

## Búsquedas, filtros y TableView

En cada caso el controlador invoca al DAO, que abre una conexión y consulta PostgreSQL. Se verifica lista y contador, no se filtra una lista fija.

| Caso | Entrada / acción | Resultado esperado | Resultado obtenido | Estado y evidencia |
|---|---|---|---|---|
| Inicial | Abrir con los 12 productos | 6 columnas; 12 filas; código ascendente | A001-A004, P001-P004, T001-T004; 12 registros | PASA; [captura](evidencias/01-listado-inicial.png), `b1AB7YSeleccion` |
| B1 | CUADERNO; Todas; casilla desmarcada | P001 y P002; 2 | Exactamente P001 y P002; 2 | PASA; DAO y test de interfaz |
| B2 | T00; sin otros filtros | T001-T004; 4 | Exactamente T001-T004; 4 | PASA; DAO y test de interfaz |
| B3 | Texto vacío; Papelería | P001-P004; 4 | Exactamente P001-P004; 4 | PASA; DAO y test de interfaz |
| B4 | Todas; solo existencias | 9; excluye P004, T004 y A003 | 9; todos con stock positivo | PASA; DAO y test de interfaz |
| B5 | USB + Tecnología + solo existencias | T001 y T003; 2 | Exactamente T001 y T003; 2 | PASA; [captura](evidencias/02-filtro-combinado.png) |
| B6 | ZZZ999; sin otros filtros | 0; aviso Sin coincidencias | Tabla vacía; contador 0; aviso correcto | PASA; `b1AB7YSeleccion` |
| B7 | Limpiar filtros después de filtrar | Vacío, Todas, casilla desmarcada, 12 | Controles restaurados y 12 filas | PASA; `b1AB7YSeleccion` |
| Recarga externa | INSERT de EXT desde conexión externa, consultar | 13 filas | 13; el DAO vuelve a consultar | PASA; `b1AB7ConsultaRealYRecargaExterna` |
| Selección | Seleccionar P001 | Los cinco campos y el ID; ID no editable | P001, Cuaderno universitario, Papelería, 65.00, 25; ID protegido | PASA; `b1AB7YSeleccion` |
| Limpiar formulario | Nuevo después de B5 | Conservar filtros y lista | USB y dos filas se conservan | PASA; `b1AB7YSeleccion` |

## Ciclo CRUD y persistencia

| Paso | Entrada / acción | Resultado esperado | Resultado obtenido | Estado y evidencia |
|---|---|---|---|---|
| Crear | X001, Producto de prueba, Accesorios, 10.00, 1 | 13; ID generado | 13; código normalizado; precio y stock correctos | PASA; [captura](evidencias/04-crud-creado.png) |
| Reiniciar tras crear | Nueva JVM y nuevo controlador | X001 persiste con 10.00 y 1 | Consulta independiente devuelve los valores y 13 filas | PASA; `cicloCrudPersistenciaYFiltros` y `PersistenciaProbe` |
| Actualizar | Seleccionar X001; precio 15.50, stock 3 | Solo su ID cambia; 13 | Mismo ID; precio 15.50, stock 3; 13 | PASA; [captura](evidencias/05-crud-actualizado.png) |
| Reiniciar tras actualizar | Nueva JVM y nuevo controlador | 15.50 y 3 persistentes | Mismos valores desde proceso independiente y formulario recargado | PASA; `PersistenciaProbe` |
| Cancelar eliminación | Seleccionar X001; cancelar confirmación | X001 permanece; 13 | Ninguna eliminación; 13 | PASA; [captura](evidencias/06-crud-cancelado.png) |
| Confirmar eliminación | Seleccionar X001; aceptar | X001 eliminado; 12 | Exclusivamente X001 eliminado; 12 | PASA; [captura](evidencias/07-crud-eliminado.png) |
| Reiniciar tras eliminar | Nueva JVM y nuevo controlador | 12 sin X001 | 12; X001 ausente | PASA; `PersistenciaProbe` |
| Filtros vigentes | Crear X001 con stock 1, filtrar existencias, actualizarlo a stock 0 | Desaparece del filtro; sigue en la base | Listado de 10 a 9; X001 existe con stock 0 | PASA; `cicloCrudConCancelacionReaperturaYFiltros` |

## Validaciones

Los subcasos rechazados se recorren en creación y actualización. En el test de interfaz se compara el catálogo completo antes/después de cada rechazo y se exige un mensaje nuevo y concreto.

| Caso | Entrada / acción | Resultado esperado | Resultado obtenido | Estado y evidencia |
|---|---|---|---|---|
| V1 | Código vacío, espacios, 11 caracteres | Rechazar; 1-10 tras trim | Todos rechazados; mensaje de longitud; BD sin cambios | PASA; validator e interfaz |
| V2 | Nombre espacios, AB, 81 caracteres | Rechazar; 3-80 tras trim | Todos rechazados; mensaje de nombre; BD sin cambios | PASA; validator e interfaz |
| V3 | Sin categoría; categoría Otra | Rechazar / impedir valor ajeno | Null rechazado; ComboBox no editable; validador rechaza Otra | PASA; validator e interfaz |
| V4 | Precio abc | Rechazar sin cerrar | Mensaje pide número con punto; aplicación sigue abierta | PASA; validator e interfaz |
| V5 | Precio 0 y -1 | Rechazar ambos | Ambos rechazados; rango indicado | PASA; validator e interfaz |
| V6 | 12.345; 100000000.00 | Rechazar decimales y rango, sin redondear | Ambos rechazados antes de JDBC | PASA; [captura](evidencias/03-validacion-precio.png) |
| V7 | Stock abc y 2.5 | Rechazar no enteros | Ambos rechazados; mensaje de entero | PASA; validator e interfaz |
| V8 | Stock -1; 2147483648; luego 0 | Rechazar primeros, admitir 0 | Primeros rechazados; 0 insertado; límite 2147483647 validado | PASA; validator e interfaz |
| V9 | Crear « p001 »; actualizar P002 a P001; guardar P001 con su propio código | Rechazar duplicados; permitir conservar código propio | SQLState 23505 en duplicados, mensaje seguro; edición propia correcta | PASA; DAO e interfaz |
| V10 | Actualizar y eliminar sin selección | Bloquear y pedir selección | Ambas acciones bloqueadas con mensaje; ninguna escritura | PASA; `v1AV10MensajesYBaseSinCambios` |

También se comprueban código de 10 caracteres, nombre de 80, precio 0.01 y 99999999.99, stock 2147483647, normalización de código/nombre, y rechazo de coma decimal/notación científica.

## Excepciones y restricciones independientes

| Caso | Entrada / acción | Resultado esperado | Resultado obtenido | Estado y evidencia |
|---|---|---|---|---|
| E1 | Conexión de prueba a 127.0.0.1:1 | Aviso comprensible, aplicación abierta, sin falso cero exitoso | SQLState 08001; ventana abierta; «Sin conexión · sin cargar» | PASA; [captura](evidencias/08-error-conexion.png) |
| E2 | DAO sin validación: código vacío/minúsculo/con espacios, nombre AB, categoría Otra, precio 0, stock -1, código null; además duplicados | SQLException, CHECK/NOT NULL/UNIQUE efectivos, catálogo intacto | 23514, 23502 y 23505; 12 productos tras cada intento | PASA; `e2RestriccionesDirectasDelDao`, `v9DuplicadosYEdicionPropia` |
| E3 | Seleccionar temporal, eliminar desde otra conexión, actualizar o eliminar en interfaz | Cero filas; advertir; recargar; sin éxito falso | Ambas acciones avisan «ya no existe» y muestran 12 | PASA; DAO e interfaz |
| E4 | Forzar E1/E2 y capturar logger | Operación + SQLState; usuario sin secretos/trazas | Registro «Operación=buscar; SQLState=08001» y estados de restricciones; mensaje seguro | PASA; `e1E3E4ConexionCeroFilasYDiagnosticoSeguro` y log |

## Incidencias detectadas y resueltas

Scene Builder integrado de IntelliJ mostraba el editor en blanco. El registro del IDE identificó `NoSuchFieldException: CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN` al cargar el FXML. Se trasladó esa configuración al método initialize del controlador y se añadió un tamaño de diseño al BorderPane. Se volvió a ejecutar `clean verify`: 12 pruebas aprobadas. La carga de la aplicación conserva la misma política de columnas y el FXML ya no hace referencia a la constante incompatible con el editor.

Primera ejecución de la validación: **FALLÓ** el subcaso V3 sin categoría por NullPointerException al consultar `List.of(...).contains(null)`. Se añadió una comprobación explícita de null antes de buscar la categoría. La segunda ejecución de las 8 pruebas de validador/DAO pasó y la ejecución integral de 12 también pasó. No se ocultó el fallo inicial.

## Evidencias entregadas y límites

Ocho capturas PNG, tres reportes TXT de JUnit, tres reportes XML de JUnit, el log de Maven y un SELECT final del conteo. Los PNG complementan los tests reproducibles y el código; no sustituyen la conexión real. La base de prueba termina restaurada con 12 registros, de los cuales 9 tienen existencias.

La cuenta GitHub dispone de un repositorio privado. Queda pendiente que Fanor indique el usuario del docente para habilitar su acceso; no se presume un acceso que no se haya concedido.
