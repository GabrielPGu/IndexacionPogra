# Lineamientos técnicos y estándares de programación — LSDFMS

**Proyecto:** Indexación y Compactación de Datos a Gran Escala  
**Asignatura:** Programación Concurrente y Paralela  
**Documento:** especificación técnica de referencia para diseño, implementación, pruebas y revisión de código  
**Estado:** lineamientos acordados para el proyecto (no representa una rúbrica oficial del docente)

## 1. Objetivo y alcance

Diseñar e implementar en **Java**, usando **archivos físicos de acceso directo**, un *Large Scale Data File Management System* (**LSDFMS**) que admita registros y campos de longitud variable, gestione metadatos e implemente las operaciones:

- **`INDEX()`**: construir y mantener índices persistentes que permitan buscar registros sin recorrer todo el conjunto de datos.
- **`PACK()`**: eliminar físicamente los registros marcados como eliminados, recuperar espacio en disco y mantener índices y metadatos consistentes.

Ambas operaciones tendrán **versión serial y versión paralela**, se probarán con datos persistentes y se compararán mediante métricas de rendimiento y pruebas de correctitud. El sistema incluirá un **caso de aplicación real**.

### 1.1. Origen y alcance de las restricciones

| Etiqueta | Significado |
|---|---|
| **[PDF]** | Requisito o descripción del enunciado actual de indexación y compactación. |
| **[ZIP]** | Enfoque o técnica presente en los materiales de clase y ejemplos proporcionados. |
| **[ACUERDO]** | Decisión técnica adoptada en nuestra planificación, aunque no esté escrita literalmente en el PDF actual. |
| **[PROPUESTA]** | Convención de implementación sugerida; puede ajustarse sin alterar los objetivos. |

**[PDF]** El enunciado actual solicita implementar computación paralela para optimizar `INDEX()` y `PACK()`, trabajar con archivos de acceso directo, estructuras de índices, registros/campos variables y metadatos, y asociar la solución a un proyecto de la asignatura.

**[ZIP]** Las prácticas incluyen ejemplos de `RandomAccessFile`, hilos Java y sincronización. El documento denominado *TRABAJO DE APLICACION 1* contiene restricciones de **otro ejercicio**: Java serial/paralelo, procesamiento en disco, no bibliotecas, no `ArrayList` y no IA. **No debe presentarse automáticamente como la rúbrica del trabajo actual.**

**[ACUERDO]** Para mantener coherencia con el enfoque del profesor, este proyecto adoptará el procesamiento basado en disco, Java y su biblioteca estándar, estructuras de memoria acotadas y hilos explícitos. Cualquier restricción académica adicional, incluida la aplicabilidad de una prohibición de herramientas de IA, debe confirmarse con el docente.

## 2. Principios técnicos no negociables

1. **Disco como fuente de verdad.** Registros, índices, metadatos, particiones y resultados intermedios estarán en archivos persistentes; no se almacenará el dataset completo en RAM. **[ACUERDO]**
2. **Memoria RAM acotada.** Solo se permitirá memoria para variables, el registro o bloque en procesamiento, buffers pequeños y estado de los hilos. El uso de RAM **no** debe crecer proporcionalmente al número de registros. **[ACUERDO]**
3. **Acceso directo real.** Utilizar `java.io.RandomAccessFile` y `seek(long)` para acceder a posiciones físicas. **[ACUERDO]**
4. **Solo biblioteca estándar de Java.** No depender de frameworks, motores de bases de datos, bibliotecas de indexación, colecciones persistentes de terceros ni ejecutables externos para realizar `INDEX()` o `PACK()`. **[ACUERDO]**
5. **Concurrencia explícita.** Implementar versiones paralelas con `Thread`, `Runnable`, `start()`, `join()` y mecanismos de sincronización de Java cuando hagan falta. **[ACUERDO]**
6. **Algoritmos implementados por el equipo.** La lógica central de lectura, indexación externa, ordenamiento/fusión y compactación debe estar en código propio. **[ACUERDO]**
7. **Sin carga masiva en memoria.** Evitar `readAllBytes()`, `Files.readAllLines()`, convertir todos los registros en objetos o construir índices completos en un `Map`, `ArrayList` u otra colección en RAM. **[ACUERDO]**
8. **Correctitud antes que velocidad.** El resultado serial será referencia verificable para contrastar el paralelo. **[ACUERDO]**
9. **Integridad ante interrupciones.** No sobrescribir destructivamente datos activos durante `PACK()`; emplear archivos temporales, validación, versionado y publicación controlada. **[ACUERDO]**
10. **Rendimiento demostrable.** Medir tiempos reales y consumo de recursos; no asumir que más hilos equivale a mayor velocidad. **[PDF + ACUERDO]**

