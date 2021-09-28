import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.callhdssviacognito.helper.AuthenticationHelper;

import com.amazonaws.auth.AWSSessionCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.AnonymousAWSCredentials;
import com.amazonaws.auth.BasicSessionCredentials;
import com.amazonaws.services.cognitoidentity.AmazonCognitoIdentity;
import com.amazonaws.services.cognitoidentity.AmazonCognitoIdentityClientBuilder;
import com.amazonaws.services.cognitoidentity.model.GetCredentialsForIdentityRequest;
import com.amazonaws.services.cognitoidentity.model.GetCredentialsForIdentityResult;
import com.amazonaws.services.cognitoidentity.model.GetIdRequest;
import com.amazonaws.services.cognitoidentity.model.GetIdResult;
import com.amazonaws.services.cognitoidp.model.NotAuthorizedException;

public class TestCognito {
    protected static final String URL_PREFIX = "url";

    private static Map<String, PoolDetails> stageToPoolMap= new HashMap<>();
    static {
        stageToPoolMap.put(
            "beta",
            new PoolDetails("user-pool-id",
                "identity-pool-id",
                "client-id",
                "region")
        );

    }

    private long lastCachedTimestamp;
    private String cachedJWT;
    private String cachedIdentityId;

    public static void main(String[] args) {
        TestCognito testCognito = new TestCognito();

        try {
            AWSSessionCredentials credentials = testCognito.authenticate(
                stageToPoolMap.get("beta"),
                "user-name",
                "passwd"
            );

            String response = new CallHDSS().post(URL_PREFIX,
                "requestDocumentUrl",
                "{\n" + "    \"name\":\"name\",\n" + "    \"type\":\"type\",\n" + "    \"keys\": [\n" +
                    "        {\"key\": \"RMA2\",\"value\":\"RRabcdefgh\"},\n" +
                    "        {\"key\":\"SHP2\", \"value\":\"SO12345678\"}\n" + "        ],\n" +
                    "        \"description\": \"description of this document\",\n" +
                    "        \"requestSource\": \"SOURCE\",\n" + "        \"locationId\":\"LocationID\",\n" +
                    "        \"userId\": \"MyName\"\n" + "}\n",
                credentials.getAWSAccessKeyId(),
                credentials.getAWSSecretKey(),
                credentials.getSessionToken()
            );
            System.out.println("Response = " + response);
        } catch (NotAuthorizedException e) {
            System.out.println("Exception =" + e.getMessage());
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public TestCognito() {
    }


    public AWSSessionCredentials authenticate(PoolDetails poolDetails, String userName, String password) throws Exception {
        cacheJWTAndIdentityId(poolDetails, userName, password);

        String idpUrl = getIdpUrl(poolDetails);

        GetCredentialsForIdentityRequest request = new GetCredentialsForIdentityRequest();
        request.setIdentityId(cachedIdentityId);
        request.addLoginsEntry(idpUrl, cachedJWT);

        AmazonCognitoIdentity provider = createCognitoItentity(poolDetails);

        GetCredentialsForIdentityResult result = provider.getCredentialsForIdentity(request);

        // Create the session credentials object
        AWSSessionCredentials sessionCredentials = new BasicSessionCredentials(
            result.getCredentials().getAccessKeyId(),
            result.getCredentials().getSecretKey(),
            result.getCredentials().getSessionToken()
        );

        System.out.println("SessionCredentials=("+ sessionCredentials.getAWSAccessKeyId() + ", " + sessionCredentials.getAWSSecretKey() +")");

        return sessionCredentials;
    }

    private void cacheJWTAndIdentityId(PoolDetails poolDetails, String userName, String password) {
        if (cachedJWT == null || cachedIdentityId == null || System.currentTimeMillis() - lastCachedTimestamp > TimeUnit.HOURS.toMillis(1)) {
            lastCachedTimestamp = System.currentTimeMillis();

            AuthenticationHelper helper = new AuthenticationHelper(poolDetails.userPoolId, poolDetails.clientId, null, poolDetails.region);

            String jwt = helper.PerformSRPAuthentication(userName, password);
            System.out.println("JWT="+ jwt);

            AmazonCognitoIdentity provider = createCognitoItentity(poolDetails);

            //get the identity id using the login map
            String idpUrl = getIdpUrl(poolDetails);
            GetIdRequest idRequest = new GetIdRequest();
            idRequest.setIdentityPoolId(poolDetails.identityPoolId);
            idRequest.addLoginsEntry(idpUrl, jwt);

            //use the provider to make the id request
            GetIdResult idResult = provider.getId(idRequest);
            System.out.println("IDENTITY ID="+ idResult.getIdentityId());

            cachedJWT = jwt;
            cachedIdentityId = idResult.getIdentityId();
        }
    }

    private String getIdpUrl(PoolDetails poolDetails) {
        return String.format("cognito-idp.%s.amazonaws.com/%s", poolDetails.region, poolDetails.userPoolId);
    }

    private AmazonCognitoIdentity createCognitoItentity(PoolDetails poolDetails) {
        //create a Cognito provider with anonymous creds
        AnonymousAWSCredentials awsCreds2 = new AnonymousAWSCredentials();
        AmazonCognitoIdentity provider = AmazonCognitoIdentityClientBuilder
            .standard()
            .withCredentials(new AWSStaticCredentialsProvider(awsCreds2))
            .withRegion(com.amazonaws.regions.Regions.fromName(poolDetails.region))
            .build();
        return provider;
    }

    static class PoolDetails {
        private String userPoolId;
        private String clientId;
        private String identityPoolId;
        private String region;

        public PoolDetails(String userPoolId, String idPoolId, String clientId, String region) {
            this.userPoolId = userPoolId;
            this.identityPoolId = idPoolId;
            this.clientId = clientId;
            this.region = region;
        }
    }
}
