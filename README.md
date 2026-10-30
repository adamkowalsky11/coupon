# Coupon Service

Heksagonalny serwis REST do tworzenia i realizacji kuponów. Użycie kuponu jest powiązane z krajem klienta, wyznaczanym na podstawie adresu IP przez zewnętrzne API `ipwho.is`.

## Funkcje

- tworzenie kuponu z unikalnym kodem, limitem użyć i krajem,
- pobieranie kuponu po kodzie,
- realizacja kuponu z weryfikacją kraju, limitu użyć i unikalności użytkownika,
- geolokalizacja IP,
- migracje Flyway, Testcontainers, OpenAPI/Swagger, testy ArchUnit.

## Stos technologiczny

| Element | Wersja |
| --- | --- |
| Java | 25 |
| Spring Boot | 4.1.2 |
| PostgreSQL | 17 |
| Flyway | zarządzany przez Spring Boot |
| springdoc OpenAPI | 3.2.0 |
| Resilience4j | 2.3.0 |
| Caffeine | zarządzany przez Spring Boot |
| ArchUnit | 1.5.1 |

## Architektura

```text
coupon-adapter-in-rest  ──zależy──▶  coupon-application  ◀──zależy── coupon-adapter-out-persistence
(HTTP wchodzi)                        (porty in/out)       ◀──zależy── coupon-adapter-out-geolocation
                                            │                        (DB / ipwho.is wychodzą)
                                            ▼ zależy
                                      coupon-domain
                          (nie zależy od innych modułów ani frameworków; aplikacja i adaptery mogą używać tego modułu)
```

Strzałki oznaczają zależności kompilacyjne. Adaptery REST, persistence i geolokalizacji
dodatkowo bezpośrednio używają `coupon-domain`. `coupon-configuration` jest composition
root: zależy od wszystkich modułów i wiąże porty wejściowe/wyjściowe z adapterami.

| Moduł | Odpowiedzialność |
| --- | --- |
| `coupon-domain` | model domenowy, reguły biznesowe, wyjątki domenowe |
| `coupon-application` | przypadki użycia, porty wejściowe/wyjściowe, transakcje |
| `coupon-adapter-in-rest` | kontroler REST, DTO, walidacja, mapowanie błędów |
| `coupon-adapter-out-persistence` | JPA, repozytoria Spring Data, mapowanie encji |
| `coupon-adapter-out-geolocation` | klient `ipwho.is`, cache, retry, circuit breaker, time limiter |
| `coupon-configuration` | konfiguracja Springa, uruchomienie, Flyway, OpenAPI |

Zależności między warstwami są weryfikowane przez `ArchitectureTest`:

- domena nie zależy od aplikacji, adapterów ani frameworków,
- aplikacja nie zależy od adapterów i poza Javą/domeną może używać tylko `org.springframework.transaction..` i `org.springframework.dao..`,
- adaptery wejściowe i wyjściowe nie zależą od siebie,
- porty aplikacji są interfejsami,
- brak cykli między pakietami.

## Uzasadnienie wyborów projektowych

Podjęte decyzje:

- **Architektura heksagonalna** oddziela reguły kuponu od Springa, JPA i zewnętrznego API. Dzięki temu domena jest testowana bez bazy i sieci, a adapter geolokalizacji można podmienić bez zmian w logice biznesowej. Kosztem jest większa liczba modułów i mapowań; granice pilnują testy ArchUnit.
- **Java 25 i Spring Boot 4.1.2** dają wspierane środowisko uruchomieniowe oraz gotowe mechanizmy transakcji, walidacji, OpenAPI i testów.
- **PostgreSQL zamiast bazy in-memory** wybrano ze względu na rzeczywiste zachowanie ograniczeń unikalności, transakcji i blokad wierszy. Flyway tworzy schemat w powtarzalny sposób, a `ddl-auto=validate` wykrywa rozjazd między encjami a migracjami. Testy integracyjne używają tego samego silnika.
- **Blokada pesymistyczna jest podstawowym mechanizmem współbieżności**, ponieważ zapewnia, że limit użyć nie zostanie przekroczony. `@Version` i ograniczony retry (maksymalnie 3 próby) są zabezpieczeniem awaryjnym dla konfliktów optymistycznych; retry nie jest jedyną ochroną, bo przy większym obciążeniu mógłby wielokrotnie powtarzać tę samą pracę.
- **Geolokalizacja jest adapterem, a nie częścią domeny**, ponieważ zewnętrzne API może być wolne lub niedostępne. Cache 24 h z odświeżaniem po godzinie, retry, circuit breaker i time limiter ograniczają liczbę wywołań i skutki awarii. Przy braku zaufanych danych aplikacja zwraca `503`, zamiast zezwalać na użycie kuponu bez weryfikacji kraju.
- **Błędy obsługiwane przez `CouponExceptionHandler` mają postać `ProblemDetail` z polem `code`**, co daje standardowy format HTTP i stabilny identyfikator do obsługi po stronie klienta. `COUPON_EXPIRED` oznacza osiągnięcie limitu użyć, nie upływ czasu; pole `remainingUsages` jednoznacznie pokazuje pozostałą liczbę użyć.
- **Dwa warianty uruchomienia** rozdzielają wygodę od pełnego stacku: `compose-dev.yaml` startuje tylko PostgreSQL dla IntelliJ, a `compose.yaml` uruchamia aplikację i bazę razem. Dependency `spring-boot-docker-compose` jest wyłącznie `developmentOnly`, więc nie trafia do produkcyjnego JAR-a.

