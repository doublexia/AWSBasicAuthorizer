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
