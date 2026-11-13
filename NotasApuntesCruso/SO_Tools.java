import java.io.BufferedReader;
import java.io.FileWriter;
import java.io.FileReader;
import java.io.RandomAccessFile;
import java.io.IOException;
import java.io.File;
import java.util.Scanner;
import java.util.Random;

public class SO_Tools {

//=============================================================
private int T = 256;
private int N = 0;
private char ITEM[] = new char[T];
private long FREQ[] = new long[T];
//-------------------------------------------------------------
// Define un vector de 26 celdas.
// La celda V[0] no se utiliza. Unicamente V[1] ... V[25]
public static String[] REGION = new String[25 + 1]; 
//=============================================================
public SO_Tools() {
}
//=============================================================
public int ReadInteger() {
Scanner OBJ = new Scanner(System.in);
   return OBJ.nextInt();
}
//=============================================================
public int[] CreateVector(int N, int a, int b) { //Generar
int VECTOR[] = new int[N];
    for(int k=0;k<=N-1;k++) {
        VECTOR[k] = a + (int)(Math.random()*(b-a+1));
    }
    return VECTOR;
}
//=============================================================
public int[][] CreateMatrix(int M, int N, int a, int b) {
int MATRIX[][] = new int[M][N];
    for(int i=0;i<=M-1;i++) {
        for(int j=0;j<=N-1;j++) {
            MATRIX[i][j] = a + (int)(Math.random()*(b-a+1));
        }
    }
    return MATRIX;
}
//=============================================================
public String Replicate(char E, int N) {
int L;
String CAD;
    L = 0;
    CAD = "";
    for(int i=0;i<=N-1;i++) {
        CAD = CAD + E;
    }
    return CAD;
}
//=============================================================
public int GetRandomNumber(int N1, int N2) {
    return N1 + (int)(Math.random()*(N2 -N1 + 1));
}
//=============================================================
public int GetRandomNumber(int N) {
    return GetRandomNumber(0,N);
}
//=============================================================
public byte[] StringToBuffer(String CADENA, int B) {
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
public void WriteVectorScreen(String DATAFILE, int N) throws IOException, InterruptedException {
String CADENA;
long T;
int W;
byte[] BUFFER;
   try {
     RandomAccessFile RAF = new RandomAccessFile(DATAFILE,"r");
     T = RAF.length();
     if(T%N==0) {
         System.out.println("\n" + DATAFILE + "\n-------------");
         W = (int)(T/N);
         BUFFER = new byte[W];
         for(int k=0;k<=N-1;k++) {
             RAF.seek(k*W);
             RAF.read(BUFFER);
             CADENA = BufferToString(BUFFER);
             System.out.println(CADENA);
         }
         RAF.close();
     }
     else {
        System.out.println("ERROR: Dimension Incorrecta!!!");
     }
   }
   catch(IOException e) {
   }
}
//=============================================================
public void WriteMatrixScreen(String DATAFILE, int M, int N) throws IOException, InterruptedException {
String CADENA;
long T;
int W;
byte[] BUFFER;
   try {
     RandomAccessFile RAF = new RandomAccessFile(DATAFILE,"r");
     T = RAF.length();
     if(T%(M*N)==0) {
         System.out.println("\n" + DATAFILE + "\n-------------");
         W = (int)(T/(M*N));
         BUFFER = new byte[W];
         for(int i=0;i<=M-1;i++) {
             for(int j=0;j<=N-1;j++) {
                 RAF.seek(i*N*W + j*W);
                 RAF.read(BUFFER);
                 CADENA = BufferToString(BUFFER);
                 System.out.print(CADENA + "\t");
             }
             System.out.println();
         }
         RAF.close();
     }
     else {
        System.out.println("ERROR: Dimension Incorrecta!!!");
     }
   }
   catch(IOException e) {
   }
}
//=============================================================
public int WidthColumns(int W[],int nn) {
int WW = 0;
    for(int j=0;j<=nn-1;j++) {
        WW = WW + W[j];
    }
    return WW;
}
//=============================================================
public void WriteMatrixScreenVariable(String DATAFILE, int M, int N, int W[]) throws IOException, InterruptedException {
String CADENA;
long L,T;
byte[] BUFFER;
   try {
     RandomAccessFile RAF = new RandomAccessFile(DATAFILE,"r");
     T = RAF.length();
     L = W.length;
     if(T%M==0) {
         System.out.println("\n" + DATAFILE + "\n-------------");
         //W = (int)(T/(M*N));
         for(int i=0;i<=M-1;i++) {
             for(int j=0;j<=N-1;j++) {
                 BUFFER = new byte[W[j]];
                 RAF.seek(i*WidthColumns(W,N) + WidthColumns(W,j));
                 RAF.read(BUFFER);
                 CADENA = BufferToString(BUFFER);
                 System.out.print(CADENA + "\t");
             }
             System.out.println();
         }
         RAF.close();
     }
     else {
        System.out.println("ERROR: Dimension Incorrecta!!!");
     }
   }
   catch(IOException e) {
   }
}
//=============================================================
public int ReadCell(RandomAccessFile RAF, int i, int j, int P, int W) throws IOException, InterruptedException {
byte[] BUFFER = new byte[W];
String CADENA;
   RAF.seek(P);
   RAF.read(BUFFER);
   CADENA = BufferToString(BUFFER).trim();
   return Integer.parseInt(CADENA);
}
//=============================================================
public void ProductoInterno(String DATAFILE, int M, int N, int j, int k, int W[]) throws IOException, InterruptedException {
long L,T;
int WW = WidthColumns(W,N);
int S=0;
   try {
     RandomAccessFile RAF = new RandomAccessFile(DATAFILE,"r");
     T = RAF.length();
     L = W.length;
     if(T%M==0) {
         S = 0;
         for(int i=0;i<=M-1;i++) {
             S = S + ReadCell(RAF,i,j,i*WW+WidthColumns(W,j),W[j])*
                     ReadCell(RAF,i,k,i*WW+WidthColumns(W,k),W[k]);
         }
         RAF.close();
         System.out.println(S);
     }
     else {
        System.out.println("ERROR: Dimension Incorrecta!!!");
     }
   }
   catch(IOException e) {
   }
}
//=============================================================
/*
public void Asociacion(int P1,int P2) {
String CADENA;
long L,T;
int WW = WidthColumns(W,N);
   try {
     RandomAccessFile RAF = new RandomAccessFile(DATAFILE,"r");
     T = RAF.length();
     L = W.length;
     if(T%M==0) {
         for(int i=0;i<=M-1;i++) {
             for(int j=0;j<=N-2;j++) {
                 for(int k=j+1;k<=N-1;k++) {
                     ReadCell(RAF,i,j,i*WW+WidthColumns(W,j),W[j])
                     ReadCell(RAF,i,k,i*WW+WidthColumns(W,k),W[k])
                 }
             }
         }
         RAF.close();
     }
     else {
        System.out.println("ERROR: Dimension Incorrecta!!!");
     }
   }
   catch(IOException e) {
   }
}*/
//=============================================================
public String SumaVectores(String DATAFILE1, String DATAFILE2, int N, int SIZE) throws IOException, InterruptedException {
String CAD1,CAD2,TMP,SUMA;
long T1,T2;
int W,AUX;
byte[] BUFFER;
   SUMA = "";
   try {
     RandomAccessFile RAF1 = new RandomAccessFile(DATAFILE1,"r");
     RandomAccessFile RAF2 = new RandomAccessFile(DATAFILE2,"r");
     T1 = RAF1.length();
     T2 = RAF2.length();
     if(T1==T2) {
        if(T1%N==0) {
            W = (int)(T1/N);
            BUFFER = new byte[W];
            for(int k=0;k<=N-1;k++) {

               RAF1.seek(k*W);
               RAF1.read(BUFFER);
               CAD1 = BufferToString(BUFFER).trim();

               RAF2.seek(k*W);
               RAF2.read(BUFFER);
               CAD2 = BufferToString(BUFFER).trim();

               AUX  = Integer.parseInt(CAD1) + 
                      Integer.parseInt(CAD2);

               TMP = "" + AUX + "";
               TMP = Replicate(' ',SIZE-TMP.length()) + TMP;
               SUMA = SUMA  +  TMP;
            }
            RAF1.close();
            RAF2.close();
        }
        else {
           System.out.println("ERROR: Dimension Incorrecta!!!");
        }
     }
     else {
        System.out.println("ERROR: Los Archivos tienen diferentes tamaños!!!");
     }
   }
   catch(IOException e) {
   }
   return SUMA;
}
//=============================================================
public void SumaMatrices(String DATAFILE1, String DATAFILE2, String DATAFILE3, int M, int N, int SIZE) throws IOException, InterruptedException {
String CAD1,CAD2,TMP;
long T1,T2;
int W,AUX,POS;
byte[] BUFFER;
   try {
     RandomAccessFile RAF1 = new RandomAccessFile(DATAFILE1,"r");
     RandomAccessFile RAF2 = new RandomAccessFile(DATAFILE2,"r");
     RandomAccessFile RAF3 = new RandomAccessFile(DATAFILE3,"rw");
     T1 = RAF1.length();
     T2 = RAF2.length();
     if(T1==T2) {
        if(T1%(M*N)==0) {
           POS = 0;
           W = (int)(T1/(M*N));
           BUFFER = new byte[W];
           for(int i=0;i<=M-1;i++) {
               for(int j=0;j<=N-1;j++) {
                   RAF1.seek(i*N*W + j*W);
                   RAF1.read(BUFFER);
                   CAD1 = BufferToString(BUFFER).trim();

                   RAF2.seek(i*N*W + j*W);
                   RAF2.read(BUFFER);
                   CAD2 = BufferToString(BUFFER).trim();

                   AUX  = Integer.parseInt(CAD1) + 
                          Integer.parseInt(CAD2);

                   TMP = "" + AUX + "";
                   TMP = Replicate(' ',SIZE-TMP.length()) + TMP;

                   BUFFER = StringToBuffer(TMP,W);
                   RAF3.seek(POS);
                   RAF3.write(BUFFER);
                   POS = POS + W;

               }
               System.out.println();
           }
           RAF3.close();
           RAF2.close();
           RAF1.close();
        }
        else {
           System.out.println("ERROR: Dimension Incorrecta!!!");
        }
     }
     else {
        System.out.println("ERROR: Los Archivos tienen diferentes tamaños!!!");
     }
   }
   catch(IOException e) {
   }
}
//=============================================================
public void PrintMatrixToScreen(int MTX[][], String RES[], String TITLE) {
int N = MTX.length;
    System.out.println(TITLE);
    for(int i=0;i<=N-1;i++) {
        for(int j=0;j<=N-1;j++) {
            System.out.print(MTX[i][j] + "\t");
        }
        System.out.print(" | " + RES[i] + "\n");
    }
    System.out.println();
}
public void PrintMatrixToScreen(int MTX[][], String TITLE) {
int N = MTX.length;
    System.out.println(TITLE);
    for(int i=0;i<=N-1;i++) {
        for(int j=0;j<=N-1;j++) {
            System.out.print(MTX[i][j] + "\t");
        }
        System.out.println();
    }
    System.out.println();
}
//=============================================================
public void PrintMatrixToFile(int MTX[][], String TITLE, String FILENAME) {
int N = MTX.length;
    try {
        FileWriter FW = new FileWriter(FILENAME);
        FW.write(TITLE + "\n");
        for(int i=0;i<=N-1;i++) {
            for(int j=0;j<=N-1;j++) {
                FW.write(MTX[i][j] + "\t");
            }
            FW.write("\n");
        }
        FW.write("\n");
        FW.close();
    }
    catch (IOException E) {
        System.out.print(E.getMessage());
    }
}
//=============================================================
public void PrintStringToFile(String FILENAME, String CADENA) {
    try {
        FileWriter FW = new FileWriter(FILENAME);
        FW.write(CADENA);
        FW.close();
    }
    catch (IOException E) {
        System.out.print(E.getMessage());
    }
}
//=============================================================
public void CreateDataFile(String FILENAME, int N, char Mode, int a, int b, int SIZE) {
int NUM;
String STR;
    try {
        FileWriter FW = new FileWriter(FILENAME);
        for(int k=0;k<=N-1;k++) {
            NUM = a + (int)(Math.random()*(b-a+1));
            STR = "" + NUM + "";
            STR = Replicate(' ',SIZE-STR.length()) + STR;
            FW.write(STR + (Mode=='H'?"":"\n") );
        }
        FW.close();
    }
    catch (IOException E) {
        System.out.print(E.getMessage());
    }
}
//=============================================================
public void CreateDataFileVariable(String FILENAME, int M, int N, char Mode, int[] a, int[] b, int SIZE[]) {
int NUM;
String STR;
    try {
        FileWriter FW = new FileWriter(FILENAME);
        for(int i=0;i<=M-1;i++) {
            for(int k=0;k<=N-1;k++) {
                NUM = a[k] + (int)(Math.random()*(b[k]-a[k]+1));
                STR = "" + NUM + "";
                STR = Replicate(' ',SIZE[k]-STR.length()) + STR;
                FW.write(STR);
            }
            FW.write((Mode=='H'?"":"\n"));
        }
        FW.close();
    }
    catch (IOException E) {
        System.out.print(E.getMessage());
    }
}
//=============================================================
public void ObtenerProvinciasPorDepartamento(String TITLE, String FILENAME) throws IOException, InterruptedException {
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
           FW.write("<HTML>");
           FW.write("<HEAD>");
           FW.write("</HEAD>");
           FW.write("<BODY>");
           FW.write("<TABLE BORDER=0 CELLPADDING=7  CELLSPACING=1>");
           for(int k=0;k<=N-1;k++) {
               RAF1.seek(k*W);
               RAF1.read(BUFFER);
               CADENA = BufferToString(BUFFER);
               if(CADENA.substring(2,6).equals("0000")==true) {

                  CodDPTO = CADENA.substring(0,2);
                  
                  FW.write("<TR>");
                  FW.write("<TD BGCOLOR=#AED6F1>" + CodDPTO + "</TD>");
                  FW.write("<TD BGCOLOR=#A3E4D7>" + CADENA.substring(7,50) + "</TD>");
                  for(int i=0;i<=N-1;i++) {
                      RAF2.seek(i*W);
                      RAF2.read(BUFFER);
                      CADENA = BufferToString(BUFFER);
                      if( (CADENA.substring(0,2).equals(CodDPTO)==true)&&
                          (CADENA.substring(2,4).equals("00")==false)&&
                          (CADENA.substring(4,6).equals("00")==true) ) {
                           FW.write("<TD BGCOLOR=#FF00FF>" + CADENA.substring(7,50) + "</TD>");
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
public void PrintMatrixToHTML(int MTX[][], String TITLE, String FILENAME) {
int M = MTX.length;
int N = MTX[0].length;
    try {
        FileWriter FW = new FileWriter(FILENAME);
        FW.write("<H1>" + TITLE + "</H1>");
        FW.write("<HTML>");
        FW.write("<HEAD>");
        FW.write("</HEAD>");
        FW.write("<BODY>");
        FW.write("<TABLE BORDER=1 CELLPADDING=7  CELLSPACING=0  BGCOLOR=#AED6F1>"); //#A3E4D7
        for(int i=0;i<=M-1;i++) {
            FW.write("<TR>");
            for(int j=0;j<=N-1;j++) {
                
              /*if(i==j) {
                   FW.write("<TD BGCOLOR=#FADBD8>");
                }
                else {
                   FW.write("<TD>");
                }*/
                FW.write("<TD>");


                FW.write(MTX[i][j] + "</TD>");
            }
            FW.write("</TR>");
        }

        FW.write("</TABLE>");
        FW.write("</BODY>");
        FW.write("</HTML>");

        FW.close();
    }
    catch (IOException E) {
        System.out.print(E.getMessage());
    }
}
//=============================================================
// METODOS DE LECTURA/ESCRITURA EN DISCO 
//=============================================================
public void WriteData1ToFile(int N, int A, int B, String FILENAME, char SEPARATOR, int LONGITUD) {
double X;
long num;
String ITEM;
    try {
        FileWriter FW = new FileWriter(FILENAME);
        for(int i=1;i<=N;i++) {
            X = A + (int)(Math.random()*(B-A+1));
            num = (long)X;
            ITEM = "" + num + "";
            if(SEPARATOR=='?') {
               if(LONGITUD>0) {
                     ITEM = Replicate(' ',LONGITUD - ITEM.length()) + ITEM;
               }
               else {
                     ITEM = ITEM + "\n";
               }
            }
            else {
               if(LONGITUD>0) {
                     ITEM = Replicate(' ',LONGITUD - ITEM.length()) + ITEM + (i<N?SEPARATOR:"");
               }
               else {
                     ITEM = ITEM + (i<N?SEPARATOR:"");
               }
            }
            FW.write(ITEM);
        }
        FW.close();
    }
    catch (IOException E) {
        System.out.print(E.getMessage());
    }
}
//=============================================================
public void WriteData2ToFile(int M, int N, int A, int B, String FILENAME) {
double X;
long num;
    try {
        FileWriter FW = new FileWriter(FILENAME);
        for(int i=1;i<=M;i++) {
            for(int j=1;j<=M;j++) {
                X = A + (int)(Math.random()*(B-A+1));
                num = (long)X;
                FW.write(num + "\t");
            }
            FW.write("\n");
        }
        FW.close();
    }
    catch (IOException E) {
        System.out.print(E.getMessage());
    }
}
//=============================================================
public String ReadDataFromFile(String FILENAME) {
String LINE="",CADENA="";
   try {
     File FILE = new File(FILENAME);
     BufferedReader BR = new BufferedReader(new FileReader(FILE));
     while ((LINE = BR.readLine()) != null) {
       CADENA = CADENA + LINE;
     }
   }
   catch(IOException e) {
   }
   return CADENA;
}
//=============================================================
public void Inicializar() {
  this.N = 0;
  for(int k=0;k<=this.T-1;k++) {
        ITEM[k] = (char)0;
        FREQ[k] = 0;
  }
}
//=============================================================
public boolean Exist(char E) {
boolean Sw;
  Sw = false;
  int k = 0;
  while((k <= N-1) && (Sw == false)){
      if(ITEM[k] == E){
          Sw = true;    
      }
      k++;
  }
  return Sw;
}
//=============================================================
public void TraceFile(String FILENAME) {
String LINE="";
int n;
   try {
     File FILE = new File(FILENAME);
     BufferedReader BR = new BufferedReader(new FileReader(FILE));
     n = 0;
     while ((LINE = BR.readLine()) != null) {
       n++;
       System.out.println("=============================================");
       System.out.println(n + ": " + LINE.trim());
       System.out.println("=============================================");
       ContadorFrecuencias(LINE);
       System.out.println("\t\tElemento\tFrecuencia");
       System.out.println("\t\t------------------------------");
       for(int k=0;k<=this.N-1;k++) {
              System.out.println("\t\t" + ITEM[k] + "\t\t" + FREQ[k]);
       }
       System.out.println("\t\t------------------------------");
       System.out.println();
     }
   }
   catch(IOException e) {
   }
}
//=============================================================
public void ContadorFrecuencias(String CAD) {
long L,F;
int P;
char E;
  Inicializar();
  L = CAD.length();
  for(int k=0;k<=L-1;k++) {
        E = CAD.charAt(k);
      if(!Exist(E)) {
           F = 0;
         for(int i=k;i<=L-1;i++) {
             if(CAD.charAt(i)==E) {
                  F++;
             }
         }
         N++;
         ITEM[N-1] = E;
         FREQ[N-1] = F;
      }
  }
}
//=============================================================
public void MostrarFrecuencias() {
       System.out.println("------------------------------");
       System.out.println("Elemento\tFrecuencia");
       System.out.println("------------------------------");
       for(int k=0;k<=this.N-1;k++) {
              System.out.println(ITEM[k] + "\t\t" + FREQ[k]);
       }
       System.out.println("------------------------------");
}
//=============================================================
//=============================================================
public static String Space(int nn) {
String SS;
   SS = "";
   for(int k=1;k<=nn;k++) {
       SS = SS + ' ';
   }
   return SS;
}
//=============================================================
// Convierte un archivo Secuencia .TXT a un archivo Random Access .DAT
public static void Convert_TXT_DAT(String FileIn, String FileOut, int T) {
FileReader FR;
BufferedReader BR;
FileWriter FW;
String LINE1,LINE2;
   try {
      FW = new FileWriter(FileOut);
      FR = new FileReader(FileIn);
      BR = new BufferedReader(FR);
      while((LINE1 = BR.readLine()) != null) {
          LINE2 = LINE1 + Space(T-LINE1.length());
          //System.out.println(LINE1);
          FW.write(LINE2);
      }
      FW.close();
      BR.close();
   }
   catch(IOException E) {
      System.out.println("Error");
   }

}
//=============================================================
// Carga los Departamentos en un vector usando Archivo Secuencial
public static void CargarRegiones() {
FileReader FR;
BufferedReader BR;
String CODE,NAME,LINEA;
   try {
      FR = new FileReader("UbiGeo.txt");
      BR = new BufferedReader(FR);
      while((LINEA = BR.readLine()) != null) {
          CODE = LINEA.substring(0,6);
          NAME = LINEA.substring(7,LINEA.length()).trim();
          if(CODE.substring(2,6).equals("0000")==true) {
             REGION[Integer.parseInt(CODE.substring(0,2))] = NAME;
          }
      }
      BR.close();
   }
   catch(IOException E) {
   }
}
//=============================================================
// Carga los Departamentos en un vector usando Archivo de Acceso Directo
public static void CargarRegiones_2() throws IOException, InterruptedException {
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
         for(int k=N-1;0<=k;k--) {
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
public static void Procesar_UbiGeo_CCPP(String CodDPTO) {
FileReader FR1,FR2,FR3,FR4;
BufferedReader BR1,BR2,BR3,BR4;
String LINEA,CodPROV,CodDIST;
FileWriter FW;
   try {
         FW = new FileWriter(REGION[Integer.parseInt(CodDPTO)] +".txt");
         FR1 = new FileReader("UbiGeo.txt");
         BR1 = new BufferedReader(FR1);
         while((LINEA = BR1.readLine()) != null) {
            if((LINEA.substring(0,2).equals(CodDPTO)==true)&&
               (LINEA.substring(2,6).equals("0000")==true)) {
                FW.write("====================================================\n");
                FW.write("Region: " + CodDPTO + " - " + LINEA.substring(7,LINEA.length()) + "\n");
                FW.write("====================================================\n");
                FR2 = new FileReader("UbiGeo.txt");
                BR2 = new BufferedReader(FR2);
                while((LINEA = BR2.readLine()) != null) {
                   if((LINEA.substring(0,2).equals(CodDPTO)==true)&&
                      (LINEA.substring(2,4).equals("00")==false)&&
                      (LINEA.substring(4,6).equals("00")==true)) {
                       CodPROV = LINEA.substring(2,4);
                       FW.write("     ===============================================\n");
                       FW.write("     Provincia: " + CodPROV + " - " + LINEA.substring(7,LINEA.length()) + "\n");
                       FW.write("     ===============================================\n");
                       FR3 = new FileReader("UbiGeo.txt");
                       BR3 = new BufferedReader(FR3);
                       while((LINEA = BR3.readLine()) != null) {
                          if((LINEA.substring(0,2).equals(CodDPTO)==true)&&
                             (LINEA.substring(2,4).equals(CodPROV)==true)&&
                             (LINEA.substring(4,6).equals("00")==false)) {
                              CodDIST = LINEA.substring(4,6);
                              FW.write("          ==========================================\n");
                              FW.write("          Distrito: " + CodDIST + " - " + LINEA.substring(7,LINEA.length()) + "\n");
                              FW.write("          ==========================================\n");
                              FR4 = new FileReader("CCPP.txt");
                              BR4 = new BufferedReader(FR4);
                              while((LINEA = BR4.readLine()) != null) {
                                 if(LINEA.substring(0,6).equals(CodDPTO+CodPROV+CodDIST)==true) {
                                    FW.write("               " + LINEA.substring(12,LINEA.length()) + "\n");
                                 }
                              }
                              BR4.close();
                          }//if
                       }//while
                       BR3.close();
                   }//if
                }//while
                BR2.close();
            }//if
         }//while
         BR1.close();
         FW.close();
   }
   catch(IOException E) {
   }
}
//=============================================================
} //class
