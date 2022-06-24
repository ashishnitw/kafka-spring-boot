GLC02DK0ZLP3XY:ssl ashish.ranjan$ keytool -keystore server.keystore.jks -alias localhost -validity 365 -genkey -keyalg RSA
Enter keystore password:  ashishnitw
Re-enter new password: ashishnitw
What is your first and last name?
  [Unknown]:  ashishnitw
What is the name of your organizational unit?
  [Unknown]:  ashishnitw
What is the name of your organization?
  [Unknown]:  ashishnitw
What is the name of your City or Locality?
  [Unknown]:  Patna
What is the name of your State or Province?
  [Unknown]:  Bihar
What is the two-letter country code for this unit?
  [Unknown]:  IN
Is CN=ashishnitw, OU=ashishnitw, O=ashishnitw, L=Patna, ST=Bihar, C=IN correct?
  [no]:  yes

Enter key password for <localhost>
        (RETURN if same as keystore password):  

Warning:
The JKS keystore uses a proprietary format. It is recommended to migrate to PKCS12 which is an industry standard format using "keytool -importkeystore -srckeystore server.keystore.jks -destkeystore server.keystore.jks -deststoretype pkcs12".
GLC02DK0ZLP3XY:ssl ashish.ranjan$ ls
Readme.md               server.keystore.jks
GLC02DK0ZLP3XY:ssl ashish.ranjan$ keytool -list -v -keystore server.keystore.jks 
Enter keystore password:  
Keystore type: jks
Keystore provider: SUN

Your keystore contains 1 entry

Alias name: localhost
Creation date: 16 Jun, 2022
Entry type: PrivateKeyEntry
Certificate chain length: 1
Certificate[1]:
Owner: CN=ashishnitw, OU=ashishnitw, O=ashishnitw, L=Patna, ST=Bihar, C=IN
Issuer: CN=ashishnitw, OU=ashishnitw, O=ashishnitw, L=Patna, ST=Bihar, C=IN
Serial number: 253ced7c
Valid from: Thu Jun 16 13:04:30 IST 2022 until: Fri Jun 16 13:04:30 IST 2023
Certificate fingerprints:
         SHA1: D3:B0:17:39:AF:E7:1D:AA:ED:39:7D:7D:FE:5B:F2:D3:54:2D:F2:D1
         SHA256: 45:F8:DC:9F:12:CF:2F:F8:A3:7A:A7:9C:F1:F6:5C:C4:D6:F5:34:7B:AF:FE:6B:24:AB:95:65:DB:96:2B:60:B2
Signature algorithm name: SHA256withRSA
Subject Public Key Algorithm: 2048-bit RSA key
Version: 3

Extensions: 

#1: ObjectId: 2.5.29.14 Criticality=false
SubjectKeyIdentifier [
KeyIdentifier [
0000: DE A5 03 4E 0C 09 E4 46   30 42 FB EC D1 E4 99 BF  ...N...F0B......
0010: 7B 8A 1C 88                                        ....
]
]



*******************************************
*******************************************



Warning:
The JKS keystore uses a proprietary format. It is recommended to migrate to PKCS12 which is an industry standard format using "keytool -importkeystore -srckeystore server.keystore.jks -destkeystore server.keystore.jks -deststoretype pkcs12".
GLC02DK0ZLP3XY:ssl ashish.ranjan$ 

GLC02DK0ZLP3XY:ssl ashish.ranjan$ openssl req -new -x509 -keyout ca-key -out ca-cert -days 365 -subj "/CN=local-security-CA"
Generating a 2048 bit RSA private key
........+++
........+++
writing new private key to 'ca-key'
Enter PEM pass phrase: ashishnitw
Verifying - Enter PEM pass phrase: ashishnitw

GLC02DK0ZLP3XY:ssl ashish.ranjan$ ls
Readme.md               ca-cert                 ca-key                  server.keystore.jks
GLC02DK0ZLP3XY:ssl ashish.ranjan$ keytool -keystore server.keystore.jks -alias localhost -certreq -file cert-file
Enter keystore password:  ashishnitw

Warning:
The JKS keystore uses a proprietary format. It is recommended to migrate to PKCS12 which is an industry standard format using "keytool -importkeystore -srckeystore server.keystore.jks -destkeystore server.keystore.jks -deststoretype pkcs12".
GLC02DK0ZLP3XY:ssl ashish.ranjan$ keytool -list -v -keystore server.keystore.jks 
Enter keystore password:  
Keystore type: jks
Keystore provider: SUN

