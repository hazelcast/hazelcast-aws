package com.hazelcast.aws.impl;

import com.hazelcast.aws.AwsConfig;
import com.hazelcast.aws.exception.AwsConnectionException;
import com.hazelcast.aws.security.Aws4RequestSigner;
import com.hazelcast.aws.utility.CloudyUtility;
import com.hazelcast.aws.utility.Environment;
import com.hazelcast.aws.utility.MetadataUtil;
import com.hazelcast.aws.utility.RetryUtils;
import com.hazelcast.config.InvalidConfigurationException;
import com.hazelcast.internal.json.JsonObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.TimeZone;
import java.util.concurrent.Callable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.hazelcast.aws.impl.Constants.DOC_VERSION;
import static com.hazelcast.aws.impl.Constants.SIGNATURE_METHOD_V4;
import static com.hazelcast.aws.utility.MetadataUtil.IAM_SECURITY_CREDENTIALS_URI;
import static com.hazelcast.aws.utility.MetadataUtil.INSTANCE_METADATA_URI;
import static com.hazelcast.aws.utility.StringUtil.isEmpty;
import static com.hazelcast.aws.utility.StringUtil.isNotEmpty;
import static com.hazelcast.nio.IOUtil.closeResource;

/**
 *
 */
public abstract class AwsOperation {
    /**
     * URI to fetch container credentials (when IAM role is enabled)
     * <p>
     * see http://docs.aws.amazon.com/AmazonECS/latest/developerguide/task-iam-roles.html
     */
    public static final String IAM_TASK_ROLE_ENDPOINT = "http://169.254.170.2";

    protected AwsConfig awsConfig;
    protected String endpoint;
    protected Map<String, String> attributes = new HashMap<String, String>();

    private static final int MIN_HTTP_CODE_FOR_AWS_ERROR = 400;
    private static final int MAX_HTTP_CODE_FOR_AWS_ERROR = 600;
    private static final String UTF8_ENCODING = "UTF-8";
    private Aws4RequestSigner rs;

    /**
     * AWS response codes for client and server errors are specified here:
     * {@see http://docs.aws.amazon.com/AWSEC2/latest/APIReference/errors-overview.html}.
     */
    private static boolean isAwsError(int responseCode) {
        return responseCode >= MIN_HTTP_CODE_FOR_AWS_ERROR && responseCode < MAX_HTTP_CODE_FOR_AWS_ERROR;
    }

    private static String extractErrorMessage(HttpURLConnection httpConnection) {
        InputStream errorStream = httpConnection.getErrorStream();
        if (errorStream == null) {
            return "";
        }
        return readFrom(errorStream);
    }

    private static String readFrom(InputStream stream) {
        Scanner scanner = new Scanner(stream, UTF8_ENCODING).useDelimiter("\\A");
        return scanner.hasNext() ? scanner.next() : "";
    }

    void fillKeysFromIamRoles()
            throws IOException {
        if (isEmpty(awsConfig.getIamRole()) || "DEFAULT".equals(awsConfig.getIamRole())) {
            String defaultIAMRole = null;
            try {
                defaultIAMRole = getDefaultIamRole();
            } catch (Throwable e) {
                System.out.println("Cannot get DEFAULT IAM role... CONTINUING! Exception was: " + e.getMessage());
            }
            awsConfig.setIamRole(defaultIAMRole);
        }

        if (isNotEmpty(awsConfig.getIamRole())) {
            System.out.println("Using getIamRole() -- BAD");
            fillKeysFromIamRole();
        } else {
            System.out.println("Using fillKeysFromIamTaskRole(...) -- GOOD!");
            fillKeysFromIamTaskRole(getEnvironment());
        }

    }

    private String getDefaultIamRole()
            throws IOException {
        String uri = INSTANCE_METADATA_URI.concat(IAM_SECURITY_CREDENTIALS_URI);
        return retrieveRoleFromURI(uri);
    }

