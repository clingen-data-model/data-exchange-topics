(ns topic-utils.util)

(defn get-env-required
  "Performs System/getenv variable-name, but throws an exception if value is empty"
  [variable-name]
  (let [a (System/getenv variable-name)]
    (if (not (empty? a))
      a
      (throw (ex-info (format "Environment variable %s must be provided" variable-name) {})))))
