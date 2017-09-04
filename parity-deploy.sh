#!/bin/bash
# Copyright 2017 Parity Technologies (UK) Ltd.
CHAIN_NAME="parity"
CHAIN_NODES="1"
CLIENT="0"
DOCKER_INCLUDE="include/docker-compose.yml"


help()  { 

echo "parity-deploy.sh OPTIONS
Usage:
REQUIRED:
	--config dev / aura / tendermint / validatorset / input.json

OPTIONAL:
	--name name_of_chain. Default: parity
	--nodes number_of_nodes (if using aura / tendermint) Default: 2
	--ethstats - Enable ethstats monitoring of authority nodes. Default: Off
  --customchain - Build configuration using custom chain toml file. Default: Off
	--expose - Expose a specific container on ports 8180 / 8545 / 30303. Default: Config specific
NOTE:
    Custom spec files can be inserted by specifiying the path to the json file. 
"

}

check_packages() {

if [ $(grep -i debian /etc/*-release | wc -l) -gt 0 ] ; then
   if [ ! -f /usr/bin/docker ] ; then 
      sudo apt-get -y install docker.io python-pip 
   fi

   if [ ! -f /usr/local/bin/docker-compose ] ; then 
      sudo pip install docker-compose
   fi
fi
}


genpw() {

openssl rand -base64 12

}


create_node_params() {

if [ ! -d deployment/$1 ] ; then 
   mkdir -p deployment/$1
fi

genpw > deployment/$1/password
./config/utils/keygen.sh deployment/$1
sed -i "s/CHAIN_NAME/$CHAIN_NAME/g" config/spec/example.spec
parity account new --chain config/spec/example.spec --password deployment/$1/password --keys-path deployment/$1/ > deployment/$1/address.txt
sed -i "s/$CHAIN_NAME/CHAIN_NAME/g" config/spec/example.spec
echo "NETWORK_NAME=$CHAIN_NAME" > .env 



}

create_reserved_peers_poa() {

  PUB_KEY=`cat deployment/$1/key.pub`
  echo "enode://$PUB_KEY@host$1:30303" >> deployment/chain/reserved_peers
}


create_reserved_peers_instantseal() {

   PUB_KEY=`cat deployment/$1/key.pub`
     echo "enode://$PUB_KEY@127.0.0.1:30303" >> deployment/chain/reserved_peers

}


build_spec() {

 display_header
 display_name
 display_engine
 display_params
 display_genesis
 display_accounts
 display_footer

} 


build_docker_config_poa() { 

 echo "version: '2.0'" > docker-compose.yml
 echo "services:" >> docker-compose.yml

 for x in ` seq 1 $CHAIN_NODES ` ; do
    cat config/docker/authority.yml | sed -e "s/NODE_NAME/$x/g" >> docker-compose.yml
 done

 cat $DOCKER_INCLUDE >> docker-compose.yml
 
}

build_docker_config_ethstats() {

 if [ "$ETHSTATS" == "1" ] ; then
    cat include/ethstats.yml >> docker-compose.yml
 fi
}


build_docker_config_instantseal() {

  cat config/docker/instantseal.yml > docker-compose.yml

}


build_docker_client() { 

  if [ "$CLIENT" == "1" ]; then
    create_node_params client
    cp config/spec/client.toml deployment/client/
    cat config/docker/client.yml >> docker-compose.yml
  fi
} 


build_custom_chain() {


  if [ "$CUSTOM_CHAIN" == "" ] ; then
     echo "Must specify argument for custom chain option."
     exit 1
  fi

  ./customchain/generate.py "$CUSTOM_CHAIN"

  exit 0
}

display_header() {
 
  cat config/spec/chain_header

}

display_footer() {

  cat config/spec/chain_footer
}


display_name() {

 cat config/spec/name | sed -e "s/CHAIN_NAME/$CHAIN_NAME/g"
}


create_node_config_poa() {

  ENGINE_SIGNER=`cat deployment/$1/address.txt`
  cat config/spec/authority_round.toml | sed -e "s/ENGINE_SIGNER/$ENGINE_SIGNER/g" > deployment/$1/authority.toml
 
}

create_node_config_instantseal() {
 
  ENGINE_SIGNER=`cat deployment/$1/address.txt`
  cat config/spec/instant_seal.toml | sed -e "s/ENGINE_SIGNER/$ENGINE_SIGNER/g" > deployment/$1/authority.toml

 } 

expose_container() {

  sed -i "s@container_name: $1@&\n       - 8080:8080\n       - 8180:8180\n       - 8545:8545\n       - 30303:30303@g" docker-compose.yml

}

select_exposed_container() {

  if [ "$EXPOSE_CLIENT" != "" ] ; then
    expose_container $EXPOSE_CLIENT
  else
    if [ "$CLIENT" == "0" ] ; then
    expose_container host1
    fi
  fi

}

display_engine() {

 case $CHAIN_ENGINE in
      dev)
	cat config/spec/engine/instantseal
	;;
      aura|validatorset|tendermint)
	for x in ` seq 1 $CHAIN_NODES ` ; do
           VALIDATOR=`cat deployment/$x/address.txt`
	   RESERVED_PEERS="$RESERVED_PEERS $VALIDATOR"
	   VALIDATORS="$VALIDATORS \"$VALIDATOR\","
	done
        # Remove trailing , from validator list
        VALIDATORS=`echo $VALIDATORS | sed 's/\(.*\),.*/\1/'`
	cat config/spec/engine/$CHAIN_ENGINE | sed -e "s/0x0000000000000000000000000000000000000000/$VALIDATORS/g"
	;;
	*)
	echo "Unknown engine: $CHAIN_ENGINE"
 esac

}

display_params() {


  cat config/spec/params/$CHAIN_ENGINE

}

display_genesis() {


  cat config/spec/genesis/$CHAIN_ENGINE

}


display_accounts() {


  cat config/spec/accounts/$CHAIN_ENGINE
 
}

while [ "$1" != "" ]; do
    case $1 in
        --name)         shift
                        CHAIN_NAME=$1
                        ;;
        -c | --config)  shift
                        CHAIN_ENGINE=$1
                        ;;
        -n | --nodes )  shift
                        CHAIN_NODES=$1
                        ;;
        -r | --release) shift
                        PARITY_RELEASE=$1
                        ;;
        -e | --ethstats) shift
                         ETHSTATS=1
                         ;;
        --customchain)  shift
                        CUSTOM_CHAIN=$1
                        echo "Custom chain: $1"
                        build_custom_chain
                        ;;
        --enable-client) shift
                        CLIENT=1
                        ;;
        --expose)       shift
                        EXPOSE_CLIENT="$1"
                        ;;
        -h | --help )   help
                        exit
                        ;;
        * )             help
                        exit 1
    esac
    shift
