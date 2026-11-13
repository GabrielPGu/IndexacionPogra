#include <iostream>
#include <cstdlib>
#include <omp.h>

#define N  1000

using namespace std;

int T[N+1][N+1];
int A[N+1][N+1];

//---------------------------------------------
void ViewData() {
	for(int i=1;i<=N;i++) {
	    for(int j=1;j<=N;j++) {
	    	cout<<A[i][j]<<"\t";
	    }
	    cout<<"\n";
	}
	cout<<"\n";
}
//---------------------------------------------
void CopyData() {
	for(int i=1;i<=N;i++) {
	    for(int j=1;j<=N;j++) {
	    	A[i][j] = T[i][j];
	    }
	    cout<<"\n";
	}
}
//---------------------------------------------
void ProcessData() {
int TMP;
    //Procesamiento por Filas
    CopyData();
    #pragma omp parallel for collapse(3)
	for(int i=1;i<=N;i++) {
	    for(int k=1;k<=N-1;k++) {
            for(int j=1;j<=N-k;j++) {
            	if(A[i][j]>A[i][j+1]) {
            	   TMP = A[i][j];
            	   A[i][j] = A[i][j+1];
            	   A[i][j+1] = TMP;
            	   cout<<"Hilo: "<<omp_get_thread_num()<<"\n";
				}
	        }
	    }
	}
	//ViewData();

    //Procesamiento por Columnas
    CopyData();
    #pragma omp parallel for collapse(3)
	for(int j=1;j<=N;j++) {
	    for(int k=1;k<=N-1;k++) {
            for(int i=1;i<=N-k;i++) {
            	if(A[i][j]>A[i+1][j]) {
            	   TMP = A[i][j];
            	   A[i][j] = A[i+1][j];
            	   A[i+1][j] = TMP;
				}
	        }
	    }
	}
	//ViewData();

}
//---------------------------------------------
void LoadData() {
	#pragma omp parallel for collapse(2)
	for(int i=1;i<=N;i++) {
	    for(int j=1;j<=N;j++) {
	    	T[i][j] = 1000 + (rand()%23)*500;
	    }
	}
	CopyData();
	//ViewData();
}
//---------------------------------------------
//---------------------------------------------
int main(int argc, char** argv) {

    #pragma omp parallel
    cout<<"Total de Hilos: "<<omp_get_num_threads()<<"\n";

	LoadData();
	ProcessData();
	return 0;	
}
//---------------------------------------------

