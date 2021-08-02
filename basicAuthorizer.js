var AWS = require('aws-sdk'),
    region = "us-west-2",
    secretName = "callDrpilCreds",
    secret,
    decodedBinarySecret;

// Create a Secrets Manager client
var client = new AWS.SecretsManager({
    region: region
});

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
    authResponse.usageIdentifierKey = "callDrpilDelegator-Alpha;

console.log("PolicyDoc=", authResponse)

    return authResponse;
}
