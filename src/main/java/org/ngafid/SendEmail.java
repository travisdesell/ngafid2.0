package org.ngafid;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.IOException;

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
            
            if (!file.exists()) {
                // Create a new file and will populate it 
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
            } else {
                bufferedReader = new BufferedReader(new FileReader(NGAFID_EMAIL_INFO));

                String username = bufferedReader.readLine();

                if (username != null && username.startsWith("#")) {
                    LOG.severe("Email not being used with the NGAFID for uploads. To change this, add the email login information to " + NGAFID_EMAIL_INFO);
                } else {
                    password = bufferedReader.readLine();
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
        public PasswordAuthentication getPasswordAuthentication() {
            return new PasswordAuthentication(username, password);
        }

        public boolean isValid() {
            return !(username == null || password == null);
        }
    }

    /**
     * Wrapper for sending an email to NGAFID admins
     * @param subject - subject of the email
     * @param body - body of the email
     */
    public static void sendAdminEmails(String subject, String body) {
        sendEmail(adminEmails, new ArrayList<>(), subject, body);
    }

    public static void sendEmail(ArrayList<String> toRecipients, ArrayList<String> bccRecipients, String subject, String body) {
        SMTPAuthenticator auth = new SMTPAuthenticator();

        if (!emailEnabled) {
            System.out.println("Emailing has been disabled, not sending email");
            return;
        }

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
                    if (toRecipient.equals("nievesn2@erau.edu")) continue;
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
        } else {
            LOG.severe("E-mail info not valid, continuing without sending.");
        }
    }


    public static void main(String [] args) {    
        // Recipient's email ID needs to be mentioned.

        ArrayList<String> recipients = new ArrayList<String>();
        recipients.add("mayurjs26@gmail.com");
//        recipients.add("tjdvse@rit.edu");
//        recipients.add("travis.desell@gmail.com");

        ArrayList<String> bccRecipients = new ArrayList<String>();

        sendEmail(recipients, bccRecipients, "test NGAFID email", "testing testing 123");
    }
}

