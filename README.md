# Sakura Store — Sistema de Gestión de Inventario y Ventas

Proyecto grupal de Programación III. Sistema para la gestión de inventario y ventas de **Sakura Store**, tienda de productos coreanos, desarrollado con **Clean Architecture**, **POO**, **PostgreSQL** y **Angular**, siguiendo la metodología **Scrum**.

## Estructura del repositorio

```
sakura-store/
├── backend/          # API en Spring Boot (Java) — Clean Architecture
│   └── src/main/java/com/sakurastore/backend/
│       ├── domain/           # Entidades de dominio puras (POO), sin dependencias de framework
│       ├── application/      # Casos de uso e interfaces (puertos) de repositorio
│       │   ├── usecase/
│       │   └── repository/
│       ├── infrastructure/   # Implementaciones concretas: JPA/PostgreSQL, configuración
│       │   ├── persistence/
│       │   └── config/
│       └── presentation/     # Controladores REST y DTOs
│           ├── controller/
│           └── dto/
├── frontend/         # Aplicación Angular
│   └── src/app/
│       ├── core/          # Servicios singleton, guards, interceptors
│       ├── shared/        # Componentes, pipes y directivas reutilizables
│       └── features/      # Módulos por funcionalidad (usuarios, inventario, ventas, dashboard)
├── docs/             # Documentación del proyecto
│   ├── cronograma/       # Cronograma Scrum, actas de reuniones
│   ├── diagramas/        # Diagrama ER, diagramas de arquitectura
│   └── manual-usuario/   # Manual de usuario final
└── .github/workflows/    # CI (build y tests automáticos)
```

## Equipo (Scrum)

| Integrante | Rol Scrum | Módulo / entidades a cargo |
|---|---|---|
| Derik Bernabe | Product Owner | Arquitectura base, Usuario/Rol, Venta/DetalleVenta |
| Marcelo Calle | Scrum Master | Frontend Angular completo |
| Alvaro Sillerico | Desarrollador | Conexión BD, Categoría/Producto, API inventario y reportes |
| Ronny Mamani | Desarrollador | Proveedor/MovimientoInventario/Cliente, repositorios |

## Stack tecnológico

- **Backend:** Java 17+, Spring Boot, Spring Data JPA
- **Base de datos:** PostgreSQL
- **Frontend:** Angular
- **Metodología:** Scrum (Sprints de 2 semanas)

## Cómo levantar el proyecto

### Backend
```bash
cd backend
mvn spring-boot:run
```
Configura las credenciales de PostgreSQL en `backend/src/main/resources/application.properties`.

### Frontend
```bash
cd frontend
npm install
ng serve
```

## Convención de ramas

- `main` — versión estable
- `develop` — integración de features
- `feature/<nombre>` — una rama por tarea del sprint (ej. `feature/entidad-usuario`, `feature/frontend-login`)

## Convención de commits

Se sugiere el formato [Conventional Commits](https://www.conventionalcommits.org/):
```
feat: agregar entidad Usuario con validaciones
fix: corregir cálculo de subtotal en DetalleVenta
docs: agregar manual de usuario
test: pruebas unitarias de la entidad Producto
```
