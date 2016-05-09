1.Description:
This simple TCP-like transport layer protocol is implemented with JAVA.
I make three classes, they are Sender.java, SenderListener.java, Receiver.java
First run the Receiver set it ready for receiving udp datagram.
Then run the sender, set it as the server in the TCP communication. then send an empty packet to receiver indicating that sender is going to sent data.
After receiver received the empty packet, it bind to the TCP socket as a client.

Then the sender start sending data from file according to the window size, it can also handle packet loss, packet corruption, packet duplication and packet reordering.

After receiver received all packets and write it to the output file, it send a fin to the sender, then the sender terminate its timer and print out the logistics of this file transmission.

2.Detail on development environment
Language: JAVA SE-1.6
OS: MAC OS 10.10.5
Editor: Sublime Text 2

3.Instructions on running the code
First using the terminal enter the directory containing all the .java files
Then input make in the command line, if there is no error, all the java files have been compiled
After that input “java Receiver <filename> <listening_port> <sender_ip> <sender_port> <log_filename> ”, if there is no error, the receiver is running, <listening_port> is for receiving udp packets, and the <sender_port>, <sender_ip> are used to send acknowledge.
Then input “java Sender <filename> <remote_ip> <remote_port> <ack_port_num> <log_filename> <window_size>”, if there is no error, the sender is running, <remote_ip> and <remote_port> is used to send udp packets to, and the <ack_port_num> is used to listen to acknowledges from receiver

4.Sample commands to invoke code
./newudpl -i127.0.0.1/9999 -o127.0.0.1/8888
./newudpl -i127.0.0.1/9999 -o127.0.0.1/8888 -L 10
./newudpl -i127.0.0.1/9999 -o127.0.0.1/8888 -O 10

“make”
“java Receiver file.txt 8888 localhost 20001 log.txt”
“java Sender try.txt localhost 41192 20001 senderlog.txt 2”

