package com.hazelcast.aws.utility;

import com.hazelcast.config.InvalidConfigurationException;

import java.util.regex.Pattern;

/**
 * Helper class used to validate AWS Region.
 */
public final class RegionValidator {
    private static final Pattern AWS_REGION_PATTERN =
        Pattern.compile("\\w{2}(-gov-|-)(north|northeast|east|southeast|south|southwest|west|northwest|central)-\\d(?!.+)");

    private RegionValidator() {
    }

    public static void validateRegion(String region) {
        if (!AWS_REGION_PATTERN.matcher(region).matches()) {
            String message = String.format("The provided region %s is not a valid AWS region.", region);
            throw new InvalidConfigurationException(message);
        }
    }
}
