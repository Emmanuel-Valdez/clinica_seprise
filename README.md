# SePrise App

App Android (Kotlin + Views) de Clínica SePrise — CU01..CU08.
Paquete `com.seprise.clinica`, minSdk 24. Abrir esta carpeta en Android Studio.

Docs: `../../01-planificacion-diseno/entrega-tp1-casos-uso.md` (spec), `../DISENO-FIGMA.md` (diseño), `../CREAR-PROYECTO.md` (ramas).

## Estructura de paquetes

Feature-first por dominio bajo `com.seprise.clinica`.

- `MainActivity.kt` — launcher (decide login vs. shell)
- `core/` — estado/datos compartidos: `AppData`, `SessionState`
- `auth/` — `LoginActivity`
- `shell/` — `MainNavActivity` (navegación por rol)
- `pacientes/` — `PerfilPacienteActivity` (y el ABM de CU02)

Cada CU nuevo = un paquete de dominio propio (`turnos/`, `agenda/`, `atencion/`, `estudios/`, `insumos/`, `honorarios/`). Las pantallas de un dominio se apuntan como `com.seprise.clinica.<dominio>.<X>Activity`. Los recursos (`res/`) van planos con prefijo (`cu01_`, `activity_`) — Android no soporta subcarpetas.


## Credenciales de prueba (CU01 — login)

No hay backend: el login valida usuario + contraseña + rol contra usuarios mock en memoria
(`app/src/main/java/com/seprise/clinica/SessionState.kt`). Los tres datos deben coincidir.

| Rol | Usuario | Contraseña |
|---|---|---|
| Administrativo | `admin` | `admin123` |
| Médico | `medico` | `medico123` |
| Paciente | `paciente` | `paciente123` |

> Solo para desarrollo y la entrega del TP. Reemplazar por usuarios reales cuando exista
> el CU02 / backend; a partir de entonces estos valores dejan de ser válidos.

## Build

```
./gradlew :app:assembleDebug
```
