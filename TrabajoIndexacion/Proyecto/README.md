# LSDFMS para datos de H&M

El sistema importa archivos CSV a un almacén binario, busca por clave con un índice en disco, marca registros como eliminados y compacta los archivos. `INDEX()` y `PACK()` usan la misma lógica en modo serial y paralelo. Con un hilo la operación se ejecuta en el hilo principal. Con varios hilos, cada trabajador procesa archivos independientes.

## Compilación y ejecución

Desde `TrabajoIndexacion/Proyecto`, con JDK 17 o posterior:

```powershell
$fuentes = Get-ChildItem src/*.java,tests/*.java | Select-Object -ExpandProperty FullName
javac --release 17 -encoding UTF-8 -d out $fuentes
java -Xmx64m -cp out SystemTest
java -Xmx64m -cp out Main ayuda
```

Se compiló con `--release 17` y se ejecutaron las pruebas con Oracle JDK 25.0.1. Las clases solo dependen de la biblioteca estándar de Java.

## Caso real

El equipo puede consultar un artículo por su `article_id` original, consultar una fila por su clave interna y retirar físicamente los registros dados de baja. El almacén conserva todas las columnas, incluidas las claves originales que relacionan transacciones con clientes y artículos.

Coloca los tres archivos del conjunto [H&M Personalized Fashion Recommendations de Kaggle](https://www.kaggle.com/competitions/h-and-m-personalized-fashion-recommendations/data) en `TrabajoIndexacion/DatasetHyM`. La descarga y el uso se rigen por las [reglas de la competencia](https://www.kaggle.com/competitions/h-and-m-personalized-fashion-recommendations/rules).

| Archivo | Contenido | Clave interna |
|---|---|---|
| `articles.csv` | Artículos, productos, categorías, color y descripción | número de fila |
| `customers.csv` | Identificador de cliente, estado de membresía, preferencias, edad y código postal | 1 000 000 000 000 + número de fila |
| `transactions_train.csv` | Fecha, cliente, artículo, precio y canal de venta | 2 000 000 000 000 + número de fila |

El número de fila empieza en 1 después de la cabecera. La clave es determinista para el mismo orden de entrada y no cambia al compactar. La partición de una fila se calcula como `(fila - 1) % particiones`. Cada partición conserva sus claves en orden creciente. Todos los valores se guardan como texto UTF-8, incluidos los ceros iniciales, campos vacíos y decimales. El importador interpreta comillas escapadas y saltos de línea dentro de comillas. La copia local de `articles.csv` tiene cabecera ZIP y el importador lee directamente su miembro `articles.csv`.

```powershell
java -Xmx64m -cp out Main importar storage/demo ../DatasetHyM 4 100000
java -Xmx64m -cp out Main index storage/demo 4
java -Xmx64m -cp out Main buscar-articulo storage/demo 0108775015
java -Xmx64m -cp out Main buscar storage/demo 2000000000001
java -Xmx64m -cp out Main eliminar storage/demo 2000000000001
java -Xmx64m -cp out Main buscar storage/demo 2000000000001
java -Xmx64m -cp out Main pack storage/demo 4
java -Xmx64m -cp out Main verificar storage/demo
```

El último argumento de `importar` limita la cantidad de filas por tabla. Omítelo para importar las tres tablas completas y usa otra ruta de almacén. Cada comando abre los archivos de nuevo. Una búsqueda ausente informa el resultado y sale normalmente. Los errores de entrada o almacenamiento devuelven código 1. Para usar el modo serial, indica `1` en `index` o `pack`.

`buscar-articulo` recibe los diez dígitos del ID de catálogo, incluidos los ceros iniciales. La salida incluye la clave interna que acepta `eliminar`. En almacenes creados con la versión anterior, vuelve a ejecutar `index` para habilitar esta consulta con los archivos de datos existentes.

## Archivos y operaciones

Los números se escriben en big-endian. Los offsets son posiciones absolutas de bytes y se almacenan como `long`.

| Archivo | Formato |
|---|---|
| `data-V/part-I.dat` | Mágico `LSDF`, versión `int`, tabla con `writeUTF`, cantidad de columnas `short` y nombres con `writeUTF`. Cada registro tiene longitud de cuerpo `int`, estado `byte` (0 activo, 1 eliminado), clave `long` y cantidad de campos `short`. Cada campo usa longitud `int` y bytes UTF-8. |
| `index-UUID.idx` | Mágico `LSIX`, versión `int`, versión de datos `long` y cantidad de entradas `long`. Cada entrada ocupa 20 bytes y guarda clave `long`, archivo `int` y offset `long`. |
| `index-UUID.articles.idx` | Mágico `LSAI` y la misma estructura de entradas fijas del índice primario. La clave es el valor numérico de `article_id`, reversible por su ancho fijo de diez dígitos. |
| `active.meta` | Mágico `LSMF`, formato de manifiesto `int`, versión activa `long`, cantidad de archivos `int`, total y eliminados como `long`, y nombre del índice con `writeUTF`. El formato 2 añade un booleano que indica si existe el índice secundario con el mismo UUID. Se leen manifiestos de formato 1. Una cadena vacía indica que todavía no hay índice. |
| `delete.meta` | Diario temporal con mágico `LSDJ`, versión de datos, conteo previo, archivo, offset y clave. Permite terminar el borrado al reabrir. |

- `INDEX()` recorre los registros activos, ordena corridas de hasta 65.536 pares clave/offset y las fusiona en grupos de hasta 16. Rechaza claves duplicadas y cruza las entradas con los registros físicos antes de publicar. La consulta hace búsqueda binaria con `RandomAccessFile.seek()`, comprueba la cabecera y valida la clave y el estado en el offset obtenido.
- En un almacén H&M, el índice secundario reutiliza el ordenamiento externo sobre las particiones de `articles`. Valida unicidad, offsets, identificadores y cobertura de los artículos activos. La búsqueda binaria accede al artículo en su offset físico y comprueba su estado.
- `eliminar` escribe el diario, marca un byte en el registro con `seek()`, sincroniza el archivo y publica el nuevo conteo. Una entrada previa para la clave queda invisible de inmediato y desaparece al reconstruir el índice.
- `PACK()` copia los cuerpos activos a archivos nuevos, verifica los conteos y reconstruye los índices con offsets nuevos. El manifiesto cambia después de validar la nueva versión. Si ocurre un fallo, se espera a los trabajadores y se retiran los archivos que el manifiesto no referencia. Tras corregir la causa, se puede reintentar en la misma instancia. Una vez publicada la nueva versión, se retira la anterior. El tamaño informado corresponde a bytes de los archivos, incluido el efecto de los encabezados.
- El bloqueo del almacén cubre toda la operación, incluidas las consultas, y da acceso a un proceso por vez. Un trabajador fallido impide la publicación. Los archivos nuevos se sincronizan con `FileDescriptor.sync()`. Si el sistema de archivos no admite reemplazo atómico del manifiesto, la publicación falla y se conserva la versión activa. En la siguiente apertura se recupera el diario de borrado y se limpian temporales y versiones sin publicar. La durabilidad ante un corte eléctrico depende del sistema de archivos y de su manejo de los cambios de directorio.

El trabajo en memoria está acotado por el número de trabajadores y los límites de formato. Hay hasta 8 hilos, 64 archivos, 256 campos por registro y 1 MiB por cuerpo de registro por defecto. Puede ajustarse ese último límite con `-Dlsdfms.maxRecord=<bytes>` y debe mantenerse compatible al reabrir el almacén. Se usan buffers de E/S de 64 KiB. Cada trabajador de indexación usa 2 MiB para ordenar su corrida. La verificación global mantiene un lector por archivo. El tamaño total de las tablas determina las corridas en disco y sus pasadas de fusión.

## Pruebas y benchmark

```powershell
java -Xmx64m -cp out SystemTest
java -Xmx64m -cp out SystemTest --offset-grande
java -Xmx64m -cp out Main benchmark results/mi-medicion.csv 1000000 8 30 3 2 4 8
```

Las pruebas comparan byte a byte las salidas serial y paralela, ejercitan la consulta secundaria y verifican el reintento en la misma instancia tras un fallo de trabajador. También simulan una interrupción de hilo y terminan procesos independientes después de escribir los datos, antes del reemplazo atómico del manifiesto y después del reemplazo. La opción de offset grande requiere espacio para un archivo de algo más de 2 GiB y comprueba `seek()` más allá de ese umbral.

El benchmark usa datos sintéticos. Cada muestra parte de una copia del mismo almacén y mide la operación completa, incluida la validación y publicación. La generación y copia quedan fuera del tiempo. Hay una ronda de calentamiento y el orden de los modos rota entre repeticiones. El CSV registra cada muestra, la mediana, mínimo, máximo, speedup, eficiencia, rendimiento, tamaños de datos e índice, tamaño de la última corrida temporal y espacio recuperado. El pico de heap es la suma de picos de los pools de heap de la JVM y puede sobreestimar el pico simultáneo. La caché del sistema operativo queda activa. El archivo de salida debe tener un nombre nuevo.

Consulta [las mediciones y su entorno](results/README.md).
