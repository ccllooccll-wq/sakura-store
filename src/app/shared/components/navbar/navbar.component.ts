import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, Router } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';
import { User } from '../../../core/models/user.model';

@Component({
  selector: 'app-navbar',
  standalone: true,
  imports: [CommonModule, RouterModule],
  template: `
    <header class="sakura-navbar">
      <div class="navbar-container">
        <div class="brand">
          <div class="logo-icon">🌸</div>
          <div class="brand-text">
            <span class="brand-title">SAKURA STORE</span>
            <span class="brand-subtitle">Inventario & Ventas</span>
          </div>
        </div>

        <nav class="nav-links" *ngIf="currentUser">
          <a routerLink="/users" routerLinkActive="active" class="nav-item">
            <i class="fa-solid fa-users"></i>
            <span>Usuarios</span>
          </a>
          <a routerLink="/inventory" routerLinkActive="active" class="nav-item disabled">
            <i class="fa-solid fa-boxes-stacked"></i>
            <span>Inventario (Sprint 2)</span>
          </a>
          <a routerLink="/sales" routerLinkActive="active" class="nav-item disabled">
            <i class="fa-solid fa-receipt"></i>
            <span>Ventas (Sprint 3)</span>
          </a>
        </nav>

        <div class="user-profile" *ngIf="currentUser">
          <div class="user-info">
            <span class="user-name">{{ currentUser.fullName }}</span>
            <span class="role-badge" [ngClass]="getRoleClass(currentUser.roleName)">
              {{ getRoleDisplayName(currentUser.roleName) }}
            </span>
          </div>
          <button class="btn-logout" (click)="logout()" title="Cerrar sesión">
            <i class="fa-solid fa-right-from-bracket"></i>
          </button>
        </div>
      </div>
    </header>
  `,
  styles: [`
    .sakura-navbar {
      background: rgba(24, 22, 35, 0.85);
      backdrop-filter: blur(16px);
      border-bottom: 1px solid rgba(255, 121, 198, 0.15);
      position: sticky;
      top: 0;
      z-index: 1000;
      padding: 12px 24px;
    }

    .navbar-container {
      max-width: 1280px;
      margin: 0 auto;
      display: flex;
      align-items: center;
      justify-content: space-between;
    }

    .brand {
      display: flex;
      align-items: center;
      gap: 12px;
    }

    .logo-icon {
      font-size: 1.8rem;
      filter: drop-shadow(0 0 10px rgba(255, 117, 160, 0.6));
      animation: pulse 3s infinite ease-in-out;
    }

    .brand-text {
      display: flex;
      flex-direction: column;
    }

    .brand-title {
      font-family: var(--font-heading);
      font-size: 1.25rem;
      font-weight: 800;
      background: linear-gradient(135deg, #ff75a0, #ffb86c);
      -webkit-background-clip: text;
      -webkit-text-fill-color: transparent;
      letter-spacing: 0.05em;
    }

    .brand-subtitle {
      font-size: 0.7rem;
      color: var(--text-muted);
      text-transform: uppercase;
      letter-spacing: 0.1em;
    }

    .nav-links {
      display: flex;
      gap: 20px;
    }

    .nav-item {
      color: var(--text-secondary);
      text-decoration: none;
      font-size: 0.9rem;
      font-weight: 600;
      display: flex;
      align-items: center;
      gap: 8px;
      padding: 8px 14px;
      border-radius: 8px;
      transition: all 0.25s ease;
    }

    .nav-item:hover:not(.disabled) {
      color: var(--sakura-pink);
      background: rgba(255, 117, 160, 0.1);
    }

    .nav-item.active {
      color: #fff;
      background: var(--sakura-gradient);
      box-shadow: 0 4px 14px rgba(255, 117, 160, 0.3);
    }

    .nav-item.disabled {
      opacity: 0.4;
      cursor: not-allowed;
    }

    .user-profile {
      display: flex;
      align-items: center;
      gap: 16px;
    }

    .user-info {
      display: flex;
      flex-direction: column;
      align-items: flex-end;
    }

    .user-name {
      font-size: 0.9rem;
      font-weight: 700;
      color: var(--text-primary);
    }

    .role-badge {
      font-size: 0.7rem;
      padding: 2px 8px;
      border-radius: 12px;
      font-weight: 700;
    }

    .role-admin { background: rgba(189, 147, 249, 0.2); color: #bd93f9; border: 1px solid rgba(189, 147, 249, 0.4); }
    .role-vendedor { background: rgba(139, 233, 253, 0.2); color: #8be9fd; border: 1px solid rgba(139, 233, 253, 0.4); }
    .role-almacenero { background: rgba(255, 184, 108, 0.2); color: #ffb86c; border: 1px solid rgba(255, 184, 108, 0.4); }

    .btn-logout {
      background: rgba(255, 85, 85, 0.15);
      color: #ff5555;
      border: 1px solid rgba(255, 85, 85, 0.3);
      padding: 8px 12px;
      border-radius: 8px;
      cursor: pointer;
      transition: all 0.2s ease;
    }

    .btn-logout:hover {
      background: rgba(255, 85, 85, 0.3);
      transform: scale(1.05);
    }

    @keyframes pulse {
      0%, 100% { transform: scale(1); }
      50% { transform: scale(1.1); }
    }
  `]
})
export class NavbarComponent {
  currentUser: User | null = null;

  constructor(private authService: AuthService, private router: Router) {
    this.authService.currentUser$.subscribe(user => this.currentUser = user);
  }

  logout() {
    this.authService.logout();
    this.router.navigate(['/login']);
  }

  getRoleDisplayName(roleName: string): string {
    switch (roleName) {
      case 'ROLE_ADMIN': return 'ADMINISTRADOR';
      case 'ROLE_VENDEDOR': return 'VENDEDOR';
      case 'ROLE_ALMACENERO': return 'ALMACENERO';
      default: return roleName;
    }
  }

  getRoleClass(roleName: string): string {
    switch (roleName) {
      case 'ROLE_ADMIN': return 'role-admin';
      case 'ROLE_VENDEDOR': return 'role-vendedor';
      case 'ROLE_ALMACENERO': return 'role-almacenero';
      default: return '';
    }
  }
}
