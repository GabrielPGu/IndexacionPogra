public class Demo_PCP1 {

private static final int n = 3; //Por ahora en X,Y,Z arrays en memoria
private static final int N = 1000;
private static final int T = N*(N-1)/2;
private static final int A = 10000;  //Limite Inferior del Rango de Valores
private static final int B = 99999;  //Limite Superior del Rango de Valores


private static int[] X = new int[N+1];
private static int[] Y = new int[N+1];
private static int[] Z = new int[N+1];

private static double[][] D = new double[T+1][3+1];
private static boolean Sw1,Sw2;
//============================================================
public static void ViewData() {
	for(int i=1;i<=N;i++) {
		System.out.println("(" + X[i] + "," + Y[i] + "," + Z[i] + ")");
	}
}
//============================================================
public static void ViewDistance() {
	for(int i=1;i<=T;i++) {
		System.out.println(D[i][1] + " - " + D[i][2] + " - " + D[i][3]);
	}
}
//============================================================
public static void DistanciaMinima() {
int P;
    P = 1;
	for(int k=1;k<=T;k++) {
		if(D[k][2]<D[P][2]) {
		   P = k;
		}
	}
	System.out.println("\tDistancia Minima: Punto[" + (int)(D[P][1]) + "] a Punto [" + (int)(D[P][3]) + "] = " + D[P][2] );
}
//============================================================
public static void DistanciaMaxima() {
int P;
    P = 1;
	for(int k=1;k<=T;k++) {
		if(D[P][2]<D[k][2]) {
		   P = k;
		}
	}
	System.out.println("\tDistancia Maxima: Punto[" + (int)(D[P][1]) + "] a Punto [" + (int)(D[P][3]) + "] = " + D[P][2] );
}
//============================================================
public static void ComputeDistance() {
int k;
	k = 0;
	for(int i=1;i<=N-1;i++) {
	    for(int j=i+1;j<=N;j++) {
	    	//System.out.println("(" + i + "," + j + ")");
	    	//Calcular Distancia de i-esimo punto al j-esimo punto
	    	//System.out.println("(" + i + "," + j + ")");
	    	k = k + 1;
	    	D[k][1] = i;
	    	D[k][2] = Math.sqrt(Math.pow(X[i]-X[j],2) + Math.pow(Y[i]-Y[j],2) + Math.pow(Z[i]-Z[j],2));
	    	D[k][3] = j;
	    }
	}
}
//============================================================
public static int[] LoadData(int A, int B) {
int[] V = new int[N+1];
	for(int i=1;i<=N;i++) {
		V[i] = (int)(Math.random()*(B-A+1));
	}
	return V; 
}
//============================================================
public static void Procesamiento_Serial() {
	System.out.println("PROCESAMIENTO SERIAL");
	DistanciaMinima();
	DistanciaMaxima();
}
//============================================================
public static void Procesamiento_Paralelo() {
    System.out.println("PROCESAMIENTO PARALELO");
    //-------------------------------------------------
    Sw1 = false;
    Sw2 = false;
    new Thread(new Runnable() {
        public void run() {
	       DistanciaMinima();
		   Sw1 = true;
        }
    }).start();
    //-------------------------------------------------
    new Thread(new Runnable() {
        public void run() {
	       DistanciaMaxima();
		   Sw2 = true;
        }
    }).start();
    //-------------------------------------------------
    while(!((Sw1==true) && (Sw2==true))) {
    	//Espera hasta que terminen los hilos
    	//Hasta que todos los flags sean TRUE
    }
}
//============================================================
public static void main(String[] args) {
  System.out.println("------------------------------------------------------");
  System.out.println("Procesamiento de " + N + "Puntos " + n + "-Dimensionales");
  System.out.println("------------------------------------------------------");
  X = LoadData(A,B);
  Y = LoadData(A,B);
  Z = LoadData(A,B);
//ViewData();
  ComputeDistance();
//ViewDistance();
  Procesamiento_Serial();
  Procesamiento_Paralelo();
}
//============================================================
} //class
