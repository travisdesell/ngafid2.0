package org.ngafid.core.util;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.ngafid.core.Database;
import org.ngafid.core.accounts.EmailType;
import org.ngafid.core.accounts.User;
import org.ngafid.core.kafka.EmailConsumer;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.io.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.*;
import java.util.logging.Logger;

public enum SendEmail {
    ;

    private static final ArrayList<String> ADMIN_EMAILS;
    private static final Logger LOG = Logger.getLogger(SendEmail.class.getName());
    private static final String BASE_URL = "https://ngafid.org";
    // private static final String baseURL = "https://ngafidbeta.rit.edu";
    private static final String UNSUB_URL_TEMPLATE = (BASE_URL + "/email_unsubscribe?id=__ID__&token=__TOKEN__");
    private static final java.sql.Date LAST_TOKEN_FREE = new java.sql.Date(0);
    private static final int EMAIL_UNSUBSCRIBE_TOKEN_EXPIRATION_MONTHS = 3;
    private static final int MS_PER_DAY = 86_400_000;
    private static final int EXPIRATION_POLL_THRESHOLD_MS = (MS_PER_DAY); // Minimum number of milliseconds needed
    private static String password;
    private static String username;
    private static boolean emailEnabled = true;
    // before trying to free old tokens

    static {

        String enabled = System.getenv("NGAFID_EMAIL_ENABLED");
        LOG.info("Email base URL: " + BASE_URL);

        if (enabled != null && enabled.equalsIgnoreCase("false")) {
            LOG.info("Emailing has been disabled");
            emailEnabled = false;
        }

        if (System.getenv("NGAFID_EMAIL_INFO") == null) {
            System.err.println("ERROR: 'NGAFID_EMAIL_INFO' environment variable not specified at runtime.");
            System.err.println("Please add the following to your ~/.bash_rc or ~/.profile file:");
            System.err.println("export NGAFID_EMAIL_INFO=<path/to/email_info_file>");
            System.exit(1);
        }

        String ngafidEmailInfo = System.getenv("NGAFID_EMAIL_INFO");

        if (System.getenv("NGAFID_ADMIN_EMAILS") == null) {
            System.err.println("ERROR: 'NGAFID_ADMIN_EMAILS' environment variable not specified at runtime.");
            System.err.println("Please add a list of semicolon separated emails following to your ~/.bash_rc or ~/" +
                    ".profile file:");
            System.err.println("export NGAFID_ADMIN_EMAILS=\"person1@address.com;person2@address.net\"");
            System.exit(1);
        }

        String ngafidAdminEmails = System.getenv("NGAFID_ADMIN_EMAILS");
        ADMIN_EMAILS = new ArrayList<>(Arrays.asList(ngafidAdminEmails.split(";")));
        System.out.println("import emails will always also be sent to the following admin emails:");
        for (String adminEmail : ADMIN_EMAILS) {
            System.out.println("\t'" + adminEmail + "'");
        }

        try {
            File file = new File(ngafidEmailInfo);

            // Email info file does not exit...
            if (!file.exists()) {

                // ...Create a new file and will populate it
                if (!file.createNewFile()) {
                    throw new IllegalStateException("Could not create email file: " + ngafidEmailInfo);
                }

                try {
                    PrintWriter pw = new PrintWriter(file);

                    pw.write("# E-mail not configured for the NGAFID\n");
                    pw.write("# To change this, replace these lines with the email login info\n");

                    pw.close();
                } catch (IOException ie) {
                    LOG.severe("ERROR: Could not write default information to email file: " + ngafidEmailInfo);
                }

                LOG.severe("Email not being with the NGAFID for uploads, to change this edit " + ngafidEmailInfo +
                        ".");

            } else { // Email info file does exist...

                // ...Read the file
                try (BufferedReader bufferedReader = new BufferedReader(new FileReader(ngafidEmailInfo))) {

                    username = bufferedReader.readLine();
                    // System.out.println("read username: '" + username + "'");

                    if (username != null && username.startsWith("#")) {
                        LOG.severe("Email not being used with the NGAFID for uploads. To change this, add the email login" +
                                " information to " + ngafidEmailInfo);
                    } else {
                        password = bufferedReader.readLine();
                        // System.out.println("read password: '" + password + "'");
                        LOG.info("Using email address to send emails: " + username);
                    }

                    // Don't remove this!
                }

            }

        } catch (IOException e) {
            System.err.println("Error reading from NGAFID_EMAIL_INFO: '" + ngafidEmailInfo + "'");
            e.printStackTrace();
            System.exit(1);
        }

    }

    public static ArrayList<String> getAdminEmails() {
        return ADMIN_EMAILS;
    }

