#include <OneWire.h>
#include <DallasTemperature.h>
#include "BluetoothSerial.h"

// === Configuration des broches ===
const int relayPin = A0;    // Broche du relais (théière)
const int speakerPin = A2;  // Broche du buzzer
const int oneWireBus = 22;  // Broche du capteur DS18B20 SCL
const int AIN1 = 14;        // Contrôle du moteur
const int AIN2 = 32;
const int BIN1 = 15;
const int BIN2 = 33;

// === Fréquences des notes musicales ===
#define NOTE_C4  261
#define NOTE_D4  294
#define NOTE_E4  329
#define NOTE_F4  349
#define NOTE_G4  392
#define NOTE_A4  440
#define NOTE_B4  493
#define NOTE_C5  523

// === Température et capteur ===
OneWire oneWire(oneWireBus);
DallasTemperature sensors(&oneWire);

// === Moteur ===
int stepSequence[4][4] = {
  {HIGH, LOW, HIGH, LOW},
  {LOW, HIGH, HIGH, LOW},
  {LOW, HIGH, LOW, HIGH},
  {HIGH, LOW, LOW, HIGH}
};
int currentStep = 0;  // Étape actuelle
int stepDelay = 10;   // Délai entre étapes

// === Bluetooth ===
String device_name = "ESP32-BT-Theiere";
BluetoothSerial SerialBT;

// === Variables générales ===
unsigned long lastReceivedTime = 0; // Timestamp du dernier message reçu
unsigned long n1 = 5000;            // Intervalle pour vérifier les messages entrants (ms)
unsigned long n2 = 2000;            // Intervalle pour envoyer tempActu (ms)
unsigned long previousSendTime = 0; // Timestamp du dernier envoi de tempActu
unsigned long currentTime;

bool connectionLost = false;        // Indique si la connexion Bluetooth est perdue
int tempActu = 30;                  // Température actuelle //TODO CHANGER LA VALEUR INITIALE TEMPERATURE (mise à jour avec le capteur de température)

void setup() {
  Serial.begin(115200);
  SerialBT.begin(device_name); // Nom du périphérique Bluetooth
  Serial.printf("The device with name \"%s\" is started.\nNow you can pair it with Bluetooth!\n", device_name.c_str());

  // === Initialisation des broches ===
  pinMode(relayPin, OUTPUT);
  pinMode(speakerPin, OUTPUT);
  pinMode(AIN1, OUTPUT);
  pinMode(AIN2, OUTPUT);
  pinMode(BIN1, OUTPUT);
  pinMode(BIN2, OUTPUT);


  // === Désactiver tous les périphériques ===
  digitalWrite(relayPin, LOW);
  digitalWrite(speakerPin, LOW);
  disableMotor();

  // === Initialiser le capteur de température ===
  sensors.begin();
}

//TODO
//Commandes sous forme keyInstruction>value|keyInstruction>value exemple: temp>90 ici on a température 90 (déjà découpé comme il faut dans une fonction plus bas)
//Rajouter ici fonctions pour traiter les commandes, il faut une fonction pour récupérer la valeur de température du capteur, une pour allumer et éteindre le chauffage, une autre pour mettre le moteur en position haute ou basse
//Suivre les TODO Pour voir où il faudra utiliser ces fonctions par la suite

void loop() {
  currentTime = millis();

  // Vérifier et traiter les messages Bluetooth
  if (SerialBT.available()) {
    lastReceivedTime = currentTime;
    connectionLost = false; // Réinitialiser l'état de perte de connexion
    String receivedMessage = SerialBT.readStringUntil('\n');
    processMessage(receivedMessage);
  }

  // Détecter les pannes ou inactivité
  if (!connectionLost && (currentTime - lastReceivedTime > 2 * n1)) { // Perte de connexion détectée
    connectionLost = true;
    Serial.println("Erreur : connexion perdue. Chauffage off, up, led red");
    //TODO Remplacer Chauffage off par le branchement chauffage 
    //Remplacer up par la commande moteur pour mettre le moteur en position haute
    digitalWrite(relayPin, LOW); // Éteindre le chauffage
    moveMotor(-1, 200); // Monter le moteur
  }

  // Envoyer les données toutes les n2 secondes
  if (currentTime - previousSendTime >= n2) {
    //== capteur ==
    sensors.requestTemperatures();
    tempActu = (int)round(sensors.getTempCByIndex(0)); //sensors.getTempCByIndex(0) pour un float

    sendTempData();
    previousSendTime = currentTime;
  }

/*
  // Lire les commandes via le moniteur série
  if (Serial.available()) {
    String command = Serial.readStringUntil('\n');
    processSerialCommand(command);
  }*/

}

