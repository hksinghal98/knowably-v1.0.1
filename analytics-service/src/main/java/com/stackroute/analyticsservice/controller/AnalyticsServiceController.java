package com.stackroute.analyticsservice.controller;

import com.stackroute.analyticsservice.cassandra.QueryResultResponse;
import com.stackroute.analyticsservice.domain.AnalyticsOutput;
import com.stackroute.analyticsservice.domain.Input;
import com.stackroute.analyticsservice.service.AnalyticsService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@CrossOrigin("*")
@RequestMapping("api/v1")
public class AnalyticsServiceController {
    private ResponseEntity responseEntity;
    private final AnalyticsService analyticsService;

    public AnalyticsServiceController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @PostMapping("response")
    public ResponseEntity<?> processResponse(@RequestBody Input input){
        try{
            if(analyticsService.existsByQuery(input.getQuery()))
            {
                analyticsService.updateQuery(input);
                responseEntity = new ResponseEntity<String>("Updated Successfully", HttpStatus.OK);
            }
            else
            {
                analyticsService.saveQuery(input);
                responseEntity = new ResponseEntity<String>("Added successfully",HttpStatus.OK);
            }
        } catch (Exception e)
        {
            responseEntity = new ResponseEntity<String>(e.getMessage(),HttpStatus.CONFLICT);
        }
        return responseEntity;
    }
    @GetMapping("display")
    public ResponseEntity<?> showResults() {
        try {
            List<QueryResultResponse> list = analyticsService.getResults();
            QueryResultResponse[] list1 = new QueryResultResponse[list.size()];
            int i=0;
            for(QueryResultResponse a:list)
            {
                list1[i] = a;
                i++;
            }
            responseEntity = new ResponseEntity<QueryResultResponse[]>(list1,HttpStatus.OK);
        } catch (Exception e)
        {
            responseEntity = new ResponseEntity<String>(e.getMessage(),HttpStatus.CONFLICT);
        }
        return responseEntity;
    }
    @GetMapping("display/movie")
    public ResponseEntity<?> showMovieResults() {
        try {
            List<QueryResultResponse> list = analyticsService.getResultsByDomain("movie");
            QueryResultResponse[] list1 = new QueryResultResponse[list.size()];
            int i=0;
            for(QueryResultResponse a:list)
            {
                list1[i] = a;
                i++;
            }
            responseEntity = new ResponseEntity<QueryResultResponse[]>(list1,HttpStatus.OK);
        } catch (Exception e)
        {
            responseEntity = new ResponseEntity<String>(e.getMessage(),HttpStatus.CONFLICT);
        }
        return responseEntity;
    }
    @GetMapping("display/medical")
    public ResponseEntity<?> showMedicalResults() {
        try {
            List<QueryResultResponse> list = analyticsService.getResultsByDomain("medical");
            QueryResultResponse[] list1 = new QueryResultResponse[list.size()];
            int i=0;
            for(QueryResultResponse a:list)
            {
                list1[i] = a;
                i++;
            }
            responseEntity = new ResponseEntity<QueryResultResponse[]>(list1,HttpStatus.OK);
        } catch (Exception e)
        {
            responseEntity = new ResponseEntity<String>(e.getMessage(),HttpStatus.CONFLICT);
        }
        return responseEntity;
    }
    @GetMapping("analytics")
    public ResponseEntity<?> getGraphData() {
        try {
            responseEntity = new ResponseEntity<AnalyticsOutput>(analyticsService.getAnalyticsData(),HttpStatus.OK);
        } catch (Exception e)
        {
            responseEntity = new ResponseEntity<String>(e.getMessage(),HttpStatus.CONFLICT);
        }
        return responseEntity;
    }
}
