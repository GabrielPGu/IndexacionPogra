import java.io.IOException;
import java.io.RandomAccessFile;

public class Demo_RAF2 {
private static int W = 57;
private static byte[] BUFFER = new byte[W];

  //===============================
  public static byte[] StringToBuffer(String CADENA, int W) {
  byte[] BUFFER = new byte[W];
  int L = CADENA.length();
      for(int i=0;i<=L-1;i++) {
         BUFFER[i] = (byte)CADENA.charAt(i);
      }
      return BUFFER;
  }
  //===============================
  public static String BufferToString(byte[] BUFFER) {
  String CADENA="";
  int L = BUFFER.length;
      for(int i=0;i<=L-1;i++) {
       CADENA = CADENA + (char)BUFFER[i];
      }
      return CADENA;
  }
  //===============================
  //===============================
  //===============================
  //===============================
  public static void List_UbiGeo() throws IOException, InterruptedException {
  RandomAccessFile RAF1 = new RandomAccessFile("UbiGeo.DAT","r");
  RandomAccessFile RAF2 = new RandomAccessFile("UbiGeo.DAT","r");
  long N,T;
  int L,n;
  String DATO,CodDpto,NomDpto;
  String      CodProv,NomProv;
  String      CodDist,NomDist;
     T = RAF1.length(); 
     N = T/W;
     n=2;
     for(int i=1;i<=N;i++) {
         RAF1.seek((i-1)*W);
         RAF1.read(BUFFER);
         DATO = BufferToString(BUFFER).trim();
         L = DATO.length();
         CodDpto = DATO.substring(0,6);
         NomDpto = DATO.substring(7,L);
         //System.out.println(DATO.trim());
     }

     for(int j=0;j<=n-2;j++){
        for(int k=j+1;k<=n-1;k++){
            for(int i=0;i<=N-1;i++){
                byte[] bufferA =new byte[W];
                RAF1.seek(i*W*n+j*W);
                RAF1.read(bufferA);
                String A=BufferToString(bufferA).trim();
                byte[] bufferB =new byte[W];
                RAF1.seek(i*W*n+k*W);
                RAF1.read(bufferB);
                String B = BufferToString(bufferB).trim();
                System.out.printf("\tRegistro A (pos %d): %s\n", j, A);
                System.out.printf("\tRegistro B (pos %d): %s\n\n", k, B);
            }
        }
     }

     RAF1.close();
  }
  //===============================
  public static void Process_RAF() throws IOException, InterruptedException {
  long N,T;
  String DATO;
     RandomAccessFile RAF = new RandomAccessFile("UbiGeo.DAT","r");
     T = RAF.length(); 
     N = T/W;
   //for(int k=1;k<=N;k++) {
     for(long k=N;1<=k;k--) {
     	RAF.seek((k-1)*W); //Direccionamiento del registro
      RAF.read(BUFFER);
      DATO = BufferToString(BUFFER);
     	System.out.println(DATO.trim());
     }
     RAF.close();
  }
  //===============================
  public static void main(String[] args) throws IOException, InterruptedException {
   //Process_RAF();
     List_UbiGeo();
  }
  //===============================
}




