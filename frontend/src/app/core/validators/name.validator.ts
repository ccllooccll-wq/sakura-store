export const NAME_PATTERN = /^ *\p{L}[\p{L}\p{M}]*(?: +\p{L}[\p{L}\p{M}]*)* *$/u;
export const NAME_MESSAGE = "El nombre solo puede contener letras y espacios, sin números ni símbolos.";
export const validName = (value: string) => NAME_PATTERN.test(value);
