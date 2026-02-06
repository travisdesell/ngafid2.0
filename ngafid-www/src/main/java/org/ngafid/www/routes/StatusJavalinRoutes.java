package org.ngafid.www.routes;

import io.javalin.http.Context;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.IntStream;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.ngafid.core.kafka.DockerServiceHeartbeat;
import org.ngafid.www.Navbar;
import org.ngafid.www.WebServer;

public class StatusJavalinRoutes {
    private static final Logger LOG = Logger.getLogger(StatusJavalinRoutes.class.getName());

    public enum ServiceStatus {
        OK,
        WARNING,
        ERROR,
        UNCHECKED,
    }

    /**
     * List of Docker service names
     */
    private static final List<String> DOCKER_SERVICES = List.of(
            "ngafid-upload-consumer", "ngafid-email-consumer", "ngafid-event-consumer", "ngafid-event-observer");

    /**
     * Maps an API route name to a systemd unit that should actually be runnin on the server.
     * <p>
     * Some of our services run multiple instances of the same unit (templated services). In this case, we will have to
     * check the status of all processes.
     */
    private static final Map<String, List<String>> SERVICE_NAME_TO_SYSTEMD_SERVICE = Map.ofEntries(
            Map.entry(
                    "flight-processing",
                    List.of("ngafid-upload-consumer@0", "ngafid-upload-consumer@1", "ngafid-upload-consumer@2")),
            Map.entry("kafka", List.of("kafka.service")),
            Map.entry("chart-service", List.of("ngafid-chart-service.service")),
            Map.entry("event-processing", List.of("ngafid-event-consumer.service")),

            // Depends on database used. In prod we use mysql
            Map.entry("database", List.of("mysqld.service")));

    /**
     * Maps service API name to a pair of the corresponding status and a long representing the nano-time of when it was fetched.
     * The time value is used for cache expiration, to prevent excessive opening of subprocesses
     */
    private static final ConcurrentHashMap<String, Pair<ServiceStatusResult, Long>> SERVICE_STATUS_CACHE =
            new ConcurrentHashMap<>();

    private static ServiceStatusResult checkSystemdService(String apiName, String serviceName) {
        // Command we are running is:
        // systemctl is-active --quiet <serviceName>
        //
        // The program will return 0 if the service is running, non-zero otherwise.
        Runtime r = Runtime.getRuntime();

        try {
            Process p = r.exec("systemctl is-active --quiet " + serviceName);
            p.waitFor();

            ServiceStatus status;
            String message;

            if (p.exitValue() == 0) {
                status = ServiceStatus.OK;
                message = "Service " + apiName + " is active.";
            } else {
                status = ServiceStatus.ERROR;
                message = "Service " + apiName + " is not active.";
            }
            return new ServiceStatusResult(status, message);
        } catch (IOException | InterruptedException e) {
            return new ServiceStatusResult(ServiceStatus.ERROR, "Encountered error: " + e.getMessage());
        }
    }

