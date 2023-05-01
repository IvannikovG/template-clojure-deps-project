(ns zframes.xf
  "Data conversion for the masses")

;; Main transformer functions

(declare evaluate-path)

(defn >>
  "Convert data to form value.
  `xf-def` is a map, whose keys will be keys for a result map,
  vector values are pathes to get value from `data`.
  Note that result map can be not flat, so `xf-def` map values
  can be maps too, which means that converting process will be
  called for them recursively."
  [xf-def data]
  (reduce
   (fn [acc [field mapper]]
     (let [field-value
           (cond
             (vector? mapper)
             (cond-> (get-in data (evaluate-path data mapper))
               (= :fn (::type (last mapper))) ((::>> (last mapper))))

             (map? mapper)
             (>> (get xf-def field) data))]
       (assoc acc field field-value)))
   {} xf-def))

(declare deep-merge)
(declare evaluate-back)

(defn <<
  "Convert form value to data. Mirrored `>>` in most cases."
  [xf-def value]
  (reduce
   (fn [acc [field mapper]]
     (cond
       (vector? mapper)
       (evaluate-back acc mapper (get value field))
       #_(assoc-in acc mapper (get value field))

       (map? mapper)
       (deep-merge acc (<< (get xf-def field) (get value field)))))
   {} xf-def))


;; Transformer operators for more complex situations


(declare index-search)

(defn search
  "An xf operator to get one object from a vector on the path.
  `condition` is a map which is used to filter by key-value equality."
  [condition]
  {::>> (fn [data] (index-search condition data))
   ::<< condition})

(defn f
  "An xf operator to apply custom function to extracted value or
   before putting value on reverse xf"
  [from-fn to-fn]
  {::type :fn
   ::>> from-fn
   ::<< to-fn})

(defn coll-xf
  "An xf shortcut to apply this transforming engine on collection
  with provided xf-def"
  [xf-def]
  {::type :fn
   ::>> (fn [vals] (mapv #(>> xf-def %) vals))
   ::<< (fn [vals] (mapv #(<< xf-def %) vals))})


;; Private helper functions


(defn- check-with-map
  "Check if `value` fields conform with `mp` fields."
  [mp value]
  (reduce
   (fn [_ [k v]]
     (if (= (get value k) v)
       true
       (reduced false)))
   true mp))

(defn- index-search
  "Get the index of the map, which conforms with `mp` fields."
  [mp data]
  {:pre [(vector? data)]
   :post [(or (nil? %) (>= % 0))]}
  (->> data
       (map-indexed vector)
       (filter #(check-with-map mp (second %)))
       ffirst))

(defn deep-merge
  "Because `merge` is not cool enough.
  Also concats vectors."
  [v & vs]
  (letfn [(rec-merge
            [v1 v2]
            (cond
              (and (map? v1) (map? v2))
              (merge-with deep-merge v1 v2)

              (and (vector? v1) (vector? v2))
              (vec (concat v1 v2))

              :default v2))]
    (when (some identity vs)
      (reduce #(rec-merge %1 %2) v vs))))

(defn- evaluate-path
  "Prepare `path` vector to be used for `get-in`, evaluating
  inner fns if necessary."
  [data path]
  {:pre [(vector? path)]
   :post [(vector? %)]}
  (reduce
   (fn [acc p]
     (cond
       (keyword? p) (conj acc p)
       (map? p)
       (case (::type p)
         :fn
         acc

         (let [f (::>> p)]
           (cond
             (fn? f) (conj acc (f (get-in data acc))))))))
   [] path))

(defn- evaluate-back
  "Puts associated data used for search back in structure."
  [out-data path value]
  (loop [v out-data prev [] [p & ps] path]
    (if (some? p)
      (cond
        (keyword? p) (recur v (conj prev p) ps)

        (map? p)
        (case (::type p)
          :fn
          (assoc-in v prev ((::<< p) value))
          
          (let [ass-data (::<< p)]
            ;; Check that the vector on our path has a map
            ;; conforming with xf rule, so we won't create a new one
            (if-let [i (some->>
                        (map-indexed vector (get-in v prev))
                        (filter #(check-with-map ass-data (second %)))
                        ffirst)]
              (recur v (conj prev i) ps)
              (recur (update-in v prev (fnil conj []) ass-data)
                     (conj prev (count (get-in v prev)))
                     ps)))))
      (assoc-in v prev value))))
