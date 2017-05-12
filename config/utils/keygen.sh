#!/bin/sh
#
# Taken from: https://kobl.one/blog/create-full-ethereum-keypair-and-address/
# Create a secp256k1 ec keypair

TMPFILE=`mktemp`

# Generate the private and public keys
openssl ecparam -name secp256k1 -genkey -noout |   openssl ec -text -noout > $TMPFILE
 

# Extract the public key and remove the EC prefix 0x04
cat $TMPFILE | grep pub -A 5 | tail -n +2 | tr -d '\n[:space:]:' | sed 's/^04//' > $1/key.pub

# Extract the private key and remove the leading zero byte
cat $TMPFILE | grep priv -A 3 | tail -n +2 | tr -d '\n[:space:]:' | sed 's/^00//' > $1/key.priv
 
rm -rf $TMPFILE


