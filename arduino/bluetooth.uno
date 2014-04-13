#include <SoftwareSerial.h>

SoftwareSerial mySerial(10, 11); // RX, TX

void setup() {
  // Open serial communications and wait for port to open:
  Serial.begin(9600);

  // set the data rate for the SoftwareSerial port
  mySerial.begin(9600);
}

void loop() { // run over and over
  if (mySerial.available()) {
    char c = mySerial.read();
    Serial.println(c);
  }

  if (Serial.available()) {
    char out = Serial.read();
    Serial.println(out);
    mySerial.write(out);
  }
}

