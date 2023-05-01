(ns user-handlers.handlers
  (:require [db :as db]
            [routing :refer [handler] :as r]
            [clojure.string :as str]
            [utils :as u]
            #_[next.jdbc.sql :as sql]
            [clj-time.core :as t]
            [handler-helpers :as hh]
            [clojure.walk :as walk]))

(def buy-subscription-link (or (System/getenv "SUBSCRIPTION_LINK") "q"))
(def global-max-tagged 300)


(defn create-repetition-group-config
  [ctx id h-id]
  (db/save!
   ctx {:resourceType           "RepetitionGroupEntry"
        :user-id                id
        :last-chosen            ""
        :total-chosen           []
        :hieroglyph_id          h-id
        :total-decisions        []
        :repetition-coefficient 0}))

(defmethod handler ::repetition-group
  [ctx {:keys [headers cookies query-params] :as _request}]
  (let [{:keys [id]} (hh/get-token-claims headers cookies)
        {:keys [hieroglyph_id]} (walk/keywordize-keys query-params)
        check-query {:h {:select [[:c.id :cid] [:c.resource :c]]
                         :from [[:RepetitionGroupEntry :c]]
                         :where [:and
                                 [:= (db/h>> :c.resource [:user-id]) id]
                                 [:= (db/h>> :c.resource [:hieroglyph_id])
                                              hieroglyph_id]]}}
        existing-rep-gr-entry-config (->> (db/query ctx check-query)
                                          (map (fn [{:keys [c cid]}]
                                                 (assoc c :id cid))))
        existing-rep-gr-entry-config* (if (seq existing-rep-gr-entry-config)
                                        (first existing-rep-gr-entry-config)
                                        (last (create-repetition-group-config
                                               ctx id hieroglyph_id)))]
    {:status 200
     :body (dissoc existing-rep-gr-entry-config*
                   :user-id
                   :id)}))

(defmethod handler ::clean-repetition-statistic
  [ctx {:keys [headers cookies] :as _request}]
  (let [{:keys [id]} (hh/get-token-claims headers cookies)
        delete-repetition-info (db/delete-record
                                (u/connection ctx)
                                :repetitiongroupentry
                                ["resource#>>'{user-id}' = ?" id])]
    {:status 200
     :body {:message delete-repetition-info}}))

(def repetition-coefficient-map
  {1    {"easy" 2 "ok"   2 "hard" 1}
   2    {"easy" 4 "ok"   4 "hard" 1}
   3    {"easy" 8 "ok"   6 "hard" 2}
   4    {"easy" 16 "ok"   8 "hard" 4}
   5    {"easy" 32 "ok"   14 "hard" 6}
   6    {"easy" 32 "ok"   20 "hard" 8}})

(def overloaded-coefficient-map
  "someone repeated more than 6 times"
  {"easy" 32
   "ok" 24
   "hard" 12})

(defn calc-repetition-coefficient
  [gr-conf _chosen decision]
  (let [_current-coeff (get gr-conf :repetition-coefficient)
        coeff (get-in repetition-coefficient-map decision
                      (get overloaded-coefficient-map (second decision)))]
    coeff))

(defn map-answer-to-coeff
  [answer]
  (get {"easy" 1
        "ok"   2
        "hard" 3}
       answer 4))

