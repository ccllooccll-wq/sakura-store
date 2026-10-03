import { AbstractControl, ValidationErrors, ValidatorFn } from "@angular/forms";

export function passwordMatch(password: string, confirmation: string): ValidatorFn {
  return (control: AbstractControl): ValidationErrors | null =>
    control.get(password)?.value === control.get(confirmation)?.value
      ? null : { passwordMismatch: true };
}
