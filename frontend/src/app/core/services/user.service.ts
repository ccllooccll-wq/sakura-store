import { Injectable, inject } from "@angular/core";
import { HttpClient, HttpParams } from "@angular/common/http";
import { Observable, map } from "rxjs";
import {
  CreateUserRequest,
  Role,
  UpdateUserRequest,
  User,
  UserFilters,
} from "../models/user.model";
import { Page } from "../models/inventory.model";
import { ApiResponse } from "../models/auth.model";

@Injectable({
  providedIn: "root",
})
export class UserService {
  private readonly apiUrl = "/api";
  private readonly http = inject(HttpClient);

  getUsers(f: UserFilters, page = 0, size = 10): Observable<Page<User>> {
    let params = new HttpParams().set("page", page).set("size", size);
    if (f.username.trim()) params = params.set("username", f.username.trim());
    if (f.fullName.trim()) params = params.set("fullName", f.fullName.trim());
    if (f.email.trim()) params = params.set("email", f.email.trim());
    if (f.roleId > 0) params = params.set("roleId", f.roleId);
    if (f.status) params = params.set("status", f.status);
    return this.http.get<Page<User>>(`${this.apiUrl}/users`, { params });
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
            role.name === "ROLE_VENDEDOR" ||
            role.name === "ROLE_CLIENTE",
        ),
      ),
    );
  }
}