(defmethod handler ::update-repetition-group
  [ctx {:keys [headers cookies query-params body] :as _request}]
  (let [{:keys [id]}            (hh/get-token-claims headers cookies)
        {:keys [_hieroglyph_id]} (walk/keywordize-keys query-params)

        {:keys [chosen hieroglyph_id]}         body
        hieroglyph-id           (str (utils/str-to-int (str hieroglyph_id)))
        check-query             {:h {:select [[:c.id :cid] [:c.resource :c]]
                                     :from [[:RepetitionGroupEntry :c]]
                                     :where [:and
                                             [:= (db/h>> :c.resource [:user-id]) id]
                                             [:= (db/h>> :c.resource [:hieroglyph_id])
                                              (str hieroglyph-id)]]}}
        existing-rep-gr-entry-config (->> (db/query ctx check-query)
                                          (map (fn [{:keys [c cid]}]
                                                 (assoc c :id cid)))
                                          first)

        decisions                    (-> existing-rep-gr-entry-config :total-decisions)

        times-decision-made          (if (nil? (keys decisions))
                                       0
                                       (->> decisions
                                            #_keys
                                            (map (fn [el] (-> el first #_utils/str-to-int)))
                                            (apply max)))

        current-decision             [(inc times-decision-made) chosen]
        decisions*                   (conj decisions current-decision)

        chosen*                      (map-answer-to-coeff chosen)

        info-to-merge                {:last-chosen            chosen
                                      :mapped-chosen          chosen*
                                      :total-chosen           (conj (get existing-rep-gr-entry-config :total-chosen)
                                                                    chosen*)
                                      :total-decisions        decisions*
                                      :hieroglyph_id          hieroglyph_id
                                      :repetition-coefficient (calc-repetition-coefficient
                                                               existing-rep-gr-entry-config chosen* current-decision)}

        existing-rep-gr-entry-config* (when (:id existing-rep-gr-entry-config)
                                        (last (db/save! ctx (merge existing-rep-gr-entry-config
                                                                   info-to-merge))))]
    (if (:id existing-rep-gr-entry-config*)
        {:status 200
         :body (dissoc existing-rep-gr-entry-config* :user-id :id)}
        {:status 400
         :body {:message (format "No repetition configuration created for: %s" hieroglyph_id)}})))

(defn create-card-config
  [ctx id]
  (last (db/save!
          ctx {:resourceType "CardViewConfiguration"
               :pinyin          "true"
               :audio           "true"
               :translation     "true"
               :graphemes       "true"
               :examples        "true"
               :tags            "true"
               :repetitive-mode "false"
               :include-already-repeated "false"
               :user-id      id})))

(seq [])

(defmethod handler ::get-card-configuration
  [ctx {:keys [headers cookies] :as _request}]
  (let [{:keys [id]} (hh/get-token-claims headers cookies)
        check-query {:h {:select [[:c.id :cid] [:c.resource :c]]
                         :from [[:CardViewConfiguration :c]]
                         :where [:and
                                 [:= (db/h>> :c.resource [:user-id]) id]]}}
        existing-card-config (->> (db/query ctx check-query)
                                  (map (fn [{:keys [c cid]}]
                                         (assoc c :id cid))))
        existing-card-config* (if (seq existing-card-config)
                                (first existing-card-config)
                                (create-card-config ctx id))]
    {:status 200
     :body (dissoc existing-card-config*
                   :user-id :id)}))

(defmethod handler ::post-card-configuration
  [ctx {:keys [body headers cookies] :as _request}]
  (let [{:keys [id]} (hh/get-token-claims headers cookies)
        check-query {:h {:select [[:c.id :cid] [:c.resource :c]]
                         :from [[:CardViewConfiguration :c]]
                         :where [:and
                                 [:= (db/h>> :c.resource [:user-id]) id]]}}
        existing-card-config (->> (db/query ctx check-query)
                                  (map (fn [{:keys [c cid]}]
                                         (assoc c :id cid)))
                                  first)
        existing-card-config* (db/save! ctx (merge existing-card-config
                                                   body))
        existing-card-config** (if (vector? existing-card-config*)
                                 (last existing-card-config*)
                                 (:resource existing-card-config*))]
    {:status 200
     :body (dissoc existing-card-config** :user-id :id)}))


(defmethod handler ::create-groups
  [ctx {:keys [body headers cookies limited-by-subscription subscription] :as _request}]
  (let [{:keys [author hieroglyphs group-name]} body
        {:keys [limit]} limited-by-subscription
        {:keys [username id]} (hh/get-token-claims headers cookies)

        hieroglyph-names (mapv :chinese hieroglyphs)
        group-uri (str "record-ids="(str (str/join "," hieroglyph-names)))
        groups-query {:h {:select [[:c.id :cid] [:c.resource :c] ]
                          :from [[:recordgroup :c]]
                          :where [:= (db/h>> :c.resource [:author])
                                  author]}}
        existing-groups (->> (db/query ctx groups-query)
                             (map (fn [{:keys [c cid]}]
                                    (assoc c :id cid))))
        count-group-exceeds (if (and (not (seq subscription))
                                      limit)
                              (<= limit (count existing-groups))
                              false)

        encode2 (fn [el]
                  (.encodeToString (java.util.Base64/getEncoder)
                                   (.getBytes (.encodeToString (java.util.Base64/getEncoder)
                                                               (.getBytes el)))))

        group {:hieroglyphs-code (encode2 (str/join "," hieroglyph-names))
               :group-uri group-uri
               :hieroglyphs hieroglyphs
               :stats {:total (count hieroglyph-names)}
               :author username
               :user-id id
               :created (u/sql-now)
               :group-name group-name}
        saved-resource (cond (and (seq author)
                                  (seq group-name)
                                  (seq hieroglyph-names)
                                  (not count-group-exceeds))
                             (-> (db/save-resource! ctx (-> group
                                                            (merge {:resourceType "RecordGroup"})))
                                 last
                                 (assoc :message (str "Group " group-name " created")))

                             (and (seq author)
                                  (seq group-name)
                                  (seq hieroglyph-names)
                                  count-group-exceeds)
                             {:message (format "Please buy subscription to add more than %s groups" limit)
                                                        :buy-subscription-link buy-subscription-link}
                             :else {:message (str "Can't save group: " group-name ":" author)})]
    {:status (if (-> saved-resource :buy-subscription-link) 403 201)
     :body saved-resource}))

(defmethod handler ::like-hieroglyph
  [ctx {:keys [_query-params body headers cookies] :as _request}]
  (let [{:keys [current-h-id text hieroglyph]} body
        {:keys [username id]} (hh/get-token-claims headers cookies)

        q                     {:h {:select [[:c.id :cid] [:c.resource :c]]
                                   :from [[:userlike :c]]
                                   :where [:and
                                           [:= (db/h>> :c.resource [:user-id]) id]]}}

        user-likes            (->> (db/query ctx q)
                                   (map (fn [{:keys [c cid]}]
                                          (merge c {:id cid}))))

        current-h-named       (->> user-likes
                                   (filter (fn [el] (= (:referring-hieroglyph-id el)
                                                       (str current-h-id)))))

        like-already-exists   (seq current-h-named)

        like                  (when (not like-already-exists)
                                (last (db/save!
                                        ctx {:resourceType            "UserLike"
                                             :user-id                 id
                                             :referring-hieroglyph-id (str current-h-id)
                                             :hieroglyph              hieroglyph
                                             :author                  username})))]

    (cond like-already-exists
          {:status 409
           :body {:message (str "WTF??" text)}}
          :else
          {:status 200
           :body {:current-h-id current-h-id
                  :record like}})))

(defmethod handler ::user-likes
  [ctx {:keys [cookies query-params headers] :as _request}]
  (let [{:keys [referring-hieroglyph-id]} (walk/keywordize-keys query-params)

        {:keys [id username]} (hh/get-token-claims headers cookies)
        query                 {:h {:select [[:c.id :cid] [:c.resource :c] ]
                                   :from [[:userlike :c]]
                                   :where [:and
                                           [:= (db/h>> :c.resource [:user-id]) id]
                                           [:= (db/h>> :c.resource
                                                       [:referring-hieroglyph-id])
                                            (str referring-hieroglyph-id)]]}}
        result                 (when (and referring-hieroglyph-id username)
                                 (->> (db/query ctx query)
                                      (map (fn [{:keys [c cid]}]
                                             (merge c {:id cid})))
                                      first))]
    {:status 200
     :body {:records result}}))

(defmethod handler ::get-user-likes
  [ctx {:keys [cookies _query-params headers] :as _request}]
  (let [{:keys [id _username]} (hh/get-token-claims headers cookies)
        query                  {:h {:select [[:c.id :cid] [:c.resource :c] ]
                                    :from [[:userlike :c]]
                                    :where [:and
                                            [:= (db/h>> :c.resource [:user-id]) id]]}}
        result                 (->> (db/query ctx query)
                                    (map (fn [{:keys [c cid]}]
                                           (merge c {:id cid}))))]
    {:status 200
     :body {:records result}}))

(defmethod handler ::delete-like
  [ctx {:keys [body] :as _request}]
  (let [{:keys [like-id _author]} body
        res (if (seq like-id)
              (try (db/delete-record (u/connection ctx)
                                     :userlike ["id = ?" like-id])
                   (catch org.postgresql.util.PSQLException e
                     {:res (str "Could not delete: " like-id)})))]
    {:status 201
     :body {:like-id like-id
            :message (str "Deleted like: " like-id "<-")}}))

(defmethod handler ::get-groups
  [ctx {:keys [query-params] :as _request}]
  (let [{:keys [author]} query-params
        groups-query {:h {:select [[:c.id :cid] [:c.resource :c]]
                          :from [[:recordgroup :c]]
                          :where [:= (db/h>> :c.resource [:author])
                                  author]}}
        groups (->> (db/query ctx groups-query)
                    (map (fn [{:keys [c cid]}]
                           (assoc c :id cid))))]
    {:status 200
     :body (or groups [{:message "No groups found"}])}))


(defmethod handler ::delete-group
  [ctx {:keys [body] :as _request}]
  (let [{:keys [group-id _author]} body
        res (try (db/delete-record (u/connection ctx)
                                   :recordgroup ["id = ?" group-id])
                 (catch org.postgresql.util.PSQLException e
                   {:res (str "deleted: " group-id)}))]
    {:status 201
     :body {:result res
            :message (str "Deleted group: " group-id)}}))


(defmethod handler ::user-tags
  [ctx {:keys [cookies query-params headers] :as _request}]
  (let [{:keys [referring-hieroglyph-id]} (walk/keywordize-keys query-params)

        {:keys [id username]} (hh/get-token-claims headers cookies)
        tags                  {:h {:select [[:c.id :cid] [:c.resource :c] ]
                                   :from [[:usertag :c]]
                                   :where [:and
                                           [:= (db/h>> :c.resource [:user-id]) id]
                                           [:= (db/h>> :c.resource
                                                       [:referring-hieroglyph-id])
                                            (str referring-hieroglyph-id)]]}}
        result                 (when (and referring-hieroglyph-id username)
                                 (->> (db/query ctx tags)
                                      (map (fn [{:keys [c cid]}]
                                             (merge {:id cid} c)))))]
    {:status 200
     :body {:records result}}))

(defmethod handler ::save-tag
  [ctx {:keys [cookies headers body] :as _request}]
  (let [{:keys [current-h-id text]} body
        {:keys [username id]} (hh/get-token-claims headers cookies)

        q                     {:h {:select [[:c.id :cid] [:c.resource :c]]
                                   :from [[:usertag :c]]
                                   :where [:and
                                           [:= (db/h>> :c.resource [:user-id]) id]
                                           [:= (db/h>> :c.resource [:tag])
                                            (-> text str (str/trim ))]]}}

        user-named-tags       (->> (db/query ctx q)
                                   (map (fn [{:keys [c cid]}]
                                          (merge c {:id cid}))))

        current-h-named       (->> user-named-tags
                                   (filter (fn [el] (= (:referring-hieroglyph-id el)
                                                       (str current-h-id)))))

        max-tagged-h-reached  (>= (count user-named-tags) global-max-tagged)

        tag-already-exists    (seq current-h-named)

        tag                   (when (not tag-already-exists)
                                (last (db/save!
                                       ctx {:resourceType            "UserTag"
                                            :tag                     text
                                            :user-id                 id
                                            :referring-hieroglyph-id (str current-h-id)
                                            :author                  username})))]
    (cond tag-already-exists
          {:status 409
           :body {:message (str "Tag already registered: " text)}}
          max-tagged-h-reached
          {:status 409
           :body {:message (format "Can't tag more that %s hieroglyphs with one tag"
                                   global-max-tagged)}}
          :else
          {:status 200
           :body {:current-h-id current-h-id
                  :record tag}})))

(defmethod handler ::delete-tag
  [ctx {:keys [body] :as _request}]
  (let [{:keys [tag-to-id]} body
        res (try (db/delete-record (u/connection ctx)
                                   :usertag ["id = ?" tag-to-id])
                 (catch org.postgresql.util.PSQLException e
                   {:res (str "deleted: " tag-to-id)}))]
    {:status 201
     :body {:result res
            :message (str "Deleted user tag: " tag-to-id)}}))

(defmethod handler ::delete-whole-tag
  [ctx {:keys [body] :as _request}]
  (let [{:keys [tag-name]} body
        res (try (db/delete-record (u/connection ctx)
                                   :usertag ["resource#>>'{tag}' = ?" tag-name])
                 (catch org.postgresql.util.PSQLException e
                   {:res (str "Can't delete: " tag-name)}))]

    {:status 201
     :body {:result res
            :message (str "Deleted whole tag: " tag-name)}}))


(defmethod handler ::buy-subscription
  [ctx {:keys [_body] :as _request}]
  (let []
    {:status 403
     :body {:result "Empty result"
            :message "Please buy subscription to enable this functionality"
            :buy-subscription-link buy-subscription-link}}))

(defmethod handler ::get-user-tags
  [ctx {:keys [headers cookies] :as _request}]
  (let [{:keys [_username id]} (hh/get-token-claims headers cookies)
        t-query  {:h {:select [[:c.id :cid] [:c.resource :c] ]
                      :from [[:usertag :c]]
                      :where [:and [:= (db/h>> :c.resource [:user-id]) id]]}}
        tags   (->> (db/query ctx t-query)
                    (map (fn [{:keys [c cid]}] (merge c {:cid cid}))))
        hier-ids-in-tags (->> tags (map (fn [el] (-> el :referring-hieroglyph-id))) distinct)
        h-query (when (and (seq tags) (seq hier-ids-in-tags))
                  {:h {:select [[:c.id :cid] [:c.resource :c]]
                       :from [[:chineserecord :c]]
                       :where [:in (db/h>> :c.resource [:resource :hieroglyph_id])
                               hier-ids-in-tags]}})
        hieroglyphs (when (and (seq tags) (seq hier-ids-in-tags))
                      (map :c (db/query ctx h-query)))
        tags*       (->> tags (map (fn [el]
                                     (select-keys el [:referring-hieroglyph-id :tag :cid])))
                         distinct)
        tag-groups  (->> (for [t tags*
                               :let [acc  {}
                                     hieroglyphs (filter (fn [el] (= (:referring-hieroglyph-id t)
                                                                     (-> el :resource :hieroglyph_id))) hieroglyphs)]]
                           (assoc acc
                                  (dissoc t :referring-hieroglyph-id) hieroglyphs))
                         (group-by (fn [el] (-> el keys first :tag)))
                         (map second))
        format-tag-group (fn [coll] (let [coll-key (-> coll first keys first)]
                                      (assoc {}
                                             :id   (:cid coll-key)
                                             :tag  (:tag coll-key)
                                             :coll (mapcat (fn [el] (-> el first second)) coll))))
        tag-content (->> tag-groups
                         (mapv format-tag-group))]
    {:status 200
     :body {:tags tag-content}}))
