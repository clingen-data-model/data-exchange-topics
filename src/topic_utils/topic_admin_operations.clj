(ns topic-utils.topic-admin-operations
  (:import (org.apache.kafka.clients.admin NewTopic
                                           KafkaAdminClient
                                           CreateTopicsOptions)))

(defn app-config 
  []
  {:kafka-host (System/getenv "KAFKA_HOST")
   :kafka-user (System/getenv "KAFKA_USER")
   :kafka-password (System/getenv "KAFKA_PASSWORD")})

(defn get-kafka-config
  [opts]
  {"ssl.endpoint.identification.algorithm" "https"
   "enable.auto.commit" "false"
   "compression.type" "gzip"
   "sasl.mechanism" "PLAIN"
   "bootstrap.servers" (:kafka-host opts)
   "security.protocol" "SASL_SSL"
   "sasl.jaas.config" (str "org.apache.kafka.common.security.plain.PlainLoginModule required username=\""
                           (:kafka-user opts) "\" password=\"" (:kafka-password opts) "\";")})


(defn create-topic 
  "Creates topic with app-config credentials, with default values.
  Throws exception on error."
  [topic-name kafka-config]
  (let [admin-client (KafkaAdminClient/create kafka-config)
        n-partitions 1
        replication-factor 3
        new-topic (NewTopic. topic-name n-partitions replication-factor)]
    (.configs new-topic 
              {"compression.type" "gzip"
               "retention.ms" "-1"
               "retention.bytes" "-1"})
    (let [create-result (.createTopics admin-client [new-topic])
          fut (get (.values create-result) topic-name)]
      ; .get is blocking and throws exception on error in the createTopics operation
      (.get fut)
      fut)))

(defn delete-topic 
  "Deletes topic using app-config credentials.
  Throws exception on error."
  [topic-name kafka-config]
  (let [admin-client (KafkaAdminClient/create kafka-config)
        delete-result (.deleteTopics admin-client [topic-name])
        fut (get (.values delete-result) topic-name)]
    (.get fut)
    fut))

(defn purge-topic
  [topic-name]
  (let [kafka-config (get-kafka-config (app-config))]
    (delete-topic topic-name kafka-config)
    (create-topic topic-name kafka-config)))
