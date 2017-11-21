package parity

import io.fabric8.kubernetes.api.model.ConfigMap
import io.fabric8.kubernetes.api.model.ConfigMapBuilder
import io.fabric8.kubernetes.api.model.ConfigMapVolumeSource
import io.fabric8.kubernetes.api.model.ConfigMapVolumeSourceBuilder
import io.fabric8.kubernetes.api.model.Container
import io.fabric8.kubernetes.api.model.ContainerBuilder
import io.fabric8.kubernetes.api.model.ContainerPort
import io.fabric8.kubernetes.api.model.ContainerPortBuilder
import io.fabric8.kubernetes.api.model.KeyToPath
import io.fabric8.kubernetes.api.model.Namespace
import io.fabric8.kubernetes.api.model.NamespaceBuilder
import io.fabric8.kubernetes.api.model.Pod
import io.fabric8.kubernetes.api.model.PodSpec
import io.fabric8.kubernetes.api.model.PodSpecBuilder
import io.fabric8.kubernetes.api.model.PodTemplateSpec
import io.fabric8.kubernetes.api.model.PodTemplateSpecBuilder
import io.fabric8.kubernetes.api.model.Service
import io.fabric8.kubernetes.api.model.ServiceBuilder
import io.fabric8.kubernetes.api.model.ServicePort
import io.fabric8.kubernetes.api.model.ServicePortBuilder
import io.fabric8.kubernetes.api.model.Volume
import io.fabric8.kubernetes.api.model.VolumeBuilder
import io.fabric8.kubernetes.api.model.VolumeMount
import io.fabric8.kubernetes.api.model.VolumeMountBuilder
import io.fabric8.kubernetes.api.model.extensions.Deployment
import io.fabric8.kubernetes.api.model.extensions.DeploymentBuilder
import io.fabric8.kubernetes.api.model.extensions.DeploymentSpec
import io.fabric8.kubernetes.api.model.extensions.DeploymentSpecBuilder
import io.fabric8.kubernetes.assertions.PodSelectionAssert
import io.fabric8.kubernetes.client.DefaultKubernetesClient
import io.fabric8.kubernetes.client.KubernetesClient
import io.fabric8.kubernetes.client.KubernetesClientException
import io.fabric8.kubernetes.client.handlers.ConfigMapHandler
import io.fabric8.kubernetes.client.handlers.DeploymentHandler
import io.fabric8.kubernetes.client.handlers.PodHandler

import static groovy.io.FileType.FILES

class KubernetesController {

    public static final String PARITYCONFIG = 'parityconfig'
    public static final String deploymentName = "parity-benchmark-deployment"
    public static final String networkName = "aura_test"
    public static final String configVolumeName = "${PARITYCONFIG}-volume"
    public static final List ports = [8180, 8545, 8546, 30303]
    public static final defaultNodes = 1
    public static final String PARITY = 'parity'
    public static final String SVC_NAME = "parity-benchmark-service"
    public static final String NAMESPACE = "parity-benchmark-namespace-1"
    public static final String CONFIG_REFERENCE = '/parity/authority.edited.toml'
//    public static final String CONFIG_REFERENCE = '/parity/authority.toml'
    public static final String CHAIN_SPEC_REFERENCE = '/parity/spec.json'
    public static final String RESERVED_PEERS_REFERENCE = '/parity/reserved_peers'
    public static final String POD_NAME = 'parity-benchmark-pod'
    public static final String NIGHTLY_IMAGE = 'parity/parity:nightly'
    public static final String BETA_IMAGE = 'parity/parity:beta'
    public static final String STABLE_IMAGE = 'parity/parity:stable'
    public static final String SERVICE_NAME = 'parity-benchmark-service'

    static String getFirstPodIp(String namespace, KubernetesClient kubernetesClient) {
        kubernetesClient.pods().inNamespace(namespace).list().items[0].status.podIP
    }

