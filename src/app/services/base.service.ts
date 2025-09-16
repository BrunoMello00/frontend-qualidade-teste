import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { EnvironmentService } from './environment.service';

@Injectable({
  providedIn: 'root'
})
export class BaseService {

  constructor(
    protected http: HttpClient,
    protected environmentService: EnvironmentService
  ) {}

  protected getApiUrl(): string {
    return this.environmentService.getApiUrl();
  }

  protected getHttpOptions(): { headers: HttpHeaders } {
    const token = localStorage.getItem('token');
    return {
      headers: new HttpHeaders({
        'Content-Type': 'application/json',
        'Authorization': token ? `Bearer ${token}` : ''
      })
    };
  }

  protected buildParams(params: any): HttpParams {
    let httpParams = new HttpParams();
    
    Object.keys(params).forEach(key => {
      if (params[key] !== null && params[key] !== undefined && params[key] !== '') {
        if (params[key] instanceof Date) {
          httpParams = httpParams.set(key, params[key].toISOString().split('T')[0]);
        } else {
          httpParams = httpParams.set(key, params[key].toString());
        }
      }
    });
    
    return httpParams;
  }

  protected get<T>(url: string, params?: { [key: string]: string }): Observable<T> {
    const options = this.getHttpOptions();
    if (params) {
      const httpParams = this.buildParams(params);
      return this.http.get<T>(`${this.getApiUrl()}${url}`, { ...options, params: httpParams });
    }
    return this.http.get<T>(`${this.getApiUrl()}${url}`, options);
  }

  protected post<T>(url: string, body: any): Observable<T> {
    return this.http.post<T>(`${this.getApiUrl()}${url}`, body, this.getHttpOptions());
  }

  protected put<T>(url: string, body: any): Observable<T> {
    return this.http.put<T>(`${this.getApiUrl()}${url}`, body, this.getHttpOptions());
  }

  protected patch<T>(url: string, body?: any): Observable<T> {
    return this.http.patch<T>(`${this.getApiUrl()}${url}`, body || {}, this.getHttpOptions());
  }

  protected delete<T>(url: string): Observable<T> {
    return this.http.delete<T>(`${this.getApiUrl()}${url}`, this.getHttpOptions());
  }
}
