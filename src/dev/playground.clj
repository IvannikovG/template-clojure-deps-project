(ns dev.playground
  (:require [clj-http.client :as http]
            [clojure.pprint :as pprint]
            [csv-helper.core :as csv-helper]
            [clojure.string :as str]
            [clj-jwt.core  :as jwt]
            [clj-jwt.key   :refer [private-key]]
            [clj-time.core :refer [now plus days]]
            [hickory.core :as h]
            [handlers :as ha]
            [routing :as r]))

;; TODO: graphemes grid-> card
;; groups (select all filtered - all???)

(comment
  (r/handler (atom (assoc-in @core/app-context [:active-route ] :auth.register/register))
             {:body {:password "A"
                     :username "M"
                     :resourceType "app-user"}}))

(def sample-user
    {:name {:first-name "Alex" :last-name "Vith"}
     :password "AND"
     :username "MMM"
     :id "app-user-id"
     :resourceType "app-user"})

#_(def claim

  {:iss "foo"
   :exp (plus (now) (days 30))
   :iat (now)})


(def user-record
  {:username "Havel"
   :email "simple@email.com"
   :active "true"})

(defn give-jwt-token
  [user-record]
  (-> user-record jwt/jwt
      (jwt/sign :HS256 "secret")
      jwt/to-str jwt/str->jwt))

(comment

  (.encodeToString (java.util.Base64/getEncoder)
                   (.getBytes (.encodeToString (java.util.Base64/getEncoder)
                                               (.getBytes "a,b,b"))))

  (String. (.decode (java.util.Base64/getDecoder) "NkwrSExPbUR2U3ptbUs4PQ==") )

  (String. (.decode (java.util.Base64/getDecoder) "6L+HLOmDvSzmmK8=") )



  (jwt/verify (:claims (give-jwt-token {:a 1})) "secret")

  (http/get "https://studychinese.ru/kljuchi/"
            {:accept :json})

  (->> (h/as-hickory (h/parse (slurp "html_resources/keys.html")))
       :content
       last
       :content
       last
       :content
       (filter #(= (:tag %) :table))
       first
       :content
       last
       :content
       (mapcat :content)
       #_first)

  )



;; Job runners
;; 1 - Bad
(defn run-job
  [job]
  (future
    (println "Running JOb...")
    (Thread/sleep 1000)
    (println "Ran job" job)))

(defn job-runner
  [ctx]
  (future
    (repeatedly
     (fn []
       (println "Job watcher is running")
       (let [wait-time 20000
             jobs [1 2 3]]
         (println "Now the jobs start")
         (doseq [job jobs]
           (run-job job))
         (Thread/sleep wait-time))))))

;; 2 - Good. will wrap running processes into atoms
(def counter (atom 1))

(defn infinite-loop [function]
  (function)
  (future (infinite-loop function))
  nil)

(defn make-literal [a]
  (.replace a "\"" "\\\"")
)

(defn extract-anything-between [prefix suffix from-string]
  (let [pattern (str (make-literal prefix) "([\\s\\S]*?)" (make-literal suffix))]
    (second (re-find (re-pattern pattern) from-string))
  )
)


(defn extract-anything-between-combine [prefix suffix from-string]
  (let [pattern (str (make-literal prefix) "([\\s\\S]*?)" (make-literal suffix))]
    (str/join "" (map #(second %) (re-seq (re-pattern pattern) from-string)) )
  )
)

(defn drop-everything-starting-from
  [string symb]
  (reduce
   (fn [acc el]
     (if (= (str el) symb)
       (reduced acc)
       (str el acc)))
   ""
   string))


(comment
  (drop-everything-starting-from  "_, _, -asd,r{:shit \"STHIG\"}" "{")
  (infinite-loop
   #(do
      (Thread/sleep 1000)
      (swap! counter inc)))

  (future
    (Thread/sleep 3000)
    (println 3000 "A")))
