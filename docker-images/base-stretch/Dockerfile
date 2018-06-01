FROM debian:stretch
MAINTAINER Will Kamp <manimaul@gmail.com>
RUN rm -rf /var/lib/apt/lists/* \
&& apt-get update \
&& apt-get upgrade -y \
&& apt-get -y install \
  sudo \
  locales \
  ca-certificates \
&& sudo echo "America/Los_Angeles" > /etc/timezone \
&& sudo dpkg-reconfigure -f noninteractive tzdata \
&& sudo sed -i -e 's/# en_US.UTF-8 UTF-8/en_US.UTF-8 UTF-8/' /etc/locale.gen \
&& sudo echo 'LANG="en_US.UTF-8"'>/etc/default/locale \
&& sudo dpkg-reconfigure --frontend=noninteractive locales \
&& sudo update-locale LANG=en_US.UTF-8
