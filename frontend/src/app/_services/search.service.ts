import { Injectable } from '@angular/core';
import {Photo} from "../_models/photo";

@Injectable()
export class SearchService {

  constructor() {}

  filter(photos: Photo[], query: string): Photo[] {
    return photos.filter(photo => SearchService.matchesFilter(photo, query));
  }

  static matchesFilter(photo: Photo, query: string): boolean {
    if (photo.description && photo.description.indexOf(query.trim()) !== -1) {
      return true;
    }
    
    return false;
  }
}
