import java.io.*;
import java.net.*;
import java.util.*;

public class SenderListener extends Thread{
	private final static int mss=576;
	//define input and output
	private DataInputStream socketInput;
	private PrintWriter output;
	
	//define buffer to store received ack
	private byte[] ackbuf;
	//initialize seq,ack on the sender's side
	private int seq=0;
	private int ack=0;
	//initialize seq,ack on the receiver's side
	
	
	//private int fin;
	//private int isack;
	
	//constructor
	public SenderListener(DataInputStream socketInput, PrintWriter output){
		this.socketInput=socketInput;
		this.output=output;
	}
	public void run(){
		//initialize the readin packet in byte
		byte[] pkt=new byte[20];
		//get the received packet
		try {
			while(true){
				socketInput.read(pkt);
				int sport=0;
				int rport=0;
				if(pkt[0]!=0){
					Date date=new Date();
					int rseq=0;
					int rack=0;
					for(int i=0;i<4;i++){
						int shift=(3-i)*8;
						rseq += (pkt[4+i] & 0x000000ff)<<shift;
						rack += (pkt[8+i] & 0x000000ff)<<shift;
					}
					
					for(int i=0;i<2;i++){
						int shift=(1-i)*8;
						sport += (pkt[i] & 0x00ff)<<shift;
						rport += (pkt[i+2] & 0x00ff)<<shift;
					}
					if(pkt[13]==1) {
						//fin=1;
						output.println(date+", "+  rport  +", "  +sport+", " +rseq+", "+rack*mss+", "+"fin");
						Sender.printFinal();
						Sender.setAnoAckno(rack);
						break;
						
					}
					if(pkt[13]==16) {
						//isack=1;
						output.println(date+", "+ rport + ", "+sport+", "+rseq+", "+rack*mss+", "+"ack");
						Sender.setEstirtt(date,rack-1);
						Sender.setAnoAckno(rack);
						Sender.setCountpktinwindow(rack);
						
					}
					
				}
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
}
