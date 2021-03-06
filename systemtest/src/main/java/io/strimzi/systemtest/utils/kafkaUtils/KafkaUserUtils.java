/*
 * Copyright Strimzi authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.strimzi.systemtest.utils.kafkaUtils;

import io.strimzi.api.kafka.Crds;
import io.strimzi.systemtest.Constants;
import io.strimzi.systemtest.resources.crd.KafkaUserResource;
import io.strimzi.systemtest.utils.kubeUtils.objects.SecretUtils;
import io.strimzi.test.TestUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static io.strimzi.test.k8s.KubeClusterResource.kubeClient;

public class KafkaUserUtils {

    private static final Logger LOGGER = LogManager.getLogger(KafkaUserUtils.class);

    private KafkaUserUtils() {}

    public static void waitForKafkaUserCreation(String userName) {
        LOGGER.info("Waiting for Kafka user creation {}", userName);
        SecretUtils.waitForSecretReady(userName);
        TestUtils.waitFor("Waits for Kafka user creation " + userName,
            Constants.POLL_INTERVAL_FOR_RESOURCE_READINESS, Constants.TIMEOUT_FOR_RESOURCE_READINESS,
            () -> KafkaUserResource.kafkaUserClient()
                .inNamespace(kubeClient().getNamespace())
                .withName(userName).get().getStatus().getConditions().get(0).getType().equals("Ready")
        );
        LOGGER.info("Kafka user {} created", userName);
    }

    public static void waitForKafkaUserDeletion(String userName) {
        LOGGER.info("Waiting for Kafka user deletion {}", userName);
        TestUtils.waitFor("Waits for Kafka user deletion " + userName,
            Constants.POLL_INTERVAL_FOR_RESOURCE_READINESS, Constants.TIMEOUT_FOR_RESOURCE_READINESS,
            () -> Crds.kafkaUserOperation(kubeClient().getClient()).inNamespace(kubeClient().getNamespace()).withName(userName).get() == null
        );
        LOGGER.info("Kafka user {} deleted", userName);
    }

    public static void waitForKafkaUserIncreaseObserverGeneration(long observation, String userName) {
        TestUtils.waitFor("Wait until increase observation generation from " + observation + " for user " + userName,
            Constants.GLOBAL_POLL_INTERVAL, Constants.TIMEOUT_FOR_SECRET_CREATION,
            () -> observation < KafkaUserResource.kafkaUserClient()
                .inNamespace(kubeClient().getNamespace()).withName(userName).get().getStatus().getObservedGeneration());
    }

    public static void waitForKafkaUserCreationError(String userName, String eoPodName) {
        String errorMessage = "InvalidResourceException: Users with TLS client authentication can have a username (name of the KafkaUser custom resource) only up to 64 characters long.";
        final String messageUserWasNotAdded = "User(" + kubeClient().getNamespace() + "/" + userName + "): createOrUpdate failed";
        TestUtils.waitFor("User operator has expected error", Constants.GLOBAL_POLL_INTERVAL, 60000,
            () -> {
                String logs = kubeClient().logs(eoPodName, "user-operator");
                return logs.contains(errorMessage) && logs.contains(messageUserWasNotAdded);
            });
    }

    public static void waitUntilKafkaUserStatusConditionIsPresent(String userName) {
        LOGGER.info("Waiting till kafka user name:{} is created in CRDs", userName);
        TestUtils.waitFor("Waiting for " + userName + " to be created in CRDs", Constants.GLOBAL_POLL_INTERVAL, Constants.GLOBAL_TIMEOUT,
            () -> Crds.kafkaUserOperation(kubeClient().getClient()).inNamespace(kubeClient().getNamespace()).withName(userName).get().getStatus().getConditions() != null
        );
        LOGGER.info("Kafka user name:{} is created in CRDs", userName);
    }
}