    private void fillKeysFromIamRole() {
        try {
            String query = IAM_SECURITY_CREDENTIALS_URI.concat(awsConfig.getIamRole());
            String uri = INSTANCE_METADATA_URI.concat(query);
            String json = retrieveRoleFromURI(uri);
            parseAndStoreRoleCreds(json);
        } catch (Exception io) {
            throw new InvalidConfigurationException("Unable to retrieve credentials from IAM Role: " + awsConfig.getIamRole(),
                    io);
        }
    }

    private void fillKeysFromIamTaskRole(Environment env)
            throws IOException {
        // before giving up, attempt to discover whether we're running in an ECS Container,
        // in which case, AWS_CONTAINER_CREDENTIALS_RELATIVE_URI will exist as an env var.
        String uri = env.getEnvVar(Constants.ECS_CREDENTIALS_ENV_VAR_NAME);
        if (uri == null) {
            throw new IllegalArgumentException("Could not acquire credentials! "
                    + "Did not find declared AWS access key or IAM Role, and could not discover IAM Task Role or default role.");
        }
        uri = IAM_TASK_ROLE_ENDPOINT + uri;

        System.out.println("Getting creds from " + uri);

        String json = "";
        try {
            json = retrieveRoleFromURI(uri);
            System.out.println("JSON:\n" + json + "\n");
            parseAndStoreRoleCreds(json);
        } catch (Exception io) {
            throw new InvalidConfigurationException(
                    "Unable to retrieve credentials from IAM Task Role. " + "URI: " + uri + ". \n HTTP Response content: " + json,
                    io);
        }
    }

    /**
     * This is a helper method that simply performs the HTTP request to retrieve the role, from a given URI.
     * (It allows us to cleanly separate the network calls out of our main code logic, so we can mock in our UT.)
     *
     * @param uri the full URI where a `GET` request will retrieve the role information, represented as JSON.
     * @return The content of the HTTP response, as a String. NOTE: This is NEVER null.
     */
    String retrieveRoleFromURI(String uri) {
        return MetadataUtil
                .retrieveMetadataFromURI(uri, awsConfig.getConnectionTimeoutSeconds(), awsConfig.getConnectionRetries());
    }

    /**
     * This helper method is responsible for just parsing the content of the HTTP response and
     * storing the access keys and token it finds there.
     *
     * @param json The JSON representation of the IAM (Task) Role.
     */
    private void parseAndStoreRoleCreds(String json) {
        JsonObject roleAsJson = JsonObject.readFrom(json);
        awsConfig.setAccessKey(roleAsJson.getString("AccessKeyId", null));
        awsConfig.setSecretKey(roleAsJson.getString("SecretAccessKey", null));
        attributes.put("X-Amz-Security-Token", roleAsJson.getString("Token", null));
        System.out.println("In parseAndStoreRoleCreds: attributes are: " + attributes.toString());
    }

    /**
     * @param reader The reader that gives access to the JSON-formatted content that includes all the role information.
     * @return A map with all the parsed keys and values from the JSON content.
     * @throws IOException In case the input from reader cannot be correctly parsed.
     * @deprecated Since we moved JSON parsing from manual pattern matching to using
     * `com.hazelcast.com.eclipsesource.json.JsonObject`, this method should be deprecated.
     */
    @Deprecated
    public Map<String, String> parseIamRole(BufferedReader reader)
            throws IOException {
        Map<String, String> map = new HashMap<String, String>();
        Pattern keyPattern = Pattern.compile("\"(.*?)\" : ");
        Pattern valuePattern = Pattern.compile(" : \"(.*?)\",");
        String line;
        for (line = reader.readLine(); line != null; line = reader.readLine()) {
            if (line.contains(":")) {
                Matcher keyMatcher = keyPattern.matcher(line);
                Matcher valueMatcher = valuePattern.matcher(line);
                if (keyMatcher.find() && valueMatcher.find()) {
                    String key = keyMatcher.group(1);
                    String value = valueMatcher.group(1);
                    map.put(key, value);
                }
            }
        }
        return map;
    }

