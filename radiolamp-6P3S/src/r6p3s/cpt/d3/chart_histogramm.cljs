(ns r6p3s.cpt.d3.chart-histogramm
  (:require [cljs.core.async :refer [put! chan <!]]
            [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [r6p3s.core :as rc]
            [r6p3s.common-input :as common-input]
            [r6p3s.cpt.d3.core :as d3c]
            [cljsjs.d3]))




(def app-init
  {:data []})

(defn component [app own {:keys [
                                 main-width
                                 main-height
                                 top
                                 left
                                 rigth
                                 bottom
                                 x-value-fn
                                 y-value-fn
                                 y-label
                                 title
                                 description
                                 fill
                                 over-fill
                                 ]
                          :or   {main-width  d3c/full-screen-width
                                 main-height 300
                                 top         15
                                 left        60
                                 rigth       15
                                 bottom      20
                                 y-label     ""
                                 fill        "orange"
                                 over-fill   "rebeccapurple"}

                          :as   opts}]

  (let [chart-width  (- main-width left rigth)
        chart-height (- main-height top bottom)]
    (reify
      om/IInitState
      (init-state [_]
        {;;:chan-update (chan)
         :path-id       (rc/uniq-id "path")
         :chart-pano-id (rc/uniq-id "chart-pano")
         :chart-pano    nil})

      ;; om/IWillMount
      ;; (will-mount [this]
      ;;   (let [{:keys [chan-update]} (om/get-state own)]
      ;;     (println "OM: chart-histogramm -> will-mount")

      ;;     (go
      ;;       (while true
      ;;         (let [_ (<! chan-update)]
      ;;           (println "OM: chart-histogramm -> chan-update -> run! "))))

      ;;     (put! chan-update 1)))

      om/IDidMount
      (did-mount [_]
        (let [{:keys [chart-pano-id]} (om/get-state own)]
          (println "OM: chart-histogramm -> did-mount")
          (let [svg (.select js/d3 (str "#" chart-pano-id))]
            (om/set-state! own :chart-pano svg))))

      om/IWillUnmount
      (will-unmount [_]
        (let [{:keys []} (om/get-state own)]
          (println "OM: chart-histogramm -> will-unmount")))




      om/IDidUpdate
      (did-update [_ _ _]
        (let [{:keys [chart-pano
                      path-id]} (om/get-state own)

              data              (@app :data)

              x-scale           (-> js/d3 .-scale .ordinal
                                    (.domain (->> data (map x-value-fn) into-array))
                                    (.rangeRoundBands #js [0 chart-width] 0.1))

              y-scale           (-> js/d3 .-scale .linear
                                    (.domain (d3c/min-max y-value-fn min 1 max 1.1 data))
                                    (.range  #js [chart-height 0]))

              x-axis            (-> js/d3 .-svg .axis (.scale x-scale) (.orient "bottom"))
              y-axis            (-> js/d3 .-svg .axis (.scale y-scale) (.orient "left"))


              data-array        (into-array data)
              ]

          (-> chart-pano
              (.selectAll "g.y.axis")
              (.call y-axis))

          (-> chart-pano
              (.selectAll "g.x.axis")
              (.call x-axis))

          (let [rects (-> chart-pano (.selectAll "rect") (.data data-array))]
            (-> rects
                .enter
                (.append "rect")
                (.style "fill" fill)
                (.on "mouseover"
                     (fn [row]
                       (this-as this
                         (-> js/d3
                             (.select this)
                             (.style "fill" over-fill)))))
                (.on "mouseout"
                     (fn [row]
                       (this-as this
                         (-> js/d3
                             (.select this)
                             (.style "fill" fill))))))

            (-> rects
                (.attr "x" #(x-scale (x-value-fn %)))
                (.attr "y" #(y-scale (y-value-fn %)))
                (.attr "width" (.rangeBand x-scale))
                (.attr "height"#(- chart-height (y-scale (y-value-fn %)))))

            (-> rects
                .exit
                .remove))
          ))

      om/IRenderState
      (render-state [_ {:keys [chart-pano-id path-id chan-update]}]
        (dom/div
         #js {:className "chart-frame"}
         (dom/h4 #js {:className "" :style #js {:marginLeft left}} title)
         (when description
           (dom/p #js {:className "text-info" :style #js {:marginLeft left}} description))
         (dom/svg #js {:width main-width :height main-height}
                  (dom/g #js {:id chart-pano-id :transform (str "translate(" left "," top ")")}
                         (dom/path #js {:id path-id})
                         (dom/g #js {:className "y axis"}
                                (dom/text #js {:transform "rotate(-90)"
                                               :y         "6" :dy ".71em" :style #js {:textAnchor "end"}}
                                          y-label))
                         (dom/g #js {:className "x axis" :transform (str "translate(0," chart-height ")")})
                         (dom/g #js {:className "x brush" :width chart-width :height chart-height}))))))))
