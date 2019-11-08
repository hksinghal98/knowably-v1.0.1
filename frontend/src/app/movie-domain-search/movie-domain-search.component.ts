import { Component, OnInit } from '@angular/core';
import {MovieSearchService} from '../movie_search_service/movie-search.service';
import { Router } from '@angular/router';

@Component({
  selector: 'app-movie-domain-search',
  templateUrl: './movie-domain-search.component.html',
  styleUrls: ['./movie-domain-search.component.css']
})
export class MovieDomainSearchComponent implements OnInit {

  constructor( private movieSearchService: MovieSearchService, private route:Router) { }

  ngOnInit() {
  }

  userSearch(searchQuery){
    console.log(searchQuery);
     this.movieSearchService.userSearchService(searchQuery)
                        .subscribe(data=>{
                              console.log(data);
                              this.route.navigateByUrl('');
                            },error=>{
                              console.log(error);
                              this.route.navigateByUrl('/movie-domain');
                            });
  }


}
