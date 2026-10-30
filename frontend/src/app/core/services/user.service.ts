import { Injectable, inject } from "@angular/core";
import { HttpClient } from "@angular/common/http";
import { Observable, map } from "rxjs";
import {
  CreateUserRequest,
  Role,
  UpdateUserRequest,
  User,
} from "../models/user.model";
import { ApiResponse } from "../models/auth.model";

@Injectable({
  providedIn: "root",
})
export class UserService {
  private readonly apiUrl = "/api";
  private readonly http = inject(HttpClient);

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
    return this.http.put<User>(`${this.apiUrl}/users/${id}`, user);
  }

  toggleUserStatus(id: number, active: boolean): Observable<User> {
    return this.http.patch<User>(
      `${this.apiUrl}/users/${id}/status`,
      { active },
    );
  }

  deleteUser(id: number): Observable<ApiResponse> {
    return this.http.delete<ApiResponse>(
      `${this.apiUrl}/users/${id}`,
    );
  }

  getRoles(): Observable<Role[]> {
    return this.http.get<Role[]>(`${this.apiUrl}/roles`).pipe(
      map((roles) =>
        roles.filter(
          (role) =>
            role.name === "ROLE_ADMIN" ||
            role.name === "ROLE_VENDEDOR",
        ),
      ),
    );
  }
}