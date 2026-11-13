import java.io.IOException;
import java.io.FileReader;
import java.io.BufferedReader;
import java.io.FileWriter;

public class MultiThreading_UbiGeo_3 {

public static SO_Tools TLS = new SO_Tools();
public static int n; //Variable de Control del n-esimo hilo

//=============================================================
public static void main(String args[]) throws IOException, InterruptedException {


//-------------------------------------------------------------------
//-------------------------------------------------------------------
int mm=100;
int nn=50;
int[][] A = new int[mm][nn];
    for(int i=0;i<=mm-1;i++) {
        for(int j=0;j<=nn-1;j++) {
            A[i][j] = 1000 + (int)(Math.random()*(9999-1000+1));
        }
    }
    TLS.PrintMatrixToHTML(A,"Matriz Aleatoria", "Salida.html");
//-------------------------------------------------------------------
//-------------------------------------------------------------------
    //Imprimir  en HTML las provincias de los regiones/departamentos
    TLS.ObtenerProvinciasPorDepartamento("Departamentos", "Salida123.html");

//-------------------------------------------------------------------
//-------------------------------------------------------------------
    TLS.Convert_TXT_DAT("UbiGeo.txt","UbiGeo.dat",50);
    TLS.Convert_TXT_DAT("CCPP.txt","CCPP.dat",60);
    //Previamente cargamos los nombres de Regiones/Departamentos
    //en el vector REGION. No se usa la posicion o celda 0.
    TLS.CargarRegiones_2();

    /*
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
    */
}//main
//=============================================================
}//class