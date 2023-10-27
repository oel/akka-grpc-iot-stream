# Akka gRPC for IoT Streams

This is a [gRPC](https://grpc.io/docs/what-is-grpc/core-concepts/) based application in Scala demonstrating how IoT device states get algorithmically updated as streaming requests/responses using [Akka gRPC](https://grpc.io/docs/what-is-grpc/core-concepts/), which is built on top of [Akka Streams](https://doc.akka.io/docs/akka/2.6/stream/index.html) and [Akka HTTP](https://doc.akka.io/docs/akka-http/10.5.3/index.html) with [HTTP/2](https://http2.github.io/) transport.

Akka gRPC generates:
1. service interfaces from a [Protobuf](https://protobuf.dev/) service schema that get implemented as Scala classes and Akka stream components on top of a Akka-HTTP server that supports HTTP/2
2. gRPC stubs through implementing the service interfaces with Akka Streams API for the gRPC clients to invoke the remote services

For an overview of the application, please visit [Genuine Blog](https://blog.genuine.com/2023/10/akka-grpc-for-iot-streams/).


## Running the gRPC server and clients on multiple JVMs


To run the server application, open a command line terminal, go to the *project-root* and and run as follows:

On terminal #1
```bash
sbt "runMain akkagrpc.IotStreamServer"
```

For the client application, IotStreamClient takes the following parameters:

```bash
IotStreamClient clientId broadcastYN propIdStart propIdEnd
# e.g. IotStreamClient client1 1 1000 1049  # broadcastYN: 1=Yes | 0=No

```

To run the client application, open a terminal for each client, go to the *project-root* and run a specific range of IDs of real estate properties:

On terminal #2
```bash
sbt "runMain akkagrpc.IotStreamClient client1 1 1000 1019"
```

On terminal #3
```bash
sbt "runMain akkagrpc.IotStreamClient client2 1 1020 1039"
```
