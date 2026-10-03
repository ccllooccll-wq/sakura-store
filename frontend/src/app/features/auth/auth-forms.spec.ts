import { HttpErrorResponse } from "@angular/common/http";
import { TestBed } from "@angular/core/testing";
import { ActivatedRoute, Router, convertToParamMap, provideRouter } from "@angular/router";
import { Subject, of, throwError } from "rxjs";
import { beforeEach, describe, expect, it, vi } from "vitest";
import type { MockInstance } from "vitest";
import { AuthService } from "../../core/services/auth.service";
import { ApiResponse } from "../../core/models/auth.model";
import { RegisterComponent } from "./register/register.component";
import { ResetPasswordComponent } from "./reset-password/reset-password.component";
import { LoginComponent } from "./login/login.component";
import { VerifyEmailComponent } from "./verify-email/verify-email.component";

describe("Formularios de acceso con OTP", () => {
  const password = "TestPassword123";
  let auth: {
    register: ReturnType<typeof vi.fn>; login: ReturnType<typeof vi.fn>;
    verifyEmail: ReturnType<typeof vi.fn>; resendCode: ReturnType<typeof vi.fn>;
    requestPasswordReset: ReturnType<typeof vi.fn>; resetPassword: ReturnType<typeof vi.fn>;
    isLoggedIn: ReturnType<typeof vi.fn>;
  };
  let navigate: MockInstance<Router["navigate"]>;
  beforeEach(() => {
    auth = {
      register: vi.fn().mockReturnValue(of({ mensaje: "Código enviado" })),
      login: vi.fn().mockReturnValue(of({ requiresOtp: true, email: "demo@example.com" })),
      verifyEmail: vi.fn().mockReturnValue(of({ mensaje: "Verificado" })),
      resendCode: vi.fn().mockReturnValue(of({ mensaje: "Enviado" })),
      requestPasswordReset: vi.fn().mockReturnValue(of({ mensaje: "Si existe la cuenta, recibirás un código" })),
      resetPassword: vi.fn().mockReturnValue(of({ mensaje: "Contraseña actualizada" })),
      isLoggedIn: vi.fn().mockReturnValue(true),
    };
    TestBed.configureTestingModule({
      imports: [RegisterComponent, ResetPasswordComponent, LoginComponent, VerifyEmailComponent],
      providers: [provideRouter([]), { provide: AuthService, useValue: auth }, {
        provide: ActivatedRoute,
        useValue: { snapshot: { queryParamMap: convertToParamMap({}) }, queryParams: of({ email: "demo@example.com" }) },
      }],
    });
    navigate = vi.spyOn(TestBed.inject(Router), "navigate").mockResolvedValue(true);
  });

  function registration() {
    const fixture = TestBed.createComponent(RegisterComponent);
    fixture.componentInstance.form.setValue({ nombre: "Demo", username: "demo123", email: "demo@example.com", password, confirmPassword: password });
    return fixture;
  }

  it("no envía el registro si las contraseñas no coinciden", () => {
    const fixture = registration();
    fixture.componentInstance.form.controls.confirmPassword.setValue("OtherPassword123");
    fixture.componentInstance.onSubmit();
    fixture.detectChanges();
    expect(auth.register).not.toHaveBeenCalled();
    expect(fixture.nativeElement.textContent).toContain("Las contraseñas no coinciden");
  });

  it("no envía una contraseña de registro con menos de 12 caracteres", () => {
    const fixture = registration();
    fixture.componentInstance.form.patchValue({ password: "short", confirmPassword: "short" });
    fixture.componentInstance.onSubmit();
    expect(auth.register).not.toHaveBeenCalled();
  });

  it("envía los campos del backend sin confirmPassword y abre la verificación por correo", () => {
    const fixture = registration();
    fixture.componentInstance.onSubmit();
    expect(auth.register).toHaveBeenCalledWith({ nombre: "Demo", username: "demo123", email: "demo@example.com", password });
    expect(navigate).toHaveBeenCalledWith(["/verificar-correo"], { queryParams: { email: "demo@example.com" } });
  });

  it("evita enviar el registro dos veces mientras está pendiente", () => {
    const response = new Subject<ApiResponse>();
    auth.register.mockReturnValue(response);
    const fixture = registration();
    fixture.componentInstance.onSubmit();
    fixture.componentInstance.onSubmit();
    expect(auth.register).toHaveBeenCalledOnce();
    response.next({ mensaje: "Enviado" });
    response.complete();
  });

  it("el login conserva el paso OTP antes de entrar al inventario", () => {
    const fixture = TestBed.createComponent(LoginComponent);
    const component = fixture.componentInstance;
    component.username = "demo123";
    component.password = password;
    component.onSubmit();
    expect(navigate).toHaveBeenCalledWith(["/verificar-correo"], { queryParams: { email: "demo@example.com" } });
    expect(navigate).not.toHaveBeenCalledWith(["/inventario"]);
  });

  it("muestra credenciales incorrectas y permite corregirlas sin redirigir", () => {
    auth.login.mockReturnValue(throwError(() => new HttpErrorResponse({ status: 401 })));
    const component = TestBed.createComponent(LoginComponent).componentInstance;
    component.username = "demo123";
    component.password = password;
    component.onSubmit();
    expect(component.loading).toBe(false);
    expect(component.errorMessage).toContain("contraseña incorrectos");
    expect(navigate).not.toHaveBeenCalled();
  });

  it("solo envía códigos de seis dígitos y abre inventario al verificar", () => {
    const fixture = TestBed.createComponent(VerifyEmailComponent);
    fixture.detectChanges();
    fixture.componentInstance.codigo = "abc123";
    fixture.componentInstance.onVerify();
    expect(auth.verifyEmail).not.toHaveBeenCalled();
    fixture.componentInstance.codigo = "123456";
    fixture.componentInstance.onVerify();
    expect(auth.verifyEmail).toHaveBeenCalledWith("demo@example.com", "123456");
    expect(navigate).toHaveBeenCalledWith(["/inventario"]);
  });

  it("la recuperación exige correo válido, OTP numérico y confirmación de contraseña", () => {
    const component = TestBed.createComponent(ResetPasswordComponent).componentInstance;
    component.requestForm.controls.email.setValue("invalid");
    component.onRequestCode();
    expect(auth.requestPasswordReset).not.toHaveBeenCalled();
    component.requestForm.controls.email.setValue("demo@example.com");
    component.onRequestCode();
    expect(component.step).toBe(2);
    component.resetForm.setValue({ codigo: "abcdef", nuevaPassword: password, confirmPassword: password });
    component.onResetPassword();
    expect(auth.resetPassword).not.toHaveBeenCalled();
    component.resetForm.patchValue({ codigo: "123456", confirmPassword: "OtherPassword123" });
    component.onResetPassword();
    expect(auth.resetPassword).not.toHaveBeenCalled();
    component.resetForm.controls.confirmPassword.setValue(password);
    component.onResetPassword();
    expect(auth.resetPassword).toHaveBeenCalledWith("demo@example.com", "123456", password);
    expect(component.passwordResetSuccess).toBe(true);
    expect(component.resetForm.controls.nuevaPassword.value).toBe("");
  });
});
