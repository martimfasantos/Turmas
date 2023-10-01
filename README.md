# Turmas

Turmas is a project for the Distributed Systems bachelor's course during the academic year 2021/2022 @ IST.

Authors | Github
--------|--------
Martim Santos   | https://github.com/martimfasantos
Inês Magessi    | https://github.com/inesmcm26
João Silveira   | https://github.com/jsilll

**Project Grade:** 19.03 / 20

---

## Getting Started

The overall system is made up of several modules. The main server is the _ClassServer_. The clients are the _Student_,
the _Professor_ and the _Admin_. The definition of messages and services is in the _Contract_. The future naming server
is the _NamingServer_.

See the [Project Statement](https://github.com/tecnico-distsys/Turmas) for a complete domain and system description.

---

### Prerequisites

The Project is configured with Java 17 (which is only compatible with Maven >= 3.8), but if you want to use Java 11 you
can too, just downgrade the version in the POMs.

To confirm that you have them installed and which versions they are, run in the terminal:

```s
javac -version
mvn -version
```

---

### Installation

#### To compile and install all modules:

```s
mvn clean install
```
#### To run Naming Server
```s
cd NamingServer
mvn exec:java
```
or
```s
mvn exec:java -Debug
```
or
```s
mvn exec:java -Dexec.args="-debug"
```
#### To run Class Server:
```s
mvn exec:java -Dexec.args="[localhost|IP] [PORT] [P|S] (-debug)"
```

#### To run Admin:
---
```s
cd Admin
mvn exec:java
```
#### To run Professor:
---
```s
cd Professor
mvn exec:java
```

#### To run Student:
---
```s
cd Student
mvn exec:java -Dexec.args="aluno'XXXX' [student name]"
```

## Built With

* [Maven](https://maven.apache.org/) - Build and dependency management tool;
* [gRPC](https://grpc.io/) - RPC framework.
