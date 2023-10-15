# Scala Developer Job Application - Project README

## Getting Started

To run this project and review my submission, follow these steps:

### Prerequisites

Before you begin, ensure you have the following prerequisites installed on your system:

- [Scala](https://www.scala-lang.org/) (version >= 2.12)
- [SBT (Scala Build Tool)](https://www.scala-sbt.org/) (version >= 1.9.6)
- [Java Development Kit (JDK)] (version >= 1.8.0)

### Installation

1. Clone this repository to your local machine:

   ```bash
   git clone https://github.com/dosht/fadi.git
   ```

2. Change into the project directory:

   ```bash
   cd fadi
   ```

### Running the Project

1. Run the application server

   ```bash
   sbt run
   ```
   
2. Send requests from another terminal.

   ```bash
   curl -i localhost:8080/evaluation\?url\=https://fid-recruiting.s3-eu-west-1.amazonaws.com/politics.csv
   ```


## Code Structure

- `Main.scala`: The starting point of the application based on `IOApp` from `cats effect`.
- `FadiServer.scala`: Includes the app server configuration and allows the server to listen for requests.
- `FadiRoutes.scala`: Responsible for defining routes, query parameters, and HTTP responses.
- `Evaluations.scala`: Contains the business logic, serialisation and deserialization, and error handling.

## Solution Explanation

The solution depends on `http4s`, `cats`, and `cats effect`. The most important function is `evaluate` that converts a list of URLs into the final result.
The logic is divided into 3 steps: parsing the CSV, then evaluate each line and lastly combine the evaluations into the final result.

The expected errors are either invalid URLs, missing CSVs, or malformed CSVs. These error are considered as client errors, so they are mapped into HTTP 400 with a json response explaining the error.

The solution is based on http4s Giter8 Template https://http4s.org/v0.23/docs/quickstart.html
