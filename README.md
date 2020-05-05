# sphairas-server
This is the server and web frontend for sphairas. It is a Java application deployed in [Payara Server](https://www.payara.fish/) and running in a Docker container. 

To compile this project and build the Docker image you need build the [desktop client](https://github.com/sphairas/sphairas-desktop) first. Building the client will install the required shared libraries in your local maven repository. [iserv-login](https://github.com/sphairas/iserv-login) is also required. To build this project run `mvn` and `docker build`. 
