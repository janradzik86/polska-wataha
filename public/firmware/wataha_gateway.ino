/*
 * Polska Wataha — bramka LoRa dla Heltec WiFi LoRa 32 V4
 * MCU: ESP32-S3R2   Radio: SX1262   Pasmo: EU868 (863–928 MHz)
 *
 * Arduino IDE:
 *   1. Płytka: Heltec WiFi LoRa 32(V3) / ESP32-S3 (pinout V4 = V3)
 *   2. Biblioteki: RadioLib (jgromes), U8g2
 *   3. Wgraj ten plik. Na OLED pojawi się PIN 6-cyfrowy.
 *   4. W aplikacji Polska Wataha: Radio LoRa → Skanuj → wpisz PIN z OLED.
 *
 * BLE: Nordic UART Service. Ramki JSON, jedna linia = jeden pakiet.
 * LoRa: 868.1 MHz, SF7, BW125 kHz, CR4/5, 14 dBm (zgodne z EU).
 * PIN: radio nie nada nic, dopóki telefon nie potwierdzi PIN z OLED.
 * 5 błędnych PIN-ów = blokada 5 minut.
 */

#include <Arduino.h>
#include <RadioLib.h>
#include <Wire.h>
#include <U8g2lib.h>
#include <BLEDevice.h>
#include <BLEServer.h>
#include <BLEUtils.h>
#include <BLE2902.h>
#include <esp_system.h>

#define LORA_NSS  8
#define LORA_DIO1 14
#define LORA_RST  12
#define LORA_BUSY 13
#define LORA_SCK  9
#define LORA_MOSI 10
#define LORA_MISO 11
#define FEM_PWR   7
#define FEM_EN    2
#define FEM_PA    46
#define VEXT      36
#define OLED_RST  21
#define OLED_SDA  17
#define OLED_SCL  18
#define LED_PIN   35

#define FREQ_MHZ  868.1
#define TX_DBM    14
#define PIN_MAX_FAIL 5
#define PIN_LOCK_MS  300000UL

static const char* NUS_SVC = "6e400001-b5a3-f393-e0a9-e50e24dcca9e";
static const char* NUS_RX  = "6e400002-b5a3-f393-e0a9-e50e24dcca9e";
static const char* NUS_TX  = "6e400003-b5a3-f393-e0a9-e50e24dcca9e";

SX1262 radio = new Module(LORA_NSS, LORA_DIO1, LORA_RST, LORA_BUSY);
U8G2_SSD1306_128X64_NONAME_F_HW_I2C u8g2(U8G2_R0, OLED_RST);

BLECharacteristic* txChar = nullptr;
bool bleConnected = false;
bool paired = false;
int pinFails = 0;
unsigned long pinLockUntil = 0;
String pinCode;
String nodeId;
String bleBuf;
volatile bool loraFlag = false;

void IRAM_ATTR onLora() { loraFlag = true; }

void femOn() {
  pinMode(FEM_PWR, OUTPUT); digitalWrite(FEM_PWR, HIGH);
  pinMode(FEM_EN, OUTPUT);  digitalWrite(FEM_EN, HIGH);
  pinMode(FEM_PA, OUTPUT);  digitalWrite(FEM_PA, HIGH);
  delay(5);
}

void oled(const char* a, const char* b, const char* c) {
  u8g2.clearBuffer();
  u8g2.setFont(u8g2_font_6x12_tf);
  u8g2.drawStr(0, 12, "POLSKA WATAHA");
  u8g2.drawStr(0, 28, a);
  u8g2.drawStr(0, 44, b);
  u8g2.drawStr(0, 60, c);
  u8g2.sendBuffer();
}

void bleSend(const String& line) {
  if (!bleConnected || !txChar) return;
  String s = line;
  if (!s.endsWith("\n")) s += "\n";
  txChar->setValue((uint8_t*)s.c_str(), s.length());
  txChar->notify();
}

