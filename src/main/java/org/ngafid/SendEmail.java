package org.ngafid;

import java.util.*;
import javax.mail.*;
import javax.mail.internet.*;
import javax.activation.*;

public class SendEmail {

    public static void main(String [] args) {    
        // Recipient's email ID needs to be mentioned.
        String to = "tjdvse@rit.edu";
        //String to = "travis.desell@gmail.com";

        // Sender's email ID needs to be mentioned
        String from = "travis.desell@ngafid.org";

        // Assuming you are sending email from localhost
        String host = "po3.ndus.edu";

        // Get system properties
        Properties properties = System.getProperties();

        // Setup mail server
        properties.setProperty("mail.smtp.host", host);

        // Get the default Session object.
        Session session = Session.getDefaultInstance(properties);

        try {
            // Create a default MimeMessage object.
            MimeMessage message = new MimeMessage(session);

            // Set From: header field of the header.
            message.setFrom(new InternetAddress(from));

            // Set To: header field of the header.
            message.addRecipient(Message.RecipientType.TO, new InternetAddress(to));

            // Set Subject: header field
            message.setSubject("Test email from the NGAFID");

            // Now set the actual message
            message.setText("Testing Testing 123");

            // Send message
            Transport.send(message);
            System.out.println("Sent message successfully....");
        } catch (MessagingException mex) {
            mex.printStackTrace();
        }
    }
}

