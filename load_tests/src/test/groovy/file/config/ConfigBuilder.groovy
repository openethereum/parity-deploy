package file.config

import com.moandjiezana.toml.Toml
import com.moandjiezana.toml.TomlWriter
import groovy.json.JsonBuilder
import groovy.json.JsonSlurper
import io.fabric8.kubernetes.client.KubernetesClient
import org.yaml.snakeyaml.DumperOptions
import org.yaml.snakeyaml.Yaml

import static parity.KubernetesController.getFirstPodIp

class ConfigBuilder{

    static writeComposeFile(File composeFile, String hostName) {
        DumperOptions options = new DumperOptions()
        options.defaultScalarStyle = DumperOptions.ScalarStyle.SINGLE_QUOTED
        options.splitLines = false
        Yaml yaml = new Yaml(options)
        LinkedHashMap compose = yaml.load(composeFile.text)

        compose.services.host1.command = getCommand("/parity/spec.json", "/parity/data")
        compose.services.host1.volumes[3] = "./deployment/1/authority.edited.toml:/parity/authority.toml:ro"
        compose.services.host1.container_name = hostName
//        compose.services.put(hostName, compose.services.host1)
        compose.services.remove('client')
//        compose.services.remove('host1')

        writeFile(composeFile, yaml.dump(compose))
    }

    static String getEngineSigner(String workingDirPath) {
        Toml toml = new Toml().read(new File("$workingDirPath/deployment/1/authority.toml"))
        return toml.mining.engine_signer
    }

    static String removeEngineSigner(String workingDirPath) {
        Toml toml = new Toml().read(new File("$workingDirPath/deployment/1/authority.toml"))

        def engine_signer = toml.mining.engine_signer
        toml.'mining'.remove('engine_signer')

        TomlWriter tomlWriter = new TomlWriter();
        tomlWriter.write(toml.values, new File("$workingDirPath/deployment/1/authority.edited.toml"));
        return engine_signer
    }

    static String getPasswordFromFile(dirPath) {
        new File("${dirPath}/deployment/1/password").readLines()[0]
    }


    static writeChainSpec(File chainSpecFile, String gasLimit, String gasLimitBoundDivisor, String stepDuration) {
        def chainSpec = new JsonSlurper().parseText(chainSpecFile.text)
        editChainSpec(stepDuration, chainSpec, gasLimit, gasLimitBoundDivisor)
        writeJson(chainSpecFile, chainSpec)
    }

    static void editChainSpec(String stepDuration, chainSpec, String gasLimit, String gasLimitBoundDivisor) {
        chainSpec.engine.authorityRound.params.stepDuration = stepDuration
        chainSpec.genesis.gasLimit = gasLimit
        chainSpec.params.gasLimitBoundDivisor = gasLimitBoundDivisor
        chainSpec.params.minGasLimit = gasLimit
    }

    static writeJson(File jsonFile, objectForJson) {
        if (jsonFile.exists()) {
            assert jsonFile.delete()
            assert jsonFile.createNewFile()
        }
        FileWriter fileWriter = new FileWriter(jsonFile, false)
        BufferedWriter buffWriter = new BufferedWriter(fileWriter)
        buffWriter.writeLine new JsonBuilder(objectForJson).toPrettyString()
        buffWriter.flush()
        buffWriter.close()
    }

    static writeFile(File fileName, textToWrite) {
        FileWriter fileWriter = new FileWriter(fileName, false)
        BufferedWriter buffWriter = new BufferedWriter(fileWriter)
        buffWriter.writeLine textToWrite
        buffWriter.flush()
        buffWriter.close()
    }

    static writeReservedPeerIp(String deploymentDir, KubernetesClient client, String namespace) {
        String podIp = getFirstPodIp(namespace, client)
        if (!podIp) throw new Exception("no pod ip")
        String pubKey = new File("$deploymentDir/deployment/1/key.pub").getText('UTF-8')
        String reservedPeers = "enode://$pubKey@$podIp:30303"
        writeFile(new File("${deploymentDir}/deployment/chain/reserved_peers"), reservedPeers)
    }

    static writeReservedPeers(deploymentDir, containerName) {
        String pubKey = new File("$deploymentDir/deployment/1/key.pub").getText('UTF-8')
        String reservedPeers = "enode://$pubKey@$containerName:30303"

//        runParityLocally(chainSpecFile, workingDir)
        sleep(5000l)

        writeFile(new File("${deploymentDir}/deployment/chain/reserved_peers"), reservedPeers)
    }

    static String getCommand(String specFile, String dataDir) {
//        "--chain $specFile --config /parity/authority.toml -d $dataDir --gasprice 0  --jsonrpc-server-threads 4  --fast-and-loose" as String
        "--chain $specFile --config /parity/authority.toml -d $dataDir --gasprice 0  --fast-and-loose" as String
    }

}