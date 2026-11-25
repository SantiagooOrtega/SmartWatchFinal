# 🌐 SmartTranslator — Traductor ES→EN para Smartwatch

Aplicación de traducción en tiempo real de **Español a Inglés** para dispositivos **Wear OS**, desarrollada como proyecto final para la materia de **Diseño de Interfaces de Software**.

---

## 🎥 Video explicativo

▶️ [Ver video en Google Drive](https://drive.google.com/file/d/1xah2k5CHYw_ZBqVizRTl8EErYmNJtsJT/view?usp=sharing)

---

## 📱 Vista previa

| Pantalla Splash | Pantalla Principal | Resultado | Historial |
|---|---|---|---|
| Logo animado + anillo pulsante | Botón de voz / Input de texto | Traducción en tiempo real | Historial de traducciones |

---

## 🏗️ Arquitectura

El proyecto sigue el patrón **MVC (Model-View-Controller)**:

```
SmartTranslator/
│
├── backend/                          ← Node.js + Express (MVC)
│   ├── config/
│   │   └── supabase.js               ← Conexión a Supabase
│   ├── models/
│   │   └── Translation.js            ← MODEL: queries a Supabase
│   ├── controllers/
│   │   └── translationController.js  ← CONTROLLER: lógica de negocio
│   ├── routes/
│   │   └── translationRoutes.js      ← Rutas REST
│   ├── database/
│   │   └── schema.sql                ← Schema de la BD
│   ├── app.js                        ← Configuración Express
│   ├── server.js                     ← Punto de entrada
│   └── .env                          ← Variables de entorno
│
└── app/                              ← Kotlin + Wear OS (MVC)
    └── src/main/java/com/smarttranslator/app/presentation/
        ├── network/
        │   └── ApiService.kt         ← MODEL: llamadas a la API
        ├── TranslatorViewModel.kt    ← CONTROLLER: lógica de la app
        ├── MainActivity.kt           ← VIEW: interfaz del smartwatch
        └── SplashActivity.kt         ← VIEW: pantalla de inicio
```

---

## 🛠️ Stack Tecnológico

### Backend
| Tecnología | Uso |
|---|---|
| **Node.js + Express** | Servidor REST API |
| **Supabase (PostgreSQL)** | Base de datos en la nube |
| **DeepL API** | Motor de traducción con IA |
| **Retrofit / Axios** | Cliente HTTP |

### Frontend (Wear OS)
| Tecnología | Uso |
|---|---|
| **Kotlin** | Lenguaje principal |
| **Jetpack Compose for Wear OS** | UI declarativa |
| **Retrofit + OkHttp** | Llamadas al backend |
| **Coroutines + Flow** | Programación asíncrona |
| **ViewModel** | Gestión de estado |

---

## 🚀 Instalación y configuración

### Requisitos previos
- Node.js 18+
- Android Studio Rabbit 1 (2026.2.1)
- Wear OS SDK (API 30+)
- Cuenta en [Supabase](https://supabase.com)
- Cuenta en [DeepL API Free](https://www.deepl.com/pro-api)

### 1. Clonar el repositorio

```bash
git clone <url-del-repositorio>
cd Smartwatch
```

### 2. Configurar el Backend

```bash
cd backend
npm install
```

Crear el archivo `.env` con las credenciales:

```env
PORT=3000
SUPABASE_URL=https://tu-proyecto.supabase.co
SUPABASE_ANON_KEY=tu-service-role-key
DEEPL_API_KEY=tu-deepl-api-key
```

Crear la tabla en Supabase (SQL Editor):

```sql
CREATE TABLE translations (
  id              UUID        DEFAULT gen_random_uuid() PRIMARY KEY,
  original_text   TEXT        NOT NULL,
  translated_text TEXT        NOT NULL,
  source_lang     VARCHAR(10) DEFAULT 'ES',
  target_lang     VARCHAR(10) DEFAULT 'EN',
  created_at      TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

GRANT ALL PRIVILEGES ON TABLE translations TO anon;
GRANT ALL PRIVILEGES ON TABLE translations TO authenticated;
GRANT ALL PRIVILEGES ON TABLE translations TO service_role;
```

Iniciar el servidor:

```bash
npm run dev
```

### 3. Configurar la App Wear OS

Abre el proyecto en **Android Studio**, configura el emulador **Wear OS Large Round (API 37)** y ejecuta la app con el botón ▶.

> **Nota:** El emulador usa `10.0.2.2:3000` para conectar al localhost de tu máquina.

---

## 📡 API REST — Endpoints

| Método | Endpoint | Descripción |
|---|---|---|
| `POST` | `/api/translate` | Traduce texto ES→EN y guarda en BD |
| `GET` | `/api/translations` | Obtiene historial completo |
| `GET` | `/api/translations/:id` | Obtiene una traducción por ID |
| `DELETE` | `/api/translations/:id` | Elimina una traducción |

### Ejemplo de uso

**Request:**
```bash
curl -X POST http://localhost:3000/api/translate \
  -H "Content-Type: application/json" \
  -d '{"text": "Buenos días"}'
```

**Response:**
```json
{
  "success": true,
  "data": {
    "id": "uuid-aqui",
    "original_text": "Buenos días",
    "translated_text": "Good morning",
    "source_lang": "ES",
    "target_lang": "EN",
    "created_at": "2026-10-04T03:51:28.190501+00:00"
  }
}
```

---

## 🎨 Diseño de Interfaz

La UI sigue los principios de diseño de **Apple Watch**:

- **Fondo negro puro** — optimizado para pantallas OLED
- **Tipografía grande y legible** — prioridad en la legibilidad
- **Paleta minimalista** — blanco, gris y azul acento (#0A84FF)
- **Animaciones fluidas** — transiciones suaves entre estados
- **Modo dual** — voz (dispositivo físico) o texto (emulador)

### Estados de la aplicación

```
IDLE → LISTENING → LOADING → SUCCESS
                            ↘ ERROR → IDLE
```

---

## 👨‍💻 Autor

Proyecto Final — Diseño de Interfaces de Software  
Aplicación para Smartwatches (Wear OS)

---

## ⌚ Instalar en un reloj real (misma red Wi-Fi)

1. En el reloj: **Ajustes → Sistema → Acerca de** → tocar **Número de compilación** 7 veces.
2. **Opciones de desarrollador** → activar **Depuración ADB** y **Depuración inalámbrica**.
3. **Depuración inalámbrica → Vincular nuevo dispositivo** y en Android Studio usar **Pair Devices Using Wi-Fi** con el código.
4. Seleccionar el reloj y presionar **Run**.
5. En el computador: `cd backend && npm start`.
6. En `ApiService.kt`, `BASE_URL` debe ser `http://<IP del computador>:3000/` (`ipconfig getifaddr en0` en macOS).

## 🩺 Solución de problemas

- Verificar el estado del backend: `curl http://localhost:3000/api/health`
- `fetch failed` en Supabase: el proyecto puede estar pausado; restaurarlo en supabase.com y ejecutar `backend/database/schema.sql`.
- Si la BD falla, la traducción igual se muestra pero no se guarda en el historial.
- Si el reloj no conecta, permitir conexiones entrantes a Node en el firewall de macOS.
