import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { UserService } from '../../../core/services/user.service';
import { AuthService } from '../../../core/services/auth.service';
import { CreateUserRequest, Role, UpdateUserRequest, User } from '../../../core/models/user.model';
import { NavbarComponent } from '../../../shared/components/navbar/navbar.component';

@Component({
  selector: 'app-user-list',
  standalone: true,
  imports: [CommonModule, FormsModule, NavbarComponent],
  template: `
    <app-navbar></app-navbar>

    <main class="container">
      <!-- Page Header -->
      <div class="page-header">
        <div>
          <h1>Gestión de Usuarios</h1>
          <p class="subtitle">Administra los usuarios, asignación de roles y accesos del sistema Sakura Store</p>
        </div>
        <button class="btn-sakura" (click)="openCreateModal()">
          <i class="fa-solid fa-user-plus"></i>
          <span>Nuevo Usuario</span>
        </button>
      </div>

      <!-- Feedback Alerts -->
      <div *ngIf="successMessage" class="alert alert-success">
        <i class="fa-solid fa-circle-check"></i>
        <span>{{ successMessage }}</span>
      </div>
      <div *ngIf="errorMessage" class="alert alert-error">
        <i class="fa-solid fa-circle-exclamation"></i>
        <span>{{ errorMessage }}</span>
      </div>

      <!-- Filters & Stats Card -->
      <div class="glass-card filter-card">
        <div class="search-box">
          <i class="fa-solid fa-magnifying-glass search-icon"></i>
          <input
            type="text"
            class="form-control"
            placeholder="Buscar por usuario, nombre o correo..."
            [(ngModel)]="searchTerm"
          />
        </div>
        <div class="user-count">
          <span>Total: <strong>{{ filteredUsers.length }}</strong> usuarios</span>
        </div>
      </div>

      <!-- Users Table Card -->
      <div class="glass-card table-card">
        <table class="sakura-table">
          <thead>
            <tr>
              <th>ID</th>
              <th>Usuario</th>
              <th>Nombre Completo</th>
              <th>Correo Electrónico</th>
              <th>Rol</th>
              <th>Estado</th>
              <th class="text-right">Acciones</th>
            </tr>
          </thead>
          <tbody>
            <tr *ngFor="let user of filteredUsers">
              <td class="id-cell">#{{ user.id }}</td>
              <td class="username-cell">
                <i class="fa-solid fa-user-circle user-avatar"></i>
                <strong>{{ user.username }}</strong>
              </td>
              <td>{{ user.fullName }}</td>
              <td class="email-cell">{{ user.email }}</td>
              <td>
                <span class="badge" [ngClass]="getRoleBadgeClass(user.roleName)">
                  {{ getRoleDisplayName(user.roleName) }}
                </span>
              </td>
              <td>
                <span class="badge" [ngClass]="user.active ? 'badge-active' : 'badge-inactive'">
                  {{ user.active ? 'ACTIVO' : 'INACTIVO' }}
                </span>
              </td>
              <td class="actions-cell text-right">
                <button class="btn-action btn-edit" (click)="openEditModal(user)" title="Editar usuario">
                  <i class="fa-solid fa-pen"></i>
                </button>
                <button
                  class="btn-action"
                  [ngClass]="user.active ? 'btn-deactivate' : 'btn-activate'"
                  (click)="toggleStatus(user)"
                  [title]="user.active ? 'Desactivar usuario' : 'Activar usuario'"
                >
                  <i [class]="user.active ? 'fa-solid fa-user-slash' : 'fa-solid fa-user-check'"></i>
                </button>
                <button class="btn-action btn-delete" (click)="deleteUser(user)" title="Eliminar usuario">
                  <i class="fa-solid fa-trash"></i>
                </button>
              </td>
            </tr>
            <tr *ngIf="filteredUsers.length === 0">
              <td colspan="7" class="empty-state">
                <i class="fa-solid fa-folder-open empty-icon"></i>
                <p>No se encontraron usuarios registrados.</p>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </main>

    <!-- Modal Form for Create / Edit -->
    <div class="modal-backdrop" *ngIf="showModal">
      <div class="glass-card modal-content">
        <div class="modal-header">
          <h3>{{ isEditMode ? 'Editar Usuario' : 'Nuevo Usuario Sakura Store' }}</h3>
          <button class="btn-close" (click)="closeModal()">&times;</button>
        </div>

        <form (ngSubmit)="saveUser()" #userForm="ngForm">
          <div class="modal-body">
            <!-- Username (Only editable when creating) -->
            <div class="form-group" *ngIf="!isEditMode">
              <label>Nombre de Usuario *</label>
              <input
                type="text"
                class="form-control"
                [(ngModel)]="formData.username"
                name="username"
                required
                placeholder="Ej. mcalle"
              />
            </div>

            <div class="form-group">
              <label>Nombre Completo *</label>
              <input
                type="text"
                class="form-control"
                [(ngModel)]="formData.fullName"
                name="fullName"
                required
                placeholder="Ej. Marcelo Calle"
              />
            </div>

            <div class="form-group">
              <label>Correo Electrónico *</label>
              <input
                type="email"
                class="form-control"
                [(ngModel)]="formData.email"
                name="email"
                required
                placeholder="ejemplo@sakurastore.com"
              />
            </div>

            <!-- Password (Only required on creation) -->
            <div class="form-group" *ngIf="!isEditMode">
              <label>Contraseña *</label>
              <input
                type="password"
                class="form-control"
                [(ngModel)]="formData.password"
                name="password"
                required
                placeholder="••••••••"
              />
            </div>

            <div class="form-group">
              <label>Rol de Usuario *</label>
              <select
                class="form-control"
                [(ngModel)]="formData.roleId"
                name="roleId"
                [disabled]="isEditMode && !isAdmin"
                required
              >
                <option [ngValue]="null" disabled>-- Selecciona un Rol --</option>
                <option *ngFor="let role of roles" [value]="role.id">
                  {{ getRoleDisplayName(role.name) }} ({{ role.description }})
                </option>
              </select>
              <small *ngIf="isEditMode && !isAdmin" class="role-warning">
                <i class="fa-solid fa-lock"></i> Solo un Administrador puede cambiar el rol.
              </small>
            </div>
          </div>

          <div class="modal-footer">
            <button type="button" class="btn-secondary" (click)="closeModal()">Cancelar</button>
            <button type="submit" class="btn-sakura" [disabled]="!userForm.valid || saving">
              <span *ngIf="!saving">{{ isEditMode ? 'Guardar Cambios' : 'Crear Usuario' }}</span>
              <span *ngIf="saving"><i class="fa-solid fa-spinner fa-spin"></i> Guardando...</span>
            </button>
          </div>
        </form>
      </div>
    </div>
  `,
  styles: [`
    .container {
      max-width: 1280px;
      margin: 30px auto;
      padding: 0 24px;
    }

    .page-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 24px;
    }

    .page-header h1 {
      font-size: 1.8rem;
    }

    .subtitle {
      color: var(--text-secondary);
      font-size: 0.9rem;
    }

    .alert {
      padding: 12px 18px;
      border-radius: var(--radius-sm);
      margin-bottom: 20px;
      display: flex;
      align-items: center;
      gap: 12px;
      font-size: 0.9rem;
    }

    .alert-success { background: rgba(80, 250, 123, 0.15); border: 1px solid rgba(80, 250, 123, 0.3); color: #50fa7b; }
    .alert-error { background: rgba(255, 85, 85, 0.15); border: 1px solid rgba(255, 85, 85, 0.3); color: #ff5555; }

    .filter-card {
      padding: 16px 24px;
      margin-bottom: 20px;
      display: flex;
      justify-content: space-between;
      align-items: center;
    }

    .search-box {
      position: relative;
      width: 360px;
    }

    .search-icon {
      position: absolute;
      left: 14px;
      top: 50%;
      transform: translateY(-50%);
      color: var(--text-muted);
    }

    .search-box .form-control {
      padding-left: 40px;
    }

    .user-count {
      color: var(--text-secondary);
      font-size: 0.9rem;
    }

    .table-card {
      padding: 8px;
      overflow-x: auto;
    }

    .sakura-table {
      width: 100%;
      border-collapse: collapse;
      text-align: left;
    }

    .sakura-table th {
      padding: 14px 16px;
      font-family: var(--font-heading);
      font-size: 0.8rem;
      text-transform: uppercase;
      letter-spacing: 0.05em;
      color: var(--text-muted);
      border-bottom: 1px solid var(--border-color);
    }

    .sakura-table td {
      padding: 16px;
      border-bottom: 1px solid rgba(255, 255, 255, 0.05);
      font-size: 0.9rem;
    }

    .username-cell {
      display: flex;
      align-items: center;
      gap: 10px;
    }

    .user-avatar {
      font-size: 1.4rem;
      color: var(--sakura-pink);
    }

    .id-cell {
      color: var(--text-muted);
      font-weight: 600;
    }

    .email-cell {
      color: var(--text-secondary);
    }

    .text-right {
      text-align: right;
    }

    .btn-action {
      border: none;
      width: 34px;
      height: 34px;
      border-radius: 8px;
      cursor: pointer;
      margin-left: 6px;
      display: inline-flex;
      align-items: center;
      justify-content: center;
      transition: all 0.2s ease;
    }

    .btn-edit { background: rgba(139, 233, 253, 0.15); color: #8be9fd; }
    .btn-edit:hover { background: rgba(139, 233, 253, 0.3); }

    .btn-deactivate { background: rgba(255, 184, 108, 0.15); color: #ffb86c; }
    .btn-deactivate:hover { background: rgba(255, 184, 108, 0.3); }

    .btn-activate { background: rgba(80, 250, 123, 0.15); color: #50fa7b; }
    .btn-activate:hover { background: rgba(80, 250, 123, 0.3); }

    .btn-delete { background: rgba(255, 85, 85, 0.2); color: #ff5555; }
    .btn-delete:hover { background: rgba(255, 85, 85, 0.4); }

    .empty-state {
      text-align: center;
      padding: 40px !important;
      color: var(--text-muted);
    }

    .empty-icon {
      font-size: 2.5rem;
      margin-bottom: 10px;
    }

    .role-warning {
      display: block;
      margin-top: 6px;
      font-size: 0.8rem;
      color: #ffb86c;
    }

    /* Modal Backdrop */
    .modal-backdrop {
      position: fixed;
      inset: 0;
      background: rgba(0, 0, 0, 0.7);
      backdrop-filter: blur(8px);
      z-index: 2000;
      display: flex;
      align-items: center;
      justify-content: center;
      padding: 20px;
    }

    .modal-content {
      width: 100%;
      max-width: 500px;
      padding: 28px;
    }

    .modal-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 20px;
    }

    .btn-close {
      background: none;
      border: none;
      color: var(--text-muted);
      font-size: 1.5rem;
      cursor: pointer;
    }

    .modal-footer {
      display: flex;
      justify-content: flex-end;
      gap: 12px;
      margin-top: 24px;
    }
  `]
})
export class UserListComponent implements OnInit {
  users: User[] = [];
  roles: Role[] = [];
  searchTerm = '';

