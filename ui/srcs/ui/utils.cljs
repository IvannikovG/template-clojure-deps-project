(ns ui.utils
  (:require-macros [reagent.ratom :refer [reaction]])
  (:require
   [goog.crypt.base64 :as b64]
   [re-frame.core :as rf]
   [clojure.string :as str]
   [goog.string :as gstring]
   [goog.string.format]
   [cljsjs.moment-timezone]
   [cljs-uuid-utils.core :as uuid]
   [ui.routes :as routes]))

(defn has-ancestor
  "Check if node `x` has ancestor `node`"
  [x node]
  (when x
    (if (= (.-nodeName x) "body")
      false
      (or (.isEqualNode x node)
          (recur (.-parentElement x) node)))))

;; FIXME: move to config
(def installation-timezone "America/New_York")

;; Tz formatters.
;; There are three behaviours on different datetime data formats.
;; Say that configured local tz is "America/New_York" (-05:00), then
;; 1. 2017-11-20T10:00:00 -- parsed as 2017-11-20T10:00:00-05:00 and displayed as 10:00
;; 2. 2017-11-20T10:00:00-05:00 -- parsed as it is and displayed as 10:00
;;    2017-11-20T18:00:00+03:00 -- because of config tz will be displayed as 10:00 too
;; 3. 2017-11-20T10:00:00Z -- parsed as it is and displayed as 05:00 because of -05:00 tz

(defn tz-n-format [timezone fmt x]
  (when x
    (.. js/moment (tz x timezone) (format fmt))))

(def format-year-only (partial tz-n-format installation-timezone "YYYY"))
(def format-date-only (partial tz-n-format installation-timezone "YYYY-MM-DD"))
(def format-date (partial tz-n-format installation-timezone "MM/DD/YYYY"))
(def format-date-short (partial tz-n-format installation-timezone "MM/DD/YY"))
(def format-date-time (partial tz-n-format installation-timezone "MM/DD/YYYY hh:mm A"))
(def format-time (partial tz-n-format installation-timezone "hh:mm A"))
(def format-day-month (partial tz-n-format installation-timezone "MMM Do"))
(def format-day-month-short (partial tz-n-format installation-timezone "MM/DD"))
(def format-day-month-year (partial tz-n-format installation-timezone "MMM D, YYYY"))
(def format-day-month-year-time
  (partial tz-n-format installation-timezone "MMM D, YYYY hh:mm A"))

(defn date-time->utc [date time]
  (.. js/moment (tz (str date " " time) installation-timezone) utc format))

(defn now-utc-string []
  (.. (js/moment.) utc format))

(defn now-utc-string-msc []
  (.format (.utc (js/moment.)) "YYYY-MM-DDTHH:mm:ss.SSSZ"))

(defn now-tz []
  (.. (js/moment.) (tz installation-timezone)))

(defn to-moment [d]
  (.. (js/moment. d) (tz installation-timezone)))

(defn local-today-string []
  (.. (js/moment.) (tz installation-timezone) (format "YYYY-MM-DD")))

(defn time-now []
  (.. (js/moment.) (tz installation-timezone) (format "hh:mm")))

(defn age [d]
  (when d
    (.. (js/moment) (diff d "years"))))

(defn days-ago [d]
  (when d
    (.. (js/moment) (diff d "days"))))

