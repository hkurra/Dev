import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import com.gdrive.desktop.client.Authorization.UserAutorization;
import com.gdrive.desktop.client.FileOperation.UploadCommand;
import com.gdrive.desktop.client.Global.DriveDesktopClient;
import com.gdrive.desktop.client.cache.GDriveFile;
import com.gdrive.desktop.client.cache.GDriveFileRevisions;
import com.gdrive.desktop.client.cache.GDriveFiles;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.repackaged.org.apache.commons.codec.binary.Base64;
import com.google.api.client.util.DateTime;
import com.google.api.services.drive.Drive.Parents;
import com.google.api.services.drive.Drive.Properties.Get;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.Drive.Properties.Update;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.ChangeList;
import com.google.api.services.drive.model.ChildList;
import com.google.api.services.drive.model.ChildReference;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.ParentReference;
import com.google.api.services.drive.model.Property;
import com.google.api.services.drive.model.PropertyList;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.GmailScopes;
import com.google.api.services.gmail.model.*;

import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

/**
 * 
 */

/**
 * @author harsh
 * 
 */
public class App {

	public static String folderID = "0BzehUff2oW6HMHBYVFFnXzNlVmc";
	public static String WATCHING_KEY = "IS_WATCHING_ITS_CHANGES";
	public static String LAST_ETAG = "LAST_ETAG" ;
	public static String LAST_CHECKSUM = "LAST_CHECKSUM";
	public static String WATCHING_VALUE_YES = "YES";
	public static String WATCHING_VALUE_NO = "NO";
	public static String Visibility = "PUBLIC";
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		

		
		
		Drive DriveService = null;
		
		DriveDesktopClient.CLIENT_ID = "882577327525.apps.googleusercontent.com";
		DriveDesktopClient.CLIENT_SECRET = "06DR_ebCYtPCpGo4RhCjD4_2";
		DriveDesktopClient.APPLICATION_NAME = "First_UI _testing";
		
		DriveDesktopClient.SCOPES.add(GmailScopes.GMAIL_COMPOSE);
		DriveDesktopClient.SCOPES.add(GmailScopes.MAIL_GOOGLE_COM);

