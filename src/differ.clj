(ns differ
  (:require [clojure.data :as d]))


(comment

  (def res1
    {:resource {:created "2022-05-04T20:57:29+00:00",
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
                           :hieroglyph_id "419"}, :resourceType "ChineseRecord"}})

  (def res2 {:resource {:created "2022-05-04T20:57:29+00:00",
                :examples [{:pinyin "xiǎngfǎ", :chinese "想法",
                            :translation {:de [], :en [],
                                          :ru ["мысль" "идея" "example-meaning"],
                                          :esp ["asdasd"], :srb []}}
                           {:pinyin "sìxiǎng", :chinese "思想",
                            :translation {:de [], :en ["ideology"],
                                          :ru ["образ мысли" "идеология"],
                                          :esp [], :srb []}}
                           {:pinyin "xiǎnghǎo", :chinese "想好",
                            :translation {:de [], :en ["decide"],
                                          :ru ["решить"], :esp [],
                                          :srb []}}
                           {:pinyin "xiǎnghǎo", :chinese "想好",
                            :translation {:de [], :en ["decide"],
                                          :ru ["решить"], :esp [],
                                          :srb []}}],
                :resource {:pinyin "xiăng", :chinese "想",
                           :translation {:de [], :en [],
                                         :ru ["думать" "хотеть" "скучать"],
                                         :esp [], :srb []},
                           :hieroglyph_id "419"}, :resourceType "ChineseRecord"}})

  (let [_ (def diff (d/diff res1 res2))
        difference (last diff)]
    (if difference
      (merge )
      ))


)
