package com.hazelcast.aws.impl;

import com.hazelcast.aws.AwsConfig;
import com.hazelcast.aws.exception.AwsConnectionException;
import com.hazelcast.aws.security.Aws4RequestSigner;
import com.hazelcast.aws.utility.Environment;
import com.hazelcast.aws.utility.MetadataUtil;
import com.hazelcast.aws.utility.RetryUtils;
import com.hazelcast.config.InvalidConfigurationException;
import com.hazelcast.internal.json.Json;
import com.hazelcast.internal.json.JsonObject;
import com.hazelcast.logging.ILogger;
import com.hazelcast.logging.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.TimeZone;
import java.util.concurrent.Callable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.hazelcast.aws.impl.Constants.SIGNATURE_METHOD_V4;
import static com.hazelcast.aws.utility.MetadataUtil.IAM_SECURITY_CREDENTIALS_URI;
import static com.hazelcast.aws.utility.MetadataUtil.INSTANCE_METADATA_URI;
import static com.hazelcast.aws.utility.StringUtil.isEmpty;
import static com.hazelcast.aws.utility.StringUtil.isNotEmpty;
import static com.hazelcast.nio.IOUtil.closeResource;

/**
 *
 */
public abstract class AwsOperation<E> {
    /**
     * URI to fetch container credentials (when IAM role is enabled)
     * <p>
     * see http://docs.aws.amazon.com/AmazonECS/latest/developerguide/task-iam-roles.html
     */
    public static final String IAM_TASK_ROLE_ENDPOINT = "http://169.254.170.2";

    private static final ILogger LOGGER = Logger.getLogger(AwsOperation.class);

    protected AwsConfig awsConfig;
    protected URL endpointURL;
    protected Map<String, String> attributes = new HashMap<String, String>();

    private static final int MIN_HTTP_CODE_FOR_AWS_ERROR = 400;
    private static final int MAX_HTTP_CODE_FOR_AWS_ERROR = 600;
    private static final String UTF8_ENCODING = "UTF-8";

    protected final String service;
    protected final String docVersion;

    protected AwsOperation(AwsConfig awsConfig, URL endpointURL, String service, String docVersion) {
        this.awsConfig = awsConfig;
        this.endpointURL = endpointURL;
        this.service = service;
        this.docVersion = docVersion;
    }

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
                LOGGER.finest("Cannot get DEFAULT IAM role... CONTINUING! Exception was: " + e.getMessage());
            }
            awsConfig.setIamRole(defaultIAMRole);
        }

        if (isNotEmpty(awsConfig.getIamRole())) {
            LOGGER.finest("Using getIamRole() -- BAD");
            fillKeysFromIamRole();
        } else {
            LOGGER.finest("Using fillKeysFromIamTaskRole(...) -- GOOD!");
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

        LOGGER.finest("Getting creds from " + uri);

        String json = "";
        try {
            json = retrieveRoleFromURI(uri);
            LOGGER.finest("JSON:\n" + json + "\n");
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
        JsonObject roleAsJson = Json.parse(json).asObject();
        awsConfig.setAccessKey(roleAsJson.getString("AccessKeyId", null));
        awsConfig.setSecretKey(roleAsJson.getString("SecretAccessKey", null));
        attributes.put("X-Amz-Security-Token", roleAsJson.getString("Token", null));
        if (LOGGER.isFinestEnabled()) {
            LOGGER.finest("In parseAndStoreRoleCreds: attributes are: " + attributes.toString());
        }
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
    public E execute()
            throws Exception {
        if (isNotEmpty(awsConfig.getIamRole()) || isEmpty(awsConfig.getAccessKey())) {
            fillKeysFromIamRoles();
        }

        LOGGER.finest("OK we have credentials, signing request...");

        String signature = getRequestSigner().sign(attributes);
        E response;
        InputStream stream = null;
        attributes.put("X-Amz-Signature", signature);
        try {
            LOGGER.finest("Calling service at " + endpointURL.toExternalForm());
            stream = callServiceWithRetries();
            response = unmarshal(stream);
            return response;
        } finally {
            closeResource(stream);
        }
    }

    private InputStream callServiceWithRetries()
            throws Exception {
        return RetryUtils.retry(new Callable<InputStream>() {
            @Override
            public InputStream call() throws Exception {
                return callService();
            }
        }, awsConfig.getConnectionRetries());
    }

    // visible for testing
    abstract InputStream callService() throws Exception;

    abstract E unmarshal(InputStream stream);

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
        String timeStamp = getFormattedTimestamp();
        Aws4RequestSigner rs = new Aws4RequestSigner(awsConfig, timeStamp, service, endpointURL.getHost());
        attributes.put("Action", this.getClass().getSimpleName());
        attributes.put("Version", docVersion);
        attributes.put("X-Amz-Algorithm", SIGNATURE_METHOD_V4);
        attributes.put("X-Amz-Credential", rs.createFormattedCredential());
        attributes.put("X-Amz-Date", timeStamp);
        attributes.put("X-Amz-SignedHeaders", "host");
        attributes.put("X-Amz-Expires", "30");
        addFilters();
        return rs;
    }

    //Added for testing (mocking) purposes.
    Environment getEnvironment() {
        return new Environment();
    }
}