> **Aclaración importante:** «Todo en disco» significa **persistencia y procesamiento externo**, no «cero bytes de RAM». El proceso Java, los buffers, los hilos y la caché del sistema operativo usan memoria inevitablemente. Nuestro requisito es evitar que el consumo de memoria de la aplicación crezca en proporción al dataset.

## 3. Plataforma y tecnologías autorizadas

| Categoría | Estándar adoptado |
|---|---|
| Lenguaje | Java. |
| Versión | **[PROPUESTA]** JDK 17 LTS, salvo versión distinta indicada por el docente; registrar la versión efectiva. |
| Compilación | `javac` y ejecución con `java`, sin necesidad de gestor de dependencias. |
| Lectura y escritura aleatoria | `RandomAccessFile`, `seek()`, `getFilePointer()`, `length()`, `setLength()` según corresponda. |
| Acceso secuencial | `RandomAccessFile` o streams estándar de Java, con buffers acotados. |
| Procesamiento paralelo | `Thread` y `Runnable`; sincronización explícita mediante `synchronized`, `wait()/notifyAll()` u otras primitivas estándar justificadas. |
| Memoria de trabajo | Arreglos primitivos (`byte[]`, `long[]`, etc.) de capacidad máxima conocida, más objetos de control pequeños. |
| Persistencia | `.dat`, `.idx`, `.meta`/manifiestos y archivos temporales en disco. |
| Dataset de entrada | Preferentemente texto/CSV con lectura por flujo. Si el dataset llega en otro formato, documentar la conversión. |
| Dependencias externas | Ninguna para ejecutar el núcleo del proyecto. |

**No se utilizarán:** base de datos SQL/NoSQL como sustituto del LSDFMS, Apache Lucene como motor de índices, frameworks de procesamiento masivo, `Files.readAllLines()`, colecciones de todo el dataset ni procesamientos externos que hagan el trabajo evaluado por `INDEX()`/`PACK()`.

Se admite usar APIs estándar para gestión de archivos, medición de tiempo, validación y pruebas. **Evitar `ArrayList` es una decisión de compatibilidad con la línea didáctica del ZIP; no significa que Java lo prohíba ni que el PDF actual lo mencione.**

## 4. Arquitectura lógica

```text
Entrada real (CSV) / generador sintético de carga
                         |
                         v
                  Importador en disco
                         |
                         v
  +------------------------------------------------+
  |                    LSDFMS                      |
  |                                                |
  |  Datos .dat <----> Metadatos / manifiesto       |
  |       ^                 ^                      |
  |       |                 |                      |
  |       v                 v                      |
  |  Índices .idx     Estado / versiones            |
  +------------------------------------------------+
              ^                     ^
              |                     |
        INDEX() serial        PACK() serial
        INDEX() paralelo      PACK() paralelo
              \                     /
               \                   /
                 Pruebas y benchmark
```

### 4.1. Responsabilidades mínimas

- **Almacenamiento:** crear, abrir, leer y escribir archivos de datos mediante offsets físicos.
- **Formato de registro:** codificar y decodificar registros variables, incluidos identificador, longitud y marca de eliminación.
- **Metadatos:** registrar archivos activos, versión de formato, cantidad de registros y ubicación de índices.
- **Índices:** construir, ordenar, fusionar, buscar y validar índices persistentes.
- **Compactación:** copiar solo registros activos, recalcular offsets y publicar versiones consistentes.
- **Concurrencia:** repartir trabajos independientes entre hilos y coordinar su finalización.
- **Medición:** comparar tiempos de las mismas operaciones bajo condiciones reproducibles.
- **Pruebas:** verificar registros, índices, recuperación y equivalencia serial/paralela.

