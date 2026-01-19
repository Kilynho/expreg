# expreg

Aplicación en Java que lee un fichero de entrada en UTF-8 (por defecto `entrada.txt`), separa párrafos por líneas en blanco y genera `salida.html` con una tabla comparando el texto original y el convertido. Las palabras/bloques en MAYÚSCULAS se muestran en negrita con reglas especiales:

- Siglas reservadas (`HTML`, `ONU`, `NASA`) se mantienen tal cual (sin negrita).
- Bloques enteros en mayúsculas se convierten a minúsculas pero se capitaliza la primera letra de cada bloque/sentencia y se resaltan en `<span class="negrita">`.
- Tras `.` `!` `?` `¡` `¿` se capitaliza la siguiente palabra.
- Se escapan caracteres HTML y `\n` se convierte en `<br>`.

## Estructura

- `src/Main.java`: implementación Java.
- `entrada.txt`: ejemplo de entrada.
- `salida.html`: salida generada (se sobrescribe al ejecutar).

## Requisitos

- Java 17+ (JRE) y `javac` (JDK) instalados.

En Debian/Ubuntu:

```bash
sudo apt-get update
sudo apt-get install -y default-jdk
```

## Compilar y ejecutar (VS Code Tasks)

Tareas disponibles en VS Code:

- build java: compila `src/Main.java` a la carpeta `out`.
- run java: ejecuta `Main` usando `entrada.txt` y genera `salida.html`.

Puedes lanzarlas desde la paleta de comandos (Run Task) o con:

```bash
# Compilar
javac -d out src/Main.java

# Ejecutar con el fichero de ejemplo
java -cp out Main entrada.txt
```

El resultado se escribe en `salida.html`.
