package org.ngafid;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.IOException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.ngafid.Database;
import org.ngafid.EmailType;
import org.ngafid.accounts.UserEmailPreferences;

import java.util.*;
import java.util.logging.Logger;

import javax.mail.*;
import javax.mail.internet.*;
import javax.activation.*;


public class SendEmail {

    private static String password;
    private static String username;
    private static ArrayList<String> adminEmails;
    private static boolean emailEnabled = true;

    private static final Logger LOG = Logger.getLogger(SendEmail.class.getName());

    static {

        String enabled = System.getenv("NGAFID_EMAIL_ENABLED");

        if (enabled != null && enabled.toLowerCase().equals("false")) {
            LOG.info("Emailing has been disabled");
            emailEnabled = false;
        }

        if (System.getenv("NGAFID_EMAIL_INFO") == null) {
            System.err.println("ERROR: 'NGAFID_EMAIL_INFO' environment variable not specified at runtime.");
            System.err.println("Please add the following to your ~/.bash_rc or ~/.profile file:");
            System.err.println("export NGAFID_EMAIL_INFO=<path/to/email_info_file>");
            System.exit(1);
        }

        String NGAFID_EMAIL_INFO = System.getenv("NGAFID_EMAIL_INFO");

        if (System.getenv("NGAFID_ADMIN_EMAILS") == null) {
            System.err.println("ERROR: 'NGAFID_ADMIN_EMAILS' environment variable not specified at runtime.");
            System.err.println("Please add a list of semicolon separated emails following to your ~/.bash_rc or ~/.profile file:");
            System.err.println("export NGAFID_ADMIN_EMAILS=\"person1@address.com;person2@address.net\"");
            System.exit(1);
        }

        String NGAFID_ADMIN_EMAILS = System.getenv("NGAFID_ADMIN_EMAILS");
        adminEmails = new ArrayList<String>(Arrays.asList(NGAFID_ADMIN_EMAILS.split(";")));
        System.out.println("import emails will always also be sent to the following admin emails:");
        for (String adminEmail : adminEmails) {
            System.out.println("\t'" + adminEmail + "'");
        }

        try {

            File file = new File(NGAFID_EMAIL_INFO);
            BufferedReader bufferedReader = null;
            
            //Email info file does not exit...
            if (!file.exists()) {

                //...Create a new file and will populate it
                file.createNewFile();

                try {
                    PrintWriter pw = new PrintWriter(file);

                    pw.write("# E-mail not configured for the NGAFID\n");
                    pw.write("# To change this, replace these lines with the email login info\n");

                    pw.close();
                } catch (IOException ie) {
                    LOG.severe("ERROR: Could not write default information to email file: " + NGAFID_EMAIL_INFO);
                }
                
                LOG.severe("Email not being with the NGAFID for uploads, to change this edit " + NGAFID_EMAIL_INFO + ".");

            }

            //Email info file does exist...
            else {

                //...Read the file
                bufferedReader = new BufferedReader(new FileReader(NGAFID_EMAIL_INFO));

                username = bufferedReader.readLine();
                //System.out.println("read username: '" + username + "'");

                if (username != null && username.startsWith("#")) {
                    LOG.severe("Email not being used with the NGAFID for uploads. To change this, add the email login information to " + NGAFID_EMAIL_INFO);
                }
                    
                else {
                    password = bufferedReader.readLine();
                    //System.out.println("read password: '" + password + "'");
                    LOG.info("Using email address to send emails: " + username);
                }

                //Don't remove this!
                bufferedReader.close();

            }

        } catch (IOException e) {
            System.err.println("Error reading from NGAFID_EMAIL_INFO: '" + NGAFID_EMAIL_INFO + "'");
            e.printStackTrace();
            System.exit(1);
        }

    }

    public static ArrayList<String> getAdminEmails() {
        return adminEmails;
    }

    private static class SMTPAuthenticator extends javax.mail.Authenticator {

        String username;
        String password;

