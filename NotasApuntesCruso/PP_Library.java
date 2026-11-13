
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.io.BufferedWriter;
import java.io.BufferedReader;
import java.io.RandomAccessFile;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Scanner;


public class PP_Library {

//Global GLB = new Global();

public PP_Library() {
}

public String FormatoDecimal(double NUMERO, String FORMATO)  {
NumberFormat NF = new DecimalFormat(FORMATO);
   return NF.format(NUMERO);
}

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
                if((i==j)&&((M==N))) {
                   FW.write("<TD BGCOLOR=#FADBD8>");
                }
                else {
                   FW.write("<TD>");
                }
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
//==================================================
//Leer Datos desde el Disco
//==================================================
//==================================================
//Grabar Datos Numericos en el Disco en el 
//intervalo [A...B] en un espacio de tamaño E
//==================================================
public String Replicate(String S, int N) {
String T;
    T = "";
    for(int k=1;k<=N;k++) {
        T = T + S;
    }
    return T;
}
//==================================================
public String Space(int N) {
String T;
    T = "";
    for(int k=1;k<=N;k++) {
        T = T + ' ';
    }
    return T;
}
//==================================================
public int[][] CreateMatrix(int M, int N, int A, int B) {
int[][] MTX = new int[M][N];
double X;
    for(int i=0;i<=M-1;i++) {
        for(int j=0;j<=N-1;j++) {
            X = A + Math.random()*(B-A+1);
            MTX[i][j] = (int)X;
        }
    }
    return MTX;
}
//==================================================
//==================================================
//==================================================
//==================================================
public int[] GetData(String FILE_READ, int N)  {
int[] V = new int[N];
int k=0;
File FL = null;
FileReader     FR = null;
BufferedReader BR = null;
String LINE="";
  try {
    //FL = new File (FileName);
      FR = new FileReader(FILE_READ);
      BR = new BufferedReader(FR);
      k = -1;
      LINE = BR.readLine();
      while((LINE!=null)&&(k<N-1)) {
         k = k + 1;
       //if(k<=N-1) {
         V[k] = Integer.parseInt(LINE);
         LINE = BR.readLine();
       //}
       //else {            System.exit(0);         }
      }
      BR.close();
  }
  catch(Exception e) {
      e.printStackTrace();
      System.exit(0);
  }
  return V;
}
//==================================================
//==================================================
public int SumaElementosVector(int[] VCT)  {
int SUM;
    SUM = 0;
    for(int i=0;i<=VCT.length-1;i++) {
          SUM = SUM + VCT[i];
    }
    return SUM;
}
//==================================================

//---------------------------------------------------
public String ReadData(String FILE_READ)  {
File FL = null;
FileReader     FR = null;
BufferedReader BR = null;
String LINE="";
double SUMA,PROMEDIO,AUX;
int N;
  PROMEDIO = 0;
  try {
    //FL = new File (FileName);
      FR = new FileReader(FILE_READ);
      BR = new BufferedReader(FR);
      N = 0;
      SUMA = 0;
      LINE = BR.readLine();
      while(LINE!=null) {
         N = N + 1;
         AUX = Double.parseDouble(LINE);
         SUMA = SUMA + AUX;
         LINE = BR.readLine();
      }
      PROMEDIO = SUMA/N;
      BR.close();
  }
  catch(Exception e) {
      e.printStackTrace();
      System.exit(0);
  }
  return FormatoDecimal(PROMEDIO,"#0.00");
}
//---------------------------------------------------
//==================================================
public void WriteData(String FileName, int M, int N, int A, int B, int E, boolean Sw) {
String Linea,cad;
double X;
int L,num;
File FF;
    FF = new File(FileName);
    try {
        FileWriter FW = new FileWriter(FF);
        BufferedWriter BW = new BufferedWriter(FW);
        PrintWriter PW = new PrintWriter(BW);  
        for(int i=1;i<=M;i++) {
            Linea = "";
            for(int j=1;j<=N;j++) {
                X = A + Math.random()*(B-A+1);
                num = (int)X;
              //cad = "" + num + "";
                cad = String.valueOf(num);
                L = cad.length();
                if(E<L) {
                   Linea = Linea + num;
                }
                else {
                   Linea = Linea + Space(E-cad.length()) + num;
                }
            }
            PW.write(Linea + "\n");
        }
        PW.close();
        BW.close();
        if(Sw==true) {
           System.out.println("Se ha generado el archivo [" + FileName + "]");
        }
    }
    catch(IOException e) {
    }
}
//==================================================
//==================================================
//==================================================
//==================================================
//==================================================
/*public void LoadVector() {
double X;
int Num;
    for(int k=0; k<=GLB.T-1; k++) {
        X = Math.random()*(GLB.B-GLB.A+1);
        Num = GLB.A + (int)(X);
        GLB.V[k] = Num;
    }
}
//-----------------------------------------------
public void PrintVector() {
    for(int k=0; k<=GLB.T-1; k++) {
        System.out.print(GLB.V[k] + "\t");
    }
    System.out.println();
}
//-----------------------------------------------
public void PrintItemFrecuency() {
    System.out.println();
    System.out.println("------------------------");
    System.out.println("ITEM\tFRECUENCIA");
    System.out.println("------------------------");
    for(int k=0;k<=GLB.POS;k++) {
        System.out.println(GLB.ITEM[k] + "\t" + GLB.FREQ[k]);
    }
    System.out.println("------------------------");
}
//-----------------------------------------------
public void ComputeFrequency() {
int k,E,P;
    GLB.POS = -1;
    for(int i=0; i<=GLB.T-1; i++) {
        E = GLB.V[i];
        P = -1;
        k = 0;
        while((k<=GLB.T-1)&&(P==-1)) {
           if(GLB.ITEM[k]==E) {
              P = k;
           }
           k++;
        }
        if(P==-1) {
           GLB.POS++;
           GLB.ITEM[GLB.POS] = E;
           GLB.FREQ[GLB.POS] = 1;
        }
        else {
           GLB.FREQ[P]++;
        }        
    }
    System.out.println();
}*/
//-----------------------------------------------
}


