package ca.licef.comete.queryengine.resource;

import ca.licef.comete.core.Core;
import com.sun.syndication.feed.WireFeed;
import com.sun.syndication.feed.atom.Feed;
import com.sun.syndication.feed.rss.Channel;
import com.sun.syndication.feed.synd.SyndFeed;
import com.sun.syndication.io.FeedException;
import com.sun.syndication.io.WireFeedOutput;

import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

@Provider
@Produces( { MediaType.APPLICATION_ATOM_XML, "application/rss+xml" } )
public class RomeSupport implements MessageBodyWriter<Object>, MessageBodyReader<Object> {

    public long getSize( Object t, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType ) {
        return( -1 );
    }

    public boolean isWriteable( Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType ) {
        return( SyndFeed.class.isAssignableFrom( type ) );
    }

    public void writeTo(Object feedOrEntry, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType, MultivaluedMap<String,Object> headers, OutputStream outputStream ) {
        if( feedOrEntry instanceof SyndFeed ) {
            SyndFeed feed = (SyndFeed)feedOrEntry;

            try {
                WireFeed wirefeed = feed.createWireFeed();
                if( wirefeed instanceof Channel ) {
                    // Specific metadata for RSS feed.
                    Channel channel = (Channel)wirefeed;
                    channel.setDocs( "http://cyber.law.harvard.edu/rss/rss.html" );
                    channel.setGenerator( "Comete " + Core.getInstance().getVersion() ); 
                    channel.setWebMaster( Core.getInstance().getAdminEmail() );
                }
                else if( wirefeed instanceof Feed ) {
                    // Specific metadata for Atom feed.
                }

                WireFeedOutput output = new WireFeedOutput();
                output.output( wirefeed, new OutputStreamWriter( outputStream ) );
            }
            catch( FeedException fe ) {
                fe.printStackTrace();
            }
            catch( IOException ioe ) {
                ioe.printStackTrace();
            }
        } 
    }


    public boolean isReadable( Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType ) {
        // Not needed for now.
        return( false ); 
    }

    public Object readFrom( Class<Object> feedOrEntry, Type genericType, Annotation[] annotations, MediaType mediaType,
           MultivaluedMap<java.lang.String,java.lang.String> httpHeaders, InputStream entityStream ) throws IOException, WebApplicationException {
       throw( new IOException( "Unsupported payload: not implemented yet." ) );
    }

}

