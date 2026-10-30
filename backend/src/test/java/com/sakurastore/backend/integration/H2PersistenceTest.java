package com.sakurastore.backend.integration;
import static org.junit.jupiter.api.Assertions.*;
import com.sakurastore.backend.BackendApplication;
import com.sakurastore.backend.application.dto.InventoryDtos.*;
import com.sakurastore.backend.application.usecase.InventoryService;
import java.math.BigDecimal;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.builder.SpringApplicationBuilder;
class H2PersistenceTest {
 @TempDir Path directory;
 @Test void fileDatabaseKeepsProductsAcrossRestarts(){
  String[] args={"--spring.profiles.active=h2","--server.port=0","--spring.datasource.url=jdbc:h2:file:"+directory.resolve("sakura")+";MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE","--app.bootstrap.email=","--app.bootstrap.password="};
  Long id;
  try(var app=new SpringApplicationBuilder(BackendApplication.class).run(args)){
   var service=app.getBean(InventoryService.class);var category=service.category(new CategoryCommand("Papelería"));
   id=service.create(new ProductCommand("PERSIST-1","Cuaderno",category.id(),BigDecimal.TEN,30)).id();
  }
  try(var app=new SpringApplicationBuilder(BackendApplication.class).run(args)){
   var products=app.getBean(InventoryService.class).products("PERSIST-1",false);
   assertEquals(1,products.size());assertEquals(id,products.getFirst().id());assertEquals("Cuaderno",products.getFirst().name());
  }
 }
}
