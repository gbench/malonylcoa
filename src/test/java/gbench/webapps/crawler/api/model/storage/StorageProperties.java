package gbench.webapps.crawler.api.model.storage;

/**
 * 
 */
public class StorageProperties {
	
	public StorageProperties(final String location){
		this.location = location;
	}

	/**
	 * Folder location for storing files
	 */
	private String location = "upload-dir";

	public String getLocation() {
		return location;
	}

	public void setLocation(String location) {
		this.location = location;
	}

}