## Start w IntelliJ (zalecany)

Uruchomienie opiera się na Docker Desktop. Spring Boot sam uruchomi PostgreSQL na podstawie `compose-dev.yaml`; nie trzeba tworzyć bazy ani ustawiać zmiennych środowiskowych.

1. Otwórz projekt w IntelliJ jako projekt Gradle i poczekaj na synchronizację.
2. Uruchom klasę `com.dev.coupon.CouponApplication`.
3. Otwórz Swagger UI: `http://localhost:8080/swagger-ui/index.html`.
4. Do testu realizacji kuponu użyj nagłówka `X-Forwarded-For: 83.16.0.1` i kuponu z `country: "PL"` — w Swagger UI jest dostępne pole nagłówka, w Postmanie ustaw go ręcznie.

PostgreSQL działa w projekcie Compose `coupon-dev`, a dane są przechowywane w wolumenie `coupon-dev_postgres-data`. Po zamknięciu aplikacji Spring zatrzymuje kontener, ale nie usuwa danych.

Jeśli baza nie startuje automatycznie, upewnij się, że Docker Desktop działa, a projekt został zaimportowany jako projekt Gradle.

Nie uruchamiaj jednocześnie aplikacji z IntelliJ i pełnego `docker compose up` na tym samym porcie `8080`.

## Start z Docker Compose

```powershell
docker compose up --build
```

Aplikacja będzie dostępna pod adresem:

- API: `http://localhost:8080/api/v1/coupons`
- Swagger UI: `http://localhost:8080/swagger-ui/index.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`

Zatrzymanie i usunięcie danych:

```powershell
docker compose down -v
```

Domyślne dane lokalne:

| Zmienna | Wartość domyślna |
| --- | --- |
| `POSTGRES_DB` | `coupon` |
| `POSTGRES_USER` | `coupon` |
| `POSTGRES_PASSWORD` | `coupon` |
| `POSTGRES_PORT` | `5432` |
| `APP_PORT` | `8080` |
| `COUPON_GEO_BASE_URL` | `https://ipwho.is` |

Przykład z innymi portami:

```powershell
$env:APP_PORT = '8081'
$env:POSTGRES_PORT = '5433'
docker compose up --build
```

Dane uwierzytelniające są przeznaczone wyłącznie do pracy lokalnej.

## API

### Pobranie wszystkich kuponów

```http
GET /api/v1/coupons
```

Zwraca wszystkie kupony, od najnowszego. Kolekcja zawiera również kupony wykorzystane do limitu (`remainingUsages = 0`); pole `remainingUsages` pokazuje liczbę pozostałych użyć.

Odpowiedź `200 OK`:

```json
[
  {
    "id": "8e5e4f54-7f8e-4ce6-8c3d-2f4ed9f0f2a1",
    "code": "SPRING-2026",
    "maxUsages": 5,
    "currentUsages": 0,
    "remainingUsages": 5,
    "country": "PL",
    "createdAt": "2026-01-01T00:00:00Z"
  }
]
```

### Utworzenie kuponu

```http
POST /api/v1/coupons
Content-Type: application/json

{
  "code": "spring-2026",
  "maxUsages": 5,
  "country": "PL"
}
```

Odpowiedź `201 Created`:

```json
{
  "id": "8e5e4f54-7f8e-4ce6-8c3d-2f4ed9f0f2a1",
  "code": "SPRING-2026",
  "maxUsages": 5,
  "currentUsages": 0,
  "remainingUsages": 5,
  "country": "PL",
  "createdAt": "2026-01-01T00:00:00Z"
}
```

