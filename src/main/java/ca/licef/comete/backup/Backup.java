package ca.licef.comete.backup;

import ca.licef.comete.core.Core;
import licef.DateUtil;
import licef.IOUtil;
import licef.ZipUtil;
import licef.reflection.Invoker;
import licef.reflection.ThreadInvoker;
import licef.tsapi.Constants;
import licef.tsapi.TripleStore;

import java.io.File;
import java.text.MessageFormat;
import java.util.Date;

/**
 * Created by amiara on 2015-04-08.
 */
public class Backup {

    private static Backup instance;
    private boolean backupProcess;

    private String restoreMarker = "restore_process";

    public static Backup getInstance() {
        if (instance == null)
            instance = new Backup();
        return (instance);
    }

    public void backup() throws Exception {
        if (backupProcess)
            throw new Exception( "A backup is already in progress." );

        ThreadInvoker inv = new ThreadInvoker( new Invoker( this, "ca.licef.comete.backup.Backup", "doBackup", new Object[] {} ) );
        inv.start();
    }

    public void doBackup() {
        long startTime = System.currentTimeMillis();
        System.out.println( MessageFormat.format( "Starting backup: {0}.", DateUtil.toISOString( new Date( startTime ), null, null ) ) );
        backupProcess = true;

        try {
            //destination folder init
            String backupFolder = Core.getInstance().getCometeBackupHome();
            File backupDir = new File(backupFolder);
            if (!backupDir.exists())
                IOUtil.createDirectory(backupDir.getAbsolutePath());

            //dump of the triple store
            String dump = Core.getInstance().getCometeHome() + "/dump.trig";
            TripleStore tripleStore = Core.getInstance().getTripleStore();
            Invoker inv = new Invoker(tripleStore, "licef.tsapi.TripleStore", "dumpDataset",
                    new Object[]{dump, Constants.TRIG});
            tripleStore.transactionalCall(inv);

            //archive creation of comete data (including triple store dump)
            Date now = new Date();
            String isoDate = DateUtil.toISOString(now, null, null);
            isoDate = isoDate.substring(0, isoDate.indexOf("T"));
            int h =  now.getHours();
            int m = now.getMinutes();
            String archiveName = "comete_backup@" + isoDate + "T" + (h < 10?"0"+h:h) + "h" + (m < 10?"0"+m:m) + ".zip";
            ZipUtil.zipFile(Core.getInstance().getCometeHome(), backupFolder + "/" + archiveName, "database", "pages");

            //delete of the dump file
            (new File(dump)).delete();
            long stopTime = System.currentTimeMillis();

            double duration = ( stopTime - startTime ) / 1000.0;
            System.out.println( MessageFormat.format( "Backup completed successfully: {0} ({1}s).", 
                DateUtil.toISOString( new Date( stopTime ), null, null ), duration ) );
        }
        catch( Throwable t ) {
            long stopTime = System.currentTimeMillis();
            double duration = ( stopTime - startTime ) / 1000.0;
            System.out.println( MessageFormat.format( "Backup could not complete successfully: {0} ({1}s).", 
                DateUtil.toISOString( new Date( stopTime ), null, null ), duration ) );
            t.printStackTrace();
        }
        finally {
            backupProcess = false;
        }
    }

    public boolean restore(Core core) throws Exception {
        String cometeHome = core.getCometeHome();
        File home = new File(cometeHome);
        File[] files = home.listFiles();
        if (files == null || files.length != 3)  //3: database, pages + backup archive)
            return false;

        File archive = null;
        for (File f : files) {
            if (!f.isDirectory() && f.getName().startsWith("comete_backup") && f.getName().endsWith(".zip")) {
                archive = new File(home, f.getName());
                break;
            }
        }

        if (archive == null)
            return false;

        ThreadInvoker inv = new ThreadInvoker(new Invoker(this,
                "ca.licef.comete.backup.Backup", "doRestore", new Object[]{core, archive}));
        inv.start();

        return true;
    }

    public void doRestore(Core core, File archive) throws Exception {
        String cometeHome = core.getCometeHome();

        //put a marker before restore process
        File marker = new File(cometeHome, restoreMarker);
        IOUtil.writeStringToFile("", marker);

        //unzip archive
        ZipUtil.unzipFile(archive.getAbsolutePath(), cometeHome);

        //load dump content
        String dump = cometeHome + "/dump.trig";
        TripleStore tripleStore = core.getTripleStore();
        Invoker inv = new Invoker(tripleStore, "licef.tsapi.TripleStore", "loadDataset_textIndex",
                new Object[]{dump, Constants.TRIG, false});
        tripleStore.transactionalCall(inv, TripleStore.WRITE_MODE);

        //delete of the dump file
        (new File(dump)).delete();

        marker.delete();

        System.out.println("Comete restore done.");
    }

    public boolean isRestoreProcess() {
        File marker = new File(Core.getInstance().getCometeHome(), restoreMarker);
        return marker.exists();
    }
}
