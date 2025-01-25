import { Component, OnInit } from '@angular/core';
import { ConfiguracionService } from '../../dataAccess/configuracion.service';
import { Configuracion } from '../../dataAccess/ConfiguracionEntity';

@Component({
  standalone: true,
  selector: 'app-home-configuracion',
  imports: [],
  templateUrl: './home-configuracion.component.html',
  styleUrls: ['./home-configuracion.component.css'], // Cambié 'styleUrl' a 'styleUrls' para corregir el error.
})
export class HomeConfiguracionComponent implements OnInit {
  configuraciones: { [key: string]: string } = {}; // Almacenará las configuraciones clave-valor.
  editable: { [key: string]: boolean } = {}; // Controlará si cada campo está habilitado o no.

  constructor(private configuracionService: ConfiguracionService) {}

  ngOnInit(): void {
    this.configuracionService.getConfiguraciones().subscribe({
      next: (data: Configuracion[]) => {
        // Convertimos la lista en un objeto clave-valor
        this.configuraciones = data.reduce((acc, item) => {
          acc[item.configClave] = item.configValor;
          this.editable[item.configClave] = false; // Inicialmente todos los campos están deshabilitados.
          return acc;
        }, {} as { [key: string]: string });
      },
      error: (error) => {
        console.error('Error obteniendo configuraciones:', error);
      },
    });
  }

  /**
   * Alterna la habilitación del input asociado a una clave específica.
   * @param campo - Clave del campo a habilitar/deshabilitar.
   */
  toggleEditable(campo: string): void {
    this.editable[campo] = !this.editable[campo];
  }
}
