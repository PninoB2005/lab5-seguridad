# Laboratorio 5 — Seguridad en APIs (HTTPS + mTLS)

API REST en Spring Boot (Java 17) que expone `GET /api/v1/salud` sobre **HTTPS**
con **HSTS** y **autenticación mutua (mTLS)**.

- Endpoint: `https://localhost:8443/api/v1/salud` → `{"estado":"ok"}`
- Puerto: `8443`
- Certificado del servidor: autofirmado para `CN=localhost` (SAN `localhost`, `127.0.0.1`)
- mTLS: el servidor exige al cliente presentar un certificado confiable (`client-auth=need`)

> **Regla de seguridad:** ningún `*.p12`, `*.crt`, `*.key`, `*.pem` ni `.env`
> se versiona en Git (ver `.gitignore`). Cada integrante genera sus propios
> certificados localmente con los comandos de abajo.

---

## 1. Requisitos

- JDK 17 o superior (incluye `keytool`)
- Maven (se incluye el wrapper `mvnw`)
- `curl`

---

## 2. Variables de entorno

Las contraseñas de los almacenes **no** están en el código: se leen de variables
de entorno. Usar contraseñas ficticias (ej. `claveprueba`), nunca personales.

### Windows (PowerShell)

```powershell
$env:KEYSTORE_PASSWORD   = "claveprueba"
$env:TRUSTSTORE_PASSWORD = "claveprueba"
```

### Linux / macOS (bash)

```bash
export KEYSTORE_PASSWORD=claveprueba
export TRUSTSTORE_PASSWORD=claveprueba
```

---

## 3. Generar el certificado del servidor (Ejercicio 3)

Desde la raíz del proyecto:

```bash
# Par de llaves del servidor (keystore)
keytool -genkeypair -alias seii -keyalg RSA -keysize 2048 -validity 30 \
  -storetype PKCS12 -keystore src/main/resources/keystore.p12 \
  -dname "CN=localhost" -ext "SAN=dns:localhost,ip:127.0.0.1" \
  -storepass "$KEYSTORE_PASSWORD"

# Exportar el certificado público del servidor
keytool -exportcert -alias seii -keystore src/main/resources/keystore.p12 \
  -rfc -file seii.crt -storepass "$KEYSTORE_PASSWORD"
```

---

## 4. Generar el certificado del cliente y el truststore (Ejercicio 4 — mTLS)

El servidor confía solo en clientes cuyo certificado esté en `truststore.p12`.

```bash
# 1) Par de llaves del cliente autorizado (servicio-pagos)
keytool -genkeypair -alias pagos -keyalg RSA -keysize 2048 -validity 30 \
  -storetype PKCS12 -keystore cliente-pagos.p12 \
  -dname "CN=servicio-pagos" -storepass "$TRUSTSTORE_PASSWORD"

# 2) Exportar el certificado público del cliente
keytool -exportcert -alias pagos -keystore cliente-pagos.p12 \
  -rfc -file cliente-pagos.crt -storepass "$TRUSTSTORE_PASSWORD"

# 3) Importar el certificado del cliente en el truststore del servidor
keytool -importcert -alias pagos -file cliente-pagos.crt -noprompt \
  -storetype PKCS12 -keystore src/main/resources/truststore.p12 \
  -storepass "$TRUSTSTORE_PASSWORD"
```

> En PowerShell, reemplazar `$VARIABLE` por `$env:VARIABLE` y las barras `\`
> de continuación de línea por el acento grave `` ` ``.

---

## 5. Ejecutar la aplicación

```bash
./mvnw spring-boot:run      # Linux/macOS
.\mvnw.cmd spring-boot:run  # Windows
```

---

## 6. Probar con curl

### HTTPS + mTLS (Ejercicio 4c)

```bash
# Sin certificado de cliente -> rechazo en el handshake (no envía Finished válido)
curl -i --cacert seii.crt https://localhost:8443/api/v1/salud

# Con certificado de cliente -> HTTP 200 {"estado":"ok"}
curl -i --cacert seii.crt \
  --cert-type P12 --cert cliente-pagos.p12:"$TRUSTSTORE_PASSWORD" \
  https://localhost:8443/api/v1/salud
```

### Solo HTTPS (sin mTLS, Ejercicio 3c)

Si se desactiva mTLS (comentar `server.ssl.client-auth=need`):

```bash
curl -i https://localhost:8443/api/v1/salud              # (60) SEC_E_UNTRUSTED_ROOT
curl -i --cacert seii.crt https://localhost:8443/api/v1/salud  # 200 + HSTS
curl -i http://localhost:8443/api/v1/salud               # 400 requires TLS
```

---

## 7. Cliente Java con truststore (Ejercicio 3d)

```bash
# Importar seii.crt en un truststore para el cliente
keytool -importcert -alias seii -file seii.crt -keystore confianza.p12 \
  -storetype PKCS12 -noprompt -storepass "$TRUSTSTORE_PASSWORD"

# Compilar y ejecutar pasando el truststore por propiedades de la JVM
cd cliente-java
javac ClienteSalud.java
java -Djavax.net.ssl.trustStore=../confianza.p12 \
     -Djavax.net.ssl.trustStoreType=PKCS12 \
     -Djavax.net.ssl.trustStorePassword="$TRUSTSTORE_PASSWORD" \
     ClienteSalud
```

---

## 8. Estructura

```
src/main/java/co/edu/unal/lab5seguridad/
  Lab5SeguridadApplication.java   # arranque Spring Boot
  controller/SaludController.java # GET /api/v1/salud (+ CN del cliente mTLS)
  filter/HstsFilter.java          # cabecera Strict-Transport-Security
cliente-java/ClienteSalud.java    # cliente HttpClient (Lab 4)
src/main/resources/application.properties
```
