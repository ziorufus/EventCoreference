package eu.newsreader.eventcoreference.naf;

import eu.kyotoproject.kaf.KafSaxParser;
import eu.kyotoproject.kaf.KafSense;
import eu.newsreader.eventcoreference.coref.ComponentMatch;
import eu.newsreader.eventcoreference.objects.*;
import eu.newsreader.eventcoreference.util.Util;

import java.io.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.TreeSet;

/**
 * Created with IntelliJ IDEA.
 * User: Piek
 * Date: 11/14/13
 * Time: 7:54 PM
 * To change this template use File | Settings | File Templates.
 */
public class ClusterEventObjects {


    /*
        @TODO
        1. proper reference to the ontologies (even if not there yet)
        2. potential bug in the labels (discussed at meeting and waiting for feedback from EHU)
        3. very long labels
        4. excessively long labels
        5. include the fragment roles in the relations
        6. exclude excessive event-coreference for high-frequent verbs
        7. parametrize the module to get high-precision or high-recall TriG
     */

    static final String USAGE = "This program processes NAF files and stores binary objects for events with all related data in different object files based on the event type and the date\n" +
            "The program has the following arguments:\n" +
            "--naf-folder     <Folder with the NAF files to be processed. Reads NAF files recursively>\n" +
            "--event-folder   <Folder below which the event folders are created that hold the object file. " +
            "The output structure is event/other, event/grammatical and event/speech.>\n" +
            "--extension      <File extension to select the NAF files .>\n" +
            "--project        <The name of the project for creating URIs>\n";


