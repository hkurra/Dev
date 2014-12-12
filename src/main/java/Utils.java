import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;

import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import com.gdrive.desktop.client.Global.DriveDesktopClient;
import com.google.api.client.repackaged.org.apache.commons.codec.binary.Base64;
import com.google.api.client.util.DateTime;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.Drive.Properties.Get;
import com.google.api.services.drive.model.Change;
import com.google.api.services.drive.model.ChangeList;
import com.google.api.services.drive.model.ChildList;
import com.google.api.services.drive.model.ChildReference;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.ParentReference;
import com.google.api.services.drive.model.Property;
import com.google.api.services.gmail.model.Message;

public class Utils {

	/**
	 * set any custom property on google drive file
	 * 
	 * @param fileID
	 * @param propertyKey
	 * @param propertyValue
	 * @return
	 */
	static boolean SetProperty(String fileID, String propertyKey,
			String propertyValue) {

		boolean setEtag = true;
		Property property = new Property();
		property.setKey(propertyKey);
		property.setValue(propertyValue);
		property.setVisibility(App.Visibility);

		try {
			property = DriveDesktopClient.DRIVE.properties()
					.insert(fileID, property).execute();
		} catch (IOException e) {
			setEtag = false;
			e.printStackTrace();
		}
		return setEtag;

	}

	/**
	 * get any custom property on google drive file
	 * 
	 * @param fileID
	 * @param propertKey
	 * @return property value if exist else null
	 */
	static String GetPropertyValue(String fileID, String propertKey) {

		String lastEtag = null;
		Get request;
		try {
			request = DriveDesktopClient.DRIVE.properties().get(fileID,
					propertKey);
			request.setVisibility(App.Visibility);
			Property property = request.execute();
			lastEtag = property.getValue();
		} catch (IOException e) {
			lastEtag = null;
			e.printStackTrace();
		}
		return lastEtag;
	}

	/**
	 * Is file has IS_WATCHING property i.e this lib watching file for changes
	 * 
	 * @param fileID
	 * @return true if IS_WATCHING property exist on file else faklse
	 */
	static boolean IsWatching(String fileID) {

		boolean isWatching = true;

		String WatchingValue = null;
		WatchingValue = GetPropertyValue(fileID, App.WATCHING_KEY);
		if (WatchingValue == null
				|| !WatchingValue.equals(App.WATCHING_VALUE_YES)) {
			isWatching = false;
		}
		return isWatching;
	}

