(ns ui.styles
  (:require [garden.core :as garden]
            [garden.units :as u]
            [goog.string :as gstring]
            [goog.string.format]))

(def px u/px)

(def *colors
  {:border "#ddd"
   :hover-color "white"
   :error "#d9534f"
   :hover-background "#0275d8"})

(def dark-gray "#343b51")
(def green "#28c194")
(def second-green "#16b67f")
(def blue "#2685e2")
(def red "#db4040")
(def yellow "#f4c648")
(def dark-yellow "#eeba00")

(def primary-blue "#017AFF")
(def blue-bg "#EFF6FF")

(def secondary-green "#39C5A6")
(def green-bg "#ECFFFB")

(def purple "#8D1FFF")
(def purple-bg "rgba(167,134,255,0.08)")

(def orange "#FFA016")
(def orange-bg "#FFFAE5")
(def light-gray   "#F9F9F9")

(defn color [nm]
  (get *colors nm))

(defn style [css]
  [:style (garden/css css)])

(defn twos
  ([] (twos 2))
  ([n] (lazy-seq (cons n (twos (* 2 n))))))

(def components-style
  (let [input-bg-color "#f9f9fa"
        input-border "1px solid #ebebee"]
    [:.form {:display :flex
             :flex-direction :column
             :padding "8px 32px 32px 0"}
     [:.cm-wrapper {:width "100%"}]
     [:.re-re-select
      {:display "block"
       :min-width 0
       :background-color input-bg-color
       :border input-border}]
     [:.actions {:display :flex
                 :flex-direction :row
                 :border-top "2px solid rgba(53, 59, 80, 0.1)"
                 :margin-top (u/px 18)
                 :padding-top (u/px 10)
                 :justify-content "flex-start"
                 :align-items :center}
      [:.btn {:height (u/px 32)
              :border-radius (u/px 2)
              :font-size (u/px 14)
              :font-weight 500
              :margin-left (u/px 4)
              :transition "background-color 200ms, color 100ms"}
       [:&.btn-success
        {:background-color "#4a90e2"
         :width (u/px 134)
         :border "solid 1px #4a90e2"}]
       [:&.btn-danger
        {:color :black
         :border :none
         :padding [[(u/px 9) 0 (u/px 9) (u/px 12)]]
         :background-color "#ffffff"}]]]
     [:.form-actions {:display :flex :flex-direction :row-reverse
                      :border-top input-border
                      :padding "10px 0"
                      :margin "5px 0"}
      [:.btn {:height (u/px 32)
              :border-radius "0px 2px"
              :margin-left "5px"
              :transition "background-color 200ms, color 100ms"}]]
     [:.zen-button-select
      {:border input-border}
      [:.option {:background-color input-bg-color}]
      [:.option.active
       {:font-weight 500
        :border-radius (u/px 2)
        :margin [[(u/px -1) (u/px 0) (u/px -1) (u/px -1)]]
        :color "#4a90e2"
        :background-color input-bg-color
        :border "solid 1px #4a90e2"}]]
     [:.re-radio-buttons
      {:border input-border}
      [:.option {:background-color input-bg-color}]
      [:.option.active
       {:font-weight 500
        :border-radius (u/px 2)
        :margin [[(u/px -1) (u/px 0) (u/px -1) (u/px -1)]]
        :color "#4a90e2"
        :background-color input-bg-color
        :border "solid 1px #4a90e2"}]]
     [:.form-row {:align-items "flex-start"
                  :display :flex
                  :margin-bottom "10px"}
      [:&.spacer {:margin-bottom "10px"
                  :margin-left 0}]
      (let [sp 8
            margin-left 16]
        [:.inp-block
         {:display :inline-block}
         [:.err-msg {:font-size (u/px 12) :color "#db4040"}]
         [:&.spacer
          {:margin-left (str margin-left "px")}]
         [:&.inp-1
          {:width (u/percent 100)}]
         (let [r (take 4 (twos))
               fw (fn [x]
                    (gstring/format
                     "calc(%d%% - %dpx)"
                     (/ 100.0 x)  (/ (* margin-left (dec x)) x)  ;; (- sp (let [s (/ sp x)] (if (= s 1) 0 s)))
                     ;; * 0 / 1
                     ;; * 1 / 2
                     ;; * 3 / 4
                     ;; * 7 / 8
                     ))]
           (reduce
            (fn [acc x]
              (conj acc [(keyword (str "&.inp-" x)) {:width (fw x)}]
                    [(keyword (str "&.inp-" x "-stretch")) {:min-width (fw x)}])) [] r))])
      [:.lbl {:display :block
              :min-height (u/px 24)
              :color "#868996"
              :margin-bottom (u/px 2)
              :margin-left (u/px 1)}
       [:&.required:after
        {:color "#db4040"
         :margin-left (u/px 2)
         :content "'*'"}]]
      [:.inp
       {:flex 1}
       [:.invalid {:border "1px solid #db4040 !important"}]
       ;;[:.re-form-field {:display "block"}]
       [:.info {:margin-left (u/px 15)}]
       [:.re-select {:padding "4px 10px"}]
       [:.tag
        {:border "1px solid #ddd"
         :border-radius (u/px 4)
         :color :inherit
         :text-decoration :inherit
         :position :relative
         :display :inline-flex
         :align-items :center
         :padding {:left (u/px 4)
                   :right (u/px 2)}
         :margin-right (u/px 8)}
        [:.cross
         {:cursor :pointer
          :font-size (u/px 16)}]]
       [:.re-input-wrap {:display "block"}]
       [:.err-msg
        {:font-size (u/px 12)
         :color "#db4040"}]
       [:input
        {:background-color input-bg-color
         :max-height "38px"
         :border-radius "6px"
         :border input-border}]
       [:.date-input
        {:border input-border
         :display :flex}
        [:input.re-input
         {:border :none
          :border-left input-border
          :border-radius 0
          :width (u/percent 100)
          :background-color input-bg-color}]]
       [:.re-tags-container
        {:display :block
         :background-color input-bg-color
         :border input-border}
        [:input {:background-color input-bg-color
                 :border :none}]]
       [:input
        {:padding "0px 10px"}
        [:&.error {:border "1px solid #db4040 !important"}]]]]]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; top-navigation
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def bar-item-widgets
  [:.zen-select {:position "relative"}

   [:.select {:display "flex"
              :line-height "24px"
              :padding "3px 0 3px 0"
              :cursor "pointer"
              :user-select "none"}
    [:.triangle {:color "gray"
                 :margin-right "8px"}]
    [:.choose-value {:color "#999"}]
    [:.cross {:color "#e1e1e1"
              :font-size "24px"
              :margin-left "3px"}
     [:&:hover {:color "gray"}]]]

   [:.input-options {:position "absolute"
                     :left 0
                     :width "100%"
                     :z-index 99
                     :margin-top "1px"}
    [:input {:width "100%"
             :padding "0px 10px"
             :background-color "#f9f9fa"
             :margin-bottom "-6px"
             :border "1px solid rgba(52, 59, 80, 0.1)"
             :line-height "32px"}
     [:&.inactive {:display "none"}]]
    [:.loader {:font-size "4px"
               :margin 0
               :position "absolute"
               :top "7px" ;;"40px"
               :right "16px"
               ;;:z-index 100
               }]
    [:.options {:display "flex"
                :flex-direction "column"
                :border "1px solid #ddd"
                :overflow-y "auto"
                ;;:max-height "200px"
                :background-color "white"
                :width :max-content}
     [:.option {:cursor "pointer"
                :display "block"
                :padding "0 16px"
                :line-height "32px"}
      [:&:hover {:background-color "#f1f1f1"}]]
     [:.add {:cursor "pointer"
             :text-align "left"
             :padding "10px"
             :color "gray"}]
     [:.info {:text-align "center"
              :padding "10px"
              :color "gray"}]]
    ]]
  )


(def top
  [:*
   [:div.top {:display :flex
              :align-items :center
              :border-bottom "1px solid #DADADC"
              :position "relative"}

    [:h1 {:display :inline-block
          :font-size "28px"
          :line-height "46px"
          :font-weight 400
          :color "#353b51"
          :margin 0}]

    [:.date-selector {:margin "0 20px"
                      :display "inline-block"
                      :color "#666"
                      :flex-grow 99}
     [:.dropdown-input {:display "inline-block"
                        :color "blue"}]
     [:.date-input {:border "none"}
      [:input.re-input {:width "5.6em"}]]
     [:input.re-input {:font-size "24px"}]]

    [:.bar-item {:display "flex"
                 :align-items "center"
                 :margin-left "25px"}
     bar-item-widgets]

    [:.actions {:position "absolute"
                :top "10px"
                :right "5px"}
     [:a {:padding "10px"
          :color "#555"
          :font-size "30px"}]]]
   ])

(def bar
  [:*
   [:div.bar {:display "flex"
              :color "black"
              :flex-direction "row"
              :justify-content "flex-start"}
    [:.bar-item {:display "flex"
                 :align-items "center"
                 :margin-left "25px"}
     [:&.bar-search {:border "none"
                     :padding 0
                     :margin 0
                     :position "relative"
                     :flex-grow 1}
      [:.mi {:position "absolute"
             :top "7px"
             :color "gray"
             :font-size "24px"}]
      [:input {:width "100%"
               :margin 0
               :border "none!important"
               :padding "8px 0 8px 30px"}
       [:&:focus {:box-shadow "none"
                  :border "none!important"}]
       [:&:active {:box-shadow "none"
                   :border "none!important"}]]]

     bar-item-widgets

     [:&.action:hover {:background "#f1f1f1"
                       :text-decoration "none"}]
     [:&.new {:color green}]]]


   [:div.tabs
     {:margin-top "-2px"
      :display "flex"
      :align-items "stretch"
      :flex-direction "row"}
     [:a.link
      {:color "#d7d8dc"
       :padding "9px 1px 0 2px"
       :text-align "center"
       :flex-grow 1
       :border-top "2px solid transparent"
       :margin-right "50px"}
      [:&:hover {:color "#343b4e"
                 :text-decoration "none"
                 :border-top "2px solid black"}]
      [:&.active
       {:font-weight "500"
        :text-decoration "none"
        :color "#343b4e"
        :border-top "2px solid black"}]]]
   ])

(def basic-style
  [:* {:outline "none !important"}
   [:a:hover {:cursor "pointer"}]
   [:.btn.btn-success {:background-color "#01b4a0"
                       :border-color "#01b4a0"}]
   [:.spacer {:margin-left (u/px 5)}]
   [:button:hover {:cursor "pointer"}]
   [:footer {:min-height "50px"}]
   [:nav {:margin-bottom (u/px 25)}]
   [:.control-errors {:color (color :error) :font-size (px 12)}]

   top
   bar
   ])

(def crud-styles
  [:.crud
   components-style

   [:h3 {;;:color :red  ;; only to see where stle does not applied
         :padding-bottom 0
         ;;:padding "10px 0"
         :margin-bottom 0
         }]

   [:.total {:text-align :center
             :margin-bottom "10px"}]


   [:.re-select-container [:.re-re-select
                           [:.flex [:i.cross {:font-size "24px"}]]
                           [:.choose-value {:color "#999"}]]]
   [:.search-line
    {:padding "0 10px"
     :border-bottom "1px solid #888"}
    [:&:hover {:background "none!important"}]
    [:.icon [:.fa
             {:font-size "40px"
              :color "gray"
              :display "inline-block"
              :width "60px"}]]
    [:.search-input {:flex-grow 1
                     :padding "10px 0"
                     :font-size "18px"
                     :border "none"}
     #_[:&:focus
      {:border "none"
       :box-shadow "none!important"
       :border-bottom "1px soild #88f"}]]]
   [:.table-condensed
    [:thead
     {:background-color "rgba(53, 59, 80, 0.03)"}
     [:tr
      [:th {:padding [[(u/px 5) (u/px 12)]]}]]]]
   [:.item {:display "flex"
            :color "black"
            :flex-direction "row"
            :padding "5px 0"
            :justify-content "flex-start"
            :text-decoration "none"
            :border-bottom "1px solid rgba(0,0,0,.05)!important"}

    ;; Andrey
    [:&.without-border-bottom {:border-bottom "none !important"}]

    [:&.new {:color green}
     [:i.fa {:color green
             :width "60px"
             :font-size "47px"}]]]])

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; zenform form widgets
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def form-styles
  [:*
   [:.dropdown-input
    [:&.date {:width "100%"}
     [:.date-input {:width "100%"}
      [:.zen-select {:display "flex"
                     :width "100%"
                     :border "1px solid #ECEBEC"}
       [:.material-icons {:padding "0px"}]
       [:.select {:width "calc(100% - 60px)"
                  :border-radius "unset"
                  :border "none"}]
       [:.date-chevrons {:width "30px"
                         :display "flex"
                         :justify-content "center"
                         :align-items "center"
                         :line-height "24px"}]]]]]
   [:.zen-input {:flex 1
                 :width "96%"}
    [:input {:background-color "rgb(244, 245, 247)"
             :width "96%"
             :max-height "36px"
             :border-radius "6px"
             :font-size "14px"
             :border "1px solid rgba(52, 59, 80, 0.1)"
             :line-height "30px"
             :padding "0px 10px"}
     [:&.errors
      :&.error
      :&.invalid
      :&.is-invalid {:border "1px solid #db4040 !important"}]]]

   [:.zen-select {:position "relative"
                  :width "96%"}
    [:.select {:display "flex"
               :width "96%"
               :background-color "#f9f9fa"
               :border "1px solid rgba(52, 59, 80, 0.1)"
               :border-radius "2px"
               :line-height "24px"
               :padding "3px 12px"
               :cursor "pointer"
               :user-select "none"}
     [:.triangle {:color "gray"
                  :margin-right "8px"}]
     [:.choose-value {:color "#999"}]
     [:.cross {:color "#e1e1e1"
               :font-size "24px"
               :margin-left "auto"}
      [:&:hover {:color "gray"}]]
     [:&.invalid {:border "1px solid #db4040 !important"}]]

    [:.input-options {:position "absolute"
                      :left 0
                      :width "100%"
                      :z-index 99
                      :margin-top "1px"}
     [:input {:width "100%"
              :padding "0px 10px"
              :background-color "#f9f9fa"
              :border "1px solid rgba(52, 59, 80, 0.1)"
              :line-height "32px"}
      [:&.inactive {:display "none"}]]
     [:.loader {:font-size "4px"
                :margin 0
                :position "absolute"
                :top "7px"
                :right "16px"}]
     [:.options {:width "100%"
                 :display "flex"
                 :flex-direction "column"
                 :border "1px solid #ddd"
                 :overflow-y "auto"
                 :max-height "200px"
                 :background-color "white"}
      [:.option {:cursor "pointer"
                 :display "block"
                 :padding "0 16px"
                 :line-height "32px"}
       [:&:hover {:background-color "#f1f1f1"}]]
      [:.add {:cursor "pointer"
              :text-align "left"
              :padding "10px"
              :color "gray"}]
      [:.info {:text-align "center"
               :padding "10px"
               :color "gray"}]
      [:.info-left {:text-align "left"
               :padding "10px"
               :color "gray"}]]]]

   [:.zen-radio {:display "flex"
                 :flex-direction "column"}
    [:.option {:display "flex"
               :flex-direction "row"
               :align-items "center"
               :height "32px"
               :width "auto"
               :cursor "pointer"}
     [:.value {:font-size "14px"}]
     [:.radio {:display "flex"
               :justify-content "center"
               :align-items "center"
               :width "24px"
               :height "24px"
               :border "1px solid #ddd"
               :background-color "white"
               :border-radius "50%"
               :margin "0 8px 0 5px"}
      [:.inner-radio {:width "16px"
                      :height "16px"
                      :border-radius "50%"
                      :background-color "#007bff"}]]]]

   [:.zen-button-select {:display "inline-block"
                         :border "1px solid #ddd"
                         :border-radius (u/px 2)}
    [:&.error {:border "1px solid red !important"}]
    [:.option {:cursor "pointer"
               :transition "background-color 200ms, color 100ms"
               :background-color "white"
               :border-right "1px solid #ddd"
               :line-height (u/px- 30 2)
               :padding-left (u/px 16)
               :padding-right (u/px 16)
               :display "inline-block"}
     [:&:hover {:background-color "#f1f1f1"}]
     [:&:first-child {:border-radius "2px 0px 0px 2px"}]
     [:&:last-child {:border-radius "0px 2px 2px 0px"
                     :border-right "none"}]
     [:&.active {:background-color "#007bff"
                 :color "white"}]]]])