### 4.2. Estructura propuesta del repositorio

```text
lsdfms/
├── README.md
├── LINEAMIENTOS_TECNICOS_LSDFMS.md
├── src/
│   ├── Main.java
│   ├── RecordCodec.java
│   ├── DataFileManager.java
│   ├── MetadataManager.java
│   ├── IndexManager.java
│   ├── PackManager.java
│   ├── SerialProcessor.java
│   ├── ParallelProcessor.java
│   ├── DiskSorter.java
│   ├── DatasetImporter.java
│   ├── DatasetGenerator.java
│   └── Benchmark.java
├── tests/
│   └── (pruebas ejecutables sin dependencias externas)
├── datasets/
│   └── README.md
├── storage/
│   ├── data/
│   ├── indexes/
│   ├── metadata/
│   └── tmp/
├── results/
│   └── (CSV de resultados reproducibles)
└── docs/
    └── (diseño, pruebas, caso de aplicación)
```

**[PROPUESTA]** Los nombres de clases y directorios son orientativos. La separación de responsabilidades y la persistencia en disco sí son principios del proyecto.

## 5. Formatos físicos de almacenamiento

### 5.1. Archivo de datos `.dat`

**[PROPUESTA]** Usar un formato binario documentado y versionado. Cada archivo tendrá una cabecera con identificador mágico, versión de formato y datos de control; a continuación, registros de longitud variable.

Formato conceptual de un registro:

```text
[longitud_total: int]
[estado: byte]                  0 = ACTIVO; 1 = ELIMINADO
[clave_primaria: long]
[cantidad_campos: short]
  repetir cantidad_campos veces:
    [id_campo: short]
    [tipo: byte]
    [longitud_valor: int]
    [valor: bytes]
```

**Reglas:**

- Las posiciones se expresan en **bytes** mediante `long`, nunca `int` para offsets de archivo.
- Los enteros se codificarán con un orden de bytes definido y documentado; `RandomAccessFile` escribe enteros multibyte en *big-endian*.
- Las cadenas se almacenarán como bytes **UTF-8** con longitud en bytes explícita.
- La longitud total permitirá saltar directamente al siguiente registro y validar límites.
- Los campos podrán variar entre registros; no se presupondrá un registro de tamaño fijo.
- Establecer un tamaño máximo de registro configurable para conservar memoria acotada; rechazar registros corruptos o excesivos con un error descriptivo.
- No permitir longitudes negativas, desbordamientos de offsets ni lecturas más allá del final del archivo.
- Un registro marcado `ELIMINADO` permanece físicamente hasta `PACK()`; no debe ser visible como un registro activo.

### 5.2. Archivo de índices `.idx`

**[PROPUESTA]** Para una clave primaria numérica de 64 bits, las entradas del índice serán de tamaño fijo:

```text
[clave: long][id_archivo: int][offset_bytes: long]
```

El índice incluirá una **cabecera** con versión, identificador de la versión de datos y cantidad de entradas; las entradas se ordenarán por clave y, si procede, por archivo/offset. Para otras clases de clave (p. ej. cadenas) se definirá una codificación comparable de tamaño fijo, una clave numérica normalizada o una estructura específica documentada.

- El índice completo **nunca se cargará en memoria**.
- La búsqueda por clave en un índice ordenado de entradas fijas podrá realizarse mediante **búsqueda binaria en disco**, calculando la posición de cada entrada y ejecutando `seek()`.
- La búsqueda binaria requiere índice correctamente ordenado, entradas de longitud fija y una política explícita de claves duplicadas.
- Si una clave aparece varias veces, documentar si se rechaza, se devuelve un rango de resultados o se usa una clave compuesta; para la clave primaria se recomienda unicidad.
- La aplicación debe comprobar la marca de eliminación antes de devolver un registro; puede actualizar/reconstruir el índice al eliminar según la estrategia implementada.
- No afirmar que todo acceso físico es `O(log N)` en tiempo real: la **búsqueda de una entrada** tiene `O(log N)` comparaciones/accesos aleatorios, mientras que la lectura del registro, la E/S y las búsquedas por duplicados añaden costo.

