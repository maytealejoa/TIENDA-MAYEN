# Ejecutar gestion con PostgreSQL

## Diagnóstico del 13 de octubre de 2026

Eclipse estaba ejecutando el proyecto de esta carpeta con los argumentos
`--spring.profiles.active=demo,consola`. Esa combinación activa H2, aunque el
driver PostgreSQL esté instalado. El servicio PostgreSQL 18 está activo y
escucha en `localhost:5432`. Se comprobó una conexión SQL correcta con el usuario
configurado en `application-postgres.properties`.

Hay dos bases diferentes: `tienda_mayenm` y `tienda_mayenn`. La URL JDBC del
perfil postgres debe coincidir exactamente con la base que se consulta en pgAdmin.
Al revisar, la configuración apuntaba a `tienda_mayenn`, que no tenía tablas,
mientras pgAdmin tenía conexiones a `tienda_mayenm`.

Se corrigió el perfil para usar `jdbc:postgresql://localhost:5432/tienda_mayenm`,
la base elegida por el usuario. Se agregó GestionPostgreSQL.launch para iniciar
con demo,postgres en Eclipse. La ejecución antigua no cambia de perfil por editar
este archivo: debe detenerse y ejecutarse la configuración nueva.

## Eclipse

1. Seleccionar gestion y pulsar F5 para actualizar los archivos.
2. Detener la ejecución anterior con Terminate en la consola.
3. Abrir Run → Run Configurations → Java Application → GestionPostgreSQL.
   También se puede hacer clic derecho en GestionPostgreSQL.launch → Run As.
4. Confirmar Program arguments: `--spring.profiles.active=demo,postgres`.
5. Confirmar Working directory: la carpeta gestion que contiene pom.xml.
6. Revisar src/main/resources/application-postgres.properties. La URL debe tener
   el nombre exacto de la base elegida. Las variables DB_URL, DB_USER y
   DB_PASSWORD de Environment, si existen, prevalecen sobre los valores del archivo.
7. Ejecutar y comprobar que la consola menciona demo y postgres, la conexión JDBC
   PostgreSQL y finalmente Started GestionApplication.

Se usan ambos perfiles porque demo configura la clave JWT y los usuarios locales;
postgres configura la conexión JDBC. No agregar consola, que habilita H2 Console.

## Comprobar en pgAdmin

Abrir la misma base indicada en JDBC → Schemas → public → Tables → Refresh.
Hibernate crea las tablas al iniciar correctamente, porque ddl-auto está en update.
No crea la base PostgreSQL en sí, que debe existir antes de arrancar.

```sql
SELECT current_database(), current_user;
SELECT tablename FROM pg_tables WHERE schemaname = 'public' ORDER BY tablename;
```

Una vez iniciada la aplicación deben aparecer productos, ventas, detalles_venta
y usuarios. Importar la colección de datos en Postman para cargar productos y ventas.
Los datos guardados en H2 no se migran automáticamente al cambiar de motor.
Postman utiliza la misma API y los mismos tokens; no conecta directamente a PostgreSQL.

H2 Console no se utiliza para esta ejecución. Administrar PostgreSQL con pgAdmin
o psql. Mantener el perfil demo únicamente para el laboratorio local.

## PowerShell

Desde esta carpeta:

```powershell
.\mvnw.cmd spring-boot:run '-Dspring-boot.run.profiles=demo,postgres'
```

Si hay un error, revisar la primera causa concreta: password authentication failed
(credenciales), database does not exist (nombre), connection refused (servicio o
puerto) o address already in use (otra ejecución Java). Tener el driver instalado
no activa el perfil postgres ni cambia por sí solo la URL JDBC.
