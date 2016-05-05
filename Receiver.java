import java.io.*;
import java.net.*;
import java.util.*;

public class Receiver{
	private static int mss=576;
	//define receiver's command line parameters
	private String filename;
	private int port;
	private InetAddress sender_ip;
	private int sender_port;
	private String logname;
	//define UDP socket
	private DatagramSocket sock=null;
	private DatagramPacket datapkt;
	private DatagramPacket ackpkt;
	//define TCP socket
	private Socket tcpSock;
	//private BufferedReader socketInput;
	//define output
    private PrintWriter output;//for log
    private DataOutputStream outfile;//for file
    private DataOutputStream socketOut;//for TCP ack
	private boolean allReceived;
	private Date date;
	//private byte[] header;
	private int seqn=40;
	private int ackn=0;
	
	private HashSet<Integer> receivedSeq;
	
	
	//constructor for receiver
	private Receiver(String filename,int port,InetAddress sender_ip, int sender_port,String logname) throws IOException{
		this.filename=filename;
		this.port=port;
		this.sender_ip=sender_ip;
		this.sender_port=sender_port;
		this.logname=logname;
	}
	
	public void execute() throws IOException{
		//set the output to file
		File file=new File(filename);
		FileOutputStream fof=new FileOutputStream(file);
		outfile=new DataOutputStream(fof);
		//set output stdout or write into log file
		if(logname.contentEquals("stdout")){
			output=new PrintWriter(System.out, true);
		}else{
			File log=new File(logname);
			FileOutputStream fos=new FileOutputStream(log);
			output=new PrintWriter(fos,true);
		}
		
		//initialize the datagram socket for received packet
		try{
			sock=new DatagramSocket(port);
		} catch (SocketException e) {
			e.printStackTrace();
		}
		
		//initialize the tcp socket
		byte[] start=new byte[2];
		DatagramPacket startPkt=new DatagramPacket(start,start.length);
		sock.receive(startPkt);
		byte[] readStart=startPkt.getData();
		if(readStart.length==2){
			tcpSock=new Socket(sender_ip,sender_port);
			socketOut=new DataOutputStream(tcpSock.getOutputStream());
		}
		seqn=40;
		while(!allReceived){
			//initialize the readin packet in byte
			byte[] pkt=new byte[mss+20];
			//get the received packet
			datapkt=new DatagramPacket(pkt,pkt.length);
			sock.receive(datapkt);
			byte[] readpkt=datapkt.getData();
			byte[] header=new byte[20];
			for(int i=0;i<20;i++){
				header[i]=readpkt[i];
			}
			int seq_no=0;
			int ack_no=0;
			int shift = 8;
			for(int i=0;i<4;i++){
				seq_no = seq_no<<shift;
				seq_no += (header[i+4] & 0xff);
				ack_no = ack_no<<shift;
				ack_no += (header[i+8] & 0xff);
			}
			short srcport=0;
			short destport=0;
			short window=0;
			short checksum=0;
			for(int i=0;i<2;i++){
				srcport = (short) (srcport<<shift);
				srcport += (header[i] & 0xff);
				destport = (short)(destport<<shift);
				destport += (header[i+2] & 0xff);
				window = (short)(window<<shift);
				window += (header[i+14] & 0xff);
				checksum = (short)(checksum<<shift);
				checksum += (header[(i+16)] & 0xff);
			}
			int fin=header[13];
			byte[] body;
			if(fin==1) {
				//getbody
				int j=readpkt.length-1;
				for(;readpkt[j]==0;) j--;
				int bodyLen=j+1-20;
				body=new byte[bodyLen];
				for(int i=0;i<bodyLen;i++){
					body[i]=readpkt[i+20];
				}
			}else {
				body=new byte[readpkt.length-20];
				for(int i=0;i<body.length;i++){
					body[i]=readpkt[i+20];
				}
			}
			//check checksum
			String flags="ack";
			if(fin==1) flags="fin";
			int receivedCheck=getCheckSum(header,body);
			date=new Date();
			if(fin==1) output.println(date+","+sender_port+","+port+","+seq_no*mss+","+ack_no+","+"fin");
			else output.println(date+","+sender_port+","+port+","+seq_no*mss+","+ack_no+","+"ack");
			
			//check if there are packets that has not been received with seq_no lower than this one
			
			if(ackn==seq_no&&receivedCheck==checksum){
				if(fin==1) allReceived=true;
				ackn=seq_no+1;
				outfile.write(body, 0, body.length);
				socketOut.write(getAck(ack_no,ackn,fin));
				date=new Date();
				output.println(date+", "+port+","+sender_port+","+seqn+", "+ackn*mss+", "+flags);
				seqn+=20;
				
			}
		}
	}
	public int getCheckSum(byte[] header,byte[] body){
//		System.out.println(Arrays.toString(header));
//		System.out.println(Arrays.toString(body));
//		System.out.println(header[16]);
//		System.out.println(header[17]);
		short sum=0;
		header[16]=0;
		header[17]=0;
		for(byte headpart : header){
			sum += (short) headpart;
		}
		for(byte bodypart:body){
			sum += (short) bodypart;
		}
		return ~sum;
	}
	public byte[] getAck(int seqn,int ackn,int fin){
		byte[] ack=new byte[20];
		//set ports in ack
		short sport=(short) sender_port;
		short rport=(short) port;
		ack[0]=(byte)((sport & 0xff00)>>8);
		ack[1]=(byte)(sport & 0x00ff);
		ack[2]=(byte)((rport & 0xff00)>>8);
		ack[3]=(byte)(rport & 0x00ff);
		//set seqno in ack
		ack[4]=(byte)((seqn & 0xff000000)>>24);
		ack[5]=(byte)((seqn & 0x00ff0000)>>16);
		ack[6]=(byte)((seqn & 0x0000ff00)>>8);
		ack[7]=(byte)(seqn & 0x000000ff);
		//set ackno in ack
		ack[8]=(byte)((ackn & 0xff000000)>>24);
		ack[9]=(byte)((ackn & 0x00ff0000)>>16);
		ack[10]=(byte)((ackn & 0x0000ff00)>>8);
		ack[11]=(byte)(ackn & 0x000000ff);
		//set ack bit
		if(fin!=1) ack[13]=(byte)16;
		else {
			ack[13]=(byte)1;
			System.out.println("Delivery completed successfully");
		}
		
		return ack;
	}
	public static void main(String[] args) throws IOException{
		int port=Integer.parseInt(args[1]);
		String filename=args[0];
		InetAddress sender_ip= InetAddress.getByName(args[2]);
		int sender_port=Integer.parseInt(args[3]);
		String logname=args[4];
		Receiver rec=new Receiver(filename,port,sender_ip, sender_port, logname);
		rec.execute();
	}
}
