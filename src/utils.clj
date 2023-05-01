(ns utils
  (:require [clojure.walk :as walk]
            [clj-time.coerce :refer [from-string] :as c]
            [clj-time.core :as time]
            [clj-jwt.core  :as jwt]
            [clj-jwt.key   :refer [private-key]]
            [clj-time.format :as f]
            [clj-time.local :as l]
            [clojure.data :as d]
            [context :as context]
            [clojure.string :as str]
            #_[next.jdbc.connection :as conn]
            #_[hikari-cp.core :refer [make-datasource close-datasource]]
            #_[next.jdbc :as jdbc
             :refer [get-datasource]])
  (:import
   [java.util UUID]
   [java.security MessageDigest]
   [java.util.concurrent Executors ExecutorService]))

(defn atom? [a]
  (instance? clojure.lang.Atom a))

(defn deatomize [a]
  (if (atom? a) @a a))

(def dev-db-spec2
  {:dbtype               "postgres"
   :dbname               "general_db"
   :classname            "org.postgresql.Driver"
   :user                 "postgres" ;; to enable datasource support
   #_#_:connectionInitSql    "COMMIT;"
   #_#_:connectionTestQuery  "select 'Hello from test_car_reporter (TEST@!)';"
   :host                 (or (System/getenv "PGHOST")
                             "localhost")
   :port                  6434
   :password             "postgres"
   :dataSourceProperties {:socketTimeout 15}})


(def test-db-spec
  {:dbtype               "postgres"
   :dbname               "test_general_db"
   :classname            "org.postgresql.Driver"
   :user                 "postgres" ;; to enable datasource support
   #_#_:connectionInitSql    "COMMIT;"
   #_#_:connectionTestQuery  "select 'Hello from test_car_reporter (TEST@!)';"
   :host                 (or (System/getenv "PGHOST")
                             "localhost")
   :port                  6435
   :password             "postgres"
   :dataSourceProperties {:socketTimeout 15}})

(defn test-ds
  []
  test-db-spec)

(defn verify-jwt
  [jwt k]
  (try
    (jwt/verify (jwt/str->jwt jwt) k)
    (catch Exception e nil)))

(defn sign-jwt
  [record secret]
  (-> record jwt/jwt
      (jwt/sign :HS256 secret)
      jwt/to-str))

(defn get-jwt-claims
  [jwt-string]
  (:claims (jwt/str->jwt jwt-string)))

(defn str-to-int [s] (try (Integer/parseInt s) (catch Exception e 0)))

(defmacro persist-scope
  "Takes local scope vars and defines them in the global scope. Useful for RDD"
  []
  `(do ~@(map (fn [v] `(def ~v ~v))
              (keys (cond-> &env (contains? &env :locals) :locals)))))

(defn safe-to-int [strint]
  (try (Integer/parseInt strint)
       (catch Exception err nil)))

(defn safe-bigdec [n]
  (when n
    (try (bigdec n)
         (catch Exception _ nil))))

(defn to-iso [d]
  (f/unparse (f/formatter :date-time) d))

(defn today-minus-days
  [days]
  (to-iso (time/minus (time/now) (time/days days))))

(defn today-exact-string
  []
  (to-iso (time/now)))

(defn today-exact
  []
  (time/now))

(defn today-plus-days
  [days]
  (to-iso (time/plus (time/now) (time/days days))))

(defn is-number?
  [s]
  (safe-to-int s))

(defn round-to-zero
  [number]
  (if (> 0 number)
    0
    number))

(defn uuid [] (str (UUID/randomUUID)))

(defn valid-token?
  [token-timestamp]
  (let [token-timestapm' (or (safe-bigdec token-timestamp) 1)
        now-timestamp (System/currentTimeMillis)]
    #_(println "TS" now-timestamp token-timestapm' (<= now-timestamp token-timestapm'))
    (<= now-timestamp token-timestapm')))

(defn when-done
    [future-to-watch function-to-call]
    (future (function-to-call @future-to-watch)))

(defn connection [ctx]
  (let [d-a-ctx     (deatomize ctx)
        test?       (= (-> ctx :config :app-env) "test")
        active-conn (get-in d-a-ctx [:active-db-connection])]
    (prn "ACT:::" active-conn)
    #_(persist-scope)
    (if test?
     active-conn
     active-conn
     #_@active-conn)))

(defn now [] (new java.util.Date))

(defn day-from-now [] (time/plus (time/now) (time/hours 24)))

(comment
  (time/date-time (time/now))
  (c/to-long (day-from-now)))

(defn compare-dates
  [f l]
  (time/after? f l))

(defn take-n
  [coll index-1 index-2]
  (mapv coll (range index-1 index-2)))

(defn timestamp-from
  [date]
  (c/to-long date))

(defn date-string
  [date]
  (.format (java.text.SimpleDateFormat. "yyyy-MM-dd") date))

(def custom-formatter (f/formatter :basic-date-time))

(defn from-tz
  [date-str]
  (c/from-string date-str))

(defn from-str [date-str]
  (from-string date-str))

(defn now-string [] (.format (java.text.SimpleDateFormat. "yyyy-MM-dd") (new java.util.Date)))
(defn sql-now [] (c/to-sql-date (now)))

(defn md5 [^String s]
  (let [algorithm (MessageDigest/getInstance "MD5")
        raw (.digest algorithm (.getBytes s))]
    (format "%032x" (BigInteger. 1 raw))))

(def random (java.util.Random.))
(def chariks (map char (concat (range 48 58)
                               (range 66 92)
                               (range 97 123))))
(defn random-char []
  "Untested func. Generates a random character"
  (nth chariks (.nextInt random (count chariks))))
(defn random-string [length]
  "Generates a random string of length n"
  (apply str (take length (repeatedly random-char))))
(defn random-sql-date []
  (c/to-sql-date (time/date-time (rand-nth (range 1940 2020))
                                 (rand-nth (range 1 13))
                                 (rand-nth (range 1 28)))))