    private String getFormattedTimestamp() {
        SimpleDateFormat df = new SimpleDateFormat(Constants.DATE_FORMAT);
        df.setTimeZone(TimeZone.getTimeZone("UTC"));
        return df.format(new Date());
    }

    /**
     * Add available filters to narrow down the scope of the query
     */
    private void addFilters() {
        Filter filter = new Filter();
        if (isNotEmpty(awsConfig.getTagKey())) {
            if (isNotEmpty(awsConfig.getTagValue())) {
                filter.addFilter("tag:" + awsConfig.getTagKey(), awsConfig.getTagValue());
            } else {
                filter.addFilter("tag-key", awsConfig.getTagKey());
            }
        } else if (isNotEmpty(awsConfig.getTagValue())) {
            filter.addFilter("tag-value", awsConfig.getTagValue());
        }

        if (isNotEmpty(awsConfig.getSecurityGroupName())) {
            filter.addFilter("instance.group-name", awsConfig.getSecurityGroupName());
        }

        filter.addFilter("instance-state-name", "running");
        attributes.putAll(filter.getFilters());
    }

    /**
     * Invoke the service to describe the instances, unmarshal the response and return the discovered node map.
     * The map contains mappings from private to public IP and all contained nodes match the filtering rules defined by
     * the {@link #awsConfig}.
     *
     * @return map from private to public IP or empty map in case of failed response unmarshalling
     * @throws Exception if there is an exception invoking the service
     */
    public Map<String, String> execute()
            throws Exception {
        if (isNotEmpty(awsConfig.getIamRole()) || isEmpty(awsConfig.getAccessKey())) {
            fillKeysFromIamRoles();
        }

        System.out.println("OK we have credentials, signing request...");

        String signature = getRequestSigner().sign("ecs", attributes); // FIXME ec2 OR ecs
        Map<String, String> response;
        InputStream stream = null;
        attributes.put("X-Amz-Signature", signature);
        try {
            System.out.println("Calling service at " + endpoint);
            stream = callServiceWithRetries(endpoint);
            response = CloudyUtility.unmarshalTheResponse(stream);
            return response;
        } finally {
            closeResource(stream);
        }
    }

    private InputStream callServiceWithRetries(final String endpoint)
            throws Exception {
        return RetryUtils.retry(new Callable<InputStream>() {
            @Override
            public InputStream call()
                    throws Exception {
                return callService(endpoint);
            }
        }, awsConfig.getConnectionRetries());
    }

    // visible for testing
    abstract InputStream callService(String endpoint)
            throws Exception;

    // visible for testing
    void checkNoAwsErrors(HttpURLConnection httpConnection)
            throws IOException {
        int responseCode = httpConnection.getResponseCode();
        if (isAwsError(responseCode)) {
            String errorMessage = extractErrorMessage(httpConnection);
            throw new AwsConnectionException(responseCode, errorMessage);
        }
    }

    public Aws4RequestSigner getRequestSigner() {
        if (null == rs) {
            String timeStamp = getFormattedTimestamp();
            rs = new Aws4RequestSigner(awsConfig, timeStamp, endpoint);
            attributes.put("Action", this.getClass().getSimpleName());
            attributes.put("Version", DOC_VERSION);
            attributes.put("X-Amz-Algorithm", SIGNATURE_METHOD_V4);
            attributes.put("X-Amz-Credential", rs.createFormattedCredential("ec2")); // FIXME ecs ec2
            attributes.put("X-Amz-Date", timeStamp);
            attributes.put("X-Amz-SignedHeaders", "host");
            attributes.put("X-Amz-Expires", "30");
            addFilters();
        }
        return rs;
    }

    //Added for testing (mocking) purposes.
    Environment getEnvironment() {
        return new Environment();
    }
}
