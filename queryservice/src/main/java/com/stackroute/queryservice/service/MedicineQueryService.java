package com.stackroute.queryservice.service;

import com.stackroute.queryservice.domain.Output;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.util.CoreMap;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MedicineQueryService {
    ArrayList<String> conceptList = new ArrayList<>();
    ArrayList<String> diseaseList = new ArrayList<>();
    ArrayList<String> medicationList = new ArrayList<>();
    ArrayList<String> symptomList = new ArrayList<>();
    ArrayList<String> riskList = new ArrayList<>();
    ArrayList<String> frequencyList = new ArrayList<>();
    ArrayList<String> deathList = new ArrayList<>();
    public MedicineQueryService() throws IOException, ParseException {
        JSONObject obj = fetchRedis();
        System.out.println("constructor");
        JSONObject concepts = (JSONObject) obj.get("labels");
        JSONArray concept = (JSONArray) concepts.get("value");
        for(int i=0;i<concept.size();i++)
        {
            conceptList.add((String) concept.get(i));
        }
        JSONObject diseases = (JSONObject) obj.get("disease");
        JSONArray disease = (JSONArray) diseases.get("value");
        for(int i=0;i<disease.size();i++)
        {
            diseaseList.add((String) disease.get(i));
        }
        JSONObject treatments = (JSONObject) obj.get("treatment");
        JSONArray treatment = (JSONArray) treatments.get("value");
        for(int i=0;i<treatment.size();i++)
        {
            medicationList.add((String)treatment.get(i));
        }
        JSONObject symptoms = (JSONObject) obj.get("symptoms");
        JSONArray symptom = (JSONArray) symptoms.get("value");
        for(int i=0;i<symptom.size();i++)
        {
            symptomList.add((String) symptom.get(i));
        }
        JSONObject riskFactors = (JSONObject) obj.get("risk factors");
        JSONArray riskFactor = (JSONArray) riskFactors.get("value");
        for(int i=0;i<riskFactor.size();i++)
        {
            riskList.add((String) riskFactor.get(i));
        }
        JSONObject frequencies = (JSONObject) obj.get("frequency");
        JSONArray frequency = (JSONArray) frequencies.get("value");
        for(int i=0;i<frequency.size();i++)
        {
            frequencyList.add((String) frequency.get(i));
        }
        JSONObject deaths = (JSONObject) obj.get("[deaths]");
        JSONArray death = (JSONArray) deaths.get("value");
        for(int i=0;i<death.size();i++)
        {
            deathList.add((String) death.get(i));
        }
    }
    private String[] stopWords = new String[]{"what","and","like","taken","also","for","it", "with","who","which","required","used","do","is","?", "by","take","are", "give", "of", "in", "times","me","How","many", "the","if","a","has","getting"};
    private static Properties properties;
    private static String propertiesName = "tokenize, ssplit, pos, lemma";
    private static StanfordCoreNLP stanfordCoreNLP;
    static {
        properties = new Properties();
        properties.setProperty("annotators", propertiesName);
    }
    public static StanfordCoreNLP getPipeline() {
        if (stanfordCoreNLP == null) {
            stanfordCoreNLP = new StanfordCoreNLP(properties);
        }
        return stanfordCoreNLP;
    }

    public JSONObject fetchRedis() throws IOException, ParseException {
//        String link = "http://13.127.108.14:8134/api";
//        String link = "http://104.154.175.62:8134/api";
        String link = "http://redis-spring:8134/api";
        System.out.println("link");
        URL url = new URL(link);
        HttpURLConnection connection = (HttpURLConnection)url.openConnection();
        connection.setRequestMethod("GET");
        connection.connect();
        System.out.println("connection");
        int resp = connection.getResponseCode();
        if(resp!=200)
        {
            throw new RuntimeException("HTTP Response Code :" + resp);
        }
        else {
            //Reading the obtained values from API Call
            Scanner sc = new Scanner(url.openStream());
            String text = "";
            while (sc.hasNext()) {
                text += sc.nextLine();
            }
            sc.close();
            JSONParser parser = new JSONParser();
            JSONObject jobj = (JSONObject)parser.parse(text);
            System.out.println("parse");
            return jobj;
        }
    }

    public String getTrimmedQuery(String query) {
        String trimmedQuery = query.trim();
        trimmedQuery = trimmedQuery.replaceAll("\\s+", " ");
        trimmedQuery = trimmedQuery.replaceAll("\\t", " ");
        trimmedQuery = trimmedQuery.replaceAll("[?.]","");
        return trimmedQuery;
    }
    public ArrayList<String> getListWithoutStopWords(String query) {
        String trimmedQuery = getTrimmedQuery(query);
        String[] wordsSplitArray = trimmedQuery.split(" ");
        ArrayList<String> wordsSplitList = new ArrayList<String>();
        for (int i = 0; i < wordsSplitArray.length; i++) wordsSplitList.add(wordsSplitArray[i]);
        for (int i = 0; i < stopWords.length; i++) {
            for (int j = 0; j < wordsSplitList.size(); j++) {
                if (wordsSplitList.get(j).equalsIgnoreCase(stopWords[i].trim())) {
                    wordsSplitList.remove(wordsSplitList.get(j));
                }
            }
        }
        return wordsSplitList;
    }
    public List<String> getLemmatizedList(String query) {

        List<String> lemmatizedWordsList = new ArrayList<String>();
        ArrayList<String> listWithoutStopWords = getListWithoutStopWords(query);
        String stringWithoutStopWords = "";
        for (int i = 0; i < listWithoutStopWords.size(); i++) {
            stringWithoutStopWords = stringWithoutStopWords + listWithoutStopWords.get(i) + " ";
        }
        Annotation document = new Annotation(stringWithoutStopWords);
        StanfordCoreNLP stanfordCoreNLP = getPipeline();
        // run all Annotators on this text
        stanfordCoreNLP.annotate(document);
        // Iterate over all of the sentences found
        List<CoreMap> sentences = document.get(CoreAnnotations.SentencesAnnotation.class);
        for (CoreMap sentence : sentences) {
            // Iterate over all tokens in a sentence
            for (CoreLabel token : sentence.get(CoreAnnotations.TokensAnnotation.class)) {
                // Retrieve and add the lemma for each word into the
                // list of lemmas
                lemmatizedWordsList.add(token.get(CoreAnnotations.LemmaAnnotation.class));
            }
        }
        return lemmatizedWordsList;
    }
    public Output RedisMatcher(String lemmatizedString)
    {
        Output output = new Output();
        output.setDomain("medical");
        List<Map> constraints = new ArrayList<>();

        for(int i=0; i < symptomList.size(); i++ ){
            Map<String, String> map = new HashMap<>();
            Pattern pattern = Pattern.compile(symptomList.get(i).toLowerCase());
            Matcher matcher = pattern.matcher(lemmatizedString);
            if(matcher.find()){
                lemmatizedString = lemmatizedString.replaceAll("symptom", "");
                map.put("key","Symptoms");
                map.put("value",symptomList.get(i));
                constraints.add(map);
                lemmatizedString = lemmatizedString.replaceAll(symptomList.get(i),"");
            }
        }

        int flag=0;
        String disease="";
        for(int i=0; i < diseaseList.size(); i++ ){
            Pattern pattern = Pattern.compile(diseaseList.get(i).toLowerCase());
            Matcher matcher = pattern.matcher(lemmatizedString);
            if(matcher.find())
            {
                if(flag==0)
                {
                    lemmatizedString = lemmatizedString.replaceAll("disease", "");
                    disease = diseaseList.get(i);
                    lemmatizedString = lemmatizedString.replaceAll(diseaseList.get(i),"");
                    flag =1;
                }
                else {
                    if(disease.length()<diseaseList.get(i).length())
                    {
                        disease = diseaseList.get(i);
                        lemmatizedString = lemmatizedString.replaceAll(diseaseList.get(i),"");
                    }
                }
            }
        }
        if(flag==1)
        {
            Map<String, String> map = new HashMap<>();
            map.put("key","Disease");
            map.put("value",disease);
            constraints.add(map);
        }

        for(int i=0; i<riskList.size();i++)
        {
            Map<String, String> map = new HashMap<>();
            Pattern pattern = Pattern.compile(riskList.get(i).toLowerCase());
            Matcher matcher = pattern.matcher(lemmatizedString);
            if(matcher.find())
            {
                lemmatizedString = lemmatizedString.replaceAll("risk", "");
                map.put("key","Risk Factors");
                map.put("value",riskList.get(i));
                constraints.add(map);
                lemmatizedString = lemmatizedString.replaceAll(riskList.get(i),"");
            }
        }

        for(int i=0; i<medicationList.size();i++)
        {
            Map<String, String> map = new HashMap<>();
            Pattern pattern = Pattern.compile(medicationList.get(i).toLowerCase());
            Matcher matcher = pattern.matcher(lemmatizedString);
            if(matcher.find())
            {
                lemmatizedString = lemmatizedString.replaceAll("medication", "");
                lemmatizedString = lemmatizedString.replaceAll("treatment", "");
                map.put("key","Treatment");
                map.put("value",medicationList.get(i));
                constraints.add(map);
                lemmatizedString = lemmatizedString.replaceAll(medicationList.get(i),"");
            }
        }

        for(int i=0; i<frequencyList.size();i++)
        {
            Map<String, String> map = new HashMap<>();
            Pattern pattern = Pattern.compile(frequencyList.get(i).toLowerCase());
            Matcher matcher = pattern.matcher(lemmatizedString);
            if(matcher.find())
            {
                lemmatizedString = lemmatizedString.replaceAll("frequency", "");
                map.put("key","Frequency");
                map.put("value",frequencyList.get(i));
                constraints.add(map);
                lemmatizedString = lemmatizedString.replaceAll(frequencyList.get(i),"");
            }
        }

        for(int i=0; i<deathList.size();i++)
        {
            Map<String, String> map = new HashMap<>();
            Pattern pattern = Pattern.compile(deathList.get(i).toLowerCase());
            Matcher matcher = pattern.matcher(lemmatizedString);
            if(matcher.find())
            {
                lemmatizedString = lemmatizedString.replaceAll("death", "");
                map.put("key","[Deaths]");
                map.put("value",deathList.get(i));
                constraints.add(map);
                lemmatizedString = lemmatizedString.replaceAll(deathList.get(i),"");
            }
        }
        output.setConstraint(constraints);

        List<String> concepts = new ArrayList<>();
        for (int i=0; i<conceptList.size();i++)
        {
            Pattern pattern = Pattern.compile(conceptList.get(i).toLowerCase());
            Matcher matcher = pattern.matcher(lemmatizedString);
            if(matcher.find())
            {
                concepts.add(conceptList.get(i));
            }
        }
        String[] conceptsArray = new String[concepts.size()];
        int i=0;
        for(String a:concepts)
        {
            conceptsArray[i] = a;
            i++;
        }
        output.setQueryResult(conceptsArray);

        return output;
    }
}