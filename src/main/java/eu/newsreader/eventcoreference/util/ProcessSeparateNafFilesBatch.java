package eu.newsreader.eventcoreference.util;

import eu.kyotoproject.kaf.KafSaxParser;
import eu.newsreader.eventcoreference.naf.GetSemFromNafFile;
import eu.newsreader.eventcoreference.objects.SemObject;
import eu.newsreader.eventcoreference.objects.SemRelation;
import eu.newsreader.eventcoreference.objects.SemTime;
import eu.newsreader.eventcoreference.output.JenaSerialization;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;

/**
 * Created by piek on 2/12/14.
 */
public class ProcessSeparateNafFilesBatch {


    static public void main (String [] args) {
        try {
            String pathToNafFolder = "/Users/piek/Desktop/NWR-DATA/techcrunch/1_3000";
            String projectName  = "techcrunch";
            String extension = ".naf";
            for (int i = 0; i < args.length; i++) {
                String arg = args[i];
                if (arg.equals("--naf-folder") && args.length>(i+1)) {
                    pathToNafFolder = args[i+1];
                }
                else if (arg.equals("--project") && args.length>(i+1)) {
                    projectName = args[i+1];
                }
                else if (arg.equals("--extension") && args.length>(i+1)) {
                    extension = args[i+1];
                }
            }
            ArrayList<File> nafFiles = Util.makeRecursiveFileList(new File(pathToNafFolder), extension);
            KafSaxParser kafSaxParser = new KafSaxParser();
            for (int i = 0; i < nafFiles.size(); i++) {
                File file = nafFiles.get(i);
                //System.out.println("file.getName() = " + file.getName());
                kafSaxParser.parseFile(file);
                SemTime docSemTime = new SemTime();
                ArrayList<SemObject> semEvents = new ArrayList<SemObject>();
                ArrayList<SemObject> semActors = new ArrayList<SemObject>();
                ArrayList<SemObject> semTimes = new ArrayList<SemObject>();
                ArrayList<SemObject> semPlaces = new ArrayList<SemObject>();
                ArrayList<SemRelation> semRelations = new ArrayList<SemRelation>();
                ArrayList<SemRelation> factRelations = new ArrayList<SemRelation>();
                GetSemFromNafFile.processNafFile(projectName, kafSaxParser, semEvents, semActors, semPlaces, semTimes, semRelations, factRelations);
                FileOutputStream fos = new FileOutputStream(file.getAbsolutePath()+".trig");
                JenaSerialization.serializeJena(fos, semEvents, semActors, semPlaces, semTimes, semRelations, factRelations, null);
                fos.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
