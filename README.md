# Authenticate via Amazon Cognito and Call Amazon APIs
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


