# Local Tests

The following "Local Tests" are pyunit integration tests which run their respective services under test in their own process using Gradle via python. "Local Tests" are a part of the CircleCI pipeline.

Note: [virtualenvwrapper](https://virtualenvwrapper.readthedocs.io/en/latest/) is your friend

* test_server.py

To run, simply invoke via python. Any java changes will be recompiled before the tests are run.
```bash
python test_server.py
```


# Containerized Tests

The following "Containerized Tests" are integration tests meant to be run in Docker container(s). "Containerized Tests" are a part of the CI/CD pipeline run periodically on controlled hardware for benchmarking purposes.  

* proxy_load_tests.py 

To run locally invoke via docker-compose, connect to the client container containing the tests and run the tests.

#### Build the server artifacts and docker images 
```bash
./gradlew :int-test-proxy-server:installDist
./gradlew :int-test-backend-server:installDist
cd int-tests
docker-compose build
```

#### Run the tests with docker-compose
```bash
docker-compose up -d
docker exec -it inttests_client_1 /bin/bash
python3 /tests/proxy_load_tests.py
exit
docker-compose down
```
