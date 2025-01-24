import { Injectable } from '@angular/core';
import {
  HttpClient,
  HttpErrorResponse,
  HttpHeaders,
} from '@angular/common/http';
import { Observable, throwError } from 'rxjs';
import { catchError } from 'rxjs/operators';
import Swal from 'sweetalert2';
import { Configuracion } from '../dataAccess/ConfiguracionEntity';

@Injectable({
  providedIn: 'root',
})
export class ConfiguracionService {
  private httpHeaders = new HttpHeaders({ 'Content-Type': 'application/json' });
  private urlEndPoint: string = 'http://127.0.0.1:5000/api/configuracion';

  constructor(private http: HttpClient) {}

  getConfiguraciones(): Observable<Configuracion[]> {
    console.log('Listando configuraciones desde el servicio');
    return this.http
      .get<Configuracion[]>(this.urlEndPoint)
      .pipe(catchError(this.handleError));
  }

  saveConfiguracion(configuracion: Configuracion): Observable<Configuracion> {
    console.log('Creando configuración desde el servicio');
    return this.http
      .post<Configuracion>(this.urlEndPoint, configuracion, {
        headers: this.httpHeaders,
      })
      .pipe(catchError(this.handleError));
  }

  updateConfiguracion(configuracion: Configuracion): Observable<Configuracion> {
    console.log(
      'Actualizando la configuración desde el servicio',
      configuracion
    );
    return this.http
      .put<Configuracion>(
        `${this.urlEndPoint}/${configuracion.configClave}`,
        configuracion,
        { headers: this.httpHeaders }
      )
      .pipe(catchError(this.handleError));
  }

  deleteConfiguracion(configClave: string): Observable<void> {
    console.log('Eliminando la configuración desde el servicio');
    return this.http
      .delete<void>(`${this.urlEndPoint}/${configClave}`, {
        headers: this.httpHeaders,
      })
      .pipe(catchError(this.handleError));
  }

  getConfiguracionByClave(configClave: string): Observable<Configuracion> {
    console.log('Obteniendo la configuración con clave:', configClave);
    return this.http
      .get<Configuracion>(`${this.urlEndPoint}/${configClave}`)
      .pipe(catchError(this.handleError));
  }

  private handleError(error: HttpErrorResponse) {
    if (error.status === 400 || error.status === 404) {
      const codigoError = error.error.codigoError;
      const mensajeError = error.error.mensaje;
      const codigoHttp = error.error.codigoHttp;
      const url = error.error.url;
      const metodo = error.error.metodo;

      console.error(
        `Error ${codigoHttp} en ${metodo} ${url}: ${mensajeError} (Código: ${codigoError})`
      );

      Swal.fire({
        icon: 'error',
        title: '¡Error!',
        text: mensajeError,
        confirmButtonText: 'Cerrar',
      });

      return throwError(() => new Error(mensajeError));
    } else {
      return throwError(() => new Error('Ocurrió un error inesperado.'));
    }
  }
}
