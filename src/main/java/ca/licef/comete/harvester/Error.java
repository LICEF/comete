package ca.licef.comete.harvester;

public class Error {

    public Error( String identifier, String url, Throwable error ) {
        this.identifier = identifier;
        this.url = url;
        this.error = error;
    }

    public Error( String identifier, Throwable error ) {
        this( identifier, null, error );
    }

    public String toString() {
        StringBuilder str = new StringBuilder();
        str.append( identifier );
        if( url != null && !"".equals( url ) )
            str.append( " (" ).append( url ).append( ")" );
        str.append( ": " ).append( error.toString() );
        return( str.toString() );
    }

    public String getIdentifier() {
        return( identifier );
    }

    public String getUrl() {
        return( url );
    }

    public Throwable getError() {
        return( error );
    }

    private String identifier;
    private String url;
    private Throwable error;

}
