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

**[!] You have to do this if you want to run client on your own**

#### Docker
- Ensure Docker and Docker Compose are installed on your machine.
- Run the following command in the directory containing the `docker-compose.yml` file:
```bash
docker-compose up -d
```
- This will:
    - Create a MySQL server accessible at `localhost:3307`
    - Host Adminer (a database manager) at `localhost:8081`
     
- Finally, browse to `localhost:8081` (adminer)
    - Login with username and password init in the `docker-compose.yml`
    - Choose database `chat-app` then run the sql init command in the file `sql-init.txt`

### Configuration Credentials
1. Copy the sample configuration file:
    ```sh
    cp src/main/resources/config.sample.properties src/main/resources/config.properties
    ```

2. Update the `config.properties` file with your database details:
    ```properties
    DATABASE_URL=jdbc:mysql://localhost:3307/chat-app
    DATABASE_USER_NAME=your-username
    DATABASE_PASSWORD=your-password
    ```
- Replace `your-username` and `your-password` with the value you initialize in your `docker-comse.yml`

### Make your chat app public to network
**Expose the Port with Ngrok**:
    - Install Ngrok from [here](https://ngrok.com/download).
    - Run the following command to expose the server port:
    ```bash
    ngrok tcp 9001
    ```

### Running the Application

#### On Linux/macOS (Terminal)

1. Server
```bash
./server/run.sh
```
2. Client
```bash
./client/run.sh
```

#### On Windows

*Prerequisites* Make sure you have the parent folder of sh.exe (including in GitBash) in your PATH

1. Server
```bash
sh .\server\run.sh
```
2. Client
```bash
sh .\client\run.sh
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
