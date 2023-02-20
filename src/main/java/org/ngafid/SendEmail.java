package org.ngafid;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.InputStream;
import java.io.IOException;

import java.util.*;
import javax.mail.*;
import javax.mail.internet.*;
import javax.activation.*;

public class SendEmail {

    private static String password;
    private static String username;
    private static ArrayList<String> adminEmails;

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
            BufferedReader bufferedReader = new BufferedReader(new FileReader(NGAFID_EMAIL_INFO));
            username = bufferedReader.readLine();
            password = bufferedReader.readLine();

            System.out.println("email username: '" + username + "'");
            System.out.println("email password: '" + password + "'");

            //Don't remove this!
            bufferedReader.close();
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
    }

    public static void sendEmail(ArrayList<String> toRecipients, ArrayList<String> bccRecipients, String subject, String body) {
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
        System.out.println("getting authenticator");
        Authenticator auth = new SMTPAuthenticator();
        System.out.println("got authenticator, getting session");
        Session session = Session.getDefaultInstance(properties, auth);
        System.out.println("got session");

        try {
            // Create a default MimeMessage object.
            MimeMessage message = new MimeMessage(session);

            // Set From: header field of the header.
            message.setFrom(new InternetAddress(from));

            // Set To: header field of the header.
            boolean erau_found = false;
            for (String toRecipient : toRecipients) {
                //list of users who do not want emails: TODO: make this a user setting
                if (toRecipient.equals("nievesn2@erau.edu") || toRecipient.equals("dicksonm@erau.edu")) {
                    erau_found = true;
                    continue;
                }

                message.addRecipient(Message.RecipientType.TO, new InternetAddress(toRecipient));
            }

            if (erau_found) {
                message.addRecipient(Message.RecipientType.TO, new InternetAddress("dbavsafe@gmail.com"));
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


    public static void main(String [] args) {    
        // Recipient's email ID needs to be mentioned.

        ArrayList<String> recipients = new ArrayList<String>();
        recipients.add("tjdvse@rit.edu");
        recipients.add("travis.desell@gmail.com");

        ArrayList<String> bccRecipients = new ArrayList<String>();

        sendEmail(recipients, bccRecipients, "test NGAFID email", "testing testing 123");
    }
}

