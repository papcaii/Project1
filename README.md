# Project 1

## Table of Contents
1. [Description](#description)
2. [Requirements](#requirements)
3. [Technology](#technology)
4. [Setup Instructions](#setup-instructions)
    - [Set up Database](#set-up-database)
    - [Running the Application](#running-the-application)
5. [Screenshots](#screenshots)
6. [Contributors](#contributors)
7. [Progress](#progress)
8. [Troubleshooting](#troubleshooting)

## Description
A JavaFX server-client chat application integrated with a MySQL database.

## Requirements
Please refer to the [Requirement](Requirement.MD) document.

## Technology
- **Language**: `Java`
- **GUI**: `JavaFX`
- **Database**: `Docker` + `MySQL`
- **Build Tool**: `Maven`
- **Public Localhost Port**: `Ngrok`

## Setup Instructions

### Set up Database
1. **Using Docker Compose**:
    - Ensure Docker and Docker Compose are installed on your machine.
    - Run the following command in the directory containing the `docker-compose.yml` file:
    ```bash
    docker-compose up -d
    ```
    - This will:
        - Create a MySQL server accessible at `localhost:3307`.
        - Host Adminer (a database manager) at `localhost:8081`.

2. **Expose the Port with Ngrok**:
    - Install Ngrok from [here](https://ngrok.com/download).
    - Run the following command to expose the server port:
    ```bash
    ngrok tcp 9001
    ```

### Running the Application

#### Server
```bash
cd server
mvn clean package exec:java
```
#### Client
```bash
cd client
mvn clean javafx:run
```

## Screenshot

## Contributor
| Student ID  | Student Name |
| ------------- | ------------- |
| 20225545 | Nguyễn Quang Hưng  |
| 20225544  | Hạ Nhật Duy  |

## Progress

For now our progress will store in this [Docs](https://docs.google.com/document/d/11w4li3BwzRBDgchZ0EQYa_aQc2JgQEDFQK2WTC5q-9c/edit?usp=sharing) and [Overleaf](https://www.overleaf.com/project/6623cdf4acf3f13eca3c4486)

Well, it will be deleted when this project is done

## Troubleshooting
