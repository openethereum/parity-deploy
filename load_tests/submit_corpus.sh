#!/bin/bash
while read obj; do
  curl -H "Content-Type: application/json" -X POST localhost:8545 --data $obj
done < $1
