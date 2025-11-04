package org.ngafid.www.routes.api

import io.javalin.apibuilder.ApiBuilder.get
import io.javalin.apibuilder.ApiBuilder.path
import io.javalin.config.JavalinConfig
import io.javalin.http.Context
import org.ngafid.core.kafka.DockerServiceHeartbeat
import org.ngafid.www.WebServer
import org.ngafid.www.routes.Role
import org.ngafid.www.routes.RouteProvider
import org.ngafid.www.routes.StatusJavalinRoutes

object StatusApiRoutes : RouteProvider() {

    data class ServiceStatusInfo(
        val status: String,
        val message: String,
        val instances: Map<String, String>? = null
    )

    data class DockerServiceStatus(
        val status: String,
        val message: String,
        val instances: Map<String, String>
    )

    data class ServiceInfo(
        val name: String,
        val units: List<String>
    )

    override fun bind(app: JavalinConfig) {
        app.router.apiBuilder {
            path("/api/status") {
                get("all", StatusApiRoutes::getAllServicesStatus, Role.OPEN)
                get("docker", StatusApiRoutes::getDockerServicesStatus, Role.OPEN)
                get("systemd", StatusApiRoutes::getSystemdServicesStatus, Role.OPEN)
            }
        }
    }

    /**
     * List of Docker service names
     */
    private val DOCKER_SERVICES = listOf(
        "ngafid-upload-consumer",
        "ngafid-email-consumer",
        "ngafid-event-consumer",
        "ngafid-event-observer"
    )

    /**
     * Maps an API route name to systemd units
     */
    private val SYSTEMD_SERVICES = mapOf(
        "flight-processing" to listOf("ngafid-upload-consumer@0", "ngafid-upload-consumer@1", "ngafid-upload-consumer@2"),
        "kafka" to listOf("kafka.service"),
        "chart-service" to listOf("ngafid-chart-service.service"),
        "event-processing" to listOf("ngafid-event-consumer.service"),
        "database" to listOf("mysqld.service")
    )

    /**
     * Returns status for all services
     * GET /api/status/all
     */
    fun getAllServicesStatus(ctx: Context) {
        data class AllServicesStatusResponse(
            val usingDocker: Boolean,
            val dockerServices: Map<String, ServiceStatusInfo>?,
            val systemdServices: Map<String, ServiceStatusInfo>?,
            val timestamp: Long
        )

        val usingDocker = DockerServiceHeartbeat.USING_DOCKER
        val timestamp = System.currentTimeMillis()

        val dockerStatuses = if (usingDocker) {
            val monitor = WebServer.getMonitor()
            DOCKER_SERVICES.associateWith { serviceName ->
                val status = monitor.status(serviceName)
                val instances = monitor.instanceStatuses(serviceName)

                ServiceStatusInfo(
                    status = status.name,
                    message = when (status) {
                        StatusJavalinRoutes.ServiceStatus.OK -> "All instances of $serviceName are healthy"
                        StatusJavalinRoutes.ServiceStatus.WARNING -> "One or more instances of $serviceName are late"
                        StatusJavalinRoutes.ServiceStatus.ERROR -> "All instances of $serviceName have timed out"
                        StatusJavalinRoutes.ServiceStatus.UNCHECKED -> "Unchecked: $serviceName"
                    },
                    instances = instances.mapValues { it.value.name }
                )
            }
        } else {
            null
        }

        val systemdStatuses = if (!usingDocker) {
            SYSTEMD_SERVICES.mapValues { (serviceName, _) ->
                // We won't actually check systemd status here in this simplified version
                // The existing /api/status/{service-name} endpoint handles that
                ServiceStatusInfo(
                    status = "UNCHECKED",
                    message = "Use /api/status/$serviceName for detailed status",
                    instances = null
                )
            }
        } else {
            null
        }

        ctx.json(
            AllServicesStatusResponse(
                usingDocker = usingDocker,
                dockerServices = dockerStatuses,
                systemdServices = systemdStatuses,
                timestamp = timestamp
            )
        )
    }

    /**
     * Returns status for Docker services only
     * GET /api/status/docker
     */
    fun getDockerServicesStatus(ctx: Context) {
        data class DockerServicesStatusResponse(
            val usingDocker: Boolean,
            val services: Map<String, DockerServiceStatus>?,
            val timestamp: Long
        )

        val usingDocker = DockerServiceHeartbeat.USING_DOCKER

        if (!usingDocker) {
            ctx.json(
                DockerServicesStatusResponse(
                    usingDocker = false,
                    services = null,
                    timestamp = System.currentTimeMillis()
                )
            )
            return
        }

        val monitor = WebServer.getMonitor()
        val services = DOCKER_SERVICES.associateWith { serviceName ->
            val status = monitor.status(serviceName)
            val instances = monitor.instanceStatuses(serviceName)

            DockerServiceStatus(
                status = status.name,
                message = when (status) {
                    StatusJavalinRoutes.ServiceStatus.OK -> "All instances of $serviceName are healthy"
                    StatusJavalinRoutes.ServiceStatus.WARNING -> "One or more instances of $serviceName are late"
                    StatusJavalinRoutes.ServiceStatus.ERROR -> "All instances of $serviceName have timed out"
                    StatusJavalinRoutes.ServiceStatus.UNCHECKED -> "Unchecked: $serviceName"
                },
                instances = instances.mapValues { it.value.name }
            )
        }

        ctx.json(
            DockerServicesStatusResponse(
                usingDocker = true,
                services = services,
                timestamp = System.currentTimeMillis()
            )
        )
    }

    /**
     * Returns list of systemd services (for non-Docker deployments)
     * GET /api/status/systemd
     */
    fun getSystemdServicesStatus(ctx: Context) {
        data class SystemdServicesStatusResponse(
            val usingDocker: Boolean,
            val services: List<ServiceInfo>,
            val message: String
        )

        val usingDocker = DockerServiceHeartbeat.USING_DOCKER

        if (usingDocker) {
            ctx.json(
                SystemdServicesStatusResponse(
                    usingDocker = true,
                    services = emptyList(),
                    message = "System is using Docker. Use /api/status/docker instead."
                )
            )
            return
        }

        val services = SYSTEMD_SERVICES.map { (name, units) ->
            ServiceInfo(name = name, units = units)
        }

        ctx.json(
            SystemdServicesStatusResponse(
                usingDocker = false,
                services = services,
                message = "Use /api/status/{service-name} to check individual service status"
            )
        )
    }
}

