(ns ui.utils
  (:import java.security.MessageDigest [java.net URI URLEncoder])
  (:require
   [clojure.data.codec.base64 :as b64]
   [clj-time.format :as tformat]
   [clj-time.core :as time]
   [cleo.queries :as queries]
   [cheshire.core :as json]
   [app.db :as db]
   [clojure.string :as str]))

(def tz (time/time-zone-for-id  "America/New_York"))

(def us-format (tformat/formatter "M/d/YY h:mm a"))
(def us-day-format (tformat/formatter "M/d/YY"))
(def us-day-format-short  (tformat/formatter "M/d/Y"))
(def us-time-format (tformat/formatter "h:mm a"))

(defn multi-formatter [tz]
  (tformat/formatter tz
                (tformat/formatter :date-hour-minute-second)
                (tformat/formatter :date-time-no-ms)
                (tformat/formatter :date-time)
                (tformat/formatter :date)
                (tformat/formatter :date-hour-minute-second-ms)
                (tformat/formatter "M/d/Y")))

(defn parse-us-date [s]
  (tformat/parse (tformat/formatter "M/d/Y") s))

(defn date-us->date-only [s]
  (->>
   (parse-us-date s)
   (tformat/unparse (tformat/formatter :date))))

(defn parse-date [s]
  (tformat/parse (multi-formatter tz) s))

(defn age [x])

(defn format-year-only [x]
  (tformat/unparse (tformat/formatter :year) (parse-date x)))

(defn format-date-only [x]
  (tformat/unparse (tformat/formatter :date) (parse-date x)))

(defn format-date [x]
  (tformat/unparse us-day-format (parse-date x)))

(defn format-date-short [x]
  (tformat/unparse us-day-format-short (parse-date x)))

(defn format-date-time [x]
  (tformat/unparse us-format (parse-date x)))

(defn to-iso [d]
  (tformat/unparse (tformat/formatter :date-time) d))

(defn now-utc-string []
  (to-iso (time/now)))

(defn local-today-string []
  (tformat/unparse (tformat/formatter :year-month-day) (time/to-time-zone (time/now) tz)))

(defn day-start-end-utc [date]
  (let [start (time/plus (tformat/parse date) (time/hours 4))]
    [start (-> start (time/plus (time/days 1)) (time/minus (time/seconds 1)))]))

(defn format-time [x] x)

(defn days-ago [d]
  (when d
    (time/in-days
     (time/interval (parse-date d)
                    (time/now)))))

