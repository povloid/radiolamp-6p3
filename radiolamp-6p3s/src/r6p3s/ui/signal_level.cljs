(ns r6p3s.ui.signal-level
  (:require [om.dom :as dom :include-macros true]
            [r6p3s.core :as c]))



(defn render [v {:keys []}]
  (let [c (cond (< v 4) "red"
                (< v 7) "yellow"
                :else   "green")]
    (->> (range 1 11)
         (reduce
          (fn [a i]
            (let [y      (- 22 (* (inc i) 2))
                  height (- 22 y)]
              (conj a
                    (dom/rect #js {:width  2
                                   :height height
                                   :x      (* i 4)
                                   :y      y
                                   :fill   (if (<= i v) c "#bbb")}))))
          [])
 
         (apply dom/svg #js {:width 50 :height 22
                             :style #js {:margin 2}}))))




