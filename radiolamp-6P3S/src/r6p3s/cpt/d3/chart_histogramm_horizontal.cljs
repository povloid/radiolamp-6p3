(ns r6p3s.cpt.d3.chart-histogramm-horizontal
  (:require [cljs.core.async :refer [put! chan <!]]
            [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [r6p3s.core :as rc]
            [r6p3s.common-input :as common-input]
            [r6p3s.cpt.d3.core :as d3c]
            [cljsjs.d3]))




(def app-init
  {:data []})

(defn component
  [app own {:keys [main-width
                   main-height
                   top
                   left
                   rigth
                   bottom
                   x-value-fn
                   y-value-fn
                   y-value-label-fn
                   y-label
                   min-p
                   max-p
                   title
                   description
                   fill
                   over-fill]
            :or   {main-width  d3c/full-screen-width
                   main-height 300
                   top         15
                   left        60
                   rigth       15
                   bottom      20
                   y-label     ""
                   min-p       1
                   max-p       1.1
                   fill        "orange"
                   over-fill   "rebeccapurple"}}]

  (let [chart-width  (- main-width left rigth)
        chart-height (- main-height top bottom)]
    (reify
      om/IInitState
      (init-state [_]
        {;;:chan-update (chan)
         :path-id       (rc/uniq-id "path")
         :chart-pano-id (rc/uniq-id "chart-pano")
         :chart-pano    nil})

      om/IDidMount
      (did-mount [_]
        (let [{:keys [chart-pano-id]} (om/get-state own)]
          (let [svg (.select js/d3 (str "#" chart-pano-id))]
            (om/set-state! own :chart-pano svg))))

      
      om/IDidUpdate
      (did-update [_ _ _]
        (let [{:keys [chart-pano]} (om/get-state own)

              data (@app :data)             

              y-scale (-> js/d3 .-scale .ordinal
                          (.domain (->> data (map x-value-fn) into-array))
                          (.rangeRoundBands #js [0 chart-height] 0.1))

              x-scale (-> js/d3 .-scale .linear
                          (.domain (d3c/min-max y-value-fn min min-p max max-p data))
                          (.range  #js [0 chart-width]))

              x-axis (-> js/d3 .-svg .axis (.scale x-scale) (.orient "bottom"))
              y-axis (-> js/d3 .-svg .axis (.scale y-scale) (.orient "left"))              
              

              data-array (into-array data)]

          (-> chart-pano
              (.selectAll "g.y.axis")
              (.call y-axis))

          (-> chart-pano
              (.selectAll "g.x.axis")
              (.call x-axis))

          (let [rects (-> chart-pano
                          (.selectAll "rect")
                          (.data data-array))]
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
                (.attr "y"      #(y-scale (x-value-fn %)))
                (.attr "height"  (.rangeBand y-scale))
                
                (.attr "x"     1)                
                (.attr "width" (fn [row]
                                 (let [d (x-scale (y-value-fn row))]
                                   (if (< 1 d) d 1)))))

            (-> rects
                .exit
                .remove))



          (let [texts (-> chart-pano
                          (.selectAll "text.value")
                          (.data data-array))]
            (-> texts
                .enter
                (.append "text")
                (.attr "class" "value"))

            (-> texts
                ;;(.style "text-anchor" "middle")
                (.attr "x" (fn [row]
                             (let [d (x-scale (y-value-fn row))]
                                   (+ 2 (if (< 1 d) d 1)))))                
                (.attr "y" #(+
                             (y-scale (x-value-fn %))
                             (/ (.rangeBand y-scale) 2)))
                (.text (fn [row]
                         (str
                          (if y-value-label-fn
                            (y-value-label-fn row)
                            (y-value-fn row))))))

            (-> texts
                .exit
                .remove))))

      om/IRenderState
      (render-state [_ {:keys [chart-pano-id path-id chan-update]}]
        (dom/div
         nil
         (dom/h4 #js {:className "" :style #js {:marginLeft 60}} title)
         (when description
           (dom/p #js {:className "text-info" :style #js {:marginLeft 60}} description))
         (dom/svg #js {:width main-width :height main-height}
                  (dom/g #js {:id chart-pano-id :transform (str "translate(" left "," top ")")}
                         (dom/g #js {:className "x axis" :transform (str "translate(0," chart-height ")")}
                                (dom/text #js {:y     -12
                                               :x     (- chart-width rigth)
                                               :dy    ".71em"
                                               :style #js {:textAnchor "end"}}
                                          y-label))
                         (dom/g #js {:className "y axis"}))))))))
