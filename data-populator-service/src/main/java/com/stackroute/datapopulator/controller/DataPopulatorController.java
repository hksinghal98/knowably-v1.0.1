package com.stackroute.datapopulator.controller;
import com.stackroute.datapopulator.service.DataFetcherService;
import com.stackroute.datapopulator.service.DataPopulatorService;
import com.stackroute.datapopulator.service.QueryTriggerService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@RestController
@RequestMapping("/api")
public class DataPopulatorController {
    private DataPopulatorService dataPopulatorService;
    private DataFetcherService dataFetcherService;
    private QueryTriggerService queryTriggerService;
    public DataPopulatorController(DataPopulatorService dataPopulatorService, DataFetcherService dataFetcherService,QueryTriggerService queryTriggerService) {
        this.dataPopulatorService = dataPopulatorService;
        this.dataFetcherService = dataFetcherService;
        this.queryTriggerService=queryTriggerService;
    }
    //------------------to populate the database-------------------------
    @PostMapping
    @KafkaListener(topics = "WikiScrapper", groupId = "group_id", containerFactory = "kafkaListenerContainerFactory")
    public boolean dataPopulatorController(String param) throws IOException {
        boolean flag=false;
        String flag1=dataPopulatorService.dataPopulator(param.toLowerCase());
        if(flag1=="null"){flag=dataPopulatorService.dataMerger();}
        else{
            flag=dataPopulatorService.dataMerger();
            queryTriggerService.queryServiceTrigger(flag1);
        }
        if (flag==true){return true;}
        else return false;
    }
    //------------------to fetch domain info from database-------------------------
    @GetMapping
    @KafkaListener(topics = "SchedulerFlag", groupId = "group_id", containerFactory = "kafkaListenerContainerFactory")
    public boolean dataFetcherController(String param){
        System.out.println(param);
        dataFetcherService.dataFetcher();
        return true;
    }

}