Kod jest przycinany i normalizowany do wielkich liter; może mieć do 64 znaków i zawierać wyłącznie litery, cyfry, `_` oraz `-`. `maxUsages` musi być większe od zera. Kraj musi być dwuliterowym kodem ISO 3166-1 alpha-2.

### Pobranie kuponu

```http
GET /api/v1/coupons/spring-2026
```

Odpowiedź `200 OK` ma taki sam format jak element listy powyżej. Dla nieistniejącego kodu zwracane jest `404 COUPON_NOT_FOUND`. Kod w ścieżce jest normalizowany tak samo jak przy tworzeniu.

### Realizacja kuponu

```http
POST /api/v1/coupons/spring-2026/couponusage
Content-Type: application/json

{
  "userId": "user-1"
}
```

Odpowiedź `201 Created`:

```json
{
  "id": "f0d2f4d2-6f3a-4a2f-b44b-6e6f1f6c8a11",
  "couponId": "8e5e4f54-7f8e-4ce6-8c3d-2f4ed9f0f2a1",
  "userId": "user-1",
  "usedAt": "2026-01-01T00:05:00Z",
  "country": "PL"
}
```

Pole `userId` jest przycinane i może mieć maksymalnie 128 znaków. Kraj klienta jest wyznaczany z adresu IP. Adres jest pobierany z pierwszego niepustego wpisu nagłówka `X-Forwarded-For`; jeśli nagłówek nie istnieje lub nie zawiera niepustego wpisu — z `remoteAddr`.

W lokalnym Swagger UI żądanie pochodzi z `127.0.0.1`, więc zewnętrzne API geolokalizacji odrzuci je i zostanie zwrócone `503`.
W operacji realizacji kuponu dostępny jest opcjonalny nagłówek `X-Forwarded-For`; wpisz tam publiczny adres IP, np. `83.16.0.1` (kraj `PL`).

### Testowanie w Postmanie

Uruchom aplikację, a następnie wykonaj żądania w kolejności:

1. **Utworzenie kuponu**

   - Metoda: `POST`
   - URL: `http://localhost:8080/api/v1/coupons`
   - Zakładka `Body` → `raw` → `JSON`:

   ```json
   {
     "code": "spring-2026",
     "maxUsages": 5,
     "country": "PL"
   }
   ```

   - Oczekiwana odpowiedź: `201 Created`.

2. **Pobranie wszystkich kuponów**

   - Metoda: `GET`
   - URL: `http://localhost:8080/api/v1/coupons`
   - Oczekiwana odpowiedź: `200 OK` i tablica kuponów, od najnowszego.

3. **Pobranie kuponu po kodzie**

   - Metoda: `GET`
   - URL: `http://localhost:8080/api/v1/coupons/spring-2026`
   - Oczekiwana odpowiedź: `200 OK`.

4. **Realizacja kuponu**

   - Metoda: `POST`
   - URL: `http://localhost:8080/api/v1/coupons/spring-2026/couponusage`
   - Nagłówki:
     - `Content-Type: application/json`
     - `X-Forwarded-For: 83.16.0.1`
   - Zakładka `Body` → `raw` → `JSON`:

   ```json
   {
     "userId": "user-1"
   }
   ```

   - Oczekiwana odpowiedź: `201 Created` z `country` równym `PL`.

Bez nagłówka `X-Forwarded-For` Postman wyśle żądanie z lokalnego adresu, a geolokalizacja zwróci `503`.
Po wykonaniu realizacji `GET /api/v1/coupons` pokaże zwiększone `currentUsages` i zmniejszone `remainingUsages`.
Jeśli używasz innego portu, podmień bazowy URL odpowiednio na `http://localhost:8081`.

## Błędy

Błędy obsługiwane przez `CouponExceptionHandler` są zwracane jako `application/problem+json` (`ProblemDetail`) z dodatkowym polem `code`.

| HTTP | `code` | Znaczenie |
| --- | --- | --- |
| 400 | `VALIDATION_FAILED` | niepoprawne dane wejściowe |
| 400 | `INVALID_REQUEST` | brakujące lub niepoprawne JSON |
| 400 | `CLIENT_IP_NOT_RESOLVED` | nie udało się ustalić IP klienta |
| 403 | `COUNTRY_NOT_ALLOWED` | kraj klienta nie odpowiada krajowi kuponu |
| 404 | `COUPON_NOT_FOUND` | kupon nie istnieje |
| 409 | `COUPON_EXPIRED` | osiągnięto limit użyć (`currentUsages >= maxUsages`) |
| 409 | `COUPON_ALREADY_USED` | użytkownik już użył kuponu |
| 409 | `COUPON_CODE_ALREADY_EXISTS` | kod kuponu już istnieje |
| 409 | `CONCURRENT_MODIFICATION` | konflikt wersji po wykorzystaniu wszystkich prób retry |
| 409 | `CONFLICT` | konflikt ograniczenia bazy danych |
| 503 | `GEO_SERVICE_UNAVAILABLE` | geolokalizacja niedostępna i brak cache |

