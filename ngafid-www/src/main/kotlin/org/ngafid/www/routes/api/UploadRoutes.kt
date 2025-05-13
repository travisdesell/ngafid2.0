package org.ngafid.www.routes.api

import com.fasterxml.jackson.annotation.JsonProperty
import io.javalin.apibuilder.ApiBuilder.*
import io.javalin.config.JavalinConfig
import io.javalin.http.BadRequestResponse
import io.javalin.http.Context
import io.javalin.http.NotFoundResponse
import io.javalin.http.UnauthorizedResponse
import jakarta.servlet.MultipartConfigElement
import org.ngafid.core.Config
import org.ngafid.core.Database
import org.ngafid.core.flights.FlightError
import org.ngafid.core.flights.FlightWarning
import org.ngafid.core.flights.Tails
import org.ngafid.core.uploads.Upload
import org.ngafid.core.uploads.UploadError
import org.ngafid.www.ErrorResponse
import org.ngafid.www.routes.*
import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import java.util.logging.Level

object UploadRoutes : RouteProvider() {
    override fun bind(app: JavalinConfig) {
        app.router.apiBuilder {
            path("/api/upload") {
                get(UploadRoutes::getUploads, Role.LOGGED_IN)
                get("imported", UploadRoutes::getImported, Role.LOGGED_IN);
                post(UploadRoutes::postNewUpload, Role.LOGGED_IN, Role.UPLOADER_ONLY)

                path("{uid}") {
                    get("file", UploadRoutes::getUpload, Role.LOGGED_IN)
                    get("errors", UploadRoutes::getUploadErrors, Role.LOGGED_IN)
                    put("chunk/{cid}", UploadRoutes::putUploadChunk, Role.LOGGED_IN, Role.UPLOADER_ONLY)
                    delete(UploadRoutes::deleteUpload, Role.LOGGED_IN, Role.UPLOADER_ONLY)
                }

                RouteUtility.getStat("count") { ctx, stats -> ctx.json(stats.uploads()) }
                RouteUtility.getStat("count/success") { ctx, stats -> ctx.json(stats.uploadsOK()) }
                RouteUtility.getStat("count/warning") { ctx, stats -> ctx.json(stats.uploadsWithWarning()) }
                RouteUtility.getStat("count/error") { ctx, stats -> ctx.json(stats.uploadsWithError()) }
                RouteUtility.getStat("count/pending") { ctx, stats -> ctx.json(stats.uploadsNotImported()) }
            }
        }
    }

    class UploadsResponse(@JsonProperty var uploads: List<Upload>, @JsonProperty var numberPages: Int)

    fun getUploads(ctx: Context) {
        val user = SessionUtility.getUser(ctx)

        Database.getConnection().use { connection ->
            val currentPage = ctx.queryParam("currentPage")!!.toInt()
            val pageSize = ctx.queryParam("pageSize")!!.toInt()
            val totalUploads = Upload.getNumUploads(connection, user.fleetId, null)
            val numberPages = totalUploads / pageSize

            val uploads =
                Upload.getUploads(connection, user.fleetId, " LIMIT " + (currentPage * pageSize) + "," + pageSize)

            ctx.json(UploadsResponse(uploads, numberPages))
        }
    }

    class ImportsResponse(@JsonProperty var imports: List<Upload>, @JsonProperty var numberPages: Int)

    fun getImported(ctx: Context) {
        val user = SessionUtility.getUser(ctx)

        Database.getConnection().use { connection ->
            val currentPage = ctx.queryParam("currentPage")!!.toInt()
            val pageSize = ctx.queryParam("pageSize")!!.toInt()

            val totalImports = Upload.getNumUploads(connection, user.fleetId, null)
            val numberPages = totalImports / pageSize
            val imports = Upload.getUploads(
                connection,
                user.fleetId,
                Upload.Status.IMPORTED_SET,
                " LIMIT " + (currentPage * pageSize) + "," + pageSize
            )

            ctx.json(ImportsResponse(imports, numberPages))
        }
    }

    fun postNewUpload(ctx: Context) {
        val user = SessionUtility.getUser(ctx)
        val uploaderId = user.id
        val fleetId = user.fleetId
        val filename = ctx.formParam("filename")!!.replace(" ".toRegex(), "_")
        val identifier = ctx.formParam("identifier")
        val numberChunks = ctx.formParam("numberChunks")!!.toInt()
        val sizeBytes = ctx.formParam("sizeBytes")!!.toLong()
        val md5Hash = ctx.formParam("md5Hash")

        ctx.attribute("org.eclipse.jetty.multipartConfig", MultipartConfigElement("/mnt/ngafid/temp"))

        if (!filename.matches("^[a-zA-Z0-9_.-]*$".toRegex())) {
            ImportUploadJavalinRoutes.LOG.info("ERROR! malformed filename")

            val errorResponse = ErrorResponse(
                "File Upload Failure",
                "The filename was malformed. Filenames must only contain letters, numbers, dashes ('-'), underscores ('_') and periods."
            )

            ctx.json(errorResponse)
            return
        }

        // options:
        // 1. file does not exist, insert into database -- start upload
        // 2. file does exist and has not finished uploading -- restart upload
        // 3. file does exist and has finished uploading -- report finished
        // 4. file does exist but with different hash -- error message
        Database.getConnection().use { connection ->
            var upload = Upload.getUploadByUser(connection, uploaderId, md5Hash)
            if (upload == null) {
                upload = Upload.createNewUpload(
                    connection, uploaderId, fleetId, filename, identifier,
                    Upload.Kind.FILE, sizeBytes, numberChunks, md5Hash
                )

                ctx.json(upload)
            } else {
                // a file with this md5 hash exists
                val dbStatus = upload.getStatus()
                val dbFilename = upload.getFilename()

                if (dbStatus == Upload.Status.UPLOADED || dbStatus.isProcessed) {
                    // 3. file does exist and has finished uploading -- report finished
                    // do the same thing, client will handle completion

                    ctx.json(
                        ErrorResponse(
                            "File Already Exists",
                            ("This file has already been uploaded to the server as '" + dbFilename
                                    + "' and does not need to be uploaded again.")
                        )
                    )
                } else {
                    // 2. file does exist and has not finished uploading -- restart upload
                    ctx.json(
                        Upload.getUploadByUser(
                            connection,
                            uploaderId,
                            md5Hash
                        )
                    )
                }
            }
        }
    }

