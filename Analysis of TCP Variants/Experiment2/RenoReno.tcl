#Create a simulator object
set ns [new Simulator]

#Open the Trace file
set tf [open RenoReno[lindex $argv 0].tr w]
$ns trace-all $tf

#Define a 'finish' procedure
proc finish {} {
        global ns nf tf
        $ns flush-trace
        #Close the Trace file
        close $tf
        exit 0
}

#Create six nodes
set n1 [$ns node]
set n2 [$ns node]
set n3 [$ns node]
set n4 [$ns node]
set n5 [$ns node]
set n6 [$ns node]


#Create links between the nodes
$ns duplex-link $n1 $n2 10Mb 10ms DropTail
$ns duplex-link $n5 $n2 10Mb 10ms DropTail
$ns duplex-link $n2 $n3 10Mb 10ms DropTail
$ns duplex-link $n3 $n4 10Mb 10ms DropTail
$ns duplex-link $n3 $n6 10Mb 10ms DropTail

#Set Queue Size of all links to 10
$ns queue-limit $n1 $n2 10
$ns queue-limit $n5 $n2 10
$ns queue-limit $n3 $n4 10
$ns queue-limit $n2 $n3 10
$ns queue-limit $n3 $n6 10


#Setup a UDP connection
set udp [new Agent/UDP]
$ns attach-agent $n2 $udp
set null [new Agent/Null]
$ns attach-agent $n3 $null
$ns connect $udp $null
$udp set fid_ 2

#Setup a CBR over UDP connection
set cbr [new Application/Traffic/CBR]
$cbr attach-agent $udp
$cbr set type_ CBR
$cbr set packet_size_ 1000
$cbr set rate_ [lindex $argv 0]mb
$cbr set random_ false


#Setup a TCP connection
set tcp1 [new Agent/TCP/Reno]
$tcp1 set class_ 2
$ns attach-agent $n1 $tcp1
set sink1 [new Agent/TCPSink]
$ns attach-agent $n4 $sink1
$ns connect $tcp1 $sink1
$tcp1 set fid_ 1

#Setup a FTP over TCP connection
set ftp1 [new Application/FTP]
$ftp1 attach-agent $tcp1
$ftp1 set type_ FTP

#Setup a TCP connection
set tcp2 [new Agent/TCP/Reno]
$tcp2 set class_ 2
$ns attach-agent $n5 $tcp2
set sink2 [new Agent/TCPSink]
$ns attach-agent $n6 $sink2
$ns connect $tcp2 $sink2
$tcp2 set fid_ 1

#Setup a FTP over TCP connection
set ftp2 [new Application/FTP]
$ftp2 attach-agent $tcp2
$ftp2 set type_ FTP

#Schedule events for the CBR and FTP agents
$ns at 0.1 "$cbr start"
$ns at 0.5 "$ftp1 start"
$ns at 0.5 "$ftp2 start"
$ns at 60.5 "$ftp1 stop"
$ns at 60.5 "$ftp2 stop"
$ns at 61.0 "$cbr stop"

#Call the finish procedure after 60 seconds of simulation time
$ns at 61.0 "finish"

#Run the simulation
$ns run

