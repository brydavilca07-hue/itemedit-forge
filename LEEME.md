# ItemEdit (mod para Minecraft 1.20.1 - Forge)

Comando `/itemedit`: edita el objeto que tienes en la mano.
Hay que tener trucos activados (cheats) u ser operador.

- `/itemedit nombre <texto>`: cambia el nombre (usa & para colores, ej: `&6Pico Dorado`).
- `/itemedit lore agregar <texto>` y `/itemedit lore borrar`: descripcion del objeto.
- `/itemedit enchanted <encantamiento> <nivel>`: nivel libre de 1 a 32767. Nivel 0 lo quita.
  Ejemplo: `/itemedit enchanted minecraft:fortune 50`
- `/itemedit irrompible`: activa o desactiva.
- `/itemedit reparar`: repara el objeto.
- `/itemedit atributo <danio|armadura|vida|velocidad> <valor>`: suma un atributo.
  La velocidad es un porcentaje: 0.5 = +50%. `/itemedit atributo borrar` los quita.

Ojo: niveles muy altos pueden romper el juego (Proteccion enorme te vuelve invencible,
Filo enorme mata de un golpe). Es cosa tuya hasta donde subirlos.

## Instalacion en TLauncher

1. Version **Forge 1.20.1**. Pon `itemedit-1.0.0.jar` en la carpeta `mods`.
2. No necesita ningun otro mod.

## Como obtener el .jar

Sube esta carpeta a un repositorio de GitHub: la pestana Actions compila el mod
y lo descargas desde Artifacts. O en tu PC (Java 17 + Gradle 8.x): `gradle build`.
