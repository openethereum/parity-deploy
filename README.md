# Parity Deploy 
The parity deploy script is used to generate parity deployment configurations. These can range from a single instance development node to a proof of authority network with an arbitrary amount of authority nodes.  

# Requirements

There are multiple dependencies for this script to work. Most modern operating systems should have the base of these already installed. On Ubuntu systems these will automatically be installed if not already present on the system.

Requires:
1. openssl (which supports secp256k1 curves)
2. python pip
3. docker
4. docker-compose

# CLI Usage Options

There are currently three main options which can be used with the parity-deploy tool. They are:

Required:

```--chain``` This option allows you to set the conensus engine of the chain. Currently four methods are supported:
* instantseal - Instant sealing of blocks for development mode. Expected to be run on a single node.
* aura - Authority Round consensus engine, where all the authorities take a turn being the block created, based on unix/linux epoch, so the time need to be syncronized between hosts.
* tendermint - Tendermint is another consensus engine that can be used with parity, however not as well tested as Authority Round.  
* validatorset - Validtor Set is used to transition a chain from one validator to another at a certain block.
Optional:

```--name``` This option allows you to set the name of the chain in use. Default chain name is parity.

```--nodes``` The amount of nodes that will be used with an aura or tendermint network. By default one non-authority node is also created. Default value is 2 nodes.

# Using the tool

Currently this scripts supports two types of chains, either instant sealing for development and authority round for proof of authority with multiple validators.

Some examples of using the script are:

A single node instant seal node, accessible via 127.0.0.1:8180:
```
./parity-deploy.sh --name testchain --engine instantseal
```

A three node proof of authority chain with one client acessable via 127.0.0.1:8180:
```
./parity-deploy.sh --name testchain --engine authorityround --nodes 3
```

The output of this tool are two main items:
1. A docker-compose.yml file which can be used with docker-compose to bring up and down the host(s). 
2. A deployments directory which will contain the keys, spec files and everything else required to configure the chain.

# Configuration of parity deploy

Once parity-deploy has been run it will generate configuration files which are kept in the ```deployment``` folder. There are a few subdirectories that may exist in this location:

1. deployment/chain - this contains chain information such as spec file (spec.json) and other files like the reserved_peers file.

2. deployment/is_authority - this directory contains the configuration for an instant sealing authority. It has key.priv (private key file), key.pub (public key file), address.txt (pre-created authority address), password (plain text password file) and authority.toml (authority's parity config file).

3. deployment/client - this directory contains the configuration for an instant sealing client. It has key.priv (private key file), key.pub (public key file), address.txt (pre-created client address), password (plain text password file) and client.toml (client's parity config file).

4. deployment/[1/2/3] - these directories are used when you are using multiple aura validators -  It has key.priv (private key file), key.pub (public key file), address.txt (pre-created authority address), password (plain text password file) and authority.toml (authority's parity config file).

All of these nodes are then added to the to the chains/reserved_peers file.

# Customisation of the chain configs.

All of the chains are templated from the config directory. Inside the config directory there are multiple possible sources of templates:

1. config/docker - This contains three example yml template files.
 * config/docker/authority.yml is a config file used for an aura authority.
 * config/docker/client.yml is a config file used to connect a client to the authority nodes.
 * config/docker/instantseal.yml is a config file used for an instantseal authority node.

2. config/spec/acccounts - This directory contains the accounts that will be added to the spec files.
 * config/specs/accounts/aura - This file contains the accounts that will be added to the default aura chain.
 * config/specs/accounts/instantseal - This file contains accounts that will be added to the default instantseal chain.
 * config/specs/accounts/tendermint - This file contains accounts that will be added to the default tendermint chain.

3. config/spec/engine - This directory contains the consensus engine information for each chain.
 * config/spec/engine/aura - This file contains the engine information for the aura chain.
 * config/spec/engine/instantseal - This file contains the engine information for the instantseal chain.
 * config/spec/engine/tendermint - This file contains the engine information for the tendermint chain.
 * config/spec/engine/validatorset - This file contains the engine information for the validator set example chain.


4. config/spec/genesis - This directory contains genesis information for each chain.
 * config/spec/genesis/aura - This file contains genesis information for the aura chain.
 * config/spec/genesis/instantseal - This file contains genesis information for the instant seal chain.
 * config/spec/genesis/tendermint - This file contains genesis information for the tendermint chain.


5. config/spec/params - This directory contains additional parameters for each chain.
 * config/spec/params/aura - This file contains additional parameters for the aura chain.
 * config/spec/params/instantseal - This file contains additional parameters for the instantseal chain.
 * config/spec/params/tendermint - This file contains additional parameters for the tendermint chain.

6. config/spec - This directory contains some toml spec files that are used as parity configurations.
 * config/spec/authority_round.toml - An example toml file for an aura chain.
 * config/spec/instantseal.toml - An example toml file for an instantseal chain.
 * config/spec/tendermint.tml - An example toml file for a tendermint chain.
 * config/spec/chain_header - An example header for the chain spec file.
 * config/spec/chain_footer - An example footer for the chain spec file.



# Launch the parity chain

Once the configuration is created you just need to run the docker-compose command to launch the machine or machines. This can be done via:
```
docker-compose up -d 
```

You will then be able to see the logs by running:
``` 
docker-compose logs -f 
```

In these logs you should see a token being generated to login to parity. Alternatively you can run the command:
```
docker-compose logs | grep token
```

Once you are logged into the web interface if you go to Add Accounts, then select the option recovery phrase and enter the account recovery phrase as ```password``` 

You now have an account with lots of ether to send around. 

# Adding custom containers

You can also include custom containers (e.g. ethstats monitoring) by including the docker-compose configuration in include/docker-compose.yml. To add Ethstats monitoring you would need to include this in the file:
```
  monitor:
    image: buythewhale/ethstats_monitor
    volumes:
      - ./monitor/app.json:/home/ethnetintel/eth-net-intelligence-api/app.json:ro
  dashboard:
    image: buythewhale/ethstats
    volumes:
      - ./dashboard/ws_secret.json:/eth-netstats/ws_secret.json:ro
    ports:
      - 3001:3000
```

