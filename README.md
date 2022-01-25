# Authenticate via Amazon Cognito and Call Amazon APIs
### Steps
Prerequisite:
1. (Amazon) Invite CEVA user in Reseller Portal.
2. (CEVA) Sign in to Reseller Portal and change password. 

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

~~
# AWSBasicAuthorizer
## Credentials
#### Create a user
1. Go to AWS console, IAM
2. Create a user, attach policy **AmazonAPIGatewayInvokeFullAccess**, generate access key/secret key pair
3. write down the user's ARN

#### Create secrets in Secret Manager
1. Go to Secret Manager on AWS console, `Store a new secret`
2. Select basic type, name it `drpilSecret`
3. Create key/value pairs
```
accessKey	<user accesskey>
secretKey	<user secret key>
```

## Lambda
#### The Code
[App.py](app.py)

#### Steps
1. Create a directory
2. Create a file app.py with above content
3. Install requests and requests_aws4auth 
   * `pip3 install --target=. requests`
   * `pip3 install --target=. requests_aws4auth`
5. (Do not install boto3)
6. Create a zip file for the files under this directory: `zip -r ../callDrpil.zip *`
7. Go to AWS console, create Lambda function from the zip  using type Python 3.7
8. Or use command `aws lambda update-function-code --function-name=callDrpil --region=us-west-2 --zip-file fileb://callDrpil.zip`
9. Add secretsmanager:GetSecretValue permission into the Lambda execution role.
10. Test the Lambda function on the console.

## Basic Authentication Authorizer
#### The code
[basicAuthorizer.js](basicAuthorizer.js)

#### Steps
1. Go to AWS console Lambda, create a function named `basicAuthorizer`
2. Pick Node.js
3. Copy/paste above code in `index.js`

## Create API from the Lambda function `callDrpil`
1. Go to AWS console API gateway
2. Create API using REST API type
3. Give it a name
4. Then create Resource, and method by attaching Lambda function
5. Deploy API
6. Create authorizer from the Authorizer Lambda, using identity source `request`
7. Update Authorization for the method in `Method Request`
8. Deploy API
9. Test using Postman.

~~

## Test AbstractTestCase
1. fill in key pair and ApiGate URL, javac AbstractTestCase.java
2. java AbstractTestCase to get a presigned url
3. upload to the presigned url
  * string: `curl -H "x-amz-server-side-encryption:AES256" --request PUT --data "<string>" --url "<presigned-url>"`
  * text file: `curl -H "x-amz-server-side-encryption:AES256" --request PUT --data @<filepath> --url "<presigned-url>"`
  * binary file: `curl -H "x-amz-server-side-encryption:AES256" --request PUT --data-binary @<filepath> --url "<presigned-url>"`


