import 'dart:io';
import 'dart:math';

class Buscaminas {
  static const int filas = 6;
  static const int columnas = 10;
  static const int totalMinas = 8;
  
  late List<List<String>> tablero;
  late List<List<bool>> minas;
  late List<List<bool>> descubierto;
  late List<List<bool>> banderas;
  bool gameOver = false;
  bool victoria = false;
  int tiradas = 0;
  bool mostrarTrucos = false;
  Random random = Random();

  Buscaminas() {
    inicializar();
  }

  void inicializar() {
    tablero = List.generate(filas, (_) => List.filled(columnas, '·'));
    minas = List.generate(filas, (_) => List.filled(columnas, false));
    descubierto = List.generate(filas, (_) => List.filled(columnas, false));
    banderas = List.generate(filas, (_) => List.filled(columnas, false));
    gameOver = false;
    victoria = false;
    tiradas = 0;
    mostrarTrucos = false;
    
    generarMinas();
    forzarMinasPorCuadrante();
  }

  void generarMinas() {
    int minasColocadas = 0;
    
    while (minasColocadas < totalMinas) {
      int fila = random.nextInt(filas);
      int columna = random.nextInt(columnas);
      
      if (!minas[fila][columna]) {
        minas[fila][columna] = true;
        minasColocadas++;
      }
    }
  }

  void forzarMinasPorCuadrante() {
    // Contar minas en cada cuadrante
    Map<int, int> conteoMinas = {1: 0, 2: 0, 3: 0, 4: 0};
    
    for (int f = 0; f < filas; f++) {
      for (int c = 0; c < columnas; c++) {
        if (minas[f][c]) {
          int cuadrante = obtenerCuadrante(f, c);
          conteoMinas[cuadrante] = (conteoMinas[cuadrante] ?? 0) + 1;
        }
      }
    }
    
    // Asegurar al menos 2 minas en cada cuadrante
    for (int cuadrante = 1; cuadrante <= 4; cuadrante++) {
      while ((conteoMinas[cuadrante] ?? 0) < 2) {
        // Obtener posición aleatoria en el cuadrante
        var pos = obtenerPosicionEnCuadrante(cuadrante);
        int f = pos[0], c = pos[1];
        
        if (!minas[f][c]) {
          // Quitar una mina de otro cuadrante que tenga más de 2
          for (int otroCuadrante = 1; otroCuadrante <= 4; otroCuadrante++) {
            if (otroCuadrante != cuadrante && (conteoMinas[otroCuadrante] ?? 0) > 2) {
              for (int ff = 0; ff < filas; ff++) {
                for (int cc = 0; cc < columnas; cc++) {
                  if (minas[ff][cc] && obtenerCuadrante(ff, cc) == otroCuadrante) {
                    minas[ff][cc] = false;
                    conteoMinas[otroCuadrante] = (conteoMinas[otroCuadrante] ?? 0) - 1;
                    
                    minas[f][c] = true;
                    conteoMinas[cuadrante] = (conteoMinas[cuadrante] ?? 0) + 1;
                    
                    break;
                  }
                }
                if (minas[f][c]) break;
              }
              break;
            }
          }
          
          if (!minas[f][c]) {
            // Si no se pudo quitar de otro, simplemente agregar una nueva
            minas[f][c] = true;
            conteoMinas[cuadrante] = (conteoMinas[cuadrante] ?? 0) + 1;
          }
        }
      }
    }
  }

  int obtenerCuadrante(int fila, int columna) {
    bool primeraMitadFilas = fila <= 2; // 0-2
    bool primeraMitadColumnas = columna <= 4; // 0-4
    
    if (primeraMitadFilas && primeraMitadColumnas) return 1;
    if (primeraMitadFilas && !primeraMitadColumnas) return 2;
    if (!primeraMitadFilas && primeraMitadColumnas) return 3;
    return 4; // !primeraMitadFilas && !primeraMitadColumnas
  }

  List<int> obtenerPosicionEnCuadrante(int cuadrante) {
    switch (cuadrante) {
      case 1: return [random.nextInt(3), random.nextInt(5)];
      case 2: return [random.nextInt(3), random.nextInt(5) + 5];
      case 3: return [random.nextInt(3) + 3, random.nextInt(5)];
      case 4: return [random.nextInt(3) + 3, random.nextInt(5) + 5];
      default: return [0, 0];
    }
  }

