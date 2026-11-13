# Resultados reproducibles

Las mediciones se hicieron en Windows 11 (build 26200), con un AMD Ryzen 7 7445HS de 6 núcleos y 12 procesadores lógicos, 16 GB de RAM, un SSD NVMe y Oracle JDK 25.0.1 con `-Xmx64m`.

## Datos sintéticos

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
