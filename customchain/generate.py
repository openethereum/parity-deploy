#!/usr/bin/python
import contoml
import os
import subprocess
import time
import sys
from shutil import copyfile

docker_template = "customchain/config/node.yml"    
compose_file = "docker-compose.yml"

def generate_docker_compose_header():
    f = open(compose_file,"w")
    f.write("version: '2.0'\nservices:\n")
    f.close()

def generate_docker_compose(name):
    f = open(docker_template,"r")
    spec = f.read()
    f.close()
    output = ""
    for host in hosts:
        node_info = spec
        output = output + node_info.replace("NODE_NAME",host)
    f = open(compose_file,"a")
    f.write(output)
    f.close()

def generate_toml_files(hosts):
    copyfile("/dev/null","deployment/reserved_peers")
    for host in hosts:
        f = open("deployment/" + host + ".toml","w")
        if mainconfig['toml'][host] == "default":
            f.write(mainconfig['spec']['default_config'])
        else:
            f.write(mainconfig['toml'][host])
        f.close()

def capture_enodes():
    print("Bringing up containers to grab enode information")
    os.system("docker-compose up -d ")
    time.sleep(20)
    cmd = ("docker-compose logs | grep enode | awk {'print $9'}")
    p = subprocess.Popen(cmd, stdin=subprocess.PIPE, stdout=subprocess.PIPE, shell=True)
    f = open("deployment/reserved_peers","w")
    for line in p.stdout.readlines():
        f.write(line)
    f.close()
    os.system("docker-compose down")

def chain_spec():
    spec = mainconfig['spec']['config']
    f = open("deployment/spec.json","w")
    f.write(spec)
    f.close()

if not os.path.exists("deployment/"):
    os.mkdir("deployment/")


hosts = []  


print("Generating configuration...")
mainconfig = contoml.load(sys.argv[1])

for host in mainconfig['toml'].items():
    
    hosts.append(host[0])
chain_spec()
generate_docker_compose_header()
generate_docker_compose(hosts)
generate_toml_files(hosts)
capture_enodes()

print("All done, run 'docker-compose up -d' to start containers")
 
