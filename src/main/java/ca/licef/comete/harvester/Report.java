package ca.licef.comete.harvester;

import licef.DateUtil;
import licef.IOUtil;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: amiara
 * Date: 14-04-03
 */
public class Report {

    String defId;
    String from;
    File defFolder;

    Date startDate;
    int cptAdded = 0;
    int cptUpdated = 0;
    int cptDeleted = 0;
    
    List<Error> errors;

    public Report(String defId, Date startDate) {
        this(defId, startDate, null);
    }

    public Report(String defId, Date startDate, String from) {
        this.defId = defId;
        this.from = from;
        this.startDate = startDate;
        defFolder = new File( Harvester.HARVESTER_FOLDER, defId );
        errors = new ArrayList<Error>();

        generateStartReport();
    }

    public void incrementAdded() {
        cptAdded++;
    }

    public void incrementUpdated() {
        cptUpdated++;
    }

    public void incrementDeleted() {
        cptDeleted++;
    }

    public void addError( Error error ) {
        errors.add( error );
    }

    String getStartReport() {
        String startISODatetime = DateUtil.toISOString(startDate, null, null);
        //report
        StringBuilder sb = new StringBuilder();
        sb.append("Harvest Report").append("\r\n");
        sb.append("--------------").append("\r\n\r\n");
        sb.append("Repository: " ).append(defId).append("\r\n");
        if( from != null ) 
            sb.append("From Date:  ").append(from).append("\r\n\r\n");

        //report time
        sb.append("Start time: ").append(startISODatetime).append("\r\n");
        return sb.toString();
    }

    public String getReportName() {
        String startISODatetime = DateUtil.toISOString(startDate, null, null);
        String startISODate = startISODatetime.substring(0, startISODatetime.indexOf("T"));
        int h =  startDate.getHours();
        int m = startDate.getMinutes();
        return defId + "@" + startISODate + "T" + (h < 10?"0"+h:h) + "h" + (m < 10?"0"+m:m) + ".txt";
    }

    public void generateStartReport() {
        String report = getStartReport() + "\r\nIn progress...";
        try {
            IOUtil.writeStringToFile(report, new File(defFolder, getReportName()));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void generateReport(Date endDate, Throwable t, boolean wasInterrupted) throws Exception {
        String startISODatetime = DateUtil.toISOString(startDate, null, null);
        String startISODate = startISODatetime.substring(0, startISODatetime.indexOf("T"));
        String endISODatetime = DateUtil.toISOString(endDate, null, null);

        File defFolder = new File( Harvester.HARVESTER_FOLDER, defId );

        //report
        StringBuilder sb = new StringBuilder();
        sb.append(getStartReport());

        //report time next
        sb.append("End time:   ").append(endISODatetime).append("\r\n");
        long[] duration = DateUtil.getDuration(startDate, endDate);
        String _dur = "";
        if (duration[0] > 0) _dur += duration[0] + " day ";
        if (duration[1] > 0) _dur += duration[1] + " hour ";
        if (duration[2] > 0) _dur += duration[2] + " min ";
        if (duration[3] > 0) _dur += duration[3] + " sec";
        if ("".equals(_dur))
            _dur = "none";
        sb.append("Duration:   " ).append(_dur).append("\r\n\r\n");

        //report results
        sb.append("New records:     " ).append("").append(cptAdded).append("\r\n");
        sb.append("Updated records: " ).append(cptUpdated).append("\r\n");
        sb.append("Deleted records: " ).append(cptDeleted).append("\r\n\r\n");

        //report status
        if (t == null)
            if( wasInterrupted )
                sb.append("Status: Interrupted manually before completion.\r\n\r\n" );
            else
                sb.append("Status: Completed successfully.\r\n\r\n" );
        else {
            StringWriter trace = new StringWriter();
            t.printStackTrace(new PrintWriter(trace));
            sb.append("Status: Uncompleted process.\r\n\r\n").append(trace);
        }

        //errors
        if( errors.size() > 0 ) {
            sb.append( errors.size() + " non-fatal errors occurred during execution:\r\n\r\n" );
            for( Error error : errors ) {
                StringWriter trace = new StringWriter();
                error.getError().printStackTrace( new PrintWriter( trace ) );
                sb.append( "On record " ).append( error.getIdentifier() );
                if( error.getUrl() != null && !"".equals( error.getUrl() ) )
                    sb.append( " (" ).append( error.getUrl() ).append( ")" );
                sb.append( "\r\n\r\n" );
                sb.append( trace );
                sb.append( "\r\n\r\n\r\n" );
            }
        }

        IOUtil.writeStringToFile(sb.toString(), new File(defFolder, getReportName()));
    }
}
