(ns ui.zenform.inputs.webcam
  (:require [reagent.core :as r]
            [garden.units :as u]
            [re-frame.core :as rf]))


(defonce widget-state (atom {}))


(defn- set-image [photo-name src]
  (let [{:keys [video canvas opts]} (get @widget-state photo-name)]
    (if canvas
      (let [context (.getContext canvas "2d")
            img (js/Image.)]
        (set! (.-crossOrigin img) "anonymous")
        (set! (.-onload img)
              #(do (.drawImage context img 0 0 (:width opts) (:height opts))
                   (set! (.. video -style -display) "none")
                   (set! (.. canvas -style -display) "block")))
        (set! (.-src img) src)
        true)
      false)))


(defn webcam [{:keys [photo-name] :as opts}]
  (r/create-class
   {:component-did-mount
    (fn [_]
      (swap! widget-state assoc-in [photo-name :opts] opts)

      ;; set images from our cash
      (let [set-images-value (::set-images @widget-state)]
        (doseq [[photo-name src] set-images-value]
          (if (set-image photo-name src)
            (swap! widget-state update ::set-images dissoc photo-name))))

      (let [video (get-in @widget-state [photo-name :video])]
        (when (and video
                   (.-mediaDevices js/navigator)
                   (.. js/navigator -mediaDevices -getUserMedia))

          #_(.. js/navigator
              -mediaDevices
              (getUserMedia (clj->js {:video true}))
              (then
               (fn [stream]
                      (swap! widget-state assoc-in [photo-name :video-tracks] (js->clj (.getVideoTracks stream)))

                      ;;[Deprecation] URL.createObjectURL with media streams is deprecated
                      ;;and will be removed in M71, around December 2018.
                      ;;Please use HTMLMediaElement.srcObject instead.
                      ;; try {
                      ;;      this.srcObject = stream;
                      ;; } catch (error) {
                      ;;      this.src = window.URL.createObjectURL(stream);
                      ;; }

                      ;;(set! (.-src video) (.. js/window -URL (createObjectURL stream)))
                      (set! (.-srcObject video) stream)
                   (.play video))))

          (-> (.getUserMedia (.-mediaDevices js/navigator) (clj->js {:audio false :video true}))
              (.then
               (fn [stream]
                 (swap! widget-state assoc-in [photo-name :video-tracks] (js->clj (.getVideoTracks stream)))
                 (set! (.-srcObject video) stream)
                 (.play video)
                 ))
              (.catch (fn [e] (prn e))))
          )))

    :component-will-unmount
    (fn [_]
      ;; FIXME here we deinit all widgets at once
      ;; assumption is they are all the part of one page
      (let [tracks (mapcat :video-tracks (vals @widget-state))]
        (doseq [t tracks]
          (when t (.stop t)))
        (reset! widget-state {})))

    :reagent-render
    (fn [{:keys [photo-name width height]}]
      [:div.re-webcam {:style {:padding "10px 0"}}
       [:video {:ref #(swap! widget-state assoc-in [photo-name :video] %)
                :width width
                :height height
                :autoPlay true}]
       [:canvas {:ref #(swap! widget-state assoc-in [photo-name :canvas] %)
                 :style {:display :none}
                 :width width
                 :height height}]])}))

(defn widget [{fp :form-path p :path :as opts}]
  [webcam (-> opts
              (dissoc :data)
              (assoc :photo-name p :on-change #(rf/dispatch [:zenform/on-change fp p %])))])

(rf/reg-fx
 :webcam/photo
 (fn [photo-name]
   (let [{:keys [video canvas opts]} (get @widget-state photo-name)
         context (.getContext canvas "2d")]
     (.drawImage context video 0 0 (:width opts) (:height opts))
     (set! (.. video -style -display) "none")
     (set! (.. canvas -style -display) "block")
     (.toBlob canvas #(do (swap! widget-state assoc-in [photo-name :blob] %)
                          ;; this is done to add value into zen-form
                          ((:on-change opts) true)
                          (when-let [on-image-ev (:on-image opts)]
                            (rf/dispatch (into on-image-ev [photo-name]))))))))

(rf/reg-fx
 :webcam/set-images
 (fn [args]
   (doall
    (for [[photo-name src] (partition 2 args)]
      (when-not (set-image photo-name src)
        (swap! widget-state assoc-in [::set-images photo-name] src))))))

(rf/reg-event-fx
 :webcam/photo
 (fn [{db :db} [_ photo-name]]
   {:webcam/photo photo-name}))

(rf/reg-fx
 :webcam/clear
 (fn [photo-name]
   (let [{:keys [video canvas opts]} (get @widget-state photo-name)]
     ((:on-change opts) nil)
     (swap! widget-state update photo-name #(dissoc % :blob))
     (set! (.. video -style -display) "block")
     (set! (.. canvas -style -display) "none"))))

(rf/reg-event-fx
 :webcam/clear
 (fn [{db :db} [_ photo-name]]
   {:webcam/clear photo-name}))

(rf/reg-cofx
 :webcam/photos
 (fn [cofx]
   (let [idx (->> @widget-state
                  vals
                  (map (fn [v] [(get-in v [:opts :photo-name]) (:blob v)]))
                  (into {}))]
     (assoc cofx :photos idx))))

