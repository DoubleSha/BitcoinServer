The BitcoinServer is a standalone server written in Java. It uses Spring Boot for its http server and provides an interface to bitcoin-related functions.<br />

Broadcasting a list of transactions to the network:<br />
> POST /broadcast

Pass a TransactionList message, defined as follows (see src/main/protos/bitcoinserver.proto):<br />
> message TransactionList {<br />
> &nbsp;&nbsp;&nbsp;&nbsp;optional string network = 1 [default = "main"]; // "main" or "test"<br />
> &nbsp;&nbsp;&nbsp;&nbsp;repeated bytes transactions = 2;    // List of transactions<br />
> }<br />

Returns a BroadcastResponse message, defined as follows (see src/main/protos.bitcoinserver.proto):<br />
> message BroadcastResponse {<br />
> &nbsp;&nbsp;&nbsp;&nbsp;optional string error = 1;<br />
> }<br />

If there is an error, |error| will include the error message.<br />
On success, error will be ommitted and the message will be simply empty.<br />
