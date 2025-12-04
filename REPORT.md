# Project Report

Author: Marcos Wang Zhu
Email: m.wangzhu@students.maastrichtuniversity.nl
Student ID number: i6382454

## Gemini Lite Client Program

(Insert user documentation for your program here. Include command-line usage instructions.)

## Gemini Lite Server Program

(Insert user documentation for your program here. Include command-line usage instructions.)

## Gemini Lite Proxy Program

(Insert user documentation for your program here. Include command-line usage instructions.)

## Alternative DNS, Bakeoff and Wireshark outputs

Lab 4:

We decided to be a non-registrar group. 
To start, we edited the /etc/bind/named.conf.local file to include our third-level domain, so the resulting file would contain the following:
    zone "muaddib.boogle.lab-kale" {
        type primary;
        file "/var/lib/bind/db.zone.lab-kale";
    };
Where muaddib.boogle is our domain.

We also edited the /var/lib/bind/db.zone.lab-kale file getting the following:
    $TTL    10
    @       IN      SOA     muaddib.boogle.lab-kale. hostmaster.muaddib.boogle.lab-kale. (
                                1         ; Serial
                                10         ; Refresh
                                10         ; Retry
                                10         ; Expire
                                10 )       ; Negative Cache TTL
    ;
    @       IN      NS      ns1.muaddib.boogle.lab-kale.
    ; change 1.2.3.4 to ip address of pi
    ns1     IN      A       10.2.0.105

After changing both files we had to run the systemctl reload named command to refresh the databases from the files.
To test these changes locally, we run  dig @127.0.0.1 -t any muaddib.boogle.lab-kale and dig @127.0.0.1 -t any ns1.muaddib.boogle.lab-kale on the raspberry pi.

Now we went to add four more records to the zone file:
-gemini.muaddib.boogle.lab-kale. IN A 10.2.0.105
-hello.muaddib.boogle.lab-kale. IN TXT "Hello world!"
-marcos.muaddib.boogle.lab-kale. IN A 10.2.0.105
-aditya.muaddib.boogle.lab-kale. IN 10.2.0.105
Where muaddib.boogle is our domain and 10.2.0.105 our IP address.
Of course, after adding everything, we checked the file syntax, reloaded the daemon and checked its status

To test the registration we (successfully) run:
-dig -t any muaddib.boogle.lab-kale
-dig ns1.muaddib.boogle.lab-kale
-dig gemini.muaddib.boogle.lab-kale
-dig -t any hello.muaddib.boogle.lab-kale


Next we did iterative resolutiion by hand.
We first run dig -t ns lab-kale, which answered ns1.lab.kale. :

pi0041@pi0041:~ $ dig -t ns lab-kale

; <<>> DiG 9.18.41-1~deb12u1-Debian <<>> -t ns lab-kale
;; global options: +cmd
;; Got answer:
;; ->>HEADER<<- opcode: QUERY, status: NOERROR, id: 65499
;; flags: qr rd ra; QUERY: 1, ANSWER: 1, AUTHORITY: 0, ADDITIONAL: 2

;; OPT PSEUDOSECTION:
; EDNS: version: 0, flags:; udp: 1232
; COOKIE: 38a72704afc995d201000000691eff7aeee7ce53505ac287 (good)
;; QUESTION SECTION:
;lab-kale.                      IN      NS

;; ANSWER SECTION:
lab-kale.               10      IN      NS      ns1.lab-kale.

;; ADDITIONAL SECTION:
ns1.lab-kale.           6       IN      A       10.2.0.100

;; Query time: 119 msec
;; SERVER: 10.2.0.1#53(10.2.0.1) (UDP)
;; WHEN: Thu Nov 20 12:46:02 CET 2025
;; MSG SIZE  rcvd: 99

After that, we run dig ns1.lab-kale to get the IP address:

pi0041@pi0041:~ $ dig ns1.lab-kale

; <<>> DiG 9.18.41-1~deb12u1-Debian <<>> ns1.lab-kale
;; global options: +cmd
;; Got answer:
;; ->>HEADER<<- opcode: QUERY, status: NOERROR, id: 64607
;; flags: qr rd ra; QUERY: 1, ANSWER: 1, AUTHORITY: 0, ADDITIONAL: 1

;; OPT PSEUDOSECTION:
; EDNS: version: 0, flags:; udp: 1232
; COOKIE: b0515dbed4b5c63b01000000691effdfa0ca180c4ff2b3c0 (good)
;; QUESTION SECTION:
;ns1.lab-kale.                  IN      A

;; ANSWER SECTION:
ns1.lab-kale.           10      IN      A       10.2.0.100

;; Query time: 7 msec
;; SERVER: 10.2.0.1#53(10.2.0.1) (UDP)
;; WHEN: Thu Nov 20 12:47:43 CET 2025
;; MSG SIZE  rcvd: 85

We then run dig @10.2.0.100 muaddib.boogle.lab-kale:

