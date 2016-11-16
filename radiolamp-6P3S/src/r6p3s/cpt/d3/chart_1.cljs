(ns r6p3s.cpt.d3.chart-1
  (:require-macros [cljs.core.async.macros :refer [go]])
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
                                 on-brushend-fn
                                 x-value-fn
                                 y-value-fn
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
      ;;     (println "OM: chart-1 -> will-mount")

      ;;     (go
      ;;       (while true
      ;;         (let [_ (<! chan-update)]
      ;;           (println "OM: chart-1 -> chan-update -> run! "))))

      ;;     (put! chan-update 1)))

      om/IDidMount
      (did-mount [_]
        (let [{:keys [chart-pano-id]} (om/get-state own)]
          (println "OM: chart-1 -> did-mount")
          (let [svg-pano (.select js/d3 (str "#" chart-pano-id))]
            (om/set-state! own :chart-pano svg-pano))))

      om/IWillUnmount
      (will-unmount [_]
        (let [{:keys []} (om/get-state own)]
          (println "OM: chart-1 -> will-unmount")))




      om/IDidUpdate
      (did-update [_ _ _]
        (let [{:keys [chart-pano path-id]} (om/get-state own)

              data                         (@app :data)

              x-scale                      (-> js/d3 .-time  .scale
                                               (.domain (d3c/min-max #(-> % x-value-fn .getTime) min max data))
                                               ;;(.domain (.extent js/d3 data :pdate))
                                               (.range #js [0 chart-width]))

              y-scale                      (-> js/d3 .-scale .linear
                                               (.domain (d3c/min-max y-value-fn min 1 max 1.1 data))
                                               (.range  #js [chart-height 0]))

              x-axis                       (-> js/d3 .-svg .axis (.scale x-scale) (.orient "bottom"))
              y-axis                       (-> js/d3 .-svg .axis (.scale y-scale) (.orient "left"))


              area-1                       (-> js/d3 .-svg .area
                                               (.x  #(x-scale (x-value-fn %)))
                                               (.y0 chart-height)
                                               (.y1  #(y-scale (y-value-fn %))))


              ;; line                  (-> js/d3
              ;;                           .-svg
              ;;                           .line
              ;;                           (.x (fn [{:keys [ptime]}]
              ;;                                 (x-scale ptime)))
              ;;                           (.y (fn [{:keys [cs_all]}]
              ;;                                 (println "y>>" cs_all ">>" (y-scale cs_all))
              ;;                                 (y-scale cs_all)))
              ;;                           (.interpolate "basis"))

              brush                        (when on-brushend-fn
                                             (-> js/d3 .-svg .brush (.x x-scale)))

              data-array                   (into-array data)]

          ;; Селектор
          (when on-brushend-fn
            (.on brush "brushend"
                 (fn [v]
                   (let [selected (js->clj (.extent brush))]
                     (on-brushend-fn selected)))))

          ;; (-> a
          ;;     .enter
          ;;     (.append "circle")
          ;;     (.attr "cx" (fn [i] (println i) (* i 30)))
          ;;     (.attr "cy" 200)
          ;;     (.attr "r" 5)
          ;;     (.style "fill" "red")
          ;;     (.style "stroke" "blueviolet"))



          (-> chart-pano
              (.select (str "#" path-id))
              (.datum data-array)
              (.attr "d" area-1)
              (.style "fill" "blueviolet"))

          (-> chart-pano
              (.selectAll "g.y.axis")
              (.call y-axis))

          (-> chart-pano
              (.selectAll "g.x.axis")
              (.call x-axis))

          (when on-brushend-fn
            (-> chart-pano
                (.selectAll "g.x.brush")
                (.call brush)
                (.selectAll "rect")
                (.attr "y" -6)
                (.attr "height" (+ chart-height 7))))

          ))

      om/IRenderState
      (render-state [_ {:keys [chart-pano-id path-id chan-update]}]
        (dom/div
         nil
         (dom/h4 #js {:className "" :style #js {:marginLeft left}} title)
         (when description
           (dom/p #js {:className "text-info" :style #js {:marginLeft left}} description))
         (dom/svg #js {:width main-width :height main-height}
                  (dom/g #js {:id chart-pano-id :transform (str "translate(" left "," top ")")}
                         (dom/path #js {:id path-id})
                         (dom/g #js {:className "y axis"}
                                (dom/text #js {:transform "rotate(-90)"
                                               :y         "2" :dy ".71em" :style #js {:textAnchor "end"}}
                                          y-label))
                         (dom/g #js {:className "x axis" :transform (str "translate(0," chart-height ")")})
                         (dom/g #js {:className "x brush" :width chart-width :height chart-height}))))))))
