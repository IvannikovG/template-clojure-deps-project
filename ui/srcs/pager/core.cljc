(ns pager.core
  (:require
    [clojure.set :as set]
    [re-frame.core :as rf]))

(defonce registry (atom {}))
(defonce mwrs (atom {}))

(rf/reg-sub
  :global/in-progress
  (fn [db _]
    (let [active-pages
          (set/intersection (set (vals @registry))
                            (set (keys db)))]
      (or (some (comp seq :not-ready :state val)
                (select-keys db active-pages))
          (:global-loading db))))) ;; support old pages without pager

(defn reg-page* [nm page]
  (swap! registry assoc nm page))

(defn reg-mw* [key handler]
  (swap! mwrs assoc key handler))

(defn db-fx [page db patch]
  (update db page merge patch))

(defn apply-mw [handler]
  (fn [cofx sec-arg]
    (if-let [interceptors (->> (get-in cofx [:db :route-map/current-route :interceptors])
                               (keep #(% @mwrs))
                               seq)]
      (reduce (fn [fx i] (i cofx fx))
              (handler cofx sec-arg)
              interceptors)
      (handler cofx sec-arg))))

(defn handler-with-meta [handler m]
  (fn [fx sec-arg]
    (try
      (handler fx sec-arg)
      #?(:clj  (catch Exception e (throw (Exception. (str "exception in " m \space (.getMessage e)))))
         :cljs (catch js/Object e (.error js/console (str "exception in " m \space e)))))))

(defn interceptor-from-meta [{:keys [page m init-event?]}]
  (letfn [(handle [not-ready]
            (-> not-ready
                (set/difference (set (:ready m)))
                (set/union (set (:not-ready m)))))]
    (rf/->interceptor
      :id    :page-state
      :after (fn [{{db :db e :event} :coeffects :as ctx}]
               (cond
                 (and init-event? (= :deinit (second e)))
                 ctx

                 (get-in ctx [:effects :db])
                 (update-in ctx [:effects :db page :state :not-ready] handle)

                 :else
                 (update ctx :effects merge
                         {:db (update-in db [page :state :not-ready] handle)}))))))

;; iterceptor that marks :json/fetch fx emited from page init event,
;; to abort them when next page inited.
(def mark-init-fetch-fx
  (rf/->interceptor
   :id :add-reg-init-meta
   :after (fn [{{e :event} :coeffects effects :effects :as ctx}]
            (let [fetch-effect (:json/fetch effects)
                  deinit-event? (= :deinit (second e))
                  mark #(assoc % :page-init-request? true)]
              (if (and fetch-effect (not deinit-event?))
                (assoc-in ctx [:effects :json/fetch] (if (vector? fetch-effect)
                                                       (mapv mark fetch-effect)
                                                       (mark fetch-effect)))
                ctx)))))

(defn reg-init* [nm f]
  (let [page (get @registry nm)
        handler
        (fn [{db :db :as fx} [_ phase params]]
          (let [contexts (keys (get db :route/context))]
            (if (= phase :deinit)
              {:db (dissoc db page)
               :dispatch-n (map #(vector % :deinit (:fragment-params db)) contexts)}
              (let [ef (f fx phase params)]
                (cond-> ef
                  (:db ef) (update :db #(db-fx page db %))
                  contexts (update :dispatch-n into (map #(vector % :init (:fragment-params db)) contexts)))))))]
    (rf/reg-event-fx page
                     (cond-> [mark-init-fetch-fx]
                       (meta f)
                       (conj (interceptor-from-meta {:page page
                                                     :init-event? true
                                                     :m (meta f)})))
                     (-> handler
                         (handler-with-meta page)
                         apply-mw))))

(defn reg-event-db*
  ([nm event f]
   (reg-event-db* nm event [] f))
  ([nm event ints f]
   (let [page (get @registry nm)
         handler
         (fn [db [_ & args]]
           (let [new-db (apply f (into [(get db page)] args))]
             (update db page merge new-db)))]
     (rf/reg-event-db
       event
       (cond-> ints
         (meta f)
         (conj (interceptor-from-meta {:page page :m (meta f)})))
       (-> handler
           (handler-with-meta page)
           apply-mw)))))

(def unwrap-fhir-bundle
  (rf/->interceptor
   :id ::unwrap-fhir-bundle
   :before
   (fn [context]
     (let [unwrap (fn [[_ response]]
                    (update response :data #(map :resource (:entry %))))]
       (update-in context [:coeffects :event] unwrap)))))


(defn reg-sub* [ns func]
  (let [page (get @registry ns)
        with-db? (:with-db (meta func))
        handler
        (fn [db arg]
          (let [db-page (get db page)]
            (if with-db?
              (merge db-page (func db-page db arg))
              (merge db-page (func db-page arg)))))]
    (rf/reg-sub page (handler-with-meta handler page))))

(defn reg-event-fx*
  ([nm key f] (reg-event-fx* nm key [] f))
  ([nm key ints f]
   (let [page (get @registry nm)
         handler
         (fn [{db :db :as fx} [_ & args]]
            (let [ef (apply f (into [fx] args))]
              (cond-> ef
                (:db ef) (update :db #(db-fx page db %)))))]
     (rf/reg-event-fx key
                      (cond-> ints
                        (meta f)
                        (conj (interceptor-from-meta {:page page :m (meta f)})))
                      (-> handler
                          (handler-with-meta page)
                          apply-mw)))))
