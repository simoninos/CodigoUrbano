# data/remote

Vacío por ahora. Aquí entra la conexión con el backend:

- `api/`: servicio HTTP (Retrofit o Ktor) con las llamadas al backend.
- `dto/`: lo que manda y recibe la API.
- `mapper/`: conversión entre DTO y los modelos de la app.

Los repositorios de `data/repository` deciden la fuente: Room como caché y la API como fuente de verdad. Las pantallas solo ven las interfaces de `domain/repository`, así que cambiar la simulación por la API no toca ningún ViewModel.
