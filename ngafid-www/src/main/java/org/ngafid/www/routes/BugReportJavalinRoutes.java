package org.ngafid.www.routes;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.ngafid.core.accounts.EmailType;
import org.ngafid.core.accounts.User;
import org.ngafid.core.util.SendEmail;
import static org.ngafid.www.HttpCodes.BAD_REQUEST;
import static org.ngafid.www.HttpCodes.INTERNAL_SERVER_ERROR;
import static org.ngafid.www.HttpCodes.OK;
import org.ngafid.www.Navbar;
import static org.ngafid.www.WebServer.gson;

import io.javalin.http.Context;


@SuppressWarnings("LoggerStringConcat")
public final class BugReportJavalinRoutes {

    private static final Logger LOG = Logger.getLogger(BugReportJavalinRoutes.class.getName());

    /* Bug Report Submission (GitHub) */
    /*
        public static void postBugReportGitHub(Context ctx) throws Exception {


            LOG.info("Received bug report...");
            LOG.info("Bug report body: " + ctx.body());
            
            var payload = ctx.bodyAsClass(BugReportPayload.class);
            
            if (payload.body == null || payload.body.isBlank()) {
                
                LOG.severe("Missing bug report description");
                ctx.status(BAD_REQUEST).result("Description is required");
                return;
                
            }
            
            String title = (payload.title == null || payload.title.isBlank())
                    ? "Bug report from website"
                    : payload.title;
            
            JsonObject reqBody = new JsonObject();
            reqBody.addProperty("title", title);
            reqBody.addProperty("body", payload.body);
            reqBody.add("labels", GSON.toJsonTree(new String[]{"from-website"}));
            
            LOG.info("GitHub issue body: " + reqBody.toString());
            
            String token = System.getenv("BUG_REPORT_GITHUB_TOKEN");
            
            //Failed to get the GitHub token, display error
            if (token == null || token.isBlank()) {
                
                ctx.status(INTERNAL_SERVER_ERROR).result("GitHub token not configured");
                return;
                
            }
            
            String owner = System.getenv().getOrDefault("BUG_REPORT_REPO_OWNER", "unset-repo-owner");
            String repo = System.getenv().getOrDefault("BUG_REPORT_REPO", "unset-repo");
            
            final int TIMEOUT_DURATION_S = 10;
            HttpRequest ghReq = HttpRequest.newBuilder()
                .uri(URI.create("https://api.github.com/repos/" + owner + "/" + repo + "/issues"))
                .timeout(Duration.ofSeconds(TIMEOUT_DURATION_S))
                .header("Accept", "application/vnd.github+json")
                .header("Authorization", "Bearer " + token)
                .POST(HttpRequest.BodyPublishers.ofString(reqBody.toString()))
                .build();
            
            LOG.info("Sending request to GitHub API: " + ghReq.toString());
            
            HttpClient http = HttpClient.newHttpClient();
            HttpResponse<String> githubResponse = http.send(ghReq, HttpResponse.BodyHandlers.ofString());
            
            //Failed to create the new issue, display error
            if (githubResponse.statusCode() != CREATED) {
                
                LOG.severe("GitHub API error " + githubResponse.statusCode() + ": " + githubResponse.body());
                ctx.status(BAD_GATEWAY).result("GitHub returned " + githubResponse.statusCode());
                return;
                
            }
            
            LOG.info("GitHub API response: " + githubResponse.body());
            
            ctx.status(CREATED).result(githubResponse.body());
            
        };
    */


    /* Bug Report Submission (Email) */
    public static void postBugReportEmail(Context ctx) throws Exception {

        LOG.info("Received bug report...");
        LOG.info("Bug report data: " + ctx.body());

        var payload = ctx.bodyAsClass(BugReportPayload.class);

        //Got null payload, report error
        if (payload == null) {

            LOG.warning("Received null payload");
            ctx.status(BAD_REQUEST).result("Payload is required").json(BAD_REQUEST);
            return;

        }

        //Missing title, report error
        if (payload.title == null || payload.title.isBlank()) {

            LOG.warning("Missing bug report title");
            ctx.status(BAD_REQUEST).result("Title is required").json(BAD_REQUEST);
            return;

        }

        //Missing body, report error
        if (payload.body == null || payload.body.isBlank()) {

            LOG.warning("Missing bug report description");
            ctx.status(BAD_REQUEST).result("Description is required").json(BAD_REQUEST);
            return;

        }

        String title = payload.title;
        String body = payload.body;
        String subject = "NGAFID Bug Report: " + title;

        //Use env variable 'NGAFID_ADMIN_EMAILS' as the recipient
        String recipientEmail = System.getenv("NGAFID_ADMIN_EMAILS");
        if (recipientEmail == null || recipientEmail.isBlank()) {

            ctx.status(INTERNAL_SERVER_ERROR).result("Admin email not configured, aborting bug report").json(INTERNAL_SERVER_ERROR);
            return;

        }
        
        //Use the email address as the recipient
        List<String> recipients = List.of(recipientEmail);

        //Use no BCC recipients
        List<String> bccRecipients = List.of();

        LOG.info("Attempting to send bug report email to '" + recipientEmail + "'...");

        //Attempt to send the email
        try {
        
            SendEmail.sendEmail(recipients, bccRecipients, subject, body, EmailType.BUG_REPORT);

        } catch (SQLException e) {

            LOG.severe("Failed to send bug report email: " + e.getMessage());
            ctx.status(INTERNAL_SERVER_ERROR).result("Failed to send bug report email (SQL Exception)").json(e.getMessage());
            return;

        } catch (Exception e) {

            LOG.severe("Failed to send bug report email: " + e.getMessage());
            ctx.status(INTERNAL_SERVER_ERROR).result("Failed to send bug report email (Unknown Exception)").json(e.getMessage());
            return;

        }

        //Email sent successfully, report success
        LOG.info("Sent bug report email to " + recipientEmail);
        ctx.status(OK).result("Bug report sent to " + recipientEmail).json(OK);

    };


    private static class BugReportPayload {

        public String title;
        public String body;

    }


    /* Bug Report Page */
    private static void getBugReport(Context ctx) {

        final String templateFile = "bug_report_page.html";

        //Inject the navbar
        final Map<String, Object> scopes = new HashMap<>();
        scopes.put("navbar_js", Navbar.getJavascript(ctx));

        //Inject user information (to get their email)
        final User user = ctx.sessionAttribute("user");
        scopes.put("user_js", "var user = JSON.parse('" + gson.toJson(user) + "');");

        ctx.header("Content-Type", "text/html; charset=UTF-8");
        ctx.render(templateFile, scopes);

    }


    public static void bindRoutes(io.javalin.Javalin app) {

        //Bug Report Submission
        /* app.post("/protected/submit_bug_report_github", BugReportJavalinRoutes::postBugReportGitHub); */
        app.post("/protected/submit_bug_report_email", BugReportJavalinRoutes::postBugReportEmail);

        //Bug Report Page
        app.get("/protected/bug_report", BugReportJavalinRoutes::getBugReport);

    }

}