  showModal = false;
  isEditMode = false;
  saving = false;
  selectedUserId: number | null = null;

  successMessage = '';
  errorMessage = '';

  formData: CreateUserRequest = {
    username: '',
    fullName: '',
    email: '',
    password: '',
    roleId: 1
  };

  constructor(private userService: UserService, public authService: AuthService) {}

  ngOnInit(): void {
    this.loadUsers();
    this.loadRoles();
  }

  get isAdmin(): boolean {
    return this.authService.isAdmin();
  }

  loadUsers(): void {
    this.userService.getUsers().subscribe({
      next: (data) => this.users = data,
      error: (err) => this.showError('No se pudo conectar con el servidor backend.')
    });
  }

  loadRoles(): void {
    this.userService.getRoles().subscribe({
      next: (data) => this.roles = data
    });
  }

  get filteredUsers(): User[] {
    if (!this.searchTerm.trim()) return this.users;
    const term = this.searchTerm.toLowerCase();
    return this.users.filter(u =>
      u.username.toLowerCase().includes(term) ||
      u.fullName.toLowerCase().includes(term) ||
      u.email.toLowerCase().includes(term)
    );
  }

  openCreateModal(): void {
    this.isEditMode = false;
    this.selectedUserId = null;
    this.formData = { username: '', fullName: '', email: '', password: '', roleId: this.roles[0]?.id || 1 };
    this.showModal = true;
  }