(defn initials [name]
  (let [[last first] (when name
                       (str/split name #","))]
    (when (and first last)
      (str (subs (str/trim first) 0 1) (subs (str/trim last) 0 1)))))

(defn subtract-from-now [amount units]
  (.. (js/moment) (subtract amount units) utc format))

(defn add-to-now [amount units]
  (.. (js/moment) (add amount units) utc format))

(defn day-start-end-utc [date]
  [(.. js/moment (tz date installation-timezone) (startOf "day") utc format)
   (.. js/moment (tz date installation-timezone) (endOf "day") utc format)])


(rf/reg-sub-raw :db/by-path
                (fn [db [_ path]]
                  (reaction (get-in @db path))))

(defn get-by-sys
  "Get value from `identifiers` vector by `system`"
  [identifiers system]
  (->> identifiers
       (filter #(= (:system %) system))
       first
       :value))

(defn parse-float [x]
  (let [px (js/parseFloat x)]
    (when-not (js/isNaN px)
      (gstring/format "%.2f" px))))


(defn from-money [x] (-> x
                         js/parseFloat
                         (.toFixed 2)
                         js/parseFloat))

(defn money [x] (or x 0))

(defn *fmt-money [x]
  (let [px (js/parseFloat x)
        third-digit-after-dot (mod (js/Math.round (* 1000 px)) 10)]
    (cond (js/isNaN px) "-"
          (= 0 third-digit-after-dot) (gstring/format "%.2f" px)
          :else (gstring/format "%.3f" px))))

(defn fmt-money [x]
  (str "$ " (*fmt-money x)))

(defn money+     [& l] (->> l (map money) (apply +) from-money))
(defn money-     [& l] (->> l (map money) (apply -) from-money))
(defn money*     [& l] (->> l (map #(or % 0)) (apply *) money from-money))
(defn money-quot [& l] (->> l (map #(or % 0)) (apply /) money from-money))
(defn money-sum [l]
  ;; (reduce money+ 0 l)
  (from-money (reduce (fn [a t] (+ a (money t))) 0 l)))


(defn breadcrumbs
  [crumbs]
  [:div.breadcrumbs
   (for [crumb crumbs]
     (let [{:keys [text href]} crumb]
       [:span
        {:key text}
        " / "
        (if href
          [:a {:href href} text]
          text)]))])


(defn filter-nils [m] (reduce (fn [a [k v]] (if (nil? v) a (assoc a k v))) {} m))

(defn to-int [x] (js/parseInt x))

(defn to-float [x] (js/parseFloat x))

(def b64-encode b64/encodeString)

(defn normalize-name [s]
  (-> (str/lower-case (or s ""))
      (str/split #" ")
      (->> (map str/capitalize)
           (str/join " "))))

(defn shorten-id [id]
  (str \# (apply str (take 5 id))))

(defn gen-uuid []
  (uuid/uuid-string (uuid/make-random-uuid)))

(defn parse-id-token [jwt]
  (let [[header payload sig] (str/split jwt #"\.")]
    (js->clj (.parse js/JSON (js/atob payload))
             :keywordize-keys true)))

(defn edn-to-json [edn]
  (js/JSON.stringify (clj->js edn)))

(defn phone-format [num]
  (str "(" (subs num 0 3) ") " (subs num 3 6) "–" (subs num 6 10)))


(defn build-class [h-map]
  (str/join "." (reduce-kv (fn [acc k v]
                             (if k
                               (conj acc v)
                               acc)) [] h-map)))

(defn uri-encode [s]
  (js/encodeURIComponent s))

(defn pagination-links
  [{:keys [uri count total]}
   {page :page :as params}]
  (let [last-page (int (Math/ceil (/ total count)))
        cur-page (if page (to-int page) 1)
        first-page 1
        offset 2
        page-nums (-> [cur-page]
                      (concat (range first-page 3))
                      (concat (range (- last-page 1) (+ last-page 1)))
                      (concat (range (- cur-page offset) cur-page))
                      (concat (range (+ cur-page 1) (+ cur-page offset 1)))
                      distinct
                      (as-> pgs (filter pos-int? pgs))
                      (as-> pgs (filter #(<= % last-page) pgs))
                      sort)]
    (map
     (fn [[prev page-num]]
       {:url (routes/href uri (assoc params :page page-num))
        :offset (and prev
                     (not= (inc prev)
                           page-num))
        :current (= page-num cur-page)
        :page page-num})
     (partition 2 1 (cons nil page-nums)))))
