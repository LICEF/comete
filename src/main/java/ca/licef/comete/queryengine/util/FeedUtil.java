package ca.licef.comete.queryengine.util;

import ca.licef.comete.core.util.ResultSet;
import ca.licef.comete.vocabularies.COMETE;
import com.hp.hpl.jena.vocabulary.RDFS;
import com.sun.syndication.feed.synd.SyndFeed;
import com.sun.syndication.feed.synd.SyndFeedImpl;
import licef.StringUtil;
import licef.tsapi.model.Triple;
import licef.tsapi.vocabulary.DCTERMS;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

public class FeedUtil {

    public static SyndFeed getFeedFromResultSet( ResultSet rs, String feedType, String absPath, String query, int start, int limit, String lang ) {
        ResourceBundle bundle = ResourceBundle.getBundle( "Strings", new Locale( lang ) );

        SyndFeed f = new SyndFeedImpl();
        /*f.setFeedType( feedType );

        List modules = f.getModules();
        OpenSearchModule osm = new OpenSearchModuleImpl();

        f.setTitle( rs.getTitle() == null ? bundle.getString( "feed.title" ) : rs.getTitle() );
        f.setDescription( rs.getDescription() == null ? bundle.getString( "feed.description" ) : rs.getDescription() ); // Required by RSS.
        f.setLanguage( lang );
        f.setCopyright( bundle.getString( "feed.copyright" ) );

        SyndImage image = new SyndImageImpl();
        image.setTitle( bundle.getString( "feed.image.title" ) );
        image.setDescription( bundle.getString( "feed.image.description" ) );
        image.setLink( Core.getInstance().getCometeUrl() );
        image.setUrl( Core.getInstance().getCometeUrl() + "/images/cometeLogo.gif" );
        f.setImage( image );

        String feedId = absPath + "?start=" + start + "&limit=" + limit;
        if( !"".equals( query ) ) {
            String encodedQuery = null;
            try {
                encodedQuery = URLEncoder.encode( query, "UTF-8" );
            }
            catch( UnsupportedEncodingException ignore ) {
                // Should never happen.
            }
            if( encodedQuery != null )
                feedId += "&" + encodedQuery;
        }

        f.setUri( feedId );
        f.setPublishedDate( new Date() );

        OSQuery q = new OSQuery();
        q.setRole( "request" );
        q.setSearchTerms( query );
        osm.addQuery( q );

        f.setLink( Core.getInstance().getCometeUrl() );

        osm.setTotalResults( rs.getTotalRecords() );
        osm.setItemsPerPage( rs.getLimit() );
        osm.setStartIndex( rs.getStart() );

        List entries = new ArrayList();
        for( ListIterator it = rs.getEntries(); it.hasNext(); ) {
            ResultEntry resEntry = (ResultEntry)it.next();

            SyndEntry entry = new SyndEntryImpl();
            
            List entryModules = entry.getModules();
            DCModule dc = new DCModuleImpl();
            CometeModule cm = new CometeModuleImpl();

            List categories = new ArrayList();

            String uri = CoreUtil.makeURI( resEntry.getId(), COMETE.LearningObject);
            entry.setUri( uri );
            if( resEntry.getTitle() != null )
                entry.setTitle( resEntry.getTitle() );

            if( resEntry.getDescription() != null ) {
                SyndContent description = new SyndContentImpl();
                description.setType( "text/plain" );
                description.setValue( resEntry.getDescription() );
                entry.setDescription( description );
            }

            Date publicationDate = getValidDate( resEntry.getCreationDate() );
            if( publicationDate != null )
                entry.setPublishedDate( publicationDate );

            Date modifDate = getValidDate( resEntry.getModificationDate() );
            if (modifDate == null)
                modifDate = publicationDate;
            if( modifDate != null )
                entry.setUpdatedDate( modifDate );

            try {
                List titles = new ArrayList();
                List descriptions = new ArrayList();
                List creators = new ArrayList();
                List publishers = new ArrayList();
                List formats = new ArrayList();
                List languages = new ArrayList();
                List subjects = new ArrayList();
                List extraInfos = new ArrayList();
                List keywords = new ArrayList();

                Triple[] tripleArray = Core.getInstance().getDefaultView().getTriples( uri, "false", false, true ); 

                ArrayList<Triple> triples = new ArrayList<Triple>(Arrays.asList( tripleArray ));
                Map labels = extractLabels( triples );
                Map emails = buildEmailTable( triples );
                for( Triple triple : triples ) {
                    if( Constants.METAMODEL_TITLE.equals( triple.getPredicate() ) ) {
                        LangString langString = new LangStringImpl();
                        langString.setString( triple.getObject() );
                        if( triple.getLanguage() != null && !"".equals( triple.getLanguage() ) )
                            langString.setLanguage( triple.getLanguage() );
                        titles.add( langString );
                    }
                    else if( Constants.METAMODEL_DESCRIPTION.equals( triple.getPredicate() ) ) {
                        LangString langString = new LangStringImpl();
                        langString.setString( triple.getObject() );
                        if( triple.getLanguage() != null && !"".equals( triple.getLanguage() ) )
                            langString.setLanguage( triple.getLanguage() );
                        descriptions.add( langString );
                    }
                    else if( Constants.METAMODEL_LANGUAGE.equals( triple.getPredicate() ) )
                        languages.add( triple.getObject() ); 
                    else if( Constants.METAMODEL_FORMAT.equals( triple.getPredicate() ) )
                        formats.add( triple.getObject() );
                    else if( Constants.METAMODEL_AUTHOR.equals( triple.getPredicate() ) ) {
                        String identityUri = triple.getObject();
                        Map creatorStrings = (Map)labels.get( identityUri );
                        String creator = (String)creatorStrings.get( lang );
                        if( creator == null )
                            creator = (String)creatorStrings.get( "--" );
                        String email = ( emails.get( identityUri ) == null ? "" : "(" + emails.get( identityUri ) + ") " );
                        creator += " " + email + "[" + identityUri + "]" ;
                        creators.add( creator );
                    }
                    else if( Constants.METAMODEL_PUBLISHER.equals( triple.getPredicate() ) ) {
                        String identityUri = triple.getObject();
                        Map publisherStrings = (Map)labels.get( identityUri );
                        String publisher = (String)publisherStrings.get( lang );
                        if( publisher == null )
                            publisher = (String)publisherStrings.get( "--" );
                        String email = ( emails.get( identityUri ) == null ? "" : "(" + emails.get( identityUri ) + ") " );
                        publisher += " " + email + "[" + identityUri + "]";
                        publishers.add( publisher );
                    }
                    else if( Constants.METAMODEL_SUBJECT.equals( triple.getPredicate() ) ) {
                        String subjectUri = triple.getObject();
                        int indexOfLastSlash = subjectUri.lastIndexOf( "/" );
                        String taxonomyUri = subjectUri.substring( 0, indexOfLastSlash );
                        String value = subjectUri.substring( indexOfLastSlash + 1 );

                        DCSubject subject = new DCSubjectImpl();
                        subject.setTaxonomyUri( taxonomyUri );
                        LangString langString = new LangStringImpl();
                        langString.setString( value );
                        subject.setValue( langString );
                        subjects.add( subject );
                        
                        SyndCategory category = new SyndCategoryImpl();
                        category.setTaxonomyUri( taxonomyUri );
                        category.setName( value );
                        categories.add( category );
                    }
                    else if( Constants.METAMODEL_EXTRA_INFO.equals( triple.getPredicate() ) ) {
                        LangString langString = new LangStringImpl();
                        langString.setString( triple.getObject() );
                        if( triple.getLanguage() != null && !"".equals( triple.getLanguage() ) )
                            langString.setLanguage( triple.getLanguage() );
                        extraInfos.add( langString );
                    }
                    else if( Constants.METAMODEL_KEYWORD.equals( triple.getPredicate() ) ) {
                        LangString langString = new LangStringImpl();
                        langString.setString( triple.getObject() );
                        if( triple.getLanguage() != null && !"".equals( triple.getLanguage() ) )
                            langString.setLanguage( triple.getLanguage() );
                        keywords.add( langString );
                    }
                    else if( Constants.METAMODEL_ADDED.equals( triple.getPredicate() ) ) {
                        Date added = DateParser.parseDate( triple.getObject() );
                        cm.setAdded( added );
                    }
                    else if( Constants.METAMODEL_UPDATED.equals( triple.getPredicate() ) ) {
                        Date updated = DateParser.parseDate( triple.getObject() );
                        cm.setUpdated( updated );
                    }
                }
                dc.setTitles( titles );
                dc.setDescriptions( descriptions );
                dc.setCreators( creators );
                dc.setPublishers( publishers );
                dc.setFormats( formats );
                dc.setLanguages( languages );
                dc.setSubjects( subjects );
                cm.setExtraInfos( extraInfos );
                cm.setKeywords( keywords );

                dc.setIdentifier( resEntry.getLocation() );
            }
            catch( Exception e ) {
                e.printStackTrace();
            }
            entry.setLink( resEntry.getLocation()  );

            entryModules.add( dc );
            entryModules.add( cm );

            entry.setCategories( categories );

            entries.add( entry );
        }
        f.setEntries( entries );

        modules.add( osm );
        f.setModules( modules );
*/
        // Implementation specific metadata for the feed are added via RomeSupport.writeTo() method. - FB

        return f;
    }