Powyższa tabela obejmuje wyjątki obsłużone przez aplikację. Błędy nieobsłużone — np. 500, 405, nieznana ścieżka 404 albo błąd blokady bazy — mogą wrócić w domyślnym formacie Spring Boota, bez pola `code`.

Przy równoległym tworzeniu tego samego kodu oba żądania mogą minąć sprawdzenie `existsByCode`; wtedy jedno z nich kończy się `409 CONFLICT` z powodu ograniczenia unikalności, a nie `COUPON_CODE_ALREADY_EXISTS`.

## Geolokalizacja

Adapter `ipwho.is`:

- wysyła `GET {coupon.geo.base-url}/{ip}`,
- akceptuje odpowiedź tylko wtedy, gdy `success=true` i `country_code` jest poprawnym kodem kraju,
- cache'uje wynik na 24 godziny i odświeża go po godzinie,
- cache jest lokalny (per instancja) i nie ma limitu rozmiaru,
- przy błędzie odświeżenia zwraca ostatnią znaną wartość z cache,
- przy braku cache zwraca `GeoLocationUnavailableException`, mapowany na `503`,
- używa retry, circuit breakera i time limitera (3 s).

Konfiguracja:

```properties
coupon.geo.base-url=https://ipwho.is
```

Wartości parametrów (timeouty połączenia i odczytu, retry, cache i time limiter) są ustawione w `GeoLocationConfiguration`; sam cache jest budowany w adapterze geolokalizacji.

## Współbieżność

Zapis użycia odbywa się w transakcji, a wiersz kuponu jest blokowany przez `@Lock(LockModeType.PESSIMISTIC_WRITE)`. Geolokalizacja IP jest wykonywana przed transakcją, więc wolne wywołanie `ipwho.is` nie trzyma blokady. Dzięki temu:

- liczba użyć nigdy nie przekracza `maxUsages`,
- ten sam użytkownik nie może użyć kuponu dwa razy,
- pozostałe żądania otrzymują `409 COUPON_EXPIRED` lub `409 COUPON_ALREADY_USED`.

Dodatkowo encja ma `@Version`, a aplikacja ponawia transakcję po `OptimisticLockingFailureException` (maksymalnie 3 próby). Retry jest zabezpieczeniem awaryjnym i przy skutecznej blokadzie wiersza praktycznie nie występuje.

## Migracja

`spring.jpa.hibernate.ddl-auto=validate` — schemat jest tworzony wyłącznie przez Flyway (`V1__create_coupon_tables.sql`).

## Testy

```powershell
.\gradlew.bat testAll
```

lub pełny build:

```powershell
.\gradlew.bat build
```
Testy integracyjne korzystają z Dockera.

## Struktura projektu

```text
coupon/
├── coupon-domain/
├── coupon-application/
├── coupon-adapter-in-rest/
├── coupon-adapter-out-persistence/
├── coupon-adapter-out-geolocation/
├── coupon-configuration/
├── Dockerfile
├── compose.yaml
├── compose-dev.yaml
└── README.md
```

## Uwagi produkcyjne

- Aplikacja bezwarunkowo ufa nagłówkowi `X-Forwarded-For` (bierze pierwszy niepusty wpis). W produkcji powinna działać za zaufanym proxy, które go nadpisuje.
- Domyślne dane PostgreSQL w Compose służą tylko do lokalnego uruchomienia.
- Zewnętrzne API `ipwho.is` może mieć limity i przerwy w działaniu; cache i circuit breaker ograniczają skutki awarii.
- Brak uwierzytelniania, autoryzacji i rate limitingu; `userId` jest dowolny, więc przed publicznym wystawieniem API trzeba je dodać.
- Błędy blokad (`PessimisticLockingFailureException`) nie mają własnego handlera i mogą zwrócić 500 w domyślnym formacie Spring Boota.
- Przed wdrożeniem warto dodać metryki, health check i logowanie.
- Cache jest lokalny (per instancja) i obecnie nie ma limitu rozmiaru — przy wielu unikalnych adresach IP rośnie zużycie pamięci. Przed wdrożeniem warto ustawić `maximumSize`.