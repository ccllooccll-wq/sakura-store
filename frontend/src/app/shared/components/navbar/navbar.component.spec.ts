import { TestBed } from "@angular/core/testing";
import { provideRouter } from "@angular/router";
import { BehaviorSubject, Observable } from "rxjs";
import { beforeEach, describe, expect, it } from "vitest";
import { AuthService } from "../../../core/services/auth.service";
import { User } from "../../../core/models/user.model";
import { NavbarComponent } from "./navbar.component";

describe("Navbar", () => {
  let activeSubscriptions: number;
  let users: BehaviorSubject<User | null>;
  beforeEach(() => {
    activeSubscriptions = 0;
    users = new BehaviorSubject<User | null>(null);
    const currentUser$ = new Observable<User | null>(subscriber => {
      activeSubscriptions++;
      const subscription = users.subscribe(subscriber);
      return () => { subscription.unsubscribe(); activeSubscriptions--; };
    });
    TestBed.configureTestingModule({ imports: [NavbarComponent], providers: [provideRouter([]), {
      provide: AuthService, useValue: { currentUser$, isAdmin: () => users.value?.roleName === "ROLE_ADMIN" },
    }] });
  });

  it("libera la suscripción cuando se destruye el menú", () => {
    const first = TestBed.createComponent(NavbarComponent);
    const second = TestBed.createComponent(NavbarComponent);
    expect(activeSubscriptions).toBe(2);
    first.destroy();
    expect(activeSubscriptions).toBe(1);
    second.destroy();
    expect(activeSubscriptions).toBe(0);
  });

  it("actualiza los datos de usuario y oculta Usuarios a un proveedor", () => {
    const fixture = TestBed.createComponent(NavbarComponent);
    users.next({ id: 1, username: "proveedor", fullName: "Proveedor Demo", email: "demo@example.com", roleId: 3, roleName: "ROLE_PROVEEDOR", active: true });
    fixture.detectChanges();
    const element: HTMLElement = fixture.nativeElement;
    expect(element.textContent).toContain("Proveedor Demo");
    expect(element.textContent).toContain("PROVEEDOR");
    expect(element.querySelector('a[href="/usuarios"]')).toBeNull();
    users.next(null);
    fixture.detectChanges();
    expect(element.querySelector(".user-profile")).toBeNull();
  });
});
