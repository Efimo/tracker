# Трекер Степана — 114 → 80 кг

## [⬇️ СКАЧАТЬ APK](https://raw.githubusercontent.com/Efimo/tracker/main/StepanTracker.apk)

> Нажми на ссылку выше → скачается файл → установи на Android

---

## Структура проекта

```
StepanTracker/
├── app/                          ← Android-приложение
│   └── src/main/
│       ├── assets/tracker.html   ← Трекер (встроен в APK)
│       ├── java/.../MainActivity.java
│       ├── AndroidManifest.xml
│       └── res/                  ← Иконки, стили
├── docs/
│   └── index.html               ← Для GitHub Pages (веб-версия)
├── build.gradle
└── README.md                     ← Ты тут
```

---

## Способ 1: Собрать APK (Android Studio)

### Шаг 1 — Установить Android Studio
1. Скачай: https://developer.android.com/studio
2. Установи (при установке выбери "Android SDK" и "Android Virtual Device")
3. Дождись загрузки SDK (~2-3 ГБ)

### Шаг 2 — Открыть проект
1. Открой Android Studio
2. `File → Open` → выбери папку `d:\Stepan\StepanTracker`
3. Дождись пока Gradle синхронизирует проект (первый раз ~5 мин)

### Шаг 3 — Собрать APK
**Для тестирования (debug):**
1. `Build → Build Bundle(s) / APK(s) → Build APK(s)`
2. APK появится в: `app/build/outputs/apk/debug/app-debug.apk`
3. Скинь на телефон и установи (включи "Установка из неизвестных источников")

**Для финальной версии (release):**
1. `Build → Generate Signed Bundle / APK`
2. Выбери APK
3. Создай ключ подписи (или используй существующий)
4. Готово!

### Шаг 4 — Установить на телефон
- Скинь APK на телефон (через USB / Telegram / облако)
- Открой файл → установить
- Если Android блокирует: Настройки → Безопасность → Разрешить установку из неизвестных источников

---

## Способ 2: Веб-версия через GitHub Pages (для ПК + браузера)

### Шаг 1 — Создать аккаунт на GitHub
1. Зайди на https://github.com
2. Зарегистрируйся (бесплатно)

### Шаг 2 — Создать репозиторий
1. Нажми `New repository`
2. Имя: `tracker` (или любое)
3. Public (обязательно для Pages)
4. Нажми `Create`

### Шаг 3 — Загрузить файлы
1. В репозитории нажми `Upload files`
2. Перетащи содержимое папки `docs/` (файл index.html)
3. Нажми `Commit`

### Шаг 4 — Включить GitHub Pages
1. `Settings → Pages`
2. Source: `Deploy from a branch`
3. Branch: `main`, папка: `/ (root)`
4. Нажми Save
5. Через 1-2 минуты сайт будет доступен по адресу:
   `https://ТВОЙ_ЛОГИН.github.io/tracker/`

### Шаг 5 — Использовать
- На ПК: открой ссылку в Chrome
- На телефоне: открой ссылку → три точки → "Добавить на главный экран"
- Синхронизация через кнопку SYNC работает автоматически

---

## Способ 3: Netlify (самый быстрый, без GitHub)

1. Зайди на https://app.netlify.com/drop
2. Перетащи папку `docs/` на страницу
3. Готово! Получишь ссылку вида `random-name.netlify.app`
4. Можно переименовать в настройках

---

## Синхронизация данных

Данные синхронизируются через JSONBin.io (облако):

1. Открой трекер на ПЕРВОМ устройстве
2. Нажми **SYNC** — создастся облачное хранилище
3. На ВТОРОМ устройстве тоже нажми **SYNC** — данные подтянутся
4. Дальше синхронизация автоматическая при каждом изменении

**Важно:** localStorage в WebView (APK) и в Chrome (веб) — это РАЗНЫЕ хранилища.
Поэтому кнопка SYNC обязательна для переноса данных между ними.

---

## Обновление трекера

Если обновил tracker.html:
1. Скопируй его в `app/src/main/assets/tracker.html`
2. Скопируй его в `docs/index.html`
3. Пересобери APK
4. Если используешь GitHub Pages — закоммить изменения
