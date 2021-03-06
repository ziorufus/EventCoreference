package eu.newsreader.eventcoreference.input;

import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.tdb.TDBFactory;
import eu.newsreader.eventcoreference.objects.JsonEvent;
import eu.newsreader.eventcoreference.util.RoleLabels;
import eu.newsreader.eventcoreference.util.Util;
import org.apache.jena.riot.RDFDataMgr;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;

/**
 * Created by piek on 1/3/14.
 */
public class TrigToJsonTimeLine {


    static final String provenanceGraph = "http://www.newsreader-project.eu/provenance";
    static final String instanceGraph = "http://www.newsreader-project.eu/instances";
    static HashMap<String, ArrayList<Statement>> tripleMapProvenance = new HashMap<String, ArrayList<Statement>>();
    static HashMap<String, ArrayList<Statement>> tripleMapInstances = new HashMap<String, ArrayList<Statement>>();
    static HashMap<String, ArrayList<Statement>> tripleMapOthers = new HashMap<String, ArrayList<Statement>>();
    static HashMap<String, ArrayList<String>> iliMap = new HashMap<String, ArrayList<String>>();
    static String ACTORNAMESPACES = "";
    static boolean ALL = false; /// if true we do not filter events

    static public void main (String[] args) {
        String project = "NewsReader timeline";
        String pathToILIfile = "";
        String trigfolder = "";
        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            if (arg.equals("--trig-folder") && args.length>(i+1)) {
                trigfolder = args[i+1];
            }
            else if (arg.equals("--ili") && args.length>(i+1)) {
                pathToILIfile = args[i+1];
            }
            else if (arg.equals("--actors") && args.length>(i+1)) {
                ACTORNAMESPACES = args[i+1];
                System.out.println("ACTORNAMESPACES = " + ACTORNAMESPACES);
            }
        }