String jsonStr(const String& line, const char* key) {
  String k = String("\"") + key + "\"";
  int i = line.indexOf(k);
  if (i < 0) return "";
  i = line.indexOf(':', i + k.length());
  if (i < 0) return "";
  i++;
  while (i < (int)line.length() && (line[i] == ' ' || line[i] == '\t')) i++;
  if (i >= (int)line.length()) return "";
  if (line[i] == '"') {
    int j = i + 1;
    while (j < (int)line.length() && line[j] != '"') j++;
    if (j >= (int)line.length()) return "";
    return line.substring(i + 1, j);
  }
  int j = i;
  while (j < (int)line.length() && line[j] != ',' && line[j] != '}' && line[j] != ' ') j++;
  return line.substring(i, j);
}

void handlePair(const String& pin) {
  if (millis() < pinLockUntil) {
    unsigned long left = (pinLockUntil - millis()) / 1000UL;
    bleSend(String("{\"v\":1,\"t\":\"paired\",\"ok\":false,\"reason\":\"locked\",\"wait\":") + left + "}");
    oled("PIN zablokowany", nodeId.c_str(), "czekaj 5 min");
    return;
  }
  if (pin.length() == 6 && pin == pinCode) {
    paired = true;
    pinFails = 0;
    bleSend(String("{\"v\":1,\"t\":\"paired\",\"ok\":true,\"dev\":\"") + nodeId + "\",\"fw\":\"0.4.0\"}");
    oled("SPAROWANO", nodeId.c_str(), ("PIN " + pinCode).c_str());
    digitalWrite(LED_PIN, HIGH);
    delay(80);
    digitalWrite(LED_PIN, LOW);
    return;
  }
  pinFails++;
  paired = false;
  if (pinFails >= PIN_MAX_FAIL) {
    pinLockUntil = millis() + PIN_LOCK_MS;
    pinFails = 0;
    bleSend("{\"v\":1,\"t\":\"paired\",\"ok\":false,\"reason\":\"locked\"}");
    oled("5 zlych PIN", "blokada 5 min", nodeId.c_str());
  } else {
    bleSend(String("{\"v\":1,\"t\":\"paired\",\"ok\":false,\"reason\":\"bad_pin\",\"left\":") + (PIN_MAX_FAIL - pinFails) + "}");
    oled("ZLY PIN", nodeId.c_str(), (String("proba ") + pinFails + "/5").c_str());
  }
}

class ServerCb : public BLEServerCallbacks {
  void onConnect(BLEServer*) override {
    bleConnected = true;
    paired = false;
  }
  void onDisconnect(BLEServer* s) override {
    bleConnected = false;
    paired = false;
    oled("LoRa OK", nodeId.c_str(), ("PIN " + pinCode).c_str());
    s->getAdvertising()->start();
  }
};

void handlePhone(const String& line);
class RxCb : public BLECharacteristicCallbacks {
  void onWrite(BLECharacteristic* ch) override {
    String v = ch->getValue().c_str();
    if (v.length() == 0 && ch->getData()) v = String((char*)ch->getData());
    bleBuf += v;
    int n;
    while ((n = bleBuf.indexOf('\n')) >= 0) {
      String line = bleBuf.substring(0, n);
      bleBuf.remove(0, n + 1);
      line.trim();
      if (line.length()) handlePhone(line);
    }
  }
};

void handlePhone(const String& line) {
  String t = jsonStr(line, "t");
  if (t.length() == 0) {
    if (line.indexOf("hello") >= 0) t = "hello";
    else if (line.indexOf("pair") >= 0) t = "pair";
    else if (line.indexOf("\"tx\"") >= 0) t = "tx";
  }

  if (t == "hello") {
    bleSend(String("{\"v\":1,\"t\":\"hello\",\"dev\":\"") + nodeId + "\",\"fw\":\"0.4.0\",\"band\":\"868.1 MHz\",\"paired\":" + (paired ? "true" : "false") + "}");
    return;
  }
  if (t == "pair") {
    handlePair(jsonStr(line, "pin"));
    return;
  }
  if (t == "ping") {
    bleSend("{\"v\":1,\"t\":\"pong\"}");
    return;
  }
  if (t == "tx") {
    if (!paired) {
      bleSend("{\"v\":1,\"t\":\"txok\",\"ok\":false,\"reason\":\"not_paired\"}");
      oled("Odrzucono TX", "najpierw PIN", nodeId.c_str());
      return;
    }
    String payload = jsonStr(line, "body");
    if (payload.length() == 0) payload = line;
    int st = radio.transmit(payload);
    bleSend(String("{\"v\":1,\"t\":\"txok\",\"ok\":") + (st == RADIOLIB_ERR_NONE ? "true" : "false") + "}");
    oled("TX LoRa", payload.c_str(), nodeId.c_str());
    radio.startReceive();
    return;
  }
}

