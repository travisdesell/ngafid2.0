// ngafid-www/src/main/java/org/ngafid/www/AzureMapsProxyController.java
package org.ngafid.www;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Logger;

import org.ngafid.core.Config;

import io.javalin.Javalin;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;

public final class AzureMapsProxyController {
    private static final Logger LOG = Logger.getLogger(AzureMapsProxyController.class.getName());

    private static final String CONFIG_KEY = "ngafid.azure.maps.key";

    // Limit what the browser can request
    private static final Set<String> TILESET_WHITELIST = Set.of(
        "microsoft.imagery",
        "microsoft.base.road",
        "microsoft.base.hybrid.road"
        /* ... */
    );

    // Bounds. Adjust if you allow deeper zooms.
    private static final int MIN_Z = 0;
    private static final int MAX_Z = 20;

    // Reusable HTTP client
    private static final HttpClient HTTP = HttpClient.newBuilder()
        .version(HttpClient.Version.HTTP_2)
        .connectTimeout(Duration.ofSeconds(5))
        .build();

    private AzureMapsProxyController() { /* ... */ }

    public static void register(Javalin app) {

        LOG.info("AzureMapsProxyController - Attempting to register routes");

        final String subscriptionKey = Objects.requireNonNull(
            Config.getProperty(CONFIG_KEY),
            "Azure Maps key missing: " + CONFIG_KEY
        );

        if (subscriptionKey.isBlank())
            throw new IllegalStateException("Azure Maps key is blank: " + CONFIG_KEY);

        // Route shape matches your OL XYZ template: /api/tiles/azure/{tilesetId}/{z}/{x}/{y}.png
        app.get("/api/tiles/azure/{tilesetId}/{z}/{x}/{y}.png", ctx -> {
            try {
                handleTile(ctx, subscriptionKey);
            } catch (IllegalArgumentException e) {
                ctx.status(HttpStatus.BAD_REQUEST).result("invalid request");
            } catch (Exception e) {
                ctx.status(HttpStatus.BAD_GATEWAY).result("upstream error");
            }
        });

        LOG.info("AzureMapsProxyController - Registered routes");

    }

    private static void handleTile(Context ctx, String subscriptionKey) throws Exception {

        final String tilesetId = ctx.pathParam("tilesetId");
        final int z = parseInt(ctx.pathParam("z"));
        final int x = parseInt(ctx.pathParam("x"));
        final int y = parseInt(ctx.pathParam("y"));

        LOG.fine(
            String.format(
                "AzureMapsProxyController - Tile request: tileset=%s z=%d x=%d y=%d",
                tilesetId, z, x, y
            )
        );

        // Whitelist doesn't contain the target tileset, reject
        if (!TILESET_WHITELIST.contains(tilesetId))
            throw new IllegalArgumentException("tileset not allowed");
        
        // Target zoom is out of range, reject
        if (z < MIN_Z || z > MAX_Z)
            throw new IllegalArgumentException("z out of range");
        
        // x or y out of range, reject
        final int maxIndex = (1 << z) - 1;
        if (x < 0 || x > maxIndex || y < 0 || y > maxIndex)
            throw new IllegalArgumentException("x/y out of range");

        // Build Azure URL. Azure expects {y}, not TMS {-y}.
        String url = "https://atlas.microsoft.com/map/tile?api-version=2.0"
            + "&tilesetId=" + URLEncoder.encode(tilesetId, StandardCharsets.UTF_8)
            + "&zoom=" + z
            + "&x=" + x
            + "&y=" + y
            + "&subscription-key=" + URLEncoder.encode(subscriptionKey, StandardCharsets.UTF_8);

        HttpRequest mapImageDataRequest = HttpRequest.newBuilder(URI.create(url))
            .timeout(Duration.ofSeconds(10))
            .header("Accept", "image/png,image/jpeg")
            .GET()
            .build();

        HttpResponse<byte[]> mapImageDataResponse = HTTP.send(mapImageDataRequest, HttpResponse.BodyHandlers.ofByteArray());

        int code = mapImageDataResponse.statusCode();

        // Fetched successfully...
        if (code == 200) {

            LOG.fine(
                String.format(
                    "AzureMapsProxyController - Tile fetched: tileset=%s z=%d x=%d y=%d",
                    tilesetId, z, x, y
                )
            );

            // Set image content-type (default to PNG)
            String contentType = headerOr(mapImageDataResponse, "content-type", "image/png");

            // Cache for a day; adjust as needed or forward upstream headers
            String cache = headerOr(mapImageDataResponse, "cache-control", "public, max-age=86400, s-maxage=86400, immutable");

            ctx.status(HttpStatus.OK);
            ctx.header("Cache-Control", cache);

            // Forward ETag/Last-Modified if Azure provides them
            forwardIfPresent(ctx, mapImageDataResponse, "etag");
            forwardIfPresent(ctx, mapImageDataResponse, "last-modified");
            ctx.contentType(contentType);
            ctx.result(new ByteArrayInputStream(mapImageDataResponse.body()));

            return;

        }

        // Tile fetch failed upstream
        LOG.warning(
            String.format(
                "AzureMapsProxyController - Tile fetch failed: tileset=%s z=%d x=%d y=%d -> %d",
                tilesetId, z, x, y, code
            )
        );

        switch (code) {
            case 404 -> ctx.status(HttpStatus.NOT_FOUND).result("tile not found");
            case 401, 403 -> // Do not reveal key state
                ctx.status(HttpStatus.BAD_GATEWAY).result("authorization failed upstream");
            default -> ctx.status(HttpStatus.BAD_GATEWAY).result("upstream " + code);
        }
        
    }

    private static int parseInt(String s) {

        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("not an int: " + s);
        }

    }

    private static String headerOr(HttpResponse<?> res, String name, String fallback) {

        return res.headers()
            .firstValue(name)
            .orElse(fallback);

    }

    private static void forwardIfPresent(Context ctx, HttpResponse<?> res, String name) {

        res.headers()
            .firstValue(name)
            .ifPresent(v -> ctx.header(capitalizeHeader(name), v));

    }

    // Use canonical header names in responses
    private static String capitalizeHeader(String h) {

        String[] parts = h.split("-");
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {

            // ...
            if (i > 0)
                b.append('-');

            
            String p = parts[i];
            
            // ...
            if (p.isEmpty())
                continue;

            b.append(Character.toUpperCase(p.charAt(0)));

            // ...
            if (p.length() > 1)
                b.append(p.substring(1).toLowerCase());
            
        }

        return b.toString();

    }

}