    private static Date getValidDate( String strDate ) {
        if (strDate == null)
            return null;
        try {
            Date date = dateFormatter.parse( strDate );
            return( date );
        }
        catch( ParseException e ) {
            try {
                Date date2 = dateFormatter2.parse( strDate );
                return( date2 );
            }
            catch( ParseException e2 ) {
                // The date is invalid.
                return( null );
            }
        }
    }

    /* Beware, this method removes label triples. - FB */
    private static Map extractLabels( ArrayList<Triple> triples ) {
        Map labels = new HashMap();
        for( ListIterator it = triples.listIterator(); it.hasNext(); ) {
            Triple triple = (Triple)it.next();
            if(RDFS.label.equals( triple.getPredicate() ) ) {
                Map strings = (Map)labels.get( triple.getSubject() );
                if( strings == null ) {
                    strings = new HashMap();
                    labels.put( triple.getSubject(), strings );
                }
                strings.put( triple.getLanguage() == null ? "--" : triple.getLanguage(), triple.getObject() );
                it.remove();
            }
        }
        return( labels );
    }

    private static Map buildEmailTable( ArrayList<Triple> triples ) {
        Map emails = new HashMap();

        ArrayList<String> identities = new ArrayList<String>();
        for( ListIterator it = triples.listIterator(); it.hasNext(); ) {
            Triple triple = (Triple)it.next();
            if( DCTERMS.creator.equals(triple.getPredicate()) ||
                DCTERMS.publisher.equals( triple.getPredicate() ) ) {
                identities.add( triple.getObject() );
            }
        }

        if( identities.size() > 0 ) {
            String[] identityArray = identities.toArray( new String[ identities.size() ] );

            String url = null;
            try {
                url = CoreUtil.getRestUrl(COMETE.Identity.getURI()) + "/emails?uris=" +
                    URLEncoder.encode( "[" + StringUtil.join( identityArray, '"', ',' ) + "]", "UTF-8" );
            }
            catch( UnsupportedEncodingException ignore ) {
                ignore.printStackTrace();
            }

            /*WebResource webResource = Core.getInstance().getRestClient().resource( url );
            ClientResponse response = webResource.accept( MediaType.APPLICATION_JSON ).get( ClientResponse.class );
            int status = response.getStatus();
            if( status == 200 ) {
                String res = response.getEntity( String.class );
                try {
                    JSONObject json = new JSONObject(res);
                    for( Iterator it = json.keys(); it.hasNext(); ) {
                        String uri = (String)it.next();
                        String email = (String)json.get( uri );
                        int indexOfMailToPrefix = email.indexOf( "mailto:" );
                        if( indexOfMailToPrefix != -1 )
                            email = email.substring( indexOfMailToPrefix + "mailto:".length() );
                        if( !"".equals( email ) )
                            emails.put( uri, email );
                    }
                } 
                catch (JSONException shouldNeverHappend) {
                    // The table will be empty but it's no big deal.
                    shouldNeverHappend.printStackTrace();
                }
            }*/
        }
        
        return( emails );
    }

    private static SimpleDateFormat dateFormatter = new SimpleDateFormat( "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'" );
    private static SimpleDateFormat dateFormatter2 = new SimpleDateFormat( "yyyy-MM-dd'T'HH:mm:ss'Z'" );

    private static ca.licef.comete.core.util.Util CoreUtil;

}