package com.knowably.notificationservice.controller;

import com.google.gson.Gson;
import com.knowably.notificationservice.domain.QueryResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;



/**
 * RestController annotation is used to create Restful web services using Spring MVC
 */
@RestController
public class NotificationController {

    private QueryResponse queryResponse;


    /**
     * Constructor based Dependency injection to inject SimpMessagingTemplate into controller
     */
    @Autowired
    private SimpMessagingTemplate template;


    /**Checking the service with the postman to make sure it is working fine*/

    /**Postmapping will call the uploadContent method and takes queryresponse object as a parameter in the requestBody*/
    @PostMapping("/content")
    public void uploadContent(@RequestBody QueryResponse queryResponse){
        this.queryResponse=new QueryResponse();
        this.queryResponse=queryResponse;
    }


//    private QueryResponse queryResponse=new QueryResponse("query","medical","cancer is .....");

    /**GetMapping will call the getNotification method*/
    @GetMapping("/content")
    public String getNotification() {
        template.convertAndSend("/topic/notification", queryResponse.getResult()[0]);

        return "Notifications successfully sent to Angular !";
    }


    /**
     * The @MessageMapping annotation ensures that if a notification is sent to destination "/notification",
     * then the getNotification() method is called.
     */
    @MessageMapping("/notification")
    public String getNotification1() {

        template.convertAndSend("/topic/notification", queryResponse.getResult()[0]);

        return "Notifications successfully sent to Angular !";
    }



/**
    *Kafka listener which can be used to call consume method when something is published in the finalresult Topic
*/
    @KafkaListener(topics ="FinalResult",groupId = "service",containerFactory = "kafkaListenerContainerFactory")
    public void consume(String receivedResponse){

        if (receivedResponse.equals("wait")) {
            template.convertAndSend("/topic/notification","Query is getting processed");

        }
        else if (receivedResponse.equals("notFound")){
            template.convertAndSend("/topic/notification","Sorry!!the result for your query is not found,Search for another query");
        }

        Gson gson=new Gson();
        QueryResponse queryResponse=gson.fromJson(receivedResponse,QueryResponse.class);
        queryResponse.setLocalDateTime(LocalDateTime.now());
        System.out.println(queryResponse);

/**
        *The return value is broadcast to users who subscribes to "/topic/notification"
*/

        template.convertAndSend("/topic/notification", queryResponse.getResult().toString());
        System.out.println("Response successfully sent to Angular !");
    }





}
