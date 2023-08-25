# Dockerized Sigma/SUMO/Vampire

## Installation

1. Ensure that Docker is properly setup on your machine
2. Add any .kif files to the kb directory you want to be part of your installation
3. Update config.xml to point to any .kif files you want to be part of your installation
   1. NOTE: By default, only tinySUMO.kif and itm.kif are active
4. From this directory, build the sigma image
   * docker build -t sigma .
   * NOTE: This can take 5-10 minutes
5. Run a container with this new image, with ports exposed
   * docker run -p 8080:8080 --name itm-sigma sigma
   * NOTE: This will spawn a docker container with the image sigma, with the container name itm-sigma, and exposing the port 8080. You can change the port mapping if 8080 is already in use

## Using Sigma/SUMO

* Web Interface
  * The default web interface of Sigma can be reached at http://localhost:8080/sigma/login.html
