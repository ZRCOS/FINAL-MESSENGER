# Minimal Messenger (Android Native)

Aplicación Android nativa desarrollada en Kotlin y Jetpack Compose.
Gestiona mensajes de WhatsApp exclusivamente mediante las notificaciones públicas del sistema Android (`NotificationListenerService` y `RemoteInput`).

## Cómo compilar en Android Studio
1. Abre Android Studio Ladybug (2024.2) o superior.
2. Selecciona **File > Open** y elige esta carpeta raíz descomprimida.
3. Espera a que Gradle descargue las dependencias (el proyecto ya incluye `gradle/libs.versions.toml` y configuración de plugins directos para evitar cualquier error de referencias).
4. Menú **Build > Build Bundle(s) / APK(s) > Build APK(s)** o pulsa **Run (Shift + F10)**.
