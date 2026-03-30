package org.thespheres.betula.web.config;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.Readiness;

/**
 *
 * @author boris.heithecker
 */
@Readiness
@ApplicationScoped
public class InternalServicesReadyCheck implements HealthCheck {

    public static final String NAME = "ping-service-internal-endpoint-from-web";

    @Inject
    private AppConfiguration config;

    @Override
    public HealthCheckResponse call() {
        try {
            final String ping = config.getInternalClient().sendPing();
            Logger.getLogger("WEB").log(Level.INFO, ping);
            return HealthCheckResponse.up(NAME);
        } catch (Exception e) {
            e.printStackTrace(System.out);
            return HealthCheckResponse.named(NAME)
                    .withData("message", e.getLocalizedMessage())
                    .down()
                    .build();
        }
    }

}
