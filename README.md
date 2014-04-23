The BitcoinServer is a standalone server written in Java. It uses Spring Boot for its http server and provides an interface to bitcoin-related functions.<br />

## Setting up the environment on Ubuntu
Install openjdk-7-jdk by running sudo apt-get install openjdk-7-jdk

TODO: Cassandra strongly recommends using Oracle jdk. Use that instead.

Install cassandra using the instructions here:

http://wiki.apache.org/cassandra/DebianPackaging


Use 20x instead of 11x to get the latest stable version of cassandra 2.0.

You can now start and stop the cassandra service by running:

sudo service cassandra [start/stop]


To verify that it is running:

sudo service cassandra status


Run the init_db.sh script to create the necessary keyspace and tables.

Now you can simply run the following to build and run the server:

./gradlew build && java -jar build/libs/BitcoinServer-0.1.0.jar