### 5.3. Metadatos y manifiesto

Guardar en disco, como mínimo:

- Versión de formato y versión activa del conjunto de datos.
- Lista e identificadores de archivos de datos.
- Ubicaciones de índices asociados.
- Cantidades de registros totales, activos y eliminados (cuando se conozcan).
- Identificador de una operación en preparación y estado de publicación/recuperación.
- Fecha o secuencia de generación, tamaño y, de ser posible, suma de verificación de archivos terminados.

**Regla de consistencia:** un índice siempre debe declarar a **qué versión de archivos de datos corresponde**; no se publicará un índice construido contra offsets obsoletos.

## 6. `INDEX()` — Indexación externa serial

La construcción debe realizarse en disco, sin guardar todas las entradas en RAM.

1. Validar metadatos y abrir los archivos `.dat` de lectura.
2. Recorrer registros de forma secuencial, validando longitudes y estados.
3. Escribir en disco pares `(clave, id_archivo, offset)` únicamente de registros activos.
4. Ordenar las entradas con un **algoritmo externo**. Se recomienda *external merge sort*:
   - formar corridas ordenadas de tamaño acotado;
   - escribir las corridas en archivos temporales;
   - fusionarlas en varias pasadas usando un número limitado de buffers.
5. Escribir un nuevo `.idx` ordenado, con cabecera y versión correspondiente.
6. Verificar orden y referencias; publicar el nuevo índice solo cuando sea válido.
7. Limpiar temporales **solo después** de que la operación concluya y el resultado esté publicado, o según el protocolo de recuperación.

**Prohibido:** leer el índice completo en un arreglo para ejecutar un ordenamiento interno de tamaño proporcional a `N`.

**Complejidad:** depende del volumen, la memoria de trabajo asignada y el número de pasadas de E/S del ordenamiento externo; **no** equivale a `O(log N)` (esa es la complejidad objetivo de la consulta binaria sobre un índice ya ordenado).

## 7. `PACK()` — Compactación externa serial

1. Identificar la versión activa de datos y los archivos afectados.
2. Crear archivos de salida **temporales y nuevos**, separados de los originales.
3. Leer secuencialmente cada registro, como máximo uno o un bloque acotado por vez.
4. Descartar los registros con estado `ELIMINADO`.
5. Escribir los registros `ACTIVO` en los nuevos archivos, registrando sus nuevos offsets.
6. Construir/reconstruir en disco los índices correspondientes a las **nuevas** posiciones.
7. Verificar conteos, integridad, posibilidad de recuperar registros y consistencia entre datos, índices y metadatos.
8. Confirmar y publicar la nueva versión mediante un protocolo seguro.
9. Conservar o retirar los archivos anteriores **solo cuando ya no se necesiten para recuperar una interrupción**.

**Invariantes obligatorios:**

- Ningún registro activo se pierde o duplica.
- Ningún registro marcado como eliminado permanece en el archivo compactado.
- Las claves y valores de los registros conservados no cambian.
- Todo offset en el índice publicado apunta al registro correcto.
- La nueva versión puede abrirse después de cerrar y reiniciar el proceso Java.

### 7.1. Publicación segura

**[PROPUESTA]** Usar directorios versionados y un **manifiesto de versión activa**. Primero se generan y validan los nuevos archivos; después se publica un nuevo manifiesto mediante reemplazo atómico **si el sistema de archivos lo soporta** (`Files.move(..., ATOMIC_MOVE, REPLACE_EXISTING)`), y finalmente se permite limpiar la versión anterior.

