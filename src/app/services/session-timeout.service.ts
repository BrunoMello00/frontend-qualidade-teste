import { Injectable } from '@angular/core';
import { timer, BehaviorSubject } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class SessionTimeoutService {
  private timeoutMinutes = 30; // 30 minutos
  private warningMinutes = 5; // Aviso 5 minutos antes
  private lastActivity = new Date();
  private timeoutSubject = new BehaviorSubject<number>(this.timeoutMinutes * 60);

  constructor() {
    this.resetTimer();
    this.startTimer();
    this.addActivityListeners();
  }

  private startTimer(): void {
    timer(0, 1000).subscribe(() => {
      const now = new Date();
      const elapsed = Math.floor((now.getTime() - this.lastActivity.getTime()) / 1000);
      const remaining = (this.timeoutMinutes * 60) - elapsed;
      
      this.timeoutSubject.next(Math.max(0, remaining));
      
      if (remaining <= 0) {
        this.logout();
      }
    });
  }

  private addActivityListeners(): void {
    ['mousedown', 'mousemove', 'keypress', 'scroll', 'touchstart', 'click'].forEach(event => {
      document.addEventListener(event, () => this.resetTimer(), true);
    });
  }

  resetTimer(): void {
    this.lastActivity = new Date();
  }



  private logout(): void {
    localStorage.clear();
    sessionStorage.clear();
    window.location.href = '/login';
  }



  setTimeoutDuration(minutes: number): void {
    this.timeoutMinutes = minutes;
    this.resetTimer();
  }

  startMonitoring(): void {
    this.resetTimer();
    console.log(`Monitoramento de sessão iniciado: ${this.timeoutMinutes} minutos`);
  }
}
