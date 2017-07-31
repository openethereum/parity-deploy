  Jmeter Load tests
  
  personal_sendTransaction_1500.jmx is set up to use 150 concurrent clients with 10 loops to hit the personal_sendTransaction method.
  
  In the second thread group a call is being made to eth_getBlockByNumber to check latest block number until it reaches 0x5dc or 1500 concluding that all transactions have been processed
  
  Resultant TPS is 1500 divided by time taken to finish the test

  
  
  