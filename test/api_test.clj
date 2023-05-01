(ns api-test
  (:require  [clojure.test :refer [is deftest] :as t]
             [db :as db]
             [core :as sut]
             [auth.register :as register]
             [matcho.core :as matcho]
             [test-utils :as u]
             [keys.core :as radicals]
             [routing :as r]
             [parser.json-parser :as p]))



(deftest rpc-test

  (do
    (def tc u/test-ctx)
    (def ta (u/test-app)))

  (matcho/match (ta {:request-method "post" :uri "/rpc"
                     :body {:method     "rpc/test-rpc"
                            :jsonrpc    "2.0"
                            :params     {:a 1 :b 2}
                            :id         1}})
                {:body
                 {:meta {:requesting-method "rpc/test-rpc", :requesting-params {:a 1, :b 2}},
                  :jsonrpc "2.0",
                  :id 1,
                  :result
                  {:rpc-test-handler-worked "true",
                   :body
                   {:method "rpc/test-rpc", :jsonrpc "2.0", :params {:a 1, :b 2}, :id 1}}},
                 :status 200})

  (matcho/match (ta {:request-method "post" :uri "/rpc"
                     :body {:method "rpc/test-rpc2"
                            :jsonrpc "2.0"
                            :params {:a 1 :b 2}
                            :id      1}})
                {:body
                 {:error
                  {:code "1",
                   :data
                   [{:type "unknown-key", :message "unknown key :b", :path [:params :b]}],
                   :message "Contained rpc validation errors"},
                  :jsonrpc "2.0",
                  :id 1,
                  :meta {:requesting-method "rpc/test-rpc2", :requesting-params {:a 1, :b 2}}},
                 :status 400})

  (matcho/match (ta {:request-method "post" :uri "/rpc"
                     :body {:method "rpc/non-rpc"
                            :jsonrpc "2.0"
                            :params  {:a 1 :b 2}
                            :id      1}})
                {:status 400, :body {:error {:message "No method found"
                                             :data {:requested-method "rpc/non-rpc"}}
                                     :jsonrpc "2.0"
                                     :id      1}})

  )

(deftest chinese-radicals

  (do
    (def tc u/test-ctx)
    (def ta (u/test-app))
    (u/test-exec! "truncate chineserecord;")
    (u/test-exec! "truncate chinesegrapheme;")
    (p/parse-chinese-json tc "medium_resources.json")
    (p/parse-graphemes-json tc "graphemes.json"))

  (db/query tc "SELECT * FROM chineserecord WHERE ((resource#>'{graphemes}' ?? '工' ))")

  (db/query tc ["SELECT * FROM chinesegrapheme WHERE (resource#>>'{id}') = ?" "47"])

  (defn records [] (->> (ta {:request-method "get"
                             :query-params
                             {"id-start" "48",
                              "id-end"   "125",
                              "include-hieroglyphs" "include"}
                             :uri "/$chinese-grapheme"})
                        :body))

  (matcho/match
   (first (:records (records)))
   {:record {:resourceType "ChineseGrapheme",
             :resource {:chinese {:general "工"}, :pinyin "gōng",
                        :type "radical", :id "48", :translation {:ru ["работа"]},
                        :raw_pinyin "gong"}, :count 78},
    :hieroglyphs [{:graphemes ["纟" "工"], :created "2022-04-02T21:44:45+00:00",
                   :resourceType "ChineseRecord",
                   :examples [{:chinese "红包", :pinyin "hóngbāo",
                               :translation {:esp [], :srb [], :en [], :de [],
                                             :ru ["красный конверт 🧧"]}}],
                   :resource {:chinese "红", :pinyin "hóng",
                              :hieroglyph_id "292",
                              :translation {:esp [], :srb [], :en [],
                                            :de [], :ru ["красный"]},
                              :raw_pinyin "hong"}}],
    :next-g {:chinese {:general "己", :additional ["已" "巳"]},
             :pinyin "jǐ", :type "radical", :id "49",
             :translation {:ru ["сам"]}, :raw_pinyin "ji"},
    :prev-g {:chinese {:general "巛", :additional ["川" "巜"]},
             :pinyin "chuān", :type "radical", :id "47",
             :translation {:esp [], :srb [], :en [],
                           :de [], :ru ["поток" "река"]},
             :raw_pinyin "chuan"}})

  )

