# ❄️ Scooter Central 🛵

![Java](https://img.shields.io/badge/Java-21-blue?style=for-the-badge&logo=openjdk)
![JavaFX](https://img.shields.io/badge/JavaFX-Enabled-orange?style=for-the-badge&logo=java)
![Maven](https://img.shields.io/badge/Maven-Build_Success-brightgreen?style=for-the-badge&logo=apachemaven)
![License](https://img.shields.io/badge/License-Educational-lightgrey?style=for-the-badge)
![Platform](https://img.shields.io/badge/Platform-Desktop-lightblue?style=for-the-badge&logo=windows)
[![Maven Build](https://github.com/alexwest1981/ScooterCentral/actions/workflows/maven.yml/badge.svg)](https://github.com/alexwest1981/ScooterCentral/actions/workflows/maven.yml)

---

**Scooter Central** är ett robust, **JavaFX-baserat administrationssystem** för uthyrning av snöskotrar och tillbehör.  
Applikationen hanterar hela flödet i en uthyrningsverksamhet – från lagerhantering och medlemsregister till aktiv uthyrning, prissättning och kvittohantering.

---

## 📋 Innehållsförteckning
- [✨ Funktioner](#-funktioner)
- [🏗 Teknisk Arkitektur](#-teknisk-arkitektur)
- [🚀 Installation & Körning](#-installation--körning)
- [🔐 Konfiguration & Säkerhet](#-konfiguration--säkerhet)
- [🛠 Utvecklingsprocess](#-utvecklingsprocess)
- [📄 Licens](#-licens)

---
## 🚦 Project status

Status: Active – under pågående utveckling.  
Fokus ligger på stabil uthyrningslogik, förbättrad UI-upplevelse och utökad statistik/dashboards.
---
## 🖼️ Screenshots

### Dashboard
_Realtidsvy med taxameter, nyckeltal och grafer._

![Dashboard screenshot](docs/images/dashboard.png)

### Uthyrningsflöde
_Utrustningsvy._
![Rental screenshot](docs/images/equipment.png)
---

## ✨ Funktioner

### 🖥️ Dashboard & Realtidsdata
- **Live-uppdatering:** En *taxameter*-funktion visar intäkter och kostnader för pågående uthyrningar i realtid.  
- **Visualisering:** Grafer och diagram visar intäktstrender och popularitet bland modeller.  
- **Widgets:** Snabba nyckeltal för aktiva uthyrningar, lagerstatus och dagskassa.

### 📦 Lagerhantering (Inventory)
- **Polymorfism:** Hanterar olika typer av utrustning (`Scooter`, `Sled`) med unika attribut (motorstorlek, vikt, etc.).  
- **Live Search:** Filtrera lagerlistan i realtid baserat på namn, typ eller tillgänglighet.  
- **Status:** Visuell feedback (färgkodning) för lediga vs uthyrda objekt.

### 💳 Kassa & Uthyrning (Point of Sale)
- **Flöde:** Starta uthyrning genom att koppla *Medlem + Utrustning + Prispolicy*.  
- **Prisstrategier:** Stöd för flera prismodeller via *Strategy Pattern* (t.ex. `StandardPricePolicy`, `StudentPricePolicy`).  
- **Dokument:** Generera och skriv ut professionella kvitton och fakturor direkt via JavaFX (*WYSIWYG*).

### 👥 Medlemsregister (CRM)
- **CRUD:** Skapa, läs, uppdatera och ta bort medlemmar.  
- **Historik:** Spåra tidigare uthyrningar per medlem.  
- **Validering:** Regex-baserad kontroll av telefonnummer och ID-hantering.

### 💾 Persistens & Säkerhet
- **Autosave:** En bakgrundstråd (*daemon thread*) sparar data var 30:e sekund utan att frysa gränssnittet.  
- **JSON:** All data lagras i lättlästa JSON-filer.  
- **Säkerhet:** Admin-inloggning krävs för känsliga funktioner. Lösenord hanteras externt.

---

## 🏗 Teknisk Arkitektur

Projektet följer en strikt **lagerarkitektur (Layered Architecture)** för att tydligt separera ansvar (*Separation of Concerns*).

graph TD</br>
UI[UI Layer (Views)] --> Service[Service Layer]</br>
Service --> Persistence[Persistence Layer]</br>
Service --> Model[Model Layer]</br>
Persistence --> JSON[JSON Files]</br>
</br>

- **UI Layer** (`se.scooterrental.ui`):  
  JavaFX-vyer. Använder *callbacks* för kommunikation mellan vyer (t.ex. `LoginView` → `MainApp`).

- **Service Layer** (`se.scooterrental.service`):  
  Innehåller affärslogik.  
  - `RentalService`: Koordinerar uthyrningar.  
  - `Inventory`: Hanterar lagersaldo och söklogik (Streams).  

- **Model Layer** (`se.scooterrental.model`):  
  POJO-klasser med validering. Använder arv (`Item` → `Scooter`).

- **Persistence Layer** (`se.scooterrental.persistence`):  
  Hanterar serialisering via *GSON*. Löser polymorfismproblem med anpassade adaptrar.

---
## 📁 Projektstruktur

Projektet följer en klassisk Maven-struktur för ett JavaFX-skrivbordsprojekt:</br>
ScooterCentral/</br>
├── .github/</br>
│ └── workflows/</br>
│ └── maven.yml # CI-build med Maven</br>
├── src/</br>
│ ├── main/</br>
│ │ ├── java/</br>
│ │ │ └── se/</br>
│ │ │ └── scooterrental/</br>
│ │ │ ├── ui/ # JavaFX-vyer och controllers</br>
│ │ │ ├── service/ # Affärslogik (RentalService, Inventory m.m.)</br>
│ │ │ ├── model/ # Domänmodeller (Item, Scooter, Sled, Member ...)</br>
│ │ │ └── persistence/ # GSON/JSON-hantering, adaptrar</br>
│ │ └── resources/ # FXML, CSS, ikoner, config m.m.</br>
│ └── test/</br>
│ └── java/ # Enhetstester</br>
├── README.md # Projektbeskrivning</br>
├── pom.xml # Maven-konfiguration</br>
└── config.json (genereras vid körning)</br>
</br>
> Denna struktur gör det lätt att särskilja UI, logik, modeller och persistens, samt att bygga och testa med Maven och CI.</br>

---

## 🚀 Installation & Körning

### Förutsättningar
- Java JDK 21 eller senare  
- Maven 3.8+

### Steg för steg

**Kloning**

git clone https://github.com/alexwest81/ScooterCentral.git
cd ScooterCentral


**Bygg projektet**

mvn clean install


**Kör applikationen**

mvn javafx:run


*Alternativt kan du köra `MainApp.java` direkt från din IDE (t.ex. IntelliJ eller Eclipse).*

---
## 🧭 Usage

1. Starta applikationen och logga in som admin (standardlösenord: `admin` första gången).
2. Skapa en ny medlem via CRM-vyn.
3. Lägg till en ny scooter eller sled i lagret.
4. Starta en uthyrning genom att koppla Medlem + Utrustning + Prispolicy.
5. Avsluta uthyrningen och generera kvitto/faktura via kvittofunktionen.
---

## 🔐 Konfiguration & Säkerhet

Applikationen använder en **extern konfigurationsfil** för att hantera känsliga inställningar som administratörslösenord.

### `config.json`
När applikationen startas för första gången skapas automatiskt filen `config.json` i rotkatalogen om den saknas.

**Standardinloggning för Admin:**

Lösenord: admin


För att ändra lösenordet, redigera `config.json`:

{
"adminPassword": "ditt-nya-starka-lösenord"
}


> **Notering:** Inloggningsrutan maskerar inmatningen (****) för ökad säkerhet.

---

## 🛠 Utvecklingsprocess

Projektet har utvecklats med fokus på **moderna Java-principer**:

- **Concurrency:** `AutosaveThread` använder `volatile`-variabler och `Platform.runLater` för trådsäkerhet mot UI:t.  
- **Streams API:** Används flitigt för filtrering, sökning och statistikberäkning (t.ex. `Collectors.groupingBy`).  
- **Design Patterns:**  
  - Strategy Pattern – för prisstrategier  
  - Observer Pattern – för UI-uppdateringar  
  - DTO/POJO – för datahantering  

---

## 📄 Licens

Detta projekt är skapat för **utbildningssyfte**.  
Utvecklat av **Alex Weström**

---
