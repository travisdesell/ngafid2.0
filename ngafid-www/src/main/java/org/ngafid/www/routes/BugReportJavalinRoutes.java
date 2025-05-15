package org.ngafid.www.routes;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.logging.Logger;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import io.javalin.http.Handler;

public final class BugReportJavalinRoutes {

    private static final Logger LOG = Logger.getLogger(BugReportJavalinRoutes.class.getName());
    private static final Gson GSON = new Gson();

    
    public static Handler postBugReport = ctx -> {



        System.out.println("Received bug report...");
        System.out.println("Bug report body: " + ctx.body());

        var payload = ctx.bodyAsClass(BugReportPayload.class);

        if (payload.body == null || payload.body.isBlank()) {
            System.out.println("Missing description");
            ctx.status(400).result("Description is required");
            return;
        }

        String title = (payload.title == null || payload.title.isBlank())
            ? "Bug report from website"
            : payload.title;
            
        JsonObject reqBody = new JsonObject();
        reqBody.addProperty("title", title);
        reqBody.addProperty("body", payload.body);
        reqBody.add("labels", GSON.toJsonTree(new String[]{"from-website"}));

        System.out.println("GitHub issue body: " + reqBody.toString());

        String token = System.getenv("BUG_REPORT_GITHUB_TOKEN");
        if (token == null || token.isBlank()) {
            ctx.status(500).result("GitHub token not configured");
            return;
        }

        String owner = System.getenv().getOrDefault("BUG_REPORT_REPO_OWNER", "your-org");
        String repo = System.getenv().getOrDefault("BUG_REPORT_REPO", "your-site");

        HttpRequest ghReq = HttpRequest.newBuilder()
            .uri(URI.create("https://api.github.com/repos/" + owner + "/" + repo + "/issues"))
            .timeout(Duration.ofSeconds(10))
            .header("Accept", "application/vnd.github+json")
            .header("Authorization", "Bearer " + token)
            .POST(HttpRequest.BodyPublishers.ofString(reqBody.toString()))
            .build();

        System.out.println("Sending request to GitHub API: " + ghReq.toString());

        HttpClient http = HttpClient.newHttpClient();
        HttpResponse<String> ghResp = http.send(ghReq, HttpResponse.BodyHandlers.ofString());

        if (ghResp.statusCode() != 201) {
            System.out.println("GitHub API error " + ghResp.statusCode() + ": " + ghResp.body());
            ctx.status(502).result("GitHub returned " + ghResp.statusCode());
            return;
        }

        System.out.println("GitHub API response: " + ghResp.body());

        ctx.status(201).result(ghResp.body());

    };

    private static class BugReportPayload {

        public String title;
        public String body;

    }

    public static void bindRoutes(io.javalin.Javalin app) {
        app.post("/protected/submit_bug_report", postBugReport);
    }

}