(def pill-styles
  (let [theme {:pallete {:action {:fg "#017AFF"
                                  :bg "#EFF6FF"}
                         :cancelled {:fg "#A3A3A3"
                                     :bg "#F8F8F8"}
                         :method {:bg "white"
                                  :fg "#00B695"}
                         :before {:bg "#F0F0F0"
                                  :fg "#1886FF"}
                         :after {:bg "#F0F0F0"
                                 :fg "#39C5A6"}
                         :primary {:fg "#AF5AE5" ;;violet
                                   :bg "#F8F6FF"}
                         :secondary {:fg "#979797" ;;gray
                                     :bg "#F0F0F0"}
                         :success {:fg "#39C5A6" ;;green
                                   :bg "#E9F9F0"}
                         :warning {:fg "#FF9700" ;;yellow
                                   :bg "#FFFAE5"}
                         :denied {:fg "#E02020" ;;red
                                  :bg "#FFEBEB"}}}]
    [:.pill {:width "fit-content"
             :border-radius "13px"
             :letter-spacing "0.3px"
             :line-height "16px"
             :padding "3px 8px"
             :font-size "12px"
             :white-space "nowrap"
             :font-weight 500
             :color (get-in theme [:pallete :action :fg])
             :background-color (get-in theme [:pallete :action :bg])}
     [:&--method {:color (get-in theme [:pallete :method :fg])
                  :background-color (get-in theme [:pallete :method :bg])}]
     [:&--cancelled {:color (get-in theme [:pallete :cancelled :fg])
                     :background-color (get-in theme [:pallete :cancelled :bg])}]
     [:&--primary {:color (get-in theme [:pallete :primary :fg])
                   :background-color (get-in theme [:pallete :primary :bg])}]
     [:&--before {:color (get-in theme [:pallete :before :fg])
                  :background-color (get-in theme [:pallete :before :bg])}]
     [:&--after {:color (get-in theme [:pallete :after :fg])
                  :background-color (get-in theme [:pallete :after :bg])}]
     [:&--secondary {:color (get-in theme [:pallete :secondary :fg])
                     :background-color (get-in theme [:pallete :secondary :bg])}]
     [:&--success {:color (get-in theme [:pallete :success :fg])
                   :background-color (get-in theme [:pallete :success :bg])}]
     [:&--warning {:color (get-in theme [:pallete :warning :fg])
                   :background-color (get-in theme [:pallete :warning :bg])}]
     [:&--denied {:color (get-in theme [:pallete :denied :fg])
                  :background-color (get-in theme [:pallete :denied :bg])}]
     [:&--long {:font-size "10px"}]]))