  openEditModal(user: User): void {
    this.isEditMode = true;
    this.selectedUserId = user.id;
    this.formData = {
      username: user.username,
      fullName: user.fullName,
      email: user.email,
      roleId: user.roleId
    };
    this.showModal = true;
  }

  closeModal(): void {
    this.showModal = false;
  }

  saveUser(): void {
    this.saving = true;
    this.errorMessage = '';

    if (this.isEditMode && this.selectedUserId) {
      const updateReq: UpdateUserRequest = {
        fullName: this.formData.fullName,
        email: this.formData.email,
        roleId: Number(this.formData.roleId)
      };
      this.userService.updateUser(this.selectedUserId, updateReq).subscribe({
        next: () => {
          this.saving = false;
          this.showSuccess('Usuario actualizado correctamente.');
          this.closeModal();
          this.loadUsers();
        },
        error: (err) => {
          this.saving = false;
          this.errorMessage = err.error?.message || 'Error al actualizar usuario.';
        }
      });
    } else {
      const createReq: CreateUserRequest = {
        ...this.formData,
        roleId: Number(this.formData.roleId)
      };
      this.userService.createUser(createReq).subscribe({
        next: () => {
          this.saving = false;
          this.showSuccess('Usuario registrado con éxito.');
          this.closeModal();
          this.loadUsers();
        },
        error: (err) => {
          this.saving = false;
          this.errorMessage = err.error?.message || 'Error al crear el usuario.';
        }
      });
    }
  }

