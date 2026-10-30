package com.sakurastore.backend.domain.model;
import static org.junit.jupiter.api.Assertions.*;
import com.sakurastore.backend.domain.exception.DomainException;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
class NameRulesTest {
 @ParameterizedTest @ValueSource(strings={"Álvaro Daniel","Muñeca","María José","Niño"," Ana María ","Álvaro"})
 void acceptsLetters(String name){assertFalse(NameRules.require(name,100).isBlank());}
 @ParameterizedTest @ValueSource(strings={"123","Ana123","Ana@","Ana-Mar","Ana_Mar","Ana😊"," ","","Ana\nMaría","Ana\tMaría","́"})
 void rejectsInvalidNames(String name){assertThrows(DomainException.class,()->NameRules.require(name,100));}
}
