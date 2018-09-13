#!/bin/sh

# This help script generate paswwords for nodes in a file  : password.
# You can set the number of password nodes you wanted to generate : -n | --nodes REQUIRED
# This password file is store in the given output folder  :  -o | --output. OPTIONAL. Default: deployment
# You can choose the password length : -l | --length. OPTIONAL. Default: 12
# You can force to reset the passord file if already exist in the target folder : -f | --force. OPTIONAL. Default: 0(false)

help()  {

  echo "pwdgen.sh OPTIONS
Usage:
REQUIRED:
        -n | --nodes number_of_nodes

OPTIONAL:
        -l | --length password_length. Default: 12
        -o | --output output folder. Default: deployment
        -f | --force reset passord file if exist. Default: 0(false)

        -h | --help
  "

}

check_packages() {
  OPENSSL_INSTALLED=$(which openssl | wc -l)
  if [ ! 1 -eq $OPENSSL_INSTALLED ]
  then
    echo "ERROR : openssl not installed "
    exit 1
  fi
}


genpw() {
  openssl rand -base64 ${PASSWORD_LENGTH}
}

create_node_pwd() {

  DEST_DIR_NODE=$DEST_DIR/$1
  if [ ! -d $DEST_DIR_NODE ] ; then
    mkdir -p $DEST_DIR_NODE
    if [ ! $? -eq 0 ] ; then
      echo "ERROR : cannot access output folder $DEST_DIR_NODE "
      exit 1
    fi
  fi


  if [ ! -f $DEST_DIR_NODE/password ]
  then
    echo "generate password in $DEST_DIR_NODE"
    echo ${PASSWORD_LENGTH}
    genpw > $DEST_DIR_NODE/password
  else
    echo "$DEST_DIR_NODE/password file already exist"
    if [ $FORCED -eq 1 ]
    then
      echo "forced mode. Erase current password"
      echo "generate password in $DEST_DIR_NODE"
      echo ${PASSWORD_LENGTH}
      genpw > $DEST_DIR_NODE/password
    else
      echo "no forced mode. Do nothing : do not erase current password"
    fi
  fi


}

isInteger() {
  if test ${1} -eq ${1} 2>/dev/null; then
    return 0
  fi
  return 1
}


#### MAIN

#default values
FORCED=0
CHAIN_NODES=""
PASSWORD_LENGTH=12
DEST_DIR="deployment"

ARGS="$@"

if [ $# -lt 1 ]
then
  echo "No arguments supplied"
  help
  exit 1
fi

while [ "$1" != "" ]; do
  case $1 in
    -n | --nodes )          shift
      CHAIN_NODES=$1
      ;;
    -l | --length)          shift
      PASSWORD_LENGTH=$1
      ;;
    -o | --output )         shift
      DEST_DIR="$1"
      ;;
    -f | --force)           FORCED=1
      ;;
    -h | --help )           help
      exit
      ;;
  esac
  shift
done

#check ${PASSWORD_LENGTH} integer
isInteger ${PASSWORD_LENGTH}
if [ $? -eq 1 ]
then
  echo "PASSWORD_LENGTH ${PASSWORD_LENGTH} must be an integer."
  exit 1
fi

#check mandatory
if [ -z $CHAIN_NODES ] ; then
  echo "-n | --nodes  arg is mandatory"
  help
  exit 1
fi

#check ${CHAIN_NODES} integer
isInteger ${CHAIN_NODES}
if [ $? -eq 1 ]
then
  echo "CHAIN_NODES ${CHAIN_NODES} must be an integer."
  exit 1
fi

## check permission
mkdir -p $DEST_DIR
if [ ! $? -eq 0 ] ; then
  echo "ERROR : cannot access output folder $DEST_DIR "
  exit 1
fi


check_packages
if [ ! $? -eq 0 ] ; then
  echo "ERROR : openssl lib nedded "
  exit 1
fi

if [ $CHAIN_NODES ] ; then
  for x in ` seq $CHAIN_NODES ` ; do
    create_node_pwd $x
  done
fi
