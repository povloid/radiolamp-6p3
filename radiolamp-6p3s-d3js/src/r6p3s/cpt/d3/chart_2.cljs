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
                                 domain-y-min
                                 domain-y-max
                                 y-label
                                 title
                                 description
                                 on-selected-fn
                                 fill?]
                          :or   {main-width     d3c/full-screen-width
                                 main-height    300
                                 top            15
                                 left           60
                                 rigth          15
                                 bottom         20
                                 y-label        ""
                                 fill?          true
                                 on-selected-fn (fn [{:keys [interval]}]
                                                  (println "CHART -> SELECT INTERVAL: " interval))}

                          :as opts}]
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
                      data]} @app


              ;; Размерность x
              x-scale   (-> js/d3 .-time  .scale
                            (.domain (d3c/min-max #(-> % x-value-fn .getTime) min max data))
                            ;;(.domain (.extent js/d3 data :pdate))
                            (.range #js [0 chart-width]))
              ;; Размерность y
              all-y     (->> yx-schema
                             (map :y-value-fn)
                             (reduce
                              (fn [a y-value-fn]
                                (let [[min max] (d3c/min-max y-value-fn min 1 max 1.1 data)]
                                  (conj a min max)))
                              []))
              y-min-max #js [(or domain-y-min (apply min all-y))
                             (or domain-y-max (apply max all-y))]
              y-scale   (-> js/d3 .-scale .linear
                            ;;(.domain (min-max y-value-fn min 1 max 1.1 data))
                            (.domain y-min-max)
                            (.range  #js [chart-height 0]))

              ;; Линейки
              x-axis (-> js/d3 .-svg .axis (.scale x-scale) (.orient "bottom"))
              y-axis (-> js/d3 .-svg .axis (.scale y-scale) (.orient "left"))



              yx-schema-array (into-array yx-schema)

              ]

          ;; PATHES ------------------------------------------------------------------------------------------------
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
                (as-> paths
                    (if fill?
                      (-> paths
                          (.style "fill" (fn [srow] (:stroke srow)))
                          (.style "fill-opacity" "0.2"))
                      paths))
                (.datum (fn [{:keys [y-value-fn filter-fn] :as srow}]
                          (let [data (->> data
                                          (reduce
                                           (fn [a row]
                                             (let [next-row [(x-scale (x-value-fn row)),
                                                             (y-scale (y-value-fn row))]]
                                               (if (nil? filter-fn)
                                                 (conj a next-row)
                                                 (if (filter-fn row)
                                                   (conj a next-row)
                                                   a))))
                                           []))

                                data (if fill?
                                       (let [data (if-let [[x _] (last data)]
                                                    (conj data [x, (y-scale 0)])
                                                    data)
                                             data (if-let [[x _] (first data)]
                                                    (concat [[x (y-scale 0)]] data)
                                                    data)]
                                         data)
                                       data)

                                ]
                            (into-array data))))
                (.attr "d" (-> js/d3
                               .-svg
                               .line
                               (.x first)
                               (.y second))))

            (-> paths
                .exit
                .remove))


          ;; Масштабные линейки -----------------------------------------------------------------------------------
          (-> chart-pano
              (.selectAll "g.y.axis")
              (.call y-axis))

          (-> chart-pano
              (.selectAll "g.x.axis")
              (.call x-axis))

          ;; Селектор [<--selector-->] ----------------------------------------------------------------------------
          (when on-selected-fn
            (let [brush (-> js/d3 .-svg .brush (.x x-scale))]
              (.on brush "brushend"
                   (fn [v]
                     (let [selected (js->clj (.extent brush))
                           [x1 x2]  selected]

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

                       ;; SELECTION ----------------------------------------------------------------------------
                       (let [selected-rows (->> data
                                                (filter
                                                 (fn [row]
                                                   (let [v (x-value-fn row)]
                                                     (and (<= x1 v) (<= v x2)))))
                                                vec)
                             selected-rows (if-not (empty? selected-rows) selected-rows
                                                   ;; Тогда выбираем первое от первого селектора
                                                   (loop [[r & t] data]
                                                     (cond (nil? r)               []
                                                           (>= (x-value-fn r) x1) [r]
                                                           :else                  (recur t))))

                             x1-row (first selected-rows)
                             x2-row (last selected-rows)

                             interval (condp = (count selected-rows)
                                        0 []
                                        1 [x1-row]
                                        [x1-row (assoc x2-row :left-text? true)])

                             data-2 (into-array
                                     (for [s   yx-schema
                                           row interval]
                                       (assoc s :row row)))
                             ]                         


                         ;; CIRCLES ----------------------------------------------------------------
                         (let [circles (-> chart-pano
                                           (.select "g.charting.pathes")
                                           (.selectAll "circle")
                                           (.data data-2))]

                           (-> circles
                               .enter
                               (.append "circle"))

                           (-> circles
                               (.style "stroke" (fn [srow] (:stroke srow)))
                               (.style "stroke-width" "2px")
                               (.style "fill" "white")
                               ;;(.style "fill" (fn [srow] (:stroke srow)))
                               (.attr "r" "3px")

                               (.attr "cx" (fn [{:keys [row]}]
                                             (x-scale (x-value-fn row))))
                               (.attr "cy" (fn [{:keys [row y-value-fn]}]
                                             (y-scale (y-value-fn row)))))

                           (-> circles
                               .exit
                               .remove))

                         ;; TEXT ----------------------------------------------------------------
                         (let [texts (-> chart-pano
                                         (.select "g.charting.pathes")
                                         (.selectAll "text")
                                         (.data data-2))]

                           (-> texts
                               .enter
                               (.append "text"))

                           (-> texts
                               (.style "fill" (fn [srow] (:stroke srow)))
                               (.attr "dy" "1.2em")
                               (.attr "x" (fn [{:keys [row]}]
                                            (+ (x-scale (x-value-fn row)) (if (:left-text? row) -25 5))))
                               (.attr "y" (fn [{:keys [row y-value-fn]}]
                                            (- (y-scale (y-value-fn row)) 18)))
                               (.text (fn [{:keys [row y-value-fn]}]
                                        (str (y-value-fn row)))))

                           (-> texts
                               .exit
                               .remove))

                         ;; вызов функции по реальному селектору
                         (when on-selected-fn
                           (on-selected-fn {:interval interval :selected-rows selected-rows}))))))

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


              (-> chart-pano
                  (.selectAll "g.charting.pathes circle")
                  .remove)
              
              (-> chart-pano
                  (.selectAll "g.charting.pathes text")
                  .remove)

              (when on-selected-fn
                (on-selected-fn {:interval [] :selected-rows []}))

              ))
          ))

      om/IRenderState
      (render-state [_ {:keys [chart-pano-id path-id chan-update]}]
        (dom/div
         nil
         (dom/h4 #js {:className "" :style #js {:marginLeft left}} title)
         (when description
           (dom/p #js {:className "text-info" :style #js {:marginLeft left}} description))
         (->> @app
              :yx-schema
              (reduce
               (fn [a {:keys [stroke title]}]
                 (conj a (dom/span #js {:className "text-muted"}
                                   (dom/b #js {:style #js {:color stroke :fontSize "1.5em"}} "⚫")
                                   title " ")))
               [])
              (apply dom/div #js {:style #js {:marginLeft left }}))

         (dom/svg #js {:width main-width :height main-height}
                  (dom/g #js {:id chart-pano-id :transform (str "translate(" left "," top ")")}

                         (dom/g #js {:className "charting pathes"})
                         (dom/g #js {:className "x brush" :width chart-width :height chart-height})
                         (dom/g #js {:className "lines brush"}
                                (dom/line #js {:className "brush-line-left"})
                                (dom/line #js {:className "brush-line-right"}))

                         (dom/g #js {:className "x axis" :transform (str "translate(0," chart-height ")")})
                         (dom/g #js {:className "y axis"}
                                (dom/text #js {:transform "rotate(-90)"
                                               :y         "2" :dy ".71em" :style #js {:textAnchor "end"}}
                                          y-label)))))))))