  int contarMinasAdyacentes(int fila, int columna) {
    int count = 0;
    
    for (int df = -1; df <= 1; df++) {
      for (int dc = -1; dc <= 1; dc++) {
        if (df == 0 && dc == 0) continue;
        
        int nf = fila + df;
        int nc = columna + dc;
        
        if (nf >= 0 && nf < filas && nc >= 0 && nc < columnas) {
          if (minas[nf][nc]) {
            count++;
          }
        }
      }
    }
    
    return count;
  }

  bool destaparCasilla(int fila, int columna, bool esPrimeraJugada, bool esJugadaUsuario) {
    // Verificar límites
    if (fila < 0 || fila >= filas || columna < 0 || columna >= columnas) {
      return false;
    }
    
    // Verificar si ya está descubierta o tiene bandera
    if (descubierto[fila][columna] || banderas[fila][columna]) {
      return false;
    }
    
    // Verificar si es mina
    if (minas[fila][columna]) {
      if (esPrimeraJugada) {
        // Mover la mina a una posición vacía
        moverMina(fila, columna);
        return destaparCasilla(fila, columna, false, false);
      } else if (esJugadaUsuario) {
        return true; // Explota
      } else {
        return false; // No explota durante recursividad
      }
    }
    
    // Contar minas adyacentes
    int numMinas = contarMinasAdyacentes(fila, columna);
    descubierto[fila][columna] = true;
    
    // Actualizar tablero
    if (numMinas == 0) {
      tablero[fila][columna] = ' ';
    } else {
      tablero[fila][columna] = numMinas.toString();
    }
    
    // Si no hay minas adyacentes, destapar recursivamente
    if (numMinas == 0) {
      for (int df = -1; df <= 1; df++) {
        for (int dc = -1; dc <= 1; dc++) {
          if (df == 0 && dc == 0) continue;
          
          int nf = fila + df;
          int nc = columna + dc;
          
          destaparCasilla(nf, nc, false, false);
        }
      }
    }
    
    return false; // No explota
  }

  void moverMina(int filaOriginal, int columnaOriginal) {
    minas[filaOriginal][columnaOriginal] = false;
    
    // Buscar una posición vacía
    while (true) {
      int f = random.nextInt(filas);
      int c = random.nextInt(columnas);
      
      if (!minas[f][c] && (f != filaOriginal || c != columnaOriginal)) {
        minas[f][c] = true;
        break;
      }
    }
  }

  void mostrarTablero() {
    print('\n 0123456789');
    
    for (int f = 0; f < filas; f++) {
      String filaStr = String.fromCharCode('A'.codeUnitAt(0) + f);
      
      for (int c = 0; c < columnas; c++) {
        if (banderas[f][c]) {
          filaStr += '#';
        } else if (!descubierto[f][c]) {
          filaStr += '·';
        } else {
          filaStr += tablero[f][c];
        }
      }
      
      print(filaStr);
    }
  }

  void mostrarTableroCompleto() {
    print('\n 0123456789');
    
    for (int f = 0; f < filas; f++) {
      String filaStr = String.fromCharCode('A'.codeUnitAt(0) + f);
      
      for (int c = 0; c < columnas; c++) {
        if (minas[f][c]) {
          filaStr += '*';
        } else if (banderas[f][c]) {
          filaStr += '#';
        } else if (!descubierto[f][c]) {
          filaStr += '·';
        } else {
          filaStr += tablero[f][c];
        }
      }
      
      print(filaStr);
    }
  }

  void mostrarAmbosTableros() {
    print(' 0123456789     0123456789');
    
    for (int f = 0; f < filas; f++) {
      String filaJuego = String.fromCharCode('A'.codeUnitAt(0) + f);
      String filaMinas = String.fromCharCode('A'.codeUnitAt(0) + f);
      
      for (int c = 0; c < columnas; c++) {
        // Tablero de juego
        if (banderas[f][c]) {
          filaJuego += '#';
        } else if (!descubierto[f][c]) {
          filaJuego += '·';
        } else {
          filaJuego += tablero[f][c];
        }
        
        // Tablero de minas
        if (minas[f][c]) {
          filaMinas += '*';
        } else if (banderas[f][c]) {
          filaMinas += '#';
        } else if (!descubierto[f][c]) {
          filaMinas += '·';
        } else {
          filaMinas += tablero[f][c];
        }
      }
      
      print('$filaJuego    $filaMinas');
    }
  }

  void toggleBandera(int fila, int columna) {
    if (descubierto[fila][columna]) {
      print('No se puede poner bandera en casilla descubierta.');
      return;
    }
    
    banderas[fila][columna] = !banderas[fila][columna];
  }

