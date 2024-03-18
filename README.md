# Local setup to test kafka mirrormaker2

## Start minikube with profile kafka-cluster with 4 cpus and 4GB memory

```shell
minikube start --profile kafka-cluster --cpus 8 --memory 8096 --kubernetes-version v1.21.2 --driver=docker
```

# Create a namespace for strimzi

```shell
kubectl create namespace strimzi
```
# Create a namespace for mm2

```shell
kubectl create namespace mm2
```


## deploy Strimzi operator with helm chart

```shell
helm repo add strimzi https://strimzi.io/charts/

helm install strimzi-release strimzi/strimzi-kafka-operator \
--namespace strimzi \
--version 0.39.0 \
--set watchNamespaces="{mm2}"
```

## Wait for strimzi operator to be ready

```shell
kubectl wait --for=condition=ready pod -l name=strimzi-cluster-operator --timeout=300s -n strimzi

```

### Deploy kafka cluster

```shell
kubectl apply -f kafka/kafka-cluster-a.yaml -n mm2
```

## wait for all pods to be ready

```shell
kubectl wait --for=condition=ready pods --all --timeout=300s -n mm2
```

## Deploy a tools pod

```shell
kubectl apply -f kafka/kafka-tools-deployment.yaml
```

## Export the name of the tools pod

```shell
export TOOL_POD=$(kubectl get pods -l app=kafka-tools-deployment -o jsonpath='{.items[0].metadata.name}')

export TOOL_POD=$(kubectl get pods -l app=kaf-deployment -o jsonpath='{.items[0].metadata.name}')
```

## Get topic list

```shell
export BOOTSTRAP_SERVER="cluster-a-kafka-bootstrap.mm2.svc.cluster.local:9092"
kubectl exec -it $TOOL_POD -- kafka-topics --bootstrap-server $BOOTSTRAP_SERVER --list
```

## Create a topic named foo.customer

```shell
kubectl exec -it $TOOL_POD -- kafka-topics --bootstrap-server $BOOTSTRAP_SERVER --create --topic foo.customer --partitions 3 --replication-factor 3
```

## Create a topic name rename.foo.customers

```shell
kubectl exec -it $TOOL_POD -- kafka-topics --bootstrap-server $BOOTSTRAP_SERVER --create --topic rename.foo.customers --partitions 3 --replication-factor 3
```

## Produce messages to topic foo.customer

```shell
kubectl exec -it $TOOL_POD -- kafka-console-producer --broker-list $BOOTSTRAP_SERVER --topic foo.customer
```

```shell
  > 1
  > 2
  > 3
```

## Consume message from topic foo.customer

```shell
kubectl exec -it $TOOL_POD -- kafka-console-consumer --bootstrap-server $BOOTSTRAP_SERVER --topic foo.customer --from-beginning
1
2
3
```

## Deploy mirror maker 2

```shell
helm install mirrormaker mirror-maker2/mirrormaker -f mirror-maker2/mirrormaker/values-sample.yaml -n mm2
```

## Wait for mirror maker pod to be ready

```shell
kubectl wait --for=condition=ready pod -l app.kubernetes.io/instance=mirrormaker --timeout=300s -n mm2
```

## Consume message from topic foo.customer with consumer group and commit offset

    ``` shell
    kubectl exec -it $TOOL_POD -- kafka-console-consumer --bootstrap-server $BOOTSTRAP_SERVER --topic foo.customer --group c-group --from-beginning
    1
    2
    3
    ```

## Consume message from topic rename.foo.customers without consumer group

      ``` shell
      kubectl exec -it $TOOL_POD -- kafka-console-consumer --bootstrap-server $BOOTSTRAP_SERVER --topic rename.foo.customers --from-beginning
      1
      2
      3
      ```

So messages are being mirrored from foo.customer to rename.foo.customers, now let's stop the consumer and check the consumer group offsets.

```shell
kubectl exec -it $TOOL_POD -- watch -n 1 kafka-consumer-groups --bootstrap-server $BOOTSTRAP_SERVER --describe --group c-group

```

Now check the consumer group offsets

```shell
kubectl exec -it $TOOL_POD -- kafka-consumer-groups --bootstrap-server $BOOTSTRAP_SERVER --describe --group c-group
GROUP           TOPIC           PARTITION  CURRENT-OFFSET  LOG-END-OFFSET  LAG             CONSUMER-ID                                           HOST            CLIENT-ID
c-group    foo.customer    0          3               3               0               console-consumer-94df1c42-1e92-4eef-90af-a53f25d41c20 /172.17.0.1     console-consumer
```

Let's run the command in watch mode to see the offset changes, and stop the previously running consumer.

