(ns ixinfestor.omut-ymap
  (:require-macros [cljs.core.async.macros :refer [go]])

  (:require [cljs.core.async :refer [put! chan <!]]
            [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]

            [goog.dom.classes :as gdc]


            [ixinfestor.net :as ixnet]
            [ixinfestor.omut :as omut]

            [clojure.set :as clojset]
            [clojure.string :as clojstr]
            )

  (:import [goog.dom query]))



(def ymap-app-state
  {:geo-objects [
                 ;;{:coordinates [55.88678512632115 37.585068359374986]}
                 ;;{:coordinates [55 38]}
                 ;;{:coordinates [55 39]}
                 ]})

(defn ymap-geo-objects [app]
  (app :geo-objects))

(defn ymap-app-geo-objects-set! [app path geo-objects]
  (assoc-in app (conj path :geo-objects) geo-objects))

(defn ymap-app-geo-objects-set!! [app geo-objects]
  (om/update! app :geo-objects geo-objects))



(defn geo-object [{:keys [type coordinates preset
                          balloon-content-header
                          balloon-content-body
                          icon-content hint-content
                          preset]
                   :or {type "Point"}}]
  (new js/ymaps.GeoObject
       ;;feature
       (clj->js { "geometry" (clj->js {:coordinates coordinates
                                       :type type})
                  "properties" (clj->js {:balloonContentHeader balloon-content-header
                                         :balloonContentBody balloon-content-body                                         
                                         :iconContent icon-content
                                         :hintContent hint-content
                                         })
                  })

       ;; options
       (clj->js {:preset (or preset  "islands#redStretchyIcon")})))

(defonce ids (atom 0))
(defn get-id [] (str "ymap-" (swap! ids inc)))