(deftest get-tags-likes-test

  (do
    (def tc u/test-ctx)
    (def ta (u/test-app))
    (u/test-exec! "truncate chineserecord;")
    (u/test-exec! "truncate chinesegrapheme;")
    (p/parse-chinese-json tc "medium_resources.json")
    (p/parse-graphemes-json tc "graphemes.json"))

  (do (u/test-exec! "truncate app_user;")
      (u/test-exec! "truncate userlike;")

      (u/test-exec! "truncate usersubscription;")

      (def sample-user {:name {:first-name "Alex" :last-name "Vith"}
                        :id "ta-user-id" :resourceType "app-user"
                        :password "a" :username "a"})

      (ta {:request-method "post" :uri "/register"
           :body {:username (.encode (java.util.Base64/getEncoder) (.getBytes "a"))
                  :email    (.encode (java.util.Base64/getEncoder) (.getBytes "anton@mail.ri"))
                  :password (.encode (java.util.Base64/getEncoder) (.getBytes "a"))}})

      (def u-id (-> (u/test-query "select * from app_user;") first :id))
      (def cookies (:cookies (ta {:request-method "post"
                                  :uri "/login"
                                  :body {:username (.encode (java.util.Base64/getEncoder)
                                                            (.getBytes "a"))
                                         :password (.encode (java.util.Base64/getEncoder)
                                                            (.getBytes "a"))}}))))

  (do
    (ta {:request-method "post" :uri "$like-hieroglyph"
         :cookies cookies
         :body {:current-h-id "150"}})


    (matcho/match
     (ta {:request-method "post" :uri "$like-hieroglyph"
          :cookies cookies
          :body {:current-h-id "150"}})
     {:status 409, :body {:message "WTF??"}})

    (matcho/match
     (ta {:request-method "post" :uri "$delete-like"
          :cookies cookies
          :body {:like-id "150"}})
     {:status 201, :body {:like-id "150",
                          :message "Deleted like: 150<-"}})

    (matcho/match
     (ta {:request-method "get" :uri "$user-likes"
          :cookies cookies
          :body {:like-id "150"}})
     {:status 200, :body {:records nil}})

    )

  (do
    (def response
      (->> (ta {:request-method "post"
                :cookies cookies
                :uri "/$subscribe"})))

    (ta {:request-method "post" :uri "$save-tag"
         :cookies cookies
         :body {:text "next"
                :current-h-id "150"}})

    (ta {:request-method "post" :uri "$save-tag"
         :cookies cookies
         :body {:text "next1"
                :current-h-id "150"}})

    (ta {:request-method "post" :uri "$save-tag"
         :cookies cookies
         :body {:text "next"
                :current-h-id "151"}})

    (def tags-page-content
      (ta {:request-method "get" :uri "$get-user-tags"
           :cookies cookies
           :body {}})))

  (matcho/match
   tags-page-content
   {:status 200,
    :body
    {:tags
     [{:id string?
       :tag "next",
       :coll
       [{:created string?
          :examples [],
          :resource
          {:pinyin "hái",
           :chinese "还",
           :raw_pinyin "huan",
           :translation {:de [], :en [], :ru ["также"], :esp [], :srb []},
           :hieroglyph_id "150"},
          :resourceType "ChineseRecord"}
        {:created "2022-01-31T23:55:11+00:00",
         :examples [],
         :resource
         {:pinyin "duō",
          :chinese "多",
          :raw_pinyin "duo",
          :translation {:de [], :en [], :ru ["много"], :esp [], :srb []},
          :hieroglyph_id "151"},
         :resourceType "ChineseRecord"}]}
      {:id string?
       :tag "next1",
       :coll
       [{:created string?
          :examples [],
          :resource
          {:pinyin "hái",
           :chinese "还",
           :raw_pinyin "huan",
           :translation {:de [], :en [], :ru ["также"], :esp [], :srb []},
           :hieroglyph_id "150"},
          :resourceType "ChineseRecord"}]}]}})

  )