(def time-func-mapping
  {"years"   #'time/years
   "months"  #'time/months
   "weeks"   #'time/weeks
   "days"    #'time/days
   "hours"   #'time/hours
   "minutes" #'time/minutes
   "seconds" #'time/seconds})

(defn subtract-from-now [amount units]
  (to-iso (time/minus (time/now) ((get time-func-mapping units)  amount))))


(defn parse-float [x]
  (let [px (try (Float/parseFloat x) (catch Exception _ nil))]
    (when px
      (format "%.2f" px))))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; money calcultions (exactly duplicates with ui or backend utils)
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn from-money [x] (double (/ x 100)))

(defn money [x] (Math/round (* 100.0 (or x 0))))

(defn fmt-money [x] (try (str "$ " (format "%.2f" (double x))) (catch Exception e nil)))

(defn *fmt-money [x] (try (format "%.2f" (double x)) (catch Exception e nil)))

(defn money+     [& l] (->> l (map money) (apply +) from-money))
(defn money-     [& l] (->> l (map money) (apply -) from-money))
(defn money*     [& l] (->> l (map #(or % 0)) (apply *) money from-money))
(defn money-quot [& l] (->> l (map #(or % 0)) (apply /) money from-money))
(defn money-sum [l]
  ;; (reduce money+ 0 l)
  (from-money (reduce (fn [a t] (+ a (money t))) 0 l)))




(defn now-utc-string [] (str (time/now)))

(defn now-utc-string-msc [] (str (time/now)))

(defn filter-nils [m] (reduce (fn [a [k v]] (if (nil? v) a (assoc a k v))) {} m))

(defn to-int [x] (try (Integer/parseInt (str x)) (catch Exception e 0)))

(defn to-float [x] (try (Double/parseDouble (str x)) (catch Exception e 0)))

(defn str-to-double [s] (try (Double/parseDouble s) (catch Exception e 0.0)))

(defn str-to-int [s] (try (Integer/parseInt s) (catch Exception e 0)))


(defn b64-encode [s] (String. (b64/encode (.getBytes s))))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; from backend
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn sha1-str [s]
  (->> (-> "sha1"
           java.security.MessageDigest/getInstance
           (.digest (.getBytes s)))
       (map #(.substring
              (Integer/toString
               (+ (bit-and % 0xff) 0x100) 16) 1))
       (apply str)))

;; postgres sql query helpers

(defn sql-date-from-NY-to-utc-string [x]
  (str
   "to_char(((("
   x
   ")::timestamp AT TIME ZONE 'America/New_York') AT TIME ZONE 'UTC'), 'YYYY-MM-DD\"T\"HH24:MI:SS\"Z\"')"))

(defn sql-utc-string-begin-of-NY-day [x] (sql-date-from-NY-to-utc-string (str "'" x "T00:00:00.000" "'")))

(defn sql-utc-string-end-of-NY-day   [x] (sql-date-from-NY-to-utc-string (str "'" x "T23:59:59.999" "'")))

;; Encounter params

(defn get-enc-price-params [db enc-id]
  (when enc-id
    (db/qf db ["select
                l.resource#>>'{address, state}' as state,
                case when (coalesce(knife_extract_text(e.resource,
                                           '[[\"coverage\",\"resource\",\"type\",\"coding\",
                                              {\"system\":\"http://hl7.org/fhir/coverage-selfpay\"},
                                              \"code\"]]'), '{???}'))[1] = 'pay'
                     then 'self'
                     -- when c.resource#>>'{type, coding, 0, code}' in ('Medicaid', 'Medicare')
                     when to_jsonb(knife_extract(c.resource, '[\"type\", \"coding\", \"code\"]')) @> '[\"Medicaid\", \"Medicare\"]'::jsonb
                     then 'medicare'
                     else 'commercial'
                end as type
              from encounter e
              left join coverage c on e.resource#>>'{coverage, id}' = c.id
              left join location l on e.resource#>>'{location, 0, location, id}' = l.id
              where e.id = ?"
               enc-id])))

(defn price-for [db enc-id cpt]
  (let [{:keys [state type]} (get-enc-price-params db enc-id)
        svc (db/qfr db (queries/svc-by-cpt cpt))]
    [(:id svc)
     (:value (first (filter #(and (= (:type %) type) (= (:state %) state)) (:price svc))))
     (str cpt ": " (:name svc))]))


(defn normalize-name [s]
  (-> (str/lower-case (or s ""))
      (str/split #" ")
      (->> (map str/capitalize)
           (str/join " "))))

(defn gen-uuid []
  (.toString (java.util.UUID/randomUUID)))


(defn parse-id-token [jwt]
  (let [[header payload sig] (str/split jwt #"\.")]
    (json/parse-string (String. (.decode (java.util.Base64/getDecoder) payload)) keyword)))

(defn edn-to-json [edn]
  (json/generate-string edn))

(defn shorten-id [id]
  (str \# (apply str (take 5 id))))

(defn phone-format [num]
  (str "(" (subs num 0 3) ") " (subs num 3 6) "–" (subs num 6 10)))


(defn uri-encode [s]
  (URLEncoder/encode s "UTF-8"))

(comment

  (fmt-money 1.255)

  (format-date (now-utc-string))
  (format-date-time (now-utc-string))

  (tformat/parse (tformat/formatters :date-time) "2001-01-01T10:00:00Z")

  )
