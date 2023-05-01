(ns keys.core
  (:require [clj-http.client :as http]
            [clojure.pprint :as pprint]
            [csv-helper.core :as csv-helper]
            [clojure.string :as str]
            [hickory.core :as h]))

(def global-fp "html_resources/keys.html")


(defn safe-content
  [chinese-content]
  (let [parsik     (fn [el] (mapv str/trim
                                  (remove str/blank?
                                          (->> el
                                               rest
                                               (mapcat :content)))))
        parse-fat  (fn [el]
                     {:general (str/trim (first el))
                      :additional (parsik el)})
        parse-thin (fn [el] {:general (->> el first :content (mapv str/trim) first)
                             :additional (parsik el)})]
    (cond (and (> (count chinese-content) 1)
               (string? (first chinese-content)))
          (parse-fat chinese-content)
          (and (> (count chinese-content) 1)
               (associative? (first chinese-content)))
          (parse-thin chinese-content)
          :else {:general (first chinese-content)})))

(defn parse-element
  [{:keys [attrs content] :as _element}]
  (let [_class-info (get attrs :class "Unknown")
        [general chinese pinyin naming] content]
    {:id      (first (:content general))
     :chinese (safe-content (:content chinese))
     :pinyin  (-> pinyin :content first)
     :naming  (mapv str/trim (-> naming :content first str (str/split #";")))
     :type    "radical"}))

(defn get-all-keys-from-file
  [file-path]
  (->> (h/as-hickory (h/parse (slurp file-path)))
       :content
       last
       :content
       last
       :content
       (filter #(= (:tag %) :table))
       first
       :content
       last
       :content
       (mapcat :content)
       (map parse-element)
       drop-last))

(comment

  (get-all-keys-from-file global-fp)


  )
