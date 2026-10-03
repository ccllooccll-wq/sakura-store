import { Component, OnInit, OnDestroy, computed, inject, signal } from "@angular/core";
import { CommonModule } from "@angular/common";
import { FormsModule } from "@angular/forms";
import { HttpErrorResponse } from "@angular/common/http";
import { httpErrorMessage } from "../../../core/utils/http-error";
import { UserService } from "../../../core/services/user.service";
import { AuthService } from "../../../core/services/auth.service";
import { ConfirmationDialogComponent } from "../../../shared/components/confirmation-dialog/confirmation-dialog.component";
import {
  CreateUserRequest,
  Role,
  UpdateUserRequest,
  User,
} from "../../../core/models/user.model";

@Component({
  selector: "app-user-list",
  standalone: true,
  imports: [CommonModule, FormsModule, ConfirmationDialogComponent],
  templateUrl: "./user-list.component.html",
  styleUrls: ["./user-list.component.css"],
})
export class UserListComponent implements OnInit, OnDestroy {
  readonly users = signal<User[]>([]);
  readonly roles = signal<Role[]>([]);
  rolesLoading = false;
  readonly searchTerm = signal("");

  showModal = false;
  isEditMode = false;
  saving = false;
  selectedUserId: number | null = null;
  userToDelete: User | null = null;
  private successTimer?: ReturnType<typeof setTimeout>;

  successMessage = "";
  errorMessage = "";

  formData: CreateUserRequest = {
    username: "",
    fullName: "",
    email: "",
    password: "",
    roleId: 0,
  };

  private readonly userService = inject(UserService);
  readonly authService = inject(AuthService);

  ngOnInit(): void {
    this.loadUsers();
    this.loadRoles();
  }

  get isAdmin(): boolean {
    return this.authService.isAdmin();
  }

  loadUsers(): void {
    this.userService.getUsers().subscribe({
      next: (data) => this.users.set(data),
      error: (err: HttpErrorResponse) => this.showError(httpErrorMessage(err, "No se pudo cargar la lista de usuarios.")),
    });
  }

  loadRoles(): void {
    if (this.rolesLoading) return;
    this.rolesLoading = true;
    this.userService.getRoles().subscribe({
      next: (data) => {
        this.roles.set(data);
        this.rolesLoading = false;
        if (!data.length) this.showError("El servidor no devolvió roles. Comprueba la configuración del backend.");
      },
      error: (error: HttpErrorResponse) => {
        this.roles.set([]);
        this.rolesLoading = false;
        this.showError(httpErrorMessage(error, "No se pudieron cargar los roles. Revisa la consola del backend y pulsa Reintentar."));
      },
    });
  }

  readonly filteredUsers = computed(() => {
    const term = this.searchTerm().trim().toLowerCase();
    return this.users().filter(u => (u.username + " " + u.fullName + " " + u.email).toLowerCase().includes(term));
  });

  openCreateModal(): void {
    if (!this.isAdmin || this.saving) return;
    this.isEditMode = false;
    this.selectedUserId = null;
    this.formData = {
      username: "",
      fullName: "",
      email: "",
      password: "",
      roleId: this.roles().find(r => r.name === "ROLE_VENDEDOR")?.id ?? 0,
    };
    this.showModal = true;
  }

  openEditModal(user: User): void {
    if (!this.isAdmin || this.saving) return;
    this.isEditMode = true;
    this.selectedUserId = user.id;
    this.formData = {
      username: user.username,
      fullName: user.fullName,
      email: user.email,
      roleId: user.roleId,
    };
    this.showModal = true;
  }

  closeModal(): void {
    if (this.saving) return;
    this.showModal = false;
  }

  saveUser(formValid = true): void {
    if (!formValid || this.saving || !this.isAdmin) return;
    if (!this.roles().some(r => r.id === this.formData.roleId)) {
      this.errorMessage = "Selecciona un rol disponible antes de guardar.";
      return;
    }
    this.saving = true;
    this.errorMessage = "";

    if (this.isEditMode && this.selectedUserId) {
      const updateReq: UpdateUserRequest = {
        fullName: this.formData.fullName.trim(),
        email: this.formData.email.trim(),
        roleId: Number(this.formData.roleId),
      };
      this.userService.updateUser(this.selectedUserId, updateReq).subscribe({
        next: () => {
          this.saving = false;
          this.showSuccess("Usuario actualizado correctamente.");
          this.closeModal();
          this.loadUsers();
        },
        error: (err: HttpErrorResponse) => {
          this.saving = false;
          this.errorMessage = httpErrorMessage(err, "Error al actualizar usuario.");
        },
      });
    } else {
      const createReq: CreateUserRequest = {
        ...this.formData,
        username: this.formData.username.trim(),
        fullName: this.formData.fullName.trim(),
        email: this.formData.email.trim(),
        roleId: Number(this.formData.roleId),
      };
      this.userService.createUser(createReq).subscribe({
        next: () => {
          this.saving = false;
          this.showSuccess("Usuario registrado con éxito.");
          this.closeModal();
          this.loadUsers();
        },
        error: (err: HttpErrorResponse) => {
          this.saving = false;
          this.errorMessage = httpErrorMessage(err, "Error al crear el usuario.");
        },
      });
    }
  }

  toggleStatus(user: User): void {
    if (!this.isAdmin || this.saving) return;
    this.saving = true;
    const newStatus = !user.active;
    this.userService.toggleUserStatus(user.id, newStatus).subscribe({
      next: () => {
        this.saving = false;
        this.showSuccess(
          `Estado del usuario '${user.username}' cambiado a ${newStatus ? "ACTIVO" : "INACTIVO"}.`,
        );
        this.loadUsers();
      },
      error: (err: HttpErrorResponse) => {
        this.saving = false;
        this.showError(httpErrorMessage(err, "No se pudo cambiar el estado."));
      },
    });
  }

  deleteUser(user: User): void {
    if (!this.isAdmin || this.saving) return;
    this.userToDelete = user;
  }

  confirmDelete(): void {
    if (!this.userToDelete || !this.isAdmin || this.saving) return;
    const user = this.userToDelete;
    this.userToDelete = null;
    this.saving = true;
    this.userService.deleteUser(user.id).subscribe({
      next: (res) => {
        this.saving = false;
        this.showSuccess(res.mensaje || `Usuario '${user.username}' eliminado exitosamente.`);
        this.loadUsers();
      },
      error: (err: HttpErrorResponse) => {
        this.saving = false;
        this.showError(httpErrorMessage(err, "No se pudo eliminar el usuario."));
      },
    });
  }

  getRoleDisplayName(roleName: string): string {
    switch (roleName) {
      case "ROLE_ADMIN":
        return "ADMIN";
      case "ROLE_VENDEDOR":
        return "VENDEDOR";
      case "ROLE_PROVEEDOR":
        return "PROVEEDOR";
      default:
        return roleName;
    }
  }

  getRoleBadgeClass(roleName: string): string {
    switch (roleName) {
      case "ROLE_ADMIN":
        return "badge-admin";
      case "ROLE_VENDEDOR":
        return "badge-vendedor";
      case "ROLE_PROVEEDOR":
        return "badge-proveedor";
      default:
        return "";
    }
  }

  showSuccess(msg: string): void {
    this.successMessage = msg;
    clearTimeout(this.successTimer);
    this.successTimer = setTimeout(() => (this.successMessage = ""), 4000);
  }

  showError(msg: string): void {
    this.errorMessage = msg;
  }

  ngOnDestroy(): void {
    clearTimeout(this.successTimer);
  }
}
