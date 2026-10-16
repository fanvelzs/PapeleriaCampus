# Papelería Campus

Laboratorio de TableView, JDBC y CRUD de productos.

**Autor:** Fanor Velasquez. **Grupo:** 3. **Fecha de verificación:** 15 de octubre de 2026 (America/Managua).

Repositorio: https://github.com/fanvelzs/PapeleriaCampus (privado). Para dar acceso al docente, agregar su usuario en Settings > Collaborators. Falta que el autor indique ese usuario.

## Requisitos y versiones utilizadas

Windows, JDK Temurin 21.0.12, JavaFX 21.0.6, PostgreSQL 18, pgJDBC 42.7.3, Maven Wrapper 3.8.5 y JUnit 5.12.1. Maven descarga las dependencias del `pom.xml`; no hace falta instalar JavaFX manualmente. Se requiere conexión a internet durante la primera compilación. Java 8 no es compatible; compruebe `JAVA_HOME` antes de ejecutar.

## Crear y seleccionar la base de datos

Con PostgreSQL iniciado y `psql`/`createdb` en PATH, ejecutar desde la raíz del proyecto:

```powershell
createdb -h localhost -p 5432 -U postgres papeleria_campus
psql -h localhost -p 5432 -U postgres -d papeleria_campus -v ON_ERROR_STOP=1 -f sql/estructura.sql
psql -h localhost -p 5432 -U postgres -d papeleria_campus -v ON_ERROR_STOP=1 -f sql/datos.sql
```

En Windows también puede usar la ruta completa, por ejemplo `& 'C:\Program Files\PostgreSQL\18\bin\psql.exe' ...`. Con pgAdmin, crear `papeleria_campus`, abrir Query Tool **en esa base**, ejecutar primero `estructura.sql` y después `datos.sql`.

`estructura.sql` crea una tabla nueva y falla si ya existe; no borra datos. `datos.sql` restaura exactamente los 12 productos del enunciado mediante TRUNCATE y reinicia la secuencia; úselo exclusivamente sobre la base local del laboratorio. Para restaurar no necesita volver a ejecutar la estructura.

## Configuración externa

```powershell
Copy-Item database.properties.example database.properties
```

Editar `db.url`, `db.user` y `db.password` con los valores locales. El archivo usa el formato Java Properties (para valores no ASCII usar escapes `\uXXXX`). La aplicación lee la configuración al conectar. `database.properties`, `.env`, logs temporales y datos locales de PostgreSQL están excluidos de Git y del ZIP.

También se admiten `CAMPUS_DB_URL`, `CAMPUS_DB_USER` y `CAMPUS_DB_PASSWORD`; las variables de entorno tienen prioridad sobre el archivo. Con `-Dcampus.config=ruta` se puede elegir otro archivo al lanzar Java. Sin configuración se intenta `localhost:5432/papeleria_campus`, usuario `postgres`, sin contraseña.

En la computadora donde se preparó la entrega quedó una instancia **local y aislada** de PostgreSQL en `127.0.0.1:55432`, con las bases `papeleria_campus` y `papeleria_campus_pruebas`. La configuración ignorada apunta a la primera. Su directorio es `tmp/pgdata` y no se entrega. Puede reiniciarse con:

```powershell
& 'C:\Program Files\PostgreSQL\18\bin\pg_ctl.exe' -D "$PWD\tmp\pgdata" -l "$PWD\tmp\postgres.log" -o '-h 127.0.0.1 -p 55432' -w start
```

Esa instancia de desarrollo permite acceso local sin contraseña; para otra computadora configure su propia instalación de PostgreSQL mediante los pasos anteriores.

## Ejecución

```powershell
$env:JAVA_HOME='C:\Users\fanvelzs\.jdks\temurin-21.0.12.1' # Ajustar en otra computadora
.\mvnw.cmd clean javafx:run
```

En IntelliJ, abrir `pom.xml` como proyecto Maven, seleccionar SDK 21 y ejecutar la tarea Maven `javafx:run`. La clase `Launcher` también permite iniciar `HelloApplication` con las dependencias configuradas por el IDE.

Buscar consulta PostgreSQL con texto parcial sobre nombre O código, categoría y existencias combinados con AND. Pulse Buscar o Enter en el texto para aplicar los filtros. Recargar conserva los controles actuales. Limpiar filtros los restablece; Nuevo / Limpiar formulario solo limpia el formulario.