    static ConfigMap replaceConfig(String workingDirPath, String namespace, KubernetesClient kubernetesClient) {
        LinkedHashMap configs = setupConfigArray(workingDirPath)
        return replaceExistingConfig(configs, PARITYCONFIG, namespace, kubernetesClient)
    }

    static ConfigMap replaceDeployment(String namespace, KubernetesClient kubernetesClient, String testRun, String workingDirPath, String containerImage) {
        def labels =  [testrun: testRun, app: PARITY]
        LinkedHashMap configs = setupConfigArray(workingDirPath)
        replaceExistingConfig(configs, PARITYCONFIG, namespace, kubernetesClient)
        replaceExistingDeployment(deploymentName, namespace, kubernetesClient, labels, getDeploymentSpec(configs, '4', labels, containerImage))
    }

    static ConfigMap getCurrentConfig(KubernetesClient kubernetesClient) {
        kubernetesClient.configMaps().inNamespace(NAMESPACE).withName(PARITYCONFIG).get()
    }

    static String getCurrentAuthority(KubernetesClient kubernetesClient) {
        kubernetesClient.configMaps().inNamespace(NAMESPACE).withName(PARITYCONFIG).get().data.get("authority.toml")
    }

    static Service launchParityNetwork(workingDirPath, String jsonRpcThreads, String namespace, KubernetesClient kubernetesClient, LinkedHashMap<String, String> labels, String containerImage) {

        createNameSpaceIfNotExist(namespace, kubernetesClient)

        LinkedHashMap configs = setupConfigArray(workingDirPath)

        ConfigMap networkConfig = removeExistingConfig(configs, PARITYCONFIG, namespace, kubernetesClient)

        kubernetesClient.configMaps().withName(PARITYCONFIG).createOrReplace(networkConfig)

        removeExistingDeployment(deploymentName, namespace, kubernetesClient, labels)

        DeploymentSpec deploymentSpec = getDeploymentSpec(configs, jsonRpcThreads, labels, containerImage)

        Deployment newDeployment = new DeploymentBuilder()
                .withSpec(deploymentSpec)
                .withNewMetadata()
                .withName(deploymentName)
                .withLabels(labels)
                .withNamespace(namespace)
                .and()
                .build();

        kubernetesClient.extensions().deployments().createOrReplace(newDeployment)

        def existingService = kubernetesClient.services().inNamespace(namespace).withName(namespace).get()
        if (!existingService) {
            def service = createService(ports, labels, namespace, kubernetesClient)
        } else {
            return kubernetesClient.services().inNamespace(namespace).withName(namespace).get()
        }
//        return createService(ports, labels, namespace, kubernetesClient)
    }

    static DeploymentSpec getDeploymentSpec(LinkedHashMap<String, String> configs, String jsonRpcThreads, LinkedHashMap<String, String> labels, String containerImage) {
        Volume volume = buildVolume(configs, PARITYCONFIG, configVolumeName)
        ArrayList<VolumeMount> volumeMountList = buildVolumeMountList(configs, PARITYCONFIG, configVolumeName)
        List<String> args = getArgs(jsonRpcThreads)

        DeploymentSpec deploymentSpec = buildNewDeploymentSpec(args, generateContainerPortList(ports), volumeMountList, volume, defaultNodes, labels, containerImage)
        deploymentSpec
    }

    static List<String> getArgs(String jsonRpcThreads) {
        List<String> args = [
                '--config', CONFIG_REFERENCE,
                '--chain', CHAIN_SPEC_REFERENCE,
                '--reserved-peers', RESERVED_PEERS_REFERENCE,
                '-d', '/var/parity',
                '--keys-path', '/parity',
//                '--jsonrpc-server-threads', "$jsonRpcThreads" as String
        ]
        args
    }

