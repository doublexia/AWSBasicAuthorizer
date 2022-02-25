# Authenticate via Amazon Cognito and Call Amazon APIs
## Use standalone Java app
Download [CallHdssViaCognito.jar](https://github.com/doublexia/AWSBasicAuthorizer/blob/main/CallHdssViaCognito.jar).
1. To see usage, run `java -jar CallHdssViaCognito.jar`
2. Usage: 
```
java -jar CallHdssViaCognito.jar -s stage -u user -p password -f file_to_upload -m metadata
```
where stage can be either beta or prod, file_to_upload is a file path and metadata is in Json format.

3. Sample usage: 
```
java -jar CallHdssViaCognito.jar -s beta -u testcarrier -p actual_password -f /Users/Username/Downloads/test-doc.pdf -m '{
    "trackingNumber": "TestProNumber",
    "returnId": "TestReturnId",
    "shipmentRequestId": "Shipment-Request-Id",
    "filename": "test-doc.pdf",
    "type": "POD",
    "description": "Test PDF attachment",
    "mimeType": "application/pdf",
    "carrierName": "TestCarrierName",
    "carrierReferenceNumber": "Carrier-Reference-#",
    "locationId": "US",
    "userId": "TestUser",
    "tenantId": "ARPOD"
}' 
```
4. Meta format:
  * trackingNumber: __required__,the shipment tracking number, aka, HAWB#, PRO#
  * returnId: optional, the associated return id
  * shipmentRequestId: optional, the associated shipment request id
  * filename: __required__, the document file name
  * type: __required__, the document type, POD, BOL, etc.
  * description: optional, the description about the document
  * mimeType: optional, the mime type , for example, application/pdf
  * carrierName: __rquired__, the carrier name
  * carrierReferenceNumber: optioanl, the carrier's reference number
  * locationId: __required__, the location identifier, can be generic as a country name or specific as a branch name
  * userId: __required__, the user/operator id
  * tenantId: __required__, must be ARPOD

## Create your own app or embed in your existing app
### Steps
Prerequisite:
1. Carrier tells Amazon their email(s) that will be used for account creation in Reseller Portal.
1. (Amazon) Invites Carrier, eg., CEVA user in Reseller Portal.
2. (Carrier) Signs in to Reseller Portal and changes password. 

PoC:
1. Create a Maven project
2. Use build:
```
<build>
        <plugins>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-compiler-plugin</artifactId>
                <configuration>
                    <source>1.8</source>
                    <target>1.8</target>
                </configuration>
            </plugin>
        </plugins>
    </build>
```
3. Add dependencies:
```
<dependencies>
        <dependency>
            <groupId>com.amazonaws</groupId>
            <artifactId>aws-java-sdk-cognitoidentity</artifactId>
            <version>1.12.74</version>
        </dependency>

        <dependency>
            <groupId>org.json</groupId>
            <artifactId>json</artifactId>
            <version>20210307</version>
        </dependency>
</dependencies>
```
4. Download [AuthenticationHelper](https://github.com/doublexia/aws-cognito-java-desktop-app/blob/master/src/main/java/com/amazonaws/sample/cognitoui/AuthenticationHelper.java) and add it into the project.
5. Download [CallHDSS](CallHDSS.java) and [TestCognito](TestCognito.java) and add them into the project.
6. Edit TestCognito.
7. Run TestCognito.



## Test AbstractTestCase
1. fill in key pair and ApiGate URL, javac AbstractTestCase.java
2. java AbstractTestCase to get a presigned url
3. upload to the presigned url
  * string: `curl -H "x-amz-server-side-encryption:AES256" --request PUT --data "<string>" --url "<presigned-url>"`
  * text file: `curl -H "x-amz-server-side-encryption:AES256" --request PUT --data @<filepath> --url "<presigned-url>"`
  * binary file: `curl -H "x-amz-server-side-encryption:AES256" --request PUT --data-binary @<filepath> --url "<presigned-url>"`