Para crear, pulse Nuevo, llene los cinco campos y Guardar. Para editar, seleccione una fila y pulse Actualizar. El ID es de solo lectura. Eliminar pide confirmación; Cancelar conserva la fila. Después de cada escritura se vuelve a consultar PostgreSQL respetando los filtros. Los mensajes se muestran en la franja inferior. Una conexión fallida conserva el listado anterior marcado como sin verificar; no se anuncia una búsqueda exitosa con cero filas.

## Organización y flujo

`hello-view.fxml` y `campus.css` definen la interfaz. `PapeleriaCampusController` recibe las acciones y valida mediante `ProductoValidator`; transforma los resultados del DAO en `ObservableList<Producto>` para el TableView. `Producto` es el modelo inmutable y usa BigDecimal. `ProductoDAO` contiene todo el SQL de aplicación y usa PreparedStatement, ResultSet y try-with-resources. `DatabaseConnection` obtiene la conexión mediante configuración externa.

Flujo: **interfaz > controlador > validador > DAO > JDBC > PostgreSQL > ResultSet > Producto > ObservableList > TableView**. Las consultas y escrituras se ejecutan en una Task para que la interfaz no se congele. Durante la operación se bloquean controles para evitar envíos dobles. UPDATE y DELETE usan exclusivamente `id_producto`; cero filas afectadas produce una advertencia. Las excepciones registran operación y SQLState sin contraseñas ni trazas del driver.

Los CHECK de PostgreSQL respaldan la normalización de código, longitud del nombre, catálogo de categorías, precio positivo y stock no negativo. La interfaz rechaza precios con más de dos decimales antes de JDBC, porque NUMERIC(10,2) puede redondearlos al recibirlos directamente por SQL.

## Pruebas

Pruebas de validación sin PostgreSQL:

```powershell
.\mvnw.cmd test
```

Las pruebas de integración se omiten si no se establece `CAMPUS_TEST_DB_URL`. Para ejecutar las 12 pruebas (requieren escritorio gráfico disponible):

```powershell
createdb -h localhost -p 5432 -U postgres papeleria_campus_pruebas
$env:CAMPUS_TEST_DB_URL='jdbc:postgresql://localhost:5432/papeleria_campus_pruebas'
$env:CAMPUS_TEST_DB_USER='postgres'
$env:CAMPUS_TEST_DB_PASSWORD='SU_CLAVE_LOCAL'
.\mvnw.cmd clean verify
```

La integración solo admite localhost/127.0.0.1 y una base llamada exactamente `papeleria_campus_pruebas`. Recrea la tabla producto y restaura los 12 registros entre pruebas. No apuntarla a una base con información útil. Las pruebas cargan el FXML real, ejecutan acciones del controlador y guardan capturas JavaFX en `evidencias/`; la confirmación se inyecta como Aceptar/Cancelar en el test y en la aplicación usa un Alert modal. También se crean JVM independientes para comprobar persistencia tras inserción, actualización y eliminación. Resultados detallados: `pruebas.md`, reportes JUnit y log de Maven en `evidencias/`.

## Entrega y Git

El ZIP `Velasquez_Fanor_LabTableView.zip` contiene fuentes, recursos, Maven Wrapper, SQL, documentación y evidencias. No incluye credenciales locales, `target`, configuración del IDE ni el clúster de desarrollo. Se incluye `historial.git.bundle` para conservar el historial de avances fuera de GitHub; puede recuperarse con `git clone historial.git.bundle PapeleriaCampus`.

## Fuentes y herramientas de apoyo

Requisitos, casos y datos iniciales: documento docente «Laboratorio TableView - CRUD.pdf». Implementación realizada con asistencia de OpenAI Codex; revisión mediante Maven, JUnit, PostgreSQL local y renderizado del FXML con JavaFX. No se reutilizó código de proyectos de terceros fuera de la plantilla JavaFX/Maven existente.

- [pgJDBC: consultas, parámetros y ResultSet](https://jdbc.postgresql.org/documentation/query/).
- [PostgreSQL 18: restricciones](https://www.postgresql.org/docs/18/ddl-constraints.html).
- [OpenJFX 21: TableView](https://openjfx.io/javadoc/21/javafx.controls/javafx/scene/control/TableView.html).
