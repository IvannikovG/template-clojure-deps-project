(ns ui.zenform.model
  #?(:cljs (:require-macros [reagent.ratom :refer [reaction run!]]))
  (:require
   [clojure.string :as str]
   [ui.zenform.validators :refer [validate]]
   [clojure.set :as set]
   [ui.zenform.validators :as v]
   #?(:cljs  [reagent.core :as r])
   [re-frame.core :as rf]
   [ui.zenform.zmethods :as zm]))

(def parseInt #?(:clj  (fn [^String s] (Integer/parseInt s))
                 :cljs (fn [^String s] (js/parseInt s))))

(def parseFloat #?(:clj  (fn [^String s] (Float/parseFloat s))
                   :cljs (fn [^String s] (js/parseFloat s))))

(defn parse-int [s]
  (when-let [x (re-find #"^[-+]?\d+$" (str s))]
    (parseInt x)))

(defn parse-float [s]
  (when-let [x (re-find #"^[-+]?\d+(?:\.\d+)?$" (str s))]
    (parseFloat x)))

(defmulti coerce (fn [tp _] tp))

(defmethod coerce :number [_ v]
  ;; it is unclear what number means so lets use float here
  (if (number? v) v (if-let [parsed-v (parse-float v)] parsed-v :no-coerce)))

(defmethod coerce :integer [_ v]
  (if (number? v) v (if-let [parsed-v (parse-int v)] parsed-v :no-coerce)))

(defmethod coerce :float [_ v]
  ;; Number check because some tests fails
  (if (number? v) v (if-let [parsed-v (parse-float v)] parsed-v :no-coerce)))

(defmethod coerce :text [_ v] (str v))
(defmethod coerce :string [_ v] (str v))
(defmethod coerce :object [_ v] v)

(defn errors [vals v tp]
  (not-empty
   (reduce-kv
    (fn [errs nm cfg]
      (let [general-validator (get v/validators nm)
            f (or (:f cfg) (:f general-validator))
            message (or (:message cfg) (:message general-validator))]
        (if (and f (not (f (if (and tp v) (coerce tp v) v))))
          (assoc errs nm message)
          errs)))
    {} vals)))

(defn mk-form-data [sch path val]
  (cond
    (= "form" (:type sch))
    (assoc sch :value
           (reduce (fn [acc [k *sch]]
                     (let [v (get val k)
                           init (:init *sch)]
                       (assoc acc k
                              (cond->
                                (assoc *sch
                                       :value (mk-form-data *sch (conj path k) v)
                                       :path (conj path k))
                                init  (assoc :state (zm/init-input init *sch v))))))
                   {} (:fields sch)))

    (= "coll" (:type sch))
    (->> (map-indexed (fn [i *val]
                        [i (mk-form-data
                            (dissoc (:item sch) :value)
                            (conj path i)
                            *val)]) val)
         (into {}))

    :else val))

(defn get-value [form-data]
  (cond
    (= (:type form-data) "form")
    (reduce-kv (fn [m k v]
                 ;; FIXME remove hack here
                 (if (and (contains? v :value) (or (nil? (:value v)) (:remove v)))
                   m (assoc m k (get-value v))))
               {} (:value form-data))

    (= (:type form-data) "coll")
    (->> (:value form-data)
         (mapv (fn [[i v]] [i (get-value v)]))
         (sort-by first)
         (mapv second))

    :else
    (if-let [tp (:type form-data)]
      (coerce tp (:value form-data))
      (let [fd (:value form-data)]
        (if (or (sequential? fd)
                (string? fd))
          (not-empty fd)
          fd)))))

(defmulti transform (fn [name dir _] [name dir]))

; ;; FIXME it seems that from cljs environment you can not defmethod multis that are defined in other namespace
; (defmethod transform [:transform/snapshot :to]
;   [_ _ resource]
;   (let [res {:resourceType (:resourceType resource)
;              :resource (dissoc resource :display)
;              :display (:display resource)}]
;     (merge res (select-keys resource [:id :uri]))))

; (defmethod transform [:transform/snapshot :from]
;   [_ _ resource]
;   (assoc (:resource resource) :display (:display resource)))

; (defmethod transform :default
;   [name dir]
;   (println "Error: No transform found for " name dir))

(defn transform? [el]
  (and (keyword? el) (= (namespace el) "transform")))

(defn fold-path [path value]
  (reduce
   (fn [value el]
     (cond
       (transform? el)
       (transform el :to value)

       (keyword? el)
       {el value}

       (vector? el)
       [value]

       (map? el)
       (merge el value)

       (fn? el)
       (el :to value)))
   value (reverse path)))

(defn existing-path [path value]
  (loop [[el & rest] path
         v value
         p []]
    (cond
      (nil? el) [p nil]

      (or (transform? el) (fn? el))
      [p (conj rest el)]

      (vector? el)
      (let [next (first rest)]
        (cond
          (or (transform? next) (fn? next))
          [p (conj rest el)]

          (map? next)
          (let [r (filter
                   (fn [[n v]]
                     (set/subset? (set next) (set v)))
                   (map-indexed vector v))]
            (if-let [[n v] (first r)]
              (recur rest v (conj p n))
              [p (conj rest el)]))

          (keyword? next)
          (throw "Not implemented: zenform/existing-path for [[] :a]")))


      (map? el)
      (recur rest v p)

      (get v el)
      (recur rest (get v el) (conj p el))

      :else
      [(conj p el) rest])))

;; TODO move to tests once zenform is extracted
(comment
  (existing-path [:a [] :b] {:a [{:b 1}]})
  (existing-path [:a [] {:a 1} :c] {:a []})
  (existing-path [:a [] {:a 1} :c] {:a [{:a 1}]})
  (existing-path [identity :a :b] {:a 1})
  (existing-path [:a [] identity] {:a [{:b 2}]})
  )

(defn form-value [form-data]
  (let [paths (map (fn [[k v]]
                     {:id k :path (get-in form-data [:fields k :path]) :value v})
                   (get-value form-data))]
    (reduce
     (fn [acc {:keys [id path value]}]
       (if path
         (let [[ex-p tr-p] (existing-path path acc)
               new-v (fold-path tr-p value)]
           (if (empty? ex-p)
             (merge acc new-v)
             (update-in acc ex-p (fn [v]
                                   (if (and (coll? new-v) (coll? v))
                                     (into v new-v)
                                     new-v)))))
         (assoc acc id value)))
     {} paths)))

(defn get-path  [pth]
  (reduce (fn [acc x]
            (conj acc :value x))
          [] pth))

(defn get-form-level-errors [validators form-plain-value]
  (->> validators
       (map (fn [v-name]
              (reduce-kv (fn [acc k v]
                           (assoc-in acc [k v-name] v))
                         {} (validate v-name form-plain-value))))
       (apply merge-with merge)))

(defn add-errors-to-db [db form-path errors global-errors]
  (-> db
      (update-in (into form-path [:value])
                 (fn [inps]
                   (reduce-kv
                     (fn [inps nm errs]
                       (assoc-in inps [nm :errors] errs))
                     inps errors)))
      (assoc-in (conj form-path :form-global-errors) global-errors)
      (assoc-in (conj form-path :form-errors) errors)))

(defn get-validators [d]
  (->> (:validators d)
       (filter (comp #{:change} :event val))
       (map (fn [[k cfg]]
              {k (assoc
                   cfg
                   :regular?
                   (or (:f cfg)
                       (:f (get v/validators k))))}))))

(defn set-value [db fp p v]
  (let [pth               (into fp (get-path p))
        d                 (get-in db pth)
        validators        (get-validators d)
        regular?          (comp :regular? first vals)
        regular-vs        (apply merge (filter regular? validators))
        global-vs         (map (comp first keys) (remove regular? validators))
        d*                (merge d {:value v
                                    :touched true
                                    :errors (errors regular-vs v (:type d))})
        db                (assoc-in db pth d*)
        form-plain-value  (get-value (get-in db fp))
        form-level-errors (get-form-level-errors global-vs form-plain-value)
        db (cond-> db
             (not-empty global-vs)
             (assoc-in (conj fp :form-global-errors) (:global form-level-errors)))]
    (loop [lp p]
      (let [ipth (into fp (get-path lp))
            node (get-in db ipth)]
        (when-let [on-change (:on-change node)]
          (rf/dispatch [(:event on-change)
                        (assoc on-change
                               :value (get-value node)
                               :form-path fp
                               :path lp)]))
        (when-not (empty? lp)
          (recur (butlast lp)))))
    db))

(rf/reg-event-db
 :zenform/on-change
 (fn [db [_ fp p v]]
   (set-value db fp p v)))

#?(:cljs
   (rf/reg-sub-raw
    :zenform/form-path
    (fn [db [_ fp p]]
      (let [cur (r/cursor db (into fp (get-path p)))]
        (reaction @cur)))))
#?(:clj
   (rf/reg-sub-raw
    :zenform/form-path
    (fn [db [_ fp p]]
      (get-in db (into fp (get-path p))))))

;; TODO add recursive forms case
#_(defn form-errors [form]
  (reduce-kv (fn [errs nm field]
               (if (contains? field :validators)
                 (if (not (:touched field))
                   (assoc errs nm (errors (:validators field) (:value field)))
                   (if-let [er (:errors field)]
                     (assoc errs nm er)
                     errs))
                 errs))
             {} (:value form)))

(defn form-errors
  ([x] (form-errors :errors x))
  ([id x] (cond
            (= "form" (:type x)) (reduce-kv (fn [acc k v] (merge acc (form-errors k v))) {} (:value x))

            (= "coll" (:type x)) (/ 1 0) ;; FIXME add coll case

            :else (if-let [e (:errors x)] {id e}))))

(rf/reg-event-fx
 :zenform/action
 (fn [{db :db} [_ form-path action & params]]
   (let [form (get-in db form-path)
         {:keys [validate? event clear? validators]} (get-in form [:actions action])
         end (cond-> {}
               event (assoc :dispatch (conj (into [event] params) (form-value form)))
               clear? (assoc :db (assoc-in db form-path nil)))]
     (if validate?
       (let [form-plain-value (get-value form)
             form-level-errors (get-form-level-errors validators form-plain-value)
             errors (->> (:value form)
                         (filter (fn [[k v]] (contains? v :validators)))
                         (reduce
                           (fn [acc [nm inp]]
                             (let [vals (->> (:validators inp)
                                             (filter (fn [[n c]]
                                                       (= (:event c) :submit)))
                                             (into {}))
                                   errs (errors vals (:value inp) (:type inp))]
                               (if errs
                                 (assoc acc nm errs)
                                 acc))) {})
                         (merge-with merge (dissoc form-level-errors :global)))
             global-errors (:global form-level-errors)]
         (if (and (empty? global-errors) (empty? errors))
           end
           {:db (add-errors-to-db db form-path errors global-errors)}))
       end))))


(defn unfold-path [path value]
  (reduce
   (fn [value el]
     (cond
       (and (transform? el) (not (nil? value)))
       (transform el :from value)

       (keyword? el)
       (get value el)

       (and (vector? el) (empty? el)) value

       (and (vector? el) (vector? value) (= (count value) 1))
       (first value)

       (and (map? el) (vector? value))
       (first
        (filter
         #(set/subset? (set el) (set %)) value))

       (number? el)
       (get value el)

       (map? el)
       value

       (fn? el)
       (el :from value)))
   value path))

(defn ->form-model [value schema]
  (reduce (fn [acc [k {:keys [datasource path type] :as field}]]
            (let [ds (some #(when (contains? value %) %)
                           (keys datasource))
                  [pth v]
                  (cond
                    ds [(get datasource ds) (get value ds)]
                    (contains? value :Self) [path (:Self value)])]
              (if-let [new-v (unfold-path pth v)]
                (assoc acc k (if (= type :number)
                               (str new-v)
                               new-v))
                acc)))
          {} (:fields schema)))

(rf/reg-event-db
 :zenform/set-value
 (fn [db [_ form-path value]]
   ;; TODO add on-change event here
   (let [form (get-in db form-path)
         new-value (->form-model value form)]
     (assoc-in db form-path
               (reduce (fn [form [k v]]
                         (assoc-in form [:value k :value] v))
                form new-value)))))

(defn init-form [db form-path schema init-value]
  ;; may be make ->form-model optional
  (let [init-value (when init-value
                     (;;or
                      merge (:Defaults init-value) (->form-model init-value schema)))]
    (-> db
        (assoc-in form-path (mk-form-data schema (conj form-path :value) (or init-value {})))
        (assoc-in (conj form-path :initialized?) true))))

(defn init [db [_ form-path schema init-value]]
  (init-form db form-path schema init-value))

(rf/reg-event-db :zenform/init init)