(defn ymap [app owner {:keys [chan-set-markers
                              chan-update
                              click-fn click-coords-fn
                              width
                              height]
                       :or {width "100%"
                            height "600px"}}]
  (reify
    om/IInitState
    (init-state [_]
      {:id (get-id)
       :chan-update (or chan-update (chan))
       :map-object nil
       :show-route? false
       })
    om/IDidMount
    (did-mount [_]
      (let [{:keys [id chan-update]} (om/get-state owner)]
        (.ready
         js/ymaps
         (fn []
           (let [map-object (new js/ymaps.Map id
                                 (clj->js {"center" (clj->js [59.95  30.316666666666666])
                                           "zoom" 8
                                           }))]


             (-> map-object .-controls (.add "routeEditor"))

             ;; Кнопка маршрута
             (let [b (new js/ymaps.control.Button
                          (clj->js {:data {:content "Маршрут"}
                                    :options {:selectOnClick true}}))]
               (-> b .-events (.add "select"
                                    (fn []
                                      (println "show route..." )
                                      (om/set-state! owner :show-route? true))))
               
               (-> b .-events (.add "deselect"
                                    (fn []
                                      (println "show route..." )
                                      (om/set-state! owner :show-route? false))))
               
               (-> map-object .-controls (.add b)))

             (when (or click-fn click-coords-fn)
               (-> map-object
                   .-events
                   (.add "click"
                         (fn [e]
                           ;;;(println "click: " e)

                           ;; Обработка события
                           (when click-fn (click-fn e))

                           ;; Получение координат щелчка
                           (when click-coords-fn
                             (let [coords (js->clj (.get e "coords"))]
                               ;;;(println coords)
                               (click-coords-fn coords) ))))))


             (om/set-state! owner :map-object map-object)
             )))))
    om/IWillUnmount
    (will-unmount [_]
      (println "Отмонтирование yandex map."
               (.destroy (om/get-state owner :map-object))
               "-> OK")
      )
    om/IRenderState
    (render-state [_ {:keys [id chan-update map-object show-route?]}]

      ;;(println ">>>>>>>" (-> map-object .-geoObjects))

      (when map-object
        (do
          (-> map-object .-geoObjects .removeAll)
          (doseq [o (@app :geo-objects)]
            (-> map-object .-geoObjects (.add (geo-object o))) )

          ;; Вариант с простыми маршрутами
          ;; #_(when show-route?
          ;;     (let [route-points (->> @app
          ;;                             :geo-objects
          ;;                             (sort-by #(* 1M (get-in % [:coordinates 0])))
          ;;                             (reduce
          ;;                              (fn [a {[lat lng] :coordinates}]
          ;;                                (conj a {:type "wayPoint" :point [lat lng]}))
          ;;                              [{:type "wayPoint" :point [60.10166 30.41011]}])
          ;;                             (#(conj % {:type "wayPoint" :point [60.10166 30.41011]})))]
          ;;       (.then (js/ymaps.route (clj->js  route-points)
          ;;                              (clj->js {:routingMode "auto"}))
          ;;              (fn [route]
          ;;                ;;(println (type route))
          ;;                ;;(println (-> route .getPaths .-options type))
          ;;                #_ (-> route .getPaths .-options
          ;;                       (.set (clj->js {
          ;;                                       :balloonContentBodyLayout
          ;;                                       (js/ymaps.templateLayoutFactory.createClass
          ;;                                        "{{properties.humanJamsTime }}")
          ;;                                       :strokeColor "0000ffff"
          ;;                                       })))
          ;;                ;;(println ">>>" (.toArray (.getPaths route)))
          ;;                (println ">>>" (.getLength route))
          ;;                (-> map-object .-geoObjects (.add route))
          ;;                ))))

          (when show-route?
            (let [route-points (->> @app
                                    :geo-objects
                                    (sort-by #(* 1M (get-in % [:coordinates 0] 0)))
                                    (reduce
                                     (fn [a{[lat lng] :coordinates}]
                                       (conj a [lat lng]))
                                     [[60.10166 30.41011]])
                                    vec
                                    (#(conj % [60.10166 30.41011])))
                  mr (new js/ymaps.multiRouter.MultiRoute
                          (clj->js {:referencePoints
                                    (clj->js route-points)})
                          (clj->js {:editorDrawOver false
                                    :wayPointDraggable true
                                    :viaPointDraggable true})
                          )]

              (-> map-object .-geoObjects (.add mr))))))




      (dom/div #js {:id id
                    :style #js {:width width
                                :height height
                                }}) )))


(defn dd-to-dms [dd]
  (let [g   (/ (int dd) 1)
        m-0 (* (- dd g) 60)
        m   (/ (int m-0) 1)
        s   (+ (.toFixed  (* (- m-0 m) 60) 8))]
    [g m s]))


(defn dms-to-dd [[g m s]]
  (+ (/ (+ (/ s 60) m) 60) g))



(defn input-coordinates [app _]
  (reify
    om/IRender
    (render [_]
      (println (str "Y-COORDINATES:" @app))
      (apply
       dom/div
       nil
       (->> @app
            :geo-objects
            (map
             (fn [i {[lat lng] :coordinates}]
               (let [[lat-g lat-m lat-s] (dd-to-dms lat)
                     [lng-g lng-m lng-s] (dd-to-dms lng)]

                 (println "<"[lat-g lat-m lat-s]">")
                 (println (type lat-g))

                 (dom/div
                  nil
                  (dom/div
                   #js {:className "form-group"}

                   ;; LAT ----------------------------------------------------------------------------
                   (dom/div
                    #js {:className "input-group"}
                    (dom/div #js {:className "input-group-addon"
                                  :style #js {:width 60}} "LAT")
                    (dom/input
                     #js {:className "form-control"
                          :value lat
                          :onChange (fn [e]
                                      (let [v (.. e -target -value)]
                                        (when (omut/is-numeric? v)
                                          (om/update! app [:geo-objects i :coordinates 0]
                                                      (new js/Number v))))
                                      1)

                          :placeholder "Координата"
                          :type "number" :min "0.000000000000000" :step "0.00001"})

                    (dom/div #js {:className "input-group-addon"} "°")
                    (dom/input
                     #js {:className "form-control"
                          :style #js {:width 70}
                          :value lat-g
                          :onChange (fn [e]
                                      (let [v (.. e -target -value)]
                                        (when (omut/is-numeric? v)
                                          (om/update! app [:geo-objects i :coordinates 0]
                                                      (dms-to-dd [(new js/Number v) lat-m lat-s]))))
                                      1)
                          :placeholder "Координата"
                          :type "number" :min "0.0" :step "1.0"})

                    (dom/div #js {:className "input-group-addon"} "'")
                    (dom/input
                     #js {:className "form-control"
                          :style #js {:width 70}
                          :value lat-m
                          :onChange (fn [e]
                                      (let [v (.. e -target -value)]
                                        (when (omut/is-numeric? v)
                                          (om/update! app [:geo-objects i :coordinates 0]
                                                      (dms-to-dd [lat-g (new js/Number v) lat-s]))))
                                      1)
                          :placeholder "Координата"
                          :type "number" :min "0" :step "1" :max "60"})

                    (dom/div #js {:className "input-group-addon"} "''")
                    (dom/input
                     #js {:className "form-control"
                          :style #js {:width 100}
                          :value lat-s
                          :onChange (fn [e]
                                      (let [v (.. e -target -value)]
                                        (when (omut/is-numeric? v)
                                          (om/update! app [:geo-objects i :coordinates 0]
                                                      (dms-to-dd [lat-g lat-m (new js/Number v)]))))
                                      1)

                          :placeholder "Координата"
                          :type "number" :min "0.0" :step "0.1" :max "60.0"})
                    )

                   ;; LNG ---------------------------------------------------------------------------

                   (dom/div
                    #js {:className "input-group"}

                    (dom/div #js {:className "input-group-addon"
                                  :style #js {:width 60}} "LNG")
                    (dom/input
                     #js {:className "form-control"
                          :value lng
                          :onChange (fn [e]
                                      (let [v (.. e -target -value)]
                                        (when (omut/is-numeric? v)
                                          (om/update! app [:geo-objects i :coordinates 1]
                                                      (new js/Number v))))
                                      1)

                          :placeholder "Координата"
                          :type "number" :min "0.000000000000000" :step "0.00001"})

                    (dom/div #js {:className "input-group-addon"} "°")
                    (dom/input
                     #js {:className "form-control"
                          :style #js {:width 70}
                          :value lng-g
                          :onChange (fn [e]
                                      (let [v (.. e -target -value)]
                                        (when (omut/is-numeric? v)
                                          (om/update! app [:geo-objects i :coordinates 1]
                                                      (dms-to-dd [(new js/Number v) lng-m lng-s]))))
                                      1)
                          :placeholder "Координата"
                          :type "number" :min "0.0" :step "1.0"})

                    (dom/div #js {:className "input-group-addon"} "'")
                    (dom/input
                     #js {:className "form-control"
                          :style #js {:width 70}
                          :value lng-m
                          :onChange (fn [e]
                                      (let [v (.. e -target -value)]
                                        (when (omut/is-numeric? v)
                                          (om/update! app [:geo-objects i :coordinates 1]
                                                      (dms-to-dd [lng-g (new js/Number v) lng-s]))))
                                      1)
                          :placeholder "Координата"
                          :type "number" :min "0" :step "1" :max "60"})

                    (dom/div #js {:className "input-group-addon"} "''")
                    (dom/input
                     #js {:className "form-control"
                          :style #js {:width 100}
                          :value lng-s
                          :onChange (fn [e]
                                      (let [v (.. e -target -value)]
                                        (when (omut/is-numeric? v)
                                          (om/update! app [:geo-objects i :coordinates 1]
                                                      (dms-to-dd [lng-g lng-m (new js/Number v)]))))
                                      1)

                          :placeholder "Координата"
                          :type "number" :min "0.0" :step "0.1" :max "60.0"})
                    )
                   )
                  ))) (range))) ))))
