import { describe, expect, it } from "vitest";
import { validName } from "./name.validator";
describe("Nombres",()=>{
 it.each(["Álvaro Daniel","María José","Muñeca de colección","Niño","Álvaro"," Ana María "])("acepta %s",n=>expect(validName(n)).toBe(true));
 it.each(["Ana123","123","Ana@","Ana-Mar","Ana_Mar","Ana😊","","  ","Ana\nMaría","Ana\tMaría","́"])("rechaza %s",n=>expect(validName(n)).toBe(false));
});
