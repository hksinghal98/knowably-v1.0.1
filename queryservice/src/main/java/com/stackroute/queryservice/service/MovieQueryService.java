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

public class MovieQueryService {
    ArrayList<String> conceptList = new ArrayList<>();
    ArrayList<String> movieList = new ArrayList<>();
    ArrayList<String> boxOfficeList = new ArrayList<>();
    ArrayList<String> productionHouseList = new ArrayList<>();
    ArrayList<String> actorList = new ArrayList<>();
    ArrayList<String> directorList = new ArrayList<>();
    ArrayList<String> releaseYearList = new ArrayList<>();
    ArrayList<String> countryList = new ArrayList<>();
    ArrayList<String> languageList = new ArrayList<>();
    public MovieQueryService() throws IOException, ParseException {
        JSONObject obj = fetchRedis();
        JSONObject concepts = (JSONObject) obj.get("labels");
        JSONArray concept = (JSONArray) concepts.get("value");
        for(int i=0;i<concept.size();i++)
        {
            conceptList.add((String) concept.get(i));
        }
        JSONObject movies = (JSONObject) obj.get("movie");
        JSONArray movie = (JSONArray) movies.get("value");
        for(int i=0;i<movie.size();i++)
        {
            movieList.add((String) movie.get(i));
        }
        JSONObject collections = (JSONObject) obj.get("boxoffice");
        JSONArray collection = (JSONArray) collections.get("value");
        for(int i=0;i<collection.size();i++)
        {
            boxOfficeList.add((String) collection.get(i));
        }
        JSONObject productions = (JSONObject) obj.get("produced by");
        JSONArray production = (JSONArray) productions.get("value");
        for(int i=0;i<production.size();i++)
        {
            productionHouseList.add((String) production.get(i));
        }
        JSONObject actors = (JSONObject) obj.get("actor");
        JSONArray actor = (JSONArray) actors.get("value");
        for(int i=0;i<actor.size();i++)
        {
            actorList.add((String) actor.get(i));
        }
        JSONObject directors = (JSONObject) obj.get("directed by");
        JSONArray director = (JSONArray) directors.get("value");
        for(int i=0;i<director.size();i++)
        {
            directorList.add((String) director.get(i));
        }
        JSONObject releases = (JSONObject) obj.get("release date");
        JSONArray release = (JSONArray) releases.get("value");
        for(int i=0;i<release.size();i++)
        {
            releaseYearList.add((String) release.get(i));
        }
        JSONObject countries = (JSONObject) obj.get("country");
        JSONArray country = (JSONArray) countries.get("value");
        for(int i=0;i<country.size();i++)
        {
            countryList.add((String) country.get(i));
        }
        JSONObject languages = (JSONObject) obj.get("language");
        JSONArray language = (JSONArray) languages.get("value");
        for(int i=0;i<language.size();i++)
        {
            languageList.add((String) language.get(i));
        }
    }
    private String[] stopWords = new String[]{"what","and","like","taken","also","for","it", "with","who","which","required","used","do","is","?", "by","take","are", "give", "times","me","How","many", "the","if","a","has","getting","that","do","name","after","before","between"};
    private String[] releaseWords = new String[]{"in","of"};
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

