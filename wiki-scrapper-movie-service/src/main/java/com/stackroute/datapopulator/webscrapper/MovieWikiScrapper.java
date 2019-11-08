package com.stackroute.datapopulator.webscrapper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.stackroute.datapopulator.domain.Wikiurl;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.*;


import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@RestController
@CrossOrigin("*")
@RequestMapping(value = "api/v1")
@PropertySource("classpath:application.properties")
public class MovieWikiScrapper {
    private static List<String> param = new ArrayList<>();
    @Value("${topic}")
    private String topic;
    @Value("${uuid}")
    private String uuid;
    @Value("${type}")
    private String type;
    @Value("${name}")
    private String name;
    @Value("${properties}")
    private String properties;
    @Value("${destNode}")
    private String destNode;
    @Value("${sourceNode}")
    private String sourceNode;
    @Value("${relation}")
    private String relation;


    KafkaTemplate<String,String> kafkaTemplate;

    @Autowired
    public MovieWikiScrapper(KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    /*
            function to listen on topic "Wikipedia" to consume wikipedia links
            and crawl the wikipedia page of Medical domain and transform
            data into structured data
        */
    @KafkaListener(topics = "MovieWikipedia",groupId = "wiki_scrapper",containerFactory = "kafkaListenerContainerFactory")
    public void collectWebTableData1(String received_input) {

        /*
            Converting String to JSON object
        */
        Gson gson=new Gson();
        int length=received_input.length();
        String receivedInput2=received_input.substring(1,length-1);
        Wikiurl input1=gson.fromJson(receivedInput2, Wikiurl.class);

        /*
            Getting url from the input object from google-search-service
         */
        String URL=input1.getUrl()[0];
        /*
        * Initializing nodes and realtionship array to store all nodes and their relations
        * */
        JSONArray nodesArray = new JSONArray();
        JSONArray relationArray = new JSONArray();
        String id = "";
        try {
            /*
             * fetching web document from the url
             */
            String header = "";
            Document source = Jsoup.connect(URL).get();

            /*
            * Extracting the table that contains the data of use from wikipedia page
            * */
            Elements table = source.select("table.infobox");
            Elements title = table.select("tr th.summary");
            for(Element el : title){
                id = UUID.randomUUID().toString();
                JSONObject node = new JSONObject();

                /*
                * Creating node for the name of movie
                * */
                node.put(this.uuid, id);
                node.put(this.type, "Movie");
                JSONObject jo = new JSONObject();
                jo.put(this.name, el.text().trim());
                node.put(this.properties, jo);
                nodesArray.add(node);
                break;
            }

            /*
            * Traversing the rows of the tables
            * */
            Elements rows = table.select("tr");
            for (Element row : rows) {
                Elements data1 = row.select("th");
                Elements data2 = row.select("td");
                if (!data1.select("div").isEmpty()) {
                    Elements headerElement = data1.select("div");
                    header = headerElement.html();
                    header = header.replaceAll("<br>", "\n").replaceAll("\n", "");
                } else {
                    header = data1.html();
                    header = header.replaceAll("<br>", " ");
                }

                //Extracting Cast of the specific movie from Html Document
                if (header.matches("Starring")) {
                    if (!data2.select("div").isEmpty()) {
                        Elements data3 = data2.select("div ul li a");
                        String str1 = data3.html();
                        String[] str = str1.replaceAll("\\[.*?\\]", "").replaceAll("\\(.*?\\)", "").replaceAll("[a-zA-Z]+[:]", ",").split("\n");
                        for (int i = 0; i < str.length; i++) {
                            if (!str[i].isEmpty()) {
                                String id1 = UUID.randomUUID().toString();
                                JSONObject node = new JSONObject();
                                /*
                                * Creating node for each actor extracted from the page
                                * */
                                node.put(this.uuid, id1);
                                node.put(this.type, "Actor");
                                JSONObject jo = new JSONObject();
                                jo.put(this.name, str[i].trim());
                                node.put(this.properties, jo);
                                nodesArray.add(node);

                                /*
                                * Establishing a relationship between actor node and the movie title node
                                * */
                                JSONObject jo1 = new JSONObject();
                                String relationId = UUID.randomUUID().toString();
                                jo1.put(this.uuid, relationId);
                                jo1.put(this.destNode,id);
                                jo1.put(this.sourceNode, id1);
                                jo1.put(this.relation, "acted by");
                                relationArray.add(jo1);
                            }
                        }
                    } else {
                        Elements data3 = data2.select("a");
                        String str1 = data3.html();
                        String[] str = str1.replaceAll("\\[.*?\\]", "").replaceAll("\\(.*?\\)", "").replaceAll("[a-zA-Z]+[:]", ",").split("\n");
                        for (int i = 0; i < str.length; i++) {
                            if (!str[i].isEmpty()) {
                                /*
                                 * Creating node for each actor extracted from the page
                                 * */
                                String id1 = UUID.randomUUID().toString();
                                JSONObject node = new JSONObject();
                                node.put(this.uuid, id1);
                                node.put(this.type, "Actor");

                                JSONObject jo = new JSONObject();
                                jo.put(this.name, str[i].trim());
                                node.put(this.properties, jo);
                                nodesArray.add(node);
                                /*
                                 * Establishing a relationship between actor node and the movie title node
                                 * */
                                JSONObject jo1 = new JSONObject();
                                String relationId = UUID.randomUUID().toString();
                                jo1.put(this.uuid, relationId);
                                jo1.put(this.sourceNode,id);
                                jo1.put(this.destNode, id1);
                                jo1.put(this.relation, "acted by");
                                relationArray.add(jo1);
                            }
                        }
                    }
                }

                //Extracting Production Company from Html Document
                if (header.matches("Productioncompany")) {
                    if (!data2.select("div").isEmpty()) {
                        Elements data3 = data2.select("div");
                        if (!data2.select("a").isEmpty()) {
                            Elements data4 = data2.select("a");
                            String str1 = data4.html();
                            str1 = str1.replaceAll("<br>", "\n");
                            String[] str = str1.replaceAll("\\[.*?\\]", "").replaceAll("\\(.*?\\)", "").replaceAll("[a-zA-Z]+[:]", ",").split("\n");
                            for (int i = 0; i < str.length; i++) {
                                if (!str[i].isEmpty()) {
                                    /*
                                     * Creating node for each production company extracted from the page
                                     * */
                                    String id1 = UUID.randomUUID().toString();
                                    JSONObject node = new JSONObject();
                                    node.put(this.uuid, id1);
                                    node.put(this.type, "Produced by");

                                    JSONObject jo = new JSONObject();
                                    jo.put(this.name, str[i].trim());
                                    node.put(this.properties, jo);
                                    nodesArray.add(node);

                                    /*
                                     * Establishing a relationship between production company node and the movie title node
                                     * */
                                    JSONObject jo1 = new JSONObject();
                                    String relationId = UUID.randomUUID().toString();
                                    jo1.put(this.uuid, relationId);
                                    jo1.put(this.sourceNode,id);
                                    jo1.put(this.destNode, id1);
                                    jo1.put(this.relation, "produced by");
                                    relationArray.add(jo1);
                                }
                            }
                        }
                        String column2 = data3.html();
                        column2 = column2.replaceAll("<br>", "\n");
                        String[] str = column2.replaceAll("\\[.*?\\]", "").replaceAll("\\(.*?\\)", "").replaceAll("[a-zA-Z]+[:]", ",").split("\n");
                        for (int i = 0; i < str.length; i++) {
                            if (!str[i].isEmpty()) {
                                if (!str[i].contains("<a ")) {
                                    /*
                                     * Creating node for each production company extracted from the page
                                     * */
                                    String id1 = UUID.randomUUID().toString();
                                    JSONObject node = new JSONObject();
                                    node.put(this.uuid, id1);
                                    node.put(this.type, "Produced by");

                                    JSONObject jo = new JSONObject();
                                    jo.put(this.name, str[i].trim());
                                    node.put(this.properties, jo);
                                    nodesArray.add(node);

                                    /*
                                     * Establishing a relationship between production company node and the movie title node
                                     * */
                                    JSONObject jo1 = new JSONObject();
                                    String relationId = UUID.randomUUID().toString();
                                    jo1.put(this.uuid, relationId);
                                    jo1.put(this.sourceNode,id);
                                    jo1.put(this.destNode, id1);
                                    jo1.put(this.relation, "produced by");
                                    relationArray.add(jo1);
                                }
                            }
                        }

                    }
                }

                //Extracting Directors from Html Document
                if (header.matches("Directed by")) {
                    Elements data4 = data2.select("a");
                    String str1 = data4.html();
                    String[] str = str1.replaceAll("\\[.*?\\]", "").replaceAll("\\(.*?\\)", "").replaceAll("[a-zA-Z]+[:]", ",").split("\n");
                    for (int i = 0; i < str.length; i++) {
                        if (!str[i].isEmpty()) {
                            /*
                             * Creating node for each Director extracted from the page
                             * */
                            String id1 = UUID.randomUUID().toString();
                            JSONObject node = new JSONObject();
                            node.put(this.uuid, id1);
                            node.put(this.type, "Directed by");

                            JSONObject jo = new JSONObject();
                            jo.put(this.name, str[i].trim());
                            node.put(this.properties, jo);
                            nodesArray.add(node);

                            /*
                             * Establishing a relationship between Director node and the movie title node
                             * */
                            JSONObject jo1 = new JSONObject();
                            String relationId = UUID.randomUUID().toString();
                            jo1.put(this.uuid, relationId);
                            jo1.put(this.sourceNode,id);
                            jo1.put(this.destNode, id1);
                            jo1.put(this.relation, "directed by");
                            relationArray.add(jo1);
                        }
                    }
                }

                //Extracting Country from Html Document
                if (header.matches("Country")) {
                    if (!data2.select("div").isEmpty()) {
                        Elements data3 = data2.select("div ul li");
                        String country = data3.html();
                        String[] str = country.replaceAll("\\[.*?\\]", "").replaceAll("\\(.*?\\)", "").replaceAll("[a-zA-Z]+[:]", ",").split("\n");
                        for (int i = 0; i < str.length; i++) {
                            if (!str[i].isEmpty()) {
                                String str1 = str[i].substring(0, str[i].indexOf("<sup"));
                                /*
                                 * Creating node for each Country extracted from the page
                                 * */
                                String id1 = UUID.randomUUID().toString();
                                JSONObject node = new JSONObject();
                                node.put(this.uuid, id1);
                                node.put(this.type, "Country");

                                JSONObject jo = new JSONObject();
                                jo.put(this.name, str1.trim());
                                node.put(this.properties, jo);
                                nodesArray.add(node);

                                /*
                                 * Establishing a relationship between Country node and the movie title node
                                 * */
                                JSONObject jo1 = new JSONObject();
                                String relationId = UUID.randomUUID().toString();
                                jo1.put(this.uuid, relationId);
                                jo1.put(this.sourceNode,id);
                                jo1.put(this.destNode, id1);
                                jo1.put(this.relation, "released in");
                                relationArray.add(jo1);
                            }
                        }
                    } else {
                        String str1 = data2.html();
                        String[] str = str1.replaceAll("\\[.*?\\]", "").replaceAll("\\(.*?\\)", "").replaceAll("[a-zA-Z]+[:]", ",").split("\n");
                        for (int i = 0; i < str.length; i++) {
                            if (!str[i].isEmpty()) {

                                /*
                                 * Creating node for each Country extracted from the page
                                 * */
                                String id1 = UUID.randomUUID().toString();
                                JSONObject node = new JSONObject();
                                node.put(this.uuid, id1);
                                node.put(this.type, "Country");

                                JSONObject jo = new JSONObject();
                                jo.put(this.name, str[i].trim());
                                node.put(this.properties, jo);
                                nodesArray.add(node);

                                /*
                                 * Establishing a relationship between Country node and the movie title node
                                 * */
                                JSONObject jo1 = new JSONObject();
                                String relationId = UUID.randomUUID().toString();
                                jo1.put(this.uuid, relationId);
                                jo1.put(this.sourceNode,id);
                                jo1.put(this.destNode, id1);
                                jo1.put(this.relation, "released in");
                                relationArray.add(jo1);
                            }
                        }
                    }
                }

                //Extracting Language from Html Document
                if (header.matches("Language")) {
                    if (!data2.select("div").isEmpty()) {
                        Elements data3 = data2.select("div ul li");
                        String country = data3.html();
                        String[] str = country.replaceAll("\\[.*?\\]", "").replaceAll("\\(.*?\\)", "").replaceAll("[a-zA-Z]+[:]", ",").split("\n");
                        for (int i = 0; i < str.length; i++) {
                            if (!str[i].isEmpty()) {
                                /*
                                 * Creating node for each Language extracted from the page
                                 * */
                                String id1 = UUID.randomUUID().toString();
                                JSONObject node = new JSONObject();
                                node.put(this.uuid, id1);
                                node.put(this.type, "Language");

                                JSONObject jo = new JSONObject();
                                jo.put(this.name, str[i].trim());
                                node.put(this.properties, jo);
                                nodesArray.add(node);

                                /*
                                 * Establishing a relationship between Language node and the movie title node
                                 * */
                                JSONObject jo1 = new JSONObject();
                                String relationId = UUID.randomUUID().toString();
                                jo1.put(this.uuid, relationId);
                                jo1.put(this.sourceNode,id);
                                jo1.put(this.destNode, id1);
                                jo1.put(this.relation, "language");
                                relationArray.add(jo1);
                            }
                        }
                    } else {
                        String str1 = data2.html();
                        String[] str = str1.replaceAll("\\[.*?\\]", "").replaceAll("\\(.*?\\)", "").replaceAll("[a-zA-Z]+[:]", ",").split("\n");
                        for (int i = 0; i < str.length; i++) {
                            if (!str[i].isEmpty()) {

                                /*
                                 * Creating node for each Language extracted from the page
                                 * */
                                String id1 = UUID.randomUUID().toString();
                                JSONObject node = new JSONObject();
                                node.put(this.uuid, id1);
                                node.put(this.type, "Language");

                                JSONObject jo = new JSONObject();
                                jo.put(this.name, str[i].trim());
                                node.put(this.properties, jo);
                                nodesArray.add(node);

                                /*
                                 * Establishing a relationship between Language node and the movie title node
                                 * */
                                JSONObject jo1 = new JSONObject();
                                String relationId = UUID.randomUUID().toString();
                                jo1.put(this.uuid, relationId);
                                jo1.put(this.sourceNode,id);
                                jo1.put(this.destNode, id1);
                                jo1.put(this.relation, "language");
                                relationArray.add(jo1);
                            }
                        }
                    }
                }

                //Extracting Release Date from Html Document
                if (header.matches("Release date")) {
                    Elements data3 = data2.select("div ul li");
                    String str1 = data3.html().substring(0, data3.html().indexOf("<span"));
                    String[] str = str1.replaceAll("&nbsp;", " ").split("\n");
                    for (int i = 0; i < str.length; i++) {
                        if (!str[i].isEmpty()) {

                            /*
                             * Creating node for release date extracted from the page
                             * */
                            String id1 = UUID.randomUUID().toString();
                            JSONObject node = new JSONObject();
                            node.put(this.uuid, id1);
                            node.put(this.type, "Release Date");

                            JSONObject jo = new JSONObject();
                            jo.put(this.name, str[i].trim());
                            node.put(this.properties, jo);
                            nodesArray.add(node);

                            /*
                             * Establishing a relationship between release date node and the movie title node
                             * */
                            JSONObject jo1 = new JSONObject();
                            String relationId = UUID.randomUUID().toString();
                            jo1.put(this.uuid, relationId);
                            jo1.put(this.sourceNode,id);
                            jo1.put(this.destNode, id1);
                            jo1.put(this.relation, "released on");
                            relationArray.add(jo1);
                        }
                    }
                }

                //Extracting Box Office Collection from Html Document
                if (header.matches("Box office")) {
                    if (!data2.select("span").isEmpty()) {
                        String boxOffice = data2.html();
                        String amountTemp = "";
                        String str1 = boxOffice.substring(boxOffice.indexOf("</span>") + 7, boxOffice.indexOf("<sup")).replaceAll("&nbsp;"," ");
                        if (data2.select("span").html().length() > 1) {
                            int len = data2.select("span").html().length();
                            String amount = data2.select("span").html().substring(1, len);
                            amountTemp = "Rs. " + amount;
                        } else {
                            amountTemp = "Rs. ";
                        }
                        str1 = amountTemp + str1;
                        /*
                         * Creating node for box office extracted from the page
                         * */
                        String id1 = UUID.randomUUID().toString();
                        JSONObject node = new JSONObject();
                        node.put(this.uuid, id1);
                        node.put(this.type, "BoxOffice");

                        JSONObject jo = new JSONObject();
                        jo.put(this.name, str1.trim());
                        node.put(this.properties, jo);
                        nodesArray.add(node);

                        /*
                         * Establishing a relationship between box office node and the movie title node
                         * */
                        JSONObject jo1 = new JSONObject();
                        String relationId = UUID.randomUUID().toString();
                        jo1.put(this.uuid, relationId);
                        jo1.put(this.sourceNode,id);
                        jo1.put(this.destNode, id1);
                        jo1.put(this.relation, "collection");
                        relationArray.add(jo1);
                    } else {
                        String boxOffice = data2.html().substring(0, data2.html().indexOf("<sup")).replaceAll("&nbsp;"," ");
                        String id1 = UUID.randomUUID().toString();
                        /*
                         * Creating node for box office extracted from the page
                         * */
                        JSONObject node = new JSONObject();
                        node.put(this.uuid, id1);
                        node.put(this.type, "BoxOffice");

                        JSONObject jo = new JSONObject();
                        jo.put(this.name, boxOffice.trim());
                        node.put(this.properties, jo);
                        nodesArray.add(node);
                        /*
                         * Establishing a relationship between box office node and the movie title node
                         * */
                        JSONObject jo1 = new JSONObject();
                        String relationId = UUID.randomUUID().toString();
                        jo1.put(this.uuid, relationId);
                        jo1.put(this.sourceNode,id);
                        jo1.put(this.destNode, id1);
                        jo1.put(this.relation, "collection");
                        relationArray.add(jo1);
                    }
                    break;
                }
            }

            /* creating a jsonObject contains nodesArray and relationship array*/
            JSONObject jsonDTO = new JSONObject();
            jsonDTO.put("nodes",nodesArray);
            jsonDTO.put("relationship",relationArray);
            jsonDTO.put("query", input1.getQuery());
            jsonDTO.put("userId",input1.getUserId());

            //producing the jsonDTO in Kafaka
            ObjectMapper objectMapper=new ObjectMapper();
            try {
                String resultPayload= objectMapper.writeValueAsString(jsonDTO);
                kafkaTemplate.send(topic,"went");
                kafkaTemplate.send(topic,resultPayload);


            } catch (JsonProcessingException e) {
                e.printStackTrace();
            }
        }

        catch (IOException e) {
            System.out.println("ERROR : Failed to access " + URL);
        }
    }

}
