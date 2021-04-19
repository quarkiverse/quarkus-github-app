package io.quarkiverse.githubapp.deployment.replay;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusDevModeTest;
import io.restassured.RestAssured;

public class ReplayEventRouteAvailableInDevModeTest {

    @RegisterExtension
    public static final QuarkusDevModeTest test = new QuarkusDevModeTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addAsResource("application.properties"));

    @Test
    public void testPayloadSignatureCheck() {
        RestAssured.when().get("/replay/events")
                .then().statusCode(200);
    }
}