// Fonction pour analyser et traiter les messages reçus Bluetooth
void processMessage(String message) {
  Serial.println("Message reçu : " + message);

  int tempRecu = -1;
  String motorCommand = "";

  // Découper les informations
  String parts[2];
  int index = 0;
  while (message.length() > 0 && index < 2) {
    int separatorIndex = message.indexOf('|');
    if (separatorIndex == -1) {
      parts[index] = message;
      break;
    }
    parts[index++] = message.substring(0, separatorIndex);
    message = message.substring(separatorIndex + 1);
  }

  // Extraire les valeurs
  for (int i = 0; i < 2; i++) {
    int valueIndex = parts[i].indexOf('>');
    if (valueIndex != -1) {
      String key = parts[i].substring(0, valueIndex);
      String value = parts[i].substring(valueIndex + 1);
      if (key == "temp") tempRecu = value.toInt();
      if (key == "motor") motorCommand = value;
    }
  }

  // Traiter les commandes
  if (tempRecu != -1) {
    if (tempActu > tempRecu) {
      Serial.println("Chauffage off"); //TODO Remplacer par éteindre le chauffage
      digitalWrite(relayPin, LOW);
    } else {
      Serial.println("Chauffage on"); //TODO Remplacer par allumage du chauffage
      digitalWrite(relayPin, HIGH);
    }
  }

  if (motorCommand == "UP") {
    Serial.println("up"); //TODO Remplacer par mise en position haute du moteur
    moveMotor(-1, 200);
  } else if (motorCommand == "DOWN") {
    Serial.println("down"); //TODO Remplacer par mise en position basse du moteur
    moveMotor(1, 200);
  }
}

// Fonction pour envoyer les données actuelles
void sendTempData() {
  char buffer[50];
  sprintf(buffer, "temp>%d", tempActu);
  SerialBT.println(buffer);
  //Serial.println("Données envoyées : " + String(buffer));
}

//TODO Remplacer cette fonction par le capteur température (tempActu = température relevée avec le capteur)
// ==========================Fonction TEMPORAIRE pour traiter les commandes entrées via le moniteur série==============================
/*void processSerialCommand(String command) {
  command.trim(); // Supprimer les espaces ou caractères invisibles
  if (command.startsWith("temp>")) {
    String tempValue = command.substring(5);
    tempActu = tempValue.toInt();
    Serial.printf("Nouvelle valeur de tempActu : %d\n", tempActu);
  } else {
    Serial.println("Commande non reconnue.");
  }
}*/
//========================================================================================================================================

// === Contrôle du moteur ===
void moveMotor(int direction, int steps) {
  for (int i = 0; i < steps; i++) {
    stepMotor(direction);
    delay(stepDelay);
  }
}

void stepMotor(int direction) {
  currentStep += direction;
  if (currentStep > 3) currentStep = 0;
  if (currentStep < 0) currentStep = 3;

  digitalWrite(AIN1, stepSequence[currentStep][0]);
  digitalWrite(AIN2, stepSequence[currentStep][1]);
  digitalWrite(BIN1, stepSequence[currentStep][2]);
  digitalWrite(BIN2, stepSequence[currentStep][3]);
}

void disableMotor() {
  digitalWrite(AIN1, LOW);
  digitalWrite(AIN2, LOW);
  digitalWrite(BIN1, LOW);
  digitalWrite(BIN2, LOW);
}

// === Fonction pour jouer une mélodie ===
void playSound() {
  playNote(NOTE_C4, 500);
  playNote(NOTE_D4, 500);
  playNote(NOTE_E4, 500);
  playNote(NOTE_F4, 500);
  playNote(NOTE_G4, 500);
  playNote(NOTE_A4, 500);
  playNote(NOTE_B4, 500);
  playNote(NOTE_C5, 1000);
}

void playNote(int frequency, int duration) {
  if (frequency == 0) {
    delay(duration);
  } else {
    ledcWriteTone(speakerPin, frequency);
    delay(duration);
    ledcWriteTone(speakerPin, 0);
  }
}
