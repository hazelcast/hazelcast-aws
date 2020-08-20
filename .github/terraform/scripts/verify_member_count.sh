#!/bin/bash

set -e
#set -o pipefail

EXPECTED_SIZE=$1

verify_hazelcast_cluster_size() {
    EXPECTED_SIZE=$1
    for i in `seq 1 3`; do
        if cat ~/logs/hazelcast.stdout.log | grep -q "Members {size:${EXPECTED_SIZE}" ; then
            echo "Hazelcast cluster size equal to ${EXPECTED_SIZE}"
            return 0
        else
            echo "Hazelcast cluster size NOT equal to ${EXPECTED_SIZE}!. Waiting.."
            sleep 5
        fi
    done
    return 1
}


echo "Checking Hazelcast cluster size"
verify_hazelcast_cluster_size $EXPECTED_SIZE 