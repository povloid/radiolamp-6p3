(ns r6p3s.cpt.d3.chart-2
  (:require [cljs.core.async :refer [put! chan <!]]
            [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [r6p3s.core :as rc]
            [r6p3s.common-input :as common-input]
            [r6p3s.cpt.d3.core :as d3c]
            [cljsjs.d3]))



(defn app-init [{:keys [x-value-fn yx-schema data]
                 :or {x-value-fn (fn [_] 1) yx-schema [] data []}}]
  {
   :x-value-fn x-value-fn
   :yx-schema yx-schema
   :data data})

(defn component [app own {:keys [main-width
                                 main-height
                                 top
                                 left
                                 rigth
                                 bottom
                                 on-brushend-fn
                                 domain-y-min
                                 domain-y-max
                                 y-label
                                 title
                                 description
                                 ]
                          :or   {main-width  d3c/full-screen-width
                                 main-height 300
                                 top         15
                                 left        60
                                 rigth       15
                                 bottom      20
                                 y-label     ""}

                          :as   opts}]
  (let [chart-width  (- main-width left rigth)
        chart-height (- main-height top bottom)
        ;;y-values-scheme (map #(assoc % :path-id (rc/uniq-id "path")) y-values-scheme)-and-data-and-data
        ]



    (reify
      om/IInitState
      (init-state [_]
        {:chart-pano-id (rc/uniq-id "chart-pano")
         :chart-pano    nil})

      ;; om/IWillMount
      ;; (will-mount [this]
      ;;   (let [{:keys [chan-update]} (om/get-state own)]
      ;;     (println "OM: chart-2 -> will-mount")

      ;;     (go
      ;;       (while true
      ;;         (let [_ (<! chan-update)]
      ;;           (println "OM: chart-2 -> chan-update -> run! "))))

      ;;     (put! chan-update 1)))

      om/IDidMount
      (did-mount [_]
        (let [{:keys [chart-pano-id]} (om/get-state own)]
          (println "OM: chart-2 -> did-mount")
          (let [svg (.select js/d3 (str "#" chart-pano-id))]
            (om/set-state! own :chart-pano svg))))

      om/IWillUnmount
      (will-unmount [_]
        (let [{:keys []} (om/get-state own)]
          (println "OM: chart-2 -> will-unmount")))




      om/IDidUpdate
      (did-update [_ _ _]
        (let [{:keys [chart-pano]} (om/get-state own)

              {:keys [x-value-fn
                      yx-schema
                      data]}       @app
              ;; Размерность x
              x-scale              (-> js/d3 .-time  .scale
                                       (.domain (d3c/min-max #(-> % x-value-fn .getTime) min max data))
                                       ;;(.domain (.extent js/d3 data :pdate))
                                       (.range #js [0 chart-width]))
              ;; Размерность y
              all-y                (->> yx-schema
                                        (map :y-value-fn)
                                        (reduce
                                         (fn [a y-value-fn]
                                           (let [[min max] (d3c/min-max y-value-fn min 1 max 1.1 data)]
                                             (conj a min max)))
                                         []))
              y-min-max            #js [(or domain-y-min (apply min all-y))
                                        (or domain-y-max (apply max all-y))]
              y-scale              (-> js/d3 .-scale .linear
                                       ;;(.domain (min-max y-value-fn min 1 max 1.1 data))
                                       (.domain y-min-max)
                                       (.range  #js [chart-height 0]))

              ;; Линейки
              x-axis               (-> js/d3 .-svg .axis (.scale x-scale) (.orient "bottom"))
              y-axis               (-> js/d3 .-svg .axis (.scale y-scale) (.orient "left"))



              yx-schema-array      (into-array yx-schema)

              ]

          (let [paths (-> chart-pano
                          (.select "g.charting.pathes")
                          (.selectAll "path")
                          (.data yx-schema-array))]

            (-> paths
                .enter
                (.append "path"))

            (-> paths
                (.style "stroke" (fn [srow] (:stroke srow)))
                (.style "stroke-width" "1.5px")
                (.style "fill" "none")
                (.datum (fn [{:keys [y-value-fn filter-fn] :as srow}]
                          (->> data
                               (reduce
                                (fn [a row]
                                  (let [next-row [(x-scale (x-value-fn row)), (y-scale (y-value-fn row))]]
                                    (if (nil? filter-fn) (conj a next-row)
                                        (if (filter-fn row) (conj a next-row)
                                            a))))
                                [])
                               into-array)))
                (.attr "d" (-> js/d3
                               .-svg
                               .line
                               (.x first)
                               (.y second))))

            (-> paths
                .exit
                .remove))


          ;; Масштабные линейки
          (-> chart-pano
              (.selectAll "g.y.axis")
              (.call y-axis))

          (-> chart-pano
              (.selectAll "g.x.axis")
              (.call x-axis))

          ;; Селектор [<--selector-->]
          (when on-brushend-fn
            (let [brush (-> js/d3 .-svg .brush (.x x-scale))]
              (.on brush "brushend"
                   (fn [v]
                     (let [selected (js->clj (.extent brush))
                           [x1 x2] selected]

                       ;; Левая пунктирная линия
                       (-> chart-pano
                           (.selectAll "g.lines.brush .brush-line-left")
                           (.attr "x1" (x-scale x1))
                           (.attr "y1" -6)
                           (.attr "x2" (x-scale x1))
                           (.attr "y2" (+ chart-height 7)))

                       ;; Правая пунктирная линия
                       (-> chart-pano
                           (.selectAll "g.lines.brush .brush-line-right")
                           (.attr "x1" (x-scale x2))
                           (.attr "y1" -6)
                           (.attr "x2" (x-scale x2))
                           (.attr "y2" (+ chart-height 7)))

                       (on-brushend-fn selected))))              

              (-> chart-pano
                  (.selectAll "g.x.brush")
                  (.call brush)
                  (.selectAll "rect")
                  (.attr "y" -6)
                  (.attr "height" (+ chart-height 7)))


              ;;Обнуляем селекторы
              (-> chart-pano
                  (.selectAll "g.lines.brush line")
                  (.attr "x1" 0)
                  (.attr "y1" 0)
                  (.attr "x2" 0)
                  (.attr "y2" 0))
              
              #_(on-brushend-fn [nil nil])
              ))

          ))

      om/IRenderState
      (render-state [_ {:keys [chart-pano-id path-id chan-update]}]
        (dom/div
         #js {:className "chart-frame"}
         (dom/h3 #js {:className "" :style #js {:marginLeft left}} title)
         (dom/p #js {:className "text-info" :style #js {:marginLeft left}} description)
         (->> @app
              :yx-schema
              (reduce
               (fn [a {:keys [stroke title]}]
                 (conj a (dom/span #js {:style #js {:color stroke}}
                                   "Δ" title " ")))
               [])
              (apply dom/p #js {:style #js {:marginLeft left}}))

         (dom/svg #js {:width main-width :height main-height}
                  (dom/g #js {:id chart-pano-id :transform (str "translate(" left "," top ")")}
                         (dom/g #js {:className "y axis"}
                                (dom/text #js {:transform "rotate(-90)"
                                               :y         "6" :dy ".71em" :style #js {:textAnchor "end"}}
                                          y-label))
                         (dom/g #js {:className "x axis" :transform (str "translate(0," chart-height ")")})
                         (dom/g #js {:className "charting pathes"})
                         (dom/g #js {:className "lines brush"}
                                (dom/line #js {:className "brush-line-left"})
                                (dom/line #js {:className "brush-line-right"}))
                         (dom/g #js {:className "x brush" :width chart-width :height chart-height}))))))))