    public static void freeExpiredUnsubscribeTokens(Connection connection) throws SQLException {
        Calendar calendar = Calendar.getInstance();
        java.sql.Date currentDate = new java.sql.Date(calendar.getTimeInMillis());

        // Wait at least 24 hours before trying to free tokens again
        long tokenDeltaTime = (currentDate.getTime() - LAST_TOKEN_FREE.getTime());
        LOG.info("Token timings: DELTA/CURRENT/LAST: " + tokenDeltaTime + " " +
                currentDate.getTime() + " " + LAST_TOKEN_FREE.getTime());
        if (tokenDeltaTime < EXPIRATION_POLL_THRESHOLD_MS) {
            LOG.info("Not attempting to free expired tokens (only " +
                    tokenDeltaTime + " / " + EXPIRATION_POLL_THRESHOLD_MS + " milliseconds have passed)");
            return;
        }
        LAST_TOKEN_FREE.setTime(currentDate.getTime());

        String query = "DELETE FROM email_unsubscribe_tokens WHERE expiration_date < ?";
        try (PreparedStatement preparedStatement = connection.prepareStatement(query)) {
            preparedStatement.setDate(1, currentDate);
            preparedStatement.execute();
        }

        LOG.info("Freed expired email unsubscribe tokens");
    }

    private static String generateUnsubscribeToken(String recipientEmail,
                                                   int userID, Connection connection) throws SQLException {

        // Generate a random string
        String token = UUID.randomUUID().toString().replace("-", "");

        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.MONTH, EMAIL_UNSUBSCRIBE_TOKEN_EXPIRATION_MONTHS);
        java.sql.Date expirationDate = new java.sql.Date(calendar.getTimeInMillis());

        String query = "INSERT INTO email_unsubscribe_tokens (token, user_id, expiration_date) VALUES (?, ?, ?)";
        try (PreparedStatement preparedStatement = connection.prepareStatement(query)) {
            preparedStatement.setString(1, token);
            preparedStatement.setInt(2, userID);
            preparedStatement.setDate(3, expirationDate);
            preparedStatement.execute();

        } catch (SQLException e) {
            LOG.severe("(SQL Exception) Failed to generate token for email recipient: "
                    + recipientEmail + ": " + e.getMessage());
            throw e;
        } catch (Exception e) {
            LOG.severe("(Non-SQL Exception) Failed to generate token for email recipient: "
                    + recipientEmail + ": " + e.getMessage());
        }

