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
import com.google.api.services.drive.model.Permission;
import com.google.api.services.drive.model.PermissionList;
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
	//0BzehUff2oW6HMHBYVFFnXzNlVmc
	
	public static String folderID = "0BzehUff2oW6HUmxoUFk4bTVmWnc";
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

			com.google.api.services.drive.model.Property prop = null;

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

			Iterator it = changeList.entrySet().iterator();
			boolean changeFolderMetadata = false;

			if (changeList.size() > 0) {
				System.out
						.println("following changes detected since your last query");
			}

			StringBuffer msgBody = new StringBuffer(
					"following changes detected since your last Feed \n\n");
			while (it.hasNext()) {
				Map.Entry pairs = (Map.Entry) it.next();
				File key = (File) pairs.getKey();
				Changes value = (Changes) pairs.getValue();

				if (key.getId().equals(folderID)) {
					changeFolderMetadata = false;
					
					StringBuffer changesToPrint = new StringBuffer("");
					
					if (value.m_Metadata) {
						changesToPrint.append(" metadata(name etc) ");
					}
					if (value.m_UnwatchedFile) {
						changesToPrint.append(" new Added ");
					}
					msgBody.append("change in folder " + changesToPrint + " by "
							+ key.getLastModifyingUserName() + " on "
							+ key.getModifiedDate() + " GMT " + "\n\n");
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
			System.out.print(msgBody);
			String senderEmailID = "er.harshkurra@gmail.com";
			String subject = "Folder feed -: automated Email";
			
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
