
import java.io.IOException;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;


import com.gdrive.desktop.client.Authorization.UserAutorization;
import com.gdrive.desktop.client.Global.DriveDesktopClient;


import com.google.api.client.auth.oauth2.Credential;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.Permission;
import com.google.api.services.drive.model.PermissionList;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.GmailScopes;
import com.google.api.services.gmail.model.*;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

/**
 * 
 */

/**
 * @author harsh
 * 
 */
public class App {
	//0BzehUff2oW6HMHBYVFFnXzNlVmc
	
	public static String folderID = "0B7ZszO4g36UZZ3VCS3Z4N3FyNzA";
	public static String WATCHING_KEY = "IS_WATCHING_ITS_CHANGES";
	public static String LAST_ETAG = "LAST_ETAG";
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
		/* File shared = null; */
		if (credential != null) {
			Gmail mailService = new Gmail.Builder(
					DriveDesktopClient.HTTP_TRANSPORT,
					DriveDesktopClient.JSON_FACTORY, credential)
					.setApplicationName("NOTIFY_DIGESTER").build();

			DriveService = new Drive.Builder(DriveDesktopClient.HTTP_TRANSPORT,
					DriveDesktopClient.JSON_FACTORY, credential)
					.setApplicationName("NOTIFY_DIGESTER").build();
			DriveDesktopClient.DRIVE = DriveService;

			HashMap<File, Changes> changeList = Utils
					.GetFolderChanges(folderID);

			Iterator<Map.Entry<File, Changes>> it = changeList.entrySet().iterator();

			if (changeList.size() > 0) {
				System.out
						.println("following changes detected since your last query");
			}

			StringBuffer msgBody = new StringBuffer(
					"Following changes detected since your last Feed \n\n");
			while (it.hasNext()) {
				Map.Entry<File, Changes> pairs = it.next();
				File key = (File) pairs.getKey();
				Changes value = (Changes) pairs.getValue();

				if (key.getId().equals(folderID)) {		
					StringBuffer changesToPrint = new StringBuffer("");
					
					if (value.m_Metadata) {
						changesToPrint.append(" metadata(name etc) ");
					}
					if (value.m_UnwatchedFile) {
						changesToPrint.append(" new Added ");
					}
					msgBody.append("change in folder " + changesToPrint + " by "
							+ key.getLastModifyingUserName() + " on "
							+ key.getModifiedDate() + " GMT \n");
				} else {
					StringBuffer changesToPrint = new StringBuffer("");

					if (value.m_Content) {
						changesToPrint.append("content ");
					}
					if (value.m_Metadata) {
						changesToPrint.append(" metadata(name etc) ");
					}
					if (value.m_UnwatchedFile) {
						changesToPrint.append(" new Added ");
					}
					msgBody.append("change in file " + key.getTitle() + ""
							+ changesToPrint + " by "
							+ key.getLastModifyingUserName() + " on "
							+ key.getModifiedDate() + " GMT " + "\n\n");
				}
			}
			msgBody.append("\n\n\n\n\n\n\n\n\n\n To know its origion visit here\n https://github.com/hkurra/Dev\n");
			System.out.print(msgBody);
			String senderEmailID = "harshkurra21@gmail.com";
			String subject = "Our Startup Folder feed -: automated Email";
			
			try {
				PermissionList permissions = DriveDesktopClient.DRIVE
						.permissions().list(folderID).execute();
				List<Permission> permList = permissions.getItems();
				
				if(permList.size() <= 1){
					return;
				}
				for (int i = 0; i < permList.size(); i++) {
					String receiverEmailAddress = permList.get(i)
							.getEmailAddress();

					try {
						System.out.println("sending mail to " + receiverEmailAddress);
						
						MimeMessage email = Utils.CreateEmail(
								receiverEmailAddress, senderEmailID,
								subject, msgBody.toString());
						Message message = Utils.CreateMessageWithEmail(email);

						mailService.users().messages()
								.send(senderEmailID, message)
								.execute();
					} catch (MessagingException e1) {

						e1.printStackTrace();
					}
				}

			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}