- `ATOMIC_MOVE` no está garantizado en todos los sistemas de archivos; se deben manejar sus excepciones y definir cómo recuperar el estado, **sin asumir que existe una transacción atómica multiarchivo**.
- Sin un método seguro de publicación, **rechazar el cambio de versión** y conservar la anterior en lugar de arriesgar corrupción.
- Para robustez adicional, sincronizar datos relevantes con `FileDescriptor.sync()` cuando aplique, documentando límites de durabilidad del sistema operativo.
- Al iniciar, reconocer temporales incompletos, manifiestos inválidos y versiones no publicadas; nunca tratarlos automáticamente como datos activos.

## 8. Concurrencia y versiones paralelas

### 8.1. Modelo de hilos

- Crear hilos explícitos (`new Thread(new Worker(...))`, `start()`, `join()`).
- Emplear una **cantidad fija/configurable de hilos**; evitar crear un hilo por registro.
- Cada trabajador tendrá identificador, tarea, archivos asignados, buffers propios y un medio para comunicar errores.
- **Preferir paralelismo por archivos independientes** al inicio: cada hilo lee sus archivos y escribe archivos temporales propios.
- Para datasets con pocos archivos, la partición por rangos/bloques es una mejora opcional; debe respetar límites completos de registros variables.
- Si se utiliza una cola de tareas compartida, deberá ser acotada y sincronizada; no almacenar millones de tareas en RAM.

### 8.2. `INDEX()` paralelo

1. Distribuir archivos o particiones válidas entre `P` trabajadores.
2. Cada hilo recorre sus datos y produce corridas/índices parciales en archivos temporales **propios**.
3. Los hilos finalizan y reportan conteos y errores.
4. Fusionar los resultados mediante fusión externa, sin cargar todos los índices parciales en memoria.
5. Validar orden global, versiones y offsets antes de publicar el índice definitivo.

### 8.3. `PACK()` paralelo

1. Dividir los archivos originales en unidades independientes de trabajo.
2. Cada trabajador crea su archivo compacto temporal y conserva el identificador del archivo fuente.
3. Ningún trabajador modifica ni reemplaza directamente los datos activos.
4. Tras finalizar todos, reconstruir los índices globales con offsets **de los archivos nuevos**.
5. Verificar consistencia global y publicar de forma coordinada una única versión completa.

### 8.4. Reglas de sincronización y seguridad

- **Nunca** compartir un mismo cursor mutable de `RandomAccessFile` entre hilos sin coordinación; preferir una instancia por hilo/archivo.
- No realizar escrituras simultáneas sobre posiciones superpuestas del mismo archivo.
- Evitar estado mutable compartido. Cuando sea inevitable, protegerlo con `synchronized` u otra primitiva estándar adecuada.
- Evitar condiciones de carrera, bloqueos mutuos, espera activa, ciclos de sondeo sin límite y contadores no sincronizados.
- Propagar al hilo coordinador los errores ocurridos en los trabajadores y **abortar la publicación** si falla alguno.
- Usar `try/finally` o `try-with-resources` para cerrar archivos; esperar correctamente a que terminen todos los hilos.
- Separar tiempos de lectura, procesamiento, escritura y combinación de resultados si la instrumentación lo permite.
- Bloquear o coordinar las operaciones que modifican el mismo repositorio (importación, borrado, `INDEX()`, `PACK()`); las lecturas deben observar una versión consistente.

### 8.5. Memoria acotada

**Invariante:** memoria de trabajo aproximada `O(P × B + P × K)` donde `P` es la cantidad de hilos, `B` es el buffer máximo por hilo y `K` es el estado acotado por hilo o por vía de fusión; **no `O(N)`** respecto al número de registros. La memoria adicional del runtime Java no desaparece y debe reportarse cuando sea relevante.

**[PROPUESTA]** Elegir buffers configurables de tamaño moderado y documentado (p. ej. decenas o cientos de KiB por hilo), sin superar el límite máximo definido para cada registro.

## 9. Importación y datasets

