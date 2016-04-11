(ns r6p3s.cpt.d3.chart-pie
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
                                 values
                                 title
                                 colors
                                 description]
                          :or   {main-width  d3c/full-screen-width
                                 main-height 300
                                 top         15
                                 left        60
                                 rigth       15
                                 bottom      20
                                 colors      d3c/order-colors}

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
                                               (.outerRadius (- radius 10))
                                               (.innerRadius 0))

              label-arc                    (-> js/d3 .-svg .arc
                                               (.outerRadius (- radius 40))
                                               (.innerRadius (- radius 40)))

              pie                          (-> js/d3 .-layout .pie
                                               (.sort nil)
                                               (.value (fn [[_ v]] v)))


              g                            (-> chart-pano
                                               (.selectAll ".arc")
                                               (.data (pie data-array)))]

          (let [g (-> g
                      .enter
                      (.append "g")
                      (.attr "class" "arc"))]
            (-> g (.append "path"))
            (-> g (.append "text")
                (.attr "dy" ".35em")))


          (-> g
              (.select "path")
              (.attr "d" arc)
              (.attr "fill" (fn [d] (-> d .-data first color))))


          (-> g
              (.select "text")
              (.text (fn [d] (-> d .-data second)))
              (.attr "transform" (fn [d] (str "translate(" (.centroid label-arc d) ")"))))


          (-> g
              .exit
              .remove)))


      om/IRenderState
      (render-state [_ {:keys [chart-pano-id path-id chan-update]}]
        (dom/div
         #js {:className "chart-frame"}
         (dom/h3 #js {:className "" :style #js {:marginLeft left}} title)
         (dom/p #js {:className "text-info" :style #js {:marginLeft left}} description)
         (dom/svg #js {:width main-width :height main-height}
                  (dom/g #js {:id chart-pano-id :transform (str "translate(" (/ chart-width 2.0) "," (/ chart-height 2.0) ")")}
                         )))))))