	/**
	 * set our custom property on folder & its child so that it will be easy
	 * identify any changes
	 * 
	 * @param folderID
	 * @return
	 */
	static boolean InitializeFolderWithOurTag(String folderID) {

		File folderMetadata = null;
		try {
			folderMetadata = DriveDesktopClient.DRIVE.files().get(folderID)
					.execute();
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		SetProperty(folderID, App.LAST_ETAG, folderMetadata.getEtag());
		SetProperty(folderID, App.WATCHING_KEY, App.WATCHING_VALUE_YES);

		Drive.Children.List request = null;
		try {
			request = DriveDesktopClient.DRIVE.children().list(folderID);
			do {
				ChildList children = (ChildList) request.execute();
				for (ChildReference child : children.getItems()) {
					File childFile = DriveDesktopClient.DRIVE.files()
							.get(child.getId()).execute();
					if (childFile.getMimeType().equals(
							DriveDesktopClient.FOLDER_MIME_TYPE)) {
						InitializeFolderWithOurTag(childFile.getId());
					} else {
						SetProperty(childFile.getId(), App.WATCHING_KEY,
								App.WATCHING_VALUE_YES);
						SetProperty(childFile.getId(), App.LAST_ETAG,
								childFile.getEtag());
						SetProperty(childFile.getId(), App.LAST_CHECKSUM,
								childFile.getMd5Checksum());
					}
				}
				request.setPageToken(children.getNextPageToken());
			} while ((request.getPageToken() != null)
					&& (request.getPageToken().length() > 0));
		} catch (Exception e) {
			e.printStackTrace();
		}
		return true;
	}

	/**
	 * find changes in folder & in its child
	 * 
	 * @param folderID
	 * @return pairing of file & its changes
	 */
	/**
	 * @param folderID
	 * @return
	 */
	public static HashMap<File, Changes> GetFolderChanges(String folderID) {
		HashMap<File, Changes> changeList = new HashMap<File, Changes>();

		if (!IsWatching(folderID)) {
			changeList = GetInterstedChangeFromServer(folderID);
			InitializeFolderWithOurTag(folderID);
		}

		else {
			changeList = FindChangesForFolder(folderID);
		}
		return changeList;
	}

	/**
	 * find changes on file if we are observing it i.e already set our custom
	 * property
	 * 
	 * @param folderID
	 * @return
	 */
	/**
	 * @param folderID
	 * @return
	 */
	private static HashMap<File, Changes> FindChangesForFolder(String folderID) {
		HashMap<File, Changes> changeList = new HashMap<File, Changes>();
		Changes folderChg = new Changes();
		File folderMetadata = null;
		try {
			folderMetadata = DriveDesktopClient.DRIVE.files().get(folderID)
					.execute();
		} catch (IOException e1) {
			e1.printStackTrace();
		}

		if (!folderMetadata.getEtag().equals(
				GetPropertyValue(folderID, App.LAST_ETAG))) {
			folderChg.m_Metadata = true;
			changeList.put(folderMetadata, folderChg);
			SetProperty(folderID, App.LAST_ETAG, folderMetadata.getEtag());
		}
		Drive.Children.List request = null;
		try {
			request = DriveDesktopClient.DRIVE.children().list(folderID);
			do {
				ChildList children = (ChildList) request.execute();
				for (ChildReference child : children.getItems()) {
					File childFile = DriveDesktopClient.DRIVE.files()
							.get(child.getId()).execute();
					Changes chg = new Changes();
					if (!IsWatching(childFile.getId())) {
						chg.m_Child = true;
						chg.m_UnwatchedFile = true;
						changeList.put(childFile, chg);

						SetProperty(childFile.getId(), App.WATCHING_KEY,
								App.WATCHING_VALUE_YES);
						SetProperty(childFile.getId(), App.LAST_ETAG,
								childFile.getEtag());
						SetProperty(childFile.getId(), App.LAST_CHECKSUM,
								childFile.getMd5Checksum());
						continue;
					}
					if (childFile.getMimeType().equals(
							DriveDesktopClient.FOLDER_MIME_TYPE)) {
						if (!childFile.getEtag().equals(
								GetPropertyValue(childFile.getId(),
										App.LAST_ETAG))) {
							chg.m_Metadata = true;
							changeList.put(childFile, chg);
							folderChg.m_Child = true;
							SetProperty(childFile.getId(), App.LAST_ETAG,
									childFile.getEtag());
						}
						changeList.putAll(FindChangesForFolder(childFile
								.getId()));
					} else {
						if (!childFile.getEtag().equals(
								GetPropertyValue(childFile.getId(),
										App.LAST_ETAG))) {
							chg.m_Metadata = true;
							changeList.put(childFile, chg);
							SetProperty(childFile.getId(), App.LAST_ETAG,
									childFile.getEtag());
							folderChg.m_Child = true;
						}
						if (childFile.getMd5Checksum() != null
								&& !childFile.getMd5Checksum().equals(
										GetPropertyValue(childFile.getId(),
												App.LAST_CHECKSUM))) {
							chg.m_Content = true;
							changeList.put(childFile, chg);
							SetProperty(childFile.getId(), App.LAST_CHECKSUM,
									childFile.getMd5Checksum());
							folderChg.m_Child = true;
						}
					}
				}
				request.setPageToken(children.getNextPageToken());
			} while ((request.getPageToken() != null)
					&& (request.getPageToken().length() > 0));
		} catch (Exception e) {
			e.printStackTrace();
		}

		return changeList;
	}

	/**
	 * find changes for file by iterating all changes from server i.e use if
	 * file dont have custom property
	 * 
	 * @param folderID
	 * @return
	 */
	private static HashMap<File, Changes> GetInterstedChangeFromServer(
			String folderID) {

		HashMap<File, Changes> changeList = new HashMap<File, Changes>();

		List<Change> result = new ArrayList<Change>();
		com.google.api.services.drive.Drive.Changes.List request = null;
		try {
			request = DriveDesktopClient.DRIVE.changes().list();
		} catch (IOException e) {
			e.printStackTrace();
		}

		do {
			try {
				ChangeList changes = request.execute();

				result.addAll(changes.getItems());
				request.setPageToken(changes.getNextPageToken());
			} catch (Exception ex) {

			}
		} while (request.getPageToken() != null
				&& request.getPageToken().length() > 0);

		List<Change> interstedResult = new ArrayList<Change>();
		for (Change chg : result) {

			File file = chg.getFile();
			if (file == null)
				continue;
			List<ParentReference> parentFolder = chg.getFile().getParents();

			for (ParentReference parent : parentFolder) {
				if (parent.getId().equals(folderID)) {
					interstedResult.add(chg);
				}
			}
			if (chg.getFileId().equals(folderID)) {
				interstedResult.add(chg);
			}
		}

		if (interstedResult.size() == 0) {
			return null;
		} else {
			for (Change interChanges : interstedResult) {
				Changes myChange = new Changes();
				myChange.m_UnwatchedFile = true;
				changeList.put(interChanges.getFile(), myChange);
			}
		}
		return changeList;
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
	public static MimeMessage CreateEmail(String to, String from,
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
	public static Message CreateMessageWithEmail(MimeMessage email)
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