    private static ServiceStatusResult checkSystemdTemplateService(String serviceName, List<String> serviceNames) {
        Runtime r = Runtime.getRuntime();

        List<Process> procs = serviceNames.stream()
                .map(name -> {
                    try {
                        return r.exec(new String[] {"systemctl", "is-active", "--quiet", name});
                    } catch (IOException e) {
                        e.printStackTrace();
                        return null;
                    }
                })
                .toList();

        int[] exitCodes = new int[procs.size()];
        Arrays.fill(exitCodes, -1);

        try {
            for (int i = 0; i < procs.size(); i++) {
                Process proc = procs.get(i);
                proc.waitFor();
                exitCodes[i] = proc.exitValue();
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        String deadProcs = String.join(
                ",",
                IntStream.range(0, exitCodes.length)
                        .filter(exit -> exitCodes[exit] != 0)
                        .mapToObj(i -> serviceNames.get(i))
                        .toList());
        if (Arrays.stream(exitCodes).allMatch(exit -> exit == 0)) {
            return new ServiceStatusResult(ServiceStatus.OK, "All instance of service " + serviceName + " are active.");
        } else if (Arrays.stream(exitCodes).anyMatch(exit -> exit == 0)) {
            return new ServiceStatusResult(
                    ServiceStatus.WARNING,
                    "One or more instances of service " + serviceName + " are dead: " + deadProcs);
        } else {
            return new ServiceStatusResult(
                    ServiceStatus.ERROR, "All instances of service " + serviceName + " are dead: " + deadProcs);
        }
    }

    private record ServiceStatusResult(ServiceStatus status, String message, Map<String, ServiceStatus> instances) {

        // Constructor for when we don't want to include instances
        public ServiceStatusResult(ServiceStatus status, String message) {
            this(status, message, null);
        }
    }

    private static String resolveDockerLogicalService(String requestedName) {

        // Null/blank request -> null
        if (requestedName == null || requestedName.isBlank()) return null;

        final String req = requestedName.toLowerCase();

        // Accept if either contains the other (covers: prefixed names, short names, etc.)
        var matches = DOCKER_SERVICES.stream()
                .filter(logical -> {
                    String l = logical.toLowerCase();
                    return req.contains(l) || l.contains(req);
                })
                .toList();

        // No matches -> null
        if (matches.isEmpty()) return null;

        // Warn when there's an ambiguous match -> null
        if (matches.size() > 1) {
            LOG.log(Level.WARNING, "Ambiguous Docker service match for ''{0}'': {1}", new Object[] {
                requestedName, matches
            });
            return null;
        }

        return matches.getFirst();
    }

    private static ServiceStatusResult checkDockerHeartbeat(String logicalService) {

        DockerServiceHeartbeatMonitor monitor = WebServer.getMonitor();

        ServiceStatus aggregate = monitor.status(logicalService);
        var instancesMap = monitor.instanceStatuses(logicalService);

        return switch (aggregate) {
            case OK ->
                new ServiceStatusResult(
                        aggregate, "All instances of %s are healthy".formatted(logicalService), instancesMap);

            case WARNING ->
                new ServiceStatusResult(
                        aggregate, "One or more instances of %s are late".formatted(logicalService), instancesMap);

            case ERROR ->
                new ServiceStatusResult(
                        aggregate, "All instances of %s have timed‑out".formatted(logicalService), instancesMap);

            case UNCHECKED ->
                new ServiceStatusResult(
                        aggregate,
                        "Unchecked: %s (This should never happen for Docker entries!)".formatted(logicalService),
                        instancesMap);
        };
    }

    private static void getServiceStatus(Context ctx) {

        String requested = ctx.pathParam("service-name");

        // Resolve docker logical name first
        String dockerLogical = resolveDockerLogicalService(requested);
        boolean isDockerRequest = (dockerLogical != null);

        if (isDockerRequest) {

            if (!DockerServiceHeartbeat.USING_DOCKER) {
                ctx.json(new ServiceStatusResult(
                        ServiceStatus.UNCHECKED,
                        "Not running in Docker; requested Docker service '" + requested + "'"));
                return;
            }

            LOG.log(Level.INFO, "Docker service request ''{0}'' resolved to logical ''{1}''", new Object[] {
                requested, dockerLogical
            });

            ServiceStatusResult r = checkDockerHeartbeat(dockerLogical);
            ctx.json(r);
            return;
        }

        // Got non-Docker request while running in Docker -> Unchecked
        if (DockerServiceHeartbeat.USING_DOCKER) {

            ctx.json(new ServiceStatusResult(
                    ServiceStatus.UNCHECKED,
                    "Using Docker, but '" + requested + "' did not match any known Docker service"));

            return;
        }

        String service = requested.toLowerCase();

        // Check cache first
        List<String> serviceNames = SERVICE_NAME_TO_SYSTEMD_SERVICE.getOrDefault(service, null);
        if (serviceNames != null) {

            ServiceStatusResult result;
            if (serviceNames.size() == 1) result = checkSystemdService(service, serviceNames.getFirst());
            else result = checkSystemdTemplateService(service, serviceNames);

            SERVICE_STATUS_CACHE.put(service, new ImmutablePair<>(result, System.nanoTime()));
            ctx.json(result);

        } else {

            ctx.json("No service with name '" + service + "'");
        }
    }

    /**
     * Fetches status page
     */
    private static void getStatus(Context ctx) {
        final String templateFile = "status_page.html";

        Map<String, Object> scopes = Map.of("navbar_js", Navbar.getJavascript(ctx));

        ctx.header("Content-Type", "text/html; charset=UTF-8");
        ctx.render(templateFile, scopes);
    }

    public static void bindRoutes(io.javalin.Javalin app) {
        // These are non-privileged routes.
        app.get("/api/status/{service-name}", StatusJavalinRoutes::getServiceStatus);
        app.get("/status", StatusJavalinRoutes::getStatus);
    }
}