    fun getUpload(ctx: Context) {
        val user = SessionUtility.getUser(ctx)

        val uid = ctx.pathParam("uid").toInt()
        val upload: Upload? = Database.getConnection().use { connection ->
            Upload.getUploadById(connection, uid, ctx.formParam("md5Hash"))
        }

        // Upload was not found, return 404
        if (upload == null) {
            throw NotFoundResponse("Upload with id $uid not found.")
        }

        // User may have upload access but not for the fleet this upload belongs to.
        if (!user.hasUploadAccess(upload.getFleetId())) {
            throw UnauthorizedResponse("User does not have upload access to fleet of upload.")
        }

        //Build the file path
        val file = File(
            "${Config.NGAFID_ARCHIVE_DIR}/${upload.getFleetId()}/${upload.getUploaderId()}/${upload.getId()}__${upload.getFilename()}",
        )

        //File was found, attempt to send the file to the client
        if (file.exists()) {
            ctx.contentType("application/zip")
            ctx.header("Content-Disposition", "attachment; filename=" + upload.getFilename())

            BufferedInputStream(FileInputStream(file)).use { buffInputStream ->
                ctx.outputStream().use { outputStream ->
                    val buffer = ByteArray(1024)
                    var bytesRead: Int
                    while ((buffInputStream.read(buffer).also { bytesRead = it }) != -1) {
                        outputStream.write(buffer, 0, bytesRead)
                    }

                    // Successful download
                    ImportUploadJavalinRoutes.LOG.log(Level.INFO, "%s file sent", file.name)
                    ctx.status(200)
                }
            }
        } else {
            throw NotFoundResponse("No such upload with id $uid")
        }
    }

    class UploadDetails(@JsonProperty val uploadId: Int) {
        @JsonProperty
        var uploadErrors: List<UploadError>? = null

        @JsonProperty
        var flightErrors: List<FlightError>? = null

        @JsonProperty
        var flightWarnings: List<FlightWarning>? = null

        init {
            Database.getConnection().use { connection ->
                // TODO(Aaron): Noticed that this can definitely be sped up. ~45 ms currently. Will update in future PR.
                uploadErrors = UploadError.getUploadErrors(connection, uploadId)
                flightErrors = FlightError.getFlightErrors(connection, uploadId)
                flightWarnings = FlightWarning.getFlightWarnings(connection, uploadId)
            }
        }
    }

    fun getUploadErrors(ctx: Context) {
        ctx.json(UploadDetails(ctx.pathParam("uid").toInt()))
    }

    fun putUploadChunk(ctx: Context) {
        val user = SessionUtility.getUser(ctx)
        val id = ctx.pathParam("uid").toInt()
        val chunkNumber = ctx.pathParam("cid").toInt()

        Database.getConnection().use { connection ->
            val upload = Upload.getUploadById(connection, id) ?: throw NotFoundResponse("Upload with id $id not found.")

            if (upload.fleetId != user.fleetId)
                throw UnauthorizedResponse("User ${user.id} does not belong to the same fleet as upload.")

            val chunkDirectory = upload.chunkDirectory
            File(chunkDirectory).mkdirs()

            val chunkFilename = "$chunkDirectory/$chunkNumber.part"
            Files.copy(
                (ctx.uploadedFile("chunk") ?: throw BadRequestResponse("Missing file attachment")).content(),
                Paths.get(chunkFilename),
                StandardCopyOption.REPLACE_EXISTING
            )

            val chunkSize = File(chunkFilename).length()

            upload.getLockedUpload(connection).use { locked ->
                locked.chunkUploaded(chunkNumber, chunkSize)

                if (upload.completed())
                    locked.complete()
            }

            ctx.json(upload)
        }
    }

    fun deleteUpload(ctx: Context) {
        Database.getConnection().use { connection ->
            val user = SessionUtility.getUser(ctx)
            val uploadId = ctx.pathParam("uid").toInt()
            val upload = Upload.getUploadById(connection, uploadId)
                ?: throw NotFoundResponse("Upload with id $uploadId not found.")

            if (upload.getFleetId() != user.fleetId)
                throw UnauthorizedResponse("User or upload is not a part of the correct fleet.")

            upload.getLockedUpload(connection).use { locked -> locked.remove() }
            Tails.removeUnused(connection)

            ctx.json(uploadId)
        }
    }
}