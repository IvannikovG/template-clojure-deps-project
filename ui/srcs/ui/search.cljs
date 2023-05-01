(ns ui.search
  (:require
   [clojure.string :as str]
   [re-frame.core :as rf]
   [goog.crypt.base64 :as b64]))


(def search-types
  {:graphemes
   {:uri "$chinese-grapheme"}})

(rf/reg-sub
 :search/result
 (fn [db [_ path & [tp]]]
   (if-let [post-xf (get-in search-types [tp :post-xf])]
     (post-xf (get-in db (conj path :value)))
     (get-in db (conj path :value)))))

(rf/reg-sub
 :search/loading
 (fn [db [_ path]]
   (get-in db (conj path :loading))))

(rf/reg-sub
 :search/plain-result
 (fn [db [_ path]]
   (let [result (get-in db (conj path :value))]
     result)))


(rf/reg-event-fx
 :search/search-graphemes
 (fn [{db :db} [_ tp path text]]
   (let [opts (get search-types tp)
         uri-entry (str (get-in db [:config :base-url]) "/" (:uri opts))
         uri    uri-entry

         success-error {:event ::search-result-graphemes
                        :path path}
         has-cache       (get-in db [:cards/index :search-cache :graphemes :value])
         already-fetched (= 213 (count (get-in db [:cards/index :search-cache
                                                   :graphemes :value])))]
     (cond
       (seq text) {:db (assoc-in db (conj path :loading) true)
                   :json/fetch
                   {:uri uri
                    :token (get-in db [:auth :id_token])
                    :headers {}
                    :params  {:search-text text}
                    :success success-error
                    :error   success-error}}
       (and already-fetched has-cache) nil
       :else {:db (assoc-in db (conj path :loading) true)
                   :json/fetch
                   {:uri uri
                    :token (get-in db [:auth :id_token])
                    :headers {}
                    :success success-error
                    :error   success-error}}))))


(defn add-display [t]
  (fn [d]
    (if d
      ((get-in search-types [t :dbify]) d)
      d)))

(rf/reg-event-db
 ::search-result-graphemes
 (fn [db [_ {:keys [type path data params] :as all}]]
   (prn "FF: Data : "  (:records data))
   (let [result (:records data)
         value (map (fn [el]
                      (prn "EL::: " el)
                      (let [record   (:record   el)
                            resource (:resource record)]
                        (assoc resource :display (str (-> resource :chinese :general) " - "
                                                      (:pinyin resource) " - "
                                                      (or (first (:naming resource))
                                                          (-> resource :translation :ru first)))
                               :short-display (-> resource :chinese :general))))
                    result)]
     (update-in db path (fn [_] {:value value})))))