(deftest chinese-records
  (do
    (def tc u/test-ctx)
    (def ta (u/test-app))
    (u/test-exec! "truncate chineserecord;")
    (u/test-exec! "truncate chinesegrapheme;")
    (p/parse-chinese-json tc "medium_resources.json")
    (p/parse-graphemes-json tc "graphemes.json"))

  (do (u/test-exec! "truncate app_user;")

      (u/test-exec! "truncate usersubscription;")

      (u/test-exec! "truncate usertag;")

      (u/test-exec! "truncate userlike;")

      (def sample-user {:name {:first-name "Alex" :last-name "Vith"}
                        :id "ta-user-id" :resourceType "app-user"
                        :password "a" :username "a"})

      (matcho/match (ta {:request-method "post" :uri "/register"
                         :body {:username (.encode (java.util.Base64/getEncoder) (.getBytes "a"))
                                :email    (.encode (java.util.Base64/getEncoder) (.getBytes "anton@mail.ri"))
                                :password (.encode (java.util.Base64/getEncoder) (.getBytes "a"))}})
                    {:status 200, :body [:resource
                                         {:email "anton@mail.ri",
                                          :password "0cc175b9c0f1b6a831c399e269772661",
                                          :username "a",
                                          :resourceType "app-user"}]})

      (def u-id (-> (u/test-query "select * from app_user;") first :id))
      (def cookies (:cookies (ta {:request-method "post"
                               :uri "/login"
                               :body {:username (.encode (java.util.Base64/getEncoder)
                                                         (.getBytes "a"))
                                      :password (.encode (java.util.Base64/getEncoder)
                                                         (.getBytes "a"))}})))

      (defn tag-save
        []
        (ta {:request-method "post" :uri "$save-tag"
             :cookies cookies
             :body {:text "next"
                    :current-h-id "258"}})))

  (matcho/match cookies {:jwt-token string?})

  (def response
    (->> (ta {:request-method "post"
              :cookies cookies
              :uri "/$subscribe"})))

  (matcho/match
   (tag-save)
   {:status 200,
    :body
    {:current-h-id "258",
     :record
     {:tag "next",
      :author "a",
      :resourceType "UserTag",
      :referring-hieroglyph-id "258"}}})

  (def records (->> (ta {:request-method "get"
                          :query-params
                          {"id-start" "100",
                           "id-end" "300",
                           "from" "2022-03-20",
                           "to" "",
                           "tags" ["next"],
                           #_#_"graphemes" "1,2,3"}
                          :uri "/$chinese-record?id-start=100&id-end=300&from=2022-03-20&to=&id-end=300"})
                    :body
                    :records))

  #_(db/query tc "select * from chineserecord")

  (def record-ids (set (map (fn [el] (-> el :resource :hieroglyph_id)) records)))

  (def record-with-tags
    (filter (fn [el] (= "258" (-> el :resource :hieroglyph_id))) records))

  (t/is (= (set ["258" "259" "260" "261" "262" "263" "264"
                 "265" "266" "267" "268" "269" "270" "271"
                 "272" "273" "274" "275" "276" "277" "278"
                 "279" "280" "281" "282" "283" "284" "285"
                 "286" "287" "288" "289" "290" "291" "292"
                 "293" "294" "295" "296" "297" "298" "299"
                 "300" ])
           record-ids))

  (matcho/match (first records)
                {:created "2022-03-20T07:51:37+00:00",
                 :examples [],
                 :id string?
                 :resource {:pinyin "bàn",
                            :chinese "半",
                            :raw_pinyin "ban"
                            :translation {:de [], :en [], :ru ["половина"], :esp [], :srb []},
                            :hieroglyph_id "258"},
                 :resourceType "ChineseRecord"})

  (matcho/match (second records)
                {:created "2022-03-20T08:02:54+00:00",
                 :id string?
                 :examples [],
                 :resource {:pinyin "chē", :chinese "车",
                            :raw_pinyin "che"
                            :translation {:de [], :en [], :ru ["машина"],
                                          :esp [], :srb []}, :hieroglyph_id "259"},
                 :resourceType "ChineseRecord"})

  (def record-good (-> (ta {:request-method "get"
                            :query-params
                            {"record-ids" "419"}
                            :uri "/$chinese-record?record-ids=419"})
                       :body
                       :records))

  (matcho/match
   record-good
   [{:created "2022-05-04T20:57:29+00:00",
     :examples [{:pinyin "xiǎngfǎ", :chinese "想法",
                 :translation {:de [], :en [],
                               :ru ["мысль" "идея"],
                               :esp [], :srb []}}
                {:pinyin "sìxiǎng", :chinese "思想",
                 :translation {:de [], :en ["ideology"],
                               :ru ["образ мысли" "идеология"],
                               :esp [], :srb []}}
                {:pinyin "xiǎnghǎo", :chinese "想好",
                 :translation {:de [], :en ["decide"],
                               :ru ["решить"], :esp [],
                               :srb []}}],
     :resource {:pinyin "xiăng", :chinese "想",
                :translation {:de [], :en [],
                              :ru ["думать" "хотеть" "скучать"],
                              :esp [], :srb []},
                :hieroglyph_id "419"}, :resourceType "ChineseRecord"}])

  (matcho/match
   (-> (ta {:request-method "get"
             :query-params
             {"search-text" "важно"}
             :uri "/$chinese-record"})
       :body
       :records)
   [{:created string?
     :examples
     [{:pinyin "zhòngyào",
       :chinese "重要",
       :translation {:de [], :en [], :ru ["важно"], :esp [], :srb []}}
      {:pinyin "zhòngshì",
       :chinese "重视",
       :translation
       {:de [],
        :en ["pay attention to"],
        :ru ["обращать внимание на"],
        :esp [],
        :srb []}}],
     :resource
     {:pinyin "zhòng",
      :chinese "重",
      :translation {:de [], :en ["weight"], :ru ["весить"], :esp [], :srb []},
      :hieroglyph_id "648"},
     :resourceType "ChineseRecord"}
    {:created "2022-07-25T18:20:54+00:00",
     :examples
     [{:pinyin "xióngmāo duì xià yǔ wú suǒ wèi",
       :chinese "熊猫 对 下雨 无 所谓",
       :translation
       {:de [], :en [], :ru ["пандам дождь не помеха"], :esp [], :srb []}}],
     :resource
     {:pinyin "wèi",
      :chinese "谓",
      :translation
      {:de [],
       :en [],
       :ru ["частица для построения оборота \"не важно\" или \"no matter\""],
       :esp [],
       :srb []},
      :hieroglyph_id "662"},
     :resourceType "ChineseRecord"}
    {:created "2022-08-04T08:16:27+00:00",
     :examples
     [{:pinyin "jǐnzhāng",
       :chinese "紧张",
       :translation
       {:de [], :en ["nervous"], :ru ["нервный" "напряжённый"], :esp [], :srb []}}
      {:pinyin "zài zhèngzhì lǐ wú guān jǐn yào de rén",
       :chinese "在 政治 里 无关紧要的 人",
       :translation
       {:de [], :en [], :ru ["не важный человек в политике"], :esp [], :srb []}}],
     :resource
     {:pinyin "jǐn",
      :chinese "紧",
      :translation {:de [], :en [], :ru ["важно"], :esp [], :srb []},
      :hieroglyph_id "682"},
     :resourceType "ChineseRecord"}]))


(deftest registration-and-login-jwt
  (do
    (def tc u/test-ctx)
    (def ta (u/test-app))
    (u/test-exec! "truncate chineserecord;")
    (u/test-exec! "truncate chinesegrapheme;")
    (p/parse-chinese-json tc "medium_resources.json")
    (p/parse-graphemes-json tc "graphemes.json"))

  (do (u/test-exec! "truncate app_user;")

      (u/test-exec! "truncate session;")
      (def sample-user {:name {:first-name "Alex" :last-name "Vith"}
                        :id "ta-user-id" :resourceType "app-user"
                        :password "a" :username "a"})
      (matcho/match (ta {:request-method "post" :uri "/register"
                         :body {:username (.encode (java.util.Base64/getEncoder) (.getBytes "a"))
                                :email (.encode (java.util.Base64/getEncoder) (.getBytes "anton@mail.ri"))
                                :password (.encode (java.util.Base64/getEncoder) (.getBytes "a"))}})

                    {:status 200, :body [:resource
                                         {:id string?
                                          :email "anton@mail.ri",
                                          :password "0cc175b9c0f1b6a831c399e269772661",
                                          :username "a",
                                          :resourceType "app-user"}]}))

  (def cookies (:cookies (ta {:request-method "post"
                               :uri "/login"
                               :body {:username (.encode (java.util.Base64/getEncoder)
                                                         (.getBytes "a"))
                                      :password (.encode (java.util.Base64/getEncoder)
                                                         (.getBytes "a"))}})))

  (matcho/match cookies {:jwt-token string?})

  (matcho/match (ta {:request-method "post" :uri "/login"
                      :body {:username (.encode (java.util.Base64/getEncoder) (.getBytes "aser"))
                             :password (.encode (java.util.Base64/getEncoder) (.getBytes "a"))}})
                {:status 400, :body {:message "No user with this username"}})

  (matcho/match (ta {:request-method "get" :uri "$jwt-required" :cookies cookies})
                {:status 200})

  (matcho/match (ta {:request-method "get" :uri "$jwt-required"
                     :cookies {:jwt-token "asd.asd.addd"}})
                {:status 403})

  )

(deftest default-handler-test

  (do
    (def tc u/test-ctx)
    (def ta (u/test-app))
    (u/test-exec! "truncate chineserecord;")
    (u/test-exec! "truncate chinesegrapheme;"))

  (matcho/match
   (ta {:request-method "get" :uri "/a"})
   {:status 404
    :body {:uri "/a", :request {:params nil,
                                :form-params nil,
                                :query-params nil,
                                :query-string nil,
                                :request-method "get"}}})

  (matcho/match
   (ta {:request-method "get" :uri "/doom-guy"})
   {:status 404
    :body {:uri "/doom-guy", :request {:params nil,
                                       :form-params nil,
                                       :query-params nil,
                                       :query-string nil,
                                       :request-method "get"}}})


  (matcho/match
   (ta {:request-method "post" :uri "/doom-guy" :body {:a 1}})
   {:status 404,
    :body
    {:uri "/doom-guy",
     :request
     {:params nil, :query-params nil, :query-string nil, :request-method "post"}}})

  (matcho/match
   (ta {:request-method "delete" :uri "/doom-guy"})
   {:status 404}))

(deftest resource-handler

  (do
    (def tc u/test-ctx)
    (def ta (u/test-app))
    (u/test-exec! "truncate chineserecord;")
    (u/test-exec! "truncate chinesegrapheme;")
    (p/parse-chinese-json tc "medium_resources.json")
    (p/parse-graphemes-json tc "graphemes.json"))

  (do (u/test-exec! "truncate app_user;")
      (db/query-first tc "select count(*) from car;")
      (u/test-exec! "truncate car;"))

  #_(u/test-exec! "drop table app_user;")

  (def uri "/resource/car?_limit=10&_offset=10&where=<brand,d:audi,BMW><color,0,code:red>&group-by=<brand,0,code><color>&order-by=<car,0,id>,<a,d,v>&paths=<brand,b,f:brand,0,code>,<color:color>")

  #_(u/test-query "select * from app_user;")

  (def sample-user
    {:name {:first-name "Alex" :last-name "Vith"}
     :password "AND"
     :username "MMM"
     :id "ta-user-id"
     :resourceType "app-user"})

  (def sample-broken-user
    {:name {:first-name "Alex" :last-name "Vith"}})

  (matcho/match
   (ta {:request-method "post" :uri "/resource/" :body sample-user})
   {:status 200, :body {:uri "/resource/", :response [:resource
                                                      {:id string?
                                                       :name {:last-name "Vith", :first-name "Alex"},
                                                       :password "AND",
                                                       :username "MMM",
                                                       :resourceType "app-user"}]}})

  (matcho/match
   (ta {:request-method "post"
        :uri "/resource/"
        :body sample-broken-user})
   {:status 200, :body {:uri "/resource/",
                        :response {:error "resourceType required"}}})

  (matcho/match
   (ta {:request-method "get" :uri uri})
   {:status 200})

  (def uri2 "/resource/car?_limit=10&_offset=10&where=<brand,d:audi,BMW><color,0,code:red>&order-by=<car,0,id>,<a,d,v>")

  (matcho/match
   (ta {:request-method "get" :uri uri2})
   {:status 200
    :body empty?})

  (def false-uri "/resource/car?group-by=<brand>&paths=<color:color>")

  (matcho/match
   (ta {:request-method "get" :uri false-uri})
   {:status 200
    :body {:errors string?
           :method string?}})

  (is (= 0 (:count (db/query-first tc "select count(*) from car;"))))

  (def car-body
    {:brand "brand"
     :color "color"
     :car-registration-number "number"
     :id "idididididid"
     :resourceType "car"})

  (matcho/match
   (ta {:request-method "post" :uri "/resource/" :body car-body})
   {:status 200,
    :body {:uri "/resource/",
           :response [:resource {:brand "brand",
                                 :color "color",
                                 :resourceType "car",
                                 :car-registration-number "number"}]}})

  (is (= 1 (:count (db/query-first tc "select count(*) from car;"))))

  #_#_#_(ta {:request-method "delete" :uri "/resource/" :body {:resourceType "car"
                                                         :ids ["idididididid"]}})

  (is (= 0 (:count (db/query-first tc "select count(*) from car;"))))

  (is (number? (:count (db/query-first tc "select count(*) from pg_indexes
                                            where tablename = 'car';"))))
  )
