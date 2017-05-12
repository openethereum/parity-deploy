# Party Deploy 
Pairty deployment script generator. 

# Requirements

To use this script you will need two dependencies, docker and docker-compose. On Ubuntu systems these will automatically be installed if not already present on the system.

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

# Adding extra containers

You can also include extra nodes (e.g. ethstats monitoring) by including the docker-compose configuration in include/docker-compose.yml. To add Ethstats monitoring you would need to include this in the file:
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

 
