import { HttpErrorResponse } from "@angular/common/http";

export function httpErrorMessage(error: HttpErrorResponse, fallback: string, unauthorized = "La sesión venció. Inicia sesión nuevamente."): string {
  if (error.status === 0) return "No se pudo conectar con el servidor. Comprueba tu conexión.";
  if (error.status === 401) return unauthorized;
  if (error.status === 403) return "No tienes permiso o el formulario venció. Recarga la página.";
  const body: unknown = error.error;
  if (typeof body === "object" && body !== null && "message" in body &&
      typeof body.message === "string") return body.message;
  return fallback;
}
