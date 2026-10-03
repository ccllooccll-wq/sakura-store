import { HttpClient, HttpErrorResponse, provideHttpClient, withInterceptors } from "@angular/common/http";
import { HttpTestingController, provideHttpClientTesting } from "@angular/common/http/testing";
import { TestBed } from "@angular/core/testing";
import { Router } from "@angular/router";
import { describe, it, expect, beforeEach, afterEach, vi } from "vitest";
import { AuthService } from "../services/auth.service";
import { CsrfTokenService } from "../services/csrf-token.service";
import { authErrorInterceptor } from "./auth-error.interceptor";
import { csrfInterceptor } from "./csrf.interceptor";

describe("Sesión y CSRF", () => {
  let http: HttpClient;
  let backend: HttpTestingController;
  let csrf: CsrfTokenService;
  let auth: AuthService;
  let router: { url: string; navigate: ReturnType<typeof vi.fn> };
  const user = { id: 1, username: "demo", fullName: "Demo", email: "demo@example.com", roleId: 3, roleName: "ROLE_ADMIN", active: true };

  beforeEach(() => {
    router = { url: "/inventario", navigate: vi.fn().mockResolvedValue(true) };
    TestBed.configureTestingModule({ providers: [
      provideHttpClient(withInterceptors([authErrorInterceptor, csrfInterceptor])),
      provideHttpClientTesting(), { provide: Router, useValue: router },
    ] });
    http = TestBed.inject(HttpClient);
    backend = TestBed.inject(HttpTestingController);
    csrf = TestBed.inject(CsrfTokenService);
    auth = TestBed.inject(AuthService);
  });
  afterEach(() => backend.verify());
  function token(value = "test-token") {
    backend.expectOne("/api/auth/csrf").flush({ token: value, headerName: "X-CSRF-TOKEN" });
  }
  function signedIn() {
    auth.loadSession().subscribe();
    backend.expectOne("/api/auth/me").flush(user);
  }

  it("comparte la petición CSRF entre escrituras simultáneas y reutiliza el token", () => {
    http.post("/api/inventory/products", {}).subscribe();
    http.post("/api/inventory/categories", {}).subscribe();
    token();
    for (const url of ["/api/inventory/products", "/api/inventory/categories"]) {
      const request = backend.expectOne(url);
      expect(request.request.headers.get("X-CSRF-TOKEN")).toBe("test-token");
      request.flush({});
    }
    http.patch("/api/users/1/status", { active: true }).subscribe();
    backend.expectNone("/api/auth/csrf");
    const request = backend.expectOne("/api/users/1/status");
    expect(request.request.headers.get("X-CSRF-TOKEN")).toBe("test-token");
    request.flush({});
  });

  it.each(["/api/auth/login", "/api/usuarios/registro", "/api/auth/verificar-email", "/api/auth/logout", "/api/auth/restablecer-password"])("renueva CSRF después de %s", (url) => {
    http.post(url, {}).subscribe();
    token("before");
    backend.expectOne(url).flush({});
    http.post("/api/inventory/products", {}).subscribe();
    token("after");
    const request = backend.expectOne("/api/inventory/products");
    expect(request.request.headers.get("X-CSRF-TOKEN")).toBe("after");
    request.flush({});
  });

  it("no añade CSRF a lecturas ni a servicios externos", () => {
    for (const url of ["/api/inventory/products", "https://example.com/public"]) {
      http.get(url).subscribe();
      const request = backend.expectOne(url);
      expect(request.request.headers.has("X-CSRF-TOKEN")).toBe(false);
      request.flush([]);
    }
    http.post("https://example.com/form", {}).subscribe();
    const external = backend.expectOne("https://example.com/form");
    expect(external.request.headers.has("X-CSRF-TOKEN")).toBe(false);
    external.flush({});
    backend.expectNone("/api/auth/csrf");
  });

  it("un 403 elimina el token, informa el error y no repite la escritura", () => {
    const error = vi.fn();
    http.post("/api/inventory/products", {}).subscribe({ error });
    token("expired");
    backend.expectOne("/api/inventory/products").flush({}, { status: 403, statusText: "Forbidden" });
    expect(error).toHaveBeenCalledOnce();
    backend.expectNone("/api/inventory/products");
    expect(router.navigate).not.toHaveBeenCalled();
    http.post("/api/inventory/products", {}).subscribe();
    token("renewed");
    backend.expectOne("/api/inventory/products").flush({});
  });

  it("permite intentar obtener CSRF después de un fallo del servidor", () => {
    http.post("/api/inventory/products", {}).subscribe({ error: () => {} });
    backend.expectOne("/api/auth/csrf").flush({}, { status: 500, statusText: "Server Error" });
    backend.expectNone("/api/inventory/products");
    http.post("/api/inventory/products", {}).subscribe();
    token();
    backend.expectOne("/api/inventory/products").flush({});
  });

  it("una respuesta anterior a la rotación no sobrescribe el token nuevo", () => {
    csrf.getToken().subscribe();
    const old = backend.expectOne("/api/auth/csrf");
    csrf.invalidate();
    csrf.getToken().subscribe();
    token("new-session");
    old.flush({ token: "old-session", headerName: "X-CSRF-TOKEN" });
    let cached = "";
    csrf.getToken().subscribe(value => cached = value.token);
    expect(cached).toBe("new-session");
    backend.expectNone("/api/auth/csrf");
  });

  it("limpia una sesión vencida y dirige al login ante un 401 protegido", () => {
    signedIn();
    http.get("/api/inventory/products").subscribe({ error: () => {} });
    backend.expectOne("/api/inventory/products").flush({}, { status: 401, statusText: "Unauthorized" });
    expect(auth.getStoredUser()).toBeNull();
    expect(router.navigate).toHaveBeenCalledWith(["/login"], { queryParams: { expired: "1" }, replaceUrl: true });
  });

  it.each(["/api/auth/login", "/api/auth/verificar-email", "/api/auth/restablecer-password"])("un código o credencial incorrectos en %s no fuerzan otra navegación", (url) => {
    http.post(url, {}).subscribe({ error: () => {} });
    token();
    backend.expectOne(url).flush({}, { status: 401, statusText: "Unauthorized" });
    expect(router.navigate).not.toHaveBeenCalled();
  });

  it("una comprobación anónima de sesión no produce un bucle de redirección", () => {
    let current = user as typeof user | null;
    auth.loadSession().subscribe(value => current = value);
    backend.expectOne("/api/auth/me").flush({}, { status: 401, statusText: "Unauthorized" });
    expect(current).toBeNull();
    expect(router.navigate).not.toHaveBeenCalled();
  });

  it.each([403, 500])("un error %s protegido conserva la sesión y no se confunde con expiración", (status) => {
    signedIn();
    http.get("/api/users").subscribe({ error: () => {} });
    backend.expectOne("/api/users").flush({}, { status, statusText: "Error" });
    expect(auth.getStoredUser()).toEqual(user);
    expect(router.navigate).not.toHaveBeenCalled();
  });

  it("un 401 de un servicio externo no cierra la sesión de Sakura", () => {
    signedIn();
    http.get("https://example.com/public").subscribe({ error: (_error: HttpErrorResponse) => {} });
    backend.expectOne("https://example.com/public").flush({}, { status: 401, statusText: "Unauthorized" });
    expect(auth.getStoredUser()).toEqual(user);
    expect(router.navigate).not.toHaveBeenCalled();
  });
});
