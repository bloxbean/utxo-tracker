## Utxo Tracker for script addresses using Yaci Store 

This is an example of utxo-tracker using [Yaci Store](https://github.com/bloxbean/yaci-store) Spring Boot Starter.

### Configuration

Update `config/application.properties` to set Cardano Network, Postgres DB connection, and 
other application specific properties like `policy_id`, `script.deploy.address` etc.

### Build and Run

#### Prerequisites

- Java 21

- Install [SDKMAN](https://sdkman.io/)

#### Steps to Install Java 21 using SDKMAN 

1. Use SDKMAN to install Java 21 
   ```bash
   sdk install java 21.0.7-tem
   ```

2. Confirm the installation and set Java 21 as the default version:
   ```bash
   sdk use java 21.0.7-tem
   sdk default java 21.0.7-tem
   ```

3. Verify that Java 21 is installed and set as default:
   ```bash
   java -version
   ```

#### Build

```bash
./mvnw clean package
```

#### Run

```
java -jar target/utxo-tracker-0.0.1-SNAPSHOT.jar
```

#### Tables

1. address : Keep track of all script addresses derived from reference script in deploy transaction.
2. address_utxo : Keep track of all utxos for tracked addresses.
3. tx_input : Spent inputs. It currently stores all inputs. We will add a filter to store only the inputs that are spent from tracked addresses.

### Swagger-UI for REST APIs

https://localhost:8080/swagger-ui/index.html

## Using Docker Compose

Change `config/application.properties` for DB to use docker service specified in the docker compose

```
docker-compose up --build
```