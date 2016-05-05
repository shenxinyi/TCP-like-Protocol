import java.io.*;
import java.net.*;
import java.util.*;

public class Sender {
	private static int mss=576;
	//define server's commend line arguments
	private String filename;
	private InetAddress remote_ip;
    private int remote_port;
	private int ack_port;
	private String logfile;
	private int window;
	//define server's file input from TCP connection
	private DataInputStream socketInput;
	//define the server's logout put
    private PrintWriter output;
    //define UDP socket
    private DatagramSocket sock;
    private DatagramPacket datapkt;
    //define TCP socket
    private ServerSocket tcpSock;
    //define field in the UDP header
    private byte[] header;
    private static int seq_no_i=0;
    private static int seq_no=0;
	private static int ack_no=40;
	//define seq,ack from receiver
	//private static int ano_seq_no=40;
	private static int ano_ack_no=0;
	private int fin=0;
	private int send_port=9999;
	//define buffers for temporary data
    private byte[] filebuf;
    //define the send time for each packet
	private static Date[] sendTime;
	private Date date;
	private static int timeout=50;
	
	private int numOfPkt;
	private static int countpktinwindow=0;
	
	private static int totalBytes=0;
	private static int totalSeg=0;
	private static int retrans=0;
	
	//private double[] estirtt;
	private static Stack<Integer> estirtt;
	private static Stack<Integer> devrtt;

	
	
	private static PrintWriter sysOut=new PrintWriter(System.out, true);
	//constructor
	Sender(String filename,InetAddress remote_ip, int remote_port, int ack_port, String logfile, int window){
		this.filename=filename;
		this.remote_ip=remote_ip;
		this.remote_port=remote_port;
		this.ack_port=ack_port;
		this.logfile=logfile;
		this.window=window;
	}
	
	public void execute() throws IOException{
		//set output stdout or write into log file
		if(logfile.contentEquals("stdout"))
			output=new PrintWriter(System.out, true);
		else{
			File log=new File(logfile);
			FileOutputStream fos=new FileOutputStream(log);
			output=new PrintWriter(fos,true);
		}
		//read from file "filename"
		File file=null;
		try {
			file=new File(filename);
			//fileinputstream object
			FileInputStream fis=new FileInputStream(file);
			//the size of the byte array equals the size of the file
			filebuf= new byte[(int)file.length()];//file.length() return the length of the file in byte
			//save all the file in the form of byte array in filebuf
			fis.read(filebuf);
		} catch (FileNotFoundException e) {
			System.out.println("file not found");
		}
		//calculate how many packets are there to be sent
		numOfPkt=(int)Math.ceil((double)file.length()/(double)mss);
		//there is a timer for each packet, here initialize the array of timer
		sendTime=new Date[numOfPkt];
		//initialize the datagram socket for sending packets
		sock=new DatagramSocket(send_port);
		//initialize the tcp socket for receiving acks
		tcpSock=new ServerSocket(ack_port);
		//send start packet to inform receiver the sender tcp server is ready
		byte[] start=new byte[2];
		start[0]=(byte)0;
		start[1]=(byte)1;
		DatagramPacket startpkt=new DatagramPacket(start,start.length,remote_ip,remote_port);
		sock.send(startpkt);
		if(true){
			Socket tcps=tcpSock.accept();
			socketInput=new DataInputStream(tcps.getInputStream());
			SenderListener listener =new SenderListener(socketInput, output);
			listener.start();
		}
		//set window size!!!
		int numInWindow=window;
		System.out.println(numInWindow);
		estirtt=new Stack<Integer>();
		estirtt.push(1);
		devrtt=new Stack<Integer>();
		devrtt.push(0);
		while(ano_ack_no<=numOfPkt-1){
			//System.out.println(ano_ack_no);
			date=new Date();
			if(ano_ack_no<seq_no_i&&date.getTime()-sendTime[ano_ack_no].getTime()>timeout) {
				seq_no_i=ano_ack_no;
				retrans+=Math.min(numOfPkt-seq_no_i, numInWindow);
				System.out.println(retrans);
				ack_no =40+(seq_no_i)*20;
				countpktinwindow=0;
				timeout *=2;
			}
			int count0=countpktinwindow;
			int count1=numInWindow-count0;
			int count2=numOfPkt-seq_no_i;
			if(count0<numInWindow&&seq_no_i+count1-1<numOfPkt){
				while(count1>0) {
					sentpkt();
					count1--;
				}
			}
			//seq_no_i<numOfPkt&&
			if(count0<numInWindow&&count2<count1){
				while(count2>0){
					sentpkt();
					count2--;
				} 
			}
		}
	}
	public static void setCountpktinwindow(int rack){
		if(rack==seq_no_i) countpktinwindow=0;
		if(rack==seq_no_i-1) countpktinwindow=1;
	}
	public void sentpkt() throws IOException{
			seq_no=seq_no_i*mss;
			byte[] body=getBody(filebuf,seq_no);
			byte[] pkt=formpkt(getHeader(ack_port,remote_port,seq_no_i, ack_no, window, body),body);
			datapkt=new DatagramPacket(pkt,pkt.length,remote_ip,remote_port);
			sock.send(datapkt);
			totalBytes += pkt.length;
			totalSeg++;
			sendTime[seq_no_i]=new Date();
			if(fin==1) output.println(sendTime[seq_no_i]+","+ack_port+","+remote_port+","+seq_no+","+ack_no+","+"fin"+", "+estirtt.peek());
			else output.println(sendTime[seq_no_i]+","+ack_port+","+remote_port+","+seq_no+","+ack_no+","+"ack"+", "+estirtt.peek());
			seq_no_i++;
			ack_no+=20;
			countpktinwindow++;
	}
	
	 
	public static synchronized void setAnoAckno(int ack){
		ano_ack_no=ack;
	}
	public static synchronized void setEstirtt(Date date,int seq_no){
		int samplertt=(int)(date.getTime()-sendTime[seq_no].getTime());
		estirtt.push((int) (estirtt.peek()*0.75+samplertt));
		devrtt.push((int)(devrtt.peek()*0.75+Math.abs(samplertt-estirtt.peek())));
		timeout=estirtt.peek()+4*devrtt.peek();
	}
	public byte[] getBody(byte[] filebuf, int seq_no){
		byte[] thisBody;
		if(seq_no>filebuf.length) return null;
		if((seq_no+mss)<filebuf.length) thisBody=new byte[mss];
		else thisBody=new byte[filebuf.length-seq_no];
		for(int i=0;i<thisBody.length;i++){
			thisBody[i]=filebuf[seq_no+i];
		}
		return thisBody;
	}
	
