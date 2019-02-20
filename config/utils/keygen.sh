#!/bin/bash
TMPFILE=`mktemp`
export PATH=$PATH:.

if [ ! $(type -P $PWD/ethkey) ];  then
    ARCH=`uname -m`
    ETHKEY_URL=`curl -sS "https://vanity-service.parity.io/parity-binaries?version=stable&format=markdown&os=linux&architecture=$ARCH" | grep ethkey | awk {'print $5'}  | cut -d"(" -f2 | cut -d")" -f1`
    wget -q $ETHKEY_URL
    chmod +x ethkey
fi

if [ ! $(type -P $PWD/ethstore) ];  then
    ARCH=`uname -m`
    ETHSTORE_URL=`curl -sS "https://vanity-service.parity.io/parity-binaries?version=stable&format=markdown&os=linux&architecture=$ARCH" | grep ethstore | awk {'print $5'}  | cut -d"(" -f2 | cut -d")" -f1`
    wget -q $ETHSTORE_URL
    chmod +x ethstore
fi


# Generate the private and public keys
./ethkey generate random > $TMPFILE

cat $TMPFILE | grep public | awk {'print $2'} > $1/key.pub
cat $TMPFILE | grep secret | awk {'print $2'} > $1/key.priv
cat $TMPFILE | grep address | awk {'print $2'} > $1/address.txt

rm -rf $TMPFILE