pi0041@pi0041:~ $ dig @10.2.0.100 muaddib.boogle.lab-kale

; <<>> DiG 9.18.41-1~deb12u1-Debian <<>> @10.2.0.100 muaddib.boogle.lab-kale
; (1 server found)
;; global options: +cmd
;; Got answer:
;; ->>HEADER<<- opcode: QUERY, status: NXDOMAIN, id: 55923
;; flags: qr aa rd; QUERY: 1, ANSWER: 0, AUTHORITY: 1, ADDITIONAL: 1
;; WARNING: recursion requested but not available

;; OPT PSEUDOSECTION:
; EDNS: version: 0, flags:; udp: 1232
; COOKIE: c7d58aac640aacf001000000691f000c0406448c23a39427 (good)
;; QUESTION SECTION:
;muaddib.boogle.lab-kale.       IN      A

;; AUTHORITY SECTION:
lab-kale.               10      IN      SOA     ns1.lab-kale. hostmaster.lab-kale. 1 10 10 10 10

;; Query time: 63 msec
;; SERVER: 10.2.0.100#53(10.2.0.100) (UDP)
;; WHEN: Thu Nov 20 12:48:28 CET 2025
;; MSG SIZE  rcvd: 139

I was not able to do steps 4 and 5 of lab 4 because, having uploaded the target/bcs2110-2025.jar file to the Pi and created the index.gmi file as well, this happened:

pi0041@pi0041:~ $ java -cp marcos.jar gemini_lite.Client gemini-lite://aditya.muaddib.boogle.lab-kale:1959/index.gmi
Error: aditya.muaddib.boogle.lab-kale

Additionally, when running pi0041@pi0041:~ $ ping marcos.muaddib.boogle.lab-kale, I would get ping: marcos.muaddib.boogle.lab-kale: Name or service not known.

Lab 5:

We followed the lab instructions and added the two DHCP packets as a file called lab5_dhcp.txt.

Lab 5: Packet capture of joining a network:
1) What are the source and destination ethernet addresses for the request packet?
Source: LiteonTechno_41:90:11 (c0:35:32:41:90:11)
Destination: Broadcast (ff:ff:ff:ff:ff:ff)

2) What are the IP source and destination addresses in the request packet?
Source Address: 0.0.0.0
Destination Address: 255.255.255.255

3) What are the UDP source and destination port numbers in the request packet?
Source Port: 68
Destination Port: 67

4) What is the value of the DHCP “Your (client) IP address” field in the request packet?
Your (client) IP address: 0.0.0.0

5) Does the request contain an Option 50 (Requested IP Address)? If so, what is the IP address being requested?
The request contains an Option 50 (Requested IP Address). The IP address being requested is 10.1.2.109

6) What are the source and destination ethernet addresses for the ACK packet?
Source: RaspberryPi_32:7f:c4 (2c:cf:67:32:7f:c4)
Destination: LiteonTechno_41:90:11 (c0:35:32:41:90:11)

7) What are the IP source and destination addresses in the ACK packet?
Source Address: 10.1.0.1
Destination Address: 10.1.2.109

8) What are the UDP source and destination port numbers in the ACK packet?
Source Port: 67
Destination Port: 68

9) What is the value of the DHCP “Your (client) IP address” field in the ACK packet?
Your (client) IP address: 10.1.2.109

10) What are the values of the following DHCP options in the ACK packet?
Option (1) Subnet Mask: 255.255.252.0
Option (3) Router: 10.1.0.3
Option (6) Domain Name Server: 10.1.0.1

11) Look back at Lab 2. Use the appropriate command (ip route, netstat -rn, or route print) to print out your laptop’s forwarding table. Satisfy yourself you can find the Subnet Mask and Router from the DHCP ACK packet in your machine’s forwarding table. Copy and paste the table into your report.
IPv4 forwarding table
============================================================================
Active routes:
Network destination            Netmask      Gateway        Interface  Metric
            0.0.0.0            0.0.0.0    10.10.0.1      10.10.11.200     35
          10.10.0.0      255.255.240.0      On-link      10.10.11.200    291
       10.10.11.200    255.255.255.255      On-link      10.10.11.200    291
       10.10.15.255    255.255.255.255      On-link      10.10.11.200    291
          127.0.0.0          255.0.0.0      On-link         127.0.0.1    331
          127.0.0.1    255.255.255.255      On-link         127.0.0.1    331
    127.255.255.255    255.255.255.255      On-link         127.0.0.1    331
          224.0.0.0          240.0.0.0      On-link         127.0.0.1    331
          224.0.0.0          240.0.0.0      On-link      10.10.11.200    291
    255.255.255.255    255.255.255.255      On-link         127.0.0.1    331
    255.255.255.255    255.255.255.255      On-link      10.10.11.200    291
============================================================================

We could not continue from step 3.5 onwards od lab 5, since the problem from lab 4 persisted and we ran out of time.

## Reflection on Gemini Lite

(Paragraph or short essay-style answers to the questions)
