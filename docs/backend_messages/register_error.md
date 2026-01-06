Poruka za backend tim - problem s /api/auth/register

Register endpoint vraća 400 ali bez error message-a u response body. Trebate da vratite JSON sa detaljima greške:

{
  "message": "Validation failed",
  "errors": {
    "email": ["Email already exists"],
    "username": ["Username already taken"]
  }
}

Trenutno vraćate prazan response body, pa frontend ne zna šta je greška. Proverite @ExceptionHandler ili validation logic u `AuthController.register()` metodi. 

Predloženi koraci:
- Provjeriti da li unutar `register()` metode hvatate i vraćate detaljan error body.
- Ako koristite validaciju (jakarta validation), osigurajte da imate `@ExceptionHandler(MethodArgumentNotValidException.class)` ili globalni `@ControllerAdvice` koji mapira validacijske greške u strukturirani JSON kao gore.
- Alternativno, eksplicitno pri hvatanju RuntimeException u `register()` vratiti `ResponseEntity.badRequest().body(...)` sa odgovarajućim mapama/porukama.

Ako želite, mogu:
- Napraviti PR koji dodaje globalni `@ControllerAdvice` i vraća standardizirani JSON za validacijske greške, ili
- Direktno izmijeniti `AuthController.register()` da vraća navedeni JSON u slučaju greške.

Obavijestite me koju opciju preferirate.
