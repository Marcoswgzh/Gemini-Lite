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

## Test Cases

(Include at least 3 of each kind of test case here, formatted as instructed in the project manual.)

### Client test cases

### Server test cases

### Proxy test cases

## Alternative DNS, Bakeoff and Wireshark outputs

(Include content as specified in labs)
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

After changing both files we had to run the **systemctl reload named** command to refresh the databases from the files.
To test these changes locally, we run  **dig @127.0.0.1 -t any muaddib.boogle.lab-kale** and **dig @127.0.0.1 -t any ns1.muaddib.boogle.lab-kale** on the raspberry pi.

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

Lab 5: Packet capture of joining a network:
1.What are the source and destination ethernet addresses for the request packet?

2.What are the IP source and destination addresses in the request packet?

3.What are the UDP source and destination port numbers in the request packet?

4.What is the value of the DHCP “Your (client) IP address” field in the request packet?

5.Does the request contain an Option 50 (Requested IP Address)? If so, what is the IP address being requested?

6.What are the source and destination ethernet addresses for the ACK packet?

7.What are the IP source and destination addresses in the ACK packet?

8.What are the UDP source and destination port numbers in the ACK packet?

9.What is the value of the DHCP “Your (client) IP address” field in the ACK packet?

10.What are the values of the following DHCP options in the ACK packet?
Option (1) Subnet Mask
Option (3) Router
Option (6) Domain Name Server

11.Look back at Lab 2. Use the appropriate command (ip route, netstat -rn, or route print) to print out your laptop’s forwarding table. Satisfy yourself you can find the Subnet Mask and Router from the DHCP ACK packet in your machine’s forwarding table. Copy and paste the table into your report.



## Reflection on Gemini Lite

(Paragraph or short essay-style answers to the questions)
