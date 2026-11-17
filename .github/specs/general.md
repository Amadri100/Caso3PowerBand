---
id: general
type: spec
scope: general
title: Arquitectura general del proyecto
status: draft            
version: 0.1.0
owner: <responsable>
created: 2026-11-17
updated: 2026-11-17
depends_on: []
---
# Proyecto

## Funciones

## Paquetes

### util
#### Constantes
+ Clase final, con constructor privado que contiene todas las constantes que utiliza el programa.
#### Configuraciones
+ Clase final, singleton, con las variables estaticas que pueden ser modificadas.
#### Otros
+ Diferentes clases que tengan metodos o objetos que sean utilizados en varias partes del codigo
### logging
+ recording y logging
### archivos
+ Maneja los archivos y los algoritmos a los que se le aplican
### input
#### Clases de inputs
+ Todas tienen un thread separado
+ Microfono utiliza la info que tiene para mandar el flujo de datos a los instrumentos
### intrumentos
#### Instrumento (abstract)
+ Contiene una referencia a un syntetizador por defecto en null (patron bridge)
+ metodos que mandan y hacen acciones en el sintetizador
+ metodos para cambiar de sintetizador
#### Clases Concretas:
+ Cada clase observa a las entradas y salidas correspondientes de su programa.
+ Microfono
+ Control
+ Tableta
### comandos
+ Commandos que puede realizar el instrumnento y se aplican al synth siguendo el patron command
### sintetizadores
+ contiene las interfaces para trabajar con sintetizadores,
+ reciben los datos y los mandan mediantes 
#### BandClient
+ clase ya creada de el cliente usando UDP
### controlador
+ Acceso global desde un metodo estatico y con una unica instancia. 
+ Interfaz para obtener la información necesaria
### ui
+ botones escenciales para iniciar la conexion, modificar configuraciones, 
+ Debe permitir cambiar el dikspositivo compatible para un cierto instrumento
+ Debe permitir intercambiar el sintetizador del dispositivo 
+ Debe permitir buscar, encontrar y guardar las grabaciones
### main
## Tecnologia usada
### Maven
+ Se utilizo maven para manejar las dependencias del proyecto
### Instrumentos
#### Microfono
+ Se utiliza TarsosDSP para detectar, capturar y procesar el audio
#### Control de juegos 
+ Se utiliza JInput para detectar, capturar y procesar las entradas del control
#### Tableta
+ Se utiliza JPen para detectar la posición general del lapiz en la tableta y la presión que se esta ejerciendo actualmente