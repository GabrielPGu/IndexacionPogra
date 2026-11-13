import java.io.IOException;
import java.io.FileReader;
import java.io.BufferedReader;
import java.io.FileWriter;

public class MultiThreading_UbiGeo_2 {

public static SO_Tools TLS = new SO_Tools();
public static int n; //Variable de Control del n-esimo hilo

//=============================================================
public static void main(String args[]) throws InterruptedException {
    //Previamente cargamos los nombres de Regiones/Departamentos
    //en el vector REGION. No se usa la posicion o celda 0.
    TLS.CargarRegiones();
    n = 1;
    while(n<=25) {
       //========================================
       new Thread(new Runnable() {
          public void run() {
             TLS.Procesar_UbiGeo_CCPP((n<10?"0":"")+n);
          }
       }).start();
       //Colocamos un temporizador (delay) para pausar el hilo
       Thread.sleep(1000);
       //========================================
       n++;
    } 
}//main
//=============================================================
}//class