- **[PDF]** Hay que demostrar un caso real de aplicación; el PDF actual **no exige un dataset específico** ni un número concreto de registros.
- **[ACUERDO]** Preferir un dataset **real** y documentar su procedencia, licencia, formato, campos y transformación.
- **[PROPUESTA]** Utilizar datos de transacciones o productos de comercio electrónico; alternativamente, otro dominio del proyecto de la asignatura que pueda justificarse.
- Importar el dataset **por streaming desde disco** hacia `.dat` mediante un conversor Java; no cargar toda la tabla a memoria.
- Si se importa CSV, manejar comillas, separadores, escapado, codificación y errores de fila; verificar que el parser no pierde valores.
- No asumir que una hoja XLSX se puede leer de forma robusta con la biblioteca estándar; seleccionar/obtener una exportación textual si se mantiene la restricción de no bibliotecas externas.
- Generar datos **sintéticos adicionales** solo para experimentos de volumen y concurrencia; distinguirlos de los datos reales.
- Introducir eliminaciones lógicas controladas para medir `PACK()`, registrando cuántos registros se marcan.

**Escenarios de evaluación propuestos, no mínimos oficiales:** 10 000, 100 000, 1 000 000 y, si el equipo tiene capacidad, 5 000 000 o más registros; probar también distintas proporciones de eliminados.

## 10. Estándares de código y mantenibilidad

1. **Nombres significativos y consistentes.** Clases en `PascalCase`, métodos y variables en `camelCase`, constantes en `UPPER_SNAKE_CASE`.
2. **Un método, una responsabilidad.** Mantener separadas serialización, acceso físico, índices, compactación, concurrencia y benchmark.
3. **Clases y métodos pequeños cuando sea posible**, sin duplicar lógica serial y paralela que pueda reutilizarse.
4. **Sin valores mágicos:** extraer constantes o parámetros para tamaños de bloques, cantidad de hilos, tipos de registro y estados.
5. **Rutas configurables**, no rutas absolutas del computador del desarrollador ni dependencias de unidades de red usadas en ejemplos docentes.
6. **Manejo explícito de errores:** mensajes útiles, contexto del archivo y offset, y salida distinta de cero ante fallos críticos.
7. **Cierre garantizado de recursos** con `try-with-resources` cuando sea viable.
8. **Tipos correctos:** `long` para offsets, contadores grandes y tamaños; comprobar rangos antes de conversiones a `int`.
9. **Validación de entradas:** claves, longitudes, identificadores, archivos faltantes, formato/versiones incompatibles y estados inválidos.
10. **Sin efectos secundarios ocultos:** una búsqueda no modifica archivos; `INDEX()` no edita registros y `PACK()` publica mediante un proceso controlado.
11. **Determinismo lógico:** sobre los mismos datos de entrada, versión serial y paralela deben producir el mismo conjunto de registros e índice equivalente, aunque el orden físico de fragmentos temporales varíe.
12. **Trazabilidad:** registrar operación, parámetros, duración, hilos, número de registros, cantidad de eliminados y versión publicada.
13. **Comentarios que expliquen decisiones difíciles**, límites de concurrencia, invariantes y formato binario, no comentarios redundantes línea por línea.
14. **Sin salida masiva a consola** durante benchmarks, para no distorsionar mediciones.
15. **Sin dependencias ocultas:** un entorno con JDK compatible debe poder compilar y ejecutar el núcleo con las instrucciones del README.

## 11. Pruebas de calidad y corrección

### 11.1. Casos funcionales mínimos

- Archivos vacíos, un único registro y múltiples archivos.
- Registros con diferentes longitudes y números de campos.
- Campos UTF-8, cadenas vacías y límites máximos permitidos.
- Búsqueda de clave existente, inexistente, primera, última y política de duplicados.
- Registros activos y eliminados intercalados; 0 %, parte y 100 % de eliminados.
- `PACK()` ejecutado dos veces: la segunda vez no debe perder datos ni volver a alterar indebidamente el resultado.
- Búsquedas correctas antes y después de `PACK()`.
- Índices coherentes al reiniciar el programa.
- Archivos grandes y offsets superiores a 2 GiB, si el entorno de prueba lo permite.
- Archivos truncados, cabeceras inválidas, longitudes corruptas y falta de permisos.
- Fallo de un hilo, interrupción de operación y temporales abandonados: **nunca publicar resultados incompletos**.

### 11.2. Equivalencia serial/paralela

Para cada dataset de ensayo:

