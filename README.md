# About

Redirect (proxy) sockets over an encrypted bridge.

To quickly understand the purpose, imagine you have 2 computers:

* C1 has a mysql database on port 3306
* C2 has a web application that would like to use the mysql database on C1 without hardcoding the hostname.
* C2 can have a software that opens the port 3306 and redirect to C1:3306
  * You could use HAProxy and use ssl certificates to encrypt the data in transit.
  * If you don't want have ssl certificates enabled or you want to access any service that do not use encryption, you can use this redirect bridge:
    * Start an *entry* bridge on C2 and an *exit* bridge on C1
  
# Features

* Encrypts traffic that goes through the bridge.
* The *entry* side has a registry files that specify where each connections goes and it can be on different *exits* machines.
* The registry file can be changed without needing to restart the application. 
  * The application with reload the registry 2 seconds after the last write or max 10 seconds after the first write.
  * When reloading, only the changes will be applied (e.g new ports open ; old ports close ; not changing ports stay open without closing)

# Usage

## Entry side

* --caCertsFile : A JSON file that contains a list of CA certificates and direct certificates that you trust. You will be able to connect to *exits* that are these certificates or signed by these certificates.
* --bridgeCertFile : The file containing the certificate for this node.
* --bridgePrivateKeyFile : The file containing the private key for this certificate.
* --entryBridgeRegistryFile : The file containing the registry of all ports to listen on and redirect.

## Exit side

* --caCertsFile : A JSON file that contains a list of CA certificates and direct certificates that you trust. You will be able to accept connections from *entries* that are these certificates or signed by these certificates.
* --bridgeCertFile : The file containing the certificate for this node.
* --bridgePrivateKeyFile : The file containing the private key for this certificate.
* --bridgePort : The port to listen on for the *entry* side.
* --exitBridgeRegistryFile : The file containing the registry of all ports to listen on and redirect.

## CA Certs File

Simply a list of String in JSON style. Here is a partial one:

```json
[ "-----BEGIN CERTIFICATE-----\nMIIEnTCCA [...] tedxJdXF9iWlp6iuNJZaDncasPo7K3Tj7\n-----END CERTIFICATE-----\n" ]
```

## Entry Bridge Registry File

```json
{
  "entries" : [ {
    "entryRawPort" : 3306,
    "remoteBridgeHost" : "mysql.example.com",
    "remoteBridgePort" : 11000,
    "remoteServiceName" : "MYSQL",
    "remoteServiceEndpoint" : "TCP"
  } ]
}
```

* entryRawPort: It is the port where your client would connect to (in this case, you could use `mysql -h 127.0.0.1` and that would connect on the mysql server that is on *mysql.example.com*)
* remoteBridgeHost: This is the host where you are running the exit node (in this case, mysql.example.com)
* remoteBridgePort: This is the port where you are running the exit node (in this case, 11000)
* remoteServiceName and remoteServiceEndpoint: This is any unique combination that you want. As long as they are known from both sides, anything is valid.

## Exit Bridge Registry File

```json
{
  "exits" : [ {
    "serviceName" : "MYSQL",
    "serviceEndpoint" : "TCP",
    "exitRawHost" : "127.0.0.1",
    "exitRawPort" : 3306
  } ]
}
```

* serviceName and serviceEndpoint: This is any unique combination that you want. As long as they are known from both sides, anything is valid.
* exitRawHost and exitRawPort: This is where you want to redirect the traffic to (in this case, the default mysql port on the local host)

## Launch the application for testing on machine

This example will redirect the local port 10800 to www.google.com:80

```bash
# Compile
./gradlew build

# Tmp dir
TMPDIR=$(mktemp -d)

# Unzip the app
cp build/distributions/redirectport-registry-master-SNAPSHOT.zip $TMPDIR
pushd $TMPDIR
unzip redirectport-registry-master-SNAPSHOT.zip
cd redirectport-registry-master-SNAPSHOT/

# Create sample data
mkdir data
./bin/redirectport-registry --createSample \
  --caCertsFile data/ca-cert.json \
  --bridgeCertFile data/node-cert \
  --bridgePrivateKeyFile data/node-key \
  --bridgePort 11000 \
  --entryBridgeRegistryFile data/entry.json \
  --exitBridgeRegistryFile data/exit.json

# Start the 2 sides
screen
./bin/redirectport-registry \
  --caCertsFile data/ca-cert.json \
  --bridgeCertFile data/node-cert-entry \
  --bridgePrivateKeyFile data/node-key-entry \
  --entryBridgeRegistryFile data/entry.json

# Ctrl+A ; C  (to create a new screen session)
./bin/redirectport-registry \
  --caCertsFile data/ca-cert.json \
  --bridgeCertFile data/node-cert-exit \
  --bridgePrivateKeyFile data/node-key-exit \
  --bridgePort 11000 \
  --exitBridgeRegistryFile data/exit.json


# Ctrl+A ; C  (to create a new screen session)
curl http://127.0.0.1:10800

# That will show you a document that says to redirect to http://www.google.com:10800/ . Of course, that won't work to go there since that is the wrong port, but
# for this demo, that is fine.

exit # To quit the "curl" screen

# You now see the output of the "exit". You can see what happened

# Ctrl+C (to exit the application)
exit # To quit the "exit side" screen

# You now see the output of the "entry". You can see what happened

# Ctrl+C (to exit the application)
exit # To quit the "entry side" screen


popd

```

