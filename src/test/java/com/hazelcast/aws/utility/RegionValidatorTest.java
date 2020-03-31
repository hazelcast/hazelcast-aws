package com.hazelcast.aws.utility;

import com.hazelcast.config.InvalidConfigurationException;
import org.junit.Test;

import static com.hazelcast.test.HazelcastTestSupport.assertThrows;
import static org.junit.Assert.assertEquals;

public class RegionValidatorTest {
    @Test
    public void validateValidRegion() {
        RegionValidator.validateRegion("us-west-1");
        RegionValidator.validateRegion("us-gov-east-1");
    }

    @Test
    public void validateInvalidRegion() {
        // given
        String region = "us-wrong-1";
        String expectedMessage = String.format("The provided region %s is not a valid AWS region.", region);

        //when
        Runnable validateRegion = () -> RegionValidator.validateRegion(region);

        //then
        InvalidConfigurationException thrownEx = assertThrows(InvalidConfigurationException.class, validateRegion);
        assertEquals(expectedMessage, thrownEx.getMessage());
    }

    @Test
    public void validateInvalidGovRegion() {
        // given
        String region = "us-gov-wrong-1";
        String expectedMessage = String.format("The provided region %s is not a valid AWS region.", region);

        // when
        Runnable validateRegion = () -> RegionValidator.validateRegion(region);

        //then
        InvalidConfigurationException thrownEx = assertThrows(InvalidConfigurationException.class, validateRegion);
        assertEquals(expectedMessage, thrownEx.getMessage());
    }

}