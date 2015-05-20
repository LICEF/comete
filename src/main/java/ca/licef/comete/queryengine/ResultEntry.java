package ca.licef.comete.queryengine;

public class ResultEntry {

    public ResultEntry() {
    }

    public ResultEntry( String id, String title, String location, String metadataFormat, 
            boolean isHidden, boolean isPending, boolean isInactive, boolean isInvalid, boolean isBrokenLink, String type ) {
        this.id = id;
        this.title = title;
        this.location = location;
        this.metadataFormat = metadataFormat;
        this.isHidden = isHidden;
        this.isPending = isPending;
        this.isInactive = isInactive;
        this.isInvalid = isInvalid;
        this.isBrokenLink = isBrokenLink;
        this.type = type; 
    }

    public String getId() {
        return( id );
    }

    public void setId( String id ) {
        this.id = id;
    }

    public String getTitle() {
        return( title );
    }

    public void setTitle( String title ) {
        this.title = title;
    }

    public String getDescription() {
        return( description );
    }

    public void setDescription( String description ) {
        this.description = description;
    }

    public String getLocation() {
        return( location );
    }

    public void setLocation( String location ) {
        this.location = location;
    }

    public String getImage() {
        return ( image );
    }

    public void setImage(String image) {
        this.image = image;
    }

    public String getLoAsHtmlLocation() {
        return(loAsHtmlLocation);
    }

    public void setLoAsHtmlLocation( String location ) {
        this.loAsHtmlLocation = location;
    }

    public String getCreationDate() {
        return( creationDate );
    }

    public void setCreationDate( String creationDate ) {
        this.creationDate = creationDate;
    }

    public String getModificationDate() {
        return( modificationDate );
    }

    public void setModificationDate( String modificationDate ) {
        this.modificationDate = modificationDate;
    }

    public String getMetadataFormat() {
        return( metadataFormat );
    }

    public void setMetadataFormat( String metadataFormat ) {
        this.metadataFormat = metadataFormat;
    }

    public boolean isHidden() {
        return( isHidden );
    }

    public void setHidden( boolean isHidden ) {
        this.isHidden = isHidden;
    }

    public boolean isPending() {
        return( isPending );
    }

    public void setPending( boolean isPending ) {
        this.isPending = isPending;
    }

    public boolean isInactive() {
        return( isInactive );
    }

    public void setInactive( boolean isInactive ) {
        this.isInactive = isInactive;
    }

    public boolean isInvalid() {
        return( isInvalid );
    }

    public void setInvalid( boolean isInvalid ) {
        this.isInvalid = isInvalid;
    }

    public boolean isBrokenLink() {
        return( isBrokenLink );
    }

    public void setBrokenLink( boolean isBrokenLink ) {
        this.isBrokenLink = isBrokenLink;
    }

    public String getType() {
        return( type );
    }

    public void setType( String type ) {
        this.type = type;
    }

    private String id;
    private String title;
    private String description;
    private String location;
    private String image;
    private String loAsHtmlLocation;
    private String creationDate;
    private String modificationDate;
    private String metadataFormat;
    private boolean isHidden;
    private boolean isPending;
    private boolean isInactive;
    private boolean isInvalid;
    private boolean isBrokenLink;
    private String type;

}