done

if [ -z $CHAIN_ENGINE ]; then
    echo "No chain argument, exiting..."
    exit 1
fi



# Get a copy of the parity binary, overwriting if release is set

if [ ! -f /usr/bin/parity ] || [ ! "$PARITY_RELEASE" == "" ] ; then

        if [ "$PARITY_RELEASE" == "" ] ; then
                echo "NO custom parity build set, downloading beta"
                bash <(curl https://get.parity.io -Lk)
        else
                echo "Custom parity build set: $PARITY_RELEASE"
                curl -o parity-download.sh https://get.parity.io -Lk
                bash parity-download.sh -r $PARITY_RELEASE
        fi
fi


mkdir -p deployment/chain
check_packages

if [ "$CHAIN_ENGINE" == "dev" ] ; then
   echo "using instantseal"
   create_node_params is_authority
   create_reserved_peers_instantseal is_authority
   create_node_config_instantseal is_authority
   build_docker_config_instantseal

elif [ "$CHAIN_ENGINE" == "aura" ] || [ "$CHAIN_ENGINE" == "validatorset" ] || [ "$CHAIN_ENGINE" == "tendermint" ] || [ -f $CHAIN_ENGINE ] ; then
  if [ $CHAIN_NODES ] ; then
     for x in ` seq $CHAIN_NODES ` ; do
	   create_node_params $x
	   create_reserved_peers_poa $x
	   create_node_config_poa $x
     done
     build_docker_config_poa
     build_docker_client
  fi

  if [ "$CHAIN_ENGINE" == "aura" ] || [ "$CHAIN_ENGINE" == "validatorset" ] || [ "$CHAIN_ENGINE" == "tendermint" ] ; then
     build_spec > deployment/chain/spec.json
     build_docker_config_ethstats
  else
     mkdir -p deployment/chain
     cp $CHAIN_ENGINE deployment/chain/spec.json
  fi
else
	echo "Could not find spec file: $CHAIN_ENGINE"
fi

select_exposed_container