    public static LinkedHashMap setupConfigArray(workingDirPath) {
        String parityConfigRoot = "${workingDirPath}/deployment"
        File chaindir = new File("${parityConfigRoot}/chain/")
        File secretsdir = new File("${parityConfigRoot}/1/")
        File networkdir = new File("$parityConfigRoot/1/${networkName}")

        LinkedHashMap configs = [:]

        secretsdir.eachFile(FILES) {
            configs.put(it.name, it.text)
        }
        chaindir.eachFile(FILES) {
            configs.put(it.name, it.text)
        }
        networkdir.eachFile(FILES) {
            configs.put(it.name, it.text)
        }
        configs
    }

    static Service createService(List<Integer> ports, LinkedHashMap<String, String> labels, String namespace, DefaultKubernetesClient kubernetesClient) {
        List servicePorts = generateServicePortList(ports)

        Service parityservice = new ServiceBuilder()
                .withNewSpec().withType('LoadBalancer')
                .withSelector(labels).withSelector(labels).withPorts(servicePorts)
                .and().withNewMetadata().withName(SERVICE_NAME).withNamespace(namespace).withLabels(labels)
                .and().build()

        kubernetesClient.services().delete(parityservice)
        return kubernetesClient.services().create(parityservice)
    }

    static List<ContainerPort> generateContainerPortList(List<Integer> ports) {
        def containerPortBuilder = new ContainerPortBuilder()

        List<ContainerPort> containerPortsList = []

        ports.each {
            containerPortsList.add(containerPortBuilder.withContainerPort(it).build())
        }

        containerPortsList
    }

    static List<ContainerPort> generateServicePortList(List<Integer> ports) {
        def servicePortBuilder = new ServicePortBuilder()

        List<ServicePort> servicePorts = []

        ports.each {
            servicePorts.add(servicePortBuilder
                    .withPort(it).withName(it as String)
                    .build())
        }

        servicePorts
    }


    static void createNameSpaceIfNotExist(String namespace, DefaultKubernetesClient client) {
        Namespace ns1 = new NamespaceBuilder().withNewMetadata().withName(namespace).and().build();
        try {
            client.namespaces().withName(namespace).createOrReplace(ns1);
        } catch (KubernetesClientException e) {
            println e.message
        }
    }

    static void removeExistingDeployment(String deploymentName, String namespace, DefaultKubernetesClient client, labels) {
        DeploymentHandler deploymentHandler = new DeploymentHandler()
        Deployment deploymentDel = new DeploymentBuilder()
                .withNewMetadata()
                .withName(deploymentName)
                .withLabels(labels)
                .withNamespace(namespace)
                .and()
                .build();

        deploymentHandler.delete(client.httpClient, client.configuration, namespace, deploymentDel)
    }

    static void deletePodsInNamespace(String namespace, DefaultKubernetesClient client) {
        client.inNamespace(namespace).pods().list().items.each { Pod pod ->
//            PodHandler podHandler = new PodHandler().delete(client.httpClient,client.configuration,namespace,pod)
            def isDeleted = new PodHandler().delete(client.httpClient,client.configuration,namespace,pod)
            println "deleted pod: $pod.metadata.name"
        }

    }
    static void replaceExistingDeployment(String deploymentName, String namespace, DefaultKubernetesClient client, labels, DeploymentSpec spec) {
        DeploymentHandler deploymentHandler = new DeploymentHandler()
        Deployment deploymentDel = new DeploymentBuilder().withSpec(spec)
                .withNewMetadata()
                .withName(deploymentName)
                .withLabels(labels)
                .withNamespace(namespace)
                .and()
                .build();

        deploymentHandler.delete(client.httpClient, client.configuration, namespace, deploymentDel)

        deploymentHandler.create(client.httpClient, client.configuration, namespace, deploymentDel)
    }

