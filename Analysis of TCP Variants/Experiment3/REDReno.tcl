#Create a simulator object
set ns [new Simulator]

#Open the Trace file
set tf [open REDReno.tr w]
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
$ns duplex-link $n1 $n2 10Mb 10ms RED
$ns duplex-link $n5 $n2 10Mb 10ms RED
$ns duplex-link $n2 $n3 10Mb 10ms RED
$ns duplex-link $n3 $n4 10Mb 10ms RED
$ns duplex-link $n3 $n6 10Mb 10ms RED

#Set Queue Size of all links to 10
$ns queue-limit $n1 $n2 10
$ns queue-limit $n5 $n2 10
$ns queue-limit $n3 $n4 10
$ns queue-limit $n2 $n3 10
$ns queue-limit $n3 $n6 10


#Setup a UDP connection
set udp [new Agent/UDP]
$ns attach-agent $n5 $udp
set null [new Agent/Null]
$ns attach-agent $n6 $null
$ns connect $udp $null
$udp set fid_ 2

#Setup a CBR over UDP connection
set cbr [new Application/Traffic/CBR]
$cbr attach-agent $udp
$cbr set type_ CBR
$cbr set packet_size_ 1000
$cbr set rate_ 7mb
$cbr set random_ false

#Setup a TCP connection
set tcp1 [new Agent/TCP/Reno]
$tcp1 set class_ 2
$tcp1 set window_ 1000
$ns attach-agent $n1 $tcp1
set sink1 [new Agent/TCPSink]
$ns attach-agent $n4 $sink1
$ns connect $tcp1 $sink1
$tcp1 set fid_ 1

#Setup a FTP over TCP connection
set ftp1 [new Application/FTP]
$ftp1 attach-agent $tcp1
$ftp1 set type_ FTP


#Schedule events for the CBR and FTP agents
$ns at  0.1 "$ftp1 start"
$ns at 60.5 "$ftp1 stop"
$ns at 5.1 "$cbr start"
$ns at 61.0 "$cbr stop"

#Call the finish procedure after 60 seconds of simulation time
$ns at 61.0 "finish"

#Run the simulation
$ns run
