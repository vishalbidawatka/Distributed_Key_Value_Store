# Distributed Key Value store
- Multiple clients will be communicating with a single coordinating server (Master) in a JSON based message format and send the data through sockets using TCP channel.
- Replication factor of 2 with consistent hashing.
![alt text](https://github.com/vishalbidawatka/Distributed_Key_Value_Store/blob/master/proj4-overview.png)

## Consistent Hashing
key-value servers will have unique 64-bit IDs. The coordinator will hash the keys to 64-bit address space. Then each key-value server will store the first copies of keys with hash values greater than the ID of its immediate predecessor up to its own ID. Note that, each key-value server will also store the keys whose first copies are stored in its predecessor.
![](https://github.com/vishalbidawatka/Distributed_Key_Value_Store/blob/master/consistent-hashing.png)

## Project Requirements
- Each key will be stored using 2PC in two key-value servers; the first of them will be selected using consistent hashing, while the second will be placed in the successor of the first one. There will be at least two key-value servers in the system.
- Key-value servers will have 64-bit globally unique IDs (use unique long numbers), and they will register with the coordinator with that ID when they start. For simplicity, you can assume that the total number of key-value servers are fixed, at any moment there will be at most one down server, and they always come back with the same ID if they crash (Note that this simplification will cause the system to block on any failed key-value server. However, not assuming this will require dynamic successor adjustment and re-replication, among other changes.).
- You do not have to support concurrent update operations irrespective of which key they are working on (i.e., 2PC PUT and DEL operations are performed one after another), but retrieval operations (i.e., GET) of different keys must be concurrent unless restricted by an ongoing update operation on the same set.
- For this particular project, you can assume that the coordinator will never fail/crash. Consequently, there is no need to log its states, nor does it require to survive failures. Individual key-value servers, however, must log necessary information to survive from failures.
- The coordinator server will include a write-through set-associative cache, which will have the same semantics as the write-through cache you used before. Caches at key-value servers will still remain write-through.
- You should bulletproof your code, such that the server does not crash under any circumstance.
- You will run the client interface of the coordinator server on port 8080.
- Individual key-value servers must use random ports assigned upon creating respective SocketServers for listening to 2PC requests (Note that when you see the SocketServer's constructor, the port is set to -1. This must be replaced with a random port number). They must register themselves with the 2PC interface of the coordinator server running on port 9090. To make things simpler, assume that the TPC master will always respond with a ack message during registration.

### Sequence diagram of concurrent read/write operations using the 2PC protocol
![](https://github.com/vishalbidawatka/Distributed_Key_Value_Store/blob/master/2pc.png)