    public JSONObject fetchRedis() throws IOException, org.json.simple.parser.ParseException {
//        String link = "http://13.127.108.14:8134/api";
//        String link = "http://104.154.175.62:8134/api";
        String link = "http://redis-spring:8134/api";
        URL url = new URL(link);
        HttpURLConnection connection = (HttpURLConnection)url.openConnection();
        connection.setRequestMethod("GET");
        connection.connect();
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
            return jobj;
        }
    }
    public String getTrimmedQuery(String query) {
        String trimmedQuery = query.trim();
        trimmedQuery = trimmedQuery.replaceAll("\\s+", " ");
        trimmedQuery = trimmedQuery.replaceAll("\\t", " ");
        trimmedQuery = trimmedQuery.replaceAll("[?.!,]","");
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
        output.setDomain("movie");
        List<Map> constraints = new ArrayList<>();

        int flag=0;
        String movie="";
        for(int i=0; i < movieList.size(); i++ ){
            Pattern pattern = Pattern.compile(movieList.get(i).toLowerCase());
            Matcher matcher = pattern.matcher(lemmatizedString);
            if(matcher.find())
            {
                if(flag==0)
                {
                    lemmatizedString = lemmatizedString.replaceAll("movie", "");
                    movie = movieList.get(i);
                    lemmatizedString = lemmatizedString.replaceAll(movieList.get(i),"");
                    flag =1;
                }
                else {
                    if(movie.length()<movieList.get(i).length())
                    {
                        movie = movieList.get(i);
                        lemmatizedString = lemmatizedString.replaceAll(movieList.get(i),"");
                    }
                }
            }
        }
        if(flag==1)
        {
            Map<String, String> map = new HashMap<>();
            map.put("key","Movie");
            map.put("value",movie);
            constraints.add(map);
        }


        for(int i=0; i < productionHouseList.size(); i++ ){
            Map<String, String> map = new HashMap<>();
            Pattern pattern = Pattern.compile(productionHouseList.get(i).toLowerCase());
            Matcher matcher = pattern.matcher(lemmatizedString);
            if(matcher.find()){
                lemmatizedString = lemmatizedString.replaceAll("production house", "");
                map.put("key","Produced by");
                map.put("value", productionHouseList.get(i));
                constraints.add(map);
                lemmatizedString = lemmatizedString.replaceAll(productionHouseList.get(i),"");
            }
        }

        for(int i=0; i<actorList.size();i++)
        {
            Map<String, String> map = new HashMap<>();
            Pattern pattern = Pattern.compile(actorList.get(i).toLowerCase());
            Matcher matcher = pattern.matcher(lemmatizedString);
            if(matcher.find())
            {
                lemmatizedString = lemmatizedString.replaceAll("act in", "");
                lemmatizedString = lemmatizedString.replaceAll("act", "");
                map.put("key","Actor");
                map.put("value",actorList.get(i));
                constraints.add(map);
                lemmatizedString = lemmatizedString.replaceAll(actorList.get(i),"");
            }
        }

        for(int i=0; i<directorList.size();i++)
        {
            Map<String, String> map = new HashMap<>();
            Pattern pattern = Pattern.compile(directorList.get(i).toLowerCase());
            Matcher matcher = pattern.matcher(lemmatizedString);
            if(matcher.find())
            {
                lemmatizedString = lemmatizedString.replaceAll("direct", "");
                map.put("key","Directed by");
                map.put("value",directorList.get(i));
                constraints.add(map);
                lemmatizedString = lemmatizedString.replaceAll(directorList.get(i),"");
            }
        }

        for(int i=0; i<countryList.size();i++)
        {
            Map<String, String> map = new HashMap<>();
            Pattern pattern = Pattern.compile(countryList.get(i).toLowerCase());
            Matcher matcher = pattern.matcher(lemmatizedString);
            if(matcher.find())
            {
                lemmatizedString = lemmatizedString.replaceAll("located", "");
                lemmatizedString = lemmatizedString.replaceAll("set", "");
                map.put("key","Country");
                map.put("value",countryList.get(i));
                constraints.add(map);
                lemmatizedString = lemmatizedString.replaceAll(countryList.get(i),"");
            }
        }

        for(int i=0; i<languageList.size();i++)
        {
            Map<String, String> map = new HashMap<>();
            Pattern pattern = Pattern.compile(languageList.get(i).toLowerCase());
            Matcher matcher = pattern.matcher(lemmatizedString);
            if(matcher.find())
            {
                lemmatizedString = lemmatizedString.replaceAll("language", "");
                map.put("key", "Language");
                map.put("value",languageList.get(i));
                constraints.add(map);
                lemmatizedString = lemmatizedString.replaceAll(languageList.get(i),"");
            }
        }

        Pattern pattern1 = Pattern.compile("[1-9][0-9]{3}");
        Matcher matcher1 = pattern1.matcher(lemmatizedString.trim());
        for(int i=0; i < releaseWords.length; i++ ){
            Map<String, String> map = new HashMap<>();
            if(lemmatizedString.contains(releaseWords[i])){
                lemmatizedString = lemmatizedString.replaceAll("release", "");
                lemmatizedString = lemmatizedString.replaceAll(releaseWords[i],"");
                if(matcher1.find())
                {
                    map.put("key","Release date");
                    map.put("value",matcher1.group());
                    constraints.add(map);
                    lemmatizedString = lemmatizedString.replaceAll(matcher1.group(),"");
                }
            }
        }


        for(int i=0; i < boxOfficeList.size(); i++ ){
            Map<String, String> map = new HashMap<>();
            Pattern pattern = Pattern.compile(boxOfficeList.get(i));
            Matcher matcher = pattern.matcher(lemmatizedString);
            if(matcher.find()){
                lemmatizedString = lemmatizedString.replaceAll("box office collection", "");
                lemmatizedString = lemmatizedString.replaceAll("collect", "");
                lemmatizedString = lemmatizedString.replaceAll("gross", "");
                map.put("key","BoxOffice");
                map.put("value",boxOfficeList.get(i));
                constraints.add(map);
                lemmatizedString = lemmatizedString.replaceAll(boxOfficeList.get(i),"");
            }
        }
        output.setConstraint(constraints);

        List<String> concepts = new ArrayList<>();
        for (int i=0; i<conceptList.size();i++)
        {
            lemmatizedString = lemmatizedString.replaceAll("film", "movie");
            Pattern pattern = Pattern.compile(conceptList.get(i).toLowerCase());
            Matcher matcher = pattern.matcher(lemmatizedString);
            if(matcher.find())
            {
                if(conceptList.get(i).equals("act")||conceptList.get(i).equals("actor"))
                {
                    concepts.add("Actor");
                    lemmatizedString = lemmatizedString.replaceAll("act", "");
                    lemmatizedString = lemmatizedString.replaceAll("actor","");
                }
                else {
                    concepts.add(conceptList.get(i));
                }
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

