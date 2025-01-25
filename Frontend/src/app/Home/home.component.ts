import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HomeConfiguracionComponent } from '../Configuracion/features/home-configuracion/home-configuracion.component';

@Component({
  standalone: true,
  selector: 'app-home',
  imports: [CommonModule, HomeConfiguracionComponent],
  templateUrl: './home.component.html',
  styleUrls: ['./home.component.css'],
})
export class HomeComponent {
  currentView: string = 'inicio'; // Vista actual

  changeView(view: string): void {
    this.currentView = view; // Cambia la vista dinámica
  }
}
