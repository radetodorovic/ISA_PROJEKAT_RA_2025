# S3 Load Tests (Trending by Location)

Ovi testovi simuliraju zahteve za lokalni trending iz vise oblasti, po S3 zadatku.
Koristi se k6 i dva scenarija:

1) Hotspot: veliki broj zahteva iz iste oblasti (npr. centar Novog Sada).
2) Distributed: zahtevi iz vise gradova (NS, BG, Nis, Subotica).

## Preduslovi
- Backend pokrenut na `http://localhost:8080`
- Baza sadrzi videe sa lokacijom (lat/lng) i dovoljan broj eventa (view/like/comment)
- Validan JWT token (endpoint `/api/trending` zahteva autentifikaciju)

### Kako do JWT tokena
1) Registruj korisnika (ako vec nemas):
```
curl -X POST http://localhost:8080/api/auth/register ^
  -H "Content-Type: application/json" ^
  -d "{\"email\":\"test@example.com\",\"username\":\"test\",\"password\":\"password123\",\"confirmPassword\":\"password123\",\"firstName\":\"Test\",\"lastName\":\"User\",\"address\":\"Novi Sad\"}"
```

2) Aktiviraj nalog (kopiraj link iz backend loga) ili rucno u bazi postavi `enabled=true`.

3) Login:
```
curl -X POST http://localhost:8080/api/auth/login ^
  -H "Content-Type: application/json" ^
  -d "{\"email\":\"test@example.com\",\"password\":\"password123\"}"
```

Iz odgovora uzmi `token`.

## Pokretanje testova
Instaliraj k6: https://k6.io/docs/get-started/installation/

### 1) Hotspot scenario
```
k6 run ISA_PROJEKAT_RA_2025/load-tests/trending-locations.js ^
  -e JWT_TOKEN=PASTE_TOKEN ^
  -e SCENARIO=hotspot ^
  -e VUS=30 ^
  -e DURATION=30s ^
  -e RADIUS_METERS=200
```

### 2) Distributed scenario
```
k6 run ISA_PROJEKAT_RA_2025/load-tests/trending-locations.js ^
  -e JWT_TOKEN=PASTE_TOKEN ^
  -e SCENARIO=distributed ^
  -e VUS=30 ^
  -e DURATION=30s ^
  -e RADIUS_METERS=200
```

### 3) Oba scenarija (poredjenje u jednoj sesiji)
```
k6 run ISA_PROJEKAT_RA_2025/load-tests/trending-locations.js ^
  -e JWT_TOKEN=PASTE_TOKEN ^
  -e SCENARIO=both ^
  -e VUS=30 ^
  -e DURATION=30s ^
  -e RADIUS_METERS=200
```

## Sta test meri
- `http_req_duration` (latency) i `http_req_failed` stopu
- Poredjenje hotspot vs distributed opterecenja

## Napomena
Ako vidis mnogo 401 odgovora, JWT token nije validan ili je istekao.
