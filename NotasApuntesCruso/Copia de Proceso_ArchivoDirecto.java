import java.io.IOException;

//import java.io.FileReader;
//import java.io.BufferedReader;

import java.io.FileWriter;

import java.io.RandomAccessFile;

public class Proceso_ArchivoDirecto {

//=============================================================
public static byte[] StringToBuffer(String CADENA, int B) {
byte[] BUFFER = new byte[B];
int L = CADENA.length();
    for(int i=0;i<=L-1;i++) {
        BUFFER[i] = (byte)CADENA.charAt(i);
    }
   return BUFFER;
}
//=============================================================
public static String BufferToString(byte[] BUFFER) {
String CADENA="";
int L = BUFFER.length;
    for(int i=0;i<=L-1;i++) {
        CADENA = CADENA + (char)BUFFER[i];
    }
    return CADENA;
}
//=============================================================
public static void CargarRegiones() throws IOException, InterruptedException {
long T;
String CADENA;
int  N,W=50;
byte[] BUFFER;
     RandomAccessFile RAF = new RandomAccessFile("UbiGeo.dat","r");
     T = RAF.length();
     System.out.println(T);
     System.out.println(W);
     System.out.println(T%W);
     if(T%W==0) {
         System.out.println("\n-----------------------------");
         N = (int)(T/W);
         BUFFER = new byte[W];
         for(int k=0;k<=N-1;k++) {

             RAF.seek(k*W);
             RAF.read(BUFFER);
             CADENA = BufferToString(BUFFER);

             if(CADENA.substring(2,6).equals("0000")==true) {
                System.out.println(CADENA);
           }
         }
         RAF.close();
     }
     else {
        System.out.println("ERROR: Dimension Incorrecta!!!");
     }
}
//=============================================================
public static void ObtenerProvinciasPorDepartamento(String TITLE, String FILENAME) throws IOException, InterruptedException {
long T;
String CADENA,CodDPTO;
int  N,W=50;
byte[] BUFFER;
RandomAccessFile RAF1 = new RandomAccessFile("UbiGeo.dat","r");
RandomAccessFile RAF2 = new RandomAccessFile("UbiGeo.dat","r");
    try {
        T = RAF1.length();
        if(T%W==0) {
           System.out.println("\n-----------------------------");
           N = (int)(T/W);
           BUFFER = new byte[W];

           FileWriter FW = new FileWriter(FILENAME);
           FW.write("<H1>" + TITLE + "</H1>");
           FW.write("<HTML>\n");
           FW.write("<HEAD>");
           FW.write("</HEAD>");
           FW.write("<BODY>\n");
           FW.write("<TABLE BORDER=0 CELLPADDING=7  CELLSPACING=1>\n");
           for(int k=0;k<=N-1;k++) {
               RAF1.seek(k*W);
               RAF1.read(BUFFER);
               CADENA = BufferToString(BUFFER);
               if(CADENA.substring(2,6).equals("0000")==true) {

                  CodDPTO = CADENA.substring(0,2);
                  
                  FW.write("\n<TR>");
                  FW.write("<TD BGCOLOR=#AED6F1>" + CodDPTO + "</TD>");
                  FW.write("<TD BGCOLOR=#A3E4D7>" + CADENA.substring(7,50) + "</TD>");
                  for(int i=0;i<=N-1;i++) {
                      RAF2.seek(i*W);
                      RAF2.read(BUFFER);
                      CADENA = BufferToString(BUFFER);
                      if( (CADENA.substring(0,2).equals(CodDPTO)==true)&&
                          (CADENA.substring(2,4).equals("00")==false)&&
                          (CADENA.substring(4,6).equals("00")==true) ) {
                           FW.write("<TD BGCOLOR=#FFFAF0>" + CADENA.substring(7,50) + "</TD>");
                      }
                  }
                  FW.write("</TR>");
               } //if
           } //for
           RAF2.close();
           RAF1.close();
           FW.write("</TABLE>");
           FW.write("</BODY>");
           FW.write("</HTML>");
           FW.close();
        } 
        else {
          System.out.println("ERROR: Dimension Incorrecta!!!");
        }
    }
    catch (IOException E) {
        System.out.print(E.getMessage());
    }
}
//=============================================================
//=============================================================
public static void main(String args[]) throws IOException, InterruptedException {

    //Previamente Ejecutar el Programa Create_RAF
    
    //Imprimir  en HTML las provincias de los regiones/departamentos
    ObtenerProvinciasPorDepartamento("Departamentos", "Salida123.html");
    //Previamente cargamos los nombres de Regiones/Departamentos
    //en el vector REGION. No se usa la posicion o celda 0.
    CargarRegiones();
}//main
//=============================================================
}//class
