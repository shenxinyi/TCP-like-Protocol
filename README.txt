{\rtf1\ansi\ansicpg1252\cocoartf1348\cocoasubrtf170
{\fonttbl\f0\fswiss\fcharset0 Helvetica;\f1\fnil\fcharset0 Menlo-Regular;}
{\colortbl;\red255\green255\blue255;}
\margl1440\margr1440\vieww10800\viewh8400\viewkind0
\pard\tx720\tx1440\tx2160\tx2880\tx3600\tx4320\tx5040\tx5760\tx6480\tx7200\tx7920\tx8640\pardirnatural

\f0\fs24 \cf0 1.Description:\
This simple TCP-like transport layer protocol is implemented with JAVA.\
I make three classes, they are Sender.java, SenderListener.java, Receiver.java\
First run the Receiver set it ready for receiving udp datagram.\
Then run the sender, set it as the server in the TCP communication. then send an empty packet to receiver indicating that sender is going to sent data.\
After receiver received the empty packet, it bind to the TCP socket as a client.\
\
Then the sender start sending data from file according to the window size, it can also handle packet loss, packet corruption, packet duplication and packet reordering.\
\
After receiver received all packets and write it to the output file, it send a fin to the sender, then the sender terminate its timer and print out the logistics of this file transmission.\
\
2. Detail on development environment\
Language: JAVA SE-1.6\
OS: MAC OS 10.10.5\
Editor: Sublime Text 2\
\
3. Instructions on running the code\
First using the terminal enter the directory containing all the .java files\
Then input make in the command line, if there is no error, all the java files have been compiled\
After that input \'93java Receiver <filename> <listening_port> <sender_ip> <sender_port> <log_filename> \'94, if there is no error, the receiver is running, <listening_port> is for receiving udp packets, and the <sender_port>, <sender_ip> are used to send acknowledge.\
Then input \'93java Sender <filename> <remote_ip> <remote_port> <ack_port_num> <log_filename> <window_size>\'94, if there is no error, the sender is running, <remote_ip> and <remote_port> is used to send udp packets to, and the <ack_port_num> is used to listen to acknowledges from receiver\
\
4.Sample commands to invoke code\
\pard\tx560\tx1120\tx1680\tx2240\tx2800\tx3360\tx3920\tx4480\tx5040\tx5600\tx6160\tx6720\pardirnatural

\f1\fs22 \cf0 \CocoaLigature0 ./newudpl -i127.0.0.1/9999 -o127.0.0.1/8888
\f0\fs24 \CocoaLigature1 \
\pard\tx720\tx1440\tx2160\tx2880\tx3600\tx4320\tx5040\tx5760\tx6480\tx7200\tx7920\tx8640\pardirnatural
\cf0 \'93make\'94\
\'93java Receiver file.txt 8888 localhost 20001 log.txt\'94\
\'93java Sender try.txt localhost 41192 20001 senderlog.txt 2\'94\
\
\
}