        public SMTPAuthenticator(String username, String password) {
            this.username = username;
            this.password = password;
            System.out.println("Created authenticator with username: '" + this.username + "' and password: '" + this.password + "'");
        }

        public PasswordAuthentication getPasswordAuthentication() {
            System.out.println("Attempting to authenticate with username: '" + this.username + "' and password: '" + this.password + "'");
            return new PasswordAuthentication(this.username, this.password);
        }

        public boolean isValid() {
            System.out.println("Checking if valid with username: '" + this.username + "' and password: '" + this.password + "'");
            return !(this.username == null || this.password == null);
        }
    
    }

    /**
     * Wrapper for sending an email to NGAFID admins
     * @param subject - subject of the email
     * @param body - body of the email
     */
    public static void sendAdminEmails(String subject, String body, EmailType emailType) {
        sendEmail(adminEmails, new ArrayList<>(), subject, body, emailType);
    }

    public static void sendEmail(ArrayList<String> toRecipients, ArrayList<String> bccRecipients, String subject, String body, EmailType emailType) {

        SMTPAuthenticator auth = new SMTPAuthenticator(username, password);

        if (!emailEnabled) {
            System.out.println("Emailing has been disabled, not sending email");
            return;
        }

        //System.out.println(String.format("Username: %s, PW: %s", username, password));

        if (auth.isValid()) {

            System.out.println("emailing to " + String.join(", ", toRecipients));
            System.out.println("BCCing to " + String.join(", ", bccRecipients));
            System.out.println("subject: '" + subject);
            System.out.println("body: '" + body);


            // Sender's email ID needs to be mentioned
            String from = "UND.ngafid@und.edu";

            // Assuming you are sending email from localhost
            //String host = "po3.ndus.edu";
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
            Session session = Session.getDefaultInstance(properties, (Authenticator) auth);

            try {

                // Create a default MimeMessage object.
                MimeMessage message = new MimeMessage(session);

                // Set From: header field of the header.
                message.setFrom(new InternetAddress(from));

                // Set To: header field of the header.
                for (String toRecipient : toRecipients) {

                    //list of users who do not want emails: TODO: make this a user setting
                    if (toRecipient.equals("nievesn2@erau.edu")) {
                        continue;
                    }

                    //Check if the emailType is forced
                    if (EmailType.isForced(emailType)) {
                        System.out.println("Delivering FORCED email type: " + emailType);
                    }

                    //Check whether or not the emailType is enabled for the user
                    else if (!UserEmailPreferences.getEmailTypeUserState(toRecipient, emailType)) {
                        continue;
                    }

                    System.out.println("EMAILING TO: " + toRecipient);

                    message.addRecipient(Message.RecipientType.TO, new InternetAddress(toRecipient));

                    }

                for (String bccRecipient : bccRecipients) {
                    message.addRecipient(Message.RecipientType.BCC, new InternetAddress(bccRecipient));
                    }

                // Set Subject: header field
                message.setSubject(subject);

                // Now set the actual message
                message.setContent(body, "text/html; charset=utf-8");

                // Send message
                System.out.println("sending message!");
                Transport.send(message);
                System.out.println("Sent message successfully....");

            } catch (MessagingException mex) {
                mex.printStackTrace();
            }

        }

        else {
            LOG.severe("E-mail info not valid, continuing without sending.");
        }
    
    }

    public static void main(String [] args) {    

        /*

        // Recipient's email ID needs to be mentioned.

        ArrayList<String> recipients = new ArrayList<String>();
        recipients.add("apl1341@rit.edu");
        recipients.add("aidan@labellahome.org");

        ArrayList<String> bccRecipients = new ArrayList<String>();

        //  New email system does not support having no Email Type specified,
        //  so this won't work unless a test Email Type is added.
        
        //  sendEmail(recipients, bccRecipients, "test NGAFID email", "testing testing 123", EmailType.TEST_EMAIL_TYPE);
        
        */
    
    }

}