- Comparar conteos de registros totales, activos y eliminados.
- Comparar secuencias o conjuntos de claves y valores recuperables.
- Verificar cada referencia de índice contra el registro físico en disco.
- Comprobar que ninguna clave activa se pierde y ninguna eliminada reaparece.
- Confirmar que la versión publicada puede volver a abrirse en un proceso nuevo.
- Comparar resultados de `INDEX()` y `PACK()` seriales con sus equivalentes paralelos mediante pruebas reproducibles.

### 11.3. Pruebas de memoria

- Probar tamaños de dataset crecientes bajo un **límite de heap configurado** (`-Xmx`).
- Confirmar que el programa no necesita incrementar proporcionalmente el heap para terminar.
- Registrar heap máximo observado cuando sea posible, además del consumo de memoria del proceso si se mide externamente.

## 12. Benchmark y evaluación del paralelismo

### 12.1. Configuraciones mínimas propuestas

| Variable | Configuraciones sugeridas |
|---|---|
| Operaciones | `INDEX()` y `PACK()` por separado. |
| Modos | Serial y paralelo. |
| Hilos | 1, 2, 4 y 8, **si el hardware y la cantidad de particiones lo justifican**. |
| Registros | 10 k, 100 k, 1 M y escala adicional factible. |
| Eliminados | 0 %, 10 %, 30 %, 50 % como escenarios de prueba. |
| Entorno | Registrar CPU, número de núcleos, RAM, tipo de almacenamiento, SO y JDK. |

### 12.2. Métricas

- Tiempo total de `INDEX()` y `PACK()` medido con `System.nanoTime()`.
- **Speedup:** `S(P) = T_serial / T_paralelo(P)`.
- **Eficiencia:** `E(P) = S(P) / P`.
- Rendimiento aproximado: registros procesados por segundo.
- Tamaño de datos, índices y temporales antes/después de cada operación.
- Espacio recuperado por `PACK()` en bytes y porcentaje.
- Consumo máximo de memoria configurada/observada, cuando sea medible.
- Errores, inconsistencias e interrupciones de operación.

### 12.3. Protocolo de medición

1. Ejecutar la **misma operación** sobre copias equivalentes del mismo dataset inicial.
2. Separar preparación/importación de datos del tiempo medido de `INDEX()` o `PACK()`, salvo que se reporte expresamente una medición integral.
3. Repetir las pruebas y reportar por lo menos mediana y variabilidad.
4. No mezclar el costo de un `PACK()` con condiciones diferentes de porcentaje de eliminados.
5. Registrar si las pruebas se hicieron en caliente o en frío: la caché del sistema operativo puede cambiar los tiempos y no se debe presentar como eliminada cuando no es posible controlarla.
6. No ocultar resultados donde el paralelo sea igual o más lento: explicar contención de disco, fusión externa, tamaño de tareas y sobrecarga de hilos.
7. Guardar resultados en CSV sobre disco para poder elaborar tablas/gráficos después.

## 13. Operaciones y ejecución

La interfaz puede ser **de consola**. Una aplicación web no es necesaria para demostrar el trabajo.

**[PROPUESTA]** Comandos mínimos conceptuales:

```text
java Main importar    --entrada dataset.csv --salida storage/
java Main index       --modo serial   --storage storage/
java Main index       --modo paralelo --hilos 4 --storage storage/
java Main buscar      --clave 1001    --storage storage/
java Main eliminar    --clave 1001    --storage storage/
java Main pack        --modo serial   --storage storage/
java Main pack        --modo paralelo --hilos 4 --storage storage/
java Main benchmark   --storage storage/ --resultados results/
java Main verificar   --storage storage/
```

Los nombres de comandos son **propuestos**: no se considera incumplimiento adoptar una sintaxis distinta con funciones equivalentes.

## 14. Restricciones de alcance (evitar complejidad innecesaria)

**No requeridos para la primera entrega:** interfaz web, microservicios, base de datos externa, contenedores, clúster, MPI, CUDA, GPU, OpenMP o sistemas distribuidos. Esas tecnologías pueden aparecer en el curso, pero **el PDF de este trabajo no las prescribe**. El enfoque inicial será Java multihilo en una máquina y almacenamiento físico local.

