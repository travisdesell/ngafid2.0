package org.ngafid.www.routes;

import io.javalin.http.Context;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

public class StatusJavalinRoutes {

    public enum ServiceStatus {
        OK,
        WARNING,
        ERROR
    }

    /**
     * Maps an API route name to a systemd unit that should actually be runnin on the server.
     * <p>
     * Some of our services run multiple instances of the same unit (templated services). In this case, we will have to
     * check the status of all processes.
     */
    private static final Map<String, List<String>> SERVICE_NAME_TO_SYSTEMD_SERVICE = Map.ofEntries(
            Map.entry("flight-processing", List.of("ngafid-upload-consumer@0", "ngafid-upload-consumer@1", "ngafid-upload-consumer@2")),
            Map.entry("kafka", List.of("kafka.service")),
            Map.entry("chart-service", List.of("ngafid-chart-service.service")),

            // Depends on database used. In prod we use mysql
            Map.entry("db", List.of("mysqld.service"))
    );

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

        List<Process> procs = serviceNames.stream().map(name -> {
            try {
                return r.exec(new String[]{"systemctl", "is-active", "--quiet", name});
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        }).toList();

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

        String deadProcs = String.join(",", IntStream.range(0, exitCodes.length).filter(exit -> exitCodes[exit] != 0).mapToObj(i -> serviceNames.get(i)).toList());
        if (Arrays.stream(exitCodes).allMatch(exit -> exit == 0)) {
            return new ServiceStatusResult(ServiceStatus.OK, "All instance of service " + serviceName + " are active.");
        } else if (Arrays.stream(exitCodes).anyMatch(exit -> exit == 0)) {
            return new ServiceStatusResult(ServiceStatus.WARNING, "One or more instances of service " + serviceName + " are dead: " + deadProcs);
        } else {
            return new ServiceStatusResult(ServiceStatus.ERROR, "All instances of service " + serviceName + " are dead: " + deadProcs);
        }
    }

    private record ServiceStatusResult(ServiceStatus status, String message) {
    }

    private static void getStatus(Context ctx) {
        String service = ctx.pathParam("service-name");

        List<String> serviceNames = SERVICE_NAME_TO_SYSTEMD_SERVICE.getOrDefault(service, null);
        if (serviceNames != null) {
            if (serviceNames.size() == 1) {
                ctx.json(checkSystemdService(service, serviceNames.get(0)));
            } else {
                ctx.json(checkSystemdTemplateService(service, serviceNames));
            }
        } else {
            ctx.json("No service with name '" + service + "'");
        }
    }

    public static void bindRoutes(io.javalin.Javalin app) {
        app.get("/status/{service-name}", StatusJavalinRoutes::getStatus);
    }
}
