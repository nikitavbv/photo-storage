import { Injectable } from '@angular/core';
import {Photo} from "../_models/photo";

@Injectable()
export class SearchService {

  readonly DATE_REQUESTS = {
    'today': [0, 1000 * 60 * 60 * 24],
    'yesterday': [1000 * 60 * 60 * 24, 1000 * 60 * 60 * 24 * 2],
    'day ago': [1000 * 60 * 60 * 24, 1000 * 60 * 60 * 24 * 2],
    'this week': [0, 1000 * 60 * 60 * 24 * 7],
    'last week': [1000 * 60 * 60 * 24 * 7, 1000 * 60 * 60 * 24 * 14],
    'past week': [1000 * 60 * 60 * 24 * 7, 1000 * 60 * 60 * 24 * 14],
    'week ago': [1000 * 60 * 60 * 24 * 7, 1000 * 60 * 60 * 24 * 14],
    'this month': [0, 1000 * 60 * 60 * 24 * 31],
    'last month': [1000 * 60 * 60 * 24 * 62, 1000 * 60 * 60 * 24 * 31],
    'past month': [1000 * 60 * 60 * 24 * 62, 1000 * 60 * 60 * 24 * 31],
    'month ago': [1000 * 60 * 60 * 24 * 62, 1000 * 60 * 60 * 24 * 31],
    'this year': [0, 1000 * 60 * 60 * 24 * 365],
    'last year': [1000 * 60 * 60 * 24 * 365 * 2, 1000 * 60 * 60 * 24 * 365],
    'past year': [1000 * 60 * 60 * 24 * 365 * 2, 1000 * 60 * 60 * 24 * 365],
    'year ago': [1000 * 60 * 60 * 24 * 365 * 2, 1000 * 60 * 60 * 24 * 365]
  };

  constructor() {}

  filter(photos: Photo[], query: string): Photo[] {
    return photos.filter(photo => this.matchesFilter(photo, query));
  }

  matchesFilter(photo: Photo, query: string): boolean {
    query = query.trim();

    if (photo.description && photo.description.indexOf(query) !== -1) {
      return true;
    }

    if (photo.tags) {
      for (let tag of photo.tags) {
        if (query.indexOf(tag) !== -1) {
          return true;
        }
      }
    }

    if (this.DATE_REQUESTS[query.toLowerCase()]) {
      const now = new Date().getTime();
      const bounds = this.DATE_REQUESTS[query.toLowerCase()];
      const minBound = now - bounds[0];
      const maxBound = now - bounds[1];

      if (photo.uploaded_at > maxBound && photo.uploaded_at < minBound) {
        return true;
      }
    }

    return false;
  }
}
