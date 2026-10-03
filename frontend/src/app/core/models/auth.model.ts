export interface RegisterRequest {
  nombre: string;
  username: string;
  email: string;
  password: string;
}

export interface ApiResponse {
  mensaje: string;
}

export interface CsrfToken {
  token: string;
  headerName: string;
}
