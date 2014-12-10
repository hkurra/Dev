import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.gdrive.desktop.client.Global.DriveDesktopClient;
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

public class Utils {

	static boolean setProperty(String fileID, String propertyKey,
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

	static String getPropertyValue(String fileID, String propertKey) {

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

	static boolean IsWatching(String fileID) {

		boolean isWatching = true;

		String WatchingValue = null;
		WatchingValue = getPropertyValue(fileID, App.WATCHING_KEY);
		if (WatchingValue == null
				|| !WatchingValue.equals(App.WATCHING_VALUE_YES)) {
			isWatching = false;
		}
		return isWatching;
	}

	static boolean InitializeFolderWithOurTag(String folderID) {

		File folderMetadata = null;
		try {
			folderMetadata = DriveDesktopClient.DRIVE.files().get(folderID)
					.execute();
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		setProperty(folderID, App.LAST_ETAG, folderMetadata.getEtag());
		setProperty(folderID, App.WATCHING_KEY, App.WATCHING_VALUE_YES);

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
						setProperty(folderID, App.WATCHING_KEY,
								App.WATCHING_VALUE_YES);
						setProperty(childFile.getId(), App.LAST_ETAG,
								childFile.getEtag());
						setProperty(childFile.getId(), App.LAST_CHECKSUM,
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

	static HashMap<File, Changes> getFolderChanges(String folderID) {
		HashMap<File, Changes> changeList = new HashMap<File, Changes>();

		if (!IsWatching(folderID)) {
			changeList = getInterstedChangeFromServer(folderID);
			InitializeFolderWithOurTag(folderID);
		}

		else {
			changeList = FindChangesForFolder(folderID);
		}
		return changeList;
	}

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
				getPropertyValue(folderID, App.LAST_ETAG))) {
			folderChg.m_Metadata = true;
			changeList.put(folderMetadata, folderChg);
			setProperty(folderID, App.LAST_ETAG, folderMetadata.getEtag());
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

						setProperty(childFile.getId(), App.WATCHING_KEY,
								App.WATCHING_VALUE_YES);
						setProperty(childFile.getId(), App.LAST_ETAG,
								childFile.getEtag());
						setProperty(childFile.getId(), App.LAST_CHECKSUM,
								childFile.getMd5Checksum());
						continue;
					}
					if (childFile.getMimeType().equals(
							DriveDesktopClient.FOLDER_MIME_TYPE)) {
						if (!childFile.getEtag().equals(
								getPropertyValue(childFile.getId(),
										App.LAST_ETAG))) {
							chg.m_Metadata = true;
							changeList.put(childFile, chg);
							folderChg.m_Child = true;
							setProperty(childFile.getId(), App.LAST_ETAG,
									childFile.getEtag());
						}
						changeList.putAll(FindChangesForFolder(childFile
								.getId()));
					} else {
						if (!childFile.getEtag().equals(
								getPropertyValue(childFile.getId(),
										App.LAST_ETAG))) {
							chg.m_Metadata = true;
							changeList.put(childFile, chg);
							setProperty(childFile.getId(), App.LAST_ETAG,
									childFile.getEtag());
							folderChg.m_Child = true;
						}
						if (childFile.getMd5Checksum() != null
								&& !childFile.getMd5Checksum().equals(
										getPropertyValue(childFile.getId(),
												App.LAST_CHECKSUM))) {
							chg.m_Content = true;
							changeList.put(childFile, chg);
							setProperty(childFile.getId(), App.LAST_CHECKSUM,
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

	private static HashMap<File, Changes> getInterstedChangeFromServer(
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
}
