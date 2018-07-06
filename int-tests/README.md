# Local Tests

The following "Local Tests" are pyunit integration tests which run their respective services under test in their own process using Gradle via python. "Local Tests" are a part of the CircleCI pipeline.

Note: [virtualenvwrapper](https://virtualenvwrapper.readthedocs.io/en/latest/) is your friend

* test_server.py


##### To run, simply invoke via python. 
Note: Any java changes will be recompiled before the tests are run.
```bash
python test_server.py
```


# Containerized Tests

The following "Containerized Tests" are integration tests meant to be run in Docker container(s). "Containerized Tests" are a part of the CI/CD pipeline run periodically on controlled hardware for benchmarking purposes.  

* proxy_load_tests.py


##### Alternatively, you can still run these tests locally with docker-compose
Note: Any java changes will be recompiled before the tests are run.
```bash
./container_tests.sh
```

# Containerized Tests (Running Manually)

```
# Build servers 
./gradlew :int-test-proxy-server:installDist && ./gradlew :int-test-backend-server:installDist

# Build docker images 
cd int-tests && docker-compose build

# Run client and and servers in docker
docker-compose run client /bin/bash

# Run tests in docker client container
python3 /tests/proxy_load_tests.py

# Run wrk, curl, h2load etc
curl -kv https://back/
curl -kv https://front/
wrk -t4 -c400 -d30s https://front/
h2load -t4 -c400 -D30 https://front/

# Exit docker client container
exit

# Stop docker containers
docker-compose stop
```