        // Log the token's expiration date
        Calendar expirationCalendar = Calendar.getInstance();
        expirationCalendar.setTime(expirationDate);
        String expirationDateString = expirationCalendar.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale.US) +
                " " + expirationCalendar.get(Calendar.DAY_OF_MONTH) + ", " + expirationCalendar.get(Calendar.YEAR) +
                " " + expirationCalendar.get(Calendar.HOUR_OF_DAY) + ":" + expirationCalendar.get(Calendar.MINUTE) +
                ":" + expirationCalendar.get(Calendar.SECOND);

        LOG.info("Generated email unsubscribe token for " + recipientEmail + ": " + token
                + " (Expires at " + expirationDateString + ")");

        return token;

    }

    /**
     * Wrapper for sending an email to NGAFID admins
     *
     * @param subject   - subject of the email
     * @param body      - body of the email
     * @param emailType - type of email to send
     */
    public static void sendAdminEmails(String subject, String body, EmailType emailType) {

        try {
            sendEmail(ADMIN_EMAILS, new ArrayList<>(), subject, body, emailType);
        } catch (SQLException e) {
            LOG.severe("(SQL Exception) Failed to send admin email: " + e.getMessage());
        }
    }

    public static void sendEmail(List<String> toRecipients, List<String> bccRecipients, String subject,
                                 String body, EmailType emailType) throws SQLException {

        // Send the email with no existing connection
        try (Connection connection = Database.getConnection()) {
            LOG.info("Sending an email with a fresh SQL connection");
            enqueueEmail(new Email(toRecipients, bccRecipients, subject, body, emailType));
        }

    }

    public record Email(
            List<String> recipients,
            List<String> bccRecipients,
            String subject,
            String body,
            EmailType emailType
    ) {
    }

    private static KafkaProducer<String, Email> producer = null;

    public static void enqueueEmail(Email email) {
        if (producer == null) {
            producer = EmailConsumer.getProducer();
        }

        producer.send(new ProducerRecord<>("email", email));
    }

    public static void sendBatchEmail(List<Email> emails, Connection connection) throws SQLException {
        SMTPAuthenticator auth = new SMTPAuthenticator(username, password);

        if (!emailEnabled) {
            LOG.info("Emailing has been disabled, not sending email");
            LOG.info("But we would have sent the following emails:");

            for (Email email : emails)
                LOG.info(email.body);

            return;
        }

        // Attempt to free expired tokens
        // freeExpiredUnsubscribeTokens(connection);

        // Sender's email ID needs to be mentioned
        String from = "UND.ngafid@und.edu";

        // Assuming you are sending email from localhost
        // String host = "po3.ndus.edu";
        String host = "smtp.office365.com";

        // Get system properties
        Properties properties = System.getProperties();

        // Setup mail server
        properties.setProperty("mail.smtp.starttls.enable", "true");
        properties.setProperty("mail.smtp.host", host);
        properties.setProperty("mail.smtp.port", "587");
        properties.setProperty("mail.smtp.auth", "true");
        properties.setProperty("mail.smtp.ssl.protocols", "TLSv1.2");

        // Get the default mail session object.
        Session session = Session.getDefaultInstance(properties, auth);

        for (Email email : emails) {
            if (auth.isValid()) {

                LOG.info("emailing to " + String.join(", ", email.recipients));
                LOG.info("BCCing to " + String.join(", ", email.bccRecipients));
                LOG.info("subject: '" + email.subject);
                LOG.info("body: '" + email.body);

                try {
                    /* SEND TO toRecipients */

                    // Create a default MimeMessage object.
                    MimeMessage message = new MimeMessage(session);

                    // Set From: header field of the header.
                    message.setFrom(new InternetAddress(from));

                    // Set To: header field of the header.
                    for (String toRecipient : email.recipients) {

                        // list of users who do not want emails: TODO: make this a user setting
                        if (toRecipient.equals("nievesn2@erau.edu")) {
                            continue;
                        }

                        // Check if the emailType is forced
                        boolean embedUnsubscribeURL = true;
                        if (EmailType.isForced(email.emailType)) {
                            LOG.info("Delivering FORCED email type: " + email.emailType);
                            embedUnsubscribeURL = false;
                        } else {
                            User user = User.get(connection, toRecipient);
                            if (user != null && !user.getUserEmailPreferences(connection).getPreference(email.emailType)) {
                                // Check if this email type is enabled for this user.
                                continue;
                            }
                        }

                        String bodyPersonalized = email.body;
                        if (embedUnsubscribeURL) {
                            try {
                                int userID = User.get(connection, toRecipient).getId();

                                // Generate a token for the user to unsubscribe
                                String token = generateUnsubscribeToken(toRecipient, userID, connection);
                                String unsubscribeURL = UNSUB_URL_TEMPLATE.replace("__ID__",
                                        Integer.toString(userID)).replace("__TOKEN__", token);

                                // Embed the Unsubscribe URL at the top of the email body
                                bodyPersonalized = ("<a href=\"" + unsubscribeURL + "\">Unsubscribe from non-critical " +
                                        "NGAFID emails</a><br><br>" + email.body);

                            } catch (Exception e) {
                                LOG.severe("Recipient email " + toRecipient + " is not mapped in UserEmailPreferences, " +
                                        "skipping unsubscribe URL");
                            }
                        }

                        // Set Subject: header field
                        message.setSubject(email.subject);
                        message.setContent(bodyPersonalized, "text/html; charset=utf-8");

                        message.setRecipient(Message.RecipientType.TO, new InternetAddress(toRecipient));

                        // Send the message to the current recipient
                        LOG.info("Sending email message!");
                        Transport.send(message);
                    }

                    /* SEND TO bccRecipients */
                    if (!email.bccRecipients.isEmpty()) {
                        // Create a default MimeMessage object.
                        message = new MimeMessage(session);

                        // Set From: header field of the header.
                        message.setFrom(new InternetAddress(from));

                        for (String bccRecipient : email.bccRecipients) {
                            message.addRecipient(Message.RecipientType.BCC, new InternetAddress(bccRecipient));
                        }

                        // Set Subject: header field
                        message.setSubject(email.subject);
                        message.setContent(email.body, "text/html; charset=utf-8");

                        // Send the message to the current BCC recipients
                        LOG.info("Sending email message (BCC)!");
                        Transport.send(message);
                    }

                    LOG.info("Sent email messages successfully...");

                } catch (MessagingException mex) {
                    mex.printStackTrace();
                }

            } else {
                LOG.severe("E-mail info not valid, continuing without sending.");
            }
        }
    }

    public static void main(String[] args) {

        /*
         *
         * // Recipient's email ID needs to be mentioned.
         *
         * ArrayList<String> recipients = new ArrayList<String>();
         * recipients.add("apl1341@rit.edu");
         * recipients.add("aidan@labellahome.org");
         *
         * ArrayList<String> bccRecipients = new ArrayList<String>();
         *
         * // New email system does not support having no Email Type specified,
         * // so this won't work unless a test Email Type is added.
         *
         * // sendEmail(recipients, bccRecipients, "test NGAFID email",
         * "testing testing 123", EmailType.TEST_EMAIL_TYPE);
         *
         */

    }

    private static class SMTPAuthenticator extends javax.mail.Authenticator {

        private final String username;
        private final String password;

        SMTPAuthenticator(String username, String password) {
            this.username = username;
            this.password = password;
            System.out.println("Created authenticator with username: '" + this.username
                    + "' and password: '" + this.password + "'");
        }

        public PasswordAuthentication getPasswordAuthentication() {
            System.out.println("Attempting to authenticate with username: '" + this.username
                    + "' and password: '" + this.password + "'");
            return new PasswordAuthentication(this.username, this.password);
        }

        public boolean isValid() {
            System.out.println("Checking if valid with username: '" + this.username
                    + "' and password: '" + this.password + "'");
            return !(this.username == null || this.password == null);
        }
    }
}
