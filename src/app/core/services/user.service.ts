import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';
import { CreateUserRequest, Role, UpdateUserRequest, User } from '../models/user.model';
import { ApiResponse, AuthService } from './auth.service';

@Injectable({
  providedIn: 'root'
})
export class UserService {
  private apiUrl = 'http://localhost:8080/api';

  constructor(private http: HttpClient, private authService: AuthService) {}

  getUsers(): Observable<User[]> {
    return this.http.get<User[]>(`${this.apiUrl}/users`);
  }

  getUserById(id: number): Observable<User> {
    return this.http.get<User>(`${this.apiUrl}/users/${id}`);
  }

  createUser(user: CreateUserRequest): Observable<User> {
    return this.http.post<User>(`${this.apiUrl}/users`, user);
  }

  updateUser(id: number, user: UpdateUserRequest): Observable<User> {
    const storedUser = this.authService.getStoredUser();
    const role = storedUser ? storedUser.roleName : '';
    const headers = new HttpHeaders({ 'X-User-Role': role });
    return this.http.put<User>(`${this.apiUrl}/users/${id}`, user, { headers });
  }

  toggleUserStatus(id: number, active: boolean): Observable<User> {
    return this.http.patch<User>(`${this.apiUrl}/users/${id}/status`, { active });
  }

  deleteUser(id: number): Observable<ApiResponse> {
    return this.http.delete<ApiResponse>(`${this.apiUrl}/users/${id}`);
  }

  getRoles(): Observable<Role[]> {
    return this.http.get<Role[]>(`${this.apiUrl}/roles`);
  }
}
