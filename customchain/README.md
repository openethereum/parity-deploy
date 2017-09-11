# Example of nested chain configuration

This is an example of a nested chain configuration to be integrated into parity-deploy. This script will run with a custom chain option is given to parity deploy. 

# Quickstart

Ensure the python package contoml is installed, you can install this using ```pip install contoml```.

``` 
./generate.py
docker-compose up
``` 


# Editing the configuration

Edit the file config/config.toml ... there are two main sections.

The first is for the spec file, just paste the whole spec file in. 

The second is for the containers to be created, they should be created in the format:

hostname =  ``` 
        parity-toml-file-goes-here

```

