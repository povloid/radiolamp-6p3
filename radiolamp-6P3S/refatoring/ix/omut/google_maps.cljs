(ns ix.omut-google-maps
  (:require-macros [cljs.core.async.macros :refer [go]])

  (:require [cljs.core.async :refer [put! chan <!]]
            [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]

            [goog.dom.classes :as gdc]

            [ix.net :as ixnet]

            [clojure.set :as clojset]
            [clojure.string :as clojstr]
            )

  (:import [goog.dom query]))



(def google-map-app-state
  {:markers [
             ;;{:lat 50 :lng 30 :title "test !"}
             ;;{:lat 60 :lng 40 :title "test !"}
             ;;{:lat 70 :lng 50 :title "test !"}
             ]})

(defn google-map-set-one-marker [app row]
  (om/update! app :markers [row]))


(defonce ids (atom 0))
(defn get-id [] (str "google-maps-" (swap! ids inc)))

(defn marker [m {:keys [lat lng title]}]
  (new google.maps.Marker
       (clj->js {"position" (google.maps.LatLng. lat lng)
                 "title" title
                 "map" m
                 })))


(defn google-map [app owner {:keys [chan-set-markers
                                    chan-update
                                    click-fn
                                    width
                                    height]
                             :or {width "100%"
                                  height "600px"}}]
  (reify
    om/IInitState
    (init-state [_]
      {:id (get-id)
       :chan-update (or chan-update (chan))
       :markers-js []})
    om/IDidMount
    (did-mount [_]
      (let [{:keys [id chan-update]} (om/get-state owner)
            map-canvas  (.getElementById js/document id)
            map-options (clj->js {"center" (google.maps.LatLng. 59.57, 30.19)
                                  "zoom" 8
                                  "mapTypeId" google.maps.MapTypeId.ROADMAP})

            m (js/google.maps.Map. map-canvas map-options)]

        (when click-fn
          (.addListener m "click"
                        (fn [e]
                          (let [v (-> e .-latLng)]
                            (click-fn {:lat (.lat v) :lng (.lng v)})))))


        (when chan-set-markers
          (go (while true
                (let [markers (<! chan-set-markers)]
                  (om/update! app :markers markers)
                  (put! chan-update 1) ))))

        (go (while true
              (let [_ (<! chan-update)]
                ;; Отрубаем старые маркеры
                (doseq [marker (om/get-state owner :markers-js)]
                  (.setMap marker nil))

                ;; Создаем новые
                (->> @app
                     :markers
                     (map (partial marker m))
                     vec
                     (om/set-state! owner :markers-js)) )))

        (put! chan-update 1)

        

        ))
    om/IWillUnmount
    (will-unmount [_]
      (println "Отмонтирование!!!")
      ;;(->> owner om/get-state :id (.getElementById js/document) .remove)
      )
    om/IRenderState
    (render-state [_ {:keys [id chan-update]}]
      (dom/div #js {:id id
                    :style #js {:width width
                                :height height
                                }}) )))
