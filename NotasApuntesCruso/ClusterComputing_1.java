import java.io.IOException;
import java.io.FileReader;
import java.io.BufferedReader;
import java.net.InetAddress;
import java.net.UnknownHostException;

public class ClusterComputing_1 {

//---------------------------------------
public static String GetHostName() {
String HostName="";
 try {
     InetAddress IA = InetAddress.getLocalHost();
     HostName = IA.getHostName();
 }
 catch(UnknownHostException E) {
     E.printStackTrace();
 }
 return HostName;
}
//--------------------------------------------------------
public static String GetAddressIP() {
String AddressIP="";
 try {
     InetAddress IA = InetAddress.getLocalHost();
     AddressIP = IA.getHostAddress();
 }
 catch(UnknownHostException E) {
     E.printStackTrace();
 }
 return AddressIP;
}

//---------------------------------------
public static void ProcesarConfigFile(String IP) {
FileReader FR;
BufferedReader BR;
String LINEA,CodDPTO,CodPROV,CodDIST;
   try {
   	  FR = new FileReader("Configuracion.txt");
   	  BR = new BufferedReader(FR);
      while((LINEA = BR.readLine()) != null) {
      	  if(LINEA.substring(10,25).trim().equals(IP)==true) {
             String V[] = LINEA.substring(28,LINEA.length()).trim().split(";");
//System.out.println("." + LINEA + ".");
//System.out.println(V.length);
             for(int k=0;k<=V.length-1;k++) {
      	  	     //System.out.println(V[k]);
                 ProcesarData(V[k]);
             }
      	  }
      }
   	  BR.close();
   }
   catch(IOException E) {
   }
}
//---------------------------------------
public static void ProcesarData(String CodDPTO) {
FileReader FR1;
FileReader FR2 = null;
FileReader FR3;
FileReader FR4;
BufferedReader BR1;
BufferedReader BR2;
BufferedReader BR3;
BufferedReader BR4;
String LINEA,CodPROV,CodDIST;
   try {
   	  FR1 = new FileReader("UbiGeo.txt");
   	  BR1 = new BufferedReader(FR1);
      while((LINEA = BR1.readLine()) != null) {
      	  if((LINEA.substring(0,2).equals(CodDPTO)==true)&&
      	  	 (LINEA.substring(2,6).equals("0000")==true)) {
      	     System.out.println(CodDPTO + " - " + LINEA.substring(7,LINEA.length()));
      	     System.out.println("======================================");
   	         FR2 = new FileReader("UbiGeo.txt");
   	         BR2 = new BufferedReader(FR2);
		     while((LINEA = BR2.readLine()) != null) {
				if((LINEA.substring(0,2).equals(CodDPTO)==true)&&
				   (LINEA.substring(2,4).equals("0000")==false)&&
				   (LINEA.substring(4,6).equals("00")==true)) {
      	  	       CodPROV = LINEA.substring(2,4);
      	           System.out.println("     " + CodPROV + " - " + LINEA.substring(7,LINEA.length()));
      	           System.out.println("     --------------------------------------");
 	   	           FR3 = new FileReader("\\\\192.168.124.60\\data\\UbiGeo.txt");
	   	           BR3 = new BufferedReader(FR3);
			       while((LINEA = BR3.readLine()) != null) {
					  if((LINEA.substring(0,2).equals(CodDPTO)==true)&&
					     (LINEA.substring(2,4).equals(CodPROV)==true)&&
					     (LINEA.substring(4,6).equals("00")==false)) {
	      	  	         CodDIST = LINEA.substring(4,6);
	      	             System.out.println("          " + CodDIST + " - " + LINEA.substring(7,LINEA.length()));
	      	             System.out.println("          --------------------------------------");
						 System.out.println("Salida");

		 	   	         FR4 = new FileReader("\\\\192.168.124.60\\data\\CCPP.txt");
			   	         BR4 = new BufferedReader(FR4);
					     while((LINEA = BR4.readLine()) != null) {
						    if(LINEA.substring(0,6).equals(CodDPTO+CodPROV+CodDIST)==true) {
			      	             System.out.println("               " + LINEA.substring(12,LINEA.length()));
						    }
					     }
			   	         BR4.close();

					  }//
			       }//
	   	           BR3.close();

				}//
		     }//Dpto
   	         BR2.close();
      	  }
      }
   	  BR1.close();
   	  //BR4.close();
   }
   catch(IOException E) {
   }
}
//---------------------------------------
public static void main(String[] args) {
String IP=GetAddressIP();
    System.out.println("- Hostname  : " + GetHostName());
    System.out.println("- IP Address: " + GetAddressIP());
    ProcesarConfigFile(IP);
}
//---------------------------------------

}