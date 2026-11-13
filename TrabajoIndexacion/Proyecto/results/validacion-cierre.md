# Validación de cierre técnico

## Compilación y suite completa

Ejecución en Windows 11 con Oracle JDK 25.0.1. La compilación apuntó a Java 17. Comandos desde `TrabajoIndexacion/Proyecto`:

```powershell
$fuentes = Get-ChildItem src/*.java,tests/*.java | Select-Object -ExpandProperty FullName
javac --release 17 -encoding UTF-8 -d out $fuentes
java -Xmx64m -cp out SystemTest --offset-grande
```

La compilación y la suite terminaron con código 0. Extracto de la salida:

```text
javac 25.0.1
javac --release 17 exit=0
Recuperación verificada: DATA_WRITTEN
Recuperación verificada: BEFORE_PUBLISH
Recuperación verificada: AFTER_PUBLISH
Estrés verificado: 64 archivos, 192 registros, 1048544 bytes por cuerpo, heap máximo 67108864 bytes
Offset verificado: 2147487744 bytes
Pruebas correctas
SystemTest --offset-grande exit=0
```

### H05 · Memoria con 64 archivos

La nueva prueba reprodujo `OutOfMemoryError: Java heap space` en `Store.verifyRows()` durante la inicialización, antes del ajuste. La verificación ahora conserva los cursores y los buffers de E/S de cada lector y comparte el buffer del registro que está procesando. Con el mismo `-Xmx64m` pasó la inicialización, INDEX con 8 hilos, la verificación, PACK con 8 hilos y la verificación final. Quedaron 128 registros activos de los 192 iniciales.

### H06 · Flujo completo con H&M

```powershell
New-Item -ItemType Directory -Force storage | Out-Null
java -Xmx64m -cp out HmFlowTest ../DatasetHyM storage/hm-cierre
```

La prueba requiere un almacén nuevo. Importa los CSV completos y usa cuatro particiones por tabla y cuatro hilos para INDEX y PACK. El artículo eliminado (`0108775015`) y el conservado (`0110065002`) comparten una partición. Al reabrir, la prueba comprueba que la clave borrada sigue ausente y que ambos índices recuperan todos los campos originales del artículo conservado desde su nueva posición.

SHA-256 de los archivos usados:

| Archivo | SHA-256 |
|---|---|
| `articles.csv` | `25f21882053517f7684ea43f524225a9481a7bb9aab9cf0abfc7fe8c4576a72b` |
| `customers.csv` | `abf3450c37ee1c16124e0d3379371153f5371148742efdcfe44c97c0445df6b2` |
| `transactions_train.csv` | `fff5e3b35c1b26016aa0bf2231be9525bc766eed32c94f1653420b465aef84fa` |

Salida de la ejecución completa:

```text
articles filas=105542
customers filas=1371980
transactions_train filas=31788324
importar version=1 archivos=12 total=33265846 activos=33265846 eliminados=0 datosBytes=4743623066 indiceBytes=0
index version=1 archivos=12 total=33265846 activos=33265846 eliminados=0 datosBytes=4743623066 indiceBytes=667427808
eliminar version=1 archivos=12 total=33265846 activos=33265845 eliminados=1 datosBytes=4743623066 indiceBytes=667427808
pack version=2 archivos=12 total=33265845 activos=33265845 eliminados=0 datosBytes=4743622749 indiceBytes=667427768
verificar version=2 archivos=12 total=33265845 activos=33265845 eliminados=0 datosBytes=4743622749 indiceBytes=667427768
H&M verificado: ID eliminado=0108775015, ID conservado=0110065002, recuperados=317 bytes, heap máximo=67108864 bytes
HmFlowTest exit=0
```