  bool verificarVictoria() {
    for (int f = 0; f < filas; f++) {
      for (int c = 0; c < columnas; c++) {
        // Si hay una mina sin bandera o una casilla sin mina sin descubrir
        if ((minas[f][c] && !banderas[f][c]) || (!minas[f][c] && !descubierto[f][c])) {
          return false;
        }
      }
    }
    return true;
  }

  void procesarComando(String comando) {
    comando = comando.trim();
    
    if (comando.isEmpty) {
      return;
    }
    
    // Ayuda
    if (comando.toLowerCase() == 'help' || comando.toLowerCase() == 'ayuda') {
      print('\n=== COMANDOS DISPONIBLES ===');
      print('• Seleccionar casilla: Letra + Número (ej: B2, D5)');
      print('• Poner/Quitar bandera: Casilla + "flag" o "bandera" (ej: E1 flag)');
      print('• Mostrar trucos: "cheat" o "trampas"');
      print('• Ayuda: "help" o "ayuda"');
      return;
    }
    
    // Trucos
    if (comando.toLowerCase() == 'cheat' || comando.toLowerCase() == 'trampas') {
      mostrarTrucos = !mostrarTrucos;
      if (mostrarTrucos) {
        mostrarAmbosTableros();
      } else {
        mostrarTablero();
      }
      return;
    }
    
    // Verificar si es una casilla con bandera
    var partes = comando.split(' ');
    if (partes.length == 2) {
      String posicion = partes[0];
      String accion = partes[1].toLowerCase();
      
      if (posicion.length >= 2) {
        String letraFila = posicion[0].toUpperCase();
        String numColumna = posicion.substring(1);
        
        int fila = letraFila.codeUnitAt(0) - 'A'.codeUnitAt(0);
        
        if (int.tryParse(numColumna) != null) {
          int columna = int.parse(numColumna);
          
          if (fila >= 0 && fila < filas && columna >= 0 && columna < columnas) {
            if (accion == 'flag' || accion == 'bandera') {
              toggleBandera(fila, columna);
              if (mostrarTrucos) {
                mostrarAmbosTableros();
              } else {
                mostrarTablero();
              }
              return;
            }
          }
        }
      }
    }
    
    // Si es solo una posición, destapar
    if (partes.length == 1 && partes[0].length >= 2) {
      String posicion = partes[0];
      String letraFila = posicion[0].toUpperCase();
      String numColumna = posicion.substring(1);
      
      int fila = letraFila.codeUnitAt(0) - 'A'.codeUnitAt(0);
      
      if (int.tryParse(numColumna) != null) {
        int columna = int.parse(numColumna);
        
        if (fila >= 0 && fila < filas && columna >= 0 && columna < columnas) {
          // Verificar si es bandera
          if (banderas[fila][columna]) {
            // Si es bandera, destapar (quitar bandera y destapar)
            banderas[fila][columna] = false;
            tiradas++;
            bool explosion = destaparCasilla(fila, columna, tiradas == 1, true);
            
            if (explosion) {
              gameOver = true;
              mostrarTableroCompleto();
              print('\n¡Has perdido!');
              print('Número de tiradas: $tiradas');
            } else {
              if (verificarVictoria()) {
                victoria = true;
                mostrarTablero();
                print('\n¡Has ganado!');
                print('Número de tiradas: $tiradas');
              } else if (mostrarTrucos) {
                mostrarAmbosTableros();
              } else {
                mostrarTablero();
              }
            }
          } else {
            // Destapar normal
            tiradas++;
            bool explosion = destaparCasilla(fila, columna, tiradas == 1, true);
            
            if (explosion) {
              gameOver = true;
              mostrarTableroCompleto();
              print('\n¡Has perdido!');
              print('Número de tiradas: $tiradas');
            } else {
              if (verificarVictoria()) {
                victoria = true;
                mostrarTablero();
                print('\n¡Has ganado!');
                print('Número de tiradas: $tiradas');
              } else if (mostrarTrucos) {
                mostrarAmbosTableros();
              } else {
                mostrarTablero();
              }
            }
          }
          return;
        }
      }
    }
    
    print('Comando no reconocido. Escribe "help" para ver los comandos disponibles.');
  }

  void jugar() {
    print('=== BUSCAMINAS ===');
    print('Tablero: 6 filas (A-F) x 10 columnas (0-9)');
    print('8 minas colocadas aleatoriamente');
    print('Escribe "help" para ver los comandos\n');
    
    mostrarTablero();
    
    while (!gameOver && !victoria) {
      stdout.write('\nEscriu una comanda: ');
      String comando = stdin.readLineSync() ?? '';
      procesarComando(comando);
    }
  }
}

void main() {
  Buscaminas juego = Buscaminas();
  juego.jugar();
}