## Jmeter Load tests

[Download Jmeter](https://jmeter.apache.org/download_jmeter.cgi)

`personal_sendTransaction_1500.jmx` is set up to use 150 concurrent clients with 10 loops to hit the `personal_sendTransaction` method.

In the second thread group a call is being made to `eth_getBlockByNumber` to check latest block number until it reaches 0x5dc or 1500 concluding that all transactions have been processed

Resultant TPS is 1500 divided by time taken to finish the test

## Run Parity

It is assumed that you are running parity somewhere and the test is configured to point at it. It is configured for localhost by default.

An easy way to run parity with docker would be for example 
``` docker run -ti -p 8180:8180 -p 8545:8545 -p 30303:30303 parity/parity:nightly --config dev-insecure --ui-interface "0.0.0.0" --ui-no-validation ui --fast-unlock```

## Standard RPC corpora

Files can be found [here](https://drive.google.com/drive/folders/0B8F2pjh7CQ9vREM5TGNEOC1fS1U?usp=sharing). Each line in those `.rpc` files contains a JSON object to be fed to a standard RPC interface (IPC or HTTP). One way is presented in the simple `submit_corpus.sh` script, which can be ran like so
```
./submit_corpus.sh eth_sendRawTransaction_basic_10000.rpc
```

Different options might be required to run different tests:
- `eth_sendRawTransaction` does not require any special options
```
parity --config dev
```
- `eth_sendTransaction` and `eth_signTransaction` require the account to be unlocked
```
parity --config dev --unlock 0x00a329c0648769a73afac7f9381e08fb43dbea72 --password empty.file --fast-unlock
```
- `personal_sendTransaction` requires `personal` RPC API
```
parity --config dev --jsonrpc-apis personal
```

## [Gradle](https://gradle.org/) build system
[Gradle](https://gradle.org/) is used to run Spock tests. Run ```./gradlew test``` in the load_tests directory to run the load tests

To run a specific test such as the local docker based test 
- ```./gradlew test  -Dtest.single=EthSendRawTransactionBasic10000ChartBlocksSpockTest```

## [Spock](http://spockframework.org/spock/docs/1.1/index.html) test benchmark

[Spock Framework](http://spockframework.org/spock/docs/1.1/index.html) is an eye friendly BDD style test framework based on a Groovy DSL. This means you need to have Java installed. The build script is a gradle build and so can be started with the wrapper shell script ./gradlew in the directory containg the build.gradle file.
Open load_tests/build/reports/tests/test/index.html to view the test results. The CLI output where benchmark times are recorded is under the Standard Output after clicking on a particular test 
 
