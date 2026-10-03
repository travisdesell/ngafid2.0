package org.ngafid.airsync

import org.ngafid.core.Database
import java.net.URL
import java.time.LocalDateTime
import java.util.*
import javax.net.ssl.HttpsURLConnection

class AirSyncAuth(val key: String, val secret: String) {
    data class AccessToken(val accessToken: String)

    val hash: ByteArray
    val accessToken: AccessToken
    var timeObtained: LocalDateTime = LocalDateTime.now()

    companion object {
        private var instance: AirSyncAuth? = null

        fun refreshInstance() {
            val query = "SELECT api_key, api_secret FROM airsync_fleet_info LIMIT 1"

            Database.getConnection().use { connection ->
                connection.prepareStatement(query).use { statement ->
                    statement.executeQuery().use { results ->
                        if (!results.next()) {
                            throw RuntimeException("Unable to find airsync credentials in airsync_fleet_info")
                        }
                        val key: String = results.getString(1)
                        val secret: String = results.getString(2)
                        instance = AirSyncAuth(key, secret)
                    }
                }
            }
        }

        fun getInstance(): AirSyncAuth {
            if (instance == null) {
                refreshInstance()
            }

            println("Bearer: ${instance?.getBearerString()}")
            println("Access: $instance")

            return instance!!
        }
    }

    init {
        val srcWord: ByteArray = ("$key:$secret").toByteArray()
        this.hash = Base64.getEncoder().encode(srcWord)
        this.accessToken = requestAuthorization()
    }

    fun getBearerString(): String = "Bearer ${accessToken.accessToken}"

    fun isOutdated(): Boolean = !LocalDateTime.now().isBefore(timeObtained.plusMinutes(60))

    /**
     * Requests authorization from the AirSync servers using the fleets information stored in the database.
     */
    private fun requestAuthorization(): AccessToken {
        val connection = URL(AirSyncEndpoints.AUTH).openConnection() as HttpsURLConnection

        connection.requestMethod = "POST"
        connection.doOutput = true
        connection.setRequestProperty("Authorization", "Basic " + String(this.hash))

        connection.inputStream.use { `is` ->
            val respRaw = `is`.readAllBytes()
            `is`.close()

            val resp = String(respRaw).replace("access_token".toRegex(), "accessToken")

            timeObtained = LocalDateTime.now()
            return Utility.OBJECT_MAPPER.readValue(
                resp,
                AccessToken::class.java,
            )
        }
    }
}
