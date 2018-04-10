FROM debian:sid

# Install packages.
RUN apt-get update  -y \
 && apt-get install -y git vim curl gnupg openjdk-9-jdk

# These volumes should be mounted on the host.
VOLUME /home/
WORKDIR /home