## Launch the application for testing in Docker (locally)


```bash
# Compile and create image
./create-local-release.sh

# Tmp dir
TMPDIR=$(mktemp -d)

# Create sample data
docker run -ti \
  --rm \
  --volume $TMPDIR:/data \
  redirectport-registry:master-SNAPSHOT \
  --createSample \
  --caCertsFile /data/ca-cert.json \
  --bridgeCertFile /data/node-cert \
  --bridgePrivateKeyFile /data/node-key \
  --bridgePort 11000 \
  --entryBridgeRegistryFile /data/entry.json \
  --exitBridgeRegistryFile /data/exit.json
  
sudo chmod 666 $TMPDIR/*

# Run mariadb
MYSQL_ROOT_PASS=qwerty
docker run \
  --rm \
  --name redirect_mariadb \
  --env MYSQL_ROOT_PASSWORD=$MYSQL_ROOT_PASS \
  --detach \
  mariadb:10.2.8

# Create database and user
sleep 20s
docker run -i \
  --link redirect_mariadb:mysql \
  --rm \
  mariadb:10.2.8 \
  sh -c 'exec mysql -h"$MYSQL_PORT_3306_TCP_ADDR" -P"$MYSQL_PORT_3306_TCP_PORT" -uroot -p"$MYSQL_ENV_MYSQL_ROOT_PASSWORD"' << _EOF
CREATE DATABASE redirect;
GRANT ALL ON redirect.* TO 'redirect'@'172.17.0.%' IDENTIFIED BY 'fjGu38d!f';
_EOF

MYSQL_IP=$(docker inspect redirect_mariadb | grep '"IPAddress"' | head -n 1 | cut -d '"' -f 4)

# Run exit
cat > $TMPDIR/exit.json << _EOF
{
  "exits" : [ {
    "serviceName" : "MYSQL",
    "serviceEndpoint" : "TCP",
    "exitRawHost" : "$MYSQL_IP",
    "exitRawPort" : 3306
  } ]
}
_EOF

docker run \
  --rm \
  --volume $TMPDIR:/data \
  --name redirect_exit \
  --detach \
  redirectport-registry:master-SNAPSHOT \
  --caCertsFile /data/ca-cert.json \
  --bridgeCertFile /data/node-cert-exit \
  --bridgePrivateKeyFile /data/node-key-exit \
  --bridgePort 11000 \
  --exitBridgeRegistryFile /data/exit.json
  
EXIT_IP=$(docker inspect redirect_exit | grep '"IPAddress"' | head -n 1 | cut -d '"' -f 4)

# Run entry
cat > $TMPDIR/entry.json << _EOF
{
  "entries" : [ {
    "entryRawPort" : 3306,
    "remoteBridgeHost" : "$EXIT_IP",
    "remoteBridgePort" : 11000,
    "remoteServiceName" : "MYSQL",
    "remoteServiceEndpoint" : "TCP"
  } ]
}
_EOF

docker run \
  --rm \
  --volume $TMPDIR:/data \
  --name redirect_entry \
  --detach \
  redirectport-registry:master-SNAPSHOT \
  --caCertsFile /data/ca-cert.json \
  --bridgeCertFile /data/node-cert-entry \
  --bridgePrivateKeyFile /data/node-key-entry \
  --entryBridgeRegistryFile /data/entry.json

ENTRY_IP=$(docker inspect redirect_entry | grep '"IPAddress"' | head -n 1 | cut -d '"' -f 4)

# Mysql to entry
docker run -i \
  --env MYSQL_PORT_3306_TCP_ADDR=$ENTRY_IP \
  --env MYSQL_PORT_3306_TCP_PORT=3306 \
  --rm \
  mariadb:10.2.8 \
  sh -c 'exec mysql -h"$MYSQL_PORT_3306_TCP_ADDR" -P"$MYSQL_PORT_3306_TCP_PORT" -uredirect -p"fjGu38d!f" redirect' << _EOF
SHOW DATABASES;
_EOF

# See that it went through
docker logs redirect_exit

# Stop everything
docker stop redirect_entry redirect_exit redirect_mariadb

```

