(defproject data-exchange-topics "0.1.0-SNAPSHOT"
  :description "ClinVar data streams for ClinGen ecosystem applications"
  :url "https://github.com/clingen-data-model/clinvar-streams"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/clojure "1.10.1"]
                 [org.clojure/core.async "1.2.603"]
                 [org.clojure/tools.namespace "1.1.0"]
                 [org.clojure/java.jdbc "0.7.11"]
                 [org.clojure/tools.cli "1.0.206"]
                 [io.pedestal/pedestal.service "0.5.7"]
                 [io.pedestal/pedestal.route "0.5.7"]
                 [io.pedestal/pedestal.jetty "0.5.7"]
                 [org.slf4j/slf4j-simple "1.7.28"]
                 [org.apache.kafka/kafka-clients "2.8.0"]
                 [mount "0.1.16"]
                 [cli-matic "0.4.3"]
                 [cheshire "5.10.0"]
                 [clj-commons/fs "1.5.2"]
                 [fundingcircle/jackdaw "0.7.4"]
                 [while-let "0.2.0"]
                 [com.google.cloud/google-cloud-storage "1.115.0"]
                 [com.taoensso/timbre "4.10.0"]
                 [c3p0/c3p0 "0.9.1.2"]
                 [org.postgresql/postgresql "42.2.16"]
                 [org.xerial/sqlite-jdbc "3.32.3.2"]]
  ;:repl-options {:init-ns clinvar-streams.core
  ;               :caught clojure.repl/pst}
  :main topic-utils.topic-metrics
  :aot :all
  :resource-paths ["resources"]
  :target-path "target/%s"
  :auto-clean false
  :profiles {;:uberjar {:uberjar-name "clinvar-streams.jar"
             ;          :aot :all}
             ;:testdata {:main clinvar-raw.generate-local-topic
             ;           ;:aot [#"clinvar-raw.*"]
             ;           :repl-options {:init-ns clinvar-raw.generate-local-topic}}
             })
