package parity

import groovy.json.JsonSlurper
import io.fabric8.kubernetes.api.model.Service
import io.fabric8.kubernetes.client.DefaultKubernetesClient
import org.apache.http.conn.HttpHostConnectException
import org.cliffc.high_scale_lib.NonBlockingHashMap
import org.joda.time.DateTime
import org.joda.time.Duration
import org.joda.time.Instant

import static groovyx.gpars.GParsPool.withPool

class ParityClient {
    static private String parityUrl

    static int getBlockTimeMillisInDayByNumber(Number blockNumber, JsonRpcClient rpcClientInstance) {
        new DateTime((new BigInteger(rpcClientInstance.eth_getBlockByNumber(convertToHexFormat(blockNumber), true).result.timestamp.substring(2), 16) as long) * 1000L).millisOfDay().get()
    }

    static addEngineSigner(JsonRpcClient rpcClientInstance, String engineSigner, String password) {
        println "adding engine signer: $engineSigner"
        return rpcClientInstance.parity_setEngineSigner(engineSigner, password).result as boolean
    }

    static def getBlockByNumber(JsonRpcClient jsonRpcClientLoopInstance, String theBlock) {
        jsonRpcClientLoopInstance.eth_getBlockByNumber(theBlock, false).result
    }

    static BigInteger getBlockTransactionCountByNumber(JsonRpcClient jsonRpcClientLoopInstance, String theBlock) {
        new BigInteger(jsonRpcClientLoopInstance.eth_getBlockTransactionCountByNumber(theBlock).result.substring(2), 16)
    }

    static int getLatestBlockNumber(rpcClientInstance) {
        new BigInteger(rpcClientInstance.eth_getBlockByNumber('latest', false).result.number.substring(2), 16).intValue()
    }

    static Duration getDurationBetweenBlockTimesByNumber(Number fromBlockNumber, Number toBlockNumber, rpcClientInstance) {
        Instant from = new DateTime((new BigInteger(rpcClientInstance.eth_getBlockByNumber(convertToHexFormat(fromBlockNumber), true).result.timestamp.substring(2), 16) as long) * 1000L).toInstant()
        Instant to = new DateTime((new BigInteger(rpcClientInstance.eth_getBlockByNumber(convertToHexFormat(toBlockNumber), true).result.timestamp.substring(2), 16) as long) * 1000L).toInstant()
        return new Duration(from,to)
    }

    static String convertToHexFormat(Number decimalNumber) {
        "0x${Long.toHexString(decimalNumber.value)}"
    }

    static String waitForParityServiceIp(String namespace, String serviceName, Service service) {
        String parityIp = ""
        while (!parityIp) {
            parityIp = new DefaultKubernetesClient().services().inNamespace(namespace).withName(serviceName).get().status.loadBalancer.ingress.ip[0]
//                parityIp = service.status.loadBalancer.ingress.ip[0]
            parityUrl = "http://$parityIp:8545/"
            println "Load Balancer IP: $parityIp"
            sleep(500l)
        }
        return parityUrl
    }
    static waitForParityAlive(JsonRpcClient jsonRpcClientInstance) {
        Number latestBlockNumber
        def timeout = 6000
        while (latestBlockNumber == null && timeout > 0 ) {
            try {
                latestBlockNumber = getLatestBlockNumber(jsonRpcClientInstance)
            } catch (HttpHostConnectException e) {
                println "parity not ready yet"
                sleep(500l)
            } catch (Exception e) {
                println "parity not ready to talk yet, $e.message"
                sleep(500l)
            }
            timeout = timeout - 1
            sleep(1500l)
        }
        if (timeout == 0) {
            println "parity not ready for 60 seconds, probably bombed out"
        } else println "parity ready!"
    }
    static waitForParityIsMining(JsonRpcClient jsonRpcClientInstance) {
        def timeout = 6000
        boolean isMining = false
        while (!isMining && timeout > 0) {
            try {
                isMining = jsonRpcClientInstance.eth_mining().result
            } catch (HttpHostConnectException e) {
                println "parity not mining yet"
                sleep(100l)
            } catch (Exception e) {
                println "parity not mining yet, $e.message"
                sleep(100l)
            }
            timeout = timeout - 1
        }
        if (timeout == 0) {
            println "parity not mining for 60 seconds, probably bombed out"
        } else println "parity mining!"
    }

    static waitForMinimumBlocksProcessed(int transactionBatchSize, Integer minimumBlocks, Integer stepDuration, txProcessed, rpcClientInstance) {

        while (txProcessed < transactionBatchSize) {
            int currentBlock = 0
            (0..minimumBlocks).each {
                try {
                    def transactionCountByNumber = getBlockTransactionCountByNumber(rpcClientInstance, it as String)
                    def latest = getLatestBlockNumber(rpcClientInstance)

                    if (latest >= currentBlock) {
                        println "block $it has $transactionCountByNumber blocks"
                        txProcessed = txProcessed + transactionCountByNumber
                        currentBlock ++
                    }
                } catch (NullPointerException e) {
                    println "block $it doesn't exist yet, latest block: ${getLatestBlockNumber(rpcClientInstance)}"
                }
                sleep(1000l* stepDuration)
            }
        }
    }

    static submitTxsParallel(NonBlockingHashMap errorsMap, NonBlockingHashMap resultMap, threads, lines, parityUrl) {
        withPool(threads) {
            lines.eachParallel { txLine ->
                    submitTx(txLine, errorsMap, resultMap, parityUrl)
            }
        }
    }

    static submitTx(txLine, errors, results, parityUrl) {
        JsonRpcClient jsonRpcClientInstance = new JsonRpcClient(parityUrl)
        def json = new JsonSlurper().parseText(txLine)
        def result = jsonRpcClientInstance.eth_sendRawTransaction(json.params[0])
        if (result.error) {
            errors.put(json.id, result.error)
        } else {
            results.put(json.id, result.result)
        }
    }


}
