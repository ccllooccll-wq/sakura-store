export interface Role {
  id: number;
  name: 'ROLE_ADMIN' | 'ROLE_VENDEDOR' | 'ROLE_ALMACENERO';
  description?: string;
}

export interface User {
  id: number;
  username: string;
  fullName: string;
  email: string;
  roleId: number;
  roleName: string;
  active: boolean;
  createdAt?: string;
  updatedAt?: string;
}

export interface CreateUserRequest {
  username: string;
  fullName: string;
  email: string;
  password?: string;
  roleId: number;
}

export interface UpdateUserRequest {
  fullName: string;
  email: string;
  roleId: number;
}

export interface AuthResponse {
  token?: string;
  tokenType?: string;
  user?: User;
  requiresOtp?: boolean;
  email?: string;
  mensaje?: string;
}
