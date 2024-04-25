package backend.utils;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.ByteArrayContent;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
//import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.GmailScopes;
import com.google.api.services.gmail.model.Message;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.security.GeneralSecurityException;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import javax.mail.internet.*;
import javax.mail.Session;
import javax.mail.MessagingException;

public class EmailSender {
    /**
   * Application name.
   */
  private static final String APPLICATION_NAME = "Gmail API Java Quickstart";
  /**
   * Global instance of the JSON factory.
   */
  private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
  /**
   * Directory to store authorization tokens for this application.
   */
  private static final String TOKENS_DIRECTORY_PATH = "tokens";

  /**
   * Global instance of the scopes required by this quickstart.
   * If modifying these scopes, delete your previously saved tokens/ folder.
   */
  //private static final List<String> SCOPES = Collections.singletonList(GmailScopes.GMAIL_LABELS);
  private static final List<String> SCOPES = Collections.singletonList(GmailScopes.GMAIL_SEND);

  /**
   *
   * @param to is the receiver's email address
   * @param msg is the message we send
   * @return good if succeeds, Error sending email if fails
   */
  public String sendNotification(String to, String msg) throws GeneralSecurityException, IOException {
    // Initialize the Gmail service
    final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
    Gmail service = new Gmail.Builder(HTTP_TRANSPORT, JSON_FACTORY, getCredentials(HTTP_TRANSPORT))
      .setApplicationName(APPLICATION_NAME)
      .build();
    
    try{
      // Create a MimeMessage object
      MimeMessage mimeMessage = createEmail("steven.h.geng@gmail.com", to, "Arrival Notice", msg);
      // Send the email
      sendMessage(service, "steven.h.geng@gmail.com", mimeMessage);
      return "good";
    } catch(MessagingException | IOException e){

      return "Error sending email";
    }
    
  }

  /**
   *
   * @param HTTP_TRANSPORT is used for HTTP connection
   * @return credential needed for sending email
   */
  private static Credential getCredentials(final NetHttpTransport HTTP_TRANSPORT) throws IOException {
    // Load client secrets.
    FileInputStream in = new FileInputStream("../app/src/main/resources/credentials.json");
    GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));
    
    // Build flow and trigger user authorization request.
    GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                                                                               HTTP_TRANSPORT, JSON_FACTORY, clientSecrets, SCOPES)
      .setDataStoreFactory(new FileDataStoreFactory(new java.io.File(TOKENS_DIRECTORY_PATH)))
      .setAccessType("offline")
      .build();
    LocalServerReceiver receiver = new LocalServerReceiver.Builder().setPort(8888).build();
    return new AuthorizationCodeInstalledApp(flow, receiver).authorize("user");
  }

  /**
   *
   * @param from is sender's email address
   * @param to is receiver's email address
   * @param subject is subject of the email
   * @param bodyText is body of the email
   * @return MineMessage that should be sent
   */
  private MimeMessage createEmail(String from, String to, String subject, String bodyText) throws MessagingException {
    Properties props = new Properties();
    Session session = Session.getDefaultInstance(props, null);
    
    MimeMessage email = new MimeMessage(session);
    email.setFrom(new InternetAddress(from));
    email.addRecipient(javax.mail.Message.RecipientType.TO, new InternetAddress(to));
    email.setSubject(subject);
    email.setText(bodyText);
    return email;
}

  /**
   *
   * @param service is gmail service
   * @param userId is the test user's email
   * @param email is the email to send
   * @return message we sent
   */
  private Message sendMessage(Gmail service, String userId, MimeMessage email) throws MessagingException, IOException {
    ByteArrayOutputStream buffer = new ByteArrayOutputStream();
    email.writeTo(buffer);
    byte[] bytes = buffer.toByteArray();
    ByteArrayContent byteArrayContent = new ByteArrayContent("message/rfc822", bytes);
    Message message = service.users().messages().send(userId, null, byteArrayContent).execute();
    return message;
  }
}