Your keystore contains 2 entries

Alias name: caroot
Creation date: 16 Jun, 2022
Entry type: trustedCertEntry

Owner: CN=local-security-CA
Issuer: CN=local-security-CA
Serial number: cc6b40a311c37810
Valid from: Thu Jun 16 13:19:15 IST 2022 until: Fri Jun 16 13:19:15 IST 2023
Certificate fingerprints:
         SHA1: 51:A6:C2:DE:24:24:27:B7:49:F9:EF:91:74:E0:55:11:AB:35:E4:91
         SHA256: B3:F2:27:8E:9A:7E:C4:D7:C6:39:EC:B5:D8:02:7C:E6:F6:F0:E1:23:29:1D:D3:45:B3:E1:21:2D:0F:81:E1:74
Signature algorithm name: SHA256withRSA
Subject Public Key Algorithm: 2048-bit RSA key
Version: 1


*******************************************
*******************************************


Alias name: localhost
Creation date: 16 Jun, 2022
Entry type: PrivateKeyEntry
Certificate chain length: 2
Certificate[1]:
Owner: CN=ashishnitw, OU=ashishnitw, O=ashishnitw, L=Patna, ST=Bihar, C=IN
Issuer: CN=local-security-CA
Serial number: a559094d1fe543ae
Valid from: Thu Jun 16 13:35:32 IST 2022 until: Fri Jun 16 13:35:32 IST 2023
Certificate fingerprints:
         SHA1: 96:83:61:90:BA:EC:D7:A5:25:61:48:62:06:6F:88:EF:37:E1:22:4D
         SHA256: 4D:BB:C7:4A:05:60:DC:52:50:0E:B0:6C:2C:66:A8:66:E0:DD:51:8E:F0:4B:6E:DD:61:CF:BE:34:DC:2C:18:E8
Signature algorithm name: SHA1withRSA (weak)
Subject Public Key Algorithm: 2048-bit RSA key
Version: 1
Certificate[2]:
Owner: CN=local-security-CA
Issuer: CN=local-security-CA
Serial number: cc6b40a311c37810
Valid from: Thu Jun 16 13:19:15 IST 2022 until: Fri Jun 16 13:19:15 IST 2023
Certificate fingerprints:
         SHA1: 51:A6:C2:DE:24:24:27:B7:49:F9:EF:91:74:E0:55:11:AB:35:E4:91
         SHA256: B3:F2:27:8E:9A:7E:C4:D7:C6:39:EC:B5:D8:02:7C:E6:F6:F0:E1:23:29:1D:D3:45:B3:E1:21:2D:0F:81:E1:74
Signature algorithm name: SHA256withRSA
Subject Public Key Algorithm: 2048-bit RSA key
Version: 1


*******************************************
*******************************************



Warning:
<localhost> uses the SHA1withRSA signature algorithm which is considered a security risk. This algorithm will be disabled in a future update.
The JKS keystore uses a proprietary format. It is recommended to migrate to PKCS12 which is an industry standard format using "keytool -importkeystore -srckeystore server.keystore.jks -destkeystore server.keystore.jks -deststoretype pkcs12".
GLC02DK0ZLP3XY:ssl ashish.ranjan$ 

GLC02DK0ZLP3XY:ssl ashish.ranjan$ keytool -keystore client.truststore.jks -alias CARoot -import -file ca-cert
Enter keystore password:  
Re-enter new password: 
Owner: CN=local-security-CA
Issuer: CN=local-security-CA
Serial number: cc6b40a311c37810
Valid from: Thu Jun 16 13:19:15 IST 2022 until: Fri Jun 16 13:19:15 IST 2023
Certificate fingerprints:
         SHA1: 51:A6:C2:DE:24:24:27:B7:49:F9:EF:91:74:E0:55:11:AB:35:E4:91
         SHA256: B3:F2:27:8E:9A:7E:C4:D7:C6:39:EC:B5:D8:02:7C:E6:F6:F0:E1:23:29:1D:D3:45:B3:E1:21:2D:0F:81:E1:74
Signature algorithm name: SHA256withRSA
Subject Public Key Algorithm: 2048-bit RSA key
Version: 1
Trust this certificate? [no]:  yes
Certificate was added to keystore
GLC02DK0ZLP3XY:ssl ashish.ranjan$ ls
Readme.md               ca-cert.srl             cert-file               client.truststore.jks
ca-cert                 ca-key                  cert-signed             server.keystore.jks
GLC02DK0ZLP3XY:ssl ashish.ranjan$ 












