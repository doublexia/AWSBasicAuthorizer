# AWSBasicAuthorizer
## Credentials
#### Create a user
1. Go to AWS console, IAM
2. Create a user, generate access key/secret key pair

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
3. Install requests and requests_aws4auth `pip3 install --target=. requests` and `pip3 install --target=. requests_aws4auth`
4. (Do not install boto3)
5. Create a zip file for the files under this directory: `zip -r ../callDrpil.zip *`
6. Go to AWS console, create Lambda function from the zip  using type Python 3.7
7. Or use command `aws lambda update-function-code --function-name=callDrpil --region=us-west-2 --zip-file fileb://callDrpil.zip`
8. Add secretsmanager:GetSecretValue permission into the Lambda execution role.
9. Test the Lambda function on the console.

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