## Launch the application for testing in Docker (from the Hub)


```bash
# Tmp dir
TMPDIR=$(mktemp -d)

# Create sample data
docker run -ti \
  --rm \
  --volume $TMPDIR:/data \
  foilen/redirectport-registry \
  --createSample \
  --caCertsFile /data/ca-cert.json \
  --bridgeCertFile /data/node-cert \
  --bridgePrivateKeyFile /data/node-key \
  --bridgePort 11000 \
  --entryBridgeRegistryFile /data/entry.json \
  --exitBridgeRegistryFile /data/exit.json
  
sudo chmod 666 $TMPDIR/*

# Run mariadb
MYSQL_ROOT_PASS=qwerty
docker run \
  --rm \
  --name redirect_mariadb \
  --env MYSQL_ROOT_PASSWORD=$MYSQL_ROOT_PASS \
  --detach \
  mariadb:10.2.8

# Create database and user
sleep 20s
docker run -i \
  --link redirect_mariadb:mysql \
  --rm \
  mariadb:10.2.8 \
  sh -c 'exec mysql -h"$MYSQL_PORT_3306_TCP_ADDR" -P"$MYSQL_PORT_3306_TCP_PORT" -uroot -p"$MYSQL_ENV_MYSQL_ROOT_PASSWORD"' << _EOF
CREATE DATABASE redirect;
GRANT ALL ON redirect.* TO 'redirect'@'172.17.0.%' IDENTIFIED BY 'fjGu38d!f';
_EOF

MYSQL_IP=$(docker inspect redirect_mariadb | grep '"IPAddress"' | head -n 1 | cut -d '"' -f 4)

# Run exit
cat > $TMPDIR/exit.json << _EOF
{
  "exits" : [ {
    "serviceName" : "MYSQL",
    "serviceEndpoint" : "TCP",
    "exitRawHost" : "$MYSQL_IP",
    "exitRawPort" : 3306
  } ]
}
_EOF

docker run \
  --rm \
  --volume $TMPDIR:/data \
  --name redirect_exit \
  --detach \
  foilen/redirectport-registry \
  --caCertsFile /data/ca-cert.json \
  --bridgeCertFile /data/node-cert-exit \
  --bridgePrivateKeyFile /data/node-key-exit \
  --bridgePort 11000 \
  --exitBridgeRegistryFile /data/exit.json
  
EXIT_IP=$(docker inspect redirect_exit | grep '"IPAddress"' | head -n 1 | cut -d '"' -f 4)

# Run entry
cat > $TMPDIR/entry.json << _EOF
{
  "entries" : [ {
    "entryRawPort" : 3306,
    "remoteBridgeHost" : "$EXIT_IP",
    "remoteBridgePort" : 11000,
    "remoteServiceName" : "MYSQL",
    "remoteServiceEndpoint" : "TCP"
  } ]
}
_EOF

docker run \
  --rm \
  --volume $TMPDIR:/data \
  --name redirect_entry \
  --detach \
  foilen/redirectport-registry \
  --caCertsFile /data/ca-cert.json \
  --bridgeCertFile /data/node-cert-entry \
  --bridgePrivateKeyFile /data/node-key-entry \
  --entryBridgeRegistryFile /data/entry.json

ENTRY_IP=$(docker inspect redirect_entry | grep '"IPAddress"' | head -n 1 | cut -d '"' -f 4)

# Mysql to entry
docker run -i \
  --env MYSQL_PORT_3306_TCP_ADDR=$ENTRY_IP \
  --env MYSQL_PORT_3306_TCP_PORT=3306 \
  --rm \
  mariadb:10.2.8 \
  sh -c 'exec mysql -h"$MYSQL_PORT_3306_TCP_ADDR" -P"$MYSQL_PORT_3306_TCP_PORT" -uredirect -p"fjGu38d!f" redirect' << _EOF
SHOW DATABASES;
_EOF

# See that it went through
docker logs redirect_exit

# Stop everything
docker stop redirect_entry redirect_exit redirect_mariadb
```