/*
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.io.BufferedWriter;
import java.io.BufferedReader;
import java.io.RandomAccessFile;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Scanner;


public class PP_Library {

Global GLB = new Global();

public PP_Library() {
}

public String FormatoDecimal(double NUMERO, String FORMATO)  {
NumberFormat NF = new DecimalFormat(FORMATO);
   return NF.format(NUMERO);
}

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
                if((i==j)&&((M==N))) {
                   FW.write("<TD BGCOLOR=#FADBD8>");
                }
                else {
                   FW.write("<TD>");
                }
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


public void SummaryMatrix(double MTX[][], String TITLE, String FILENAME) {
int M = MTX.length;
int N = MTX[0].length;
double PROMEDIO=0,X,SUMA;
    try {
        FileWriter FW = new FileWriter(FILENAME);
        FW.write("<H1>" + TITLE + "</H1>");
        FW.write("<HTML>");
        FW.write("<HEAD>");
        FW.write("</HEAD>");
        FW.write("<BODY BGCOLOR=#FFFFFF>");
        FW.write("<TABLE BORDER=0 CELLPADDING=7  CELLSPACING=1>"); //#A3E4D7

        //================================================
        FW.write("<TR>");
        FW.write("<TD BORDER=0 BGCOLOR=#FFFFFF></TD>");
        for(int j=1;j<=N;j++) {
            FW.write("<TD BGCOLOR=#500A50>" + j + "</TD>");
        }
        FW.write("<TD BORDER=0 BGCOLOR=#FFFFFF></TD>");        
        FW.write("</TR>");
        //================================================
        for(int i=1;i<=M;i++) {
            FW.write("<TR>");
            FW.write("<TD BGCOLOR=#500A50>" + i + "</TD>");
            SUMA = 0;
            for(int j=1;j<=N;j++) {
                X = MTX[i-1][j-1];
                SUMA = SUMA + X;
                if((i==j)&&((M==N))) {
                   FW.write("<TD BGCOLOR=#FADBD8>");
                }
                else {
                   FW.write("<TD BGCOLOR=#AED6F1>");
                }
                FW.write(X + "</TD>");
            }
            PROMEDIO = SUMA/N;
            FW.write("<TD BGCOLOR=#0ADBD8>" + FormatoDecimal(PROMEDIO,"#0.00") + "</TD>");
            FW.write("</TR>");
        }
        //================================================
        //================================================
        FW.write("<TR>");
        FW.write("<TD BORDER=0 BGCOLOR=#FFFFFF></TD>");
        for(int j=1;j<=N;j++) {
            SUMA = 0;
            for(int i=1;i<=M;i++) {
                X = MTX[i-1][j-1];
                SUMA = SUMA + X;
            }
            PROMEDIO = SUMA/M;
            FW.write("<TD BGCOLOR=#0ADBD8>" + FormatoDecimal(PROMEDIO,"#0.00") + "</TD>");
        }
        FW.write("<TD BORDER=0 BGCOLOR=#FFFFFF></TD>");
        FW.write("</TR>");
        //================================================
        //================================================

        FW.write("</TABLE>");
        FW.write("</BODY>");
        FW.write("</HTML>");

        FW.close();
    }
    catch (IOException E) {
        System.out.print(E.getMessage());
    }
}
//==================================================
//Leer Datos desde el Disco
//==================================================
//==================================================
//Grabar Datos Numericos en el Disco en el 
//intervalo [A...B] en un espacio de tamaño E
//==================================================
public String Replicate(char C, int N) {
String T;
    T = "";
    for(int k=1;k<=N;k++) {
        T = T + C;
    }
    return T;
}
//==================================================
public String Replicate(String S, int N) {
String T;
    T = "";
    for(int k=1;k<=N;k++) {
        T = T + S;
    }
    return T;
}
//==================================================
public String Space(int N) {
String T;
    T = "";
    for(int k=1;k<=N;k++) {
        T = T + ' ';
    }
    return T;
}
//==================================================
public int[][] CreateMatrix(int M, int N, int A, int B) {
int[][] MTX = new int[M][N];
double X;
    for(int i=0;i<=M-1;i++) {
        for(int j=0;j<=N-1;j++) {
            X = A + Math.random()*(B-A+1);
            MTX[i][j] = (int)X;
        }
    }
    return MTX;
}
//==================================================
//==================================================
//==================================================
//==================================================
//---------------------------------------------------
public String ReadData(String FILE_READ)  {
File FL = null;
FileReader     FR = null;
BufferedReader BR = null;
String LINE="";
double SUMA,PROMEDIO,AUX;
int N;
  PROMEDIO = 0;
  try {
    //FL = new File (FileName);
      FR = new FileReader(FILE_READ);
      BR = new BufferedReader(FR);
      N = 0;
      SUMA = 0;
      LINE = BR.readLine();
      while(LINE!=null) {
         N = N + 1;
         AUX = Double.parseDouble(LINE);
         SUMA = SUMA + AUX;
         LINE = BR.readLine();
      }
      PROMEDIO = SUMA/N;
      BR.close();
  }
  catch(Exception e) {
      e.printStackTrace();
      System.exit(0);
  }
  return FormatoDecimal(PROMEDIO,"#0.00");
}
//---------------------------------------------------
//==================================================
public void WriteData(String FileName, int M, int N, double A, double B, int PD, int E, boolean Sw) {
String Linea,cad;
double X;
int L;
File FF;
    FF = new File(FileName);
    try {
        FileWriter FW = new FileWriter(FF);
        BufferedWriter BW = new BufferedWriter(FW);
        PrintWriter PW = new PrintWriter(BW);  
        for(int i=1;i<=M;i++) {
            Linea = "";
            for(int j=1;j<=N;j++) {
                X = A + Math.random()*(B-A+1);
                cad = FormatoDecimal(X,"#0." + Replicate('0',PD));

                String tmp = ""; 
                for(int k=0;k<=cad.length()-1;k++) {
                    if(cad.charAt(k)==',') {
                       tmp = tmp + ".";
                    }
                    else {
                       tmp = tmp + cad.charAt(k);
                    }
                }
                cad = tmp;



                L = cad.length();
                if(E<L) {
                   Linea = Linea + cad;
                }
                else {
                   Linea = Linea + Space(E-cad.length()) + cad;
                }
            }
            PW.write(Linea + "\n");
        }
        PW.close();
        BW.close();
        if(Sw==true) {
           System.out.println("Se ha generado el archivo [" + FileName + "]");
        }
    }
    catch(IOException e) {
    }
}

public double[][] CreateMatrixFromDisk(String DataFile, int M, int N, int E, String H) {
int i;
String LINE ,SS;
double MTX[][] = new double [M][N];
    if(((H.length()==0)&&(E>0))||((H.length()==1)&&(E==0))) {
        i=-1;
        try (FileReader FR = new FileReader(DataFile);
             BufferedReader BR = new BufferedReader(FR)) {
            while ((LINE = BR.readLine()) != null) {
                i++; 
                for(int j=0 ;j<=N-1;j++) {
                    SS = LINE.substring(E*j,E*j+E).trim();
                    MTX[i][j] = Double.parseDouble(SS);
                }
            }
        }
        catch (IOException e) {
            System.out.println("Error al leer el archivo: " + e.getMessage());
        }
    }
    return MTX;
}



//==================================================
public void WriteData(String FileName, int M, int N, int A, int B, int E, boolean Sw) {
String Linea,cad;
double X;
int L,num;
File FF;
    FF = new File(FileName);
    try {
        FileWriter FW = new FileWriter(FF);
        BufferedWriter BW = new BufferedWriter(FW);
        PrintWriter PW = new PrintWriter(BW);  
        for(int i=1;i<=M;i++) {
            Linea = "";
            for(int j=1;j<=N;j++) {
                X = A + Math.random()*(B-A+1);
                num = (int)X;
              //cad = "" + num + "";
                cad = String.valueOf(num);
                L = cad.length();
                if(E<L) {
                   Linea = Linea + num;
                }
                else {
                   Linea = Linea + Space(E-cad.length()) + num;
                }
            }
            PW.write(Linea + "\n");
        }
        PW.close();
        BW.close();
        if(Sw==true) {
           System.out.println("Se ha generado el archivo [" + FileName + "]");
        }
    }
    catch(IOException e) {
    }
}
//==================================================
//==================================================
//==================================================
//==================================================
public void LoadVector() {
double X;
int Num;
    for(int k=0; k<=GLB.T-1; k++) {
        X = Math.random()*(GLB.B-GLB.A+1);
        Num = GLB.A + (int)(X);
        GLB.V[k] = Num;
    }
}
//-----------------------------------------------
public void PrintVector() {
    for(int k=0; k<=GLB.T-1; k++) {
        System.out.print(GLB.V[k] + "\t");
    }
    System.out.println();
}
//-----------------------------------------------
public void PrintItemFrecuency() {
    System.out.println();
    System.out.println("------------------------");
    System.out.println("ITEM\tFRECUENCIA");
    System.out.println("------------------------");
    for(int k=0;k<=GLB.POS;k++) {
        System.out.println(GLB.ITEM[k] + "\t" + GLB.FREQ[k]);
    }
    System.out.println("------------------------");
}
//-----------------------------------------------
public void ComputeFrequency() {
int k,E,P;
    GLB.POS = -1;
    for(int i=0; i<=GLB.T-1; i++) {
        E = GLB.V[i];
        P = -1;
        k = 0;
        while((k<=GLB.T-1)&&(P==-1)) {
           if(GLB.ITEM[k]==E) {
              P = k;
           }
           k++;
        }
        if(P==-1) {
           GLB.POS++;
           GLB.ITEM[GLB.POS] = E;
           GLB.FREQ[GLB.POS] = 1;
        }
        else {
           GLB.FREQ[P]++;
        }        
    }
    System.out.println();
}
//-----------------------------------------------
}

*/