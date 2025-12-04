# Project Report

Author: Marcos Wang Zhu
Email: m.wangzhu@students.maastrichtuniversity.nl
Student ID number: i6382454

## Gemini Lite Client Program
The Gemini Lite client connects to Gemini Lite server (port 1958 by default) to retrieve content using the gemini-lite:// scheme.

Usage:
java -cp target/bcs2110-2025.jar gemini_lite.Client "URL" ["input"]

Where:
- "URL" is a required URL with the form gemini-lite://hostname:port/path
- "input" is an optional input provided by the user

The client handles status codes for multiple cateogries:
1) 1x: Input expected
2) 2x: Success
3) 3x: Redirection
4) 4x: Temporary failure
5) 5x: Permanent failure

Specifically, for status code 44 "slow down", the client implements exponential backoff:
- The delay is calculated such that the waiting time doubles each retry attempt
- A maximum of 5 retries is set before aborting with IOException
- Both client and Proxy implement the same logic

The server continues processing the same request URL after each delay until success or max retries exceeded.

Other client features include:
- A maximum of 5 redirects
- Strict CRLF parsing
- Prompting user for input on 1x responses
- Proxy support through the GEMINI_LITE_PROXY environment variable

## Gemini Lite Server Program
The Gemini Lite server serves files from a specified directory (port 1958 by default).

Usage:
java -cp target/bcs2110-2025.jar gemini_lite.Server "directory" ["port"]

Where:
- "directory": Required root directory to serve files from (must exist and be readable)
- "port": Optional port number (1958 by default)

Features:
- MIME Types:
    + gmi: text/gemini
    + txt: text/plain
    + Others:  application/octet-stream
- URL Parsing: Strips query strings (?) and rejects fragments (#) and userinfo (@)
- Error Responses:
    + 51 Not Found: Missing/unreadable files or directories
    + 59 Bad Request: Malformed URLs or protocol violations
    + 50 Server Error: File read failures

Requests for URLs corresponding to directories (not files) return a status code 51 Not Found error:

## Gemini Lite Proxy Program
The Gemini Lite proxy acts as a forwarding server between client and server.

Usage:
java -cp target/bcs2110-2025.jar gemini_lite.Proxy "port"

Where:
- "port": Required port to listen on for client connections

Features:
- Maximum of 5 redirects
- Slowdown (status code 44): Exponential backoff, maximum 5 retries
- Handles the same status codes as the client

There is a particular error, error 43, that will show in some specific scenarios:
- Redirect loop: Same URL visited twice in redirect chain
- Too many redirects: >5 redirects followed
- Server protocol error: ProtocolSyntaxException from origin server
- Connection failure: IOException connecting to origin server
- Invalid status code: Origin server returns status outside 10-59 range

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
- gemini.muaddib.boogle.lab-kale. IN A 10.2.0.105
- hello.muaddib.boogle.lab-kale. IN TXT "Hello world!"
- marcos.muaddib.boogle.lab-kale. IN A 10.2.0.105
- aditya.muaddib.boogle.lab-kale. IN 10.2.0.105

Where muaddib.boogle is our domain and 10.2.0.105 our IP address.
Of course, after adding everything, we checked the file syntax, reloaded the daemon and checked its status

To test the registration we (successfully) run:
- dig -t any muaddib.boogle.lab-kale
- dig ns1.muaddib.boogle.lab-kale
- dig gemini.muaddib.boogle.lab-kale
- dig -t any hello.muaddib.boogle.lab-kale


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

Gemini Lite uses a two-digit response code system categorized into five main classes: 
- 1x: Input expected
- 2x: Success
- 3x: Redirects
- 4x: Temporary failure
- 5x: Permanent failure
On the other hand, HTTP/1.1 uses a three-digit system across five classes:
- 1xx: Informational
- 2xx: Success
- 3xx: Redirection
- 4xx: Client error
- 5xx: Server error
The designer may have decided to depart from this classification scheme to prioritize simplicity and reduce implementation complexity, while, at the same time, being able to maintain its effectiveness

Caching proxies are difficult to implement under the Gemini Lite specification because the protocol lacks important features like cache-control headers, timestamps, or version tags that tell whether a cached copy is still "fresh". Without these, a proxy cannot know if it should serve its stored copy or ask the upstream server for an updated version while the server is still available. Archival mode, which only uses cached copies when the server is offline, is easier to manage, but true caching that improves efficiency requires information not provided by Gemini’s current design. This makes building a fully functional caching proxy challenging.

Within the Gemini Lite specification, proxies are allowed to forward redirection responses, meaning they simply pass these responses from the server to the client without changes. However, deliberately slowing down responses could harm user experience and contradict the protocol’s goals for simplicity and efficiency. In my opinion, the specification should clearly forbid proxies from intentionally delaying responses to ensure that they improve or at least do not degrade performance.

To support inline images in a graphical Gemini browser, no changes are needed to the Gemini Lite network protocol itself. The protocol allows clients to request resources, and images can be requested as separate entities. The Gemtext format does not need to be altered either because links to external resources already exist , which can be images. The main change would be in client behavior: the client should recognize image MIME types and handle image links by fetching and displaying images inline without requiring changes in the specification.

Gemini Lite lacks reliable delivery, relying on raw TCP without retries, acknowledgments, or congestion control beyond TLS. Bandwidth efficiency is limited due to the absence of content compression, partial transfers, or size-related metadata, making data transfer less optimal. While the protocol’s simplicity provides clarity and ease of implementation, it could benefit from enhancements such as optional content length headers, basic caching instructions, or mechanisms for better bandwidth use. These measures could improve reliability and efficiency without compromising the protocol’s minimalist philosophy.