    static public void main (String [] args) {
        if (args.length==0) {
            System.out.println(USAGE);
            System.out.println("NOW RUNNING WITH DEFAULT SETTINGS");
          //  return;
        }
       // String pathToNafFolder = "/Users/piek/Desktop/NWR/NWR-ontology/reasoning/change-of-scale";
       // String pathToEventFolder = "/Users/piek/Desktop/NWR/NWR-ontology/reasoning/change-of-scale";
        String pathToNafFolder = "/Users/piek/Desktop/NWR/NWR-DATA/cars/car-work-sample/naf";
        String pathToEventFolder = "/Users/piek/Desktop/NWR/NWR-DATA/cars/car-work-sample";
        //String pathToNafFolder = "/Users/piek/Desktop/NWR/NWR-DATA/worldcup/ian-test";
        //String pathToEventFolder = "/Users/piek/Desktop/NWR/NWR-DATA/worldcup";
       // String pathToNafFolder = "/Code/vu/newsreader/EventCoreference/LN_football_test_out-tiny";
        String projectName  = "cars";
        String extension = ".naf.coref";
        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            if (arg.equals("--naf-folder") && args.length>(i+1)) {
                pathToNafFolder = args[i+1];
            }
            else if (arg.equals("--event-folder") && args.length>(i+1)) {
                pathToEventFolder = args[i+1];
            }
            else if (arg.equals("--extension") && args.length>(i+1)) {
                extension = args[i+1];
            }
            else if (arg.equals("--project") && args.length>(i+1)) {
                projectName = args[i+1];
            }
        }
        try {
            processFolderEvents(projectName, new File(pathToNafFolder), new File (pathToEventFolder), extension);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void processFolderEvents (String project, File pathToNafFolder, File eventParentFolder, String extension

    ) throws IOException {
        File eventFolder = new File(eventParentFolder + "/events");
        if (!eventFolder.exists()) {
            eventFolder.mkdir();
        }
        if (!eventFolder.exists()) {
            System.out.println("Cannot create the eventFolder = " + eventFolder);
            return;
        }
        File speechFolder = new File(eventFolder + "/" + "source");
        if (!speechFolder.exists()) {
            speechFolder.mkdir();
        }
        if (!speechFolder.exists()) {
            System.out.println("Cannot create the speechFolder = " + speechFolder);
            return;
        }
        File otherFolder = new File(eventFolder + "/" + "contextual");
        if (!otherFolder.exists()) {
            otherFolder.mkdir();
        }
        if (!otherFolder.exists()) {
            System.out.println("Cannot create the otherFolder = " + otherFolder);
            return;
        }
        File grammaticalFolder = new File(eventFolder + "/" + "grammatical");
        if (!grammaticalFolder.exists()) {
            grammaticalFolder.mkdir();
        }
        if (!grammaticalFolder.exists()) {
            System.out.println("Cannot create the grammaticalFolder = " + grammaticalFolder);
            return;
        }

        KafSaxParser kafSaxParser = new KafSaxParser();
        ArrayList<SemObject> semEvents = new ArrayList<SemObject>();
        ArrayList<SemObject> semActors = new ArrayList<SemObject>();
        ArrayList<SemObject> semTimes = new ArrayList<SemObject>();
        ArrayList<SemObject> semPlaces = new ArrayList<SemObject>();
        ArrayList<SemRelation> semRelations = new ArrayList<SemRelation>();
        ArrayList<SemRelation> factRelations = new ArrayList<SemRelation>();

        ArrayList<File> files = Util.makeRecursiveFileList(pathToNafFolder, extension);
        System.out.println("files.size() = " + files.size());
        for (int i = 0; i < files.size(); i++) {
            File file = files.get(i);
/*            if (!file.getName().startsWith("7YXG-CS51-2RYC-J2CN.xml")) {
                     continue;
            }*/

            if (i % 10 == 0) {
                System.out.println("i = " + i);
                //  System.out.println("file.getName() = " + file.getAbsolutePath());
            }
            semEvents = new ArrayList<SemObject>();
            semActors = new ArrayList<SemObject>();
            semTimes = new ArrayList<SemObject>();
            semPlaces = new ArrayList<SemObject>();
            semRelations = new ArrayList<SemRelation>();
            factRelations = new ArrayList<SemRelation>();
          //  System.out.println("file.getName() = " + file.getName());
            kafSaxParser.parseFile(file.getAbsolutePath());
            GetSemFromNafFile.processNafFile(project, kafSaxParser, semEvents, semActors, semPlaces, semTimes, semRelations, factRelations);
            // We need to create output objects that are more informative than the Trig output and store these in files per date
            //System.out.println("semTimes = " + semTimes.size());
            for (int j = 0; j < semEvents.size(); j++) {
                SemEvent mySemEvent = (SemEvent) semEvents.get(j);
                ArrayList<SemTime> myTimes =  Util.castToTime(ComponentMatch.getMySemObjects(mySemEvent, semRelations, semTimes));
             //   System.out.println("myTimes.size() = " + myTimes.size());
                ArrayList<SemPlace> myPlaces = Util.castToPlace(ComponentMatch.getMySemObjects(mySemEvent, semRelations, semPlaces));
                ArrayList<SemActor> myActors = Util.castToActor(ComponentMatch.getMySemObjects(mySemEvent, semRelations, semActors));
                ArrayList<SemRelation> myRelations = ComponentMatch.getMySemRelations(mySemEvent, semRelations);

                ArrayList<SemRelation> myFacts = ComponentMatch.getMySemRelations(mySemEvent, factRelations);
                CompositeEvent compositeEvent = new CompositeEvent(mySemEvent, myActors, myPlaces, myTimes, myRelations, myFacts);
                File folder = otherFolder;
                for (int k = 0; k < mySemEvent.getConcepts().size(); k++) {
                    KafSense kafSense = mySemEvent.getConcepts().get(k);
                    if (kafSense.getSensecode().equalsIgnoreCase("speech-cognition")) {
                        folder = speechFolder;
                        break;
                    } else if (kafSense.getSensecode().equalsIgnoreCase("communication")) {
                        folder = speechFolder;
                        break;
                    } else if (kafSense.getSensecode().equalsIgnoreCase("cognition")) {
                        folder = speechFolder;
                        break;
                    } else if (kafSense.getSensecode().equalsIgnoreCase("grammatical")) {
                        folder = grammaticalFolder;
                        break;
                    }
                }
                File timeFile = null;

                ArrayList<SemTime> outputTimes = myTimes;
                // eventFos.writeObject(compositeEvent);
                /// now we need to write the event data and relations to the proper time folder for comparison
/*                if (outputTimes.size() == 0) {
                    /// we use the doc times as fall back;
                    outputTimes = compositeEvent.getMyDocTimes();
                }*/
                if (outputTimes.size() == 0) {
                    /// timeless
                        timeFile = new File(folder.getAbsolutePath() + "/" + "events-" + "timeless" + ".obj");
                }
                else if (outputTimes.size() == 1) {
                    /// time: same year or exact?
                    SemTime myTime = outputTimes.get(0);
                    String timePhrase = "-" + myTime.getOwlTime().toString();
                    timeFile = new File(folder.getAbsolutePath() + "/" + "events" + timePhrase + ".obj");
                }
                else {
                    /// special case if multiple times, what to do? create a period?
                    //// ?????
                    TreeSet<String> treeSet = new TreeSet<String>();
                    String timePhrase = "";
                    for (int k = 0; k < outputTimes.size(); k++) {
                        SemTime semTime = (SemTime) outputTimes.get(k);
                        timePhrase = semTime.getOwlTime().toString();
                        if (!treeSet.contains(timePhrase)) {
                            treeSet.add(timePhrase);
                        }
                    }
                    timePhrase = "";
                    Iterator keys = treeSet.iterator();
                    while (keys.hasNext()) {
                        timePhrase += "-" + keys.next();
                    }
                    timeFile = new File(folder.getAbsolutePath() + "/" + "events" + timePhrase + ".obj");
                }
                if (timeFile != null) {
                    if (timeFile.exists()) {
                      //    System.out.println("appending to timeFile.getName() = " + timeFile.getName());
                        OutputStream os = new FileOutputStream(timeFile, true);
                        Util.AppendableObjectOutputStream eventFos = new Util.AppendableObjectOutputStream(os);
                        try {
                            eventFos.writeObject(compositeEvent);
                        } catch (IOException e) {
                            //e.printStackTrace();
                        }
                        os.close();
                        eventFos.close();
                    } else {
                     //  System.out.println("timeFile.getName() = " + timeFile.getName());
                        OutputStream os = new FileOutputStream(timeFile);
                        ObjectOutputStream eventFos = new ObjectOutputStream(os);
                        try {
                            eventFos.writeObject(compositeEvent);
                        } catch (IOException e) {
                            // e.printStackTrace();
                        }
                        os.close();
                        eventFos.close();
                    }
                }
                else {
                 //   System.out.println("timeFile = " + timeFile);
                }
            }
        }

    }


}