  toggleStatus(user: User): void {
    const newStatus = !user.active;
    this.userService.toggleUserStatus(user.id, newStatus).subscribe({
      next: () => {
        this.showSuccess(`Estado del usuario '${user.username}' cambiado a ${newStatus ? 'ACTIVO' : 'INACTIVO'}.`);
        this.loadUsers();
      },
      error: (err) => this.showError(err.error?.message || 'No se pudo cambiar el estado.')
    });
  }

  deleteUser(user: User): void {
    if (confirm(`¿Estás seguro de que deseas eliminar al usuario '${user.username}'? Esta acción no se puede deshacer.`)) {
      this.userService.deleteUser(user.id).subscribe({
        next: (res) => {
          this.showSuccess(res.mensaje || `Usuario '${user.username}' eliminado exitosamente.`);
          this.loadUsers();
        },
        error: (err) => this.showError(err.error?.message || 'No se pudo eliminar el usuario.')
      });
    }
  }

  getRoleDisplayName(roleName: string): string {
    switch (roleName) {
      case 'ROLE_ADMIN': return 'ADMIN';
      case 'ROLE_VENDEDOR': return 'VENDEDOR';
      case 'ROLE_ALMACENERO': return 'ALMACENERO';
      default: return roleName;
    }
  }

  getRoleBadgeClass(roleName: string): string {
    switch (roleName) {
      case 'ROLE_ADMIN': return 'badge-admin';
      case 'ROLE_VENDEDOR': return 'badge-vendedor';
      case 'ROLE_ALMACENERO': return 'badge-almacenero';
      default: return '';
    }
  }

  showSuccess(msg: string): void {
    this.successMessage = msg;
    setTimeout(() => this.successMessage = '', 4000);
  }

  showError(msg: string): void {
    this.errorMessage = msg;
    setTimeout(() => this.errorMessage = '', 5000);
  }
}
