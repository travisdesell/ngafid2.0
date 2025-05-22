package org.ngafid.www.routes;

import io.javalin.http.Context;
import org.ngafid.core.Config;
import org.ngafid.core.accounts.EmailType;
import org.ngafid.core.accounts.User;
import org.ngafid.core.util.SendEmail;
import org.ngafid.www.Navbar;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import static org.ngafid.www.HttpCodes.*;
import static org.ngafid.www.WebServer.gson;


@SuppressWarnings("LoggerStringConcat")
public final class BugReportJavalinRoutes {

    private static final Logger LOG = Logger.getLogger(BugReportJavalinRoutes.class.getName());

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
        String senderEmail = payload.senderEmail;
        boolean includeEmail = payload.includeEmail;
        String subject = "NGAFID Bug Report: " + title;

        //Use env variable 'NGAFID_ADMIN_EMAILS' as the recipient
        String recipientEmail = Config.NGAFID_ADMIN_EMAILS;
        if (recipientEmail == null || recipientEmail.isBlank()) {

            ctx.status(INTERNAL_SERVER_ERROR).result("Admin email not configured, aborting bug report").json(INTERNAL_SERVER_ERROR);
            return;

        }

        //Use the email address as the recipient
        List<String> recipients = List.of(recipientEmail);


        List<String> bccRecipients;

        //Flagged to include the sender's email address, add it to the BCC list
        if (includeEmail && senderEmail != null && !senderEmail.isBlank())
            bccRecipients = List.of(senderEmail);

            //Otherwise, no BCC recipients
        else
            bccRecipients = List.of();

        LOG.info(
                "Attempting to send bug report email to '" + recipientEmail
                        + "'" + " from  '" + senderEmail
                        + "'" + " (BCC Status: " + includeEmail + ")"
                        + "..."
        );

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

    }

    ;

    private static class BugReportPayload {
        public String title;
        public String body;
        public String senderEmail;
        public boolean includeEmail;

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
        app.post("/api/bug", BugReportJavalinRoutes::postBugReportEmail);

        //Bug Report Page
        app.get("/protected/bug_report", BugReportJavalinRoutes::getBugReport);

    }

}