    static DeploymentSpec buildNewDeploymentSpec(List<String> args, List<ContainerPort> containerPortList,
                                                 ArrayList<VolumeMount> volumeMountList, Volume volume,
                                                 int nodes, LinkedHashMap<String, String> labels, String containerImage) {
        Container container = new ContainerBuilder()
                .withName(POD_NAME)
                .withImage(containerImage)
                .withImagePullPolicy('Always')
                .withArgs(args)
                .withPorts(containerPortList)
                .withVolumeMounts(volumeMountList)
                .build()

        PodSpec podSpec = new PodSpecBuilder()
                .withContainers(container)
                .withVolumes(volume)
                .build()

        PodTemplateSpec podTemplateSpec = new PodTemplateSpecBuilder()
                .withNewMetadata().withLabels(labels)
                .and()
                .withSpec(podSpec)
                .build()

        DeploymentSpec deploymentSpec = new DeploymentSpecBuilder()
                .withTemplate(podTemplateSpec).withReplicas(nodes)
                .withNewSelector().withMatchLabels(labels)
                .and()
                .build()
        deploymentSpec
    }

    static Volume buildVolume(LinkedHashMap<String, String> configs, parityconfigName, String volumName) {
        ConfigMapVolumeSource configMapVolumeSource = generateConfigMapVolumeSource(configs, parityconfigName)
        new VolumeBuilder()
                .withName(volumName)
                .withConfigMap(configMapVolumeSource)
                .build()

    }

    static ConfigMapVolumeSource generateConfigMapVolumeSource(LinkedHashMap<String, String> configs, parityconfigName) {
        List<KeyToPath> keyToPathArrayList = []

        configs.each { Map.Entry<Object, String> entry ->
            keyToPathArrayList.add(new KeyToPath(entry.key, 0777, entry.key))
        }

        ConfigMapVolumeSource configMapVolumeSource = new ConfigMapVolumeSourceBuilder()
                .withName(parityconfigName)
                .withItems(keyToPathArrayList)
                .build()
        configMapVolumeSource
    }

    static ArrayList<VolumeMount> buildVolumeMountList(LinkedHashMap<String, String> configs, String configVolumeName, String volumeMountName) {
        List<VolumeMount> volumeMountList = new ArrayList<VolumeMount>()

        generateConfigMapVolumeSource(configs, configVolumeName).items.each {
            volumeMountList.add(new VolumeMountBuilder()
                    .withName(volumeMountName)
                    .withMountPath("/parity/$it.path")
                    .withSubPath(it.path)
                    .withReadOnly(true)
                    .build())
        }

        volumeMountList
    }

    static ConfigMap replaceExistingConfig(LinkedHashMap<String, String> configs, String parityconfigName, String namespace, DefaultKubernetesClient client) {
        ConfigMapHandler configMapHandler = new ConfigMapHandler()

        ConfigMap networkConfig = new ConfigMapBuilder().withData(configs)
                .withNewMetadata()
                .withName(parityconfigName)
                .withNamespace(namespace)
                .and().build()

        configMapHandler.replace(client.httpClient, client.configuration, namespace, networkConfig)
        networkConfig
    }

    static ConfigMap removeExistingConfig(LinkedHashMap<String, String> configs, String parityconfigName, String namespace, DefaultKubernetesClient client) {
        ConfigMapHandler configMapHandler = new ConfigMapHandler()

        ConfigMap networkConfig = new ConfigMapBuilder().withData(configs)
                .withNewMetadata()
                .withName(parityconfigName)
                .withNamespace(namespace)
                .and().build()

        configMapHandler.delete(client.httpClient, client.configuration, namespace, networkConfig)
        networkConfig
    }

    static void waitForPodReady(KubernetesClient client, testRun, Integer nodes) {
        Map<String, String> labels =  [testrun: testRun, app: PARITY]
        def selectionAssert = new PodSelectionAssert(client, nodes, labels,null,'')
        selectionAssert.isPodReadyForPeriod(1500l,500l)
    }

//    static void waitForServiceReady(KubernetesClient client, testRun, Integer nodes) {
//        Map<String, String> labels =  [testrun: testRun, app: PARITY]
//        def selectionAssert = new ServiceAssert(new Service())
//        selectionAssert.isPodReadyForPeriod(1500l,500l)
//    }
}