        // trigfolder = "/Users/piek/Desktop/NWR/Cross-lingual/corpus_NAF_output_141214-lemma/corpus_airbus/events/contextual";
        // pathToILIfile = "/Users/piek/Desktop/NWR/Cross-lingual/wn3-ili-synonyms.txt";
        iliMap = Util.ReadFileToStringHashMap(pathToILIfile);
        Dataset dataset = TDBFactory.createDataset();
        ArrayList<File> trigFiles = Util.makeRecursiveFileList(new File(trigfolder), ".trig");
        ArrayList<String> provenanceTriples = new ArrayList<String>();
        ArrayList<String> instanceTriples = new ArrayList<String>();
        ArrayList<String> otherTriples = new ArrayList<String>();
        for (int i = 0; i < trigFiles.size(); i++) {
            File file = trigFiles.get(i);
            //System.out.println("file.getAbsolutePath() = " + file.getAbsolutePath());
            dataset = RDFDataMgr.loadDataset(file.getAbsolutePath());
            Iterator<String> it = dataset.listNames();
            while (it.hasNext()) {
                String name = it.next();
                // System.out.println("name = " + name);
                if (name.equals(provenanceGraph)) {
                    Model namedModel = dataset.getNamedModel(name);
                    StmtIterator siter = namedModel.listStatements();
                    while (siter.hasNext()) {
                        Statement s = siter.nextStatement();
                        String subject = s.getSubject().getURI();
                        if (tripleMapProvenance.containsKey(subject)) {
                            ArrayList<Statement> triples = tripleMapProvenance.get(subject);
                            triples.add(s);
                            tripleMapProvenance.put(subject, triples);
                        }
                        else {

                            ArrayList<Statement> triples = new ArrayList<Statement>();
                            triples.add(s);
                            tripleMapProvenance.put(subject, triples);
                        }
                    }
                }
                else if (name.equals(instanceGraph)) {
                    Model namedModel = dataset.getNamedModel(name);
                    StmtIterator siter = namedModel.listStatements();
                    while (siter.hasNext()) {
                        Statement s = siter.nextStatement();
                        String subject = s.getSubject().getURI();
                        if (tripleMapInstances.containsKey(subject)) {
                            ArrayList<Statement> triples = tripleMapInstances.get(subject);
                            triples.add(s);
                            tripleMapInstances.put(subject, triples);
                        }
                        else {

                            ArrayList<Statement> triples = new ArrayList<Statement>();
                            triples.add(s);
                            tripleMapInstances.put(subject, triples);
                        }
                    }
                }
                else {
                    Model namedModel = dataset.getNamedModel(name);
                    StmtIterator siter = namedModel.listStatements();
                    while (siter.hasNext()) {
                        Statement s = siter.nextStatement();
                        String subject = s.getSubject().getURI();
                        if (tripleMapOthers.containsKey(subject)) {
                            ArrayList<Statement> triples = tripleMapOthers.get(subject);
                            triples.add(s);
                            tripleMapOthers.put(subject, triples);
                        }
                        else {

                            ArrayList<Statement> triples = new ArrayList<Statement>();
                            triples.add(s);
                            tripleMapOthers.put(subject, triples);
                        }
                    }
                }
            }
            dataset.close();
        }
        try {
            ArrayList<JSONObject> jsonObjects = getJSONObjectArray();
            writeJsonObjectArray(trigfolder, project, jsonObjects);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    static String getInstanceType (String subject) {
        String type = "";
        if (subject.indexOf("dbpedia.org")>-1) {
            type = "DBP";
        }
        else if (subject.indexOf("/entities/")>-1) {
            type = "ENT";
        }
        else if (subject.indexOf("ili-30")>-1) {
            type = "IEV";
        }
        else if (subject.indexOf("#ev")>-1) {
            type = "LEV";
        }
        else if (subject.indexOf("http://www.newsreader-project.eu/time/")>-1) {
            type = "DATE";
        }

        return type;
    }


    static boolean hasActor (ArrayList<Statement> triples) {
        for (int i = 0; i < triples.size(); i++) {
            Statement s = triples.get(i);
            String predicate = s.getPredicate().getURI();
            if (predicate.toLowerCase().endsWith("hasactor")) {
                return true;
            }
            else {
              //  System.out.println("predicate = " + predicate);
            }
        }
        return false;
    }

    static boolean hasILI (ArrayList<Statement> triples) {
        for (int i = 0; i < triples.size(); i++) {
            Statement statement = triples.get(i);
            String object = "";
            if (statement.getObject().isLiteral()) {
                object = statement.getObject().asLiteral().toString();
            } else if (statement.getObject().isURIResource()) {
                object = statement.getObject().asResource().getURI();
            }
            if (object.indexOf("ili-")>-1) {
                return true;
            }
        }
        return false;
    }

    static boolean hasFrameNet (ArrayList<Statement> triples) {
        for (int i = 0; i < triples.size(); i++) {
            Statement statement = triples.get(i);
            String object = "";
            if (statement.getObject().isLiteral()) {
                object = statement.getObject().asLiteral().toString();
            } else if (statement.getObject().isURIResource()) {
                object = statement.getObject().asResource().getURI();
            }
            if (object.indexOf("framenet")>-1) {
                return true;
            }
        }
        return false;
    }

    static String getTimeAnchor (ArrayList<Statement> triples) {
        for (int i = 0; i < triples.size(); i++) {
            Statement statement = triples.get(i);
            String predicate = statement.getPredicate().getURI();
            if (predicate.toLowerCase().endsWith("hastime")) {
                String object = "";
                if (statement.getObject().isLiteral()) {
                    object = statement.getObject().asLiteral().toString();
                } else if (statement.getObject().isURIResource()) {
                    object = statement.getObject().asResource().getURI();
                }
                if (tripleMapInstances.containsKey( object)) {
                    ArrayList<Statement> instanceTriples = tripleMapInstances.get(object);
                    for (int j = 0; j < instanceTriples.size(); j++) {
                        Statement timeStatement = instanceTriples.get(j);
                        if (timeStatement.getPredicate().getURI().toLowerCase().endsWith("indatetime")) {
                            if (timeStatement.getObject().isLiteral()) {
                                return timeStatement.getObject().asLiteral().toString();
                            } else if (statement.getObject().isURIResource()) {
                                return timeStatement.getObject().asResource().getURI();
                            }
                        }
                    }
                }

            }
        }
        return "NOTIMEANCHOR";
    }

    //        TimeLanguage.setLanguage(kafSaxParser.getLanguage());

    static public String getSynsetsFromIli (String key) {
        String synset = "";
        int idx = key.lastIndexOf("/");
        if (idx>-1) key = key.substring(idx+1);
        synset = key;
        ArrayList<String> synsetArray = new ArrayList<String>();
        String [] ilis = key.split("-and-");
        for (int i = 0; i < ilis.length; i++) {
            String ili = ilis[i];
            if (iliMap.containsKey(ili)) {
                ArrayList<String> syns = iliMap.get(ili);
                for (int j = 0; j < syns.size(); j++) {
                    String s = syns.get(j);
                    s = s.substring(0, s.indexOf("%"));
                    if (!synsetArray.contains(s)) {
                        synsetArray.add(s);
                    }
                }
            }
        }
        if (synsetArray.size()>0) {
            synset = "";
            for (int i = 0; i < synsetArray.size(); i++) {
                String s = synsetArray.get(i);
                if (!synset.isEmpty()) {
                    synset+= ";";
                }
                synset += s;
            }
        }
        return synset;
    }

    static ArrayList<JSONObject> getJSONObjectArray() throws JSONException {
        ArrayList<JSONObject> jsonObjectArrayList = new ArrayList<JSONObject>();

        Set keySet = tripleMapInstances.keySet();
        Iterator<String> keys = keySet.iterator();
        while (keys.hasNext()) {
            String key = keys.next(); //// this is the subject of the triple which should point to an event
            ArrayList<Statement> instanceTriples = tripleMapInstances.get(key);
            if (hasILI(instanceTriples) || hasFrameNet(instanceTriples) || ALL) {
                if (tripleMapOthers.containsKey( key)) {
                    ArrayList<Statement> otherTriples = tripleMapOthers.get(key);
                    if (hasActor(otherTriples) || ALL) {
                        /// we ignore events without actors.....
                        JSONObject jsonObject = new JSONObject();
                        jsonObject.put("event", getSynsetsFromIli(key));
                        String timeAnchor = getTimeAnchor(otherTriples);
                        int idx = timeAnchor.lastIndexOf("/");
                        if (idx>-1) {
                            timeAnchor = timeAnchor.substring(idx+1);
                        }
                        jsonObject.put("time", timeAnchor);

                        JSONObject jsonClasses = getClassesJSONObjectFromInstanceStatement(instanceTriples);
                        if (jsonClasses.keys().hasNext()) {
                            jsonObject.put("classes", jsonClasses);
                        }
                        JSONObject jsonLabels = getLabelsJSONObjectFromInstanceStatement(instanceTriples);
                        if (jsonLabels.keys().hasNext()) {
                            jsonObject.put("labels", jsonLabels.get("labels"));
                        }
                        JSONObject jsonMentions = getMentionsJSONObjectFromInstanceStatement(instanceTriples);
                        if (jsonMentions.keys().hasNext()) {
                            jsonObject.put("mentions", jsonMentions.get("mentions"));
                        }
                        JSONObject actors = getActorsJSONObjectFromInstanceStatement(otherTriples);
                        if (actors.keys().hasNext()) {
                            jsonObject.put("actors",actors);
                        }
                        jsonObjectArrayList.add(jsonObject);
                    }
                }
            }
        }
        return jsonObjectArrayList;
    }



    static ArrayList<JSONObject> getJSONObjectArrayRDF() throws JSONException {
        ArrayList<JSONObject> jsonObjectArrayList = new ArrayList<JSONObject>();

        Set keySet = tripleMapOthers.keySet();
        Iterator<String> keys = keySet.iterator();
        while (keys.hasNext()) {
            String key = keys.next(); //// this is the subject of the triple which should point to an event
            ArrayList<Statement> otherTriples = tripleMapOthers.get(key);
            if (!hasActor(otherTriples)) {
               /// we ignore events without actors.....
            }
            else {

                JSONObject jsonObject = new JSONObject();

                jsonObject.put("event", key);
                String timeAnchor = getTimeAnchor(otherTriples);
                // String timeString = semTime.getOwlTime().toString().replaceAll("-", ",");

                jsonObject.put("time", timeAnchor);
                if (tripleMapInstances.containsKey( key)) {
                    ArrayList<Statement> instanceTriples = tripleMapInstances.get(key);
                    for (int i = 0; i < instanceTriples.size(); i++) {
                        Statement statement = instanceTriples.get(i);
                        String predicate = statement.getPredicate().getURI();
                      //  if (predicate.)
                        String object = "";
                        if (statement.getObject().isLiteral()) {
                            object = statement.getObject().asLiteral().toString();
                        } else if (statement.getObject().isURIResource()) {
                            object = statement.getObject().asResource().getURI();
                        }
                        jsonObject.put(predicate, object);
                    }
                }
                for (int i = 0; i < otherTriples.size(); i++) {
                    Statement statement = otherTriples.get(i);
                    String predicate = statement.getPredicate().getURI();
                    if (!predicate.toLowerCase().endsWith("hastime")) {
                        String object = "";
                        if (statement.getObject().isLiteral()) {
                            object = statement.getObject().asLiteral().toString();
                        } else if (statement.getObject().isURIResource()) {
                            object = statement.getObject().asResource().getURI();
                        }
                        jsonObject.put(predicate, object);
                    }
                }
                jsonObjectArrayList.add(jsonObject);
            }
        }
        return jsonObjectArrayList;
    }


    static void writeJsonObjectArray (String pathToFolder, String project, ArrayList<JSONObject> objects) {
        try {
            try {
                File folder = new File(pathToFolder);
                OutputStream jsonOut = new FileOutputStream(folder.getParentFile() + "/" + folder.getName()+".timeline.json");
                JSONObject timeLineObject = JsonEvent.createTimeLineProperty(new File(pathToFolder).getName(), project);

                for (int j = 0; j < objects.size(); j++) {
                    JSONObject jsonObject = objects.get(j);
                    timeLineObject.append("events", jsonObject);
                }
                String str = "{ \"timeline\":\n";
                jsonOut.write(str.getBytes());
                StringWriter out = new StringWriter();
                timeLineObject.write(out);
                jsonOut.write(out.toString().getBytes());
                str ="}\n";
                jsonOut.write(str.getBytes());
                jsonOut.close();
            } catch (JSONException e) {
                e.printStackTrace();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    static String getNameSpaceString (String value) {
        String property = "";
        if (value.indexOf("/framenet/") > -1) {
            property = "fn";
        }
        else if (value.indexOf("/propbank/") > -1) {
            property = "pb";
        }
        else if (value.indexOf("/sem/") > -1) {
            property = "sem";
        }
        else if (value.indexOf("/sumo/") > -1) {
            property = "sumo";
        }
        else if (value.indexOf("/eso/") > -1) {
            property = "eso";
        }
        else if (value.indexOf("/domain-ontology") > -1) {
            property = "eso";
        }
        else if (value.indexOf("ili-30") > -1) {
            property = "wn";
        }
        return property;
    }

    static String getValue (String predicate) {
        int idx = predicate.lastIndexOf("#");
        if (idx>-1) {
            return predicate.substring(idx + 1);
        }
        else {
            idx = predicate.lastIndexOf("/");
            if (idx>-1) {
                return predicate.substring(idx + 1);
            }
            else {
                return predicate;
            }
        }
    }

    static JSONObject getClassesJSONObjectFromInstanceStatement (ArrayList<Statement> statements) throws JSONException {
        JSONObject jsonClassesObject = new JSONObject();
        ArrayList<String> coveredValues = new ArrayList<String>();

        for (int i = 0; i < statements.size(); i++) {
            Statement statement = statements.get(i);

            String predicate = statement.getPredicate().getURI();
            if (predicate.endsWith("#type")) {
                String object = "";
                if (statement.getObject().isLiteral()) {
                    object = statement.getObject().asLiteral().toString();
                } else if (statement.getObject().isURIResource()) {
                    object = statement.getObject().asResource().getURI();
                }
                String [] values = object.split(",");
                for (int j = 0; j < values.length; j++) {
                    String value = values[j];
                    String property = getNameSpaceString(value);
                    if (!property.isEmpty() && !property.equalsIgnoreCase("sem")) {
                        value = getValue(value);
                        if (!coveredValues.contains(property+value)) {
                            coveredValues.add(property+value);
                            jsonClassesObject.append(property, value);
                        }
                    }
                }
            }
        }
        return jsonClassesObject;
    }

    static JSONObject getActorsJSONObjectFromInstanceStatement (ArrayList<Statement> statements) throws JSONException {
        JSONObject jsonActorsObject = new JSONObject();

        HashMap<String, ArrayList<Statement>> actorMap = new HashMap<String, ArrayList<Statement>>();
        for (int i = 0; i < statements.size(); i++) {
            Statement statement = statements.get(i);

            String predicate = statement.getPredicate().getURI();
            if (predicate.toLowerCase().endsWith("hastime")) {
                ///
            }
            else if (predicate.toLowerCase().endsWith("hasactor")) {
                ///
            }
            else {
                String object = "";
                if (statement.getObject().isLiteral()) {
                    object = statement.getObject().asLiteral().toString();
                } else if (statement.getObject().isURIResource()) {
                    object = statement.getObject().asResource().getURI();
                }
                String property = getNameSpaceString(predicate);
                if (!property.isEmpty()) {
                    if (ACTORNAMESPACES.indexOf(property)>-1 || ACTORNAMESPACES.isEmpty()) {
                        if (property.equalsIgnoreCase("pb")) {
                            predicate = property + "/" + RoleLabels.normalizeProbBankValue(getValue(predicate));
                        }
                        else {
                            predicate = property + "/" + getValue(predicate);
                        }
                        String[] values = object.split(",");
                        ArrayList<String> coveredValues = new ArrayList<String>();
                        for (int j = 0; j < values.length; j++) {
                            String value = values[j];
                            if (!coveredValues.contains(value)) {
                                coveredValues.add(value);
                                jsonActorsObject.append(predicate, value);
                            }
                        }
                    }
                }
            }
        }
        return jsonActorsObject;
    }

    static JSONObject getUniqueActorsJSONObjectFromInstanceStatement (ArrayList<Statement> statements) throws JSONException {
        JSONObject jsonActorsObject = new JSONObject();

        HashMap<String, ArrayList<Statement>> actorMap = new HashMap<String, ArrayList<Statement>>();
        for (int i = 0; i < statements.size(); i++) {
            Statement statement = statements.get(i);

            String predicate = statement.getPredicate().getURI();
            if (predicate.toLowerCase().endsWith("hastime")) {
                ///
            }
            else if (predicate.toLowerCase().endsWith("hasactor")) {
                ///
            }
            else {
                String object = "";
                if (statement.getObject().isLiteral()) {
                    object = statement.getObject().asLiteral().toString();
                } else if (statement.getObject().isURIResource()) {
                    object = statement.getObject().asResource().getURI();
                }
                if (actorMap.containsKey(object)) {
                    ArrayList<Statement> actorStatements = actorMap.get(object);
                    actorStatements.add(statement);
                    actorMap.put(object, actorStatements);
                }
                else {
                    ArrayList<Statement> actorStatements = new ArrayList<Statement>();
                    actorStatements.add(statement);
                    actorMap.put(object, actorStatements);
                }
            }
        }
        Set keySet = actorMap.keySet();
        Iterator<String> keys = keySet.iterator();
        while (keys.hasNext()) {
            String key = keys.next();
            ArrayList<Statement> actorStatements = actorMap.get(key);
            String combinedPredicate = "";
            for (int i = 0; i < actorStatements.size(); i++) {
                Statement statement = actorStatements.get(i);
                String predicate = statement.getPredicate().getURI();
                String property = getNameSpaceString(predicate);
                if (!property.isEmpty()) {
                    if (ACTORNAMESPACES.indexOf(property)>-1 || ACTORNAMESPACES.isEmpty()) {
                        if (property.equalsIgnoreCase("pb")) {
                            predicate = property + "/" + RoleLabels.normalizeProbBankValue(getValue(predicate));
                        }
                        else {
                            predicate = property + "/" + getValue(predicate);
                        }
                        if (!combinedPredicate.isEmpty()) {
                            combinedPredicate+="+";
                        }
                        combinedPredicate += predicate;
                        /*String[] values = key.split(",");
                        ArrayList<String> coveredValues = new ArrayList<String>();
                        for (int j = 0; j < values.length; j++) {
                            String value = values[j];
                            if (!coveredValues.contains(value)) {
                                coveredValues.add(value);
                                jsonActorsObject.append(predicate, value);
                            }
                        }*/
                    }
                }
              //  break; //// only takes the first
            }
            if (!combinedPredicate.isEmpty()) {
                String[] values = key.split(",");
                ArrayList<String> coveredValues = new ArrayList<String>();
                for (int j = 0; j < values.length; j++) {
                    String value = values[j];
                    if (!coveredValues.contains(value)) {
                        coveredValues.add(value);
                        jsonActorsObject.append(combinedPredicate, value);
                    }
                }
            }
        }
        return jsonActorsObject;
    }

    static JSONObject getMentionsJSONObjectFromInstanceStatement (ArrayList<Statement> statements) throws JSONException {
        JSONObject jsonClassesObject = new JSONObject();
        ArrayList<String> coveredValues = new ArrayList<String>();

        for (int i = 0; i < statements.size(); i++) {
            Statement statement = statements.get(i);

            String predicate = statement.getPredicate().getURI();
            if (predicate.endsWith("#denotedBy")) {
                String object = "";
                if (statement.getObject().isLiteral()) {
                    object = statement.getObject().asLiteral().toString();
                } else if (statement.getObject().isURIResource()) {
                    object = statement.getObject().asResource().getURI();
                }
                //"http://www.w3.org/1999/02/22-rdf-syntax-ns#type":"http://www.newsreader-project.eu/ontologies/framenet/Manufacturing"
                String [] values = object.split(",");
                for (int j = 0; j < values.length; j++) {
                    String value = values[j];
                    if (!coveredValues.contains(value)) {
                        coveredValues.add(value);
                        jsonClassesObject.append("mentions", value);
                    }
                }
            }
        }
        return jsonClassesObject;
    }

    static JSONObject getLabelsJSONObjectFromInstanceStatement (ArrayList<Statement> statements) throws JSONException {
        JSONObject jsonClassesObject = new JSONObject();
        ArrayList<String> coveredValues = new ArrayList<String>();
        for (int i = 0; i < statements.size(); i++) {
            Statement statement = statements.get(i);

            String predicate = statement.getPredicate().getURI();
            if (predicate.endsWith("#label")) {
                String object = "";
                if (statement.getObject().isLiteral()) {
                    object = statement.getObject().asLiteral().toString();
                } else if (statement.getObject().isURIResource()) {
                    object = statement.getObject().asResource().getURI();
                }
                String [] values = object.split(",");
                for (int j = 0; j < values.length; j++) {
                    String value = values[j];
                    if (!coveredValues.contains(value)) {
                        coveredValues.add(value);
                        jsonClassesObject.append("labels", value);
                    }
                }
            }
        }
        return jsonClassesObject;
    }


}
