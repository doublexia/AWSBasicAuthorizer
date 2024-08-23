## Use standalone Java app

Download [CallHdssViaCognito.jar](https://github.com/doublexia/AWSBasicAuthorizer/blob/main/CallHdssViaCognito.jar).

To see usage, run java -jar CallHdssViaCognito.jar

Usage:
```
java -jar CallHdssViaCognito.jar -a -s stage -u user -p password -f file_to_upload -m metadata
```
where stage can be either beta or prod, file_to_upload is a file path and metadata is in Json format.

Sample usage:

(Linux, MacOs)
```
java -jar CallHdssViaCognito.jar -s beta -u testcarrier -p actual_password -f /Users/Username/Downloads/test-doc.pdf -m '{
  "proNumber": "<proNumber-or-trackingId>",
  "filename": "<doc-filename.pdf>",
  "type": "POD/BOL",
  "description": "<doc description>",
  "mimeType": "application/pdf",
  "requestSource": "<carrierName>",
  "locationId": "<country-or-city-such-as-US>",
  "userId": "<user-id>"
}’
```

(Windows)
```
java -jar CallHdssViaCognito.jar -s devo -u testcarrier -p actual_password -f "path_of_file_to_be_uploaded" -m "{\"proNumber\": \"<proNumber-or-trackingId>\", \"filename\": \"<doc-filename.pdf>\", \"type\": \"POD/BOL\", \"description\": \"<doc description>\", \"mimeType\": \"application/pdf\", \"requestSource\": \"<carrierName>\", \"locationId\": \"<country-or-city-such-as-US>\", \"userId\": \"<user-id>\" }" 
```

### Metadata fields:
proNumber: required,the shipment tracking number, aka, HAWB#, PRO#
filename: required, the document file name
type: required, the document type, POD, BOL, etc.
description: optional, the description about the document
mimeType: optional, the mime type, for example, application/pdf
requestSource: required, the carrier name
locationId: required, the location identifier, can be generic as a country name or specific as a branch name
userId: required, the user/operator id
