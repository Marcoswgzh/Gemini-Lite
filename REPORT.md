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



## Reflection on Gemini Lite

(Paragraph or short essay-style answers to the questions)
