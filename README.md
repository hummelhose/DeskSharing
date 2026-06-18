# DeskSharing

## Deutsch

DeskSharing ist eine Webanwendung zur Verwaltung und Reservierung von Büroarbeitsplätzen.

Die Anwendung ermöglicht es Benutzern, Arbeitsplätze über visuelle Raumpläne zu reservieren. Administratoren können Büros, Räume, Arbeitsplätze, Benutzer und Reservierungen verwalten.

## Funktionen

* Arbeitsplatzreservierung über visuelle Raumpläne
* Verwaltung von Büros und Räumen
* Platzierung und Bearbeitung von Arbeitsplätzen
* Benutzer- und Admin-Rollen
* Microsoft Entra Login
* PostgreSQL-Datenbank
* Docker-Compose-Deployment
* Spring-Boot-Backend
* Vaadin-Weboberfläche

## Technologien

* Java 21
* Spring Boot
* Vaadin
* PostgreSQL
* Docker
* Microsoft Entra ID

## Deployment

Die Anleitung für das Docker-Deployment befindet sich in:

[DEPLOYMENT.md](DEPLOYMENT.md)

## Konfiguration

Die Beispiel-Konfiguration befindet sich in:

```bash
.env.example
```

Für den Betrieb muss daraus eine echte `.env` Datei erstellt werden.

```bash
cp .env.example .env
```

Danach müssen die Werte in der `.env` Datei angepasst werden.

---

# English

DeskSharing is a web application for managing and reserving office desks.

The application allows users to reserve desks using visual floor plans. Administrators can manage offices, rooms, desks, users, and reservations.

## Features

* Desk reservation via visual floor plans
* Office and room management
* Desk placement and editing
* User and admin roles
* Microsoft Entra login
* PostgreSQL database
* Docker Compose deployment
* Spring Boot backend
* Vaadin web UI

## Tech Stack

* Java 21
* Spring Boot
* Vaadin
* PostgreSQL
* Docker
* Microsoft Entra ID

## Deployment

Docker deployment instructions are available in:

[DEPLOYMENT.md](DEPLOYMENT.md)

## Configuration

The example configuration file is:

```bash
.env.example
```

For deployment, copy it to a real `.env` file.

```bash
cp .env.example .env
```

Then adjust the values inside the `.env` file.

## License

The license is defined in the `LICENSE` file.
