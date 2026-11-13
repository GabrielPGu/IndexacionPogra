# Resultados reproducibles

Las mediciones se hicieron en Windows 11 (build 26200), con un AMD Ryzen 7 7445HS de 6 núcleos y 12 procesadores lógicos, 16 GB de RAM, un SSD NVMe y Oracle JDK 25.0.1 con `-Xmx64m`.

## Medición final

Los seis archivos `final-*.csv` contienen 240 muestras y 48 medianas. Se ejecutaron cinco repeticiones por configuración después de una ronda de calentamiento, con ocho archivos y 1, 2, 4 u 8 hilos. Las operaciones se midieron sin otras pruebas del proyecto en curso. La caché del sistema estaba activa. El benchmark usa el almacén sintético y mide la ruta del índice primario.

Para un millón de registros y 30 % de eliminados, estas son las medianas. El rango mínimo–máximo de cada configuración está en [el CSV](final-1000000-30.csv).

| Hilos | INDEX ms | Aceleración INDEX | PACK ms | Aceleración PACK |
|---:|---:|---:|---:|---:|
| 1 | 725,587 | 1,000× | 887,859 | 1,000× |
| 2 | 552,910 | 1,312× | 653,305 | 1,359× |
| 4 | 504,550 | 1,438× | 498,843 | 1,780× |
| 8 | 458,705 | 1,582× | 513,336 | 1,730× |

Las cuatro configuraciones paralelas con mediana superior a la serial se conservan junto con todas las muestras.

| Operación | Registros | Eliminados | Hilos | Aceleración |
|---|---:|---:|---:|---:|
| INDEX | 10.000 | 30 % | 8 | 0,924× |
| PACK | 10.000 | 30 % | 8 | 0,934× |
| INDEX | 100.000 | 0 % | 2 | 0,978× |
| INDEX | 100.000 | 30 % | 8 | 0,973× |

Para reproducir la medición final desde `Proyecto` con PowerShell:

```powershell
foreach ($n in 10000,100000,1000000) {
    java -Xmx64m -cp out Main benchmark "results/repeticion-${n}-30.csv" $n 8 30 5 2 4 8
}
foreach ($pct in 0,10,50) {
    java -Xmx64m -cp out Main benchmark "results/repeticion-100000-${pct}.csv" 100000 8 $pct 5 2 4 8
}
```

## Validación de recuperación

`java -Xmx64m -cp out SystemTest --offset-grande` terminó con código 0. Tras un fallo de trabajador, la prueba comprobó que se conservó el manifiesto anterior, se retiró la versión parcial y el reintento tras corregir el archivo funcionó en la misma instancia.

| Cierre forzado del proceso | Resultado al reabrir |
|---|---|
| Después de escribir los datos compactados | Se mantuvo la versión anterior, se limpió la copia y el reintento publicó correctamente. |
| Con ambos índices validados y el manifiesto temporal sincronizado | Se mantuvo la versión anterior, se retiraron los archivos nuevos y el reintento publicó correctamente. |
| Después del reemplazo atómico y antes de la limpieza | Se conservaron los datos y ambos índices nuevos. Se retiró la versión anterior. |

En las tres etapas se comprobaron consultas por `article_id`, el borrado y la verificación completa del almacén recuperado. La prueba de offsets recuperó el registro mediante el índice a **2.147.487.744 bytes**, 4.096 bytes por encima de 2 GiB.

## Datos sintéticos de la primera entrega

Se probaron 10.000, 100.000 y 1.000.000 de registros en ocho archivos, con 1, 2, 4 y 8 hilos. El escenario principal marca el 30 % de los registros. Para 100.000 registros se probaron también 0 %, 10 % y 50 %. Cada configuración tiene tres mediciones después del calentamiento. Los CSV incluyen los valores individuales y su rango.

| Registros | Eliminados | INDEX 1 hilo | INDEX 8 hilos | PACK 1 hilo | PACK 8 hilos |
|---:|---:|---:|---:|---:|---:|
| 10.000 | 30 % | 239,478 ms | 224,259 ms | 2.155,235 ms | 861,812 ms |
| 100.000 | 30 % | 181,022 ms | 157,042 ms | 439,832 ms | 409,735 ms |
| 1.000.000 | 30 % | 1.351,126 ms | 777,870 ms | 1.835,538 ms | 673,972 ms |

En un millón de registros, ocho hilos dieron 1,737× en INDEX y 2,723× en PACK, con eficiencias de 0,217 y 0,340. Con 100.000 filas y 50 % de eliminados, el INDEX con ocho hilos tardó 226,815 ms frente a 211,843 ms en serial. La fusión global, la verificación y la publicación incluyen trabajo serial. El disco, las sincronizaciones de escritura y el número de archivos también condicionan cuánto pueden avanzar los trabajadores a la vez.

La caché del sistema estaba activa y parte de las mediciones coincidió con otras pruebas locales de E/S, incluida la ejecución con H&M. En particular, el rango del PACK serial de 10.000 filas fue de 445,227 a 2.441,733 ms. Las cifras describen esta ejecución y su variabilidad.

Para reproducir el barrido desde `Proyecto` con PowerShell:

```powershell
foreach ($n in 10000,100000,1000000) {
    java -Xmx64m -cp out Main benchmark "results/repeticion-${n}-30.csv" $n 8 30 3 2 4 8
}
foreach ($pct in 0,10,50) {
    java -Xmx64m -cp out Main benchmark "results/repeticion-100000-${pct}.csv" 100000 8 $pct 3 2 4 8
}
```

## Datos reales

La ejecución completa con cuatro particiones por tabla importó 105.542 artículos, 1.371.980 clientes y 31.788.324 transacciones. El total de 33.265.846 registros ocupó 4.743.623.066 bytes en datos. Con cuatro hilos, el INDEX produjo 665.316.944 bytes de índice en 87,411 s. El límite de heap fue de 64 MiB.

La prueba de muestra sobre 300.000 registros buscó la transacción con clave 2.000.000.000.001, la marcó como eliminada y compactó el almacén. El tamaño de datos bajó de 75.581.929 a 75.581.789 bytes, el índice perdió una entrada y la búsqueda posterior indicó que la clave estaba ausente.

En el almacén completo, PACK con cuatro hilos tardó 155,112 s. La versión 2 quedó con 33.265.845 registros activos y cero marcas de borrado. La diferencia fue de 140 bytes en datos y 20 bytes en el índice, que reconstruyó los offsets de los registros restantes. Un proceso nuevo confirmó la ausencia de la clave eliminada y verificó todos los registros y sus referencias.

En la revisión, se volvió a ejecutar INDEX sobre ese almacén existente. Se construyó un índice secundario de 105.542 artículos sin volver a importar. La consulta por `0108775015` devolvió el artículo `Strap top`, y `0000000000` devolvió una clave ausente. Después de ejecutar PACK con cuatro hilos y `-Xmx64m`, la consulta devolvió los mismos campos y se verificaron los 33.265.845 registros y ambos índices en la versión 3.