void setup() {
  Serial.begin(115200);
  pinMode(LED_PIN, OUTPUT);
  pinMode(VEXT, OUTPUT);
  digitalWrite(VEXT, LOW);
  pinMode(OLED_RST, OUTPUT);
  digitalWrite(OLED_RST, HIGH);
  femOn();

  uint32_t chip = (uint32_t)ESP.getEfuseMac();
  char id[20];
  snprintf(id, sizeof(id), "WATAHA-%04X", (unsigned)(chip & 0xFFFF));
  nodeId = id;
  char pin[8];
  snprintf(pin, sizeof(pin), "%06lu", (unsigned long)((chip ^ 0xA5A5u) % 1000000u));
  pinCode = pin;

  Wire.begin(OLED_SDA, OLED_SCL);
  u8g2.begin();
  oled("Start radia", "EU868 SX1262", ("PIN " + pinCode).c_str());

  SPI.begin(LORA_SCK, LORA_MISO, LORA_MOSI, LORA_NSS);
  int st = radio.begin(FREQ_MHZ);
  if (st == RADIOLIB_ERR_NONE) {
    radio.setSpreadingFactor(7);
    radio.setBandwidth(125.0);
    radio.setCodingRate(5);
    radio.setOutputPower(TX_DBM);
    radio.setCRC(true);
    radio.setDio1Action(onLora);
    radio.startReceive();
  }
  oled(st == RADIOLIB_ERR_NONE ? "LoRa OK" : "LoRa BLAD", nodeId.c_str(), ("PIN " + pinCode).c_str());
  Serial.print("WATAHA node=");
  Serial.print(nodeId);
  Serial.print(" PIN=");
  Serial.println(pinCode);

  BLEDevice::init(nodeId.c_str());
  BLEServer* server = BLEDevice::createServer();
  server->setCallbacks(new ServerCb());
  BLEService* svc = server->createService(NUS_SVC);
  txChar = svc->createCharacteristic(NUS_TX, BLECharacteristic::PROPERTY_NOTIFY);
  txChar->addDescriptor(new BLE2902());
  BLECharacteristic* rx = svc->createCharacteristic(
      NUS_RX, BLECharacteristic::PROPERTY_WRITE | BLECharacteristic::PROPERTY_WRITE_NR);
  rx->setCallbacks(new RxCb());
  svc->start();
  BLEAdvertising* adv = BLEDevice::getAdvertising();
  adv->addServiceUUID(NUS_SVC);
  adv->setScanResponse(true);
  adv->start();
}

void loop() {
  if (loraFlag) {
    loraFlag = false;
    String incoming;
    int st = radio.readData(incoming);
    if (st == RADIOLIB_ERR_NONE) {
      int rssi = (int)radio.getRSSI();
      float snr = radio.getSNR();
      incoming.replace("\"", "'");
      incoming.replace("\n", " ");
      String out = "{\"v\":1,\"t\":\"rx\",\"rssi\":";
      out += rssi;
      out += ",\"snr\":";
      out += String(snr, 1);
      out += ",\"body\":\"";
      out += incoming;
      out += "\",\"ts\":";
      out += millis();
      out += "}";
      bleSend(out);
      oled("RX LoRa", incoming.c_str(), (String("RSSI ") + rssi).c_str());
      digitalWrite(LED_PIN, HIGH); delay(40); digitalWrite(LED_PIN, LOW);
    }
    radio.startReceive();
  }
  delay(5);
}
