(ns r6p3s.cpt.d3.chart-pie
  (:require [cljs.core.async :refer [put! chan <!]]
            [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [r6p3s.core :as rc]
            [r6p3s.common-input :as common-input]
            [r6p3s.cpt.d3.core :as d3c]
            [cljsjs.d3]))



(defn mid-angle [d]
  (+ (.-startAngle d) (/ (- (.-endAngle d) (.-startAngle d)) 2.0)))



(def app-init
  {:data []})

(defn component [app own {:keys [
                                 main-width
                                 main-height
                                 top
                                 left
                                 rigth
                                 bottom
                                 values
                                 title
                                 colors
                                 description
                                 to-str-fn
                                 rotate-angle]
                          :or   {main-width   d3c/full-screen-width
                                 main-height  300
                                 top          15
                                 left         60
                                 rigth        15
                                 bottom       20
                                 colors       d3c/order-colors
                                 to-str-fn    str
                                 rotate-angle 0}

                          :as   opts}]

  (let [chart-width  (- main-width left rigth)
        chart-height (- main-height top bottom)]
    (reify
      om/IInitState
      (init-state [_]
        {:path-id       (rc/uniq-id "path")
         :chart-pano-id (rc/uniq-id "chart-pano")
         :chart-pano    nil})

      om/IDidMount
      (did-mount [_]
        (let [{:keys [chart-pano-id]} (om/get-state own)]
          (println "OM: chart-pie -> did-mount")
          (let [svg-pano (.select js/d3 (str "#" chart-pano-id))]
            (om/set-state! own :chart-pano svg-pano))))


      om/IDidUpdate
      (did-update [_ _ _]
        (let [{:keys [chart-pano path-id]} (om/get-state own)

              data                         (@app :data)

              data-array                   (into-array data)

              radius                       (/ (min chart-width chart-height) 2.0)

              color                        (-> js/d3 .-scale .ordinal
                                               (.range (clj->js colors)))

              arc                          (-> js/d3 .-svg .arc
                                               (.outerRadius (* radius 0.8))
                                               (.innerRadius (* radius 0.4))
                                               (.startAngle
                                                (fn [d] (+ (.-startAngle d) rotate-angle)))
                                               (.endAngle
                                                (fn [d] (+ (.-endAngle d) rotate-angle))))

              outer-arc                    (-> js/d3 .-svg .arc
                                               (.outerRadius (* radius 0.9))
                                               (.innerRadius (* radius 0.9))
                                               (.startAngle
                                                (fn [d] (+ (.-startAngle d) rotate-angle)))
                                               (.endAngle
                                                (fn [d] (+ (.-endAngle d) rotate-angle))))


              ;; label-arc                    (-> js/d3 .-svg .arc
              ;;                                  (.outerRadius (- radius 40))
              ;;                                  (.innerRadius (- radius 40)))

              pie                          (-> js/d3 .-layout .pie
                                               (.sort nil)
                                               (.value (fn [[_ v]] v)))


              g                            chart-pano

              ]

          (let [path (-> g
                         (.selectAll "path")
                         (.data (pie data-array)))]

            (-> path
                .enter
                (.append "path"))

            (-> path
                (.attr "d" arc)
                (.attr "fill" (fn [d] (-> d .-data first color))))

            (-> path
                .exit
                .remove))


          (let [text (-> g
                         (.selectAll "text")
                         (.data (pie data-array)))]

            (-> text
                .enter
                (.append "text")
                (.attr "dy" ".35em"))

            (-> text
                (.text (fn [d] (to-str-fn (.-data d))))
                ;;Старый вариант текста по середине
                ;;(.attr "transform" (fn [d] (str "translate(" (.centroid label-arc d) ")")))
                ;;Новый вариант
                (.attr
                 "transform"
                 (fn [d]
                   (let [pos       (.centroid outer-arc d)
                         [_ pos-1] (js->clj pos)
                         pos-0     (* radius (if (< (mid-angle d) js/Math.PI) 1 -1))]
                     (str "translate(" pos-0 "," pos-1 ")"))))
                (.style
                 "text-anchor"
                 (fn [d] (if (< (mid-angle d) js/Math.PI) "start" "end"))))

            (-> text
                .exit
                .remove))

          (let [polyline (-> g
                             (.selectAll "polyline")
                             (.data (pie data-array)))]

            (-> polyline
                .enter
                (.append "polyline"))

            (-> polyline
                (.attr
                 "points"
                 (fn [d]
                   (let [pos       (.centroid outer-arc d)
                         [_ pos-1] (js->clj pos)
                         pos-0     (* radius 0.95 (if (< (mid-angle d) js/Math.PI) 1 -1))]
                     (clj->js [(.centroid arc d)
                               (.centroid outer-arc d)
                               (clj->js [pos-0 pos-1])])))))

            (-> polyline
                .exit
                .remove))))


      om/IRenderState
      (render-state [_ {:keys [chart-pano-id path-id chan-update]}]
        (dom/div
         #js {:className "chart-frame"}
         (dom/h4 #js {:className "" :style #js {:marginLeft left}} title)
         (when description
           (dom/p #js {:className "text-info" :style #js {:marginLeft left}} description))
         (dom/svg #js {:width main-width :height main-height}
                  (dom/g #js {:id        chart-pano-id :transform (str "translate(" (/ chart-width 2.0) "," (/ chart-height 2.0) ")")
                              :className "arc"}
                         )))))))
