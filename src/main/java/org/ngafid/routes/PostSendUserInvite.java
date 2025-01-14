package org.ngafid.routes;

import com.google.gson.Gson;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.logging.Logger;
import org.ngafid.SendEmail;
import org.ngafid.accounts.EmailType;
import org.ngafid.accounts.User;
import spark.Request;
import spark.Response;
import spark.Route;
import spark.Session;
import spark.Spark;

public class PostSendUserInvite implements Route {
    private static final Logger LOG = Logger.getLogger(PostSendUserInvite.class.getName());
    private Gson gson;

    public PostSendUserInvite(Gson gson) {
        this.gson = gson;

        LOG.info("post " + this.getClass().getName() + " initalized");
    }

    private static class InvitationSent {
        String message = "Invitation Sent.";
    }

    @Override
    public Object handle(Request request, Response response) throws Exception {
        LOG.info("handling " + this.getClass().getName());

        final Session session = request.session();
        User user = session.attribute("user");

        int fleetId = Integer.parseInt(request.queryParams("fleetId"));
        String fleetName = request.queryParams("fleetName");
        String inviteEmail = request.queryParams("email");

        //check to see if the logged-in user can invite users to this fleet
        if (!user.managesFleet(fleetId)) {
            LOG.severe("INVALID ACCESS: user did not have access to invite other users.");
            Spark.halt(401, "User did not have access to invite other users.");
            return null;
        } else {
            ArrayList<String> recipient = new ArrayList<String>();
            recipient.add(inviteEmail);

            StringBuilder body = getEmailStringBuilder(fleetName, inviteEmail);

            ArrayList<String> bccRecipients = new ArrayList<>();
            SendEmail.sendEmail(recipient, bccRecipients, "NGAFID Account Creation Invite", body.toString(), EmailType.ACCOUNT_CREATION_INVITE);
            return gson.toJson(new InvitationSent());
        }
    }

    private static StringBuilder getEmailStringBuilder(String fleetName, String inviteEmail) throws UnsupportedEncodingException {
        String encodedFleetName = URLEncoder.encode(fleetName, StandardCharsets.UTF_8);

        String formattedInviteLink = "https://ngafid.org/create_account?fleet_name=" + encodedFleetName + "&email=" + inviteEmail;

        StringBuilder body = new StringBuilder();
        body.append("<html><body>");
        body.append("<p>Hi,<p><br>");
        body.append("<p>A account creation invitation was sent to your account for fleet: ").append(fleetName).append("<p>");
        body.append("<p>Please click the link below to create an account.<p>");
        body.append("<p> <a href=").append(formattedInviteLink).append(">Create Account</a></p><br>");
        body.append("</body></html>");
        return body;
    }
}