(def grid-navigation-styles
  [:body
   form-styles
   [:div.bar
    [:.search-widget {:font-size "16px"
                      :display "flex"
                      :border "1px solid #E1DFDF"
                      :border-radius "6px"
                      :background-color "white"
                      :padding-right "15px"
                      :align-items "center"}
     [:.zen-select {:font-size "13px"}
      [:.choose-value {:font-size "12px"
                       :font-weight "600"}]
      [:.select {:background-color "white"
                 :border "none"}]]
     [:.input
      [:input {:border-radius "6px"
               :padding "8px 16px"}]]]]
   [:.crud
    [:.form {:padding "0"
             :background-color "white"
             :width "calc(100% - 75px)"}
     [:.form-row {:margin-bottom "5px"}
      [:.lbl {:font-size "13px"
              :font-weight "500"
              :color "#7D7D7D"}]]]
    [:.grid-navigation {:margin-top "-15px"
                        :user-select "none"
                        :z-index "5"
                        :top 0
                        :position "sticky"
                        :margin-bottom "10px"
                        :border-radius "8px"}
     [:.sorted {:display "flex"
                :align-items "center"
                :margin-left "16px"
                :align-self "center"}
      [:.lbl {:font-weight "500"
              :margin-left "10px"
              :color "#7D7D7D"}]]
     [:.form-row {:margin-left "0px"
                  :margin-right "0px"}]
     [:.up {:display "flex"
            :justify-content "space-between"
            :align-items "center"
            :border-radius "8px"
            :background-color "#F8F8F8"}
      [:.bar {:height "50px"
              :align-items "center"
              :border-top-left-radius "6px"
              :border-bottom-left-radius "6px"
              :width "calc(100% - 75px)"
              :padding-left "15px"
              :background-color "#f8f8f8"}
       [:.form-row {:margin-bottom "0px"}]
       [:.inp-4 {:width "190px"}]
       [:.bar-search {:flex-grow "initial"}
        [:.search-widget {:width "398px"
                          :justify-content "space-between"}
         [:.input {:width "280px"}]]]]]
     [:.filters {:padding "15px 0px 20px 15px"
                 :margin-top "-10px"
                 :display "flex"
                 :background-color "white"
                 :border-radius "6px"
                 :box-shadow "-1px -1px 2px 0 rgba(0,0,0,0.06), 0 2px 6px 0 rgba(0,0,0,0.06)"}
      [:&.hidden {:display "none"}]]
     [:.date-input {:border "none"}]
     [:.clear {:width "75px"
               :display "flex"
               :justify-content "center"
               :color "#007AFF"}
      [:.label {:text-align "center"
                :margin-top "32px"
                :cursor "pointer"}]]
     [:.expand {:cursor "pointer"
                :display "flex"
                :align-items "center"
                :justify-content "center"
                :border-top-right-radius "6px"
                :border-bottom-right-radius "6px"
                :background-color "#F0F0F0"
                :height "50px"
                :color "#7D7D7D"
                :width "75px"}
      [:&.expanded {:user-select "none"
                    :color "#007AFF"}]]]]

   [:.zen-select {:border-radius "6px"}
    [:.selector {:background-color "white"
                 :width "30px"}
     [:&.right {:border-top-right-radius "6px"
                :border-bottom-right-radius "6px"
                :border-left "1px solid #ECEBEC"}]
     [:&.left {:border-right "1px solid #ECEBEC"
               :border-top-left-radius "6px"
               :border-bottom-left-radius "6px"}]]
    [:.select {:line-height "30px"
               :background-color "#F8F9FA"
               :border-radius "6px"}
     [:.value {:color "#007AFF"}]
     [:.cross {:display "flex"
               :align-items "center"}]]]])


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; widgets migrated from re-form
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;


