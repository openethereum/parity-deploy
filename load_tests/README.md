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
