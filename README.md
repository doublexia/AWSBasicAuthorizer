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
```python
from requests_aws4auth import AWS4Auth
import boto3, requests, json

def handler(event, context):
    print("event", event)
    print("context", context)

    region = 'us-west-2'
    secret_manager = boto3.client('secretsmanager', region_name = region)
    secret_string = secret_manager.get_secret_value(SecretId='drpilSecret')['SecretString']
    creds = json.loads(secret_string)

    method = 'POST'
    headers = {}
    body = event['body']
    print("Body", body)
    service = 'execute-api'
    url = 'https://8z6q03bct7.execute-api.us-west-2.amazonaws.com/Development/requestDocumentUrl'

    auth = AWS4Auth(creds['accessKey'], creds['secretKey'], region, service)
    response = requests.request(method, url, auth=auth, data=body, headers=headers)
    jsonReturn = {
        "statusCode": response.status_code,
        "isBase64Encoded":False,
        "multiValueHeaders":{"Content-Type":["application/json"]},
        "body": response.text
    }
    print("Response", jsonReturn)
    return jsonReturn
    
if __name__ == "__main__":
    handler({}, {})
```
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
```javascript
exports.handler =  function(event, context, callback) {
  const token = event.headers['Authorization'];

  console.log('Token', token)

  if (!token) return callback('Unauthorized-2')
 
  let encodedCreds = token.split(' ')[1]
  let plainCredString = Buffer.from(encodedCreds, 'base64').toString()
  console.log("Plain Cred string", plainCredString)
  //var plainCreds = (Buffer.alloc(encodedCreds.length, encodedCreds, 'base64')).toString().split(':')
  let plainCreds = plainCredString.split(':')
  console.log('Plain cred', plainCreds)
  const username = plainCreds[0]
  const password = plainCreds[1]
  
  console.log('Username', username)
  console.log('password', password)
  
  client.getSecretValue({SecretId: secretName}, function(err, data) {
    if (err) {
        if (err.code === 'DecryptionFailureException')
            // Secrets Manager can't decrypt the protected secret text using the provided KMS key.
            // Deal with the exception here, and/or rethrow at your discretion.
            throw err;
        else if (err.code === 'InternalServiceErrorException')
            // An error occurred on the server side.
            // Deal with the exception here, and/or rethrow at your discretion.
            throw err;
        else if (err.code === 'InvalidParameterException')
            // You provided an invalid value for a parameter.
            // Deal with the exception here, and/or rethrow at your discretion.
            throw err;
        else if (err.code === 'InvalidRequestException')
            // You provided a parameter value that is not valid for the current state of the resource.
            // Deal with the exception here, and/or rethrow at your discretion.
            throw err;
        else if (err.code === 'ResourceNotFoundException')
            // We can't find the resource that you asked for.
            // Deal with the exception here, and/or rethrow at your discretion.
            throw err;
    }
    else {
        // Decrypts secret using the associated KMS CMK.
        // Depending on whether the secret is a string or binary, one of these fields will be populated.
        if ('SecretString' in data) {
            var storedCreds = data.SecretString;
        } else {
            let buff = new Buffer(data.SecretBinary, 'base64');
            decodedBinarySecret = buff.toString('ascii');
        }
    }
    
    if (!storedCreds) return callback('Unauthorized-3')
    
    const jsonStoredCreds = JSON.parse(storedCreds)
    console.log("Retrieved Secret", jsonStoredCreds.user, jsonStoredCreds.passwd, "enter username/password", username, password)
    //if (token == 'Basic YWRtaW46cGFzc3dvcmQ=') {
    if (username == jsonStoredCreds.user && password == jsonStoredCreds.passwd) {
      console.log("ALLOWED")
      return callback(null, generatePolicy('user', 'Allow', event.methodArn));
    }
    else {
      console.log("DENIED")
      return callback(null, generatePolicy('user', 'Deny', event.methodArn));
    }
  })
};

// Helper function to generate an IAM policy
var generatePolicy = function(principalId, effect, resource) {
    var authResponse = {};

    authResponse.principalId = principalId;
    if (effect) {
        var policyDocument = {};
        policyDocument.Version = '2012-10-17';
        policyDocument.Statement = [];
        var statementOne = {};
        statementOne.Action = 'execute-api:Invoke';
        statementOne.Effect = effect;
        statementOne.Resource = "*";
        policyDocument.Statement[0] = statementOne;
        authResponse.policyDocument = policyDocument;
    }

    // Optional output with custom properties
    authResponse.context = {
        "userID": 1,
    };

    // Asign a usage identifier API Key if it's needed
    authResponse.usageIdentifierKey = "1C3uCXWZSQ8CJL2AbKyfY8B7sgekeI9F*****";

console.log("PolicyDoc=", authResponse)

    return authResponse;
}
```
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




