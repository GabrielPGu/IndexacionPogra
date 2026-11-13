import java.io.IOException;
import java.io.FileReader;
import java.io.BufferedReader;
import java.io.FileWriter;

public class Create_RAF {

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
public static void main(String args[]) throws IOException, InterruptedException {
    Convert_TXT_DAT("UbiGeo.txt","UbiGeo.dat",50);
    Convert_TXT_DAT("CCPP.txt","CCPP.dat",60);
}
//=============================================================
}
