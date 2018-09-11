#!/bin/sh
TMPFILE=`mktemp`

# Generate the private and public keys
ethkey generate random > $TMPFILE
 
cat $TMPFILE | grep public | awk {'print $2'} > $1/key.pub
cat $TMPFILE | grep secret | awk {'print $2'} > $1/key.priv

rm -rf $TMPFILE