(def default-base-consts
  {:m 12
   :h 16
   :h2 24
   :h3 32
   :w 8
   :w2 16
   :radius 2
   :selection-bg-color "#007bff"
   :hover-bg-color "#f1f1f1"
   :gray-color "rgba(52, 59, 81, 0.4)"
   :border "1px solid #ddd"
   :error-border "1px solid #db4040 !important"})

(defn codemirror-style
  [{:keys [w h h2 h3 selection-bg-color hover-bg-color border]}]
  [:*
   [:div.cm-wrapper
    {:width (u/px 244)}] ;; FIXME: discuss inputs' width policy
   [:.text :.CodeMirror
    {:width (u/percent 100)
     :height :auto
     :font-family  :inherit
     :letter-spacing :inherit
     :font-weight :inherit
     :font-style :inherti
     :color :inherit
     :padding [[0 (u/px w)]]
     :font-size :inherit
     :border border
     :border-radius (u/px 2)
     :line-height :inherit}]])

(defn date-time-style
  [{:keys [w h h2 h3 selection-bg-color gray-color hover-bg-color border error-border]}]
  [:*
   [:.date-chevrons
    {:display :inline-block
     :user-select :none
     :font-size (u/px h2)
     :cursor :pointer
     :line-height (u/px* h2 1.5)}]
   [:.dropdown-input
    [:.calendar-dropdown
     {:position :absolute
      :z-index 1
      :display :flex
      :justify-content :center
      :width (u/px 238)
      :border border
      :background-color :white}
     [:&.chevron-offset
      {:margin-left (u/px h2)}]
     [:.clickable {:cursor :pointer}]]]
   [:.date-input
    {:position :relative
     :border border
     :display :inline-flex
     :align-items :center
     :border-radius (u/px 2)
     :line-height "30px"}
    [:&.error
     {:border error-border}]
    [:i.material-icons {:display :inline-block
                        :cursor :pointer
                        :font-size (u/px h2)
                        :vertical-align :middle
                        :color gray-color
                        :padding [[0 (u/px w)]]}]
    [:input.re-input {:display :inline-block
                      :border :none
                      :padding-left 0
                      :width (u/px- 244 h2 (* 3 w))}]]])

