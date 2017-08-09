## Jmeter Load tests

[Download Jmeter](https://jmeter.apache.org/download_jmeter.cgi)

`personal_sendTransaction_1500.jmx` is set up to use 150 concurrent clients with 10 loops to hit the `personal_sendTransaction` method.

In the second thread group a call is being made to `eth_getBlockByNumber` to check latest block number until it reaches 0x5dc or 1500 concluding that all transactions have been processed

Resultant TPS is 1500 divided by time taken to finish the test

## Standard RPC corpora

Files can be found [here](https://drive.google.com/drive/folders/0B8F2pjh7CQ9vREM5TGNEOC1fS1U?usp=sharing). Each line in those `.rpc` files contains a JSON object to be fed to a standard RPC interface (IPC or HTTP).

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

## [Spock](http://spockframework.org/spock/docs/1.1/index.html) test benchmark

[Spock Framework](http://spockframework.org/spock/docs/1.1/index.html) is an eye friendly BDD style test framework based on a Groovy DSL. This means you need to have Java installed. The build script is a gradle build and so can be started with the wrapper shell script ./gradlew in the directory containg the build.gradle file.
Open load_tests/build/reports/tests/test/index.html to view the test results. The CLI output where benchmark times are recorded is under the Standard Output after clicking on a particular test 
 