```shell
Every 1.0s: kafka-consumer-groups --bootstrap-server cluster-a-kafka-bootstrap.mm2.svc.cluster.local:9092 --describe --group c-group                                         kafka-tools-deployment-7fddff4484-f6g72: Tue Mar  5 05:16:09 2024


Consumer group 'c-group' has no active members.

GROUP           TOPIC                PARTITION  CURRENT-OFFSET  LOG-END-OFFSET  LAG             CONSUMER-ID     HOST            CLIENT-ID
c-group         rename.foo.customers 1          5               5               0               -               -               -
c-group         foo.customer         0          0               0               0               -               -               -
c-group         rename.foo.customers 2          0               0               0               -               -               -
c-group         foo.customer         2          0               0               0               -               -               -
c-group         foo.customer         1          5               5               0               -               -               -
c-group         rename.foo.customers 0          0               0               0               -               -               -
```

If you want to produce messages to topic foo.customer continuously.

```shell
kubectl exec -it $TOOL_POD -- kafka-producer-perf-test --num-records 10 --topic foo.customer --throughput 1 --record-size 1 --producer-props bootstrap.servers=$BOOTSTRAP_SERVER
```


# Call connect api

```shell
kubectl exec -it $TOOL_POD  -- curl -X GET http://mirrormaker-mirrormaker2-api.mm2.svc.cluster.local:8083/connectors |jq
[
  "0-source-foo-customer->0-target-rename-foo-customers.MirrorHeartbeatConnector",
  "0-source-foo-customer->0-target-rename-foo-customers.MirrorCheckpointConnector",
  "0-source-foo-customer->0-target-rename-foo-customers.MirrorSourceConnector"
]
```
# Get the status of all connectors with pipe the output to jq

```shell
kubectl exec $TOOL_POD -- curl -s -X GET http://mirrormaker-mirrormaker2-api.mm2.svc.cluster.local:8083/connectors | jq -r '.[]' | xargs -I {} kubectl exec $TOOL_POD -- curl -s -X GET http://mirrormaker-mirrormaker2-api.mm2.svc.cluster.local:8083/connectors/{}/status | jq
{
  "name": "0-source-foo-customer->0-target-rename-foo-customers.MirrorHeartbeatConnector",
  "connector": {
    "state": "RUNNING",
    "worker_id": "mirrormaker-mirrormaker2-0.mirrormaker-mirrormaker2.mm2.svc:8083"
  },
  "tasks": [
    {
      "id": 0,
      "state": "RUNNING",
      "worker_id": "mirrormaker-mirrormaker2-0.mirrormaker-mirrormaker2.mm2.svc:8083"
    }
  ],
  "type": "source"
}
{
  "name": "0-source-foo-customer->0-target-rename-foo-customers.MirrorCheckpointConnector",
  "connector": {
    "state": "RUNNING",
    "worker_id": "mirrormaker-mirrormaker2-0.mirrormaker-mirrormaker2.mm2.svc:8083"
  },
  "tasks": [
    {
      "id": 0,
      "state": "RUNNING",
      "worker_id": "mirrormaker-mirrormaker2-0.mirrormaker-mirrormaker2.mm2.svc:8083"
    }
  ],
  "type": "source"
}
{
  "name": "0-source-foo-customer->0-target-rename-foo-customers.MirrorSourceConnector",
  "connector": {
    "state": "RUNNING",
    "worker_id": "mirrormaker-mirrormaker2-0.mirrormaker-mirrormaker2.mm2.svc:8083"
  },
  "tasks": [
    {
      "id": 0,
      "state": "RUNNING",
      "worker_id": "mirrormaker-mirrormaker2-0.mirrormaker-mirrormaker2.mm2.svc:8083"
    },
    {
      "id": 1,
      "state": "RUNNING",
      "worker_id": "mirrormaker-mirrormaker2-0.mirrormaker-mirrormaker2.mm2.svc:8083"
    },
    {
      "id": 2,
      "state": "RUNNING",
      "worker_id": "mirrormaker-mirrormaker2-0.mirrormaker-mirrormaker2.mm2.svc:8083"
    }
  ],
  "type": "source"
}

```

# Get offsets of all connector

