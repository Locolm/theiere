#include <OneWire.h>
#include <DallasTemperature.h>

const int relayPin = A0;  // Connecter à A0 = 26 // Broche pour le relais (théière)
const int speakerPin = A2; // Broche pour le buzzer SDA
// Broche de connexion du capteur DS18B20 à l'ESP32
const int oneWireBus = 22; //SDL
// Définition des broches de contrôle du moteur
const int AIN1 = 14;  // D2 
const int AIN2 = 32;  // D3
const int BIN1 = 15;  // D4
const int BIN2 = 33;  // D5

// Fréquences des notes musicales (en Hz)
#define NOTE_C4  261
#define NOTE_D4  294
#define NOTE_E4  329
#define NOTE_F4  349
#define NOTE_G4  392
#define NOTE_A4  440
#define NOTE_B4  493
#define NOTE_C5  523


// Création d'un objet OneWire pour la communication
OneWire oneWire(oneWireBus);

// Création d'un objet DallasTemperature pour gérer le DS18B20
DallasTemperature sensors(&oneWire);

// Tableau pour définir les séquences des bobines pour les 4 phases
int stepSequence[4][4] = {
  {HIGH, LOW, HIGH, LOW},   // Séquence 1
  {LOW, HIGH, HIGH, LOW},   // Séquence 2
  {LOW, HIGH, LOW, HIGH},   // Séquence 3
  {HIGH, LOW, LOW, HIGH}    // Séquence 4
};

// Variables pour contrôler le moteur
int currentStep = 0;  // Étape actuelle du moteur
int stepDelay = 10;   // Délai entre chaque étape en millisecondes

void setup() {

  Serial.begin(115200);

  // Initialisation de la broche du relais comme sortie
  pinMode(relayPin, OUTPUT);
  pinMode(speakerPin, OUTPUT);

  // Configuration des broches comme sorties
  pinMode(AIN1, OUTPUT);
  pinMode(AIN2, OUTPUT);
  pinMode(BIN1, OUTPUT);
  pinMode(BIN2, OUTPUT);
  
  // Initialiser le relais en position éteinte
  digitalWrite(relayPin, LOW);
  digitalWrite(speakerPin, LOW);
  // Démarrer toutes les broches à LOW (désactivé)
  disableMotor();

  // Initialisation du capteur DS18B20
  sensors.begin();

  Serial.println("Système prêt");
}

void loop() {

  // Demande au capteur de lire la température
  sensors.requestTemperatures();
  float temperatureC = sensors.getTempCByIndex(0); 
  Serial.print("Temperature: ");
  Serial.print(temperatureC);
  Serial.println(" °C");

  // Allumer le relais (activer la théière)
  Serial.println("Allumage de la théière");
  digitalWrite(relayPin, HIGH);  // Activer le relais (allumer la théière)

  // Attendre 1 minute (60 000 millisecondes)
  delay(10000);

  // Éteindre le relais (éteindre la théière)
  Serial.println("Extinction de la théière");
  digitalWrite(relayPin, LOW);   // Désactiver le relais (éteindre la théière)

  // Jouer une mélodie via le buzzer
  Serial.println("Lecture de la mélodie");
  digitalWrite(speakerPin, HIGH);
  playSound();
  delay(10000);
  Serial.println("Désactivation du speaker");
  digitalWrite(speakerPin, LOW);

  // Faire monter et descendre le filtre plusieurs fois
  for (int cycle = 0; cycle < 13; cycle++) {
    Serial.print("Cycle Descente ");
    Serial.print(cycle + 1);

    // Descente du filtre
    Serial.println(" Descente du filtre...");
    moveMotor(1, 200); // 200 pas dans le sens horaire (Descente)

    disableMotor();
    delay(1000); // Pause après la montée

  }

  for (int cycle = 0; cycle < 13; cycle++) {
    Serial.print("Cycle Montée ");
    Serial.print(cycle + 1);

    // Montée du filtre
    Serial.println(" Montée du filtre...");
    moveMotor(-1, 200); // 200 pas dans le sens anti-horaire (descente)
    delay(1000); // Pause après la descente

    disableMotor();
    delay(1000); // Pause après la montée
  }

}

// Fonction pour déplacer le moteur
void moveMotor(int direction, int steps) {
  for (int i = 0; i < steps; i++) {
    stepMotor(direction);
    delay(stepDelay);
  }
}

// Fonction pour faire avancer le moteur d'un pas dans une direction donnée
void stepMotor(int direction) {
  // Mise à jour de l'étape actuelle
  currentStep += direction;

  // Limiter l'étape actuelle pour qu'elle reste entre 0 et 3
  if (currentStep > 3) currentStep = 0;
  if (currentStep < 0) currentStep = 3;

  // Appliquer la séquence de la phase à chaque bobine
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

void playNote(int frequency, int duration) {
  if (frequency == 0) {
    // Pause
    delay(duration);
  } else {
    
    ledcWriteTone(speakerPin, frequency); // Jouer la note
    ledcWrite(speakerPin, 500); // Ajuster le volume 

    delay(duration);
    
    ledcWriteTone(speakerPin, 0);// Arrêter la note
  }
}

// Fonction pour jouer une petite symphonie
void playSound() {
  ledcAttach(speakerPin, 2000, 12); // Configurer le canal LEDC pour le speaker

  int maxVolume = 255;
  // Jouer une petite mélodie
  playNote(NOTE_C4, 500);  // Do (C4) pendant 500ms
  playNote(NOTE_D4, 500);  // Ré (D4) pendant 500ms
  playNote(NOTE_E4, 500);  // Mi (E4) pendant 500ms
  playNote(NOTE_F4, 500);  // Fa (F4) pendant 500ms
  playNote(NOTE_G4, 500);  // Sol (G4) pendant 500ms
  playNote(NOTE_A4, 500);  // La (A4) pendant 500ms
  playNote(NOTE_B4, 500);  // Si (B4) pendant 500ms
  playNote(NOTE_C5, 1000); // Do (C5) pendant 1 seconde

  delay(1000);
}
