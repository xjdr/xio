FROM debian:stretch
MAINTAINER Will Kamp <manimaul@gmail.com>
RUN rm -rf /var/lib/apt/lists/*
RUN apt-get update
RUN apt-get upgrade -y
RUN apt-get -y install sudo locales ca-certificates
RUN sudo echo "America/Los_Angeles" > /etc/timezone
RUN sudo dpkg-reconfigure -f noninteractive tzdata
RUN sudo sed -i -e 's/# en_US.UTF-8 UTF-8/en_US.UTF-8 UTF-8/' /etc/locale.gen
RUN sudo echo 'LANG="en_US.UTF-8"'>/etc/default/locale
RUN sudo dpkg-reconfigure --frontend=noninteractive locales
RUN sudo update-locale LANG=en_US.UTF-8