```shell
kubectl exec $TOOL_POD -- curl -s -X GET http://mirrormaker-mirrormaker2-api.mm2.svc.cluster.local:8083/connectors | jq -r '.[]' | xargs -I {} kubectl exec $TOOL_POD -- curl -s -X GET http://mirrormaker-mirrormaker2-api.mm2.svc.cluster.local:8083/connectors/{}/offsets | jq

{
  "offsets": [
    {
      "partition": {
        "sourceClusterAlias": "0-source-foo-customer",
        "targetClusterAlias": "0-target-rename-foo-customers"
      },
      "offset": {
        "offset": 0
      }
    }
  ]
}
{
  "offsets": [
    {
      "partition": {
        "partition": 1,
        "topic": "rename.foo.customers",
        "group": "c-group"
      },
      "offset": {
        "offset": 0
      }
    }
  ]
}
{
  "offsets": [
    {
      "partition": {
        "cluster": "0-source-foo-customer",
        "partition": 1,
        "topic": "foo.customer"
      },
      "offset": {
        "offset": 4
      }
    }
  ]
}

```

# Stop all connector

  ```shell
  kubectl exec $TOOL_POD -- curl -s -X GET http://mirrormaker-mirrormaker2-api.mm2.svc.cluster.local:8083/connectors | jq -r '.[]' | xargs -I {} kubectl exec $TOOL_POD -- curl -s -X PUT http://mirrormaker-mirrormaker2-api.mm2.svc.cluster.local:8083/connectors/{}/stop | jq
  ```
# Make sure all connectors are stopped

```shell
kubectl exec $TOOL_POD -- curl -s -X GET http://mirrormaker-mirrormaker2-api.mm2.svc.cluster.local:8083/connectors | jq -r '.[]' | xargs -I {} kubectl exec $TOOL_POD -- curl -s -X GET http://mirrormaker-mirrormaker2-api.mm2.svc.cluster.local:8083/connectors/\{\}/status | jq
{
  "name": "0-source-foo-customer->0-target-rename-foo-customers.MirrorHeartbeatConnector",
  "connector": {
    "state": "STOPPED",
    "worker_id": "mirrormaker-mirrormaker2-0.mirrormaker-mirrormaker2.mm2.svc:8083"
  },
  "tasks": [],
  "type": "source"
}
{
  "name": "0-source-foo-customer->0-target-rename-foo-customers.MirrorCheckpointConnector",
  "connector": {
    "state": "STOPPED",
    "worker_id": "mirrormaker-mirrormaker2-0.mirrormaker-mirrormaker2.mm2.svc:8083"
  },
  "tasks": [],
  "type": "source"
}
{
  "name": "0-source-foo-customer->0-target-rename-foo-customers.MirrorSourceConnector",
  "connector": {
    "state": "STOPPED",
    "worker_id": "mirrormaker-mirrormaker2-0.mirrormaker-mirrormaker2.mm2.svc:8083"
  },
  "tasks": [],
  "type": "source"
}
```



Reset offset of all connector, you need to stop the connector and delete the offset immediately if your connector is set to auto restart mode, otherwise it will restart and you will not be able to delete the offset.

```shell
  kubectl exec $TOOL_POD -- curl -s -X GET http://mirrormaker-mirrormaker2-api.mm2.svc.cluster.local:8083/connectors | jq -r '.[]' | xargs -I {} kubectl exec $TOOL_POD -- curl -s -X PUT http://mirrormaker-mirrormaker2-api.mm2.svc.cluster.local:8083/connectors/{}/stop | jq
  kubectl exec $TOOL_POD -- curl -s -X GET http://mirrormaker-mirrormaker2-api.mm2.svc.cluster.local:8083/connectors | jq -r '.[]' | xargs -I {} kubectl exec $TOOL_POD -- curl -s -X DELETE http://mirrormaker-mirrormaker2-api.mm2.svc.cluster.local:8083/connectors/{}/offsets | jq
```

# Start connector again if it is not set to auto restart mode

```shell
kubectl exec $TOOL_POD -- curl -s -X GET http://mirrormaker-mirrormaker2-api.mm2.svc.cluster.local:8083/connectors | jq -r '.[]' | xargs -I {} kubectl exec $TOOL_POD -- curl -s -X PUT http://mirrormaker-mirrormaker2-api.mm2.svc.cluster.local:8083/connectors/{}/start | jq
```


# Clean up

```shell
kubectl delete -f kafka/kafka-cluster-a.yaml -n mm2
helm uninstall mirrormaker -n mm2
helm uninstall strimzi-release -n strimzi
minikube delete -p kafka-cluster
```


# References
- https://stackoverflow.com/questions/75278012/mm2-0-consumer-group-behavior
- https://issues.apache.org/jira/browse/KAFKA-15177
- https://cwiki.apache.org/confluence/display/KAFKA/KIP-875%3A+First-class+offsets+support+in+Kafka+Connect
- https://blog.cloudera.com/a-look-inside-kafka-mirrormaker-2/
- https://issues.apache.org/jira/browse/KAFKA-2333
- https://cwiki.apache.org/confluence/display/KAFKA/KIP-875%3A+First-class+offsets+support+in+Kafka+Connect