	public byte[] getHeader(int srcport, int destport, int seq_no,int ack_no,int window, byte[] body){
		header=new byte[20];
		//put source port into the header
		short sport=(short) srcport;
		header[0]=(byte)((sport & 0xff00)>>8);
		header[1]=(byte)(sport&0x00ff);
		//put destination port into the header
		short dport=(short) destport;
		header[2]=(byte)((dport & 0xff00)>>8);
		header[3]=(byte)(dport & 0x00ff);
		//put sequence number into the header
		header[4]=(byte)((seq_no & 0xff000000)>>24);
		header[5]=(byte)((seq_no & 0x00ff0000)>>16);
		header[6]=(byte)((seq_no & 0x0000ff00)>>8);
		header[7]=(byte)((seq_no & 0x000000ff)>>0);
		//put acknowledge number into the header
		header[8]=(byte)((ack_no & 0xff000000)>>24);
		header[9]=(byte)((ack_no & 0x00ff0000)>>16);
		header[10]=(byte)((ack_no & 0x0000ff00)>>8);
		header[11]=(byte)((ack_no & 0x000000ff)>>0);
		//header[12]=(byte)(1);
		
		//put fin flag into the header
		if(numOfPkt-1==seq_no) fin=1;
		else fin=0;
		if(fin==1) header[13]=(byte)(1);
		else header[13]=(byte)(16);
		//put window size into the header
		short wsize=(short) window;
		header[14]=(byte)((wsize & 0xff00)>>8);
		header[15]=(byte)(wsize & 0x00ff);
		
		//put length into header
		short len=(short)(body.length+20);
		header[18]=(byte)((len & 0xff00)>>8);
		header[19]=(byte)(len & 0x00ff);
		//put checksum into header
		short sum=(short)0;
		for(byte headpart : header){
			sum += (short) headpart;		
		}
		for(byte bodypart:body){
			sum += (short) bodypart;
		}
		short check=(short) ~sum;
		header[16]=(byte)((check & 0xff00)>>8);
		header[17]=(byte)(check & 0x00ff);
		return header;
	}
	public byte[] formpkt(byte[] header, byte[] body){
		byte[] pkt=new byte[header.length+body.length];
		System.arraycopy(header, 0, pkt, 0, header.length);
		System.arraycopy(body, 0, pkt, header.length, body.length);
		return pkt;
	}
	
	public static synchronized void printFinal(){
		sysOut.println("Delivery completed successfully");
		sysOut.println("Total bytes sent = "+ totalBytes);
		sysOut.println("Segments sent = "+ totalSeg);
		sysOut.println("Segments retransmitted = " + ((double)retrans/(double)totalSeg)*100+"%");
	}
	public static void main(String[] args) throws UnknownHostException{
		String filename=args[0];
		InetAddress remote_ip=InetAddress.getByName(args[1]);
		int remote_port=Integer.parseInt(args[2]);
		int ack_port=Integer.parseInt(args[3]);
		String logfile=args[4];
		int window=Integer.parseInt(args[5]);
		//new a sender
		Sender sender=new Sender(filename,remote_ip,remote_port,ack_port, logfile,window);
		//execute sender
		try {
			sender.execute();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	
}
