import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
//====================================================================
public class SynchronizedQueue {
    //----------------------------------------------------------------
    public static void main(String[] args) {
    PP_Library LIB = new PP_Library();


        BlockingQueue<Integer> REQUEST  = new LinkedBlockingQueue<>();
        BlockingQueue<Result>  RESPONSE = new LinkedBlockingQueue<>();
        Operation OBJ = new Operation(REQUEST,RESPONSE);

      /*LIB.WriteData("VECTOR10000.TXT",1000000,1,-100000,999999,0,true);
        LIB.WriteData("VECTOR20000.TXT",1000000,1,-100000,999999,0,true);
        LIB.WriteData("VECTOR30000.TXT",1000000,1,-100000,999999,0,true);
        LIB.WriteData("VECTOR40000.TXT",1000000,1,-100000,999999,0,true);
        LIB.WriteData("VECTOR50000.TXT",1000000,1,-100000,999999,0,true);
        LIB.WriteData("VECTOR60000.TXT",1000000,1,-100000,999999,0,true);
        LIB.WriteData("VECTOR70000.TXT",1000000,1,-100000,999999,0,true);
        LIB.WriteData("VECTOR80000.TXT",1000000,1,-100000,999999,0,true);
        LIB.WriteData("VECTOR90000.TXT",1000000,1,-100000,999999,0,true);
        System.exit(0);*/

        OBJ.start();
        try {

        int[] U; 


            //SERIAL
            U = LIB.GetData("VECTOR50000.TXT",50000); System.out.println("Suma de Elementos del vector V[#" + 50000 + "]: " + LIB.SumaElementosVector(U) + "");
            U = LIB.GetData("VECTOR40000.TXT",40000); System.out.println("Suma de Elementos del vector V[#" + 40000 + "]: " + LIB.SumaElementosVector(U) + "");
            U = LIB.GetData("VECTOR30000.TXT",30000); System.out.println("Suma de Elementos del vector V[#" + 30000 + "]: " + LIB.SumaElementosVector(U) + "");
            U = LIB.GetData("VECTOR20000.TXT",20000); System.out.println("Suma de Elementos del vector V[#" + 20000 + "]: " + LIB.SumaElementosVector(U) + "");
            U = LIB.GetData("VECTOR10000.TXT",10000); System.out.println("Suma de Elementos del vector V[#" + 10000 + "]: " + LIB.SumaElementosVector(U) + "");

            //PARALEO EXPLICITO

            //PARALELO EN COLA SINCRONIZADA
            REQUEST.put(50000);
            REQUEST.put(40000);
            REQUEST.put(30000);
            REQUEST.put(20000);
            REQUEST.put(10000);
            System.out.println(RESPONSE.take());
            System.out.println(RESPONSE.take());
            System.out.println(RESPONSE.take());
            System.out.println(RESPONSE.take());
            System.out.println(RESPONSE.take());
        }
        catch (InterruptedException ERROR) {
            ERROR.printStackTrace();
            System.out.println(ERROR);
        }
        System.exit(0);
    }
}
//====================================================================
//====================================================================
//====================================================================
class Operation {
//private static int N=1000; 
private final BlockingQueue<Integer> IN;
private final BlockingQueue<Result>  OUT;
private int[] U;
private int[] V;
    //----------------------------------------------------------------
    public Operation(BlockingQueue<Integer> REQUEST, BlockingQueue<Result> RESPONSE) {
        this.IN  = REQUEST;
        this.OUT = RESPONSE;
    }
    //----------------------------------------------------------------
    //----------------------------------------------------------------
    public long Proceso(int N) {
        LoadFromDisk(N);  //Load(N);
        Sort(N);
      //View(N);
        return Suma(N);
    }
    //----------------------------------------------------------------
    public long Suma(int N) {
    long S=0;
        for(int i=0;i<=N-1;i++) {
           S = S + V[i];
        }
        return S;
    }
    //----------------------------------------------------------------
    public void LoadFromDisk(int N) {
    PP_Library LIB = new PP_Library();
        V = LIB.GetData("VECTOR" + N + ".TXT",N);
    }
    //----------------------------------------------------------------
    public void Load(int N) {
    double X;
        V = new int[N];
        for(int i=0;i<=N-1;i++) {
           X = 10000 + Math.random()*90000;
           V[i] = (int)X;
        }
    }
    //----------------------------------------------------------------
    public void Sort(int N) {
    double X;
    int aux;
        for(int k=1;k<=N-1;k++) {
            for(int i=0;i<=N-k-1;i++) {
                if(V[i]>V[i+1]) {
                   aux    = V[i];
                   V[i]   = V[i+1];
                   V[i+1] = aux;
                }
            }
        }
    }
    //----------------------------------------------------------------
    public void View(int N) {
        //System.out.println(TITLE);
        //System.out.println("------------------------");
        System.out.println(N + ": ");
        for(int i=0;i<=N-1;i++) {
           System.out.print(V[i] + "\t");
        }
    }
    //----------------------------------------------------------------
    //----------------------------------------------------------------
    private long Divisores(int N) {
    long S;
        S = 0;
        for(int i=1; i<=N; i++) {
            if(N%i==0) {
                S = S + i;
                System.out.println(N + ") - " + i);
            }
        }
        return S;
    }
    //----------------------------------------------------------------
    public void start() {
        new Thread(new Runnable() {
            public void run() {
            int n;
            long T;
                while (true) {
                    try {
                        n = IN.take();
                        T = Proceso(n);  // Divisores(n);
                        OUT.put(new Result(n,T));
                    }
                    catch (InterruptedException ERROR) {
                        ERROR.printStackTrace();
                    }
                }
            }
        }).start();
    }
}
//====================================================================
//====================================================================
//====================================================================
class Result {
    private final int IN;
    private final long OUT;
    //----------------------------------------------------------------
    public Result(int A, long B) {
        this.IN  = A;
        this.OUT = B;
    }
    //----------------------------------------------------------------
    @Override public String toString() {
        return "Suma de Elementos del vector V[#" + IN + "]: " + OUT;
      //return "Suma de Divisores de " + IN + ": " + OUT;
    }
}
//====================================================================
//====================================================================
//====================================================================
