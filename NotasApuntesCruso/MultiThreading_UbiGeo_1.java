public class MultiThreading_UbiGeo_1 {

public static SO_Tools TLS = new SO_Tools(); //CREA E INSTANCIA
//=============================================================
public static void main(String args[]) throws InterruptedException {
    //---------------------------------------------------------
    //Previamente cargamos los nombres de Regiones/Departamentos
    //en el vector REGION. No se usa la posicion o celda 0.
    //---------------------------------------------------------
    TLS.CargarRegiones();
    //---------------------------------------------------------
    //Crear 25 Hilos. Uno para cada REGION.
    //---------------------------------------------------------
    new Thread(new Runnable() {
       public void run() {
          TLS.Procesar_UbiGeo_CCPP("01");
       }
    }).start();
    //----------------------------------------
    new Thread(new Runnable() {
       public void run() {
          TLS.Procesar_UbiGeo_CCPP("02");
       }
    }).start();
    //----------------------------------------
    new Thread(new Runnable() {
       public void run() {
          TLS.Procesar_UbiGeo_CCPP("03");
       }
    }).start();
    //----------------------------------------
    new Thread(new Runnable() {
       public void run() {
          TLS.Procesar_UbiGeo_CCPP("04");
       }
    }).start();
    //----------------------------------------
    new Thread(new Runnable() {
       public void run() {
          TLS.Procesar_UbiGeo_CCPP("05");
       }
    }).start();
    //----------------------------------------
    new Thread(new Runnable() {
       public void run() {
          TLS.Procesar_UbiGeo_CCPP("06");
       }
    }).start();
    //----------------------------------------
    new Thread(new Runnable() {
       public void run() {
          TLS.Procesar_UbiGeo_CCPP("07");
       }
    }).start();
    //----------------------------------------
    new Thread(new Runnable() {
       public void run() {
          TLS.Procesar_UbiGeo_CCPP("08");
       }
    }).start();
    //----------------------------------------
    new Thread(new Runnable() {
       public void run() {
          TLS.Procesar_UbiGeo_CCPP("09");
       }
    }).start();
    //----------------------------------------
    new Thread(new Runnable() {
       public void run() {
          TLS.Procesar_UbiGeo_CCPP("10");
       }
    }).start();
    //----------------------------------------
    new Thread(new Runnable() {
       public void run() {
          TLS.Procesar_UbiGeo_CCPP("11");
       }
    }).start();
    //----------------------------------------
    new Thread(new Runnable() {
       public void run() {
          TLS.Procesar_UbiGeo_CCPP("12");
       }
    }).start();
    //----------------------------------------
    new Thread(new Runnable() {
       public void run() {
          TLS.Procesar_UbiGeo_CCPP("13");
       }
    }).start();
    //----------------------------------------
    new Thread(new Runnable() {
       public void run() {
          TLS.Procesar_UbiGeo_CCPP("14");
       }
    }).start();
    //----------------------------------------
    new Thread(new Runnable() {
       public void run() {
          TLS.Procesar_UbiGeo_CCPP("15");
       }
    }).start();
    //----------------------------------------
    new Thread(new Runnable() {
       public void run() {
          TLS.Procesar_UbiGeo_CCPP("16");
       }
    }).start();
    //----------------------------------------
    new Thread(new Runnable() {
       public void run() {
          TLS.Procesar_UbiGeo_CCPP("17");
       }
    }).start();
    //----------------------------------------
    new Thread(new Runnable() {
       public void run() {
          TLS.Procesar_UbiGeo_CCPP("18");
       }
    }).start();
    //----------------------------------------
    new Thread(new Runnable() {
       public void run() {
          TLS.Procesar_UbiGeo_CCPP("19");
       }
    }).start();
    //----------------------------------------
    new Thread(new Runnable() {
       public void run() {
          TLS.Procesar_UbiGeo_CCPP("20");
       }
    }).start();
    //----------------------------------------
    new Thread(new Runnable() {
       public void run() {
          TLS.Procesar_UbiGeo_CCPP("21");
       }
    }).start();
    //----------------------------------------
    new Thread(new Runnable() {
       public void run() {
          TLS.Procesar_UbiGeo_CCPP("22");
       }
    }).start();
    //----------------------------------------
    new Thread(new Runnable() {
       public void run() {
          TLS.Procesar_UbiGeo_CCPP("23");
       }
    }).start();
    //----------------------------------------
    new Thread(new Runnable() {
       public void run() {
          TLS.Procesar_UbiGeo_CCPP("24");
       }
    }).start();
    //----------------------------------------
    new Thread(new Runnable() {
       public void run() {
          TLS.Procesar_UbiGeo_CCPP("25");
       }
    }).start();
    //----------------------------------------
}//main
//=============================================================
}//class
