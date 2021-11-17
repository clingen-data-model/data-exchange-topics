(ns topic-utils.topic-metrics
  (:require [topic-utils.util :as util]
            [clojure.java.shell :as shell]
            [clojure.string :as s]
            [clojure.java.io :as io]
            [clojure.datafy :refer [datafy]]
            [clojure.pprint :refer [pprint]]
            [clojure.spec.alpha :as spec]
            [jackdaw.client :as jc])
  (:import (java.time Duration Instant)
           (org.apache.kafka.common TopicPartition PartitionInfo)
           (org.apache.kafka.clients.consumer KafkaConsumer)))

(def env (System/getenv))
(def poll-duration (Duration/ofSeconds 5))
(defn app-config []
  {:kafka-host "pkc-4yyd6.us-east1.gcp.confluent.cloud:9092"
   :kafka-user (get env "KAFKA_USER")
   :kafka-password (get env "KAFKA_PASSWORD")})

(defn read-cluster-configs
  "Returns the contents of file-name parsed with read-string. Expects an edn map
  of cluster name to :user :password maps"
  [file-name]
  (let [config (-> file-name io/file slurp read-string)]
    config))

(defn get-kafka-config
  [opts]
  {"ssl.endpoint.identification.algorithm" "https"
   "enable.auto.commit" "false"
   "compression.type" "gzip"
   "sasl.mechanism" "PLAIN"
   "bootstrap.servers" (:kafka-host opts)
   "key.serializer" "org.apache.kafka.common.serialization.StringSerializer"
   "value.serializer" "org.apache.kafka.common.serialization.StringSerializer"
   "key.deserializer" "org.apache.kafka.common.serialization.StringDeserializer"
   "value.deserializer" "org.apache.kafka.common.serialization.StringDeserializer"
   "security.protocol" "SASL_SSL"
   "sasl.jaas.config" (str "org.apache.kafka.common.security.plain.PlainLoginModule required username=\""
                           (:kafka-user opts) "\" password=\"" (:kafka-password opts) "\";")})

;(defn ^KafkaConsumer anonymous-consumer []
;  (jc/consumer (get-kafka-config (app-config))))

(defn get-max-offset
  [consumer topic-name partition-num]
  ;(jc/subscribe consumer [{:topic-name topic-name}])
  (jc/assign consumer (TopicPartition. topic-name partition-num))
  (jc/seek-to-end-eager consumer)
  (jc/position consumer (TopicPartition. topic-name partition-num)))

(defn get-min-offset
  [consumer topic-name partition-num]
  ;(jc/subscribe consumer [{:topic-name topic-name}])
  (jc/assign consumer (TopicPartition. topic-name partition-num))
  (jc/seek-to-beginning-eager consumer)
  (jc/position consumer (TopicPartition. topic-name partition-num)))

(defn topic-partitions
  "Returns a seq of TopicPartitions that exist for topic-name."
  [consumer topic-name]
  (let [partition-infos (.partitionsFor consumer topic-name)]
    (map #(TopicPartition. (.topic %) (.partition %)) partition-infos)))

(defn message-at-offset
  "Given a seq of jackdaw datafied Kafka records, return the max offset for
  each topic partition represented in the seq. Returns a seq of maps of form:
  {:topic-name ... :partition ... :offset ...}"
  [consumer topic-name partition-num offset]
  (jc/assign consumer [(TopicPartition. topic-name partition-num)])
  (jc/seek consumer (TopicPartition. topic-name partition-num) offset)
  (jc/poll consumer (Duration/ofSeconds 5)))


; (defn read-nl-file
;   "Reads the list of terms from a given filename, newline separated.
;    Trims start and end whitespace from each line."
;   [filename]
;   (-> filename
;       io/file
;       slurp
;       (s/split #"\n")
;       (->> (map s/trim))))

(defn list-topics
  "Given a consumer, list the topics available to it.
   This is based on credentials, not subscription state.

   Return value is a map of topic-name to a list of PartitionInfo objects."
  ;; .listTopics returns a HashMap. into {} makes it iterable as KVPs tuples instead of HashMap$Nodes
  [consumer]
  ; public Map<String,List<PartitionInfo>> listTopics ()
  ;(into {} (map #(vector (first %)
  ;                       (map datafy (second %)))
  ;              (into {} (.listTopics consumer))))
  (into {} (.listTopics consumer)))

(defn get-all-topic-infos
  [consumer]
  (let [topic-map (list-topics consumer)
        topic-infos []]))

(defn PartitionInfo->TopicPartition
  [^PartitionInfo partition-info]
  (TopicPartition. (.topic partition-info) (.partition partition-info)))

(defn get-first-message
  "Seeks to before the first message and polls, returning the first of the fetched messages.
  Assumes some message exists."
  [consumer ^TopicPartition topic-partition]
  (.seekToBeginning consumer [topic-partition])
  (first (jc/poll consumer poll-duration)))

(defn get-last-message
  "Seeks to offset before the end of the topic and polls for that message.
  Assumes some message exists."
  [consumer ^TopicPartition topic-partition]
  (.seekToEnd consumer [topic-partition])
  (let [position (.position consumer topic-partition)]
    (if (< 0 position)                                      ; if no messages are on topic, the result is this will poll an empty seq
      (jc/seek consumer topic-partition (dec position)))
    (first (jc/poll consumer poll-duration))))

(defn get-watermark-offsets
  [^KafkaConsumer consumer topic-partition]
  (let []
    (.seekToBeginning consumer [topic-partition])
    (let [lower-offset (.position consumer topic-partition)]
      (.seekToEnd consumer [topic-partition])
      (let [upper-offset (.position consumer topic-partition)]
        [lower-offset upper-offset]))))

(defn partition-metrics
  [consumer topic-partition]
  (let [[first-offset last-offset] (get-watermark-offsets consumer topic-partition)
        first-message (get-first-message consumer topic-partition)
        last-message (get-last-message consumer topic-partition)
        first-ts (:timestamp first-message)
        last-ts (:timestamp last-message)]
    {:topic (.topic topic-partition)
     :partition (.partition topic-partition)
     :first-timestamp first-ts
     :first-timestamp-iso8601 (if first-ts (.toString (Instant/ofEpochMilli first-ts)))
     :last-timestamp (:timestamp last-message)
     :last-timestamp-iso8601 (if last-ts (.toString (Instant/ofEpochMilli last-ts)))
     :next-offset (inc last-offset)
     :message-count (if (= 0 last-offset) 0 (+ 1 (- last-offset first-offset)))}))

(defn get-all-topic-infos [app-config]
  (letfn [(make-consumer [] (jc/consumer (get-kafka-config app-config)))]
    (let [; Get topic + partitions map, convert PartitionInfos to TopicPartitions
          topic-map (into {} (mapv #(vector (first %) (mapv PartitionInfo->TopicPartition (second %)))
                                   (list-topics (make-consumer))))
          ;topic-map (into {} (filter #(= "broad-dsp-clinvar" (first %)) topic-map))
          ]
      (pmap (fn [[topic-name topic-partitions]]
              (let [consumer (make-consumer)]
                (jc/assign-all consumer [topic-name])
                [topic-name (mapv #(partition-metrics consumer %) topic-partitions)]))
            topic-map))))

(defn -main [& args]
  (let [topic-infos
        (into {} (for [[cluster-name cluster-config] (read-cluster-configs "cluster-configs.edn")]
                   [cluster-name (doall (get-all-topic-infos cluster-config))]))]
    (pprint topic-infos)))
