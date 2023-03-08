# Platform Service's Rotating Shop Items Plugin gRPC Demo App

A CLI demo app to prepare required data and execute Rotating Shop Items Plugin gRPC for Platform Service.

## Prerequsites

* Java 11
* Gradle

## Build

To build this CLI sample app, execute the following command.

```bash
$ ./gradlew installDist
```
or
```bash
$ ./gradlew imageNative
```
to utilize GraalVm.

## Usage

### Setup

The following environment variables are used by this CLI demo app.
```
export AB_BASE_URL='https://demo.accelbyte.io'
export AB_CLIENT_ID='xxxxxxxxxx'
export AB_CLIENT_SECRET='xxxxxxxxxx'

export AB_NAMESPACE='namespace'
export AB_USERNAME='USERNAME'
export AB_PASSWORD='PASSWORD'
```
If these variables aren't provided, you'll need to supply the required values via command line arguments.

Also, you will need `Rotating Shop Items Plugin gRPC` server already deployed and accessible.
> Current AGS deployment does not support mTLS and authorization for custom grpc plugin. Make sure you disable mTls and authorization in your deployed Grpc server.

### Executable
- Without GraalVm, the executable is:
```
app/target/install/app/bin/app
```
- Or, with GraalVm, the executable is:
```
app/target/graal/platformGrpcDemo
```

### Example
- Without any environment variables
```bash
$ ./app/target/install/app/bin/app -b='https://demo.accelbyte.io' -c='CLIENT-ID-VALUE' -s='CLIENT-SECRET-VALUE' -n='NAMESPACE-VALUE' -u='<USERNAME>' -p='<PASSWORD>' <GRPC_PLUGIN_SERVER_URL>
```
- With basic environment variables setup
```bash
$ ./app/target/install/app/bin/app -u='<USERNAME>' -p='<PASSWORD>' -n='<NAMESPACE-VALUE>' <GRPC_PLUGIN_SERVER_URL>
```
- With all environment variables setup
```bash
$ ./app/target/install/app/bin/app <GRPC_PLUGIN_SERVER_URL>
```
- Show usage help
```bash
 $ ./app/target/install/app/bin/app -h
```