		UserAutorization authorization = new UserAutorization();
		Credential credential = authorization.authorize();
/*		File shared = null;*/
		if (credential != null) {
			
			com.google.api.services.drive.model.Property prop = null;
			
			Gmail mailService = new Gmail.Builder(
					DriveDesktopClient.HTTP_TRANSPORT,
					DriveDesktopClient.JSON_FACTORY, credential)
					.setApplicationName("NOTIFY_DIGESTER").build();

			DriveService = new Drive.Builder(
					DriveDesktopClient.HTTP_TRANSPORT,
					DriveDesktopClient.JSON_FACTORY, credential)
					.setApplicationName("NOTIFY_DIGESTER").build();
			DriveDesktopClient.DRIVE = DriveService;
			
			HashMap<File, Changes> changeList = Utils.getFolderChanges(folderID);
			
			   Iterator it = changeList.entrySet().iterator();
			   boolean changeFolderMetadata = false;
			   
			   if (changeList.size() > 0) {
				   System.out.println("following changes detected since your last query");
			   }
			    while (it.hasNext()) {
			        Map.Entry pairs = (Map.Entry)it.next();
			        File key = (File)pairs.getKey();
			        Changes value = (Changes)pairs.getValue();
			        
			        
			        if (key.getId().equals(folderID)) {
			        	changeFolderMetadata = false;
			        	System.out.println("change in folder metadata by " + key.getLastModifyingUserName() +" on "+key.getModifiedDate() + " GMT ");
			        }
			        else {
			        	StringBuffer changesToPrint = new StringBuffer("");
			        	
			        	if(value.m_Content) {
			        		changesToPrint.append("content ");
			        	}
			        	if(value.m_Metadata) {
			        		changesToPrint.append(" metadata ");
			        	}
			        	if(value.m_UnwatchedFile) {
			        		changesToPrint.append(" new Added ");
			        	}
			        	System.out.println("change in file " + key.getTitle() + "" + changesToPrint + " by " + key.getLastModifyingUserName() + " on " + key.getModifiedDate() + " GMT ");
			        }
			    }
			    
			
/*			try {
				shared = DriveService.files()
				.get(folderID).execute();
			} catch (IOException e1) {
				e1.printStackTrace();
			}*/
			/*try {
																		
				prop = new Property();
				prop.setKey(WATCHING_KEY);
				prop.setValue(WATCHING_VALUE_YES);
				prop.setVisibility(Visibility);

				Get request = DriveService.properties().get(
						folderID, WATCHING_KEY);
				request.setVisibility("PUBLIC");
				Property property = request.execute();

				if (property.getKey().equals(prop.getKey())) {
					
					request = DriveService.properties().get(
							folderID, LAST_ETAG);
					request.setVisibility("PUBLIC");
					property = request.execute();
					if(property.getValue().equals(shared.getEtag())) {
						System.out.println("NO_CHANGE");
						
						//find is there any change in child
						
					}
					else {
						System.out.println("change");
						property.setValue(shared.getEtag());
						
						Property py = DriveService.properties()
						.insert(folderID, property)
						.execute();
					}
					property.setValue("NO");
					Update py = DriveService.properties()
					.update(folderID, WATCHING_KEY, property);
					py.setVisibility(Visibility);
					py.execute();
					
					
				} else {
					//DriveService.properties().delete(arg0, arg1)
				}

			} catch (IOException e) {
				try {
					Property py = DriveService.properties()
							.insert(folderID, prop)
							.execute();
					System.out.println("We start monitering its changes, give me a minute to find earlier changes on it");
					
					List<Change> result = new ArrayList<Change>();
				    Changes.List request = DriveService.changes().list();
				    
				    do {
				        try {
				          ChangeList changes = request.execute();

				          result.addAll(changes.getItems());
				          request.setPageToken(changes.getNextPageToken());
				        }
				        catch(Exception ex) {
				        	
				        }
				    }
				    while(request.getPageToken() != null &&
				             request.getPageToken().length() > 0);
				    
				    List<Change> interstedResult = new ArrayList<Change>();
				    for (Change chg: result) {
				    	
				    	File file = chg.getFile();
				    	if (file == null) continue;
				    	List<ParentReference> parentFolder = chg.getFile().getParents();
				    	
				    	for(ParentReference parent :parentFolder) {
				    		if (parent.getId().equals(folderID)) {
				    			interstedResult.add(chg);
				    		}
				    	}
				    	if (chg.getFileId().equals(folderID) ) {
				    		interstedResult.add(chg);
				    	}
				    }
				    
				    if(interstedResult.size() == 0) {
				    	System.out.println("No chages find for your rquested file");
				    }
				    else {
				    	for(Change interChanges : interstedResult) {
				    		DateTime lastModifiedDate  = interChanges.getFile().getModifiedDate();
				    		if(!interChanges.getFileId().equals(folderID)) {
				    			System.out.println("This folder underGoes Chnages on following date" + lastModifiedDate.toString() + "due to change in its child " + interChanges.getFile().getTitle());
				    			System.out.println(interChanges.getId());
				    		}
				    		else {
				    		System.out.println("This folder underGoes Chnages on following date" + lastModifiedDate.toString());
				    		}
				    	}
				    }
					System.out.println("I am setting some flag so it will be easy to find changes next time");
					
					Property py = DriveService.properties()
					.insert(folderID, prop)
					.execute();
					
					prop = new Property();
					prop.setKey(LAST_ETAG);
					prop.setValue(shared.getEtag());
					prop.setVisibility(Visibility);
					
					py = DriveDesktopClient.DRIVE.properties()
					.insert(folderID, prop)
					.execute();
					if(shared.getMimeType().equals(DriveDesktopClient.FOLDER_MIME_TYPE)) {
						folderProcessing(shared);
					}
				} catch (IOException ex) {
					e.printStackTrace();
				}
				System.out.println("An error occured: " + e);
			}*/
			// List<com.google.api.services.drive.model.Property> pl = new
			// ArrayList<Property>();
			// pl.add(prop);
			// fileMetadata.getDFile().setProperties(pl);
			// fileMetadata.setFolder(true);
			// String parentID = null;
			// UploadCommand gCmd = new UploadCommand(fileMetadata, parentID);
			// try {
			// gCmd.execute();
			// } catch (Exception e2) {
			// // TODO Auto-generated catch block
			// e2.printStackTrace();
			// }
			//
			// String value =
			// gCmd.getUploadedFile().getProperties().get(0).getValue();
			//
			//
			// MimeMessage email = null;
			// String from = "er.harshkurra@gmail.com";
			// try {
			// email = createEmail("harshkurra21@gmail.com", from, "be ready",
			// "asss");
			// } catch (MessagingException e1) {
			// // TODO Auto-generated catch block
			// e1.printStackTrace();
			// }
			// Message message = null;
			// try {
			// message = createMessageWithEmail(email);
			// } catch (MessagingException e1) {
			// // TODO Auto-generated catch block
			// e1.printStackTrace();
			// } catch (IOException e1) {
			// // TODO Auto-generated catch block
			// e1.printStackTrace();
			// }
			// try {
			// mailService.users().messages().send("er.harshkurra@gmail.com",
			// message).execute();//messages().list("me").setQ("").execute();
			// } catch (IOException e) {
			// // TODO Auto-generated catch block
			// e.printStackTrace();
			// }
			// //msg.se
			// } else {
			// System.out.println("Fail to connect");
		}
		
	}

	public static void folderProcessing(File folder) {
		Drive.Children.List request = null;
		try {
			request = DriveDesktopClient.DRIVE.children().list(
					folder.getId()); 
			do {
				ChildList children = (ChildList) request.execute();
				for (ChildReference child : children.getItems()) {
					File childFile = DriveDesktopClient.DRIVE.files().get(child.getId()).execute();
					if(childFile.getMimeType().equals(DriveDesktopClient.FOLDER_MIME_TYPE)) {
						folderProcessing(childFile);
					}
					else {
						Property prop = new Property();
						prop.setKey(LAST_ETAG);
						prop.setValue(childFile.getEtag());
						prop.setVisibility(Visibility);
						
						Property py = DriveDesktopClient.DRIVE.properties()
						.insert(childFile.getId(), prop)
						.execute();
						
						prop.setKey(LAST_CHECKSUM);
						prop.setValue(childFile.getMd5Checksum());
						
						py = DriveDesktopClient.DRIVE.properties()
						.insert(childFile.getId(), prop)
						.execute();
						
					}
				}
				request.setPageToken(children.getNextPageToken());
			}while((request.getPageToken() != null)
					&& (request.getPageToken().length() > 0));
		}
		catch (Exception e) {
			// TODO: handle exception
		}
	}
	/**
	 * Create a MimeMessage using the parameters provided.
	 * 
	 * @param to
	 *            Email address of the receiver.
	 * @param from
	 *            Email address of the sender, the mailbox account.
	 * @param subject
	 *            Subject of the email.
	 * @param bodyText
	 *            Body text of the email.
	 * @return MimeMessage to be used to send email.
	 * @throws MessagingException
	 */
	public static MimeMessage createEmail(String to, String from,
			String subject, String bodyText) throws MessagingException {
		Properties props = new Properties();
		Session session = Session.getDefaultInstance(props, null);

		MimeMessage email = new MimeMessage(session);
		InternetAddress tAddress = new InternetAddress(to);
		InternetAddress fAddress = new InternetAddress(from);

		email.setFrom(new InternetAddress(from));
		email.addRecipient(javax.mail.Message.RecipientType.TO,
				new InternetAddress(to));
		email.setSubject(subject);
		email.setText(bodyText);
		return email;
	}

	/**
	 * Create a Message from an email
	 * 
	 * @param email
	 *            Email to be set to raw of message
	 * @return Message containing base64 encoded email.
	 * @throws IOException
	 * @throws MessagingException
	 */
	public static Message createMessageWithEmail(MimeMessage email)
			throws MessagingException, IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		email.writeTo(baos);
		String encodedEmail = Base64.encodeBase64URLSafeString(baos
				.toByteArray());
		Message message = new Message();
		message.setRaw(encodedEmail);
		return message;
	}
}
