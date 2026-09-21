## Precondiciones

Para levantar el proyecto se necesita:

- Docker 24 o superior.
- Docker Compose v2 o superior.
- Java 21 o superior si se quiere ejecutar la aplicación fuera de Docker.
- Maven 3.9 o superior. El proyecto incluye Maven Wrapper (`mvnw` / `mvnw.cmd`), por lo que no es obligatorio tener Maven instalado.

Para probar la API usar la siguiente API key:

`sagat`

Debe enviarse en el header:

`X-API-Key: sagat`

La configuración local de PostgreSQL utilizada es:

- database: `sagat`
- username: `sagat`
- password: `sagat`

Estos valores se pueden modificar mediante variables de entorno:

- `NOTIFICATION_API_KEY`
- `SPRING_DATASOURCE_USERNAME`
- `SPRING_DATASOURCE_PASSWORD`

## Diseño

El proyecto está organizado utilizando una arquitectura por capas. La idea principal es mantener separadas las responsabilidades de la API, la lógica de negocio y el acceso a datos.

La estructura principal es la siguiente:

- `controller`: recibe las requests HTTP, valida la entrada y devuelve las respuestas.
- `services`: contiene la lógica de negocio y el procesamiento de las notificaciones.
- `repository`: acceso a PostgreSQL mediante Spring Data JPA.
- `entity`: entidades que se persisten en la base de datos.
- `payloads`: objetos utilizados para los requests y responses de la API.
- `enums`: estados, canales y prioridades disponibles.
- `config`: configuración de autenticación y manejo global de errores.

Elegí esta estructura porque para el tamaño actual del proyecto es simple de entender y permite separar bastante bien cada responsabilidad.

No es una arquitectura pensada necesariamente para un sistema muy grande. Si el proyecto creciera, probablemente tendría sentido separar más la lógica de negocio de la infraestructura, por ejemplo utilizando una arquitectura hexagonal o basada en eventos.

### Procesamiento asíncrono

El envío de las notificaciones no se realiza directamente durante el `POST`.

El flujo actual es el siguiente:

1. El cliente hace `POST /api/notifications`.
2. La notificación se guarda en la base de datos con estado `PENDING`.
3. La API responde `202 Accepted`.
4. Cada 60 segundos se ejecuta `NotificationService.processQueue()` utilizando `@Scheduled`.
5. El proceso busca las notificaciones pendientes y las intenta despachar.
6. Dependiendo del resultado, el estado cambia a `SENT` o `FAILED`.

Actualmente la cola está implementada utilizando PostgreSQL como almacenamiento y haciendo polling sobre las notificaciones pendientes.

Guardar primero la notificación en PostgreSQL permite que una notificación pendiente no se pierda simplemente porque la aplicación se reinicie.

La desventaja es que el polling introduce una demora de hasta 60 segundos no es la mejor opción si existen varias instancias de la aplicación procesando la misma cola.

En un escenario con mayor volumen o varias instancias, probablemente reemplazaría este mecanismo por un broker de mensajes.

### Jakarta EE y WildFly

No tengo experiencia práctica trabajando con WildFly, por lo que esta parte está basada en la documentación y en cómo debería adaptarse la solución.

Cambios:

- Reemplazar Spring Boot por componentes Jakarta EE como JAX-RS, CDI, JPA y JTA.
- Configurar el datasource y las credenciales desde WildFly en lugar de hacerlo desde la aplicación.
- Reemplazar el polling con `@Scheduled` por JMS.
- Utilizar un consumidor administrado por el application server para procesar los mensajes.
- Separar las transacciones de base de datos de las llamadas HTTP hacia servicios externos.
- Implementar reintentos persistentes e idempotencia.
- Utilizar la configuración de seguridad y logging proporcionada por WildFly.
- Separar la lógica de negocio de los adaptadores de envío para evitar que quede acoplada a HTTP, JMS u otra tecnología concreta.

El cambio más importante sería reemplazar el mecanismo actual de polling por JMS y utilizar un Message-Driven Bean (MDB) como consumidor.

## Cómo levantar el proyecto

Desde la raíz del proyecto:

`docker compose up --build -d`

La API queda disponible en:

`http://localhost:8080`

La API key utilizada por defecto es:

`sagat`

y debe enviarse utilizando:

`X-API-Key: sagat`

## Uso

El campo `channel` puede ser, por ejemplo, `SERVICE` o `LOG`.

Cuando se utiliza `SERVICE`, el campo `recipient` debe apuntar al endpoint de prueba incluido en el proyecto:

`http://localhost:8080/api/service-notifications`

Cuando el canal es `LOG`, la notificación no se envía al servicio HTTP y se procesa mediante el mecanismo correspondiente a ese canal.

### Crear una notificación

Linux / macOS:

`curl -X POST "http://localhost:8080/api/notifications" \`
`  -H "Content-Type: application/json" \`
`  -H "X-API-Key: sagat" \`
`  -d '{`
`    "recipient": "http://localhost:8080/api/service-notifications",`
`    "channel": "SERVICE",`
`    "subject": "Pedido creado",`
`    "body": "El pedido fue creado",`
`    "priority": "HIGH",`
`    "metadata": {`
`      "orderId": "123"`
`    }`
`  }'`

PowerShell:

$body = @{
    recipient = "http://localhost:8080/api/service-notifications"
    channel   = "SERVICE"
    subject   = "Pedido creado"
    body      = "El pedido fue creado"
    priority  = "HIGH"
    metadata  = @{
        orderId = "123"
    }
} | ConvertTo-Json -Depth 10

Invoke-RestMethod `
    -Uri "http://localhost:8080/api/notifications" `
    -Method POST `
    -ContentType "application/json" `
    -Headers @{
        "X-API-Key" = "sagat"
    } `
    -Body $body

Si la request es válida, la API devuelve `202 Accepted` y la notificación queda almacenada inicialmente con estado `PENDING`.

### Consultar una notificación

La notificación se puede consultar mediante su `id`:

Linux / macOS:

`curl -X GET "http://localhost:8080/api/notifications/{id}" \`
`  -H "X-API-Key: sagat"`

PowerShell:

curl.exe -X GET "http://localhost:8080/api/notifications/bc1db40c-1dbb-4537-b504-909a06f77002" -H "X-API-Key: sagat"

Dependiendo de si el worker ya procesó la notificación, el estado puede ser `PENDING`, `SENT` o `FAILED`.
