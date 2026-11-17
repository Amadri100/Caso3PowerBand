---
id : general
layer : general
status: desarrollo
---
# Estructura
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
+ contiene tambien la conexion con las redes (solo necesita la parte de redes)
### controlador (singleton?)
+ Acceso global desde un metodo estatico
### ui