(defn switch-box-style
  [{:keys [h h2 h3 selection-bg-color hover-bg-color border gray-color]}]
  [:.re-switch
   {:font-decoration :none}
   [:.re-label {:line-height (u/px h3)
                :display :inline-block}]
   [:.re-switch-line {:width (u/px* h3 2)
                      :top (u/px 5)
                      :height (u/px h2)
                      :display :inline-block
                      :background-color gray-color
                      :border-radius (u/px h2)
                      :margin-right (u/px 12)
                      :position :relative}
    [:.re-box {:width (u/px h3)
               :height (u/px h3)
               :transition "left 300ms ease-out"
               :background-color :white
               :border border
               :border-radius (u/percent 50)
               :position :absolute
               :top (u/px- (/ h2 2) (/ h3 2))
               :left 0}]]
   [:&.re-checked
    [:.re-box {:left (u/px h3)}]
    [:.re-switch-line {:background-color "#007bff"}]]])

(defn calendar-style [_]
  [:.re-calendar {:display "inline-block"}
   [:.calendar-title {:padding-top "20px"
                      :display :flex
                      :color :gray
                      :font-size "12px"}]
   [:table
    {:width "auto"}
    [:th {:font-size "16px" :text-align "center" :color "#888" :font-weight "normal"}]
    [:td {:text-align "center" :color "#aaa"
          :font-size "0.8em"
          :width "30px" :height "30px" :cursor "pointer"}
     [:&:hover {:background-color "#f1f1f1"}]
     [:&.active {:background-color "#007bff" :color "white!important"}]
     [:&.current {:color "#555"}]
     [:&.today {:font-weight "bold" :color "#333"}]]]
   [:.date {:font-size "16px" :text-align "center"}]])



(def input-style-fns
  [
   date-time-style
   calendar-style
   switch-box-style
   codemirror-style
   ])

(defn form-style-fn
  [{:keys [m h gray-color] :as s}]
  (into [:* #_{:font-family "Roboto, sans-serif"}
         [:.re-form-comp
          {:margin-bottom (u/px h)}]
         [:.re-form-label
          {:font-size (u/px h)
           :line-height (u/px* h 1.5)
           :margin-bottom (u/px 3)}
          [:&.sub
           {:font-size (u/px m)
            :line-height (u/px* m 1.5)
            :color gray-color}]]
         [:.re-form-field {:display "inline-block"}]]
        (map #(% s) input-style-fns)))

(def some-widgets-styles (form-style-fn default-base-consts))

(def common-styles
  [:* [:.flex {:display "flex"
               :align-items "center"}]
      [:.between {:justify-content "space-between"}]
      [:.m-right {:margin-right "10px"}]
      [:.red {:color "red"}]])
