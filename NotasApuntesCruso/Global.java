public class Global {

public final int T = 100;
public final int A = 18;
public final int B = 25;
public int V[]		= new int[T];
public int ITEM[]	= new int[T];
public int FREQ[]	= new int[T];
public int POS;

public Global() {
	for(int k = 0; k <= T-1; k++) {
		V[k] = 0;
		ITEM[k] = 0;
		FREQ[k] =  0;
		POS = -1;
	}
}


}