**No alterar la consigna original:** `INDEX()` debe construir/consultar índices físicos; `PACK()` debe eliminar físicamente los registros marcados. No reemplazarlos por filtros en memoria, «borrado lógico sin compactación» ni funcionalidades de una base de datos existente.

## 15. Criterios de aceptación del proyecto

### 15.1. Obligatorios para declarar terminado el núcleo técnico

- [ ] Compila y se ejecuta con una versión documentada del JDK y **sin dependencias de terceros**.
- [ ] Administra múltiples archivos físicos de acceso directo con registros y campos variables.
- [ ] Guarda y vuelve a leer metadatos, índices y versiones desde disco.
- [ ] Usa `RandomAccessFile.seek()` para acceso por offset.
- [ ] Importa datos de forma incremental, sin cargar toda la fuente en RAM.
- [ ] `INDEX()` serial genera un índice persistente válido.
- [ ] `INDEX()` paralelo utiliza hilos explícitos y produce un índice equivalente.
- [ ] Una búsqueda usa el índice sin escanear todas las filas del dataset.
- [ ] La eliminación lógica marca registros sin alterar incorrectamente otros.
- [ ] `PACK()` serial elimina físicamente los registros marcados.
- [ ] `PACK()` paralelo utiliza hilos y produce datos equivalentes.
- [ ] Tras cada `PACK()`, los índices apuntan a los offsets nuevos correctos.
- [ ] Una falla antes de publicar no reemplaza datos válidos por una versión incompleta.
- [ ] La memoria de trabajo permanece acotada al aumentar el volumen.
- [ ] Existen pruebas que verifican casos límite y equivalencia serial/paralela.
- [ ] Existe un caso real de aplicación y se documenta el origen de datos.
- [ ] Se presentan tiempos y métricas reproducibles de serial frente a paralelo.
- [ ] El README explica formato, compilación, ejecución, pruebas y resultados.

### 15.2. Propuesta de avance del 50 %

El PDF menciona un avance del 50 %, **sin definir aquí su desglose funcional**. Para un hito interno razonable se propone: arquitectura y formatos aprobados; importación/lectura en disco; metadatos; eliminación lógica; `INDEX()` y `PACK()` **seriales**, más pruebas de correctitud básicas. Las variantes paralelas y el benchmark comparativo cerrarían la segunda parte. Este reparto **no es un criterio oficial del docente**.

## 16. Fuentes de referencia del propio curso

1. **PDF:** *TRABAJO DE APLICACIÓN - Indexación y Compactación de Datos a Gran Escala.pdf*; objetivo de `INDEX()`/`PACK()`, LSDFMS, acceso directo, índices, metadatos y caso real.
2. **ZIP:** `Create_RAF.java`, `Demo_RAF2.java`, `SO_Tools.java`; ejemplos de acceso a archivos y `RandomAccessFile`.
3. **ZIP:** `MultiThreading_UbiGeo_1.java`, `MultiThreading_UbiGeo_2.java`, `MultiThreading_UbiGeo_3.java`, `SynchronizedQueue.java`; ejemplos de hilos y coordinación.
4. **ZIP:** `NOTAS DE CLASE PROG_ CONCURRENTE PARALELA.docx` y `SILABO 2026-2 ... PROGRAMACION CONCURRENTE Y PARALELA.docx`; fundamentos y métricas del procesamiento paralelo.
5. **ZIP, otro ejercicio:** `TRABAJO DE APLICACION 1.docx`; antecedente de preferencias sobre Java, procesamiento en disco, restricciones de estructuras y dependencias. **No debe atribuirse al PDF actual como condición explícita.**

---

**Decisión arquitectónica resumida:** **Java + biblioteca estándar + `RandomAccessFile` + datos/índices/metadatos en disco + memoria RAM acotada + `INDEX()` con ordenamiento externo + `PACK()` con archivos temporales y publicación segura + versiones serial y paralela con `Thread` + pruebas reproducibles de correctitud y rendimiento.**
