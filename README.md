# Migración de Procesos Batch - Banco XYZ

Proyecto Spring Batch que moderniza tres procesos batch legacy del Banco XYZ:

1. **Reporte de Transacciones Diarias**
2. **Cálculo de Intereses Mensuales**
3. **Generación de Estados de Cuenta Anuales**

Los datos legacy provienen de archivos CSV con errores típicos: fechas mal formateadas, montos negativos o cero, campos vacíos, duplicados y tipos no válidos.

## Tecnología usada

- Spring Boot **3.3.4** (Spring Batch 5.1.2)
- Java **21**
- Maven **3.9.6+**
- Base de datos H2 embebida (modo memoria)

> Nota: la versión `4.1.0` de Spring Boot no existe. Se usó la última versión estable compatible con JDK 21: **3.3.4**.

## Requisitos

- Java 21 (probado con `jdk-21.0.11`)
- Maven 3.9.6 o superior
- Variables de entorno recomendadas en Windows:
  - `JAVA_HOME` apuntando al JDK 21
  - `MAVEN_HOME` / `M2_HOME` apuntando a Maven, o añadir `bin` al `Path`

## Estructura del proyecto

```
bank-legacy-batch/
├── src/main/java/com/banco/batch
│   ├── BankLegacyBatchApplication.java
│   ├── config/          # Configuración de Jobs (Transacciones, Intereses, Cuentas anuales)
│   ├── enums/           # Tipos de transacción y cuenta
│   ├── exception/       # Excepciones personalizadas
│   ├── launcher/        # Lanzador de Jobs por línea de comandos
│   ├── listener/        # Listeners de Job/Step y política de skip
│   ├── model/           # DTOs de entrada y entidades de salida
│   └── util/            # Utilidades de parseo legacy
├── src/main/resources
│   ├── application.properties
│   ├── schema.sql
│   └── data/            # CSVs legacy (semana_1, semana_2, semana_3)
└── pom.xml
```

## Configuración de base de datos

Por defecto se usa **H2 embebida en memoria**. Los datos se crean al iniciar la aplicación y se destruyen al cerrarla. Si deseas consultar las tablas después de la ejecución, cambia la URL en `application.properties` a modo archivo:

```properties
spring.datasource.url=jdbc:h2:file:./bankdb
```

La consola H2 está disponible durante la ejecución en:

```
http://localhost:8080/h2-console
JDBC URL: jdbc:h2:mem:bankdb
User: sa
Password: (vacío)
```

## Compilación

Desde la terminal de VS Code (`Ctrl + ñ`) o cualquier terminal, navega al proyecto:

```bash
cd "C:/Users/felip/Desktop/backend semana 1/Exp1_S1_grupo_9/bank-legacy-batch"
```

Compila con Maven global:

```bash
mvn clean package -DskipTests
```

> Si `mvn` no se reconoce, cierra y vuelve a abrir VS Code para que recargue las variables de entorno.

Si Maven no está en el `PATH`, usa la ruta absoluta:

```bash
C:\apache-maven-3.9.16\bin\mvn.cmd clean package -DskipTests
```

Alternativa con el wrapper incluido:

```bash
# Linux / macOS
./mvnw clean package -DskipTests

# Windows
mvnw.cmd clean package -DskipTests
```

El resultado es `target/bank-legacy-batch-0.0.1-SNAPSHOT.jar`.

## Ejecución de los Jobs

Cada Job recibe dos parámetros:

- `--job.name=<nombre del job>`
- `--semana=<semana_1|semana_2|semana_3>`

### Job 1: Reporte de Transacciones Diarias

```bash
java -jar target/bank-legacy-batch-0.0.1-SNAPSHOT.jar --job.name=reporteTransaccionesDiariasJob --semana=semana_1
```

### Job 2: Cálculo de Intereses Mensuales

```bash
java -jar target/bank-legacy-batch-0.0.1-SNAPSHOT.jar --job.name=calculoInteresesMensualesJob --semana=semana_1
```

### Job 3: Generación de Estados de Cuenta Anuales

```bash
java -jar target/bank-legacy-batch-0.0.1-SNAPSHOT.jar --job.name=generacionEstadosCuentaAnualesJob --semana=semana_1
```

## Transformaciones y validaciones aplicadas

- **Fechas**: se normalizan a `yyyy-MM-dd` aceptando `yyyy/MM/dd`, `dd-MM-yyyy` y `dd/MM/yyyy`.
- **Montos**: se rechazan valores negativos, cero o no numéricos; se marcan como anomalías.
- **Tipos**: se normalizan a mayúsculas; se detectan tipos inválidos.
- **Edades**: deben estar entre 18 y 100.
- **Duplicados**: en el Job de intereses se detectan registros exactos duplicados y se marcan como rechazados.
- **Descripciones vacías**: se marcan como anomalías en el Job de cuentas anuales.

## Manejo de errores

- Política de `SkipPolicy` personalizada (`BankSkipPolicy`) que permite saltar errores de parseo, validación y valores nulos sin detener el proceso.
- Listeners de Job y Step que imprimen conteos de lecturas, escrituras y registros saltados.
- Transacciones por chunk de 100 registros para optimizar rendimiento.

## Tablas generadas

- `transacciones_procesadas`
- `reporte_transacciones_diarias`
- `cuentas_intereses`
- `cuentas_anuales_procesadas`
- `estados_cuenta_anuales`
- Tablas de metadatos de Spring Batch (`BATCH_*`).
